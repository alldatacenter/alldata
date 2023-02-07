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

import java.io.IOException;
import java.util.List;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTranslatableTable;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.drill.exec.util.Utilities;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

/**
 * Base class for logical/physical scan rel implemented in Drill.
 */
public abstract class DrillScanRelBase extends TableScan implements DrillRelNode {
  protected final GroupScan groupScan;
  protected final DrillTable drillTable;

  public DrillScanRelBase(RelOptCluster cluster,
                          RelTraitSet traits,
                          RelOptTable table,
                          final List<SchemaPath> columns) {
    super(cluster, traits, table);
    this.drillTable = Utilities.getDrillTable(table);
    assert drillTable != null;
    try {
      this.groupScan = drillTable.getGroupScan().clone(columns);
    } catch (final IOException e) {
      throw new DrillRuntimeException("Failure creating scan.", e);
    }
  }

  public DrillScanRelBase(RelOptCluster cluster,
                          RelTraitSet traits,
                          GroupScan grpScan,
                          RelOptTable table) {
    super(cluster, traits, table);
    DrillTable unwrap = table.unwrap(DrillTable.class);
    if (unwrap == null) {
      unwrap = table.unwrap(DrillTranslatableTable.class).getDrillTable();
    }
    this.drillTable = unwrap;
    assert drillTable != null;
    this.groupScan = grpScan;
  }

  public DrillTable getDrillTable() {
    return drillTable;
  }

  public GroupScan getGroupScan() {
    return groupScan;
  }

  @Override public double estimateRowCount(RelMetadataQuery mq) {
    return mq.getRowCount(this);
  }

  @Override public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    double dRows = estimateRowCount(mq);
    double dCpu = dRows + 1; // ensure non-zero cost
    double dIo = 0;
    return planner.getCostFactory().makeCost(dRows, dCpu, dIo);
  }

  public abstract DrillScanRelBase copy(RelTraitSet traitSet, GroupScan scan, RelDataType rowType);
}
