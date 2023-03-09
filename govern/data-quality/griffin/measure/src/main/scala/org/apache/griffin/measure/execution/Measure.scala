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

package org.apache.griffin.measure.execution

import scala.reflect.ClassTag

import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions._

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * Measure
 *
 * An abstraction for a data quality measure implementation.
 */
trait Measure extends Loggable {
  import Measure._

  /**
   * SparkSession for this Griffin Application.
   */
  val sparkSession: SparkSession

  /**
   * Object representation of user defined measure.
   */
  val measureParam: MeasureParam

  /**
   * If this measure supports record writing.
   */
  val supportsRecordWrite: Boolean

  /**
   * If this measure supports metric writing.
   */
  val supportsMetricWrite: Boolean

  /**
   * Metric values column.
   */
  final val valueColumn = s"${MeasureColPrefix}_${measureParam.getName}"

  /**
   * Helper method to get a typed value from measure configuration based on given key.
   *
   * @param key given key for which the value needs to be fetched.
   * @param defValue default value in case of no value.
   * @tparam T type of value to get.
   * @return value for given key
   */
  def getFromConfig[T: ClassTag](key: String, defValue: T): T = {
    measureParam.getConfig.getAnyRef[T](key, defValue)
  }

  /**
   * Enriches metrics dataframe with some additional keys.
   */
  // todo add status col to persist blank metrics if the measure fails
  def preProcessMetrics(input: DataFrame): DataFrame = {
    if (supportsMetricWrite) {
      input.withColumn(Metrics, col(valueColumn)).select(Metrics)
    } else input
  }

  /**
   * Enriches records dataframe with a status column marking rows as good or bad based on values.
   */
  def preProcessRecords(input: DataFrame): DataFrame = {
    if (supportsRecordWrite) {
      input
        .withColumn(Status, when(col(valueColumn) === 0, Good).otherwise(Bad))
        .drop(valueColumn)
    } else input
  }

  /**
   * Implementation of this measure.
   *
   * @return tuple of records dataframe and metric dataframe
   */
  def impl(dataSource: DataFrame): (DataFrame, DataFrame)

  /**
   * Implementation should define validation checks in this method (if required).
   * This method needs to be called explicitly call this method (preferably during measure creation).
   *
   * Defaults to no-op.
   */
  def validate(): Unit = {}

  /**
   * Executes this measure specific transformation on input data source.
   *
   * @return enriched tuple of records dataframe and metric dataframe
   */
  def execute(dataSource: DataFrame): (DataFrame, DataFrame) = {
    val (recordsDf, metricDf) = impl(dataSource)

    val processedRecordDf = preProcessRecords(recordsDf)
    val processedMetricDf = preProcessMetrics(metricDf)

    (processedRecordDf, processedMetricDf)
  }

  protected def nullToZero(column: Column): Column = when(column.isNull, 0).otherwise(column)

}

/**
 * Measure Constants.
 */
object Measure {

  final val DataSource = "data_source"
  final val Expression = "expr"
  final val MeasureColPrefix = "__measure"
  final val Status = "__status"
  final val BatchId = "batch_id"

  final val MeasureName = "measure_name"
  final val MeasureType = "measure_type"
  final val MetricName = "metric_name"
  final val MetricValue = "metric_value"
  final val Metrics = "metrics"
  final val Good = "good"
  final val Bad = "bad"

  final val Total: String = "total"
  final val BadRecordDefinition = "bad.record.definition"
  final val AllColumns: String = "*"
  final val RowNumber: String = "__row_number"

  final val emptyCol: Column = lit(StringUtils.EMPTY)

}
