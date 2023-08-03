package com.linkedin.feathr.swj.aggregate

import com.linkedin.feathr.swj.join.FeatureColumnMetaData
import AggregationType.AggregationType
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DataType, StructType}

/**
 * Base class for aggregation functions that support the deagg operation (e.g. Sum/Avg). This class provides common
 * functionality to keep track of a running count aggregation while performing the implemented aggregation type.
 * Keeping track of the running count is important in order to identify when all records have been deagged out so the
 * aggregate value can be properly reset. This is important to maintain current expectation where aggregate value is
 * null when there are no records to aggregate together and to prevent floating point rounding errors causing the
 * deagged aggregate value to be infinitesimally small instead of equal to zero. This class abstracts way the management
 * of the count aggregation column so child classes only have to worry about implementing the aggregation function
 * for the value column.
 */
abstract class AggregationWithDeaggBase extends AggregationSpec {
  val CountAggregate: CountAggregate = new CountAggregate(countCol)

  /**
   * @inheritdoc
   */
  def aggregation: AggregationType

  /**
   * @inheritdoc
   */
  def metricName: String

  /**
   * Name of the metric column or a Spark SQL column expression for derived metric that will be aggregated
   */
  def valueCol: String

  /**
   * Name of the column that keeps track of the count which is required for all aggregation functions that support
   * deagg in order to identify when all records have been deagged out
   */
  def countCol: String

  /**
   * Analogous to [[AggregationSpec.agg()]], we aggregate the parsed value record to the intermediate aggregate value
   *
   * @param aggregate The intermediate aggregate value
   * @param valueRecord Parsed value record to aggregate
   * @param valueDataType The value record datatype
   * @return The result of aggregating the value record to the intermediate aggregate value
   */
  def aggValue(aggregate: Any, valueRecord: Any, valueDataType: DataType): Any

  /**
   * Analogous to [[AggregationSpec.deagg()]], we deagg the parsed value record from the intermediate aggregate value
   *
   * @param aggregate The intermediate aggregate value
   * @param valueRecord Parsed value record to deagg
   * @param valueDataType The value record datatype
   * @return The result of deagging the value record from the intermediate aggregate value
   */
  def deaggValue(aggregate: Any, valueRecord: Any, valueDataType: DataType): Any

  /**
   * Analogous to [[AggregationSpec.calculateAggregate()]], perform an additional step to calculate the final aggregate
   * value. This method has access to the current running count if needed for the calculation (e.g. AVG). Default
   * implementation is to return the aggregate value as is.
   *
   * @param aggregate The intermediate aggregate value
   * @param valueDataType The value record datatype
   * @param count The intermediate count value
   * @param countDataType The count record datatype
   * @return The calculated aggregate value
   */
  def calculateAggregateWithCount(aggregate: Any, valueDataType: DataType, count: Any, countDataType: DataType): Any = aggregate

  override def metricCol: String = s"struct($valueCol, $countCol)"

  private case class MetricCol(
    value: Any,
    count: Any,
    dataTypes: DataTypes
  )

  private case class DataTypes(
    valueDataType: DataType,
    countDataType: DataType
  )

  private def parseMetricCol(struct: Any, dataType: DataType): MetricCol = {
    val dataTypes = parseDataTypes(dataType)
    val (value, count) = {
      if (struct == null) {
        (null, null)
      } else {
        val structAsRow = struct.asInstanceOf[Row]
        (structAsRow.get(0), structAsRow.get(1))
      }
    }

    MetricCol(value = value, count = count, dataTypes = dataTypes)
  }

  private def parseDataTypes(dataType: DataType): DataTypes = {
    dataType match {
      case StructType(fields) if fields.length == 2 =>
        DataTypes(valueDataType = fields(0).dataType,  countDataType = fields(1).dataType)
      case _ => throw getInvalidDataTypeException(dataType)
    }
  }

  override def isIncrementalAgg: Boolean = true

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    val aggregateMetricCol = parseMetricCol(aggregate, dataType)
    val recordMetricCol = parseMetricCol(record, dataType)

    Row(
      aggValue(aggregateMetricCol.value, recordMetricCol.value, aggregateMetricCol.dataTypes.valueDataType),
      CountAggregate.agg(aggregateMetricCol.count, recordMetricCol.count, aggregateMetricCol.dataTypes.countDataType)
    )
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    val aggregateMetricCol = parseMetricCol(aggregate, dataType)
    val recordMetricCol = parseMetricCol(record, dataType)

    val countAggregate = CountAggregate.deagg(aggregateMetricCol.count, recordMetricCol.count, aggregateMetricCol.dataTypes.countDataType)
    if (CountAggregate.isZeroOrNull(countAggregate, aggregateMetricCol.dataTypes.countDataType)) {
      // Reset aggregate value if all records are deagged out
      Row(null, null)
    } else {
      Row(deaggValue(aggregateMetricCol.value, recordMetricCol.value, aggregateMetricCol.dataTypes.valueDataType), countAggregate)
    }
  }

  override def calculateAggregate(aggregate: Any, dataType: DataType): Any = {
    val aggregateMetricCol = parseMetricCol(aggregate, dataType)

    calculateAggregateWithCount(
      aggregateMetricCol.value, aggregateMetricCol.dataTypes.valueDataType,
      aggregateMetricCol.count, aggregateMetricCol.dataTypes.countDataType
    )
  }

  override def updateAggDataType(featureColumnMetaData: FeatureColumnMetaData): Unit = {
    featureColumnMetaData.aggDataType = parseDataTypes(featureColumnMetaData.aggDataType).valueDataType
  }

  private def getInvalidDataTypeException(dataType: DataType): Exception = {
    new RuntimeException(s"Invalid data type for ${aggregation.toString} metric col $metricCol. " +
      s"The metric column is a StructType with first the value column to aggregate over and second the count column to track the running count. " +
      s"In value column, only Arrays of Int, Long, Float, and Double with non null elements are supported. " +
      s"In count column, only Int and Long are supported. We instead got ${dataType.prettyJson}")
  }
}