package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.{FeatureTypeConfig, SELECTED_FEATURES}
import com.linkedin.feathr.offline.anchored.anchorExtractor.TimeWindowConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.job.TransformedResult
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import com.linkedin.feathr.offline.testfwk.TestFwkUtils
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.expr

/**
 * Evaluator that transforms features using TimeWindowConfigurableAnchorExtractor,
 * Currently, this is only used in feature generation. Similar to Spark-algorithm repo's SWA join.
 */

private[offline] object WindowAggregationEvaluator {

  /**
   * Transform and add feature column to input dataframe using TimeWindowConfigurableAnchorExtractor
   * @param transformer SimpleAnchorExtractorSpark implementation
   * @param inputDf input dataframe
   * @param requestedFeatureNameAndPrefix feature names and prefix pairs.
   * @param featureAnchorWithSource feature anchor with source that has the transformer
   * @return (dataframe with features, feature column format), feature column format can only be FeatureColumnFormatRAW for now
   */
  def transform(
      transformer: TimeWindowConfigurableAnchorExtractor,
      inputDf: DataFrame,
      requestedFeatureNameAndPrefix: Seq[(String, String)],
      featureAnchorWithSource: FeatureAnchorWithSource,
      inputDateInterval: Option[DateTimeInterval]): TransformedResult = {
    val requestedFeatureNames = requestedFeatureNameAndPrefix.map(_._1)
    transformer.setInternalParams(SELECTED_FEATURES, s"[${requestedFeatureNames.mkString(",")}]")
    val filteredDF = {
      // for sliding window aggregation features, we need to filter the datasets with timestamps
      val timeWindowParams = SlidingWindowFeatureUtils.getTimeWindowParam(featureAnchorWithSource.source)
      val timestampCol = timeWindowParams.timestampColumn // referenceEndDateTime
      val timestampFormat = timeWindowParams.timestampColumnFormat // dateTimeFormat
      val timestampColExpr = SlidingWindowFeatureUtils.constructTimeStampExpr(timestampCol, timestampFormat)
      val keyAlias = featureAnchorWithSource.featureAnchor.sourceKeyExtractor.getKeyColumnAlias(None)
      val defaultTimeParams = featureAnchorWithSource.dateParam
      // The following line is specific for aggregating the old delta window during the incremental aggregation.
      // Currently, dateParam field is initialized based on the endTime and feature aggregation window in the config.
      // And any record in df lying out the range of dateParam is filtered out. Part of old delta window df
      // falls out the range of dateParam.
      // Ideally we should reconsider how to initialize dateParam properly or provide an interface to update the
      // dataParam field.

      val intervalOpt = inputDateInterval.orElse(defaultTimeParams.map(OfflineDateTimeUtils.createIntervalFromFeatureGenDateParam))
      transformer.initParams(timestampCol, keyAlias)
      if (intervalOpt.isDefined) {
        if (TestFwkUtils.IS_DEBUGGER_ENABLED) {
          println(f"${Console.GREEN}Defined window is from {${intervalOpt.get.getStart}} to ${intervalOpt.get.getEnd}${Console.RESET}")
        }
        val interval = intervalOpt.get
        val epocStart = interval.getStart.toInstant.getEpochSecond
        val epocEnd = interval.getEnd.toInstant.getEpochSecond
        val TIMESTAMP_COLUMN_NAME = "_feathr_swa_timestamp_col"
        inputDf
          .withColumn(TIMESTAMP_COLUMN_NAME, expr(timestampColExpr))
          .where(expr(s"${TIMESTAMP_COLUMN_NAME} >= ${epocStart} and ${TIMESTAMP_COLUMN_NAME} < ${epocEnd}"))
      } else {
        // dataset does not have timestamp column, relying on dataset path to specify time info, which is already applied
        // when loading the dataset, so we don't need to filter here
        inputDf
      }
    }
    // Apply transformation and get transformed column
    val transformedColumns = transformer.transformAsColumns(filteredDF)
    val transformedDF = transformedColumns.foldLeft(filteredDF)((baseDF, columnWithName) => baseDF.withColumn(columnWithName._1, columnWithName._2))

    if (TestFwkUtils.IS_DEBUGGER_ENABLED) {
      println(f"${Console.GREEN}Showing the dataset in the window: ${Console.RESET}")
      if (transformedDF.isEmpty) {
        println(f"${Console.RED}There doesnt seem to have any data in the window you defined. Please check your window configurations.${Console.RESET}")
      }
      transformedDF.show(10)
    }

    TransformedResult(
      requestedFeatureNameAndPrefix,
      transformedDF,
      requestedFeatureNames.map(c => (c, FeatureColumnFormat.RAW)).toMap,

      Map.empty[String, FeatureTypeConfig])
  }
}
