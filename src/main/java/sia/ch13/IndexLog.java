package sia.ch13;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import sia.ExampleDriver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Index log messages into the logmill search index.
 */
public class IndexLog extends ExampleDriver.SolrJClientExample {

    public static Logger log = Logger.getLogger(IndexLog.class);

    private static final String ZK_HOST = "localhost:9983";
    private static final String COLLECTION = "logmill";

    private static final SimpleDateFormat TS_PARSER = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.S");

    /*
     * Enum of log formats supported by this log indexer utility.
     */
    static enum LogFormat {
        solr;
    }

    private StringBuilder multilineBuf = new StringBuilder();
    private int currentMultiline = -1;

    public String getDescription() {
        return "Parse solr.log to create log messages in logmill search index.";
    }

    @SuppressWarnings("static-access")
    public Option[] getOptions() {
        return new Option[] {
                OptionBuilder.withArgName("connectString").hasArg().isRequired(false)
                        .withDescription("Zookeeper connection string, default: localhost:9983").create("zkhost"),
                OptionBuilder.withArgName("COLLECTION").hasArg().isRequired(false)
                        .withDescription("Collection name, default: logmill").create("collection"),
                OptionBuilder.withArgName("MS").hasArg().isRequired(false)
                        .withDescription("Zookeeper client timeout in ms, default: 15000").create("zkClientTimeout"),
                OptionBuilder.withArgName("FILE").hasArg().isRequired(true)
                        .withDescription("Path to log file to index").create("log"),
                OptionBuilder.withArgName("fmt").hasArg().isRequired(false)
                        .withDescription("Log file format e.g. solr").create("format"),
                OptionBuilder.withArgName("#").hasArg().isRequired(false)
                        .withDescription("Batch size, default is 500").create("batchSize")
        };
    }

    /**
     * Main method of this example.
     */
    public void runExample(ExampleDriver driver) throws Exception {
        long startMs = System.currentTimeMillis();

        CommandLine cli = driver.getCommandLine();

        // Size of index batch requests to Solr
        int batchSize = Integer.parseInt(cli.getOptionValue("batchSize", "500"));

        // Get a connection to Solr cloud using Zookeeper
        String zkHost = cli.getOptionValue("zkhost", ZK_HOST);
        String collectionName = cli.getOptionValue("collection", COLLECTION);
        int zkClientTimeout = Integer.parseInt(cli.getOptionValue("zkClientTimeout", "15000"));

        CloudSolrServer solr = new CloudSolrServer(zkHost);
        solr.setDefaultCollection(collectionName);
        solr.setZkClientTimeout(zkClientTimeout);
        solr.connect();

        int numSent = 0;
        int numSkipped = 0;
        int lineNum = 0;
        SolrInputDocument doc = null;
        String line = null;

        // read file line-by-line
        BufferedReader reader = new BufferedReader(driver.readFile("log"));
        driver.rememberCloseable(reader);

        LogFormat fmt = LogFormat.valueOf(cli.getOptionValue("format", "solr"));

        // process each sighting as a document
        while ((line = reader.readLine()) != null) {
            doc = parseNextDoc(line, ++lineNum, fmt);
            if (doc != null) {
                addDocWithRetry(solr, doc, 10);
                ++numSent;
            } else {
                ++numSkipped;
                continue;
            }

            if (lineNum % 1000 == 0)
                log.info(String.format("Processed %d lines.", lineNum));
        }

        // hard commit all docs sent
        solr.commit(true,true);

        float tookSecs = Math.round(((System.currentTimeMillis() - startMs)/1000f)*100f)/100f;
        log.info(String.format("Sent %d log messages (skipped %d) took %f seconds", numSent, numSkipped, tookSecs));
        
        // queries to demonstrate results of indexing
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.setRows(0);
        QueryResponse resp = solr.query(solrQuery);
        SolrDocumentList hits = resp.getResults();        
        log.info("Match all docs distributed query found "+hits.getNumFound()+" docs.");        

        solrQuery.set("shards", "shard1");
        resp = solr.query(solrQuery);
        hits = resp.getResults();        
        log.info("Match all docs non-distributed query to shard1 found "+hits.getNumFound()+" docs.");        

        solrQuery.set("shards", "shard2");
        resp = solr.query(solrQuery);
        hits = resp.getResults();        
        log.info("Match all docs non-distributed query to shard2 found "+hits.getNumFound()+" docs.");        
        
        solr.shutdown();        
    }
    
    /**
     * Send a document to Solr for indexing with re-try support in case of communication exception. 
     */
    protected void addDocWithRetry(CloudSolrServer solr, SolrInputDocument doc, int retryInSecs) 
        throws Exception
    {
      try {
        solr.add(doc);
      } catch (Exception solrExc) {
        // add some basic re-try logic in the event of a communication error
        Throwable rootCause = SolrException.getRootCause(solrExc);
        if (rootCause instanceof IOException) {
          log.error(rootCause.getClass().getSimpleName()+
              " when trying to send a document to SolrCloud, will re-try after waiting "+
              retryInSecs+" seconds; "+rootCause);
          try {
            Thread.sleep(retryInSecs*1000);
          } catch (InterruptedException ignoreMe) {}
          // re-try this doc
          solr.add(doc);
        }
      }      
    }

    /**
     * Transforms a line from a log file to a SolrInputDocument.
     * @param line
     * @param lineNum
     * @return
     */
    protected SolrInputDocument parseNextDoc(String line, int lineNum, LogFormat fmt) {
        line = line.trim();

        if (line.length() == 0)
            return null;

        SolrInputDocument doc = null;

        if (fmt == LogFormat.solr) {

            if (currentMultiline == -1 && line.endsWith("{")) {
                // multi-line log message ... buffer lines until we reach the closing }
                currentMultiline = lineNum;
                multilineBuf.setLength(0);
                multilineBuf.append(line);
                return null;
            }

            if (currentMultiline != -1) {
                multilineBuf.append(line);

                if (line.endsWith("}")) {
                    // captured all lines
                    lineNum = currentMultiline;
                    currentMultiline = -1;
                    line = multilineBuf.toString().replace("\\s+", " ");
                } else {
                    // still capturing lines
                    return null;
                }
            }

            doc = parseSolrLogLine(line, lineNum);
        } else {
            throw new IllegalStateException("LogFormat "+fmt+" not supported!");
        }

        return doc;
    }

    protected SolrInputDocument parseSolrLogLine(String line, int lineNum) {

        int prev = 0;
        int pos = line.indexOf(" - ");
        if (pos != 5)
            return null;

        String level = line.substring(0,5).trim();
        prev = pos+3;
        pos = line.indexOf(";", prev);
        if (pos == -1)
            return null;

        String timestamp = line.substring(prev, pos).trim();
        prev = pos+1;

        pos = line.indexOf(";", prev);
        if (pos == -1)
            return null;

        Date timestampDt = null;
        try {
            timestampDt = TS_PARSER.parse(timestamp);
        } catch (ParseException pe) {
            log.warn("Failed to parse timestamp at line "+lineNum+" due to: "+pe);
            return null;
        }

        String category = line.substring(prev, pos).trim();

        String message = line.substring(pos+1).trim();

        // unique ID based on host, line num, log level, and timestamp
        String docId = String.format("%s/%d/%s/%d/%s",
                "localhost",
                lineNum,
                level,
                timestampDt.getTime(),
                ExampleDriver.getMD5Hash(message)).toLowerCase();

        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", docId);
        doc.setField("source_s", "solr"); // name of the app that created the log message
        doc.setField("host_s", "localhost:8983"); // host and port of app that created the log message
        doc.setField("level_s", level);
        doc.setField("timestamp_tdt", timestampDt);
        doc.setField("category_s", category);
        doc.setField("text_en", message);

        log.info("Parsed log message at line "+lineNum+" into SolrInputDocument: "+doc);

        return doc;
    }    
}
