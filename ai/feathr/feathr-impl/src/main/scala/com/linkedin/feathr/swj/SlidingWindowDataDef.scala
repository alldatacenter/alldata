package com.linkedin.feathr.swj

import com.linkedin.feathr.swj.aggregate.AggregationSpec

import java.time.Duration
import org.apache.spark.sql.DataFrame

/**
  * Defines the label dataset.
  *
  * @param dataSource label dataset as a DataFrame
  * @param joinKey column name for the join key. It can be something like 'x.id'
  *                representing nested column
  * @param timestampCol Timestamp column name. The data is assumed to be epoc of type long
  */
case class LabelData(
    dataSource: DataFrame,
    joinKey: Seq[String],
    timestampCol: String)

/**
  * Defines the fact dataset.
  *
  * @param dataSource fact dataset as a DataFrame
  * @param joinKey column name for the join key. It can be something like 'x.id'
  *                representing nested column
  * @param timestampCol Timestamp column name. The data is assumed to be epoc of type long
  * @param aggFeatures A list of [[SlidingWindowFeature]] representing all sliding window
  *                    aggregated features defined on one fact dataset.
  */
case class FactData(
    dataSource: DataFrame,
    joinKey: Seq[String],
    timestampCol: String,
    aggFeatures: List[SlidingWindowFeature])

/**
  * Defines a sliding window aggregated feature
  *
  * @param name Name of the feature. Used to uniquely identify a column in the DataFrame
  * @param agg An [[AggregationSpec]] for the sliding window aggregated feature
  * @param windowSpec A [[WindowSpec]] for the sliding window aggregated feature
  * @param filterCondition Filter condition to determine if a given record should be considered
  *                        in the sliding window aggregation. The String value should represent
  *                        a Spark SQL column expression
  * @param groupBy An optional [[GroupBySpec]] for the sliding window aggregated feature
  */
case class SlidingWindowFeature(
    name: String,
    agg: AggregationSpec,
    windowSpec: WindowSpec,
    filterCondition: Option[String] = None,
    groupBy: Option[GroupBySpec] = None,
    lateralView: Option[LateralViewParams] = None) extends Serializable

/**
  * Defines the group by specification for a sliding window aggregated feature
  *
  * @param groupByCol Name of the group by column. It can be something like 'x.id'
  *                   representing nested column
  * @param limit Applies a top-K limit on the group by results
  */
case class GroupBySpec(
    groupByCol: String,
    limit: Int) extends Serializable

/**
  * Defines the window specification for a sliding window aggregated feature
  *
  * @param width Window width
  * @param delay Simulated delay. Default to zero.
  */
case class WindowSpec(
    width: Duration,
    delay: Duration = Duration.ZERO) extends Serializable

/**
  * Defines lateral view specification for a sliding window aggregated feature
  * @param lateralViewDef
  * @param lateralViewItemAlias
  * @param lateralViewFilter
  */
case class LateralViewParams(
    lateralViewDef: String,
    lateralViewItemAlias: String,
    lateralViewFilter: Option[String]) extends Serializable