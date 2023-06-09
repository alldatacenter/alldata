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
package org.apache.drill.exec.planner.physical;

import org.apache.calcite.avatica.util.Quoting;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValidator;
import org.apache.drill.exec.server.options.OptionValidator.OptionDescription;
import org.apache.drill.exec.server.options.TypeValidators.BooleanValidator;
import org.apache.drill.exec.server.options.TypeValidators.EnumeratedStringValidator;
import org.apache.drill.exec.server.options.TypeValidators.LongValidator;
import org.apache.drill.exec.server.options.TypeValidators.DoubleValidator;
import org.apache.drill.exec.server.options.TypeValidators.PositiveLongValidator;
import org.apache.drill.exec.server.options.TypeValidators.RangeDoubleValidator;
import org.apache.drill.exec.server.options.TypeValidators.RangeLongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.exec.server.options.TypeValidators.MinRangeDoubleValidator;
import org.apache.drill.exec.server.options.TypeValidators.MaxRangeDoubleValidator;
import org.apache.calcite.plan.Context;

public class PlannerSettings implements Context{
  private static final Logger logger = LoggerFactory.getLogger(PlannerSettings.class);

  private int numEndPoints;
  private boolean useDefaultCosting; // True: use default Optiq costing, False: use Drill costing
  private boolean forceSingleMode;

  public static final int MAX_BROADCAST_THRESHOLD = Integer.MAX_VALUE;
  public static final int DEFAULT_IDENTIFIER_MAX_LENGTH = 1024;

  // initial off heap memory allocation (1M)
  private static final long INITIAL_OFF_HEAP_ALLOCATION_IN_BYTES = 1024 * 1024;
  // default off heap memory for planning (256M)
  @SuppressWarnings("unused")
  private static final long DEFAULT_MAX_OFF_HEAP_ALLOCATION_IN_BYTES = 256 * 1024 * 1024;
  // max off heap memory for planning (16G)
  private static final long MAX_OFF_HEAP_ALLOCATION_IN_BYTES = 16l * 1024 * 1024 * 1024;

  public static final OptionValidator CONSTANT_FOLDING = new BooleanValidator("planner.enable_constant_folding",
      new OptionDescription("If one side of a filter condition is a constant expression, constant folding evaluates the expression in the planning phase and replaces the expression with the constant value. For example, Drill can rewrite WHERE age + 5 < 42 as WHERE age < 37."));
  public static final String DISABLE_EXCHANGE_OPTION = "planner.disable_exchanges";
  public static final OptionValidator EXCHANGE = new BooleanValidator(DISABLE_EXCHANGE_OPTION,
      new OptionDescription("Toggles the state of hashing to a random exchange."));
  public static final String ENABLE_HASH_AGG_OPTION = "planner.enable_hashagg";
  public static final OptionValidator HASHAGG = new BooleanValidator(ENABLE_HASH_AGG_OPTION,
      new OptionDescription("Enable hash aggregation; otherwise, Drill does a sort-based aggregation. Writes to disk. Enable is recommended."));
  public static final String ENABLE_STREAM_AGG_OPTION = "planner.enable_streamagg";
  public static final OptionValidator STREAMAGG = new BooleanValidator(ENABLE_STREAM_AGG_OPTION,
      new OptionDescription("Sort-based operation. Writes to disk."));
  public static final OptionValidator TOPN = new BooleanValidator("planner.enable_topn",
      new OptionDescription("Generates the topN plan for queries with the ORDER BY and LIMIT clauses."));
  public static final String ENABLE_HASH_JOIN_OPTION = "planner.enable_hashjoin";
  public static final OptionValidator HASHJOIN = new BooleanValidator(ENABLE_HASH_JOIN_OPTION,
      new OptionDescription("Enable the memory hungry hash join. Drill assumes that a query will have adequate memory to complete and tries to use the fastest operations possible to complete the planned inner, left, right, or full outer joins using a hash table. Does not write to disk. Disabling hash join allows Drill to manage arbitrarily large data in a small memory footprint."));
  public static final OptionValidator SEMIJOIN = new BooleanValidator("planner.enable_semijoin",
          new OptionDescription("Enable the semi join optimization. Planner removes the distinct processing below the hash join and sets the semi join flag in hash join."));
  public static final OptionValidator MERGEJOIN = new BooleanValidator("planner.enable_mergejoin",
      new OptionDescription("Sort-based operation. A merge join is used for inner join, left and right outer joins. Inputs to the merge join must be sorted. It reads the sorted input streams from both sides and finds matching rows. Writes to disk."));
  public static final OptionValidator NESTEDLOOPJOIN = new BooleanValidator("planner.enable_nestedloopjoin",
      new OptionDescription("Sort-based operation. Writes to disk."));
  public static final OptionValidator MULTIPHASE = new BooleanValidator("planner.enable_multiphase_agg",
      new OptionDescription("Each minor fragment does a local aggregation in phase 1, distributes on a hash basis using GROUP-BY keys partially aggregated results to other fragments, and all the fragments perform a total aggregation using this data."));
  public static final OptionValidator BROADCAST = new BooleanValidator("planner.enable_broadcast_join",
      new OptionDescription("Changes the state of aggregation and join operators. The broadcast join can be used for hash join, merge join and nested loop join. Use to join a large (fact) table to relatively smaller (dimension) tables. Do not disable."));
  public static final OptionValidator BROADCAST_THRESHOLD = new PositiveLongValidator("planner.broadcast_threshold", MAX_BROADCAST_THRESHOLD,
      new OptionDescription("The maximum number of records allowed to be broadcast as part of a query. After one million records, Drill reshuffles data rather than doing a broadcast to one side of the join. Range: 0-2147483647"));
  public static final OptionValidator BROADCAST_FACTOR = new RangeDoubleValidator("planner.broadcast_factor", 0, Double.MAX_VALUE,
      new OptionDescription("A heuristic parameter for influencing the broadcast of records as part of a query."));
  public static final OptionValidator NESTEDLOOPJOIN_FACTOR = new RangeDoubleValidator("planner.nestedloopjoin_factor", 0, Double.MAX_VALUE,
      new OptionDescription("A heuristic value for influencing the nested loop join."));
  public static final OptionValidator NLJOIN_FOR_SCALAR = new BooleanValidator("planner.enable_nljoin_for_scalar_only",
      new OptionDescription("Supports nested loop join planning where the right input is scalar in order to enable NOT-IN, Inequality, Cartesian, and uncorrelated EXISTS planning."));
  public static final OptionValidator JOIN_ROW_COUNT_ESTIMATE_FACTOR = new RangeDoubleValidator("planner.join.row_count_estimate_factor", 0, Double.MAX_VALUE,
      new OptionDescription("The factor for adjusting the estimated row count when considering multiple join order sequences during the planning phase."));
  public static final OptionValidator MUX_EXCHANGE = new BooleanValidator("planner.enable_mux_exchange",
      new OptionDescription("Toggles the state of hashing to a multiplexed exchange."));
  public static final OptionValidator ORDERED_MUX_EXCHANGE = new BooleanValidator(ExecConstants.ORDERED_MUX_EXCHANGE,
      new OptionDescription("Generates the MUX exchange operator for ORDER BY queries with many minor fragments."));
  public static final OptionValidator DEMUX_EXCHANGE = new BooleanValidator("planner.enable_demux_exchange",
      new OptionDescription("Toggles the state of hashing to a demulitplexed exchange."));
  public static final OptionValidator PARTITION_SENDER_THREADS_FACTOR = new LongValidator("planner.partitioner_sender_threads_factor",
      new OptionDescription("A heuristic param to use to influence final number of threads. The higher the value the fewer the number of threads."));
  public static final OptionValidator PARTITION_SENDER_MAX_THREADS = new LongValidator("planner.partitioner_sender_max_threads",
      new OptionDescription("Upper limit of threads for outbound queuing."));
  public static final OptionValidator PARTITION_SENDER_SET_THREADS = new LongValidator("planner.partitioner_sender_set_threads",
      new OptionDescription("Overwrites the number of threads used to send out batches of records. Set to -1 to disable. Typically not changed."));
  public static final OptionValidator PRODUCER_CONSUMER = new BooleanValidator("planner.add_producer_consumer",
      new OptionDescription("Increase prefetching of data from disk. Disable for in-memory reads."));
  public static final OptionValidator PRODUCER_CONSUMER_QUEUE_SIZE = new LongValidator("planner.producer_consumer_queue_size",
      new OptionDescription("How much data to prefetch from disk in record batches out-of-band of query execution. The larger the queue size, the greater the amount of memory that the queue and overall query execution consumes."));
  public static final OptionValidator HASH_SINGLE_KEY = new BooleanValidator("planner.enable_hash_single_key",
      new OptionDescription("Each hash key is associated with a single value."));
  public static final OptionValidator HASH_JOIN_SWAP = new BooleanValidator("planner.enable_hashjoin_swap",
      new OptionDescription("Enables consideration of multiple join order sequences during the planning phase. Might negatively affect the performance of some queries due to inaccuracy of estimated row count especially after a filter, join, or aggregation."));
  public static final OptionValidator HASH_JOIN_SWAP_MARGIN_FACTOR = new RangeDoubleValidator("planner.join.hash_join_swap_margin_factor", 0, 100,
      new OptionDescription("The number of join order sequences to consider during the planning phase."));
  public static final String ENABLE_DECIMAL_DATA_TYPE_KEY = "planner.enable_decimal_data_type";
  public static final BooleanValidator ENABLE_DECIMAL_DATA_TYPE = new BooleanValidator(ENABLE_DECIMAL_DATA_TYPE_KEY,
      new OptionDescription("False disables the DECIMAL data type, including casting to DECIMAL and reading DECIMAL types from Parquet and Hive."));
  public static final OptionValidator HEP_OPT = new BooleanValidator("planner.enable_hep_opt", null);
  public static final OptionValidator HEP_PARTITION_PRUNING = new BooleanValidator("planner.enable_hep_partition_pruning", null);
  public static final OptionValidator ROWKEYJOIN_CONVERSION = new BooleanValidator("planner.enable_rowkeyjoin_conversion",
      new OptionDescription("Enables runtime filter pushdown(via rowkey-join) for queries that only filter on rowkeys"));
  public static final RangeDoubleValidator ROWKEYJOIN_CONVERSION_SELECTIVITY_THRESHOLD =
      new RangeDoubleValidator("planner.rowkeyjoin_conversion_selectivity_threshold", 0.0, 1.0,
          new OptionDescription("Sets the selectivity (as a percentage) under which Drill uses a rowkey join for queries that only filter on rowkeys"));
  public static final OptionValidator ROWKEYJOIN_CONVERSION_USING_HASHJOIN = new BooleanValidator("planner.rowkeyjoin_conversion_using_hashjoin",
      new OptionDescription("Enables runtime filter pushdown(via hash-join) for queries that only filter on rowkeys"));
  public static final OptionValidator PLANNER_MEMORY_LIMIT = new RangeLongValidator("planner.memory_limit",
      INITIAL_OFF_HEAP_ALLOCATION_IN_BYTES, MAX_OFF_HEAP_ALLOCATION_IN_BYTES,
      new OptionDescription("Defines the maximum amount of direct memory allocated to a query for planning. When multiple queries run concurrently, each query is allocated the amount of memory set by this parameter.Increase the value of this parameter and rerun the query if partition pruning failed due to insufficient memory."));
  public static final String UNIONALL_DISTRIBUTE_KEY = "planner.enable_unionall_distribute";
  public static final BooleanValidator UNIONALL_DISTRIBUTE = new BooleanValidator(UNIONALL_DISTRIBUTE_KEY, null);

  // ------------------------------------------- Index planning related options BEGIN --------------------------------------------------------------
  public static final String USE_SIMPLE_OPTIMIZER_KEY = "planner.use_simple_optimizer";
  public static final BooleanValidator USE_SIMPLE_OPTIMIZER = new BooleanValidator(USE_SIMPLE_OPTIMIZER_KEY,
      new OptionDescription("Simple optimizer applies fewer rules to reduce planning time and is meant to be used only for simple operational queries that use limit, sort, and filter."));
  public static final BooleanValidator INDEX_PLANNING = new BooleanValidator("planner.enable_index_planning",
      new OptionDescription("Enables or disables index planning."));
  public static final BooleanValidator ENABLE_STATS = new BooleanValidator("planner.enable_statistics",
      new OptionDescription("Enable or disable statistics for the filter conditions on indexed columns."));
  public static final BooleanValidator DISABLE_FULL_TABLE_SCAN = new BooleanValidator("planner.disable_full_table_scan",
      new OptionDescription("Disable generating a full table scan plan (only for internal testing use)"));
  public static final RangeLongValidator INDEX_MAX_CHOSEN_INDEXES_PER_TABLE = new RangeLongValidator("planner.index.max_chosen_indexes_per_table", 0, 100,
      new OptionDescription("The maximum number of 'chosen' indexes for a table after index costing and ranking."));
  public static final BooleanValidator INDEX_FORCE_SORT_NONCOVERING = new BooleanValidator("planner.index.force_sort_noncovering",
      new OptionDescription("Forces Drill to sort for non-covering indexes. If the query has an ORDER-BY on index columns and a non-covering index is chosen, by default Drill leverages the sortedness of the index columns and does not sort. Fast changing primary table data may produce a partial sort. This option forces a sort within Drill."));
  public static final BooleanValidator INDEX_USE_HASHJOIN_NONCOVERING = new BooleanValidator("planner.index.use_hashjoin_noncovering",
      new OptionDescription("Enable using HashJoin for non-covering index plans instead of RowKeyJoin (only for internal testing use)."));
  public static final RangeDoubleValidator INDEX_COVERING_SELECTIVITY_THRESHOLD =
      new RangeDoubleValidator("planner.index.covering_selectivity_threshold", 0.0, 1.0,
          new OptionDescription("For covering indexes, this option specifies the filter selectivity that corresponds to the leading prefix of the index below which the index is considered for planning."));
  public static final RangeDoubleValidator INDEX_NONCOVERING_SELECTIVITY_THRESHOLD =
      new RangeDoubleValidator("planner.index.noncovering_selectivity_threshold", 0.0, 1.0,
          new OptionDescription("For non-covering indexes, this option specifies the filter selectivity that corresponds to the leading prefix of the index below which the index is considered for planning."));
  public static final RangeDoubleValidator INDEX_ROWKEYJOIN_COST_FACTOR =
      new RangeDoubleValidator("planner.index.rowkeyjoin_cost_factor", 0, Double.MAX_VALUE,
          new OptionDescription("The cost factor that provides some control over the I/O cost for non-covering indexes when the rowkey join back to the primary table causes random I/O from the primary table."));
  // TODO: Deprecate the following 2 (also in SystemOptionManager.java)
  public static final BooleanValidator INDEX_PREFER_INTERSECT_PLANS = new BooleanValidator("planner.index.prefer_intersect_plans",
      new OptionDescription("Given 2 or more single column indexes, this option allows preferring index intersect plans compared to single column indexes (only for internal testing use)."));
  public static final RangeLongValidator INDEX_MAX_INDEXES_TO_INTERSECT = new RangeLongValidator("planner.index.max_indexes_to_intersect", 2, 100,
      new OptionDescription("The maximum number of indexes to intersect in a single query (only for internal testing use)."));
  public static final RangeDoubleValidator INDEX_STATS_ROWCOUNT_SCALING_FACTOR =
      new RangeDoubleValidator("planner.index.statistics_rowcount_scaling_factor", 0.0, 1.0,
          new OptionDescription("A factor that allows scaling the row count estimates returned from the storage/format plugin to compensate for under or over estimation."));
  // ------------------------------------------- Index planning related options END ----------------------------------------------------------------

  public static final OptionValidator IDENTIFIER_MAX_LENGTH =
      new RangeLongValidator("planner.identifier_max_length", 128 /* A minimum length is needed because option names are identifiers themselves */,
                              Integer.MAX_VALUE,
                              new OptionDescription("A minimum length is needed because option names are identifiers themselves."));

  public static final DoubleValidator FILTER_MIN_SELECTIVITY_ESTIMATE_FACTOR =
          new MinRangeDoubleValidator("planner.filter.min_selectivity_estimate_factor",
          0.0, 1.0, "planner.filter.max_selectivity_estimate_factor",
          new OptionDescription("Available as of Drill 1.8. Sets the minimum filter selectivity estimate to increase the parallelization of the major fragment performing a join. This option is useful for deeply nested queries with complicated predicates and serves as a workaround when statistics are insufficient or unavailable. The selectivity can vary between 0 and 1. The value of this option caps the estimated SELECTIVITY. The estimated ROWCOUNT is derived by multiplying the estimated SELECTIVITY by the estimated ROWCOUNT of the upstream operator. The estimated ROWCOUNT displays when you use the EXPLAIN PLAN INCLUDING ALL ATTRIBUTES FOR command. This option does not control the estimated ROWCOUNT of downstream operators (post FILTER). However, estimated ROWCOUNTs may change because the operator ROWCOUNTs depend on their downstream operators. The FILTER operator relies on the input of its immediate upstream operator, for example SCAN, AGGREGATE. If two filters are present in a plan, each filter may have a different estimated ROWCOUNT based on the immediate upstream operator\'s estimated ROWCOUNT."));
  public static final DoubleValidator FILTER_MAX_SELECTIVITY_ESTIMATE_FACTOR =
          new MaxRangeDoubleValidator("planner.filter.max_selectivity_estimate_factor",
          0.0, 1.0, "planner.filter.min_selectivity_estimate_factor",
          new OptionDescription("Available as of Drill 1.8. Sets the maximum filter selectivity estimate. The selectivity can vary between 0 and 1. For more details, see planner.filter.min_selectivity_estimate_factor."));

  public static final String TYPE_INFERENCE_KEY = "planner.enable_type_inference";
  public static final BooleanValidator TYPE_INFERENCE = new BooleanValidator(TYPE_INFERENCE_KEY, null);
  public static final LongValidator IN_SUBQUERY_THRESHOLD = new PositiveLongValidator("planner.in_subquery_threshold", Integer.MAX_VALUE,
      new OptionDescription("Defines the threshold of values in the IN list of the query to generate a hash join instead of an OR predicate.")); /* Same as Calcite's default IN List subquery size */

  public static final String PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_KEY = "planner.store.parquet.rowgroup.filter.pushdown.enabled";
  public static final BooleanValidator PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING = new BooleanValidator(PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_KEY,
      new OptionDescription("Enables filter pushdown optimization for Parquet files. Drill reads the file metadata, stored in the footer, to eliminate row groups based on the filter condition. Default is true. (Drill 1.9+)"));
  public static final String PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD_KEY = "planner.store.parquet.rowgroup.filter.pushdown.threshold";
  public static final LongValidator PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD = new LongValidator(PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD_KEY,
      new OptionDescription("Maximal number of row groups a table can have to enable pruning by the planner. Base this setting on the data set - increasing if needed " +
        "would add planning overhead, but may reduce execution overhead if the filter is relevant (e.g., on a sorted column, or many nulls). " +
        "Reduce this setting if the planning time is significant or you do not see any benefit at runtime. A non-positive value disables plan time pruning."));

  public static final String QUOTING_IDENTIFIERS_KEY = "planner.parser.quoting_identifiers";
  public static final EnumeratedStringValidator QUOTING_IDENTIFIERS = new EnumeratedStringValidator(
      QUOTING_IDENTIFIERS_KEY,
      new OptionDescription("Sets the type of identifier quotes for the SQL parser. Default is backticks ('`'). The SQL parser accepts double quotes ('\"') and square brackets ('['). (Drill 1.11+)"),
      Quoting.BACK_TICK.string, Quoting.DOUBLE_QUOTE.string, Quoting.BRACKET.string);

  /*
    "planner.enable_unnest_lateral" is to allow users to choose enable unnest+lateraljoin feature.
   */
  public static final String ENABLE_UNNEST_LATERAL_KEY = "planner.enable_unnest_lateral";
  public static final BooleanValidator ENABLE_UNNEST_LATERAL = new BooleanValidator(ENABLE_UNNEST_LATERAL_KEY,
      new OptionDescription("Enables lateral join functionality. Default is true. (Drill 1.15+)"));

  /*
     Enables rules that re-write query joins in the most optimal way.
     Though its turned on be default and its value in query optimization is undeniable, user may want turn off such
     optimization to leave join order indicated in sql query unchanged.

     For example:
     Currently only nested loop join allows non-equi join conditions usage.
     During planning stage nested loop join will be chosen when non-equi join is detected
     and {@link #NLJOIN_FOR_SCALAR} set to false. Though query performance may not be the most optimal in such case,
     user may use such workaround to execute queries with non-equi joins.

     Nested loop join allows only INNER and LEFT join usage and implies that right input is smaller that left input.
     During LEFT join when join optimization is enabled and detected that right input is larger that left,
     join will be optimized: left and right inputs will be flipped and LEFT join type will be changed to RIGHT one.
     If query contains non-equi joins, after such optimization it will fail, since nested loop does not allow
     RIGHT join. In this case if user accepts probability of non optimal performance, he may turn off join optimization.
     Turning off join optimization, makes sense only if user are not sure that right output is less or equal to left,
     otherwise join optimization can be left turned on.

     Note: once hash and merge joins will allow non-equi join conditions,
     the need to turn off join optimization may go away.
   */
  public static final BooleanValidator JOIN_OPTIMIZATION = new BooleanValidator("planner.enable_join_optimization",
      new OptionDescription("Enables join ordering optimization."));
  // for testing purpose
  public static final String FORCE_2PHASE_AGGR_KEY = "planner.force_2phase_aggr";
  public static final BooleanValidator FORCE_2PHASE_AGGR = new BooleanValidator(FORCE_2PHASE_AGGR_KEY,
      new OptionDescription("Forces the cost-based query planner to generate a two phase aggregation for an aggregate operator."));

  public static final BooleanValidator STATISTICS_USE = new BooleanValidator("planner.statistics.use", null);

  public static final RangeDoubleValidator STATISTICS_MULTICOL_NDV_ADJUST_FACTOR = new RangeDoubleValidator("planner.statistics.multicol_ndv_adjustment_factor", 0.0, 1.0, null);

  public OptionManager options = null;
  public FunctionImplementationRegistry functionImplementationRegistry = null;

  public PlannerSettings(OptionManager options, FunctionImplementationRegistry functionImplementationRegistry){
    this.options = options;
    this.functionImplementationRegistry = functionImplementationRegistry;
  }

  public OptionManager getOptions() {
    return options;
  }

  public boolean isSingleMode() {
    return forceSingleMode || options.getOption(EXCHANGE.getOptionName()).bool_val;
  }

  public void forceSingleMode() {
    forceSingleMode = true;
  }

  public int numEndPoints() {
    return numEndPoints;
  }

  public double getRowCountEstimateFactor(){
    return options.getOption(JOIN_ROW_COUNT_ESTIMATE_FACTOR.getOptionName()).float_val;
  }

  public double getBroadcastFactor(){
    return options.getOption(BROADCAST_FACTOR.getOptionName()).float_val;
  }

  public double getNestedLoopJoinFactor(){
    return options.getOption(NESTEDLOOPJOIN_FACTOR.getOptionName()).float_val;
  }

  public boolean isNlJoinForScalarOnly() {
    return options.getOption(NLJOIN_FOR_SCALAR.getOptionName()).bool_val;
  }

  public boolean useDefaultCosting() {
    return useDefaultCosting;
  }

  public void setNumEndPoints(int numEndPoints) {
    this.numEndPoints = numEndPoints;
  }

  public void setUseDefaultCosting(boolean defcost) {
    this.useDefaultCosting = defcost;
  }

  public boolean isHashAggEnabled() {
    return options.getOption(HASHAGG.getOptionName()).bool_val;
  }

  public boolean isConstantFoldingEnabled() {
    return options.getOption(CONSTANT_FOLDING.getOptionName()).bool_val;
  }

  public boolean isStreamAggEnabled() {
    return options.getOption(STREAMAGG.getOptionName()).bool_val;
  }

  public boolean isHashJoinEnabled() {
    return options.getOption(HASHJOIN.getOptionName()).bool_val;
  }

  public boolean isSemiJoinEnabled() {
    return options.getOption(SEMIJOIN.getOptionName()).bool_val;
  }

  public boolean isMergeJoinEnabled() {
    return options.getOption(MERGEJOIN.getOptionName()).bool_val;
  }

  public boolean isNestedLoopJoinEnabled() {
    return options.getOption(NESTEDLOOPJOIN.getOptionName()).bool_val;
  }

  public boolean isMultiPhaseAggEnabled() {
    return options.getOption(MULTIPHASE.getOptionName()).bool_val;
  }

  public boolean isBroadcastJoinEnabled() {
    return options.getOption(BROADCAST.getOptionName()).bool_val;
  }

  public boolean isHashSingleKey() {
    return options.getOption(HASH_SINGLE_KEY.getOptionName()).bool_val;
  }

  public boolean isHashJoinSwapEnabled() {
    return options.getOption(HASH_JOIN_SWAP.getOptionName()).bool_val;
  }

  public boolean isHepPartitionPruningEnabled() { return options.getOption(HEP_PARTITION_PRUNING.getOptionName()).bool_val;}

  public boolean isRowKeyJoinConversionEnabled() { return options.getOption(ROWKEYJOIN_CONVERSION.getOptionName()).bool_val;}

  public boolean isRowKeyJoinConversionUsingHashJoin() { return options.getOption(ROWKEYJOIN_CONVERSION_USING_HASHJOIN.getOptionName()).bool_val;}

  public double getRowKeyJoinConversionSelThreshold() { return options.getOption(ROWKEYJOIN_CONVERSION_SELECTIVITY_THRESHOLD);}

  public boolean isHepOptEnabled() { return options.getOption(HEP_OPT.getOptionName()).bool_val;}

  public double getHashJoinSwapMarginFactor() {
    return options.getOption(HASH_JOIN_SWAP_MARGIN_FACTOR.getOptionName()).float_val / 100d;
  }

  public long getBroadcastThreshold() {
    return options.getOption(BROADCAST_THRESHOLD.getOptionName()).num_val;
  }

  public long getSliceTarget(){
    return options.getOption(ExecConstants.SLICE_TARGET).num_val;
  }

  public boolean isMemoryEstimationEnabled() {
    return options.getOption(ExecConstants.ENABLE_MEMORY_ESTIMATION_KEY).bool_val;
  }

  public String getFsPartitionColumnLabel() {
    return options.getOption(ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL).string_val;
  }

  public long getIdentifierMaxLength(){
    return options.getOption(IDENTIFIER_MAX_LENGTH.getOptionName()).num_val;
  }

  public long getPlanningMemoryLimit() {
    return options.getOption(PLANNER_MEMORY_LIMIT.getOptionName()).num_val;
  }

  public static long getInitialPlanningMemorySize() {
    return INITIAL_OFF_HEAP_ALLOCATION_IN_BYTES;
  }

  public double getFilterMinSelectivityEstimateFactor() {
    return options.getOption(FILTER_MIN_SELECTIVITY_ESTIMATE_FACTOR);
  }

  public double getFilterMaxSelectivityEstimateFactor(){
    return options.getOption(FILTER_MAX_SELECTIVITY_ESTIMATE_FACTOR);
  }

  public boolean isTypeInferenceEnabled() {
    return options.getOption(TYPE_INFERENCE);
  }

  public boolean isForce2phaseAggr() { return options.getOption(FORCE_2PHASE_AGGR);} // for testing

  public long getInSubqueryThreshold() {
    return options.getOption(IN_SUBQUERY_THRESHOLD);
  }

  public boolean isUnionAllDistributeEnabled() {
    return options.getOption(UNIONALL_DISTRIBUTE);
  }

  public boolean isParquetRowGroupFilterPushdownPlanningEnabled() {
    return options.getOption(PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING);
  }

  public long getParquetRowGroupFilterPushDownThreshold() {
    return options.getOption(PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD);
  }

  /**
   * @return Quoting enum for current quoting identifiers character
   */
  public Quoting getQuotingIdentifiers() {
    String quotingIdentifiersCharacter = options.getOption(QUOTING_IDENTIFIERS);
    for (Quoting value : Quoting.values()) {
      if (value.string.equals(quotingIdentifiersCharacter)) {
        return value;
      }
    }
    // this is never reached
    throw UserException.validationError()
        .message("Unknown quoting identifier character '%s'", quotingIdentifiersCharacter)
        .build(logger);
  }

  public boolean isJoinOptimizationEnabled() {
    return options.getOption(JOIN_OPTIMIZATION);
  }

  public boolean isUnnestLateralEnabled() {
    return options.getOption(ENABLE_UNNEST_LATERAL);
  }

  public boolean isIndexPlanningEnabled() {
    return options.getOption(INDEX_PLANNING);
  }

  public boolean isStatisticsEnabled() {
    return options.getOption(ENABLE_STATS);
  }

  public boolean isDisableFullTableScan() {
    return options.getOption(DISABLE_FULL_TABLE_SCAN);
  }

  public long getIndexMaxChosenIndexesPerTable() {
    return options.getOption(INDEX_MAX_CHOSEN_INDEXES_PER_TABLE);
  }

  public boolean isIndexForceSortNonCovering() {
    return options.getOption(INDEX_FORCE_SORT_NONCOVERING);
  }

  public boolean isIndexUseHashJoinNonCovering() {
    return options.getOption(INDEX_USE_HASHJOIN_NONCOVERING);
  }

  public double getIndexCoveringSelThreshold() {
    return options.getOption(INDEX_COVERING_SELECTIVITY_THRESHOLD);
  }

  public double getIndexNonCoveringSelThreshold() {
    return options.getOption(INDEX_NONCOVERING_SELECTIVITY_THRESHOLD);
  }

  public double getIndexRowKeyJoinCostFactor() {
    return options.getOption(INDEX_ROWKEYJOIN_COST_FACTOR);
  }

  public boolean isIndexIntersectPlanPreferred() {
    return options.getOption(INDEX_PREFER_INTERSECT_PLANS);
  }

  public long getMaxIndexesToIntersect() {
    return options.getOption(INDEX_MAX_INDEXES_TO_INTERSECT);
  }

  public double getIndexStatsRowCountScalingFactor() {
    return options.getOption(INDEX_STATS_ROWCOUNT_SCALING_FACTOR);
  }

  public boolean useStatistics() {
    return options.getOption(STATISTICS_USE);
  }

  public double getStatisticsMultiColNdvAdjustmentFactor() {
    return options.getOption(STATISTICS_MULTICOL_NDV_ADJUST_FACTOR);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(Class<T> clazz) {
    if(clazz == PlannerSettings.class){
      return (T) this;
    }else{
      return null;
    }
  }
}
