package sia.ch5;

import java.util.Random;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

import sia.ExampleDriver;

/**
 * Fill a Solr index with test documents for learning about precision step.
 */
public class PrecisionStepBenchmark extends ExampleDriver.SolrJClientExample {
    
    public String getDescription() {
        return "Build an index with docs having a single integer field " +
        		"for doing an informal benchmark of precision step values.";
    }

    public void runExample(ExampleDriver driver) throws Exception {
        String serverUrl =
            driver.getCommandLine().getOptionValue("solr", ExampleDriver.DEFAULT_SOLR_URL);
        SolrServer solr = new ConcurrentUpdateSolrServer(serverUrl, 1000, 2);

        Random random = new Random(500L);

        int minPrice = 110000;
        int maxPrice = 5000000 - minPrice;
        int numDocs = 500000;

        for (int d=0; d < numDocs; d++) {

            int randomPrice = random.nextInt(maxPrice);
            randomPrice += minPrice;

            // round to nearest $100
            randomPrice = randomPrice - (randomPrice % 100);

            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("id", Integer.toString(d));
            doc.setField("listing_price", randomPrice);
            solr.add(doc);
        }
        System.out.println(">> Done adding "+numDocs+" docs. Committing ...");
        solr.commit(true, true);
        System.out.println(">> Optimizing ...");
        solr.optimize(true, true, 1);
        System.out.println(">> Done.");

        // queries
        SolrServer solrServer = new HttpSolrServer(serverUrl);
        int numQueries = 10000;
        int randomPrice = 0;
        int randomPrice2 = 0;
        String queryStr;
        SolrQuery query = new SolrQuery();
        query.setStart(0);
        query.setRows(10);
        query.setFields("id","listing_price");
        long startAt = System.currentTimeMillis();
        for (int q=0; q < numQueries; q++) {
            randomPrice = random.nextInt(maxPrice) + minPrice;
            randomPrice = randomPrice - (randomPrice % 100);

            randomPrice2 = random.nextInt(maxPrice) + minPrice;
            randomPrice2 = randomPrice2 - (randomPrice2 % 100);
            while (randomPrice2 == randomPrice) {
                randomPrice2 = random.nextInt(maxPrice) + minPrice;
                randomPrice2 += minPrice;
                randomPrice2 = randomPrice2 - (randomPrice2 % 100);
            }

            if (randomPrice2 > randomPrice) {
                queryStr = String.format("listing_price:[%d TO %d]", randomPrice, randomPrice2);
            } else {
                queryStr = String.format("listing_price:[%d TO %d]", randomPrice2, randomPrice);
            }
            query.setQuery(queryStr);
            QueryResponse resp = solrServer.query(query);
            resp = null;

            //if (q % 10000 == 0) {
            //    System.out.println(">> done with "+q+" queries");
            //}
        }
        long diff = (System.currentTimeMillis() - startAt);
        double avg = (double)diff/numQueries;
        System.out.println(">> avg: "+avg);
    }
}
