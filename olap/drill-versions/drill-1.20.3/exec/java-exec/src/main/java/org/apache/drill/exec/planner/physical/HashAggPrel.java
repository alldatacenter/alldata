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
package org.apache.drill.exec.planner.physical;

import java.io.IOException;
import java.util.List;

import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;

public class HashAggPrel extends AggPrelBase implements Prel{

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HashAggPrel.class);

  public HashAggPrel(RelOptCluster cluster,
                     RelTraitSet traits,
                     RelNode child,
                     ImmutableBitSet groupSet,
                     List<ImmutableBitSet> groupSets,
                     List<AggregateCall> aggCalls,
                     OperatorPhase phase) throws InvalidRelException {
    super(cluster, traits, child, groupSet, groupSets, aggCalls, phase);
  }

  @Override
  public Aggregate copy(RelTraitSet traitSet, RelNode input, ImmutableBitSet groupSet, List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls) {
    try {
      return new HashAggPrel(getCluster(), traitSet, input, groupSet, groupSets, aggCalls, this.getOperatorPhase());
    } catch (InvalidRelException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    return super.computeHashAggCost(planner, mq);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {

    Prel child = (Prel) this.getInput();
    HashAggregate g = new HashAggregate(child.getPhysicalOperator(creator), operPhase, keys, aggExprs, 1.0f);

    return creator.addMetadata(this, g);

  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }
}
