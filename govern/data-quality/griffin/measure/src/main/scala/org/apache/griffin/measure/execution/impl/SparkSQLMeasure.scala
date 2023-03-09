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
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions.{expr => sparkExpr, _}
import org.apache.spark.sql.types.{BooleanType, StringType}

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure

/**
 * SparkSQL Measure.
 *
 * In some cases, the pre-defined dimensions/ measures may not enough to model a complete
 * data quality definition. For such cases, Apache Griffin allows the definition of complex
 * custom user-defined checks as SparkSQL queries.
 *
 * SparkSQL measure is like a pro mode that allows advanced users to configure complex custom checks
 * that are not covered by other measures. These SparkSQL queries may contain clauses like
 * select, from, where, group-by, order-by , limit, join, etc.
 *
 * @param sparkSession SparkSession for this Griffin Application.
 * @param measureParam Object representation of this measure and its configuration.
 */
case class SparkSQLMeasure(sparkSession: SparkSession, measureParam: MeasureParam)
    extends Measure {

  import CompletenessMeasure._
  import Measure._

  /**
   * SparkSQL measure supports record and metric write
   */
  override val supportsRecordWrite: Boolean = true

  override val supportsMetricWrite: Boolean = true

  /**
   * The value for expr is a valid SparkSQL query string. This is a mandatory parameter.
   */
  private val expr = getFromConfig[String](Expression, StringUtils.EMPTY)

  /**
   * As the key suggests, its value defines what exactly would be considered as a bad record
   * after this query executes. In order to separate the good data from bad data, a
   * bad.record.definition expression must be set. This expression can be a SparkSQL like
   * expression and must yield a column with boolean data type.
   *
   * Note: This expression describes the bad records, i.e. if bad.record.definition = true
   * for a record, it is marked as bad/ incomplete record.
   */
  private val badnessExpr = getFromConfig[String](BadRecordDefinition, StringUtils.EMPTY)

  validate()

  /**
   * Runs the user provided SparkSQL query and marks the records as complete/ incomplete based on the
   * `BadRecordDefinition`.
   *
   * SparkSQL produces the following 3 metrics as result,
   *  - Total records
   *  - Complete records
   *  - Incomplete records
   *
   *  @return tuple of records dataframe and metric dataframe
   */
  override def impl(dataSource: DataFrame): (DataFrame, DataFrame) = {
    val df = dataSource.sparkSession.sql(expr).withColumn(valueColumn, sparkExpr(badnessExpr))

    assert(
      df.schema.exists(f => f.name.matches(valueColumn) && f.dataType.isInstanceOf[BooleanType]),
      s"Invalid condition provided as $BadRecordDefinition. Does not yield a boolean result.")

    val selectCols =
      Seq(Total, Complete, InComplete).map(e =>
        map(lit(MetricName), lit(e), lit(MetricValue), nullToZero(col(e).cast(StringType))))
    val metricColumn: Column = array(selectCols: _*).as(valueColumn)

    val badRecordsDf = df.withColumn(valueColumn, when(col(valueColumn), 1).otherwise(0))
    val metricDf = badRecordsDf
      .withColumn(Total, lit(1))
      .agg(sum(Total).as(Total), sum(valueColumn).as(InComplete))
      .withColumn(Complete, col(Total) - col(InComplete))
      .select(metricColumn)

    (badRecordsDf, metricDf)
  }

  /**
   * Validates if the `Expression` and `BadRecordDefinition` are not null and non empty.
   */
  override def validate(): Unit = {
    assert(
      !StringUtil.isNullOrEmpty(expr),
      "Invalid query provided as expr. Must not be null, empty or of invalid type.")

    assert(
      !StringUtil.isNullOrEmpty(badnessExpr),
      "Invalid condition provided as bad.record.definition.")
  }
}
