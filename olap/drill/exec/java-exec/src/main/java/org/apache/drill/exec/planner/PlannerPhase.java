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
package org.apache.drill.exec.planner;

import org.apache.drill.exec.planner.logical.ConvertMetadataAggregateToDirectScanRule;
import org.apache.drill.exec.planner.physical.MetadataAggPrule;
import org.apache.drill.exec.planner.physical.MetadataControllerPrule;
import org.apache.drill.exec.planner.physical.MetadataHandlerPrule;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet.Builder;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.rules.JoinToMultiJoinRule;
import org.apache.calcite.rel.rules.LoptOptimizeJoinRule;
import org.apache.calcite.tools.RuleSet;
import org.apache.calcite.tools.RuleSets;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.planner.index.rules.DbScanSortRemovalRule;
import org.apache.drill.exec.planner.index.rules.DbScanToIndexScanPrule;
import org.apache.drill.exec.planner.logical.DrillAggregateRule;
import org.apache.drill.exec.planner.logical.DrillCorrelateRule;
import org.apache.drill.exec.planner.logical.DrillFilterAggregateTransposeRule;
import org.apache.drill.exec.planner.logical.DrillFilterItemStarReWriterRule;
import org.apache.drill.exec.planner.logical.DrillFilterJoinRules;
import org.apache.drill.exec.planner.logical.DrillFilterRule;
import org.apache.drill.exec.planner.logical.DrillJoinRel;
import org.apache.drill.exec.planner.logical.DrillJoinRule;
import org.apache.drill.exec.planner.logical.DrillLimitRule;
import org.apache.drill.exec.planner.logical.DrillMergeProjectRule;
import org.apache.drill.exec.planner.logical.DrillProjectLateralJoinTransposeRule;
import org.apache.drill.exec.planner.logical.DrillProjectPushIntoLateralJoinRule;
import org.apache.drill.exec.planner.logical.DrillProjectRule;
import org.apache.drill.exec.planner.logical.DrillPushFilterPastProjectRule;
import org.apache.drill.exec.planner.logical.DrillPushLimitToScanRule;
import org.apache.drill.exec.planner.logical.DrillPushProjectIntoScanRule;
import org.apache.drill.exec.planner.logical.DrillPushProjectPastFilterRule;
import org.apache.drill.exec.planner.logical.DrillPushProjectPastJoinRule;
import org.apache.drill.exec.planner.logical.DrillPushRowKeyJoinToScanRule;
import org.apache.drill.exec.planner.logical.DrillReduceAggregatesRule;
import org.apache.drill.exec.planner.logical.DrillReduceExpressionsRule;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.planner.logical.DrillScanRule;
import org.apache.drill.exec.planner.logical.DrillSortRule;
import org.apache.drill.exec.planner.logical.DrillUnionAllRule;
import org.apache.drill.exec.planner.logical.DrillUnnestRule;
import org.apache.drill.exec.planner.logical.DrillValuesRule;
import org.apache.drill.exec.planner.logical.DrillWindowRule;
import org.apache.drill.exec.planner.logical.partition.ParquetPruneScanRule;
import org.apache.drill.exec.planner.logical.partition.PruneScanRule;
import org.apache.drill.exec.planner.logical.ConvertCountToDirectScanRule;
import org.apache.drill.exec.planner.physical.AnalyzePrule;
import org.apache.drill.exec.planner.physical.ConvertCountToDirectScanPrule;
import org.apache.drill.exec.planner.physical.LateralJoinPrule;
import org.apache.drill.exec.planner.physical.DirectScanPrule;
import org.apache.drill.exec.planner.physical.FilterPrule;
import org.apache.drill.exec.planner.physical.HashAggPrule;
import org.apache.drill.exec.planner.physical.HashJoinPrule;
import org.apache.drill.exec.planner.physical.LimitPrule;
import org.apache.drill.exec.planner.physical.LimitExchangeTransposeRule;
import org.apache.drill.exec.planner.physical.MergeJoinPrule;
import org.apache.drill.exec.planner.physical.NestedLoopJoinPrule;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.ProjectPrule;
import org.apache.drill.exec.planner.physical.PushLimitToTopN;
import org.apache.drill.exec.planner.physical.RowKeyJoinPrule;
import org.apache.drill.exec.planner.physical.ScanPrule;
import org.apache.drill.exec.planner.physical.ScreenPrule;
import org.apache.drill.exec.planner.physical.SortConvertPrule;
import org.apache.drill.exec.planner.physical.SortPrule;
import org.apache.drill.exec.planner.physical.StreamAggPrule;
import org.apache.drill.exec.planner.physical.UnionAllPrule;
import org.apache.drill.exec.planner.physical.UnnestPrule;
import org.apache.drill.exec.planner.physical.ValuesPrule;
import org.apache.drill.exec.planner.physical.WindowPrule;
import org.apache.drill.exec.planner.physical.WriterPrule;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.parquet.FilePushDownFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Returns RuleSet for concrete planner phase.
 * Only rules which use DrillRelFactories should be used in this enum.
 */
public enum PlannerPhase {

  LOGICAL_PRUNE_AND_JOIN("Logical Planning (with join and partition pruning)") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(
          getDrillBasicRules(context),
          getPruneScanRules(context),
          getJoinPermRules(context),
          getDrillUserConfigurableLogicalRules(context),
          getStorageRules(context, plugins, this));
    }
  },

  WINDOW_REWRITE("Window Function rewrites") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return RuleSets.ofList(
          RuleInstance.CALC_INSTANCE,
          RuleInstance.PROJECT_TO_LOGICAL_PROJECT_AND_WINDOW_RULE
          );
    }
  },

  SUBQUERY_REWRITE("Sub-queries rewrites") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return RuleSets.ofList(
          RuleInstance.SUB_QUERY_FILTER_REMOVE_RULE,
          RuleInstance.SUB_QUERY_PROJECT_REMOVE_RULE,
          RuleInstance.SUB_QUERY_JOIN_REMOVE_RULE
      );
    }
  },

  LOGICAL_PRUNE("Logical Planning (with partition pruning)") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(
          getDrillBasicRules(context),
          getPruneScanRules(context),
          getDrillUserConfigurableLogicalRules(context),
          getStorageRules(context, plugins, this));
    }
  },

  JOIN_PLANNING("LOPT Join Planning") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      List<RelOptRule> rules = Lists.newArrayList();
      if (context.getPlannerSettings().isJoinOptimizationEnabled()) {
        rules.add(DRILL_JOIN_TO_MULTIJOIN_RULE);
        rules.add(DRILL_LOPT_OPTIMIZE_JOIN_RULE);
      }
      rules.add(RuleInstance.PROJECT_REMOVE_RULE);
      return PlannerPhase.mergedRuleSets(
          RuleSets.ofList(rules),
          getStorageRules(context, plugins, this)
          );
    }
  },

  ROWKEYJOIN_CONVERSION("Convert Join to RowKeyJoin") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      List<RelOptRule> rules = Lists.newArrayList();
      if (context.getPlannerSettings().isRowKeyJoinConversionEnabled()) {
        rules.add(DrillPushRowKeyJoinToScanRule.JOIN);
      }
      return PlannerPhase.mergedRuleSets(
          RuleSets.ofList(rules),
          getStorageRules(context, plugins, this)
      );
    }
  },

  SUM_CONVERSION("Convert SUM to $SUM0") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(
          RuleSets.ofList(
              DrillReduceAggregatesRule.INSTANCE_SUM,
              DrillReduceAggregatesRule.INSTANCE_WINDOW_SUM),
          getStorageRules(context, plugins, this)
          );
    }
  },

  PARTITION_PRUNING("Partition Prune Planning") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(getPruneScanRules(context), getStorageRules(context, plugins, this));
    }
  },

  PHYSICAL_PARTITION_PRUNING("Physical Partition Prune Planning") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(getPhysicalPruneScanRules(context), getStorageRules(context, plugins, this));
    }
  },

  DIRECTORY_PRUNING("Directory Prune Planning") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(getDirPruneScanRules(context), getStorageRules(context, plugins, this));
    }
  },

  LOGICAL("Logical Planning (no pruning or join).") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(
          PlannerPhase.getDrillBasicRules(context),
          PlannerPhase.getDrillUserConfigurableLogicalRules(context),
          getStorageRules(context, plugins, this));
    }
  },

  PHYSICAL("Physical Planning") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.mergedRuleSets(
          PlannerPhase.getPhysicalRules(context),
          getIndexRules(context),
          getStorageRules(context, plugins, this));
    }
  },

  PRE_LOGICAL_PLANNING("Planning with Hep planner only for rules, which are failed for Volcano planner") {
    @Override
    public RuleSet getRules (OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return PlannerPhase.getSetOpTransposeRules();
    }
  },

  TRANSITIVE_CLOSURE("Transitive closure") {
    @Override
    public RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins) {
      return getJoinTransitiveClosureRules();
    }
  };

  public final String description;

  PlannerPhase(String description) {
    this.description = description;
  }

  public abstract RuleSet getRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins);

  @SuppressWarnings("deprecation")
  private static RuleSet getStorageRules(OptimizerRulesContext context, Collection<StoragePlugin> plugins,
      PlannerPhase phase) {
    final Builder<RelOptRule> rules = ImmutableSet.builder();
    for (StoragePlugin plugin : plugins) {
      if (plugin instanceof AbstractStoragePlugin) {
        rules.addAll(((AbstractStoragePlugin) plugin).getOptimizerRules(context, phase));
      } else {
        rules.addAll(plugin.getOptimizerRules(context));
      }
    }
    return RuleSets.ofList(rules.build());
  }

  static final RelOptRule DRILL_JOIN_TO_MULTIJOIN_RULE =
      new JoinToMultiJoinRule(DrillJoinRel.class, DrillRelFactories.LOGICAL_BUILDER);
  static final RelOptRule DRILL_LOPT_OPTIMIZE_JOIN_RULE =
      new LoptOptimizeJoinRule(DrillRelBuilder.proto(
          DrillRelFactories.DRILL_LOGICAL_JOIN_FACTORY,
          DrillRelFactories.DRILL_LOGICAL_PROJECT_FACTORY,
          DrillRelFactories.DRILL_LOGICAL_FILTER_FACTORY));

  /**
   * Get a list of logical rules that can be turned on or off by session/system options.
   *
   * If a rule is intended to always be included with the logical set, it should be added
   * to the immutable list created in the getDrillBasicRules() method below.
   *
   * @param optimizerRulesContext - used to get the list of planner settings, other rules may
   *                                also in the future need to get other query state from this,
   *                                such as the available list of UDFs (as is used by the
   *                                DrillMergeProjectRule created in getDrillBasicRules())
   * @return - a list of rules that have been filtered to leave out
   *         rules that have been turned off by system or session settings
   */
  static RuleSet getDrillUserConfigurableLogicalRules(OptimizerRulesContext optimizerRulesContext) {
    final PlannerSettings ps = optimizerRulesContext.getPlannerSettings();

    // This list is used to store rules that can be turned on an off
    // by user facing planning options
    final Builder<RelOptRule> userConfigurableRules = ImmutableSet.builder();

    if (ps.isConstantFoldingEnabled()) {
      // TODO - DRILL-2218
      userConfigurableRules.add(RuleInstance.PROJECT_INSTANCE);
      userConfigurableRules.add(DrillReduceExpressionsRule.FILTER_INSTANCE_DRILL);
      userConfigurableRules.add(DrillReduceExpressionsRule.CALC_INSTANCE_DRILL);
    }

    return RuleSets.ofList(userConfigurableRules.build());
  }

  /*
   * These basic rules don't require any context, so singleton instances can be used.
   * These are merged with per-query rules in getDrillBasicRules() below.
   */
  final static ImmutableSet<RelOptRule> staticRuleSet = ImmutableSet.<RelOptRule> builder().add(
      // Add support for Distinct Union (by using Union-All followed by Distinct)
      RuleInstance.UNION_TO_DISTINCT_RULE,

      // Add support for WHERE style joins.
      DrillFilterJoinRules.FILTER_INTO_JOIN,
      DrillFilterJoinRules.JOIN_PUSH_CONDITION,
      RuleInstance.JOIN_PUSH_EXPRESSIONS_RULE,
      // End support for WHERE style joins.

      /*
       Filter push-down related rules
       */
      DrillPushFilterPastProjectRule.LOGICAL,
      DrillPushFilterPastProjectRule.DRILL_INSTANCE,
      // Due to infinite loop in planning (DRILL-3257/CALCITE-1271), temporarily use this rule in Hep planner
      // RuleInstance.FILTER_SET_OP_TRANSPOSE_RULE,
      DrillFilterAggregateTransposeRule.INSTANCE,
      DrillProjectLateralJoinTransposeRule.INSTANCE,
      DrillProjectPushIntoLateralJoinRule.INSTANCE,
      RuleInstance.FILTER_MERGE_RULE,
      RuleInstance.FILTER_CORRELATE_RULE,
      RuleInstance.AGGREGATE_REMOVE_RULE,
      RuleInstance.PROJECT_REMOVE_RULE,
      RuleInstance.SORT_REMOVE_RULE,

      RuleInstance.AGGREGATE_EXPAND_DISTINCT_AGGREGATES_RULE,
      DrillReduceAggregatesRule.INSTANCE,

      /*
       Projection push-down related rules
       */
      DrillPushProjectPastFilterRule.INSTANCE,
      DrillPushProjectPastJoinRule.INSTANCE,

      // Due to infinite loop in planning (DRILL-3257/CALCITE-1271), temporarily use this rule in Hep planner
      // RuleInstance.PROJECT_SET_OP_TRANSPOSE_RULE,
      RuleInstance.PROJECT_WINDOW_TRANSPOSE_RULE,
      DrillPushProjectIntoScanRule.INSTANCE,
      DrillPushProjectIntoScanRule.DRILL_LOGICAL_INSTANCE,

      /*
       Convert from Calcite Logical to Drill Logical Rules.
       */
      RuleInstance.EXPAND_CONVERSION_RULE,
      DrillScanRule.INSTANCE,
      DrillFilterRule.INSTANCE,
      DrillProjectRule.INSTANCE,
      DrillWindowRule.INSTANCE,
      DrillAggregateRule.INSTANCE,

      DrillLimitRule.INSTANCE,
      DrillSortRule.INSTANCE,
      DrillJoinRule.INSTANCE,
      DrillUnionAllRule.INSTANCE,
      DrillValuesRule.INSTANCE,
      DrillUnnestRule.INSTANCE,
      DrillCorrelateRule.INSTANCE
      ).build();

  /**
   * Get an immutable list of rules that will always be used when running
   * logical planning.
   *
   * This cannot be a static singleton because some of the rules need to
   * reference state owned by the current query (including its allocator).
   *
   * If a logical rule needs to be user configurable, such as turning
   * it on and off with a system/session option, add it in the
   * getDrillUserConfigurableLogicalRules() method instead of here.
   *
   * @param optimizerRulesContext - shared state used during planning, currently used here
   *                                to gain access to the function registry described above.
   * @return - a RuleSet containing the logical rules that will always
   *           be used, either by VolcanoPlanner directly, or
   *           used VolcanoPlanner as pre-processing for LOPTPlanner.
   *
   * Note : Join permutation rule is excluded here.
   */
  static RuleSet getDrillBasicRules(OptimizerRulesContext optimizerRulesContext) {
    /*
     * We have to create another copy of the ruleset with the context dependent elements;
     * this cannot be reused across queries.
     */
    ImmutableSet.Builder<RelOptRule> basicRules = ImmutableSet.<RelOptRule>builder()
        .addAll(staticRuleSet)
        .add(
            DrillMergeProjectRule.getInstance(true, RelFactories.DEFAULT_PROJECT_FACTORY,
                optimizerRulesContext.getFunctionRegistry())
            );
    if (optimizerRulesContext.getPlannerSettings().isHashJoinEnabled() &&
        optimizerRulesContext.getPlannerSettings().isSemiJoinEnabled()) {
      basicRules.add(RuleInstance.SEMI_JOIN_PROJECT_RULE);
    }

    return RuleSets.ofList(basicRules.build());
  }

  /**
   *   Get an immutable list of partition pruning rules that will be used in logical planning.
   */
  static RuleSet getPruneScanRules(OptimizerRulesContext optimizerRulesContext) {
    final ImmutableSet<RelOptRule> pruneRules = ImmutableSet.<RelOptRule>builder()
        .addAll(getItemStarRules())
        .add(
            PruneScanRule.getDirFilterOnProject(optimizerRulesContext),
            PruneScanRule.getDirFilterOnScan(optimizerRulesContext),
            ParquetPruneScanRule.getFilterOnProjectParquet(optimizerRulesContext),
            ParquetPruneScanRule.getFilterOnScanParquet(optimizerRulesContext),
            // Include LIMIT_ON_PROJECT since LIMIT_ON_SCAN may not work without it
            DrillPushLimitToScanRule.LIMIT_ON_PROJECT,
            DrillPushLimitToScanRule.LIMIT_ON_SCAN,
            PruneScanRule.getConvertAggScanToValuesRule(optimizerRulesContext)
        )
        .build();

    return RuleSets.ofList(pruneRules);
  }

  static RuleSet getIndexRules(OptimizerRulesContext optimizerRulesContext) {
    final PlannerSettings ps = optimizerRulesContext.getPlannerSettings();
    if (!ps.isIndexPlanningEnabled()) {
      return RuleSets.ofList(ImmutableSet.<RelOptRule>builder().build());
    }

    final ImmutableSet<RelOptRule> indexRules = ImmutableSet.<RelOptRule>builder()
        .add(
            DbScanToIndexScanPrule.REL_FILTER_SCAN,
            DbScanToIndexScanPrule.SORT_FILTER_PROJECT_SCAN,
            DbScanToIndexScanPrule.SORT_PROJECT_FILTER_PROJECT_SCAN,
            DbScanToIndexScanPrule.PROJECT_FILTER_PROJECT_SCAN,
            DbScanToIndexScanPrule.SORT_PROJECT_FILTER_SCAN,
            DbScanToIndexScanPrule.FILTER_PROJECT_SCAN,
            DbScanToIndexScanPrule.FILTER_SCAN,
            DbScanSortRemovalRule.INDEX_SORT_EXCHANGE_PROJ_SCAN,
            DbScanSortRemovalRule.INDEX_SORT_EXCHANGE_SCAN,
            DbScanSortRemovalRule.INDEX_SORT_SCAN,
            DbScanSortRemovalRule.INDEX_SORT_PROJ_SCAN
        )
        .build();
    return RuleSets.ofList(indexRules);
  }

  /**
   *   Get an immutable list of pruning rules that will be used post physical planning.
   */
  static RuleSet getPhysicalPruneScanRules(OptimizerRulesContext optimizerRulesContext) {
    final ImmutableSet<RelOptRule> pruneRules = ImmutableSet.<RelOptRule>builder()
        .add(
            // See DRILL-4998 for more detail.
            // Main reason for doing this is we want to reduce the performance regression possibility
            // caused by a different join order, as a result of reduced row count in scan operator.
            // Ideally this should be done in logical planning, before join order planning is done.
            // Before we can make such change, we have to figure out how to adjust the selectivity
            // estimation of filter operator, after filter is pushed down to scan.

            FilePushDownFilter.getFilterOnProject(optimizerRulesContext),
            FilePushDownFilter.getFilterOnScan(optimizerRulesContext),
            DrillPushProjectIntoScanRule.DRILL_PHYSICAL_INSTANCE
        )
        .build();

    return RuleSets.ofList(pruneRules);
  }

  /**
   *  Get an immutable list of directory-based partition pruning rules that will be used in Calcite logical planning.
   *
   * @param optimizerRulesContext rules context
   * @return directory-based partition pruning rules
   */
  static RuleSet getDirPruneScanRules(OptimizerRulesContext optimizerRulesContext) {
    final Set<RelOptRule> pruneRules = ImmutableSet.<RelOptRule>builder()
        .addAll(getItemStarRules())
        .add(
            PruneScanRule.getDirFilterOnProject(optimizerRulesContext),
            PruneScanRule.getDirFilterOnScan(optimizerRulesContext),
            PruneScanRule.getConvertAggScanToValuesRule(optimizerRulesContext),
            ConvertCountToDirectScanRule.AGG_ON_PROJ_ON_SCAN,
            ConvertCountToDirectScanRule.AGG_ON_SCAN
          )
        .build();

    return RuleSets.ofList(pruneRules);
  }

  /**
   * RuleSet for join permutation, used only in VolcanoPlanner.
   * @param optimizerRulesContext shared state used during planning
   * @return set of planning rules
   */
  static RuleSet getJoinPermRules(OptimizerRulesContext optimizerRulesContext) {
    return RuleSets.ofList(ImmutableSet.<RelOptRule> builder().add(
        RuleInstance.JOIN_PUSH_THROUGH_JOIN_RULE_RIGHT,
        RuleInstance.JOIN_PUSH_THROUGH_JOIN_RULE_LEFT
        ).build());
  }

  static final RuleSet DRILL_PHYSICAL_DISK = RuleSets.ofList(ImmutableSet.of(
      ProjectPrule.INSTANCE
    ));

  static RuleSet getPhysicalRules(OptimizerRulesContext optimizerRulesContext) {
    final List<RelOptRule> ruleList = new ArrayList<>();
    final PlannerSettings ps = optimizerRulesContext.getPlannerSettings();
    ruleList.add(ConvertCountToDirectScanPrule.AGG_ON_PROJ_ON_SCAN);
    ruleList.add(ConvertCountToDirectScanPrule.AGG_ON_SCAN);
    ruleList.add(SortConvertPrule.INSTANCE);
    ruleList.add(SortPrule.INSTANCE);
    ruleList.add(ProjectPrule.INSTANCE);
    ruleList.add(ScanPrule.INSTANCE);
    ruleList.add(ScreenPrule.INSTANCE);
    ruleList.add(RuleInstance.EXPAND_CONVERSION_RULE);
    ruleList.add(FilterPrule.INSTANCE);
    ruleList.add(LimitPrule.INSTANCE);
    ruleList.add(WriterPrule.INSTANCE);
    ruleList.add(WindowPrule.INSTANCE);
    ruleList.add(PushLimitToTopN.INSTANCE);
    ruleList.add(LimitExchangeTransposeRule.INSTANCE);
    ruleList.add(UnionAllPrule.INSTANCE);
    ruleList.add(ValuesPrule.INSTANCE);
    ruleList.add(DirectScanPrule.INSTANCE);
    ruleList.add(RowKeyJoinPrule.INSTANCE);
    ruleList.add(AnalyzePrule.INSTANCE);

    ruleList.add(MetadataControllerPrule.INSTANCE);
    ruleList.add(MetadataHandlerPrule.INSTANCE);
    ruleList.add(MetadataAggPrule.INSTANCE);
    ruleList.add(ConvertMetadataAggregateToDirectScanRule.INSTANCE);

    ruleList.add(UnnestPrule.INSTANCE);
    ruleList.add(LateralJoinPrule.INSTANCE);

    ruleList.add(DrillPushLimitToScanRule.LIMIT_ON_PROJECT);
    ruleList.add(DrillPushLimitToScanRule.LIMIT_ON_SCAN);

    if (ps.isHashAggEnabled()) {
      ruleList.add(HashAggPrule.INSTANCE);
    }

    if (ps.isStreamAggEnabled()) {
      ruleList.add(StreamAggPrule.INSTANCE);
    }

    if (ps.isHashJoinEnabled()) {
      ruleList.add(HashJoinPrule.DIST_INSTANCE);
      if (ps.isSemiJoinEnabled()) {
        ruleList.add(HashJoinPrule.SEMI_DIST_INSTANCE);
      }
      if(ps.isBroadcastJoinEnabled()){
        ruleList.add(HashJoinPrule.BROADCAST_INSTANCE);
        if (ps.isSemiJoinEnabled()) {
          ruleList.add(HashJoinPrule.SEMI_BROADCAST_INSTANCE);
        }
      }
    }

    if (ps.isMergeJoinEnabled()) {
      ruleList.add(MergeJoinPrule.DIST_INSTANCE);

      if(ps.isBroadcastJoinEnabled()){
        ruleList.add(MergeJoinPrule.BROADCAST_INSTANCE);
      }
    }

    // NLJ plans consist of broadcasting the right child, hence we need
    // broadcast join enabled.
    if (ps.isNestedLoopJoinEnabled() && ps.isBroadcastJoinEnabled()) {
      ruleList.add(NestedLoopJoinPrule.INSTANCE);
    }

    return RuleSets.ofList(ImmutableSet.copyOf(ruleList));
  }

  static RuleSet create(ImmutableSet<RelOptRule> rules) {
    return RuleSets.ofList(rules);
  }

  static RuleSet mergedRuleSets(RuleSet... ruleSets) {
    final Builder<RelOptRule> relOptRuleSetBuilder = ImmutableSet.builder();
    for (final RuleSet ruleSet : ruleSets) {
      for (final RelOptRule relOptRule : ruleSet) {
        relOptRuleSetBuilder.add(relOptRule);
      }
    }
    return RuleSets.ofList(relOptRuleSetBuilder.build());
  }

  /**
   * @return collection of rules to re-write item star operator for filter push down and partition pruning
   */
  private static ImmutableSet<RelOptRule> getItemStarRules() {
    return ImmutableSet.<RelOptRule>builder()
       .add(
             DrillFilterItemStarReWriterRule.PROJECT_ON_SCAN,
             DrillFilterItemStarReWriterRule.FILTER_ON_SCAN,
             DrillFilterItemStarReWriterRule.FILTER_PROJECT_SCAN
       ).build();
  }

  /**
   *  Get an immutable list of rules to transpose SetOp(Union) operator with other operators.<p>
   *  Note: Used by Hep planner only (failed for Volcano planner - CALCITE-1271)
   *
   * @return SetOp(Union) transpose rules
   */
  private static RuleSet getSetOpTransposeRules() {
    return RuleSets.ofList(ImmutableSet.<RelOptRule> builder()
        .add(
            RuleInstance.FILTER_SET_OP_TRANSPOSE_RULE,
            RuleInstance.PROJECT_SET_OP_TRANSPOSE_RULE
        ).build());
  }

  /**
   * RuleSet for join transitive closure, used only in HepPlanner.<p>
   * TODO: {@link RuleInstance#DRILL_JOIN_PUSH_TRANSITIVE_PREDICATES_RULE} should be moved into {@link #staticRuleSet},
   * (with using {@link DrillRelFactories#LOGICAL_BUILDER}) once CALCITE-1048 is solved. This block can be removed then.
   *
   * @return set of planning rules
   */
  static RuleSet getJoinTransitiveClosureRules() {
    return RuleSets.ofList(ImmutableSet.<RelOptRule> builder()
        .add(
            RuleInstance.DRILL_JOIN_PUSH_TRANSITIVE_PREDICATES_RULE,
            DrillFilterJoinRules.DRILL_FILTER_INTO_JOIN,
            RuleInstance.REMOVE_IS_NOT_DISTINCT_FROM_RULE,
            DrillFilterAggregateTransposeRule.DRILL_LOGICAL_INSTANCE,
            RuleInstance.DRILL_FILTER_MERGE_RULE
        ).build());
  }
}
