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
package org.apache.drill.exec.planner.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql2rel.SqlRexConvertlet;
import org.apache.calcite.sql2rel.SqlRexConvertletTable;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import org.apache.drill.exec.planner.sql.parser.DrillCalciteWrapperUtility;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

/**
 * Convertlet table which allows to plug-in custom rex conversion of calls to
 * Calcite's standard operators.
 */
public class DrillConvertletTable implements SqlRexConvertletTable {

  public static final SqlRexConvertletTable INSTANCE = new DrillConvertletTable();
  private static final DrillSqlOperator CastHighOp = new DrillSqlOperator("CastHigh", 1, false,
      new SqlReturnTypeInference() {
        @Override
        public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
          return TypeInferenceUtils.createCalciteTypeWithNullability(
              opBinding.getTypeFactory(),
              SqlTypeName.ANY,
              opBinding.getOperandType(0).isNullable());
        }
      }, false);

  private final Map<SqlOperator, SqlRexConvertlet> operatorToConvertletMap;

  private DrillConvertletTable() {
    operatorToConvertletMap = ImmutableMap.<SqlOperator, SqlRexConvertlet>builder()
        .put(SqlStdOperatorTable.EXTRACT, extractConvertlet())
        .put(SqlStdOperatorTable.SQRT, sqrtConvertlet())
        .put(SqlStdOperatorTable.COALESCE, coalesceConvertlet())
        .put(SqlStdOperatorTable.TIMESTAMP_DIFF, timestampDiffConvertlet())
        .put(SqlStdOperatorTable.ROW, rowConvertlet())
        .put(SqlStdOperatorTable.AVG, avgVarianceConvertlet(DrillConvertletTable::expandAvg))
        .put(SqlStdOperatorTable.STDDEV_POP, avgVarianceConvertlet(arg -> expandVariance(arg, true, true)))
        .put(SqlStdOperatorTable.STDDEV_SAMP, avgVarianceConvertlet(arg -> expandVariance(arg, false, true)))
        .put(SqlStdOperatorTable.STDDEV, avgVarianceConvertlet(arg -> expandVariance(arg, false, true)))
        .put(SqlStdOperatorTable.VAR_POP, avgVarianceConvertlet(arg -> expandVariance(arg, true, false)))
        .put(SqlStdOperatorTable.VAR_SAMP, avgVarianceConvertlet(arg -> expandVariance(arg, false, false)))
        .put(SqlStdOperatorTable.VARIANCE, avgVarianceConvertlet(arg -> expandVariance(arg, false, false)))
        .build();
  }

  /**
   * Lookup the hash table to see if we have a custom convertlet for a given
   * operator, if we don't use StandardConvertletTable.
   */
  @Override
  public SqlRexConvertlet get(SqlCall call) {

    SqlRexConvertlet convertlet;
    if (call.getOperator() instanceof DrillCalciteSqlWrapper) {
      final SqlOperator wrapper = call.getOperator();
      final SqlOperator wrapped = DrillCalciteWrapperUtility.extractSqlOperatorFromWrapper(call.getOperator());
      if ((convertlet = operatorToConvertletMap.get(wrapped)) != null) {
        return convertlet;
      }

      ((SqlBasicCall) call).setOperator(wrapped);
      SqlRexConvertlet sqlRexConvertlet = StandardConvertletTable.INSTANCE.get(call);
      ((SqlBasicCall) call).setOperator(wrapper);
      return sqlRexConvertlet;
    }
    if ((convertlet = operatorToConvertletMap.get(call.getOperator())) != null) {
      return convertlet;
    }
    return StandardConvertletTable.INSTANCE.get(call);
  }

  /**
   * Custom convertlet to handle extract functions. Calcite rewrites
   * extract functions as divide and modulo functions, based on the
   * data type. We cannot do that in Drill since we don't know the data type
   * till we start scanning. So we don't rewrite extract and treat it as
   * a regular function.
   */
  private static SqlRexConvertlet extractConvertlet() {
    return (cx, call) -> {
      List<SqlNode> operands = call.getOperandList();
      List<RexNode> exprs = new LinkedList<>();
      RelDataTypeFactory typeFactory = cx.getTypeFactory();

      for (SqlNode node : operands) {
        exprs.add(cx.convertExpression(node));
      }

      RelDataType returnType;
      if (call.getOperator() == SqlStdOperatorTable.EXTRACT) {
        // Legacy code:
        // The return type is wrong!
        // Legacy code choose SqlTypeName.BIGINT simply to avoid conflicting against Calcite's inference mechanism
        // (which chose BIGINT in validation phase already)
        returnType = typeFactory.createSqlType(SqlTypeName.BIGINT);
      } else {
        String timeUnit = ((SqlIntervalQualifier) operands.get(0)).timeUnitRange.toString();
        returnType = typeFactory.createSqlType(TypeInferenceUtils.getSqlTypeNameForTimeUnit(timeUnit));
      }
      // Determine nullability using 2nd argument.
      returnType = typeFactory.createTypeWithNullability(returnType, exprs.get(1).getType().isNullable());
      return cx.getRexBuilder().makeCall(returnType, call.getOperator(), exprs);
    };
  }

  /**
   * SQRT needs it's own convertlet because calcite overrides it to POWER(x, 0.5)
   * which is not suitable for Infinity value case
   */
  private static SqlRexConvertlet sqrtConvertlet() {
    return (cx, call) -> {
      RexNode operand = cx.convertExpression(call.operand(0));
      return cx.getRexBuilder().makeCall(SqlStdOperatorTable.SQRT, operand);
    };
  }

  /**
   * Rewrites COALESCE function into CASE WHEN IS NOT NULL operand1 THEN operand1...
   * all Calcite interval representations correctly.
   * Custom convertlet to avoid rewriting TIMESTAMP_DIFF by Calcite,
   * since Drill does not support Reinterpret function and does not handle
   */
  private static SqlRexConvertlet coalesceConvertlet() {
    return (cx, call) -> {
      int operandsCount = call.operandCount();
      if (operandsCount == 1) {
        return cx.convertExpression(call.operand(0));
      } else {
        List<RexNode> caseOperands = new ArrayList<>();
        for (int i = 0; i < operandsCount - 1; i++) {
          RexNode caseOperand = cx.convertExpression(call.operand(i));
          caseOperands.add(cx.getRexBuilder().makeCall(
              SqlStdOperatorTable.IS_NOT_NULL, caseOperand));
          caseOperands.add(caseOperand);
        }
        caseOperands.add(cx.convertExpression(call.operand(operandsCount - 1)));
        return cx.getRexBuilder().makeCall(SqlStdOperatorTable.CASE, caseOperands);
      }
    };
  }

  private static SqlRexConvertlet timestampDiffConvertlet() {
    return (cx, call) -> {
      SqlLiteral unitLiteral = call.operand(0);
      SqlIntervalQualifier qualifier =
          new SqlIntervalQualifier(unitLiteral.symbolValue(TimeUnit.class), null, SqlParserPos.ZERO);

      List<RexNode> operands = Arrays.asList(
          cx.convertExpression(qualifier),
          cx.convertExpression(call.operand(1)),
          cx.convertExpression(call.operand(2)));

      RelDataTypeFactory typeFactory = cx.getTypeFactory();

      RelDataType returnType = typeFactory.createTypeWithNullability(
          typeFactory.createSqlType(SqlTypeName.BIGINT),
          cx.getValidator().getValidatedNodeType(call.operand(1)).isNullable()
              || cx.getValidator().getValidatedNodeType(call.operand(2)).isNullable());

      return cx.getRexBuilder().makeCall(returnType,
          SqlStdOperatorTable.TIMESTAMP_DIFF, operands);
    };
  }

  private static SqlRexConvertlet rowConvertlet() {
    return (cx, call) -> {
      List<RexNode> args = call.getOperandList().stream()
          .map(cx::convertExpression)
          .collect(Collectors.toList());
      return cx.getRexBuilder().makeCall(SqlStdOperatorTable.ROW, args);
    };
  }

  private static SqlRexConvertlet avgVarianceConvertlet(Function<SqlNode, SqlNode> expandFunc) {
    return (cx, call) -> cx.convertExpression(expandFunc.apply(call.operand(0)));
  }

  private static SqlNode expandAvg(final SqlNode arg) {
    SqlNode sum = DrillCalciteSqlAggFunctionWrapper.SUM.createCall(SqlParserPos.ZERO, arg);
    SqlNode count = SqlStdOperatorTable.COUNT.createCall(SqlParserPos.ZERO, arg);
    SqlNode sumAsDouble = CastHighOp.createCall(SqlParserPos.ZERO, sum);
    return SqlStdOperatorTable.DIVIDE.createCall(SqlParserPos.ZERO, sumAsDouble, count);
  }

  /**
   * This code is adapted from calcite's AvgVarianceConvertlet. The difference being
   * we add a cast to double before we perform the division. The reason we have a separate implementation
   * from calcite's code is because while injecting a similar cast, calcite will look
   * at the output type of the aggregate function which will be 'ANY' at that point and will
   * inject a cast to 'ANY' which does not solve the problem.
   */
  private static SqlNode expandVariance(SqlNode arg, boolean biased, boolean sqrt) {
    /* stddev_pop(x) ==>
     *   power(
     *    (sum(x * x) - sum(x) * sum(x) / count(x))
     *    / count(x),
     *    .5)

     * stddev_samp(x) ==>
     *  power(
     *    (sum(x * x) - sum(x) * sum(x) / count(x))
     *    / (count(x) - 1),
     *    .5)

     * var_pop(x) ==>
     *    (sum(x * x) - sum(x) * sum(x) / count(x))
     *    / count(x)

     * var_samp(x) ==>
     *    (sum(x * x) - sum(x) * sum(x) / count(x))
     *    / (count(x) - 1)
     */
    final SqlParserPos pos = SqlParserPos.ZERO;

    // cast the argument to double
    final SqlNode castHighArg = CastHighOp.createCall(pos, arg);
    final SqlNode argSquared =
        SqlStdOperatorTable.MULTIPLY.createCall(pos, castHighArg, castHighArg);
    final SqlNode sumArgSquared =
        DrillCalciteSqlAggFunctionWrapper.SUM.createCall(pos, argSquared);
    final SqlNode sum =
        DrillCalciteSqlAggFunctionWrapper.SUM.createCall(pos, castHighArg);
    final SqlNode sumSquared =
        SqlStdOperatorTable.MULTIPLY.createCall(pos, sum, sum);
    final SqlNode count =
        SqlStdOperatorTable.COUNT.createCall(pos, castHighArg);
    final SqlNode avgSumSquared =
        SqlStdOperatorTable.DIVIDE.createCall(
            pos, sumSquared, count);
    final SqlNode diff =
        SqlStdOperatorTable.MINUS.createCall(
            pos, sumArgSquared, avgSumSquared);
    final SqlNode denominator;
    if (biased) {
      denominator = count;
    } else {
      final SqlNumericLiteral one =
          SqlLiteral.createExactNumeric("1", pos);
      denominator =
          SqlStdOperatorTable.MINUS.createCall(
              pos, count, one);
    }
    final SqlNode diffAsDouble =
        CastHighOp.createCall(pos, diff);
    final SqlNode div =
        SqlStdOperatorTable.DIVIDE.createCall(
            pos, diffAsDouble, denominator);
    SqlNode result = div;
    if (sqrt) {
      final SqlNumericLiteral half =
          SqlLiteral.createExactNumeric("0.5", pos);
      result =
          SqlStdOperatorTable.POWER.createCall(pos, div, half);
    }
    return result;
  }
}
