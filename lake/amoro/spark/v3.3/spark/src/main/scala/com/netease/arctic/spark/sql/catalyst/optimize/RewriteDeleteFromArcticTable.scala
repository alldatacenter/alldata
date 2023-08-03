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

import com.netease.arctic.spark.sql.ArcticExtensionUtils
import com.netease.arctic.spark.sql.ArcticExtensionUtils.{asTableRelation, isArcticRelation, ArcticTableHelper}
import com.netease.arctic.spark.sql.catalyst.plans.ArcticRowLevelWrite
import com.netease.arctic.spark.sql.utils.{ArcticRewriteHelper, ProjectingInternalRow, WriteQueryProjections}
import com.netease.arctic.spark.sql.utils.RowDeltaUtils.{DELETE_OPERATION, OPERATION_COLUMN}
import com.netease.arctic.spark.table.{ArcticSparkTable, SupportsExtendIdentColumns, SupportsRowLevelOperator}
import com.netease.arctic.spark.writer.WriteMode
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.arctic.catalyst.ArcticSpark33Helper
import org.apache.spark.sql.catalyst.expressions.{Alias, AttributeReference, Expression, Literal}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.v2.{DataSourceV2Relation, DataSourceV2ScanRelation}
import org.apache.spark.sql.types.StructType

case class RewriteDeleteFromArcticTable(spark: SparkSession) extends Rule[LogicalPlan]
  with ArcticRewriteHelper {

  def buildDeleteProjections(
      plan: LogicalPlan,
      targetRowAttrs: Seq[AttributeReference],
      isKeyedTable: Boolean): WriteQueryProjections = {
    val (frontRowProjection, backRowProjection) = if (isKeyedTable) {
      val frontRowProjection =
        Some(ProjectingInternalRow.newProjectInternalRow(plan, targetRowAttrs, isFront = true, 0))
      (frontRowProjection, null)
    } else {
      val attributes = plan.output.filter(r => r.name.equals("_file") || r.name.equals("_pos"))
      val frontRowProjection =
        Some(ProjectingInternalRow.newProjectInternalRow(
          plan,
          targetRowAttrs ++ attributes,
          isFront = true,
          0))
      (frontRowProjection, null)
    }
    WriteQueryProjections(frontRowProjection, backRowProjection)
  }

  override def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case DeleteFromTable(table, condition) if isArcticRelation(table) =>
      val r = asTableRelation(table)
      val upsertWrite = r.table.asUpsertWrite
      val scanBuilder = upsertWrite.newUpsertScanBuilder(r.options)
      pushFilter(scanBuilder, condition, r.output)
      val query = buildUpsertQuery(r, upsertWrite, scanBuilder, condition)
      var options: Map[String, String] = Map.empty
      options += (WriteMode.WRITE_MODE_KEY -> WriteMode.UPSERT.toString)
      val writeBuilder = ArcticSpark33Helper.newWriteBuilder(r.table, query.schema, options)
      val write = writeBuilder.build()

      val projections =
        buildDeleteProjections(query, r.output, ArcticExtensionUtils.isKeyedTable(r))
      ArcticRowLevelWrite(r, query, options, projections, Some(write))
  }

  def buildUpsertQuery(
      r: DataSourceV2Relation,
      upsert: SupportsRowLevelOperator,
      scanBuilder: SupportsExtendIdentColumns,
      condition: Expression): LogicalPlan = {
    r.table match {
      case table: ArcticSparkTable =>
        if (table.table().isUnkeyedTable) {
          if (upsert.requireAdditionIdentifierColumns()) {
            scanBuilder.withIdentifierColumns()
          }
        }
      case _ =>
    }
    val scan = scanBuilder.build()
    val outputAttr = toOutputAttrs(scan.readSchema(), r.output)
    val valuesRelation = DataSourceV2ScanRelation(r, scan, outputAttr)

    val matchValueQuery = Filter(condition, valuesRelation)
    val withOperation =
      Seq(Alias(Literal(DELETE_OPERATION), OPERATION_COLUMN)()) ++ matchValueQuery.output
    val deleteQuery = Project(withOperation, matchValueQuery)
    deleteQuery
  }

  def toOutputAttrs(schema: StructType, attrs: Seq[AttributeReference]): Seq[AttributeReference] = {
    val nameToAttr = attrs.map(_.name).zip(attrs).toMap
    schema.map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)()).map {
      a =>
        nameToAttr.get(a.name) match {
          case Some(ref) =>
            // keep the attribute id if it was present in the relation
            a.withExprId(ref.exprId)
          case _ =>
            // if the field is new, create a new attribute
            AttributeReference(a.name, a.dataType, a.nullable, a.metadata)()
        }
    }
  }
}
