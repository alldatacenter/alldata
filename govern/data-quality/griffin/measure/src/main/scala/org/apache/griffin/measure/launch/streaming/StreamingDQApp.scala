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

package org.apache.griffin.measure.launch.streaming

import java.util.{Date, Timer, TimerTask}
import java.util.concurrent.{Executors, ThreadPoolExecutor, TimeUnit}

import scala.util.Try

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Milliseconds, StreamingContext}

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition._
import org.apache.griffin.measure.configuration.enums.ProcessType.StreamingProcessType
import org.apache.griffin.measure.context._
import org.apache.griffin.measure.context.streaming.checkpoint.lock.CheckpointLock
import org.apache.griffin.measure.context.streaming.checkpoint.offset.OffsetCheckpointClient
import org.apache.griffin.measure.context.streaming.metric.CacheResults
import org.apache.griffin.measure.datasource.DataSourceFactory
import org.apache.griffin.measure.job.DQJob
import org.apache.griffin.measure.job.builder.DQJobBuilder
import org.apache.griffin.measure.launch.DQApp
import org.apache.griffin.measure.sink.Sink
import org.apache.griffin.measure.step.builder.udf.GriffinUDFAgent
import org.apache.griffin.measure.utils.{HdfsUtil, TimeUtil}

case class StreamingDQApp(allParam: GriffinConfig) extends DQApp {

  val envParam: EnvConfig = allParam.getEnvConfig
  val dqParam: DQConfig = allParam.getDqConfig

  val sparkParam: SparkParam = envParam.getSparkParam
  val metricName: String = dqParam.getName
  val sinkParams: Seq[SinkParam] = getSinkParams

  def retryable: Boolean = true

  def init: Try[_] = Try {
    // build spark 2.0+ application context
    val conf = new SparkConf().setAppName(metricName)
    conf.setAll(sparkParam.getConfig)
    conf.set("spark.sql.crossJoin.enabled", "true")
    sparkSession = SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val logLevel = getGriffinLogLevel
    sparkSession.sparkContext.setLogLevel(sparkParam.getLogLevel)
    griffinLogger.setLevel(logLevel)

    // clear checkpoint directory
    clearCpDir()

    // init info cache instance
    OffsetCheckpointClient.initClient(envParam.getCheckpointParams, metricName)
    OffsetCheckpointClient.init()

    // register udf
    GriffinUDFAgent.register(sparkSession)
  }

  def run: Try[Boolean] = Try {

    // streaming context
    val ssc = StreamingContext.getOrCreate(sparkParam.getCpDir, () => {
      try {
        createStreamingContext
      } catch {
        case e: Throwable =>
          error(s"create streaming context error: ${e.getMessage}")
          throw e
      }
    })

    // start time
    val measureTime = getMeasureTime
    val contextId = ContextId(measureTime)

    // generate data sources
    val dataSources = DataSourceFactory.getDataSources(sparkSession, ssc, dqParam.getDataSources)
    dataSources.foreach(_.init())

    // create dq context
    val globalContext: DQContext =
      DQContext(contextId, metricName, dataSources, sinkParams, StreamingProcessType)(
        sparkSession)
    globalContext.loadDataSources()

    // start id
    val applicationId = sparkSession.sparkContext.applicationId
    globalContext.getSinks.foreach(_.open(applicationId))

    // process thread
    val dqCalculator = StreamingDQCalculator(globalContext, dqParam.getEvaluateRule)

    val processInterval = TimeUtil.milliseconds(sparkParam.getProcessInterval) match {
      case Some(interval) => interval
      case _ => throw new Exception("invalid batch interval")
    }
    val process = Scheduler(processInterval, dqCalculator)
    process.startup()

    ssc.start()
    ssc.awaitTermination()
    ssc.stop(stopSparkContext = true, stopGracefully = true)

    // clean context
    globalContext.clean()

    // finish
    globalContext.getSinks.foreach(_.close())

    true
  }

  def close: Try[_] = Try {
    sparkSession.close()
    sparkSession.stop()
  }

  def createStreamingContext: StreamingContext = {
    val batchInterval = TimeUtil.milliseconds(sparkParam.getBatchInterval) match {
      case Some(interval) => Milliseconds(interval)
      case _ => throw new Exception("invalid batch interval")
    }
    val ssc = new StreamingContext(sparkSession.sparkContext, batchInterval)
    ssc.checkpoint(sparkParam.getCpDir)

    ssc
  }

  private def clearCpDir(): Unit = {
    if (sparkParam.needInitClear) {
      val cpDir = sparkParam.getCpDir
      info(s"clear checkpoint directory $cpDir")
      HdfsUtil.deleteHdfsPath(cpDir)
    }
  }

  /**
   *
   * @param globalContext
   * @param evaluateRuleParam
   */
  case class StreamingDQCalculator(globalContext: DQContext, evaluateRuleParam: EvaluateRuleParam)
      extends Runnable
      with Loggable {

    val lock: CheckpointLock = OffsetCheckpointClient.genLock("process")
    val appSink: Iterable[Sink] = globalContext.getSinks

    var dqContext: DQContext = _
    var dqJob: DQJob = _

    def run(): Unit = {
      val updateTimeDate = new Date()
      val updateTime = updateTimeDate.getTime
      println(s"===== [$updateTimeDate] process begins =====")
      val locked = lock.lock(5, TimeUnit.SECONDS)
      if (locked) {
        try {
          import org.apache.griffin.measure.utils.CommonUtils

          OffsetCheckpointClient.startOffsetCheckpoint()

          CommonUtils.timeThis({
            // start time
            val startTime = new Date().getTime

            val contextId = ContextId(startTime)

            // create dq context
            dqContext = globalContext.cloneDQContext(contextId)

            // build job
            dqJob = DQJobBuilder.buildDQJob(dqContext, evaluateRuleParam)

            // dq job execute
            dqJob.execute(dqContext)

            // finish calculation
            finishCalculation(dqContext)

            // end time
          }, TimeUnit.MILLISECONDS)

          OffsetCheckpointClient.endOffsetCheckpoint()

          // clean old data
          cleanData(dqContext)

        } catch {
          case e: Throwable => error(s"process error: ${e.getMessage}")
        } finally {
          lock.unlock()
        }
      } else {
        println(s"===== [$updateTimeDate] process ignores =====")
      }
      val endTime = new Date().getTime
      println(s"===== [$updateTimeDate] process ends, using ${endTime - updateTime} ms =====")
    }

    // finish calculation for this round
    private def finishCalculation(context: DQContext): Unit = {
      context.dataSources.foreach(_.processFinish())
    }

    // clean old data and old result cache
    private def cleanData(context: DQContext): Unit = {
      try {
        context.dataSources.foreach(_.cleanOldData())

        context.clean()

        val cleanTime = OffsetCheckpointClient.getCleanTime
        CacheResults.refresh(cleanTime)
      } catch {
        case e: Throwable => error(s"clean data error: ${e.getMessage}")
      }
    }

  }

  /**
   *
   * @param interval
   * @param runnable
   */
  case class Scheduler(interval: Long, runnable: Runnable) {

    val pool: ThreadPoolExecutor =
      Executors.newFixedThreadPool(5).asInstanceOf[ThreadPoolExecutor]

    val timer = new Timer("process", true)

    val timerTask: TimerTask = new TimerTask() {
      override def run(): Unit = {
        pool.submit(runnable)
      }
    }

    def startup(): Unit = {
      timer.schedule(timerTask, interval, interval)
    }

    def shutdown(): Unit = {
      timer.cancel()
      pool.shutdown()
      pool.awaitTermination(10, TimeUnit.SECONDS)
    }

  }

}
