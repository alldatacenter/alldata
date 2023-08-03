package com.linkedin.feathr.offline.join.algorithms


import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureJoinException}
import com.linkedin.feathr.offline.join.algorithms.JoinType.JoinType
import com.linkedin.feathr.offline.join.util.{FrequentItemEstimator, PreComputedFrequentItemEstimator}
import com.linkedin.feathr.offline.util.FeathrUtils
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.util.Random

/**
 * This join algorithm salts the join key to fight data skew for dataframe join
 * @param frequentItemEstimator the estimator used to estimate the frequent items
 */
private[offline] class SaltedSparkJoin(ss: SparkSession, frequentItemEstimator: FrequentItemEstimator) extends SparkJoin {
  val supportedJoinTypes = Seq(JoinType.inner, JoinType.left_outer)
  val REPLICATION_FACTOR_LOW = 1 // duplicate factor for those record with frequency of at most 0.01%

  /**
   * Join the left (skewed data) and right (uniform data) on column joinColumn
   * This is a special version of join that aims to improve cases where data is skewed on left table
   * assuming skewedDf is skewed on join key column
   * and uniformDf is uniformly distributed on join key column
   * @param leftJoinColumns join column name for left DataFrame (for salted join it is same as rightColumnName).
   * @param leftDF left dataframe to join
   * @param rightJoinColumns join column name for right DataFrame (for salted join it is same as rightColumnName).
   * @param rightDF right dataframe to join
   * @param joinType join type, only "inner" and "left_outer" are supported
   * @return joined dataframe
   */
  override def join(leftJoinColumns: Seq[String], leftDF: DataFrame, rightJoinColumns: Seq[String], rightDF: DataFrame, joinType: JoinType): DataFrame = {
    if (!supportedJoinTypes.contains(joinType)) {
      throw new FeathrFeatureJoinException(
        ErrorLabel.FEATHR_ERROR,
        s"Salted join does not support join type ${joinType}, supported types are: ${supportedJoinTypes}")
    }

    if (leftJoinColumns.size != 1 || rightJoinColumns.size != 1) {
      throw new FeathrFeatureJoinException(
        ErrorLabel.FEATHR_ERROR,
        "In salted join, there should be only one " +
          s"join columns, but found left: [${leftJoinColumns.mkString(", ")}], right: [${rightJoinColumns.mkString(", ")}]")
    }
    // Drop duplicates in right DataFrame
    val left = leftDF
    val right = rightDF.dropDuplicates(rightJoinColumns).withColumnRenamed(rightJoinColumns.head, leftJoinColumns.head)
    val joinColumn = leftJoinColumns.head
    val conf = ss.sparkContext.getConf
    // From high-level, it add random number (the salt) to keys that appear in many rows in the original dataframe.
    // So that they can be distributed to multiple workers.
    val persist = FeathrUtils.getFeathrJobParam(conf, FeathrUtils.SALTED_JOIN_PERSIST).toBoolean &&
      !frequentItemEstimator.isInstanceOf[PreComputedFrequentItemEstimator]
    if (persist) {
      // We need to cache the dataframe before calculate the frequent items, to qvoid calculate it twice.
      left.persist(StorageLevel.MEMORY_AND_DISK)
    }

    // 1. collect the frequent join keys in the left, i.e. skewed join keys, e.g. hot ids
    // frequent Id list as a dataframe
    val freqThreshold = FeathrUtils.getFeathrJobParam(conf, FeathrUtils.SALTED_JOIN_FREQ_ITEM_THRESHOLD).toFloat
    val freqKeyDf = SaltedSparkJoin.getFrequentItemsDataFrame(frequentItemEstimator, left, joinColumn, freqThreshold)
    // The freqKeyDf always has only one key column. Multi keys column are combined to one before calling salted join.
    val freqKeySet = freqKeyDf
      .select(expr(joinColumn).cast(StringType).as(joinColumn))
      .collect()
      .map(_.getString(0))
      .toSet

    SaltedSparkJoin.log.info("Salted Join: applying salted join for join column:" + joinColumn)

    // 2. create a new join key column with the 'salt'
    //    2.1 for the right dataframe, we just duplicate each row multiple times, during each time, we append
    //         a sequence number to it which will be used as part of the new join key later
    val newJoinKeyColumn = "new_join_key_" + joinColumn
    val replicationColumn = "replicate_id"
    val newJoinKeyExpr = concat_ws("#", expr(joinColumn), expr(replicationColumn))
    // generate the sequence number dataframe as a helper, to improve performance, for frequent rows,
    // we duplicate them more times than the non-frequent keys
    // assume there're 10 billion records, we have 10k records that with 0.01% frequency,
    // i.e. each record is duplicated at least 10b*0.01% = 1M times, up to 10 billion times
    // if replicationFactorHigh is 10, at most, we add 10 * 10000 = 200k records of the feature dataset
    val replicationFactorHigh = FeathrUtils.getFeathrJobParam(conf, FeathrUtils.SALTED_JOIN_REPLICATION_FACTOR_HIGH).toInt

    val addReplicationUdf = createAddReplicationUdf(freqKeySet, replicationFactorHigh, REPLICATION_FACTOR_LOW)
    val rightWithSalt = right
      .withColumn(replicationColumn, explode(addReplicationUdf(expr(joinColumn))))
      .withColumn(newJoinKeyColumn, newJoinKeyExpr)
      .drop(replicationColumn)
      .drop(joinColumn)

    //   2.2 for the left dataframe, we append a random sequence number(from 0 to max sequence number added to the right dataframe) to each row
    val addSaltUdf = createAddSaltUdf(freqKeySet, replicationFactorHigh, REPLICATION_FACTOR_LOW)
    val leftWithSalt = left.withColumn(newJoinKeyColumn, addSaltUdf(expr(joinColumn)))

    // 3. join the left and right dataframes.
    leftWithSalt.join(rightWithSalt, Seq(newJoinKeyColumn), joinType.toString).drop(newJoinKeyColumn)
  }

  /**
   * create a Spark UDF to add a column with an array with replication numbers.
   * @param freqKeySet the frequent item set
   * @param replicationFactorHigh how many times the frequent item should be replicated
   * @param replicationFactorLow how many times the infrequent item should be replicated
   * @return UDF to add the replication array
   */
  private def createAddReplicationUdf(freqKeySet: Set[String], replicationFactorHigh: Int, replicationFactorLow: Int): UserDefinedFunction = {
    val replicationHigh = (0 until replicationFactorHigh).toArray
    val replicationLow = (0 until replicationFactorLow).toArray
    udf((x: String) => if (freqKeySet.contains(x)) replicationHigh else replicationLow)
  }

  /**
   * create a Spark UDF to add a column with the salted key
   * @param freqKeySet the frequent item set
   * @param replicationFactorHigh how many times the frequent item should be replicated
   * @param replicationFactorLow how many times the infrequent item should be replicated
   * @return UDF to add the salted key
   */
  private def createAddSaltUdf(freqKeySet: Set[String], replicationFactorHigh: Int, replicationFactorLow: Int): UserDefinedFunction = {
    udf(
      (x: String) =>
        if (freqKeySet.contains(x)) x + "#" + Random.nextInt(replicationFactorHigh)
        else x + "#" + Random.nextInt(replicationFactorLow))
  }
}

object SaltedSparkJoin {
  @transient private lazy val log = LogManager.getLogger(getClass.getName)

  /**
   * get the frequentItems. If it's cached, just return the cached dataframe. Otherwise calculate using the estimator.
   * @param inputDf dataframe that you want to estimate the frequent items
   * @param targetColumn target column name to compute the frequent items
   * @param freqThreshold define how often the items need to be so that they can be treated as 'frequent items'
   * @return dataframe that contains all estimate frequent items, one item per row. The column name is the same as
   *         the input target column name
   * @return
   */
  def getFrequentItemsDataFrame(frequentItemEstimator: FrequentItemEstimator, inputDf: DataFrame, targetColumn: String, freqThreshold: Float): DataFrame = {
    val startTime = System.currentTimeMillis()
    log.info("Salted Join: calculate frequent items for salted join: " + targetColumn)
    val freqItems = frequentItemEstimator.estimateFrequentItems(inputDf, targetColumn, freqThreshold)
    log.info("Salted Join frequent item calculation time: " + (System.currentTimeMillis() - startTime))
    log.info("Salted Join: frequent item count:" + freqItems.count())
    freqItems
  }

  /**
   * This class is used to pass the parameters for salted join.
   * @param estimator estimator for frequent items.
   * @param frequentItemThreshold define how often the items need to be so that they can be treated as 'frequent items'
   */
  case class JoinParameters(estimator: FrequentItemEstimator, frequentItemThreshold: Float)
}
