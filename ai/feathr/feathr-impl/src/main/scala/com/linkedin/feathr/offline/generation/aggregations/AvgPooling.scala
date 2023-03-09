package com.linkedin.feathr.offline.generation.aggregations

import com.linkedin.feathr.common.Params
import com.linkedin.feathr.common.configObj.ConfigObj
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.sparkcommon.ComplexAggregation
import com.typesafe.config.Config
import org.apache.spark.sql._
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import scala.collection.JavaConverters._

/**
 * A wrapper for class for the average pooling UDAF to conform to new API. The calculation is done
 * in getIntermediateColumns by calling the UDAF, and the first result is returned in getAggColumn.
 * No post processing is needed.
 */
private[offline] class AvgPooling extends ComplexAggregation {
  var avgPoolingDefConfig: AvgPoolingDefConfig = _
  var avgPoolingUDAF: AvgPoolingUDAF = new AvgPoolingUDAF

  override def init(configObj: ConfigObj): Unit = {
    avgPoolingDefConfig = configObj.asInstanceOf[AvgPoolingDefConfig]
    avgPoolingUDAF.init(avgPoolingDefConfig._params.get)
  }

  // The intermediate column is the avg pooling column where we store the result.
  override def getIntermediateColumnNames(): Seq[String] = {
    Seq(avgPoolingDefConfig.avgPoolingCol)
  }

  // Applies the average pooling UDAF on the def column
  override def getIntermediateColumns(dataFrameWithKeyColumns: DataFrame): Seq[(String, Column)] = {
    Seq(
      (
        avgPoolingDefConfig.avgPoolingCol,
        avgPoolingUDAF(dataFrameWithKeyColumns.col(avgPoolingDefConfig.defCol))
          .over(Window.partitionBy(avgPoolingDefConfig.featureKeys.head, avgPoolingDefConfig.featureKeys.tail: _*))))
  }

  // Since each row will have the same aggregated value, return the first.
  override def getAggColumn(): Column = {
    first(col(avgPoolingDefConfig.avgPoolingCol)).as(avgPoolingDefConfig.featureName)
  }
}

// Definition config for average pooling.
private[offline] class AvgPoolingDefConfig extends ConfigObj with Params {

  // Params passed in from Feathr config
  var featureKeys: Seq[String] = _
  var defCol: String = _
  var featureName: String = _
  var embeddingSize: Int = _
  var window: String = _

  // derived column names
  var avgPoolingCol: String = _

  override def init(params: Config): Unit = {
    super.init(params)

    defCol = _params.get.getString("def")
    featureKeys = _params.get.getStringList("key").asScala
    featureName = _params.get.getString("featureName")
    embeddingSize = _params.get.getNumber("embeddingSize").intValue()
    window = _params.get.getString("window")

    avgPoolingCol = s"${defCol}_${window}"
  }
}

/**
 * A Spark UDAF for computing average pooling of a group of embedding vectors.
 *
 * The average pooling of k embedding vectors are the element-wise sum of all
 * k vectors divided by k.
 */
private[offline] class AvgPoolingUDAF extends UserDefinedAggregateFunction with Params {

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
   * This UDAF keeps track of the element-wise sum of all embedding vector,
   * as well as the number of vectors.
   */
  override def bufferSchema: StructType = StructType(Seq(StructField("sum", ArrayType(DoubleType, false)), StructField("count", LongType)))

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
    buffer(0) = Seq.fill(embeddingSize)(0.0d)
    buffer(1) = 0L
  }

  /**
   * Update the buffer with 1 input row.
   */
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    calculate(buffer, input)
  }

  /**
   * Merge 2 aggregation buffers.
   * For this UDAF, the sum vectors of 2 buffers would be added up, as well as
   * the counts.
   */
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    val sum1 = buffer1.getAs[Seq[Double]](0)
    val count1 = buffer1.getAs[Long](1)
    val sum2 = buffer2.getAs[Seq[Double]](0)
    val count2 = buffer2.getAs[Long](1)

    buffer1.update(0, sum1.zip(sum2).map { case (e1, e2) => e1 + e2 })
    buffer1.update(1, count1 + count2)
  }

  /**
   * Output the final aggregated value from the buffers.
   * This UDAF divides the sum vector with the count to get the average vector.
   */
  override def evaluate(buffer: Row): Any = {
    val sumVector = buffer.getAs[Seq[Double]](0)
    val count = buffer.getAs[Long](1)
    if (count == 0) {
      // Multiple scenarios can result in zero count, eg. filter statement filters out all records for the time window.
      // When count is 0, it won't make sense to compute the average, also dividing by zero will result in an array of NaN.
      // In the zero-count case, it essentially means the resulting feature data should be unset, thus we return null here.
      null
    } else {
      sumVector.map(x => x / count)
    }
  }

  /**
   * Given the current state of the buffer and a new input row,
   * update the buffer with the value in the input row.
   *
   * For this UDAF, we calculate the element-wise sum of the buffer and new input,
   * and increment the count.
   */
  private def calculate(buffer: MutableAggregationBuffer, row: Row): Unit = {
    val embedding = row.getAs[Seq[Double]](0)
    val aggregate = buffer.getAs[Seq[Number]](0).map(x => x.doubleValue())
    if (embedding != null) {
      val count = buffer.getAs[Long](1)
      if (embedding.size != embeddingSize) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"embedding vector size has a length of ${embedding.size}, different from expected size ${embeddingSize}")
      }
      val newAgg = aggregate.zip(embedding).map { case (x, y) => x + y }
      buffer.update(0, newAgg)
      buffer.update(1, count + 1)
    }
  }
}
