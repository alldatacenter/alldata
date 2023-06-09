/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rules.FilterJoinRule;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.DrillRelBuilder;

import java.util.ArrayList;
import java.util.List;

public class DrillFilterJoinRules {
  /** Predicate that always returns true for any filter in OUTER join, and only true
   * for EQUAL or IS_DISTINCT_FROM over RexInputRef in INNER join. With this predicate,
   * the filter expression that return true will be kept in the JOIN OP.
   * Example:  INNER JOIN,   L.C1 = R.C2 and L.C3 + 100 = R.C4 + 100 will be kepted in JOIN.
   *                         L.C5 < R.C6 will be pulled up into Filter above JOIN.
   *           OUTER JOIN,   Keep any filter in JOIN.
  */
  public static final FilterJoinRule.Predicate EQUAL_IS_DISTINCT_FROM =
      new FilterJoinRule.Predicate() {
        public boolean apply(Join join, JoinRelType joinType, RexNode exp) {
          if (joinType != JoinRelType.INNER) {
            return true;  // In OUTER join, we could not pull-up the filter.
                          // All we can do is keep the filter with JOIN, and
                          // then decide whether the filter could be pushed down
                          // into LEFT/RIGHT.
          }

          List<RexNode> tmpLeftKeys = new ArrayList<>();
          List<RexNode> tmpRightKeys = new ArrayList<>();
          List<RelDataTypeField> sysFields = new ArrayList<>();
          List<Integer> filterNulls = new ArrayList<>();

          RexNode remaining = RelOptUtil.splitJoinCondition(sysFields, join.getLeft(), join.getRight(),
              exp, tmpLeftKeys, tmpRightKeys, filterNulls, null);

          return remaining.isAlwaysTrue();
        }
      };

  /** Predicate that always returns true for any filter in OUTER join, and only true
   * for strict EQUAL or IS_DISTINCT_FROM conditions (without any mathematical operations) over RexInputRef in INNER join.
   * With this predicate, the filter expression that return true will be kept in the JOIN OP.
   * Example:  INNER JOIN,   L.C1 = R.C2 will be kepted in JOIN.
   *                         L.C3 + 100 = R.C4 + 100, L.C5 < R.C6 will be pulled up into Filter above JOIN.
   *           OUTER JOIN,   Keep any filter in JOIN.
  */
  public static final FilterJoinRule.Predicate STRICT_EQUAL_IS_DISTINCT_FROM =
      new FilterJoinRule.Predicate() {
        public boolean apply(Join join, JoinRelType joinType, RexNode exp) {
          if (joinType != JoinRelType.INNER) {
            return true;
          }

          List<Integer> tmpLeftKeys = new ArrayList<>();
          List<Integer> tmpRightKeys = new ArrayList<>();
          List<Boolean> filterNulls = new ArrayList<>();

          RexNode remaining =
              RelOptUtil.splitJoinCondition(join.getLeft(), join.getRight(), exp, tmpLeftKeys, tmpRightKeys, filterNulls);

          return remaining.isAlwaysTrue();
        }
      };


  /** Rule that pushes predicates from a Filter into the Join below them. */
  public static final FilterJoinRule FILTER_INTO_JOIN =
      new FilterJoinRule.FilterIntoJoinRule(true, DrillRelFactories.LOGICAL_BUILDER, EQUAL_IS_DISTINCT_FROM);

  /** The same as above, but with Drill's operators. */
  public static final FilterJoinRule DRILL_FILTER_INTO_JOIN =
      new FilterJoinRule.FilterIntoJoinRule(true,
          DrillRelBuilder.proto(DrillRelFactories.DRILL_LOGICAL_PROJECT_FACTORY,
              DrillRelFactories.DRILL_LOGICAL_FILTER_FACTORY), STRICT_EQUAL_IS_DISTINCT_FROM);

  /** Rule that pushes predicates in a Join into the inputs to the join. */
  public static final FilterJoinRule JOIN_PUSH_CONDITION =
      new FilterJoinRule.JoinConditionPushRule(DrillRelFactories.LOGICAL_BUILDER, EQUAL_IS_DISTINCT_FROM);

}
