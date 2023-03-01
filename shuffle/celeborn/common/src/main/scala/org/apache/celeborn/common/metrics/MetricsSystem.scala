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

package org.apache.celeborn.common.metrics

import java.util.Properties
import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.metrics.sink.{PrometheusHttpRequestHandler, PrometheusServlet, Sink}
import org.apache.celeborn.common.metrics.source.Source
import org.apache.celeborn.common.util.Utils

class MetricsSystem(
    val instance: String,
    conf: CelebornConf,
    val servletPath: String) extends Logging {
  private[this] val metricsConfig = new MetricsConfig(conf)

  private val sinks = new mutable.ArrayBuffer[Sink]
  private val sources = new mutable.ArrayBuffer[Source]
  private val registry = new MetricRegistry()

  private var prometheusServlet: Option[PrometheusServlet] = None

  var running: Boolean = false

  metricsConfig.initialize()

  def getPrometheusHandler: PrometheusHttpRequestHandler = {
    require(running, "Can only call getServletHandlers on a running MetricsSystem")
    prometheusServlet.map(_.getHandler(conf)).orNull
  }

  def start(registerStaticSources: Boolean = true) {
    require(!running, "Attempting to start a MetricsSystem that is already running")
    running = true
    if (registerStaticSources) {
      registerSources()
    }
    registerSinks()
    sinks.foreach(_.start())
  }

  def stop() {
    if (running) {
      sinks.foreach(_.stop())
    } else {
      logWarning("Stopping a MetricsSystem that is not running")
    }
    running = false
  }

  def report() {
    sinks.foreach(_.report())
  }

  private def buildRegistryName(source: Source): String = {
    MetricRegistry.name(source.sourceName)
  }

  def getSourcesByName(sourceName: String): Seq[Source] =
    sources.filter(_.sourceName == sourceName)

  def registerSource(source: Source) {
    sources += source
    try {
      val regName = buildRegistryName(source)
      registry.register(regName, source.metricRegistry)
    } catch {
      case e: IllegalArgumentException => logInfo("Metrics already registered", e)
    }
  }

  def removeSource(source: Source) {
    sources -= source
    val regName = buildRegistryName(source)
    registry.removeMatching(new MetricFilter {
      def matches(name: String, metric: Metric): Boolean = name.startsWith(regName)
    })
  }

  private def registerSources() {
    val instConfig = metricsConfig.getInstance(instance)
    val sourceConfigs = metricsConfig.subProperties(instConfig, MetricsSystem.SOURCE_REGEX)

    // Register all the sources related to instance
    sourceConfigs.foreach { kv =>
      val classPath = kv._2.getProperty("class")
      try {
        val source = Utils.classForName(classPath).newInstance()
        registerSource(source.asInstanceOf[Source])
      } catch {
        case e: Exception => logError("Source class " + classPath + " cannot be instantiated", e)
      }
    }
  }

  private def registerSinks() {
    val instConfig = metricsConfig.getInstance(instance)
    val sinkConfigs = metricsConfig.subProperties(instConfig, MetricsSystem.SINK_REGEX)

    sinkConfigs.foreach { kv =>
      val classPath = kv._2.getProperty("class")
      if (null != classPath) {
        try {
          if (kv._1 == "prometheusServlet") {
            val servlet = Utils.classForName(classPath)
              .getConstructor(
                classOf[Properties],
                classOf[MetricRegistry],
                classOf[ArrayBuffer[Source]],
                classOf[String])
              .newInstance(kv._2, registry, sources, servletPath)
            prometheusServlet = Some(servlet.asInstanceOf[PrometheusServlet])
          } else {
            val sink = Utils.classForName(classPath)
              .getConstructor(classOf[Properties], classOf[MetricRegistry])
              .newInstance(kv._2, registry)
            sinks += sink.asInstanceOf[Sink]
          }
        } catch {
          case e: Exception =>
            logError("Sink class " + classPath + " cannot be instantiated")
            throw e
        }
      }
    }
  }

}

object MetricsSystem {
  val SINK_REGEX: Regex = "^sink\\.(.+)\\.(.+)".r
  val SOURCE_REGEX: Regex = "^org.apache.celeborn.common.metrics.source\\.(.+)\\.(.+)".r

  val ROLE_WORKER = "Worker"
  val ROLE_MASTER = "Master"

  private[this] val MINIMAL_POLL_UNIT = TimeUnit.SECONDS
  private[this] val MINIMAL_POLL_PERIOD = 1

  def checkMinimalPollingPeriod(pollUnit: TimeUnit, pollPeriod: Int) {
    val period = MINIMAL_POLL_UNIT.convert(pollPeriod, pollUnit)
    if (period < MINIMAL_POLL_PERIOD) {
      throw new IllegalArgumentException("Polling period " + pollPeriod + " " + pollUnit +
        " below than minimal polling period ")
    }
  }

  def createMetricsSystem(
      instance: String,
      conf: CelebornConf,
      servletPath: String): MetricsSystem = {
    new MetricsSystem(instance, conf, servletPath)
  }
}
