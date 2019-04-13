package sia.ch6;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class ResolveUrlTokenFilterFactory extends TokenFilterFactory {

    private UrlResolver urlResolver;
    private Pattern patternToMatchShortenedUrls;
    
    public ResolveUrlTokenFilterFactory(Map<String,String> args) {
        super(args);
        
        assureMatchVersion();
        String shortenedUrlPattern = require(args, "shortenedUrlPattern");        
        patternToMatchShortenedUrls = Pattern.compile(shortenedUrlPattern);
        determineUrlResolver(args);
    }

    private void determineUrlResolver(Map<String, String> args) {
        try {
            urlResolver = (UrlResolver) Class.forName(args.get("urlResolver")).newInstance();
        } catch (ClassNotFoundException e) {
            urlResolver = getDefaultUrlResolver();
        } catch (InstantiationException e) {
            urlResolver = getDefaultUrlResolver();
        } catch (IllegalAccessException e) {
            urlResolver = getDefaultUrlResolver();
        }
    }

    private UrlResolver getDefaultUrlResolver() {
        return new BitlyExampleResolver();
    }
    
    @Override
    public TokenFilter create(TokenStream input) {
        return new ResolveUrlTokenFilter(input, patternToMatchShortenedUrls, urlResolver);
    }
}
