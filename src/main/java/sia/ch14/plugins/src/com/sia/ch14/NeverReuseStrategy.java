package com.sia.ch14;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.ReuseStrategy;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;

/**
   * Implementation of {@link ReuseStrategy} that never caches anything, 
   * causing TokenStreamComponents to be re-created on every call.
   * Note that this will hurt performance by excessive object creation
   * if you don't cache elsewhere.  Because the 
   * {@link Analyzer#tokenStream(String, Reader)} is declared "final", 
   * it is necessary to cache outside of the Analyzer's ReuseStrategy if
   * you need additional cache keys besides just the field name.
   */
  public class NeverReuseStrategy extends ReuseStrategy {
	    /**
	     * {@inheritDoc}
	     */
	    @SuppressWarnings("unchecked")
	    public TokenStreamComponents getReusableComponents(String key) {
	      return null; //nothing cached
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @SuppressWarnings("unchecked")
	    public void setReusableComponents(String key, TokenStreamComponents components) {
	      //No need to cache anything
	    }
	  }