package com.linkedin.feathr.offline.anchored.anchorExtractor

import com.fasterxml.jackson.annotation.JsonProperty
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.config.{ComplexAggregationFeature, TimeWindowFeatureDefinition}
import com.linkedin.feathr.offline.generation.aggregations._
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils.convertFeathrDefToSwjDef
import com.linkedin.feathr.sparkcommon.SimpleAnchorExtractorSpark
import com.linkedin.feathr.swj.aggregate.AggregationType
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, DataFrame}

import java.time.Duration

/**
 * A time-window configurable FeatureAnchor that extract features based on its definitions.
 * This extractor support same syntax as the sliding window aggregation, but does not require
 * observation data. Currently, it is only used in feature generation, we will extend using it
 * in feature join in the future.
 *
 * This class unifies all aggregation features (basic or complex) into a unified calculation pattern,
 * i.e., intermediateColumns -> aggregateColumns -> postProcessing, by creating intermediate columns for
 * the non-complex aggregation types. See more details of the design here:

 *
 * @param features A map of feature name to feature definition that provides the feature for a given data record
 *
 */
private[offline] class TimeWindowConfigurableAnchorExtractor(@JsonProperty("features")val features: Map[String, TimeWindowFeatureDefinition]) extends SimpleAnchorExtractorSpark {
  // timestamp column expression for input dataframe
  var _timestampColExpr: String = _
  // key alias of the join key for input dataframe
  var _keyAlias: Seq[String] = _
  val TIMESTAMP_COLUMN_NAME = "_feathr_swa_timestamp_col"

  // init SWA related parameters, e.g., timestamp column, key alias
  // this function will be called before transformAsColumns and aggregateAsColumns
  def initParams(timestampCol: String, keyAlias: Seq[String]): Unit = {
    _timestampColExpr = timestampCol
    _keyAlias = keyAlias
  }

  @transient private lazy val log = LogManager.getLogger(getClass)

  // make it lazy, as _keyAlias is not be initialized when loading the config
  lazy private val aggFeatures = features.map {
    case (featureName, featureDef) =>
      val swjFeature = convertFeathrDefToSwjDef(featureName, featureDef, Duration.ZERO, None)
      featureName -> ComplexAggregationFeature(featureDef, swjFeature)
  }

  /**
   * apply aggregation transformation on the groupedDataframe
   * @param groupedDataFrame original input dataframe after grouped by feature key
   * @return list of (column name, column expression) for Feathr to append to grouped dataframe, these
   *         columns could be intermediate columns of the extractor, or could be the final output
   *         of the extractor
   */
  override def aggregateAsColumns(groupedDataFrame: DataFrame): Seq[(String, Column)] = {
    val columnPairs = aggFeatures.collect {
      case (featureName, featureDef) =>
        // for basic sliding window aggregation
        // no complex aggregation will be defined
        if (featureDef.swaFeature.lateralView.isDefined) {
          throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Lateral view is not supported in TimeWindowConfigurableAnchorExtractor yet")
        }
        val aggFuncName = featureDef.timeWindowFeatureDefinition.aggregationType.toString
        val aggType = AggregationType.withName(aggFuncName)
        val colName = getFeatureColumnName(featureName, aggFuncName)

        val baseAggCol = if (featureDef.timeWindowFeatureDefinition.groupBy.isDefined) {
          val groupByKey = featureDef.timeWindowFeatureDefinition.groupBy.get
          val collectMapUDF = new CollectTermValueMap
          if (aggType == AggregationType.LATEST) {
            /*
             *  latest aggregation with groupby allow duplicate value(term) to override the old values with same term
             *  i.e. newer data override old data if the term(group id) are the same. Note that we already sort the
             *  data according to timestamp before
             */
            val params = ConfigFactory.parseString(s"{${CollectTermValueMap.ALLOW_DUPLICATES}: true}")
            collectMapUDF.init(params)
          }
          collectMapUDF(expr(groupByKey), expr(colName))
        } else {
          val aggCol = aggType match {
            case AggregationType.MAX => max(expr(colName))
            case AggregationType.MIN => min(expr(colName))
            case AggregationType.SUM => sum(expr(colName))
            case AggregationType.AVG => avg(expr(colName))
            case AggregationType.COUNT => count(expr(colName))
            case AggregationType.MAX_POOLING => first(expr(colName))
            case AggregationType.MIN_POOLING => first(expr(colName))
            case AggregationType.AVG_POOLING => first(expr(colName))
            case AggregationType.LATEST => last(expr(colName), true)
            case tp =>
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"AggregationType ${tp} is not supported in aggregateAsColumns of TimeWindowConfigurableAnchorExtractor.")
          }
          aggCol.alias(featureName)
        }
        val baseAggCols = Some(Seq((featureName, baseAggCol)))
        baseAggCols.getOrElse(Seq.empty[(String, Column)])
    }
    columnPairs.flatten.toSeq
  }

  override def getProvidedFeatureNames: Seq[String] = features.keys.toIndexedSeq

  /**
   * Apply postProcessing transformation on the aggregatedDataFrame
   * Since there is no complex aggregation, here directly return empty Seq
   * @param aggregatedDataFrame original input dataframe after grouped by feature key
   * @return list of (column name, column expression) for Feathr to append to grouped dataframe, these
   *         columns could be intermediate columns of the extractor, or could be the final output
   *         of the extractor
   */
  override def postProcessing(aggregatedDataFrame: DataFrame): Seq[(String, Column)] = {
    Seq()
  }

  /**
   * Apply the user defined SQL non-aggregation transformation to the dataframe and produce the (feature name, feature column) pairs,
   * one pair for each provided feature.
   * @param inputDF input dataframe
   * @return list of (column name, column expression) for Feathr to append to input dataframe, these
   *         columns could be intermediate columns of the extractor, or could be the final output
   *         of the extractor
   */
  override def transformAsColumns(inputDF: DataFrame): Seq[(String, Column)] = {
    // apply sql transformation for the features
    val featureDefColumns = aggFeatures
      .flatMap(featureNameAndDef => {
        val featureDef = featureNameAndDef._2
        val swaFeatureDef = featureDef.swaFeature
        if (swaFeatureDef.lateralView.isDefined) {
          throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Lateral view is not supported in TimeWindowConfigurableAnchorExtractor yet")
        }
        val hasFilter = swaFeatureDef.filterCondition.isDefined
        val filter = swaFeatureDef.filterCondition.getOrElse("")
        val metricColExpr =
          if (hasFilter) {
            expr(s" CASE WHEN $filter THEN ${featureDef.timeWindowFeatureDefinition.`def`} ELSE null END")
          } else {
            expr(featureDef.timeWindowFeatureDefinition.`def`)
          }
        val featureName = featureDef.swaFeature.name
        val aggFuncName = featureDef.timeWindowFeatureDefinition.aggregationType.toString
        val aggType = AggregationType.withName(aggFuncName)
        val colName = getFeatureColumnName(featureName, aggFuncName)

        // no complex aggregation is defined, just normal SWA feature
        val intermediateDefCols = if (featureDef.timeWindowFeatureDefinition.groupBy.isDefined) {
          val aggCol = aggType match {
            case AggregationType.MAX => max(metricColExpr)
            case AggregationType.MIN => min(metricColExpr)
            case AggregationType.SUM => sum(metricColExpr)
            case AggregationType.AVG => avg(metricColExpr)
            case AggregationType.COUNT => count(metricColExpr)
            case tp =>
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"${tp} with groupBy is not supported in transformAsColumns of TimeWindowConfigurableAnchorExtractor.")
          }
          val groupByKey = Seq(featureDef.timeWindowFeatureDefinition.groupBy.get) ++ _keyAlias

          val window = {
            val baseWindow = Window.partitionBy(groupByKey.map(expr): _*)
            if (AggregationType.LATEST == aggType) {
              baseWindow.orderBy(expr(_timestampColExpr))
            } else baseWindow
          }
          Some(Seq((colName, aggCol.over(window))))
        } else if (Seq(AggregationType.MAX_POOLING, AggregationType.MIN_POOLING, AggregationType.AVG_POOLING).contains(aggType)) {
          // handle pooling
          val window = Window.partitionBy(_keyAlias.head, _keyAlias.tail: _*)
          val params = s"{ embeddingSize: ${featureDef.timeWindowFeatureDefinition.embeddingSize.get} }"
          val paramsConfig = ConfigFactory.parseString(params)
          val poolCol = aggType match {
            case AggregationType.MAX_POOLING =>
              val maxPoolingUDAF = new MaxPoolingUDAF
              maxPoolingUDAF.init(paramsConfig)
              maxPoolingUDAF(metricColExpr)
            case AggregationType.MIN_POOLING =>
              val minPoolingUDAF = new MinPoolingUDAF
              minPoolingUDAF.init(paramsConfig)
              minPoolingUDAF(metricColExpr)
            case AggregationType.AVG_POOLING =>
              val avgPoolingUDAF = new AvgPoolingUDAF
              avgPoolingUDAF.init(paramsConfig)
              avgPoolingUDAF(metricColExpr)
          }
          Some(Seq((colName, poolCol.over(window))))
        } else if (AggregationType.LATEST == aggType) {
          val window = Window.partitionBy(_keyAlias.head, _keyAlias.tail: _*).orderBy(expr(_timestampColExpr))
          val latestCol = last(metricColExpr, true)
          Some(Seq((colName, latestCol.over(window))))
        } else {
          // other aggregations without groupBy, e.g, max, min, avg, etc.
          Some(Seq((colName, metricColExpr)))
        }

        intermediateDefCols.getOrElse(Seq())
      })
      .toSeq

    featureDefColumns
  }

  // intermediate column name convention
  private def getFeatureColumnName(featureName: String, aggFuncName: String): String = featureName + "_" + aggFuncName + "_col"
}
