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

import java.util.Collections;
import java.util.List;

import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScreenPrel;
import org.apache.drill.exec.planner.physical.UnionPrel;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class FinalColumnReorderer extends BasePrelVisitor<Prel, Void, RuntimeException> {

  private static final FinalColumnReorderer INSTANCE = new FinalColumnReorderer();

  public static Prel addFinalColumnOrdering(Prel prel) {
    return prel.accept(INSTANCE, null);
  }

  @Override
  public Prel visitScreen(ScreenPrel prel, Void value) throws RuntimeException {
    Prel newChild = addTrivialOrderedProjectPrel(((Prel) prel.getInput()).accept(this, value), true);
    if (newChild == prel.getInput()) {
      return prel;
    }
    return prel.copy(prel.getTraitSet(), Collections.singletonList(newChild));
  }

  private Prel addTrivialOrderedProjectPrel(Prel prel) {
    RelDataType t = prel.getRowType();

    RexBuilder b = prel.getCluster().getRexBuilder();
    List<RexNode> projections = Lists.newArrayList();
    int projectCount = t.getFieldList().size();

    // no point in reordering if we only have one column
    if (projectCount < 2) {
      return prel;
    }

    for (int i = 0; i < projectCount; i++) {
      projections.add(b.makeInputRef(prel, i));
    }
    return new ProjectPrel(prel.getCluster(), prel.getTraitSet(), prel, projections, prel.getRowType());
  }

  private Prel addTrivialOrderedProjectPrel(Prel prel, boolean checkNecessity) {
    if(checkNecessity && !prel.needsFinalColumnReordering()) {
      return prel;
    } else {
      return addTrivialOrderedProjectPrel(prel);
    }
  }

  @Override
  public Prel visitWriter(WriterPrel prel, Void value) throws RuntimeException {
    Prel newChild = ((Prel) prel.getInput()).accept(this, null);
    if (newChild == prel.getInput()) {
      return prel;
    }
    return prel.copy(prel.getTraitSet(), Collections.singletonList(addTrivialOrderedProjectPrel(newChild, true)));
  }

  @Override
  public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
    if(prel instanceof UnionPrel) {
      return addColumnOrderingBelowUnion(prel);
    }

    List<RelNode> children = Lists.newArrayList();
    boolean changed = false;
    for (Prel p : prel) {
      Prel newP = p.accept(this, null);
      if (newP != p) {
        changed = true;
      }
      children.add(newP);
    }
    if (changed) {
      return (Prel) prel.copy(prel.getTraitSet(), children);
    } else {
      return prel;
    }
  }

  private Prel addColumnOrderingBelowUnion(Prel prel) {
    List<RelNode> children = Lists.newArrayList();
    for (Prel p : prel) {
      Prel child = p.accept(this, null);

      boolean needProjectBelowUnion = !(p instanceof ProjectPrel);
      if(needProjectBelowUnion) {
        child = addTrivialOrderedProjectPrel(child, false);
      }

      children.add(child);
    }

    return (Prel) prel.copy(prel.getTraitSet(), children);
  }
}
