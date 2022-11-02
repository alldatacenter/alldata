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

import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import za.co.absa.commons.reflect.ReflectionUtils._
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.harvester.builder.UnionNodeBuilder.ExtraFields
import za.co.absa.spline.harvester.plugin.embedded.DataSourceV2Plugin.{IsByName, `_: V2WriteCommand`}
import za.co.absa.spline.producer.model.{DataOperation, ExecutionPlan, WriteOperation}

import scala.language.reflectiveCalls

class AttributeReorderingFilter extends AbstractInternalPostProcessingFilter {

  override def processExecutionPlan(plan: ExecutionPlan, ctx: HarvestingContext): ExecutionPlan = {
    val isByName = plan
      .operations.write.params
      .flatMap(_.get(IsByName))
      .exists(_.asInstanceOf[Boolean])

    if (isByName)
      addSyntheticReorderingSelect(plan, ctx)
    else
      plan
  }

  private def addSyntheticReorderingSelect(plan: ExecutionPlan, ctx: HarvestingContext): ExecutionPlan = {

    val writeOp = plan.operations.write
    val writeChildOutput = getWriteChildOutput(plan, writeOp)

    val reorderedOutput = newOrder(ctx)
      .zip(writeChildOutput)
      .sortBy { case (order, attribute) => order }
      .map { case (order, attribute) => attribute }

    val syntheticProjection = DataOperation(
      id = ctx.idGenerators.operationIdGenerator.nextId(),
      name = Some("Project"),
      childIds = Some(writeOp.childIds),
      output = Some(reorderedOutput),
      params = None,
      extra = Some(Map(ExtraFields.Synthetic -> true))
    )

    plan.copy(
      operations = plan.operations.copy(
        write = writeOp.copy(childIds = Seq(syntheticProjection.id)),
        other = Some(plan.operations.other.getOrElse(Seq.empty) :+ syntheticProjection)
      )
    )
  }

  private def newOrder(ctx: HarvestingContext): Seq[Int] = ctx.logicalPlan match {
    case `_: V2WriteCommand`(writeCommand) =>
      val namedRelation = extractFieldValue[AnyRef](writeCommand, "table")
      val finalOutput = extractFieldValue[Seq[Attribute]](namedRelation, "output")
      val query = extractFieldValue[LogicalPlan](writeCommand, "query")

      query.output.map(att => finalOutput.indexWhere(_.name == att.name))
  }

  private def getWriteChildOutput(plan: ExecutionPlan, writeOp: WriteOperation): Seq[String] = {
    import za.co.absa.commons.ProducerApiAdapters._

    val Seq(writeChildId) = writeOp.childIds

    plan.operations.all.find(_.id == writeChildId).get.output
  }
}

object AttributeReorderingFilter {

  object `_: V2WriteCommand` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.V2WriteCommand")

}
