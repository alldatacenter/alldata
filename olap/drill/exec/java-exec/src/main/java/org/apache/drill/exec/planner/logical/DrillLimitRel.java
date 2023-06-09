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

import java.math.BigDecimal;
import java.util.List;

import org.apache.drill.common.logical.data.Limit;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.planner.torel.ConversionContext;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;

public class DrillLimitRel extends DrillLimitRelBase implements DrillRel {

  public DrillLimitRel(RelOptCluster cluster, RelTraitSet traitSet, RelNode child, RexNode offset, RexNode fetch) {
    super(cluster, traitSet, child, offset, fetch);
  }

  public DrillLimitRel(RelOptCluster cluster, RelTraitSet traitSet, RelNode child, RexNode offset, RexNode fetch, boolean pushDown) {
    super(cluster, traitSet, child, offset, fetch, pushDown);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new DrillLimitRel(getCluster(), traitSet, sole(inputs), offset, fetch, isPushDown());
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs, boolean pushDown) {
    return new DrillLimitRel(getCluster(), traitSet, sole(inputs), offset, fetch, pushDown);
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    LogicalOperator inputOp = implementor.visitChild(this, 0, getInput());

    // First offset to include into results (inclusive). Null implies it is starting from offset 0
    int first = offset != null ? Math.max(0, RexLiteral.intValue(offset)) : 0;

    // Last offset to stop including into results (exclusive), translating fetch row counts into an offset.
    // Null value implies including entire remaining result set from first offset
    Integer last = fetch != null ? Math.max(0, RexLiteral.intValue(fetch)) + first : null;
    Limit limit = new Limit(first, last);
    limit.setInput(inputOp);
    return limit;
  }

  public static DrillLimitRel convert(Limit limit, ConversionContext context) throws InvalidRelException{
    RelNode input = context.toRel(limit.getInput());
    RexNode first = context.getRexBuilder().makeExactLiteral(BigDecimal.valueOf(limit.getFirst()), context.getTypeFactory().createSqlType(SqlTypeName.INTEGER));
    RexNode last = context.getRexBuilder().makeExactLiteral(BigDecimal.valueOf(limit.getLast()), context.getTypeFactory().createSqlType(SqlTypeName.INTEGER));
    return new DrillLimitRel(context.getCluster(), context.getLogicalTraits(), input, first, last);
  }


}
