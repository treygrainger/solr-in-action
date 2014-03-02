package sia;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.HelpFormatter;
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
public class Listing implements ExampleDriver.Example {

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
            "/ufo/select?q=blue+fireball+in+the+rain&df=sighting_en&wt=xml&rows=10&hl=true");
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

        // chapter 10: Query Suggestions
        listings.put("10.1", "/solrpedia/select?q=atmosphear");
        listings.put("10.2", "/solrpedia/select?q=title:anaconda");
        listings.put("10.3", "/solrpedia/select?q=Julius&df=suggest&fl=title");
        listings.put("10.7", "/solrpedia/select?q=northatlantic+curent&df=suggest&wt=xml");
        listings.put("10.8", "/solrpedia/select?q=northatlantic+curent&df=suggest&q.op=AND&spellcheck.dictionary=wordbreak&spellcheck.dictionary=default");
        listings.put("10.10", "/solrpedia/suggest?q=atm");
        listings.put("10.15", "/solrpedia_instant/select?q=query_ngram:willia&sort=popularity+desc&rows=1&fl=query&wt=json");
        listings.put("10.16", "/solrpedia_instant/select?q=%7B!boost+b%3D%24recency+v%3D%24qq%7D&sort=score%20desc&rows=1&wt=json&qq=query_ngram:willia&recency=product(recip(ms(NOW/HOUR,last_executed_on),1.27E-10,0.08,0.05),popularity)");

        // chapter 11: Result grouping/field collapsing
        listings.put("11.2",
            "/ecommerce/select?fl=id%2Cproduct%2Cformat&sort=popularity%20asc&q=spider-man");
        listings.put("11.3",
            "/ecommerce/select?fl=id%2Cproduct%2Cformat&sort=popularity%20asc&q=spider-man&group=true&group.field=product&group.limit=1");
        listings.put("11.4",
            "/ecommerce/select?fl=id%2Cproduct%2Cformat&sort=popularity%20asc&q=spider-man&group=true&group.field=product&group.main=true");
        listings.put("11.5",
            "/ecommerce/select?fl=id%2Cproduct%2Cformat&sort=popularity%20asc&q=spider-man&group=true&group.field=product&group.format=simple");
        listings.put("11.6",
            "/ecommerce/select?q=spider-man&fl=id%2Cproduct%2Cformat&sort=popularity%20asc&group=true&group.field=type&group.limit=3&rows=5&start=0&group.offset=0");
        listings.put("11.7",
            "/ecommerce/select?fl=id%2Cproduct%2Cformat&sort=popularity%20asc&q=spider-man&group=true&group.limit=3&rows=5&group.func=map%28map%28map%28popularity%2C1%2C5%2C1%29%2C6%2C10%2C2%29%2C11%2C100%2C3%29");
        listings.put("11.8",
            "/ecommerce/select?sort=popularity%20asc&fl=id%2Ctype%2Cformat%2Cproduct&group.limit=2&q=*%3A*&group=true&group.query=type%3AMovies&group.query=games&group.query=%22The%20Hunger%20Games%22");
        listings.put("11.9",
            "/ecommerce/select?fl=product&group=true&sort=popularity%20asc&q=*%3A*&facet=true&facet.mincount=1&group.format=simple&fq=type%3AMovies&facet.field=type&group.field=product");
        listings.put("11.10",
            "/ecommerce/select?fl=product&group=true&sort=popularity%20asc&q=*%3A*&facet=true&facet.mincount=1&group.format=simple&fq=type%3AMovies&facet.field=type&group.field=product&group.facet=true");

        // chapter 13: SolrCloud
        listings.put("13.6",
            "/admin/collections?action=CREATE&name=support&numShards=1&replicationFactor=2&maxShardsPerNode=1&collection.configName=support");
        listings.put("13.7",
            "/admin/collections?action=CREATEALIAS&name=logmill-write&collections=logmill");

        // chapter 14: Multilingual Search
        listings.put("14.4",
            "/field-per-language/select?fl=title&defType=edismax&qf=content_english%20content_french%20content_spanish&q=%22he%20told%20the%20truth%22%20OR%20%22il%20%C3%A9tait%20pr%C3%AAtre%22%20OR%20%22ver%20la%20vida%20como%20es%22");
        listings.put("14.5", new String[]{
            "/field-per-language/select?fl=title&defType=edismax&qf=content_english%20content_french%20content_spanish&q=%22wisdom%22",
            "/field-per-language/select?fl=title&defType=edismax&qf=content_english%20content_french%20content_spanish&q=%22sabidur%C3%ADa%22",
            "/field-per-language/select?fl=title&defType=edismax&qf=content_english%20content_french%20content_spanish&q=%22sagesse%22"});
        listings.put("14.9",
            "/multi-language-field/select?fl=title&df=content&q=en,fr,es%7Cabandon%20AND%20en,fr,es%7Cunderstanding%20AND%20en,fr,es%7Csagess");
        listings.put("14.12",
            "/langid/select?q=*:*&fl=title,language,languages");
        listings.put("14.14",
            "/langid2/select?q=id:[1%20TO%203]&fl=title,language,content_english,content_spanish,content_french&defType=edismax&qf=content_english%20content_spanish%20content_french");
        listings.put("14.17",
            "/multi-langid/select?q=*:*&df=content&fl=title,content,language");
        listings.put("14.18",
            "/multi-langid/select?q=en,es,fr%7Cabandon&df=content&fl=title,content,language");

        // chapter 15: Performing Queries and Handling Results
        listings.put("15.1", "/salestax/select?q=*:*&userSalesTax=0.07&fl=id,basePrice,totalPrice:product(basePrice,%20sum(1,%20$userSalesTax))");
        listings.put("15.5", "/geospatial/select?q=*:*&fl=id,city,distance:geodist(location,37.77493,%20-122.41942)");
        listings.put("15.6", "/geospatial/select?q=*:*&fl=id,city,distance:geodist(location,37.77493,%20-122.41942)&sort=geodist(location,37.77493,%20-122.41942)%20asc,%20score%20desc");
        listings.put("15.8", new String[]{ 
            "/distancefacet/select?q=*:*&rows=0&fq=%7B!geofilt%20sfield=location%20pt=37.777,-122.420%20d=80%7D&facet=true&facet.field=city&facet.limit=10",
            "/distancefacet/select?q=*:*&rows=0&facet=true&fq=(_query_:%22%7B!geofilt%20sfield=location%20pt=37.777,-122.420%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.338,-121.886%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.805,-122.273%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.445,-122.161%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.356,-121.954%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.386,-122.083%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.372,-122.038%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.551,-121.982%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.484,-122.227%20d=20%7D%22%20OR%20_query_:%22%7B!geofilt%20sfield=location%20pt=37.870,-122.271%20d=20%7D%22)&facet.query=%7B!geofilt%20key=%22san%20francisco,%20ca%22%20sfield=location%20pt=37.7770,-122.4200%20d=20%7D&facet.query=%7B!geofilt%20key=%22san%20jose,%20ca%22%20sfield=location%20pt=37.338,-121.886%20d=20%7D&facet.query=%7B!geofilt%20key=%22oakland,%20ca%22%20sfield=location%20pt=37.805,-122.273%20d=20%7D&facet.query=%7B!geofilt%20key=%22palo%20alto,%20ca%22%20sfield=location%20pt=37.445,-122.161%20d=20%7D&facet.query=%7B!geofilt%20key=%22santa%20clara,%20ca%22%20sfield=location%20pt=37.356,-121.954%20d=20%7D&facet.query=%7B!geofilt%20key=%22mountain%20view%22%20sfield=location%20pt=37.386,-122.083%20d=20%7D&facet.query=%7B!geofilt%20key=%22sunnyvale,%20ca%22%20sfield=location%20pt=37.372,-122.038%20d=20%7D&facet.query=%7B!geofilt%20key=%22fremont,%20ca%22%20sfield=location%20pt=37.551,-121.982%20d=20%7D&facet.query=%7B!geofilt%20key=%22redwood%20city,%20ca%22%20sfield=location%20pt=37.484,-122.227%20d=20%7D&facet.query=%7B!geofilt%20key=%22berkeley,%20ca%22%20sfield=location%20pt=37.870,-122.271%20d=20%7D"});
        listings.put("15.9", "/pivotfaceting/select?q=*:*&fq=rating:[4%20TO%205]&facet=true&facet.limit=3&facet.pivot.mincount=1&facet.pivot=state,city,rating");

        // chapter 16: Mastering Relevancy
        listings.put("16.1", "/no-title-boost/select?defType=edismax&q=red%20lobster&qf=restaurant_name%20description&debug=true");
        listings.put("16.2", "/no-title-boost/select?defType=edismax&q=red%20lobster&qf=restaurant_name%20description&fl=id,restaurant_name,description,[explain]");
        listings.put("16.3", "/title-boost/select?defType=edismax&q=red%20lobster&qf=restaurant_name%20description&fl=id,restaurant_name,description");
        listings.put("16.5", "/jobs/select/?fl=jobtitle,city,state,salary&q=(jobtitle:%22nurse%20educator%22%5E25%20OR%20jobtitle:(nurse%20educator)%5E10)%20AND%20((city:%22Boston%22%20AND%20state:%22MA%22)%5E15%20OR%20state:%22MA%22)%20AND%20_val_:%22map(salary,%2040000,60000,10,0)%22");
        listings.put("16.6", "/jobs/select/?df=classification&q=((%22healthcare.nursing.oncology%22%5E40%20OR%20%22healthcare.nursing%22%5E20%20OR%20%22healthcare%22%5E10)%20OR%20(%22healthcare.nursing.transplant%22%5E20%20OR%20%22healthcare.nursing%22%5E10%20OR%20%22healthcare%22%5E5)%20OR%20(%22education.postsecondary.nursing%22%5E10%20OR%20%22education.postsecondary%22%5E5%20OR%20%22education%22))");
        listings.put("16.7", "/jobs/mlt?df=jobdescription&fl=id,jobtitle&rows=5&q=J2EE&mlt.fl=jobtitle,jobdescription&mlt.interestingTerms=details");
        listings.put("16.8", "/jobs/mlt?df=jobdescription&q=id:fc57931d42a7ccce3552c04f3db40af8dabc99dc&mlt.fl=jobtitle,jobdescription&mlt.interestingTerms=details&mlt.boost=true");
        listings.put("16.9", "/jobs/mlt?df=jobdescription&mlt.fl=jobtitle,jobdescription&mlt.interestingTerms=details&mlt.boost=true&stream.body=Solr%20is%20an%20open%20source%20enterprise%20search%20platform%20from%20the%20Apache%20Lucene%20project.%20Its%20major%20features%20include%20full-text%20search,%20hit%20highlighting,%20faceted%20search,%20dynamic%20clustering,%20database%20integration,%20and%20rich%20document%20(e.g.,%20Word,%20PDF)%20handling.%20Providing%20distributed%20search%20and%20index%20replication,%20Solr%20is%20highly%20scalable.%20Solr%20is%20the%20most%20popular%20enterprise%20search%20engine.%20Solr%204%20adds%20NoSQL%20features.");
        listings.put("16.11", "/jobs/clustering?q=content:(solr%20OR%20lucene)&fl=id,jobtitle&rows=100&carrot.title=jobtitle&carrot.snippet=jobtitle&LingoClusteringAlgorithm.desiredClusterCountBase=25");
        listings.put("16.12", "/jobs/select?df=jobdescription&fl=id,jobtitle&q=(solr%20OR%20lucene)%20OR%20%22Java%20Engineer%22%20OR%20%22Software%20Developer%22");
    }

    @SuppressWarnings("static-access")
    public Option[] getOptions() {
      return new Option[] {
            OptionBuilder.withArgName("#").hasArg().isRequired(false).withDescription("Required: Listing #, such as 4.4")
                .withLongOpt("num")
                .create("n"),
            OptionBuilder.withArgName("URL").hasArg().isRequired(false)
                .withDescription("Base URL for the Solr server; default: http://localhost:8983/solr")
                .withLongOpt("solr") 
                .create("s"),
            OptionBuilder.withArgName("json|xml|csv").hasArg().isRequired(false)
                .withDescription("Override response writer type parameter \"wt\"")
                .withLongOpt("format") 
                .create("f")
        };
    }

    public void runExample(ExampleDriver driver) throws Exception {
        String wt = driver.getCommandLine().getOptionValue("f", "xml");
        String listingKey = driver.getCommandLine().getOptionValue("n");        
        if (driver.getCommandLine().getArgList().size() == 0){
          Option[] options = this.getOptions();
          options[0].setRequired(true);
          ExampleDriver.processCommandLineArgs(this, new String[]{"--help"});
        }
        
        if ((listingKey == null) && driver.getCommandLine().getArgList().size() == 1){
          listingKey = driver.getCommandLine().getArgs()[0];
        }
        
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

    public String getDescription() {
        return "Utility to execute an HTTP request from a listing in the book.";
    }
}
