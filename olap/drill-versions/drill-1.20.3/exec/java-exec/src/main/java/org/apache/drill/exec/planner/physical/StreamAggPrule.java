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

import java.util.List;

import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.util.BitSets;

import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.planner.logical.DrillAggregateRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.AggPrelBase.OperatorPhase;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.util.trace.CalciteTrace;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;

public class StreamAggPrule extends AggPruleBase {
  public static final RelOptRule INSTANCE = new StreamAggPrule();
  protected static final Logger tracer = CalciteTrace.getPlannerTracer();

  private StreamAggPrule() {
    super(RelOptHelper.some(DrillAggregateRel.class, RelOptHelper.any(RelNode.class)), "StreamAggPrule");
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    return PrelUtil.getPlannerSettings(call.getPlanner()).isStreamAggEnabled();
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillAggregateRel aggregate = call.rel(0);
    RelNode input = aggregate.getInput();
    final RelCollation collation = getCollation(aggregate);
    RelTraitSet traits;

    if (aggregate.containsDistinctCall()) {
      // currently, don't use StreamingAggregate if any of the logical aggrs contains DISTINCT
      return;
    }

    try {
      if (aggregate.getGroupSet().isEmpty()) {
        DrillDistributionTrait singleDist = DrillDistributionTrait.SINGLETON;
        final RelTraitSet singleDistTrait = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(singleDist);

        if (create2PhasePlan(call, aggregate)) {
          traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL);

          RelNode convertedInput = convert(input, traits);
          new SubsetTransformer<DrillAggregateRel, InvalidRelException>(call){

            @Override
            public RelNode convertChild(final DrillAggregateRel join, final RelNode rel) throws InvalidRelException {
              DrillDistributionTrait toDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
              RelTraitSet traits = newTraitSet(Prel.DRILL_PHYSICAL, toDist);
              RelNode newInput = convert(rel, traits);

              StreamAggPrel phase1Agg = new StreamAggPrel(
                  aggregate.getCluster(),
                  traits,
                  newInput,
                  aggregate.getGroupSet(),
                  aggregate.getGroupSets(),
                  aggregate.getAggCallList(),
                  OperatorPhase.PHASE_1of2);

              UnionExchangePrel exch =
                  new UnionExchangePrel(phase1Agg.getCluster(), singleDistTrait, phase1Agg);

              ImmutableBitSet newGroupSet = remapGroupSet(aggregate.getGroupSet());
              List<ImmutableBitSet> newGroupSets = Lists.newArrayList();
              for (ImmutableBitSet groupSet : aggregate.getGroupSets()) {
                newGroupSets.add(remapGroupSet(groupSet));
              }

              return  new StreamAggPrel(
                  aggregate.getCluster(),
                  singleDistTrait,
                  exch,
                  newGroupSet,
                  newGroupSets,
                  phase1Agg.getPhase2AggCalls(),
                  OperatorPhase.PHASE_2of2);
            }
          }.go(aggregate, convertedInput);

        } else {
          createTransformRequest(call, aggregate, input, singleDistTrait);
        }
      } else {
        // hash distribute on all grouping keys
        final DrillDistributionTrait distOnAllKeys =
            new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
                                       ImmutableList.copyOf(getDistributionField(aggregate, true)));

        traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collation).plus(distOnAllKeys);
        createTransformRequest(call, aggregate, input, traits);

        // hash distribute on one grouping key
        DrillDistributionTrait distOnOneKey =
            new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
                                       ImmutableList.copyOf(getDistributionField(aggregate, false)));

        traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collation).plus(distOnOneKey);
        // Temporarily commenting out the single distkey plan since a few tpch queries (e.g 01.sql) get stuck
        // in VolcanoPlanner.canonize() method. Note that the corresponding single distkey plan for HashAggr works
        // ok.  One possibility is that here we have dist on single key but collation on all keys, so that
        // might be causing some problem.
        /// TODO: re-enable this plan after resolving the issue.
        // createTransformRequest(call, aggregate, input, traits);

        if (create2PhasePlan(call, aggregate)) {
          traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL);
          RelNode convertedInput = convert(input, traits);

          new SubsetTransformer<DrillAggregateRel, InvalidRelException>(call){

            @Override
            public RelNode convertChild(final DrillAggregateRel aggregate, final RelNode rel) throws InvalidRelException {
              DrillDistributionTrait toDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
              RelTraitSet traits = newTraitSet(Prel.DRILL_PHYSICAL, collation, toDist);
              RelNode newInput = convert(rel, traits);

              StreamAggPrel phase1Agg = new StreamAggPrel(
                  aggregate.getCluster(),
                  traits,
                  newInput,
                  aggregate.getGroupSet(),
                  aggregate.getGroupSets(),
                  aggregate.getAggCallList(),
                  OperatorPhase.PHASE_1of2);

              int numEndPoints = PrelUtil.getSettings(phase1Agg.getCluster()).numEndPoints();

              HashToMergeExchangePrel exch =
                  new HashToMergeExchangePrel(phase1Agg.getCluster(), phase1Agg.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(distOnAllKeys),
                      phase1Agg, ImmutableList.copyOf(getDistributionField(aggregate, true)),
                      collation,
                      numEndPoints);

              ImmutableBitSet newGroupSet = remapGroupSet(aggregate.getGroupSet());
              List<ImmutableBitSet> newGroupSets = Lists.newArrayList();
              for (ImmutableBitSet groupSet : aggregate.getGroupSets()) {
                newGroupSets.add(remapGroupSet(groupSet));
              }

              return new StreamAggPrel(
                  aggregate.getCluster(),
                  exch.getTraitSet(),
                  exch,
                  newGroupSet,
                  newGroupSets,
                  phase1Agg.getPhase2AggCalls(),
                  OperatorPhase.PHASE_2of2);
            }
          }.go(aggregate, convertedInput);
        }
      }
    } catch (InvalidRelException e) {
      tracer.warn(e.toString());
    }
  }

  private void createTransformRequest(RelOptRuleCall call, DrillAggregateRel aggregate,
                                      RelNode input, RelTraitSet traits) throws InvalidRelException {

    final RelNode convertedInput = convert(input, traits);

    StreamAggPrel newAgg = new StreamAggPrel(
        aggregate.getCluster(),
        traits,
        convertedInput,
        aggregate.getGroupSet(),
        aggregate.getGroupSets(),
        aggregate.getAggCallList(),
        OperatorPhase.PHASE_1of1);

    call.transformTo(newAgg);
  }


  private RelCollation getCollation(DrillAggregateRel rel) {

    List<RelFieldCollation> fields = Lists.newArrayList();
    for (int group : BitSets.toIter(rel.getGroupSet())) {
      fields.add(new RelFieldCollation(group));
    }
    return RelCollations.of(fields);
  }
}
