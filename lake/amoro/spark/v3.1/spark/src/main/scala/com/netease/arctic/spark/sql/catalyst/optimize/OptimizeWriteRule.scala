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

package com.netease.arctic.spark.sql.catalyst.optimize

import com.netease.arctic.spark.{SparkSQLProperties, SupportSparkAdapter}
import com.netease.arctic.spark.sql.ArcticExtensionUtils.{isArcticIcebergRelation, isArcticRelation}
import com.netease.arctic.spark.sql.catalyst.plans.ArcticRowLevelWrite
import com.netease.arctic.spark.table.{ArcticIcebergSparkTable, ArcticSparkTable}
import com.netease.arctic.spark.util.DistributionAndOrderingUtil
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.{Expression, SortOrder}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation

case class OptimizeWriteRule(spark: SparkSession) extends Rule[LogicalPlan]
  with SupportSparkAdapter {
  override def apply(plan: LogicalPlan): LogicalPlan = {
    if (!optimizeWriteEnabled()) {
      plan
    } else {
      optimizeWritePlan(plan)
    }
  }

  // do optimize write for insert overwrite. insert into.
  // update will not enable optimize write for reason that we should
  // write update_before and update_after in same time.
  def optimizeWritePlan(plan: LogicalPlan): LogicalPlan = plan transformDown {
    case o @ OverwritePartitionsDynamic(r: DataSourceV2Relation, query, writeOptions, _)
        if isArcticRelation(r) =>
      val newQuery = distributionQuery(query, r.table, rowLevelOperation = false)
      val options = writeOptions + ("writer.distributed-and-ordered" -> "true")
      o.copy(query = newQuery, writeOptions = options)

    case o @ OverwriteByExpression(r: DataSourceV2Relation, _, query, writeOptions, _)
        if isArcticRelation(r) =>
      val newQuery = distributionQuery(query, r.table, rowLevelOperation = false)
      val options = writeOptions + ("writer.distributed-and-ordered" -> "true")
      o.copy(query = newQuery, writeOptions = options)

    case a @ AppendData(r: DataSourceV2Relation, query, writeOptions, _)
        if isArcticRelation(r) =>
      val newQuery = distributionQuery(query, r.table, rowLevelOperation = false)
      val options = writeOptions + ("writer.distributed-and-ordered" -> "true")
      a.copy(query = newQuery, writeOptions = options)

    case a @ AppendData(r: DataSourceV2Relation, query, _, _)
        if isArcticIcebergRelation(r) =>
      val newQuery = distributionQuery(query, r.table, rowLevelOperation = false)
      a.copy(query = newQuery)

    case o @ OverwriteByExpression(r: DataSourceV2Relation, _, query, _, _)
        if isArcticIcebergRelation(r) =>
      val newQuery = distributionQuery(query, r.table, rowLevelOperation = false)
      o.copy(query = newQuery)

    case o @ OverwritePartitionsDynamic(r: DataSourceV2Relation, query, _, _)
        if isArcticIcebergRelation(r) =>
      val newQuery = distributionQuery(query, r.table, rowLevelOperation = false)
      o.copy(query = newQuery)
  }

  def optimizeWriteEnabled(): Boolean = {
    val optimizeEnabled = spark.sessionState.conf.getConfString(
      SparkSQLProperties.OPTIMIZE_WRITE_ENABLED,
      SparkSQLProperties.OPTIMIZE_WRITE_ENABLED_DEFAULT)
    java.lang.Boolean.parseBoolean(optimizeEnabled)
  }

  private def distributionQuery(
      query: LogicalPlan,
      table: Table,
      rowLevelOperation: Boolean,
      writeBase: Boolean = true): LogicalPlan = {
    import org.apache.spark.sql.connector.expressions.{Expression => Expr}

    def toCatalyst(expr: Expr): Expression = sparkAdapter.expressions().toCatalyst(expr, query)

    val arcticTable = table match {
      case t: ArcticSparkTable => t.table()
      case t: ArcticIcebergSparkTable => t.table()
    }

    val distribution =
      DistributionAndOrderingUtil.buildTableRequiredDistribution(arcticTable, writeBase)
        .toSeq.map(e => toCatalyst(e))
        .asInstanceOf[Seq[Expression]]

    val queryWithDistribution = if (distribution.nonEmpty) {
      val partitionNum = conf.numShufflePartitions
      val pp = RepartitionByExpression(distribution, query, partitionNum)
      pp
    } else {
      query
    }

    val orderingExpressions = DistributionAndOrderingUtil.buildTableRequiredSortOrder(
      arcticTable,
      rowLevelOperation,
      writeBase)
    val ordering = orderingExpressions.toSeq
      .map(e => toCatalyst(e))
      .asInstanceOf[Seq[SortOrder]]

    val queryWithDistributionAndOrdering = if (ordering.nonEmpty) {
      Sort(ordering, global = false, child = queryWithDistribution)
    } else {
      queryWithDistribution
    }
    queryWithDistributionAndOrdering
  }

}
