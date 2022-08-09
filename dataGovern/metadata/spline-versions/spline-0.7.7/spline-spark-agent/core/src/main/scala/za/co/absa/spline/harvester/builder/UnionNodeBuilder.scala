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

package za.co.absa.spline.harvester.builder

import org.apache.spark.sql.catalyst.plans.logical.Union
import org.apache.spark.sql.catalyst.{expressions => sparkExprssions}
import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.spline.harvester.IdGeneratorsBundle
import za.co.absa.spline.harvester.builder.UnionNodeBuilder._
import za.co.absa.spline.harvester.converter.{DataConverter, DataTypeConverter}
import za.co.absa.spline.harvester.postprocessing.PostProcessor
import za.co.absa.spline.producer.model.{AttrOrExprRef, Attribute, FunctionalExpression}

class UnionNodeBuilder
(override val operation: Union)
  (idGenerators: IdGeneratorsBundle, dataTypeConverter: DataTypeConverter, dataConverter: DataConverter, postProcessor: PostProcessor)
  extends GenericNodeBuilder(operation)(idGenerators, dataTypeConverter, dataConverter, postProcessor) {

  private lazy val unionInputs: Seq[Seq[Attribute]] = inputAttributes.transpose

  override lazy val functionalExpressions: Seq[FunctionalExpression] =
    unionInputs
      .zip(operation.output)
      .map { case (input, output) => constructUnionFunction(input, output) }

  override lazy val outputAttributes: Seq[Attribute] =
    unionInputs
      .zip(functionalExpressions)
      .map { case (input, function) => constructUnionAttribute(input, function) }

  private def constructUnionFunction(
    inputSplineAttributes: Seq[Attribute],
    outputSparkAttribute: sparkExprssions.Attribute
  ) =
    FunctionalExpression(
      id = idGenerators.expressionIdGenerator.nextId(),
      dataType = dataTypeConverter
        .convert(outputSparkAttribute.dataType, outputSparkAttribute.nullable).id.asOption,
      childRefs = inputSplineAttributes.map(att => AttrOrExprRef(Some(att.id), None)).asOption,
      extra = Map(ExtraFields.Synthetic -> true).asOption,
      name = Names.Union,
      params = None
    )

  private def constructUnionAttribute(attributes: Seq[Attribute], function: FunctionalExpression) = {
    val attr1 = attributes.head
    Attribute(
      id = idGenerators.attributeIdGenerator.nextId(),
      dataType = function.dataType,
      childRefs = List(AttrOrExprRef(None, Some(function.id))).asOption,
      extra = Map(ExtraFields.Synthetic -> true).asOption,
      name = attr1.name
    )
  }

}

object UnionNodeBuilder {

  object Names {
    val Union = "union"
  }

  object ExtraFields {
    val Synthetic = "synthetic"
  }

}
