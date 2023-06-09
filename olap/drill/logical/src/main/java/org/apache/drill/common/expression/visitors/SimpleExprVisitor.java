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
package org.apache.drill.common.expression.visitors;

import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.SchemaPath;
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

public abstract class SimpleExprVisitor<T> implements ExprVisitor<T, Void, RuntimeException>{

  @Override
  public T visitFunctionCall(FunctionCall call, Void value) throws RuntimeException {
    return visitFunctionCall(call);
  }

  @Override
  public T visitFunctionHolderExpression(FunctionHolderExpression holder, Void value) throws RuntimeException {
    return visitFunctionHolderExpression(holder);
  }

  @Override
  public T visitIfExpression(IfExpression ifExpr, Void value) throws RuntimeException {
    return visitIfExpression(ifExpr);
  }

  @Override
  public T visitSchemaPath(SchemaPath path, Void value) throws RuntimeException {
    return visitSchemaPath(path);
  }

  @Override
  public T visitIntConstant(IntExpression intExpr, Void value) throws RuntimeException {
    return visitIntConstant(intExpr);
  }

  @Override
  public T visitFloatConstant(FloatExpression fExpr, Void value) throws RuntimeException {
    return visitFloatConstant(fExpr);
  }

  @Override
  public T visitLongConstant(LongExpression intExpr, Void value) throws RuntimeException {
    return visitLongConstant(intExpr);
  }

  @Override
  public T visitDateConstant(DateExpression intExpr, Void value) throws RuntimeException {
    return visitDateConstant(intExpr);
  }


  @Override
  public T visitTimeConstant(TimeExpression intExpr, Void value) throws RuntimeException {
    return visitTimeConstant(intExpr);
  }

  @Override
  public T visitIntervalYearConstant(IntervalYearExpression intExpr, Void value) throws RuntimeException {
    return visitIntervalYearConstant(intExpr);
  }

  @Override
  public T visitIntervalDayConstant(IntervalDayExpression intExpr, Void value) throws RuntimeException {
    return visitIntervalDayConstant(intExpr);
  }

  @Override
  public T visitTimeStampConstant(TimeStampExpression intExpr, Void value) throws RuntimeException {
    return visitTimeStampConstant(intExpr);
  }

  @Override
  public T visitDecimal9Constant(Decimal9Expression decExpr, Void value) throws RuntimeException {
    return visitDecimal9Constant(decExpr);
  }

  @Override
  public T visitDecimal18Constant(Decimal18Expression decExpr, Void value) throws RuntimeException {
    return visitDecimal18Constant(decExpr);
  }

  @Override
  public T visitDecimal28Constant(Decimal28Expression decExpr, Void value) throws RuntimeException {
    return visitDecimal28Constant(decExpr);
  }

  @Override
  public T visitDecimal38Constant(Decimal38Expression decExpr, Void value) throws RuntimeException {
    return visitDecimal38Constant(decExpr);
  }

  @Override
  public T visitVarDecimalConstant(VarDecimalExpression decExpr, Void value) throws RuntimeException {
    return visitVarDecimalConstant(decExpr);
  }

  @Override
  public T visitDoubleConstant(DoubleExpression dExpr, Void value) throws RuntimeException {
    return visitDoubleConstant(dExpr);
  }

  @Override
  public T visitBooleanConstant(BooleanExpression e, Void value) throws RuntimeException {
    return visitBooleanConstant(e);
  }

  @Override
  public T visitQuotedStringConstant(QuotedString e, Void value) throws RuntimeException {
    return visitQuotedStringConstant(e);
  }


  public abstract T visitFunctionCall(FunctionCall call);
  public abstract T visitFunctionHolderExpression(FunctionHolderExpression call);
  public abstract T visitIfExpression(IfExpression ifExpr);
  public abstract T visitSchemaPath(SchemaPath path);
  public abstract T visitIntConstant(IntExpression intExpr);
  public abstract T visitFloatConstant(FloatExpression fExpr);
  public abstract T visitLongConstant(LongExpression intExpr);
  public abstract T visitDateConstant(DateExpression intExpr);
  public abstract T visitTimeConstant(TimeExpression intExpr);
  public abstract T visitIntervalYearConstant(IntervalYearExpression intExpr);
  public abstract T visitIntervalDayConstant(IntervalDayExpression intExpr);
  public abstract T visitTimeStampConstant(TimeStampExpression intExpr);
  public abstract T visitDecimal9Constant(Decimal9Expression intExpr);
  public abstract T visitDecimal18Constant(Decimal18Expression intExpr);
  public abstract T visitDecimal28Constant(Decimal28Expression intExpr);
  public abstract T visitDecimal38Constant(Decimal38Expression intExpr);
  public abstract T visitVarDecimalConstant(VarDecimalExpression intExpr);
  public abstract T visitDoubleConstant(DoubleExpression dExpr);
  public abstract T visitBooleanConstant(BooleanExpression e);
  public abstract T visitQuotedStringConstant(QuotedString e);
}
