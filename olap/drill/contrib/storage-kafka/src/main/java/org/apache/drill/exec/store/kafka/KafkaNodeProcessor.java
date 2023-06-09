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
package org.apache.drill.exec.store.kafka;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions.BooleanExpression;
import org.apache.drill.common.expression.ValueExpressions.DateExpression;
import org.apache.drill.common.expression.ValueExpressions.DoubleExpression;
import org.apache.drill.common.expression.ValueExpressions.FloatExpression;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

class KafkaNodeProcessor extends AbstractExprVisitor<Boolean, LogicalExpression, RuntimeException> {

  private String functionName;
  private Boolean success;
  private Long value;
  private String path;

  public KafkaNodeProcessor(String functionName) {
    this.functionName = functionName;
    this.success = false;
  }

  public static boolean isPushdownFunction(String functionName) {
    return COMPARE_FUNCTIONS_TRANSPOSE_MAP.keySet().contains(functionName);
  }

  @Override
  public Boolean visitUnknown(LogicalExpression e, LogicalExpression valueArg) throws RuntimeException {
    return false;
  }

  public static KafkaNodeProcessor process(FunctionCall call) {
    String functionName = call.getName();
    LogicalExpression nameArg = call.arg(0);
    LogicalExpression valueArg = call.argCount() >= 2? call.arg(1) : null;
    KafkaNodeProcessor evaluator = new KafkaNodeProcessor(functionName);

    if (VALUE_EXPRESSION_CLASSES.contains(nameArg.getClass())) {
      LogicalExpression swapArg = valueArg;
      valueArg = nameArg;
      nameArg = swapArg;
      evaluator.functionName = COMPARE_FUNCTIONS_TRANSPOSE_MAP.get(functionName);
    }
    evaluator.success = nameArg.accept(evaluator, valueArg);
    return evaluator;
  }

  public boolean isSuccess() {
    // TODO Auto-generated method stub
    return success;
  }

  public String getPath() {
    return path;
  }

  public Long getValue() {
    return value;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Override
  public Boolean visitSchemaPath(SchemaPath path, LogicalExpression valueArg) throws RuntimeException {
    this.path = path.getRootSegmentPath();

    if(valueArg == null) {
      return false;
    }

    switch (this.path) {
      case "kafkaMsgOffset":
        /*
         * Do not pushdown FunctionNames.NE on kafkaMsgOffset.
         */
        if(functionName.equals(FunctionNames.NE)) {
          return false;
        }
      case "kafkaPartitionId":
        if(valueArg instanceof IntExpression) {
          value = (long) ((IntExpression) valueArg).getInt();
          return true;
        }

        if(valueArg instanceof LongExpression) {
          value = ((LongExpression) valueArg).getLong();
          return true;
        }
        break;
      case "kafkaMsgTimestamp":
        /*
        Only pushdown FunctionNames.EQ, FunctionNames.GT, "greater_than_or_equal" on kafkaMsgTimestamp
         */
        if(!functionName.equals(FunctionNames.EQ) && !functionName.equals(FunctionNames.GT)
               && !functionName.equals(FunctionNames.GE)) {
          return false;
        }

        if(valueArg instanceof LongExpression) {
          value = ((LongExpression) valueArg).getLong();
          return true;
        }

        if (valueArg instanceof DateExpression) {
          value = ((DateExpression)valueArg).getDate();
          return true;
        }

        if (valueArg instanceof TimeExpression) {
          value = (long) ((TimeExpression)valueArg).getTime();
          return true;
        }

        if (valueArg instanceof TimeStampExpression) {
          value = ((TimeStampExpression) valueArg).getTimeStamp();
          return true;
        }

        if(valueArg instanceof IntExpression) {
          value = (long) ((IntExpression) valueArg).getInt();
          return true;
        }
        break;
    }
    return false;
  }

  private static final ImmutableSet<Class<? extends LogicalExpression>> VALUE_EXPRESSION_CLASSES;
  static {
    ImmutableSet.Builder<Class<? extends LogicalExpression>> builder = ImmutableSet.builder();
    VALUE_EXPRESSION_CLASSES = builder
                                   .add(BooleanExpression.class)
                                   .add(DateExpression.class)
                                   .add(DoubleExpression.class)
                                   .add(FloatExpression.class)
                                   .add(IntExpression.class)
                                   .add(LongExpression.class)
                                   .add(QuotedString.class)
                                   .add(TimeExpression.class)
                                   .build();
  }

  private static final ImmutableMap<String, String> COMPARE_FUNCTIONS_TRANSPOSE_MAP;
  static {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    COMPARE_FUNCTIONS_TRANSPOSE_MAP = builder
                                          .put(FunctionNames.EQ, FunctionNames.EQ)
                                          .put(FunctionNames.NE, FunctionNames.NE)
                                          .put(FunctionNames.GE, FunctionNames.LE)
                                          .put(FunctionNames.GT, FunctionNames.LT)
                                          .put(FunctionNames.LE, FunctionNames.GE)
                                          .put(FunctionNames.LT, FunctionNames.GT)
                                          .build();
  }
}


