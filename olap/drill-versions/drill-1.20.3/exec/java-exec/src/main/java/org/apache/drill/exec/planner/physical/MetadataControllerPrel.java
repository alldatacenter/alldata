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
import org.apache.calcite.rel.BiRel;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.metastore.analyze.MetastoreAnalyzeConstants;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.MetadataControllerPOP;
import org.apache.drill.exec.planner.common.DrillRelNode;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.metastore.analyze.MetadataControllerContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MetadataControllerPrel extends BiRel implements DrillRelNode, Prel {
  private final MetadataControllerContext context;

  protected MetadataControllerPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left,
      RelNode right, MetadataControllerContext context) {
    super(cluster, traits, left, right);
    this.context = context;
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel left = (Prel) this.getLeft();
    Prel right = (Prel) this.getRight();
    MetadataControllerPOP physicalOperator =
        new MetadataControllerPOP(left.getPhysicalOperator(creator), right.getPhysicalOperator(creator), context);
    return creator.addMetadata(this, physicalOperator);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitPrel(this, value);
  }

  @Override
  public BatchSchema.SelectionVectorMode[] getSupportedEncodings() {
    return BatchSchema.SelectionVectorMode.ALL;
  }

  @Override
  public BatchSchema.SelectionVectorMode getEncoding() {
    return BatchSchema.SelectionVectorMode.NONE;
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    Preconditions.checkArgument(inputs.size() == 2);
    return new MetadataControllerPrel(getCluster(), traitSet, inputs.get(0), inputs.get(1), context);
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return true;
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getLeft(), getRight());
  }

  @Override
  protected RelDataType deriveRowType() {
    return new RelDataTypeFactory.Builder(getCluster().getTypeFactory())
        .add(MetastoreAnalyzeConstants.OK_FIELD_NAME, SqlTypeName.BOOLEAN)
        .add(MetastoreAnalyzeConstants.SUMMARY_FIELD_NAME, SqlTypeName.VARCHAR)
        .build();
  }
}
