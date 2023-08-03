package com.linkedin.feathr.offline.join

import com.linkedin.feathr.offline.client._
import com.linkedin.feathr.offline.config.FeatureJoinConfig
import com.linkedin.feathr.offline.job.DataFrameStatFunctions
import com.linkedin.feathr.offline.join.algorithms.SaltedSparkJoin
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import com.linkedin.feathr.offline.util.SourceUtils
import com.linkedin.feathr.offline.util.datetime.DateTimeInterval
import com.linkedin.feathr.offline.{FeatureName, JoinStage, KeyTagIdTuple}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.functions.monotonically_increasing_id
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.util.sketch.BloomFilter

import scala.collection.mutable

/**
 *
 * @param bloomFilters bloomfilters built for each join stage
 * @param keyAndUidOnlyDF trimmed observation for slick join algorithm, which has only the necessary columns to join the feature dataset
 * @param withUidDF original observation data with a unique id column appended, used to join back the observation/context fields after feature join
 * @param swaObsTime the date time range of the observation data
 * @param extraColumnsInSlickJoin Extra columns that are kept in the trimmed observation dataset(i.e. keyAndUidOnlyDF), will needs to be removed
 *                          before the last join with withUidDF. Mostly are column related to sliding window aggregation.
 * @param saltedJoinFrequentItemDFs a map from key tags to pre-calculated frequent items used by salted join.
 */
private[offline] case class PreprocessedObservation(
    bloomFilters: Option[Map[Seq[Int], BloomFilter]],
    keyAndUidOnlyDF: DataFrame,
    withUidDF: DataFrame,
    swaObsTime: Option[DateTimeInterval],
    extraColumnsInSlickJoin: Seq[String] = Seq.empty[String],
    saltedJoinFrequentItemDFs: Map[Seq[Int], DataFrame])

// utils for the join optimizers
private[offline] object OptimizerUtils {

  /*
   * extimateSize will passed to create bloomfilter
   * where numBitsOfBloomfilter with 3% FP(false positive) rate will be (-n * Math.log(p) / (Math.log(2) * Math.log(2)))
   * hence, 100M items will have a 200MB bloomfilter
   * if more than 100M, we will sacrifice FP rate, e.g, just set estimate to 100m,
   */
  val maxExpectedItemForBloomfilter = 50000000L

  // The false-positive rate of BloomFilters.
  val bloomFilterFPP = 0.05

  @transient private lazy val log = LogManager.getLogger(getClass.getName)

  /**
   * preprocess observation data. i.g. generate bloomfilters and frequent items, trim observation for slick join algorithm
   *
   * @param inputDF observation data
   * @param rowBloomFilterThreshold threshold to generate bloom filters for all key combinations of features.
   * @param saltedJoinParameters parameters generating frequent items.
   *                             No frequent items will be generated if this parameter is None.
   * @param joinStages all join stages
   * @param keyTagList all key tag list
   * @param columnsToPreserve observation data columns that must be preserved
   * @return preprocessed observation data
   */
  def preProcessObservation(
      inputDF: DataFrame,
      joinConfig: FeatureJoinConfig,
      joinStages: Seq[JoinStage],
      keyTagList: Seq[String],
      rowBloomFilterThreshold: Option[Int],
      saltedJoinParameters: Option[SaltedSparkJoin.JoinParameters],
      columnsToPreserve: Seq[String]): PreprocessedObservation = {

    val (swaObsTime, obsSWATimeExpr) = SlidingWindowFeatureUtils.getObsSwaDataTimeRange(inputDF, joinConfig.settings)
    val extraColumnsInSlickJoin = getSlidingWindowRelatedColumns(obsSWATimeExpr, joinStages, keyTagList)

    val keyTagsToBloomFilterColumnMap = new mutable.HashMap[Seq[Int], String]
    val (_, withKeyDF, joinKeyColumnNames) = joinStages.foldLeft((keyTagsToBloomFilterColumnMap, inputDF, Seq.empty[String]))((accFilterMapDF, joinStage) => {
      val keyTags: Seq[Int] = joinStage._1
      val stringKeyTagList = keyTags.map(keyTagList)
      val accFilterMap = accFilterMapDF._1
      val df = accFilterMapDF._2
      // Spark-provided bloom filters could only operate on single column with primitive types.
      // To make it work for multi-key features, we first generate a new column that is the concatenation of
      // all key columns (DataFrameKeyCombiner.combine()).
      val (bfKeyColName, contextDFWithKeys) = DataFrameKeyCombiner().combine(df, stringKeyTagList)
      accFilterMap.put(keyTags, bfKeyColName)
      (accFilterMap, contextDFWithKeys, accFilterMapDF._3 ++ Seq(bfKeyColName))
    })
    val origJoinKeyColumns = keyTagsToBloomFilterColumnMap.map(_._2).toSeq

    val extraColumns = Seq(DataFrameColName.UidColumnName) ++ extraColumnsInSlickJoin
    val withKeyAndUidDF = withKeyDF.withColumn(DataFrameColName.UidColumnName, monotonically_increasing_id)
    val allSelectedColumns = (extraColumns ++ origJoinKeyColumns ++ columnsToPreserve).distinct
    // trims the observation for slick join, after trimmed, the observation will only have the join keys, some sliding
    // window aggregation time and join key columns
    // later use it to join the features, and finally join back other fields of the original observation data.
    // This reduces the shuffle size during the feature join, as the observation/context data will be much smaller during the feature join
    val keyAndUidOnlyDF = withKeyAndUidDF.select(allSelectedColumns.head, allSelectedColumns.tail: _*)

    val bloomFilters = generateBloomFilters(rowBloomFilterThreshold, withKeyAndUidDF, joinStages, origJoinKeyColumns, keyTagsToBloomFilterColumnMap)
    val withUidDF = withKeyAndUidDF.drop(joinKeyColumnNames: _*)

    val saltedJoinFrequentItemDFs = generateSaltedJoinFrequentItems(keyTagsToBloomFilterColumnMap.toMap, withKeyDF, saltedJoinParameters)

    PreprocessedObservation(bloomFilters, keyAndUidOnlyDF, withUidDF, swaObsTime, extraColumnsInSlickJoin, saltedJoinFrequentItemDFs)
  }

  /**
   * Generate bloomFilters
   * @param rowBloomFilterThreshold thresholds to generate bloomFilters
   * @param contextDF observation dataframe
   * @param joinStages all join stages
   * @param origJoinKeyColumns join keys columns to generate bloomfilters, one bloomfilter for each join key column
   * @param keyTagsToBloomFilterColumnMap map from key tags to the name to build bloomfilter on
   * @return bloomFilters
   */
  private def generateBloomFilters(
      rowBloomFilterThreshold: Option[Int],
      contextDF: DataFrame,
      joinStages: Seq[(KeyTagIdTuple, Seq[FeatureName])],
      origJoinKeyColumns: Seq[String],
      keyTagsToBloomFilterColumnMap: mutable.HashMap[Seq[Int], String]): Option[Map[Seq[Int], BloomFilter]] = {

    // Join window aggregation features first, separately from all other features.
    val estimatedSize = math.min(SourceUtils.estimateRDDRow(contextDF.rdd), maxExpectedItemForBloomfilter)
    val forceDisable = (rowBloomFilterThreshold.isDefined && rowBloomFilterThreshold.get == 0)

    val generateBloomfilters = if (estimatedSize <= 0) {
      true
    } else {
      val thresholdMet = if (rowBloomFilterThreshold.isEmpty) {
        true
      } else {
        val threshold = rowBloomFilterThreshold.get
        // -1 means force enable
        threshold == -1 || (threshold > 0 && estimatedSize < threshold)
      }
      if (thresholdMet && joinStages.nonEmpty) {
        true
      } else {
        false
      }
    }

    val bloomFilters = if (!forceDisable && generateBloomfilters) {
      // When the estimate is negative which means estimate failed to get a number within acceptable confidence,
      // in this case, we generate bloomfilter with maximum expect item in Feathr set up
      val expectItemNum = if (estimatedSize > 0) estimatedSize else maxExpectedItemForBloomfilter
      val filters = new DataFrameStatFunctions(contextDF).batchCreateBloomFilter(origJoinKeyColumns, expectItemNum, bloomFilterFPP)
      // use map value
      val filterMap = keyTagsToBloomFilterColumnMap.toSeq
        .zip(filters)
        .map {
          case ((tag, _), filter) =>
            (tag, filter)
        }
        .toMap
      Some(filterMap)
    } else None
    bloomFilters
  }

  /**
   * Get the columns in the observation dataframe that are required to do sliding window aggregation join
   * @param obsSWATimeExpr timestamp expression for SWA
   * @param joinStages all join stags
   * @param keyTagList all key tag list
   * @return
   */
  private def getSlidingWindowRelatedColumns(
      obsSWATimeExpr: Option[String],
      joinStages: Seq[(KeyTagIdTuple, Seq[FeatureName])],
      keyTagList: Seq[String]): Seq[String] = {

    // Get the top level fields referenced in a sql expression
    def getTopLevelReferencedFields(sqlExpr: String, ss: SparkSession) = {
      ss.sessionState.sqlParser.parseExpression(sqlExpr).references.map(_.name.split("\\.").head).toSeq
    }

    obsSWATimeExpr
      .map(timeExpr => {
        val ss: SparkSession = SparkSession.builder().getOrCreate()
        val swaTimeColumns = try {
          // Keep the top level field, e.g. for a map field "foo", expression "foo.bar" refers to a key "bar" in the field "foo"
          getTopLevelReferencedFields(timeExpr, ss)
        } catch {
          case _: Exception =>
            Seq.empty[String]
        }
        val swaJoinKeyColumns = try {
          joinStages.flatMap { joinStage =>
            val keyTags: Seq[Int] = joinStage._1
            keyTags.flatMap(keyTagId => {
              getTopLevelReferencedFields(keyTagList(keyTagId), ss)
            })
          }
        } catch {
          case _: Exception =>
            Seq.empty[String]
        }
        (swaTimeColumns ++ swaJoinKeyColumns).distinct
      })
      .getOrElse(Seq.empty[String])
  }

  /**
   * Calculate the frequent items used for salted join.
   * @param withKeyDF the input dataframe with key columns.
   * @param keyTagsToColumnMap a map from key tags to the key column.
   * @param saltedJoinParameters parameters generating frequent items.
   *                             No frequent items will be generated if this parameter is None.
   * @return a map from key tags to the frequent item dataframe.
   */
  private def generateSaltedJoinFrequentItems(
      keyTagsToColumnMap: Map[Seq[Int], String],
      withKeyDF: DataFrame,
      saltedJoinParameters: Option[SaltedSparkJoin.JoinParameters]): Map[Seq[Int], DataFrame] = {
    if (saltedJoinParameters.isEmpty) return Map[Seq[Int], DataFrame]()
    keyTagsToColumnMap.flatMap {
      case (keyTags, keyColumnName) =>
        val frequentItemsDf = SaltedSparkJoin.getFrequentItemsDataFrame(
          saltedJoinParameters.get.estimator,
          withKeyDF,
          keyColumnName,
          saltedJoinParameters.get.frequentItemThreshold)
        if (frequentItemsDf.head(1).nonEmpty) {
          Some(keyTags, frequentItemsDf)
        } else {
          log.info("Salted Join: no frequent items for key: " + keyColumnName)
          None
        }
    }
  }
}
