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

import com.netease.arctic.spark.sql.catalyst.plans.{AlterArcticTableDropPartition, TruncateArcticTable}
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.writer.WriteMode
import com.netease.arctic.table.KeyedTable
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.analysis.ResolvedTable
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.command.CreateTableLikeCommand

/**
 * Rule for rewrite some spark commands to arctic's implementation.
 * @param sparkSession
 */
case class RewriteArcticCommand(sparkSession: SparkSession) extends Rule[LogicalPlan] {
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
      case CreateTableLikeCommand(targetTable, sourceTable, storage, provider, properties, ifNotExists)
        if provider.get != null && provider.get.equals("arctic") =>
          val (sourceCatalog, sourceIdentifier) = buildCatalogAndIdentifier(sparkSession, sourceTable)
          val (targetCatalog, targetIdentifier) = buildCatalogAndIdentifier(sparkSession, targetTable)
          val table = sourceCatalog.loadTable(sourceIdentifier)
          var targetProperties = properties
          targetProperties += ("provider" -> "arctic")
          table match {
            case keyedTable: ArcticSparkTable =>
              keyedTable.table() match {
                case table: KeyedTable =>
                  targetProperties += ("primary.keys" -> String.join(",", table.primaryKeySpec().fieldNames()))
                case _ =>
              }
            case _ =>
          }
          CreateV2Table(targetCatalog, targetIdentifier,
            table.schema(), table.partitioning(), targetProperties, ifNotExists)
      case _ => plan
    }
  }
}