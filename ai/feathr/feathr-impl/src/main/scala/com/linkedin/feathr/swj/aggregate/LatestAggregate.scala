package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * LATEST aggregation implementation
  *
  * @param metricCol Name of the column that will be used to determine latest value.
  */
class LatestAggregate(val metricCol: String) extends AggregationSpec {
  override def aggregation: AggregationType = LATEST

  override def metricName = "latest_col"

  override def isIncrementalAgg = false

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    // Since data is sorted by timestamp during aggregation, the next record for
    // aggregation will always be the later record.
    // Here we always return the later record, if it is not null.
    if (record == null) {
      aggregate
    } else {
      record
    }
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    throw new RuntimeException("Method deagg for LATEST aggregate is not implemented because LATEST is " +
      "not an incremental aggregation.")
  }
}
