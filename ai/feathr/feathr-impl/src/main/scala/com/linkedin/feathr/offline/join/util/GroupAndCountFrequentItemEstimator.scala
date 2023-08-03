package com.linkedin.feathr.offline.join.util

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

/**
 *  This estimator do a group-by and exact count to return accurate frequent items.
 */
private[offline] class GroupAndCountFrequentItemEstimator extends FrequentItemEstimator {

  /**
   * count the frequency of each item and return the most frequent ones.
   *
   * @param inputDf       dataframe that you want to estimate the frequent items
   * @param targetColumn  target column name to compute the frequent items
   * @param freqThreshold define how often the items need to be so that they can be treated as 'frequent items',
   *                      value ranges from 0 to 1
   * @return dataframe that contains all estimate frequent items, one item per row. The column name is the same as
   *         the input target column name
   */
  override def estimateFrequentItems(inputDf: DataFrame, targetColumn: String, freqThreshold: Float): DataFrame = {
    val internalCountColumnName = "_feathr_estimate_count"
    val totalCount = inputDf.count
    val threshold = (totalCount * freqThreshold).toInt
    val result = inputDf
      .groupBy(targetColumn)
      .agg(count(lit(1)).alias(internalCountColumnName))
      .filter(col(internalCountColumnName) > threshold)
      .select(targetColumn)
    result.cache().count
    result
  }
}
