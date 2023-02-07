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
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.exec.planner.physical.HashJoinPrel;
import org.apache.drill.exec.planner.physical.JoinPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.RelNode;

import java.util.List;

/**
 * Visit Prel tree. Find all the HashJoinPrel nodes and set the flag to swap the Left/Right for HashJoinPrel
 * when 1) It's inner join, 2) left rowcount is < (1 + percentage) * right_row_count.
 * The purpose of this visitor is to prevent planner from putting bigger dataset in the RIGHT side,
 * which is not good performance-wise.
 *
 * @see org.apache.drill.exec.planner.physical.HashJoinPrel
 */

public class SwapHashJoinVisitor extends BasePrelVisitor<Prel, Double, RuntimeException> {

  private static SwapHashJoinVisitor INSTANCE = new SwapHashJoinVisitor();

  public static Prel swapHashJoin(Prel prel, Double marginFactor){
    return prel.accept(INSTANCE, marginFactor);
  }

  private SwapHashJoinVisitor() {

  }

  @Override
  public Prel visitPrel(Prel prel, Double value) throws RuntimeException {
    List<RelNode> children = Lists.newArrayList();
    for(Prel child : prel){
      child = child.accept(this, value);
      children.add(child);
    }

    return (Prel) prel.copy(prel.getTraitSet(), children);
  }

  @Override
  public Prel visitJoin(JoinPrel prel, Double value) throws RuntimeException {
    JoinPrel newJoin = (JoinPrel) visitPrel(prel, value);

    if (prel instanceof HashJoinPrel &&
        !((HashJoinPrel) prel).isRowKeyJoin() /* don't swap for rowkey joins */) {
      // Mark left/right is swapped, when INNER hash join's left row count < ( 1+ margin factor) right row count.
      RelMetadataQuery mq = newJoin.getCluster().getMetadataQuery();
      if (newJoin.getLeft().estimateRowCount(mq) < (1 + value) * newJoin.getRight().estimateRowCount(mq) &&
          newJoin.getJoinType() == JoinRelType.INNER && !newJoin.isSemiJoin()) {
        ((HashJoinPrel) newJoin).setSwapped(true);
      }
    }

    return newJoin;
  }

}
