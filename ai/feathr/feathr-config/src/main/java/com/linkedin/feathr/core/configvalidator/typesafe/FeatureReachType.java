package com.linkedin.feathr.core.configvalidator.typesafe;

/**
 * Enum for feature reachable.
 * A feature is reachable if and only if the feature is defined in anchors section, or
 *   its depend features (a.k.a input features or base features) are all reachable.
 */
enum FeatureReachType {
  UNREACHABLE,
  REACHABLE
}
