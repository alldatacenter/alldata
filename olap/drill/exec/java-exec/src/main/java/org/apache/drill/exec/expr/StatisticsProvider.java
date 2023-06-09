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
package org.apache.drill.exec.expr;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedFieldExpr;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.expression.fn.FuncHolder;
import org.apache.drill.common.expression.fn.FunctionReplacementUtils;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.fn.DrillSimpleFuncHolder;
import org.apache.drill.exec.expr.fn.interpreter.InterpreterEvaluator;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.vector.ValueHolderHelper;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class StatisticsProvider<T extends Comparable<T>> extends AbstractExprVisitor<ColumnStatistics<?>, Void, RuntimeException> {

  private final Map<SchemaPath, ColumnStatistics<?>> columnStatMap;
  private final long rowCount;

  public StatisticsProvider(Map<SchemaPath, ColumnStatistics<?>> columnStatMap, long rowCount) {
    this.columnStatMap = columnStatMap;
    this.rowCount = rowCount;
  }

  public long getRowCount() {
    return this.rowCount;
  }

  @Override
  public ColumnStatistics<?> visitUnknown(LogicalExpression e, Void value) {
    // do nothing for the unknown expression
    return null;
  }

  @Override
  public ColumnStatistics<?> visitTypedFieldExpr(TypedFieldExpr typedFieldExpr, Void value) {
    ColumnStatistics<?> columnStatistics = columnStatMap.get(typedFieldExpr.getPath().getUnIndexed());
    if (columnStatistics != null) {
      return columnStatistics;
    } else if (typedFieldExpr.getMajorType().equals(Types.OPTIONAL_INT)) {
      // field does not exist.
      return StatisticsProvider.getColumnStatistics(null, null, rowCount, typedFieldExpr.getMajorType().getMinorType());
    } else {
      return null;
    }
  }

  @Override
  public ColumnStatistics<Integer> visitIntConstant(ValueExpressions.IntExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getInt(), expr);
  }

  @Override
  public ColumnStatistics<Boolean> visitBooleanConstant(ValueExpressions.BooleanExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getBoolean(), expr);
  }

  @Override
  public ColumnStatistics<Long> visitLongConstant(ValueExpressions.LongExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getLong(), expr);
  }

  @Override
  public ColumnStatistics<Float> visitFloatConstant(ValueExpressions.FloatExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getFloat(), expr);
  }

  @Override
  public ColumnStatistics<Double> visitDoubleConstant(ValueExpressions.DoubleExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getDouble(), expr);
  }

  @Override
  public ColumnStatistics<Long> visitDateConstant(ValueExpressions.DateExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getDate(), expr);
  }

  @Override
  public ColumnStatistics<Long> visitTimeStampConstant(ValueExpressions.TimeStampExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getTimeStamp(), expr);
  }

  @Override
  public ColumnStatistics<Integer> visitTimeConstant(ValueExpressions.TimeExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getTime(), expr);
  }

  @Override
  public ColumnStatistics<String> visitQuotedStringConstant(ValueExpressions.QuotedString expr, Void value) {
    return getConstantColumnStatistics(expr.getString(), expr);
  }

  @Override
  public ColumnStatistics<BigInteger> visitVarDecimalConstant(ValueExpressions.VarDecimalExpression expr, Void value) {
    return getConstantColumnStatistics(expr.getBigDecimal().unscaledValue(), expr);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ColumnStatistics<?> visitFunctionHolderExpression(FunctionHolderExpression holderExpr, Void value) {
    FuncHolder funcHolder = holderExpr.getHolder();

    if (!(funcHolder instanceof DrillSimpleFuncHolder)) {
      // Only Drill function is allowed.
      return null;
    }

    String funcName = ((DrillSimpleFuncHolder) funcHolder).getRegisteredNames()[0];

    if (FunctionReplacementUtils.isCastFunction(funcName)) {
      ColumnStatistics<T> stat = (ColumnStatistics<T>) holderExpr.args.get(0).accept(this, null);
      if (!IsPredicate.isNullOrEmpty(stat)) {
        return evalCastFunc(holderExpr, stat);
      }
    }
    return null;
  }

  private ColumnStatistics<?> evalCastFunc(FunctionHolderExpression holderExpr, ColumnStatistics<T> input) {
    try {
      DrillSimpleFuncHolder funcHolder = (DrillSimpleFuncHolder) holderExpr.getHolder();

      DrillSimpleFunc interpreter = funcHolder.createInterpreter();

      ValueHolder minHolder;
      ValueHolder maxHolder;

      TypeProtos.MinorType srcType = holderExpr.args.get(0).getMajorType().getMinorType();
      TypeProtos.MinorType destType = holderExpr.getMajorType().getMinorType();

      if (srcType.equals(destType)) {
        // same type cast ==> NoOp.
        return input;
      } else if (!CAST_FUNC.containsKey(srcType) || !CAST_FUNC.get(srcType).contains(destType)) {
        return null; // cast func between srcType and destType is NOT allowed.
      }

      switch (srcType) {
        case INT :
          minHolder = ValueHolderHelper.getIntHolder((Integer) ComparisonPredicate.getMinValue(input));
          maxHolder = ValueHolderHelper.getIntHolder((Integer) ComparisonPredicate.getMaxValue(input));
          break;
        case BIGINT:
          minHolder = ValueHolderHelper.getBigIntHolder((Long) ComparisonPredicate.getMinValue(input));
          maxHolder = ValueHolderHelper.getBigIntHolder((Long) ComparisonPredicate.getMaxValue(input));
          break;
        case FLOAT4:
          minHolder = ValueHolderHelper.getFloat4Holder((Float) ComparisonPredicate.getMinValue(input));
          maxHolder = ValueHolderHelper.getFloat4Holder((Float) ComparisonPredicate.getMaxValue(input));
          break;
        case FLOAT8:
          minHolder = ValueHolderHelper.getFloat8Holder((Double) ComparisonPredicate.getMinValue(input));
          maxHolder = ValueHolderHelper.getFloat8Holder((Double) ComparisonPredicate.getMaxValue(input));
          break;
        case DATE:
          minHolder = ValueHolderHelper.getDateHolder((Long) ComparisonPredicate.getMinValue(input));
          maxHolder = ValueHolderHelper.getDateHolder((Long) ComparisonPredicate.getMaxValue(input));
          break;
        default:
          return null;
      }

      ValueHolder[] args1 = {minHolder};
      ValueHolder[] args2 = {maxHolder};

      ValueHolder minFuncHolder = InterpreterEvaluator.evaluateFunction(interpreter, args1, holderExpr.getName());
      ValueHolder maxFuncHolder = InterpreterEvaluator.evaluateFunction(interpreter, args2, holderExpr.getName());

      switch (destType) {
        case INT:
          return StatisticsProvider.getColumnStatistics(
              ((IntHolder) minFuncHolder).value,
              ((IntHolder) maxFuncHolder).value,
              ColumnStatisticsKind.NULLS_COUNT.getFrom(input),
              destType);
        case BIGINT:
          return StatisticsProvider.getColumnStatistics(
              ((BigIntHolder) minFuncHolder).value,
              ((BigIntHolder) maxFuncHolder).value,
              ColumnStatisticsKind.NULLS_COUNT.getFrom(input),
              destType);
        case FLOAT4:
          return StatisticsProvider.getColumnStatistics(
              ((Float4Holder) minFuncHolder).value,
              ((Float4Holder) maxFuncHolder).value,
              ColumnStatisticsKind.NULLS_COUNT.getFrom(input),
              destType);
        case FLOAT8:
          return StatisticsProvider.getColumnStatistics(
              ((Float8Holder) minFuncHolder).value,
              ((Float8Holder) maxFuncHolder).value,
              ColumnStatisticsKind.NULLS_COUNT.getFrom(input),
              destType);
        case TIMESTAMP:
          return StatisticsProvider.getColumnStatistics(
              ((TimeStampHolder) minFuncHolder).value,
              ((TimeStampHolder) maxFuncHolder).value,
              ColumnStatisticsKind.NULLS_COUNT.getFrom(input),
              destType);
        default:
          return null;
      }
    } catch (Exception e) {
      throw new DrillRuntimeException("Error in evaluating function of " + holderExpr.getName());
    }
  }

  /**
   * Returns {@link ColumnStatistics} instance with set min, max values and nulls count statistics specified in the arguments.
   *
   * @param minVal     min value
   * @param maxVal     max value
   * @param nullsCount nulls count
   * @param type       type of the column
   * @param <V>        type of min and max values
   * @return {@link ColumnStatistics} instance with set min, max values and nulls count statistics
   */
  public static <V> ColumnStatistics<V> getColumnStatistics(V minVal, V maxVal, long nullsCount, TypeProtos.MinorType type) {
    return new ColumnStatistics<>(
        Arrays.asList(new StatisticsHolder<>(minVal, ColumnStatisticsKind.MIN_VALUE),
            new StatisticsHolder<>(maxVal, ColumnStatisticsKind.MAX_VALUE),
            new StatisticsHolder<>(nullsCount, ColumnStatisticsKind.NULLS_COUNT)),
        type);
  }

  /**
   * Returns {@link ColumnStatistics} instance with min and max values set to {@code minMaxValue}
   * and nulls count set to 0. Resulting {@link ColumnStatistics} instance corresponds
   * to a constant value, so nulls count is set to 0.
   *
   * @param minMaxValue value of min and max statistics
   * @param expr        source of column type
   * @param <V>         type of min and max values
   * @return {@link ColumnStatistics} instance with min and max values set to {@code minMaxValue} and nulls count set to 0
   */
  public static <V> ColumnStatistics<V> getConstantColumnStatistics(V minMaxValue, LogicalExpression expr) {
    return getConstantColumnStatistics(minMaxValue, expr.getMajorType().getMinorType());
  }

  /**
   * Returns {@link ColumnStatistics} instance with min and max values set to {@code minMaxValue}
   * and nulls count set to 0. Resulting {@link ColumnStatistics} instance corresponds
   * to a constant value, so nulls count is set to 0.
   *
   * @param minMaxValue value of min and max statistics
   * @param type        column type
   * @param <V>         type of min and max values
   * @return {@link ColumnStatistics} instance with min and max values set to {@code minMaxValue} and nulls count set to 0
   */
  public static <V> ColumnStatistics<V> getConstantColumnStatistics(V minMaxValue, TypeProtos.MinorType type) {
    return getColumnStatistics(minMaxValue, minMaxValue, 0, type);
  }

  private static final Map<TypeProtos.MinorType, Set<TypeProtos.MinorType>> CAST_FUNC = new EnumMap<>(TypeProtos.MinorType.class);
  static {
    // float -> double , int, bigint
    Set<TypeProtos.MinorType> float4Types = EnumSet.noneOf(TypeProtos.MinorType.class);
    CAST_FUNC.put(TypeProtos.MinorType.FLOAT4, float4Types);
    float4Types.add(TypeProtos.MinorType.FLOAT8);
    float4Types.add(TypeProtos.MinorType.INT);
    float4Types.add(TypeProtos.MinorType.BIGINT);

    // double -> float, int, bigint
    Set<TypeProtos.MinorType> float8Types = EnumSet.noneOf(TypeProtos.MinorType.class);
    CAST_FUNC.put(TypeProtos.MinorType.FLOAT8, float8Types);
    float8Types.add(TypeProtos.MinorType.FLOAT4);
    float8Types.add(TypeProtos.MinorType.INT);
    float8Types.add(TypeProtos.MinorType.BIGINT);

    // int -> float, double, bigint
    Set<TypeProtos.MinorType> intTypes = EnumSet.noneOf(TypeProtos.MinorType.class);
    CAST_FUNC.put(TypeProtos.MinorType.INT, intTypes);
    intTypes.add(TypeProtos.MinorType.FLOAT4);
    intTypes.add(TypeProtos.MinorType.FLOAT8);
    intTypes.add(TypeProtos.MinorType.BIGINT);

    // bigint -> int, float, double
    Set<TypeProtos.MinorType> bigIntTypes = EnumSet.noneOf(TypeProtos.MinorType.class);
    CAST_FUNC.put(TypeProtos.MinorType.BIGINT, bigIntTypes);
    bigIntTypes.add(TypeProtos.MinorType.INT);
    bigIntTypes.add(TypeProtos.MinorType.FLOAT4);
    bigIntTypes.add(TypeProtos.MinorType.FLOAT8);

    // date -> timestamp
    Set<TypeProtos.MinorType> dateTypes = EnumSet.noneOf(TypeProtos.MinorType.class);
    CAST_FUNC.put(TypeProtos.MinorType.DATE, dateTypes);
    dateTypes.add(TypeProtos.MinorType.TIMESTAMP);
  }
}
