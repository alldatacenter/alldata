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

import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.spline.harvester.IdGeneratorsBundle
import za.co.absa.spline.harvester.converter.{DataConverter, DataTypeConverter, OperationParamsConverter}
import za.co.absa.spline.harvester.postprocessing.PostProcessor
import za.co.absa.spline.producer.model.DataOperation

class GenericNodeBuilder
  (val operation: LogicalPlan)
  (val idGenerators: IdGeneratorsBundle, val dataTypeConverter: DataTypeConverter, val dataConverter: DataConverter, postProcessor: PostProcessor)
  extends OperationNodeBuilder {

  override protected type R = DataOperation

  protected lazy val operationParamsConverter =
    new OperationParamsConverter(dataConverter, exprToRefConverter)

  override def build(): DataOperation = {
    val dop = DataOperation(
      id = operationId,
      name = operation.nodeName.asOption,
      childIds = childIds.asOption,
      output = outputAttributes.map(_.id).asOption,
      params = operationParamsConverter.convert(operation).asOption,
      extra = None
    )

    postProcessor.process(dop)
  }
}
