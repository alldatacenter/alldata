/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.execution.impl

import io.netty.util.internal.StringUtil
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure
import org.apache.griffin.measure.execution.Measure._

/**
 * Profiling measure.
 *
 * Data processing and its analysis can't truly be complete without data profiling -
 * reviewing source data for content and quality. Data profiling helps to find data quality rules and
 * requirements that will support a more thorough data quality assessment in a later step.
 *
 * The process of Data profiling involves:
 *   - Collecting descriptive statistics like min, max, count and sum
 *   - Collecting data types, length and recurring patterns
 *   - Discovering metadata and assessing its accuracy, etc.
 *
 * A common problem in data management circles is the confusion around what is meant by
 * Data profiling as opposed to Data Quality Assessment due to the interchangeable use of these 2 terms.
 *
 * Data Profiling helps us create a huge amount of insight into the quality levels of our
 * data and helps to find data quality rules and requirements that will support a more thorough
 * data quality assessment in a later step. For example, data profiling can help us to discover value
 * frequencies, formats and patterns for each attribute in the data asset. Using data profiling alone
 * we can find some perceived defects and outliers in the data asset, and we end up with a whole
 * range of clues based on which correct Quality assessment measures can be defined like
 * completeness/ distinctness etc.
 *
 * @param sparkSession SparkSession for this Griffin Application.
 * @param measureParam Object representation of this measure and its configuration.
 */
case class ProfilingMeasure(sparkSession: SparkSession, measureParam: MeasureParam)
    extends Measure {

  import ProfilingMeasure._

  /**
   * Profiling measure supports only metric write
   */
  override val supportsRecordWrite: Boolean = false

  override val supportsMetricWrite: Boolean = true

  /**
   * The value for `expr` is a comma separated string of columns in the data asset on which the
   * profiling measure is to be executed. `expr` is an optional key for Profiling measure,
   * i.e., if it is not defined, all columns in the data set will be profiled.
   */
  val exprs: String = getFromConfig[String](Expression, null)

  /**
   * Several resultant metrics of profiling measure are floating-point numbers. This key controls to extent
   * to which these floating-point numbers are rounded. For example, if `round.scale = 2` then all
   * floating-point metric values will be rounded to 2 decimal places.
   */
  val roundScale: Int = getFromConfig[java.lang.Integer](RoundScaleStr, 3)

  /**
   * The value of this key determines what percentage of data is to be profiled. The decimal value
   * belongs to range [0.0, 1.0], where 0.0 means the whole dataset will be skipped, 1.0 means the whole
   * dataset will be profiled. An intermediate value, say 0.5 will approximately take random 50% of
   * the dataset rows (without replacement) and perform profiling on it.
   *
   * This option can be used when the dataset to be profiled is large, and an approximate profile is needed.
   */
  val dataSetSample: Double = getFromConfig[java.lang.Double](DataSetSampleStr, 1.0)

  /**
   * The value for this key is boolean. If this is `true`, the distinct counts will be approximated
   * to allow up to 5% error. Approximate counts are usually faster by are less accurate. If this is set
   * to `false`, then the counts will be 100% accurate.
   */
  val approxDistinctCount: Boolean =
    getFromConfig[java.lang.Boolean](ApproxDistinctCountStr, true)

  validate()

  /**
   * Various metrics are calculated for columns of the data set. If expr is correctly defined,
   * then metrics are generated for only the given subset of columns else, its generated for all columns.
   *
   * List of profiling metrics that are generated,
   *  - avg_col_len
   *  - max_col_len
   *  - min_col_len
   *  - avg
   *  - max
   *  - min
   *  - approx_distinct_count OR distinct_count
   *  - variance
   *  - kurtosis
   *  - std_dev
   *  - total
   *  - data_type
   *
   *  @return tuple of records dataframe and metric dataframe
   */
  override def impl(dataSource: DataFrame): (DataFrame, DataFrame) = {
    info(s"Selecting random ${dataSetSample * 100}% of the rows for profiling.")
    val input = dataSource.sample(dataSetSample)
    val profilingColNames = keyCols(input)

    val profilingCols = input.schema.fields.filter(f => profilingColNames.contains(f.name))

    val profilingExprs = profilingCols.foldLeft(Array.empty[Column])((exprList, field) => {
      val colName = field.name
      val profilingExprs =
        getProfilingExprs(field, roundScale, approxDistinctCount, dataSetSample).map(nullToZero)

      exprList.:+(map(profilingExprs: _*).as(s"$DetailsPrefix$colName"))
    })

    val aggregateDf = profilingCols
      .foldLeft(input)((df, field) => {
        val colName = field.name
        val column = col(colName)

        val lengthColName = lengthColFn(colName)
        val nullColName = nullsInColFn(colName)

        df.withColumn(lengthColName, length(column))
          .withColumn(nullColName, when(isnull(column), 1L).otherwise(0L))
      })
      .agg(count(lit(1L)).as(Total), profilingExprs: _*)

    val detailCols =
      aggregateDf.columns
        .filter(_.startsWith(DetailsPrefix))
        .flatMap(c => Seq(map(lit(c.stripPrefix(DetailsPrefix)), col(c))))

    val metricDf = aggregateDf.select(array(detailCols: _*).as(valueColumn))

    (sparkSession.emptyDataFrame, metricDf)
  }

  override def validate(): Unit = {
    val input = sparkSession.read.table(measureParam.getDataSource)
    val kc = keyCols(input)

    assert(kc.nonEmpty, s"Columns defined in '$Expression' is empty.")
    kc.foreach(c =>
      assert(input.columns.contains(c), s"Provided column '$c' does not exist in the dataset."))

    assert(
      dataSetSample > 0.0d && dataSetSample <= 1.0d,
      "Sample fraction of rows must be in range [0.0, 1.0].")
  }

  private def keyCols(input: DataFrame): Array[String] = {
    if (StringUtil.isNullOrEmpty(exprs)) input.columns
    else exprs.split(",").map(_.trim)
  }.distinct

}

/**
 * Profiling measure constants
 */
object ProfilingMeasure {

  /**
   * Options Keys
   */
  final val DataSetSampleStr: String = "dataset.sample"
  final val RoundScaleStr: String = "round.scale"
  final val ApproxDistinctCountStr: String = "approx.distinct.count"

  /**
   * Structure Keys
   */
  final val ColumnDetails: String = "column_details"
  private final val DataTypeStr: String = "data_type"

  /**
   * Prefix Keys
   */
  private final val ApproxPrefix: String = "approx_"
  private final val DetailsPrefix: String = "details_"
  private final val ColumnLengthPrefix: String = "col_len"
  private final val IsNullPrefix: String = "is_null"

  /**
   * Column Detail Keys
   */
  private final val NullCount: String = "null_count"
  private final val DistinctCount: String = "distinct_count"
  private final val Min: String = "min"
  private final val Max: String = "max"
  private final val Avg: String = "avg"
  private final val StdDeviation: String = "std_dev"
  private final val Variance: String = "variance"
  private final val Kurtosis: String = "kurtosis"
  private final val MinColLength: String = s"${Min}_$ColumnLengthPrefix"
  private final val MaxColLength: String = s"${Max}_$ColumnLengthPrefix"
  private final val AvgColLength: String = s"${Avg}_$ColumnLengthPrefix"

  private def lengthColFn(colName: String): String = s"${ColumnLengthPrefix}_$colName"

  private def nullsInColFn(colName: String): String = s"${IsNullPrefix}_$colName"

  private def forNumericFn(t: DataType, value: Column, alias: String): Column = {
    (if (t.isInstanceOf[NumericType]) value else lit(null)).as(alias)
  }

  /**
   * Calculates profiling metrics for a column.
   *
   * @param field column
   * @param roundScale round off places.
   * @param approxDistinctCount to approximate distinct or not.
   * @return
   */
  private def getProfilingExprs(
      field: StructField,
      roundScale: Int,
      approxDistinctCount: Boolean,
      dataSetSample: Double): Seq[Column] = {
    val colName = field.name
    val colType = field.dataType

    val column = col(colName)
    val lengthColExpr = col(lengthColFn(colName))
    val nullColExpr = col(nullsInColFn(colName))
    val (distinctCountName, distinctCountExpr) =
      if (approxDistinctCount) {
        (
          lit(s"$ApproxPrefix$DistinctCount"),
          approx_count_distinct(column).as(s"$ApproxPrefix$DistinctCount"))
      } else {
        (lit(DistinctCount), countDistinct(column).as(DistinctCount))
      }

    val distinctExpr = if (dataSetSample == 1) {
      Seq(lit(distinctCountName), distinctCountExpr)
    } else Nil

    Seq(
      Seq(lit(DataTypeStr), lit(colType.catalogString).as(DataTypeStr)),
      Seq(lit(Total), sum(lit(1)).as(Total)),
      Seq(lit(MinColLength), min(lengthColExpr).as(MinColLength)),
      Seq(lit(MaxColLength), max(lengthColExpr).as(MaxColLength)),
      Seq(lit(AvgColLength), avg(lengthColExpr).as(AvgColLength)),
      Seq(lit(Min), forNumericFn(colType, min(column), Min)),
      Seq(lit(Max), forNumericFn(colType, max(column), Max)),
      Seq(lit(Avg), forNumericFn(colType, bround(avg(column), roundScale), Avg)),
      Seq(
        lit(StdDeviation),
        forNumericFn(colType, bround(stddev(column), roundScale), StdDeviation)),
      Seq(lit(Variance), forNumericFn(colType, bround(variance(column), roundScale), Variance)),
      Seq(lit(Kurtosis), forNumericFn(colType, bround(kurtosis(column), roundScale), Kurtosis)),
      distinctExpr,
      Seq(lit(NullCount), sum(nullColExpr).as(NullCount))).flatten
  }
}
