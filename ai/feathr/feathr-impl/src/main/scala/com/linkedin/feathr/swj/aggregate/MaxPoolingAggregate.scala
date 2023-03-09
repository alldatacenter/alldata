package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * MAX_POOLING aggregation implementation.
  *
  * @param metricCol Name of the metric column or a Spark SQL column expression for derived metric
  *                  that will be aggregated using MAX_POOLING. This operation differs from MAX in that this
  *                  applies the MAX operation across an entire Seq of Numerics instead of a single value
  */
class MaxPoolingAggregate(val metricCol: String) extends AggregationSpec {
  override def aggregation: AggregationType = MAX_POOLING

  override def metricName = "max_pooling_col"

  override def isIncrementalAgg = false

  override def agg(aggregate: Any, record: Any, dataType: DataType): Any = {
    if (aggregate == null) {
      record
    } else if (record == null) {
      aggregate
    } else {
      dataType match {
        case arrayType: ArrayType => {
          arrayType.elementType match {
            case IntegerType => MaxPoolingAggregate.update(aggregate.asInstanceOf[Seq[Int]], record.asInstanceOf[Seq[Int]])
            case LongType => MaxPoolingAggregate.update(aggregate.asInstanceOf[Seq[Long]], record.asInstanceOf[Seq[Long]])
            case FloatType => MaxPoolingAggregate.update(aggregate.asInstanceOf[Seq[Float]], record.asInstanceOf[Seq[Float]])
            case DoubleType => MaxPoolingAggregate.update(aggregate.asInstanceOf[Seq[Double]], record.asInstanceOf[Seq[Double]])
            case _ => throw MaxPoolingAggregate.getInvalidDataTypeException(metricCol, dataType)
          }
        }
        case _ => throw MaxPoolingAggregate.getInvalidDataTypeException(metricCol, dataType)
      }
    }
  }

  override def deagg(aggregate: Any, record: Any, dataType: DataType): Any = {
    throw new RuntimeException("Method deagg for MAX_POOLING aggregate is not implemented because MAX_POOLING is " +
      "not an incremental aggregation.")
  }
}

object MaxPoolingAggregate {

  private def getInvalidDataTypeException(metricCol: String, dataType: DataType): Exception = {
    new RuntimeException(s"Invalid data type for MAX_POOLING metric col $metricCol. " +
      s"Only Arrays of Int, Long, Float, and Double with non null elements are supported, but got ${dataType.prettyJson}")
  }

  private def update[T](aggregate: Seq[T], record: Seq[T])(implicit numeric: Numeric[T]): Seq[T] = {
    require(aggregate.length == record.length, "Embeddings to pool are all expected to have equal length. " +
      s"Aggregate Embedding Length: ${aggregate.length}, Record Embedding Length: ${record.length}.")
    aggregate.zip(record).map { case (x, y) => numeric.max(x, y) }
  }
}


