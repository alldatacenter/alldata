/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester

import org.apache.spark
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.util.QueryExecutionListener
import za.co.absa.commons.version.Version
import za.co.absa.commons.version.Version._
import za.co.absa.spline.agent.{AgentBOM, AgentConfig, SplineAgent}
import za.co.absa.spline.harvester.IdGenerator.{UUIDGeneratorFactory, UUIDNamespace}
import za.co.absa.spline.harvester.SparkLineageInitializer.{InitFlagKey, SparkQueryExecutionListenersKey}
import za.co.absa.spline.harvester.conf._
import za.co.absa.spline.harvester.listener.QueryExecutionListenerDecorators._
import za.co.absa.spline.harvester.listener.{QueryExecutionListenerDelegate, SplineQueryExecutionListener}
import za.co.absa.spline.producer.model.ExecutionPlan

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
 * Spline agent initializer - enables lineage tracking for the given Spark Session.
 * This is an alternative method to so called "codeless initialization" (via using `spark.sql.queryExecutionListeners`
 * config parameter). Use either of those, not both.
 *
 * The programmatic approach provides a higher level of customization in comparison to the declarative (codeless) one,
 * but on the other hand for the majority of practical purposes the codeless initialization is sufficient and
 * provides a lower coupling between the Spline agent and the target application by not requiring any dependency
 * on Spline agent library from the client source code. Thus we recommend to prefer a codeless init, rather than
 * calling this method directly unless you have specific customization requirements.
 *
 * (See the Spline agent doc for details)
 *
 */
object SparkLineageInitializer {

  def enableLineageTracking(sparkSession: SparkSession): SparkSession = {
    sparkSession.enableLineageTracking()
  }

  def enableLineageTracking(sparkSession: SparkSession, userConfig: AgentConfig): SparkSession = {
    sparkSession.enableLineageTracking(userConfig)
  }

  /**
   * Allows for the fluent DSL like this:
   * {{{
   *   sparkSession
   *     .enableLineageTracking(...)
   *     .read
   *     .parquet(...)
   *     .etc
   * }}}
   *
   * @param sparkSession a Spark Session on which the lineage tracking should be enabled.
   */
  implicit class SplineSparkSessionWrapper(sparkSession: SparkSession) {

    private implicit val executionContext: ExecutionContext = ExecutionContext.global

    /**
     * Enables data lineage tracking on a Spark Session.
     * The method is used for programmatic initialization of Spline agent.
     *
     * <br>
     * Usage examples:
     *
     * <br>
     *  1. Enable Spline using default configuration. See [[https://github.com/AbsaOSS/spline-spark-agent#configuration]]
     * {{{
     *  spark.enableLineageTracking()
     * }}}
     *
     *  1. Enable Spline using customized configuration
     * {{{
     *  spark.enableLineageTracking(
     *    AgentConfig.builder()
     *      .splineMode(...)
     *      .config("key1", "value1")
     *      .config("key2", "value2")
     *      .build()
     *  )
     * }}}
     *
     * @see [[https://github.com/AbsaOSS/spline-spark-agent#initialization-programmatic]]
     * @param userConfig user agent configuration. See [[AgentConfig]]
     * @return the same instance of the spark session
     */
    def enableLineageTracking(userConfig: AgentConfig = AgentConfig.empty): SparkSession = {
      new SparkLineageInitializer(sparkSession)
        .createListener(isCodelessInit = false, userConfig)
        .foreach(eventHandler =>
          sparkSession.listenerManager.register(eventHandler)
        )
      sparkSession
    }
  }

  val InitFlagKey = "spline.initializedFlag"

  val SparkQueryExecutionListenersKey = "spark.sql.queryExecutionListeners"
}

private[spline] class SparkLineageInitializer(sparkSession: SparkSession) extends Logging {

  def createListener(isCodelessInit: Boolean, userConfig: AgentConfig = AgentConfig.empty): Option[QueryExecutionListener] = {
    val bom = AgentBOM.createFrom(
      StandardSplineConfigurationStack.defaultConfig,
      StandardSplineConfigurationStack.configStack(sparkSession, userConfig),
      sparkSession
    )

    logInfo("Initializing Spline Agent...")
    logInfo(s"Spline Version: ${SplineBuildInfo.Version} (rev. ${SplineBuildInfo.Revision})")
    logInfo(s"Init Type: ${if (isCodelessInit) "AUTO (codeless)" else "PROGRAMMATIC"}")
    logInfo(s"Init Mode: ${bom.splineMode}")

    if (bom.splineMode == SplineMode.DISABLED) {
      logInfo("initialization aborted")
      None
    }
    else withErrorHandling(bom.splineMode) {
      if (isCodelessInit)
        Some(createListener(bom))
      else
        assureOneListenerPerSession(createListener(bom))
    }
  }

  def createListener(bom: AgentBOM): QueryExecutionListener = {
    logInfo(s"Lineage Dispatcher: ${bom.lineageDispatcher.name}")
    logInfo(s"Post-Processing Filter: ${bom.postProcessingFilter.map(_.name) getOrElse ""}")
    logInfo(s"Ignore-Write Detection Strategy: ${bom.iwdStrategy.name}")

    val lineageDispatcher = bom.lineageDispatcher
    val userPostProcessingFilter = bom.postProcessingFilter
    val iwdStrategy = bom.iwdStrategy
    val execPlanUUIDGeneratorFactory = UUIDGeneratorFactory.forVersion[UUIDNamespace, ExecutionPlan](bom.execPlanUUIDVersion)

    val agent = SplineAgent.create(
      sparkSession,
      lineageDispatcher,
      userPostProcessingFilter,
      iwdStrategy,
      execPlanUUIDGeneratorFactory
    )

    logInfo("Spline successfully initialized. Spark Lineage tracking is ENABLED.")

    bom.sqlFailureCaptureMode match {
      case SQLFailureCaptureMode.NONE => new QueryExecutionListenerDelegate(agent) with AnyFailureOmitting
      case SQLFailureCaptureMode.NON_FATAL => new QueryExecutionListenerDelegate(agent) with FatalFailureOmitting
      case SQLFailureCaptureMode.ALL => new QueryExecutionListenerDelegate(agent)
    }
  }

  private def withErrorHandling(splineMode: SplineMode)(body: => Option[QueryExecutionListener]) = {
    try {
      body
    } catch {
      case NonFatal(e) if splineMode == SplineMode.BEST_EFFORT =>
        logError(s"Spline initialization failed! Spark Lineage tracking is DISABLED.", e)
        None
    }
  }

  private def assureOneListenerPerSession(body: => QueryExecutionListener) = {
    val isCodelessInitActive =
      (Version.asSimple(spark.SPARK_VERSION) >= ver"2.3.0"
        && sparkSession.sparkContext.getConf
        .getOption(SparkQueryExecutionListenersKey).toSeq
        .flatMap(s => s.split(",").toSeq)
        .contains(classOf[SplineQueryExecutionListener].getCanonicalName))

    def getOrSetIsInitialized(): Boolean = sparkSession.synchronized {
      val sessionConf = sparkSession.conf
      sessionConf getOption InitFlagKey match {
        case Some("true") =>
          true
        case _ =>
          sessionConf.set(InitFlagKey, true.toString)
          false
      }
    }

    if (isCodelessInitActive || getOrSetIsInitialized()) {
      logWarning("Spline lineage tracking is already initialized!")
      None
    } else try {
      Some(body)
    } catch {
      case NonFatal(e) =>
        sparkSession.synchronized {
          sparkSession.conf.set(InitFlagKey, false.toString)
        }
        throw e
    }
  }
}
