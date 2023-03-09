package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * TIMESINCE aggregation implementation
  *
  * @param metricCol Name of the timestamp column that will be aggregated using TIMESINCE.
  */
class TimesinceAggregate(val metricCol: String) extends AggregationSpec{
  override def aggregation: AggregationType = TIMESINCE

  override def metricName = "timesince_col"

  override def isIncrementalAgg = true

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      record
    } else if (record == null) {
      aggregate
    } else {
      dataType match {
        case LongType => record.asInstanceOf[Long]
        case _ => throw new RuntimeException(s"Invalid data type for TIMESINCE metric col $metricCol. " +
          s"Only Long is supported, but got ${dataType.typeName}")
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
        case LongType =>
          val aggregateTimestamp = aggregate.asInstanceOf[Long]
          val recordTimestamp = record.asInstanceOf[Long]
          if (recordTimestamp < aggregateTimestamp) {
            aggregateTimestamp
          } else {
            recordTimestamp
          }
        case _ => throw new RuntimeException(s"Invalid data type for TIMESINCE metric col $metricCol. " +
          s"Only Long is supported, but got ${dataType.typeName}")
      }
    }
  }
}
