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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.common.DrillRelNode;
import java.util.List;

/**
 * Interface which needs to be implemented by all the join relation expressions.
 */
public interface DrillJoin extends DrillRelNode {

  /* Columns of left table that are part of join condition */
  List<Integer> getLeftKeys();

  /* Columns of right table that are part of join condition */
  List<Integer> getRightKeys();

  /* JoinType of the join operation*/
  JoinRelType getJoinType();

  /* Join condition of the join relation */
  RexNode getCondition();

  /* Left RelNode of the Join Relation */
  RelNode getLeft();

  /* Right RelNode of the Join Relation */
  RelNode getRight();

  /* Does semi-join? */
  boolean isSemiJoin();
}
