package com.linkedin.feathr.offline.generation.aggregations

import com.linkedin.feathr.common.Params
import com.linkedin.feathr.common.configObj.ConfigObj
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.sparkcommon.ComplexAggregation
import com.typesafe.config.Config
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction, Window}
import org.apache.spark.sql.functions.{col, first}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, DataFrame, Row}

import scala.collection.JavaConverters._

/**
 * A wrapper for class for the min pooling UDAF to conform to new API. The calculation is done
 * in getIntermediateColumns by calling the UDAF, and the first result is returned in getAggColumn.
 * No post processing is needed.
 */
private[offline] class MinPooling extends ComplexAggregation {
  var minPoolingDefConfig: MinPoolingDefConfig = _
  var minPoolingUDAF: MinPoolingUDAF = new MinPoolingUDAF

  override def init(configObj: ConfigObj): Unit = {
    minPoolingDefConfig = configObj.asInstanceOf[MinPoolingDefConfig]
    minPoolingUDAF.init(minPoolingDefConfig._params.get)
  }

  // The intermediate column is the min pooling column where we store the result.
  override def getIntermediateColumnNames(): Seq[String] = {
    Seq(minPoolingDefConfig.minPoolingCol)
  }

  // Applies the min pooling UDAF on the def column
  override def getIntermediateColumns(dataFrameWithKeyColumns: DataFrame): Seq[(String, Column)] = {
    Seq(
      (
        minPoolingDefConfig.minPoolingCol,
        minPoolingUDAF(dataFrameWithKeyColumns.col(minPoolingDefConfig.defCol))
          .over(Window.partitionBy(minPoolingDefConfig.featureKeys.head, minPoolingDefConfig.featureKeys.tail: _*))))
  }

  // Since each row will have the same aggregated value, return the first.
  override def getAggColumn(): Column = {
    first(col(minPoolingDefConfig.minPoolingCol)).as(minPoolingDefConfig.featureName)
  }
}

// Definition config for min pooling.
private[offline] class MinPoolingDefConfig extends ConfigObj with Params {

  // Params passed in from Feathr config
  var featureKeys: Seq[String] = _
  var defCol: String = _
  var featureName: String = _
  var embeddingSize: Int = _
  var window: String = _

  // derived column names
  var minPoolingCol: String = _

  override def init(params: Config): Unit = {
    super.init(params)

    defCol = _params.get.getString("def")
    featureKeys = _params.get.getStringList("key").asScala
    featureName = _params.get.getString("featureName")
    embeddingSize = _params.get.getNumber("embeddingSize").intValue()
    window = _params.get.getString("window")

    minPoolingCol = s"${defCol}_${window}"
  }
}

/**
 * A Spark UDAF for computing min pooling of a group of embedding vectors.
 *
 * The min pooling of k embedding vectors are the element-wise min value of all
 * k vectors.
 */
private[offline] class MinPoolingUDAF extends UserDefinedAggregateFunction with Params {

  var embeddingSize = 0

  /**
   * Initialize the parameters from Feathr config.
   * This UDAF needs 1 extra parameter "embeddingSize".
   * @param params A Feathr Params map.
   */
  override def init(params: Config): Unit = {
    super.init(params)
    embeddingSize = _params.get.getInt("embeddingSize")
  }

  /**
   * Input schema of this UDAF.
   * This UDAF accepts 1 column of embedding vectors as input.
   */
  override def inputSchema: org.apache.spark.sql.types.StructType =
    StructType(Seq(StructField("value", ArrayType(DoubleType, false))))

  /**
   * Schema for the buffer Row of this UDAF.
   * This UDAF keeps track of the element-wise max of all embedding vector.
   */
  override def bufferSchema: StructType = StructType(Seq(StructField("agg", ArrayType(DoubleType, false))))

  /**
   * Output data type.
   * This UDAF outputs 1 aggregated embedding vector, as an array of double.
   */
  override def dataType: DataType = ArrayType(DoubleType, false)

  override def deterministic: Boolean = true

  /**
   * Initialize the buffer.
   */
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = Seq.fill(embeddingSize)(Double.MaxValue)
  }

  /**
   * Update the buffer with 1 input row.
   */
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    calculate(buffer, input)
  }

  /**
   * Merge 2 aggregation buffers.
   * For this UDAF, we take the min of 2 aggregated vectors.
   */
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    calculate(buffer1, buffer2)
  }

  /**
   * Output the final aggregated value from the buffers.
   */
  override def evaluate(buffer: Row): Any = {
    buffer.getAs[Seq[Double]](0)
  }

  /**
   * Given the current state of the buffer and a new input row,
   * update the buffer with the value in the input row.
   *
   * For this UDAF, we calculate the element-wise min of the buffer and new input.
   */
  private def calculate(buffer: MutableAggregationBuffer, row: Row): Unit = {
    val embedding = row.getAs[Seq[Double]](0)
    val aggregate = buffer.getAs[Seq[Number]](0).map(x => x.doubleValue())
    if (embedding != null) {
      if (embedding.size != embeddingSize) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"embedding vector size has a length of ${embedding.size}, different from expected size ${embeddingSize}")
      }
      val newAgg = aggregate.zip(embedding).map { case (x, y) => Math.min(x, y) }
      buffer.update(0, newAgg)
    }
  }
}
