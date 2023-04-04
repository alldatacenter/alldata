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

import com.netease.arctic.spark.sql.ArcticExtensionUtils.buildCatalogAndIdentifier
import com.netease.arctic.spark.sql.catalyst.plans.{AlterArcticTableDropPartition, TruncateArcticTable}
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.writer.WriteMode
import com.netease.arctic.spark.{ArcticSparkCatalog, ArcticSparkSessionCatalog}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.analysis.ResolvedTable
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.command.CreateTableLikeCommand

/**
 * Rule for rewrite some spark commands to arctic's implementation.
 * @param sparkSession
 */
case class RewriteArcticCommand(sparkSession: SparkSession) extends Rule[LogicalPlan] {

  def isCreateArcticTableLikeCommand(targetTable: TableIdentifier, provider: Option[String]): Boolean = {
    val (targetCatalog, _) = buildCatalogAndIdentifier(sparkSession, targetTable)
    targetCatalog match {
      case _: ArcticSparkCatalog =>
        if (provider.isEmpty || provider.get.equalsIgnoreCase("arctic")) {
          true
        } else {
          throw new UnsupportedOperationException(s"Provider must be arctic or null when using ${classOf[ArcticSparkCatalog].getName}.")
        }
      case _: ArcticSparkSessionCatalog[_] =>
        provider.isDefined && provider.get.equalsIgnoreCase("arctic")
      case _ =>
        false
    }
  }

  override def apply(plan: LogicalPlan): LogicalPlan = {
    import com.netease.arctic.spark.sql.ArcticExtensionUtils._
    plan match {
      // Rewrite the AlterTableDropPartition to AlterArcticTableDropPartition
      case a@AlterTableDropPartition(r: ResolvedTable, parts, ifExists, purge, retainData)
        if isArcticTable(r.table) =>
        AlterArcticTableDropPartition(a.child, parts, ifExists, purge, retainData)
      case t@TruncateTable(r: ResolvedTable, partitionSpec)
        if isArcticTable(r.table) =>
        TruncateArcticTable(t.child, partitionSpec)
      case c@CreateTableAsSelect(catalog, _, _, _, props, options, _) if isArcticCatalog(catalog) =>
        var propertiesMap: Map[String, String] = props
        var optionsMap: Map[String, String] = options
        if (options.contains("primary.keys")) {
          propertiesMap += ("primary.keys" -> options("primary.keys"))
        }
        if (propertiesMap.contains("primary.keys")) {
          optionsMap += (WriteMode.WRITE_MODE_KEY -> WriteMode.OVERWRITE_DYNAMIC.mode)
        }
        c.copy(properties = propertiesMap, writeOptions = optionsMap)
      case c@CreateTableLikeCommand(targetTable, sourceTable, storage, provider, properties, ifNotExists)
        if isCreateArcticTableLikeCommand(targetTable, provider) => {
        val (sourceCatalog, sourceIdentifier) = buildCatalogAndIdentifier(sparkSession, sourceTable)
        val (targetCatalog, targetIdentifier) = buildCatalogAndIdentifier(sparkSession, targetTable)
        val table = sourceCatalog.loadTable(sourceIdentifier)
        var targetProperties = properties
        table match {
          case arcticTable: ArcticSparkTable if arcticTable.table().isKeyedTable =>
            targetProperties += ("primary.keys" -> String.join(",", arcticTable.table().asKeyedTable().primaryKeySpec().fieldNames()))
          case _ =>
        }
        targetProperties += ("provider" -> "arctic")
        CreateV2Table(targetCatalog, targetIdentifier,
          table.schema(), table.partitioning(), targetProperties, ifNotExists)
      }
      case _ => plan
    }
  }
}