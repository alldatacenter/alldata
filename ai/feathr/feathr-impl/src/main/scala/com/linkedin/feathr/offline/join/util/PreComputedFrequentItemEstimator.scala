package com.linkedin.feathr.offline.join.util

import org.apache.spark.sql.DataFrame

/**
 *  An estimator that return the pre-calculated value.
 */
private[offline] class PreComputedFrequentItemEstimator(frequentItems: DataFrame) extends FrequentItemEstimator {

  /**
   * Just return the pre-computed dataframe
   *
   * @param inputDf       dataframe that you want to estimate the frequent items
   * @param targetColumn  target column name to compute the frequent items
   * @param freqThreshold define how often the items need to be so that they can be treated as 'frequent items',
   *                      value ranges from 0 to 1
   * @return dataframe that contains all estimate frequent items, one item per row. The column name is the same as
   *         the input target column name
   */
  override def estimateFrequentItems(inputDf: DataFrame, targetColumn: String, freqThreshold: Float): DataFrame = {
    frequentItems
  }
}
