package sia.ch14;

import java.util.HashMap;


public class MultiTextFieldSettings {
	
	public final AnalyzerModes analyzerMode;
	public final char keyFromTextDelimiter;
	public final char multiKeyDelimiter;
	public final String defaultFieldTypeName;
	public final HashMap<String, String> fieldMappings;
	public final boolean ignoreMissingMappings;
	public final boolean removeDuplicates;
	public final boolean hidePrependedLangs;

		
	public enum AnalyzerModes{
		index,
		query,
		multiTerm
	}
	
	public MultiTextFieldSettings(
	  AnalyzerModes analyzerMode, char keyFromTextDelimiter, char multiKeyDelimiter,
	  String defaultFieldTypeName, HashMap<String, String> fieldMappings, 
	  boolean ignoreMissingMappings, boolean removeDuplicates, boolean hidePrependedLangs){
		
		this.analyzerMode = analyzerMode;		
		this.keyFromTextDelimiter = keyFromTextDelimiter;
		this.multiKeyDelimiter = multiKeyDelimiter;
		this.defaultFieldTypeName = defaultFieldTypeName;
		this.fieldMappings = fieldMappings;
		this.ignoreMissingMappings = ignoreMissingMappings;
		this.removeDuplicates = removeDuplicates;
		this.hidePrependedLangs = hidePrependedLangs;
		
	}


	
}
