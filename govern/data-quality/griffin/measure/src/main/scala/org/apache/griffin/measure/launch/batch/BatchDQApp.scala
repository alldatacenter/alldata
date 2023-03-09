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

package org.apache.griffin.measure.launch.batch

import java.util.concurrent.{Executors, TimeUnit}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.Try

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import org.apache.griffin.measure.configuration.dqdefinition._
import org.apache.griffin.measure.configuration.enums.ProcessType.BatchProcessType
import org.apache.griffin.measure.context._
import org.apache.griffin.measure.datasource.DataSourceFactory
import org.apache.griffin.measure.execution.MeasureExecutor
import org.apache.griffin.measure.job.builder.DQJobBuilder
import org.apache.griffin.measure.launch.DQApp
import org.apache.griffin.measure.step.builder.udf.GriffinUDFAgent
import org.apache.griffin.measure.utils.CommonUtils

case class BatchDQApp(allParam: GriffinConfig) extends DQApp {

  val envParam: EnvConfig = allParam.getEnvConfig
  val dqParam: DQConfig = allParam.getDqConfig

  val sparkParam: SparkParam = envParam.getSparkParam
  val metricName: String = dqParam.getName
  val sinkParams: Seq[SinkParam] = getSinkParams

  var dqContext: DQContext = _

  def retryable: Boolean = false

  def init: Try[_] = Try {
    // build spark 2.0+ application context
    val conf = new SparkConf().setAppName(metricName)
    conf.setAll(sparkParam.getConfig)
    conf.set("spark.sql.crossJoin.enabled", "true")
    sparkSession = SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val logLevel = getGriffinLogLevel
    sparkSession.sparkContext.setLogLevel(sparkParam.getLogLevel)
    griffinLogger.setLevel(logLevel)

    // register udf
    GriffinUDFAgent.register(sparkSession)
  }

  def run: Try[Boolean] = {
    val result =
      CommonUtils.timeThis(
        {
          val measureTime = getMeasureTime
          val contextId = ContextId(measureTime)

          // get data sources
          val dataSources =
            DataSourceFactory.getDataSources(sparkSession, null, dqParam.getDataSources)
          dataSources.foreach(_.init())

          // create dq context
          dqContext = DQContext(contextId, metricName, dataSources, sinkParams, BatchProcessType)(
            sparkSession)

          dqContext.loadDataSources()

          // start id
          val applicationId = sparkSession.sparkContext.applicationId
          dqContext.getSinks.foreach(_.open(applicationId))

          if (dqParam.getMeasures != null && dqParam.getMeasures.nonEmpty) {
            Try {
              // Size of thread pool for parallel measure execution.
              // Defaults to number of processors available to the spark driver JVM.
              val numThreads: Int = sparkSession.sparkContext.getConf
                .getInt(
                  "spark.griffin.measure.parallelism",
                  Runtime.getRuntime.availableProcessors())

              // Service to handle threaded execution of tasks (measures).
              val ec: ExecutionContextExecutorService =
                ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))

              MeasureExecutor(dqContext, ec).execute(dqParam.getMeasures)
              true
            }
          } else {
            // build job
            val dqJob = DQJobBuilder.buildDQJob(dqContext, dqParam.getEvaluateRule)

            // dq job execute
            dqJob.execute(dqContext)
          }
        },
        TimeUnit.MILLISECONDS)

    // clean context
    dqContext.clean()

    // finish
    dqContext.getSinks.foreach(_.close())

    result
  }

  def close: Try[_] = Try {
    sparkSession.stop()
  }

}
