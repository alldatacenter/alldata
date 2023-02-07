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

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.planner.index.IndexCallContext;
import org.apache.drill.exec.planner.index.IndexPlanUtils;
import org.apache.drill.exec.planner.logical.DrillFilterRel;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillSortRel;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.exec.planner.common.OrderedRel;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.physical.SubsetTransformer;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.Prule;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.HashToMergeExchangePrel;
import org.apache.drill.exec.planner.physical.SingleMergeExchangePrel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.TopNPrel;
import org.apache.drill.exec.planner.physical.SortPrel;
import org.apache.drill.exec.planner.physical.LimitPrel;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;

public abstract class AbstractIndexPlanGenerator extends SubsetTransformer<RelNode, InvalidRelException>{

  private static final Logger logger = LoggerFactory.getLogger(AbstractIndexPlanGenerator.class);

  final protected DrillProjectRelBase origProject;
  final protected DrillScanRelBase origScan;
  final protected DrillProjectRelBase upperProject;

  final protected RexNode indexCondition;
  final protected RexNode remainderCondition;
  final protected RexBuilder builder;
  final protected IndexCallContext indexContext;
  final protected PlannerSettings settings;

  public AbstractIndexPlanGenerator(IndexCallContext indexContext,
      RexNode indexCondition,
      RexNode remainderCondition,
      RexBuilder builder,
      PlannerSettings settings) {
    super(indexContext.getCall());
    this.origProject = indexContext.getLowerProject();
    this.origScan = indexContext.getScan();
    this.upperProject = indexContext.getUpperProject();
    this.indexCondition = indexCondition;
    this.remainderCondition = remainderCondition;
    this.indexContext = indexContext;
    this.builder = builder;
    this.settings = settings;
  }

  // Provides the utility functions that don't rely on index(one or multiple) or final plan (covering or not),
  // but those helper functions that focus on serving building index plan (project-filter-indexscan)

  public static int getRowKeyIndex(RelDataType rowType, DrillScanRelBase origScan) {
    List<String> fieldNames = rowType.getFieldNames();
    int idx = 0;
    for (String field : fieldNames) {
      if (field.equalsIgnoreCase(((DbGroupScan)IndexPlanUtils.getGroupScan(origScan)).getRowKeyName())) {
        return idx;
      }
      idx++;
    }
    return -1;
  }

  protected RelDataType convertRowType(RelDataType origRowType, RelDataTypeFactory typeFactory) {
    if ( getRowKeyIndex(origRowType, origScan)>=0 ) { // row key already present
      return origRowType;
    }
    List<RelDataTypeField> fields = new ArrayList<>();

    fields.addAll(origRowType.getFieldList());
    fields.add(new RelDataTypeFieldImpl(
        ((DbGroupScan)IndexPlanUtils.getGroupScan(origScan)).getRowKeyName(), fields.size(),
            typeFactory.createSqlType(SqlTypeName.ANY)));
    return new RelRecordType(fields);
  }

  protected boolean checkRowKey(List<SchemaPath> columns) {
    for (SchemaPath s : columns) {
      if (s.equals(((DbGroupScan)IndexPlanUtils.getGroupScan(origScan)).getRowKeyPath())) {
        return true;
      }
    }
    return false;
  }

  // Range distribute the right side of the join, on row keys using a range partitioning function
  protected RelNode createRangeDistRight(final RelNode rightPrel,
                                         final RelDataTypeField rightRowKeyField,
                                         final DbGroupScan origDbGroupScan) {

    List<DrillDistributionTrait.DistributionField> rangeDistFields =
        Lists.newArrayList(new DrillDistributionTrait.DistributionField(0 /* rowkey ordinal on the right side */));

    FieldReference rangeDistRef = FieldReference.getWithQuotedRef(rightRowKeyField.getName());
    List<FieldReference> rangeDistRefList = Lists.newArrayList();
    rangeDistRefList.add(rangeDistRef);

    final DrillDistributionTrait distRight;
    if (IndexPlanUtils.scanIsPartition(origDbGroupScan)) {
      distRight = new DrillDistributionTrait(
          DrillDistributionTrait.DistributionType.RANGE_DISTRIBUTED,
          ImmutableList.copyOf(rangeDistFields),
          origDbGroupScan.getRangePartitionFunction(rangeDistRefList));
    }
    else {
      distRight = DrillDistributionTrait.SINGLETON;
    }

    RelTraitSet rightTraits = newTraitSet(distRight).plus(Prel.DRILL_PHYSICAL);
    RelNode convertedRight = Prule.convert(rightPrel, rightTraits);

    return convertedRight;
  }

  @Override
  public RelTraitSet newTraitSet(RelTrait... traits) {
    RelTraitSet set = indexContext.getCall().getPlanner().emptyTraitSet();
    for (RelTrait t : traits) {
      if (t != null) {
        set = set.plus(t);
      }
    }
    return set;
  }

  protected static boolean toRemoveSort(RelCollation sortCollation, RelCollation inputCollation) {
    return (inputCollation != null) && inputCollation.satisfies(sortCollation);
  }

  public static RelNode getExchange(RelOptCluster cluster, boolean isSingleton, boolean isExchangeRequired,
                                    RelTraitSet traits, DrillDistributionTrait distributionTrait,
                                    IndexCallContext indexContext, RelNode input) {
    if (!isExchangeRequired) {
      return input;
    }

    if (isSingleton) {
      return new SingleMergeExchangePrel(cluster,
              traits.replace(DrillDistributionTrait.SINGLETON),
              input, indexContext.getCollation());
    } else {
      return new HashToMergeExchangePrel(cluster,
              traits.replace(distributionTrait),
              input, distributionTrait.getFields(), indexContext.getCollation(),
              PrelUtil.getSettings(cluster).numEndPoints());
    }
  }

  private static RelNode getSortOrTopN(IndexCallContext indexContext,
                                       RelNode sortNode, RelNode newRel, RelNode child) {
    if (sortNode instanceof TopNPrel) {
      return new TopNPrel(sortNode.getCluster(),
                    newRel.getTraitSet().replace(Prel.DRILL_PHYSICAL).plus(indexContext.getCollation()),
                    child, ((TopNPrel)sortNode).getLimit(), indexContext.getCollation());
    }
    return new SortPrel(sortNode.getCluster(),
            newRel.getTraitSet().replace(Prel.DRILL_PHYSICAL).plus(indexContext.getCollation()),
            child, indexContext.getCollation());
  }

  public static RelNode getSortNode(IndexCallContext indexContext, RelNode newRel, boolean donotGenerateSort,
                                    boolean isSingleton, boolean isExchangeRequired) {
    OrderedRel rel = indexContext.getSort();
    DrillDistributionTrait hashDistribution =
        new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
            ImmutableList.copyOf(indexContext.getDistributionFields()));

    if ( toRemoveSort(indexContext.getCollation(), newRel.getTraitSet().getTrait(RelCollationTraitDef.INSTANCE))) {
      //we are going to remove sort
      logger.debug("Not generating SortPrel since we have the required collation");
      if (IndexPlanUtils.generateLimit(rel)) {
        newRel = new LimitPrel(newRel.getCluster(),
                newRel.getTraitSet().plus(indexContext.getCollation()).plus(Prel.DRILL_PHYSICAL),
                newRel, IndexPlanUtils.getOffset(rel), IndexPlanUtils.getFetch(rel));
      }
      RelTraitSet traits = newRel.getTraitSet().plus(indexContext.getCollation()).plus(Prel.DRILL_PHYSICAL);
      newRel = Prule.convert(newRel, traits);
      newRel = getExchange(newRel.getCluster(), isSingleton, isExchangeRequired,
                                 traits, hashDistribution, indexContext, newRel);
    }
    else {
      if (donotGenerateSort) {
        logger.debug("Not generating SortPrel and index plan, since just picking index for full index scan is not beneficial.");
        return null;
      }
      RelTraitSet traits = newRel.getTraitSet().plus(indexContext.getCollation()).plus(Prel.DRILL_PHYSICAL);
      newRel = getSortOrTopN(indexContext, rel, newRel,
                  Prule.convert(newRel, newRel.getTraitSet().replace(Prel.DRILL_PHYSICAL)));
      newRel = getExchange(newRel.getCluster(), isSingleton, isExchangeRequired,
                                 traits, hashDistribution, indexContext, newRel);
    }
    return newRel;
  }

  @Override
  public abstract RelNode convertChild(RelNode current, RelNode child) throws InvalidRelException;

  @Override
  public boolean forceConvert(){
    return true;
  }

  public void go() throws InvalidRelException {
    RelNode top = indexContext.getCall().rel(0);
    final RelNode input;
    if (top instanceof DrillProjectRel) {
      DrillProjectRel topProject = (DrillProjectRel) top;
      input = topProject.getInput();
    }
    else if (top instanceof DrillFilterRel) {
      DrillFilterRel topFilter = (DrillFilterRel)top;
      input = topFilter.getInput();
    } else if (top instanceof DrillSortRel) {
      DrillSortRel topSort = (DrillSortRel)top;
      input = topSort.getInput();
    }
    else if ( top instanceof DrillSortRel) {
      DrillSortRel topSort = (DrillSortRel) top;
      input = topSort.getInput();
    }
    else {
      return;
    }
    RelTraitSet traits = input.getTraitSet().plus(Prel.DRILL_PHYSICAL);
    RelNode convertedInput = Prule.convert(input, traits);
    this.go(top, convertedInput);
  }
}
