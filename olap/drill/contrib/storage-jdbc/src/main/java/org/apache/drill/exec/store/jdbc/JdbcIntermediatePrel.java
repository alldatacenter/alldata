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
package org.apache.drill.exec.store.jdbc;

import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.physical.PhysicalPlanCreator;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.SinglePrel;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.planner.sql.handlers.PrelFinalizable;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

/**
 * Prel used to represent a JDBC Conversion within an expression tree. This Prel will replaced with a full JdbcPrel
 * before execution can happen.
 */
public class JdbcIntermediatePrel extends SinglePrel implements PrelFinalizable {

  public JdbcIntermediatePrel(RelOptCluster cluster, RelTraitSet traits, RelNode child) {
    super(cluster, traits, child);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new JdbcIntermediatePrel(getCluster(), traitSet, getInput());
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return copy(getTraitSet(), getInputs());
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public Prel finalizeRel() {
    return new JdbcPrel(getCluster(), getTraitSet(), this);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) {
    throw new UnsupportedOperationException("This needs to be finalized before using a PrelVisitor.");
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return false;
  }
}
