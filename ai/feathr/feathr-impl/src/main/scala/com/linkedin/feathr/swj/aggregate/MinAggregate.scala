package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types.{DataType, DoubleType, FloatType, IntegerType, LongType}

/**
  * MIM aggregation implementation.
  *
  * @param metricCol Name of the metric column or a Spark SQL column expression for derived metric
  *                  that will be aggregated using MIN.
  */
class MinAggregate(val metricCol: String) extends AggregationSpec {
  override def aggregation: AggregationType = MIN

  override def metricName = "min_col"

  override def isIncrementalAgg = false

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      record
    } else if (record == null) {
      aggregate
    } else {
      dataType match {
        case IntegerType => math.min(aggregate.asInstanceOf[Int], record.asInstanceOf[Int])
        case LongType => math.min(aggregate.asInstanceOf[Long], record.asInstanceOf[Long])
        case DoubleType => math.min(aggregate.asInstanceOf[Double], record.asInstanceOf[Double])
        case FloatType => math.min(aggregate.asInstanceOf[Float], record.asInstanceOf[Float])
        case _ => throw new RuntimeException(s"Invalid data type for MIN metric col $metricCol. " +
          s"Only Int, Long, Double, and Float are supported, but got ${dataType.typeName}")
      }
    }
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    throw new RuntimeException("Method deagg for MIN aggregate is not implemented because MIN is " +
      "not an incremental aggregation.")
  }
}
