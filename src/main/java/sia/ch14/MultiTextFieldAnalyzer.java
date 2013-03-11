package sia.ch14;

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.schema.IndexSchema;


public class MultiTextFieldAnalyzer extends Analyzer {

protected IndexSchema indexSchema;
	
protected MultiTextFieldSettings settings;

	public MultiTextFieldAnalyzer(IndexSchema indexSchema, MultiTextFieldSettings settings) {
		super(new PerFieldReuseStrategy());
		this.settings = settings;
		this.indexSchema = indexSchema;
	}
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		
		MultiTextFieldTokenizer multiTokenizer = new MultiTextFieldTokenizer(
				indexSchema, reader, fieldName, settings);

		return new TokenStreamComponents(multiTokenizer);

	}
	
  
}
