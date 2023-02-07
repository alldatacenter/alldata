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

import java.util.List;

import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;

/**
 * Abstract base class for an Index descriptor
 *
 */
public abstract class AbstractIndexDescriptor extends DrillIndexDefinition implements IndexDescriptor {

  public AbstractIndexDescriptor(List<LogicalExpression> indexCols,
                                 CollationContext indexCollationContext,
                                 List<LogicalExpression> nonIndexCols,
                                 List<LogicalExpression> rowKeyColumns,
                                 String indexName,
                                 String tableName,
                                 IndexType type,
                                 NullDirection nullsDirection) {
    super(indexCols, indexCollationContext, nonIndexCols, rowKeyColumns, indexName, tableName, type, nullsDirection);
  }

  @Override
  public double getRows(RelNode scan, RexNode indexCondition) {
    throw new UnsupportedOperationException("getRows() not supported for this index.");
  }

  @Override
  public boolean supportsRowCountStats() {
    return false;
  }

  @Override
  public IndexGroupScan getIndexGroupScan() {
    throw new UnsupportedOperationException("Group scan not supported for this index.");
  }

  @Override
  public boolean supportsFullTextSearch() {
    return false;
  }

  @Override
  public RelOptCost getCost(IndexProperties indexProps, RelOptPlanner planner,
      int numProjectedFields, GroupScan primaryGroupScan) {
    throw new UnsupportedOperationException("getCost() not supported for this index.");
  }

  @Override
  public boolean isAsyncIndex() {
    return true;
  }

}
