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

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.exec.expr.stat.RowsMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class BooleanPredicate<C extends Comparable<C>> extends BooleanOperator implements FilterPredicate<C> {
  private static final Logger logger = LoggerFactory.getLogger(BooleanPredicate.class);

  private BooleanPredicate(String name, List<LogicalExpression> args, ExpressionPosition pos) {
    super(name, args, pos);
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitBooleanOperator(this, value);
  }

  @SuppressWarnings("unchecked")
  private static <C extends Comparable<C>> LogicalExpression createAndPredicate(
      String name, List<LogicalExpression> args, ExpressionPosition pos) {
    return new BooleanPredicate<C>(name, args, pos) {
      /**
       * Evaluates a compound "AND" filter on the statistics of a RowGroup (the filter reads "filterA and filterB").
       * Return value :<ul>
       *   <li>ALL : only if all filters return ALL
       *   <li>NONE : if one filter at least returns NONE
       *   <li>SOME : all other cases
       * </ul>
       */
      @Override
      public RowsMatch matches(StatisticsProvider<C> evaluator) {
        RowsMatch resultMatch = RowsMatch.ALL;
        for (LogicalExpression child : this) {
          if (child instanceof FilterPredicate) {
            switch (((FilterPredicate<C>) child).matches(evaluator)) {
              case NONE:
                return RowsMatch.NONE;  // No row comply to 1 filter part => can drop RG
              case SOME:
                resultMatch = RowsMatch.SOME;
              default: // Do nothing
            }
          }
        }
        return resultMatch;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <C extends Comparable<C>> LogicalExpression createOrPredicate(
      String name, List<LogicalExpression> args, ExpressionPosition pos) {
    return new BooleanPredicate<C>(name, args, pos) {
      /**
       * Evaluates a compound "OR" filter on the statistics of a RowGroup (the filter reads "filterA or filterB").
       * Return value :<ul>
       *   <li>NONE : only if all filters return NONE
       *   <li>ALL : if one filter at least returns ALL
       *   <li>SOME : all other cases
       * </ul>
       */
      @Override
      public RowsMatch matches(StatisticsProvider<C> evaluator) {
        RowsMatch resultMatch = RowsMatch.NONE;
        for (LogicalExpression child : this) {
          if (child instanceof FilterPredicate) {
            switch (((FilterPredicate<C>) child).matches(evaluator)) {
              case ALL:
                return RowsMatch.ALL;  // One at least is ALL => can drop filter but not RG
              case SOME:
                resultMatch = RowsMatch.SOME;
              default: // Do nothing
            }
          }
        }
        return resultMatch;
      }
    };
  }

  public static <C extends Comparable<C>> LogicalExpression createBooleanPredicate(
      String function, String name, List<LogicalExpression> args, ExpressionPosition pos) {
    switch (function) {
      case FunctionNames.OR:
        return BooleanPredicate.<C>createOrPredicate(name, args, pos);
      case FunctionNames.AND:
        return BooleanPredicate.<C>createAndPredicate(name, args, pos);
      default:
        logger.warn("Unknown Boolean '{}' predicate.", function);
        return null;
    }
  }
}
