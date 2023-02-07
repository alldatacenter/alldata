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
package org.apache.drill.exec.planner.common;

import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Correlate;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.physical.PrelUtil;

import java.util.ArrayList;
import java.util.List;


public abstract class DrillLateralJoinRelBase extends Correlate implements DrillRelNode {

  final public static String IMPLICIT_COLUMN = DrillRelOptUtil.IMPLICIT_COLUMN;

  final private static double CORRELATE_MEM_COPY_COST = DrillCostBase.MEMORY_TO_CPU_RATIO * DrillCostBase.BASE_CPU_COST;
  final public boolean excludeCorrelateColumn;
  public DrillLateralJoinRelBase(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, boolean excludeCorrelateCol,
                               CorrelationId correlationId, ImmutableBitSet requiredColumns, JoinRelType semiJoinType) {
    super(cluster, traits, left, right, correlationId, requiredColumns, semiJoinType);
    this.excludeCorrelateColumn = excludeCorrelateCol;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    DrillCostBase.DrillCostFactory costFactory = (DrillCostBase.DrillCostFactory) planner.getCostFactory();

    double rowCount = estimateRowCount(mq);
    long fieldWidth = PrelUtil.getPlannerSettings(planner).getOptions()
        .getLong(ExecConstants.AVERAGE_FIELD_WIDTH_KEY);

    double rowSize = left.getRowType().getFieldList().size() * fieldWidth;

    double cpuCost = rowCount * rowSize * DrillCostBase.BASE_CPU_COST;
    double memCost = !excludeCorrelateColumn ? CORRELATE_MEM_COPY_COST : 0.0;
    return costFactory.makeCost(rowCount, cpuCost, 0, 0, memCost);
  }

  @Override
  protected RelDataType deriveRowType() {
    switch (joinType) {
      case LEFT:
      case INNER:
        return constructRowType(SqlValidatorUtil.deriveJoinRowType(left.getRowType(),
          removeImplicitField(right.getRowType()), joinType,
          getCluster().getTypeFactory(), null,
          ImmutableList.of()));
      case ANTI:
      case SEMI:
        return constructRowType(left.getRowType());
      default:
        throw new IllegalStateException("Unknown join type " + joinType);
    }
  }

  /**
   * Returns number of fields in {@link RelDataType} for
   * input rel node with specified ordinal considering value of
   * {@code excludeCorrelateColumn}.
   *
   * @param ordinal ordinal of input rel node
   * @return number of fields in input's {@link RelDataType}
   */
  public int getInputSize(int ordinal) {
    if (this.excludeCorrelateColumn && ordinal == 0) {
      return getInput(ordinal).getRowType().getFieldList().size() - 1;
    }
    return getInput(ordinal).getRowType().getFieldList().size();
  }

  public RelDataType constructRowType(RelDataType inputRowType) {
    Preconditions.checkArgument(this.requiredColumns.cardinality() == 1);

    List<RelDataType> fields = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();
    if (excludeCorrelateColumn) {
      int corrVariable = this.requiredColumns.nextSetBit(0);

      for (RelDataTypeField field : inputRowType.getFieldList()) {
        if (field.getIndex() == corrVariable) {
          continue;
        }
        fieldNames.add(field.getName());
        fields.add(field.getType());
      }

      return getCluster().getTypeFactory().createStructType(fields, fieldNames);
    }
    return inputRowType;
  }

  public RelDataType removeImplicitField(RelDataType inputRowType) {
    List<RelDataType> fields = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();

    for (RelDataTypeField field : inputRowType.getFieldList()) {
      if (field.getName().equals(IMPLICIT_COLUMN)) {
        continue;
      }
      fieldNames.add(field.getName());
      fields.add(field.getType());
    }

    return getCluster().getTypeFactory().createStructType(fields, fieldNames);
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return mq.getRowCount(left);
  }
}
