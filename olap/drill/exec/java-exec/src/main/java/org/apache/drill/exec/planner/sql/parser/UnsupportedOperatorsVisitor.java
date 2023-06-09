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
package org.apache.drill.exec.planner.sql.parser;

import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.util.Litmus;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.UnsupportedOperatorCollector;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;

import org.apache.calcite.sql.SqlSelectKeyword;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWindow;
import org.apache.calcite.sql.fun.SqlCountAggFunction;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.SqlDataTypeSpec;

import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class UnsupportedOperatorsVisitor extends SqlShuttle {
  private QueryContext context;
  private static List<String> disabledType = Lists.newArrayList();
  private static List<String> disabledOperators = Lists.newArrayList();
  private static List<String> dirExplorers = Lists.newArrayList();

  static {
    disabledType.add(SqlTypeName.TINYINT.name());
    disabledType.add(SqlTypeName.SMALLINT.name());
    disabledType.add(SqlTypeName.REAL.name());
    disabledOperators.add("CARDINALITY");
    dirExplorers.add("MAXDIR");
    dirExplorers.add("IMAXDIR");
    dirExplorers.add("MINDIR");
    dirExplorers.add("IMINDIR");
  }

  private UnsupportedOperatorCollector unsupportedOperatorCollector;

  private UnsupportedOperatorsVisitor(QueryContext context) {
    this.context = context;
    this.unsupportedOperatorCollector = new UnsupportedOperatorCollector();
  }

  public static UnsupportedOperatorsVisitor createVisitor(QueryContext context) {
    return new UnsupportedOperatorsVisitor(context);
  }

  public void convertException() throws SqlUnsupportedException {
    unsupportedOperatorCollector.convertException();
  }

  @Override
  public SqlNode visit(SqlDataTypeSpec type) {
    for (String strType : disabledType) {
      if (type.getTypeName().getSimple().equalsIgnoreCase(strType)) {
        unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.DATA_TYPE,
            type.getTypeName().getSimple() + " is not supported\n" +
            "See Apache Drill JIRA: DRILL-1959");
        throw new UnsupportedOperationException();
      }
    }

    return type;
  }

  @Override
  public SqlNode visit(SqlCall sqlCall) {
    // Inspect the window functions
    if (sqlCall instanceof SqlSelect) {
      SqlSelect sqlSelect = (SqlSelect) sqlCall;

      checkGrouping((sqlSelect));

      checkRollupCubeGrpSets(sqlSelect);

      for (SqlNode nodeInSelectList : sqlSelect.getSelectList()) {
        // If the window function is used with an alias,
        // enter the first operand of AS operator
        if (nodeInSelectList.getKind() == SqlKind.AS
            && (((SqlCall) nodeInSelectList).getOperandList().get(0).getKind() == SqlKind.OVER)) {
          nodeInSelectList = ((SqlCall) nodeInSelectList).getOperandList().get(0);
        }

        if (nodeInSelectList.getKind() == SqlKind.OVER) {
          // Throw exceptions if window functions are disabled
          if (!context.getOptions().getOption(ExecConstants.ENABLE_WINDOW_FUNCTIONS).bool_val) {
            unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                "Window functions are disabled\n" +
                "See Apache Drill JIRA: DRILL-2559");
            throw new UnsupportedOperationException();
          }

          // DRILL-3182, DRILL-3195
          SqlCall over = (SqlCall) nodeInSelectList;
          if (over.getOperandList().get(0) instanceof SqlCall) {
            SqlCall function = (SqlCall) over.getOperandList().get(0);

            // DRILL-3182
            // Window function with DISTINCT qualifier is temporarily disabled
            if (function.getFunctionQuantifier() != null
                && function.getFunctionQuantifier().getValue() == SqlSelectKeyword.DISTINCT) {
              unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                  "DISTINCT for window aggregate functions is not currently supported\n" +
                  "See Apache Drill JIRA: DRILL-3182");
              throw new UnsupportedOperationException();
            }

            // DRILL-3596: we only allow (<column-name>) or (<column-name>, 1)
            final String functionName = function.getOperator().getName().toUpperCase();
            if ("LEAD".equals(functionName) || "LAG".equals(functionName)) {
              boolean supported = true;
              if (function.operandCount() > 2) {
                // we don't support more than 2 arguments
                supported = false;
              } else if (function.operandCount() == 2) {
                SqlNode operand = function.operand(1);
                if (operand instanceof SqlNumericLiteral) {
                  SqlNumericLiteral offsetLiteral = (SqlNumericLiteral) operand;
                  try {
                    if (offsetLiteral.intValue(true) != 1) {
                      // we don't support offset != 1
                      supported = false;
                    }
                  } catch (AssertionError e) {
                    // we only support offset as an integer
                    supported = false;
                  }
                } else {
                  // we only support offset as a numeric literal
                  supported = false;
                }
              }

              if (!supported) {
                unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                  "Function " + functionName + " only supports (<value expression>) or (<value expression>, 1)\n" +
                    "See Apache DRILL JIRA: DRILL-3596");
                throw new UnsupportedOperationException();
              }
            }
          }
        }
      }
    }

    // DRILL-3188
    // Disable frame which is other than the default
    // (i.e., BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
    if (sqlCall instanceof SqlWindow) {
      SqlWindow window = (SqlWindow) sqlCall;

      SqlNode lowerBound = window.getLowerBound();
      SqlNode upperBound = window.getUpperBound();

      // If no frame is specified
      // it is a default frame
      boolean isSupported = (lowerBound == null && upperBound == null);

      // When OVER clause contain an ORDER BY clause the following frames are supported:
      // RANGE UNBOUNDED PRECEDING
      // RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
      // RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      if (window.getOrderList().size() != 0
          && !window.isRows()
          && SqlWindow.isUnboundedPreceding(lowerBound)
          && (upperBound == null || SqlWindow.isCurrentRow(upperBound) || SqlWindow.isUnboundedFollowing(upperBound))) {
        isSupported = true;
      }

      // ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
      // is supported with and without the ORDER BY clause
      if (window.isRows()
          && SqlWindow.isUnboundedPreceding(lowerBound)
          && (upperBound == null || SqlWindow.isCurrentRow(upperBound))) {
        isSupported = true;
      }

      // RANGE BETWEEN CURRENT ROW AND CURRENT ROW
      // is supported with and without an ORDER BY clause
      if (!window.isRows() &&
          SqlWindow.isCurrentRow(lowerBound) &&
          SqlWindow.isCurrentRow(upperBound)) {
        isSupported = true;
      }

      // When OVER clause doesn't contain an ORDER BY clause, the following are equivalent to the default frame:
      // RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      // ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      if (window.getOrderList().size() == 0
          && SqlWindow.isUnboundedPreceding(lowerBound)
          && SqlWindow.isUnboundedFollowing(upperBound)) {
        isSupported = true;
      }

      if (!isSupported) {
        unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
            "This type of window frame is currently not supported \n" +
            "See Apache Drill JIRA: DRILL-3188");
        throw new UnsupportedOperationException();
      }

      // DRILL-3189: Disable DISALLOW PARTIAL
      if (!window.isAllowPartial()) {
        unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
            "Disallowing partial windows is currently not supported \n" +
            "See Apache Drill JIRA: DRILL-3189");
        throw new UnsupportedOperationException();
      }
    }

    // Disable unsupported Intersect, Except
    if (sqlCall.getKind() == SqlKind.INTERSECT || sqlCall.getKind() == SqlKind.EXCEPT) {
      unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.RELATIONAL,
          sqlCall.getOperator().getName() + " is not supported\n" +
          "See Apache Drill JIRA: DRILL-1921");
      throw new UnsupportedOperationException();
    }

    // Disable unsupported JOINs
    if (sqlCall.getKind() == SqlKind.JOIN) {
      SqlJoin join = (SqlJoin) sqlCall;

      // Block Natural Join
      if (join.isNatural()) {
        unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.RELATIONAL,
            "NATURAL JOIN is not supported\n" +
            "See Apache Drill JIRA: DRILL-1986");
        throw new UnsupportedOperationException();
      }
    }

    //Disable UNNEST if the configuration disable it
    if (sqlCall.getKind() == SqlKind.UNNEST) {
      if (!context.getPlannerSettings().isUnnestLateralEnabled()) {
        unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.RELATIONAL,
            "Unnest is not enabled per configuration");
        throw new UnsupportedOperationException();
      }
    }

    // Disable Function
    for (String strOperator : disabledOperators) {
      if (sqlCall.getOperator().isName(strOperator, true)) { // true is passed to preserve previous behavior
        unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
            sqlCall.getOperator().getName() + " is not supported\n" +
            "See Apache Drill JIRA: DRILL-2115");
        throw new UnsupportedOperationException();
      }
    }

    // Disable complex functions incorrect placement
    if (sqlCall instanceof SqlSelect) {
      SqlSelect sqlSelect = (SqlSelect) sqlCall;

      for (SqlNode nodeInSelectList : sqlSelect.getSelectList()) {
        if (checkDirExplorers(nodeInSelectList)) {
          unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
              "Directory explorers " + dirExplorers + " functions are not supported in Select List\n" +
                  "See Apache Drill JIRA: DRILL-3944");
          throw new UnsupportedOperationException();
        }
      }

      if (sqlSelect.hasWhere()) {
        if (checkDirExplorers(sqlSelect.getWhere()) && !context.getPlannerSettings().isConstantFoldingEnabled()) {
          unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
              "Directory explorers " + dirExplorers + " functions can not be used " +
                  "when " + PlannerSettings.CONSTANT_FOLDING.getOptionName() + " option is set to false\n" +
                  "See Apache Drill JIRA: DRILL-3944");
          throw new UnsupportedOperationException();
        }
      }

      if (sqlSelect.hasOrderBy()) {
        for (SqlNode sqlNode : sqlSelect.getOrderList()) {
          if (containsFlatten(sqlNode)) {
            unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                "Flatten function is not supported in Order By\n" +
                "See Apache Drill JIRA: DRILL-2181");
            throw new UnsupportedOperationException();
          } else if (checkDirExplorers(sqlNode)) {
            unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                "Directory explorers " + dirExplorers + " functions are not supported in Order By\n" +
                "See Apache Drill JIRA: DRILL-3944");
            throw new UnsupportedOperationException();
          }
        }
      }

      if (sqlSelect.getGroup() != null) {
        for (SqlNode sqlNode : sqlSelect.getGroup()) {
          if (containsFlatten(sqlNode)) {
            unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                "Flatten function is not supported in Group By\n" +
                "See Apache Drill JIRA: DRILL-2181");
            throw new UnsupportedOperationException();
          } else if (checkDirExplorers(sqlNode)) {
                unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                "Directory explorers " + dirExplorers + " functions are not supported in Group By\n" +
                "See Apache Drill JIRA: DRILL-3944");
            throw new UnsupportedOperationException();
          }
        }
      }

      if (sqlSelect.isDistinct()) {
        for (SqlNode column : sqlSelect.getSelectList()) {
          if (column.getKind() ==  SqlKind.AS) {
            if (containsFlatten(((SqlCall) column).getOperandList().get(0))) {
              unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                  "Flatten function is not supported in Distinct\n" +
                  "See Apache Drill JIRA: DRILL-2181");
              throw new UnsupportedOperationException();
            }
          } else {
            if (containsFlatten(column)) {
              unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
                  "Flatten function is not supported in Distinct\n" +
                  "See Apache Drill JIRA: DRILL-2181");
              throw new UnsupportedOperationException();
            }
          }
        }
      }
    }

    if (DrillCalciteWrapperUtility.extractSqlOperatorFromWrapper(sqlCall.getOperator()) instanceof SqlCountAggFunction) {
      for (SqlNode sqlNode : sqlCall.getOperandList()) {
        if (containsFlatten(sqlNode)) {
          unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
              "Flatten function in aggregate functions is not supported\n" +
              "See Apache Drill JIRA: DRILL-2181");
          throw new UnsupportedOperationException();
        }
      }
    }

    return sqlCall.getOperator().acceptCall(this, sqlCall);
  }

  private void checkRollupCubeGrpSets(SqlSelect sqlSelect) {
    final ExprFinder rollupCubeGrpSetsFinder = new ExprFinder(RollupCubeGrpSets);
    sqlSelect.accept(rollupCubeGrpSetsFinder);
    if (rollupCubeGrpSetsFinder.find()) {
      unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
          "Rollup, Cube, Grouping Sets are not supported in GROUP BY clause.\n" +
              "See Apache Drill JIRA: DRILL-3962");
      throw new UnsupportedOperationException();
    }
  }

  private void checkGrouping(SqlSelect sqlSelect) {
    final ExprFinder groupingFinder = new ExprFinder(GroupingID);
    sqlSelect.accept(groupingFinder);
    if (groupingFinder.find()) {
      unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
          "Grouping, Grouping_ID, Group_ID are not supported.\n" +
              "See Apache Drill JIRA: DRILL-3962");
      throw new UnsupportedOperationException();
    }
  }

  private boolean checkDirExplorers(SqlNode sqlNode) {
    final ExprFinder dirExplorersFinder = new ExprFinder(DirExplorersCondition);
    sqlNode.accept(dirExplorersFinder);
    return dirExplorersFinder.find();
  }

  /**
   * A function that replies true or false for a given expression.
   *
   * @see org.apache.calcite.rel.rules.PushProjector.OperatorExprCondition
   */
  private interface SqlNodeCondition {
    /**
     * Evaluates a condition for a given expression.
     *
     * @param sqlNode Expression
     * @return result of evaluating the condition
     */
    boolean test(SqlNode sqlNode);
  }

  /**
   * A condition that returns true if SqlNode has rollup, cube, grouping_sets.
   * */
  private final SqlNodeCondition RollupCubeGrpSets = new SqlNodeCondition() {
    @Override
    public boolean test(SqlNode sqlNode) {
      if (sqlNode instanceof SqlCall) {
        final SqlOperator operator = DrillCalciteWrapperUtility.extractSqlOperatorFromWrapper(((SqlCall) sqlNode).getOperator());
        if (operator == SqlStdOperatorTable.ROLLUP
            || operator == SqlStdOperatorTable.CUBE
            || operator == SqlStdOperatorTable.GROUPING_SETS) {
          return true;
        }
      }
      return false;
    }
  };

  /**
   * A condition that returns true if SqlNode has Grouping, Grouping_ID, GROUP_ID.
   */
  private final SqlNodeCondition GroupingID = new SqlNodeCondition() {
    @Override
    public boolean test(SqlNode sqlNode) {
      if (sqlNode instanceof SqlCall) {
        final SqlOperator operator = DrillCalciteWrapperUtility.extractSqlOperatorFromWrapper(((SqlCall) sqlNode).getOperator());
          if (operator == SqlStdOperatorTable.GROUPING
              || operator == SqlStdOperatorTable.GROUPING_ID
              || operator == SqlStdOperatorTable.GROUP_ID) {
          return true;
        }
      }
      return false;
    }
  };

  /**
   * A condition that returns true if SqlNode has Directory Explorers.
   */
  private final SqlNodeCondition DirExplorersCondition = new SqlNodeCondition() {
    @Override
    public boolean test(SqlNode sqlNode) {
      return sqlNode instanceof SqlCall && checkOperator((SqlCall) sqlNode, dirExplorers, true);
    }

    /**
     * Checks recursively if operator and its operands are present in provided list of operators
     */
    private boolean checkOperator(SqlCall sqlCall, List<String> operators, boolean checkOperator) {
      if (checkOperator) {
        return operators.contains(sqlCall.getOperator().getName().toUpperCase()) || checkOperator(sqlCall, operators, false);
      }
      for (SqlNode sqlNode : sqlCall.getOperandList()) {
        if (!(sqlNode instanceof SqlCall)) {
          continue;
        }
        if (checkOperator((SqlCall) sqlNode, operators, true)) {
          return true;
        }
      }
      return false;
    }

  };

  /**
   * A visitor to check if the given SqlNodeCondition is tested as true or not.
   * If the condition is true, mark flag 'find' as true.
   */
  private static class ExprFinder extends SqlBasicVisitor<Void> {
    private boolean find;
    private final SqlNodeCondition condition;

    public ExprFinder(SqlNodeCondition condition) {
      this.find = false;
      this.condition = condition;
    }

    public boolean find() {
      return this.find;
    }

    @Override
    public Void visit(SqlCall call) {
      if (this.condition.test(call)) {
        this.find = true;
      }
      return super.visit(call);
    }
  }

  private boolean containsFlatten(SqlNode sqlNode) throws UnsupportedOperationException {
    return sqlNode instanceof SqlCall
        && ((SqlCall) sqlNode).getOperator().getName().toLowerCase().equals("flatten");
  }

  /**
   * Disable multiple partitions in a SELECT-CLAUSE
   * If multiple partitions are defined in the query,
   * SqlUnsupportedException would be thrown to inform
   * @param sqlSelect SELECT-CLAUSE in the query
   */
  private void detectMultiplePartitions(SqlSelect sqlSelect) {
    for (SqlNode nodeInSelectList : sqlSelect.getSelectList()) {
      // If the window function is used with an alias,
      // enter the first operand of AS operator
      if (nodeInSelectList.getKind() == SqlKind.AS
          && (((SqlCall) nodeInSelectList).getOperandList().get(0).getKind() == SqlKind.OVER)) {
        nodeInSelectList = ((SqlCall) nodeInSelectList).getOperandList().get(0);
      }

      if (nodeInSelectList.getKind() != SqlKind.OVER) {
        continue;
      }

      // This is used to keep track of the window function which has been defined
      SqlNode definedWindow = null;
      SqlNode window = ((SqlCall) nodeInSelectList).operand(1);

      // Partition window is referenced as a SqlIdentifier,
      // which is defined in the window list
      if (window instanceof SqlIdentifier) {
        // Expand the SqlIdentifier as the expression defined in the window list
        for (SqlNode sqlNode : sqlSelect.getWindowList()) {
          if (((SqlWindow) sqlNode).getDeclName().equalsDeep(window, Litmus.IGNORE)) {
            window = sqlNode;
            break;
          }
        }

        assert !(window instanceof SqlIdentifier) : "Identifier should have been expanded as a window defined in the window list";
      }

      // In a SELECT-SCOPE, only a partition can be defined
      if (definedWindow == null) {
        definedWindow = window;
      } else {
        if (!definedWindow.equalsDeep(window, Litmus.IGNORE)) {
          unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.FUNCTION,
              "Multiple window definitions in a single SELECT list is not currently supported \n" +
              "See Apache Drill JIRA: DRILL-3196");
          throw new UnsupportedOperationException();
        }
      }
    }
  }
}