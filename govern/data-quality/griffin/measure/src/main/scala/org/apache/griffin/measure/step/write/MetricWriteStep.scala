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

package org.apache.griffin.measure.step.write

import scala.util.Try

import org.apache.griffin.measure.configuration.enums.{FlattenType, SimpleMode, TimestampMode}
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.builder.ConstantColumns
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * write metrics into context metric wrapper
 */
case class MetricWriteStep(
    name: String,
    inputName: String,
    flattenType: FlattenType.FlattenType,
    writeTimestampOpt: Option[Long] = None)
    extends WriteStep {

  val emptyMetricMap: Map[Long, Map[String, Any]] = Map[Long, Map[String, Any]]()
  val emptyMap: Map[String, Any] = Map[String, Any]()

  def execute(context: DQContext): Try[Boolean] = Try {
    val timestamp = writeTimestampOpt.getOrElse(context.contextId.timestamp)

    // get metric list from data frame
    val metricMaps: Seq[Map[String, Any]] = getMetricMaps(context)

    // get timestamp and normalize metric
    val writeMode = writeTimestampOpt.map(_ => SimpleMode).getOrElse(context.writeMode)
    val timestampMetricMap: Map[Long, Map[String, Any]] = writeMode match {

      case SimpleMode =>
        val metrics: Map[String, Any] = flattenMetric(metricMaps, name, flattenType)
        emptyMetricMap + (timestamp -> metrics)

      case TimestampMode =>
        val tmstMetrics = metricMaps.map { metric =>
          val tmst = metric.getLong(ConstantColumns.tmst, timestamp)
          val pureMetric = metric.removeKeys(ConstantColumns.columns)
          (tmst, pureMetric)
        }
        tmstMetrics.groupBy(_._1).map { pair =>
          val (k, v) = pair
          val maps = v.map(_._2)
          val mtc = flattenMetric(maps, name, flattenType)
          (k, mtc)
        }
    }

    // write to metric wrapper
    timestampMetricMap.foreach { pair =>
      val (timestamp, v) = pair
      context.metricWrapper.insertMetric(timestamp, v)
    }

    true
  }

  private def getMetricMaps(context: DQContext): Seq[Map[String, Any]] = {
    try {
      val pdf = context.sparkSession.table(s"`$inputName`")
      val rows = pdf.collect()
      val columns = pdf.columns
      if (rows.length > 0) {
        rows.map(_.getValuesMap(columns))
      } else Nil
    } catch {
      case e: Throwable =>
        error(s"get metric $name fails", e)
        Nil
    }
  }

  private def flattenMetric(
      metrics: Seq[Map[String, Any]],
      name: String,
      flattenType: FlattenType.FlattenType): Map[String, Any] = {
    flattenType match {
      case FlattenType.EntriesFlattenType => metrics.headOption.getOrElse(emptyMap)
      case FlattenType.ArrayFlattenType => Map[String, Any](name -> metrics)
      case FlattenType.MapFlattenType =>
        val v = metrics.headOption.getOrElse(emptyMap)
        Map[String, Any](name -> v)
      case _ =>
        if (metrics.size > 1) Map[String, Any](name -> metrics)
        else metrics.headOption.getOrElse(emptyMap)
    }
  }

}
