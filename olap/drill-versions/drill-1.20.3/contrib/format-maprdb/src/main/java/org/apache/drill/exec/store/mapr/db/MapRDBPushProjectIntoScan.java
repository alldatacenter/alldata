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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.rules.ProjectRemoveRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.mapr.db.json.JsonTableGroupScan;
import org.apache.drill.exec.util.Utilities;

import java.util.List;

/**
 * Push a physical Project into Scan. Currently, this rule is only doing projection pushdown for MapRDB-JSON tables
 * since it was needed for the secondary index feature which only applies to Json tables.
 * For binary tables, note that the DrillPushProjectIntoScanRule is still applicable during the logical
 * planning phase.
 */
public abstract class MapRDBPushProjectIntoScan extends StoragePluginOptimizerRule {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MapRDBPushProjectIntoScan.class);

  private MapRDBPushProjectIntoScan(RelOptRuleOperand operand, String description) {
    super(operand, description);
  }

  public static final StoragePluginOptimizerRule PROJECT_ON_SCAN = new MapRDBPushProjectIntoScan(
      RelOptHelper.some(ProjectPrel.class, RelOptHelper.any(ScanPrel.class)), "MapRDBPushProjIntoScan:Proj_On_Scan") {
    @Override
    public void onMatch(RelOptRuleCall call) {
      final ProjectPrel project = call.rel(0);
      final ScanPrel scan = call.rel(1);

      if (scan.getGroupScan() instanceof JsonTableGroupScan) {
        JsonTableGroupScan groupScan = (JsonTableGroupScan) scan.getGroupScan();

        doPushProjectIntoGroupScan(call, project, scan, groupScan);
      }
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(1);

      // See class level comments above for why only JsonGroupScan is considered
      if (scan.getGroupScan() instanceof JsonTableGroupScan) {
        return super.matches(call);
      }
      return false;
    }
  };

  protected void doPushProjectIntoGroupScan(RelOptRuleCall call,
      ProjectPrel project, ScanPrel scan, JsonTableGroupScan groupScan) {
    try {

      DrillRelOptUtil.ProjectPushInfo columnInfo =
          DrillRelOptUtil.getFieldsInformation(scan.getRowType(), project.getProjects());
      if (columnInfo == null || Utilities.isStarQuery(columnInfo.getFields())
          || !groupScan.canPushdownProjects(columnInfo.getFields())) {
        return;
      }
      RelTraitSet newTraits = call.getPlanner().emptyTraitSet();
      // Clear out collation trait
      for (RelTrait trait : scan.getTraitSet()) {
        if (!(trait instanceof RelCollation)) {
          newTraits.plus(trait);
        }
      }
      final ScanPrel newScan = new ScanPrel(scan.getCluster(), newTraits.plus(Prel.DRILL_PHYSICAL),
          groupScan.clone(columnInfo.getFields()),
          columnInfo.createNewRowType(project.getInput().getCluster().getTypeFactory()), scan.getTable());

      List<RexNode> newProjects = Lists.newArrayList();
      for (RexNode n : project.getChildExps()) {
        newProjects.add(n.accept(columnInfo.getInputReWriter()));
      }

      final ProjectPrel newProj =
          new ProjectPrel(project.getCluster(),
              project.getTraitSet().plus(Prel.DRILL_PHYSICAL),
              newScan,
              newProjects,
              project.getRowType());

      if (ProjectRemoveRule.isTrivial(newProj) &&
          // the old project did not involve any column renaming
          sameRowTypeProjectionsFields(project.getRowType(), newScan.getRowType())) {
        call.transformTo(newScan);
      } else {
        call.transformTo(newProj);
      }
    } catch (Exception e) {
      throw new DrillRuntimeException(e);
    }
  }

  private boolean sameRowTypeProjectionsFields(RelDataType oldRowType, RelDataType newRowType) {
    for (RelDataTypeField oldField : oldRowType.getFieldList()) {
      String oldProjName = oldField.getName();
      boolean match = false;
      for (RelDataTypeField newField : newRowType.getFieldList()) {
        if (oldProjName.equals(newField.getName())) {
          match = true;
          break;
        }
      }
      if (!match) {
        return false;
      }
    }
    return true;
  }
}
