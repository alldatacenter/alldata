package com.linkedin.feathr.swj.aggregate

import com.linkedin.feathr.swj.join.FeatureColumnMetaData
import AggregationType._
import org.apache.spark.sql.types.DataType

/**
  * Defines the aggregation specification for a sliding window aggregated feature.
  * This trait is not exposed to users of sliding window join library, but rather
  * defines the required fields a sliding window aggregation needs to implement.
  * When implementing a new AggregationSpec, the developer needs to implement all
  * fields except metricCol. The field metricCol is supposed to be passed in via
  * the constructor of the concrete AggregationSpec class.
  */
private[feathr] trait AggregationSpec extends Serializable {
  // Type of the aggregation as an AggregationType
  def aggregation: AggregationType
  // It can be either the name of the metric column or a Spark SQL column expression
  // if the metric is derived, i.e., 'col_1 + col_2' or 'metric_col * weight_col' or 'lit(1)'.
  def metricCol: String
  // Name of the metric column in the transformed feature column. This is to standardize
  // metric column name in the transformed feature DataFrame
  def metricName: String
  // Specify if a given aggregation type can be calculated incrementally or not.
  // AVG, COUNT, SUM, and TIMESINCE are incremental, but MAX is not.
  def isIncrementalAgg: Boolean
  // Given the intermediate aggregate value and the record to aggregate, both of which should
  // represent data of the specified type, perform the aggregation
  def agg(aggregate: Any, record: Any, dataType: DataType): Any
  // Given the intermediate aggregate value and the record to remove from the aggregate value,
  // both of which should represent data of the specified type, perform the removal
  def deagg(aggregate: Any, record: Any, dataType: DataType): Any
  // For certain aggregate types such as AVG, it needs an additional step to calculate the final
  // aggregate result. This is usually used by aggregate type that keeps track of multiple
  // columns during aggregation and needs to convert the multiple columns into a final result
  // value. Default implementation of this method directly returns the input object. This
  // implementation is used by all single column aggregate types.
  def calculateAggregate(aggregate: Any, dataType: DataType): Any = aggregate
  // Indicate whether invocation to calculateAggregate is needed for a given aggregate type.
  // This flag can be used to skip always calling calculateAggregate for aggregation types
  // that do not need it to reduce overhead.
  def isCalculateAggregateNeeded: Boolean = false
  // Update the aggDataType field in the corresponding FeatureColumnMetaData if necessary.
  // The default implementation is noop. AVG aggregate type and other potential aggregation
  // types that have different DataType for metric column and aggregate result need to implement
  // this method.
  def updateAggDataType(featureColumnMetaData: FeatureColumnMetaData): Unit = {}
}
