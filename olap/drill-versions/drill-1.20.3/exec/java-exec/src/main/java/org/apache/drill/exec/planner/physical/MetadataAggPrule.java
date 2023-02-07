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

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationImpl;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.MetadataAggRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.AggPrelBase.OperatorPhase;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.NamedDistributionField;
import org.apache.drill.exec.store.parquet.FilterEvaluatorUtils.FieldReferenceFinder;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataAggPrule extends Prule {
  public static final MetadataAggPrule INSTANCE = new MetadataAggPrule();

  public MetadataAggPrule() {
    super(RelOptHelper.any(MetadataAggRel.class, DrillRel.DRILL_LOGICAL),
        "MetadataAggPrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    MetadataAggRel aggregate = call.rel(0);
    RelNode input = aggregate.getInput();

    int groupByExprsSize = aggregate.getContext().groupByExpressions().size();

    List<RelFieldCollation> collations = new ArrayList<>();
    List<String> names = new ArrayList<>();
    for (int i = 0; i < groupByExprsSize; i++) {
      collations.add(new RelFieldCollation(i + 1));
      SchemaPath fieldPath = getArgumentReference(aggregate.getContext().groupByExpressions().get(i));
      names.add(fieldPath.getRootSegmentPath());
    }

    RelCollation collation = new NamedRelCollation(collations, names);

    RelTraitSet traits;

    if (aggregate.getContext().groupByExpressions().isEmpty()) {
      DrillDistributionTrait singleDist = DrillDistributionTrait.SINGLETON;
      RelTraitSet singleDistTrait = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(singleDist);

      createTransformRequest(call, aggregate, input, singleDistTrait);
    } else {
      // hash distribute on all grouping keys
      DrillDistributionTrait distOnAllKeys =
          new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
              ImmutableList.copyOf(getDistributionFields(aggregate.getContext().groupByExpressions())));

      PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());
      boolean smallInput =
          input.estimateRowCount(input.getCluster().getMetadataQuery()) < settings.getSliceTarget();

      // force 2-phase aggregation for bottom aggregate call
      // to produce sort locally before aggregation is produced for large inputs
      if (aggregate.getContext().createNewAggregations() && !smallInput) {
        traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL);
        RelNode convertedInput = convert(input, traits);

        new TwoPhaseMetadataAggSubsetTransformer(call, collation, distOnAllKeys)
            .go(aggregate, convertedInput);
      } else {
        // TODO: DRILL-7433 - replace DrillDistributionTrait.SINGLETON with distOnAllKeys when parallelization for MetadataHandler is implemented
        traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collation).plus(DrillDistributionTrait.SINGLETON);
        createTransformRequest(call, aggregate, input, traits);
      }
    }
  }

  private void createTransformRequest(RelOptRuleCall call, MetadataAggRel aggregate,
      RelNode input, RelTraitSet traits) {

    RelNode convertedInput = convert(input, PrelUtil.fixTraits(call, traits));

    MetadataStreamAggPrel newAgg = new MetadataStreamAggPrel(
        aggregate.getCluster(),
        traits,
        convertedInput,
        aggregate.getContext(),
        OperatorPhase.PHASE_1of1);

    call.transformTo(newAgg);
  }

  /**
   * Returns list with named distribution fields which correspond to specified expressions arguments.
   *
   * @param namedExpressions expressions list
   * @return list of {@link NamedDistributionField} instances
   */
  private static List<NamedDistributionField> getDistributionFields(List<NamedExpression> namedExpressions) {
    List<NamedDistributionField> distributionFields = new ArrayList<>();
    int groupByExprsSize = namedExpressions.size();

    for (int index = 0; index < groupByExprsSize; index++) {
      SchemaPath fieldPath = getArgumentReference(namedExpressions.get(index));
      NamedDistributionField field =
          new NamedDistributionField(index + 1, fieldPath.getRootSegmentPath());
      distributionFields.add(field);
    }

    return distributionFields;
  }

  /**
   * Returns {@link FieldReference} instance which corresponds to the argument of specified {@code namedExpression}.
   *
   * @param namedExpression expression
   * @return {@link FieldReference} instance
   */
  private static FieldReference getArgumentReference(NamedExpression namedExpression) {
    Set<SchemaPath> arguments = namedExpression.getExpr().accept(FieldReferenceFinder.INSTANCE, null);
    assert arguments.size() == 1 : "Group by expression contains more than one argument";
    return new FieldReference(arguments.iterator().next());
  }

  /**
   * Implementation of {@link RelCollationImpl} with field name.
   * Stores {@link RelFieldCollation} list and corresponding field names to be used in sort operators.
   * Field name is required for the case of dynamic schema discovering
   * when field is not present in rel data type at planning time.
   */
  public static class NamedRelCollation extends RelCollationImpl {
    private final List<String> names;

    protected NamedRelCollation(List<RelFieldCollation> fieldCollations, List<String> names) {
      super(com.google.common.collect.ImmutableList.copyOf(fieldCollations));
      this.names = Collections.unmodifiableList(names);
    }

    public String getName(int collationIndex) {
      return names.get(collationIndex - 1);
    }
  }

  /**
   * {@link SubsetTransformer} for creating two-phase metadata aggregation.
   */
  private static class TwoPhaseMetadataAggSubsetTransformer
      extends SubsetTransformer<MetadataAggRel, RuntimeException> {

    private final RelCollation collation;
    private final DrillDistributionTrait distributionTrait;

    public TwoPhaseMetadataAggSubsetTransformer(RelOptRuleCall call,
        RelCollation collation, DrillDistributionTrait distributionTrait) {
      super(call);
      this.collation = collation;
      this.distributionTrait = distributionTrait;
    }

    @Override
    public RelNode convertChild(MetadataAggRel aggregate, RelNode child) {
      DrillDistributionTrait toDist = child.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
      RelTraitSet traits = newTraitSet(Prel.DRILL_PHYSICAL, RelCollations.EMPTY, toDist);
      RelNode newInput = convert(child, traits);

      // maps group by expressions to themselves to be able to produce the second aggregation
      List<NamedExpression> identityExpressions = aggregate.getContext().groupByExpressions().stream()
          .map(namedExpression -> new NamedExpression(namedExpression.getExpr(), getArgumentReference(namedExpression)))
          .collect(Collectors.toList());

      // use hash aggregation for the first stage to avoid sorting raw data
      MetadataHashAggPrel phase1Agg = new MetadataHashAggPrel(
          aggregate.getCluster(),
          traits,
          newInput,
          aggregate.getContext().toBuilder().groupByExpressions(identityExpressions).build(),
          OperatorPhase.PHASE_1of2);

      traits = newTraitSet(Prel.DRILL_PHYSICAL, collation, toDist).plus(distributionTrait);
      SortPrel sort = new SortPrel(
          aggregate.getCluster(),
          traits,
          phase1Agg,
          (RelCollation) traits.getTrait(collation.getTraitDef()));

      int numEndPoints = PrelUtil.getSettings(phase1Agg.getCluster()).numEndPoints();

      HashToMergeExchangePrel exch =
          new HashToMergeExchangePrel(phase1Agg.getCluster(),
              traits,
              sort,
              ImmutableList.copyOf(getDistributionFields(aggregate.getContext().groupByExpressions())),
              collation,
              numEndPoints);

      return new MetadataStreamAggPrel(
          aggregate.getCluster(),
          newTraitSet(Prel.DRILL_PHYSICAL, collation, DrillDistributionTrait.SINGLETON),
          exch,
          aggregate.getContext(),
          OperatorPhase.PHASE_2of2);
    }
  }
}
