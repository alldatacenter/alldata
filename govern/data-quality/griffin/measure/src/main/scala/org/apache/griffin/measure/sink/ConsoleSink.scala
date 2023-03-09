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

package org.apache.griffin.measure.sink

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

import org.apache.griffin.measure.execution.Measure
import org.apache.griffin.measure.utils.JsonUtil
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * Console Sink for Records and Metrics.
 * Records are shown in a tabular structure and Metrics are logged as JSON string.
 *
 * Supported Configurations:
 *  - truncate : [[Boolean]] Whether truncate long strings. If true, strings more than 20 characters
 *  will be truncated and all cells will be aligned right. Default is true.
 *  - numRows : [[Int]] Number of rows to show. Default is 20.
 */
case class ConsoleSink(config: Map[String, Any], jobName: String, timeStamp: Long) extends Sink {

  val block: Boolean = true

  val Truncate: String = "truncate"
  val truncateRecords: Boolean = config.getBoolean(Truncate, defValue = true)

  val NumberOfRows: String = "numRows"
  val numRows: Int = config.getInt(NumberOfRows, 20)

  val Unknown: String = "UNKNOWN"

  def validate(): Boolean = true

  override def open(applicationId: String): Unit = {
    info(
      s"Opened ConsoleSink for job with name '$jobName', " +
        s"timestamp '$timeStamp' and applicationId '$applicationId'")
  }

  override def close(): Unit = {
    info(s"Closed ConsoleSink for job with name '$jobName' and timestamp '$timeStamp'")
  }

  override def sinkRecords(records: RDD[String], name: String): Unit = {}

  override def sinkRecords(records: Iterable[String], name: String): Unit = {}

  override def sinkMetrics(metrics: Map[String, Any]): Unit = {
    val measureName = metrics.getOrElse(Measure.MeasureName, Unknown)
    griffinLogger.info(
      s"Metrics for measure with name '$measureName':\n${JsonUtil.toJson(metrics)}")
  }

  override def sinkBatchRecords(dataset: DataFrame, key: Option[String] = None): Unit = {
    dataset.show(numRows, truncateRecords)
    griffinLogger.info(s"Records written for measure with name '${key.getOrElse(Unknown)}'.")
  }

}
