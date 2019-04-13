package sia.ch6;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

final public class ResolveUrlTokenFilter extends TokenFilter {

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final Pattern patternToMatchShortenedUrls;
    private final UrlResolver urlResolver;

    public ResolveUrlTokenFilter(TokenStream in, Pattern patternToMatchShortenedUrls, UrlResolver urlResolver) {
        super(in);
        this.patternToMatchShortenedUrls = patternToMatchShortenedUrls;
        this.urlResolver = urlResolver;
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
        return urlResolver.expand(toResolve);
    }
}
