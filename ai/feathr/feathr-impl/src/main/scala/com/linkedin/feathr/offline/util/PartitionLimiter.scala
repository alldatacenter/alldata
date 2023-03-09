package com.linkedin.feathr.offline.util

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * repartition the DataFrame or RDD if the number of partitions is too high or too low.
 * @param ss spark session
 */
private[offline] class PartitionLimiter(ss: SparkSession) {

  // If spark.default.parallelism is not set, this value will be used as the default and used
  // to cap numPartitions later; otherwise, the spark.default.paralleism value set by user will be used
  val SPARK_JOIN_PARALLELISM_DEFAULT = 5000
  // Internal parameter (We don't expect user to change it) as an empirical factor 'threshold' to control whether limit the partition or not
  val SPARK_JOIN_LIMIT_PARTITION_FACTOR = 2

  /**
   * limitPartition is only for 'soft' adjust partition numbers after loading the feature source data.
   * It is 'soft' limit, as we don't want to repartition dataset with partition number barely exceeds the threshold, e.g.
   * repartition from 100001 to 10000(repartition is expensive).
   * We added this since in some cases, the default input source data might be too small or too big which may decrease
   * the processing efficiency or even fail the execution.
   * It will not adjust partition for local run for improve performance
   *
   * @param df                                dataframe to limit the number of partitions
   * @param maxSparkJoinParallelismBeforeJoin max partition number
   * @param minSparkJoinParallelismBeforeJoin min partition number, set to -1 will not adjust data partition, e.g. user can set this in azkaban
   *                                          to turn this 'adjust' off.
   * @return input DataFrame with (possibly) its number of partitions adjusted
   */
  def limitPartition(df: DataFrame, maxSparkJoinParallelismBeforeJoin: Int, minSparkJoinParallelismBeforeJoin: Int): DataFrame = {
    // if df has more than SPARK_JOIN_LIMIT_PARTITION_FACTOR * maxSparkJoinParallelismBeforeJoin partition, reduce to maxSparkJoinParallelismBeforeJoin
    // if df has less than minSparkJoinParallelismBeforeJoin partition, repartition to minSparkJoiMath.max(SPARK_JOIN_PARALLELISM_DEFAULT.toInt,
    // minSparkJoinParallelismBeforeJoin)
    // if maxSparkJoinParallelismBeforeJoin == minSparkJoinParallelismBeforeJoin, will force repartition to minSparkJoinParallelismBeforeJoin
    val numParts = df.rdd.getNumPartitions
    val isLocal = ss.sparkContext.isLocal
    if (minSparkJoinParallelismBeforeJoin < 0 || isLocal) {
      df
    } else if (maxSparkJoinParallelismBeforeJoin == minSparkJoinParallelismBeforeJoin && numParts != maxSparkJoinParallelismBeforeJoin) {
      df.repartition(maxSparkJoinParallelismBeforeJoin)
    } else if (numParts > maxSparkJoinParallelismBeforeJoin * SPARK_JOIN_LIMIT_PARTITION_FACTOR) {
      df.coalesce(maxSparkJoinParallelismBeforeJoin)
    } else if (numParts < minSparkJoinParallelismBeforeJoin / SPARK_JOIN_LIMIT_PARTITION_FACTOR) {
      df.repartition(Math.max(SPARK_JOIN_PARALLELISM_DEFAULT, minSparkJoinParallelismBeforeJoin))
    } else {
      df
    }
  }

  /**
   * limitPartition is only for 'soft' adjust partition numbers after loading the feature source data.
   * It is 'soft' limit, as we don't want to repartition dataset with partition number barely exceeds the threshold, e.g.
   * repartition from 100001 to 10000(repartition is expensive).
   * We added this since in some cases, the default input source data might be too small or too big which may decrease
   * the processing efficiency or even fail the execution.
   * It will not adjust partition for local run for improve performance
   *
   * @param df                                dataframe to limit the number of partitions
   * @return input DataFrame with (possibly) its number of partitions adjusted
   */
  def limitPartition(df: DataFrame): DataFrame = {
    // if df has more than SPARK_JOIN_LIMIT_PARTITION_FACTOR * maxSparkJoinParallelismBeforeJoin partition, reduce to maxSparkJoinParallelismBeforeJoin
    // if df has less than minSparkJoinParallelismBeforeJoin partition, repartition to minSparkJoiMath.max(SPARK_JOIN_PARALLELISM_DEFAULT.toInt,
    // minSparkJoinParallelismBeforeJoin)
    // if maxSparkJoinParallelismBeforeJoin == minSparkJoinParallelismBeforeJoin, will force repartition to minSparkJoinParallelismBeforeJoin
    val maxSparkJoinParallelismBeforeJoin = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.SPARK_JOIN_MAX_PARALLELISM).toInt
    val minSparkJoinParallelismBeforeJoin = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.SPARK_JOIN_MIN_PARALLELISM).toInt
    limitPartition(df, maxSparkJoinParallelismBeforeJoin, minSparkJoinParallelismBeforeJoin)
  }

    /**
   * limitPartition is only for 'soft' adjust partition numbers after loading the feature source data.
   * It is 'soft' limit, as we don't want to repartition dataset with partition number barely exceeds the threshold, e.g.
   * repartition from 100001 to 10000(repartition is expensive).
   * We added this since in some cases, the default input source data might be too small or too big which may decrease
   * the processing efficiency or even fail the execution.
   * It will not adjust partition for local run to improve performance
   *
   * @param rdd                               rdd to limit the number of partitions
   * @param maxSparkJoinParallelismBeforeJoin max partition number
   * @param minSparkJoinParallelismBeforeJoin min partition number, set to -1 will not adjust data partition, e.g. user can set this in azkaban
   *                                          to turn this 'adjust' off.
   * @return input rdd with (possibly) its number of partitions adjusted
   */
  def limitPartition[T](
    rdd: RDD[T],
    maxSparkJoinParallelismBeforeJoin: Int,
    minSparkJoinParallelismBeforeJoin: Int): RDD[T] = {
    // if rdd has more than SPARK_JOIN_LIMIT_PARTITION_FACTOR * maxSparkJoinParallelismBeforeJoin partition, reduce to maxSparkJoinParallelismBeforeJoin
    // if rdd has less than minSparkJoinParallelismBeforeJoin / SPARK_JOIN_LIMIT_PARTITION_FACTOR partition, repartition to minSparkJoinParallelismBeforeJoin
    // if maxSparkJoinParallelismBeforeJoin == minSparkJoinParallelismBeforeJoin, will force repartition to minSparkJoinParallelismBeforeJoin
    // if minSparkJoinParallelismBeforeJoin < 0 will return skip the partition adjustment
    val numParts = rdd.getNumPartitions
    val isLocal = ss.sparkContext.isLocal
    if (minSparkJoinParallelismBeforeJoin < 0 || isLocal) {
      rdd
    } else if (maxSparkJoinParallelismBeforeJoin == minSparkJoinParallelismBeforeJoin && numParts != maxSparkJoinParallelismBeforeJoin) {
      rdd.repartition(maxSparkJoinParallelismBeforeJoin)
    } else if (rdd.getNumPartitions > maxSparkJoinParallelismBeforeJoin * SPARK_JOIN_LIMIT_PARTITION_FACTOR) {
      rdd.coalesce(maxSparkJoinParallelismBeforeJoin)
    } else if (rdd.getNumPartitions < minSparkJoinParallelismBeforeJoin / SPARK_JOIN_LIMIT_PARTITION_FACTOR) {
      rdd.repartition(minSparkJoinParallelismBeforeJoin)
    } else {
      rdd
    }
  }
}
