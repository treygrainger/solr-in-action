package sia.ch14;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.TextField;

import sia.ch14.MultiTextFieldSettings.AnalyzerModes;

public class MultiTextFieldTokenizer extends Tokenizer {
  protected String fieldName;
  protected IndexSchema indexSchema;
  protected MultiTextFieldSettings settings;
  protected LinkedHashMap<String, Analyzer> namedAnalyzers;
  protected MultiTextFieldInput multiTextInput;

  private CharTermAttribute charTermAttribute;
  private OffsetAttribute offsetAttribute;
  private TypeAttribute typeAttribute;
  private PositionIncrementAttribute positionAttribute;
  private LinkedList<Token> tokens;
  private Integer startingOffset;

  protected MultiTextFieldTokenizer(IndexSchema indexSchema, Reader input,
      String fieldName, MultiTextFieldSettings settings) {
    super(input);
    this.indexSchema = indexSchema;
    this.fieldName = fieldName;
    this.settings = settings;
    init();
  }

  private void init() {
    charTermAttribute = addAttribute(CharTermAttribute.class);
    offsetAttribute = addAttribute(OffsetAttribute.class);
    typeAttribute = addAttribute(TypeAttribute.class);
    positionAttribute = addAttribute(PositionIncrementAttribute.class);
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    this.tokens = null;
    if (this.multiTextInput == null) {
      this.multiTextInput = new MultiTextFieldInput(
          this.input, this.settings.keyFromTextDelimiter, 
          this.settings.multiKeyDelimiter);
    } else {
      this.multiTextInput.setReader(this.input);
    }
    this.namedAnalyzers = getNamedAnalyzers();
    this.startingOffset = this.multiTextInput.StrippedIncomingPrefixLength 
        >= 0 ? this.multiTextInput.StrippedIncomingPrefixLength : 0;
  }

  private LinkedHashMap<String, Analyzer> getNamedAnalyzers() {

    /*
     * TODO: Add caching of namedAnalyzers using ClosableThreadLocal per cache
     * key to prevent them from having to be regenerated on every request
     */
    LinkedHashMap<String, Analyzer> namedAnalyzers = new LinkedHashMap<String, Analyzer>();

    // Create a list of all sub-field Types for this field + keys
    FieldType fieldType;
    for (int i = 0; i < this.multiTextInput.Keys.size(); i++) {

      String fieldTypeName = this.multiTextInput.Keys.get(i);
      if (this.settings.fieldMappings != null) {
        fieldTypeName = this.settings.fieldMappings
            .get(this.multiTextInput.Keys.get(i));
      }

      fieldType = this.indexSchema.getFieldTypeByName(fieldTypeName);
      if (fieldType != null) {
        if (this.settings.analyzerMode == AnalyzerModes.query) {
          namedAnalyzers.put(fieldTypeName, fieldType.getQueryAnalyzer());
        } else if (this.settings.analyzerMode == AnalyzerModes.multiTerm) {

          namedAnalyzers.put(fieldTypeName,
              ((TextField) fieldType).getMultiTermAnalyzer());
        } else {
          namedAnalyzers.put(fieldTypeName, fieldType.getAnalyzer());
        }
      } else {
        if (!this.settings.ignoreMissingMappings) {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
              "Invalid FieldMapping requested: '"
                  + this.multiTextInput.Keys.get(i) + "'");
        }
      }
    }

    if (namedAnalyzers.size() < 1) {
      if (this.settings.defaultFieldTypeName != null
          && this.settings.defaultFieldTypeName.length() > 0) {
        if (this.settings.analyzerMode == AnalyzerModes.query) {
          namedAnalyzers.put(
              "",
              this.indexSchema.getFieldTypeByName(
                  this.settings.defaultFieldTypeName).getQueryAnalyzer());
        } else if (this.settings.analyzerMode == AnalyzerModes.multiTerm) {
          namedAnalyzers.put("", ((TextField) this.indexSchema
              .getFieldTypeByName(this.settings.defaultFieldTypeName))
              .getMultiTermAnalyzer());
        } else {
          namedAnalyzers.put(
              "",
              this.indexSchema.getFieldTypeByName(
                  this.settings.defaultFieldTypeName).getAnalyzer());
        }
      }

    }

    if (namedAnalyzers.size() == 0) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
          "No FieldMapping was Requested, and no DefaultField"
              + " is defined for MultiTextField '" + this.fieldName
              + "'. A MultiTextField must have one or more "
              + "FieldTypes requested to execute a query.");
    }

    return namedAnalyzers;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (this.tokens == null) {
      String data = convertReaderToString(this.multiTextInput.Reader);
      if (data.equals("")) {
        return false;
      }

      // get tokens
      this.tokens = mergeToSingleTokenStream(createPositionsToTokensMap(
          this.namedAnalyzers, data));

      if (this.tokens == null) {
        // at end of stream for some reason
        return false;
      }
    }

    if (tokens.isEmpty()) {
      this.tokens = null;
      return false;
    } else {
      clearAttributes();
      Token token = tokens.removeFirst();

      this.charTermAttribute.copyBuffer(token.buffer(), 0, token.length());
      this.offsetAttribute.setOffset(token.startOffset(), token.endOffset()
          + this.startingOffset);
      this.typeAttribute.setType(token.type());
      this.positionAttribute.setPositionIncrement(token.getPositionIncrement());

      return true;
    }

  }

  private SortedMap<Integer, LinkedList<Token>> createPositionsToTokensMap(
      LinkedHashMap<String, Analyzer> namedAnalyzers, String text)
      throws IOException {

    /*
     * Maps Position in document (PositionIncrement) to List of tokens at that
     * position this allows similarly tokenized token streams to store identical
     * tokens at the same position to preserve cross-language phrase searching
     * and allow duplicates to be removed
     */
    SortedMap<Integer, LinkedList<Token>> tokenHash = new TreeMap<Integer, LinkedList<Token>>();

    /*
     * add the tokenstream for each sub-field type into the position to token
     * mapping
     */
    for (Map.Entry<String, Analyzer> namedAnalyzer : this.namedAnalyzers
        .entrySet()) {

      /*
       * Important - Because TokenStreamComponents are cached per field, we need
       * to simulate unique sub-fields within each field
       */
      String subFieldName = (this.fieldName + " " + namedAnalyzer.getKey())
          .trim();

      addTokenStreamForFieldType(tokenHash, namedAnalyzer.getValue()
          .tokenStream(subFieldName, new StringReader(text)));
    }

    return tokenHash;
  }

  /**
   * Takes each token from tokenizer, and adds it to a list of terms that appear
   * at that token's position in the input stream.
   * 
   * @throws IOException
   */
  private void addTokenStreamForFieldType(
      SortedMap<Integer, LinkedList<Token>> tokenHash, TokenStream tokenStream)
      throws IOException {

    tokenStream.reset();
    int position = 0;

    // setup attributes
    CharTermAttribute charTermAtt = null;
    PositionIncrementAttribute posIncrAtt = null;
    OffsetAttribute offsetAtt = null;
    TypeAttribute typeAtt = null;

    if (tokenStream.hasAttribute(CharTermAttribute.class)) {
      charTermAtt = tokenStream.getAttribute(CharTermAttribute.class);
    }

    if (tokenStream.hasAttribute(PositionIncrementAttribute.class)) {
      posIncrAtt = tokenStream.getAttribute(PositionIncrementAttribute.class);
    }

    if (tokenStream.hasAttribute(OffsetAttribute.class)) {
      offsetAtt = tokenStream.getAttribute(OffsetAttribute.class);
    }

    if (tokenStream.hasAttribute(TypeAttribute.class)) {
      typeAtt = tokenStream.getAttribute(TypeAttribute.class);
    }

    for (boolean hasMoreTokens = tokenStream.incrementToken(); hasMoreTokens; hasMoreTokens = tokenStream
        .incrementToken()) {

      String multiTermSafeType = null;

      if (charTermAtt == null
          || offsetAtt == null
          || (typeAtt == null && this.settings.analyzerMode != AnalyzerModes.multiTerm)) {
        return;
      }

      if (typeAtt != null) {
        multiTermSafeType = typeAtt.type();
      }

      Token clone = new Token(charTermAtt.toString().trim(),
          offsetAtt.startOffset(), offsetAtt.endOffset(), multiTermSafeType);
      position += ((posIncrAtt != null) ? posIncrAtt.getPositionIncrement() : 1);

      if (!tokenHash.containsKey(position)) {
        tokenHash.put(position, new LinkedList<Token>());
      }

      tokenHash.get(position).add(clone);
    }
    tokenStream.close();

  }

  private static String convertReaderToString(Reader reader) {
    try {
      StringBuilder readInto = new StringBuilder();
      CharBuffer buffer = CharBuffer.allocate(100);
      while (reader.read(buffer) > 0) {
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
   * Convert the mapping{position, terms} to a list of tokens with appropriate
   * position increments.
   */
  private static LinkedList<Token> mergeToSingleTokenStream(
      SortedMap<Integer, LinkedList<Token>> tokenHash) {

    LinkedList<Token> result = new LinkedList<Token>();

    int currentPosition = 0;
    for (int newPosition : tokenHash.keySet()) {
      int incrementTokenIndex = result.size();

      LinkedList<Token> brothers = tokenHash.get(newPosition);
      /*
       * The first item in the list gets the position increment; the rest have a
       * position increment of 0.
       */

      int positionIncrement = newPosition - currentPosition;

      // set all token to 0 increment
      for (Token token : brothers) {
        token.setPositionIncrement(0);
        result.add(token);
      }

      // increment position of the first token
      if (result.size() > incrementTokenIndex
          && result.get(incrementTokenIndex) != null) {
        result.get(incrementTokenIndex).setPositionIncrement(positionIncrement);
      }

      currentPosition = newPosition;
    }

    return result;
  }

}
