package sia.ch14;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.update.processor.DetectedLanguage;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.LangDetectLanguageIdentifierUpdateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTextFieldLanguageIdentifierUpdateProcessor extends
    LangDetectLanguageIdentifierUpdateProcessor {

  protected final static Logger log = LoggerFactory
      .getLogger(MultiTextFieldLanguageIdentifierUpdateProcessor.class);

  private static String MULTI_TEXT_FIELD_LANGID = "mtf-langid";
  private static String PREPEND_GRANULARITY = MULTI_TEXT_FIELD_LANGID
      + ".prependGranularity";
  private final static String HIDE_PREPENDED_LANGS = MULTI_TEXT_FIELD_LANGID
      + ".hidePrependedLangs";

  private enum PrependGranularities {
    document, field, fieldValue
  }

  private static String PREPEND_FIELDS = MULTI_TEXT_FIELD_LANGID
      + ".prependFields";

  protected IndexSchema indexSchema;
  protected Collection<String> prependFields = new LinkedHashSet<String>();
  private PrependGranularities prependGranularity = PrependGranularities.document;
  private Boolean hidePrependedLangs = true;

  public MultiTextFieldLanguageIdentifierUpdateProcessor(SolrQueryRequest req,
      SolrQueryResponse rsp, UpdateRequestProcessor next) {
    super(req, rsp, next);
    indexSchema = req.getSchema();
    initParams(req.getParams());
  }

  private void initParams(SolrParams params) {
    String prependFields = params.get(PREPEND_FIELDS);
    for (String field : prependFields.split(",")) {
      String trimmed = field.trim();
      if (this.indexSchema.getFieldOrNull(trimmed) != null) {
        this.prependFields.add(trimmed);
      } else {
        log.error("Unsupported format for" + PREPEND_FIELDS + ":" + trimmed
            + ". Skipping prepending langs to this field.");
      }
    }

    String prependGranularity = params.get(PREPEND_GRANULARITY);
    if (prependGranularity != null && prependGranularity.trim().length() > 0) {
      if (prependGranularity.trim().equals("document")) {
        this.prependGranularity = PrependGranularities.document;
      } else if (prependGranularity.trim().equals("field")) {
        this.prependGranularity = PrependGranularities.field;
      } else if (prependGranularity.trim().equals("fieldValue")) {
        this.prependGranularity = PrependGranularities.fieldValue;
      } else {
        log.error("Unsupported format for" + PREPEND_GRANULARITY + ":"
            + prependGranularity + ". Using "
            + this.prependGranularity.toString() + ".");
      }
    }

    this.hidePrependedLangs = params.getBool(HIDE_PREPENDED_LANGS, false);

  }

  @Override
  protected SolrInputDocument process(SolrInputDocument doc) {
    SolrInputDocument outputDocument = super.process(doc);

    Collection<String> fieldNames = new ArrayList<String>();
    for (String nextFieldName : outputDocument.getFieldNames()) {
      fieldNames.add(nextFieldName);
    }

    List<DetectedLanguage> documentLangs = this.detectLanguage(this
        .concatFields(doc, this.inputFields));

    for (String nextFieldName : this.prependFields) {
      if (indexSchema.getFieldOrNull(nextFieldName) != null) {
        if (indexSchema.getField(nextFieldName).getType() instanceof MultiTextField) {
          outputDocument = detectAndPrependLanguages(outputDocument,
              nextFieldName, documentLangs);
        } else {
          log.error("Invalid field " + PREPEND_FIELDS + ":" + nextFieldName
              + ". Field is not a " + MultiTextField.class + ".");
        }
      } else {
        log.error("Invalid field " + PREPEND_FIELDS + ":" + nextFieldName
            + ". Field does not exist in indexSchema.");

      }

    }

    return outputDocument;
  }

  protected SolrInputDocument detectAndPrependLanguages(SolrInputDocument doc,
      String multiTextFieldName, List<DetectedLanguage> documentLangs) {
    MultiTextField mtf = (MultiTextField) indexSchema
        .getFieldType(multiTextFieldName);
    MultiTextFieldAnalyzer mtfAnalyzer = (MultiTextFieldAnalyzer) mtf
        .getAnalyzer();

    List<DetectedLanguage> fieldLangs = null;
    if (this.prependGranularity == PrependGranularities.field
        || this.prependGranularity == PrependGranularities.fieldValue) {
      fieldLangs = this.detectLanguage(this.concatFields(doc,
          new String[] { multiTextFieldName }));
    }
    if (fieldLangs == null || fieldLangs.size() == 0) {
      fieldLangs = documentLangs;
    }

    SolrInputField inputField = doc.getField(multiTextFieldName);
    SolrInputField outputField = new SolrInputField(inputField.getName());
    if (inputField.getValues() != null) {
      for (final Object inputValue : inputField.getValues()) {
        Object outputValue = inputValue;

        List<DetectedLanguage> fieldValueLangs = null;
        if (this.prependGranularity == PrependGranularities.fieldValue) {
          if (inputValue instanceof String) {
            fieldValueLangs = this.detectLanguage(inputValue.toString());
          }
        }
        if (fieldValueLangs == null || fieldValueLangs.size() == 0) {
          fieldValueLangs = fieldLangs;
        }
        LinkedHashSet<String> langsToPrepend = new LinkedHashSet<String>();
        for (DetectedLanguage lang : fieldValueLangs) {
          langsToPrepend.add(lang.getLangCode());
        }

        StringBuilder fieldLangsPrefix = new StringBuilder();
        for (String lang : langsToPrepend) {
          if (mtfAnalyzer.Settings.ignoreMissingMappings
              || mtfAnalyzer.Settings.fieldMappings.containsKey(lang)
              || indexSchema.getFieldOrNull(lang) != null) {
            if (fieldLangsPrefix.length() > 0) {
              fieldLangsPrefix.append(mtfAnalyzer.Settings.multiKeyDelimiter);
            }
            fieldLangsPrefix.append(lang);

          }
        }
        if (fieldLangsPrefix.length() > 0) {
          fieldLangsPrefix.append(mtfAnalyzer.Settings.keyFromTextDelimiter);
        }

        if (this.hidePrependedLangs) {
          fieldLangsPrefix.insert(0, '[');
          fieldLangsPrefix.append(']');
        }

        outputValue = fieldLangsPrefix + (String) outputValue;
        outputField.addValue(outputValue, 1.0F);
      }
    }
    outputField.setBoost(inputField.getBoost());

    doc.removeField(multiTextFieldName);
    doc.put(multiTextFieldName, outputField);
    return doc;

  }

  /*
   * Concatenates content from multiple fields
   */
  protected String getCurrentFieldValue(SolrInputDocument doc, String fieldName) {
    StringBuffer sb = new StringBuffer();
    if (doc.containsKey(fieldName)) {
      Object content = doc.getFieldValue(fieldName);
      if (content instanceof String) {
        sb.append((String) doc.getFieldValue(fieldName));
        sb.append(" ");
      } else {
        log.warn("Field " + fieldName
            + " not a String value, not including in detection");
      }
    }
    return sb.toString();
  }

  //

}
