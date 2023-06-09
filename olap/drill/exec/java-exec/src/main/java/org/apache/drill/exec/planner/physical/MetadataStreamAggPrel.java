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
import org.apache.calcite.rel.SingleRel;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.MetadataStreamAggPOP;
import org.apache.drill.exec.planner.common.DrillRelNode;
import org.apache.drill.exec.planner.physical.AggPrelBase.OperatorPhase;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.metastore.analyze.MetadataAggregateContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MetadataStreamAggPrel extends SingleRel implements DrillRelNode, Prel {
  private final MetadataAggregateContext context;
  private final OperatorPhase phase;

  public MetadataStreamAggPrel(RelOptCluster cluster, RelTraitSet traits, RelNode input,
      MetadataAggregateContext context, OperatorPhase phase) {
    super(cluster, traits, input);
    this.context = context;
    this.phase = phase;
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();
    MetadataStreamAggPOP physicalOperator = new MetadataStreamAggPOP(child.getPhysicalOperator(creator), context, phase);
    return creator.addMetadata(this, physicalOperator);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitPrel(this, value);
  }

  @Override
  public BatchSchema.SelectionVectorMode[] getSupportedEncodings() {
    return BatchSchema.SelectionVectorMode.DEFAULT;
  }

  @Override
  public BatchSchema.SelectionVectorMode getEncoding() {
    return BatchSchema.SelectionVectorMode.NONE;
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return false;
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getInput());
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    Preconditions.checkState(inputs.size() == 1);
    return new MetadataStreamAggPrel(getCluster(), traitSet, inputs.iterator().next(), context, phase);
  }

  public OperatorPhase getPhase() {
    return phase;
  }
}
