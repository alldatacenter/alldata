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

import com.netease.arctic.spark.SparkSQLProperties
import com.netease.arctic.spark.sql.ArcticExtensionUtils
import com.netease.arctic.spark.sql.ArcticExtensionUtils.isArcticRelation
import com.netease.arctic.spark.sql.catalyst.plans
import com.netease.arctic.spark.sql.catalyst.plans.{ArcticRowLevelWrite, MergeIntoArcticTable, MergeRows}
import com.netease.arctic.spark.sql.utils.RowDeltaUtils.{DELETE_OPERATION, INSERT_OPERATION, OPERATION_COLUMN, UPDATE_OPERATION}
import com.netease.arctic.spark.sql.utils.{FieldReference, ProjectingInternalRow, WriteQueryProjections}
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.writer.WriteMode
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.arctic.catalyst.ExpressionHelper
import org.apache.spark.sql.catalyst.analysis.EliminateSubqueryAliases
import org.apache.spark.sql.catalyst.expressions.Literal.TrueLiteral
import org.apache.spark.sql.catalyst.expressions.{Alias, Attribute, AttributeReference, ExprId, Expression, IsNotNull, Literal}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.plans.{Inner, RightOuter}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.connector.expressions.NamedReference
import org.apache.spark.sql.execution.datasources.v2.{DataSourceV2Relation, DataSourceV2ScanRelation}
import org.apache.spark.sql.types.{IntegerType, StructType}

import scala.collection.{Seq, mutable}


case class RewriteMergeIntoTable(spark: SparkSession) extends Rule[LogicalPlan] {

  private final val ROW_FROM_SOURCE = "__row_from_source"
  private final val ROW_FROM_TARGET = "__row_from_target"

  private final val ROW_FROM_SOURCE_REF = FieldReference(ROW_FROM_SOURCE)
  private final val ROW_FROM_TARGET_REF = FieldReference(ROW_FROM_TARGET)

  override def apply(plan: LogicalPlan): LogicalPlan = plan resolveOperators {
    case MergeIntoArcticTable(aliasedTable, source, cond, matchedActions, notMatchedActions, None) =>

      EliminateSubqueryAliases(aliasedTable) match {
        case r @ DataSourceV2Relation(tbl, _, _, _, _) if isArcticRelation(r) =>
          val rewritePlan =
            buildRowLevelWritePlan(r, tbl, source, cond, matchedActions, notMatchedActions)

          rewritePlan

        case p =>
          throw new UnsupportedOperationException(s"$p is not an Arctic table")
      }
  }

  def buildRelationAndAttrs(relation: DataSourceV2Relation, cond: Expression, operationTable: Table):
  (Seq[Attribute], LogicalPlan) = {
    relation.table match {
      case arctic: ArcticSparkTable =>
        if (arctic.table().isKeyedTable) {
          val keyAttrs = {
            val primarys = arctic.table().asKeyedTable().primaryKeySpec().fieldNames()
            cond.references.filter(p => primarys.contains(p.name)).toSeq
          }
          val attrs = dedupAttrs(relation.output)
          (keyAttrs, relation.copy(table = operationTable, output = attrs))
        } else {
          val (keyAttrs, valuesRelation) = {
            if (arctic.requireAdditionIdentifierColumns()) {
              val scanBuilder = arctic.newUpsertScanBuilder(relation.options)
              scanBuilder.withIdentifierColumns()
              val scan = scanBuilder.build()
              val outputAttr = toOutputAttrs(scan.readSchema(), relation.output)
              val valuesRelation = DataSourceV2ScanRelation(relation, scan, outputAttr)
              val references = cond.references.toSeq
              (references, valuesRelation)
            } else {
              throw new UnsupportedOperationException(s"Can not build relation and keyAttrs for table $arctic")
            }
          }
          (keyAttrs, valuesRelation)
        }
    }
  }

  protected def toOutputAttrs(schema: StructType, attrs: Seq[AttributeReference]): Seq[AttributeReference] = {
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

  def buildWriteQueryProjections(
    plan: MergeRows,
    source: LogicalPlan,
    targetRowAttrs: Seq[AttributeReference],
    rowIdAttrs: Seq[Attribute],
    isKeyedTable: Boolean
  ): WriteQueryProjections = {
    val (frontRowProjection, backRowProjection) = if (isKeyedTable) {
      val frontRowProjection =
        Some(ProjectingInternalRow.newProjectInternalRow(plan, targetRowAttrs, isFront = true, 0))
      val backRowProjection =
        ProjectingInternalRow.newProjectInternalRow(source, targetRowAttrs, isFront = false, 1 + rowIdAttrs.size)
      (frontRowProjection, backRowProjection)
    } else {
      val frontRowProjection =
        Some(ProjectingInternalRow.newProjectInternalRow(plan, targetRowAttrs ++ rowIdAttrs, isFront = true, 0))
      val backRowProjection =
        ProjectingInternalRow.newProjectInternalRow(source, targetRowAttrs, isFront = false, 1 + rowIdAttrs.size)
      (frontRowProjection, backRowProjection)
    }
    WriteQueryProjections(frontRowProjection, backRowProjection)
  }

  def buildRowIdAttrs(relation: LogicalPlan): Seq[Attribute] = {
    val attributes = relation.output.filter(r => r.name.equals("_file") || r.name.equals("_pos"))
    attributes
  }

  // build a rewrite plan for sources that support row deltas
  private def buildRowLevelWritePlan(
    relation: DataSourceV2Relation,
    operationTable: Table,
    source: LogicalPlan,
    cond: Expression,
    matchedActions: Seq[MergeAction],
    notMatchedActions: Seq[MergeAction]
  ): ArcticRowLevelWrite = {
    // construct a scan relation and include all required metadata columns
    val rowAttrs = relation.output
    val (keyAttrs, readRelation) = buildRelationAndAttrs(relation, cond, operationTable)
    val rowIdAttrs = buildRowIdAttrs(readRelation)
    val readAttrs = readRelation.output

    // project an extra column to check if a target row exists after the join
    val targetTableProjExprs = readAttrs :+ Alias(TrueLiteral, ROW_FROM_TARGET)()
    val targetTableProj = Project(targetTableProjExprs, readRelation)

    // project an extra column to check if a source row exists after the join
    val sourceTableProjExprs = source.output :+ Alias(TrueLiteral, ROW_FROM_SOURCE)()
    val sourceTableProj = Project(sourceTableProjExprs, source)

    // use inner join if there is no NOT MATCHED action, unmatched source rows can be discarded
    // use right outer join in all other cases, unmatched source rows may be needed
    // also disable broadcasts for the target table to perform the cardinality check
    val joinType = if (notMatchedActions.isEmpty) Inner else RightOuter
    val joinHint = JoinHint(leftHint = Some(HintInfo(Some(NO_BROADCAST_HASH))), rightHint = None)
    val joinPlan = Join(targetTableProj, sourceTableProj, joinType, Some(cond), joinHint)

    val matchedConditions = matchedActions.map(actionCondition)
    val matchedOutputs = matchedActions.map(rowLevelWriteOutput(_, readRelation.output, source.output))

    val notMatchedConditions = notMatchedActions.map(actionCondition)
    val notMatchedOutputs = notMatchedActions.map(rowLevelWriteOutput(_, readRelation.output, source.output))

    val operationTypeAttr = AttributeReference(OPERATION_COLUMN, IntegerType, nullable = false)()
    val rowFromSourceAttr = resolveAttrRef(ROW_FROM_SOURCE_REF, joinPlan)
    val rowFromTargetAttr = resolveAttrRef(ROW_FROM_TARGET_REF, joinPlan)

    // merged rows must contain values for the operation type and all read attrs
    val mergeRowsOutput = buildMergeRowsOutput(matchedOutputs, notMatchedOutputs, operationTypeAttr +: readAttrs)

    val unMatchedRowNeedCheck = java.lang.Boolean.valueOf(spark.sessionState.conf.
      getConfString(
        SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE,
        SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE_DEFAULT)) && notMatchedOutputs.nonEmpty &&
      ArcticExtensionUtils.isKeyedTable(relation)

    val mergeRows = plans.MergeRows(
      isSourceRowPresent = IsNotNull(rowFromSourceAttr),
      isTargetRowPresent = if (notMatchedActions.isEmpty) TrueLiteral else IsNotNull(rowFromTargetAttr),
      matchedConditions = matchedConditions,
      matchedOutputs = matchedOutputs,
      notMatchedConditions = notMatchedConditions,
      notMatchedOutputs = notMatchedOutputs,
      rowIdAttrs = keyAttrs,
      matchedRowCheck = isMatchedRowCheckNeeded(matchedActions),
      unMatchedRowCheck = unMatchedRowNeedCheck,
      emitNotMatchedTargetRows = false,
      output = mergeRowsOutput,
      joinPlan)

    // build a plan to write the row delta to the table
    val writeRelation = relation.copy(table = operationTable)
    var options: Map[String, String] = Map.empty
    options += (WriteMode.WRITE_MODE_KEY -> WriteMode.MERGE.toString)
    val projections = buildWriteQueryProjections(
      mergeRows, source, rowAttrs, rowIdAttrs,
      ArcticExtensionUtils.isKeyedTable(relation))
    ArcticRowLevelWrite(writeRelation, mergeRows, options, projections)
  }

  private def actionCondition(action: MergeAction): Expression = {
    action.condition.getOrElse(TrueLiteral)
  }

  def dedupAttrs(attrs: Seq[AttributeReference]): Seq[AttributeReference] = {
    val exprIds = mutable.Set.empty[ExprId]
    attrs.flatMap { attr =>
      if (exprIds.contains(attr.exprId)) {
        None
      } else {
        exprIds += attr.exprId
        Some(attr)
      }
    }
  }

  private def rowLevelWriteOutput(
    action: MergeAction,
    targetOutput: Seq[Expression],
    sourceOutput: Seq[Attribute]
  ): Seq[Expression] = {

    action match {
      case _: UpdateAction =>
        Seq(Literal(UPDATE_OPERATION)) ++ targetOutput ++ sourceOutput

      case _: DeleteAction =>
        Seq(Literal(DELETE_OPERATION)) ++ targetOutput ++ sourceOutput

      case _: InsertAction =>
        Seq(Literal(INSERT_OPERATION)) ++ targetOutput ++ sourceOutput

      case other =>
        throw new UnsupportedOperationException(s"Unexpected action: $other")
    }
  }

  private def buildMergeRowsOutput(
    matchedOutputs: Seq[Seq[Expression]],
    notMatchedOutputs: Seq[Seq[Expression]],
    attrs: Seq[Attribute]
  ): Seq[Attribute] = {

    // collect all outputs from matched and not matched actions (ignoring DELETEs)
    val outputs = matchedOutputs.filter(_.nonEmpty) ++ notMatchedOutputs.filter(_.nonEmpty)

    // build a correct nullability map for output attributes
    // an attribute is nullable if at least one matched or not matched action may produce null
    val nullabilityMap = attrs.indices.map { index =>
      index -> outputs.exists(output => output(index).nullable)
    }.toMap

    attrs.zipWithIndex.map { case (attr, index) =>
      attr.withNullability(nullabilityMap(index))
    }
  }

  private def isMatchedRowCheckNeeded(actions: Seq[MergeAction]): Boolean = actions match {
    case Seq(DeleteAction(None)) => false
    case _ => true
  }


  private def resolveAttrRef(ref: NamedReference, plan: LogicalPlan): AttributeReference = {
    ExpressionHelper.resolveRef[AttributeReference](ref, plan)
  }
}
