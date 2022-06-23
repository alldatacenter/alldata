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

package za.co.absa.spline.producer.modelmapper.v1.spark

import za.co.absa.spline.producer.model.v1_1.{AttrOrExprRef, ExpressionLike, FunctionalExpression, Literal}
import za.co.absa.spline.producer.modelmapper.v1.TypesV1.ExprDef
import za.co.absa.spline.producer.modelmapper.v1.{ExpressionConverter, FieldNamesV1, TypesV1}

import java.util.UUID

class SparkSplineExpressionConverter(
  attrRefConverter: AttributeRefConverter
) extends ExpressionConverter {

  import za.co.absa.commons.lang.OptionImplicits._

  override def isExpression(obj: Any): Boolean = PartialFunction.cond(obj) {
    case exprDef: TypesV1.ExprDef => exprDef
      .get(FieldNamesV1.ExpressionDef.TypeHint)
      .map(_.toString)
      .exists(typeHint => typeHint.startsWith("expr.") && typeHint != "expr.AttrRef")
  }

  override def convert(exprDef: TypesV1.ExprDef): ExpressionLike = {
    exprDef(FieldNamesV1.ExpressionDef.TypeHint) match {
      case "expr.Literal" => toLiteral(exprDef)
      case "expr.Alias" => toFunctionalExpression(exprDef, "alias")
      case "expr.Binary" => toFunctionalExpression(exprDef, exprDef(FieldNamesV1.ExpressionDef.Symbol))
      case "expr.UDF" => toFunctionalExpression(exprDef, exprDef(FieldNamesV1.ExpressionDef.Name))
      case "expr.Generic"
           | "expr.GenericLeaf"
           | "expr.UntypedExpression" =>
        toFunctionalExpression(exprDef, exprDef(FieldNamesV1.ExpressionDef.Name))
    }
  }

  private def toLiteral(exprDef: TypesV1.ExprDef) = Literal(
    id = newId,
    dataType = exprDef.get(FieldNamesV1.ExpressionDef.DataTypeId).map(_.toString),
    value = exprDef.get(FieldNamesV1.ExpressionDef.Value).orNull,
    extra = exprDef.filterKeys(FieldNamesV1.ExpressionDef.TypeHint.==)
      ++ exprDef.get(FieldNamesV1.ExpressionDef.ExprType).map("simpleClassName" -> _)
  )

  private def toFunctionalExpression(exprDef: TypesV1.ExprDef, name: Any): FunctionalExpression = {
    val children = exprDef
      .get(FieldNamesV1.ExpressionDef.Children)
      .orElse(exprDef.get(FieldNamesV1.ExpressionDef.Child).toList.asOption)
      .getOrElse(Nil)
      .asInstanceOf[Seq[ExprDef]]

    val childRefs = children.map {
      exprDef =>
        if (attrRefConverter.isAttrRef(exprDef))
          attrRefConverter.convert(exprDef)
        else
          AttrOrExprRef.exprRef(this.convert(exprDef).id)
    }

    FunctionalExpression(
      id = newId,
      dataType = exprDef.get(FieldNamesV1.ExpressionDef.DataTypeId).map(_.toString),
      name = name.toString,
      childRefs = childRefs,

      params = Map.empty
        ++ exprDef.getOrElse(FieldNamesV1.ExpressionDef.Params, Map.empty).asInstanceOf[ExpressionLike.Params]
        ++ exprDef.get(FieldNamesV1.ExpressionDef.Alias).map("name" -> _),

      extra = exprDef.filterKeys(Set(
        FieldNamesV1.ExpressionDef.TypeHint,
        FieldNamesV1.ExpressionDef.Symbol,
      )) ++ exprDef.get(FieldNamesV1.ExpressionDef.ExprType).map("simpleClassName" -> _)
    )
  }

  private def newId: String = UUID.randomUUID.toString
}
