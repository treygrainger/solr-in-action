package sia.ch14;

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilter;
import org.apache.solr.schema.IndexSchema;


public class MultiTextFieldAnalyzer extends Analyzer {

protected IndexSchema indexSchema;
	
public final MultiTextFieldSettings Settings;

	public MultiTextFieldAnalyzer(IndexSchema indexSchema, MultiTextFieldSettings settings) {
		super(new PerFieldReuseStrategy());
		this.Settings = settings;
		this.indexSchema = indexSchema;
	}
	
	@Override
	public TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		
		MultiTextFieldTokenizer multiTokenizer = new MultiTextFieldTokenizer(
				indexSchema, reader, fieldName, Settings);
		
		Tokenizer source = multiTokenizer;
		TokenStream result = multiTokenizer;
		if (Settings.removeDuplicates){
			result = new RemoveDuplicatesTokenFilter(multiTokenizer);
		}
		
		return new TokenStreamComponents(source, result);

	}
	
}
