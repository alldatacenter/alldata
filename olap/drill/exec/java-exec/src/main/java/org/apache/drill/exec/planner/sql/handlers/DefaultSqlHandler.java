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
package org.apache.drill.exec.planner.sql.handlers;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.calcite.plan.RelOptCostImpl;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableFunctionScan;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.LogicalValues;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.RelDecorrelator;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.RuleSet;
import org.apache.calcite.tools.ValidationException;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.logical.PlanProperties;
import org.apache.drill.common.logical.PlanProperties.Generator.ResultMode;
import org.apache.drill.common.logical.PlanProperties.PlanPropertiesBuilder;
import org.apache.drill.common.logical.PlanProperties.PlanType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.impl.join.JoinUtils;
import org.apache.drill.exec.planner.PlannerPhase;
import org.apache.drill.exec.planner.PlannerType;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.planner.logical.DrillScreenRel;
import org.apache.drill.exec.planner.logical.PreProcessLogicalRel;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait;
import org.apache.drill.exec.planner.physical.PhysicalPlanCreator;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.explain.PrelSequencer;
import org.apache.drill.exec.planner.physical.visitor.AdjustOperatorsSchemaVisitor;
import org.apache.drill.exec.planner.physical.visitor.ComplexToJsonPrelVisitor;
import org.apache.drill.exec.planner.physical.visitor.ExcessiveExchangeIdentifier;
import org.apache.drill.exec.planner.physical.visitor.FinalColumnReorderer;
import org.apache.drill.exec.planner.physical.visitor.InsertLocalExchangeVisitor;
import org.apache.drill.exec.planner.physical.visitor.LateralUnnestRowIDVisitor;
import org.apache.drill.exec.planner.physical.visitor.MemoryEstimationVisitor;
import org.apache.drill.exec.planner.physical.visitor.RelUniqifier;
import org.apache.drill.exec.planner.physical.visitor.RewriteProjectToFlatten;
import org.apache.drill.exec.planner.physical.visitor.RuntimeFilterVisitor;
import org.apache.drill.exec.planner.physical.visitor.SelectionVectorPrelVisitor;
import org.apache.drill.exec.planner.physical.visitor.SplitUpComplexExpressions;
import org.apache.drill.exec.planner.physical.visitor.StarColumnConverter;
import org.apache.drill.exec.planner.physical.visitor.SwapHashJoinVisitor;
import org.apache.drill.exec.planner.physical.visitor.TopProjectVisitor;
import org.apache.drill.exec.planner.sql.parser.UnsupportedOperatorsVisitor;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class DefaultSqlHandler extends AbstractSqlHandler {
  private static final Logger logger = LoggerFactory.getLogger(DefaultSqlHandler.class);

  private final Pointer<String> textPlan;
  private final long targetSliceSize;
  protected final SqlHandlerConfig config;
  protected final QueryContext context;

  public DefaultSqlHandler(SqlHandlerConfig config) {
    this(config, null);
  }

  public DefaultSqlHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    super();
    this.config = config;
    this.context = config.getContext();
    this.textPlan = textPlan;
    this.targetSliceSize = config.getContext().getOptions().getOption(ExecConstants.SLICE_TARGET_OPTION);
  }

  protected void log(final PlannerType plannerType, final PlannerPhase phase, final RelNode node, final Logger logger,
      Stopwatch watch) {
    if (logger.isDebugEnabled()) {
      log(plannerType.name() + ":" + phase.description, node, logger, watch);
    }
  }

  protected void log(final String description, final RelNode node, final Logger logger, Stopwatch watch) {
    if (logger.isDebugEnabled()) {
      final String plan = RelOptUtil.toString(node, SqlExplainLevel.ALL_ATTRIBUTES);
      final String time = watch == null ? "" : String.format(" (%dms)", watch.elapsed(TimeUnit.MILLISECONDS));
      logger.debug(String.format("%s%s:\n%s", description, time, plan));
    }
  }

  protected void logAndSetTextPlan(final String description, final Prel prel, final Logger logger) {
    final String plan = PrelSequencer.printWithIds(prel, SqlExplainLevel.ALL_ATTRIBUTES);
    if (textPlan != null) {
      textPlan.value = plan;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("%s:\n%s", description, plan));
    }
  }

  protected void log(final String name, final PhysicalPlan plan, final Logger logger) {
    if (logger.isDebugEnabled()) {
      PropertyFilter filter = new SimpleBeanPropertyFilter.SerializeExceptFilter(Sets.newHashSet("password"));
      String planText = plan.unparse(context.getLpPersistence().getMapper()
              .writer(new SimpleFilterProvider().addFilter("passwordFilter", filter)));
      logger.debug(name + " : \n" + planText);
    }
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ValidationException, RelConversionException, IOException, ForemanSetupException {
    final ConvertedRelNode convertedRelNode = validateAndConvert(sqlNode);
    final RelDataType validatedRowType = convertedRelNode.getValidatedRowType();
    final RelNode queryRelNode = convertedRelNode.getConvertedNode();

    final DrillRel drel = convertToDrel(queryRelNode);
    final Prel prel = convertToPrel(drel, validatedRowType);
    logAndSetTextPlan("Drill Physical", prel, logger);
    final PhysicalOperator pop = convertToPop(prel);
    final PhysicalPlan plan = convertToPlan(pop);
    log("Drill Plan", plan, logger);
    return plan;
  }

  /**
   * Rewrite the parse tree. Used before validating the parse tree. Useful if a
   * particular statement needs to converted into another statement.
   *
   * @param node sql parse tree to be rewritten
   * @return Rewritten sql parse tree
   */
  protected SqlNode rewrite(SqlNode node) throws RelConversionException, ForemanSetupException {
    return node;
  }

  protected ConvertedRelNode validateAndConvert(SqlNode sqlNode) throws ForemanSetupException, RelConversionException, ValidationException {
    final SqlNode rewrittenSqlNode = rewrite(sqlNode);
    final Pair<SqlNode, RelDataType> validatedTypedSqlNode = validateNode(rewrittenSqlNode);
    final SqlNode validated = validatedTypedSqlNode.getKey();

    RelNode rel = convertToRel(validated);
    rel = preprocessNode(rel);

    return new ConvertedRelNode(rel, validatedTypedSqlNode.getValue());
  }

  /**
   * Given a relNode tree for SELECT statement, convert to Drill Logical RelNode tree.
   *
   * @param relNode relational node
   * @return Drill Logical RelNode tree
   * @throws SqlUnsupportedException if query cannot be planned
   */
  protected DrillRel convertToRawDrel(final RelNode relNode) throws SqlUnsupportedException {
    if (context.getOptions().getOption(ExecConstants.EARLY_LIMIT0_OPT) &&
        context.getPlannerSettings().isTypeInferenceEnabled() &&
        FindLimit0Visitor.containsLimit0(relNode)) {
      // if the schema is known, return the schema directly
      final DrillRel shorterPlan;
      if ((shorterPlan = FindLimit0Visitor.getDirectScanRelIfFullySchemaed(relNode)) != null) {
        return shorterPlan;
      }

      if (FindHardDistributionScans.canForceSingleMode(relNode)) {
        // disable distributed mode
        context.getPlannerSettings().forceSingleMode();
      }
    }

    try {
      // HEP for rules, which are failed at the LOGICAL_PLANNING stage for Volcano planner
      final RelNode setOpTransposeNode = transform(PlannerType.HEP, PlannerPhase.PRE_LOGICAL_PLANNING, relNode);

      // HEP Directory pruning.
      final RelNode pruned = transform(PlannerType.HEP_BOTTOM_UP, PlannerPhase.DIRECTORY_PRUNING, setOpTransposeNode);
      final RelTraitSet logicalTraits = pruned.getTraitSet().plus(DrillRel.DRILL_LOGICAL);

      final RelNode convertedRelNode;
      if (!context.getPlannerSettings().isHepOptEnabled()) {
        // hep is disabled, use volcano
        convertedRelNode = transform(PlannerType.VOLCANO, PlannerPhase.LOGICAL_PRUNE_AND_JOIN, pruned, logicalTraits);

      } else {
        final RelNode intermediateNode2;
        final RelNode intermediateNode3;
        if (context.getPlannerSettings().isHepPartitionPruningEnabled()) {

          final RelNode intermediateNode = transform(PlannerType.VOLCANO, PlannerPhase.LOGICAL, pruned, logicalTraits);

          // HEP Join Push Transitive Predicates
          final RelNode transitiveClosureNode =
              transform(PlannerType.HEP, PlannerPhase.TRANSITIVE_CLOSURE, intermediateNode);

          // hep is enabled and hep pruning is enabled.
          intermediateNode2 = transform(PlannerType.HEP_BOTTOM_UP, PlannerPhase.PARTITION_PRUNING, transitiveClosureNode);

        } else {
          // Only hep is enabled
          final RelNode intermediateNode =
              transform(PlannerType.VOLCANO, PlannerPhase.LOGICAL_PRUNE, pruned, logicalTraits);

          // HEP Join Push Transitive Predicates
          intermediateNode2 = transform(PlannerType.HEP, PlannerPhase.TRANSITIVE_CLOSURE, intermediateNode);
        }

        // Do Join Planning.
        intermediateNode3 = transform(PlannerType.HEP_BOTTOM_UP, PlannerPhase.JOIN_PLANNING, intermediateNode2);

        if (context.getPlannerSettings().isRowKeyJoinConversionEnabled()) {
          // Covert Join to RowKeyJoin, where applicable.
          convertedRelNode = transform(PlannerType.HEP_BOTTOM_UP, PlannerPhase.ROWKEYJOIN_CONVERSION, intermediateNode3);
        } else {
          convertedRelNode = intermediateNode3;
        }
      }

      // Convert SUM to $SUM0
      final RelNode convertedRelNodeWithSum0 = transform(PlannerType.HEP_BOTTOM_UP, PlannerPhase.SUM_CONVERSION, convertedRelNode);

      DrillRel drillRel = (DrillRel) convertedRelNodeWithSum0;
      // If the query contains a limit 0 clause, disable distributed mode since it is overkill for determining schema.
      if (FindLimit0Visitor.containsLimit0(convertedRelNodeWithSum0) &&
          FindHardDistributionScans.canForceSingleMode(convertedRelNodeWithSum0)) {
        context.getPlannerSettings().forceSingleMode();
        if (context.getOptions().getOption(ExecConstants.LATE_LIMIT0_OPT)) {
          drillRel = FindLimit0Visitor.addLimitOnTopOfLeafNodes(drillRel);
        }
      }

      return drillRel;
    } catch (RelOptPlanner.CannotPlanException ex) {
      logger.error(ex.getMessage());

      if (JoinUtils.checkCartesianJoin(relNode)) {
        throw JoinUtils.cartesianJoinPlanningException();
      } else {
        throw ex;
      }
    }
  }

  /**
   * Return Drill Logical RelNode tree for a SELECT statement, when it is executed / explained directly.
   * Adds screen operator on top of converted node.
   *
   * @param relNode root RelNode corresponds to Calcite Logical RelNode.
   * @return Drill Logical RelNode tree
   * @throws SqlUnsupportedException if query cannot be planned
   */
  protected DrillRel convertToDrel(RelNode relNode) throws SqlUnsupportedException {
    final DrillRel convertedRelNode = convertToRawDrel(relNode);

    return new DrillScreenRel(convertedRelNode.getCluster(), convertedRelNode.getTraitSet(),
        convertedRelNode);
  }

  /**
   * Finalize all RelNodes.
   */
  private static class PrelFinalizer extends RelShuttleImpl {

    @Override
    public RelNode visit(RelNode other) {
      if (other instanceof PrelFinalizable) {
        return ((PrelFinalizable) other).finalizeRel();
      } else {
        return super.visit(other);
      }
    }
  }

  /**
   * Transform RelNode to a new RelNode without changing any traits. Also will log the outcome.
   *
   * @param plannerType The type of Planner to use.
   * @param phase The transformation phase we're running.
   * @param input The original RelNode
   * @return The transformed relnode.
   */
  private RelNode transform(PlannerType plannerType, PlannerPhase phase, RelNode input) {
    return transform(plannerType, phase, input, input.getTraitSet());
  }

  /**
   * Transform RelNode to a new RelNode, targeting the provided set of traits. Also will log the outcome.
   *
   * @param plannerType The type of Planner to use.
   * @param phase The transformation phase we're running.
   * @param input The original RelNode
   * @param targetTraits The traits we are targeting for output.
   * @return The transformed relnode.
   */
  protected RelNode transform(PlannerType plannerType, PlannerPhase phase, RelNode input, RelTraitSet targetTraits) {
    return transform(plannerType, phase, input, targetTraits, true);
  }

  /**
   * Transform RelNode to a new RelNode, targeting the provided set of traits. Also will log the outcome if asked.
   *
   * @param plannerType The type of Planner to use.
   * @param phase The transformation phase we're running.
   * @param input The original RelNode
   * @param targetTraits The traits we are targeting for output.
   * @param log Whether to log the planning phase.
   * @return The transformed relnode.
   */
  protected RelNode transform(PlannerType plannerType, PlannerPhase phase, RelNode input, RelTraitSet targetTraits,
      boolean log) {
    final Stopwatch watch = Stopwatch.createStarted();
    final RuleSet rules = config.getRules(phase, input);
    final RelTraitSet toTraits = targetTraits.simplify();

    final RelNode output;
    switch (plannerType) {
    case HEP_BOTTOM_UP:
    case HEP: {
      final HepProgramBuilder hepPgmBldr = new HepProgramBuilder();
      if (plannerType == PlannerType.HEP_BOTTOM_UP) {
        hepPgmBldr.addMatchOrder(HepMatchOrder.BOTTOM_UP);
      }
      for (RelOptRule rule : rules) {
        hepPgmBldr.addRuleInstance(rule);
      }

      // Set noDAG = true to avoid caching problems which lead to incorrect Drill work.
      final HepPlanner planner = new HepPlanner(hepPgmBldr.build(), context.getPlannerSettings(), true, null,
          RelOptCostImpl.FACTORY);

      JaninoRelMetadataProvider relMetadataProvider = Utilities.registerJaninoRelMetadataProvider();

      // Modify RelMetaProvider for every RelNode in the SQL operator Rel tree.
      input.accept(new MetaDataProviderModifier(relMetadataProvider));
      planner.setRoot(input);
      if (!input.getTraitSet().equals(targetTraits)) {
        planner.changeTraits(input, toTraits);
      }
      output = planner.findBestExp();
      break;
    }
    case VOLCANO:
    default: {
      // as weird as it seems, the cluster's only planner is the volcano planner.
      final RelOptPlanner planner = input.getCluster().getPlanner();
      final Program program = Programs.of(rules);
      Preconditions.checkArgument(planner instanceof VolcanoPlanner,
          "Cluster is expected to be constructed using VolcanoPlanner. Was actually of type %s.", planner.getClass()
              .getName());
      output = program.run(planner, input, toTraits,
          ImmutableList.of(), ImmutableList.of());

      break;
    }
    }

    if (log) {
      log(plannerType, phase, output, logger, watch);
    }

    return output;
  }

  /**
   * Applies physical rules and certain transformations to convert drill relational node into physical one.
   *
   * @param drel relational node
   * @param validatedRowType final output row type
   * @return physical relational node
   * @throws RelConversionException
   * @throws SqlUnsupportedException
   */
  protected Prel convertToPrel(RelNode drel, RelDataType validatedRowType)
      throws RelConversionException, SqlUnsupportedException {
    Preconditions.checkArgument(drel.getConvention() == DrillRel.DRILL_LOGICAL);

    final RelTraitSet traits = drel.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(DrillDistributionTrait.SINGLETON);
    Prel phyRelNode;
    try {
      final Stopwatch watch = Stopwatch.createStarted();
      final RelNode relNode = transform(PlannerType.VOLCANO, PlannerPhase.PHYSICAL, drel, traits, false);
      phyRelNode = (Prel) relNode.accept(new PrelFinalizer());
      // log externally as we need to finalize before traversing the tree.
      log(PlannerType.VOLCANO, PlannerPhase.PHYSICAL, phyRelNode, logger, watch);
    } catch (RelOptPlanner.CannotPlanException ex) {
      logger.error(ex.getMessage());

      if (JoinUtils.checkCartesianJoin(drel)) {
        throw JoinUtils.cartesianJoinPlanningException();
      } else {
        throw ex;
      }
    }

    OptionManager queryOptions = context.getOptions();

    if (context.getPlannerSettings().isMemoryEstimationEnabled()
        && !MemoryEstimationVisitor.enoughMemory(phyRelNode, queryOptions, context.getActiveEndpoints().size())) {
      log("Not enough memory for this plan", phyRelNode, logger, null);
      logger.debug("Re-planning without hash operations.");

      queryOptions.setLocalOption(PlannerSettings.HASHJOIN.getOptionName(), false);
      queryOptions.setLocalOption(PlannerSettings.HASHAGG.getOptionName(), false);

      try {
        final RelNode relNode = transform(PlannerType.VOLCANO, PlannerPhase.PHYSICAL, drel, traits);
        phyRelNode = (Prel) relNode.accept(new PrelFinalizer());
      } catch (RelOptPlanner.CannotPlanException ex) {
        logger.error(ex.getMessage());

        if (JoinUtils.checkCartesianJoin(drel)) {
          throw JoinUtils.cartesianJoinPlanningException();
        } else {
          throw ex;
        }
      }
    }
    // Handy way to visualize the plan while debugging
    //ExplainHandler.printPlan(phyRelNode, context);

    /* The order of the following transformations is important */

    /*
     * 0.)
     * Add top project before screen operator or writer to ensure that final output column names are preserved.
     */
    phyRelNode = TopProjectVisitor.insertTopProject(phyRelNode, validatedRowType);

    /*
     * 1.) For select * from join query, we need insert project on top of scan and a top project just
     * under screen operator. The project on top of scan will rename from * to T1*, while the top project
     * will rename T1* to *, before it output the final result. Only the top project will allow
     * duplicate columns, since user could "explicitly" ask for duplicate columns ( select *, col, *).
     * The rest of projects will remove the duplicate column when we generate POP in json format.
     */
    phyRelNode = StarColumnConverter.insertRenameProject(phyRelNode);
    log("Physical RelNode after Top and Rename Project inserting: ", phyRelNode, logger, null);

    /*
     * 2.)
     * Join might cause naming conflicts from its left and right child.
     * In such case, we have to insert Project to rename the conflicting names.
     * Unnest operator might need to adjust the correlated field after the physical planning.
     */
    phyRelNode = AdjustOperatorsSchemaVisitor.adjustSchema(phyRelNode);

    /*
     * 2.1) Swap left / right for INNER hash join, if left's row count is < (1 + margin) right's row count.
     * We want to have smaller dataset on the right side, since hash table builds on right side.
     */
    if (context.getPlannerSettings().isHashJoinSwapEnabled()) {
      phyRelNode = SwapHashJoinVisitor.swapHashJoin(phyRelNode,
          context.getPlannerSettings().getHashJoinSwapMarginFactor());
    }

    /* Parquet row group filter pushdown in planning time */

    if (context.getPlannerSettings().isParquetRowGroupFilterPushdownPlanningEnabled()) {
      phyRelNode = (Prel) transform(PlannerType.HEP_BOTTOM_UP, PlannerPhase.PHYSICAL_PARTITION_PRUNING, phyRelNode);
    }

    /*
     * 2.2) Break up all expressions with complex outputs into their own project operations
     */
    phyRelNode = phyRelNode.accept(
        new SplitUpComplexExpressions(
            config.getConverter().getTypeFactory(),
            context.getPlannerSettings().functionImplementationRegistry,
            phyRelNode.getCluster().getRexBuilder()
        ),
        null);

    /*
     * 2.3) Projections that contain reference to flatten are rewritten as Flatten operators followed by Project
     */
    phyRelNode = phyRelNode.accept(
        new RewriteProjectToFlatten(config.getConverter().getTypeFactory(), context.getDrillOperatorTable()), null);

    /*
     * 3.)
     * Since our operators work via names rather than indices, we have to reorder any
     * output before we return data to the user as we may have accidentally shuffled things.
     * This adds a trivial project to reorder columns prior to output.
     */
    phyRelNode = FinalColumnReorderer.addFinalColumnOrdering(phyRelNode);

    /*
     * 4.)
     * If two fragments are both estimated to be parallelization one, remove the exchange
     * separating them.
     */
    phyRelNode = ExcessiveExchangeIdentifier.removeExcessiveExchanges(phyRelNode, targetSliceSize);

    /* Insert the IMPLICIT_COLUMN in the lateral unnest pipeline */
    phyRelNode = LateralUnnestRowIDVisitor.insertRowID(phyRelNode);

    /* 5.)
     * Add ProducerConsumer after each scan if the option is set
     * Use the configured queueSize
     */
    /* DRILL-1617 Disabling ProducerConsumer as it produces incorrect results
    if (context.getOptions().getOption(PlannerSettings.PRODUCER_CONSUMER.getOptionName()).bool_val) {
      long queueSize = context.getOptions().getOption(PlannerSettings.PRODUCER_CONSUMER_QUEUE_SIZE.getOptionName()).num_val;
      phyRelNode = ProducerConsumerPrelVisitor.addProducerConsumerToScans(phyRelNode, (int) queueSize);
    }
    */

    /* 6.)
     * if the client does not support complex types (Map, Repeated)
     * insert a project which would convert
     */
    if (!context.getSession().isSupportComplexTypes()) {
      logger.debug("Client does not support complex types, add ComplexToJson operator.");
      phyRelNode = ComplexToJsonPrelVisitor.addComplexToJsonPrel(phyRelNode);
    }

    /* 7.)
     * Insert LocalExchange (mux and/or demux) nodes
     */
    phyRelNode = InsertLocalExchangeVisitor.insertLocalExchanges(phyRelNode, queryOptions);

    /*
     * 8.)
     * Insert RuntimeFilter over Scan nodes
     */
    if (context.isRuntimeFilterEnabled()) {
      phyRelNode = RuntimeFilterVisitor.addRuntimeFilter(phyRelNode, context);
    }

    /* 9.)
     * Next, we add any required selection vector removers given the supported encodings of each
     * operator. This will ultimately move to a new trait but we're managing here for now to avoid
     * introducing new issues in planning before the next release
     */
    phyRelNode = SelectionVectorPrelVisitor.addSelectionRemoversWhereNecessary(phyRelNode);

    /*
     * 10.)
     * Insert project above the screen operator or writer to ensure that final output column names are preserved after all optimizations.
     */
    phyRelNode = TopProjectVisitor.insertTopProject(phyRelNode, validatedRowType);

    /* 11.)
     * Finally, Make sure that the no rels are repeats.
     * This could happen in the case of querying the same table twice as Optiq may canonicalize these.
     */
    phyRelNode = RelUniqifier.uniqifyGraph(phyRelNode);

    return phyRelNode;
  }

  protected PhysicalOperator convertToPop(Prel prel) throws IOException {
    PhysicalPlanCreator creator = new PhysicalPlanCreator(context, PrelSequencer.getIdMap(prel));
    PhysicalOperator op = prel.getPhysicalOperator(creator);
    return op;
  }

  protected PhysicalPlan convertToPlan(PhysicalOperator op) {
    PlanPropertiesBuilder propsBuilder = PlanProperties.builder();
    propsBuilder.type(PlanType.APACHE_DRILL_PHYSICAL);
    propsBuilder.version(1);
    propsBuilder.options(new JSONOptions(context.getOptions().getOptionList()));
    propsBuilder.resultMode(ResultMode.EXEC);
    propsBuilder.generator(this.getClass().getSimpleName(), "");
    PhysicalPlan plan =  new PhysicalPlan(propsBuilder.build(), getPops(op));
    return plan;

  }

  public static List<PhysicalOperator> getPops(PhysicalOperator root) {
    List<PhysicalOperator> ops = Lists.newArrayList();
    PopCollector c = new PopCollector();
    root.accept(c, ops);
    return ops;
  }

  private static class PopCollector extends
      AbstractPhysicalVisitor<Void, Collection<PhysicalOperator>, RuntimeException> {

    @Override
    public Void visitOp(PhysicalOperator op, Collection<PhysicalOperator> collection) throws RuntimeException {
      collection.add(op);
      for (PhysicalOperator o : op) {
        o.accept(this, collection);
      }
      return null;
    }

  }

  protected Pair<SqlNode, RelDataType> validateNode(SqlNode sqlNode) throws ValidationException, RelConversionException, ForemanSetupException {
    final SqlNode sqlNodeValidated = config.getConverter().validate(sqlNode);
    final Pair<SqlNode, RelDataType> typedSqlNode = new Pair<>(sqlNodeValidated, config.getConverter().getOutputType(
        sqlNodeValidated));

    // Check if the unsupported functionality is used
    UnsupportedOperatorsVisitor visitor = UnsupportedOperatorsVisitor.createVisitor(context);
    try {
      sqlNodeValidated.accept(visitor);
    } catch (UnsupportedOperationException ex) {
      // If the exception due to the unsupported functionalities
      visitor.convertException();

      // If it is not, let this exception move forward to higher logic
      throw ex;
    }

    return typedSqlNode;
  }

  private RelNode convertToRel(SqlNode node) {
    final RelNode convertedNode = config.getConverter().toRel(node).rel;
    log("INITIAL", convertedNode, logger, null);
    RelNode transformedNode = transform(PlannerType.HEP,
        PlannerPhase.SUBQUERY_REWRITE, convertedNode);

    RelNode decorrelatedNode = RelDecorrelator.decorrelateQuery(transformedNode,
        DrillRelFactories.LOGICAL_BUILDER.create(transformedNode.getCluster(), null));

    return transform(PlannerType.HEP, PlannerPhase.WINDOW_REWRITE, decorrelatedNode);
  }

  private RelNode preprocessNode(RelNode rel) throws SqlUnsupportedException {
    /*
     * Traverse the tree to do the following pre-processing tasks: 1. replace the convert_from, convert_to function to
     * actual implementations Eg: convert_from(EXPR, 'JSON') be converted to convert_fromjson(EXPR); TODO: Ideally all
     * function rewrites would move here instead of DrillOptiq.
     *
     * 2. see where the tree contains unsupported functions; throw SqlUnsupportedException if there is any.
     */

    PreProcessLogicalRel visitor = PreProcessLogicalRel.createVisitor(config.getConverter().getTypeFactory(),
        context.getDrillOperatorTable(),
        rel.getCluster().getRexBuilder());
    try {
      rel = rel.accept(visitor);
    } catch (UnsupportedOperationException ex) {
      visitor.convertException();
      throw ex;
    }

    // moves complex expressions below Uncollect to the right side of Correlate
    return ComplexUnnestVisitor.rewriteUnnestWithComplexExprs(rel);
  }

  protected DrillRel addRenamedProject(DrillRel rel, RelDataType validatedRowType) {
    RelDataType t = rel.getRowType();

    RexBuilder b = rel.getCluster().getRexBuilder();
    List<RexNode> projections = Lists.newArrayList();
    int projectCount = t.getFieldList().size();

    for (int i =0; i < projectCount; i++) {
      projections.add(b.makeInputRef(rel, i));
    }

    final List<String> fieldNames2 = SqlValidatorUtil.uniquify(
        validatedRowType.getFieldNames(),
        SqlValidatorUtil.EXPR_SUGGESTER,
        rel.getCluster().getTypeFactory().getTypeSystem().isSchemaCaseSensitive());

    RelDataType newRowType = RexUtil.createStructType(rel.getCluster().getTypeFactory(),
        projections, fieldNames2, null);

    DrillProjectRel topProj = DrillProjectRel.create(rel.getCluster(), rel.getTraitSet(), rel, projections, newRowType);

    // Add a final non-trivial Project to get the validatedRowType, if child is not project.
    if (rel instanceof Project && DrillRelOptUtil.isTrivialProject(topProj, true)) {
      return rel;
    } else{
      return topProj;
    }
  }

  public static class MetaDataProviderModifier extends RelShuttleImpl {
    private final RelMetadataProvider metadataProvider;

    public MetaDataProviderModifier(RelMetadataProvider metadataProvider) {
      this.metadataProvider = metadataProvider;
    }

    @Override
    public RelNode visit(TableScan scan) {
      scan.getCluster().setMetadataProvider(metadataProvider);
      return super.visit(scan);
    }

    @Override
    public RelNode visit(TableFunctionScan scan) {
      scan.getCluster().setMetadataProvider(metadataProvider);
      return super.visit(scan);
    }

    @Override
    public RelNode visit(LogicalValues values) {
      values.getCluster().setMetadataProvider(metadataProvider);
      return super.visit(values);
    }

    @Override
    protected RelNode visitChild(RelNode parent, int i, RelNode child) {
      child.accept(this);
      parent.getCluster().setMetadataProvider(metadataProvider);
      return parent;
    }
  }

  protected class ConvertedRelNode {
    private final RelNode relNode;
    private final RelDataType validatedRowType;

    public ConvertedRelNode(RelNode relNode, RelDataType validatedRowType) {
      this.relNode = relNode;
      this.validatedRowType = validatedRowType;
    }

    public RelNode getConvertedNode() {
      return this.relNode;
    }

    public RelDataType getValidatedRowType() {
      return this.validatedRowType;
    }
  }
}
