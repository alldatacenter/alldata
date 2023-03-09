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

import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DataType, StringType}

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure
import org.apache.griffin.measure.utils.CommonUtils.safeReduce

/**
 * SchemaConformance Measure.
 *
 * Schema Conformance ensure that the attributes of a given dataset follow a set of
 * standard data definitions in terms of data type.
 * Most binary file formats (orc, avro, etc.) and tabular sources (Hive, RDBMS, etc.)
 * already impose type constraints on the data they represent but text based formats like
 * csv, json, xml, etc. do not retain schema information.
 *
 * For example, date of birth of customer should be a date, age should be an integer.
 *
 * @param sparkSession SparkSession for this Griffin Application.
 * @param measureParam Object representation of this measure and its configuration.
 */
case class SchemaConformanceMeasure(sparkSession: SparkSession, measureParam: MeasureParam)
    extends Measure {

  import CompletenessMeasure._
  import Measure._

  /**
   * Representation of a single SchemaConformance expression object.
   *
   * @param sourceCol name of source column
   * @param dataType name of reference column
   */
  final case class SchemaConformanceExpr(sourceCol: String, dataType: DataType)

  /**
   * SchemaConformance Constants
   */
  final val SourceColStr: String = "source.col"
  final val DataTypeStr: String = "type"

  /**
   * SchemaConformance measure supports record and metric write
   */
  override val supportsRecordWrite: Boolean = true

  override val supportsMetricWrite: Boolean = true

  /**
   * The value for expr is a json array of mapping objects where each object has 2 fields -
   * `source.col` and `type`. Each `source.col` must exist in the source data set and is checked
   * to be of type `type`.
   * This key is mandatory and expr array must not be empty i.e. at least one mapping must be defined.
   */
  val exprOpt: Option[Seq[Map[String, String]]] =
    Option(getFromConfig[Seq[Map[String, String]]](Expression, null))

  validate()

  /**
   * SchemaConformance measure evaluates the user provided `expr` for each row of the input dataset.
   * Each row that fails this type expression is tagged as incomplete record(s), all other record(s) are complete.
   *
   * SchemaConformance produces the following 3 metrics as result,
   *  - Total records
   *  - Complete records
   *  - Incomplete records
   *
   *  @return tuple of records dataframe and metric dataframe
   */
  override def impl(input: DataFrame): (DataFrame, DataFrame) = {
    val expr = exprOpt.getOrElse(throw new AssertionError(s"'$Expression' must be defined."))
    val givenExprs = expr.map(toSchemaConformanceExpr).distinct

    val incompleteExpr = safeReduce(
      givenExprs
        .map(e =>
          when(col(e.sourceCol).cast(StringType).cast(e.dataType).isNull, true)
            .otherwise(false)))(_ or _)

    val selectCols =
      Seq(Total, Complete, InComplete).map(e =>
        map(lit(MetricName), lit(e), lit(MetricValue), nullToZero(col(e).cast(StringType))))
    val metricColumn: Column = array(selectCols: _*).as(valueColumn)

    val badRecordsDf = input.withColumn(valueColumn, when(incompleteExpr, 1).otherwise(0))

    val metricDf = badRecordsDf
      .withColumn(Total, lit(1))
      .agg(sum(Total).as(Total), sum(valueColumn).as(InComplete))
      .withColumn(Complete, col(Total) - col(InComplete))
      .select(metricColumn)

    (badRecordsDf, metricDf)
  }

  /**
   * JSON representation of the  `expr` is deserialized as Map internally which is now converted to an
   * `SchemaConformanceExpr` representation for a fixed structure across all expression object(s).
   *
   * @param map map representation of the `expr`
   * @return instance of `SchemaConformanceExpr`
   */
  private def toSchemaConformanceExpr(map: Map[String, String]): SchemaConformanceExpr = {
    assert(map.contains(SourceColStr), s"'$SourceColStr' must be defined.")
    assert(map.contains(DataTypeStr), s"'$DataTypeStr' must be defined.")

    SchemaConformanceExpr(map(SourceColStr), DataType.fromDDL(map(DataTypeStr)))
  }

  /**
   * Validates if the expression is not null and non empty along with some dataset specific validations.
   */
  override def validate(): Unit = {
    val expr = exprOpt.getOrElse(throw new AssertionError(s"'$Expression' must be defined."))
    assert(expr.flatten.nonEmpty, s"'$Expression' must not be empty or of invalid type.")

    val datasourceName = measureParam.getDataSource

    val dataSourceCols = sparkSession.read.table(datasourceName).columns.toSet
    val schemaConformanceExprExpr = expr.map(toSchemaConformanceExpr).distinct

    val forDataSource =
      schemaConformanceExprExpr.map(e => (e.sourceCol, dataSourceCols.contains(e.sourceCol)))
    val invalidColsDataSource = forDataSource.filterNot(_._2)

    assert(
      invalidColsDataSource.isEmpty,
      s"Column(s) [${invalidColsDataSource.map(_._1).mkString(", ")}] " +
        s"do not exist in data set with name '$datasourceName'")
  }

}
