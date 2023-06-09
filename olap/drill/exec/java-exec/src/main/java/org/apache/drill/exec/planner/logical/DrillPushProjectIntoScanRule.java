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

import org.apache.calcite.adapter.enumerable.EnumerableTableScan;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.rules.ProjectRemoveRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.common.DrillRelOptUtil.ProjectPushInfo;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.util.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * When table support project push down, rule can be applied to reduce number of read columns
 * thus improving scan operator performance.
 */
public class DrillPushProjectIntoScanRule extends RelOptRule {
  public static final RelOptRule INSTANCE =
      new DrillPushProjectIntoScanRule(LogicalProject.class,
          EnumerableTableScan.class,
          "DrillPushProjectIntoScanRule:enumerable") {

        @Override
        protected boolean skipScanConversion(RelDataType projectRelDataType, TableScan scan) {
          // do not allow skipping conversion of EnumerableTableScan to DrillScanRel if rule is applicable
          return false;
        }
      };

  public static final RelOptRule DRILL_LOGICAL_INSTANCE =
      new DrillPushProjectIntoScanRule(LogicalProject.class,
          DrillScanRel.class,
          "DrillPushProjectIntoScanRule:logical");

  public static final RelOptRule DRILL_PHYSICAL_INSTANCE =
      new DrillPushProjectIntoScanRule(ProjectPrel.class,
          ScanPrel.class,
          "DrillPushProjectIntoScanRule:physical") {

        @Override
        protected ScanPrel createScan(TableScan scan, ProjectPushInfo projectPushInfo) {
          ScanPrel drillScan = (ScanPrel) scan;

          return new ScanPrel(drillScan.getCluster(),
              drillScan.getTraitSet().plus(Prel.DRILL_PHYSICAL),
              drillScan.getGroupScan().clone(projectPushInfo.getFields()),
              projectPushInfo.createNewRowType(drillScan.getCluster().getTypeFactory()),
              drillScan.getTable());
        }

        @Override
        protected ProjectPrel createProject(Project project, TableScan newScan, List<RexNode> newProjects) {
          return new ProjectPrel(project.getCluster(),
              project.getTraitSet().plus(Prel.DRILL_PHYSICAL),
              newScan,
              newProjects,
              project.getRowType());
        }
      };

  public DrillPushProjectIntoScanRule(Class<? extends Project> projectClass, Class<? extends TableScan> scanClass, String description) {
    super(RelOptHelper.some(projectClass, RelOptHelper.any(scanClass)), description);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    Project project = call.rel(0);
    TableScan scan = call.rel(1);

    try {
      if (scan.getRowType().getFieldList().isEmpty()) {
        return;
      }

      ProjectPushInfo projectPushInfo = DrillRelOptUtil.getFieldsInformation(scan.getRowType(), project.getProjects());
      if (!canPushProjectIntoScan(scan.getTable(), projectPushInfo)
          || skipScanConversion(projectPushInfo.createNewRowType(project.getCluster().getTypeFactory()), scan)) {
        // project above scan may be removed in ProjectRemoveRule for the case when it is trivial
        return;
      }

      TableScan newScan = createScan(scan, projectPushInfo);

      List<RexNode> newProjects = new ArrayList<>();
      for (RexNode n : project.getChildExps()) {
        newProjects.add(n.accept(projectPushInfo.getInputReWriter()));
      }

      Project newProject =
          createProject(project, newScan, newProjects);

      if (ProjectRemoveRule.isTrivial(newProject)) {
        call.transformTo(newScan);
      } else {
        call.transformTo(newProject);
      }
    } catch (IOException e) {
      throw new DrillRuntimeException(e);
    }
  }

  /**
   * Checks whether conversion of input {@code TableScan} instance to target
   * {@code TableScan} may be omitted.
   *
   * @param projectRelDataType project rel data type
   * @param scan               TableScan to be checked
   * @return true if specified {@code TableScan} has the same row type as specified.
   */
  protected boolean skipScanConversion(RelDataType projectRelDataType, TableScan scan) {
    return projectRelDataType.equals(scan.getRowType());
  }

  /**
   * Creates new {@code DrillProjectRelBase} instance with specified {@code TableScan newScan} child
   * and {@code List<RexNode> newProjects} expressions using specified {@code Project project} as prototype.
   *
   * @param project     the prototype of resulting project
   * @param newScan     new project child
   * @param newProjects new project expressions
   * @return new project instance
   */
  protected Project createProject(Project project, TableScan newScan, List<RexNode> newProjects) {
    return new DrillProjectRel(project.getCluster(),
        project.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
        newScan,
        newProjects,
        project.getRowType());
  }

  /**
   * Creates new {@code DrillScanRelBase} instance with row type and fields list
   * obtained from specified {@code ProjectPushInfo projectPushInfo}
   * using specified {@code TableScan scan} as prototype.
   *
   * @param scan            the prototype of resulting scan
   * @param projectPushInfo the source of row type and fields list
   * @return new scan instance
   */
  protected TableScan createScan(TableScan scan, ProjectPushInfo projectPushInfo) {
    return new DrillScanRel(scan.getCluster(),
        scan.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
        scan.getTable(),
        projectPushInfo.createNewRowType(scan.getCluster().getTypeFactory()),
        projectPushInfo.getFields());
  }

  /**
   * Push project into scan be done only if this is not a star query and
   * table supports project push down.
   *
   * @param table table instance
   * @param projectPushInfo fields information
   * @return true if push project into scan can be performed, false otherwise
   */
  protected boolean canPushProjectIntoScan(RelOptTable table, ProjectPushInfo projectPushInfo) throws IOException {
    DrillTable drillTable = Utilities.getDrillTable(table);
    return !Utilities.isStarQuery(projectPushInfo.getFields())
        && drillTable.getGroupScan().canPushdownProjects(projectPushInfo.getFields());
  }
}
