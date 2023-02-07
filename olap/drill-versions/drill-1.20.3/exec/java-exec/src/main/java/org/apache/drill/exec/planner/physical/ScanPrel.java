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
package org.apache.drill.exec.planner.physical;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanPrel extends DrillScanRelBase implements LeafPrel, HasDistributionAffinity {
  private static final Logger logger = LoggerFactory.getLogger(ScanPrel.class);

  private final RelDataType rowType;

  public ScanPrel(RelOptCluster cluster, RelTraitSet traits,
                  GroupScan groupScan, RelDataType rowType, RelOptTable table) {
    super(cluster, traits, getCopy(groupScan), table);
    this.rowType = rowType;
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new ScanPrel(this.getCluster(), traitSet, this.getGroupScan(),
        this.rowType, this.getTable());
  }

  @Override
  public ScanPrel copy(RelTraitSet traitSet, GroupScan scan, RelDataType rowType) {
    return new ScanPrel(getCluster(), traitSet, scan, rowType, getTable());
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new ScanPrel(this.getCluster(), this.getTraitSet(), getCopy(this.getGroupScan()),
        this.rowType, this.getTable());
  }

  @SuppressWarnings("unchecked")
  private static GroupScan getCopy(GroupScan scan){
    try {
      return (GroupScan) scan.getNewWithChildren((List<PhysicalOperator>) (Object) Collections.emptyList());
    } catch (ExecutionSetupException e) {
      throw new DrillRuntimeException("Unexpected failure while coping node.", e);
    }
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator)
      throws IOException {
    return creator.addMetadata(this, this.getGroupScan());
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("groupscan", this.getGroupScan().getDigest());
  }

  @Override
  public RelDataType deriveRowType() {
    return this.rowType;
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    final PlannerSettings settings = PrelUtil.getPlannerSettings(getCluster());

    double rowCount = this.getGroupScan().getScanStats(settings).getRecordCount();
    logger.debug("#{}.estimateRowCount get rowCount {} from  groupscan {}",
        this.getId(), rowCount, System.identityHashCode(this.getGroupScan()));
    return rowCount;
  }

  @Override
  public RelOptCost computeSelfCost(final RelOptPlanner planner, RelMetadataQuery mq) {
    final PlannerSettings settings = PrelUtil.getPlannerSettings(planner);
    final ScanStats stats = getGroupScan().getScanStats(settings);
    final int columnCount = getRowType().getFieldCount();

    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return planner.getCostFactory().makeCost(stats.getRecordCount() * columnCount, stats.getCpuCost(), stats.getDiskCost());
    }

    double rowCount = mq.getRowCount(this);

    // As DRILL-4083 points out, when columnCount == 0, cpuCost becomes zero,
    // which makes the costs of HiveScan and HiveDrillNativeParquetScan the same
    // For now, assume cpu cost is proportional to row count.
    // Note that this ignores the disk cost estimate (which should be a proxy for
    // row count * row width.)
    double cpuCost = rowCount * Math.max(columnCount, 1);

    // If a positive value for CPU cost is given multiply the default CPU cost by given CPU cost.
    if (stats.getCpuCost() > 0) {
      cpuCost *= stats.getCpuCost();
    }

    double ioCost = stats.getDiskCost();
    DrillCostFactory costFactory = (DrillCostFactory)planner.getCostFactory();
    return costFactory.makeCost(rowCount, cpuCost, ioCost, 0);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitScan(this, value);
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public DistributionAffinity getDistributionAffinity() {
    return this.getGroupScan().getDistributionAffinity();
  }

}
