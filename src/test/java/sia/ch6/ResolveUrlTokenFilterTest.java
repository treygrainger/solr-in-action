package sia.ch6;

import junit.framework.TestCase;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class ResolveUrlTokenFilterTest extends TestCase {

    @Test
    public void testResolveUrl() throws IOException {
        Map<String, String> args = new HashMap<String, String>();
        args.put("shortenedUrlPattern", "http:\\/\\/bit.ly/[\\w\\-]+");
        args.put("luceneMatchVersion", Version.LUCENE_47.toString());
        args.put("urlResolver", "sia.ch6.BitlyExampleResolver");
        ResolveUrlTokenFilterFactory factory = new ResolveUrlTokenFilterFactory(args);

        String string = "checkout: http://bit.ly/3ynriE";
        StringReader reader = new StringReader(string);
        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_47, reader);
        TokenFilter tokenFilter = factory.create(tokenizer);
        CharTermAttribute attribute = tokenFilter.getAttribute(CharTermAttribute.class);
        tokenFilter.reset();
        List<String> actual = new ArrayList<String>();
        while (tokenFilter.incrementToken()) {
            actual.add(attribute.toString());
        }
        List<String> expected = Arrays.asList("checkout:", "lucene.apache.org/solr");
        assertEquals(expected, actual);
        tokenFilter.end();
        tokenFilter.close();
    }

}