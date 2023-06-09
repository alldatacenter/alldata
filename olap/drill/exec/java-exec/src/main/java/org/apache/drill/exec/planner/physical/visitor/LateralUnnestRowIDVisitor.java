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
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.rules.ProjectCorrelateTransposeRule;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.planner.physical.LateralJoinPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.UnnestPrel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LateralUnnestRowIDVisitor traverses the physical plan and modifies all the operators in the
 * pipeline of Lateral and Unnest operators to accommodate IMPLICIT_COLUMN. The source for the
 * IMPLICIT_COLUMN is unnest operator and the sink for the column is the corresponding Lateral
 * join operator.
 */
public class LateralUnnestRowIDVisitor extends BasePrelVisitor<Prel, Boolean, RuntimeException> {

  private static final LateralUnnestRowIDVisitor INSTANCE = new LateralUnnestRowIDVisitor();

  public static Prel insertRowID(Prel prel){
    return prel.accept(INSTANCE, false);
  }

  @Override
  public Prel visitPrel(Prel prel, Boolean isRightOfLateral) throws RuntimeException {
    List<RelNode> children = getChildren(prel, isRightOfLateral);
    if (isRightOfLateral) {
      return prel.prepareForLateralUnnestPipeline(children);
    } else if (children.equals(prel.getInputs())) {
      return prel;
    } else {
      return (Prel) prel.copy(prel.getTraitSet(), children);
    }
  }

  private List<RelNode> getChildren(Prel prel, Boolean isRightOfLateral) {
    List<RelNode> children = Lists.newArrayList();
    for(Prel child : prel){
      child = child.accept(this, isRightOfLateral);
      children.add(child);
    }
    return children;
  }

  @Override
  public Prel visitLateral(LateralJoinPrel prel, Boolean isRightOfLateral) throws RuntimeException {
    List<RelNode> children = Lists.newArrayList();
    children.add(((Prel) prel.getInput(0)).accept(this, isRightOfLateral));
    children.add(((Prel) prel.getInput(1)).accept(this, true));

    if (!isRightOfLateral) {
      return (Prel) prel.copy(prel.getTraitSet(), children);
    } else {
      //Adjust the column numbering due to an additional column "$drill_implicit_field$" is added to the inputs.
      Map<Integer, Integer> requiredColsMap = new HashMap<>();
      for (Integer corrColIndex : prel.getRequiredColumns()) {
        requiredColsMap.put(corrColIndex, corrColIndex + 1);
      }
      ImmutableBitSet requiredColumns = prel.getRequiredColumns().shift(1);

      CorrelationId corrId = prel.getCluster().createCorrel();
      RexCorrelVariable updatedCorrel =
              (RexCorrelVariable) prel.getCluster().getRexBuilder().makeCorrel(
                      children.get(0).getRowType(),
                      corrId);
      RelNode rightChild = children.get(1).accept(
              new CorrelateVarReplacer(
                      new ProjectCorrelateTransposeRule.RexFieldAccessReplacer(prel.getCorrelationId(),
                              updatedCorrel, prel.getCluster().getRexBuilder(), requiredColsMap)));
      return (Prel) prel.copy(prel.getTraitSet(), children.get(0), rightChild,
              corrId, requiredColumns, prel.getJoinType());
    }
  }

  @Override
  public Prel visitUnnest(UnnestPrel prel, Boolean isRightOfLateral) throws RuntimeException {
    return prel.prepareForLateralUnnestPipeline(null);
  }

  /**
   * Visitor for RelNodes which applies specified {@link RexShuttle} visitor
   * for every node in the tree.
   */
  public static class CorrelateVarReplacer extends ProjectCorrelateTransposeRule.RelNodesExprsHandler {
    protected final RexShuttle rexVisitor;

    public CorrelateVarReplacer(RexShuttle rexVisitor) {
      super(rexVisitor);
      this.rexVisitor = rexVisitor;
    }

    @Override
    public RelNode visit(RelNode other) {
      return super.visit(other.accept(rexVisitor));
    }
  }
}
