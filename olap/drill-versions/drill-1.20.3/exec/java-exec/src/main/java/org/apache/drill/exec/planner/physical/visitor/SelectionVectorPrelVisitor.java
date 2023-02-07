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

import java.util.List;

import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.SelectionVectorRemoverPrel;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelNode;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;


public class SelectionVectorPrelVisitor extends BasePrelVisitor<Prel, Void, RuntimeException>{

  private static final SelectionVectorPrelVisitor INSTANCE = new SelectionVectorPrelVisitor();

  public static Prel addSelectionRemoversWhereNecessary(Prel prel) {
    return prel.accept(INSTANCE, null);
  }

  @Override
  public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
    SelectionVectorMode[] encodings = prel.getSupportedEncodings();
    List<RelNode> children = Lists.newArrayList();
    for (Prel child : prel) {
      child = child.accept(this, null);
      children.add(convert(encodings, child));
    }

    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }

  private Prel convert(SelectionVectorMode[] encodings, Prel prel) {
    for (SelectionVectorMode m : encodings) {
      if (prel.getEncoding() == m) {
        return prel;
      }
    }
    return new SelectionVectorRemoverPrel(prel);
  }

}
