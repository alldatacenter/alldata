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

package za.co.absa.spline.harvester.postprocessing

import org.apache.commons.configuration.Configuration
import za.co.absa.commons.CaptureGroupReplacer
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper
import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.harvester.postprocessing.DataSourcePasswordReplacingFilter._
import za.co.absa.spline.producer.model.{ReadOperation, WriteOperation}

import scala.util.matching.Regex

class DataSourcePasswordReplacingFilter(
  replacement: String,
  sensitiveNameRegexes: Seq[Regex],
  sensitiveValueRegexes: Seq[Regex]
) extends AbstractPostProcessingFilter("Password replace") {

  def this(conf: Configuration) = this(
    conf.getRequiredString(ReplacementKey),
    conf.getRequiredStringArray(SensitiveNameRegexesKey).map(_.r),
    conf.getRequiredStringArray(SensitiveValueRegexesKey).map(_.r)
  )

  override def processReadOperation(op: ReadOperation, ctx: HarvestingContext): ReadOperation =
    op.copy(
      inputSources = op.inputSources.map(filter),
      params = op.params.map(filter)
    )

  override def processWriteOperation(op: WriteOperation, ctx: HarvestingContext): WriteOperation =
    op.copy(
      outputSource = filter(op.outputSource),
      params = op.params.map(filter)
    )

  private val valueReplacer = new CaptureGroupReplacer(replacement)

  private def filter(str: String): String =
    valueReplacer.replace(str, sensitiveValueRegexes)

  private def filter(map: Map[String, _]): Map[String, _] = map.map {
    case (k, _) if sensitiveNameRegexes.exists(_.pattern.matcher(k).matches) => k -> replacement
    case (k, v) => k -> filter(v)
  }

  private def filter(a: Any): Any = a match {
    case str: String => filter(str)
    case seq: Seq[Any] => seq.map(filter)
    case opt: Some[Any] => opt.map(filter)
    case map: Map[String, Any] => filter(map)
    case x => x
  }
}

object DataSourcePasswordReplacingFilter {
  final val ReplacementKey = "replacement"
  final val SensitiveValueRegexesKey = "valueRegexes"
  final val SensitiveNameRegexesKey = "nameRegexes"
}
