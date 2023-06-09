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

import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
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

public class HashVisitor extends AbstractExprVisitor<Integer,Void,RuntimeException> {
  @Override
  public Integer visitFunctionCall(FunctionCall call, Void value) throws RuntimeException {
    return compute(call, 1);
  }

  @Override
  public Integer visitFunctionHolderExpression(FunctionHolderExpression holder, Void value) throws RuntimeException {
    return compute(holder, 2);
  }

  @Override
  public Integer visitIfExpression(IfExpression ifExpr, Void value) throws RuntimeException {
    return compute(ifExpr, 3);
  }

  @Override
  public Integer visitBooleanOperator(BooleanOperator op, Void value) throws RuntimeException {
    return compute(op, 4);
  }

  @Override
  public Integer visitSchemaPath(SchemaPath path, Void value) throws RuntimeException {
    return compute(path, 5);
  }

  @Override
  public Integer visitFloatConstant(FloatExpression fExpr, Void value) throws RuntimeException {
    return compute(fExpr, 6);
  }

  @Override
  public Integer visitIntConstant(IntExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 7);
  }

  @Override
  public Integer visitLongConstant(LongExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 8);
  }


  @Override
  public Integer visitDecimal9Constant(Decimal9Expression decExpr, Void value) throws RuntimeException {
    return compute(decExpr, 9);
  }

  @Override
  public Integer visitDecimal18Constant(Decimal18Expression decExpr, Void value) throws RuntimeException {
    return compute(decExpr, 10);
  }

  @Override
  public Integer visitDecimal28Constant(Decimal28Expression decExpr, Void value) throws RuntimeException {
    return compute(decExpr, 11);
  }

  @Override
  public Integer visitDecimal38Constant(Decimal38Expression decExpr, Void value) throws RuntimeException {
    return compute(decExpr, 12);
  }

  @Override
  public Integer visitDateConstant(DateExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 13);
  }

  @Override
  public Integer visitTimeConstant(TimeExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 14);
  }

  @Override
  public Integer visitTimeStampConstant(TimeStampExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 15);
  }

  @Override
  public Integer visitIntervalYearConstant(IntervalYearExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 16);
  }

  @Override
  public Integer visitIntervalDayConstant(IntervalDayExpression intExpr, Void value) throws RuntimeException {
    return compute(intExpr, 17);
  }

  @Override
  public Integer visitDoubleConstant(DoubleExpression dExpr, Void value) throws RuntimeException {
    return compute(dExpr, 18);
  }

  @Override
  public Integer visitBooleanConstant(BooleanExpression e, Void value) throws RuntimeException {
    return compute(e, 19);
  }

  @Override
  public Integer visitQuotedStringConstant(QuotedString e, Void value) throws RuntimeException {
    return compute(e, 20);
  }

  @Override
  public Integer visitCastExpression(CastExpression e, Void value) throws RuntimeException {
    return compute(e, 21);
  }

  @Override
  public Integer visitConvertExpression(ConvertExpression e, Void value) throws RuntimeException {
    return compute(e, 22);
  }

  @Override
  public Integer visitNullConstant(TypedNullConstant e, Void value) throws RuntimeException {
    return compute(e, 23);
  }

  @Override
  public Integer visitNullExpression(NullExpression e, Void value) throws RuntimeException {
    return compute(e, 24);
  }

  @Override
  public Integer visitUnknown(LogicalExpression e, Void value) throws RuntimeException {
    return compute(e, 25);
  }

  @Override
  public Integer visitVarDecimalConstant(VarDecimalExpression decExpr, Void value) throws RuntimeException {
    return compute(decExpr, 26);
  }

  private int compute(LogicalExpression e, int seed) {
    int hash = seed;
    for (LogicalExpression child : e) {
      hash = hash * 31 + child.accept(this, null);
    }
    return hash;
  }
}
