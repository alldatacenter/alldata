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

import java.util

import com.netease.arctic.spark.sql.catalyst.plans._
import com.netease.arctic.spark.sql.utils.{ProjectingInternalRow, WriteQueryProjections}
import com.netease.arctic.spark.sql.utils.RowDeltaUtils.{OPERATION_COLUMN, UPDATE_OPERATION}
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.writer.WriteMode
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.{Alias, And, Attribute, EqualTo, Expression, Literal}
import org.apache.spark.sql.catalyst.plans.RightOuter
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation

case class RewriteAppendArcticTable(spark: SparkSession) extends Rule[LogicalPlan] {

  import com.netease.arctic.spark.sql.ArcticExtensionUtils._

  override def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case AppendData(r: DataSourceV2Relation, query, writeOptions, _)
        if isArcticRelation(r) && isUpsert(r) =>
      val upsertQuery = rewriteAppendAsUpsertQuery(r, query)
      val insertQuery = Project(
        Seq(Alias(Literal(UPDATE_OPERATION), OPERATION_COLUMN)()) ++ upsertQuery.output,
        upsertQuery)
      val insertAttribute =
        insertQuery.output.filter(_.name.contains("_arctic_before_"))
      val projections = buildInsertProjections(insertQuery, insertAttribute, isUpsert = true)
      val upsertOptions = writeOptions + (WriteMode.WRITE_MODE_KEY -> WriteMode.DELTAWRITE.mode)
      ArcticRowLevelWrite(r, insertQuery, upsertOptions, projections)
  }

  def buildInsertProjections(
      plan: LogicalPlan,
      targetRowAttrs: Seq[Attribute],
      isUpsert: Boolean): WriteQueryProjections = {
    val (frontRowProjection, backRowProjection) = if (isUpsert) {
      val frontRowProjection =
        Some(ProjectingInternalRow.newProjectInternalRow(plan, targetRowAttrs, isFront = true, 0))
      val backRowProjection =
        ProjectingInternalRow.newProjectInternalRow(plan, targetRowAttrs, isFront = false, 0)
      (frontRowProjection, backRowProjection)
    } else {
      val backRowProjection =
        ProjectingInternalRow.newProjectInternalRow(plan, targetRowAttrs, isFront = true, 0)
      (null, backRowProjection)
    }
    WriteQueryProjections(frontRowProjection, backRowProjection)
  }

  def buildJoinCondition(
      primaries: util.List[String],
      tableScan: LogicalPlan,
      insertPlan: LogicalPlan): Expression = {
    var i = 0
    var joinCondition: Expression = null
    val expressions = new util.ArrayList[Expression]
    while (i < primaries.size) {
      val primary = primaries.get(i)
      val primaryAttr = insertPlan.output.find(_.name == primary).get
      val joinAttribute =
        tableScan.output.find(_.name.replace("_arctic_before_", "") == primary).get
      val experssion = EqualTo(primaryAttr, joinAttribute)
      expressions.add(experssion)
      i += 1
    }
    expressions.forEach(experssion => {
      if (joinCondition == null) {
        joinCondition = experssion
      } else {
        joinCondition = And(joinCondition, experssion)
      }
    })
    joinCondition
  }

  def rewriteAppendAsUpsertQuery(
      r: DataSourceV2Relation,
      query: LogicalPlan): LogicalPlan = {
    r.table match {
      case arctic: ArcticSparkTable =>
        if (arctic.table().isKeyedTable) {
          val primaries = arctic.table().asKeyedTable().primaryKeySpec().fieldNames()
          val tablePlan = buildKeyedTableBeforeProject(r)
          // val insertPlan = buildKeyedTableInsertProjection(query)
          val joinCondition = buildJoinCondition(primaries, tablePlan, query)
          Join(tablePlan, query, RightOuter, Some(joinCondition), JoinHint.NONE)
        } else {
          query
        }
    }
  }

  private def buildKeyedTableBeforeProject(relation: DataSourceV2Relation): LogicalPlan = {
    val output = relation.output
    val outputWithValues = output.map(a => {
      Alias(a, "_arctic_before_" + a.name)()
    })
    Project(outputWithValues, relation)
  }

}
