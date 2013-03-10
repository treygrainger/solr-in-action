package com.sia.ch14;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.TextField;

public class MultiTextAnalyzer extends Analyzer {

protected IndexSchema indexSchema;

	
	final static char KEY_FROM_TEXT_DELIMITER = '|';
	final static char MULTI_KEY_DELIMITER = ',';

	protected String defaultFieldName = "";
	//protected Boolean ignoreInvalidFieldTypes = false;
	
	protected HashMap<String, String> fieldMappings;
	
	protected KeyedReuseStrategy tokenStreamComponentsCache;
	
	public enum AnalyzerModes{
		index,
		query,
		multiTerm
	}
	
	protected AnalyzerModes analyzerMode;
	
	public MultiTextAnalyzer(IndexSchema indexSchema, AnalyzerModes analyzerMode) {
		super(new NeverReuseStrategy()); //Don't cache per field in base class
		this.analyzerMode = analyzerMode;
		this.indexSchema = indexSchema;
		this.tokenStreamComponentsCache = new KeyedReuseStrategy(); //for cached object reuse
	}
	
	public void setDefaultFieldType(String defaultFieldTypeName){
		this.defaultFieldName = defaultFieldTypeName;
	}
	
	public void setFieldMappings(HashMap<String, String> fieldMappings){
		this.fieldMappings = fieldMappings;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		//This field swaps out token components on a per doc/query basis depending upon
		//the keys passed in at the beginning of the reader's text.  As such, it caches
		//its own key to components mapping and swaps it out on each call.
				
		MultiTextInput multiTextInput = new MultiTextInput(reader);
		StringBuilder cacheKey = new StringBuilder();
		cacheKey.append(fieldName);
		cacheKey.append(" ");
		cacheKey.append(this.analyzerMode.toString());
		cacheKey.append(" ");
		for (int i = 0; i < multiTextInput.Keys.size(); i++){
			if (i > 0) {
				cacheKey.append(",");
			}
			cacheKey.append(multiTextInput.Keys.get(i));
		}
		TokenStreamComponents components = tokenStreamComponentsCache.getReusableComponents(cacheKey.toString());		
		
		final Reader r = initReader(fieldName, multiTextInput.Reader);
	    if (components == null) {
	      components = createComponents(cacheKey.toString(), multiTextInput.Keys,  r, multiTextInput.StrippedIncomingPrefixLength);
	      tokenStreamComponentsCache.setReusableComponents(cacheKey.toString(), components);
	    } 
	    else {
	    	try{
	    		components.getTokenizer().setReader(r);
	    	}
	    	catch(IOException e){
	    		//cache is busted... re-create components
	    		components = createComponents(fieldName, multiTextInput.Keys, r, multiTextInput.StrippedIncomingPrefixLength);
	    		tokenStreamComponentsCache.setReusableComponents(cacheKey.toString(), components);
	    	}
	    }
	    return components;
		
		
	}
	
	protected TokenStreamComponents createComponents(String fieldName, List<String> keys,
			Reader reader, Integer startingOffset) {
		
		LinkedHashMap<String, Analyzer> namedAnalyzers = new LinkedHashMap<String, Analyzer>();
		
		//Create a list of all sub-field Types for this field + keys
		FieldType fieldType;
		for (int i = 0; i < keys.size(); i++){
			
			String fieldTypeName = keys.get(i);
			if (this.fieldMappings != null){
				fieldTypeName = this.fieldMappings.get(keys.get(i));
			}
			
			fieldType = this.indexSchema.getFieldTypeByName(fieldTypeName);
			if (fieldType != null){
				if (this.analyzerMode == AnalyzerModes.query){
					namedAnalyzers.put(fieldTypeName, fieldType.getQueryAnalyzer());
				}
				else if (this.analyzerMode == AnalyzerModes.multiTerm){
				
					namedAnalyzers.put(fieldTypeName, ((TextField)fieldType).getMultiTermAnalyzer());
				}
				else{
					namedAnalyzers.put(fieldTypeName,fieldType.getAnalyzer());
				}
			}
			else{
				throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Invalid Text FieldType requested: '" + keys.get(i) + "'");
			}
		}
			

		if (namedAnalyzers.size() < 1){
			if (this.defaultFieldName != null && this.defaultFieldName.length() > 0) {
				if (this.analyzerMode == AnalyzerModes.query){
					namedAnalyzers.put("", this.indexSchema.getFieldType(this.defaultFieldName).getQueryAnalyzer());
				}
				else if (this.analyzerMode == AnalyzerModes.multiTerm){
					namedAnalyzers.put("", 
							((TextField)this.indexSchema.getFieldType(this.defaultFieldName)).getMultiTermAnalyzer());
				}
				else{
					namedAnalyzers.put("", this.indexSchema.getFieldType(this.defaultFieldName).getAnalyzer());
				}
			}

		}
		
		///pass the list of analyzers to the MultiTokenizer
		MultiAnalyzerTokenizer multiTokenizer = new MultiAnalyzerTokenizer(reader, fieldName, namedAnalyzers, startingOffset);
		return new TokenStreamComponents(multiTokenizer);

	}	
	
  
}
