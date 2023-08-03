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

package com.netease.arctic.spark.sql.catalyst.analysis

import com.netease.arctic.spark.{ArcticSparkCatalog, ArcticSparkSessionCatalog}
import com.netease.arctic.spark.sql.ArcticExtensionUtils.buildCatalogAndIdentifier
import com.netease.arctic.spark.sql.catalyst.plans.{AlterArcticTableDropPartition, TruncateArcticTable}
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.writer.WriteMode
import com.netease.arctic.table.KeyedTable
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.analysis.{ResolvedDBObjectName, ResolvedTable}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.connector.catalog.TableCatalog
import org.apache.spark.sql.execution.command.CreateTableLikeCommand

/**
 * Rule for rewrite some spark commands to arctic's implementation.
 */
case class RewriteArcticCommand(sparkSession: SparkSession) extends Rule[LogicalPlan] {
  private def isCreateArcticTableLikeCommand(
                                              targetTable: TableIdentifier,
                                              provider: Option[String]): Boolean = {
    val (targetCatalog, _) = buildCatalogAndIdentifier(sparkSession, targetTable)
    isCreateArcticTable(targetCatalog, provider)
  }

  private def isCreateArcticTable(catalog: TableCatalog, provider: Option[String]): Boolean = {
    catalog match {
      case _: ArcticSparkCatalog => true
      case _: ArcticSparkSessionCatalog[_] =>
        provider.isDefined && provider.get.equalsIgnoreCase("arctic")
      case _ => false
    }
  }

  override def apply(plan: LogicalPlan): LogicalPlan = {
    import com.netease.arctic.spark.sql.ArcticExtensionUtils._
    plan match {
      // Rewrite the AlterTableDropPartition to AlterArcticTableDropPartition
      case DropPartitions(r: ResolvedTable, parts, ifExists, purge)
          if isArcticTable(r.table) =>
        AlterArcticTableDropPartition(r, parts, ifExists, purge)
      case t @ TruncateTable(r: ResolvedTable)
          if isArcticTable(r.table) =>
        TruncateArcticTable(t.child)

      case c @ CreateTableAsSelect(ResolvedDBObjectName(catalog: TableCatalog, _), _, _, tableSpec, options, _)
        if isCreateArcticTable(catalog, tableSpec.provider) =>
        var propertiesMap: Map[String, String] = tableSpec.properties
        var optionsMap: Map[String, String] = options
        if (options.contains("primary.keys")) {
          propertiesMap += ("primary.keys" -> options("primary.keys"))
        }
        optionsMap += (WriteMode.WRITE_MODE_KEY -> WriteMode.OVERWRITE_DYNAMIC.mode)
        val newTableSpec = tableSpec.copy(properties = propertiesMap)
        c.copy(tableSpec = newTableSpec, writeOptions = optionsMap)
      case CreateTableLikeCommand(targetTable, sourceTable, _, provider, properties, ifNotExists)
          if isCreateArcticTableLikeCommand(targetTable, provider) =>
        val (sourceCatalog, sourceIdentifier) = buildCatalogAndIdentifier(sparkSession, sourceTable)
        val (targetCatalog, targetIdentifier) = buildCatalogAndIdentifier(sparkSession, targetTable)
        val table = sourceCatalog.loadTable(sourceIdentifier)
        var targetProperties = properties
        targetProperties += ("provider" -> "arctic")
        table match {
          case keyedTable: ArcticSparkTable =>
            keyedTable.table() match {
              case table: KeyedTable =>
                targetProperties += ("primary.keys" -> String.join(
                  ",",
                  table.primaryKeySpec().fieldNames()))
              case _ =>
            }
          case _ =>
        }
        val tableSpec = TableSpec(
          properties = targetProperties.toMap,
          provider = provider,
          options = Map.empty,
          location = None,
          comment = None,
          serde = None,
          external = false)
        val seq: Seq[String] = Seq(targetTable.database.get, targetTable.identifier)
        val name = ResolvedDBObjectName(targetCatalog, seq)
        CreateTable(name, table.schema(), table.partitioning(), tableSpec, ifNotExists)
      case _ => plan
    }
  }
}
