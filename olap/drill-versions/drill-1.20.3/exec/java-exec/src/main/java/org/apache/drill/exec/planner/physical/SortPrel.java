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
import java.util.Iterator;
import java.util.List;

import org.apache.drill.exec.planner.common.DrillSortRelBase;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.RelCollationImpl;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rex.RexNode;

public class SortPrel extends DrillSortRelBase implements Prel {
  private final boolean isRemovable;

  /** Creates a DrillSortRel. */
  public SortPrel(RelOptCluster cluster, RelTraitSet traits, RelNode input, RelCollation collation) {
    super(cluster, traits, input, collation);
    isRemovable = true;
  }

  /** Creates a DrillSortRel with offset and fetch. */
  public SortPrel(RelOptCluster cluster, RelTraitSet traits, RelNode input, RelCollation collation, RexNode offset, RexNode fetch) {
    super(cluster, traits, input, collation, offset, fetch);
    isRemovable = true;
  }

  /** Creates a DrillSortRel. */
  public SortPrel(RelOptCluster cluster, RelTraitSet traits, RelNode input, RelCollation collation, boolean isRemovable) {
    super(cluster, traits, input, collation);
    this.isRemovable = isRemovable;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if(PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      //We use multiplier 0.05 for TopN operator, and 0.1 for Sort, to make TopN a preferred choice.
      return super.computeSelfCost(planner, mq).multiplyBy(.1);
    }

    RelNode child = this.getInput();
    double inputRows = mq.getRowCount(child);
    // int  rowWidth = child.getRowType().getPrecision();
    int numSortFields = this.collation.getFieldCollations().size();
    double cpuCost = DrillCostBase.COMPARE_CPU_COST * numSortFields * inputRows * (Math.log(inputRows)/Math.log(2));
    double diskIOCost = 0; // assume in-memory for now until we enforce operator-level memory constraints

    // TODO: use rowWidth instead of avgFieldWidth * numFields
    // avgFieldWidth * numFields * inputRows
    double numFields = this.getRowType().getFieldCount();
    long fieldWidth = PrelUtil.getPlannerSettings(planner).getOptions()
      .getOption(ExecConstants.AVERAGE_FIELD_WIDTH_KEY).num_val;

    double memCost = fieldWidth * numFields * inputRows;

    DrillCostFactory costFactory = (DrillCostFactory) planner.getCostFactory();
    return costFactory.makeCost(inputRows, cpuCost, diskIOCost, 0, memCost);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    org.apache.drill.exec.physical.config.Sort g = new ExternalSort(childPOP, PrelUtil.getOrdering(this.collation, getInput().getRowType()), false);
    return creator.addMetadata(this, g);
  }

  @Override
  public SortPrel copy(
      RelTraitSet traitSet,
      RelNode newInput,
      RelCollation newCollation,
      RexNode offset,
      RexNode fetch) {
    return new SortPrel(getCluster(), traitSet, newInput, newCollation);
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getInput());
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitPrel(this, value);
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.NONE_AND_TWO;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.FOUR_BYTE;
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return true;
  }

  @Override
  public Prel prepareForLateralUnnestPipeline(List<RelNode> children) {
    List<RelFieldCollation> relFieldCollations = Lists.newArrayList();
    relFieldCollations.add(new RelFieldCollation(0,
                            RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST));
    for (RelFieldCollation fieldCollation : this.collation.getFieldCollations()) {
      relFieldCollations.add(new RelFieldCollation(fieldCollation.getFieldIndex() + 1,
              fieldCollation.direction, fieldCollation.nullDirection));
    }

    @SuppressWarnings("deprecation")
    RelCollation collationTrait = RelCollationImpl.of(relFieldCollations);
    RelTraitSet traits = RelTraitSet.createEmpty()
                                    .replace(this.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE))
                                    .replace(collationTrait)
                                    .replace(DRILL_PHYSICAL);

    return this.copy(traits, children.get(0), collationTrait, this.offset, this.fetch);
  }

  @Override
  public boolean canBeDropped() {
    return isRemovable;
  }
}
