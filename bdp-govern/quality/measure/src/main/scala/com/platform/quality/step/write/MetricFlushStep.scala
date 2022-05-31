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

package com.platform.quality.step.write

import com.platform.quality.configuration.dqdefinition.MeasureParam
import com.platform.quality.context.DQContext

import scala.util.Try

/**
 * flush final metric map in context and write
 */
case class MetricFlushStep(measureParamOpt: Option[MeasureParam] = None) extends WriteStep {

  val name: String = ""
  val inputName: String = ""
  val writeTimestampOpt: Option[Long] = None

  def execute(context: DQContext): Try[Boolean] = Try {
    val res = context.metricWrapper.flush(measureParamOpt).foldLeft(true) { (ret, pair) =>
      val (t, metric) = pair
      val pr =
        try {
          context.getSinks(t).foreach { sink =>
            try {
              sink.sinkMetrics(metric)
            } catch {
              case e: Throwable => error(s"sink metrics error: ${e.getMessage}", e)
            }
          }
          true
        } catch {
          case e: Throwable =>
            error(s"flush metrics error: ${e.getMessage}", e)
            false
        }
      ret && pr
    }

    context.metricWrapper.clear()
    res
  }

}
