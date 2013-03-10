package com.sia.ch14;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public class MultiAnalyzerTokenizer extends Tokenizer {
	protected String fieldName;
	protected LinkedHashMap<String, Analyzer> namedAnalyzers;
	private LinkedList<Token> tokens;
	private Integer startingOffset;
	private CharTermAttribute charTermAttribute;
    private OffsetAttribute offsetAttribute;
    private TypeAttribute typeAttribute;
    private PositionIncrementAttribute positionAttribute;

	
	
	protected MultiAnalyzerTokenizer(Reader input, String fieldName, LinkedHashMap<String, Analyzer> namedAnalyzers, Integer startingOffset) {
		super(input);
		this.fieldName = fieldName;
		this.namedAnalyzers = namedAnalyzers;
		this.startingOffset = startingOffset >= 0 ? startingOffset : 0;
		init();
		
	}
	
	private void init(){
		charTermAttribute = (CharTermAttribute)addAttribute(org.apache.lucene.analysis.tokenattributes.CharTermAttribute.class);
		offsetAttribute = (OffsetAttribute)addAttribute(org.apache.lucene.analysis.tokenattributes.OffsetAttribute.class);
		typeAttribute = (TypeAttribute)addAttribute(org.apache.lucene.analysis.tokenattributes.TypeAttribute.class);
		positionAttribute = (PositionIncrementAttribute)addAttribute(org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute.class);
	}
	
	@Override
	public void reset() throws IOException{
		super.reset();
		this.tokens = null;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if(this.tokens == null){
			String data = convertReaderToString(input);
	        if (data.equals("")){
	        	return false;
	        }
	        
	        // get tokens        
	        this.tokens = mergeToSingleTokenStream(createPositionsToTokensMap(this.namedAnalyzers, data));
			
			if(this.tokens == null){
				// at end of stream for some reason
				return false;
			}
		}
		
		if(tokens.isEmpty()){
			this.tokens = null;
			return false;
		} else {
			clearAttributes();
			Token token = tokens.removeFirst();
			
			this.charTermAttribute.copyBuffer(token.buffer(), 0, token.length());
			this.offsetAttribute.setOffset(token.startOffset(), token.endOffset() + this.startingOffset);
			this.typeAttribute.setType(token.type());
			this.positionAttribute.setPositionIncrement(token.getPositionIncrement());
			
            return true;
		}

	}
	
         
	protected SortedMap<Integer, LinkedList<Token>> createPositionsToTokensMap(LinkedHashMap<String, Analyzer> namedAnalyzers, String text) throws IOException{

		// A mapping from an absolute document position to all the tokens
        // that are at that position.
        SortedMap<Integer, LinkedList<Token>> tokenHash = new TreeMap<Integer, LinkedList<Token>>();

        // The tokenizer for each language will each spit out a token at the
        // same position; thus we have a map from position to a list of
        // tokens.
        for (Map.Entry<String, Analyzer> namedAnalyzer : namedAnalyzers.entrySet()) {

        	//Important - Because TokenStreamComponents are cached per field, we need
        	//to simulate unique sub-fields withing each field
        	String subFieldName = (this.fieldName + " " + namedAnalyzer.getKey()).trim();
        	
            fillHash(tokenHash, namedAnalyzer.getValue().tokenStream(subFieldName, new StringReader(text)));
        }

        return tokenHash;
	}

	/**
     * Takes each token from tokenizer, and adds it to a list of terms that
     * appear at that token's position in the input stream.
	 * @throws IOException 
     */
	private void fillHash(SortedMap<Integer, LinkedList<Token>> tokenHash, TokenStream tokenStream) throws IOException {

		tokenStream.reset();
		int position = 0;
		
		// setup attributes 
		CharTermAttribute charTermAtt = null;
		PositionIncrementAttribute posIncrAtt = null;
		OffsetAttribute offsetAtt = null;
		TypeAttribute typeAtt = null;
		
		
		if(tokenStream.hasAttribute(CharTermAttribute.class)){
			charTermAtt = (CharTermAttribute) tokenStream.getAttribute(CharTermAttribute.class);
		}
		
    	if (tokenStream.hasAttribute(PositionIncrementAttribute.class)) {
    		posIncrAtt = (PositionIncrementAttribute) tokenStream.getAttribute(PositionIncrementAttribute.class);
    	}
    	
    	if(tokenStream.hasAttribute(OffsetAttribute.class)){
    		offsetAtt = (OffsetAttribute)tokenStream.getAttribute(OffsetAttribute.class);
    	}
		
    	if(tokenStream.hasAttribute(TypeAttribute.class)){
    		typeAtt = (TypeAttribute)tokenStream.getAttribute(TypeAttribute.class);
    	}

    	
    	try{
	    	// loop 

			for(boolean hasMoreTokens = tokenStream.incrementToken(); hasMoreTokens; hasMoreTokens = tokenStream.incrementToken()){
				
				if(charTermAtt == null || offsetAtt == null || typeAtt == null){
					return;
				}
				
				Token clone = new Token(charTermAtt.toString().trim(), offsetAtt.startOffset(), offsetAtt.endOffset(), typeAtt.type());
				position += ((posIncrAtt != null) ? posIncrAtt.getPositionIncrement() : 1);
				
				if(!tokenHash.containsKey(position)){
					tokenHash.put(position, new LinkedList<Token>());
				}
				
				tokenHash.get(position).add(clone);
			}
    	} catch (IOException e) {
		  throw new RuntimeException(e); // to avoid adding to signature.
		}
		
	}
	
	public static String convertReaderToString(Reader reader){
		try {
            StringBuilder readInto = new StringBuilder();
            CharBuffer buffer = CharBuffer.allocate(100);
            while(reader.read(buffer) > 0) {
                buffer.flip();
                readInto.append(buffer);
                buffer.rewind();
            }
            return readInto.toString();
        } catch (IOException e) { 
            throw new RuntimeException(e); 
        }
	}
	
	/**
     * Convert the mapping{position, terms} to a list of tokens 
     * with appropriate position increments.
     */
	public static LinkedList<Token> mergeToSingleTokenStream(SortedMap<Integer, LinkedList<Token>> tokenHash) {
    	
    	LinkedList<Token> result = new LinkedList<Token>();
        
    	int currentPosition = 0;
        for (int newPosition : tokenHash.keySet()) {
        	int incrementTokenIndex = result.size();
        	
            LinkedList<Token> brothers = tokenHash.get(newPosition);
            // The first item in the list gets the position increment; the
            // rest have a position increment of 0.
           
            int positionIncrement = newPosition - currentPosition;
            
            // set all token to 0 increment
            for(Token token : brothers){
            		token.setPositionIncrement(0);            		
            		result.add(token);          	
            }
            
            // increment position of the first token
            if(result.size() > incrementTokenIndex && result.get(incrementTokenIndex) != null){
            	result.get(incrementTokenIndex).setPositionIncrement(positionIncrement);
            }            
            
            currentPosition = newPosition;
        }

        return result;
    }
	

}
