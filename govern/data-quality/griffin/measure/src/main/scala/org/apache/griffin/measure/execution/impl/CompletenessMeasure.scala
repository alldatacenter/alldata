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
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure

/**
 * Completeness Measure.
 *
 * Completeness refers to the degree to which values are present in a data collection.
 * When data is incomplete due to unavailability (missing records), this does not represent a lack of completeness.
 * As far as an individual datum is concerned, only two situations are possible - either a value is assigned
 * to the attribute in question or not. The latter case is usually represented by a `null` value.
 *
 * @param sparkSession SparkSession for this Griffin Application.
 * @param measureParam Object representation of this measure and its configuration.
 */
case class CompletenessMeasure(sparkSession: SparkSession, measureParam: MeasureParam)
    extends Measure {

  import CompletenessMeasure._
  import Measure._

  /**
   * Completeness measure supports record and metric write
   */
  override val supportsRecordWrite: Boolean = true

  override val supportsMetricWrite: Boolean = true

  /**
   * The value for expr is a SQL-like expression string which definition this completeness.
   * For more complex definitions, expressions can be clubbed with AND and OR.
   *
   * For a tabular data set with columns name, email and age, some examples of `expr` are mentioned below,
   *   - name is NULL
   *   - name is NULL and age is NULL
   *   - email NOT RLIKE `'^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$'` (without ` and `)
   * Note: This expression describes the bad or incomplete records. This means that
   * for "expr": "zipcode is NULL" the records which contain null in zipcode column are considered as incomplete.
   */
  val exprOpt: Option[String] = Option(getFromConfig[String](Expression, null))

  validate()

  /**
   * Completeness evaluates the user provided `expr` for each row of the input dataset.
   * Each row that fails this expression is tagged as incomplete record(s), all other record(s) are complete.
   *
   * Completeness produces the following 3 metrics as result,
   *  - Total records
   *  - Complete records
   *  - Incomplete records
   *
   *  @return tuple of records dataframe and metric dataframe
   */
  override def impl(input: DataFrame): (DataFrame, DataFrame) = {
    val exprStr = exprOpt.getOrElse(throw new AssertionError(s"'$Expression' must be defined."))

    val selectCols =
      Seq(Total, Complete, InComplete).map(e =>
        map(lit(MetricName), lit(e), lit(MetricValue), nullToZero(col(e).cast(StringType))))
    val metricColumn: Column = array(selectCols: _*).as(valueColumn)

    val badRecordsDf = input.withColumn(valueColumn, when(expr(exprStr), 0).otherwise(1))

    val metricDf = badRecordsDf
      .withColumn(Total, lit(1))
      .agg(sum(Total).as(Total), sum(valueColumn).as(InComplete))
      .withColumn(Complete, col(Total) - col(InComplete))
      .select(metricColumn)

    (badRecordsDf, metricDf)
  }

  /**
   * Validates if expression is defined and is non empty.
   */
  override def validate(): Unit = {
    val expr = exprOpt.getOrElse(throw new AssertionError(s"'$Expression' must be defined."))
    assert(!StringUtil.isNullOrEmpty(expr), s"'$Expression' must not be null or empty.")
  }
}

object CompletenessMeasure {

  /**
   * Completeness Constants
   */
  final val Complete: String = "complete"
  final val InComplete: String = "incomplete"
}
