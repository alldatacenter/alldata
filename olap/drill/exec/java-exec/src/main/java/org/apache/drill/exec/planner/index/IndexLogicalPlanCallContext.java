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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.exec.planner.logical.DrillFilterRel;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillSortRel;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.common.OrderedRel;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.calcite.rel.RelNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IndexLogicalPlanCallContext implements IndexCallContext {
  final public RelOptRuleCall call;
  final public DrillSortRel sort;
  final public DrillProjectRel upperProject;
  final public DrillFilterRel filter;
  final public DrillProjectRel lowerProject;
  final public DrillScanRel scan;
  final public String indexHint;

  public Set<LogicalExpression> leftOutPathsInFunctions;

  public IndexableExprMarker origMarker;
  public List<LogicalExpression> sortExprs;

  public RexNode origPushedCondition;

  public IndexLogicalPlanCallContext(RelOptRuleCall call,
                       DrillProjectRel capProject,
                       DrillFilterRel filter,
                       DrillProjectRel project,
                       DrillScanRel scan) {
    this(call, null, capProject, filter, project, scan);
  }

  public IndexLogicalPlanCallContext(RelOptRuleCall call,
      DrillSortRel sort,
      DrillProjectRel capProject,
      DrillFilterRel filter,
      DrillProjectRel project,
      DrillScanRel scan) {
    this.call = call;
    this.sort = sort;
    this.upperProject = capProject;
    this.filter = filter;
    this.lowerProject = project;
    this.scan = scan;
    this.indexHint = ((DbGroupScan)this.scan.getGroupScan()).getIndexHint();
  }

  public IndexLogicalPlanCallContext(RelOptRuleCall call,
                       DrillSortRel sort,
                       DrillProjectRel project,
                       DrillScanRel scan) {
    this.call = call;
    this.sort = sort;
    this.upperProject = null;
    this.filter = null;
    this.lowerProject = project;
    this.scan = scan;
    this.indexHint = ((DbGroupScan)this.scan.getGroupScan()).getIndexHint();
  }

  public DbGroupScan getGroupScan() {
    return (DbGroupScan) scan.getGroupScan();
  }

  public DrillScanRelBase getScan() {
    return scan;
  }

  public List<RelCollation> getCollationList() {
    if (sort != null) {
      return sort.getCollationList();
    }
    return null;
  }

  public RelCollation getCollation() {
    if (sort != null) {
      return sort.getCollation();
    }
    return null;
  }

  public boolean hasLowerProject() {
    return lowerProject != null;
  }

  public boolean hasUpperProject() {
    return upperProject != null;
  }

  public RelOptRuleCall getCall() {
    return call;
  }

  public Set<LogicalExpression> getLeftOutPathsInFunctions() {
    return leftOutPathsInFunctions;
  }

  public RelNode getFilter() {
    return filter;
  }

  public IndexableExprMarker getOrigMarker() {
    return origMarker;
  }

  public List<LogicalExpression> getSortExprs() {
    return sortExprs;
  }

  public DrillProjectRelBase getLowerProject() {
    return lowerProject;
  }

  public DrillProjectRelBase getUpperProject() {
    return upperProject;
  }

  public void setLeftOutPathsInFunctions(Set<LogicalExpression> exprs) {
    leftOutPathsInFunctions = exprs;
  }

  public List<SchemaPath> getScanColumns() {
    return scan.getColumns();
  }

  public RexNode getFilterCondition() {
    return filter.getCondition();
  }

  public RexNode getOrigCondition() {
    return origPushedCondition;
  }

  public OrderedRel getSort() {
    return sort;
  }

  public void createSortExprs() {
    sortExprs = Lists.newArrayList();
  }

  public RelNode getExchange() { return null; }

  public List<DistributionField> getDistributionFields() { return Collections.EMPTY_LIST; }
}
