package com.sia.ch14;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

class MultiTextInput{
	
	public List<String> Keys;
	public Reader Reader;
	public Integer StrippedIncomingPrefixLength;
	
	public MultiTextInput(Reader reader) {

		StringBuffer beforeDelimiter = new StringBuffer();
		StringBuffer afterDelimiter = new StringBuffer();  
		this.Keys = new LinkedList<String>();
	    boolean delimiterWasHit = false; 
		try { 
	          for( int nextChar = reader.read(); 
	        		  nextChar != -1; 
	        		  nextChar = reader.read() ) { 
	        	  if ( (!delimiterWasHit) && ( (char)nextChar == MultiTextAnalyzer.KEY_FROM_TEXT_DELIMITER ) ) {
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
	      }
		catch (IOException e){
			//Really bad - no way to recover.  Blow up
			//as opposed to giving incorrect behavior
			this.Keys.clear();
			this.Reader = null;
			
		}
		
		String textBeforeDelimiter = beforeDelimiter.toString();
		this.StrippedIncomingPrefixLength = (textBeforeDelimiter + MultiTextAnalyzer.MULTI_KEY_DELIMITER).length();				
		
		String[] multiKeysArray = textBeforeDelimiter.split(String.valueOf(MultiTextAnalyzer.MULTI_KEY_DELIMITER));
		String currentKey;
		for (int i = 0; i < multiKeysArray.length; i++){
			if ( multiKeysArray[i] != null){
				currentKey = multiKeysArray[i].trim();
				if (currentKey.length() > 0 && !this.Keys.contains(currentKey) ){
					this.Keys.add(currentKey);
				}
			}
		}
		
		this.Reader = new StringReader(afterDelimiter.toString());
	      
	}
	
}