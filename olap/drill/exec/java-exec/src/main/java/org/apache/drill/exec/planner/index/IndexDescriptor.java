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

import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.cost.PluginCost;


/**
 * IndexDefinition + functions to access materialized index(index table/scan, etc)
 */

public interface IndexDescriptor extends IndexDefinition {

  /**
   * Get the estimated row count for a single index condition
   * @param input The rel node corresponding to the primary table
   * @param indexCondition The index condition (e.g index_col1 < 10 AND index_col2 = 'abc')
   * @return The estimated row count
   */
  double getRows(RelNode input, RexNode indexCondition);

  /**
   * Whether or not the index supports getting row count statistics
   * @return True if index supports getting row count, False otherwise
   */
  boolean supportsRowCountStats();

  /**
   * Get an instance of the group scan associated with this index descriptor
   * @return An instance of group scan for this index
   */
  IndexGroupScan getIndexGroupScan();

  /**
   * Whether or not the index supports full-text search (to allow pushing down such filters)
   * @return True if index supports full-text search, False otherwise
   */
  boolean supportsFullTextSearch();

  /**
   * Get the functional index information associated with this index (Functional indexes are
   * indexes involving expressions e.g CAST(a as INT).
   */
  FunctionalIndexInfo getFunctionalInfo();

  /**
   * Get the total cost of index access (I/O, CPU) in the context of the current query
   * @param indexProps properties (metrics) of a single index in the context of current query
   * @param planner Planner instance
   * @param numProjectedFields Number of projected fields
   * @param primaryGroupScan Primary table's GroupScan instance
   * @return a RelOptCost instance representing the total cost
   */
  RelOptCost getCost(IndexProperties indexProps, RelOptPlanner planner,
      int numProjectedFields, GroupScan primaryGroupScan);

  /**
   * Get the costing factors associated with the storage/format plugin
   */
  PluginCost getPluginCostModel();

  /**
   * Whether this index is maintained synchronously (i.e primary table updates are propagated to the index
   * synchronously) or asynchronously with some delay.  The latter is more common for distributed NoSQL databases.
   * @return True if the index is maintained asynchronously, False otherwise
   */
  boolean isAsyncIndex();

}
