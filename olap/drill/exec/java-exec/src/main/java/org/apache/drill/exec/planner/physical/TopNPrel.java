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
import java.math.BigDecimal;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.TopN;
import org.apache.drill.exec.planner.common.OrderedRel;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;

public class TopNPrel extends SinglePrel implements OrderedRel,Prel {

  protected int limit;
  protected final RelCollation collation;

  public TopNPrel(RelOptCluster cluster, RelTraitSet traitSet, RelNode child, int limit, RelCollation collation) {
    super(cluster, traitSet, child);
    this.limit = limit;
    this.collation = collation;
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new TopNPrel(getCluster(), traitSet, sole(inputs), this.limit, this.collation);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    TopN topN = new TopN(childPOP, PrelUtil.getOrdering(this.collation, getInput().getRowType()), false, this.limit);
    return creator.addMetadata(this, topN);
  }

  @Override
  public RelCollation getCollation() {
    return collation;
  }

  @Override
  public RexNode getOffset() {
    return getCluster().getRexBuilder().makeExactLiteral(BigDecimal.ZERO,
                  getCluster().getTypeFactory().createSqlType(SqlTypeName.INTEGER));
  }

  @Override
  public RexNode getFetch() {
    return getCluster().getRexBuilder().makeExactLiteral(BigDecimal.valueOf(limit),
                 getCluster().getTypeFactory().createSqlType(SqlTypeName.INTEGER));
  }

  @Override
  public boolean canBeDropped() {
    return true;
  }

  /**
   * Cost of doing Top-N is proportional to M log N where M is the total number of
   * input rows and N is the limit for Top-N.  This makes Top-N preferable to Sort
   * since cost of full Sort is proportional to M log M .
   */
  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if(PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      //We use multiplier 0.05 for TopN operator, and 0.1 for Sort, to make TopN a preferred choice.
      return super.computeSelfCost(planner, mq).multiplyBy(0.05);
    }
    RelNode child = this.getInput();
    double inputRows = mq.getRowCount(child);
    int numSortFields = this.collation.getFieldCollations().size();
    double cpuCost = DrillCostBase.COMPARE_CPU_COST * numSortFields * inputRows * (Math.log(limit)/Math.log(2));
    double diskIOCost = 0; // assume in-memory for now until we enforce operator-level memory constraints
    DrillCostFactory costFactory = (DrillCostFactory)planner.getCostFactory();
    return costFactory.makeCost(inputRows, cpuCost, diskIOCost, 0);
  }


  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw)
        .item("limit", limit);
  }

  public int getLimit() {
    return limit;
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
  public Prel prepareForLateralUnnestPipeline(List<RelNode> children) {
    List<RelFieldCollation> relFieldCollations = Lists.newArrayList();
    relFieldCollations.add(new RelFieldCollation(0,
                          RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST));
    for (RelFieldCollation fieldCollation : this.collation.getFieldCollations()) {
      relFieldCollations.add(new RelFieldCollation(fieldCollation.getFieldIndex() + 1,
              fieldCollation.direction, fieldCollation.nullDirection));
    }

    RelCollation collationTrait = RelCollations.of(relFieldCollations);
    RelTraitSet traits = RelTraitSet.createEmpty()
                                    .replace(this.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE))
                                    .replace(collationTrait)
                                    .replace(DRILL_PHYSICAL);
    return transformTopNToSortAndLimit(children, traits, collationTrait);
  }

  private Prel transformTopNToSortAndLimit(List<RelNode> children, RelTraitSet traits, RelCollation collationTrait) {
    SortPrel sortprel = new SortPrel(this.getCluster(), traits, children.get(0), collationTrait);
    RexNode offset = this.getCluster().getRexBuilder().makeExactLiteral(BigDecimal.valueOf(0),
            this.getCluster().getTypeFactory().createSqlType(SqlTypeName.INTEGER));
    RexNode limit = this.getCluster().getRexBuilder().makeExactLiteral(BigDecimal.valueOf(this.limit),
            this.getCluster().getTypeFactory().createSqlType(SqlTypeName.INTEGER));
    //SMEX is not needed here because Lateral/Unnest pipeline doesn't support exchanges.
    LimitPrel limitPrel = new LimitPrel(this.getCluster(), traits, sortprel, offset, limit, false, true);
    return limitPrel;
  }
}
