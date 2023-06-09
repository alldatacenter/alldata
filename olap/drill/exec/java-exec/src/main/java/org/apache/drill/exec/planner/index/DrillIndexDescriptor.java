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

import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class DrillIndexDescriptor extends AbstractIndexDescriptor {

  private static final Logger logger = LoggerFactory.getLogger(DrillIndexDescriptor.class);

  /**
   * The name of Drill's Storage Plugin on which the Index was stored
   */
  private String storage;

  private DrillTable table;

  public DrillIndexDescriptor(List<LogicalExpression> indexCols,
                              CollationContext indexCollationContext,
                              List<LogicalExpression> nonIndexCols,
                              List<LogicalExpression> rowKeyColumns,
                              String indexName,
                              String tableName,
                              IndexType type,
                              NullDirection nullsDirection) {
    super(indexCols, indexCollationContext, nonIndexCols, rowKeyColumns, indexName, tableName, type, nullsDirection);
  }

  public DrillIndexDescriptor(DrillIndexDefinition def) {
    this(def.indexColumns, def.indexCollationContext, def.nonIndexColumns, def.rowKeyColumns, def.indexName,
        def.getTableName(), def.getIndexType(), def.nullsDirection);
  }

  @Override
  public double getRows(RelNode scan, RexNode indexCondition) {
    // TODO: real implementation is to use Drill's stats implementation. for now return fake value 1.0
    return 1.0;
  }

  @Override
  public IndexGroupScan getIndexGroupScan() {
    try {
      final DrillTable idxTable = getDrillTable();
      GroupScan scan = idxTable.getGroupScan();

      if (!(scan instanceof IndexGroupScan)){
        logger.error("The Groupscan from table {} is not an IndexGroupScan", idxTable.toString());
        return null;
      }
      return (IndexGroupScan)scan;
    }
    catch(IOException e) {
      logger.error("Error in getIndexGroupScan ", e);
    }
    return null;
  }

  /**
   * Set the storage plugin name
   * @param storageName
   */
  public void setStorageName(String storageName) {
    storage = storageName;
  }

  /**
   * Get storage plugin name for this index descriptor
   * @return name of the storage plugin
   */
  public String getStorageName() {
    return storage;
  }

  /**
   * Set the drill table corresponding to the index
   * @param table
   */
  public void setDrillTable(DrillTable table) {
    this.table = table;
  }

  /**
   * Get the drill table corresponding to the index descriptor
   * @return instance of DrillTable
   */
  public DrillTable getDrillTable() {
    return this.table;
  }

  @Override
  public FunctionalIndexInfo getFunctionalInfo() {
    return null;
  }

  @Override
  public PluginCost getPluginCostModel() {
    return null;
  }
}
