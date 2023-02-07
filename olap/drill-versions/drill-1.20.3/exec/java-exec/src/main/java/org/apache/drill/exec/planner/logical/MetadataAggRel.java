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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.SingleRel;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.MetadataAggregate;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.metastore.analyze.MetadataAggregateContext;

import java.util.List;

public class MetadataAggRel extends SingleRel implements DrillRel {

  private final MetadataAggregateContext context;

  public MetadataAggRel(RelOptCluster cluster, RelTraitSet traits, RelNode input,
      MetadataAggregateContext context) {
    super(cluster, traits, input);
    this.context = context;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    double dRows = mq.getRowCount(getInput());
    double dCpu = dRows * DrillCostBase.COMPARE_CPU_COST;
    double dIo = 0;
    return planner.getCostFactory().makeCost(dRows, dCpu, dIo);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new MetadataAggRel(getCluster(), traitSet, sole(inputs), context);
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    LogicalOperator inputOp = implementor.visitChild(this, 0, getInput());
    MetadataAggregate rel = new MetadataAggregate();
    rel.setInput(inputOp);
    return rel;
  }

  public MetadataAggregateContext getContext() {
    return context;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("context: ", context);
  }
}
