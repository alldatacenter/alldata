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

package za.co.absa.spline.producer.service.model

import za.co.absa.spline.persistence.model._

case class ExecutionPlanPersistentModel(

  // execution plan
  executionPlan: ExecutionPlan,
  executes: Edge, // ... (write) operation
  depends: Seq[Edge], // ... (on) data source
  affects: Edge, // ... data source

  // operation
  operations: Seq[Operation],
  follows: Seq[Edge], // ... operation
  readsFrom: Seq[Edge], // ... data source
  writesTo: Edge, // ... data source
  emits: Seq[Edge], // ... schema
  uses: Seq[Edge], // ... attribute or expression
  produces: Seq[Edge], // ... attribute

  // data source
  dataSources: Seq[DataSource],

  // schema
  schemas: Seq[Schema],
  consistsOf: Seq[Edge], // ... attributes

  // attribute
  attributes: Seq[Attribute],
  computedBy: Seq[Edge], // ... expression
  derivesFrom: Seq[Edge], // ... attribute

  // expression
  expressions: Seq[Expression],
  takes: Seq[Edge], // ... attribute or expression
)
