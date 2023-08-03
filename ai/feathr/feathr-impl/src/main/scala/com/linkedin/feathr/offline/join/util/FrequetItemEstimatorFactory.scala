package com.linkedin.feathr.offline.join.util

import com.linkedin.feathr.offline.join.util.FrequentItemEstimatorType.FrequentItemEstimatorType
import org.apache.spark.sql.DataFrame

private[offline] object FrequentItemEstimatorFactory {

  /**
   * create an estimator from a type string.
   * @param estimatorType the estimator type. Valid values are: spark, countMinSketch, groupAndCount
   * @return a frequent item estimator
   */
  def create(estimatorType: FrequentItemEstimatorType): FrequentItemEstimator = {
    estimatorType match {
      case FrequentItemEstimatorType.spark => new SparkFrequentItemEstimator
      case FrequentItemEstimatorType.countMinSketch => new CountMinSketchFrequentItemEstimator
      case FrequentItemEstimatorType.groupAndCount => new GroupAndCountFrequentItemEstimator
      case _ => throw new IllegalArgumentException(s"invalid estimator type: ${estimatorType.toString}.")
    }
  }

  /**
   * Create an estimator from a pre-computed cache.
   * @param frequentItems a pre-computed dataframe of frequent items.
   * @return a frequent item estimator
   */
  def createFromCache(frequentItems: DataFrame): FrequentItemEstimator = {
    new PreComputedFrequentItemEstimator(frequentItems)
  }
}
