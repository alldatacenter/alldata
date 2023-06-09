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

import org.apache.drill.shaded.guava.com.google.common.base.Predicate;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.core.Window;
import org.apache.calcite.util.BitSets;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillWindowRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlAggFunction;

import java.util.List;

public class WindowPrule extends Prule {
  public static final RelOptRule INSTANCE = new WindowPrule();

  private WindowPrule() {
    super(RelOptHelper.some(DrillWindowRel.class, DrillRel.DRILL_LOGICAL, RelOptHelper.any(RelNode.class)), "Prel.WindowPrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillWindowRel window = call.rel(0);
    RelNode input = call.rel(1);

    // TODO: Order window based on existing partition by
    //input.getTraitSet().subsumes()

    boolean partitionby = false;
    boolean addMerge = false;

    // The start index of the constant fields of DrillWindowRel
    final int startConstantsIndex = window.getInput().getRowType().getFieldCount();

    int constantShiftIndex = 0;
    for (final Ord<Window.Group> w : Ord.zip(window.groups)) {
      Window.Group windowBase = w.getValue();
      RelTraitSet traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL);

      // For empty Over-Clause
      if(windowBase.keys.isEmpty()
          && windowBase.orderKeys.getFieldCollations().isEmpty()) {
        DrillDistributionTrait distEmptyKeys =
            new DrillDistributionTrait(DrillDistributionTrait.DistributionType.SINGLETON);

        traits = traits.plus(distEmptyKeys);
      } else if (windowBase.keys.size() > 0) {
        DrillDistributionTrait distOnAllKeys =
            new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
                ImmutableList.copyOf(getDistributionFields(windowBase)));

        partitionby = true;
        traits = traits.plus(distOnAllKeys);
      } else if (windowBase.orderKeys.getFieldCollations().size() > 0) {
        // if only the order-by clause is specified, there is a single partition
        // consisting of all the rows, so we do a distributed sort followed by a
        // single merge as the input of the window operator
        DrillDistributionTrait distKeys =
            new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
                ImmutableList.copyOf(getDistributionFieldsFromCollation(windowBase)));

        traits = traits.plus(distKeys);
        if(!isSingleMode(call)) {
          addMerge = true;
        }
      }

      // Add collation trait if either partition-by or order-by is specified.
      if (partitionby || windowBase.orderKeys.getFieldCollations().size() > 0) {
        RelCollation collation = getCollation(windowBase);
        traits = traits.plus(collation);
      }

      RelNode convertedInput = convert(input, traits);

      if (addMerge) {
        traits = traits.plus(DrillDistributionTrait.SINGLETON);
        convertedInput = new SingleMergeExchangePrel(window.getCluster(), traits,
                         convertedInput, windowBase.collation());
      }

      List<RelDataTypeField> newRowFields = Lists.newArrayList();
      newRowFields.addAll(convertedInput.getRowType().getFieldList());

      Iterable<RelDataTypeField> newWindowFields = Iterables.filter(window.getRowType().getFieldList(), new Predicate<RelDataTypeField>() {
            @Override
            public boolean apply(RelDataTypeField relDataTypeField) {
              return relDataTypeField.getName().startsWith("w" + w.i + "$");
            }
      });

      for(RelDataTypeField newField : newWindowFields) {
        newRowFields.add(newField);
      }

      RelDataType rowType = new RelRecordType(newRowFields);

      List<Window.RexWinAggCall> newWinAggCalls = Lists.newArrayList();
      for(Ord<Window.RexWinAggCall> aggOrd : Ord.zip(windowBase.aggCalls)) {
        Window.RexWinAggCall aggCall = aggOrd.getValue();

        // If the argument points at the constant and
        // additional fields have been generated by the Window below,
        // the index of constants will be shifted
        final List<RexNode> newOperandsOfWindowFunction = Lists.newArrayList();
        for(RexNode operand : aggCall.getOperands()) {
          if(operand instanceof RexInputRef) {
            final RexInputRef rexInputRef = (RexInputRef) operand;
            final int refIndex = rexInputRef.getIndex();

            // Check if this RexInputRef points at the constants
            if(rexInputRef.getIndex() >= startConstantsIndex) {
              operand = new RexInputRef(refIndex + constantShiftIndex,
                  window.constants.get(refIndex - startConstantsIndex).getType());
            }
          }

          newOperandsOfWindowFunction.add(operand);
        }
        aggCall = new Window.RexWinAggCall(
            (SqlAggFunction) aggCall.getOperator(),
            aggCall.getType(),
            newOperandsOfWindowFunction,
            aggCall.ordinal,
            aggCall.distinct);

        newWinAggCalls.add(new Window.RexWinAggCall(
            (SqlAggFunction)aggCall.getOperator(), aggCall.getType(), aggCall.getOperands(), aggOrd.i, aggCall.distinct)
        );
      }

      windowBase = new Window.Group(
          windowBase.keys,
          windowBase.isRows,
          windowBase.lowerBound,
          windowBase.upperBound,
          windowBase.orderKeys,
          newWinAggCalls
      );

      input = new WindowPrel(
          window.getCluster(),
          window.getTraitSet().merge(traits),
          convertedInput,
          window.getConstants(),
          rowType,
          windowBase);

      constantShiftIndex += windowBase.aggCalls.size();
    }

    call.transformTo(input);
  }

  /**
   * Create a RelCollation that has partition-by as the leading keys followed by order-by keys
   * @param window The window specification
   * @return a RelCollation with {partition-by keys, order-by keys}
   */
  private RelCollation getCollation(Window.Group window) {
    List<RelFieldCollation> fields = Lists.newArrayList();
    for (int group : BitSets.toIter(window.keys)) {
      fields.add(new RelFieldCollation(group));
    }

    fields.addAll(window.orderKeys.getFieldCollations());

    return RelCollations.of(fields);
  }

  private List<DrillDistributionTrait.DistributionField> getDistributionFields(Window.Group window) {
    List<DrillDistributionTrait.DistributionField> groupByFields = Lists.newArrayList();
    for (int group : BitSets.toIter(window.keys)) {
      DrillDistributionTrait.DistributionField field = new DrillDistributionTrait.DistributionField(group);
      groupByFields.add(field);
    }

    return groupByFields;
  }

  private List<DistributionField> getDistributionFieldsFromCollation(Window.Group window) {
    List<DistributionField> distFields = Lists.newArrayList();

    for (RelFieldCollation relField : window.collation().getFieldCollations()) {
      DistributionField field = new DistributionField(relField.getFieldIndex());
      distFields.add(field);
    }

    return distFields;
  }

}
