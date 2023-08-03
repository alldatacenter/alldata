package com.linkedin.feathr.common;

/**
 * An Enum that defines the supported feature types in feathr. This list will be extended in the future.
 */
public enum FeatureTypes {
  BOOLEAN,
  NUMERIC,
  CATEGORICAL,
  DENSE_VECTOR,
  TERM_VECTOR,
  CATEGORICAL_SET,
  UNSPECIFIED,
  TENSOR
}
