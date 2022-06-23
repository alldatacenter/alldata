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

package za.co.absa.spline.harvester.builder

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.plans.logical.Join
import za.co.absa.spline.harvester.IdGeneratorsBundle
import za.co.absa.spline.harvester.converter.{DataConverter, DataTypeConverter}
import za.co.absa.spline.harvester.postprocessing.PostProcessor
import za.co.absa.spline.producer.model.DataOperation

class JoinNodeBuilder
  (operation: Join)
  (idGenerators: IdGeneratorsBundle, dataTypeConverter: DataTypeConverter, dataConverter: DataConverter, postProcessor: PostProcessor)
  extends GenericNodeBuilder(operation)(idGenerators, dataTypeConverter, dataConverter, postProcessor) with Logging {

  override def build(): DataOperation = {

    val duplicates = operation.output.groupBy(_.exprId).collect { case (exprId, attrs) if attrs.length > 1 => exprId }
    if (duplicates.nonEmpty) {
      logWarning(s"Duplicated attributes found in Join operation output, ExprIds of duplicates: $duplicates")
    }

    super.build()
  }

}
