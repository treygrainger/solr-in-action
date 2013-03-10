package com.sia.ch14;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.TextField;

import com.sia.ch14.MultiTextAnalyzer.AnalyzerModes;

/**
 * 
 * This field exist purely to expose the schema with defined
 * field types from the schema to the MultiTextAnalyzer.
 *
 */
public class MultiTextField extends TextField  {

	private String FIELD_MAPPINGS = "fieldMappings";
	private String DEFAULT_FIELD_TYPE = "defaultFieldType";
	
	public MultiTextField() {
		// TODO Auto-generated constructor stub
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void init(IndexSchema schema, Map<String,String> args) {
		super.init(schema, args);
		MultiTextAnalyzer indexAnalyzer = new MultiTextAnalyzer(schema, AnalyzerModes.index);
		MultiTextAnalyzer queryAnalyzer = new MultiTextAnalyzer(schema, AnalyzerModes.query);
		MultiTextAnalyzer multiTermAnalyzer = new MultiTextAnalyzer(schema, AnalyzerModes.multiTerm);
		
		if (args.containsKey(DEFAULT_FIELD_TYPE)){
			if (schema.getFieldTypes().containsKey(args.get(DEFAULT_FIELD_TYPE))){
				indexAnalyzer.setDefaultFieldType(args.get(DEFAULT_FIELD_TYPE));
				queryAnalyzer.setDefaultFieldType(args.get(DEFAULT_FIELD_TYPE));
				multiTermAnalyzer.setDefaultFieldType(args.get(DEFAULT_FIELD_TYPE));
			}
			else {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Invalid defaultFieldType defined in " 
							+ this.getClass().getSimpleName() + ". FieldType '" + args.get(DEFAULT_FIELD_TYPE)
							+ " does not exist in the schema.");
				
			}
		}
		
		if (args.containsKey(FIELD_MAPPINGS)){
			HashMap<String, String> possibleFieldMappings = new HashMap<String, String>();
			if (schema.getFieldTypes().containsKey(args.get(FIELD_MAPPINGS))){
				if (args.get(FIELD_MAPPINGS).length() > 0){
					String[] mappingPairs = args.get(FIELD_MAPPINGS).split(",");
					for (int i = 0; i< mappingPairs.length; i++){
						String[] mapping = mappingPairs[i].split(":");
						for (int j = 0; j < mapping.length; j++){
							String key = "";
							String fieldType = "";
							if (mapping.length == 2){
								key = mapping[0].trim();
								fieldType = mapping[1].trim();
							}
							else if (mapping.length == 1){
								fieldType = mapping[1].trim();
								key = fieldType;								
							}
							if (mapping.length == 0 || mapping.length > 2){
								throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
									"Schema configuration error for " + this.getClass().getSimpleName()
									+ ". Field Mapping '" + mappingPairs [i] + "' is syntactically incorrect.");
							}
							else{
								if (!schema.getFieldTypes().containsKey(fieldType)){
									throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
										"Schema configuration error for " + this.getClass().getSimpleName()
										+ ".  FieldType '" + fieldType + "' is not defined.");
								}
							}
							possibleFieldMappings.put(key, fieldType);
						}
					}
					
				}
				
				indexAnalyzer.setFieldMappings(possibleFieldMappings);
				queryAnalyzer.setFieldMappings(possibleFieldMappings);
				multiTermAnalyzer.setFieldMappings(possibleFieldMappings);
			}
		 }
		
		this.setAnalyzer(indexAnalyzer);
		this.setQueryAnalyzer(queryAnalyzer);
		this.setMultiTermAnalyzer(multiTermAnalyzer);
	}
	
	
}
