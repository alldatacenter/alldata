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

package za.co.absa.spline.producer.modelmapper.v1

import za.co.absa.commons.lang.Converter
import za.co.absa.spline.producer.{model => v1}

class RecursiveSchemaFinder(operations: Seq[v1.OperationLike]) extends Converter {

  override type From = Int
  override type To = Option[TypesV1.Schema]

  private val operationById = operations.map(op => op.id -> op).toMap

  override def convert(opId: From): To = operationById(opId)
    .schema.asInstanceOf[Option[Seq[String]]]
    .orElse {
      // We assume that the graph is consistent in terms of schema definitions.
      // E.i. if the schema is unknown/undefined than it's unknown/undefined for every operation in the DAG.
      // Or if the schema is None because it's the same as the input's schema than EVERY input has the same schema by definition.
      // In either case it's enough to only traverse any of the inputs to resolve a schema if one is defined in the DAG.
      val maybeChildId = operationById(opId).childIds.headOption
      maybeChildId.flatMap(childId => this.convert(childId))
    }

  def findSchemaForOpId(opId: Int): TypesV1.Schema = this.convert(opId).getOrElse(Nil)
}
