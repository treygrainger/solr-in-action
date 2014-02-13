package sia;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

/**
 * Simple utility for executing HTTP requests from a
 * Solr In Action listing against your local Solr server.
 */
public class Http implements ExampleDriver.Example {

    static final Map<String, Object> listings = new HashMap<String, Object>();
    static {
        // chapter 2: Getting to know Solr
        listings.put("2.1",
          "/collection1/select?q=iPod&fq=manu%3ABelkin&sort=price+asc&fl=name%2Cprice%2Cfeatures%2Cscore&df=text&wt=xml&start=0&rows=10");
 
        // chapter 4: Configuring Solr
        listings.put("4.4",
          "/collection1/select?q=iPod&fq=manu%3ABelkin&sort=price+asc&fl=name%2Cprice%2Cfeatures%2Cscore&df=text&wt=xml&start=0&rows=10");
        listings.put("4.7",
          "/collection1/browse?q=iPod&wt=xml&echoParams=all");
        listings.put("4.8",
          "/collection1/select?q=*%3A*&wt=xml&stats=true&stats.field=price");
        listings.put("4.9",
          "/collection1/browse?q=iPod&wt=xml&debugQuery=true");
        listings.put("4.12",
          "/collection1/select?q=iPod&fq=manu%3ABelkin&sort=price+asc&fl=name%2Cprice%2Cfeatures%2Cscore&df=text&wt=xml&start=0&rows=10");

        //chapter 8: Faceted Search
        listings.put("8.3",
          "/restaurants/select?q=*:*&rows=0&facet=true&facet.field=name");
        listings.put("8.4",
          "/restaurants/select?q=*:*&facet=true&facet.field=tags");
        listings.put("8.5",
          "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=state&f.state.facet.limit=50&f.state.facet.sort=index&facet.field=name&f.name.facet.mincount=1&facet.field=tags&f.tags.facet.limit=5");
        listings.put("8.6", new String[]{
          "/restaurants/select?q=*:*&fq=price:[5%20TO%2025]",
          "/restaurants/select?q=*:*&fq=price:[5%20TO%2025]&fq=state:(%22New%20York%22%20OR%20%22Georgia%22%20OR%20%22South%20Carolina%22)",
          "/restaurants/select?q=*:*&fq=price:[5%20TO%2025]&fq=state:(%22Illinois%22%20OR%20%22Texas%22)",
          "/restaurants/select?q=*:*&fq=price:[5%20TO%2025]&fq=state:(%22California%22)"});
        listings.put("8.7", 
            "/restaurants/select?q=*:*&fq=price:[5%20TO%2025]&facet=true&facet.query=state:(%22New%20York%22%20OR%20%22Georgia%22%20OR%20%22South%20Carolina%22)&facet.query=state:(%22Illinois%22%20OR%20%22Texas%22)&facet.query=state:(%22California%22)");
        listings.put("8.8",
            "/restaurants/select?q=*:*&rows=0&facet=true&facet.query=price:[*%20TO%205%7D&facet.query=price:[5%20TO%2010%7D&facet.query=price:[10%20TO%2020%7D&facet.query=price:[20%20TO%2050%7D&facet.query=price:[50%20TO%20*]"); 
        listings.put("8.9",
            "/restaurants/select?q=*:*&facet=true&facet.range=price&facet.range.start=0&facet.range.end=50&facet.range.gap=5"); 
        listings.put("8.10",
            "/restaurants/select?q=*:*&facet=true&facet.field=state&facet.field=city&facet.query=price:[*%20TO%2010%7D&facet.query=price:[10%20TO%2025%7D&facet.query=price:[25%20TO%2050%7D&facet.query=price:[50%20TO%20*]");
        listings.put("8.11", "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=state&facet.field=city&facet.query=price:[*%20TO%2010%7D&facet.query=price:[10%20TO%2025%7D&facet.query=price:[25%20TO%2050%7D&facet.query=price:[50%20TO%20*]&fq=state:California");        
        listings.put("8.12", "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=state&facet.field=city&facet.query=price:[*%20TO%2010%7D&facet.query=price:[10%20TO%2025%7D&facet.query=price:[25%20TO%2050%7D&facet.query=price:[50%20TO%20*]&fq=state:California&fq=price:[*%20TO%2010%7D");
        listings.put("8.13", new String[]{
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=name&facet.field=tags",
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=name&facet.field=tags&fq=tags:coffee",
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=name&facet.field=tags&fq=tags:coffee&fq=tags:hamburgers"});                          
        listings.put("8.14", new String[]{
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=name&facet.field=tags&fq=%7B!term%20f=tags%7Dcoffee&fq=%7B!term%20f=tags%7Dhamburgers",
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=name&facet.field=tags&fq=_query_:%22%7B!term%20f=tags%7Dcoffee%22%20AND%20_query_:%22%7B!term%20f=tags%7Dhamburgers%22"});
        listings.put("8.15", new String[]{
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=city&facet.query=price:[*%20TO%2010%7D&facet.query=price:[10%20TO%2025%7D&facet.query=price:[25%20TO%2050%7D&facet.query=price:[50%20TO%20*]",
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=%7B!key=%22Location%22%7Dcity&facet.query=%7B!key=%22%3C%2410%22%7Dprice:[*%20TO%2010%7D&facet.query=%7B!key=%22%2410%20-%20%2425%22%7Dprice:[10%20TO%2025%7D&facet.query=%7B!key=%22%2425%20-%20%2450%22%7Dprice:[25%20TO%2050%7D&facet.query=%7B!key=%22%3E%2450%22%7Dprice:[50%20TO%20*]"});
        listings.put("8.16", 
            "/restaurants/select?q=*:*&facet=true&facet.mincount=1&facet.field=%7B!ex=tagForState%7Dstate&facet.field=%7B!ex=tagForCity%7Dcity&facet.query=%7B!ex=tagForPrice%7Dprice:[*%20TO%205%7D&facet.query=%7B!ex=tagForPrice%7Dprice:[5%20TO%2010%7D&facet.query=%7B!ex=tagForPrice%7Dprice:[10%20TO%2020%7D&facet.query=%7B!ex=tagForPrice%7Dprice:[20%20TO%2050%7D&facet.query=%7B!ex=tagForPrice%7Dprice:[50%20TO%20*]&fq=%7B!tag=%22tagForState%22%7Dstate:California");

        // chapter 9: Hit Highlighting
        listings.put("9.1",
            "/ufo/select?q=blue+fireball+in+the+rain&df=sighting_en&rows=10&wt=xml&hl=true");
        listings.put("9.2",
            "/ufo/select?q=blue+fireball+in+the+rain&df=sighting_en&wt=xml&hl=true&hl.snippets=2");
        listings.put("9.4",
            "/ufo/select?q=blue+fireball+in+the+rain&df=sighting_en&wt=xml&hl=true&hl.snippets=2&hl.fl=sighting_en&" +
            "facet=true&facet.limit=4&facet.field=shape_s&facet.field=location_s&facet.field=month_s");
        listings.put("9.5",
            "/ufo/select?q=blue+fireball+in+the+rain&df=sighting_en&wt=xml&hl=true&hl.snippets=2&hl.fl=sighting_en&" +
            "hl.q=blue+fireball+in+the+rain+light&fq=shape_s%3Alight");
        listings.put("9.6",
            "/ufo/select?q=fire+cluster+clouds+thunder&df=nearby_objects_en&wt=xml&hl=true&hl.snippets=2");
        listings.put("9.7",
            "/ufo/select?q=blue+fireball+in+the+rain&df=sighting_en&wt=xml&hl=true&hl.snippets=2&hl.useFastVectorHighlighter=true");

        listings.put("10.1", "/solrpedia/select?q=atmosphear");
        listings.put("10.2", "/solrpedia/select?q=title:anaconda");
        listings.put("10.3", "/solrpedia/select?q=Julius&df=suggest&fl=title");
        listings.put("10.7", "/solrpedia/select?q=northatlantic+curent&df=suggest&wt=xml");
        listings.put("10.8", "/solrpedia/select?q=northatlantic+curent&df=suggest&q.op=AND&spellcheck.dictionary=wordbreak&spellcheck.dictionary=default");
        listings.put("10.10", "/solrpedia/suggest?q=atm");
        listings.put("10.15", "/solrpedia_instant/select?q=query_ngram:willia&sort=popularity+desc&rows=1&fl=query&wt=json");
        listings.put("10.16", "/solrpedia_instant/select?q=%7B!boost+b%3D%24recency+v%3D%24qq%7D&sort=score+desc&rows=1&wt=json&qq=query_ngram:willia&recency=product(recip(ms(NOW/HOUR,last_executed_on),1.27E-10,0.08,0.05),popularity)");

        listings.put("15.1", "/salestax/select?q=*:*&userSalesTax=0.07&fl=id,basePrice,totalPrice:product(basePrice, sum(1, $userSalesTax))");
        listings.put("15.5", "/geospatial/select?q=*:*&fl=id,city,distance:geodist(location,37.77493, -122.41942)");
        listings.put("15.6", "/geospatial/select?q=*:*&fl=id,city,distance:geodist(location,37.77493, -122.41942)&sort=geodist(location,37.77493, -122.41942) asc, score desc");
        listings.put("15.8", "/distancefacet/select/?q=*:*&rows=0&fq={!geofilt sfield=location pt=37.777,-122.420 d=80}&facet=true&facet.field=city&facet.limit=10");
        listings.put("15.9", "/pivotfaceting/select?q=*:*&fq=rating:[4 TO 5]&facet=true&facet.limit=3&facet.pivot.mincount=1&facet.pivot=state,city,rating");

        listings.put("16.1", "/no-title-boost/select?defType=edismax&q=red lobster&qf=restaurant_name description&debug=true");
        listings.put("16.2", "/no-title-boost/select?defType=edismax&q=red lobster&qf=restaurant_name description&fl=id,restaurant_name,description,[explain]");
        listings.put("16.3", "/no-title-boost/select?defType=edismax&q=red lobster&qf=restaurant_name description&fl=id,restaurant_name,description");
    }

    @Override
    @SuppressWarnings("static-access")
    public Option[] getOptions() {
        return new Option[] {
            OptionBuilder.withArgName("#").hasArg().isRequired(true).withDescription("Listing #, such as 4.4")
                .create("listing"),
            OptionBuilder.withArgName("URL").hasArg().isRequired(false)
                .withDescription("Base URL for the Solr server; default: http://localhost:8983/solr")
                .create("solr"),
            OptionBuilder.withArgName("json|xml|csv").hasArg().isRequired(false)
                .withDescription("Override response writer type parameter \"wt\"")
                .create("wt")
        };
    }

    @Override
    public void runExample(ExampleDriver driver) throws Exception {
        String wt = driver.getCommandLine().getOptionValue("wt", "xml");
        String listingKey = driver.getCommandLine().getOptionValue("listing");
        Object listingObject = listings.get(listingKey);
        
        String[] listingValues;
        if(!(listingObject instanceof String[])) {
             listingValues = new String[]{(String)listingObject};
        }
        else{
            listingValues = (String[])listingObject;
        }

        for (String listing : listingValues){
            //String listing = listings.get(listingKey);
            if (listing == null) {
                System.err.println("Sorry, Listing " + listingKey
                    + " is not available for execution.");
                System.exit(1);
            }

            if (!"xml".equals(wt)) {
                if (listing.indexOf("wt=") != -1) {
                    listing = listing.replaceAll("wt=xml", "wt="+wt);
                } else {
                    listing += "&wt="+wt;
                }
            }

            String serverUrl = driver.getCommandLine().getOptionValue("solr", "http://localhost:8983/solr");

            // trick - get the HTTP Client from the SolrServer client
            HttpSolrServer solr = new HttpSolrServer(serverUrl);
            HttpClient httpClient = solr.getHttpClient();

            // Prepare a request object
            String getUrl = serverUrl + listing;
            HttpGet httpget = new HttpGet(getUrl+"&indent=true");

            // Execute the request
            System.out.println("Sending HTTP GET request (listing " + listingKey + "):");
            System.out.println(URLDecoder.decode("\n\t"+getUrl.replace("?", "?\n\t  ").replace("&", "&\n\t  ") + "\n", "UTF-8"));
            HttpResponse response = httpClient.execute(httpget);

            // Examine the response status
            System.out.println("Solr returned: "+response.getStatusLine()+"\n");

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();

            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null) {
                String line;
                InputStream instream = entity.getContent();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (RuntimeException ex) {
                    // In case of an unexpected exception you may want to abort
                    // the HTTP request in order to shut down the underlying
                    // connection and release it back to the connection manager.
                    httpget.abort();
                    throw ex;

                } finally {
                    // Closing the input stream will trigger connection release
                    instream.close();
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Utility to execute an HTTP request from a listing in the book.";
    }
}
