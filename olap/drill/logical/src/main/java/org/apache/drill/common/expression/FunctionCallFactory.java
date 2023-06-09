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
package org.apache.drill.common.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.types.TypeProtos.MajorType;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class FunctionCallFactory {

  private static final Map<String, String> OP_TO_FUNC_NAME = ImmutableMap.<String, String>builder()
      .put("+",   FunctionNames.ADD)
      .put("-",   FunctionNames.SUBTRACT)
      .put("/",   FunctionNames.DIVIDE)
      .put("*",   FunctionNames.MULTIPLY)
      .put("%",   FunctionNames.MODULO)
      .put("^",   FunctionNames.XOR)
      .put("||",  FunctionNames.CONCAT)
      .put("or",  FunctionNames.OR)
      .put("and", FunctionNames.AND)
      .put(">",   FunctionNames.GT)
      .put("<",   FunctionNames.LT)
      .put("==",  FunctionNames.EQ)
      .put("=",   FunctionNames.EQ)
      .put("!=",  FunctionNames.NE)
      .put("<>",  FunctionNames.NE)
      .put(">=",  FunctionNames.GE)
      .put("<=",  FunctionNames.LE)
      .put("is null", FunctionNames.IS_NULL)
      .put("is not null", FunctionNames.IS_NOT_NULL)
      .put("is true", FunctionNames.IS_TRUE)
      .put("is not true", FunctionNames.IS_NOT_TRUE)
      .put("is false", FunctionNames.IS_FALSE)
      .put("is not false", FunctionNames.IS_NOT_FALSE)
      .put("similar to", FunctionNames.SIMILAR_TO)
      .put("!",   FunctionNames.NOT)
      .put("u-",  FunctionNames.NEGATE)
      .build();

  public static String convertToDrillFunctionName(String op) {
    return OP_TO_FUNC_NAME.getOrDefault(op, op);
  }

  public static boolean isBooleanOperator(String funcName) {
    String drillFuncName  = convertToDrillFunctionName(funcName);
    return drillFuncName.equals(FunctionNames.AND) || drillFuncName.equals(FunctionNames.OR);
  }

  /*
   * create a cast function.
   * arguments : type -- targetType
   *             ep   -- input expression position
   *             expr -- input expression
   */
  public static LogicalExpression createCast(MajorType type, ExpressionPosition ep, LogicalExpression expr){
    return new CastExpression(expr, type, ep);
  }

  public static LogicalExpression createConvert(String function, String conversionType, LogicalExpression expr, ExpressionPosition ep) {
    return new ConvertExpression(function, conversionType, expr, ep);
  }

  public static LogicalExpression createAnyValue(ExpressionPosition ep, LogicalExpression expr) {
    return new AnyValueExpression(expr, ep);
  }

  public static LogicalExpression createAnyValue(String functionName, List<LogicalExpression> args) {
    return createExpression(functionName, args);
  }

  public static LogicalExpression createExpression(String functionName, List<LogicalExpression> args){
    return createExpression(functionName, ExpressionPosition.UNKNOWN, args);
  }

  public static LogicalExpression createExpression(String functionName, ExpressionPosition ep, List<LogicalExpression> args){
    String name = convertToDrillFunctionName(functionName);
    if (isBooleanOperator(name)) {
      return new BooleanOperator(name, args, ep);
    } else {
      return new FunctionCall(name, args, ep);
    }
  }

  public static LogicalExpression createExpression(String functionName, ExpressionPosition ep, LogicalExpression... e){
    return createExpression(functionName, ep, Lists.newArrayList(e));
  }

  public static LogicalExpression createBooleanOperator(String functionName, List<LogicalExpression> args){
    return createBooleanOperator(functionName, ExpressionPosition.UNKNOWN, args);
  }

  public static LogicalExpression createBooleanOperator(String functionName, ExpressionPosition ep, List<LogicalExpression> args){
    return new BooleanOperator(convertToDrillFunctionName(functionName), args, ep);
  }

  public static LogicalExpression createByOp(List<LogicalExpression> args, ExpressionPosition ep, List<String> opTypes) {
    if (args.size() == 1) {
      return args.get(0);
    }

    if (args.size() - 1 != opTypes.size()) {
      throw new DrillRuntimeException("Must receive one more expression then the provided number of operators.");
    }

    LogicalExpression first = args.get(0);
    for (int i = 0; i < opTypes.size(); i++) {
      List<LogicalExpression> l2 = new ArrayList<LogicalExpression>();
      l2.add(first);
      l2.add(args.get(i + 1));
      first = createExpression(opTypes.get(i), ep, l2);
    }
    return first;
  }
}
