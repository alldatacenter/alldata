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
package org.apache.drill.exec.store.plan.rel;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.store.plan.PluginImplementor;

import java.io.IOException;

/**
 * Join implementation for Drill plugins.
 */
public class PluginJoinRel extends DrillJoinRelBase implements PluginRel {

  public PluginJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right,
      RexNode condition, JoinRelType joinType) {
    super(cluster, traits, left, right, condition, joinType);
  }

  @Override
  public Join copy(RelTraitSet traitSet, RexNode conditionExpr, RelNode left, RelNode right,
      JoinRelType joinType, boolean semiJoinDone) {
    return new PluginJoinRel(getCluster(), traitSet, left, right, conditionExpr, joinType);
  }

  @Override
  public void implement(PluginImplementor implementor) throws IOException {
    implementor.implement(this);
  }

  @Override
  public boolean canImplement(PluginImplementor implementor) {
    return implementor.canImplement(this);
  }
}
