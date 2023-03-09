package com.linkedin.feathr.swj.aggregate

import com.linkedin.feathr.swj.join.FeatureColumnMetaData
import AggregationType._
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

/**
  * AVG aggregation implementation.
  *
  * @param valueCol Name of the metric column or a Spark SQL column expression for derived metric
  *                  that will be aggregated using AVG
  * @param countCol Name of the column that keeps track of the count. This is needed for pre-aggregated
  *                 fact dataset. For non-aggregated dataset, if the column name is not provided, using
  *                 the default column expression lit(1).
  */
class AvgAggregate(valueCol: String, countCol: String = "1") extends AggregationSpec {
  override def aggregation: AggregationType = AVG

  // For AvgAggregate, the metricCol is a StructType containing both the value column and the
  // count column. If other aggregate type requiring multiple columns is needed in the future,
  // similar technique can be applied.
  override def metricCol = s"struct($valueCol, $countCol)"

  override def metricName = "avg_col"

  override def isIncrementalAgg = true

  override def isCalculateAggregateNeeded: Boolean = true

  override def calculateAggregate(aggregate: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      aggregate
    } else {
      dataType match {
        case StructType(fields) if (fields.length == 2) =>
          val valueType = fields(0).dataType
          val countType = fields(1).dataType
          val aggregateRow = aggregate.asInstanceOf[Row]
          val count = countType match {
            case IntegerType => aggregateRow.getInt(1).toDouble
            case LongType => aggregateRow.getLong(1).toDouble
            case _ => throw new RuntimeException(s"Invalid data type for count column for AVG " +
              s"aggregate result. Only Int and Long are supported, but got ${countType.typeName}")
          }
          valueType match {
            case IntegerType =>
              aggregateRow.getInt(0) / count
            case LongType =>
              aggregateRow.getLong(0) / count
            case DoubleType =>
              aggregateRow.getDouble(0) / count
            case FloatType =>
              aggregateRow.getFloat(0) / count
            case _ => throw new RuntimeException(s"Invalid data type for value column for AVG " +
              s"aggregate. Only Int, Long, Double and Float are supported, but got " +
              s"${valueType.typeName}")
          }
        case _ => throw new RuntimeException(s"Invalid data type for AVG aggregate result. " +
          s"Only StructType with 2 fields is supported, but got ${dataType.typeName}")
      }
    }
  }

  override def updateAggDataType(featureColumnMetaData: FeatureColumnMetaData): Unit = {
    featureColumnMetaData.aggDataType = DoubleType
  }

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      record
    } else if (record == null) {
      aggregate
    } else {
      dataType match {
        case StructType(fields) if (fields.length == 2) =>
          genRow(fields(0).dataType, fields(1).dataType,
            aggregate.asInstanceOf[Row], record.asInstanceOf[Row], isAgg = true)
        case _ => throw new RuntimeException(s"Invalid data type for AVG metric col $metricCol. " +
          s"Only StructType with 2 fields is supported, but got ${dataType.typeName}")
      }
    }
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (record == null) {
      aggregate
    } else if (aggregate == null) {
      throw new RuntimeException(s"Aggregate result is null but the record to be removed from aggregate " +
        s"is not. Such scenario should never happen.")
    } else {
      dataType match {
        case StructType(fields) if (fields.length == 2) =>
          genRow(fields(0).dataType, fields(1).dataType,
            aggregate.asInstanceOf[Row], record.asInstanceOf[Row], isAgg = false)
        case _ => throw new RuntimeException(s"Invalid data type for AVG metric col $metricCol. " +
          s"Only StructType with 2 fields is supported, but got ${dataType.typeName}")
      }
    }
  }

  /**
    * Method to generate a new Row representing the value of updating the aggregateRow with recordRow
    * for both the value and count column.
    * @param valueType DataType of value column
    * @param countType DataType of count column
    * @param aggregateRow The Row containing the values of the previous aggregate
    * @param recordRow The Row representing a new record to be aggregated
    * @param isAgg Flag indicating whether this method is used for agg or deagg
    */
  private def genRow(
      valueType: DataType,
      countType: DataType,
      aggregateRow: Row,
      recordRow: Row,
      isAgg: Boolean): Row = {
    val count: AnyVal = countType match {
      case IntegerType =>
        val aggCount = aggregateRow.getInt(1)
        val recordCount = recordRow.getInt(1)
        if (isAgg) {
          aggCount + recordCount
        } else {
          aggCount - recordCount
        }
      case LongType =>
        val aggCount = aggregateRow.getLong(1)
        val recordCount = recordRow.getLong(1)
        if (isAgg) {
          aggCount + recordCount
        } else {
          aggCount - recordCount
        }
      case _ => throw new RuntimeException(s"Invalid data type for count column $countCol for " +
        s"AVG aggregate. Only Int and Long are supported, but got ${countType.typeName}")
    }
    val value: AnyVal = valueType match {
      case IntegerType =>
        val aggValue = aggregateRow.getInt(0)
        val recordValue = recordRow.getInt(0)
        if (isAgg) {
          aggValue + recordValue
        } else {
          aggValue - recordValue
        }
      case LongType =>
        val aggValue = aggregateRow.getLong(0)
        val recordValue = recordRow.getLong(0)
        if (isAgg) {
          aggValue + recordValue
        } else {
          aggValue - recordValue
        }
      case DoubleType =>
        val aggValue = aggregateRow.getDouble(0)
        val recordValue = recordRow.getDouble(0)
        if (isAgg) {
          aggValue + recordValue
        } else {
          aggValue - recordValue
        }
      case FloatType =>
        val aggValue = aggregateRow.getFloat(0)
        val recordValue = recordRow.getFloat(0)
        if (isAgg) {
          aggValue + recordValue
        } else {
          aggValue - recordValue
        }
      case _ => throw new RuntimeException(s"Invalid data type for value column $valueCol " +
        s"for AVG aggregate. Only Int, Long, Double and Float are supported, but got " +
        s"${valueType.typeName}")
    }
    Row(value, count)
  }
}
