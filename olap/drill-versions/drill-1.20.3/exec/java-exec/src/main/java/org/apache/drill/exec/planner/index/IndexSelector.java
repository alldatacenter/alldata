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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.common.DrillScanRelBase;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class IndexSelector  {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IndexSelector.class);
  private static final double COVERING_TO_NONCOVERING_FACTOR = 100.0;
  private RexNode indexCondition;   // filter condition on indexed columns
  private RexNode otherRemainderCondition;  // remainder condition on all other columns
  private double totalRows;
  private Statistics stats;         // a Statistics instance that will be used to get estimated rowcount for filter conditions
  private IndexConditionInfo.Builder builder;
  private List<IndexProperties> indexPropList;
  private DrillScanRelBase primaryTableScan;
  private IndexCallContext indexContext;
  private RexBuilder rexBuilder;

  public IndexSelector(RexNode indexCondition,
      RexNode otherRemainderCondition,
      IndexCallContext indexContext,
      IndexCollection collection,
      RexBuilder rexBuilder,
      double totalRows) {
    this.indexCondition = indexCondition;
    this.otherRemainderCondition = otherRemainderCondition;
    this.indexContext = indexContext;
    this.totalRows = totalRows;
    this.stats = indexContext.getGroupScan().getStatistics();
    this.rexBuilder = rexBuilder;
    this.builder =
        IndexConditionInfo.newBuilder(indexCondition, collection, rexBuilder, indexContext.getScan());
    this.primaryTableScan = indexContext.getScan();
    this.indexPropList = Lists.newArrayList();
  }

  /**
   * This constructor is to build selector for no index condition case (no filter)
   * @param indexContext
   */
  public IndexSelector(IndexCallContext indexContext) {
    this.indexCondition = null;
    this.otherRemainderCondition = null;
    this.indexContext = indexContext;
    this.totalRows = Statistics.ROWCOUNT_UNKNOWN;
    this.stats = indexContext.getGroupScan().getStatistics();
    this.rexBuilder = indexContext.getScan().getCluster().getRexBuilder();
    this.builder = null;
    this.primaryTableScan = indexContext.getScan();
    this.indexPropList = Lists.newArrayList();
  }

  public void addIndex(IndexDescriptor indexDesc, boolean isCovering, int numProjectedFields) {
    IndexProperties indexProps = new DrillIndexProperties(indexDesc, isCovering, otherRemainderCondition, rexBuilder,
        numProjectedFields, totalRows, primaryTableScan);
    indexPropList.add(indexProps);
  }

  /**
   * This method analyzes an index's columns and starting from the first column, checks
   * which part of the filter condition matches that column.  This process continues with
   * subsequent columns.  The goal is to identify the portion of the filter condition that
   * match the prefix columns.  If there are additional conditions that don't match prefix
   * columns, that condition is set as a remainder condition.
   * @param indexProps
   */
  public void analyzePrefixMatches(IndexProperties indexProps) {
    RexNode initCondition = indexCondition.isAlwaysTrue() ? null : indexCondition;
    Map<LogicalExpression, RexNode> leadingPrefixMap = Maps.newHashMap();
    List<LogicalExpression> indexCols = indexProps.getIndexDesc().getIndexColumns();
    boolean satisfiesCollation = false;

    if (indexCols.size() > 0) {
      if (initCondition != null) { // check filter condition
        initCondition = IndexPlanUtils.getLeadingPrefixMap(leadingPrefixMap, indexCols, builder, indexCondition);
      }
      if (requiredCollation()) {
        satisfiesCollation = buildAndCheckCollation(indexProps);
      }
    }

    indexProps.setProperties(leadingPrefixMap, satisfiesCollation,
        initCondition /* the remainder condition for indexed columns */, stats);
  }

  private boolean requiredCollation() {
    if (indexContext.getCollationList() != null && indexContext.getCollationList().size() > 0) {
      return true;
    }
    return false;
  }

  private boolean buildAndCheckCollation(IndexProperties indexProps) {
    IndexDescriptor indexDesc = indexProps.getIndexDesc();
    FunctionalIndexInfo functionInfo = indexDesc.getFunctionalInfo();

    RelCollation inputCollation;
    // for the purpose of collation we can assume that a covering index scan would provide
    // the collation property that would be relevant for non-covering as well
    ScanPrel indexScanPrel =
        IndexPlanUtils.buildCoveringIndexScan(indexContext.getScan(), indexDesc.getIndexGroupScan(), indexContext, indexDesc);
    inputCollation = indexScanPrel.getTraitSet().getTrait(RelCollationTraitDef.INSTANCE);

    // we don't create collation for Filter because it will inherit the child's collation

    if (indexContext.hasLowerProject()) {
      inputCollation =
          IndexPlanUtils.buildCollationProject(indexContext.getLowerProject().getProjects(), null,
              indexContext.getScan(), functionInfo,indexContext);
    }

    if (indexContext.hasUpperProject()) {
      inputCollation =
          IndexPlanUtils.buildCollationProject(indexContext.getUpperProject().getProjects(), indexContext.getLowerProject(),
              indexContext.getScan(), functionInfo, indexContext);
    }

    if ((inputCollation != null) &&
         (inputCollation.satisfies(indexContext.getCollation()))) {
      return true;
    }

    return false;
  }

  private void addIndexIntersections(List<IndexGroup> candidateIndexes,
      IndexConditionInfo.Builder infoBuilder, long maxIndexesToIntersect) {
    // Sort the non-covering indexes for candidate intersect indexes. Generating all
    // possible combinations of candidates is not feasible so we guide our decision
    // based on selectivity/collation considerations
    IndexGroup indexesInCandidate = new IndexGroup();
    final double SELECTIVITY_UNKNOWN = -1.0;
    // We iterate over indexes upto planner.index.max_indexes_to_intersect. An index which allows
    // filter pushdown is added to the existing list of indexes provided it reduces the selectivity
    // by COVERING_TO_NONCOVERING_FACTOR
    double prevSel = SELECTIVITY_UNKNOWN;
    for (int idx = 0; idx < candidateIndexes.size(); idx++) {
      // Maximum allowed indexes is intersect plan reached!
      if (indexesInCandidate.numIndexes() == maxIndexesToIntersect) {
        break;
      }
      // Skip covering indexes
      if (candidateIndexes.get(idx).getIndexProps().get(0).isCovering()) {
        continue;
      }
      // Check if adding the current index to the already existing intersect indexes is redundant
      List<IndexDescriptor> candidateDescs = Lists.newArrayList();
      for (IndexProperties prop : indexesInCandidate.getIndexProps()) {
        candidateDescs.add(prop.getIndexDesc());
      }
      candidateDescs.add(candidateIndexes.get(idx).getIndexProps().get(0).getIndexDesc());
      Map<IndexDescriptor, IndexConditionInfo> intersectIdxInfoMap
          = infoBuilder.getIndexConditionMap(candidateDescs);
      // Current index redundant(no conditions pushed down) - skip!
      if (intersectIdxInfoMap.keySet().size() < candidateDescs.size()) {
        continue;
      }
      IndexProperties curProp = candidateIndexes.get(idx).getIndexProps().get(0);
      indexesInCandidate.addIndexProp(curProp);
      double currSel = 1.0;
      if (indexesInCandidate.isIntersectIndex()) {
        for (IndexProperties prop : indexesInCandidate.getIndexProps()) {
          currSel *= prop.getLeadingSelectivity();
        }
        if (prevSel == SELECTIVITY_UNKNOWN ||
            currSel/prevSel < COVERING_TO_NONCOVERING_FACTOR) {
          prevSel = currSel;
        } else {
          indexesInCandidate.removeIndexProp(curProp);
        }
      }
    }
    // If intersect plan was generated, add it to the set of candidate indexes.
    if (indexesInCandidate.isIntersectIndex()) {
      candidateIndexes.add(indexesInCandidate);
    }
  }

  /**
   * Run the index selection algorithm and return the top N indexes
   */
  public void getCandidateIndexes(IndexConditionInfo.Builder infoBuilder, List<IndexGroup> coveringIndexes,
      List<IndexGroup> nonCoveringIndexes, List<IndexGroup> intersectIndexes) {

    RelOptPlanner planner = indexContext.getCall().getPlanner();
    PlannerSettings settings = PrelUtil.getPlannerSettings(planner);
    List<IndexGroup> candidateIndexes = Lists.newArrayList();

    logger.info("index_plan_info: Analyzing {} indexes for prefix matches: {}",
        indexPropList.size(), indexPropList);
    // analysis phase
    for (IndexProperties p : indexPropList) {
      analyzePrefixMatches(p);

      // only consider indexes that either have some leading prefix of the filter condition or
      // can satisfy required collation
      if (p.numLeadingFilters() > 0 || p.satisfiesCollation()) {
        double selThreshold = p.isCovering() ? settings.getIndexCoveringSelThreshold() :
          settings.getIndexNonCoveringSelThreshold();
        // only consider indexes whose selectivity is <= the configured threshold OR consider
        // all when full table scan is disable to avoid a CannotPlanException
        if (settings.isDisableFullTableScan() || p.getLeadingSelectivity() <= selThreshold) {
          IndexGroup index = new IndexGroup();
          index.addIndexProp(p);
          candidateIndexes.add(index);
        }
        else {
          if (p.getLeadingSelectivity() > selThreshold) {
            logger.debug("Skipping index {}. The leading selectivity {} is larger than threshold {}",
                p.getIndexDesc().getIndexName(), p.getLeadingSelectivity(), selThreshold);
          }
        }
      }
    }

    if (candidateIndexes.size() == 0) {
      logger.info("index_plan_info: No suitable indexes found !");
      return;
    }

    int max_candidate_indexes = (int)PrelUtil.getPlannerSettings(planner).getIndexMaxChosenIndexesPerTable();

    // Ranking phase. Technically, we don't need to rank if there are fewer than max_candidate_indexes
    // but we do it anyways for couple of reasons: the log output will show the indexes in a properly ranked
    // order which helps diagnosing problems and secondly for internal unit/functional testing we want this code
    // to be exercised even for few indexes
    if (candidateIndexes.size() > 1) {
      Collections.sort(candidateIndexes, new IndexComparator(planner, builder));
    }

    // Generate index intersections for ranking
    addIndexIntersections(candidateIndexes, infoBuilder, settings.getMaxIndexesToIntersect());

    // Sort again after intersect plan is added to the list
    if (candidateIndexes.size() > 1) {
      Collections.sort(candidateIndexes, new IndexComparator(planner, builder));
    }

    logger.info("index_plan_info: The top ranked indexes are: ");

    int count = 0;
    boolean foundCovering = false;
    boolean foundCoveringCollation = false;
    boolean foundNonCoveringCollation = false;

    // pick the best N indexes
    for (int i=0; i < candidateIndexes.size(); i++) {
      IndexGroup index = candidateIndexes.get(i);
      if (index.numIndexes() == 1
          && index.getIndexProps().get(0).isCovering()) {
        IndexProperties indexProps = index.getIndexProps().get(0);
        if (foundCoveringCollation) {
          // if previously we already found a higher ranked covering index that satisfies collation,
          // then skip this one (note that selectivity and cost considerations were already handled
          // by the ranking phase)
          logger.debug("index_plan_info: Skipping covering index {} because a higher ranked covering index with collation already exists.", indexProps.getIndexDesc().getIndexName());
          continue;
        }
        coveringIndexes.add(index);
        logger.info("index_plan_info: name: {}, covering, collation: {}, leadingSelectivity: {}, cost: {}",
            indexProps.getIndexDesc().getIndexName(),
            indexProps.satisfiesCollation(),
            indexProps.getLeadingSelectivity(),
            indexProps.getSelfCost(planner));
        count++;
        foundCovering = true;
        if (indexProps.satisfiesCollation()) {
          foundCoveringCollation = true;
        }
      } else if (index.numIndexes() == 1) {  // non-covering
        IndexProperties indexProps = index.getIndexProps().get(0);
        // skip this non-covering index if (a) there was a higher ranked covering index
        // with collation or (b) there was a higher ranked covering index and this
        // non-covering index does not have collation
        if (foundCoveringCollation ||
            (foundCovering && !indexProps.satisfiesCollation())) {
          logger.debug("index_plan_info: Skipping non-covering index {} because it does not have collation and a higher ranked covering index already exists.",
              indexProps.getIndexDesc().getIndexName());
          continue;
        }
        if (indexProps.satisfiesCollation()) {
          foundNonCoveringCollation = true;
        }
        // all other non-covering indexes can be added to the list because 2 or more non-covering index could
        // be considered for intersection later; currently the index selector is not costing the index intersection
        nonCoveringIndexes.add(index);
        logger.info("index_plan_info: name: {}, non-covering, collation: {}, leadingSelectivity: {}, cost: {}",
            indexProps.getIndexDesc().getIndexName(),
            indexProps.satisfiesCollation(),
            indexProps.getLeadingSelectivity(),
            indexProps.getSelfCost(planner));
        count++;
      } else {  // intersect indexes
        if (foundCoveringCollation ||
            (foundCovering && !index.getIndexProps().get(index.numIndexes()-1).satisfiesCollation()) ||
            foundNonCoveringCollation) {
          continue;
        }
        IndexGroup intersectIndex = new IndexGroup();
        double isectLeadingSel = 1.0;
        String isectName = "Intersect-"+count;
        for (IndexProperties indexProps : index.getIndexProps()) {
          intersectIndex.addIndexProp(indexProps);
          isectLeadingSel *= indexProps.getLeadingSelectivity();
          logger.info("name: {}, {}, collation: {}, leadingSelectivity: {}, cost: {}",
              indexProps.getIndexDesc().getIndexName(),
              isectName,
              indexProps.satisfiesCollation(),
              indexProps.getLeadingSelectivity(),
              indexProps.getSelfCost(planner));
        }
        logger.info("name: {}, intersect-idx, collation: {}, leadingSelectivity: {}, cost: {}",
            isectName,
            index.getIndexProps().get(index.numIndexes()-1).satisfiesCollation(),
            isectLeadingSel,
            index.getIndexProps().get(0).getIntersectCost(index, builder, planner));
        intersectIndexes.add(intersectIndex);
      }
      if (count == max_candidate_indexes) {
        break;
      }
    }
  }

  /**
   * we assume all the indexes added in indexPropList are all applicable (and covering).
   * For now this function is used and tested only in IndexScanWithSortOnlyPrule
   */
  public IndexProperties getBestIndexNoFilter() {
    if (indexPropList.size() == 0) {
      return null;
    }

    RelOptPlanner planner = indexContext.getCall().getPlanner();
    List<IndexGroup> candidateIndexes = Lists.newArrayList();

    for (IndexProperties p : indexPropList) {
      p.setSatisfiesCollation(buildAndCheckCollation(p));
        IndexGroup index = new IndexGroup();
        index.addIndexProp(p);
        candidateIndexes.add(index);
    }
    Collections.sort(candidateIndexes, new IndexComparator(planner, builder));
    return candidateIndexes.get(0).getIndexProps().get(0);
  }

  public static class IndexComparator implements Comparator<IndexGroup> {

    private RelOptPlanner planner;
    private IndexConditionInfo.Builder builder;
    private PlannerSettings settings;

    public IndexComparator(RelOptPlanner planner, IndexConditionInfo.Builder builder) {
      this.planner = planner;
      this.builder = builder;
      this.settings = PrelUtil.getPlannerSettings(planner);
    }

    @Override
    public int compare(IndexGroup index1, IndexGroup index2) {
      // given a covering and a non-covering index, prefer covering index unless the
      // difference in their selectivity is bigger than a configurable factor
      List<IndexProperties> o1, o2;
      boolean o1Covering, o2Covering, o1SatisfiesCollation, o2SatisfiesCollation;
      int o1NumLeadingFilters, o2NumLeadingFilters;
      double o1LeadingSelectivity, o2LeadingSelectivity;
      DrillCostBase o1SelfCost, o2SelfCost;

      o1 = index1.getIndexProps();
      o2 = index2.getIndexProps();
      Preconditions.checkArgument(o1.size() > 0 && o2.size() > 0);

      if (o1.size() == 1) {
        o1Covering = o1.get(0).isCovering();
        o1NumLeadingFilters = o1.get(0).numLeadingFilters();
        o1SatisfiesCollation = o1.get(0).satisfiesCollation();
        o1LeadingSelectivity = o1.get(0).getLeadingSelectivity();
      } else {
        o1Covering = false;
        // If the intersect plan is a left-deep join tree, the last index
        // in the join would satisfy the collation. For now, assume no collation.
        o1SatisfiesCollation = false;
        o1NumLeadingFilters = o1.get(0).numLeadingFilters();
        for (int idx=1; idx<o1.size(); idx++) {
          o1NumLeadingFilters+=o1.get(idx).numLeadingFilters();
        }
        o1LeadingSelectivity = o1.get(0).getLeadingSelectivity();
        for (int idx=1; idx<o1.size(); idx++) {
          o1LeadingSelectivity*=o1.get(idx).getLeadingSelectivity();
        }
      }
      if (o2.size() == 1) {
        o2Covering = o2.get(0).isCovering();
        o2NumLeadingFilters = o2.get(0).numLeadingFilters();
        o2SatisfiesCollation = o2.get(0).satisfiesCollation();
        o2LeadingSelectivity = o2.get(0).getLeadingSelectivity();
      } else {
        o2Covering = false;
        // If the intersect plan is a left-deep join tree, the last index
        // in the join would satisfy the collation. For now, assume no collation.
        o2SatisfiesCollation = false;
        o2NumLeadingFilters = o2.get(0).numLeadingFilters();
        for (int idx=1; idx<o2.size(); idx++) {
          o2NumLeadingFilters+=o2.get(idx).numLeadingFilters();
        }
        o2LeadingSelectivity = o2.get(0).getLeadingSelectivity();
        for (int idx=1; idx<o2.size(); idx++) {
          o2LeadingSelectivity*=o2.get(idx).getLeadingSelectivity();
        }
      }

      if (o1Covering && !o2Covering) {
        if (o1LeadingSelectivity/o2LeadingSelectivity < COVERING_TO_NONCOVERING_FACTOR) {
          return -1;  // covering is ranked higher (better) than non-covering
        }
      }

      if (o2Covering && !o1Covering) {
        if (o2LeadingSelectivity/o1LeadingSelectivity < COVERING_TO_NONCOVERING_FACTOR) {
          return 1;  // covering is ranked higher (better) than non-covering
        }
      }

      if (!o1Covering && !o2Covering) {
        if (o1.size() > 1) {
          if (o1LeadingSelectivity/o2LeadingSelectivity < COVERING_TO_NONCOVERING_FACTOR) {
            return -1; // Intersect is ranked higher than non-covering/intersect
          }
        } else if (o2.size() > 1) {
          if (o2LeadingSelectivity/o1LeadingSelectivity < COVERING_TO_NONCOVERING_FACTOR) {
            return -1; // Intersect is ranked higher than non-covering/intersect
          }
        }
      }

      if (o1SatisfiesCollation && !o2SatisfiesCollation) {
        return -1;  // index with collation is ranked higher (better) than one without collation
      } else if (o2SatisfiesCollation && !o1SatisfiesCollation) {
        return 1;
      }

      if (o1.size() == 1) {
        o1SelfCost = (DrillCostBase) o1.get(0).getSelfCost(planner);
      } else {
        o1SelfCost = (DrillCostBase) o1.get(0).getIntersectCost(index1, builder, planner);
      }
      if (o2.size() == 1) {
        o2SelfCost = (DrillCostBase) o2.get(0).getSelfCost(planner);
      } else {
        o2SelfCost = (DrillCostBase) o2.get(0).getIntersectCost(index2, builder, planner);
      }

      DrillCostBase cost1 = o1SelfCost;
      DrillCostBase cost2 = o2SelfCost;

      if (cost1.isLt(cost2)) {
        return -1;
      } else if (cost1.isEqWithEpsilon(cost2)) {
        if (o1NumLeadingFilters > o2NumLeadingFilters) {
          return -1;
        } else if (o1NumLeadingFilters < o2NumLeadingFilters) {
          return 1;
        }
        return 0;
      } else {
        return 1;
      }
    }
  }

  /**
   * IndexProperties encapsulates the various metrics of a single index that are related to
   * the current query. These metrics are subsequently used to rank the index in comparison
   * with other indexes.
   */
  public static class DrillIndexProperties  implements IndexProperties {
    private IndexDescriptor indexDescriptor; // index descriptor

    private double leadingSel = 1.0;    // selectivity of leading satisfiable conjunct
    private double remainderSel = 1.0;  // selectivity of all remainder satisfiable conjuncts
    private boolean satisfiesCollation = false; // whether index satisfies collation
    private boolean isCovering = false;         // whether index is covering
    private double avgRowSize;          // avg row size in bytes of the selected part of index

    private int numProjectedFields;
    private double totalRows;
    private DrillScanRelBase primaryTableScan = null;
    private RelOptCost selfCost = null;

    private List<RexNode> leadingFilters = Lists.newArrayList();
    private Map<LogicalExpression, RexNode> leadingPrefixMap;
    private RexNode indexColumnsRemainderFilter = null;
    private RexNode otherColumnsRemainderFilter = null;
    private RexBuilder rexBuilder;

    public DrillIndexProperties(IndexDescriptor indexDescriptor,
        boolean isCovering,
        RexNode otherColumnsRemainderFilter,
        RexBuilder rexBuilder,
        int numProjectedFields,
        double totalRows,
        DrillScanRelBase primaryTableScan) {
      this.indexDescriptor = indexDescriptor;
      this.isCovering = isCovering;
      this.otherColumnsRemainderFilter = otherColumnsRemainderFilter;
      this.rexBuilder = rexBuilder;
      this.numProjectedFields = numProjectedFields;
      this.totalRows = totalRows;
      this.primaryTableScan = primaryTableScan;
    }

    public void setProperties(Map<LogicalExpression, RexNode> prefixMap,
        boolean satisfiesCollation,
        RexNode indexColumnsRemainderFilter,
        Statistics stats) {
      this.indexColumnsRemainderFilter = indexColumnsRemainderFilter;
      this.satisfiesCollation = satisfiesCollation;
      leadingPrefixMap = prefixMap;

      logger.info("index_plan_info: Index {}: leading prefix map: {}, satisfies collation: {}, remainder condition: {}",
          indexDescriptor.getIndexName(), leadingPrefixMap, satisfiesCollation, indexColumnsRemainderFilter);

      // iterate over the columns in the index descriptor and lookup from the leadingPrefixMap
      // the corresponding conditions
      leadingFilters = IndexPlanUtils.getLeadingFilters(leadingPrefixMap, indexDescriptor.getIndexColumns());

      // compute the estimated row count by calling the statistics APIs
      // NOTE: the calls to stats.getRowCount() below supply the primary table scan
      // which is ok because its main use is to convert the ordinal-based filter
      // to a string representation for stats lookup.
      String idxIdentifier = stats.buildUniqueIndexIdentifier(this.getIndexDesc());
      for (RexNode filter : leadingFilters) {
        double filterRows = stats.getRowCount(filter, idxIdentifier, primaryTableScan /* see comment above */);
        double sel = 1.0;
        if (filterRows != Statistics.ROWCOUNT_UNKNOWN) {
          sel = filterRows/totalRows;
          logger.info("index_plan_info: Filter: {}, filterRows = {}, totalRows = {}, selectivity = {}",
              filter, filterRows, totalRows, sel);
        } else {
          sel = RelMdUtil.guessSelectivity(filter);
          if (stats.isStatsAvailable()) {
            logger.debug("index_plan_info: Filter row count is UNKNOWN for filter: {}, using guess {}", filter, sel);
          }
        }
        leadingSel *= sel;
      }

      logger.debug("index_plan_info: Combined selectivity of all leading filters: {}", leadingSel);

      if (indexColumnsRemainderFilter != null) {
        // The remainder filter is evaluated against the primary table i.e. NULL index
        double remFilterRows = stats.getRowCount(indexColumnsRemainderFilter, null, primaryTableScan);
        if (remFilterRows != Statistics.ROWCOUNT_UNKNOWN) {
          remainderSel = remFilterRows/totalRows;
          logger.debug("index_plan_info: Selectivity of index columns remainder filters: {}", remainderSel);
        } else {
          remainderSel = RelMdUtil.guessSelectivity(indexColumnsRemainderFilter);
          if (stats.isStatsAvailable()) {
            logger.debug("index_plan_info: Filter row count is UNKNOWN for remainder filter : {}, using guess {}",
                indexColumnsRemainderFilter, remainderSel);
          }
        }
      }

      // get the average row size based on the leading column filter
      avgRowSize = stats.getAvgRowSize(idxIdentifier, false);
      if (avgRowSize == Statistics.AVG_ROWSIZE_UNKNOWN) {
        avgRowSize = numProjectedFields * Statistics.AVG_COLUMN_SIZE;
        if (stats.isStatsAvailable()) {
          logger.debug("index_plan_info: Average row size is UNKNOWN based on leading filter: {}, using guess {}, columns {}, columnSize {}",
              leadingFilters.size() > 0 ? leadingFilters.get(0).toString() : "<NULL>",
              avgRowSize, numProjectedFields, Statistics.AVG_COLUMN_SIZE);
        }
      } else {
        logger.debug("index_plan_info: Filter: {}, Average row size: {}",
            leadingFilters.size() > 0 ? leadingFilters.get(0).toString() : "<NULL>", avgRowSize);
      }
    }

    public double getLeadingSelectivity() {
      return leadingSel;
    }

    public double getRemainderSelectivity() {
      return remainderSel;
    }

    public boolean isCovering() {
      return isCovering;
    }

    public double getTotalRows() {
      return totalRows;
    }

    public IndexDescriptor getIndexDesc() {
      return indexDescriptor;
    }

    public RexNode getLeadingColumnsFilter() {
      return IndexPlanUtils.getLeadingColumnsFilter(leadingFilters, rexBuilder);
    }

    public RexNode getTotalRemainderFilter() {
      return IndexPlanUtils.getTotalRemainderFilter(indexColumnsRemainderFilter, otherColumnsRemainderFilter, rexBuilder);
    }

    public boolean satisfiesCollation() {
      return satisfiesCollation;
    }

    public void setSatisfiesCollation(boolean satisfiesCollation) {
      this.satisfiesCollation = satisfiesCollation;
    }

    public RelOptCost getSelfCost(RelOptPlanner planner) {
      if (selfCost != null) {
        return selfCost;
      }
      selfCost = indexDescriptor.getCost(this, planner, numProjectedFields, IndexPlanUtils.getGroupScan(primaryTableScan));
      return selfCost;
    }

    public RelOptCost getIntersectCost(IndexGroup index, IndexConditionInfo.Builder builder,
        RelOptPlanner planner) {
      return getIntersectCost(index, builder, planner, indexDescriptor.getPluginCostModel(), primaryTableScan);
    }

    public int numLeadingFilters() {
      return leadingFilters.size();
    }

    public double getAvgRowSize() {
      return avgRowSize;
    }

    public DrillScanRelBase getPrimaryTableScan() {
      return this.primaryTableScan;
    }

    public RelOptCost getIntersectCost(IndexGroup index, IndexConditionInfo.Builder builder,
                                       RelOptPlanner planner, PluginCost costBase, DrillScanRelBase scanRel) {
      DrillCostBase.DrillCostFactory costFactory = (DrillCostBase.DrillCostFactory)planner.getCostFactory();
      double totLeadRowCount = 1.0;
      double totalRows = 0.0, totCpuCost = 0.0, totDiskCost = 0.0, totNetworkCost = 0.0, totMemoryCost = 0.0;
      double rightSideRows = Statistics.ROWCOUNT_UNKNOWN;
      DbGroupScan primaryTableGroupScan = (DbGroupScan) IndexPlanUtils.getGroupScan(scanRel);
      RexNode remFilters;
      final List<RexNode> remFilterList = Lists.newArrayList();
      for (IndexProperties indexProps : index.getIndexProps()) {
        remFilterList.add(indexProps.getTotalRemainderFilter());
      }
      remFilters = RexUtil.composeConjunction(scanRel.getCluster().getRexBuilder(), remFilterList, false);

      for (IndexProperties indexProps : index.getIndexProps()) {
        totalRows = indexProps.getTotalRows();
        double leadRowCount = indexProps.getLeadingSelectivity() * indexProps.getRemainderSelectivity() * totalRows;
        totLeadRowCount *= indexProps.getLeadingSelectivity();
        double avgRowSize = indexProps.getAvgRowSize();
        Preconditions.checkArgument(primaryTableGroupScan instanceof DbGroupScan);
        // IO Cost (Filter evaluation CPU cost for pushed down filters)
        double numBlocksIndex = Math.ceil((leadRowCount * avgRowSize) / costBase.getBlockSize(primaryTableGroupScan));
        double diskCostIndex = numBlocksIndex * costBase.getSequentialBlockReadCost(primaryTableGroupScan);
        totDiskCost += diskCostIndex;
        // Join Cost (include network cost?) borrow from Hash Join
        if (rightSideRows != Statistics.ROWCOUNT_UNKNOWN) {
          DrillCostBase joinCost = (DrillCostBase) DrillJoinRelBase.computeHashJoinCostWithRowCntKeySize(planner,
              rightSideRows, leadRowCount, 1);
          totDiskCost += joinCost.getIo();
          totCpuCost += joinCost.getCpu();
          totMemoryCost += joinCost.getMemory();
          // No NDV statistics; compute join rowcount as max of build side and probe side rows
          rightSideRows = PrelUtil.getPlannerSettings(planner).getRowCountEstimateFactor() *
              Math.max(leadRowCount, rightSideRows);
        } else {
          rightSideRows = leadRowCount;
        }
        remFilters = remainderCondition(indexProps.getIndexDesc(), builder, remFilters);
      }

      totLeadRowCount *= totalRows;
      // for the primary table join-back each row may belong to a different block, so in general num_blocks = num_rows;
      // however, num_blocks cannot exceed the total number of blocks of the table
      DbGroupScan dbGroupScan = (DbGroupScan) primaryTableGroupScan;
      double totalBlocksPrimary = Math.ceil((dbGroupScan.getColumns().size() *
          costBase.getAverageColumnSize(dbGroupScan) * totalRows) / costBase.getBlockSize(dbGroupScan));
      double diskBlocksPrimary = Math.min(totalBlocksPrimary, totLeadRowCount);
      double diskCostPrimary = diskBlocksPrimary * costBase.getRandomBlockReadCost(dbGroupScan);
      totDiskCost += diskCostPrimary;
      // CPU cost of remainder condition evaluation over the selected rows
      if (remFilters != null) {
        totCpuCost += totLeadRowCount * DrillCostBase.COMPARE_CPU_COST;
      }
      double networkCost = 0.0; // TODO: add network cost once full table scan also considers network cost
      return costFactory.makeCost(totLeadRowCount, totCpuCost, totDiskCost, totNetworkCost, totMemoryCost);
    }

    public RexNode remainderCondition(IndexDescriptor indexDesc, IndexConditionInfo.Builder builder,
                                      RexNode initCondition) {
      List<LogicalExpression> indexCols = indexDesc.getIndexColumns();
      boolean prefix = true;
      if (indexCols.size() > 0 && initCondition != null) {
        int i=0;
        while (prefix && i < indexCols.size()) {
          LogicalExpression p = indexCols.get(i++);
          List<LogicalExpression> prefixCol = ImmutableList.of(p);
          IndexConditionInfo info = builder.indexConditionRelatedToFields(prefixCol, initCondition);
          if(info != null && info.hasIndexCol) {
            initCondition = info.remainderCondition;
            if (initCondition.isAlwaysTrue()) {
              // all filter conditions are accounted for, so if the remainder is TRUE, set it to NULL because
              // we don't need to keep track of it for rest of the index selection
              initCondition = null;
              break;
            }
          } else {
            prefix = false;
          }
        }
      }
      return initCondition;
    }
  }
}
