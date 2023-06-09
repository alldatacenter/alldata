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
package org.apache.drill.exec.store.mapr.db;

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexLiteral;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.LimitPrel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.RowKeyJoinPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.hbase.HBaseScanSpec;
import org.apache.drill.exec.store.mapr.db.binary.BinaryTableGroupScan;
import org.apache.drill.exec.store.mapr.db.json.JsonTableGroupScan;
import org.apache.drill.exec.store.mapr.db.json.RestrictedJsonTableGroupScan;

public abstract class MapRDBPushLimitIntoScan extends StoragePluginOptimizerRule {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MapRDBPushLimitIntoScan.class);

  private MapRDBPushLimitIntoScan(RelOptRuleOperand operand, String description) {
    super(operand, description);
  }

  public static final StoragePluginOptimizerRule LIMIT_ON_SCAN =
      new MapRDBPushLimitIntoScan(RelOptHelper.some(LimitPrel.class, RelOptHelper.any(ScanPrel.class)),
          "MapRDBPushLimitIntoScan:Limit_On_Scan") {

    @Override
    public void onMatch(RelOptRuleCall call) {
      final LimitPrel limit = call.rel(0);
      final ScanPrel scan = call.rel(1);
      doPushLimitIntoGroupScan(call, limit, null, scan, scan.getGroupScan());
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(1);
      final LimitPrel limit = call.rel(0);
      // pushdown only apply limit but not offset,
      // so if getFetch() return null no need to run this rule.
      if (scan.getGroupScan().supportsLimitPushdown()
            && !limit.isPushDown() && limit.getFetch() != null) {
        if ((scan.getGroupScan() instanceof JsonTableGroupScan
              && ((JsonTableGroupScan) scan.getGroupScan()).isIndexScan())
            || (scan.getGroupScan() instanceof RestrictedJsonTableGroupScan)) {
          return true;
        }
      }
      return false;
    }
  };

  public static final StoragePluginOptimizerRule LIMIT_ON_PROJECT =
      new MapRDBPushLimitIntoScan(RelOptHelper.some(LimitPrel.class,
          RelOptHelper.any(ProjectPrel.class)), "MapRDBPushLimitIntoScan:Limit_On_Project") {

    @Override
    public void onMatch(RelOptRuleCall call) {
      final ProjectPrel project = call.rel(1);
      final LimitPrel limit = call.rel(0);
      RelNode child = project.getInput();
      final RelNode limitUnderProject = new LimitPrel(child.getCluster(), child.getTraitSet(),
          child, limit.getOffset(), limit.getFetch());
      final RelNode newProject = new ProjectPrel(project.getCluster(), project.getTraitSet(),
          limitUnderProject, project.getProjects(), project.getRowType());
      if (DrillRelOptUtil.isProjectFlatten(project)) {
        //Preserve limit above the project since Flatten can produce more rows. Also mark it so we do not fire the rule again.
        child = newProject;
        final RelNode limitAboveProject = new LimitPrel(child.getCluster(), child.getTraitSet(),
            child, limit.getOffset(), limit.getFetch(), true);
        call.transformTo(limitAboveProject);
      } else {
        call.transformTo(newProject);
      }
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      LimitPrel limitPrel = call.rel(0);
      ProjectPrel projectPrel = call.rel(1);
      // pushdown only apply limit but not offset,
      // so if getFetch() return null no need to run this rule.
      // Do not push across Project containing CONVERT_FROMJSON for limit 0 queries. For limit 0 queries, this would
      // mess up the schema since Convert_FromJson() is different from other regular functions in that it only knows
      // the output schema after evaluation is performed. When input has 0 row, Drill essentially does not have a way
      // to know the output type.
      if (!limitPrel.isPushDown() && (limitPrel.getFetch() != null)
          && (!DrillRelOptUtil.isLimit0(limitPrel.getFetch())
          || !DrillRelOptUtil.isProjectOutputSchemaUnknown(projectPrel))) {
        return true;
      }
      return false;
    }
  };

  public static final StoragePluginOptimizerRule LIMIT_ON_RKJOIN =
      new MapRDBPushLimitIntoScan(RelOptHelper.some(LimitPrel.class, RelOptHelper.any(RowKeyJoinPrel.class)),
          "MapRDBPushLimitIntoScan:Limit_On_RKJoin") {

    @Override
    public void onMatch(RelOptRuleCall call) {
      final RowKeyJoinPrel join = call.rel(1);
      final LimitPrel limit = call.rel(0);
      doPushLimitIntoRowKeyJoin(call, limit, null, join);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final LimitPrel limit = call.rel(0);
      // We do not fire this rule if fetch() is null (indicating we have to fetch all the
      // remaining rows starting from offset.
      return !limit.isPushDown() && limit.getFetch() != null;
    }
  };

  protected void doPushLimitIntoGroupScan(RelOptRuleCall call,
      LimitPrel limit, final ProjectPrel project, ScanPrel scan, GroupScan groupScan) {
    try {
      final GroupScan newGroupScan = getGroupScanWithLimit(groupScan, limit);
      if (newGroupScan == null) {
        return;
      }
      final ScanPrel newScan = new ScanPrel(scan.getCluster(), scan.getTraitSet(), newGroupScan,
          scan.getRowType(), scan.getTable());
      final RelNode newChild;
      if (project != null) {
        final ProjectPrel newProject = new ProjectPrel(project.getCluster(), project.getTraitSet(),
            newScan, project.getProjects(), project.getRowType());
        newChild = newProject;
      } else {
        newChild = newScan;
      }
      call.transformTo(newChild);
      logger.debug("pushLimitIntoGroupScan: Converted to a new ScanPrel " + newScan.getGroupScan());
    } catch (Exception e) {
      logger.warn("pushLimitIntoGroupScan: Exception while trying limit pushdown!", e);
    }
  }

  private GroupScan getGroupScanWithLimit(GroupScan groupScan, LimitPrel limit) {
    final int offset = limit.getOffset() != null ? Math.max(0, RexLiteral.intValue(limit.getOffset())) : 0;
    final int fetch = Math.max(0, RexLiteral.intValue(limit.getFetch()));
    // Scan Limit uses conservative approach:  use offset 0 and fetch = parent limit offset + parent limit fetch.
    if (groupScan instanceof JsonTableGroupScan) {
      JsonTableGroupScan jsonTableGroupScan = (JsonTableGroupScan) groupScan;
      return (jsonTableGroupScan.clone(jsonTableGroupScan.getScanSpec()).applyLimit(offset + fetch));
    } else if (groupScan instanceof BinaryTableGroupScan) {
      BinaryTableGroupScan binaryTableGroupScan = (BinaryTableGroupScan) groupScan;
      final HBaseScanSpec oldScanSpec = binaryTableGroupScan.getHBaseScanSpec();
      final HBaseScanSpec newScanSpec = new HBaseScanSpec(oldScanSpec.getTableName(), oldScanSpec.getStartRow(),
          oldScanSpec.getStopRow(), oldScanSpec.getFilter());
      return new BinaryTableGroupScan(binaryTableGroupScan.getUserName(), binaryTableGroupScan.getStoragePlugin(),
          binaryTableGroupScan.getFormatPlugin(), newScanSpec, binaryTableGroupScan.getColumns(),
          binaryTableGroupScan.getTableStats(), binaryTableGroupScan.getMetadataProvider()).applyLimit(offset + fetch);
    }
    return null;
  }

  protected void doPushLimitIntoRowKeyJoin(RelOptRuleCall call,
    LimitPrel limit, final ProjectPrel project, RowKeyJoinPrel join) {
    final RelNode newChild;
    try {
      RelNode left = join.getLeft();
      RelNode right = join.getRight();
      final RelNode limitOnLeft = new LimitPrel(left.getCluster(), left.getTraitSet(), left,
          limit.getOffset(), limit.getFetch());
      RowKeyJoinPrel newJoin = new RowKeyJoinPrel(join.getCluster(), join.getTraitSet(), limitOnLeft, right,
          join.getCondition(), join.getJoinType());
      if (project != null) {
        final ProjectPrel newProject = new ProjectPrel(project.getCluster(), project.getTraitSet(), newJoin,
            project.getProjects(), project.getRowType());
        newChild = newProject;
      } else {
        newChild = newJoin;
      }
      call.transformTo(newChild);
      logger.debug("pushLimitIntoRowKeyJoin: Pushed limit on left side of Join " + join.toString());
    } catch (Exception e) {
      logger.warn("pushLimitIntoRowKeyJoin: Exception while trying limit pushdown!", e);
    }
  }
}

