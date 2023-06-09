/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.catalyst.plans

import org.apache.spark.sql.catalyst.plans.logical.{Command, LogicalPlan, V2CreateTablePlan}
import org.apache.spark.sql.connector.catalog.{Identifier, TableCatalog}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.types.StructType

case class CreateArcticTableAsSelect(catalog: TableCatalog,
                                     tableName: Identifier,
                                     partitioning: Seq[Transform],
                                     query: LogicalPlan,
                                     validateQuery: LogicalPlan,
                                     properties: Map[String, String],
                                     writeOptions: Map[String, String],
                                     ignoreIfExists: Boolean) extends Command with V2CreateTablePlan {
  import com.netease.arctic.spark.sql.ArcticExtensionUtils._

  override def tableSchema: StructType = query.schema

  override def children: Seq[LogicalPlan] = Seq(query, validateQuery)

  override lazy val resolved: Boolean = childrenResolved && {
    // the table schema is created from the query schema, so the only resolution needed is to check
    // that the columns referenced by the table's partitioning exist in the query schema
    val references = partitioning.flatMap(_.references).toSet
    references.map(_.fieldNames).forall(query.schema.findNestedField(_).isDefined)
  }

  override def withPartitioning(rewritten: Seq[Transform]): V2CreateTablePlan = {
    this.copy(partitioning = rewritten)
  }
}
