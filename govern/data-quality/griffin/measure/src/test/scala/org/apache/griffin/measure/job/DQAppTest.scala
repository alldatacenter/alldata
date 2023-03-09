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

package org.apache.griffin.measure.job

import scala.util._

import org.scalatest.matchers.should._

import org.apache.griffin.measure.{Loggable, SparkSuiteBase}
import org.apache.griffin.measure.Application._
import org.apache.griffin.measure.configuration.dqdefinition._
import org.apache.griffin.measure.configuration.enums.ProcessType
import org.apache.griffin.measure.configuration.enums.ProcessType._
import org.apache.griffin.measure.launch.DQApp
import org.apache.griffin.measure.launch.batch.BatchDQApp
import org.apache.griffin.measure.launch.streaming.StreamingDQApp

class DQAppTest extends SparkSuiteBase with Matchers with Loggable {

  var envParam: EnvConfig = _
  var sparkParam: SparkParam = _

  var dqApp: DQApp = _

  def getConfigFilePath(fileName: String): String = {
    Try(getClass.getResource(fileName).getFile).toOption match {
      case Some(n) => n
      case None => throw new Exception(s"resource [$fileName] not found")
    }
  }

  def runApp(dqParamFile: String): DQApp = {
    val dqParam = readParamFile[DQConfig](getConfigFilePath(dqParamFile)) match {
      case Success(p) => p
      case Failure(ex) =>
        throw ex
    }

    val allParam: GriffinConfig = GriffinConfig(envParam, dqParam)

    // choose process
    val procType = ProcessType.withNameWithDefault(allParam.getDqConfig.getProcType)
    dqApp = procType match {
      case BatchProcessType => BatchDQApp(allParam)
      case StreamingProcessType => StreamingDQApp(allParam)
      case _ =>
        error(s"$procType is unsupported process type!")
        sys.exit(-4)
    }

    dqApp.sparkSession = spark

    dqApp.run match {
      case Success(ret) => assert(ret)
      case Failure(ex) =>
        error(s"process run error: ${ex.getMessage}", ex)
        throw ex
    }

    dqApp
  }
}
