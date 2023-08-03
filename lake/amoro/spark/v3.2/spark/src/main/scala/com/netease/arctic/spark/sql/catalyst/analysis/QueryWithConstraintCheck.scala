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

import com.netease.arctic.spark.{ArcticSparkCatalog, ArcticSparkSessionCatalog, SparkSQLProperties}
import com.netease.arctic.spark.sql.ArcticExtensionUtils.isArcticKeyedRelation
import com.netease.arctic.spark.sql.catalyst.plans.QueryWithConstraintCheckPlan
import com.netease.arctic.spark.table.ArcticSparkTable
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.{Alias, Attribute, EqualNullSafe, Expression, GreaterThan, Literal}
import org.apache.spark.sql.catalyst.expressions.aggregate.{AggregateExpression, Complete, Count}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.connector.catalog.{CatalogPlugin, TableCatalog}
import org.apache.spark.sql.execution.datasources.DataSourceAnalysis.resolver
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation

case class QueryWithConstraintCheck(spark: SparkSession) extends Rule[LogicalPlan] {

  override def apply(plan: LogicalPlan): LogicalPlan = plan resolveOperatorsUp {
    case a @ AppendData(r: DataSourceV2Relation, query, _, _, _)
        if checkDuplicatesEnabled() && isArcticKeyedRelation(r) =>
      val validateQuery = buildValidatePrimaryKeyDuplication(r, query)
      val checkDataQuery = QueryWithConstraintCheckPlan(query, validateQuery)
      a.copy(query = checkDataQuery)

    case a @ OverwritePartitionsDynamic(r: DataSourceV2Relation, query, _, _, _)
        if checkDuplicatesEnabled() && isArcticKeyedRelation(r) =>
      val validateQuery = buildValidatePrimaryKeyDuplication(r, query)
      val checkDataQuery = QueryWithConstraintCheckPlan(query, validateQuery)
      a.copy(query = checkDataQuery)

    case a @ OverwriteByExpression(r: DataSourceV2Relation, deleteExpr, query, _, _, _)
        if checkDuplicatesEnabled() && isArcticKeyedRelation(r) =>
      val validateQuery = buildValidatePrimaryKeyDuplication(r, query)
      var finalExpr: Expression = deleteExpr
      deleteExpr match {
        case expr: EqualNullSafe =>
          finalExpr = expr.copy(query.output.last, expr.right)
        case _ =>
      }
      val checkDataQuery = QueryWithConstraintCheckPlan(query, validateQuery)
      a.copy(query = checkDataQuery)

    case c @ CreateTableAsSelect(catalog, _, _, query, props, _, _)
        if checkDuplicatesEnabled() && isCreateKeyedTable(catalog, props) =>
      val primaries = props("primary.keys").split(",")
      val validateQuery = buildValidatePrimaryKeyDuplicationByPrimaries(primaries, query)
      val checkDataQuery = QueryWithConstraintCheckPlan(query, validateQuery)
      c.copy(query = checkDataQuery)
  }

  def checkDuplicatesEnabled(): Boolean = {
    java.lang.Boolean.valueOf(spark.sessionState.conf.getConfString(
      SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE,
      SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE_DEFAULT))
  }

  def isCreateKeyedTable(catalog: TableCatalog, props: Map[String, String]): Boolean = {
    catalog match {
      case _: ArcticSparkCatalog =>
        props.contains("primary.keys")
      case _: ArcticSparkSessionCatalog[_] =>
        props("provider").equalsIgnoreCase("arctic") && props.contains("primary.keys")
      case _ =>
        false
    }
  }

  def buildValidatePrimaryKeyDuplication(
      r: DataSourceV2Relation,
      query: LogicalPlan): LogicalPlan = {
    r.table match {
      case arctic: ArcticSparkTable =>
        if (arctic.table().isKeyedTable) {
          val primaries = arctic.table().asKeyedTable().primaryKeySpec().fieldNames()
          val attributes = query.output.filter(p => primaries.contains(p.name))
          val aggSumCol = Alias(
            AggregateExpression(Count(Literal(1)), Complete, isDistinct = false),
            SUM_ROW_ID_ALIAS_NAME)()
          val aggPlan = Aggregate(attributes, Seq(aggSumCol), query)
          val sumAttr = findOutputAttr(aggPlan.output, SUM_ROW_ID_ALIAS_NAME)
          val havingExpr = GreaterThan(sumAttr, Literal(1L))
          Filter(havingExpr, aggPlan)
        } else {
          throw new UnsupportedOperationException(s"UnKeyed table can not validate")
        }
    }
  }

  def buildValidatePrimaryKeyDuplicationByPrimaries(
      primaries: Array[String],
      query: LogicalPlan): LogicalPlan = {
    val attributes = query.output.filter(p => primaries.contains(p.name))
    val aggSumCol = Alias(
      AggregateExpression(Count(Literal(1)), Complete, isDistinct = false),
      SUM_ROW_ID_ALIAS_NAME)()
    val aggPlan = Aggregate(attributes, Seq(aggSumCol), query)
    val sumAttr = findOutputAttr(aggPlan.output, SUM_ROW_ID_ALIAS_NAME)
    val havingExpr = GreaterThan(sumAttr, Literal(1L))
    Filter(havingExpr, aggPlan)
  }

  protected def findOutputAttr(attrs: Seq[Attribute], attrName: String): Attribute = {
    attrs.find(attr => resolver(attr.name, attrName)).getOrElse {
      throw new UnsupportedOperationException(s"Cannot find $attrName in $attrs")
    }
  }

  final private val SUM_ROW_ID_ALIAS_NAME = "_sum_"

}
