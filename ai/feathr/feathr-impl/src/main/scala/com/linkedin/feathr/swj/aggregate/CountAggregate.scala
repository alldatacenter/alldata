package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types.{DataType, IntegerType, LongType}

/**
  * COUNT aggregation implementation.
  *
  * @param metricCol Name of the metric column or a Spark SQL column expression for derived metric
  *                  that will be aggregated using COUNT. For pre-aggregated dataset, this needs to
  *                  be a column name. For non-aggregated dataset, this valu could default to column
  *                  expression lit(1).
  */
class CountAggregate(val metricCol: String = "1") extends AggregationSpec {
  override def aggregation: AggregationType = COUNT

  override def metricName = "count_col"

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
        case _ => throw new RuntimeException(s"Invalid data type for COUNT metric col $metricCol. " +
          s"Only Int and Long are supported, but got ${dataType.typeName}")
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
        case _ => throw new RuntimeException(s"Invalid data type for COUNT metric col $metricCol. " +
          s"Only Int and Long are supported, but got ${dataType.typeName}")
      }
    }
  }

  /**
   * Returns true if the aggregate count is null or zero. A null aggregate count effectively means zero.
   *
   * @param aggregate The aggregate count value
   * @param dataType The datatype of the aggregate count value
   * @return true if the aggregate count is null or zero
   */
  def isZeroOrNull(aggregate: Any, dataType: DataType): Boolean = {
    aggregate == null || {
      dataType match {
        case IntegerType => aggregate.asInstanceOf[Int] == 0
        case LongType => aggregate.asInstanceOf[Long] == 0L
        case _ => throw new RuntimeException(s"Invalid data type for COUNT metric col $metricCol. " +
          s"Only Int and Long are supported, but got ${dataType.typeName}")
      }
    }
  }
}
