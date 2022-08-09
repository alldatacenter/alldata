package za.co.absa.spline.persistence.atlas.conversion

import java.util.UUID

import org.apache.atlas.v1.model.instance.Id

import za.co.absa.spline.persistence.api.composition._
import za.co.absa.spline.persistence.atlas.model.Expression
import za.co.absa.spline.persistence.atlas.model._


/**
 * The class is responsible for conversion of [[Expression Spline expressions]] to [[za.co.absa.spline.persistence.atlas.model.Expression Atlas expressions]].
 * @param attributeMap A map with all attributes with the current lineage accessible via their unique ids
 * @param dataTypeIdMap A mapping from Spline data type ids to ids assigned by Atlas API.
 */
class ExpressionConverter(attributeMap : Map[UUID, za.co.absa.spline.persistence.api.composition.Attribute], dataTypeIdMap: Map[UUID, Id]) {

  /**
   * The method converts [[Expression Spline expression]] to [[za.co.absa.spline.persistence.atlas.model.Expression Atlas expression]].
   * @param qualifiedNamePrefix A prefix ensuring uniqueness of expression identifiers in the global context
   * @param expression A Spline expression
   * @return An Atlas expression
   */
  def convert(qualifiedNamePrefix: String, expression: expr.Expression): Expression = {
    val qualifiedName = qualifiedNamePrefix + "@" + getText(expression)
    val children = expression.children.zipWithIndex.map(i => convert(qualifiedName + "@" + i._2, i._1))
    val mainProperties = ExpressionCommonProperties(
      qualifiedName,
      getText(expression),
      getExpressionType(expression),
      dataTypeIdMap(getTypeUUID(expression)),
      children
    )

    expression match {
      case expr.Binary(symbol, _, _) => new BinaryExpression(mainProperties, symbol)
      case expr.AttrRef(attributeId) => new AttributeReferenceExpression(mainProperties, attributeId, getText(expression))
      case expr.Alias(alias, _) => new AliasExpression(mainProperties, alias)
      case expr.UDF(name, _, _) => new UserDefinedFunctionExpression(mainProperties, name)
      case _ => new Expression(mainProperties)
    }
  }

  private def getTypeUUID(e: expr.Expression): UUID = e match {
    case expr.Literal(_, dataTypeId) => dataTypeId
    case expr.Binary(_, dataTypeId, _) => dataTypeId
    case expr.Alias(_, child) => getTypeUUID(child)
    case expr.UDF(_, dataTypeId, _) => dataTypeId
    case expr.AttrRef(refId) => attributeMap(refId).dataTypeId
    case expr.Generic(_, dataTypeId, _, _, _) => dataTypeId
    case expr.GenericLeaf(_, dataTypeId, _, _) => dataTypeId
  }

  private def getExpressionType(e: expr.Expression): String = e match {
    case _: expr.Literal => "literal"
    case _: expr.Binary => "binary"
    case _: expr.Alias => "alias"
    case _: expr.UDF => "user-defined function"
    case _: expr.AttrRef => "attribute reference"
    case expr.Generic(_, _, _, exprType, _) => exprType
    case expr.GenericLeaf(_, _, exprType, _) => exprType
  }

  private def getText(e: expr.Expression): String = e match {
    case expr.Literal(value, _) => if (value != null) value.toString else ""
    case expr.Binary(symbol, _, children) =>
      def getOperandText(operand: expr.Expression) : String = operand match {
        case _: expr.Binary => s"(${getText(operand)})"
        case _ => getText(operand)
      }
      s"${getOperandText(children(0))} $symbol ${getOperandText(children(1))}"
    case expr.Alias(alias, child) => s"${getText(child)} AS $alias"
    case expr.UDF(name, _, children) => s"UDF:$name(${children.map(getText).mkString(", ")})"
    case expr.AttrRef(refId) => attributeMap.get(refId).map(_.name).getOrElse("")
    case expr.GenericLeaf(name, _, _, _) => name
    case expr.Generic(name, _, children, _, _) =>
      val childrenText = children.map(getText).mkString(", ")
      s"$name($childrenText)"
  }

}
