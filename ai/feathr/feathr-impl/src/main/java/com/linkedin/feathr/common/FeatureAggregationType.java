package com.linkedin.feathr.common;

/**
 * Feature aggregation types
 */
public enum FeatureAggregationType {
  UNION,
  SUM,
  AVG,
  MAX,
  MIN,
  ELEMENTWISE_MAX,
  ELEMENTWISE_MIN,
  ELEMENTWISE_AVG,
  ELEMENTWISE_SUM,
  FIRST
}
