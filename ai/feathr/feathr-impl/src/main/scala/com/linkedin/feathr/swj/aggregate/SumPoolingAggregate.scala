package com.linkedin.feathr.swj.aggregate

import AggregationType._
import org.apache.spark.sql.types._

/**
  * SUM_POOLING aggregation implementation.
  *
  * @param valueCol Name of the metric column or a Spark SQL column expression for derived metric
  *                 that will be aggregated using SUM_POOLING. This operation differs from SUM in that this
  *                 applies the SUM operation across an entire Seq of Numerics instead of a single value
  * @param countCol Name of the column that keeps track of the count which is required for all aggregation functions
  *                 that support deagg in order to identify when all records have been deagged out.
  *                 This is needed for pre-aggregated fact dataset. For non-aggregated dataset,
  *                 if the column name is not provided, use the default column expression lit(1).
  */
class SumPoolingAggregate(val valueCol: String, val countCol: String = "1") extends AggregationWithDeaggBase {
  override def aggregation: AggregationType = SUM_POOLING

  override def metricName = "sum_pooling_col"

  // This is needed because in order to detect integer or float overflow, we perform the intermediate aggregations
  // using longs and doubles respectively. We then use the calculate aggregation function to detect overflow and coerce
  // back to integer and float respectively.
  override def isCalculateAggregateNeeded = true

  override def calculateAggregateWithCount(aggregate: Any, valueDataType: DataType, count: Any, countDataType: DataType): Any = {
    if (aggregate == null) {
      aggregate
    } else {
      valueDataType match {
        case arrayType: ArrayType => {
          arrayType.elementType match {
            case IntegerType => {
              val longSeq = aggregate.asInstanceOf[Seq[Long]]
              longSeq.foreach { l =>
                if (l < Int.MinValue || l > Int.MaxValue)
                  throw new ArithmeticException(s"Arithmetic int overflow error detected when SUM_POOLING metric col $metricCol")
              }
              longSeq.map(l => l.toInt)
            }
            case FloatType => {
              val doubleSeq = aggregate.asInstanceOf[Seq[Double]]
              doubleSeq.foreach { d =>
                if (d < Float.MinValue || d > Float.MaxValue)
                  throw new ArithmeticException(s"Arithmetic float overflow error detected when SUM_POOLING metric col $metricCol")
              }
              doubleSeq.map(l => l.toFloat)
            }
            case LongType => aggregate
            case DoubleType => aggregate
            case _ => throw getInvalidDataTypeException(valueDataType)
          }
        }
        case _ => throw getInvalidDataTypeException(valueDataType)
      }
    }
  }

  override def aggValue(aggregate: Any, valueRecord: Any, valueDataType: DataType): Any = {
    if (aggregate == null && valueRecord == null) {
      null
    } else if (aggregate == null && valueRecord != null) {
      val coercedRecord = valueDataType match {
        case arrayType: ArrayType => {
          arrayType.elementType match {
            case IntegerType => valueRecord.asInstanceOf[Seq[Int]].map(i => i.toLong)
            case FloatType => valueRecord.asInstanceOf[Seq[Float]].map(i => i.toDouble)
            case LongType => valueRecord
            case DoubleType => valueRecord
            case _ => throw getInvalidDataTypeException(valueDataType)
          }
        }
        case _ => throw getInvalidDataTypeException(valueDataType)
      }
      coercedRecord
    } else if (valueRecord == null && aggregate != null) {
      aggregate
    } else {
      valueDataType match {
        case arrayType: ArrayType => {
          arrayType.elementType match {
            case IntegerType => SumPoolingAggregate.update(aggregate.asInstanceOf[Seq[Long]], valueRecord.asInstanceOf[Seq[Int]].map(i => i.toLong))
            case FloatType => SumPoolingAggregate.update(aggregate.asInstanceOf[Seq[Double]], valueRecord.asInstanceOf[Seq[Float]].map(i => i.toDouble))
            case LongType => SumPoolingAggregate.update(aggregate.asInstanceOf[Seq[Long]], valueRecord.asInstanceOf[Seq[Long]])
            case DoubleType => SumPoolingAggregate.update(aggregate.asInstanceOf[Seq[Double]], valueRecord.asInstanceOf[Seq[Double]])
            case _ => throw getInvalidDataTypeException(valueDataType)
          }
        }
        case _ => throw getInvalidDataTypeException(valueDataType)
      }
    }
  }

  override def deaggValue(aggregate: Any, valueRecord: Any, valueDataType: DataType): Any = {
    if (valueRecord == null) {
      aggregate
    } else if (aggregate == null) {
      throw new RuntimeException(s"Aggregate result is null but the record to be removed from aggregate " +
        s"is not. Such scenario should never happen.")
    } else {
      valueDataType match {
        case arrayType: ArrayType => {
          arrayType.elementType match {
            case IntegerType => SumPoolingAggregate.remove(aggregate.asInstanceOf[Seq[Long]], valueRecord.asInstanceOf[Seq[Int]].map(i => i.toLong))
            case FloatType => SumPoolingAggregate.remove(aggregate.asInstanceOf[Seq[Double]], valueRecord.asInstanceOf[Seq[Float]].map(i => i.toDouble))
            case LongType => SumPoolingAggregate.remove(aggregate.asInstanceOf[Seq[Long]], valueRecord.asInstanceOf[Seq[Long]])
            case DoubleType => SumPoolingAggregate.remove(aggregate.asInstanceOf[Seq[Double]], valueRecord.asInstanceOf[Seq[Double]])
            case _ => throw getInvalidDataTypeException(valueDataType)
          }
        }
        case _ => throw getInvalidDataTypeException(valueDataType)
      }
    }
  }

  private def getInvalidDataTypeException(dataType: DataType): Exception = {
    new RuntimeException(s"Invalid data type for ${aggregation.toString} value col $valueCol. " +
      s"Only Arrays of Int, Long, Float, and Double with non null elements are supported, but got ${dataType.prettyJson}")
  }
}

object SumPoolingAggregate {
  private def update[T](aggregate: Seq[T], record: Seq[T])(implicit numeric: Numeric[T]): Seq[T] = {
    require(aggregate.length == record.length, "Embeddings to pool are all expected to have equal length. " +
      s"Aggregate Embedding Length: ${aggregate.length}, Record Embedding Length: ${record.length}.")
    aggregate.zip(record).map { case (x, y) => numeric.plus(x, y) }
  }

  private def remove[T](aggregate: Seq[T], record: Seq[T])(implicit numeric: Numeric[T]): Seq[T] = {
    require(aggregate.length == record.length, "Embeddings to pool are all expected to have equal length. " +
      s"Aggregate Embedding Length: ${aggregate.length}, Record Embedding Length: ${record.length}.")
    aggregate.zip(record).map { case (x, y) => numeric.minus(x, y) }
  }
}


