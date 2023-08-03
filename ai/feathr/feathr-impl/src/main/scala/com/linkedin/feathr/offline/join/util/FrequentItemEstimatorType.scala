package com.linkedin.feathr.offline.join.util

/**
 * types of frequent items we support
 */
private[offline] object FrequentItemEstimatorType extends Enumeration {
  type FrequentItemEstimatorType = Value
  val spark, countMinSketch, groupAndCount, preComputed = Value
}
