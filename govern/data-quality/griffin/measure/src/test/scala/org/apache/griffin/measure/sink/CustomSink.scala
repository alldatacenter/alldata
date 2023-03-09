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

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

import org.apache.griffin.measure.configuration.enums.MeasureTypes
import org.apache.griffin.measure.execution.Measure._

/**
 * A dummy batch sink for testing.
 *
 * @param config sink configurations
 * @param jobName Griffin Job Name
 * @param timeStamp timestamp for job
 * @param block is blocking or not
 */
case class CustomSink(config: Map[String, Any], jobName: String, timeStamp: Long, block: Boolean)
    extends Sink {
  def validate(): Boolean = true

  def log(rt: Long, msg: String): Unit = {}

  val allRecords: ListBuffer[String] = mutable.ListBuffer[String]()

  override def sinkRecords(records: RDD[String], name: String): Unit = {
    allRecords ++= records.collect()
  }

  override def sinkRecords(records: Iterable[String], name: String): Unit = {
    allRecords ++= records
  }

  val allMetrics: mutable.Map[String, Any] = mutable.Map[String, Any]()

  override def sinkMetrics(metrics: Map[String, Any]): Unit = {
    val measureName = metrics(MeasureName).toString
    val measureType =
      MeasureTypes.withNameWithDefault(metrics.getOrElse(MeasureType, "unknown").toString)

    val value = metrics(Metrics)
      .asInstanceOf[Seq[Map[String, Any]]]
      .map(x => {
        if (measureType == MeasureTypes.Profiling)
          x.head
        else
          x(MetricName).toString -> x(MetricValue)
      })
      .toMap

    CustomSinkResultRegister.setMetrics(measureName, value)

    allMetrics ++= metrics
  }

  override def sinkBatchRecords(dataset: DataFrame, key: Option[String] = None): Unit = {
    CustomSinkResultRegister.setBatch(key.get, dataset.toJSON.collect())
    allRecords ++= dataset.toJSON.rdd.collect()
  }
}

/**
 * Register for storing test sink results in memory
 */
object CustomSinkResultRegister {

  val _metricsSink: mutable.Map[String, Map[String, Any]] = mutable.HashMap.empty
  private val _batchSink: mutable.Map[String, Array[String]] = mutable.HashMap.empty

  def setMetrics(key: String, metrics: Map[String, Any]): Unit = {
    val updatedMetrics = _metricsSink.getOrElse(key, Map.empty) ++ metrics
    _metricsSink.put(key, updatedMetrics)
  }

  def getMetrics(key: String): Option[Map[String, Any]] = _metricsSink.get(key)

  def setBatch(key: String, batch: Array[String]): Unit = {
    val updatedBatch = _batchSink.getOrElse(key, Array.empty) ++ batch
    _batchSink.put(key, updatedBatch)
  }

  def getBatch(key: String): Option[Array[String]] = _batchSink.get(key)

  def clear(): Unit = {
    _metricsSink.clear()
    _batchSink.clear()
  }

}
