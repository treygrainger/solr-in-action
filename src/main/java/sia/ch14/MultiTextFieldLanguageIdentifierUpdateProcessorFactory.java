package sia.ch14;

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.LangDetectLanguageIdentifierUpdateProcessor;
import org.apache.solr.update.processor.LangDetectLanguageIdentifierUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.util.SolrPluginUtils;

public class MultiTextFieldLanguageIdentifierUpdateProcessorFactory 
	extends LangDetectLanguageIdentifierUpdateProcessorFactory {

	  @Override
	  public UpdateRequestProcessor getInstance(SolrQueryRequest req,
	                                            SolrQueryResponse rsp, UpdateRequestProcessor next) {
	    // Process defaults, appends and invariants if we got a request
	    if(req != null) {
	      SolrPluginUtils.setDefaults(req, defaults, appends, invariants);
	    }
	    return new MultiTextFieldLanguageIdentifierUpdateProcessor(req, rsp, next);
	  }
	  
	  
	  
	  
	
}
