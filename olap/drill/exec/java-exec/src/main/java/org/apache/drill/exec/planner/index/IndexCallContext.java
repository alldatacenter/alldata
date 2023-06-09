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

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.exec.planner.common.OrderedRel;
import java.util.List;
import java.util.Set;

public interface IndexCallContext {
  DrillScanRelBase getScan();

  DbGroupScan getGroupScan();

  List<RelCollation> getCollationList();

  RelCollation getCollation();

  boolean hasLowerProject();

  boolean hasUpperProject();

  RelOptRuleCall getCall();

  Set<LogicalExpression> getLeftOutPathsInFunctions();

  RelNode getFilter();

  IndexableExprMarker getOrigMarker();

  List<LogicalExpression> getSortExprs();

  DrillProjectRelBase getLowerProject();

  DrillProjectRelBase getUpperProject();

  void setLeftOutPathsInFunctions(Set<LogicalExpression> exprs);

  List<SchemaPath> getScanColumns();

  RexNode getFilterCondition();

  RexNode getOrigCondition();

  OrderedRel getSort();

  void createSortExprs();

  RelNode getExchange();

  List<DistributionField> getDistributionFields();
}
