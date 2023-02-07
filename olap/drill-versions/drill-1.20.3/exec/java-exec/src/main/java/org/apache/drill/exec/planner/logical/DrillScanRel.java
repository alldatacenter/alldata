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

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.torel.ConversionContext;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.util.Utilities;

/**
 * GroupScan of a Drill table.
 */
public class DrillScanRel extends DrillScanRelBase implements DrillRel {
  public static final int STAR_COLUMN_COST = 10_000;

  private PlannerSettings settings;
  private final List<SchemaPath> columns;
  private final boolean partitionFilterPushdown;
  private final RelDataType rowType;

  public DrillScanRel(final RelOptCluster cluster, final RelTraitSet traits,
                      final RelOptTable table) {
    this(cluster, traits, table, false);
  }

  public DrillScanRel(final RelOptCluster cluster, final RelTraitSet traits,
                      final RelOptTable table, boolean partitionFilterPushdown) {
    this(cluster, traits, table, table.getRowType(), getProjectedColumns(table, false), partitionFilterPushdown);
    this.settings = PrelUtil.getPlannerSettings(cluster.getPlanner());
  }

  public DrillScanRel(final RelOptCluster cluster, final RelTraitSet traits,
                      final RelOptTable table, final RelDataType rowType, final List<SchemaPath> columns) {
    this(cluster, traits, table, rowType, columns, false);
  }

  public DrillScanRel(final RelOptCluster cluster, final RelTraitSet traits,
                      final RelOptTable table, final RelDataType rowType, final List<SchemaPath> columns, boolean partitionFilterPushdown) {
    super(cluster, traits, table, columns);
    this.settings = PrelUtil.getPlannerSettings(cluster.getPlanner());
    this.rowType = rowType;
    Preconditions.checkNotNull(columns);
    this.columns = columns;
    this.partitionFilterPushdown = partitionFilterPushdown;
  }

  /** Creates a DrillScanRel for a particular GroupScan */
  public DrillScanRel(final RelOptCluster cluster, final RelTraitSet traits,
                      final RelOptTable table, final GroupScan groupScan, final RelDataType rowType, final List<SchemaPath> columns) {
    this(cluster, traits, table, groupScan, rowType, columns, false);
  }

  /** Creates a DrillScanRel for a particular GroupScan */
  public DrillScanRel(final RelOptCluster cluster, final RelTraitSet traits,
                      final RelOptTable table, final GroupScan groupScan, final RelDataType rowType, final List<SchemaPath> columns, boolean partitionFilterPushdown) {
    super(cluster, traits, groupScan, table);
    this.rowType = rowType;
    this.columns = columns;
    this.settings = PrelUtil.getPlannerSettings(cluster.getPlanner());
    this.partitionFilterPushdown = partitionFilterPushdown;
  }

  public List<SchemaPath> getColumns() {
    return this.columns;
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    Scan.Builder builder = Scan.builder();
    builder.storageEngine(drillTable.getStorageEngineName());
    builder.selection(new JSONOptions(drillTable.getSelection()));
    implementor.registerSource(drillTable);
    return builder.build();
  }

  public static DrillScanRel convert(Scan scan, ConversionContext context) {
    return new DrillScanRel(context.getCluster(), context.getLogicalTraits(),
        context.getTable(scan));
  }

  @Override
  public RelDataType deriveRowType() {
    return this.rowType;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("groupscan", getGroupScan().getDigest());
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return getGroupScan().getScanStats(settings).getRecordCount();
  }

  // TODO: this method is same as the one for ScanPrel...eventually we should consolidate
  // this and few other methods in a common base class which would be extended
  // by both logical and physical rels.
  // TODO: Further changes may have caused the versions to diverge.
  // TODO: Does not compute IO cost by default, but should. Changing that may break
  // existing plugins.
  @Override
  public RelOptCost computeSelfCost(final RelOptPlanner planner, RelMetadataQuery mq) {
    final ScanStats stats = getGroupScan().getScanStats(settings);
    double columnCount = Utilities.isStarQuery(columns)
      ? STAR_COLUMN_COST
      : Math.pow(getRowType().getFieldCount(), 2) / Math.max(columns.size(), 1);

    // double rowCount = RelMetadataQuery.getRowCount(this);
    double rowCount = Math.max(1, stats.getRecordCount());

    double valueCount = rowCount * columnCount;
    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      // TODO: makeCost() wants a row count, but we provide a value count.
      // Likely a bug, but too risky to change as it may affect existing plugins.
      // If we do make the fix, then the default costing path is the same as the
      // full cost path.
      // TODO: At this late date, with many plugins exploiting (if only by
      // accident) the default costing here, it is not clear if we even want
      // the planner to control the cost model. That is, remove this path.
      return planner.getCostFactory().makeCost(valueCount, stats.getCpuCost(), stats.getDiskCost());
    }

    double cpuCost;
    double ioCost;
    if (stats.getGroupScanProperty().hasFullCost()) {
      cpuCost = stats.getCpuCost();
      ioCost = stats.getDiskCost();
    } else {
      // for now, assume cpu cost is proportional to row count and number of columns
      cpuCost = valueCount;

      // Default io cost should be proportional to valueCount
      ioCost = 0;
    }
    return planner.getCostFactory().makeCost(rowCount, cpuCost, ioCost);
  }

  public boolean partitionFilterPushdown() {
    return this.partitionFilterPushdown;
  }

  public static List<SchemaPath> getProjectedColumns(final RelOptTable table, boolean isSelectStar) {
    List<String> columnNames = table.getRowType().getFieldNames();
    List<SchemaPath> projectedColumns = new ArrayList<>(columnNames.size());

    for (String columnName : columnNames) {
       projectedColumns.add(SchemaPath.getSimplePath(columnName));
    }

    // If the row-type doesn't contain the STAR keyword, then insert it
    // as we are dealing with a  SELECT_STAR query.
    if (isSelectStar && !Utilities.isStarQuery(projectedColumns)) {
      projectedColumns.add(SchemaPath.STAR_COLUMN);
    }

    return projectedColumns;
  }

  @Override
  public DrillScanRel copy(RelTraitSet traitSet, GroupScan scan, RelDataType rowType) {
    return new DrillScanRel(getCluster(), getTraitSet(), getTable(), scan, rowType, getColumns(), partitionFilterPushdown());
  }
}
