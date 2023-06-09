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

import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Correlate;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.commons.collections.ListUtils;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.planner.common.DrillLateralJoinRelBase;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LateralJoinPrel extends DrillLateralJoinRelBase implements Prel {


  protected LateralJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, boolean excludeCorrelateCol,
                            CorrelationId correlationId, ImmutableBitSet requiredColumns, JoinRelType semiJoinType) {
    super(cluster, traits, left, right, excludeCorrelateCol, correlationId, requiredColumns, semiJoinType);
  }

  @Override
  public Correlate copy(RelTraitSet traitSet,
                        RelNode left, RelNode right, CorrelationId correlationId,
                        ImmutableBitSet requiredColumns, JoinRelType joinType) {
    return new LateralJoinPrel(this.getCluster(), this.getTraitSet(), left, right, this.excludeCorrelateColumn, correlationId, requiredColumns,
        this.getJoinType());
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {

    PhysicalOperator leftPop = ((Prel)left).getPhysicalOperator(creator);
    PhysicalOperator rightPop = ((Prel)right).getPhysicalOperator(creator);

    JoinRelType jtype = this.getJoinType();
    List<SchemaPath> excludedColumns = new ArrayList<>();
    if (getColumn() != null) {
      excludedColumns.add(getColumn());
    }
    LateralJoinPOP ljoin = new LateralJoinPOP(leftPop, rightPop, jtype, DrillLateralJoinRelBase.IMPLICIT_COLUMN, excludedColumns);
    return creator.addMetadata(this, ljoin);
  }

  private SchemaPath getColumn() {
    if (this.excludeCorrelateColumn) {
      int index = this.getRequiredColumns().asList().get(0);
      return  SchemaPath.getSimplePath(this.getInput(0).getRowType().getFieldNames().get(index));
    }
    return null;
  }

  /**
   * Check to make sure that the fields of the inputs are the same as the output field names.
   * If not, insert a project renaming them.
   */
  public RelNode getLateralInput(int ordinal, RelNode input) {
    int offset = ordinal == 0 ? 0 : getInputSize(0);
    Preconditions.checkArgument(DrillJoinRelBase.uniqueFieldNames(input.getRowType()));
    final List<String> fields = getRowType().getFieldNames();
    final List<String> inputFields = input.getRowType().getFieldNames();
    final List<String> outputFields = fields.subList(offset, offset + getInputSize(ordinal));
    if (ListUtils.subtract(outputFields, inputFields).size() != 0) {
      // Ensure that input field names are the same as output field names.
      // If there are duplicate field names on left and right, fields will get
      // lost.
      // In such case, we need insert a rename Project on top of the input.
      return rename(input, input.getRowType().getFieldList(), outputFields);
    } else {
      return input;
    }
  }

  private RelNode rename(RelNode input, List<RelDataTypeField> inputFields, List<String> outputFieldNames) {
    List<RexNode> exprs = Lists.newArrayList();

    for (RelDataTypeField field : inputFields) {
      RexNode expr = input.getCluster().getRexBuilder().makeInputRef(field.getType(), field.getIndex());
      exprs.add(expr);
    }

    RelDataType rowType = RexUtil.createStructType(input.getCluster().getTypeFactory(),
        exprs, outputFieldNames, null);

    ProjectPrel proj = new ProjectPrel(input.getCluster(), input.getTraitSet(), input, exprs, rowType);

    return proj;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    if (this.excludeCorrelateColumn) {
      return super.explainTerms(pw).item("column excluded from output: ", this.getColumn());
    } else {
      return super.explainTerms(pw);
    }
  }


  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> visitor, X value) throws E {
    return visitor.visitLateral(this, value);
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getLeft(), getRight());
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return true;
  }

  @Override
  public BatchSchema.SelectionVectorMode[] getSupportedEncodings() {
    return BatchSchema.SelectionVectorMode.DEFAULT;
  }

  @Override
  public BatchSchema.SelectionVectorMode getEncoding() {
    return BatchSchema.SelectionVectorMode.NONE;
  }

}
