package sia.ch6;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class ResolveUrlTokenFilterFactory extends TokenFilterFactory {
    
    protected Pattern patternToMatchShortenedUrls;
    
    @Override
    public void init(Map<String,String> args) {
        super.init(args);
        assureMatchVersion();        
        patternToMatchShortenedUrls = Pattern.compile(args.get("shortenedUrlPattern"));
    }    
    
    @Override
    public TokenFilter create(TokenStream input) {
        return new ResolveUrlTokenFilter(input, patternToMatchShortenedUrls);
    }
}
