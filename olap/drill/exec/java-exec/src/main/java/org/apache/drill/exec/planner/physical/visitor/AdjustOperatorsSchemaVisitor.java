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

import java.util.ArrayList;
import java.util.List;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.physical.JoinPrel;
import org.apache.drill.exec.planner.physical.LateralJoinPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.calcite.rel.RelNode;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.planner.physical.UnnestPrel;

/**
 * AdjustOperatorsSchemaVisitor visits corresponding operators' which depending upon their functionality
 * adjusts their output row types. The adjusting mechanism is unique to each operator. In case of joins this visitor
 * adjusts the field names to make sure that upstream operator only sees that there are unique field names even though
 * the children of the join has same field names. Whereas in case of lateral/unnest operators it changes the correlated
 * field and also the unnest operator's output row type.
 */
public class AdjustOperatorsSchemaVisitor extends BasePrelVisitor<Prel, Void, RuntimeException>{

  private Prel registeredPrel = null;

  private static AdjustOperatorsSchemaVisitor INSTANCE = new AdjustOperatorsSchemaVisitor();

  public static Prel adjustSchema(Prel prel){
    return prel.accept(INSTANCE, null);
  }

  private void register(Prel prel) {
    this.registeredPrel = prel;
  }

  private Prel getRegisteredPrel() {
    return this.registeredPrel;
  }

  @Override
  public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
    return preparePrel(prel, getChildren(prel));
  }

  public void unRegister() {
    this.registeredPrel = null;
  }

  private List<RelNode> getChildren(Prel prel, int registerForChild) {
    int ch = 0;
    List<RelNode> children = Lists.newArrayList();
    for(Prel child : prel){
      if (ch == registerForChild) {
        register(prel);
      }
      child = child.accept(this, null);
      if (ch == registerForChild) {
        unRegister();
      }
      children.add(child);
      ch++;
    }
    return children;
  }

  private List<RelNode> getChildren(Prel prel) {
    return getChildren(prel, -1);
  }

  private Prel preparePrel(Prel prel, List<RelNode> renamedNodes) {
    return (Prel) prel.copy(prel.getTraitSet(), renamedNodes);
  }

  @Override
  public Prel visitJoin(JoinPrel prel, Void value) throws RuntimeException {

    List<RelNode> children = getChildren(prel);

    final int leftCount = children.get(0).getRowType().getFieldCount();

    List<RelNode> reNamedChildren = Lists.newArrayList();

    RelNode left = prel.getJoinInput(0, children.get(0));
    RelNode right = prel.getJoinInput(leftCount, children.get(1));

    reNamedChildren.add(left);
    reNamedChildren.add(right);

    return preparePrel(prel, reNamedChildren);
  }

  @Override
  public Prel visitLateral(LateralJoinPrel prel, Void value) throws RuntimeException {

    List<RelNode> children = getChildren(prel, 1);
    List<RelNode> reNamedChildren = new ArrayList<>();

    for (int i = 0; i < children.size(); i++) {
      reNamedChildren.add(prel.getLateralInput(i, children.get(i)));
    }

    return preparePrel(prel, reNamedChildren);
  }

  @Override
  public Prel visitUnnest(UnnestPrel prel, Void value) throws RuntimeException {
    Preconditions.checkArgument(registeredPrel != null && registeredPrel instanceof LateralJoinPrel);
    Preconditions.checkArgument(prel.getRowType().getFieldCount() == 1);
    RexBuilder builder = prel.getCluster().getRexBuilder();

    LateralJoinPrel lateralJoinPrel = (LateralJoinPrel) getRegisteredPrel();
    int correlationIndex = lateralJoinPrel.getRequiredColumns().nextSetBit(0);
    String correlationColumnName = lateralJoinPrel.getLeft().getRowType().getFieldNames().get(correlationIndex);
    RexNode corrRef = builder.makeCorrel(lateralJoinPrel.getLeft().getRowType(), lateralJoinPrel.getCorrelationId());
    RexNode fieldAccess = builder.makeFieldAccess(corrRef, correlationColumnName, false);

    List<String> fieldNames = new ArrayList<>();
    List<RelDataType> fieldTypes = new ArrayList<>();
    for (RelDataTypeField field : prel.getRowType().getFieldList()) {
      fieldNames.add(correlationColumnName);
      fieldTypes.add(field.getType());
    }

    UnnestPrel unnestPrel = new UnnestPrel(prel.getCluster(), prel.getTraitSet(),
            prel.getCluster().getTypeFactory().createStructType(fieldTypes, fieldNames), fieldAccess);
    return unnestPrel;
  }
}
