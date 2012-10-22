package sia.ch5;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import sia.ExampleDriver;

/**
 * Minimal example client to index documents using SolrJ version 4.x+
 */
public class ExampleSolrJClient extends ExampleDriver.SolrJClientExample {

    public String getDescription() {
        return "Use SolrJ client API to index some example documents and then query Solr.";
    }
    
    public void runExample(ExampleDriver driver) throws Exception {
        // read in server URL as parameter or assume local if not supplied
        String serverUrl =
            driver.getCommandLine().getOptionValue("solr", ExampleDriver.DEFAULT_SOLR_URL);

        // Get a connection to Solr
        SolrServer solr = new HttpSolrServer(serverUrl);

        // Add some example docs
        SolrInputDocument doc1 = new SolrInputDocument();
        doc1.setField("id", "1");
        doc1.setField("screen_name_s", "@thelabdude");
        doc1.setField("type_s", "post");
        doc1.setField("lang_s", "en");
        doc1.setField("timestamp_tdt", "2012-05-22T09:30:22Z/HOUR");
        doc1.setField("favourites_count_ti", "10");
        doc1.setField("text_t", "#Yummm :) Drinking a latte at Caffe Grecco in SF's" +
          " historic North Beach... Learning text analysis with #SolrInAction by @Manning on my i-Pad");

        solr.add(doc1);

        SolrInputDocument doc2 = new SolrInputDocument();
        doc2.setField("id", "2");
        doc2.setField("screen_name_s", "@thelabdude");
        doc2.setField("type_s", "post");
        doc2.setField("lang_s", "en");
        doc2.setField("timestamp_tdt", "2012-05-22T09:30:22Z/HOUR");
        doc2.setField("favourites_count_ti", "10");
        doc2.setField("text_t", "Just downloaded the MEAP for #SolrInAction from" +
          " @Manning http://bit.ly/15tzw to learn more about #Solr http://bit.ly/3ynriE");
        doc2.addField("link_ss", "http://manning.com/");
        doc2.addField("link_ss", "http://lucene.apache.org/solr/");

        solr.add(doc2);

        // Make the docs we just added searchable using a "hard" commit
        solr.commit(true, true);

        // Send and process the first 10 rows of the match all docs query
        for (SolrDocument next : simpleSolrQuery(solr, "*:*", 10)) {
            prettyPrint(System.out, next);
        }        
    }

    SolrDocumentList simpleSolrQuery(SolrServer solr, String query, int rows) throws SolrServerException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setRows(rows);
        QueryResponse resp = solr.query(solrQuery);
        SolrDocumentList hits = resp.getResults();
        return hits;
    }

    void prettyPrint(PrintStream out, SolrDocument doc) {
        List<String> sortedFieldNames = new ArrayList<String>(doc.getFieldNames());
        Collections.sort(sortedFieldNames);
        out.println();
        for (String field : sortedFieldNames) {
            out.println(String.format("\t%s: %s", field, doc.getFieldValue(field)));
        }
        out.println();
    }
}
