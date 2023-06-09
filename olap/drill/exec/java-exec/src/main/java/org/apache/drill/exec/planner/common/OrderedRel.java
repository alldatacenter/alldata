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
package org.apache.drill.exec.planner.common;

import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rex.RexNode;

/**
 * Class implementing OrderedPrel interface guarantees to provide ordered
 * output on certain columns. TopNPrel and SortPrel base classes which implement
 * this interface.
 */
public interface OrderedRel extends DrillRelNode {

  /**
   * A method to return ordering columns of the result.
   * @return Collation order of the output.
   */
  RelCollation getCollation();

  /**
   * Offset value represented in RexNode.
   * @return offset.
   */
  RexNode getOffset();

  /**
   * Fetch value represented in RexNode.
   * @return fetch
   */
  RexNode getFetch();

  /**
   * A method to return if this relational node can be dropped during optimization process.
   * @return true if this node can be dropped, false otherwise.
   */
  boolean canBeDropped();
}
