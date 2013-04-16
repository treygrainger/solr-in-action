package sia.ch14;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

class MultiTextFieldInput{
	
	public final char keyFromTextDelimiter;
	public final char multiKeyDelimiter;
	
	public List<String> Keys;
	public Reader Reader;
	public Integer StrippedIncomingPrefixLength;
	
	public MultiTextFieldInput(Reader reader, char keyFromTextDelimiter, char multiKeyDelimiter) throws IOException {
		this.keyFromTextDelimiter = keyFromTextDelimiter;
		this.multiKeyDelimiter = multiKeyDelimiter;
		setReader(reader);  
	}

	public void setReader(Reader reader) throws IOException{
		StringBuffer beforeDelimiter = new StringBuffer();
		StringBuffer afterDelimiter = new StringBuffer();  
		this.Keys = new LinkedList<String>();
		this.StrippedIncomingPrefixLength = 0;
	    boolean delimiterWasHit = false; 
		for( int nextChar = reader.read(); 
		    nextChar != -1; 
		    nextChar = reader.read() ) { 
			if ( (!delimiterWasHit) && ( (char)nextChar == this.keyFromTextDelimiter ) ) {
				delimiterWasHit = true;
			}
			else{
				if (!delimiterWasHit){
					beforeDelimiter.append( (char)nextChar );
				}
				else{
					afterDelimiter.append( (char)nextChar );		        		  
				}	
			}
		} 

		String textAfterDelimiter = afterDelimiter.toString();
		String textBeforeDelimiter = beforeDelimiter.toString();
				
		if (delimiterWasHit){
			this.StrippedIncomingPrefixLength = (textBeforeDelimiter + this.multiKeyDelimiter).length();	
		
			//special case: if prefix is wrapped in [ ], this means the prefix does not exist
			//in the stored version of this field.  As such, we should adjust the position offsets.
			//i.e. [en,es|]hablo spanish.  In this case, the en,es| were added inside solr by language
			//detection or were stripped out before the stored values were saved
			if ( textBeforeDelimiter.startsWith("[") && textAfterDelimiter.startsWith("]") ){
				this.StrippedIncomingPrefixLength = 0;
				textBeforeDelimiter = textBeforeDelimiter.substring(1);
				textAfterDelimiter = textAfterDelimiter.substring(1);
			}
			
			String[] multiKeysArray = textBeforeDelimiter.split(String.valueOf(this.multiKeyDelimiter));
			String currentKey;
			for (int i = 0; i < multiKeysArray.length; i++){
				if ( multiKeysArray[i] != null){
					currentKey = multiKeysArray[i].trim();
					if (currentKey.length() > 0 && !this.Keys.contains(currentKey) ){
						this.Keys.add(currentKey);
					}
				}
			}
		}
		else{
			textAfterDelimiter = textBeforeDelimiter;
			textBeforeDelimiter = "";	
			this.StrippedIncomingPrefixLength = 0;
		}			
		
		this.Reader = new StringReader(textAfterDelimiter);
	}
}