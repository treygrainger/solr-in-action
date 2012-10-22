package sia.ch6;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class ResolveUrlTokenFilter extends TokenFilter {

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final Pattern patternToMatchShortenedUrls;

    public ResolveUrlTokenFilter(TokenStream in, Pattern patternToMatchShortenedUrls) {
        super(in);
        this.patternToMatchShortenedUrls = patternToMatchShortenedUrls;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken())
            return false;

        char[] term = termAttribute.buffer();
        int len = termAttribute.length();

        String token = new String(term, 0, len);
        if (patternToMatchShortenedUrls.matcher(token).matches()) {
            // token is a shortened URL, resolve it and replace
            termAttribute.setEmpty().append(resolveShortenedUrl(token));
        }

        return true;
    }

    private String resolveShortenedUrl(String toResolve) {
        try {
            // TODO: implement a real way to resolve shortened URLs
            if ("http://bit.ly/3ynriE".equals(toResolve)) {
                return "lucene.apache.org/solr";
            } else if ("http://bit.ly/15tzw".equals(toResolve)) {
                return "manning.com";
            }
        } catch (Exception exc) {
            // rather than failing analysis if you can't resolve the URL,
            // you should log the error and return the un-resolved value
            exc.printStackTrace();
        }
        return toResolve;
    }
}
