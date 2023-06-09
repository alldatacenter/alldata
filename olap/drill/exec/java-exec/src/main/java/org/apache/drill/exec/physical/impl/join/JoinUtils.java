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
package org.apache.drill.exec.physical.impl.join;

import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableFunctionScan;
import org.apache.calcite.rel.logical.LogicalCorrelate;
import org.apache.calcite.rel.logical.LogicalExchange;
import org.apache.calcite.rel.logical.LogicalIntersect;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rel.logical.LogicalMinus;
import org.apache.calcite.rel.logical.LogicalSort;
import org.apache.calcite.rel.logical.LogicalUnion;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.util.Util;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.common.Comparator;
import org.apache.drill.exec.planner.logical.DrillAggregateRel;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.planner.logical.DrillLimitRel;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.resolver.TypeCastRules;
import org.apache.drill.exec.work.foreman.UnsupportedRelOperatorException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.apache.drill.exec.planner.physical.PlannerSettings.NLJOIN_FOR_SCALAR;

public class JoinUtils {

  public enum JoinCategory {
    EQUALITY,  // equality join
    INEQUALITY,  // inequality join: <>, <, >
    CARTESIAN   // no join condition
  }
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JoinUtils.class);

  public static final String FAILED_TO_PLAN_CARTESIAN_JOIN = String.format(
      "This query cannot be planned possibly due to either a cartesian join or an inequality join. %n" +
          "If a cartesian or inequality join is used intentionally, set the option '%s' to false and try again.",
      NLJOIN_FOR_SCALAR.getOptionName());

  // Check the comparator is supported in join condition. Note that a similar check is also
  // done in JoinPrel; however we have to repeat it here because a physical plan
  // may be submitted directly to Drill.
  public static Comparator checkAndReturnSupportedJoinComparator(JoinCondition condition) {
    switch(condition.getRelationship().toUpperCase()) {
      case "EQUALS":
      case "==": /* older json plans still have '==' */
        return Comparator.EQUALS;
      case "IS_NOT_DISTINCT_FROM":
        return Comparator.IS_NOT_DISTINCT_FROM;
    }
    throw UserException.unsupportedError()
        .message("Invalid comparator supplied to this join: " + condition.getRelationship())
        .build(logger);
  }

    /**
     * Check if the given RelNode contains any Cartesian join.
     * Return true if find one. Otherwise, return false.
     *
     * @param relNode     the RelNode to be inspected.
     * @param leftKeys    a list used for the left input into the join which has
     *                    equi-join keys. It can be empty or not (but not null),
     *                    this method will clear this list before using it.
     * @param rightKeys   a list used for the right input into the join which has
     *                    equi-join keys. It can be empty or not (but not null),
     *                    this method will clear this list before using it.
     * @param filterNulls The join key positions for which null values will not
     *                    match.
     * @return            Return true if the given relNode contains Cartesian join.
     *                    Otherwise, return false
     */
  public static boolean checkCartesianJoin(RelNode relNode, List<Integer> leftKeys, List<Integer> rightKeys, List<Boolean> filterNulls) {
    if (relNode instanceof Join) {
      leftKeys.clear();
      rightKeys.clear();

      Join joinRel = (Join) relNode;
      RelNode left = joinRel.getLeft();
      RelNode right = joinRel.getRight();

      RexNode remaining = RelOptUtil.splitJoinCondition(left, right, joinRel.getCondition(), leftKeys, rightKeys, filterNulls);
      if (joinRel.getJoinType() == JoinRelType.INNER) {
        if (leftKeys.isEmpty() || rightKeys.isEmpty()) {
          return true;
        }
      } else {
        if (!remaining.isAlwaysTrue() || leftKeys.isEmpty() || rightKeys.isEmpty()) {
          return true;
        }
      }
    }

    for (RelNode child : relNode.getInputs()) {
      if (checkCartesianJoin(child, leftKeys, rightKeys, filterNulls)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if the given RelNode contains any Cartesian join.
   * Return true if find one. Otherwise, return false.
   *
   * @param relNode     {@link RelNode} instance to be inspected
   * @return            Return true if the given relNode contains Cartesian join.
   *                    Otherwise, return false
   */
  public static boolean checkCartesianJoin(RelNode relNode) {
    return checkCartesianJoin(relNode, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
  }

  /**
   * Checks if implicit cast is allowed between the two input types of the join condition. Currently we allow
   * implicit casts in join condition only between numeric types and varchar/varbinary types.
   * @param input1
   * @param input2
   * @return true if implicit cast is allowed false otherwise
   */
  private static boolean allowImplicitCast(TypeProtos.MinorType input1, TypeProtos.MinorType input2) {
    // allow implicit cast if both the input types are numeric and any of them is non-decimal
    // or both of them are decimal
    if (TypeCastRules.isNumericType(input1) && TypeCastRules.isNumericType(input2)
        && ((!Types.isDecimalType(input1) && !Types.isDecimalType(input2))
          || Types.areDecimalTypes(input1, input2))) {
      return true;
    }

    // allow implicit cast if input types are date/ timestamp
    if ((input1 == TypeProtos.MinorType.DATE || input1 == TypeProtos.MinorType.TIMESTAMP) &&
        (input2 == TypeProtos.MinorType.DATE || input2 == TypeProtos.MinorType.TIMESTAMP)) {
      return true;
    }

    // allow implicit cast if both the input types are varbinary/ varchar
    if ((input1 == TypeProtos.MinorType.VARCHAR || input1 == TypeProtos.MinorType.VARBINARY) &&
        (input2 == TypeProtos.MinorType.VARCHAR || input2 == TypeProtos.MinorType.VARBINARY)) {
      return true;
    }

    return false;
  }

  /**
   * Utility method used by joins to add implicit casts on one of the sides of the join condition in case the two
   * expressions have different types.
   * @param leftExpressions array of expressions from left input into the join
   * @param leftBatch left input record batch
   * @param rightExpressions array of expressions from right input into the join
   * @param rightBatch right input record batch
   * @param context fragment context
   */
  public static void addLeastRestrictiveCasts(LogicalExpression[] leftExpressions, VectorAccessible leftBatch,
                                              LogicalExpression[] rightExpressions, VectorAccessible rightBatch,
                                              FragmentContext context) {
    assert rightExpressions.length == leftExpressions.length;

    for (int i = 0; i < rightExpressions.length; i++) {
      LogicalExpression rightExpression = rightExpressions[i];
      LogicalExpression leftExpression = leftExpressions[i];
      TypeProtos.MinorType rightType = rightExpression.getMajorType().getMinorType();
      TypeProtos.MinorType leftType = leftExpression.getMajorType().getMinorType();

      if (rightType == TypeProtos.MinorType.UNION || leftType == TypeProtos.MinorType.UNION) {
        continue;
      }
      if (rightType != leftType) {

        // currently we only support implicit casts if the input types are numeric or varchar/varbinary
        if (!allowImplicitCast(rightType, leftType)) {
          throw new DrillRuntimeException(String.format("Join only supports implicit casts between\n" +
              "1. Numeric data (none of types is decimal or both of them are decimal)\n" +
              "2. Varchar, Varbinary data\n3. Date, Timestamp data\n" +
              "Left type: %s, Right type: %s. Add explicit casts to avoid this error", leftType, rightType));
        }

        // We need to add a cast to one of the expressions
        List<TypeProtos.MinorType> types = new LinkedList<>();
        types.add(rightType);
        types.add(leftType);
        TypeProtos.MinorType result = TypeCastRules.getLeastRestrictiveType(types);
        ErrorCollector errorCollector = new ErrorCollectorImpl();

        if (result == null) {
          throw new DrillRuntimeException(String.format("Join conditions cannot be compared failing left " +
                  "expression:" + " %s failing right expression: %s", leftExpression.getMajorType().toString(),
              rightExpression.getMajorType().toString()));
        } else if (result != rightType) {
          // Add a cast expression on top of the right expression
          LogicalExpression castExpr = ExpressionTreeMaterializer.addCastExpression(rightExpression, leftExpression.getMajorType(), context.getFunctionRegistry(), errorCollector);
          // Store the newly casted expression
          rightExpressions[i] =
              ExpressionTreeMaterializer.materialize(castExpr, rightBatch, errorCollector,
                  context.getFunctionRegistry());
        } else if (result != leftType) {
          // Add a cast expression on top of the left expression
          LogicalExpression castExpr = ExpressionTreeMaterializer.addCastExpression(leftExpression, rightExpression.getMajorType(), context.getFunctionRegistry(), errorCollector);
          // store the newly casted expression
          leftExpressions[i] =
              ExpressionTreeMaterializer.materialize(castExpr, leftBatch, errorCollector,
                  context.getFunctionRegistry());
        }
      }
    }
  }

  /**
   * Utility method to check if a subquery (represented by its root RelNode) is provably scalar. Currently
   * only aggregates with no group-by are considered scalar. In the future, this method should be generalized
   * to include more cases and reconciled with Calcite's notion of scalar.
   * @param root The root RelNode to be examined
   * @return True if the root rel or its descendant is scalar, False otherwise
   */
  public static boolean isScalarSubquery(RelNode root) {
    DrillAggregateRel agg = null;
    RelNode currentrel = root;
    while (agg == null && currentrel != null) {
      if (currentrel instanceof DrillAggregateRel) {
        agg = (DrillAggregateRel)currentrel;
      } else if (currentrel instanceof RelSubset) {
        currentrel = ((RelSubset) currentrel).getBest();
      } else if (currentrel instanceof DrillLimitRel) {
        // TODO: Improve this check when DRILL-5691 is fixed.
        // The problem is that RelMdMaxRowCount currently cannot be used
        // due to CALCITE-1048.
        Integer fetchValue = ((RexLiteral) ((DrillLimitRel) currentrel).getFetch()).getValueAs(Integer.class);
        return fetchValue != null && fetchValue <= 1;
      } else if (currentrel.getInputs().size() == 1) {
        // If the rel is not an aggregate or RelSubset, but is a single-input rel (could be Project,
        // Filter, Sort etc.), check its input
        currentrel = currentrel.getInput(0);
      } else {
        break;
      }
    }

    if (agg != null) {
      if (agg.getGroupSet().isEmpty()) {
        return true;
      }
      // Checks that expression in group by is a single and it is literal.
      // When Calcite rewrites EXISTS sub-queries using SubQueryRemoveRule rules,
      // it creates project with TRUE literal in expressions list and aggregate on top of it
      // with empty call list and literal from project expression in group set.
      if (agg.getAggCallList().isEmpty() && agg.getGroupSet().cardinality() == 1) {
        ProjectExpressionsCollector expressionsCollector = new ProjectExpressionsCollector();
        agg.accept(expressionsCollector);
        List<RexNode> projectedExpressions = expressionsCollector.getProjectedExpressions();
        return projectedExpressions.size() == 1
            && RexUtil.isLiteral(projectedExpressions.get(agg.getGroupSet().nth(0)), true);
      }
    }
    return false;
  }

  public static JoinCategory getJoinCategory(RelNode left, RelNode right, RexNode condition,
      List<Integer> leftKeys, List<Integer> rightKeys, List<Boolean> filterNulls) {
    if (condition.isAlwaysTrue()) {
      return JoinCategory.CARTESIAN;
    }
    leftKeys.clear();
    rightKeys.clear();
    filterNulls.clear();
    RexNode remaining = RelOptUtil.splitJoinCondition(left, right, condition, leftKeys, rightKeys, filterNulls);

    if (!remaining.isAlwaysTrue() || (leftKeys.size() == 0 || rightKeys.size() == 0) ) {
      // for practical purposes these cases could be treated as inequality
      return JoinCategory.INEQUALITY;
    }
    return JoinCategory.EQUALITY;
  }

  /**
   * Utility method to check if a any of input RelNodes is provably scalar.
   *
   * @param left  the RelNode to be inspected.
   * @param right the RelNode to be inspected.
   * @return      Return true if any of the given RelNodes is provably scalar.
   *              Otherwise, return false
   */
  public static boolean hasScalarSubqueryInput(RelNode left, RelNode right) {
    return isScalarSubquery(left) || isScalarSubquery(right);
  }

  /**
   * Creates new exception for queries that cannot be planned due
   * to presence of cartesian or inequality join.
   *
   * @return new {@link UnsupportedRelOperatorException} instance
   */
  public static UnsupportedRelOperatorException cartesianJoinPlanningException() {
    return new UnsupportedRelOperatorException(FAILED_TO_PLAN_CARTESIAN_JOIN);
  }

  /**
   * Collects expressions list from the input project.
   * For the case when input rel node has single input, its input is taken.
   */
  private static class ProjectExpressionsCollector extends RelShuttleImpl {
    private final List<RexNode> expressions = new ArrayList<>();

    @Override
    public RelNode visit(RelNode other) {
      // RelShuttleImpl doesn't have visit methods for Project and RelSubset.
      if (other instanceof RelSubset) {
        return visit((RelSubset) other);
      } else if (other instanceof Project) {
        return visit((Project) other);
      }
      return super.visit(other);
    }

    @Override
    public RelNode visit(TableFunctionScan scan) {
      return scan;
    }

    @Override
    public RelNode visit(LogicalJoin join) {
      return join;
    }

    @Override
    public RelNode visit(LogicalCorrelate correlate) {
      return correlate;
    }

    @Override
    public RelNode visit(LogicalUnion union) {
      return union;
    }

    @Override
    public RelNode visit(LogicalIntersect intersect) {
      return intersect;
    }

    @Override
    public RelNode visit(LogicalMinus minus) {
      return minus;
    }

    @Override
    public RelNode visit(LogicalSort sort) {
      return sort;
    }

    @Override
    public RelNode visit(LogicalExchange exchange) {
      return exchange;
    }

    private RelNode visit(Project project) {
      expressions.addAll(project.getProjects());
      return project;
    }

    private RelNode visit(RelSubset subset) {
      return Util.first(subset.getBest(), subset.getOriginal()).accept(this);
    }

    public List<RexNode> getProjectedExpressions() {
      return expressions;
    }
  }
}
