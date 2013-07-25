package sia.ch13;

import org.apache.solr.cloud.CloudDescriptor;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.PingRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends Solr's PingRequestHandler to check a replica's cluster status as part of the health check.
 */
public class ClusterStateAwarePingRequestHandler extends PingRequestHandler {

    public static Logger log = LoggerFactory.getLogger(ClusterStateAwarePingRequestHandler.class);

    @Override
    public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse) throws Exception {
        // delegate to the base class to check the status of this local index
        super.handleRequestBody(solrQueryRequest, solrQueryResponse);

        // if ping status is OK, then check cluster state of this core
        if ("OK".equals(solrQueryResponse.getValues().get("status"))) {
            verifyThisReplicaIsActive(solrQueryRequest.getCore());
        }
    }

    /**
     * Verifies this replica is "active".
     */
    protected void verifyThisReplicaIsActive(SolrCore solrCore) throws SolrException {
        String replicaState = "unknown";
        String nodeName = "?";
        String shardName = "?";
        String collectionName = "?";
        String role = "?";
        Exception exc = null;
        try {
            CoreDescriptor coreDescriptor = solrCore.getCoreDescriptor();
            CoreContainer coreContainer = coreDescriptor.getCoreContainer();
            CloudDescriptor cloud = coreDescriptor.getCloudDescriptor();

            shardName = cloud.getShardId();
            collectionName = cloud.getCollectionName();
            role = (cloud.isLeader() ? "Leader" : "Replica");

            ZkController zkController = coreContainer.getZkController();
            if (zkController != null) {
                nodeName = zkController.getNodeName();
                if (zkController.isConnected()) {
                    ClusterState clusterState = zkController.getClusterState();
                    Slice slice = clusterState.getSlice(collectionName, shardName);
                    replicaState = (slice != null) ? slice.getState() : "gone";
                } else {
                    replicaState = "not connected to Zookeeper";
                }
            } else {
                replicaState = "Zookeeper not enabled/configured";
            }
        } catch (Exception e) {
            replicaState = "error determining cluster state";
            exc = e;
        }

        if ("active".equals(replicaState)) {
            log.info(String.format("%s at %s for %s in the %s collection is active.",
                    role, nodeName, shardName, collectionName));
        } else {
            // fail the ping by raising an exception
            String errMsg = String.format("%s at %s for %s in the %s collection is not active! State is: %s",
                    role, nodeName, shardName, collectionName, replicaState);
            if (exc != null) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, errMsg, exc);
            } else {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, errMsg);
            }
        }
    }
}
