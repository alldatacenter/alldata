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

import org.apache.calcite.plan.Contexts;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.JoinInfo;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.tools.RelBuilderFactory;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.planner.DrillRelBuilder;

import java.util.List;
import java.util.Set;

import static org.apache.calcite.rel.core.RelFactories.DEFAULT_AGGREGATE_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_FILTER_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_JOIN_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_MATCH_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_PROJECT_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_SET_OP_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_SORT_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_TABLE_SCAN_FACTORY;
import static org.apache.calcite.rel.core.RelFactories.DEFAULT_VALUES_FACTORY;
import static org.apache.drill.exec.planner.logical.DrillRel.DRILL_LOGICAL;

/**
 * Contains factory implementation for creating various Drill Logical Rel nodes.
 */

public class DrillRelFactories {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillRelFactories.class);
  public static final RelFactories.ProjectFactory DRILL_LOGICAL_PROJECT_FACTORY =
      new DrillProjectFactoryImpl();

  public static final RelFactories.FilterFactory DRILL_LOGICAL_FILTER_FACTORY =
      new DrillFilterFactoryImpl();

  public static final RelFactories.JoinFactory DRILL_LOGICAL_JOIN_FACTORY = new DrillJoinFactoryImpl();

  public static final RelFactories.AggregateFactory DRILL_LOGICAL_AGGREGATE_FACTORY = new DrillAggregateFactoryImpl();

  public static final RelFactories.SemiJoinFactory DRILL_SEMI_JOIN_FACTORY = new SemiJoinFactoryImpl();

  private static class SemiJoinFactoryImpl implements RelFactories.SemiJoinFactory {
    public RelNode createSemiJoin(RelNode left, RelNode right,
                                  RexNode condition) {
      final JoinInfo joinInfo = JoinInfo.of(left, right, condition);
      return DrillSemiJoinRel.create(left, right,
              condition, joinInfo.leftKeys, joinInfo.rightKeys);
    }
  }
  /**
   * A {@link RelBuilderFactory} that creates a {@link DrillRelBuilder} that will
   * create logical relational expressions for everything.
   */
  public static final RelBuilderFactory LOGICAL_BUILDER =
      DrillRelBuilder.proto(
          Contexts.of(DEFAULT_PROJECT_FACTORY,
              DEFAULT_FILTER_FACTORY,
              DEFAULT_JOIN_FACTORY,
              DRILL_SEMI_JOIN_FACTORY,
              DEFAULT_SORT_FACTORY,
              DEFAULT_AGGREGATE_FACTORY,
              DEFAULT_MATCH_FACTORY,
              DEFAULT_SET_OP_FACTORY,
              DEFAULT_VALUES_FACTORY,
              DEFAULT_TABLE_SCAN_FACTORY));

  /**
   * Implementation of {@link RelFactories.ProjectFactory} that returns a vanilla
   * {@link DrillProjectRel}.
   */
  private static class DrillProjectFactoryImpl implements RelFactories.ProjectFactory {
    @Override
    public RelNode createProject(RelNode child,
                                 List<? extends RexNode> childExprs, List<String> fieldNames) {
      final RelOptCluster cluster = child.getCluster();
      final RelDataType rowType =
          RexUtil.createStructType(cluster.getTypeFactory(), childExprs, fieldNames, null);

      return DrillProjectRel.create(cluster, child.getTraitSet().plus(DRILL_LOGICAL), child, childExprs, rowType);
    }
  }

  /**
   * Implementation of {@link RelFactories.FilterFactory} that
   * returns a vanilla {@link DrillFilterRel}.
   */
  private static class DrillFilterFactoryImpl implements RelFactories.FilterFactory {
    @Override
    public RelNode createFilter(RelNode child, RexNode condition, Set<CorrelationId> variablesSet) {
      return DrillFilterRel.create(child, condition);
    }
  }

  /**
   * Implementation of {@link RelFactories.JoinFactory} that returns a vanilla
   * {@link DrillJoinRel}.
   */
  private static class DrillJoinFactoryImpl implements RelFactories.JoinFactory {

    @Override
    public RelNode createJoin(RelNode left, RelNode right,
                              RexNode condition, Set<CorrelationId> variablesSet,
                              JoinRelType joinType, boolean semiJoinDone) {
      return new DrillJoinRel(left.getCluster(), left.getTraitSet().plus(DRILL_LOGICAL), left, right, condition, joinType);
    }

    @Override
    public RelNode createJoin(RelNode left, RelNode right,
                              RexNode condition, JoinRelType joinType,
                              Set<String> variablesStopped, boolean semiJoinDone) {
      return new DrillJoinRel(left.getCluster(), left.getTraitSet().plus(DRILL_LOGICAL), left, right, condition, joinType);
    }
  }

  /**
   * Implementation of {@link RelFactories.AggregateFactory} that returns a vanilla
   * {@link DrillAggregateRel}.
   */
  private static class DrillAggregateFactoryImpl implements RelFactories.AggregateFactory {

    @Override
    public RelNode createAggregate(RelNode input, ImmutableBitSet groupSet,
                                   com.google.common.collect.ImmutableList<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls) {
      return new DrillAggregateRel(input.getCluster(), input.getTraitSet().plus(DRILL_LOGICAL), input, groupSet, groupSets, aggCalls);
    }
  }
}
