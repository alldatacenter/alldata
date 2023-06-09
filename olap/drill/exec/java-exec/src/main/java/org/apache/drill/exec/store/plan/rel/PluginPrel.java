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
package org.apache.drill.exec.store.plan.rel;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.physical.PhysicalPlanCreator;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.store.SubsetRemover;
import org.apache.drill.exec.store.plan.PluginImplementor;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Represents a plugin-specific plan once children nodes have been pushed down into group scan.
 */
public class PluginPrel extends AbstractRelNode implements Prel {
  private final GroupScan groupScan;
  private final RelDataType rowType;

  public PluginPrel(RelOptCluster cluster, PluginIntermediatePrel intermediatePrel) {
    super(cluster, intermediatePrel.getTraitSet());
    this.rowType = intermediatePrel.getRowType();
    PluginRel input = (PluginRel) intermediatePrel.getInput().accept(SubsetRemover.INSTANCE);
    try {
      PluginImplementor implementor = intermediatePrel.getPluginImplementor();
      input.implement(implementor);
      this.groupScan = implementor.getPhysicalOperator();
    } catch (IOException e) {
      throw new DrillRuntimeException(e);
    }
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) {
    return creator.addMetadata(this, groupScan);
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
    return Collections.emptyIterator();
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("groupScan", groupScan);
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return groupScan.getScanStats(mq).getRecordCount();
  }

  @Override
  protected RelDataType deriveRowType() {
    return rowType;
  }
}
