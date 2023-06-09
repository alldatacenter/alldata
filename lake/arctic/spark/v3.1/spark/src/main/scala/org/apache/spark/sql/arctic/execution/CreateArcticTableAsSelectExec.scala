/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.arctic.execution

import com.netease.arctic.spark.table.ArcticSparkTable
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.util.CharVarcharUtils
import org.apache.spark.sql.connector.catalog.{Identifier, TableCatalog}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.ArcticTableWriteExec
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import scala.collection.JavaConverters.mapAsJavaMapConverter

case class CreateArcticTableAsSelectExec(
  catalog: TableCatalog,
  ident: Identifier,
  partitioning: Seq[Transform],
  plan: LogicalPlan,
  queryInsert: SparkPlan,
  validateQuery: SparkPlan,
  properties: Map[String, String],
  writeOptions: CaseInsensitiveStringMap,
  ifNotExists: Boolean
) extends ArcticTableWriteExec {

  var arcticTable: ArcticSparkTable = _

  override protected def run(): Seq[InternalRow] = {
    if (catalog.tableExists(ident)) {
      if (ifNotExists) {
        return Nil
      }

      throw new TableAlreadyExistsException(ident)
    }

    val schema = CharVarcharUtils.getRawSchema(queryInsert.schema).asNullable
    val table = catalog.createTable(
      ident, schema,
      partitioning.toArray, properties.asJava)
    table match {
      case table: ArcticSparkTable =>
        arcticTable = table
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported others table")
    }
    validateData()
    writeToTable(catalog, table, writeOptions, ident)
  }

  override def table: ArcticSparkTable = arcticTable

  override def left: SparkPlan = queryInsert

  override def right: SparkPlan = validateQuery
}
