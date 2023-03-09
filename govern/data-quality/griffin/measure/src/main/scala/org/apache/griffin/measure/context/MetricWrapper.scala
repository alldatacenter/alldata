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

package org.apache.griffin.measure.context

import scala.collection.mutable.{Map => MutableMap}

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure._

/**
 * wrap metrics into one, each calculation produces one metric map
 */
case class MetricWrapper(name: String, applicationId: String) extends Serializable {

  val JobName = "job_name"
  val Timestamp = "tmst"
  val ApplicationID = "applicationId"

  val metrics: MutableMap[Long, Map[String, Any]] = MutableMap()

  def insertMetric(timestamp: Long, value: Map[String, Any]): Unit = {
    val newValue = metrics.get(timestamp) match {
      case Some(v) => v ++ value
      case _ => value
    }
    metrics += (timestamp -> newValue)
  }

  def flush(measureParamOpt: Option[MeasureParam] = None): Map[Long, Map[String, Any]] = {
    metrics.toMap.map { pair =>
      {
        val (timestamp, value) = pair
        val metrics = if (value.contains(Metrics)) value(Metrics) else value
        val jobDetails =
          Map(
            JobName -> name,
            Timestamp -> timestamp,
            ApplicationID -> applicationId,
            Metrics -> metrics)

        val measureDetails = measureParamOpt
          .map(
            mp =>
              Map(
                MeasureName -> mp.getName,
                MeasureType -> mp.getType.toString,
                DataSource -> mp.getDataSource))
          .getOrElse(Map.empty)

        (timestamp, jobDetails ++ measureDetails)
      }
    }
  }

  def clear(): Unit = metrics.clear()

}
