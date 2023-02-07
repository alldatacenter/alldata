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

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.LogicalExpressionBase;
import org.apache.drill.common.expression.TypedFieldExpr;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.expr.stat.RowsMatch;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

public class IsPredicate<C extends Comparable<C>> extends LogicalExpressionBase implements FilterPredicate<C> {
  private static final Logger logger = LoggerFactory.getLogger(IsPredicate.class);

  private final LogicalExpression expr;

  private final BiFunction<ColumnStatistics<C>, StatisticsProvider<C>, RowsMatch> predicate;

  private IsPredicate(LogicalExpression expr,
                      BiFunction<ColumnStatistics<C>, StatisticsProvider<C>, RowsMatch> predicate) {
    super(expr.getPosition());
    this.expr = expr;
    this.predicate = predicate;
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    final List<LogicalExpression> args = new ArrayList<>();
    args.add(expr);
    return args.iterator();
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitUnknown(this, value);
  }

  /**
   * Apply the filter condition against the meta of the rowgroup.
   */
  @Override
  public RowsMatch matches(StatisticsProvider<C> evaluator) {
    @SuppressWarnings("unchecked")
    ColumnStatistics<C> exprStat = (ColumnStatistics<C>) expr.accept(evaluator, null);
    return isNullOrEmpty(exprStat) ? RowsMatch.SOME : predicate.apply(exprStat, evaluator);
  }

  /**
   * @param stat statistics object
   * @return <tt>true</tt> if the input stat object is null or has invalid statistics; false otherwise
   */
  public static boolean isNullOrEmpty(ColumnStatistics<?> stat) {
    return stat == null
        || !stat.contains(ColumnStatisticsKind.MIN_VALUE)
        || !stat.contains(ColumnStatisticsKind.MAX_VALUE)
        || !stat.contains(ColumnStatisticsKind.NULLS_COUNT)
        || ColumnStatisticsKind.NULLS_COUNT.getFrom(stat) == Statistic.NO_COLUMN_STATS;
  }

  /**
   * After the applying of the filter against the statistics of the rowgroup, if the result is RowsMatch.ALL,
   * then we still must know if the rowgroup contains some null values, because it can change the filter result.
   * If it contains some null values, then we change the RowsMatch.ALL into RowsMatch.SOME, which sya that maybe
   * some values (the null ones) should be disgarded.
   */
  private static RowsMatch checkNull(ColumnStatistics<?> exprStat) {
    return hasNoNulls(exprStat) ? RowsMatch.ALL : RowsMatch.SOME;
  }

  /**
   * Checks that column chunk's statistics does not have nulls
   *
   * @param stat column statistics
   * @return <tt>true</tt> if the statistics does not have nulls and <tt>false</tt> otherwise
   */
  static boolean hasNoNulls(ColumnStatistics<?> stat) {
    return ColumnStatisticsKind.NULLS_COUNT.getFrom(stat) == 0;
  }

  /**
   * IS NULL predicate.
   */
  private static <C extends Comparable<C>> LogicalExpression createIsNullPredicate(LogicalExpression expr) {
    return new IsPredicate<C>(expr,
      (exprStat, evaluator) -> {
        // for arrays we are not able to define exact number of nulls
        // [1,2,3] vs [1,2] -> in second case 3 is absent and thus it's null but statistics shows no nulls
        if (expr instanceof TypedFieldExpr) {
          TypedFieldExpr typedFieldExpr = (TypedFieldExpr) expr;
          if (typedFieldExpr.getPath().isArray()) {
            return RowsMatch.SOME;
          }
        }
        if (hasNoNulls(exprStat)) {
          return RowsMatch.NONE;
        }
        return isAllNulls(exprStat, evaluator.getRowCount()) ? RowsMatch.ALL : RowsMatch.SOME;
      });
  }

  /**
   * Checks that column chunk's statistics has only nulls.
   * <p/>
   * Besides comparing number of nulls, we need to check
   * if min and max values are also nulls to cover use cases for arrays,
   * since array can hold N number of elements and nulls statistics
   * is collected for all elements, thus number of nulls may be greater
   * or equal to the number of rows.
   * <p/>
   * Two rows: [null, {"id": 1}], [null, {"id": 2}]
   * <br/>
   * Statistics: rows => 2, nulls => 2, min => 1, max => 2
   *
   * @param stat column statistics
   * @param rowCount number of rows of the specified statistics
   * @param <T> type of column values
   * @return <tt>true</tt> if all rows are null, <tt>false</tt> otherwise
   */
  static <T> boolean isAllNulls(ColumnStatistics<T> stat, long rowCount) {
    Preconditions.checkArgument(rowCount >= 0, "negative rowCount %d is not valid", rowCount);
    return ColumnStatisticsKind.NULLS_COUNT.getFrom(stat) >= rowCount
      && ColumnStatisticsKind.MIN_VALUE.getValueStatistic(stat) == null
      && ColumnStatisticsKind.MAX_VALUE.getValueStatistic(stat) == null;
  }

  static <T> boolean hasNonNullValues(ColumnStatistics<T> stat, long rowCount) {
    return rowCount > ColumnStatisticsKind.NULLS_COUNT.getFrom(stat)
        && ColumnStatisticsKind.MIN_VALUE.getValueStatistic(stat) != null
        && ColumnStatisticsKind.MAX_VALUE.getValueStatistic(stat) != null;
  }

  /**
   * IS NOT NULL predicate.
   */
  private static <C extends Comparable<C>> LogicalExpression createIsNotNullPredicate(LogicalExpression expr) {
    return new IsPredicate<C>(expr,
      (exprStat, evaluator) -> isAllNulls(exprStat, evaluator.getRowCount()) ? RowsMatch.NONE : checkNull(exprStat)
    );
  }

  /**
   * IS TRUE predicate.
   */
  private static LogicalExpression createIsTruePredicate(LogicalExpression expr) {
    return new IsPredicate<Boolean>(expr, (exprStat, evaluator) -> {
      if (isAllNulls(exprStat, evaluator.getRowCount())) {
        return RowsMatch.NONE;
      }
      if (!hasNonNullValues(exprStat, evaluator.getRowCount())) {
        return RowsMatch.SOME;
      }
      if (!ColumnStatisticsKind.MAX_VALUE.getValueStatistic(exprStat)) {
        return RowsMatch.NONE;
      }
      return ColumnStatisticsKind.MIN_VALUE.getValueStatistic(exprStat) ? checkNull(exprStat) : RowsMatch.SOME;
    });
  }

  /**
   * IS FALSE predicate.
   */
  private static LogicalExpression createIsFalsePredicate(LogicalExpression expr) {
    return new IsPredicate<Boolean>(expr, (exprStat, evaluator) -> {
      if (isAllNulls(exprStat, evaluator.getRowCount())) {
        return RowsMatch.NONE;
      }
      if (!hasNonNullValues(exprStat, evaluator.getRowCount())) {
        return RowsMatch.SOME;
      }
      if (ColumnStatisticsKind.MIN_VALUE.getValueStatistic(exprStat)) {
        return RowsMatch.NONE;
      }
      return ColumnStatisticsKind.MAX_VALUE.getValueStatistic(exprStat) ? RowsMatch.SOME : checkNull(exprStat);
    });
  }

  /**
   * IS NOT TRUE predicate.
   */
  private static LogicalExpression createIsNotTruePredicate(LogicalExpression expr) {
    return new IsPredicate<Boolean>(expr, (exprStat, evaluator) -> {
      if (isAllNulls(exprStat, evaluator.getRowCount())) {
        return RowsMatch.ALL;
      }
      if (!hasNonNullValues(exprStat, evaluator.getRowCount())) {
        return RowsMatch.SOME;
      }
      if (ColumnStatisticsKind.MIN_VALUE.getValueStatistic(exprStat)) {
        return hasNoNulls(exprStat) ? RowsMatch.NONE : RowsMatch.SOME;
      }
      return ColumnStatisticsKind.MAX_VALUE.getValueStatistic(exprStat) ? RowsMatch.SOME : RowsMatch.ALL;
    });
  }

  /**
   * IS NOT FALSE predicate.
   */
  private static LogicalExpression createIsNotFalsePredicate(LogicalExpression expr) {
    return new IsPredicate<Boolean>(expr, (exprStat, evaluator) -> {
      if (isAllNulls(exprStat, evaluator.getRowCount())) {
        return RowsMatch.ALL;
      }
      if (!hasNonNullValues(exprStat, evaluator.getRowCount())) {
        return RowsMatch.SOME;
      }
      if (!ColumnStatisticsKind.MAX_VALUE.getValueStatistic(exprStat)) {
        return hasNoNulls(exprStat) ? RowsMatch.NONE : RowsMatch.SOME;
      }
      return ColumnStatisticsKind.MIN_VALUE.getValueStatistic(exprStat) ? RowsMatch.ALL : RowsMatch.SOME;
    });
  }

  public static <C extends Comparable<C>> LogicalExpression createIsPredicate(String function, LogicalExpression expr) {
    switch (function) {
      case FunctionGenerationHelper.IS_NULL:
        return IsPredicate.<C>createIsNullPredicate(expr);
      case FunctionGenerationHelper.IS_NOT_NULL:
        return IsPredicate.<C>createIsNotNullPredicate(expr);
      case FunctionGenerationHelper.IS_TRUE:
        return createIsTruePredicate(expr);
      case FunctionGenerationHelper.IS_NOT_TRUE:
        return createIsNotTruePredicate(expr);
      case FunctionGenerationHelper.IS_FALSE:
      case FunctionGenerationHelper.NOT:
        return createIsFalsePredicate(expr);
      case FunctionGenerationHelper.IS_NOT_FALSE:
        return createIsNotFalsePredicate(expr);
      default:
        logger.warn("Unhandled IS function. Function name: {}", function);
        return null;
    }
  }
}
