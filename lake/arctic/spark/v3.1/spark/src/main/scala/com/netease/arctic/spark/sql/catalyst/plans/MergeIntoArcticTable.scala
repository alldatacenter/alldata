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

package com.netease.arctic.spark.sql.catalyst.plans

import org.apache.spark.sql.arctic.catalyst.AssignmentHelper
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.plans.logical._

case class MergeIntoArcticTable(
  targetTable: LogicalPlan,
  sourceTable: LogicalPlan,
  mergeCondition: Expression,
  matchedActions: Seq[MergeAction],
  notMatchedActions: Seq[MergeAction],
  rewritePlan: Option[LogicalPlan] = None
) extends Command {

  lazy val aligned: Boolean = {
    val matchedActionsAligned = matchedActions.forall {
      case UpdateAction(_, assignments) =>
        AssignmentHelper.aligned(targetTable, assignments)
      case _: DeleteAction =>
        true
      case _ =>
        false
    }

    val notMatchedActionsAligned = notMatchedActions.forall {
      case InsertAction(_, assignments) =>
        AssignmentHelper.aligned(targetTable, assignments)
      case _ =>
        false
    }

    matchedActionsAligned && notMatchedActionsAligned
  }

  def condition: Option[Expression] = Some(mergeCondition)

  override def children: Seq[LogicalPlan] = if (rewritePlan.isDefined) {
    targetTable :: sourceTable :: rewritePlan.get :: Nil
  } else {
    targetTable :: sourceTable :: Nil
  }
}
