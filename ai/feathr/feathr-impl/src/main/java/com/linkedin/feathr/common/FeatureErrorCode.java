package com.linkedin.feathr.common;

/**
 * Error code associated with a feature request
 */
public enum FeatureErrorCode {
  FEATURE_DEFINITION_ERROR, // Feature definition errors
  FEATURE_REQUEST_ERROR, // Errors associated with the feature request
  FEATURE_SOURCE_ERROR, // Errors from feature source during fetch/join
  FEATURE_COMPUTATION_ERROR, // Errors during feature computation such as extraction and derivation
  INTERNAL_ERROR, // Errors internal to Feathr
  UNKNOWN; // unknown error
}
