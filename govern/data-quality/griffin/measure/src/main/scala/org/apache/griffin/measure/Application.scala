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

package org.apache.griffin.measure

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

import org.apache.griffin.measure.configuration.dqdefinition.{
  DQConfig,
  EnvConfig,
  GriffinConfig,
  Param
}
import org.apache.griffin.measure.configuration.dqdefinition.reader.ParamReaderFactory
import org.apache.griffin.measure.configuration.enums.ProcessType
import org.apache.griffin.measure.configuration.enums.ProcessType._
import org.apache.griffin.measure.launch.DQApp
import org.apache.griffin.measure.launch.batch.BatchDQApp
import org.apache.griffin.measure.launch.streaming.StreamingDQApp

/**
 * application entrance
 */
object Application extends Loggable {

  def main(args: Array[String]): Unit = {
    info(args.toString)
    if (args.length < 2) {
      error("Usage: class <env-param> <dq-param>")
      sys.exit(-1)
    }

    val envParamFile = args(0)
    val dqParamFile = args(1)

    info(envParamFile)
    info(dqParamFile)

    // read param files
    val envParam = readParamFile[EnvConfig](envParamFile) match {
      case Success(p) => p
      case Failure(ex) =>
        error(ex.getMessage, ex)
        sys.exit(-2)
    }
    val dqParam = readParamFile[DQConfig](dqParamFile) match {
      case Success(p) => p
      case Failure(ex) =>
        error(ex.getMessage, ex)
        sys.exit(-2)
    }
    val allParam: GriffinConfig = GriffinConfig(envParam, dqParam)

    // choose process
    val procType = ProcessType.withNameWithDefault(allParam.getDqConfig.getProcType)
    val dqApp: DQApp = procType match {
      case BatchProcessType => BatchDQApp(allParam)
      case StreamingProcessType => StreamingDQApp(allParam)
      case _ =>
        error(s"$procType is unsupported process type!")
        sys.exit(-4)
    }

    startup()

    // dq app init
    dqApp.init match {
      case Success(_) =>
        info("process init success")
      case Failure(ex) =>
        error(s"process init error: ${ex.getMessage}", ex)
        shutdown()
        sys.exit(-5)
    }

    // dq app run
    val success = dqApp.run match {
      case Success(result) =>
        info("process run result: " + (if (result) "success" else "failed"))
        result

      case Failure(ex) =>
        error(s"process run error: ${ex.getMessage}", ex)

        if (dqApp.retryable) {
          throw ex
        } else {
          shutdown()
          sys.exit(-5)
        }
    }

    // dq app end
    dqApp.close match {
      case Success(_) =>
        info("process end success")
      case Failure(ex) =>
        error(s"process end error: ${ex.getMessage}", ex)
        shutdown()
        sys.exit(-5)
    }

    shutdown()

    if (!success) {
      sys.exit(-5)
    } else {
      sys.exit(0)
    }
  }

  def readParamFile[T <: Param](file: String)(implicit m: ClassTag[T]): Try[T] = {
    val paramReader = ParamReaderFactory.getParamReader(file)
    paramReader.readConfig[T]
  }

  private def startup(): Unit = {}

  private def shutdown(): Unit = {}

}
