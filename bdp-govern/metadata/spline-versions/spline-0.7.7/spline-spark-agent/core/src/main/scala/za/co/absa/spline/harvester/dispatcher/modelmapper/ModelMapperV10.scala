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

package za.co.absa.spline.harvester.dispatcher.modelmapper

import za.co.absa.spline.harvester.IdGeneratorsBundle
import za.co.absa.spline.harvester.converter.ExpressionConverter.{ExprExtra, ExprV1}
import za.co.absa.spline.producer.dto.v1_0
import za.co.absa.spline.producer.model._

import java.text.MessageFormat


object ModelMapperV10 extends ModelMapper[v1_0.ExecutionPlan, v1_0.ExecutionEvent] {

  object FieldsV1 {

    object ExecutionEventExtra {
      val DurationNs = "durationNs"
    }

    object ExecutionPlanExtra {
      val AppName = "appName"
      val Attributes = "attributes"
    }

    object OperationExtras {
      val Name = "name"
    }

    object Expression {
      val TypeHint: String = ExprV1.TypeHint
      val RefId = "refId"
      val Value = "value"
      val DataTypeId = "dataTypeId"
      val ExprType = "exprType"
      val Name = "name"
      val Symbol = "symbol"
      val Params = "params"
      val Child = "child"
      val Children = "children"
      val Alias = "alias"
    }

  }

  object ExprTypesV1 {
    val AttrRef = "expr.AttrRef"
    val Literal = "expr.Literal"
  }

  override def toDTO(plan: ExecutionPlan): Option[v1_0.ExecutionPlan] = {
    val exprById =
      plan.expressions
        .map(expressions =>
          (expressions.constants.getOrElse(Nil).groupBy(_.id) ++
            expressions.functions.getOrElse(Nil).groupBy(_.id)
            ).mapValues(_.head))
        .getOrElse(Map.empty)

    def toV1Operations(operations: Operations) =
      v1_0.Operations(
        toV1WriteOperation(operations.write),
        operations.reads.map(ops => ops.map(toV1ReadOperation)),
        operations.other.map(ops => ops.map(toV1DataOperation))
      )

    def toV1WriteOperation(operation: WriteOperation) =
      v1_0.WriteOperation(
        operation.outputSource,
        None,
        operation.append,
        toV1OperationId(operation.id),
        operation.childIds.map(toV1OperationId),
        operation.params.map(toV1OperationParams),
        Some(operation.extra.getOrElse(Map.empty) ++ operation.name.map(FieldsV1.OperationExtras.Name -> _))
      )

    def toV1ReadOperation(operation: ReadOperation) =
      v1_0.ReadOperation(
        Nil,
        operation.inputSources,
        toV1OperationId(operation.id),
        operation.output,
        operation.params.map(toV1OperationParams),
        Some(operation.extra.getOrElse(Map.empty) ++ operation.name.map(FieldsV1.OperationExtras.Name -> _))
      )

    def toV1DataOperation(operation: DataOperation) =
      v1_0.DataOperation(
        toV1OperationId(operation.id),
        operation.childIds.map(ids => ids.map(toV1OperationId)),
        operation.output,
        operation.params.map(toV1OperationParams),
        Some(operation.extra.getOrElse(Map.empty) ++ operation.name.map(FieldsV1.OperationExtras.Name -> _))
      )

    def toV1OperationId(opIdV11: String): Int = {
      val Array(opIdV1: Number) = new MessageFormat(IdGeneratorsBundle.OperationIdTemplate).parse(opIdV11)
      opIdV1.intValue()
    }

    def toV1SystemInfo(nav: NameAndVersion) = v1_0.SystemInfo(nav.name, nav.version)

    def toV1AgentInfo(nav: NameAndVersion) = v1_0.AgentInfo(nav.name, nav.version)

    def toV1OperationParams(params: Map[String, Any]): Map[String, Any] = {
      def convert(x: Any): Any = x match {
        case Some(v) => convert(v)
        case xs: Seq[_] => xs.map(convert)
        case ys: Map[String, _] => ys.mapValues(convert)
        case ref: AttrOrExprRef => refToV1Expression(ref)
        case _ => x
      }

      params.mapValues(convert)
    }

    def refToV1Expression(ref: AttrOrExprRef): Map[String, Any] = ref match {
      case AttrOrExprRef(None, Some(exprId)) => exprToV1Expression(exprById(exprId))
      case AttrOrExprRef(Some(attrId), None) => Map(
        FieldsV1.Expression.TypeHint -> ExprTypesV1.AttrRef,
        FieldsV1.Expression.RefId -> attrId
      )
    }

    def exprToV1Expression(expr: Product): Map[String, Any] = expr match {
      case lit: Literal => Map(
        FieldsV1.Expression.TypeHint -> ExprTypesV1.Literal,
        FieldsV1.Expression.Value -> lit.value,
        FieldsV1.Expression.DataTypeId -> lit.dataType
      )
      case fun: FunctionalExpression =>
        fun.extra.map(_ (FieldsV1.Expression.TypeHint)).foldLeft(Map.empty[String, Any]) {
          case (exprV1, typeHint) => (
            exprV1
              + (FieldsV1.Expression.TypeHint -> typeHint)
              ++ (
              typeHint match {
                case ExprV1.Types.Generic => Map(
                  FieldsV1.Expression.Name -> fun.name,
                  FieldsV1.Expression.DataTypeId -> fun.dataType,
                  FieldsV1.Expression.Children -> fun.childRefs.map(children => children.map(refToV1Expression)),
                  FieldsV1.Expression.ExprType -> fun.extra.map(_.get(ExprExtra.SimpleClassName)),
                  FieldsV1.Expression.Params -> fun.params
                )
                case ExprV1.Types.GenericLeaf => Map(
                  FieldsV1.Expression.Name -> fun.name,
                  FieldsV1.Expression.DataTypeId -> fun.dataType,
                  FieldsV1.Expression.ExprType -> fun.extra.map(_.get(ExprExtra.SimpleClassName)),
                  FieldsV1.Expression.Params -> fun.params
                )
                case ExprV1.Types.Alias => Map(
                  FieldsV1.Expression.Alias -> fun.name,
                  FieldsV1.Expression.Child -> fun.childRefs.map(children => refToV1Expression(children.head))
                )
                case ExprV1.Types.Binary => Map(
                  FieldsV1.Expression.Symbol -> fun.extra.map(_.get(ExprExtra.Symbol)),
                  FieldsV1.Expression.DataTypeId -> fun.dataType,
                  FieldsV1.Expression.Children -> fun.childRefs.map(children => children.map(refToV1Expression))
                )
                case ExprV1.Types.UDF => Map(
                  FieldsV1.Expression.Name -> fun.name,
                  FieldsV1.Expression.DataTypeId -> fun.dataType,
                  FieldsV1.Expression.Children -> fun.childRefs.map(children => children.map(refToV1Expression))
                )
                case ExprV1.Types.UntypedExpression => Map(
                  FieldsV1.Expression.Name -> fun.name,
                  FieldsV1.Expression.Children -> fun.childRefs.map(children => children.map(refToV1Expression)),
                  FieldsV1.Expression.ExprType -> fun.extra.map(_.get(ExprExtra.SimpleClassName)),
                  FieldsV1.Expression.Params -> fun.params
                )
              })
            )
        }
    }

    def toV1Attribute(attr: Attribute): Map[String, Any] = Map(
      "id" -> attr.id,
      "name" -> attr.name,
      "dataTypeId" -> attr.dataType
    )

    Some(v1_0.ExecutionPlan(
      plan.id,
      toV1Operations(plan.operations),
      toV1SystemInfo(plan.systemInfo),
      plan.agentInfo.map(toV1AgentInfo),
      Some(plan.extraInfo.getOrElse(Map.empty)
        ++ plan.name.map(FieldsV1.ExecutionPlanExtra.AppName -> _)
        ++ plan.attributes.map(FieldsV1.ExecutionPlanExtra.Attributes -> _.map(toV1Attribute))
      )
    ))
  }

  override def toDTO(event: ExecutionEvent): Option[v1_0.ExecutionEvent] = PartialFunction.condOpt(event) {
    case e if e.error.isEmpty =>
      v1_0.ExecutionEvent(
        event.planId,
        event.timestamp,
        event.error,
        Some(event.extra.getOrElse(Map.empty) ++ event.durationNs.map(FieldsV1.ExecutionEventExtra.DurationNs -> _))
      )
  }
}
