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
import org.apache.calcite.rel.BiRel;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.MetadataController;
import org.apache.drill.exec.metastore.analyze.MetastoreAnalyzeConstants;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.metastore.analyze.MetadataControllerContext;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.List;

public class MetadataControllerRel extends BiRel implements DrillRel {
  private final MetadataControllerContext context;

  public MetadataControllerRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right,
      MetadataControllerContext context) {
    super(cluster, traits, left, right);
    this.context = context;
  }

  public MetadataControllerContext getContext() {
    return context;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    double dRows = Math.max(mq.getRowCount(getLeft()), mq.getRowCount(getRight()));
    double dCpu = dRows * DrillCostBase.COMPARE_CPU_COST;
    double dIo = 0;
    return planner.getCostFactory().makeCost(dRows, dCpu, dIo);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    Preconditions.checkArgument(inputs.size() == 2);
    return new MetadataControllerRel(getCluster(), traitSet, inputs.get(0), inputs.get(1), context);
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    LogicalOperator left = implementor.visitChild(this, 0, getLeft());
    LogicalOperator right = implementor.visitChild(this, 1, getRight());
    return new MetadataController(left, right);
  }

  @Override
  protected RelDataType deriveRowType() {
    return getCluster().getTypeFactory().builder()
        .add(MetastoreAnalyzeConstants.OK_FIELD_NAME, SqlTypeName.BOOLEAN)
        .add(MetastoreAnalyzeConstants.SUMMARY_FIELD_NAME, SqlTypeName.VARCHAR)
        .build();
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("context: ", context);
  }
}
