/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.agent

import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import za.co.absa.commons.HierarchicalObjectFactory
import za.co.absa.spline.harvester.conf.{SQLFailureCaptureMode, SplineMode}
import za.co.absa.spline.harvester.dispatcher.LineageDispatcher
import za.co.absa.spline.harvester.iwd.IgnoredWriteDetectionStrategy
import za.co.absa.spline.harvester.postprocessing.PostProcessingFilter

import scala.Function.tupled
import scala.collection.JavaConverters._

abstract class AgentConfig extends BaseConfiguration

object AgentConfig {

  def empty: AgentConfig = from(Map.empty)

  def from(configuration: Configuration): AgentConfig = from(
    configuration
      .getKeys.asScala.toSeq.asInstanceOf[Seq[String]]
      .map(k => k -> configuration.getProperty(k)))

  def from(options: Iterable[(String, Any)]): AgentConfig =
    builder().config(options).build()

  def builder(): Builder = new Builder

  class Builder private[agent] {

    private var options: Map[String, Any] = Map.empty

    def config(key: String, value: Any): this.type = synchronized {
      options += key -> value
      this
    }

    def config(keyValuePairs: Iterable[(String, Any)]): this.type = synchronized {
      options ++= keyValuePairs
      this
    }

    def splineMode(mode: SplineMode): this.type = synchronized {
      options += ConfProperty.Mode -> mode.name
      this
    }

    def sqlFailureCaptureMode(mode: SQLFailureCaptureMode): this.type = synchronized {
      options += ConfProperty.SQLFailureCaptureMode -> mode.name
      this
    }

    def postProcessingFilter(filter: PostProcessingFilter): this.type = synchronized {
      options += ConfProperty.RootPostProcessingFilter -> filter
      this
    }

    def lineageDispatcher(dispatcher: LineageDispatcher): this.type = synchronized {
      options += ConfProperty.RootLineageDispatcher -> dispatcher
      this
    }

    def ignoredWriteDetectionStrategy(iwdStrategy: IgnoredWriteDetectionStrategy): this.type = synchronized {
      options += ConfProperty.IgnoreWriteDetectionStrategy -> iwdStrategy
      this
    }

    def build(): AgentConfig = new AgentConfig {
      options.foreach(tupled(addProperty))
    }
  }

  object ConfProperty {

    /**
     * How Spline should behave.
     *
     * @see [[SplineMode]]
     */
    val Mode = "spline.mode"

    /**
     * How Spline should handle failed SQL executions.
     *
     * @see [[SQLFailureCaptureMode]]
     */
    val SQLFailureCaptureMode = "spline.sql.failure.capture"

    /**
     * The UUID version that is used for ExecutionPlan ID.
     * Note: Hash based versions (3 and 5) produce deterministic IDs based on the ExecutionPlan body.
     */
    val ExecPlanUUIDVersion = "spline.internal.execPlan.uuid.version"

    /**
     * Lineage dispatcher name - defining namespace for rest of properties for that dispatcher
     */
    val RootLineageDispatcher = "spline.lineageDispatcher"

    /**
     * User defined filter: allowing modification and enrichment of generated lineage
     */
    val RootPostProcessingFilter = "spline.postProcessingFilter"

    /**
     * Strategy used to detect ignored writes
     */
    val IgnoreWriteDetectionStrategy = "spline.IWDStrategy"

    def dispatcherClassName(logicalName: String): String = s"$RootLineageDispatcher.$logicalName.${HierarchicalObjectFactory.ClassName}"

    def filterClassName(logicalName: String): String = s"$RootPostProcessingFilter.$logicalName.${HierarchicalObjectFactory.ClassName}"
  }

}
