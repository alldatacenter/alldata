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
package org.apache.drill.exec.planner.index.rules;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.planner.index.IndexPhysicalPlanCallContext;
import org.apache.drill.exec.planner.index.IndexProperties;
import org.apache.drill.exec.planner.index.IndexSelector;
import org.apache.drill.exec.planner.index.IndexPlanUtils;
import org.apache.drill.exec.planner.index.IndexCollection;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.FunctionalIndexInfo;
import org.apache.drill.exec.planner.index.generators.AbstractIndexPlanGenerator;
import org.apache.drill.exec.planner.index.generators.CoveringPlanNoFilterGenerator;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.Prule;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.common.OrderedRel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.HashToRandomExchangePrel;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DbScanSortRemovalRule extends Prule {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DbScanSortRemovalRule.class);

  public static final RelOptRule INDEX_SORT_EXCHANGE_SCAN =
      new DbScanSortRemovalRule(RelOptHelper.some(OrderedRel.class,
          RelOptHelper.some(HashToRandomExchangePrel.class,
              RelOptHelper.any(ScanPrel.class))), "DbScanSortRemovalRule:sort_exchange_Scan", new MatchSES());

  public static final RelOptRule INDEX_SORT_SCAN =
          new DbScanSortRemovalRule(RelOptHelper.some(OrderedRel.class,
                          RelOptHelper.any(ScanPrel.class)), "DbScanSortRemovalRule:Sort_Scan", new MatchSS());

  public static final RelOptRule INDEX_SORT_PROJ_SCAN =
          new DbScanSortRemovalRule(RelOptHelper.some(OrderedRel.class,
                  RelOptHelper.some(ProjectPrel.class,
                    RelOptHelper.any(ScanPrel.class))), "DbScanSortRemovalRule:Sort_Proj_Scan", new MatchSPS());

  public static final RelOptRule INDEX_SORT_EXCHANGE_PROJ_SCAN =
      new DbScanSortRemovalRule(RelOptHelper.some(OrderedRel.class,
          RelOptHelper.some(HashToRandomExchangePrel.class,
              RelOptHelper.some(ProjectPrel.class,
                  RelOptHelper.any(ScanPrel.class)))), "DbScanSortRemovalRule:sort_exchange_proj_Scan", new MatchSEPS());

  final private MatchFunction match;

  private DbScanSortRemovalRule(RelOptRuleOperand operand,
                                   String description,
                                   MatchFunction match) {
    super(operand, description);
    this.match = match;
  }

  private static boolean isRemovableRel(OrderedRel node) {
    return node.canBeDropped();
  }

  private static class MatchSES extends AbstractMatchFunction<IndexPhysicalPlanCallContext> {

    public boolean match(RelOptRuleCall call) {
      final OrderedRel sort = call.rel(0);
      final ScanPrel scan = call.rel(2);
      return sort instanceof Prel && checkScan(scan.getGroupScan()) && isRemovableRel(sort);
    }

    public IndexPhysicalPlanCallContext onMatch(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(2);
      final OrderedRel sort = call.rel(0);
      final ExchangePrel exch = call.rel(1);
      return new IndexPhysicalPlanCallContext(call, sort, null, scan, exch);
    }
  }

  private static class MatchSS extends AbstractMatchFunction<IndexPhysicalPlanCallContext> {

    public boolean match(RelOptRuleCall call) {
      final OrderedRel sort = call.rel(0);
      final ScanPrel scan = call.rel(1);
      return sort instanceof Prel && checkScan(scan.getGroupScan()) && isRemovableRel(sort);
    }

    public IndexPhysicalPlanCallContext onMatch(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(1);
      final OrderedRel sort = call.rel(0);
      return new IndexPhysicalPlanCallContext(call, sort, null, scan, null);
    }
  }

  private static class MatchSPS extends AbstractMatchFunction<IndexPhysicalPlanCallContext> {

    public boolean match(RelOptRuleCall call) {
      final OrderedRel sort = call.rel(0);
      final ScanPrel scan = call.rel(2);
      return sort instanceof Prel && checkScan(scan.getGroupScan()) && isRemovableRel(sort);
    }

    public IndexPhysicalPlanCallContext onMatch(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(2);
      final ProjectPrel proj = call.rel(1);
      final OrderedRel sort = call.rel(0);
      return new IndexPhysicalPlanCallContext(call, sort, proj, scan, null);
    }
  }

  private static class MatchSEPS extends AbstractMatchFunction<IndexPhysicalPlanCallContext> {

    public boolean match(RelOptRuleCall call) {
      final OrderedRel sort = call.rel(0);
      final ScanPrel scan = call.rel(3);
      return sort instanceof Prel && checkScan(scan.getGroupScan()) && isRemovableRel(sort);
    }

    public IndexPhysicalPlanCallContext onMatch(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(3);
      final OrderedRel sort = call.rel(0);
      final ProjectPrel proj = call.rel(2);
      final ExchangePrel exch = call.rel(1);
      return new IndexPhysicalPlanCallContext(call,  sort, proj, scan, exch);
    }
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    return match.match(call);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    doOnMatch((IndexPhysicalPlanCallContext) match.onMatch(call));
  }

  private void doOnMatch(IndexPhysicalPlanCallContext indexContext) {
    Stopwatch indexPlanTimer = Stopwatch.createStarted();
    final PlannerSettings settings = PrelUtil.getPlannerSettings(indexContext.call.getPlanner());

    DbGroupScan groupScan = (DbGroupScan)indexContext.scan.getGroupScan();
    boolean isIndexScan = groupScan.isIndexScan();
    if (!isIndexScan) {
      // This case generates the index scan and removes the sort if possible.
      final IndexCollection indexCollection = groupScan.getSecondaryIndexCollection(indexContext.scan);
      if (indexCollection == null) {
        return;
      }
      if (settings.isStatisticsEnabled()) {
        groupScan.getStatistics().initialize(null, indexContext.scan, indexContext);
      }
      IndexPlanUtils.updateSortExpression(indexContext, indexContext.getSort() != null ?
              indexContext.getCollation().getFieldCollations() : null);
      IndexSelector selector = new IndexSelector(indexContext);
      for (IndexDescriptor indexDesc : indexCollection) {
        indexDesc.getIndexGroupScan().setStatistics(groupScan.getStatistics());
        FunctionalIndexInfo functionInfo = indexDesc.getFunctionalInfo();
        if (IndexPlanUtils.isCoveringIndex(indexContext, functionInfo)) {
          selector.addIndex(indexDesc, true,
                  indexContext.lowerProject != null ? indexContext.lowerProject.getRowType().getFieldCount() :
                          indexContext.scan.getRowType().getFieldCount());
        }
      }

      IndexProperties idxProp = selector.getBestIndexNoFilter();
      if (idxProp != null) {
        try {
          //generate a covering plan
          CoveringPlanNoFilterGenerator planGen =
                  new CoveringPlanNoFilterGenerator(indexContext, idxProp.getIndexDesc().getFunctionalInfo(),
                          false, settings);
          if (planGen.convertChild() != null) {
            indexContext.getCall().transformTo(planGen.convertChild());
          } else {
            logger.debug("Not able to generate index plan in {}", this.getClass().toString());
          }
        } catch (Exception e) {
          logger.warn("Exception while trying to generate indexscan to remove sort", e);
        }
      }
    } else {
      Preconditions.checkNotNull(indexContext.getSort());
      //This case tries to use the already generated index to see if a sort can be removed.
      if (indexContext.scan.getTraitSet().getTrait(RelCollationTraitDef.INSTANCE).getFieldCollations().size() == 0) {
        return;
      }
      try {
        RelNode finalRel = indexContext.scan.copy(indexContext.scan.getTraitSet(), indexContext.scan.getInputs());
        if (indexContext.lowerProject != null) {
          List<RelNode> inputs = Lists.newArrayList();
          inputs.add(finalRel);
          finalRel = indexContext.lowerProject.copy(indexContext.lowerProject.getTraitSet(), inputs);
        }

        finalRel = AbstractIndexPlanGenerator.getSortNode(indexContext, finalRel, true,false,
                  indexContext.exch != null);

        if (finalRel == null) {
          logger.debug("Not able to generate index plan in {}", this.getClass().toString());
          return;
        }

        finalRel = Prule.convert(finalRel, finalRel.getTraitSet().plus(Prel.DRILL_PHYSICAL));
        indexContext.getCall().transformTo(finalRel);
      } catch (Exception e) {
        logger.warn("Exception while trying to use the indexscan to remove the sort", e);
      }
    }

    indexPlanTimer.stop();
    logger.debug("Index Planning took {} ms", indexPlanTimer.elapsed(TimeUnit.MILLISECONDS));
  }
}
