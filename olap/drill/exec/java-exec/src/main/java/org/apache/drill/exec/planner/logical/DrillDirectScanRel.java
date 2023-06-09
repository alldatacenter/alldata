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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.store.direct.DirectGroupScan;

/**
 * Logical RelNode representing a {@link DirectGroupScan}. This is not backed by a {@link DrillTable},
 * unlike {@link DrillScanRel}.
 */
public class DrillDirectScanRel extends AbstractRelNode implements DrillRel {

  private final DirectGroupScan groupScan;
  private final RelDataType rowType;

  public DrillDirectScanRel(RelOptCluster cluster, RelTraitSet traitSet, DirectGroupScan directGroupScan,
                            RelDataType rowType) {
    super(cluster, traitSet);
    this.groupScan = directGroupScan;
    this.rowType = rowType;
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    return null;
  }

  @Override
  public RelDataType deriveRowType() {
    return this.rowType;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("directscan", groupScan.getDigest());
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    final PlannerSettings settings = PrelUtil.getPlannerSettings(getCluster());
    return groupScan.getScanStats(settings).getRecordCount();
  }

  public DirectGroupScan getGroupScan() {
    return groupScan;
  }
}
