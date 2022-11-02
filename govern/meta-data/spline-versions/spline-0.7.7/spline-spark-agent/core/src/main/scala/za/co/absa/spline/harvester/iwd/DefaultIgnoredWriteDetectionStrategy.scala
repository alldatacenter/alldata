/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.harvester.iwd

import org.apache.commons.configuration.Configuration
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper
import za.co.absa.spline.harvester.LineageHarvester.Metrics
import za.co.absa.spline.harvester.iwd.DefaultIgnoredWriteDetectionStrategy._

object DefaultIgnoredWriteDetectionStrategy {
  val OnMissingMetricsKey = "onMissingMetrics"

  object Behaviour {
    val IgnoreLineage = "IGNORE_LINEAGE"
    val CaptureLineage = "CAPTURE_LINEAGE"
  }
}

class DefaultIgnoredWriteDetectionStrategy(ignoreLineageOnMissingMetric: Boolean)
  extends IgnoredWriteDetectionStrategy {

  def this(configuration: Configuration) = this({
    val behaviour = configuration.getRequiredString(OnMissingMetricsKey)
    behaviour == Behaviour.IgnoreLineage
  })

  override def name = "Write metrics"

  override def wasWriteIgnored(writeMetrics: Metrics): Boolean = {
    writeMetrics.get("numFiles").map(_ == 0).getOrElse(ignoreLineageOnMissingMetric)
  }

}
