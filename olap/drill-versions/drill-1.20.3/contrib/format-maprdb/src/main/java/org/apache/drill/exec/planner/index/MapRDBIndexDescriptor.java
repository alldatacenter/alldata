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


import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.CloneVisitor;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.store.mapr.PluginConstants;
import org.apache.drill.exec.util.EncodedSchemaPathSet;
import org.apache.drill.common.expression.LogicalExpression;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class MapRDBIndexDescriptor extends DrillIndexDescriptor {

  protected final Object desc;
  protected final Set<LogicalExpression> allFields;
  protected final Set<LogicalExpression> indexedFields;
  protected MapRDBFunctionalIndexInfo functionalInfo;
  protected PluginCost pluginCost;

  public MapRDBIndexDescriptor(List<LogicalExpression> indexCols,
                               CollationContext indexCollationContext,
                               List<LogicalExpression> nonIndexCols,
                               List<LogicalExpression> rowKeyColumns,
                               String indexName,
                               String tableName,
                               IndexType type,
                               Object desc,
                               DbGroupScan scan,
                               NullDirection nullsDirection) {
    super(indexCols, indexCollationContext, nonIndexCols, rowKeyColumns, indexName, tableName, type, nullsDirection);
    this.desc = desc;
    this.indexedFields = ImmutableSet.copyOf(indexColumns);
    this.allFields = new ImmutableSet.Builder<LogicalExpression>()
        .add(PluginConstants.DOCUMENT_SCHEMA_PATH)
        .addAll(indexColumns)
        .addAll(nonIndexColumns)
        .build();
    this.pluginCost = scan.getPluginCostModel();
  }

  public Object getOriginalDesc(){
    return desc;
  }

  @Override
  public boolean isCoveringIndex(List<LogicalExpression> expressions) {
    List<LogicalExpression> decodedCols = new DecodePathinExpr().parseExpressions(expressions);
    return columnsInIndexFields(decodedCols, allFields);
  }

  @Override
  public boolean allColumnsIndexed(Collection<LogicalExpression> expressions) {
    List<LogicalExpression> decodedCols = new DecodePathinExpr().parseExpressions(expressions);
    return columnsInIndexFields(decodedCols, indexedFields);
  }

  @Override
  public boolean someColumnsIndexed(Collection<LogicalExpression> columns) {
    return columnsIndexed(columns, false);
  }

  private boolean columnsIndexed(Collection<LogicalExpression> expressions, boolean allColsIndexed) {
    List<LogicalExpression> decodedCols = new DecodePathinExpr().parseExpressions(expressions);
    if (allColsIndexed) {
      return columnsInIndexFields(decodedCols, indexedFields);
    } else {
      return someColumnsInIndexFields(decodedCols, indexedFields);
    }
  }

  public FunctionalIndexInfo getFunctionalInfo() {
    if (this.functionalInfo == null) {
      this.functionalInfo = new MapRDBFunctionalIndexInfo(this);
    }
    return this.functionalInfo;
  }

  /**
   * Search through a LogicalExpression, finding all referenced schema paths
   * and replace them with decoded paths.
   * If one encoded path could be decoded to multiple paths, add these decoded paths to
   * the end of returned list of expressions from parseExpressions.
   */
  private class DecodePathinExpr extends CloneVisitor {
    Set<SchemaPath> schemaPathSet = Sets.newHashSet();

    public List<LogicalExpression> parseExpressions(Collection<LogicalExpression> expressions) {
      List<LogicalExpression> allCols = Lists.newArrayList();
      Collection<SchemaPath> decoded;

      for (LogicalExpression expr : expressions) {
        LogicalExpression nonDecoded = expr.accept(this, null);
        if (nonDecoded != null) {
          allCols.add(nonDecoded);
        }
      }

      if (schemaPathSet.size() > 0) {
        decoded = EncodedSchemaPathSet.decode(schemaPathSet);
        allCols.addAll(decoded);
      }
      return allCols;
    }

    @Override
    public LogicalExpression visitSchemaPath(SchemaPath path, Void value) {
      if (EncodedSchemaPathSet.isEncodedSchemaPath(path)) {
        // if decoded size is not one, incoming path is encoded path thus there is no cast or other function applied on it,
        // since users won't pass in encoded fields, so it is safe to return null,
        schemaPathSet.add(path);
        return null;
      } else {
        return path;
      }
    }

  }

  @Override
  public RelOptCost getCost(IndexProperties indexProps, RelOptPlanner planner,
      int numProjectedFields, GroupScan primaryTableGroupScan) {
    Preconditions.checkArgument(primaryTableGroupScan instanceof DbGroupScan);
    DbGroupScan dbGroupScan = (DbGroupScan) primaryTableGroupScan;
    DrillCostFactory costFactory = (DrillCostFactory)planner.getCostFactory();
    double totalRows = indexProps.getTotalRows();
    double leadRowCount = indexProps.getLeadingSelectivity() * totalRows;
    double avgRowSize = indexProps.getAvgRowSize();
    if (indexProps.isCovering()) { // covering index
      // int numIndexCols = allFields.size();
      // for disk i/o, all index columns are going to be read into memory
      double numBlocks = Math.ceil((leadRowCount * avgRowSize)/pluginCost.getBlockSize(primaryTableGroupScan));
      double diskCost = numBlocks * pluginCost.getSequentialBlockReadCost(primaryTableGroupScan);
      // cpu cost is cost of filter evaluation for the remainder condition
      double cpuCost = 0.0;
      if (indexProps.getTotalRemainderFilter() != null) {
        cpuCost = leadRowCount * DrillCostBase.COMPARE_CPU_COST;
      }
      double networkCost = 0.0; // TODO: add network cost once full table scan also considers network cost
      return costFactory.makeCost(leadRowCount, cpuCost, diskCost, networkCost);

    } else { // non-covering index
      // int numIndexCols = allFields.size();
      double numBlocksIndex = Math.ceil((leadRowCount * avgRowSize)/pluginCost.getBlockSize(primaryTableGroupScan));
      double diskCostIndex = numBlocksIndex * pluginCost.getSequentialBlockReadCost(primaryTableGroupScan);
      // for the primary table join-back each row may belong to a different block, so in general num_blocks = num_rows;
      // however, num_blocks cannot exceed the total number of blocks of the table
      double totalBlocksPrimary = Math.ceil((dbGroupScan.getColumns().size() *
          pluginCost.getAverageColumnSize(primaryTableGroupScan) * totalRows)/
          pluginCost.getBlockSize(primaryTableGroupScan));
      double diskBlocksPrimary = Math.min(totalBlocksPrimary, leadRowCount);
      double diskCostPrimary = diskBlocksPrimary * pluginCost.getRandomBlockReadCost(primaryTableGroupScan);
      double diskCostTotal = diskCostIndex + diskCostPrimary;

      // cpu cost of remainder condition evaluation over the selected rows
      double cpuCost = 0.0;
      if (indexProps.getTotalRemainderFilter() != null) {
        cpuCost = leadRowCount * DrillCostBase.COMPARE_CPU_COST;
      }
      double networkCost = 0.0; // TODO: add network cost once full table scan also considers network cost
      return costFactory.makeCost(leadRowCount, cpuCost, diskCostTotal, networkCost);
    }
  }

  // Future use once full table scan also includes network cost
  private double getNetworkCost(double leadRowCount, int numProjectedFields, boolean isCovering,
      GroupScan primaryTableGroupScan) {
    if (isCovering) {
      // db server will send only the projected columns to the db client for the selected
      // number of rows, so network cost is based on the number of actual projected columns
      double networkCost = leadRowCount * numProjectedFields * pluginCost.getAverageColumnSize(primaryTableGroupScan);
      return networkCost;
    } else {
      // only the rowkey column is projected from the index and sent over the network
      double networkCostIndex = leadRowCount * 1 * pluginCost.getAverageColumnSize(primaryTableGroupScan);

      // after join-back to primary table, all projected columns are sent over the network
      double networkCostPrimary = leadRowCount * numProjectedFields * pluginCost.getAverageColumnSize(primaryTableGroupScan);

      return networkCostIndex + networkCostPrimary;
    }

  }

  @Override
  public PluginCost getPluginCostModel() {
    return pluginCost;
  }
}
