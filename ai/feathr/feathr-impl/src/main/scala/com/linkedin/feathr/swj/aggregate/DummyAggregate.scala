package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * Dummy aggregation. To be used as placeholder for operations not implemented in SWJ library yet.
  *
  * @param metricCol not used
  */
class DummyAggregate(val metricCol: String) extends AggregationSpec {
  override def aggregation: AggregationType = DUMMY

  override def metricName = ""

  override def isIncrementalAgg = true

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    throw new RuntimeException("Method agg for DummyAggregate aggregate should never be called!")
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    throw new RuntimeException("Method deagg for DummyAggregate aggregate should never be called!")
  }
}