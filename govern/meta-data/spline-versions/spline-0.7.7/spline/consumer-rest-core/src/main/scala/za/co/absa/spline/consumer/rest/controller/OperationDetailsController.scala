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

package za.co.absa.spline.consumer.rest.controller

import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._
import za.co.absa.commons.lang.{CachingConverter, Converter}
import za.co.absa.spline.common.JsonPath
import za.co.absa.spline.consumer.rest.controller.OperationDetailsController.{NodeId, NodeToExprAssemblyConverter}
import za.co.absa.spline.consumer.service.model.ExpressionEdge.ExpressionEdgeType
import za.co.absa.spline.consumer.service.model._
import za.co.absa.spline.consumer.service.repo.{ExpressionRepository, OperationRepository}

import java.{util => ju}
import scala.annotation.tailrec
import scala.concurrent.Future


@RestController
@RequestMapping(Array("/operations"))
@Api(tags = Array("operations"))
class OperationDetailsController @Autowired()
(
  val operationRepo: OperationRepository,
  val expressionRepo: ExpressionRepository
) {

  import scala.concurrent.ExecutionContext.Implicits._

  @GetMapping(Array("/{operationId}"))
  @ApiOperation("Get Operation details")
  def operation(
    @ApiParam(value = "Id of the operation node to retrieve")
    @PathVariable("operationId") operationId: Operation.Id
  ): Future[OperationDetails] = {

    for {
      opDetails <- operationRepo.findById(operationId)
      exprGraph <- expressionRepo.expressionGraphUsedByOperation(operationId)
    } yield {
      opDetails.copy(
        dataTypes = reducedDataTypes(opDetails.dataTypes, opDetails.schemas),
        operation = opDetails.operation.copy(
          properties = attachExpressions(
            opDetails.operation.properties,
            exprGraph
          ))
      )
    }
  }

  def attachExpressions(opProps: Map[String, Any], expressionGraph: ExpressionGraph): Map[String, Any] = {
    val (uses, takes) = expressionGraph.edges.partition(_.`type` == ExpressionEdgeType.Uses)

    val nodeById: Map[String, ExpressionNode] =
      expressionGraph.nodes
        .map(node => node._id -> node)
        .toMap

    val operandMapping: Map[NodeId, Seq[NodeId]] = takes
      .groupBy(_.source)
      .mapValues(_.sortBy(_.index).map(_.target).toSeq)
      .view.force // see: https://github.com/scala/bug/issues/4776

    val nodeToExprConverter = new NodeToExprAssemblyConverter(nodeById, operandMapping) with CachingConverter {
      override protected def keyOf(node: ExpressionNode): Key = node._id
    }

    uses.foldLeft(opProps) { (z, u) =>
      val relPath = u.path.stripPrefix("$['params']")
      val jsonPath = JsonPath.parse(relPath)
      val nodeId = u.target
      val node = nodeById(nodeId)
      val expr = nodeToExprConverter.convert(node)
      jsonPath.set(z, expr)
    }
  }

  private def reducedDataTypes(dataTypes: Array[DataType], schemas: Array[Array[Attribute]]): Array[DataType] = {
    val dataTypesIdToKeep = schemas.flatten.map(attributeRef => attributeRef.dataTypeId).toSet
    val dataTypesSet = dataTypes.toSet
    dataTypesFilter(dataTypesSet, dataTypesIdToKeep).toArray
  }

  @tailrec
  private def dataTypesFilter(dataTypes: Set[DataType], dataTypesIdToKeep: Set[String]): Set[DataType] = {
    val dt = dataTypes.filter(dataType => dataTypesIdToKeep.contains(dataType.id))
    if (getAllIds(dt).size != dataTypesIdToKeep.size) {
      dataTypesFilter(dataTypes, getAllIds(dt))
    } else {
      dt
    }
  }

  private def getAllIds(dataTypes: Set[DataType]): Set[String] = {
    dataTypes.flatMap {
      case dt@(_: SimpleDataType) => Set(dt.id)
      case dt@(adt: ArrayDataType) => Set(dt.id, adt.elementDataTypeId)
      case dt@(sdt: StructDataType) => sdt.fields.map(attributeRef => attributeRef.dataTypeId).toSet ++ Set(dt.id)
    }
  }
}

object OperationDetailsController {
  private type NodeId = String
  private type Node = ju.Map[String, Any]
  private type Link = ju.Map[String, Any]
  private type ExprAssembly = Map[String, Any]

  private class NodeToExprAssemblyConverter(
    nodeById: Map[String, ExpressionNode],
    operandMapping: Map[NodeId, Seq[NodeId]]
  ) extends Converter {
    override type From = ExpressionNode
    override type To = ExprAssembly

    override def convert(node: ExpressionNode): ExprAssembly = {
      val params = node.params
      val extra = node.extra
      val typeHint = extra.getOrElse("_typeHint", "expr.AttrRef")

      def children: Seq[ExprAssembly] = {
        val nodeId = node._id
        operandMapping
          .getOrElse(nodeId, Nil)
          .map(nodeById)
          .map(convert)
      }

      Map(
        "_typeHint" -> typeHint,
        "dataTypeId" -> node.dataType,
        "name" -> node.name
      ) ++ (typeHint match {
        case "expr.Alias" => Map(
          "alias" -> params("name"),
          "child" -> children.head
        )
        case "expr.Binary" => Map(
          "symbol" -> extra("symbol"),
          "children" -> children
        )
        case "expr.Literal" => Map(
          "value" -> node.value
        )
        case "expr.AttrRef" => Map(
          "refId" -> node._id
        )
        case "expr.UDF" => Map(
          "children" -> children
        )
        case "expr.Generic" => Map(
          "exprType" -> extra("simpleClassName"),
          "params" -> params,
          "children" -> children
        )
        case "expr.GenericLeaf" => Map(
          "exprType" -> extra("simpleClassName"),
          "params" -> params
        )
        case "expr.UntypedExpression" => Map(
          "exprType" -> extra("simpleClassName"),
          "params" -> params,
          "children" -> children
        )
      })
    }
  }

}
