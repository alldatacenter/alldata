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

import com.netease.arctic.spark.sql.catalyst.plans._
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.writer.WriteMode
import com.netease.arctic.spark.{ArcticSparkCatalog, SparkSQLProperties}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.aggregate.{AggregateExpression, Complete, Count}
import org.apache.spark.sql.catalyst.expressions.{Alias, And, Cast, EqualNullSafe, EqualTo, Expression, GreaterThan, Literal}
import org.apache.spark.sql.catalyst.plans.RightOuter
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation
import org.apache.spark.sql.types.LongType

import java.util

case class RewriteAppendArcticTable(spark: SparkSession) extends Rule[LogicalPlan] {

  import com.netease.arctic.spark.sql.ArcticExtensionUtils._

  override def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case a @ AppendData(r: DataSourceV2Relation, query, writeOptions, _) if isArcticRelation(r) =>
      val arcticRelation = asTableRelation(r)
      val upsertWrite = arcticRelation.table.asUpsertWrite
      val (newQuery, options) = if (upsertWrite.appendAsUpsert()) {
        val upsertQuery = rewriteAppendAsUpsertQuery(r, query)
        val upsertOptions = writeOptions + (WriteMode.WRITE_MODE_KEY -> WriteMode.UPSERT.mode)
        (upsertQuery, upsertOptions)
      } else {
        (query, writeOptions)
      }
      arcticRelation.table match {
        case tbl: ArcticSparkTable =>
          if (tbl.table().isKeyedTable) {
            val validateQuery = buildValidatePrimaryKeyDuplication(r, query)
            if (checkDuplicatesEnabled()) {
              AppendArcticData(arcticRelation, newQuery, validateQuery, options)
            } else {
              ReplaceArcticData(arcticRelation, newQuery, options)
            }
          } else {
            a
          }
      }
    case a @ OverwritePartitionsDynamic(r: DataSourceV2Relation, query, writeOptions, _)
      if checkDuplicatesEnabled() =>
      val arcticRelation = asTableRelation(r)
      arcticRelation.table match {
        case table: ArcticSparkTable =>
          if (table.table().isKeyedTable) {
            val validateQuery = buildValidatePrimaryKeyDuplication(r, query)
            OverwriteArcticPartitionsDynamic(arcticRelation, query, validateQuery, writeOptions)
          } else {
            a
          }
        case _ =>
          a
      }

    case a @ OverwriteByExpression(r: DataSourceV2Relation, deleteExpr, query, writeOptions, _)
      if checkDuplicatesEnabled() =>
      val arcticRelation = asTableRelation(r)
      arcticRelation.table match {
        case table: ArcticSparkTable =>
          if (table.table().isKeyedTable) {
            val validateQuery = buildValidatePrimaryKeyDuplication(r, query)
            var finalExpr: Expression = deleteExpr
            deleteExpr match {
              case expr: EqualNullSafe =>
                finalExpr = expr.copy(query.output.last, expr.right)
              case _ =>
            }
            OverwriteArcticDataByExpression(arcticRelation, finalExpr, query, validateQuery, writeOptions)

          } else {
            a
          }
        case _ =>
          a
      }

    case c @ CreateTableAsSelect(catalog, ident, parts, query, props, options, ifNotExists)
      if checkDuplicatesEnabled() =>
      catalog match {
        case _: ArcticSparkCatalog =>
          if (props.contains("primary.keys")) {
            val primaries = props("primary.keys").split(",")
            val than = GreaterThan(AggregateExpression(Count(Literal(1)), Complete, isDistinct = false), Cast(Literal(1), LongType))
            val alias = Alias(than, "count")()
            val attributes = query.output.filter(p => primaries.contains(p.name))
            val validateQuery = Aggregate(attributes, Seq(alias), query)
            CreateArcticTableAsSelect(
              catalog, ident, parts, query, validateQuery,
              props, options, ifNotExists)
          } else {
            c
          }
        case _ =>
          c
      }
  }

  def buildValidatePrimaryKeyDuplication(r: DataSourceV2Relation, query: LogicalPlan): LogicalPlan = {
    r.table match {
      case arctic: ArcticSparkTable =>
        if (arctic.table().isKeyedTable) {
          val primaries = arctic.table().asKeyedTable().primaryKeySpec().fieldNames()
          val than = GreaterThan(AggregateExpression(Count(Literal(1)), Complete, isDistinct = false), Cast(Literal(1), LongType))
          val alias = Alias(than, "count")()
          val attributes = query.output.filter(p => primaries.contains(p.name))
          Aggregate(attributes, Seq(alias), query)
        } else {
          throw new UnsupportedOperationException(s"UnKeyed table can not validate")
        }
    }
  }

  def checkDuplicatesEnabled(): Boolean = {
    java.lang.Boolean.valueOf(spark.sessionState.conf.
      getConfString(
        SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE,
        SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE_DEFAULT))
  }

  def buildJoinCondition(primaries: util.List[String], tableScan: LogicalPlan, insertPlan: LogicalPlan): Expression = {
    var i = 0
    var joinCondition: Expression = null
    val expressions = new util.ArrayList[Expression]
    while (i < primaries.size) {
      val primary = primaries.get(i)
      val primaryAttr = insertPlan.output.find(_.name == primary).get
      val joinAttribute = tableScan.output.find(_.name.replace("_arctic_before_", "") == primary).get
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
    query: LogicalPlan
  ): LogicalPlan = {
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

  private def buildKeyedTableInsertProjection(relation: LogicalPlan): LogicalPlan = {
    val output = relation.output
    val outputWithValues = output.map(a => {
      Alias(a, "_arctic_after_" + a.name)()
    })
    Project(outputWithValues, relation)
  }

  private def buildKeyedTableBeforeProject(relation: DataSourceV2Relation): LogicalPlan = {
    val output = relation.output
    val outputWithValues = output.map(a => {
      Alias(a, "_arctic_before_" + a.name)()
    })
    Project(outputWithValues, relation)
  }

}
