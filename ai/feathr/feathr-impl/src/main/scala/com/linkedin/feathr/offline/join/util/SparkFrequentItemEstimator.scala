package com.linkedin.feathr.offline.join.util

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{explode, expr}

/**
 * This implementation uses Spark internal frequent item estimator to estimate the frequent items in a dataframe
 */
private[offline] class SparkFrequentItemEstimator extends FrequentItemEstimator {

  /**
   * Efficiently estimate frequent items from the inputDf, false positive are possible, not true negative is not,
   * which means if may return items that are not frequent, but will never fail to return items that are frequent.
   *
   * e.g. for a daframe:
   *
   * key  label
   * 1     L1
   * 1     L2
   * 2     L3
   *
   * if the target column is 'key' and freqThreshold(support) is 0.5, then it may return a dataframe (exact result):
   *
   * key
   * 1
   *
   * or another dataframe (with false positive item '2'):
   * key
   * 1
   * 2
   *
   * The current implementation is using a Spark function, which is based on a research paper, see more details in the implementation
   *
   * @param inputDf       dataframe that you want to estimate the frequent items
   * @param targetColumn  target column name to compute the frequent items
   * @param freqThreshold define how often the items need to be so that they can be treated as 'frequent items',
   *                      value ranges from 0 to 1
   * @return dataframe that contains all estimate frequent items, one item per row. The column name is the same as
   *         the input target column name
   */
  override def estimateFrequentItems(inputDf: DataFrame, targetColumn: String, freqThreshold: Float): DataFrame = {
    val resultColumnName = targetColumn + "_freqItems"

    // we delegate to third party Spark's default implementation for now
    // we may try other ways to estimate the frequent items later
    val freqNewJoinKeyColumnExpr = explode(expr(resultColumnName)).as(targetColumn)
    inputDf.stat.freqItems(Seq(targetColumn), freqThreshold).select(freqNewJoinKeyColumnExpr)
  }
}
