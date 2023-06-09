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
package org.apache.drill.exec.expr.fn;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.IfExpression.IfCondition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.HoldingContainerExpression;

public class FunctionGenerationHelper implements FunctionNames {

  /**
   * Finds ordering comparator ("compare_to...") FunctionHolderExpression with
   * a specified ordering for NULL (and considering NULLS <i>equal</i>).
   * @param  null_high  whether NULL should compare as the lowest value (if
   *                    {@code false}) or the highest value (if {@code true})
   * @param  left  ...
   * @param  right  ...
   * @param  functionLookupContext  ...
   * @return
   *     FunctionHolderExpression containing the found function implementation
   */
  public static LogicalExpression getOrderingComparator(
      boolean null_high,
      HoldingContainer left,
      HoldingContainer right,
      FunctionLookupContext functionLookupContext) {
    final String comparator_name =
        null_high ? COMPARE_TO_NULLS_HIGH : COMPARE_TO_NULLS_LOW;

    if (   ! isComparableType(left.getMajorType() )
        || ! isComparableType(right.getMajorType() )
            ){
      throw new UnsupportedOperationException(
          formatCanNotCompareMsg(left.getMajorType(), right.getMajorType()));
    }
    LogicalExpression comparisonFunctionExpression = getFunctionExpression(comparator_name, left, right);

    ErrorCollector collector = new ErrorCollectorImpl();
    if (!isUnionType(left.getMajorType()) && !isUnionType(right.getMajorType())) {
      return ExpressionTreeMaterializer.materialize(comparisonFunctionExpression, null, collector, functionLookupContext);
    } else {
      LogicalExpression typeComparisonFunctionExpression = getTypeComparisonFunction(comparisonFunctionExpression, left, right);
      return ExpressionTreeMaterializer.materialize(typeComparisonFunctionExpression, null, collector, functionLookupContext);
    }
  }

  private static boolean isUnionType(MajorType majorType) {
    return majorType.getMinorType() == MinorType.UNION;
  }

  /**
   * Finds ordering comparator ("compare_to...") FunctionHolderExpression with
   * a "NULL high" ordering (and considering NULLS <i>equal</i>).
   * @param  left  ...
   * @param  right  ...
   * @param  registry  ...
   * @return FunctionHolderExpression containing the function implementation
   */
  public static LogicalExpression getOrderingComparatorNullsHigh(
      HoldingContainer left,
      HoldingContainer right,
      FunctionLookupContext registry) {
    return getOrderingComparator(true, left, right, registry);
  }

  private static LogicalExpression getFunctionExpression(String name, HoldingContainer... args) {
    List<MajorType> argTypes = new ArrayList<MajorType>(args.length);
    List<LogicalExpression> argExpressions = new ArrayList<LogicalExpression>(args.length);
    for (HoldingContainer c : args) {
      argTypes.add(c.getMajorType());
      argExpressions.add(new HoldingContainerExpression(c));
    }

    return new FunctionCall(name, argExpressions, ExpressionPosition.UNKNOWN);
  }

  /**
   * Wraps the comparison function in an If-statement which compares the types first, evaluating the comaprison function only
   * if the types are equivialent
   *
   * @param comparisonFunction
   * @param args
   * @return
   */
  private static LogicalExpression getTypeComparisonFunction(LogicalExpression comparisonFunction, HoldingContainer... args) {
    List<LogicalExpression> argExpressions = Lists.newArrayList();
    List<MajorType> argTypes = Lists.newArrayList();
    for (HoldingContainer c : args) {
      argTypes.add(c.getMajorType());
      argExpressions.add(new HoldingContainerExpression(c));
    }
    FunctionCall call = new FunctionCall("compareType", argExpressions, ExpressionPosition.UNKNOWN);

    List<LogicalExpression> newArgs = Lists.newArrayList();
    newArgs.add(call);
    newArgs.add(new IntExpression(0, ExpressionPosition.UNKNOWN));
    FunctionCall notEqual = new FunctionCall(FunctionNames.NE, newArgs, ExpressionPosition.UNKNOWN);

    IfExpression.IfCondition ifCondition = new IfCondition(notEqual, call);
    IfExpression ifExpression = IfExpression.newBuilder().setIfCondition(ifCondition).setElse(comparisonFunction).build();
    return ifExpression;
  }

  private static final void appendType(MajorType mt, StringBuilder sb) {
    sb.append(mt.getMinorType().name());
    sb.append(":");
    sb.append(mt.getMode().name());
  }

  private static boolean isComparableType(MajorType type) {
    if (type.getMinorType() == MinorType.MAP ||
        type.getMinorType() == MinorType.LIST ||
        type.getMode() == TypeProtos.DataMode.REPEATED ) {
      return false;
    } else {
      return true;
    }
  }

  private static String formatCanNotCompareMsg(MajorType left, MajorType right) {
    StringBuilder sb = new StringBuilder();
    sb.append("Map, Array, Union or repeated scalar type should not be used in group by, order by or in a comparison operator. Drill does not support compare between ");

    appendType(left, sb);
    sb.append(" and ");
    appendType(right, sb);
    sb.append(".");

    return sb.toString();
  }
}
