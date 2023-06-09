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
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Correlate;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.common.logical.data.LateralJoin;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.exec.planner.common.DrillLateralJoinRelBase;

import java.util.ArrayList;
import java.util.List;

public class DrillLateralJoinRel extends DrillLateralJoinRelBase implements DrillRel {

  protected DrillLateralJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, boolean excludeCorrelateCol,
                                CorrelationId correlationId, ImmutableBitSet requiredColumns, JoinRelType semiJoinType) {
    super(cluster, traits, left, right, excludeCorrelateCol, correlationId, requiredColumns, semiJoinType);
  }

  @Override
  public Correlate copy(RelTraitSet traitSet,
        RelNode left, RelNode right, CorrelationId correlationId,
        ImmutableBitSet requiredColumns, JoinRelType joinType) {
    return new DrillLateralJoinRel(this.getCluster(), this.getTraitSet(), left, right, this.excludeCorrelateColumn, correlationId, requiredColumns,
        this.getJoinType());
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("exclude correlate column: ", excludeCorrelateColumn);
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    List<String> fields = new ArrayList<>();
    fields.addAll(getInput(0).getRowType().getFieldNames());
    fields.addAll(getInput(1).getRowType().getFieldNames());
    assert DrillJoinRel.isUnique(fields);
    final int leftCount = getInputSize(0);

    final LogicalOperator leftOp = DrillJoinRel.implementInput(implementor, 0, 0, left, this, fields);
    final LogicalOperator rightOp = DrillJoinRel.implementInput(implementor, 1, leftCount, right, this, fields);

    return new LateralJoin(leftOp, rightOp);
  }
}
