package sia.ch13;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.util.NamedList;
import sia.ExampleDriver;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Command-line utility for creating backups using Solr replication handler
 * on each shard leader in the cluster. This application issues the backups
 * and then blocks until all backups are completed on each shard leader, typically
 * about 5-10 minutes. Once the backup completes, you can move the backup files
 * outside of the data center, such as S3. This process is for disaster recovery
 * vs. day-to-day operations.
 */
public class BackupDriver extends ExampleDriver.SolrJClientExample {

    private static final int ZK_CLIENT_TIMEOUT = 15000; // 15 sec timeout for zk ops
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final long MS_IN_MIN = 60 * 1000L;

    private static final String DEFAULT_ZK_HOST = "localhost:9983";
    private static final String DEFAULT_BACKUP_DIR = "/tmp";
    private static final String DEFAULT_COLLECTION_NAME = "logmill";

    public static Logger log = Logger.getLogger(BackupDriver.class);

    // Holds the backup state for a shard
    private class ShardBackupState {
        String shard;
        HttpSolrServer solrServer;
        Date completedAt;

        ShardBackupState(String shard, HttpSolrServer solrServer) {
            this.shard = shard;
            this.solrServer = solrServer;
        }

        boolean isComplete() {
            return (completedAt != null);
        }
    }

    public String getDescription() {
        return "Create a backup of every shard in a collection using the replication handler.";
    }

    @SuppressWarnings("static-access")
    public Option[] getOptions() {
        return new Option[] {
                OptionBuilder.withArgName("ZK_HOST").hasArg().isRequired(false)
                    .withDescription("Address of the Zookeeper ensemble; defaults to: "+DEFAULT_ZK_HOST).create("zkHost"),
                OptionBuilder.withArgName("DIR").hasArg().isRequired(false)
                    .withDescription("Backup location on each host, defaults to: "+DEFAULT_BACKUP_DIR).create("backupDir"),
                OptionBuilder.withArgName("COLLECTION").hasArg().isRequired(false)
                    .withDescription("Name of collection; defaults to: "+DEFAULT_COLLECTION_NAME).create("collection"),
                OptionBuilder.withArgName("yyyy-MM-ddTHH:mm:ss").hasArg().isRequired(false)
                    .withDescription("Useful for a failed run where all the backup requests were queued but then app failed.").create("restartDate"),
                OptionBuilder.withArgName("<INT>").hasArg().isRequired(false)
                    .withDescription("Number of minutes to sleep between status checks, defaults to: 1").create("sleepInterval"),
                OptionBuilder.withArgName("<INT>").hasArg().isRequired(false)
                    .withDescription("Max minutes to wait for backups to complete, defaults to: 10").create("maxWaitForCompletion")
        };
    }

    public void runExample(ExampleDriver driver) throws Exception {

        long enterMs = System.currentTimeMillis();

        CommandLine cli = driver.getCommandLine();

        // process command-line args to configure this application
        String collection = cli.getOptionValue("collection", DEFAULT_COLLECTION_NAME);
        String backupLocation = cli.getOptionValue("backupDir", DEFAULT_BACKUP_DIR);
        String zkHost = cli.getOptionValue("zkHost", DEFAULT_ZK_HOST);
        String restartDate = cli.getOptionValue("restartDate");
        int sleepInterval = Integer.parseInt(cli.getOptionValue("sleepInterval", "1"));
        int maxWaitForCompletion = Integer.parseInt(cli.getOptionValue("maxWaitForCompletion", "10"));

        // Connect to Zookeeper to get a Map of shard names and the URL for the leaders
        // as we only need to backup the leader's index
        Map<String,String> leaders = null;
        log.info("Connecting to Solr cloud cluster: "+zkHost);
        CloudSolrServer cloudSolrServer = null;
        try {
            cloudSolrServer = new CloudSolrServer(zkHost);
            cloudSolrServer.setDefaultCollection(collection);
            cloudSolrServer.connect();

            log.info("Doing a hard commit ...");
            long startMs = System.currentTimeMillis();
            cloudSolrServer.commit();
            long diffMs = (System.currentTimeMillis() - startMs);

            log.info("Commit succeeded, took: "+diffMs+" (ms). Collecting cluster state information ...");
            leaders = getShardLeaders(cloudSolrServer, collection);
        } finally {
            if (cloudSolrServer != null) {
                try {
                    cloudSolrServer.shutdown();
                } catch (Exception ignore){}
            }
        }
        log.info("Found leaders for "+leaders.size()+" shards");

        Date now = null;
        if (restartDate != null) {
            now = DATE_FMT.parse(restartDate);
        } else {
            Calendar rightNow = Calendar.getInstance();
            rightNow.set(Calendar.MILLISECOND, 0); // zero out millis for local backups of small indexes
            now = rightNow.getTime();
        }
        log.info("NOW: "+DATE_FMT.format(now)+" <-- Useful if you need to restart with -restartDate param");

        Map<String,ShardBackupState> leaderClients = new HashMap<String,ShardBackupState>();
        for (String shard : leaders.keySet()) {
            String leaderUrl = leaders.get(shard);
            HttpSolrServer solr = new HttpSolrServer(leaderUrl);
            if (restartDate == null) {
                HttpClient httpClient = solr.getHttpClient();
                log.info("Sending backup request to "+shard+" leader at: "+leaderUrl);

                String getUrl = String.format("%sreplication?command=backup&location=%s/%s",
                        leaderUrl, URLEncoder.encode(backupLocation,"UTF-8"), shard);
                driver.sendRequest(httpClient, getUrl);
                Thread.sleep(1000L); // slight delay between requests
            }

            leaderClients.put(leaderUrl, new ShardBackupState(shard, solr));
        }

        // max wait
        long startAt = System.currentTimeMillis();
        long maxWaitMs = maxWaitForCompletion * MS_IN_MIN;

        log.info("Starting to poll each shard leader for backup status ...");

        int finished = 0;
        while ((System.currentTimeMillis() - startAt) < maxWaitMs) {
            boolean allFinished = true;

            for (String leaderUrl : leaderClients.keySet()) {

                ShardBackupState backupState = leaderClients.get(leaderUrl);
                if (backupState.isComplete())
                    continue;

                Date snapshotCompletedAt = null;
                try {
                    snapshotCompletedAt =
                        requestSnapshotCompletedAt(driver, backupState.solrServer.getHttpClient(),
                                                   leaderUrl+"replication?command=details");
                } catch (Exception exc) {
                    log.error("Failed to get snapshot completed at date for shard "+
                        backupState.shard+" from ["+leaderUrl+"] due to: "+exc, exc);
                }

                if (snapshotCompletedAt == null || snapshotCompletedAt.before(now)) {
                    // keep looping as this shard leader is not done yet
                    allFinished = false;

                    log.info("Leader for "+backupState.shard+" at ["+leaderUrl+"] is NOT finished backing up");
                } else {
                    ++finished;
                    backupState.completedAt = snapshotCompletedAt;
                    log.info("Leader for "+backupState.shard+" at ["+leaderUrl+
                        "] finished backing up at: "+DATE_FMT.format(snapshotCompletedAt));
                }
            }

            if (allFinished)
                break;

            log.info("Found "+finished+" of "+leaderClients.size()+
                " leaders finished backing up. Sleeping for "+sleepInterval+" minutes before checking again ...");

            try {
                Thread.sleep(sleepInterval * MS_IN_MIN);
            } catch (InterruptedException ie) {
              // just ignore it
            }
        }

        // shutdown connections to all Solr servers
        for (String leaderUrl : leaderClients.keySet()) {
            leaderClients.get(leaderUrl).solrServer.shutdown();
        }

        long exitMs = System.currentTimeMillis();
        log.info("BackupDriver completed successfully after "+Math.round((exitMs-enterMs)/1000d)+" seconds");
    }

    /**
     * Request details from the replication handler to extract the snapshotCompletedAt
     * date for the current backup.
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    private Date requestSnapshotCompletedAt(ExampleDriver driver, HttpClient httpClient, String getUrl) throws Exception {

        NamedList<Object> details = driver.sendRequest(httpClient, getUrl);
        if (details.get("details") != null) {
            details = (NamedList<Object>)details.get("details");
        }

        Date completedAt = null;
        NamedList<Object> backup = (NamedList<Object>)details.get("backup");
        if (backup != null) {
            String snapshotCompletedAt = (String)backup.get("snapshotCompletedAt");
            if (snapshotCompletedAt != null) {
                log.info("Replication details for "+getUrl+" returned snapshotCompletedAt: "+snapshotCompletedAt);
                completedAt = new Date(snapshotCompletedAt);
            }
        }

        return completedAt;
    }

    private final Map<String,String> getShardLeaders(CloudSolrServer solr, String collection) throws Exception {
        Map<String,String> leaders = new TreeMap<String,String>();
        ZkStateReader zkStateReader = solr.getZkStateReader();
        for (Slice slice : zkStateReader.getClusterState().getSlices(collection)) {
            leaders.put(slice.getName(), zkStateReader.getLeaderUrl(collection, slice.getName(), ZK_CLIENT_TIMEOUT));
        }
        return leaders;
    }
}
