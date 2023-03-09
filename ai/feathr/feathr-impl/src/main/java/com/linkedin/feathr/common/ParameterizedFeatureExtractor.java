package com.linkedin.feathr.common;

import java.util.Map;


/**
 * This extension of the FeatureExtractor rely on extra parameters to extract features.  The expectation is that the
 * Feathr engine will call `init` when constructing an instance of this Extractor and implementation of this interface
 * is responsible for maintaining any state for the parameters as needed.
 */
public interface ParameterizedFeatureExtractor<SOURCE_DATA, PARAM> extends FeatureExtractor<SOURCE_DATA> {
  /**
   * Initialize the extractor params.
   * @param params It's a map from feature name to PARAM.
   */
  void init(Map<String, PARAM> params);
}
