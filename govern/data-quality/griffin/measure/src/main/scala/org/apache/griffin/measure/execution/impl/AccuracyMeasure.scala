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
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure
import org.apache.griffin.measure.utils.CommonUtils.safeReduce

/**
 * Accuracy Measure.
 *
 * Data accuracy refers to the degree to which the values of a said attribute in a data source agree
 * with an identified reference truth data (source of correct information).
 * In-accurate data may come from different sources like,
 *   - Dynamically computed values,
 *   - the result of a manual workflow,
 *   - irate customers, etc.
 *
 * Accuracy measure quantifies the extent to which data sets contains are correct, reliable and certified
 * values that are free of error. Higher accuracy values signify that the said data set represents
 * the "real-life" values/ objects that it intends to model.
 *
 * Accuracy measure is comparative in nature - attributes of data source to be checked are compared with
 * attributes of another reference source. Thus, unlike other measures/ dimensions, Accuracy
 * relies on definition of 2 sources,
 *   - the reference (truth) source which contains the good/ correct/ accurate values.
 *   - the actual data source to be assessed and measured for data accuracy.
 *
 * @param sparkSession SparkSession for this Griffin Application.
 * @param measureParam Object representation of this measure and its configuration.
 */
case class AccuracyMeasure(sparkSession: SparkSession, measureParam: MeasureParam)
    extends Measure {

  /**
   * Representation of a single accuracy expression object.
   *
   * @param sourceCol name of source column
   * @param refCol name of reference column
   */
  final case class AccuracyExpr(sourceCol: String, refCol: String)

  import AccuracyMeasure._
  import Measure._

  /**
   * Accuracy measure supports record and metric write
   */
  override val supportsRecordWrite: Boolean = true

  override val supportsMetricWrite: Boolean = true

  /**
   * The value for expr is a json array of comparison objects where each object has 2 fields -
   * `source.col` and `ref.col` which must be actual columns in the source and reference data sets respectively.
   * This key is mandatory and expr array must not be empty i.e. at least one comparison must be defined.
   */
  val exprOpt: Option[Seq[Map[String, String]]] =
    Option(getFromConfig[Seq[Map[String, String]]](Expression, null))

  /**
   * This is a mandatory parameter which selects the data source which will be used as reference.
   * This is a mandatory parameter and this data source must be defined in the sources section
   * of the application configuration.
   */
  val refSource: String = getFromConfig[String](ReferenceSourceStr, null)

  validate()

  /**
   * Performs a measurement of common values as a join between the mapped columns of the reference and source
   * data sets.
   *
   * Accuracy produces the following 3 metrics as result,
   *  - Total records
   *  - Accurate records
   *  - In accurate records
   *
   *  @return tuple of records dataframe and metric dataframe
   */
  override def impl(input: DataFrame): (DataFrame, DataFrame) = {
    val originalCols = input.columns

    val dataSource = addColumnPrefix(input, SourcePrefixStr)

    val refDataSource =
      addColumnPrefix(sparkSession.read.table(refSource), refPrefixStr)

    val expr = exprOpt.getOrElse(throw new AssertionError(s"'$Expression' must be defined."))
    val accuracyExprs = expr
      .map(toAccuracyExpr)
      .distinct
      .map(x => AccuracyExpr(s"$SourcePrefixStr${x.sourceCol}", s"$refPrefixStr${x.refCol}"))

    val joinExpr = safeReduce(
      accuracyExprs
        .map(e => col(e.sourceCol) === col(e.refCol)))(_ and _)

    val indicatorExpr =
      safeReduce(
        accuracyExprs
          .map(e =>
            coalesce(col(e.sourceCol), emptyCol) notEqual coalesce(col(e.refCol), emptyCol)))(
        _ or _)

    val nullExpr = safeReduce(accuracyExprs.map(e => col(e.sourceCol).isNull))(_ or _)

    val cols = accuracyExprs.map(_.refCol).map(col)
    val window = Window.partitionBy(cols: _*).orderBy(cols: _*)

    val recordsDf = removeColumnPrefix(
      dataSource
        .join(refDataSource.withColumn(RowNumber, row_number().over(window)), joinExpr, "left")
        .where(col(RowNumber) === 1 or col(RowNumber).isNull)
        .withColumn(valueColumn, when(indicatorExpr or nullExpr, 1).otherwise(0)),
      SourcePrefixStr)
      .select((originalCols :+ valueColumn).map(col): _*)

    val selectCols =
      Seq(Total, AccurateStr, InAccurateStr).map(e =>
        map(lit(MetricName), lit(e), lit(MetricValue), nullToZero(col(e).cast(StringType))))
    val metricColumn: Column = array(selectCols: _*).as(valueColumn)

    val metricDf = recordsDf
      .withColumn(Total, lit(1))
      .agg(sum(Total).as(Total), sum(valueColumn).as(InAccurateStr))
      .withColumn(AccurateStr, col(Total) - col(InAccurateStr))
      .select(metricColumn)

    (recordsDf, metricDf)
  }

  /**
   * JSON representation of the  `expr` is deserialized as Map internally which is now converted to an
   * `AccuracyExpr` representation for a fixed structure across all expression object(s).
   *
   * @param map map representation of the `expr`
   * @return instance of `AccuracyExpr`
   */
  private def toAccuracyExpr(map: Map[String, String]): AccuracyExpr = {
    assert(map.contains(SourceColStr), s"'$SourceColStr' must be defined.")
    assert(map.contains(ReferenceColStr), s"'$ReferenceColStr' must be defined.")

    AccuracyExpr(map(SourceColStr), map(ReferenceColStr))
  }

  /**
   * Validates if the expression is not null and non empty along with some dataset specific validations.
   */
  override def validate(): Unit = {
    val expr = exprOpt.getOrElse(throw new AssertionError(s"'$Expression' must be defined."))
    assert(expr.flatten.nonEmpty, s"'$Expression' must not be empty or of invalid type.")

    assert(
      !StringUtil.isNullOrEmpty(refSource),
      s"'$ReferenceSourceStr' must not be null, empty or of invalid type.")

    assert(
      sparkSession.catalog.tableExists(refSource),
      s"Reference source with name '$refSource' does not exist.")

    val datasourceName = measureParam.getDataSource

    val dataSourceCols = sparkSession.read.table(datasourceName).columns.toSet
    val refDataSourceCols = sparkSession.read.table(refSource).columns.toSet

    val accuracyExpr = expr.map(toAccuracyExpr).distinct
    val (forDataSource, forRefDataSource) =
      accuracyExpr
        .map(
          e =>
            (
              (e.sourceCol, dataSourceCols.contains(e.sourceCol)),
              (e.refCol, refDataSourceCols.contains(e.refCol))))
        .unzip

    val invalidColsDataSource = forDataSource.filterNot(_._2)
    val invalidColsRefSource = forRefDataSource.filterNot(_._2)

    assert(
      invalidColsDataSource.isEmpty,
      s"Column(s) [${invalidColsDataSource.map(_._1).mkString(", ")}] " +
        s"do not exist in data set with name '$datasourceName'")

    assert(
      invalidColsRefSource.isEmpty,
      s"Column(s) [${invalidColsRefSource.map(_._1).mkString(", ")}] " +
        s"do not exist in reference data set with name '$refSource'")
  }

  /**
   * Helper method to prepend a prefix to all column names to uniquely identify them.
   * In case if they exist in both source and target data sets there is no collision.
   *
   * @param dataFrame data set
   * @param prefix prefix to set
   * @return
   */
  private def addColumnPrefix(dataFrame: DataFrame, prefix: String): DataFrame = {
    val columns = dataFrame.columns
    columns.foldLeft(dataFrame)((df, c) => df.withColumnRenamed(c, s"$prefix$c"))
  }

  /**
   * Helper method to strip a prefix from all column names that previously helped in uniquely identify them.
   *
   * @param dataFrame data set
   * @param prefix prefix to remove
   * @return
   */
  private def removeColumnPrefix(dataFrame: DataFrame, prefix: String): DataFrame = {
    val columns = dataFrame.columns
    columns.foldLeft(dataFrame)((df, c) => df.withColumnRenamed(c, c.stripPrefix(prefix)))
  }
}

/**
 * Accuracy measure constants
 */
object AccuracyMeasure {
  final val SourcePrefixStr: String = "__source_"
  final val refPrefixStr: String = "__ref_"

  final val ReferenceSourceStr: String = "ref.source"
  final val SourceColStr: String = "source.col"
  final val ReferenceColStr: String = "ref.col"

  final val AccurateStr: String = "accurate"
  final val InAccurateStr: String = "inaccurate"
}
