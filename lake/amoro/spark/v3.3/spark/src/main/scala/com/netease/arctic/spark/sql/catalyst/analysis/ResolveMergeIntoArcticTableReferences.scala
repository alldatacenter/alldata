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

import com.netease.arctic.spark.sql.ArcticExtensionUtils.isArcticRelation
import com.netease.arctic.spark.sql.catalyst.plans.{MergeIntoArcticTable, UnresolvedMergeIntoArcticTable}
import com.netease.arctic.spark.table.ArcticSparkTable
import org.apache.spark.sql.{AnalysisException, SparkSession}
import org.apache.spark.sql.catalyst.analysis.{caseInsensitiveResolution, withPosition, AnalysisErrorAt, EliminateSubqueryAliases, GetColumnByOrdinal, Resolver, UnresolvedAttribute, UnresolvedExtractValue}
import org.apache.spark.sql.catalyst.expressions.{Alias, Attribute, CurrentDate, CurrentTimestamp, Expression, ExtractValue, LambdaFunction}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.catalyst.trees.CurrentOrigin.withOrigin
import org.apache.spark.sql.catalyst.util.toPrettySQL
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation

case class ResolveMergeIntoArcticTableReferences(spark: SparkSession) extends Rule[LogicalPlan] {

  def checkConditionIsPrimaryKey(aliasedTable: LogicalPlan, cond: Expression): Unit = {
    EliminateSubqueryAliases(aliasedTable) match {
      case r @ DataSourceV2Relation(tbl, _, _, _, _) if isArcticRelation(r) =>
        tbl match {
          case arctic: ArcticSparkTable =>
            if (arctic.table().isKeyedTable) {
              val primaryKeys = arctic.table().asKeyedTable().primaryKeySpec().fieldNames()
              val attributes = aliasedTable.output.filter(p => primaryKeys.contains(p.name))
              val condRefs = cond.references.filter(f => attributes.contains(f))
              if (condRefs.isEmpty) {
                throw new UnsupportedOperationException(
                  s"Condition ${cond.references}. is not allowed because is not a primary key")
              }
            }
        }
      case p =>
        throw new UnsupportedOperationException(s"$p is not an Arctic table")
    }
  }

  override def apply(plan: LogicalPlan): LogicalPlan = plan resolveOperatorsUp {
    case m @ UnresolvedMergeIntoArcticTable(
          aliasedTable,
          source,
          cond,
          matchedActions,
          notMatchedActions) =>
      checkConditionIsPrimaryKey(aliasedTable, cond)

      val resolvedMatchedActions = matchedActions.map {
        case DeleteAction(cond) =>
          val resolvedCond = cond.map(resolveCond("DELETE", _, m))
          DeleteAction(resolvedCond)

        case UpdateAction(cond, assignments) =>
          val resolvedUpdateCondition = cond.map(resolveCond("UPDATE", _, m))
          // for UPDATE *, the value must be from the source table
          val resolvedAssignments =
            resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true)
          UpdateAction(resolvedUpdateCondition, resolvedAssignments)

        case UpdateStarAction(updateCondition) =>
          val resolvedUpdateCondition = updateCondition.map(resolveCond("UPDATE", _, m))
          val assignments = aliasedTable.output.map { attr =>
            Assignment(attr, UnresolvedAttribute(Seq(attr.name)))
          }
          // for UPDATE *, the value must be from the source table
          val resolvedAssignments =
            resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true)
          UpdateAction(resolvedUpdateCondition, resolvedAssignments)

        case _ =>
          throw new UnsupportedOperationException(
            "Matched actions can only contain UPDATE or DELETE")
      }

      val resolvedNotMatchedActions = notMatchedActions.map {
        case InsertAction(cond, assignments) =>
          val resolvedCond = cond.map(resolveCond("INSERT", _, Project(Nil, m.sourceTable)))
          val resolvedAssignments =
            resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true)
          InsertAction(resolvedCond, resolvedAssignments)
        case InsertStarAction(cond) =>
          // the insert action is used when not matched, so its condition and value can only
          // access columns from the source table
          val resolvedCond = cond.map(resolveCond("INSERT", _, Project(Nil, m.sourceTable)))
          val assignments = aliasedTable.output.map { attr =>
            Assignment(attr, UnresolvedAttribute(Seq(attr.name)))
          }
          val resolvedAssignments =
            resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true)
          InsertAction(resolvedCond, resolvedAssignments)

        case _ =>
          throw new UnsupportedOperationException("Not matched actions can only contain INSERT")
      }

      val resolvedMergeCondition = resolveCond("SEARCH", cond, m)

      MergeIntoArcticTable(
        aliasedTable,
        source,
        mergeCondition = resolvedMergeCondition,
        matchedActions = resolvedMatchedActions,
        notMatchedActions = resolvedNotMatchedActions)
  }

  private def resolveLiteralFunction(
      nameParts: Seq[String],
      attribute: UnresolvedAttribute,
      plan: LogicalPlan): Option[Expression] = {
    if (nameParts.length != 1) return None
    val isNamedExpression = plan match {
      case Aggregate(_, aggregateExpressions, _) => aggregateExpressions.contains(attribute)
      case Project(projectList, _) => projectList.contains(attribute)
      case Window(windowExpressions, _, _, _) => windowExpressions.contains(attribute)
      case _ => false
    }
    val wrapper: Expression => Expression =
      if (isNamedExpression) f => Alias(f, toPrettySQL(f))() else identity
    // support CURRENT_DATE and CURRENT_TIMESTAMP
    val literalFunctions = Seq(CurrentDate(), CurrentTimestamp())
    val name = nameParts.head
    val func = literalFunctions.find(e => caseInsensitiveResolution(e.prettyName, name))
    func.map(wrapper)
  }

  def resolveExpressionBottomUp(
      expr: Expression,
      plan: LogicalPlan,
      throws: Boolean = false): Expression = {
    if (expr.resolved) return expr
    try {
      expr transformUp {
        case GetColumnByOrdinal(ordinal, _) => plan.output(ordinal)
        case u @ UnresolvedAttribute(nameParts) =>
          val result =
            withPosition(u) {
              plan.resolve(nameParts, resolver)
                .orElse(resolveLiteralFunction(nameParts, u, plan))
                .getOrElse(u)
            }
          logDebug(s"Resolving $u to $result")
          result
        case UnresolvedExtractValue(child, fieldName) if child.resolved =>
          ExtractValue(child, fieldName, resolver)
      }
    } catch {
      case a: AnalysisException if !throws => expr
    }
  }

  private def resolveCond(condName: String, cond: Expression, plan: LogicalPlan): Expression = {
    val resolvedCond = resolveExpressionBottomUp(cond, plan)

    val unresolvedAttrs = resolvedCond.references.filter(!_.resolved)
    if (unresolvedAttrs.nonEmpty) {
      throw new UnsupportedOperationException(
        s"Cannot resolve ${unresolvedAttrs.map(_.sql).mkString("[", ",", "]")} in $condName condition " +
          s"of MERGE operation given input columns: ${plan.inputSet.toSeq.map(_.sql).mkString("[", ",", "]")}")
    }

    resolvedCond
  }

  def resolver: Resolver = conf.resolver

  def resolveExpressionByPlanChildren(
      e: Expression,
      q: LogicalPlan): Expression = {
    resolveExpression(
      e,
      resolveColumnByName = nameParts => {
        q.resolveChildren(nameParts, resolver)
      },
      getAttrCandidates = () => {
        assert(q.children.length == 1)
        q.children.head.output
      },
      throws = true)
  }

  private def resolveExpression(
      expr: Expression,
      resolveColumnByName: Seq[String] => Option[Expression],
      getAttrCandidates: () => Seq[Attribute],
      throws: Boolean): Expression = {
    def innerResolve(e: Expression, isTopLevel: Boolean): Expression = {
      if (e.resolved) return e
      e match {
        case f: LambdaFunction if !f.bound => f

        case GetColumnByOrdinal(ordinal, _) =>
          val attrCandidates = getAttrCandidates()
          assert(ordinal >= 0 && ordinal < attrCandidates.length)
          attrCandidates(ordinal)

        case u @ UnresolvedAttribute(nameParts) =>
          val result = withPosition(u) {
            resolveColumnByName(nameParts).map {
              case Alias(child, _) if !isTopLevel => child
              case other => other
            }.getOrElse(u)
          }
          logDebug(s"Resolving $u to $result")
          result

        case u @ UnresolvedExtractValue(child, fieldName) =>
          val newChild = innerResolve(child, isTopLevel = false)
          if (newChild.resolved) {
            withOrigin(u.origin) {
              ExtractValue(newChild, fieldName, resolver)
            }
          } else {
            u.copy(child = newChild)
          }

        case _ => e.mapChildren(innerResolve(_, isTopLevel = false))
      }
    }

    try {
      innerResolve(expr, isTopLevel = true)
    } catch {
      case _: AnalysisException if !throws => expr
    }
  }

  // copied from ResolveReferences in Spark
  private def resolveAssignments(
      assignments: Seq[Assignment],
      mergeInto: UnresolvedMergeIntoArcticTable,
      resolveValuesWithSourceOnly: Boolean): Seq[Assignment] = {
    assignments.map { assign =>
      val resolvedKey = assign.key match {
        case c if !c.resolved =>
          resolveMergeExprOrFail(c, Project(Nil, mergeInto.targetTable))
        case o => o
      }
      val resolvedValue = assign.value match {
        // The update values may contain target and/or source references.
        case c if !c.resolved =>
          if (resolveValuesWithSourceOnly) {
            resolveMergeExprOrFail(c, Project(Nil, mergeInto.sourceTable))
          } else {
            resolveMergeExprOrFail(c, mergeInto)
          }
        case o => o
      }
      Assignment(resolvedKey, resolvedValue)
    }
  }

  // copied from ResolveReferences in Spark
  private def resolveMergeExprOrFail(e: Expression, p: LogicalPlan): Expression = {
    val resolved = resolveExpressionByPlanChildren(e, p)
    resolved.references.filter(!_.resolved).foreach { a =>
      // Note: This will throw error only on unresolved attribute issues,
      // not other resolution errors like mismatched data types.
      val cols = p.inputSet.toSeq.map(_.sql).mkString(", ")
      a.failAnalysis(s"cannot resolve ${a.sql} in MERGE command given columns [$cols]")
    }
    resolved
  }
}
