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

import org.apache.drill.common.expression.AnyValueExpression;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.IfExpression.IfCondition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedFieldExpr;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.expression.ValueExpressions.BooleanExpression;
import org.apache.drill.common.expression.ValueExpressions.DateExpression;
import org.apache.drill.common.expression.ValueExpressions.Decimal18Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal28Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal38Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal9Expression;
import org.apache.drill.common.expression.ValueExpressions.DoubleExpression;
import org.apache.drill.common.expression.ValueExpressions.IntervalDayExpression;
import org.apache.drill.common.expression.ValueExpressions.IntervalYearExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.ValueExpressions.VarDecimalExpression;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;

public class ExpressionValidator implements ExprVisitor<Void, ErrorCollector, RuntimeException> {

  @Override
  public Void visitFunctionCall(FunctionCall call, ErrorCollector errors) throws RuntimeException {
    // we throw an exception here because this is a fundamental operator programming problem as opposed to an expression
    // problem. At this point in an expression's lifecycle, all function calls should have been converted into
    // FunctionHolders.
    throw new UnsupportedOperationException("FunctionCall is not expected here. "
        + "It should have been converted to FunctionHolderExpression in materialization");
  }

  @Override
  public Void visitFunctionHolderExpression(FunctionHolderExpression holder, ErrorCollector errors)
      throws RuntimeException {
    // make sure aggregate functions are not nested inside aggregate functions
    AggregateChecker.isAggregating(holder, errors);

    // make sure arguments are constant if the function implementation expects constants for any arguments
    ConstantChecker.checkConstants(holder, errors);

    return null;
  }

  @Override
  public Void visitBooleanOperator(BooleanOperator op, ErrorCollector errors) throws RuntimeException {
    int i = 0;
    for (LogicalExpression arg : op.args()) {
      if ( arg.getMajorType().getMinorType() != MinorType.BIT) {
        errors
            .addGeneralError(
                arg.getPosition(),
                String
                    .format(
                        "Failure composing boolean operator %s.  All conditions must return a boolean type.  Condition %d was of Type %s.",
                        op.getName(), i, arg.getMajorType().getMinorType()));
      }
      i++;
    }

    return null;
  }

  @Override
  public Void visitIfExpression(IfExpression ifExpr, ErrorCollector errors) throws RuntimeException {
    // confirm that all conditions are required boolean values.
    IfCondition cond = ifExpr.ifCondition;
    MajorType majorType = cond.condition.getMajorType();
    if ( majorType
        .getMinorType() != MinorType.BIT) {
      errors
          .addGeneralError(
              cond.condition.getPosition(),
              String
                  .format(
                      "Failure composing If Expression.  All conditions must return a boolean type.  Condition was of Type %s.",
                      majorType.getMinorType()));
    }

    // confirm that all outcomes are the same type.
    final MajorType mt = ifExpr.elseExpression.getMajorType();
    cond = ifExpr.ifCondition;
    MajorType innerT = cond.expression.getMajorType();
    if ((innerT.getMode() == DataMode.REPEATED && mt.getMode() != DataMode.REPEATED) || //
        ((innerT.getMinorType() != mt.getMinorType()) &&
        (innerT.getMode() != DataMode.OPTIONAL && mt.getMode() != DataMode.OPTIONAL &&
        (innerT.getMinorType() != MinorType.NULL && mt.getMinorType() != MinorType.NULL)))) {
      errors
          .addGeneralError(
              cond.condition.getPosition(),
              String
                  .format(
                      "Failure composing If Expression.  All expressions must return the same MinorType as the else expression.  The if condition returned type type %s but the else expression was of type %s",
                      innerT, mt));
    }
    return null;
  }

  @Override
  public Void visitSchemaPath(SchemaPath path, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitIntConstant(ValueExpressions.IntExpression intExpr, ErrorCollector value) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitFloatConstant(ValueExpressions.FloatExpression fExpr, ErrorCollector value) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitLongConstant(LongExpression intExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitDecimal9Constant(Decimal9Expression decExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitDecimal18Constant(Decimal18Expression decExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitDecimal28Constant(Decimal28Expression decExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitDecimal38Constant(Decimal38Expression decExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitVarDecimalConstant(VarDecimalExpression decExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitDateConstant(DateExpression intExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitTimeConstant(TimeExpression intExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitIntervalYearConstant(IntervalYearExpression intExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitIntervalDayConstant(IntervalDayExpression intExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitTimeStampConstant(TimeStampExpression intExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitDoubleConstant(DoubleExpression dExpr, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitBooleanConstant(BooleanExpression e, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitQuotedStringConstant(QuotedString e, ErrorCollector errors) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitUnknown(LogicalExpression e, ErrorCollector value) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitCastExpression(CastExpression e, ErrorCollector value) throws RuntimeException {
    return e.getInput().accept(this, value);
  }

  @Override
  public Void visitNullConstant(TypedNullConstant e, ErrorCollector value) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitNullExpression(NullExpression e, ErrorCollector value) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitConvertExpression(ConvertExpression e, ErrorCollector value)
      throws RuntimeException {
    return e.getInput().accept(this, value);
  }

  @Override
  public Void visitAnyValueExpression(AnyValueExpression e, ErrorCollector value)
      throws RuntimeException {
    return e.getInput().accept(this, value);
  }

  @Override
  public Void visitParameter(ValueExpressions.ParameterExpression e, ErrorCollector value) throws RuntimeException {
    return null;
  }

  @Override
  public Void visitTypedFieldExpr(TypedFieldExpr e, ErrorCollector value) throws RuntimeException {
    return null;
  }
}
