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
package org.apache.drill.exec.store.base.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillFilterRel;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.base.filter.ExprNode.AndNode;
import org.apache.drill.exec.store.base.filter.FilterPushDownListener.ScanPushDownListener;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

/**
 * Generalized filter push-down strategy which performs all the tree-walking
 * and tree restructuring work, allowing a "listener" to do the work needed
 * for a particular scan.
 * <p>
 * General usage in a storage plugin: <code><pre>
 * public Set<StoragePluginOptimizerRule> getPhysicalOptimizerRules(
 *        OptimizerRulesContext optimizerRulesContext) {
 *   return FilterPushDownStrategy.rulesFor(optimizerRulesContext,
 *      new MyPushDownListener(...));
 * }
 * </pre></code>
 */
public class FilterPushDownStrategy {

  private static final Collection<String> BANNED_OPERATORS =
      Collections.singletonList("flatten");

  /**
   * Base rule that passes target information to the push-down strategy
   */
  private static abstract class AbstractFilterPushDownRule extends StoragePluginOptimizerRule {

    protected final FilterPushDownStrategy strategy;

    public AbstractFilterPushDownRule(RelOptRuleOperand operand, String description,
        FilterPushDownStrategy strategy) {
      super(operand, description);
      this.strategy = strategy;
    }
  }

  /**
   * Custom rule passed to Calcite for FILTER --> PROJECT --> SCAN
   */
  private static class ProjectAndFilterRule extends AbstractFilterPushDownRule {

    private ProjectAndFilterRule(FilterPushDownStrategy strategy) {
      super(RelOptHelper.some(FilterPrel.class, RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class))),
            strategy.namePrefix() + "PushDownFilter:Filter_On_Project",
            strategy);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      if (!super.matches(call)) {
        return false;
      }
      DrillScanRel scan = call.rel(2);
      return strategy.isTargetScan(scan);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      DrillFilterRel filterRel = call.rel(0);
      DrillProjectRel projectRel = call.rel(1);
      DrillScanRel scanRel = call.rel(2);
      strategy.onMatch(call, filterRel, projectRel, scanRel);
    }
  }

  /**
   * Custom rule passed to Calcite to handle FILTER --> SCAN
   */
  private static class FilterWithoutProjectRule extends AbstractFilterPushDownRule {

    private FilterWithoutProjectRule(FilterPushDownStrategy strategy) {
      super(RelOptHelper.some(DrillFilterRel.class, RelOptHelper.any(DrillScanRel.class)),
            strategy.namePrefix() + "PushDownFilter:Filter_On_Scan",
            strategy);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      if (!super.matches(call)) {
        return false;
      }
      DrillScanRel scan = call.rel(1);
      return strategy.isTargetScan(scan);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      DrillFilterRel filterRel = call.rel(0);
      DrillScanRel scanRel = call.rel(1);
      strategy.onMatch(call, filterRel, null, scanRel);
    }
  }

  /**
   * Implement filter push-down for one scan.
   */
  private static class FilterPushDownBuilder {

    private final RelOptRuleCall call;
    private final DrillFilterRel filter;
    private final DrillProjectRel project;
    private final DrillScanRel scan;
    private final ScanPushDownListener scanListener;
    // Predicates which cannot be converted to a filter predicate
    List<RexNode> nonConvertedPreds = new ArrayList<>();

    protected FilterPushDownBuilder(RelOptRuleCall call, DrillFilterRel filter, DrillProjectRel project, DrillScanRel scan, ScanPushDownListener scanListener) {
      this.call = call;
      this.filter = filter;
      this.project = project;
      this.scan = scan;
      this.scanListener = scanListener;
    }

    void apply() {
      AndNode cnfNode = sortPredicates();
      if (cnfNode == null) {
        return;
      }

      Pair<GroupScan, List<RexNode>> translated =
          scanListener.transform(cnfNode);

      // Listener abandoned effort. (Allows a stub early in development.)
      if (translated == null) {
        return;
      }

      // Listener rejected the DNF terms
      GroupScan newGroupScan = translated.left;
      if (newGroupScan == null) {
        return;
      }

      // Gather unqualified and rewritten predicates
      List<RexNode> remainingPreds = new ArrayList<>();
      remainingPreds.addAll(nonConvertedPreds);
      if (translated.right != null) {
        remainingPreds.addAll(translated.right);
      }

      // Replace the child with the new filter on top of the child/scan
      call.transformTo(rebuildTree(newGroupScan, remainingPreds));
    }

    private AndNode sortPredicates() {

      // Get the filter expression
      RexNode condition;
      if (project == null) {
        condition = filter.getCondition();
      } else {
        // get the filter as if it were below the projection.
        condition = RelOptUtil.pushPastProject(filter.getCondition(), project);
      }

      // Skip if no expression or expression is trivial.
      // This seems to never happen because Calcite optimizes away
      // any expression of the form WHERE true, 1 = 1 or 0 = 1.
      if (condition == null || condition.isAlwaysTrue() || condition.isAlwaysFalse()) {
        return null;
      }

      // Get a conjunction of the filter conditions. For each conjunction, if it refers
      // to ITEM or FLATTEN expression then it cannot be pushed down. Otherwise, it's
      // qualified to be pushed down.
      List<RexNode> filterPreds = RelOptUtil.conjunctions(
          RexUtil.toCnf(filter.getCluster().getRexBuilder(), condition));

      DrillParseContext parseContext = new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner()));
      List<ExprNode> conjuncts = new ArrayList<>();
      for (RexNode pred : filterPreds) {
        ExprNode conjunct = identifyCandidate(parseContext, scan, pred);
        if (conjunct == null) {
          nonConvertedPreds.add(pred);
        } else {
          conjunct.tag(pred);
          conjuncts.add(conjunct);
        }
      }
      return conjuncts.isEmpty() ? null : new AndNode(conjuncts);
    }

    public ExprNode identifyCandidate(DrillParseContext parseContext, DrillScanRel scan, RexNode pred) {
      if (DrillRelOptUtil.findOperators(pred, Collections.emptyList(), BANNED_OPERATORS) != null) {
        return null;
      }

      // Extract an AND term, which may be an OR expression.
      LogicalExpression drillPredicate = DrillOptiq.toDrill(parseContext, scan, pred);
      ExprNode expr = drillPredicate.accept(FilterPushDownUtils.REL_OP_EXTRACTOR, null);
      if (expr == null) {
        return null;
      }

      // Check if each term can be pushed down, and, if so, return a new RelOp
      // with the value normalized.
      return scanListener.accept(expr);
    }

    /**
     * Rebuilds the query plan subtree to include any substitutions and removals requested
     * by the listener.
     *
     * @param newGroupScan the optional replacement scan node given by the listener
     * @param remainingPreds the Calcite predicates which the listener *does not* handle
     * and which should remain in the plan tree
     * @return a rebuilt query subtree
     */
    private RelNode rebuildTree(GroupScan newGroupScan, List<RexNode> remainingPreds) {

      // Rebuild the subtree with transformed nodes.

      // Scan: new if available, else existing.
      RelNode newNode;
      if (newGroupScan == null) {
        newNode = scan;
      } else {
        newNode = new DrillScanRel(scan.getCluster(), scan.getTraitSet(), scan.getTable(),
            newGroupScan, scan.getRowType(), scan.getColumns());
      }

      // Copy project, if exists
      if (project != null) {
        newNode = project.copy(project.getTraitSet(), Collections.singletonList(newNode));
      }

      // Add filter, if any predicates remain.
      if (!remainingPreds.isEmpty()) {

        // If some of the predicates weren't used in the filter, creates new filter with them
        // on top of current scan. Excludes the case when all predicates weren't used in the filter.
        // FILTER(a, b, c) --> SCAN becomes FILTER(a, d) --> SCAN
        newNode = filter.copy(filter.getTraitSet(), newNode,
            RexUtil.composeConjunction(
                filter.getCluster().getRexBuilder(),
                remainingPreds,
                true));
      }

      return newNode;
    }
  }

  private final FilterPushDownListener listener;

  public FilterPushDownStrategy(FilterPushDownListener listener) {
    this.listener = listener;
  }

  public Set<StoragePluginOptimizerRule> rules() {
    return ImmutableSet.of(
        new ProjectAndFilterRule(this),
        new FilterWithoutProjectRule(this));
  }

  public static Set<StoragePluginOptimizerRule> rulesFor(
      FilterPushDownListener listener) {
    return new FilterPushDownStrategy(listener).rules();
  }

  private String namePrefix() { return listener.prefix(); }

  private boolean isTargetScan(DrillScanRel scan) {
    return listener.isTargetScan(scan.getGroupScan());
  }

  public void onMatch(RelOptRuleCall call, DrillFilterRel filter, DrillProjectRel project, DrillScanRel scan) {

    // Skip if rule has already been applied.
    ScanPushDownListener scanListener = listener.builderFor(scan.getGroupScan());
    if (scanListener != null) {
      new FilterPushDownBuilder(call, filter, project, scan, scanListener).apply();
    }
  }
}
