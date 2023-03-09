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

package org.apache.griffin.measure.launch

import scala.util.Try

import org.apache.spark.metrics.sink.Sink
import org.apache.spark.sql.SparkSession

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.{DQConfig, EnvConfig, SinkParam}

/**
 * dq application process
 */
trait DQApp extends Loggable with Serializable {

  val envParam: EnvConfig
  val dqParam: DQConfig

  implicit var sparkSession: SparkSession = _

  def init: Try[_]

  /**
   * @return execution success
   */
  def run: Try[Boolean]

  def close: Try[_]

  /**
   * application will exit if it fails in run phase.
   * if retryable is true, the exception will be threw to spark env,
   * and enable retry strategy of spark application
   */
  def retryable: Boolean

  /**
   * timestamp as a key for metrics
   */
  protected def getMeasureTime: Long = {
    dqParam.getTimestampOpt match {
      case Some(t) if t > 0 => t
      case _ => System.currentTimeMillis
    }
  }

  /**
   * Gets a valid [[Sink]] definition from the Env Config for each [[Sink]] defined in Job Config.
   *
   * @throws AssertionError if Env Config does not contain definition for a sink defined in Job Config
   * @return [[Seq]] of [[Sink]] definitions
   */
  protected def getSinkParams: Seq[SinkParam] = {
    val sinkParams = dqParam.getSinkNames
      .map(_.toLowerCase())
      .map { sinkName =>
        (sinkName, envParam.getSinkParams.find(_.getName.toLowerCase().matches(sinkName)))
      }

    val missingSinks = sinkParams.filter(_._2.isEmpty).map(_._1)

    assert(
      missingSinks.isEmpty,
      s"Sink(s) ['${missingSinks.mkString("', '")}'] not defined in env config.")

    sinkParams.flatMap(_._2)
  }

}
