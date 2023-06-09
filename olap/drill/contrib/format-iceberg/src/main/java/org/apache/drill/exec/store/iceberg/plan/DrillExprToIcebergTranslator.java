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
package org.apache.drill.exec.store.iceberg.plan;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.exec.store.iceberg.IcebergGroupScan;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;

public class DrillExprToIcebergTranslator extends AbstractExprVisitor<Expression, Void, RuntimeException> {

  public static final ExprVisitor<Expression, Void, RuntimeException> INSTANCE = new DrillExprToIcebergTranslator();

  @Override
  public Expression visitFunctionCall(FunctionCall call, Void value) throws RuntimeException {
    switch (call.getName()) {
      case FunctionNames.AND: {
        Expression left = call.arg(0).accept(this, null);
        Expression right = call.arg(1).accept(this, null);
        if (left != null && right != null) {
          return Expressions.and(left, right);
        }
        return null;
      }
      case FunctionNames.OR: {
        Expression left = call.arg(0).accept(this, null);
        Expression right = call.arg(1).accept(this, null);
        if (left != null && right != null) {
          return Expressions.or(left, right);
        }
        return null;
      }
      case FunctionNames.NOT: {
        Expression expression = call.arg(0).accept(this, null);
        if (expression != null) {
          return Expressions.not(expression);
        }
        return null;
      }
      case FunctionNames.IS_NULL: {
        LogicalExpression arg = call.arg(0);
        if (arg instanceof SchemaPath) {
          String name = IcebergGroupScan.getPath((SchemaPath) arg);
          return Expressions.isNull(name);
        }
        return null;
      }
      case FunctionNames.IS_NOT_NULL: {
        LogicalExpression arg = call.arg(0);
        if (arg instanceof SchemaPath) {
          String name = IcebergGroupScan.getPath((SchemaPath) arg);
          return Expressions.notNull(name);
        }
        return null;
      }
      case FunctionNames.LT: {
        LogicalExpression nameRef = call.arg(0);
        Expression expression = call.arg(1).accept(this, null);
        if (nameRef instanceof SchemaPath && expression instanceof ConstantExpression) {
          String name = IcebergGroupScan.getPath((SchemaPath) nameRef);
          return Expressions.lessThan(name, ((ConstantExpression<?>) expression).getValue());
        }
        return null;
      }
      case FunctionNames.LE: {
        LogicalExpression nameRef = call.arg(0);
        Expression expression = call.arg(1).accept(this, null);
        if (nameRef instanceof SchemaPath && expression instanceof ConstantExpression) {
          String name = IcebergGroupScan.getPath((SchemaPath) nameRef);
          return Expressions.lessThanOrEqual(name, ((ConstantExpression<?>) expression).getValue());
        }
        return null;
      }
      case FunctionNames.GT: {
        LogicalExpression nameRef = call.args().get(0);
        Expression expression = call.args().get(1).accept(this, null);
        if (nameRef instanceof SchemaPath && expression instanceof ConstantExpression) {
          String name = IcebergGroupScan.getPath((SchemaPath) nameRef);
          return Expressions.greaterThan(name, ((ConstantExpression<?>) expression).getValue());
        }
        return null;
      }
      case FunctionNames.GE: {
        LogicalExpression nameRef = call.args().get(0);
        Expression expression = call.args().get(0).accept(this, null);
        if (nameRef instanceof SchemaPath && expression instanceof ConstantExpression) {
          String name = IcebergGroupScan.getPath((SchemaPath) nameRef);
          return Expressions.greaterThanOrEqual(name, ((ConstantExpression<?>) expression).getValue());
        }
        return null;
      }
      case FunctionNames.EQ: {
        LogicalExpression nameRef = call.args().get(0);
        Expression expression = call.args().get(1).accept(this, null);
        if (nameRef instanceof SchemaPath && expression instanceof ConstantExpression) {
          String name = IcebergGroupScan.getPath((SchemaPath) nameRef);
          return Expressions.equal(name, ((ConstantExpression<?>) expression).getValue());
        }
        return null;
      }
      case FunctionNames.NE: {
        LogicalExpression nameRef = call.args().get(0);
        Expression expression = call.args().get(1).accept(this, null);
        if (nameRef instanceof SchemaPath && expression instanceof ConstantExpression) {
          String name = IcebergGroupScan.getPath((SchemaPath) nameRef);
          return Expressions.notEqual(name, ((ConstantExpression<?>) expression).getValue());
        }
        return null;
      }
    }
    return null;
  }

  @Override
  public Expression visitFloatConstant(ValueExpressions.FloatExpression fExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(fExpr.getFloat());
  }

  @Override
  public Expression visitIntConstant(ValueExpressions.IntExpression intExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(intExpr.getInt());
  }

  @Override
  public Expression visitLongConstant(ValueExpressions.LongExpression longExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(longExpr.getLong());
  }

  @Override
  public Expression visitDecimal9Constant(ValueExpressions.Decimal9Expression decExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(decExpr.getIntFromDecimal());
  }

  @Override
  public Expression visitDecimal18Constant(ValueExpressions.Decimal18Expression decExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(decExpr.getLongFromDecimal());
  }

  @Override
  public Expression visitDecimal28Constant(ValueExpressions.Decimal28Expression decExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(decExpr.getBigDecimal());
  }

  @Override
  public Expression visitDecimal38Constant(ValueExpressions.Decimal38Expression decExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(decExpr.getBigDecimal());
  }

  @Override
  public Expression visitVarDecimalConstant(ValueExpressions.VarDecimalExpression decExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(decExpr.getBigDecimal());
  }

  @Override
  public Expression visitDateConstant(ValueExpressions.DateExpression dateExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(dateExpr.getDate());
  }

  @Override
  public Expression visitTimeConstant(ValueExpressions.TimeExpression timeExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(timeExpr.getTime());
  }

  @Override
  public Expression visitTimeStampConstant(ValueExpressions.TimeStampExpression timestampExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(timestampExpr.getTimeStamp());
  }

  @Override
  public Expression visitDoubleConstant(ValueExpressions.DoubleExpression dExpr, Void value) throws RuntimeException {
    return new ConstantExpression<>(dExpr.getDouble());
  }

  @Override
  public Expression visitBooleanConstant(ValueExpressions.BooleanExpression e, Void value) throws RuntimeException {
    return new ConstantExpression<>(e.getBoolean());
  }

  @Override
  public Expression visitQuotedStringConstant(ValueExpressions.QuotedString e, Void value) throws RuntimeException {
    return new ConstantExpression<>(e.getString());
  }

  @Override
  public Expression visitUnknown(LogicalExpression e, Void value) throws RuntimeException {
    return null;
  }

  private static class ConstantExpression<T> implements Expression {
    private final T value;

    public ConstantExpression(T value) {
      this.value = value;
    }

    @Override
    public Operation op() {
      return null;
    }

    public T getValue() {
      return value;
    }
  }
}
