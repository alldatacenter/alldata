package com.linkedin.feathr.core.config.producer.definitions;

/**
 * Specifies the feature type of a feature.
 * This is the same as the FeatureTypes in frame-common.
 */
public enum FeatureType {
  BOOLEAN,
  NUMERIC,
  CATEGORICAL,
  CATEGORICAL_SET,
  TERM_VECTOR,
  VECTOR,
  DENSE_VECTOR,
  TENSOR,
  UNSPECIFIED,
  DENSE_TENSOR,
  SPARSE_TENSOR,
  RAGGED_TENSOR
}
