package com.linkedin.feathr.offline.transformation

private[offline] object FeatureColumnFormat extends Enumeration {
  type FeatureColumnFormat = Value
  // raw format, produced by sql expression, the feature uses the datatype returned by the sql expression directly

  val RAW, // raw format
  FDS_TENSOR = Value // Tensor defined by Featurized Dataset format
}
