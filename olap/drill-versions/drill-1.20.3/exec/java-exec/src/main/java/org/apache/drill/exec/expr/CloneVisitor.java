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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.IfExpression.IfCondition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions.BooleanExpression;
import org.apache.drill.common.expression.ValueExpressions.DateExpression;
import org.apache.drill.common.expression.ValueExpressions.Decimal18Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal28Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal38Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal9Expression;
import org.apache.drill.common.expression.ValueExpressions.DoubleExpression;
import org.apache.drill.common.expression.ValueExpressions.FloatExpression;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.expression.ValueExpressions.IntervalDayExpression;
import org.apache.drill.common.expression.ValueExpressions.IntervalYearExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.ValueExpressions.VarDecimalExpression;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

import java.util.List;

/**
 * Creates a deep copy of a LogicalExpression. Specifically, it creates new instances of the literal expressions
 */
public class CloneVisitor extends AbstractExprVisitor<LogicalExpression,Void,RuntimeException> {
  @Override
  public LogicalExpression visitFunctionCall(FunctionCall call, Void value) throws RuntimeException {
    List<LogicalExpression> args = Lists.newArrayList();
    for (LogicalExpression arg : call.args()) {
      args.add(arg.accept(this, null));
    }

    return new FunctionCall(call.getName(), args, call.getPosition());
  }

  @Override
  public LogicalExpression visitFunctionHolderExpression(FunctionHolderExpression holder, Void value) throws RuntimeException {
    if (holder instanceof DrillFuncHolderExpr) {
      List<LogicalExpression> args = Lists.newArrayList();
      for (LogicalExpression arg : holder.args) {
        args.add(arg.accept(this, null));
      }
      return new DrillFuncHolderExpr(holder.getName(), (DrillFuncHolder) holder.getHolder(), args, holder.getPosition());
    }
    return null;
  }

  @Override
  public LogicalExpression visitIfExpression(IfExpression ifExpr, Void value) throws RuntimeException {
    LogicalExpression ifCondition = ifExpr.ifCondition.condition.accept(this, null);
    LogicalExpression ifExpression = ifExpr.ifCondition.expression.accept(this, null);
    LogicalExpression elseExpression = ifExpr.elseExpression.accept(this, null);
    IfExpression.IfCondition condition = new IfCondition(ifCondition, ifExpression);
    return IfExpression.newBuilder().setIfCondition(condition).setElse(elseExpression).build();
  }

  @Override
  public LogicalExpression visitBooleanOperator(BooleanOperator op, Void value) throws RuntimeException {
    return visitUnknown(op, value);
  }

  @Override
  public LogicalExpression visitSchemaPath(SchemaPath path, Void value) throws RuntimeException {
    return path;
  }

  @Override
  public LogicalExpression visitFloatConstant(FloatExpression fExpr, Void value) throws RuntimeException {
    return visitUnknown(fExpr, value);
  }

  @Override
  public LogicalExpression visitIntConstant(IntExpression intExpr, Void value) throws RuntimeException {
    return new IntExpression(intExpr.getInt(), intExpr.getPosition());
  }

  @Override
  public LogicalExpression visitLongConstant(LongExpression intExpr, Void value) throws RuntimeException {
    return visitUnknown(intExpr, value);
  }


  @Override
  public LogicalExpression visitDecimal9Constant(Decimal9Expression decExpr, Void value) throws RuntimeException {
    return visitUnknown(decExpr, value);
  }

  @Override
  public LogicalExpression visitDecimal18Constant(Decimal18Expression decExpr, Void value) throws RuntimeException {
    return visitUnknown(decExpr, value);
  }

  @Override
  public LogicalExpression visitDecimal28Constant(Decimal28Expression decExpr, Void value) throws RuntimeException {
    return visitUnknown(decExpr, value);
  }

  @Override
  public LogicalExpression visitDecimal38Constant(Decimal38Expression decExpr, Void value) throws RuntimeException {
    return visitUnknown(decExpr, value);
  }

  @Override
  public LogicalExpression visitVarDecimalConstant(VarDecimalExpression decExpr, Void value) throws RuntimeException {
    return visitUnknown(decExpr, value);
  }

  @Override
  public LogicalExpression visitDateConstant(DateExpression intExpr, Void value) throws RuntimeException {
    return visitUnknown(intExpr, value);
  }

  @Override
  public LogicalExpression visitTimeConstant(TimeExpression intExpr, Void value) throws RuntimeException {
    return visitUnknown(intExpr, value);
  }

  @Override
  public LogicalExpression visitTimeStampConstant(TimeStampExpression intExpr, Void value) throws RuntimeException {
    return visitUnknown(intExpr, value);
  }

  @Override
  public LogicalExpression visitIntervalYearConstant(IntervalYearExpression intExpr, Void value) throws RuntimeException {
    return visitUnknown(intExpr, value);
  }

  @Override
  public LogicalExpression visitIntervalDayConstant(IntervalDayExpression intExpr, Void value) throws RuntimeException {
    return visitUnknown(intExpr, value);
  }

  @Override
  public LogicalExpression visitDoubleConstant(DoubleExpression dExpr, Void value) throws RuntimeException {
    return visitUnknown(dExpr, value);
  }

  @Override
  public LogicalExpression visitBooleanConstant(BooleanExpression e, Void value) throws RuntimeException {
    return visitUnknown(e, value);
  }

  @Override
  public LogicalExpression visitQuotedStringConstant(QuotedString e, Void value) throws RuntimeException {
    return visitUnknown(e, value);
  }

  @Override
  public LogicalExpression visitCastExpression(CastExpression e, Void value) throws RuntimeException {
    return visitUnknown(e, value);
  }

  @Override
  public LogicalExpression visitConvertExpression(ConvertExpression e, Void value) throws RuntimeException {
    return visitUnknown(e, value);
  }

  @Override
  public LogicalExpression visitNullConstant(TypedNullConstant e, Void value) throws RuntimeException {
    return visitUnknown(e, value);
  }

  @Override
  public LogicalExpression visitNullExpression(NullExpression e, Void value) throws RuntimeException {
    return visitUnknown(e, value);
  }

  @Override
  public LogicalExpression visitUnknown(LogicalExpression e, Void value) throws RuntimeException {
    return e;
  }
}
