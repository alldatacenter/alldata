/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.converter

import org.apache.commons.lang3.StringUtils.substringAfter
import org.apache.spark.sql.catalyst.expressions.ExprId
import org.apache.spark.sql.catalyst.{expressions => sparkExprssions}
import za.co.absa.commons.lang.Converter
import za.co.absa.spline.harvester.IdGenerator
import za.co.absa.spline.harvester.converter.ReflectiveExtractor.extractProperties
import za.co.absa.spline.producer.model._

import scala.language.reflectiveCalls


class ExpressionConverter(
  idGen: IdGenerator[Any, String],
  dataTypeConverter: DataTypeConverter,
  exprTpRefConverter: => ExprToRefConverter
) extends Converter {

  import ExpressionConverter._
  import za.co.absa.commons.lang.OptionImplicits._

  override type From = sparkExprssions.Expression
  override type To = FunctionalExpression

  def convert(sparkExpr: From): To = sparkExpr match {

    case a: sparkExprssions.Alias =>
      FunctionalExpression(
        id = idGen.nextId(),
        dataType = convertDataType(a),
        childRefs = convertChildren(a).asOption,
        extra = createExtra(sparkExpr, ExprV1.Types.Alias).asOption,
        name = a.name,
        params = getExpressionParameters(a).asOption
      )

    case bo: sparkExprssions.BinaryOperator =>
      FunctionalExpression(
        id = idGen.nextId(),
        dataType = convertDataType(bo),
        childRefs = convertChildren(bo).asOption,
        extra = (createExtra(bo, ExprV1.Types.Binary) + (ExprExtra.Symbol -> bo.symbol)).asOption,
        name = bo.prettyName,
        params = getExpressionParameters(bo).asOption
      )

    case u: sparkExprssions.ScalaUDF =>
      FunctionalExpression(
        id = idGen.nextId(),
        dataType = convertDataType(u),
        childRefs = convertChildren(u).asOption,
        extra = createExtra(u, ExprV1.Types.UDF).asOption,
        name = u.udfName getOrElse u.function.getClass.getName,
        params = getExpressionParameters(u).asOption
      )

    case e: sparkExprssions.LeafExpression =>
      FunctionalExpression(
        id = idGen.nextId(),
        dataType = convertDataType(e),
        childRefs = None,
        extra = createExtra(e, ExprV1.Types.GenericLeaf).asOption,
        name = e.prettyName,
        params = getExpressionParameters(e).asOption
      )

    case _: sparkExprssions.WindowSpecDefinition
         | _: sparkExprssions.WindowFrame =>
      FunctionalExpression(
        id = idGen.nextId(),
        dataType = None,
        childRefs = convertChildren(sparkExpr).asOption,
        extra = createExtra(sparkExpr, ExprV1.Types.UntypedExpression).asOption,
        name = sparkExpr.prettyName,
        params = getExpressionParameters(sparkExpr).asOption
      )

    case e: sparkExprssions.Expression =>
      FunctionalExpression(
        id = idGen.nextId(),
        dataType = convertDataType(e),
        childRefs = convertChildren(e).asOption,
        extra = createExtra(e, ExprV1.Types.Generic).asOption,
        name = e.prettyName,
        params = getExpressionParameters(e).asOption
      )
  }

  private def convertDataType(expr: sparkExprssions.Expression) = {
    val dataType = dataTypeConverter.convert(expr.dataType, expr.nullable)
    Some(dataType.id)
  }

  private def convertChildren(e: sparkExprssions.Expression): Seq[AttrOrExprRef] = {
    e.children.map(exprTpRefConverter.convert)
  }

}

object ExpressionConverter {

  object ExprExtra {
    val SimpleClassName = "simpleClassName"
    val Symbol = "symbol"
  }

  object ExprV1 {
    val TypeHint = "_typeHint"

    object Types {
      val Alias = "expr.Alias"
      val Binary = "expr.Binary"
      val UDF = "expr.UDF"
      val GenericLeaf = "expr.GenericLeaf"
      val Generic = "expr.Generic"
      val UntypedExpression = "expr.UntypedExpression"
    }

  }

  private val IgnoredSparkExprPropNames: Set[String] = Set("children", "dataType", "nullable")
  private val IgnoredSparkExprPropTypes: Set[Class[_]] = Set(classOf[ExprId])

  private def getExpressionSimpleClassName(expr: sparkExprssions.Expression) = {
    val fullName = expr.getClass.getName
    val simpleName = substringAfter(fullName, "org.apache.spark.sql.catalyst.expressions.")
    if (simpleName.nonEmpty) simpleName else fullName
  }

  def createExtra(expr: sparkExprssions.Expression, typeHint: String) = Map(
    ExprExtra.SimpleClassName -> getExpressionSimpleClassName(expr),
    ExprV1.TypeHint -> typeHint
  )

  private def getExpressionParameters(e: sparkExprssions.Expression): Map[String, Any] = {
    val isChildExpression = PartialFunction.cond(_: Any) {
      case ei: sparkExprssions.Expression => e.containsChild(ei)
    }

    val renderedParams =
      for {
        (p, v) <- extractProperties(e)
        if v != null
        if !IgnoredSparkExprPropNames(p)
        if !IgnoredSparkExprPropTypes(v.getClass)
        if !isChildExpression(v)
        w <- ValueDecomposer.decompose(v, Unit)
      } yield p -> w

    renderedParams
  }
}
