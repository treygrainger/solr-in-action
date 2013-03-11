package sia.ch14;

import java.util.HashMap;


public class MultiTextFieldSettings {

	public char keyFromTextDelimiter = '|';
	public char multiKeyDelimiter = ',';
	public String defaultFieldTypeName = "";
	public HashMap<String, String> fieldMappings;
	public boolean ignoreMissingMappings = false;
	public AnalyzerModes analyzerMode;
		
	public enum AnalyzerModes{
		index,
		query,
		multiTerm
	}

}
