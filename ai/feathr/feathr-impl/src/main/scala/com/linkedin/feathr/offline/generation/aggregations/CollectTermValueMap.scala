package com.linkedin.feathr.offline.generation.aggregations

import com.linkedin.feathr.common.Params
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureTransformationException}
import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types._

private[offline] object CollectTermValueMap {
  val ALLOW_DUPLICATES = "allowDuplicates"
}

/**
 * A Spark UDAF for collecting a column of key and a column of value into
 * a term-value map (term vector), represented by Map[String, Float].
 */
private[offline] class CollectTermValueMap extends UserDefinedAggregateFunction with Params {

  /*
   * for most aggregations we should not allow duplicate value for the same term, however,
   * 'LATEST' aggregation have duplicate value by definition and we should just override the old value,
   * instead of throwing exception
   */
  private var allowDuplicates: Boolean = false

  /**
   * Input schema of this UDAF.
   * This UDAF accepts 2 columns of values as input:
   * a column of Strings as key (terms),
   * and a column of Floats as value.
   */
  override def inputSchema: org.apache.spark.sql.types.StructType =
    StructType(Seq(StructField("key", StringType), StructField("value", FloatType)))

  /**
   * Schema for the buffer Row of this UDAF.
   * This UDAF stores a Map of String -> Float as buffer.
   */
  override def bufferSchema: StructType = StructType(Seq(StructField("map", MapType(StringType, FloatType))))

  /**
   * Output data type.
   * This UDAF outputs the aggregated term-value map.
   */
  override def dataType: DataType = MapType(StringType, FloatType)

  override def deterministic: Boolean = true

  /**
   * Initialize the buffer.
   */
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = Map()
    allowDuplicates = _params
      .collect {
        case param if param.hasPath(CollectTermValueMap.ALLOW_DUPLICATES) =>
          param.getBoolean(CollectTermValueMap.ALLOW_DUPLICATES)
      }
      .getOrElse(false)
  }

  /**
   * Update the buffer with 1 input row.
   * Note: Spark uses Scala immutable.Map as the concrete type for MapType. As a
   * result, each time this method is called, the map in the buffer will be deserialized,
   * copied into a new immutable.Map for updating a new term-value pair, and serialized
   * back to the buffer. Such operation would affect the performance if the amount of
   * term-value pair is large.
   */
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    if (input.get(1) != null) {
      val k = input.getAs[String](0)
      val v = input.getAs[Number](1).floatValue() // would convert other numeric type to float
      val bufferMap = buffer.getAs[Map[String, Float]](0)
      // When collecting term-value pairs, different rows with the same term should always have
      // the same value. Throw an error if otherwise.
      if (!allowDuplicates && bufferMap.contains(k) && (!bufferMap(k).equals(v))) {
        throw new FeathrFeatureTransformationException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Error during collectTermValueMap: Same term ${k} appeared twice with different value ${v} and ${bufferMap(k)}")
      }
      val newMap = bufferMap.updated(k, v)
      buffer.update(0, newMap)
    }
  }

  /**
   * Merge 2 aggregation buffers.
   * For this UDAF, we merge the 2 collected map together and write it back into buffer1.
   * Note: each time this method is called, the maps from buffer1 and buffer2 would be
   * copied into a new Map, before being serialized and written back to buffer1. Such
   * operation would affect the performance if the amount of term-value pair is large.
   */
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    val bufferMap1 = buffer1.getAs[Map[String, Float]](0)
    val bufferMap2 = buffer2.getAs[Map[String, Float]](0)
    buffer1.update(0, bufferMap1 ++ bufferMap2)
  }

  /**
   * Output the final aggregated value from the buffers.
   * This UDAF returns the collected term-value map.
   */
  override def evaluate(buffer: Row): Any = {
    buffer.getAs[Map[String, Float]](0)
  }
}
