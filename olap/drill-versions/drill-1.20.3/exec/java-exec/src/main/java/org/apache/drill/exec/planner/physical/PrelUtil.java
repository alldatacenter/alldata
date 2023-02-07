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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class PrelUtil {

  public static List<Ordering> getOrdering(RelCollation collation, RelDataType rowType) {
    List<Ordering> orderExpr = new ArrayList<>();

    List<String> childFields = rowType.getFieldNames();
    Function<RelFieldCollation, String> fieldNameProvider;
    if (collation instanceof MetadataAggPrule.NamedRelCollation) {
      fieldNameProvider = fieldCollation -> {
        MetadataAggPrule.NamedRelCollation namedCollation = (MetadataAggPrule.NamedRelCollation) collation;
        return namedCollation.getName(fieldCollation.getFieldIndex());
      };
    } else {
      fieldNameProvider = fieldCollation -> childFields.get(fieldCollation.getFieldIndex());
    }

    collation.getFieldCollations().forEach(fieldCollation -> {
      FieldReference fieldReference = new FieldReference(fieldNameProvider.apply(fieldCollation), ExpressionPosition.UNKNOWN);
      orderExpr.add(new Ordering(fieldCollation.getDirection(), fieldReference, fieldCollation.nullDirection));
    });

    return orderExpr;
  }


  public static Iterator<Prel> iter(RelNode... nodes) {
    return (Iterator<Prel>) (Object) Arrays.asList(nodes).iterator();
  }

  public static Iterator<Prel> iter(List<RelNode> nodes) {
    return (Iterator<Prel>) (Object) nodes.iterator();
  }

  public static PlannerSettings getSettings(final RelOptCluster cluster) {
    return getPlannerSettings(cluster);
  }

  public static PlannerSettings getPlannerSettings(final RelOptCluster cluster) {
    return cluster.getPlanner().getContext().unwrap(PlannerSettings.class);
  }

  public static PlannerSettings getPlannerSettings(RelOptPlanner planner) {
    return planner.getContext().unwrap(PlannerSettings.class);
  }

  public static Prel removeSvIfRequired(Prel prel, SelectionVectorMode... allowed) {
    SelectionVectorMode current = prel.getEncoding();
    for (SelectionVectorMode m : allowed) {
      if (current == m) {
        return prel;
      }
    }
    return new SelectionVectorRemoverPrel(prel);
  }

  public static int getLastUsedColumnReference(List<RexNode> projects) {
    LastUsedRefVisitor lastUsed = new LastUsedRefVisitor();
    for (RexNode rex : projects) {
      rex.accept(lastUsed);
    }
    return lastUsed.getLastUsedReference();
  }


  // Simple visitor class to determine the last used reference in the expression
  private static class LastUsedRefVisitor extends RexVisitorImpl<Void> {

    int lastUsedRef = -1;

    protected LastUsedRefVisitor() {
      super(true);
    }

    @Override
    public Void visitInputRef(RexInputRef inputRef) {
      lastUsedRef = Math.max(lastUsedRef, inputRef.getIndex());
      return null;
    }

    @Override
    public Void visitCall(RexCall call) {
      for (RexNode operand : call.operands) {
        operand.accept(this);
      }
      return null;
    }

    public int getLastUsedReference() {
      return lastUsedRef;
    }
  }


  public static RelTraitSet fixTraits(RelOptRuleCall call, RelTraitSet set) {
    return fixTraits(call.getPlanner(), set);
  }

  public static RelTraitSet fixTraits(RelOptPlanner cluster, RelTraitSet set) {
    if (getPlannerSettings(cluster).isSingleMode()) {
      return set.replace(DrillDistributionTrait.ANY);
    } else {
      return set;
    }
  }

  // DRILL-6089 make sure no collations are added to HashJoin
  public static RelTraitSet removeCollation(RelTraitSet traitSet, RelOptRuleCall call) {
    RelTraitSet newTraitSet = call.getPlanner().emptyTraitSet();

    for (RelTrait trait: traitSet) {
      if (!trait.getTraitDef().getTraitClass().equals(RelCollation.class)) {
        newTraitSet = newTraitSet.plus(trait);
      }
    }

    return newTraitSet;
  }
}
