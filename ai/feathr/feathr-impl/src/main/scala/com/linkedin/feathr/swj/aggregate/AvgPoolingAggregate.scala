package com.linkedin.feathr.swj.aggregate

import com.linkedin.feathr.swj.join.FeatureColumnMetaData
import AggregationType._
import org.apache.spark.sql.types._

/**
  * AVG_POOLING aggregation implementation.
  *
  * @param valueCol Name of the metric column or a Spark SQL column expression for derived metric
  *                 that will be aggregated using AVG_POOLING. This operation differs from AVG in that this
  *                 applies the AVG operation across an entire Seq of Numerics instead of a single value
  * @param countCol Name of the column that keeps track of the count which is required for all aggregation functions
  *                 that support deagg in order to identify when all records have been deagged out.
  *                 This is needed for pre-aggregated fact dataset. For non-aggregated dataset,
  *                 if the column name is not provided, us the default column expression lit(1).
  */
class AvgPoolingAggregate(valueCol: String, countCol: String = "1") extends SumPoolingAggregate(valueCol, countCol) {
  override def aggregation: AggregationType = AVG_POOLING

  override def metricName = "avg_pooling_col"

  override def isCalculateAggregateNeeded = true

  override def calculateAggregateWithCount(aggregate: Any, valueDataType: DataType, count: Any, countDataType: DataType): Any = {
    if (aggregate == null || count == null) {
      null
    } else {
      // Our parent SumPoolingAggregate also has isCalculateAggregateNeeded as true so we need to make sure we also delegate to that aggregation
      val calculatedValueAggregate = super.calculateAggregateWithCount(aggregate, valueDataType, count, countDataType)

      val countAsDouble = countDataType match {
        case IntegerType => count.asInstanceOf[Int].toDouble
        case LongType => count.asInstanceOf[Long].toDouble
        case _ => throw getInvalidCountDataTypeException(countDataType)
      }

      valueDataType match {
        case arrayType: ArrayType => {
          arrayType.elementType match {
            case IntegerType => AvgPoolingAggregate.calculateAvg(calculatedValueAggregate.asInstanceOf[Seq[Int]], countAsDouble)
            case LongType => AvgPoolingAggregate.calculateAvg(calculatedValueAggregate.asInstanceOf[Seq[Long]], countAsDouble)
            case FloatType => AvgPoolingAggregate.calculateAvg(calculatedValueAggregate.asInstanceOf[Seq[Float]], countAsDouble)
            case DoubleType => AvgPoolingAggregate.calculateAvg(calculatedValueAggregate.asInstanceOf[Seq[Double]], countAsDouble)
            case _ => throw getInvalidValueDataTypeException(valueDataType)
          }
        }
        case _ => throw getInvalidValueDataTypeException(valueDataType)
      }
    }
  }

  override def updateAggDataType(featureColumnMetaData: FeatureColumnMetaData): Unit = {
    featureColumnMetaData.aggDataType = ArrayType(DoubleType)
  }

  private def getInvalidValueDataTypeException(dataType: DataType): Exception = {
    new RuntimeException(s"Invalid data type for ${aggregation.toString} value col $valueCol. " +
      s"Only Arrays of Int, Long, Float, and Double with non null elements are supported, but got ${dataType.prettyJson}")
  }

  private def getInvalidCountDataTypeException(dataType: DataType): Exception = {
    new RuntimeException(s"Invalid data type for ${aggregation.toString} count col $countCol. " +
      s"Only Int and Long are supported, but got ${dataType.prettyJson}")
  }
}

object AvgPoolingAggregate {
  private def calculateAvg[T](pooledSum: Seq[T], countAsDouble: Double)(implicit numericSum: Numeric[T]): Seq[Double] = {
    pooledSum.map { x => numericSum.toDouble(x) / countAsDouble }
  }
}


