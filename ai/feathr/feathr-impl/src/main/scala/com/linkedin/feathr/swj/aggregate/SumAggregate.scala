package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * SUM aggregation implementation
  *
  * @param metricCol Name of the metric column or a Spark SQL column expression for derived metric
  *                  that will be aggregated using SUM.
  */
class SumAggregate(val metricCol: String) extends AggregationSpec {
  override def aggregation: AggregationType = SUM

  override def metricName = "sum_col"

  override def isIncrementalAgg = true

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      record
    } else if (record == null) {
      aggregate
    } else {
      dataType match {
        case IntegerType => aggregate.asInstanceOf[Int] + record.asInstanceOf[Int]
        case LongType => aggregate.asInstanceOf[Long] + record.asInstanceOf[Long]
        case DoubleType => aggregate.asInstanceOf[Double] + record.asInstanceOf[Double]
        case FloatType => aggregate.asInstanceOf[Float] + record.asInstanceOf[Float]
        case _ => throw new RuntimeException(s"Invalid data type for SUM metric col $metricCol. " +
          s"Only Int, Long, Double, and Float are supported, but got ${dataType.typeName}")
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
        case IntegerType => aggregate.asInstanceOf[Int] - record.asInstanceOf[Int]
        case LongType => aggregate.asInstanceOf[Long] - record.asInstanceOf[Long]
        case DoubleType => aggregate.asInstanceOf[Double] - record.asInstanceOf[Double]
        case FloatType => aggregate.asInstanceOf[Float] - record.asInstanceOf[Float]
        case _ => throw new RuntimeException(s"Invalid data type for SUM metric col $metricCol. " +
          s"Only Int, Long, Double, and Float are supported, but got ${dataType.typeName}")
      }
    }
  }
}
