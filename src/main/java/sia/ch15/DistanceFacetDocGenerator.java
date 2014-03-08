package sia.ch15;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import sia.ExampleDriver;

public class DistanceFacetDocGenerator implements ExampleDriver.Example {

private static Random random = new Random();
	
	public static void main(String[] args) {

		String file = args[0];
		
		List<WeightedLocation> locations = getWeightedLocations();

		File outputFile = new File(file);
        BufferedWriter writer = null;
        Integer nextDocId = 1;
        try{
          writer = new BufferedWriter (new FileWriter(outputFile));
          writer.write("<add>\n");
    		for (WeightedLocation location : locations){
    			for (Integer i = 0; i < location.numDocs; i++){
    				StringBuilder doc = new StringBuilder();
    				doc.append("  <doc>\n");
    				doc.append("    <field name=\"id\">" + nextDocId.toString() + "</field>\n");
    				doc.append("    <field name=\"location\">" + changeLastDigit(location.latitude) + "," + changeLastDigit(location.longitude) + "</field>\n");
    				doc.append("    <field name=\"city\">" + location.place + "</field>\n");
    				doc.append("  </doc>\n");
    				writer.write(doc.toString());
                    nextDocId +=1;
    			}
    		}
    		writer.write("</add>");
		
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				writer.close();
			}
			catch (Exception e){
			}
		}
		
		System.out.println("Solr documents written to: " + outputFile.toString());
			
		
	}
	
	private static Double changeLastDigit(Double numberToChange){
		 String newDouble = numberToChange.toString().substring(0, numberToChange.toString().length() -1) + random.nextInt(9);
		 return Double.parseDouble(newDouble);
	}
	
	private static List<WeightedLocation> getWeightedLocations(){
		List<WeightedLocation> locations = new ArrayList<WeightedLocation>();
		
		locations.add(new WeightedLocation("San Francisco, CA", 37.777,-122.420, 11713));
		locations.add(new WeightedLocation("San Jose, CA", 37.338,-121.886, 3071));
		locations.add(new WeightedLocation("Oakland, CA", 37.805,-122.273, 1482));
		locations.add(new WeightedLocation("Palo Alto, CA", 37.445,-122.161, 1318));
		locations.add(new WeightedLocation("Santa Clara, CA", 37.356,-121.954, 1212));
		locations.add(new WeightedLocation("Mountain View, CA", 37.386,-122.083, 1045));
		locations.add(new WeightedLocation("Sunnyvale, CA", 37.372,-122.038, 1004));
		locations.add(new WeightedLocation("Fremont, CA", 37.551,-121.982, 726));
		locations.add(new WeightedLocation("Redwood City, CA", 37.484,-122.227, 633));
		locations.add(new WeightedLocation("Berkeley, CA", 37.870,-122.271, 599));
		locations.add(new WeightedLocation("San Mateo, CA", 37.547,-122.315, 500));
		locations.add(new WeightedLocation("New York, NY", 40.715,-74.007, 12107));
		locations.add(new WeightedLocation("Atlanta, GA", 33.748,-84.391, 68453));
		
		return locations;
	}
	static public class WeightedLocation{
		final String place;
		final Double latitude;
		final Double longitude;
		final Integer numDocs;
		
		public WeightedLocation(String place, Double latitude, Double longitude, Integer numDocs){
			this.place = place;
			this.latitude = latitude;
			this.longitude = longitude;
			this.numDocs = numDocs;
		}
	}
	@SuppressWarnings("static-access")
	public Option[] getOptions() { 
            return new Option[] {
                    OptionBuilder.withArgName("file").hasArg().isRequired(false)
                    .withDescription("File into which location documents should be saved").create("file")            		
            };
	}

	public void runExample(ExampleDriver driver) throws Exception {
		CommandLine cli = driver.getCommandLine();
		
		CodeSource codeSource = ExampleDriver.class.getProtectionDomain().getCodeSource();
		File jarFile = new File(codeSource.getLocation().toURI().getPath());
		String jarDir = jarFile.getParentFile().getPath();
		
        String file = cli.getOptionValue("file", jarDir + "/example-docs/ch15/documents/distancefacet.xml");
		main(new String[]{file});			
	}

	public String getDescription() {
		return "Generated over 100,000 example documents (mostly) near San Francisco for geo-distance-based analytics";
	}

}
