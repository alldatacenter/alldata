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

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.Correlate;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.Uncollect;
import org.apache.calcite.rel.logical.LogicalCorrelate;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.planner.logical.DrillRelFactories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Visitor that moves non-{@link RexFieldAccess} rex node from project below {@link Uncollect}
 * to the left side of the {@link Correlate}.
 */
public class ComplexUnnestVisitor extends RelShuttleImpl {
  private static final String COMPLEX_FIELD_NAME = "$COMPLEX_FIELD_NAME";

  private final Map<CorrelationId, RelNode> leftInputs = new HashMap<>();
  private final Map<CorrelationId, CorrelationId> updatedCorrelationIds = new HashMap<>();

  private ComplexUnnestVisitor() {
  }

  @Override
  public RelNode visit(LogicalCorrelate correlate) {
    RelNode left = correlate.getLeft().accept(this);
    leftInputs.put(correlate.getCorrelationId(), left);

    RelNode right = correlate.getRight().accept(this);
    // if right input wasn't changed or left input wasn't changed
    // after rewriting right input, no need to create Correlate with new CorrelationId
    if (correlate.getRight() == right
        || left == leftInputs.get(correlate.getCorrelationId())) {
      if (correlate.getLeft() == left) {
        return correlate;
      }
      // changed only inputs, but CorrelationId left the same
      return correlate.copy(correlate.getTraitSet(), Arrays.asList(left, right));
    }

    Correlate newCorrelate = correlate.copy(correlate.getTraitSet(),
        leftInputs.get(correlate.getCorrelationId()), right,
        updatedCorrelationIds.get(correlate.getCorrelationId()),
        ImmutableBitSet.of(left.getRowType().getFieldCount()), correlate.getJoinType());

    RelBuilder builder = DrillRelFactories.LOGICAL_BUILDER.create(correlate.getCluster(), null);
    builder.push(newCorrelate);

    List<RexNode> topProjectExpressions = left.getRowType().getFieldList().stream()
            .map(field -> builder.getRexBuilder().makeInputRef(newCorrelate, field.getIndex()))
            .collect(Collectors.toList());

    //Accommodate the new $COMPLEX_FIELD_NAME column.
    int rightStartIndex = left.getRowType().getFieldList().size() + 1;
    switch (correlate.getJoinType()) {
      case LEFT:
      case INNER:
        // adds field from the right input of correlate to the top project
        topProjectExpressions.addAll(right.getRowType().getFieldList().stream()
                .map(field -> builder.getRexBuilder().makeInputRef(newCorrelate, field.getIndex() + rightStartIndex))
                .collect(Collectors.toList()));
        // fall through
      case ANTI:
      case SEMI:
        builder.project(topProjectExpressions, correlate.getRowType().getFieldNames());
    }
    return builder.build();
  }

  @Override
  public RelNode visit(RelNode other) {
    if (other instanceof Uncollect) {
      return visit((Uncollect) other);
    }
    return super.visit(other);
  }

  public RelNode visit(Uncollect uncollect) {
    RelBuilder builder = DrillRelFactories.LOGICAL_BUILDER.create(uncollect.getCluster(), null);
    RexBuilder rexBuilder = builder.getRexBuilder();

    assert uncollect.getInput() instanceof Project : "Uncollect should have Project input";

    Project project = (Project) uncollect.getInput();
    // If project below uncollect contains only field references, no need to rewrite it
    List<RexNode> projectChildExps = project.getChildExps();
    assert projectChildExps.size() == 1 : "Uncollect does not support multiple expressions";

    RexNode projectExpr = projectChildExps.iterator().next();
    if (projectExpr.getKind() == SqlKind.FIELD_ACCESS) {
      return uncollect;
    }

    // Collects CorrelationId instances used in current rel node
    RelOptUtil.VariableUsedVisitor variableUsedVisitor = new RelOptUtil.VariableUsedVisitor(null);
    project.accept(variableUsedVisitor);

    assert variableUsedVisitor.variables.size() == 1 : "Uncollect supports only single correlated reference";

    CorrelationId oldCorrId = variableUsedVisitor.variables.iterator().next();
    RelNode left = leftInputs.get(oldCorrId);

    // Creates new project to be placed on top of the left input of correlate
    List<RexNode> leftProjExprs = new ArrayList<>();

    List<String> fieldNames = new ArrayList<>();
    for (RelDataTypeField field : left.getRowType().getFieldList()) {
      leftProjExprs.add(rexBuilder.makeInputRef(left, field.getIndex()));
      fieldNames.add(field.getName());
    }
    fieldNames.add(COMPLEX_FIELD_NAME);

    builder.push(left);

    // Adds complex expression with replaced correlation
    // to the projected list from the left
    leftProjExprs.add(new RexFieldAccessReplacer(builder).apply(projectExpr));

    RelNode leftProject =
        builder.project(leftProjExprs, fieldNames)
            .build();
    leftInputs.put(oldCorrId, leftProject);

    builder.push(project.getInput());

    CorrelationId newCorrId = uncollect.getCluster().createCorrel();
    // stores new CorrelationId to be used during the creation of new Correlate
    updatedCorrelationIds.put(oldCorrId, newCorrId);

    RexNode rexCorrel = rexBuilder.makeCorrel(leftProject.getRowType(), newCorrId);

    // constructs Project below Uncollect with updated RexCorrelVariable
    builder.project(
        ImmutableList.of(rexBuilder.makeFieldAccess(rexCorrel, leftProjExprs.size() - 1)),
        ImmutableList.of(COMPLEX_FIELD_NAME));
    return uncollect.copy(uncollect.getTraitSet(), builder.build());
  }

  /**
   * Rewrites rel node tree and moves non-{@link RexFieldAccess} rex node from the project
   * below {@link Uncollect} to the left side of the {@link Correlate}.
   *
   * @param relNode tree to be rewritten
   * @return rewritten rel node tree
   */
  public static RelNode rewriteUnnestWithComplexExprs(RelNode relNode) {
    ComplexUnnestVisitor visitor = new ComplexUnnestVisitor();
    return relNode.accept(visitor);
  }

  /**
   * Visitor for RexNode which replaces {@link RexFieldAccess}
   * with a reference to the field used in {@link RexFieldAccess}.
   */
  private static class RexFieldAccessReplacer extends RexShuttle {
    private final RelBuilder builder;

    public RexFieldAccessReplacer(RelBuilder builder) {
      this.builder = builder;
    }

    @Override
    public RexNode visitFieldAccess(RexFieldAccess fieldAccess) {
      return builder.field(fieldAccess.getField().getName());
    }
  }
}
