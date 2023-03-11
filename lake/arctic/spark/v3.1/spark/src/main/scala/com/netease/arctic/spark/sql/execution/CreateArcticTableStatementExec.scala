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

package com.netease.arctic.spark.sql.execution

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.connector.catalog.{Identifier, TableCatalog}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.V2CommandExec
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConverters

case class CreateArcticTableStatementExec(catalog: TableCatalog,
                                          ident: Identifier,
                                          structType: StructType,
                                          partitioning: Seq[Transform],
                                          map: Map[String, String],
                                          ignoreIfExists: Boolean) extends V2CommandExec {

  override protected def run(): Seq[InternalRow] = {
    if (!catalog.tableExists(ident)) {
      try {
        catalog.createTable(ident, structType, partitioning.toArray, JavaConverters.mapAsJavaMap(map));
      } catch {
        case _: TableAlreadyExistsException if ignoreIfExists =>
          logWarning(s"Table ${ident.name()} was created concurrently. Ignoring.")
      }
    } else if (!ignoreIfExists) {
      throw new TableAlreadyExistsException(ident)
    }

    Nil
  }

  override def output: Seq[Attribute] = Nil

  override def children: Seq[SparkPlan] = Nil

}
