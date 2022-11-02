/*
 * Copyright 2022 ABSA Group Limited
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

package za.co.absa.spline.harvester.postprocessing.metadata

import org.apache.commons.configuration.Configuration
import org.apache.commons.io.IOUtils
import org.apache.spark.internal.Logging
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper
import za.co.absa.spline.harvester.ExtraMetadataImplicits._
import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.harvester.json.HarvesterJsonSerDe.impl._
import za.co.absa.spline.harvester.postprocessing.PostProcessingFilter
import za.co.absa.spline.harvester.postprocessing.metadata.MetadataCollectingFilter.{RuleDef, createRuleDefs, evaluateRules}
import za.co.absa.spline.producer.model._

import java.net.URL
import scala.util.Try

class MetadataCollectingFilter(rulesMap: Map[BaseNodeName.Type, Seq[RuleDef]]) extends PostProcessingFilter {

  def this(conf: Configuration) = this(createRuleDefs(conf))

  override def name = "Metadata collecting"

  override def processExecutionEvent(event: ExecutionEvent, ctx: HarvestingContext): ExecutionEvent = {
    withEvaluatedValues(BaseNodeName.ExecutionEvent, event, ctx)(_ withAddedMetadata _)
  }

  override def processExecutionPlan(plan: ExecutionPlan, ctx: HarvestingContext): ExecutionPlan = {
    withEvaluatedValues(BaseNodeName.ExecutionPlan, plan, ctx)(_ withAddedMetadata _)
  }

  override def processReadOperation(read: ReadOperation, ctx: HarvestingContext): ReadOperation = {
    withEvaluatedValues(BaseNodeName.Read, read, ctx)(_ withAddedMetadata _)
  }

  override def processWriteOperation(write: WriteOperation, ctx: HarvestingContext): WriteOperation = {
    withEvaluatedValues(BaseNodeName.Write, write, ctx)(_ withAddedMetadata _)
  }

  override def processDataOperation(operation: DataOperation, ctx: HarvestingContext): DataOperation = {
    withEvaluatedValues(BaseNodeName.Operation, operation, ctx)(_ withAddedMetadata _)
  }

  private def withEvaluatedValues[A: ExtraAdder](name: BaseNodeName.Type, entity: A, ctx: HarvestingContext)(fn: (A, EvaluatedTemplate) => A): A = {
    rulesMap
      .get(name)
      .map(defs => {
        val values = evaluateRules(name, entity, defs, ctx)
        fn(entity, values)
      })
      .getOrElse(entity)
  }
}

object MetadataCollectingFilter extends Logging {

  val InjectRulesKey = "rules"

  case class RuleDef(nodeName: BaseNodeName.Type, predicate: Predicate, template: DataTemplate)

  private def createRuleDefs(conf: Configuration): Map[BaseNodeName.Type, Seq[RuleDef]] = {
    val rulesJsonOrUrl: String = conf.getRequiredString(InjectRulesKey)

    val rulesJson =
      Try(new URL(rulesJsonOrUrl))
        .toOption
        .map(IOUtils.toString) // load from URL, or
        .getOrElse(rulesJsonOrUrl) // treat it as JSON

    val extraDefMap = rulesJson
      .fromJson[Map[String, Map[String, Any]]]
      .toSeq
      .map {
        case (baseKey, extra) =>
          val (name, predicate) = PredicateParser.parse(baseKey)
          val template = TemplateParser.parse(extra)
          RuleDef(name, predicate, template)
      }
      .groupBy(_.nodeName)

    validate(extraDefMap)

    extraDefMap
  }

  private def evaluateRules(nodeName: BaseNodeName.Type, node: Any, defs: Seq[RuleDef], ctx: HarvestingContext): EvaluatedTemplate = {
    if (defs.isEmpty) {
      EvaluatedTemplate.empty
    }
    else {
      val bindings = contextBindings(ctx)
      val jsBindings = bindings + (nodeName -> node)
      val predicateBindings = bindings + ("@" -> node)

      defs
        .filter(_.predicate.eval(predicateBindings))
        .map(_.template.eval(jsBindings))
        .reduceLeftOption((t1, t2) => t1.merge(t2))
        .getOrElse(EvaluatedTemplate.empty)
    }
  }

  private def contextBindings(ctx: HarvestingContext): Map[String, Any] = Map(
    "logicalPlan" -> ctx.logicalPlan,
    "executedPlanOpt" -> ctx.executedPlanOpt,
    "session" -> ctx.session
  )

  private def validate(defsMap: Map[BaseNodeName.Type, Seq[RuleDef]]): Unit = {
    def checkLabelsNotPresent(defs: Seq[RuleDef]): Unit =
      defs
        .find(_.template.labels.nonEmpty)
        .map(d => throw new IllegalArgumentException(s"Labels are not supported for ${d.nodeName} node"))

    defsMap.get(BaseNodeName.Read).foreach(checkLabelsNotPresent)
    defsMap.get(BaseNodeName.Write).foreach(checkLabelsNotPresent)
    defsMap.get(BaseNodeName.Operation).foreach(checkLabelsNotPresent)
  }


}
