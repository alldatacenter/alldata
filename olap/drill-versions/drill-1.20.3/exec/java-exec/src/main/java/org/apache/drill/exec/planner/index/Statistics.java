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
package org.apache.drill.exec.planner.index;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.common.DrillScanRelBase;

public interface Statistics {

  double ROWCOUNT_UNKNOWN = -1;
  //HUGE is same as DrillCostBase.HUGE
  double ROWCOUNT_HUGE = Double.MAX_VALUE;
  double AVG_ROWSIZE_UNKNOWN = -1;
  long AVG_COLUMN_SIZE = 10;

  /** Returns whether statistics are available. Should be called prior to using the statistics
   */
  boolean isStatsAvailable();

  /** Returns a unique index identifier
   *  @param idx - Index specified as a {@link IndexDescriptor}
   *  @return The unique index identifier
   */
  String buildUniqueIndexIdentifier(IndexDescriptor idx);

  /** Returns the rowcount for the specified filter condition
   *  @param condition - Filter specified as a {@link RexNode}
   *  @param tabIdxName - The index name generated using {@code buildUniqueIndexIdentifier}
   *  @param scanRel - The current scan rel
   *  @return the rowcount for the filter
   */
  double getRowCount(RexNode condition, String tabIdxName, RelNode scanRel);

  /** Returns the leading rowcount for the specified filter condition
   *  Leading rowcount means rowcount for filter condition only on leading index columns.
   *  @param condition - Filter specified as a {@link RexNode}
   *  @param tabIdxName - The index name generated using {@code buildUniqueIndexIdentifier}
   *  @param scanRel - The current scan rel
   *  @return the leading rowcount
   */
  public double getLeadingRowCount(RexNode condition, String tabIdxName, DrillScanRelBase scanRel);
  /** Returns the average row size for the specified filter condition
   * @param tabIdxName - The index name generated using {@code buildUniqueIndexIdentifier}
   * @param isIndexScan - Whether the current rel is an index scan (false for primary table)
   */
  double getAvgRowSize(String tabIdxName, boolean isIndexScan);

  boolean initialize(RexNode condition, DrillScanRelBase scanRel, IndexCallContext context);
}
