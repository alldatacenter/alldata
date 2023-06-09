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

package org.apache.drill.exec.planner.index.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollations;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.planner.common.JoinControl;
import org.apache.drill.exec.planner.index.IndexLogicalPlanCallContext;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.FunctionalIndexInfo;
import org.apache.drill.exec.planner.index.FunctionalIndexHelper;
import org.apache.drill.exec.planner.index.IndexPlanUtils;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait;
import org.apache.drill.exec.planner.physical.DrillDistributionTraitDef;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.HashJoinPrel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.Prule;
import org.apache.drill.exec.planner.physical.RowKeyJoinPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Generate a non-covering index plan that is equivalent to the original plan. The non-covering plan consists
 * of a join-back between an index lookup and the primary table. This join-back is performed using a rowkey join.
 * For the primary table, we use a restricted scan that allows doing skip-scan instead of sequential scan.
 *
 * Original Plan:
 *               Filter
 *                 |
 *            DBGroupScan
 *
 * New Plan:
 *
 *            RowKeyJoin
 *          /         \
 * Remainder Filter  Exchange
 *         |            |
 *   Restricted    Filter (with index columns only)
 *   DBGroupScan        |
 *                  IndexGroupScan
 *
 * This plan will be further optimized by the filter pushdown rule of the Index plugin which should
 * push the index column filters into the index scan.
 */
public class NonCoveringIndexPlanGenerator extends AbstractIndexPlanGenerator {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NonCoveringIndexPlanGenerator.class);
  final protected IndexGroupScan indexGroupScan;
  final private IndexDescriptor indexDesc;
  // Ideally This functionInfo should be cached along with indexDesc.
  final protected FunctionalIndexInfo functionInfo;

  public NonCoveringIndexPlanGenerator(IndexLogicalPlanCallContext indexContext,
                                       IndexDescriptor indexDesc,
                                       IndexGroupScan indexGroupScan,
                                       RexNode indexCondition,
                                       RexNode remainderCondition,
                                       RexBuilder builder,
                                       PlannerSettings settings) {
    super(indexContext, indexCondition, remainderCondition, builder, settings);
    this.indexGroupScan = indexGroupScan;
    this.indexDesc = indexDesc;
    this.functionInfo = indexDesc.getFunctionalInfo();
  }

  @Override
  public RelNode convertChild(final RelNode topRel, final RelNode input) throws InvalidRelException {

    if (indexGroupScan == null) {
      logger.error("Null indexgroupScan in NonCoveringIndexPlanGenerator.convertChild");
      return null;
    }

    RelDataType dbscanRowType = convertRowType(origScan.getRowType(), origScan.getCluster().getTypeFactory());
    RelDataType indexScanRowType = FunctionalIndexHelper.convertRowTypeForIndexScan(
        origScan, indexContext.getOrigMarker(), indexGroupScan, functionInfo);

    DrillDistributionTrait partition = IndexPlanUtils.scanIsPartition(IndexPlanUtils.getGroupScan(origScan))?
        DrillDistributionTrait.RANDOM_DISTRIBUTED : DrillDistributionTrait.SINGLETON;

    ScanPrel indexScanPrel = new ScanPrel(origScan.getCluster(),
        origScan.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(partition), indexGroupScan, indexScanRowType, origScan.getTable());
    DbGroupScan origDbGroupScan = (DbGroupScan)IndexPlanUtils.getGroupScan(origScan);

    // right (build) side of the rowkey join: do a distribution of project-filter-indexscan subplan
    RexNode convertedIndexCondition = FunctionalIndexHelper.convertConditionForIndexScan(indexCondition,
        origScan, indexScanRowType, builder, functionInfo);
    FilterPrel  rightIndexFilterPrel = new FilterPrel(indexScanPrel.getCluster(), indexScanPrel.getTraitSet(),
          indexScanPrel, convertedIndexCondition);

    double finalRowCount = indexGroupScan.getRowCount(indexContext.getOrigCondition(), origScan);

    // project the rowkey column from the index scan
    List<RexNode> rightProjectExprs = Lists.newArrayList();
    int rightRowKeyIndex = getRowKeyIndex(indexScanPrel.getRowType(), origScan);//indexGroupScan.getRowKeyOrdinal();
    assert rightRowKeyIndex >= 0;

    rightProjectExprs.add(RexInputRef.of(rightRowKeyIndex, indexScanPrel.getRowType()));

    final List<RelDataTypeField> indexScanFields = indexScanPrel.getRowType().getFieldList();

    final RelDataTypeFactory.FieldInfoBuilder rightFieldTypeBuilder =
        indexScanPrel.getCluster().getTypeFactory().builder();

    // build the row type for the right Project
    final RelDataTypeField rightRowKeyField = indexScanFields.get(rightRowKeyIndex);
    rightFieldTypeBuilder.add(rightRowKeyField);
    final RelDataType rightProjectRowType = rightFieldTypeBuilder.build();

    final ProjectPrel rightIndexProjectPrel = new ProjectPrel(indexScanPrel.getCluster(), indexScanPrel.getTraitSet(),
        rightIndexFilterPrel, rightProjectExprs, rightProjectRowType);

    // create a RANGE PARTITION on the right side (this could be removed later during ExcessiveExchangeIdentifier phase
    // if the estimated row count is smaller than slice_target
    final RelNode rangeDistRight = createRangeDistRight(rightIndexProjectPrel, rightRowKeyField, origDbGroupScan);

    // the range partitioning adds an extra column for the partition id but in the final plan we already have a
    // renaming Project for the _id field inserted as part of the JoinPrelRenameVisitor. Thus, we are not inserting
    // a separate Project here.
    final RelNode convertedRight = rangeDistRight;

    // left (probe) side of the rowkey join

    List<SchemaPath> cols = new ArrayList<SchemaPath>(origDbGroupScan.getColumns());
    if (!checkRowKey(cols)) {
      cols.add(origDbGroupScan.getRowKeyPath());
    }

    // Create a restricted groupscan from the primary table's groupscan
    DbGroupScan restrictedGroupScan  = (DbGroupScan)origDbGroupScan.getRestrictedScan(cols);
    if (restrictedGroupScan == null) {
      logger.error("Null restricted groupscan in NonCoveringIndexPlanGenerator.convertChild");
      return null;
    }
    // Set left side (restricted scan) row count as rows returned from right side (index scan)
    DrillScanRel rightIdxRel = new DrillScanRel(origScan.getCluster(), origScan.getTraitSet(),
        origScan.getTable(), origScan.getRowType(), indexContext.getScanColumns());
    double rightIdxRowCount = indexGroupScan.getRowCount(indexCondition, rightIdxRel);
    restrictedGroupScan.setRowCount(null, rightIdxRowCount, rightIdxRowCount);

    RelTraitSet origScanTraitSet = origScan.getTraitSet();
    RelTraitSet restrictedScanTraitSet = origScanTraitSet.plus(Prel.DRILL_PHYSICAL);

    // Create the collation traits for restricted scan based on the index columns under the
    // conditions that (a) the index actually has collation property (e.g hash indexes don't)
    // and (b) if an explicit sort operation is not enforced
    RelCollation collation = null;
    if (indexDesc.getCollation() != null &&
         !settings.isIndexForceSortNonCovering()) {
      collation = IndexPlanUtils.buildCollationNonCoveringIndexScan(indexDesc, indexScanRowType, dbscanRowType, indexContext);
      if (restrictedScanTraitSet.contains(RelCollationTraitDef.INSTANCE)) { // replace existing trait
        restrictedScanTraitSet = restrictedScanTraitSet.plus(partition).replace(collation);
      } else {  // add new one
        restrictedScanTraitSet = restrictedScanTraitSet.plus(partition).plus(collation);
      }
    }

    ScanPrel dbScan = new ScanPrel(origScan.getCluster(),
        restrictedScanTraitSet, restrictedGroupScan, dbscanRowType, origScan.getTable());
    RelNode lastLeft = dbScan;
    // build the row type for the left Project
    List<RexNode> leftProjectExprs = Lists.newArrayList();
    int leftRowKeyIndex = getRowKeyIndex(dbScan.getRowType(), origScan);
    final RelDataTypeField leftRowKeyField = dbScan.getRowType().getFieldList().get(leftRowKeyIndex);
    final RelDataTypeFactory.FieldInfoBuilder leftFieldTypeBuilder =
        dbScan.getCluster().getTypeFactory().builder();

    // We are applying the same index condition to primary table's restricted scan. The reason is, the index may be an async
    // index .. i.e it is not synchronously updated along with the primary table update as part of a single transaction, so it
    // is possible that after or during index scan, the primary table rows may have been updated and no longer satisfy the index
    // condition. By re-applying the index condition here, we will ensure non-qualifying records are filtered out.
    // The remainder condition will be applied on top of RowKeyJoin.
    FilterPrel leftIndexFilterPrel = null;
    if (indexDesc.isAsyncIndex()) {
      leftIndexFilterPrel = new FilterPrel(dbScan.getCluster(), dbScan.getTraitSet(),
            dbScan, indexContext.getOrigCondition());
      lastLeft = leftIndexFilterPrel;
    }

    RelDataType origRowType = origProject == null ? origScan.getRowType() : origProject.getRowType();

    if (origProject != null) {// then we also  don't need a project
      // new Project's rowtype is original Project's rowtype [plus rowkey if rowkey is not in original rowtype]
      List<RelDataTypeField> origProjFields = origRowType.getFieldList();
      leftFieldTypeBuilder.addAll(origProjFields);
      // get the exprs from the original Project

      leftProjectExprs.addAll(IndexPlanUtils.getProjects(origProject));
      // add the rowkey IFF rowkey is not in orig scan
      if (getRowKeyIndex(origRowType, origScan) < 0) {
        leftFieldTypeBuilder.add(leftRowKeyField);
        leftProjectExprs.add(RexInputRef.of(leftRowKeyIndex, dbScan.getRowType()));
      }

      final RelDataType leftProjectRowType = leftFieldTypeBuilder.build();

      //build collation in project
      if (!settings.isIndexForceSortNonCovering()){
        collation = IndexPlanUtils.buildCollationProject(leftProjectExprs, null, dbScan, functionInfo, indexContext);
      }

      final ProjectPrel leftIndexProjectPrel = new ProjectPrel(dbScan.getCluster(),
          collation != null ? dbScan.getTraitSet().plus(collation) : dbScan.getTraitSet(),
          leftIndexFilterPrel == null ? dbScan : leftIndexFilterPrel, leftProjectExprs, leftProjectRowType);
      lastLeft = leftIndexProjectPrel;
    }
    final RelTraitSet leftTraits = dbScan.getTraitSet().plus(Prel.DRILL_PHYSICAL);
    // final RelNode convertedLeft = convert(leftIndexProjectPrel, leftTraits);
    final RelNode convertedLeft = Prule.convert(lastLeft, leftTraits);

    // find the rowkey column on the left side of join
    final int leftRowKeyIdx = getRowKeyIndex(convertedLeft.getRowType(), origScan);
    final int rightRowKeyIdx = 0; // only rowkey field is being projected from right side

    assert leftRowKeyIdx >= 0;

    List<Integer> leftJoinKeys = ImmutableList.of(leftRowKeyIdx);
    List<Integer> rightJoinKeys = ImmutableList.of(rightRowKeyIdx);

    RexNode joinCondition =
        RelOptUtil.createEquiJoinCondition(convertedLeft, leftJoinKeys,
            convertedRight, rightJoinKeys, builder);

    RelNode newRel;
    if (settings.isIndexUseHashJoinNonCovering()) {
      //for hash join, collation will be cleared
      HashJoinPrel hjPrel = new HashJoinPrel(topRel.getCluster(), leftTraits, convertedLeft,
          convertedRight, joinCondition, JoinRelType.INNER, false /* no swap */,
          null /* no runtime filter */,
          true /* useful for join-restricted scans */, JoinControl.DEFAULT);
      newRel = hjPrel;
    } else {
      //if there is collation, add to rowkey join
      RowKeyJoinPrel rjPrel = new RowKeyJoinPrel(topRel.getCluster(),
          collation != null ? leftTraits.plus(collation) : leftTraits,
          convertedLeft, convertedRight, joinCondition, JoinRelType.INNER);

      rjPrel.setEstimatedRowCount(finalRowCount);
      newRel = rjPrel;
    }

    final RelDataTypeFactory.FieldInfoBuilder finalFieldTypeBuilder =
        origScan.getCluster().getTypeFactory().builder();

    List<RelDataTypeField> rjRowFields = newRel.getRowType().getFieldList();
    int toRemoveRowKeyCount = 1;
    if (getRowKeyIndex(origRowType, origScan)  < 0 ) {
      toRemoveRowKeyCount = 2;
    }
    finalFieldTypeBuilder.addAll(rjRowFields.subList(0, rjRowFields.size()-toRemoveRowKeyCount));
    final RelDataType finalProjectRowType = finalFieldTypeBuilder.build();

    List<RexNode> resetExprs = Lists.newArrayList();
    for (int idx=0; idx<rjRowFields.size()-toRemoveRowKeyCount; ++idx) {
      resetExprs.add(RexInputRef.of(idx, newRel.getRowType()));
    }

    //rewrite the collation for this projectPrel
    final ProjectPrel resetProjectPrel = new ProjectPrel(newRel.getCluster(), newRel.getTraitSet(),
        newRel, resetExprs, finalProjectRowType);
    newRel = resetProjectPrel;

    if ( upperProject != null) {
      RelCollation newCollation = RelCollations.of(RelCollations.EMPTY.getFieldCollations());
      DrillDistributionTrait newDist = null;

      newDist = upperProject.getInput().getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
      if (!settings.isIndexForceSortNonCovering()) {
        newCollation = IndexPlanUtils.buildCollationProject(IndexPlanUtils.getProjects(upperProject), origProject, origScan,
            functionInfo, indexContext);
      }
      RelTraitSet newProjectTraits = newTraitSet(Prel.DRILL_PHYSICAL, newDist, newCollation);
      ProjectPrel cap = new ProjectPrel(upperProject.getCluster(),
          newProjectTraits,
          newRel, IndexPlanUtils.getProjects(upperProject), upperProject.getRowType());
      newRel = cap;
    }

    //whether to remove sort
    if (indexContext.getSort() != null) {
      // When ordering is required, serialize the index scan side. With parallel index scans, the rowkey join may receive
      // unsorted input because ordering is not guaranteed across different parallel inputs.
      if (toRemoveSort(indexContext.getCollation(), newRel.getTraitSet().getTrait(RelCollationTraitDef.INSTANCE))) {
        ((IndexGroupScan)indexScanPrel.getGroupScan()).setParallelizationWidth(1);
      }
      newRel = getSortNode(indexContext, newRel, false,true, true);
      Preconditions.checkArgument(newRel != null);
    }

    RelNode finalRel = Prule.convert(newRel, newRel.getTraitSet());
    logger.debug("NonCoveringIndexPlanGenerator got finalRel {} from origScan {}",
        finalRel.toString(), origScan.toString());
    return finalRel;
  }
}

