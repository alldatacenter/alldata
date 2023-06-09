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
package org.apache.drill.exec.store.parquet;

import org.apache.calcite.rel.core.Filter;
import org.apache.drill.exec.physical.base.AbstractGroupScanWithMetadata;
import org.apache.drill.exec.expr.FilterPredicate;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class FilePushDownFilter extends StoragePluginOptimizerRule {

  private static final Logger logger = LoggerFactory.getLogger(FilePushDownFilter.class);

  private static final Collection<String> BANNED_OPERATORS;

  static {
    BANNED_OPERATORS = new ArrayList<>(1);
    BANNED_OPERATORS.add("flatten");
  }

  public static RelOptRule getFilterOnProject(OptimizerRulesContext optimizerRulesContext) {
    return new FilePushDownFilter(
        RelOptHelper.some(FilterPrel.class, RelOptHelper.some(ProjectPrel.class, RelOptHelper.any(ScanPrel.class))),
        "FilePushDownFilter:Filter_On_Project", optimizerRulesContext) {

      @Override
      public boolean matches(RelOptRuleCall call) {
        final ScanPrel scan = call.rel(2);
        if (scan.getGroupScan().supportsFilterPushDown()) {
          return super.matches(call);
        }
        return false;
      }

      @Override
      public void onMatch(RelOptRuleCall call) {
        final FilterPrel filterRel = call.rel(0);
        final ProjectPrel projectRel = call.rel(1);
        final ScanPrel scanRel = call.rel(2);
        doOnMatch(call, filterRel, projectRel, scanRel);
      }

    };
  }

  public static StoragePluginOptimizerRule getFilterOnScan(OptimizerRulesContext optimizerContext) {
    return new FilePushDownFilter(
        RelOptHelper.some(FilterPrel.class, RelOptHelper.any(ScanPrel.class)),
        "FilePushDownFilter:Filter_On_Scan", optimizerContext) {

      @Override
      public boolean matches(RelOptRuleCall call) {
        final ScanPrel scan = call.rel(1);
        if (scan.getGroupScan().supportsFilterPushDown()) {
          return super.matches(call);
        }
        return false;
      }

      @Override
      public void onMatch(RelOptRuleCall call) {
        final FilterPrel filterRel = call.rel(0);
        final ScanPrel scanRel = call.rel(1);
        doOnMatch(call, filterRel, null, scanRel);
      }
    };
  }

  // private final boolean useNewReader;
  protected final OptimizerRulesContext optimizerContext;

  private FilePushDownFilter(RelOptRuleOperand operand, String id, OptimizerRulesContext optimizerContext) {
    super(operand, id);
    this.optimizerContext = optimizerContext;
  }

  protected void doOnMatch(RelOptRuleCall call, FilterPrel filter, ProjectPrel project, ScanPrel scan) {
    AbstractGroupScanWithMetadata<?> groupScan = (AbstractGroupScanWithMetadata<?>) scan.getGroupScan();
    if (groupScan.getFilter() != null && !groupScan.getFilter().equals(ValueExpressions.BooleanExpression.TRUE)) {
      return;
    }

    RexNode condition;
    if (project == null) {
      condition = filter.getCondition();
    } else {
      // get the filter as if it were below the projection.
      condition = RelOptUtil.pushPastProject(filter.getCondition(), project);
    }

    if (condition == null || condition.isAlwaysTrue()) {
      return;
    }

    // get a conjunctions of the filter condition. For each conjunction, if it refers to ITEM or FLATTEN expression
    // then we could not pushed down. Otherwise, it's qualified to be pushed down.
    // Limits the number of nodes that can be created out of the conversion to avoid
    // exponential growth of nodes count and further OOM
    final List<RexNode> predList = RelOptUtil.conjunctions(RexUtil.toCnf(filter.getCluster().getRexBuilder(), 100, condition));

    final List<RexNode> qualifiedPredList = new ArrayList<>();

    // list of predicates which cannot be converted to filter predicate
    List<RexNode> nonConvertedPredList = new ArrayList<>();

    for (RexNode pred : predList) {
      if (DrillRelOptUtil.findOperators(pred, Collections.emptyList(), BANNED_OPERATORS) == null) {
        LogicalExpression drillPredicate = DrillOptiq.toDrill(
            new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner())), scan, pred);

        // checks whether predicate may be used for filter pushdown
        FilterPredicate<?> filterPredicate =
            groupScan.getFilterPredicate(drillPredicate,
                optimizerContext,
                optimizerContext.getFunctionRegistry(), optimizerContext.getPlannerSettings().getOptions(), false);
        // collects predicates that contain unsupported for filter pushdown expressions
        // to build filter with them
        if (filterPredicate == null) {
          nonConvertedPredList.add(pred);
        }
        qualifiedPredList.add(pred);
      } else {
        nonConvertedPredList.add(pred);
      }
    }

    final RexNode qualifiedPred = RexUtil.composeConjunction(filter.getCluster().getRexBuilder(), qualifiedPredList, true);

    if (qualifiedPred == null) {
      return;
    }

    LogicalExpression conditionExp = DrillOptiq.toDrill(
        new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner())), scan, qualifiedPred);

    // Default - pass the original filter expr to (potentially) be used at run-time
    groupScan.setFilterForRuntime(conditionExp, optimizerContext); // later may remove or set to another filter (see below)

    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    AbstractGroupScanWithMetadata<?> newGroupScan = groupScan.applyFilter(conditionExp, optimizerContext,
        optimizerContext.getFunctionRegistry(), optimizerContext.getPlannerSettings().getOptions());
    if (timer != null) {
      logger.debug("Took {} ms to apply filter. ", timer.elapsed(TimeUnit.MILLISECONDS));
      timer.stop();
    }

    // For the case when newGroupScan wasn't created, the old one may
    // fully match the filter for the case when row group pruning did not happen.
    if (newGroupScan == null) {
      if (groupScan.isMatchAllMetadata()) {
        RelNode child = project == null ? scan : project;
        // If current row group fully matches filter,
        // but row group pruning did not happen, remove the filter.
        if (nonConvertedPredList.isEmpty()) {
          groupScan.setFilterForRuntime(null, optimizerContext); // disable the original filter expr (i.e. don't use it at run-time)
          call.transformTo(child);
        } else if (nonConvertedPredList.size() == predList.size()) {
          // None of the predicates participated in filter pushdown.
          return;
        } else {
          // If some of the predicates weren't used in the filter, creates new filter with them
          // on top of current scan. Excludes the case when all predicates weren't used in the filter.
          Filter theNewFilter  = filter.copy(filter.getTraitSet(), child,
            RexUtil.composeConjunction(
              filter.getCluster().getRexBuilder(),
              nonConvertedPredList,
              true));

          LogicalExpression filterPredicate = DrillOptiq.toDrill(
            new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner())), scan, theNewFilter.getCondition());

          groupScan.setFilterForRuntime(filterPredicate, optimizerContext); // pass the new filter expr to (potentialy) be used at run-time

          call.transformTo(theNewFilter); // Replace the child with the new filter on top of the child/scan
        }
      }
      return;
    }

    RelNode newNode = new ScanPrel(scan.getCluster(), scan.getTraitSet(), newGroupScan, scan.getRowType(), scan.getTable());

    if (project != null) {
      newNode = project.copy(project.getTraitSet(), Collections.singletonList(newNode));
    }

    if (newGroupScan.isMatchAllMetadata()) {
      // creates filter from the expressions which can't be pushed to the scan
      if (!nonConvertedPredList.isEmpty()) {
        Filter theFilterRel  = filter.copy(filter.getTraitSet(), newNode,
          RexUtil.composeConjunction(
            filter.getCluster().getRexBuilder(),
            nonConvertedPredList,
            true));

        LogicalExpression filterPredicate = DrillOptiq.toDrill(
          new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner())), scan, theFilterRel.getCondition());

        newGroupScan.setFilterForRuntime(filterPredicate, optimizerContext); // pass the new filter expr to (potentialy) be used at run-time

        newNode = theFilterRel; // replace the new node with the new filter on top of that new node
      }
      call.transformTo(newNode);
      return;
    }

    final RelNode newFilter = filter.copy(filter.getTraitSet(), Collections.singletonList(newNode));
    call.transformTo(newFilter);
  }
}
