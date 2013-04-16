package sia.ch14;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.update.processor.DetectedLanguage;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class MultiTextFieldLanguageIdentifierUpdateProcessor
    extends org.apache.solr.update.processor.LangDetectLanguageIdentifierUpdateProcessor {

	protected IndexSchema indexSchema;
	
	public MultiTextFieldLanguageIdentifierUpdateProcessor(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		super(req, rsp, next) ;
		indexSchema = req.getSchema();
	}
	
	@Override
	protected SolrInputDocument process(SolrInputDocument doc) {
		SolrInputDocument outputDocument = super.process(doc);
		
		for (String nextFieldName : outputDocument.getFieldNames()){
			if (indexSchema.getFieldOrNull(nextFieldName) != null){
				if (indexSchema.getField(nextFieldName).getType() instanceof MultiTextField){
					outputDocument = detectAndPrependLanguages(outputDocument, nextFieldName);
				}
			}

				
		}
		return outputDocument;
	}
		
	protected SolrInputDocument detectAndPrependLanguages(SolrInputDocument doc, String multiTextFieldName){
		MultiTextField mtf = (MultiTextField) indexSchema.getFieldType(multiTextFieldName);
		MultiTextFieldAnalyzer mtfAnalyzer = (MultiTextFieldAnalyzer)mtf.getAnalyzer();
		List<DetectedLanguage> detectedLanguages = this.detectLanguage(this.concatFields(doc, new String[]{multiTextFieldName}));
		
		StringBuilder fieldValuePrefix = new StringBuilder();
		for (DetectedLanguage lang : detectedLanguages){
			if ( mtfAnalyzer.Settings.ignoreMissingMappings 
			  || mtfAnalyzer.Settings.fieldMappings.containsKey(lang) 
			  || indexSchema.getFieldOrNull(lang.getLangCode()) != null ){
				if (fieldValuePrefix.length() > 0){
					fieldValuePrefix.append(mtfAnalyzer.Settings.multiKeyDelimiter);
				}
				fieldValuePrefix.append(lang.getLangCode());
				
			}
		}
		if (fieldValuePrefix.length() > 0 ){
			fieldValuePrefix.append(mtfAnalyzer.Settings.keyFromTextDelimiter);
		}
	
		//do I need to pre-pend for every value, or just the first on in a multivalued field?
		//going with just the first on for now under the theory that the tokenizer is only used once
		//per field regardless on num of values... we'll see.
		boolean prefixAppendedToFirstValue = false;
		
		SolrInputField inputField = doc.getField(multiTextFieldName);
	    SolrInputField outputField = new SolrInputField(inputField.getName());
	    for (final Object inputValue : inputField.getValues()) {
	      Object outputValue = inputValue;
	      if (!prefixAppendedToFirstValue){
	    	  outputValue = "[" + fieldValuePrefix + "]" + (String)outputValue;
	      }
	        outputField.addValue(outputValue, 1.0F);
	    }
	    outputField.setBoost(inputField.getBoost());
	    
	    doc.removeField(multiTextFieldName);
	    doc.put(multiTextFieldName, outputField);
	return doc;
		
	}
		
}
	
