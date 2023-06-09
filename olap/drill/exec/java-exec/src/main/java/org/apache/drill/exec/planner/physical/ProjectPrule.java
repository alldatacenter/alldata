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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.linq4j.Ord;

import org.apache.calcite.rel.RelCollations;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionType;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class ProjectPrule extends Prule {
  public static final RelOptRule INSTANCE = new ProjectPrule();

  private ProjectPrule() {
    super(RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(RelNode.class)), "ProjectPrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillProjectRel project = call.rel(0);
    final RelNode input = project.getInput();

    RelTraitSet traits = input.getTraitSet().plus(Prel.DRILL_PHYSICAL);
    RelNode convertedInput = convert(input, traits);

    // Maintain two different map for distribution trait and collation trait.
    // For now, the only difference comes from the way how cast function impacts propagating trait.
    final Map<Integer, Integer> distributionMap = getDistributionMap(project);
    final Map<Integer, Integer> collationMap = getCollationMap(project);

    boolean traitPull = new ProjectTraitPull(call, distributionMap, collationMap).go(project, convertedInput);

    if(!traitPull){
      call.transformTo(new ProjectPrel(project.getCluster(), convertedInput.getTraitSet(), convertedInput, project.getProjects(), project.getRowType()));
    }
  }

  private class ProjectTraitPull extends SubsetTransformer<DrillProjectRel, RuntimeException> {
    final Map<Integer, Integer> distributionMap;
    final Map<Integer, Integer> collationMap;

    public ProjectTraitPull(RelOptRuleCall call, Map<Integer, Integer> distributionMap, Map<Integer, Integer> collationMap) {
      super(call);
      this.distributionMap = distributionMap;
      this.collationMap = collationMap;
    }

    @Override
    public RelNode convertChild(DrillProjectRel project, RelNode rel) throws RuntimeException {
      DrillDistributionTrait childDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
      RelCollation childCollation = rel.getTraitSet().getTrait(RelCollationTraitDef.INSTANCE);


      DrillDistributionTrait newDist = convertDist(childDist, distributionMap);
      RelCollation newCollation = convertRelCollation(childCollation, collationMap);
      RelTraitSet newProjectTraits = newTraitSet(Prel.DRILL_PHYSICAL, newDist, newCollation);
      return new ProjectPrel(project.getCluster(), newProjectTraits, rel, project.getProjects(), project.getRowType());
    }

  }

  private DrillDistributionTrait convertDist(DrillDistributionTrait srcDist, Map<Integer, Integer> inToOut) {
    List<DistributionField> newFields = Lists.newArrayList();

    for (DistributionField field : srcDist.getFields()) {
      if (inToOut.containsKey(field.getFieldId())) {
        newFields.add(new DistributionField(inToOut.get(field.getFieldId())));
      }
    }

    // After the projection, if the new distribution fields is empty, or new distribution fields is a subset of
    // original distribution field, we should replace with either SINGLETON or RANDOM_DISTRIBUTED.
    if (newFields.isEmpty() || newFields.size() < srcDist.getFields().size()) {
      if (srcDist.getType() != DistributionType.SINGLETON) {
        return DrillDistributionTrait.RANDOM_DISTRIBUTED;
      } else {
        return DrillDistributionTrait.SINGLETON;
      }
    } else {
      return new DrillDistributionTrait(srcDist.getType(), ImmutableList.copyOf(newFields));
    }
  }

  private RelCollation convertRelCollation(RelCollation src, Map<Integer, Integer> inToOut) {
    List<RelFieldCollation> newFields = Lists.newArrayList();

    for ( RelFieldCollation field : src.getFieldCollations()) {
      if (inToOut.containsKey(field.getFieldIndex())) {
        newFields.add(new RelFieldCollation(inToOut.get(field.getFieldIndex()), field.getDirection(), field.nullDirection));
      }
    }

    if (newFields.isEmpty()) {
      return RelCollations.of();
    } else {
      return RelCollations.of(newFields);
    }
  }

  private Map<Integer, Integer> getDistributionMap(DrillProjectRel project) {
    Map<Integer, Integer> m = new HashMap<>();

    for (Ord<RexNode> node : Ord.zip(project.getProjects())) {
      // For distribution, either $0 or cast($0 as ...) would keep the distribution after projection.
      if (node.e instanceof RexInputRef) {
        m.put( ((RexInputRef) node.e).getIndex(), node.i);
      } else if (node.e.isA(SqlKind.CAST)) {
        RexNode operand = ((RexCall) node.e).getOperands().get(0);
        if (operand instanceof RexInputRef) {
          m.put(((RexInputRef) operand).getIndex(), node.i);
        }
      }
    }
    return m;

  }

  private Map<Integer, Integer> getCollationMap(DrillProjectRel project) {
    Map<Integer, Integer> m = new HashMap<>();

    for (Ord<RexNode> node : Ord.zip(project.getProjects())) {
      // For collation, only $0 will keep the sort-ness after projection.
      if (node.e instanceof RexInputRef) {
        m.put( ((RexInputRef) node.e).getIndex(), node.i);
      }
    }
    return m;

  }

}
