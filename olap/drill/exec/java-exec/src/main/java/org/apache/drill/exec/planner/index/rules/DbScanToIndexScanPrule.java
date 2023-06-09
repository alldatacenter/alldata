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

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.common.DrillRelNode;
import org.apache.drill.exec.planner.index.IndexSelector;
import org.apache.drill.exec.planner.index.IndexPlanUtils;
import org.apache.drill.exec.planner.index.IndexCollection;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.FunctionalIndexInfo;
import org.apache.drill.exec.planner.index.IndexLogicalPlanCallContext;
import org.apache.drill.exec.planner.index.IndexConditionInfo;
import org.apache.drill.exec.planner.index.IndexGroup;
import org.apache.drill.exec.planner.index.IndexProperties;
import org.apache.drill.exec.planner.index.IndexableExprMarker;
import org.apache.drill.exec.planner.index.Statistics;
import org.apache.drill.exec.planner.index.generators.CoveringIndexPlanGenerator;
import org.apache.drill.exec.planner.index.generators.IndexIntersectPlanGenerator;
import org.apache.drill.exec.planner.index.generators.NonCoveringIndexPlanGenerator;
import org.apache.drill.exec.planner.logical.DrillFilterRel;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillSortRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.logical.partition.RewriteAsBinaryOperators;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.Prule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DbScanToIndexScanPrule extends Prule {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DbScanToIndexScanPrule.class);
  final public MatchFunction match;

  public static final RelOptRule REL_FILTER_SCAN = new DbScanToIndexScanPrule(
      RelOptHelper.some(DrillRelNode.class, RelOptHelper.some(DrillFilterRel.class, RelOptHelper.any(DrillScanRel.class))),
      "DbScanToIndexScanPrule:Rel_Filter_Scan", new MatchRelFS());

  public static final RelOptRule PROJECT_FILTER_PROJECT_SCAN = new DbScanToIndexScanPrule(
      RelOptHelper.some(DrillProjectRel.class, RelOptHelper.some(DrillFilterRel.class,
         RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class)))),
     "DbScanToIndexScanPrule:Project_Filter_Project_Scan", new MatchPFPS());

  public static final RelOptRule SORT_FILTER_PROJECT_SCAN = new DbScanToIndexScanPrule(
     RelOptHelper.some(DrillSortRel.class, RelOptHelper.some(DrillFilterRel.class,
        RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class)))),
    "DbScanToIndexScanPrule:Sort_Filter_Project_Scan", new MatchSFPS());

  public static final RelOptRule SORT_PROJECT_FILTER_PROJECT_SCAN = new DbScanToIndexScanPrule(
      RelOptHelper.some(DrillSortRel.class, RelOptHelper.some(DrillProjectRel.class, RelOptHelper.some(DrillFilterRel.class,
          RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class))))),
      "DbScanToIndexScanPrule:Sort_Project_Filter_Project_Scan", new MatchSPFPS());

  public static final RelOptRule SORT_PROJECT_FILTER_SCAN = new DbScanToIndexScanPrule(
      RelOptHelper.some(DrillSortRel.class, RelOptHelper.some(DrillProjectRel.class, RelOptHelper.some(DrillFilterRel.class,
          RelOptHelper.any(DrillScanRel.class)))),
      "DbScanToIndexScanPrule:Sort_Project_Filter_Scan", new MatchSPFS());

  public static final RelOptRule FILTER_SCAN = new DbScanToIndexScanPrule(
      RelOptHelper.some(DrillFilterRel.class, RelOptHelper.any(DrillScanRel.class)),
      "DbScanToIndexScanPrule:Filter_On_Scan", new MatchFS());

  public static final RelOptRule FILTER_PROJECT_SCAN = new DbScanToIndexScanPrule(
      RelOptHelper.some(DrillFilterRel.class,
          RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class))),
      "DbScanToIndexScanPrule:Filter_Project_Scan", new MatchFPS());


  private DbScanToIndexScanPrule(RelOptRuleOperand operand, String description, MatchFunction match) {
    super(operand, description);
    this.match = match;
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    if (getMatchIfRoot(call) != null) {
      return true;
    }
    return match.match(call);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    if (getMatchIfRoot(call) != null) {
      getMatchIfRoot(call).onMatch(call);
      return;
    }
    doOnMatch((IndexLogicalPlanCallContext) match.onMatch(call));
  }

  private MatchFunction getMatchIfRoot(RelOptRuleCall call) {
    List<RelNode> rels = call.getRelList();
    if (call.getPlanner().getRoot().equals(call.rel(0))) {
      if (rels.size() == 2) {
        if ((rels.get(0) instanceof DrillFilterRel) && (rels.get(1) instanceof DrillScanRel)) {
          return ((DbScanToIndexScanPrule)FILTER_SCAN).match;
        }
      }
      else if (rels.size() == 3) {
        if ((rels.get(0) instanceof DrillFilterRel) && (rels.get(1) instanceof DrillProjectRel) && (rels.get(2) instanceof DrillScanRel)) {
          return ((DbScanToIndexScanPrule)FILTER_PROJECT_SCAN).match;
        }
      }
    }
    return null;
  }

  private static class MatchFPS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {

    public boolean match(RelOptRuleCall call) {
      final DrillScanRel scan = (DrillScanRel) call.rel(2);
      return checkScan(scan);
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      final DrillFilterRel filter = call.rel(0);
      final DrillProjectRel project = call.rel(1);
      final DrillScanRel scan = call.rel(2);
      return new IndexLogicalPlanCallContext(call, null, filter, project, scan);
    }

  }

  private static class MatchFS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {
    public boolean match(RelOptRuleCall call) {
      final DrillScanRel scan = call.rel(1);
      return checkScan(scan);
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      final DrillFilterRel filter = call.rel(0);
      final DrillScanRel scan = call.rel(1);
      return new IndexLogicalPlanCallContext(call, null, filter, null, scan);
    }
  }

  private static class MatchRelFS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {
    public boolean match(RelOptRuleCall call) {
      if (call.rel(0) instanceof DrillProjectRel ||
          call.rel(0) instanceof DrillSortRel) {
        final DrillScanRel scan = call.rel(2);
        return checkScan(scan);
      }
      return false;
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      DrillProjectRel capProject = null;
      DrillSortRel sort = null;
      if (call.rel(0) instanceof DrillProjectRel) {
        capProject = call.rel(0);
      } else if (call.rel(0) instanceof DrillSortRel) {
        sort = call.rel(0);
      }
      final DrillFilterRel filter = call.rel(1);
      final DrillScanRel scan = call.rel(2);
      return new IndexLogicalPlanCallContext(call, sort, capProject, filter, null, scan);
    }
  }

  private static class MatchPFPS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {
    public boolean match(RelOptRuleCall call) {
      final DrillScanRel scan = call.rel(3);
      return checkScan(scan);
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      final DrillProjectRel capProject = call.rel(0);
      final DrillFilterRel filter = call.rel(1);
      final DrillProjectRel project = call.rel(2);
      final DrillScanRel scan = call.rel(3);
      return new IndexLogicalPlanCallContext(call, null, capProject, filter, project, scan);
    }
  }

  private static class MatchSFPS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {
    public boolean match(RelOptRuleCall call) {
      final DrillScanRel scan = call.rel(3);
      return checkScan(scan);
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      final DrillSortRel sort = call.rel(0);
      final DrillFilterRel filter = call.rel(1);
      final DrillProjectRel project = call.rel(2);
      final DrillScanRel scan = call.rel(3);
      return new IndexLogicalPlanCallContext(call, sort, null, filter, project, scan);
    }
  }

  private static class MatchSPFPS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {
    public boolean match(RelOptRuleCall call) {
      final DrillScanRel scan = call.rel(4);
      return checkScan(scan);
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      final DrillSortRel sort = call.rel(0);
      final DrillProjectRel capProject = call.rel(1);
      final DrillFilterRel filter = call.rel(2);
      final DrillProjectRel project = call.rel(3);
      final DrillScanRel scan = call.rel(4);
      return new IndexLogicalPlanCallContext(call, sort, capProject, filter, project, scan);
    }
  }

  private static class MatchSPFS extends AbstractMatchFunction<IndexLogicalPlanCallContext> {
    public boolean match(RelOptRuleCall call) {
      final DrillScanRel scan = call.rel(3);
      return checkScan(scan);
    }

    public IndexLogicalPlanCallContext onMatch(RelOptRuleCall call) {
      final DrillSortRel sort = call.rel(0);
      final DrillProjectRel capProject = call.rel(1);
      final DrillFilterRel filter = call.rel(2);
      final DrillScanRel scan = call.rel(3);
      return new IndexLogicalPlanCallContext(call, sort, capProject, filter, null, scan);
    }
  }

  protected void doOnMatch(IndexLogicalPlanCallContext indexContext) {

    Stopwatch indexPlanTimer = Stopwatch.createStarted();
    final PlannerSettings settings = PrelUtil.getPlannerSettings(indexContext.call.getPlanner());
    final IndexCollection indexCollection = getIndexCollection(settings, indexContext.scan);
    if( indexCollection == null ) {
      return;
    }

    logger.debug("Index Rule {} starts", this.description);

    RexBuilder builder = indexContext.filter.getCluster().getRexBuilder();

    RexNode condition = null;
    if (indexContext.lowerProject == null) {
      condition = indexContext.filter.getCondition();
    } else {
      // get the filter as if it were below the projection.
      condition = RelOptUtil.pushFilterPastProject(indexContext.filter.getCondition(), indexContext.lowerProject);
    }

    //save this pushed down condition, in case it is needed later to build filter when joining back primary table
    indexContext.origPushedCondition = condition;

    RewriteAsBinaryOperators visitor = new RewriteAsBinaryOperators(true, builder);
    condition = condition.accept(visitor);

    if (indexCollection.supportsIndexSelection()) {
      try {
        processWithIndexSelection(indexContext, settings, condition,
            indexCollection, builder);
      } catch(Exception e) {
        logger.warn("Exception while doing index planning ", e);
      }
    } else {
      throw new UnsupportedOperationException("Index collection must support index selection");
    }
    indexPlanTimer.stop();
    logger.info("index_plan_info: Index Planning took {} ms", indexPlanTimer.elapsed(TimeUnit.MILLISECONDS));
  }
  /**
   * Return the index collection relevant for the underlying data source
   * @param settings
   * @param scan
   */
  public IndexCollection getIndexCollection(PlannerSettings settings, DrillScanRel scan) {
    DbGroupScan groupScan = (DbGroupScan)scan.getGroupScan();
    return groupScan.getSecondaryIndexCollection(scan);
  }

  private void processWithIndexSelection(
      IndexLogicalPlanCallContext indexContext,
      PlannerSettings settings,
      RexNode condition,
      IndexCollection collection,
      RexBuilder builder) {
    double totalRows = 0;
    double filterRows = totalRows;
    DrillScanRel scan = indexContext.scan;
    if (! (indexContext.scan.getGroupScan() instanceof DbGroupScan) ) {
      return;
    }
    IndexConditionInfo.Builder infoBuilder = IndexConditionInfo.newBuilder(condition, collection, builder, indexContext.scan);
    IndexConditionInfo cInfo = infoBuilder.getCollectiveInfo(indexContext);
    boolean isValidIndexHint = infoBuilder.isValidIndexHint(indexContext);

    if (!cInfo.hasIndexCol) {
      logger.info("index_plan_info: No index columns are projected from the scan..continue.");
      return;
    }

    if (cInfo.indexCondition == null) {
      logger.info("index_plan_info: No conditions were found eligible for applying index lookup.");
      return;
    }

    if (!indexContext.indexHint.equals("") && !isValidIndexHint) {
      logger.warn("index_plan_info: Index Hint {} is not useful as index with that name is not available", indexContext.indexHint);
    }

    RexNode indexCondition = cInfo.indexCondition;
    RexNode remainderCondition = cInfo.remainderCondition;

    if (remainderCondition.isAlwaysTrue()) {
      remainderCondition = null;
    }
    logger.debug("index_plan_info: condition split into indexcondition: {} and remaindercondition: {}", indexCondition, remainderCondition);

    IndexableExprMarker indexableExprMarker = new IndexableExprMarker(indexContext.scan);
    indexCondition.accept(indexableExprMarker);
    indexContext.origMarker = indexableExprMarker;

    if (scan.getGroupScan() instanceof DbGroupScan) {
      // Initialize statistics
      DbGroupScan dbScan = ((DbGroupScan) scan.getGroupScan());
      if (settings.isStatisticsEnabled()) {
        dbScan.getStatistics().initialize(condition, scan, indexContext);
      }
      totalRows = dbScan.getRowCount(null, scan);
      filterRows = dbScan.getRowCount(condition, scan);
      double sel = filterRows/totalRows;
      if (totalRows != Statistics.ROWCOUNT_UNKNOWN &&
          filterRows != Statistics.ROWCOUNT_UNKNOWN &&
          !settings.isDisableFullTableScan() && !isValidIndexHint &&
          sel > Math.max(settings.getIndexCoveringSelThreshold(),
              settings.getIndexNonCoveringSelThreshold() )) {
        // If full table scan is not disabled, generate full table scan only plans if selectivity
        // is greater than covering and non-covering selectivity thresholds
        logger.info("index_plan_info: Skip index planning because filter selectivity: {} is greater than thresholds {}, {}",
            sel, settings.getIndexCoveringSelThreshold(), settings.getIndexNonCoveringSelThreshold());
        return;
      }
    }

    if (totalRows == Statistics.ROWCOUNT_UNKNOWN ||
        totalRows == 0 || filterRows == Statistics.ROWCOUNT_UNKNOWN ) {
      logger.warn("index_plan_info: Total row count is UNKNOWN or 0, or filterRows UNKNOWN; skip index planning");
      return;
    }

    List<IndexGroup> coveringIndexes = Lists.newArrayList();
    List<IndexGroup> nonCoveringIndexes = Lists.newArrayList();
    List<IndexGroup> intersectIndexes = Lists.newArrayList();

    //update sort expressions in context, it is needed for computing collation, so do it before IndexSelector
    IndexPlanUtils.updateSortExpression(indexContext, indexContext.sort != null ?
            indexContext.sort.collation.getFieldCollations() : null);

    IndexSelector selector = new IndexSelector(indexCondition,
        remainderCondition,
        indexContext,
        collection,
        builder,
        totalRows);

    for (IndexDescriptor indexDesc : collection) {
      logger.info("index_plan_info indexDescriptor: {}", indexDesc.toString());
      // check if any of the indexed fields of the index are present in the filter condition
      if (IndexPlanUtils.conditionIndexed(indexableExprMarker, indexDesc) != IndexPlanUtils.ConditionIndexed.NONE) {
        if (isValidIndexHint && !indexContext.indexHint.equals(indexDesc.getIndexName())) {
          logger.info("index_plan_info: Index {} is being discarded due to index Hint", indexDesc.getIndexName());
          continue;
        }
        FunctionalIndexInfo functionInfo = indexDesc.getFunctionalInfo();
        selector.addIndex(indexDesc, IndexPlanUtils.isCoveringIndex(indexContext, functionInfo),
            indexContext.lowerProject != null ? indexContext.lowerProject.getRowType().getFieldCount() :
                scan.getRowType().getFieldCount());
      }
    }
    // get the candidate indexes based on selection
    selector.getCandidateIndexes(infoBuilder, coveringIndexes, nonCoveringIndexes, intersectIndexes);

    if (logger.isDebugEnabled()) {
      StringBuilder strb = new StringBuilder();
      if (coveringIndexes.size() > 0) {
        strb.append("Covering indexes:");
        for (IndexGroup index : coveringIndexes) {
          strb.append(index.getIndexProps().get(0).getIndexDesc().getIndexName()).append(", ");
        }
      }
      if(nonCoveringIndexes.size() > 0) {
        strb.append("Non-covering indexes:");
        for (IndexGroup index : nonCoveringIndexes) {
          strb.append(index.getIndexProps().get(0).getIndexDesc().getIndexName()).append(", ");
        }
      }
      logger.debug("index_plan_info: IndexSelector return: {}",strb.toString());
    }

    GroupScan primaryTableScan = indexContext.scan.getGroupScan();
    // Only non-covering indexes can be intersected. Check if
    // (a) there are no covering indexes. Intersect plans will almost always be more
    // expensive than a covering index, so no need to generate one if there is covering.
    // (b) there is more than 1 non-covering indexes that can be intersected
    // TODO: this logic for intersect should eventually be migrated to the IndexSelector
    if (coveringIndexes.size() == 0 && nonCoveringIndexes.size() > 1) {
      List<IndexDescriptor> indexList = Lists.newArrayList();
      for (IndexGroup index : nonCoveringIndexes) {
        IndexDescriptor indexDesc = index.getIndexProps().get(0).getIndexDesc();
        IndexGroupScan idxScan = indexDesc.getIndexGroupScan();
        //Copy primary table statistics to index table
        idxScan.setStatistics(((DbGroupScan) primaryTableScan).getStatistics());
        indexList.add(index.getIndexProps().get(0).getIndexDesc());
      }

      Map<IndexDescriptor, IndexConditionInfo> indexInfoMap = infoBuilder.getIndexConditionMap(indexList);

      //no usable index
      if (indexInfoMap == null || indexInfoMap.size() == 0) {
        logger.info("index_plan_info: skipping intersect plan generation as there is no usable index");
        return;
      }

      //if there is only one index found, no need to do intersect, but just a regular non-covering plan
      //some part of filter condition needs to apply on primary table.
      if(indexInfoMap.size() > 1) {
        logger.info("index_plan_info: intersect plan is generated");

        if (logger.isDebugEnabled()) {
          List<String> indices = new ArrayList<>(nonCoveringIndexes.size());
          for (IndexGroup index : nonCoveringIndexes) {
            indices.add(index.getIndexProps().get(0).getIndexDesc().getIndexName());
          }
          logger.debug("index_plan_info: intersect plan is generated on index list {}", indices);
        }
        boolean intersectPlanGenerated = false;
        //multiple indexes, let us try to intersect results from multiple index tables
        //TODO: make sure the smallest selectivity of these indexes times rowcount smaller than broadcast threshold
        for (IndexGroup index : intersectIndexes) {
          List<IndexDescriptor> candidateDesc = Lists.newArrayList();
          for (IndexProperties candProp : index.getIndexProps()) {
            candidateDesc.add(candProp.getIndexDesc());
          }
          Map<IndexDescriptor, IndexConditionInfo> intersectIdxInfoMap = infoBuilder.getIndexConditionMap(candidateDesc);
          IndexIntersectPlanGenerator planGen = new IndexIntersectPlanGenerator(
              indexContext, intersectIdxInfoMap, builder, settings);
          try {
            planGen.go();
            intersectPlanGenerated = true;
          } catch (Exception e) {
            // If error while generating intersect plans, continue onto generating non-covering plans
            logger.warn("index_plan_info: Exception while trying to generate intersect index plan", e);
          }
        }
        // If intersect plans are forced do not generate further non-covering plans
        if (intersectPlanGenerated && settings.isIndexIntersectPlanPreferred()) {
          return;
        }
      }
    }

    try {
      for (IndexGroup index : coveringIndexes) {
        IndexProperties indexProps = index.getIndexProps().get(0);
        IndexDescriptor indexDesc = indexProps.getIndexDesc();
        IndexGroupScan idxScan = indexDesc.getIndexGroupScan();
        FunctionalIndexInfo indexInfo = indexDesc.getFunctionalInfo();

        indexCondition = indexProps.getLeadingColumnsFilter();
        remainderCondition = indexProps.getTotalRemainderFilter();
        //Copy primary table statistics to index table
        idxScan.setStatistics(((DbGroupScan) scan.getGroupScan()).getStatistics());
        logger.info("index_plan_info: Generating covering index plan for index: {}, query condition {}", indexDesc.getIndexName(), indexCondition.toString());

        CoveringIndexPlanGenerator planGen = new CoveringIndexPlanGenerator(indexContext, indexInfo, idxScan,
            indexCondition, remainderCondition, builder, settings);

        planGen.go();
      }
    } catch (Exception e) {
      logger.warn("Exception while trying to generate covering index plan", e);
    }

    // Create non-covering index plans.

    //First, check if the primary table scan supports creating a restricted scan
    if (primaryTableScan instanceof DbGroupScan &&
        (((DbGroupScan) primaryTableScan).supportsRestrictedScan())) {
      try {
        for (IndexGroup index : nonCoveringIndexes) {
          IndexProperties indexProps = index.getIndexProps().get(0);
          IndexDescriptor indexDesc = indexProps.getIndexDesc();
          IndexGroupScan idxScan = indexDesc.getIndexGroupScan();

          indexCondition = indexProps.getLeadingColumnsFilter();
          remainderCondition = indexProps.getTotalRemainderFilter();
          //Copy primary table statistics to index table
          idxScan.setStatistics(((DbGroupScan) primaryTableScan).getStatistics());
          logger.info("index_plan_info: Generating non-covering index plan for index: {}, query condition {}", indexDesc.getIndexName(), indexCondition.toString());
          NonCoveringIndexPlanGenerator planGen = new NonCoveringIndexPlanGenerator(indexContext, indexDesc,
            idxScan, indexCondition, remainderCondition, builder, settings);
          planGen.go();
        }
      } catch (Exception e) {
        logger.warn("Exception while trying to generate non-covering index access plan", e);
      }
    }
  }

}
