package sia;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.util.NamedList;

/**
 * Serves as a common framework for running Solr in Action examples.
 */
public class ExampleDriver {

    public static Logger log = Logger.getLogger(ExampleDriver.class);

    public static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr/collection1";

    private static final Pattern FIND_NUMBER = Pattern.compile("\\d+");

    /**
     * Defines an example that gets executed in a common framework by this ExampleDriver application.
     */
    public static interface Example {
        Option[] getOptions();
        void runExample(ExampleDriver driver) throws Exception;
        String getDescription();
    }

    /**
     * Abstract base class for examples that need a URL to initialize the SolrJ client.
     */
    public static abstract class SolrJClientExample implements Example {
        @SuppressWarnings("static-access")
        public Option[] getOptions() {
            return new Option[] { OptionBuilder.withArgName("URL").hasArg().isRequired(false)
                .withDescription("Base URL for the Solr server; default: " + DEFAULT_SOLR_URL).create("solr") };
        }
    }

    /**
     * Main entry point to the ExampleDriver execution environment.
     *
     * @param args
     *            Command-line args parsed using Commons CLI (GnuParser)
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        // the first argument must be the name of the example to execute
        if (args == null || args.length == 0) {
            displayListOfAvailableExamplesAndExit(null);
        } else if ((args[0].startsWith("ch") && args[0].indexOf(".") == -1) || FIND_NUMBER.matcher(args[0]).matches()) {
            displayListOfAvailableExamplesAndExit(args[0]);
        }

        String exampleClassName = args[0];
        if (!exampleClassName.startsWith("sia.")) {
            exampleClassName = "sia." + exampleClassName;
        }
        Class<Example> exampleClass = null;
        try {
            exampleClass = (Class<Example>)ExampleDriver.class.getClassLoader().loadClass(exampleClassName);
        } catch (ClassNotFoundException cnf) {
            // scan all known Example classes and find the one that matches the arg
            List<Class<Example>> exampleClasses = getClassesInPackage("sia");
            if (!exampleClasses.isEmpty()) {
                String argLc = args[0].toLowerCase();
                for (Class<Example> next : exampleClasses) {
                    String nameLc = next.getName().toLowerCase();
                    if (nameLc.indexOf(argLc) != -1) {
                        log.info(String.format("Found example class %s for arg %s", next.getName(), args[0]));
                        exampleClass = next;
                        break;
                    }
                }
            }

            if (exampleClass == null) {
                throw cnf;
            }
        }

        Example example = exampleClass.newInstance();

        // all but the first arg is treated as args to the example
        StringBuilder sb = new StringBuilder();
        String[] exampleArgs = new String[args.length - 1];
        String prevArg = null;
        for (int i = 0; i < exampleArgs.length; i++) {
            exampleArgs[i] = args[i + 1];

            if (i > 0)
                sb.append(" ");
            if ("-pass".equals(prevArg) || "-password".equals(prevArg)) {
                // don't display the password
                sb.append("********");
            } else {
                sb.append(exampleArgs[i]);
            }
            prevArg = exampleArgs[i];
        }

        // run the driver
        log.info(String.format("Running example %s with args: %s", example.getClass().getSimpleName(), sb.toString()));
        (new ExampleDriver(example, exampleArgs)).run();
    }

    /**
     * Displays a list of available examples to stdout and then exits the application.
     */
    private static void displayListOfAvailableExamplesAndExit(String chapter) {

        int exitCode = 0;
        List<Class<Example>> exampleClasses = getClassesInPackage("sia");
        if (exampleClasses == null || exampleClasses.isEmpty()) {
            System.err.println("No example classes found! Check that you're launching " +
            		"this driver using: java -jar solr-in-action.jar");
            exitCode = 1;
        } else {
            int numExamplesFound = 0;
            System.out.println("Solr in Action Examples:\n");
          SortedMap<String, String> displayExamples = new TreeMap<String,String>();
            for (Class<Example> exampleClass : exampleClasses) {
                try {
                    Example example = exampleClass.newInstance();
                    String shortClassName = exampleClass.getName().substring(4);
                    if (chapter != null) {
                        // extract the number value from the chapter filter
                        // and from the class package part
                        int dotAt = shortClassName.indexOf(".");
                        if (dotAt != -1) {
                            String classNum =
                                    extractNumPart(shortClassName.substring(0,dotAt));
                            String argNum = extractNumPart(chapter);
                            if (argNum != null && !argNum.equals(classNum)) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                    String originalChapterNum = extractNumPart(shortClassName);
                    if (originalChapterNum == null) { originalChapterNum = "0"; }
                    String newChapterNum = originalChapterNum;
                    if (originalChapterNum.length() == 1){ newChapterNum = "0" + originalChapterNum; }
                    displayExamples.put(shortClassName.replace(originalChapterNum, newChapterNum), String.format("\t%s:\n\t\t%s\n",
                    shortClassName, example.getDescription()));

                    ++numExamplesFound;
                } catch (Exception e) {
                    // probably never happens
                    e.printStackTrace();
                }
            }

            if (numExamplesFound == 0 && chapter != null) {
                System.out.println("\n\tWARNING: No examples found for chapter: "+extractNumPart(chapter)+"\n\n");
            }
            else{
              for (String key : displayExamples.keySet()){
                System.out.println(displayExamples.get(key));
              }  
            }
        }
        System.exit(exitCode);
    }

    /**
     * Utility method for extracting a number part from a string.
     */
    private static String extractNumPart(String str) {
        Matcher numFinder =  FIND_NUMBER.matcher(str);
        return (numFinder.find()) ? numFinder.group() : null;
    }

    /**
     * Parses the command-line arguments passed by the user.
     *
     * @param example
     * @param args
     * @return CommandLine The Apache Commons CLI object.
     */
    public static CommandLine processCommandLineArgs(Example example, String[] args) {
        Options options = new Options();

        options.addOption("h", "help", false, "Print this message");
        options.addOption("v", "verbose", false, "Generate verbose log messages");

        Option[] customOptions = example.getOptions();
        if (customOptions != null) {
            for (int i = 0; i < customOptions.length; i++) {
                options.addOption(customOptions[i]);
            }
        }

        CommandLine cli = null;
        try {
            cli = (new GnuParser()).parse(options, args);
        } catch (ParseException exp) {
            boolean hasHelpArg = false;
            if (args != null && args.length > 0) {
                for (int z = 0; z < args.length; z++) {
                    if ("-h".equals(args[z]) || "-help".equals(args[z])) {
                        hasHelpArg = true;
                        break;
                    }
                }
            }
            if (!hasHelpArg) {
                System.err.println("Failed to parse command-line arguments due to: " + exp.getMessage());
            }
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(example.getClass().getSimpleName(), options);
            System.exit(1);
        }

        if (cli.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(example.getClass().getSimpleName(), options);
            System.exit(0);
        }

        return cli;
    }

    @SuppressWarnings("static-access")
    public static Option buildOption(String argName, String shortDescription, String defaultValue) {
        if (defaultValue != null) {
            shortDescription += (" Default is " + defaultValue);
        }
        return OptionBuilder.hasArg().isRequired((defaultValue == null)).withDescription(shortDescription)
            .create(argName);
    }

    public static final String getMD5Hash(final String msg) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            byte[] aby = md5.digest(msg.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer(32);
            for (int i = 0; i < aby.length; ++i) {
                sb.append(Integer.toHexString((aby[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    protected CommandLine cli;
    protected Example example;

    protected List<Closeable> closeables;

    public ExampleDriver(Example example, String[] args) {
        this.example = example;
        cli = processCommandLineArgs(example, args);
    }

    /**
     * Remember to close a Closeable resource after the app finishes.
     *
     * @param closeable
     */
    public void rememberCloseable(Closeable closeable) {
        if (closeables == null) {
            closeables = new ArrayList<Closeable>();
        }
        closeables.add(closeable);
    }

    /**
     * Executes the specified Example.
     */
    public void run() throws Exception {
        try {
            example.runExample(this);
        } finally {
            shutdown();
        }
    }

    /**
     * Get access to the parsed command-line options.
     *
     * @return CommandLine
     */
    public CommandLine getCommandLine() {
        return cli;
    }

    /**
     * Closes all resources held by this driver.
     */
    protected void shutdown() {
        if (closeables != null) {
            for (Closeable next : closeables) {
                try {
                    next.close();
                } catch (Exception nothingWeCanDo) {
                }
            }
        }
    }

    /**
     * Reads a File into a byte[].
     *
     * @param file
     * @return byte[] The bytes in the file.
     * @throws IOException
     */
    public byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int r = 0;
        byte[] aby = new byte[256];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            while ((r = fis.read(aby)) != -1) {
                bytes.write(aby, 0, r);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception zzz) {
                }
            }
        }
        return bytes.toByteArray();
    }

    /**
     * Saves bytes to a File.
     *
     * @param file
     * @param bytes
     * @throws IOException
     */
    public void saveToFile(File file, byte[] bytes) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception zzz) {
                }
            }
        }
    }

    /**
     * Opens a Writer for output to a file specified on the command-line.
     *
     * @param arg
     * @return
     * @throws IOException
     */
    public Writer openWriter(String arg) throws IOException {
        File outputFile = new File(cli.getOptionValue(arg));
        Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
        rememberCloseable(writer);
        return writer;
    }

    /**
     * Opens a reader for a file specified on the command-line.
     * @param arg
     * @return
     * @throws IOException
     */
    public InputStreamReader readFile(String arg) throws IOException {
        String path = cli.getOptionValue(arg);
        File file = new File(path);
        if (!file.isFile())
            throw new IllegalArgumentException("Required file '"+
               file.getAbsolutePath()+"' supplied by arg '"+arg+"' not found!");

        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        rememberCloseable(reader);
        return reader;
    }

    /**
     * Format and log an informational message using varargs.
     *
     * @param msg
     * @param args
     */
    public void out(String msg, Object... args) {
        log.info(String.format(msg, args));
    }

    /**
     * Utility method for scanning JARs on the classpath for example classes.
     */
    @SuppressWarnings("unchecked")
    private static List<Class<Example>> getClassesInPackage(String packageName) {
        List<Class<Example>> exampleClasses = new ArrayList<Class<Example>>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            Set<String> classes = new TreeSet<String>();
            while (resources.hasMoreElements()) {
                URL resource = (URL) resources.nextElement();
                classes.addAll(findClasses(resource.getFile(), packageName));
            }

            for (String classInPackage : classes) {
                Class theClass = Class.forName(classInPackage);
                if (Example.class.isAssignableFrom(theClass)) {
                    exampleClasses.add((Class<Example>)theClass);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exampleClasses;
    }

    private static Set<String> findClasses(String path, String packageName) throws Exception {
        Set<String> classes = new TreeSet<String>();
        if (path.startsWith("file:") && path.contains("!")) {
            String[] split = path.split("!");
            URL jar = new URL(split[0]);
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "")
                        .replace('/', '.');
                    if (className.startsWith(packageName)) {
                        classes.add(className);
                    }
                }
            }
        }
        return classes;
    }

    /**
     * Send HTTP GET request to Solr.
     */
    public NamedList<Object> sendRequest(HttpClient httpClient, String getUrl) throws Exception {
        NamedList<Object> solrResp = null;

        // Prepare a request object
        HttpGet httpget = new HttpGet(getUrl);

        // Execute the request
        HttpResponse response = httpClient.execute(httpget);

        // Get hold of the response entity
        HttpEntity entity = response.getEntity();
        if (response.getStatusLine().getStatusCode() != 200) {
            StringBuilder body = new StringBuilder();
            if (entity != null) {
                InputStream instream = entity.getContent();
                String line;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                } catch (Exception ignore) {
                    // squelch it - just trying to compose an error message here
                } finally {
                    instream.close();
                }
            }
            throw new Exception("GET request ["+getUrl+"] failed due to: "+response.getStatusLine()+": "+body);
        }

        // If the response does not enclose an entity, there is no need
        // to worry about connection release
        if (entity != null) {
            InputStream instream = entity.getContent();
            try {
                solrResp = (new XMLResponseParser()).processResponse(instream, "UTF-8");
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

        return solrResp;
    }
}
