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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.planner.common.DrillUnnestRelBase;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UnnestPrel extends DrillUnnestRelBase implements LeafPrel {

  protected final UnnestPOP unnestPOP;

  public UnnestPrel(RelOptCluster cluster, RelTraitSet traits,
                    RelDataType rowType, RexNode ref) {
    super(cluster, traits, ref);
    this.unnestPOP = new UnnestPOP(null, SchemaPath.getSimplePath(((RexFieldAccess)ref).getField().getName()), DrillUnnestRelBase.IMPLICIT_COLUMN);
    this.rowType = rowType;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> visitor, X value) throws E {
    return visitor.visitUnnest(this, value);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator)
      throws IOException {
    return creator.addMetadata(this, unnestPOP);
  }

  @Override
  public BatchSchema.SelectionVectorMode[] getSupportedEncodings() {
    return BatchSchema.SelectionVectorMode.DEFAULT;
  }

  @Override
  public BatchSchema.SelectionVectorMode getEncoding() {
    return BatchSchema.SelectionVectorMode.NONE;
  }

  public Class<?> getParentClass() {
    return LateralJoinPrel.class;
  }

  @Override
  public RelNode accept(RexShuttle shuttle) {
    RexNode ref = shuttle.apply(this.ref);
    if (this.ref == ref) {
      return this;
    }
    return new UnnestPrel(getCluster(), traitSet, rowType, ref);
  }

  @Override
  public Prel prepareForLateralUnnestPipeline(List<RelNode> children) {
    RelDataTypeFactory typeFactory = this.getCluster().getTypeFactory();
    List<String> fieldNames = new ArrayList<>();
    List<RelDataType> fieldTypes = new ArrayList<>();

    fieldNames.add(IMPLICIT_COLUMN);
    fieldTypes.add(typeFactory.createSqlType(SqlTypeName.INTEGER));

    for (RelDataTypeField field : this.rowType.getFieldList()) {
      fieldNames.add(field.getName());
      fieldTypes.add(field.getType());
    }

    RelDataType newRowType = typeFactory.createStructType(fieldTypes, fieldNames);
    return new UnnestPrel(this.getCluster(), this.getTraitSet(), newRowType, ref);
  }
}
