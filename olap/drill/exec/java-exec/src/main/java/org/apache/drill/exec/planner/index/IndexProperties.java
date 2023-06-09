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
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.planner.common.DrillScanRelBase;

import java.util.Map;

/**
 * IndexProperties encapsulates the various metrics of a single index that are related to
 * the current query. These metrics are subsequently used to rank the index in comparison
 * with other indexes.
 */
public interface IndexProperties  {

  void setProperties(Map<LogicalExpression, RexNode> prefixMap,
                            boolean satisfiesCollation,
                            RexNode indexColumnsRemainderFilter,
                            Statistics stats);

  RexNode getLeadingColumnsFilter();

  RexNode getTotalRemainderFilter();

  double getLeadingSelectivity();

  double getRemainderSelectivity();

  boolean isCovering();

  double getTotalRows();

  IndexDescriptor getIndexDesc();

  DrillScanRelBase getPrimaryTableScan();

  RelOptCost getIntersectCost(IndexGroup index, IndexConditionInfo.Builder builder,
                                     RelOptPlanner planner);

  boolean satisfiesCollation();

  void setSatisfiesCollation(boolean satisfiesCollation);

  RelOptCost getSelfCost(RelOptPlanner planner);

  int numLeadingFilters();

  double getAvgRowSize();
}

