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

package org.apache.griffin.measure.execution

import java.util.Date

import scala.concurrent.{ExecutionContextExecutorService, Future}
import scala.util._

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.configuration.enums.{MeasureTypes, OutputType}
import org.apache.griffin.measure.configuration.enums.FlattenType.DefaultFlattenType
import org.apache.griffin.measure.context.{ContextId, DQContext}
import org.apache.griffin.measure.execution.impl._
import org.apache.griffin.measure.step.write.{MetricFlushStep, MetricWriteStep, RecordWriteStep}

/**
 * MeasureExecutor
 *
 * This acts as the starting point for the execution of different data quality measures
 * defined by the users in `DQConfig`. Execution of the measures involves the following steps,
 *  - Create a fix pool of threads which will be used to execute measures in parallel
 *  - For each measure defined per data source,
 *    - Caching data source(s) if necessary
 *    - In parallel do the following,
 *      - Create Measure entity (transformation step)
 *      - Write Metrics if required and if supported (metric write step)
 *      - Write Records if required and if supported (record write step)
 *      - Clear internal objects (metric flush step)
 *    - Un caching data source(s) if cached already.
 *
 * In contrast to the execution of `GriffinDslDQStepBuilder`, `MeasureExecutor` executes each of
 * the defined measures independently. This means that the outputs (metrics and records) are written
 * independently for each measure.
 *
 * @param context Instance of `DQContext`
 */
case class MeasureExecutor(context: DQContext, ec: ExecutionContextExecutorService)
    extends Loggable {

  /**
   * SparkSession for this Griffin Application.
   */
  private val sparkSession: SparkSession = context.sparkSession

  /**
   * Enable or disable caching of data sources before execution. Defaults to `true`.
   */
  private val cacheDataSources: Boolean = sparkSession.sparkContext.getConf
    .getBoolean("spark.griffin.measure.cacheDataSources", defaultValue = true)

  /**
   * Starting point of measure execution.
   *
   * @param measureParams Object representation(s) of user defined measure(s).
   */
  def execute(measureParams: Seq[MeasureParam]): Unit = {
    implicit val measureCountByDataSource: Map[String, Int] = measureParams
      .map(_.getDataSource)
      .groupBy(x => x)
      .mapValues(_.length)

    measureParams
      .groupBy(measureParam => measureParam.getDataSource)
      .foreach(measuresForSource => {
        val dataSourceName = measuresForSource._1
        val measureParams = measuresForSource._2

        val dataSource = sparkSession.read.table(dataSourceName)

        if (dataSource.isStreaming) {
          // TODO this is a no op as streaming queries need to be registered.

          dataSource.writeStream
            .foreachBatch((batchDf: Dataset[Row], batchId: Long) => {
              executeMeasures(batchDf, dataSourceName, measureParams, batchId)
            })
        } else {
          executeMeasures(dataSource, dataSourceName, measureParams)
        }
      })
  }

  /**
   * Performs a function with cached data source if necessary.
   * Caches data sources if it has more than 1 measure defined for it, and, `cacheDataSources` is `true`.
   * After the function is complete, the data source is uncached.
   *
   * @param dataSourceName name of data source
   * @param numMeasures number of measures for each data source
   * @param f function to perform
   * @return
   */
  private def withCacheIfNecessary(
      dataSourceName: String,
      numMeasures: Int,
      dataSource: DataFrame,
      f: => Unit): Unit = {
    var isCached = false
    if (cacheDataSources && numMeasures > 1) {
      if (!dataSource.isStreaming) {
        info(
          s"Caching data source with name '$dataSourceName' as $numMeasures measures are applied on it.")
        dataSource.persist()
        isCached = true
      }
    }

    f

    if (isCached) {
      info(
        s"Un-Caching data source with name '$dataSourceName' as measure execution is complete for it.")
      dataSource.unpersist(true)
    }
  }

  /**
   * Executes measures for a data sources. Involves the following steps,
   *  - Transformation
   *  - Persist metrics if required
   *  - Persist records if required
   *
   *  All measures are executed in parallel.
   *
   * @param measureParams Object representation(s) of user defined measure(s).
   * @param batchId Option batch Id in case of streaming sources to identify micro batches.
   */
  private def executeMeasures(
      input: DataFrame,
      dataSourceName: String,
      measureParams: Seq[MeasureParam],
      batchId: Long = -1L): Unit = {
    val numMeasures: Int = measureParams.length

    withCacheIfNecessary(
      dataSourceName,
      numMeasures,
      input, {
        import java.util.concurrent.TimeUnit

        import scala.concurrent.duration.Duration

        // define the tasks
        val tasks = (for (i <- measureParams.indices)
          yield {
            val measureParam = measureParams(i)
            val measure = createMeasure(measureParam)
            val measureName = measureParam.getName

            (measure, Future {
              info(s"Started execution of measure with name '$measureName'")

              val (recordsDf, metricsDf) = measure.execute(input)
              val currentContext = context.cloneDQContext(ContextId(new Date().getTime))

              persistMetrics(currentContext, measure, metricsDf)
              persistRecords(currentContext, measure, recordsDf)

              MetricFlushStep(Some(measureParam)).execute(currentContext)
            }(ec))
          }).toMap

        tasks.foreach(task => {
          val measureName = task._1.measureParam.getName

          task._2.onComplete {
            case Success(_) =>
              info(
                s"Successfully executed measure with name '$measureName' on data source with name " +
                  s"'$dataSourceName")
            case Failure(exception) =>
              error(
                s"Error occurred while executing measure with name '$measureName' on data source with name " +
                  s"'$dataSourceName'",
                exception)
          }(ec)
        })

        var deadline = Duration(10, TimeUnit.SECONDS).fromNow

        while (!tasks.forall(_._2.isCompleted)) {
          if (deadline.isOverdue()) {
            val unfinishedMeasureNames = tasks
              .filterNot(_._2.isCompleted)
              .map(_._1.measureParam.getName)
              .mkString("['", "', '", "']")

            info(s"Measures with name $unfinishedMeasureNames are still executing.")
            deadline = Duration(10, TimeUnit.SECONDS).fromNow
          }
        }

        info(
          "Completed execution of all measures for data source with " +
            s"name '${measureParams.head.getDataSource}'.")
      })
  }

  /**
   * Instantiates measure implementations based on the user defined configurations.
   *
   * @param measureParam Object representation of user defined a measure.
   * @return
   */
  private def createMeasure(measureParam: MeasureParam): Measure = {
    measureParam.getType match {
      case MeasureTypes.Completeness => CompletenessMeasure(sparkSession, measureParam)
      case MeasureTypes.Duplication => DuplicationMeasure(sparkSession, measureParam)
      case MeasureTypes.Profiling => ProfilingMeasure(sparkSession, measureParam)
      case MeasureTypes.Accuracy => AccuracyMeasure(sparkSession, measureParam)
      case MeasureTypes.SparkSQL => SparkSQLMeasure(sparkSession, measureParam)
      case MeasureTypes.SchemaConformance => SchemaConformanceMeasure(sparkSession, measureParam)
      case _ =>
        val errorMsg = s"Measure type '${measureParam.getType}' is not supported."
        val exception = new NotImplementedError(errorMsg)
        error(errorMsg, exception)
        throw exception
    }
  }

  /**
   * Persists records to one or more sink based on the user defined measure configuration.
   *
   * @param context DQ Context.
   * @param measure a measure implementation
   * @param recordsDf records dataframe to persist.
   */
  private def persistRecords(context: DQContext, measure: Measure, recordsDf: DataFrame): Unit = {
    val measureParam: MeasureParam = measure.measureParam

    measureParam.getOutputOpt(OutputType.RecordOutputType) match {
      case Some(_) =>
        if (measure.supportsRecordWrite) {
          recordsDf.createOrReplaceTempView("recordsDf")
          RecordWriteStep(measureParam.getName, "recordsDf").execute(context)
        } else warn(s"Measure with name '${measureParam.getName}' doesn't support record write")
      case None =>
    }
  }

  /**
   * Persists metrics to one or more sink based on the user defined measure configuration.
   *
   * @param context DQ Context.
   * @param measure a measure implementation
   * @param metricsDf metrics dataframe to persist
   */
  private def persistMetrics(context: DQContext, measure: Measure, metricsDf: DataFrame): Unit = {
    val measureParam: MeasureParam = measure.measureParam

    measureParam.getOutputOpt(OutputType.MetricOutputType) match {
      case Some(_) =>
        if (measure.supportsMetricWrite) {
          val metricDfName = s"${measureParam.getName}_metricsDf"
          metricsDf.createOrReplaceTempView(metricDfName)
          MetricWriteStep(measureParam.getName, metricDfName, DefaultFlattenType).execute(context)
        } else warn(s"Measure with name '${measureParam.getName}' doesn't support metric write")
      case None =>
    }
  }

}
