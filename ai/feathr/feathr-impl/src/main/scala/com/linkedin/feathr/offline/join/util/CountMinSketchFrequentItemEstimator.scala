package com.linkedin.feathr.offline.join.util

import org.apache.spark.sql.DataFrame

// This implementation uses a count min sketch to estimate the count of frequent items
// see https://en.wikipedia.org/wiki/Count%E2%80%93min_sketch
// and http://dimacs.rutgers.edu/~graham/pubs/papers/cm-full.pdf for more details on count min sketch
private[offline] class CountMinSketchFrequentItemEstimator extends FrequentItemEstimator {

  // Empirical parameters for countMinSketch
  // Relative error of the sketch
  private val eps = 0.01
  // The confidence level we have in the accuracy of the estimate
  // If this turns out not enough in future cases, try to increase these value (must less than 1.0)
  private val confidence = 0.95
  // The seed used to generate random numbers during the estimate
  private val seed = 7
  // with these values, here are the internal impact:
  // width = Math.ceil(2 / eps).toInt = 200
  // depth = Math.ceil(-Math.log(1 - confidence) / Math.log(2)).toInt = 5
  // Since we are using 'long' type for each counter, the memory usage of this sketch would be :
  //  8 * 200 * 5 = 8k
  // see some info here: https://redislabs.com/blog/count-min-sketch-the-art-and-science-of-estimating-stuff/

  /**
   * Get frequent items from the inputDf, the result is accurate, performance may not be as good as other estimator
   * which means if may return items that are not frequent, but will never fail to return items that are frequent.
   *
   * e.g. for a daframe:
   *
   * key  label
   * 1     L1
   * 1     L2
   * 2     L3
   *
   * if the target column is 'key' and freqThreshold(support) is 0.5, then it will return a dataframe (exact result):
   *
   * key
   * 1
   *
   * @param inputDf       dataframe that you want to estimate the frequent items
   * @param targetColumn  target column name to compute the frequent items
   * @param freqThreshold define how often the items need to be so that they can be treated as 'frequent items',
   *                      value ranges from 0 to 1
   * @return dataframe that contains all frequent items, one item per row. The column name is the same as
   *         the input target column name
   */
  override def estimateFrequentItems(inputDf: DataFrame, targetColumn: String, freqThreshold: Float): DataFrame = {
    import org.apache.spark.sql.functions._
    val minSketch = inputDf.stat.countMinSketch(targetColumn, eps, confidence, seed)
    val countUDF = udf((item: Any) => minSketch.estimateCount(item))
    val internalCountColumnName = "_feathr_estimate_count"
    val totalCount = inputDf.count
    val freqDf = inputDf
      .withColumn(internalCountColumnName, countUDF(expr(targetColumn)))
      .filter(expr(s"${internalCountColumnName}*1.0/${totalCount} > ${freqThreshold}"))
      .select(targetColumn)
      .distinct()
    freqDf
  }
}
