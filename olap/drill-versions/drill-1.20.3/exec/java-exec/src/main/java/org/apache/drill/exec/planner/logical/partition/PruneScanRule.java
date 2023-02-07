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
package org.apache.drill.exec.planner.logical.partition;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.SelectionBasedTableScan;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.calcite.adapter.enumerable.EnumerableTableScan;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.LogicalValues;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.BitSets;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.fn.interpreter.InterpreterEvaluator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.FileGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.FileSystemPartitionDescriptor;
import org.apache.drill.exec.planner.PartitionDescriptor;
import org.apache.drill.exec.planner.PartitionLocation;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillValuesRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.drill.exec.store.dfs.MetadataContext.PruneStatus;
import org.apache.drill.exec.vector.NullableBitVector;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rex.RexNode;
import org.apache.commons.lang3.tuple.Pair;

import org.apache.drill.exec.vector.ValueVector;
import org.apache.hadoop.fs.Path;

public abstract class PruneScanRule extends StoragePluginOptimizerRule {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PruneScanRule.class);

  final OptimizerRulesContext optimizerContext;

  public PruneScanRule(RelOptRuleOperand operand, String id, OptimizerRulesContext optimizerContext) {
    super(operand, id);
    this.optimizerContext = optimizerContext;
  }

  private static class DirPruneScanFilterOnProjectRule extends PruneScanRule {
    public DirPruneScanFilterOnProjectRule(OptimizerRulesContext optimizerRulesContext) {
      super(RelOptHelper.some(Filter.class, RelOptHelper.some(Project.class, RelOptHelper.any(TableScan.class))), "DirPruneScanRule:Filter_On_Project", optimizerRulesContext);
    }

    @Override
    public PartitionDescriptor getPartitionDescriptor(PlannerSettings settings, TableScan scanRel) {
      return new FileSystemPartitionDescriptor(settings, scanRel);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final TableScan scan = call.rel(2);
      return isQualifiedDirPruning(scan);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      final Filter filterRel = call.rel(0);
      final Project projectRel = call.rel(1);
      final TableScan scanRel = call.rel(2);
      doOnMatch(call, filterRel, projectRel, scanRel);
    }
  }

  private static class DirPruneScanFilterOnScanRule extends PruneScanRule {
    public DirPruneScanFilterOnScanRule(OptimizerRulesContext optimizerRulesContext) {
      super(RelOptHelper.some(Filter.class, RelOptHelper.any(TableScan.class)), "DirPruneScanRule:Filter_On_Scan", optimizerRulesContext);
    }

    @Override
    public PartitionDescriptor getPartitionDescriptor(PlannerSettings settings, TableScan scanRel) {
      return new FileSystemPartitionDescriptor(settings, scanRel);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final TableScan scan = call.rel(1);
      return isQualifiedDirPruning(scan);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      final Filter filterRel = call.rel(0);
      final TableScan scanRel = call.rel(1);
      doOnMatch(call, filterRel, null, scanRel);
    }
  }

  public static RelOptRule getDirFilterOnProject(OptimizerRulesContext optimizerRulesContext) {
    return new DirPruneScanFilterOnProjectRule(optimizerRulesContext);
  }

  public static RelOptRule getDirFilterOnScan(OptimizerRulesContext optimizerRulesContext) {
    return new DirPruneScanFilterOnScanRule(optimizerRulesContext);
  }

  public static RelOptRule getConvertAggScanToValuesRule(OptimizerRulesContext optimizerRulesContext) {
    return new ConvertAggScanToValuesRule(optimizerRulesContext);
  }

  protected void doOnMatch(RelOptRuleCall call, Filter filterRel, Project projectRel, TableScan scanRel) {

    final String pruningClassName = getClass().getName();
    logger.debug("Beginning partition pruning, pruning class: {}", pruningClassName);
    Stopwatch totalPruningTime = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;

    final PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());
    PartitionDescriptor descriptor = getPartitionDescriptor(settings, scanRel);
    final BufferAllocator allocator = optimizerContext.getAllocator();

    final Object selection = DrillRelOptUtil.getDrillTable(scanRel).getSelection();
    MetadataContext metaContext = null;
    if (selection instanceof FormatSelection) {
         metaContext = ((FormatSelection)selection).getSelection().getMetaContext();
    }

    RexNode condition;
    if (projectRel == null) {
      condition = filterRel.getCondition();
    } else {
      // get the filter as if it were below the projection.
      condition = RelOptUtil.pushPastProject(filterRel.getCondition(), projectRel);
    }

    RewriteAsBinaryOperators visitor = new RewriteAsBinaryOperators(true, filterRel.getCluster().getRexBuilder());
    condition = condition.accept(visitor);

    Map<Integer, String> fieldNameMap = new HashMap<>();
    List<String> fieldNames = scanRel.getRowType().getFieldNames();
    BitSet columnBitset = new BitSet();
    BitSet partitionColumnBitSet = new BitSet();
    Map<Integer, Integer> partitionMap = new HashMap<>();

    int relColIndex = 0;
    for (String field : fieldNames) {
      final Integer partitionIndex = descriptor.getIdIfValid(field);
      if (partitionIndex != null) {
        fieldNameMap.put(partitionIndex, field);
        partitionColumnBitSet.set(partitionIndex);
        columnBitset.set(relColIndex);
        // mapping between the relColIndex and partitionIndex
        partitionMap.put(relColIndex, partitionIndex);
      }
      relColIndex++;
    }

    if (partitionColumnBitSet.isEmpty()) {
      if (totalPruningTime != null) {
        logger.debug("No partition columns are projected from the scan..continue. Total pruning elapsed time: {} ms",
            totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
      }
      setPruneStatus(metaContext, PruneStatus.NOT_PRUNED);
      return;
    }

    // stop watch to track how long we spend in different phases of pruning
    // first track how long we spend building the filter tree
    Stopwatch miscTimer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;

    FindPartitionConditions c = new FindPartitionConditions(columnBitset, filterRel.getCluster().getRexBuilder());
    c.analyze(condition);
    RexNode pruneCondition = c.getFinalCondition();
    BitSet referencedDirsBitSet = c.getReferencedDirs();

    if (miscTimer != null) {
      logger.debug("Total elapsed time to build and analyze filter tree: {} ms", miscTimer.elapsed(TimeUnit.MILLISECONDS));
      miscTimer.reset();
    }

    if (pruneCondition == null) {
      if (totalPruningTime != null) {
        logger.debug("No conditions were found eligible for partition pruning. Total pruning elapsed time: {} ms",
            totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
      }
      setPruneStatus(metaContext, PruneStatus.NOT_PRUNED);
      return;
    }

    // set up the partitions
    List<PartitionLocation> newPartitions = new ArrayList<>();
    long numTotal = 0; // total number of partitions
    int batchIndex = 0;
    PartitionLocation firstLocation = null;
    LogicalExpression materializedExpr = null;
    String[] spInfo = null;
    int maxIndex = -1;
    BitSet matchBitSet = new BitSet();

    // Outer loop: iterate over a list of batches of PartitionLocations
    for (List<PartitionLocation> partitions : descriptor) {
      numTotal += partitions.size();
      logger.debug("Evaluating partition pruning for batch {}", batchIndex);
      if (batchIndex == 0) { // save the first location in case everything is pruned
        firstLocation = partitions.get(0);
      }
      final NullableBitVector output = new NullableBitVector(MaterializedField.create("", Types.optional(MinorType.BIT)), allocator);
      final VectorContainer container = new VectorContainer();

      try {
        final ValueVector[] vectors = new ValueVector[descriptor.getMaxHierarchyLevel()];
        for (int partitionColumnIndex : BitSets.toIter(partitionColumnBitSet)) {
          SchemaPath column = SchemaPath.getSimplePath(fieldNameMap.get(partitionColumnIndex));
          // ParquetPartitionDescriptor.populatePruningVector() expects nullable value vectors,
          // so force nullability here to avoid class cast exceptions
          MajorType type = descriptor.getVectorType(column, settings).toBuilder().setMode(TypeProtos.DataMode.OPTIONAL).build();
          MaterializedField field = MaterializedField.create(column.getLastSegment().getNameSegment().getPath(), type);
          ValueVector v = TypeHelper.getNewVector(field, allocator);
          v.allocateNew();
          vectors[partitionColumnIndex] = v;
          container.add(v);
        }

        if (miscTimer != null) {
          // track how long we spend populating partition column vectors
          miscTimer.start();
        }

        // populate partition vectors.
        descriptor.populatePartitionVectors(vectors, partitions, partitionColumnBitSet, fieldNameMap);

        if (miscTimer != null) {
          logger.debug("Elapsed time to populate partitioning column vectors: {} ms within batchIndex: {}",
              miscTimer.elapsed(TimeUnit.MILLISECONDS), batchIndex);
          miscTimer.reset();
        }

        // materialize the expression; only need to do this once
        if (batchIndex == 0) {
          materializedExpr = materializePruneExpr(pruneCondition, settings, scanRel, container);
          if (materializedExpr == null) {
            // continue without partition pruning; no need to log anything here since
            // materializePruneExpr logs it already
            if (totalPruningTime != null) {
              logger.debug("Total pruning elapsed time: {} ms", totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
            }
            setPruneStatus(metaContext, PruneStatus.NOT_PRUNED);
            return;
          }
        }

        output.allocateNew(partitions.size());

        if (miscTimer != null) {
          // start the timer to evaluate how long we spend in the interpreter evaluation
          miscTimer.start();
        }

        InterpreterEvaluator.evaluate(partitions.size(), optimizerContext, container, output, materializedExpr);

        if (miscTimer != null) {
          logger.debug("Elapsed time in interpreter evaluation: {} ms within batchIndex: {} with # of partitions : {}",
              miscTimer.elapsed(TimeUnit.MILLISECONDS), batchIndex, partitions.size());
          miscTimer.reset();
        }

        int recordCount = 0;
        int qualifiedCount = 0;

        if (descriptor.supportsMetadataCachePruning() &&
            partitions.get(0).isCompositePartition() /* apply single partition check only for composite partitions */) {
          // Inner loop: within each batch iterate over the PartitionLocations
          for (PartitionLocation part : partitions) {
            assert part.isCompositePartition();
            if(!output.getAccessor().isNull(recordCount) && output.getAccessor().get(recordCount) == 1) {
              newPartitions.add(part);
              // Rather than using the PartitionLocation, get the array of partition values for the directories that are
              // referenced by the filter since we are not interested in directory references in other parts of the query.
              Pair<String[], Integer> p = composePartition(referencedDirsBitSet, partitionMap, vectors, recordCount);
              String[] parts = p.getLeft();
              int tmpIndex = p.getRight();
              maxIndex = Math.max(maxIndex, tmpIndex);
              if (spInfo == null) { // initialization
                spInfo = parts;
                for (int j = 0; j <= tmpIndex; j++) {
                  if (parts[j] != null) {
                    matchBitSet.set(j);
                  }
                }
              } else {
                // compare the new partition with existing partition
                for (int j=0; j <= tmpIndex; j++) {
                  if (parts[j] == null || spInfo[j] == null) { // nulls don't match
                    matchBitSet.clear(j);
                  } else {
                    if (!parts[j].equals(spInfo[j])) {
                      matchBitSet.clear(j);
                    }
                  }
                }
              }
              qualifiedCount++;
            }
            recordCount++;
          }
        } else {
          // Inner loop: within each batch iterate over the PartitionLocations
          for(PartitionLocation part: partitions){
            if(!output.getAccessor().isNull(recordCount) && output.getAccessor().get(recordCount) == 1) {
              newPartitions.add(part);
              qualifiedCount++;
            }
            recordCount++;
          }
        }
        logger.debug("Within batch {}: total records: {}, qualified records: {}", batchIndex, recordCount, qualifiedCount);
        batchIndex++;
      } catch (Exception e) {
        logger.warn("Exception while trying to prune partition.", e);
        if (totalPruningTime != null) {
          logger.debug("Total pruning elapsed time: {} ms", totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
        }

        setPruneStatus(metaContext, PruneStatus.NOT_PRUNED);
        return; // continue without partition pruning
      } finally {
        container.clear();
        if (output != null) {
          output.clear();
        }
      }
    }

    try {
      if (newPartitions.size() == numTotal) {
        logger.debug("No partitions were eligible for pruning");
        return;
      }

      // handle the case all partitions are filtered out.
      boolean canDropFilter = true;
      boolean wasAllPartitionsPruned = false;
      Path cacheFileRoot = null;

      if (newPartitions.isEmpty()) {
        assert firstLocation != null;
        // Add the first non-composite partition location, since execution requires schema.
        // In such case, we should not drop filter.
        newPartitions.add(firstLocation.getPartitionLocationRecursive().get(0));
        canDropFilter = false;
        // NOTE: with DRILL-4530, the PruneScanRule may be called with only a list of
        // directories first and the non-composite partition location will still return
        // directories, not files.  So, additional processing is done depending on this flag
        wasAllPartitionsPruned = true;
        logger.debug("All {} partitions were pruned; added back a single partition to allow creating a schema", numTotal);

        // set the cacheFileRoot appropriately
        if (firstLocation.isCompositePartition()) {
          cacheFileRoot = Path.mergePaths(descriptor.getBaseTableLocation(), firstLocation.getCompositePartitionPath());
        }
      }

      logger.debug("Pruned {} partitions down to {}", numTotal, newPartitions.size());

      List<RexNode> conjuncts = RelOptUtil.conjunctions(condition);
      List<RexNode> pruneConjuncts = RelOptUtil.conjunctions(pruneCondition);
      conjuncts.removeAll(pruneConjuncts);
      RexNode newCondition = RexUtil.composeConjunction(filterRel.getCluster().getRexBuilder(), conjuncts, false);

      RewriteCombineBinaryOperators reverseVisitor = new RewriteCombineBinaryOperators(true, filterRel.getCluster().getRexBuilder());

      condition = condition.accept(reverseVisitor);
      pruneCondition = pruneCondition.accept(reverseVisitor);

      if (descriptor.supportsMetadataCachePruning() && !wasAllPartitionsPruned) {
        // if metadata cache file could potentially be used, then assign a proper cacheFileRoot
        int index = -1;
        if (!matchBitSet.isEmpty()) {
          StringBuilder path = new StringBuilder();
          index = matchBitSet.length() - 1;

          for (int j = 0; j < matchBitSet.length(); j++) {
            if (!matchBitSet.get(j)) {
              // stop at the first index with no match and use the immediate
              // previous index
              index = j-1;
              break;
            }
          }
          for (int j = 0; j <= index; j++) {
            path.append("/")
                .append(spInfo[j]);
          }
          cacheFileRoot = Path.mergePaths(descriptor.getBaseTableLocation(),
              DrillFileSystemUtil.createPathSafe(path.toString()));
        }
        if (index != maxIndex) {
          // if multiple partitions are being selected, we should not drop the filter
          // since we are reading the cache file at a parent/ancestor level
          canDropFilter = false;
        }

      }

      RelNode inputRel = descriptor.supportsMetadataCachePruning() ?
          descriptor.createTableScan(newPartitions, cacheFileRoot, wasAllPartitionsPruned, metaContext) :
            descriptor.createTableScan(newPartitions, wasAllPartitionsPruned);

      if (projectRel != null) {
        inputRel = projectRel.copy(projectRel.getTraitSet(), Collections.singletonList(inputRel));
      }

      if (newCondition.isAlwaysTrue() && canDropFilter) {
        call.transformTo(inputRel);
      } else {
        final RelNode newFilter = filterRel.copy(filterRel.getTraitSet(), Collections.singletonList(inputRel));
        call.transformTo(newFilter);
      }

      setPruneStatus(metaContext, PruneStatus.PRUNED);

    } catch (Exception e) {
      logger.warn("Exception while using the pruned partitions.", e);
    } finally {
      if (totalPruningTime != null) {
        logger.debug("Total pruning elapsed time: {} ms", totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
      }
    }
  }

  /** Compose the array of partition values for the directories that are referenced by filter:
   *  e.g suppose the dir hierarchy is year/quarter/month and the query is:
   *     SELECT * FROM T WHERE dir0=2015 AND dir1 = 'Q1',
   * then for 2015/Q1/Feb, this will have ['2015', 'Q1', null]
   * If the query filter condition is WHERE dir1 = 'Q2'  (i.e no dir0 condition) then the array will
   * have [null, 'Q2', null]
   */
  private Pair<String[], Integer> composePartition(BitSet referencedDirsBitSet,
      Map<Integer, Integer> partitionMap,
      ValueVector[] vectors,
      int recordCount) {
    String[] partition = new String[vectors.length];
    int maxIndex = -1;
    for (int referencedDirsIndex : BitSets.toIter(referencedDirsBitSet)) {
      int partitionColumnIndex = partitionMap.get(referencedDirsIndex);
      ValueVector vv = vectors[partitionColumnIndex];
      if (vv.getAccessor().getValueCount() > 0 &&
          vv.getAccessor().getObject(recordCount) != null) {
        String value = vv.getAccessor().getObject(recordCount).toString();
        partition[partitionColumnIndex] = value;
        maxIndex = Math.max(maxIndex, partitionColumnIndex);
      }
    }
    return Pair.of(partition, maxIndex);
  }

  protected LogicalExpression materializePruneExpr(RexNode pruneCondition,
      PlannerSettings settings,
      RelNode scanRel,
      VectorContainer container
      ) {
    // materialize the expression
    logger.debug("Attempting to prune {}", pruneCondition);
    final LogicalExpression expr = DrillOptiq.toDrill(new DrillParseContext(settings), scanRel, pruneCondition);
    final ErrorCollectorImpl errors = new ErrorCollectorImpl();

    LogicalExpression materializedExpr = ExpressionTreeMaterializer.materialize(expr, container, errors, optimizerContext.getFunctionRegistry());
    // Make sure pruneCondition's materialized expression is always of BitType, so that
    // it's same as the type of output vector.
    if (materializedExpr.getMajorType().getMode() == TypeProtos.DataMode.REQUIRED) {
      materializedExpr = ExpressionTreeMaterializer.convertToNullableType(
          materializedExpr,
          materializedExpr.getMajorType().getMinorType(),
          optimizerContext.getFunctionRegistry(),
          errors);
    }

    if (errors.getErrorCount() != 0) {
      logger.warn("Failure while materializing expression [{}].  Errors: {}", expr, errors);
      return null;
    }
    return materializedExpr;
  }

  protected OptimizerRulesContext getOptimizerRulesContext() {
    return optimizerContext;
  }

  public abstract PartitionDescriptor getPartitionDescriptor(PlannerSettings settings, TableScan scanRel);

  private static boolean isQualifiedDirPruning(final TableScan scan) {
    if (scan instanceof EnumerableTableScan) {
      final Object selection = DrillRelOptUtil.getDrillTable(scan).getSelection();
      if (selection instanceof FormatSelection
          && ((FormatSelection)selection).supportsDirPruning()) {
        return true;  // Do directory-based pruning in Calcite logical
      } else {
        return false; // Do not do directory-based pruning in Calcite logical
      }
    } else if (scan instanceof DrillScanRel) {
      final GroupScan groupScan = ((DrillScanRel) scan).getGroupScan();
      // this rule is applicable only for dfs based partition pruning in Drill Logical
      return groupScan instanceof FileGroupScan && groupScan.supportsPartitionFilterPushdown() && !((DrillScanRel)scan).partitionFilterPushdown();
    }
    return false;
  }

  private static void setPruneStatus(MetadataContext metaContext, PruneStatus pruneStatus) {
    if (metaContext != null) {
      metaContext.setPruneStatus(pruneStatus);
    }
  }

  /**
   * A rule which converts {@link Aggregate} on {@link TableScan} with directory columns
   * (see {@link org.apache.drill.exec.ExecConstants#FILESYSTEM_PARTITION_COLUMN_LABEL}) only
   * into {@link DrillValuesRel} to avoid scanning at all.
   *
   * Resulting {@link DrillValuesRel} will be populated with constant literals obtained from:
   * <ol>
   *   <li>metadata directory file if it exists</li>
   *   <li>or from file selection</li>
   * </ol>
   */
  private static class ConvertAggScanToValuesRule extends PruneScanRule {

    private final Pattern dirPattern;

    private ConvertAggScanToValuesRule(OptimizerRulesContext optimizerRulesContext) {
      super(RelOptHelper.some(Aggregate.class, DrillRel.DRILL_LOGICAL, RelOptHelper.any(TableScan.class)),
          "PartitionColumnScanPruningRule:Prune_On_Scan", optimizerRulesContext);
      String partitionColumnLabel = optimizerRulesContext.getPlannerSettings().getFsPartitionColumnLabel();
      dirPattern = Pattern.compile(partitionColumnLabel + "\\d+");
    }

    @Override
    public PartitionDescriptor getPartitionDescriptor(PlannerSettings settings, TableScan scanRel) {
      return new FileSystemPartitionDescriptor(settings, scanRel);
    }

    // Checks if query references directory columns only and has DISTINCT or GROUP BY operation
    @Override
    public boolean matches(RelOptRuleCall call) {
      Aggregate aggregate = call.rel(0);
      TableScan scan = call.rel(1);

      if (!isQualifiedFilePruning(scan)
          || scan.getRowType().getFieldCount() != aggregate.getRowType().getFieldCount()) {
        return false;
      }

      List<String> fieldNames = scan.getRowType().getFieldNames();
      // Check if select contains partition columns (dir0, dir1, dir2,..., dirN) only
      for (String field : fieldNames) {
        if (!dirPattern.matcher(field).matches()) {
          return false;
        }
      }

      return scan.isDistinct() || aggregate.getGroupCount() > 0;
    }

    /*
      Transforms Scan node to DrillValuesRel node to avoid unnecessary scanning of selected files.
      If cache metadata directory file exists, directory columns will be read from it,
      otherwise directories will be gathered from selection (PartitionLocations).
      DrillValuesRel will contain gathered constant literals.

      For example, plan for "select dir0, dir1 from `t` group by 1, 2", where table `t` has directory structure year/quarter

      00-00    Screen
      00-01      Project(dir0=[$0], dir1=[$1])
      00-02        HashAgg(group=[{0, 1}])
      00-03          Scan(table=[[t]], groupscan=[ParquetGroupScan [entries=[ReadEntryWithPath [path=file:/path/t/1996/Q4/orders_96_q4.parquet],
        ReadEntryWithPath [path=file:/path/t/1996/Q1/file_96_q1.parquet], ReadEntryWithPath [path=file:/path/t/1996/Q3/file_96_q3.parquet],
        ReadEntryWithPath [path=file:/path/t/1996/Q2/file_96_q2.parquet], ReadEntryWithPath [path=file:/path/t/1994/Q4/file_94_q4.parquet],
        ReadEntryWithPath [path=file:/path/t/1994/Q1/file_94_q1.parquet], ReadEntryWithPath [path=file:/path/t/1994/Q3/file_94_q3.parquet],
        ReadEntryWithPath [path=file:/path/t/1994/Q2/file_94_q2.parquet], ReadEntryWithPath [path=file:/path/t/1995/Q4/file_95_q4.parquet],
        ReadEntryWithPath [path=file:/path/t/1995/Q1/file_95_q1.parquet], ReadEntryWithPath [path=file:/path/t/1995/Q3/file_95_q3.parquet],
        ReadEntryWithPath [path=file:/path/t/1995/Q2/file_95_q2.parquet]], selectionRoot=file:/path/t, ..., columns=[`dir0`, `dir1`]]])

      will be changed to

      00-00    Screen
      00-01      Project(dir0=[$0], dir1=[$1])
      00-02        HashAgg(group=[{0, 1}])
      00-03          Values(tuples=[[{ '1995', 'Q1' }, { '1994', 'Q4' }, { '1996', 'Q3' }, { '1996', 'Q2' }, { '1994', 'Q2' },
        { '1995', 'Q4' }, { '1996', 'Q1' }, { '1995', 'Q3' }, { '1996', 'Q4' }, { '1994', 'Q3' }, { '1994', 'Q1' }, { '1995', 'Q2' }]])
     */
    @Override
    public void onMatch(RelOptRuleCall call) {
      TableScan scan = call.rel(1);

      String pruningClassName = getClass().getName();
      logger.debug("Beginning file partition pruning, pruning class: {}", pruningClassName);
      Stopwatch totalPruningTime = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;

      Object selection = DrillRelOptUtil.getDrillTable(scan).getSelection();
      MetadataContext metaContext = null;
      FileSelection fileSelection = null;
      if (selection instanceof FormatSelection) {
        fileSelection = ((FormatSelection) selection).getSelection();
        metaContext = fileSelection.getMetaContext();
      }

      PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());
      PartitionDescriptor descriptor = getPartitionDescriptor(settings, scan);

      List<String> fieldNames = scan.getRowType().getFieldNames();
      List<String> values = Collections.emptyList();
      List<Integer> indexes = new ArrayList<>(fieldNames.size());
      for (String field : fieldNames) {
        int index = descriptor.getPartitionHierarchyIndex(field);
        indexes.add(index);
      }

      if (metaContext != null && metaContext.getDirectories() != null) {
        // Dir metadata cache file exists
        logger.debug("Using Metadata Directories cache");
        values = getValues(fileSelection.getSelectionRoot(), metaContext.getDirectories(), indexes);
      }

      if (values.isEmpty()) {
        logger.debug("Not using Metadata Directories cache");
        int batchIndex = 0;
        // Outer loop: iterate over a list of batches of PartitionLocations
        values = new ArrayList<>();
        for (List<PartitionLocation> partitions : descriptor) {
          logger.debug("Evaluating file partition pruning for batch {}", batchIndex);

          try {
            values.addAll(getValues(partitions, indexes));
          } catch (Exception e) {
            logger.warn("Exception while trying to prune files.", e);
            if (totalPruningTime != null) {
              logger.debug("Total pruning elapsed time: {} ms", totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
            }

            // continue without partition pruning
            return;
          }
          batchIndex++;
        }

        if (values.isEmpty()) {
          // No changes are required
          return;
        }
      }

      try {
        // Transform Scan node to DrillValuesRel node
        List<RelDataTypeField> typeFields = new ArrayList<>(fieldNames.size());
        RelDataTypeFactory typeFactory = scan.getCluster().getTypeFactory();

        int i = 0;
        for (String field : fieldNames) {
          RelDataType dataType = typeFactory.createTypeWithNullability(
              typeFactory.createSqlType(SqlTypeName.VARCHAR, Types.MAX_VARCHAR_LENGTH), true);
          typeFields.add(new RelDataTypeFieldImpl(field, i++, dataType));
        }
        RelRecordType t = new RelRecordType(scan.getRowType().getStructKind(), typeFields);
        RelNode newInput = DrillRelFactories.LOGICAL_BUILDER.create(scan.getCluster(), null)
            .values(t, values.toArray())
            .build();

        RelTraitSet traits = newInput.getTraitSet().plus(DrillRel.DRILL_LOGICAL);
        newInput = new DrillValuesRel(
            newInput.getCluster(),
            newInput.getRowType(),
            ((LogicalValues) newInput).getTuples(), traits
        );

        Aggregate aggregate = call.rel(0);
        Aggregate newAggregate = aggregate.copy(
            aggregate.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
            newInput,
            aggregate.getGroupSet(),
            aggregate.getGroupSets(),
            aggregate.getAggCallList()
        );
        call.transformTo(newAggregate);
      } catch (Exception e) {
        logger.warn("Exception while using the pruned partitions.", e);
      } finally {
        if (totalPruningTime != null) {
          logger.debug("Total pruning elapsed time: {} ms", totalPruningTime.elapsed(TimeUnit.MILLISECONDS));
        }
      }
    }

    private List<String> getValues(Path selectionRoot, List<Path> directories, List<Integer> indexes) {
      List<String> values = new ArrayList<>();
      for (Path dir : directories) {
        List<String> parts = ColumnExplorer.listPartitionValues(dir, selectionRoot, true);
        for (int index : indexes) {
          if (index < parts.size()) {
            values.add(parts.get(index));
          } else {
            // No partition value for given index - set null value
            values.add(null);
          }
        }
      }

      return values;
    }

    private List<String> getValues(List<PartitionLocation> partitions, List<Integer> indexes) {
      List<String> values = new ArrayList<>(partitions.size() * indexes.size());
      partitions.forEach(partition -> indexes.forEach(
          index -> values.add(partition.getPartitionValue(index)))
      );
      return values;
    }

    private static boolean isQualifiedFilePruning(final TableScan scan) {
      if (scan instanceof EnumerableTableScan) {
        Object selection = DrillRelOptUtil.getDrillTable(scan).getSelection();
        return selection instanceof FormatSelection;
      } else if (scan instanceof DrillScanRel) {
        GroupScan groupScan = ((DrillScanRel) scan).getGroupScan();
        // this rule is applicable only for dfs based partition pruning in Drill Logical
        return groupScan instanceof FileGroupScan;
      }
      return false;
    }
  }

  private static boolean supportsScan(TableScan scan) {
    return scan instanceof SelectionBasedTableScan;
  }
}
