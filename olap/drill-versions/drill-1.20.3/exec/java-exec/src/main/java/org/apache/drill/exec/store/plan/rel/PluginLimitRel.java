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
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.store.plan.PluginImplementor;

import java.io.IOException;
import java.util.List;

/**
 * Limit implementation for Drill plugins.
 */
public class PluginLimitRel extends DrillLimitRelBase implements PluginRel {

  public PluginLimitRel(RelOptCluster cluster, RelTraitSet traitSet,
      RelNode child, RexNode offset, RexNode fetch, boolean pushDown) {
    super(cluster, traitSet, child, offset, fetch, pushDown);
    assert getConvention() == child.getConvention();
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    return super.computeSelfCost(planner, mq).multiplyBy(0.05);
  }

  @Override
  public PluginLimitRel copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new PluginLimitRel(getCluster(), traitSet, inputs.get(0), offset, fetch, isPushDown());
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs, boolean pushDown) {
    return new PluginLimitRel(getCluster(), traitSet, inputs.get(0), offset, fetch, pushDown);
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
