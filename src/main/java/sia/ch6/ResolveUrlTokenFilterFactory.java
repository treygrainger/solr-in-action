package sia.ch6;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class ResolveUrlTokenFilterFactory extends TokenFilterFactory {
    
    protected Pattern patternToMatchShortenedUrls;
    
    public ResolveUrlTokenFilterFactory(Map<String,String> args) {
        super(args);
        
        assureMatchVersion();
        String shortenedUrlPattern = require(args, "shortenedUrlPattern");        
        patternToMatchShortenedUrls = Pattern.compile(shortenedUrlPattern);
    }
    
    @Override
    public TokenFilter create(TokenStream input) {
        return new ResolveUrlTokenFilter(input, patternToMatchShortenedUrls);
    }
}
