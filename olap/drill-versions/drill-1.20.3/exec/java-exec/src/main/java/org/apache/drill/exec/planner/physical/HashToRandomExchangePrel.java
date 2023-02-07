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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.linq4j.Ord;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.HashToRandomExchange;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.drill.exec.server.options.OptionManager;

public class HashToRandomExchangePrel extends ExchangePrel {

  private final List<DistributionField> fields;

  public HashToRandomExchangePrel(RelOptCluster cluster, RelTraitSet traitSet, RelNode input, List<DistributionField> fields) {
    super(cluster, traitSet, input);
    this.fields = fields;
    assert input.getConvention() == Prel.DRILL_PHYSICAL;
  }

  /**
   * HashToRandomExchange processes M input rows and hash partitions them
   * based on computing a hash value on the distribution fields.
   * If there are N nodes (endpoints), we can assume for costing purposes
   * on average each sender will send M/N rows to 1 destination endpoint.
   * (See DrillCostBase for symbol notations)
   * Include impact of skewness of distribution : the more keys used, the less likely the distribution will be skewed.
   * The hash cpu cost will be proportional to 1 / #_keys.
   * C =  CPU cost of hashing k fields of M/N rows
   *      + CPU cost of SV remover for M/N rows
   *      + Network cost of sending M/N rows to 1 destination.
   * So, C = (h * 1/k * M/N) + (s * M/N) + (w * M/N)
   * Total cost = N * C
   */
  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner, mq).multiplyBy(.1);
    }

    RelNode child = this.getInput();
    double inputRows = mq.getRowCount(child);

    int rowWidth = child.getRowType().getFieldCount() * DrillCostBase.AVG_FIELD_WIDTH;

    double hashCpuCost = DrillCostBase.HASH_CPU_COST * inputRows / fields.size();
    double svrCpuCost = DrillCostBase.SVR_CPU_COST * inputRows;
    double networkCost = DrillCostBase.BYTE_NETWORK_COST * inputRows * rowWidth;
    DrillCostFactory costFactory = (DrillCostFactory) planner.getCostFactory();
    return costFactory.makeCost(inputRows, hashCpuCost + svrCpuCost, 0, networkCost);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new HashToRandomExchangePrel(getCluster(), traitSet, sole(inputs), fields);
  }

  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    if (PrelUtil.getSettings(getCluster()).isSingleMode()) {
      return childPOP;
    }

    // TODO - refactor to different exchange name
    HashToRandomExchange g = new HashToRandomExchange(childPOP, HashPrelUtil.getHashExpression(this.fields, getInput().getRowType()));
    return creator.addMetadata(this, g);
  }

  public List<DistributionField> getFields() {
    return this.fields;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    super.explainTerms(pw);
      for (Ord<DistributionField> ord : Ord.zip(fields)) {
        pw.item("dist" + ord.i, ord.e);
      }
    return pw;
  }

  /**
   * This method creates a new UnorderedMux and Demux exchanges if mux operators are enabled.
   * @param child input to the new Unordered[Mux/Demux]Prel or new HashToRandomExchange node.
   * @param options options manager to check if mux is enabled.
   */
  @Override
  public Prel constructMuxPrel(Prel child, OptionManager options) {
    boolean isMuxEnabled = options.getOption(PlannerSettings.MUX_EXCHANGE.getOptionName()).bool_val;
    Prel newPrel = child;

    final List<String> childFields = child.getRowType().getFieldNames();

    List <RexNode> removeUpdatedExpr = null;

    if (isMuxEnabled) {
      // Insert Project Operator with new column that will be a hash for HashToRandomExchange fields
      final List<DistributionField> distFields = getFields();
      final List<String> outputFieldNames = Lists.newArrayList(childFields);
      final RexBuilder rexBuilder = getCluster().getRexBuilder();
      final List<RelDataTypeField> childRowTypeFields = child.getRowType().getFieldList();

      final HashPrelUtil.HashExpressionCreatorHelper<RexNode> hashHelper =
                                    new HashPrelUtil.RexNodeBasedHashExpressionCreatorHelper(rexBuilder);

      final List<RexNode> distFieldRefs = Lists.newArrayListWithExpectedSize(distFields.size());
      for (DistributionField distField : distFields) {
        final int fieldId = distField.getFieldId();
        distFieldRefs.add(rexBuilder.makeInputRef(childRowTypeFields.get(fieldId).getType(), fieldId));
      }

      final List <RexNode> updatedExpr = Lists.newArrayListWithExpectedSize(childRowTypeFields.size());
      removeUpdatedExpr = Lists.newArrayListWithExpectedSize(childRowTypeFields.size());
      for (RelDataTypeField field : childRowTypeFields) {
        RexNode rex = rexBuilder.makeInputRef(field.getType(), field.getIndex());
        updatedExpr.add(rex);
        removeUpdatedExpr.add(rex);
      }

      outputFieldNames.add(HashPrelUtil.HASH_EXPR_NAME);
      final RexNode distSeed = rexBuilder.makeBigintLiteral(BigDecimal.valueOf(HashPrelUtil.DIST_SEED)); // distribution seed
      updatedExpr.add(HashPrelUtil.createHashBasedPartitionExpression(distFieldRefs, distSeed, hashHelper));

      RelDataType rowType = RexUtil.createStructType(getCluster().getTypeFactory(),
          updatedExpr, outputFieldNames, null);

      ProjectPrel addColumnprojectPrel = new ProjectPrel(child.getCluster(), child.getTraitSet(), child, updatedExpr, rowType);

      newPrel = new UnorderedMuxExchangePrel(addColumnprojectPrel.getCluster(), addColumnprojectPrel.getTraitSet(),
              addColumnprojectPrel);
    }

    newPrel = new HashToRandomExchangePrel(getCluster(), getTraitSet(), newPrel, getFields());

    if (options.getOption(PlannerSettings.DEMUX_EXCHANGE.getOptionName()).bool_val) {
      HashToRandomExchangePrel hashExchangePrel = (HashToRandomExchangePrel) newPrel;
      // Insert a DeMuxExchange to narrow down the number of receivers
      newPrel = new UnorderedDeMuxExchangePrel(getCluster(), getTraitSet(), hashExchangePrel, hashExchangePrel.getFields());
    }

    if (isMuxEnabled) {
      // remove earlier inserted Project Operator - since it creates issues down the road in HashJoin
      RelDataType removeRowType = RexUtil.createStructType(newPrel.getCluster().getTypeFactory(),
          removeUpdatedExpr, childFields, null);

      ProjectPrel removeColumnProjectPrel = new ProjectPrel(newPrel.getCluster(), newPrel.getTraitSet(),
                                                            newPrel, removeUpdatedExpr, removeRowType);
      return removeColumnProjectPrel;
    }
    return newPrel;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.ALL;
  }

}
