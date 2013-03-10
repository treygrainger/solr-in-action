package com.sia.ch14;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer.ReuseStrategy;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;

/**
   * Implementation of {@link ReuseStrategy} that reuses components per-key, 
   * by maintaining a Map of TokenStreamComponent with a unique key.
   * This can be used as a wholesale replacement for PerFieldReuseStrategy,
   * which does the same thing but unnecessarily assumes the key is a field name.
   */
  public class KeyedReuseStrategy extends ReuseStrategy {

	    /**
	     * {@inheritDoc}
	     */
	    @SuppressWarnings("unchecked")
	    public TokenStreamComponents getReusableComponents(String key) {
	      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
	      return componentsPerField != null ? componentsPerField.get(key) : null;
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @SuppressWarnings("unchecked")
	    public void setReusableComponents(String key, TokenStreamComponents components) {
	      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
	      if (componentsPerField == null) {
	        componentsPerField = new HashMap<String, TokenStreamComponents>();
	        setStoredValue(componentsPerField);
	      }
	      componentsPerField.put(key, components);
	    }
	  }