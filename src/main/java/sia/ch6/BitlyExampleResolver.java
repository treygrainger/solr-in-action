package sia.ch6;

public class BitlyExampleResolver implements UrlResolver {
    public String expand(String shortenedUrl) {
        // TODO: implement a real way to resolve shortened URLs
        if ("http://bit.ly/3ynriE".equals(shortenedUrl)) {
            return "lucene.apache.org/solr";
        } else if ("http://bit.ly/15tzw".equals(shortenedUrl)) {
            return "manning.com";
        } else {
            return shortenedUrl;
        }
    }
}
