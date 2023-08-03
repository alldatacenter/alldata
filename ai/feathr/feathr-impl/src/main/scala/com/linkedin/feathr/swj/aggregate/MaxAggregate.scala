package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * MAX aggregation implementation.
  *
  * @param metricCol Name of the metric column or a Spark SQL column expression for derived metric
  *                  that will be aggregated using MAX.
  */
class MaxAggregate(val metricCol: String) extends AggregationSpec {
  override def aggregation: AggregationType = MAX

  override def metricName = "max_col"

  override def isIncrementalAgg = false

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      record
    } else if (record == null) {
      aggregate
    } else {
      dataType match {
        case IntegerType => math.max(aggregate.asInstanceOf[Int], record.asInstanceOf[Int])
        case LongType => math.max(aggregate.asInstanceOf[Long], record.asInstanceOf[Long])
        case DoubleType => math.max(aggregate.asInstanceOf[Double], record.asInstanceOf[Double])
        case FloatType => math.max(aggregate.asInstanceOf[Float], record.asInstanceOf[Float])
        case _ => throw new RuntimeException(s"Invalid data type for MAX metric col $metricCol. " +
          s"Only Int, Long, Double, and Float are supported, but got ${dataType.typeName}")
      }
    }
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    throw new RuntimeException("Method deagg for MAX aggregate is not implemented because MAX is " +
      "not an incremental aggregation.")
  }
}
