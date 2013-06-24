package sia.ch9;

import java.io.BufferedReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.noggit.ObjectBuilder;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.common.SolrInputDocument;

import sia.ExampleDriver;

/**
 * Index UFO sightings in Solr using SolrJ to learn about hit highlighting.
 */
public class IndexUfoSightings extends ExampleDriver.SolrJClientExample {

    public static Logger log = Logger.getLogger(IndexUfoSightings.class);

    private static final String UFO_CORE = "http://localhost:8983/solr/ufo";

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat MONTH_NAME_FMT = new SimpleDateFormat("MMMM");

    private static final Pattern MATCH_US_CITY_AND_STATE = Pattern.compile("^([^,]+),\\s([A-Z]{2})$");

    private boolean beVerbose = false;

    public String getDescription() {
        return "Use SolrJ ConcurrentUpdateSolrServer to index UFO sightings for learning about hit highlighting.";
    }

    @SuppressWarnings("static-access")
    public Option[] getOptions() {
        return new Option[] {
            OptionBuilder.withArgName("URL").hasArg().isRequired(false)
                .withDescription("Base URL for the Solr server; default: "+UFO_CORE).create("solr"),
            OptionBuilder.withArgName("FILE").hasArg().isRequired(true)
                .withDescription("Path to ufo_awesome.json").create("jsonInput"),
            OptionBuilder.withArgName("#").hasArg().isRequired(false)
                .withDescription("Batch size, default is 500").create("batchSize")
        };
    }

    /**
     * Main method of this example, use the ConcurrentUpdateSolrServer to rapidly
     * index many UFO sighting documents in Solr.
     */
    public void runExample(ExampleDriver driver) throws Exception {
        long startMs = System.currentTimeMillis();

        CommandLine cli = driver.getCommandLine();

        beVerbose = cli.hasOption("verbose");

        // Size of index batch requests to Solr
        int batchSize = Integer.parseInt(cli.getOptionValue("batchSize", "500"));

        // Get a connection to Solr
        String serverUrl = cli.getOptionValue("solr", UFO_CORE);
        SolrServer solr = new ConcurrentUpdateSolrServer(serverUrl, batchSize, 1);

        int numSent = 0;
        int numSkipped = 0;
        int lineNum = 0;
        SolrInputDocument doc = null;
        String line = null;

        // read file line-by-line
        BufferedReader reader = new BufferedReader(driver.readFile("jsonInput"));
        driver.rememberCloseable(reader);

        // process each sighting as a document
        while ((line = reader.readLine()) != null) {
            doc = parseNextDoc(line, ++lineNum);
            if (doc != null) {
                solr.add(doc);
                ++numSent;
            } else {
                ++numSkipped;
                continue;
            }

            if (lineNum % 5000 == 0)
                log.info(String.format("Processed %d lines.", lineNum));
        }

        // add one fictitious sighting for highlighting multi-valued fields
        solr.add(createFictitiousSightingWithMultiValuedField());

        // hard commit all docs sent
        solr.commit(true,true);

        solr.shutdown();

        float tookSecs = Math.round(((System.currentTimeMillis() - startMs)/1000f)*100f)/100f;
        log.info(String.format("Sent %d sightings (skipped %d) took %f seconds", numSent, numSkipped, tookSecs));
    }

    /**
     * Transforms a UFO sighting JSON object into a SolrInputDocument for indexing
     * (sometimes you need to scrub your input data before sending to Solr).
     * @param line
     * @param lineNum
     * @return
     */
    protected SolrInputDocument parseNextDoc(String line, int lineNum) {
        Map jsonObj = null;
        try {
            jsonObj = (Map)ObjectBuilder.fromJSON(line);
        } catch (Exception jsonErr) {
            if (beVerbose) {
                log.warn("Skipped invalid sighting at line "+lineNum+
                    "; Failed to parse ["+line+"] into JSON due to: "+jsonErr);
            }
            return null;
        }

        String sighted_at = readField(jsonObj, "sighted_at");
        String location = readField(jsonObj, "location");
        String description = readField(jsonObj, "description");

        // ignore rows that don't have valid data
        if (sighted_at == null || location == null || description == null) {
            if (beVerbose) {
                log.warn("Skipped incomplete sighting at line "+lineNum+"; "+line);
            }
            return null;
        }

        // require the sighted_at date to be valid
        Date sighted_at_dt = null;
        try {
            sighted_at_dt = DATE_FORMATTER.parse(sighted_at);
        } catch (java.text.ParseException pe) {
            if (beVerbose) {
                log.warn("Skipped sighting at line "+lineNum+
                    " due to invalid sighted_at date ("+sighted_at+") caused by: "+pe);
            }
            return null;
        }

        // Verify the location matches the pattern of US City and State
        Matcher matcher = MATCH_US_CITY_AND_STATE.matcher(location);
        if (!matcher.matches()) {
            if (beVerbose) {
                log.warn("Skipped sighting at line "+lineNum+
                    " because location ["+location+"] does not look like a US city and state.");
            }
            return null;
        }

        // split the cit and state into separate fields
        String city = matcher.group(1);
        String state = matcher.group(2);

        // Clean-up the sighting description, mostly for display purposes

        // description has some XML escape sequences ... convert back to chars
        description = description.replace("&quot;", "\"").replace("&amp;", "&").replace("&apos;", "'");
        description = description.replaceAll("\\s+", " "); // collapse all whitespace down to 1 space
        description = description.replaceAll("([a-z])([\\.\\?!,;])([A-Z])", "$1$2 $3"); // fix missing space at end of sentence
        description = description.replaceAll("([a-z])([A-Z])", "$1 $2"); // fix missing space between end of word and new word

        String reported_at = readField(jsonObj, "reported_at");
        String shape = readField(jsonObj, "shape");
        String duration = readField(jsonObj, "duration");

        // every doc needs a unique id - create a composite key based on sighting data
        String docId = String.format("%s/%s/%s/%s/%s/%s",
            sighted_at,
            (reported_at != null ? reported_at : "?"),
            city.replaceAll("\\s+",""),
            state,
            (shape != null ? shape : "?"),
            ExampleDriver.getMD5Hash(description)).toLowerCase();

        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", docId);
        doc.setField("sighted_at_dt", sighted_at_dt);
        // another field to facet on
        doc.setField("month_s", MONTH_NAME_FMT.format(sighted_at_dt));

        if (reported_at != null) {
            try {
                doc.setField("reported_at_dt", DATE_FORMATTER.parse(reported_at));
            } catch (java.text.ParseException pe) {
                // not fatal - just ignore this invalid field
            }
        }

        doc.setField("city_s", city);
        doc.setField("state_s", state);
        doc.setField("location_s", location); // keep this field around for faceting on the full location

        if (shape != null) {
            doc.setField("shape_s", shape);
        }

        if (duration != null) {
            doc.setField("duration_s", duration);
        }

        doc.setField("sighting_en", description);

        return doc;
    }

    protected String readField(Map jsonObj, String key) {
        String val = (String)jsonObj.get(key);
        if (val != null) {
            val = val.trim();
            if (val.length() == 0)
                val = null;
        }
        return val;
    }

    /**
     * Create a fictitious sighting to experiment with highlighting multi-valued
     * fields in Solr.
     */
    protected SolrInputDocument createFictitiousSightingWithMultiValuedField() throws ParseException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", "sia-fictitious-sighting");
        doc.setField("sighted_at_dt", DATE_FORMATTER.parse("20130401"));
        doc.setField("month_s", "April");
        doc.setField("reported_at_dt", DATE_FORMATTER.parse("20130401"));
        doc.setField("city_s", "Denver");
        doc.setField("state_s", "CO");
        doc.setField("location_s", "Denver, CO"); // keep this field around for faceting on the full location
        doc.setField("shape_s", "unicorn");
        doc.setField("duration_s", "5 seconds");
        doc.setField("sighting_en", "This is a fictitious UFO sighting.");
        doc.addField("nearby_objects_en", "arc of red fire");
        doc.addField("nearby_objects_en", "cluster of dark clouds");
        doc.addField("nearby_objects_en", "thunder and lightning");
        return doc;
    }
}
