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

import za.co.absa.commons.lang.CachingConverter
import za.co.absa.commons.version.Version._
import za.co.absa.spline.producer.model.v1_1
import za.co.absa.spline.producer.modelmapper.v1.TypesV1.AttrDef
import za.co.absa.spline.producer.modelmapper.v1.{RecursiveSchemaFinder, _}
import za.co.absa.spline.producer.{model => v1}

import scala.util.Try

class SparkSplineExecutionPlanComponentConverterFactory(agentVersion: String, plan1: v1.ExecutionPlan) extends ExecutionPlanComponentConverterFactory {

  override def execPlanNameExtractor: v1.ExecutionPlan => Option[v1_1.ExecutionPlan.Name] = _.extraInfo.get(FieldNamesV1.PlanExtraInfo.AppName).map(_.toString)

  override def operationNameExtractor: v1.OperationLike => Option[v1_1.OperationLike.Name] = _.extra.get(FieldNamesV1.OperationExtraInfo.Name).map(_.toString)

  override def expressionConverter: Option[CachingConverter {type To = v1_1.ExpressionLike}] = Some(_expressionConverter)

  override def attributeConverter: Option[CachingConverter {type To = v1_1.Attribute}] = Some(_attributeConverter)

  override def outputConverter: Option[OperationOutputConverter] = Some(_outputConverter)

  override def objectConverter: ObjectConverter = new SparkSplineObjectConverter(AttributeRefConverter, _expressionConverter)

  private val operationSchemaFinder = new RecursiveSchemaFinder(plan1.operations.all) with CachingConverter

  private val _expressionConverter = new SparkSplineExpressionConverter(AttributeRefConverter) with CachingConverter

  private val _attributeConverter = new SparkSplineAttributeConverter with CachingConverter {
    override protected def keyOf(attrDef: AttrDef): Key = attrDef(FieldNamesV1.AttributeDef.Id)
  }

  private val _outputConverter =
    new SparkSplineOperationOutputConverter(
      _attributeConverter,
      attrDefinitions,
      operationSchemaFinder.findSchemaForOpId,
      maybeADR
    )

  private def attrDefinitions = plan1.extraInfo(FieldNamesV1.PlanExtraInfo.Attributes).asInstanceOf[Seq[TypesV1.AttrDef]]

  private def maybeADR = if (isSplinePrior04) None else Some(SparkSpline04AttributeDependencyResolver)

  private def isSplinePrior04 = {
    val splineVersion = Try(semver"$agentVersion") getOrElse semver"0.3.0"
    splineVersion < semver"0.4.0"
  }
}
