package sia.ch14;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.PreAnalyzedField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.processor.DetectedLanguage;
import org.apache.solr.update.processor.LanguageIdentifierUpdateProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class MultiTextFieldLanguageIdentifierUpdateProcessor
    extends org.apache.solr.update.processor.LangDetectLanguageIdentifierUpdateProcessor {
	
	  //protected final static Logger log = LoggerFactory
	    //      .getLogger(MultiTextFieldLanguageIdentifierUpdateProcessor.class);
	
	private static String MULTI_TEXT_FIELD_LANGID = "mtf-langid";
	private static String MULTI_VALUED_FIELD_LANGS = MULTI_TEXT_FIELD_LANGID + ".multiValuedFieldLangs";
	private static String PREPEND_FIELDS = MULTI_TEXT_FIELD_LANGID + ".prependFields";
	
	protected IndexSchema indexSchema;
	protected Boolean separateLanguagePerValueInMultiValuedFields = true;
	protected Collection<String> fieldsForPrependingLanguages = new LinkedHashSet<String>();
	
	public MultiTextFieldLanguageIdentifierUpdateProcessor(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		super(req, rsp, next) ;
		indexSchema = req.getSchema();
		
		initParams(req.getParams());
	}
	
	
	private void initParams(SolrParams params){
	      String prependFields = params.get(PREPEND_FIELDS);
	      for (String field : prependFields.split(",")){
	    	  String trimmed = field.trim();
	    	  if (this.indexSchema.getFieldOrNull(trimmed) != null){
	    		  this.fieldsForPrependingLanguages.add(trimmed);
	    	  }
	    	  else{
	    		  log.error("Unsupported format for" + PREPEND_FIELDS + ":" + trimmed + ". Skipping prepending langs to this field.");
	    	  }
	      }
          this.separateLanguagePerValueInMultiValuedFields = params.getBool(MULTI_VALUED_FIELD_LANGS);
	}
	
	
	@Override
	protected SolrInputDocument process(SolrInputDocument doc) {
		SolrInputDocument outputDocument = super.process(doc);
		
		Collection<String> fieldNames = new ArrayList<String>();
		for (String nextFieldName : outputDocument.getFieldNames()){
			fieldNames.add(nextFieldName);			
		}

		for (String nextFieldName : fieldNames){
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
			  || mtfAnalyzer.Settings.fieldMappings.containsKey(lang.getLangCode()) 
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
		
		SolrInputField inputField = doc.getField(multiTextFieldName);
	    SolrInputField outputField = new SolrInputField(inputField.getName());
	    for (final Object inputValue : inputField.getValues()) {
	      Object outputValue = inputValue;
	      outputValue = "[" + fieldValuePrefix + "]" + (String)outputValue;
	      
	      IndexableField separatedField = fromString(indexSchema.getField(multiTextFieldName), outputValue.toString(), inputValue.toString(), 1.0f);
	        outputField.addValue(separatedField, 1.0F);
	    }
	    outputField.setBoost(inputField.getBoost());
	    
	    doc.removeField(multiTextFieldName);
	    doc.put(multiTextFieldName, outputField);
	return doc;
		
	}
	
	public IndexableField fromString(SchemaField field, String indexableValue, String storableValue, float boost) {
		org.apache.lucene.document.FieldType type = PreAnalyzedField.createFieldType(field);
	    if (type == null) {
	      return null;
	    }
	    Field f = null;
	    if (storableValue != null) {
	      if (field.stored()) {
	        f = new Field(field.getName(), storableValue, type);
	      } else {
	        type.setStored(false);
	      } 
	    } else {
	      type.setStored(false);
	    }
	    
	    if (indexableValue != null) {
	      if (field.indexed()) {
	        type.setIndexed(true);
	        type.setTokenized(true);
	        if (f != null) {
	        	f.setStringValue(indexableValue);
	          //f.setTokenStream(f.tokenStreamValue());
	        } else {
	          f = new Field(field.getName(), indexableValue, type);
	        }
	      } else {
	        if (f != null) {
	          f.fieldType().setIndexed(false);
	          f.fieldType().setTokenized(false);
	        }
	      }
	    }
	    if (f != null) {
	      f.setBoost(boost);
	    }
	    return f;
	}
	
		
}
	
