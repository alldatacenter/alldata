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

package za.co.absa.spline.producer.modelmapper.v1


import za.co.absa.commons.graph.GraphImplicits._
import za.co.absa.commons.lang.CachingConverter
import za.co.absa.spline.producer.model.v1_1._
import za.co.absa.spline.producer.modelmapper.ModelMapper
import za.co.absa.spline.producer.{model => v1}

object ModelMapperV1 extends ModelMapper {

  override type P = v1.ExecutionPlan
  override type E = v1.ExecutionEvent

  private implicit object OpNav extends DAGNodeIdMapping[v1.OperationLike, Int] {

    override def selfId(op: v1.OperationLike): Int = op.id

    override def refIds(op: v1.OperationLike): Seq[Int] = op.childIds
  }

  override def fromDTO(plan1: v1.ExecutionPlan): ExecutionPlan = {

    val epccf = ExecutionPlanComponentConverterFactory.forPlan(plan1)

    val maybeAttributesConverter = epccf.attributeConverter
    val maybeExpressionConverter = epccf.expressionConverter
    val maybeOutputConverter = epccf.outputConverter
    val objectConverter = epccf.objectConverter
    val execPlanNameExtractor = epccf.execPlanNameExtractor
    val operationNameExtractor = epccf.operationNameExtractor

    val operationConverter = new OperationConverter(objectConverter, maybeOutputConverter, operationNameExtractor) with CachingConverter

    plan1.operations.all
      .sortedTopologically(reverse = true)
      .foreach(operationConverter.convert)

    val operations = asOperationsObject(operationConverter.values)
    val attributes = maybeAttributesConverter.map(_.values).getOrElse(Nil)

    val maybeExpressions =
      for {
        expressionConverter <- maybeExpressionConverter
        expressions = expressionConverter.values
        if expressions.nonEmpty
      } yield asExpressionsObject(expressions)

    ExecutionPlan(
      id = plan1.id,
      name = execPlanNameExtractor(plan1),
      operations = operations,
      attributes = attributes,
      expressions = maybeExpressions,
      systemInfo = NameAndVersion(plan1.systemInfo.name, plan1.systemInfo.version),
      agentInfo = plan1.agentInfo.map(ai => NameAndVersion(ai.name, ai.version)),
      extraInfo = plan1.extraInfo
    )
  }

  override def fromDTO(event: v1.ExecutionEvent): ExecutionEvent = {
    // Strictly speaking I should not have been doing this, and instead resolve a linked exec plan,
    // look at its `agentInfo` property, find a proper conversion rule and use that one for conversion.
    // But realistically speaking I don't believe there are many enough non-Spline v1 agents out there,
    // for the risk of misinterpreting the "durationNs" property in extras to be practically possible.
    // So I'm going to make a shortcut here for sake of performance and simplicity.
    val durationNs: Option[ExecutionEvent.DurationNs] = {
      event.extra.get(FieldNamesV1.EventExtraInfo.DurationNs).
        flatMap(PartialFunction.condOpt(_) {
          case num: Long => num
          case str: String if str.forall(_.isDigit) => str.toLong
        })
    }

    ExecutionEvent(
      planId = event.planId,
      timestamp = event.timestamp,
      durationNs = durationNs,
      error = event.error,
      extra = event.extra
    )
  }

  private def asOperationsObject(ops: Seq[OperationLike]) = {
    val (Some(write), reads, others) =
      ops.foldLeft((Option.empty[WriteOperation], Seq.empty[ReadOperation], Seq.empty[DataOperation])) {
        case (z, wop: WriteOperation) => z.copy(_1 = Some(wop))
        case (z, rop: ReadOperation) => z.copy(_2 = rop +: z._2)
        case (z, dop: DataOperation) => z.copy(_3 = dop +: z._3)
      }
    Operations(
      write = write,
      reads = reads,
      other = others
    )
  }

  private def asExpressionsObject(exprs: Seq[ExpressionLike]) = {
    val (funcs, consts) =
      exprs.foldLeft((Seq.empty[FunctionalExpression], Seq.empty[Literal])) {
        case (z, f: FunctionalExpression) => z.copy(_1 = f +: z._1)
        case (z, c: Literal) => z.copy(_2 = c +: z._2)
        case (z, _) => z
      }
    Expressions(
      functions = funcs,
      constants = consts
    )
  }
}
