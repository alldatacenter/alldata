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

import java.util.List;

class EqualityVisitor extends AbstractExprVisitor<Boolean,LogicalExpression,RuntimeException> {

  @Override
  public Boolean visitFunctionCall(FunctionCall call, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof FunctionCall)) {
      return false;
    }
    if (!checkType(call, value)) {
      return false;
    }
    if (!call.getName().equals(((FunctionCall) value).getName())) {
      return false;
    }
    return checkChildren(call, value);
  }

  @Override
  public Boolean visitFunctionHolderExpression(FunctionHolderExpression holder, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof FunctionHolderExpression)) {
      return false;
    }
    if (!checkType(holder, value)) {
      return false;
    }
    if (!holder.getName().equals(((FunctionHolderExpression) value).getName())) {
      return false;
    }
    if (holder.isRandom()) {
      return false;
    }
    return checkChildren(holder, value);
  }

  @Override
  public Boolean visitIfExpression(IfExpression ifExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof IfExpression)) {
      return false;
    }
    return checkChildren(ifExpr, value);
  }

  @Override
  public Boolean visitBooleanOperator(BooleanOperator call, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof BooleanOperator)) {
      return false;
    }
    if (!call.getName().equals(((BooleanOperator) value).getName())) {
      return false;
    }
    return checkChildren(call, value);
  }

  @Override
  public Boolean visitSchemaPath(SchemaPath path, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof SchemaPath)) {
      return false;
    }
    return path.equals(value);
  }

  @Override
  public Boolean visitIntConstant(IntExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof IntExpression)) {
      return false;
    }
    return intExpr.getInt() == ((IntExpression) value).getInt();
  }

  @Override
  public Boolean visitFloatConstant(FloatExpression fExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof FloatExpression)) {
      return false;
    }
    return fExpr.getFloat() == ((FloatExpression) value).getFloat();
  }

  @Override
  public Boolean visitLongConstant(LongExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof LongExpression)) {
      return false;
    }
    return intExpr.getLong() == ((LongExpression) value).getLong();
  }

  @Override
  public Boolean visitDateConstant(DateExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof DateExpression)) {
      return false;
    }
    return intExpr.getDate() == ((DateExpression) value).getDate();
  }

  @Override
  public Boolean visitTimeConstant(TimeExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof TimeExpression)) {
      return false;
    }
    return intExpr.getTime() == ((TimeExpression) value).getTime();
  }

  @Override
  public Boolean visitTimeStampConstant(TimeStampExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof TimeStampExpression)) {
      return false;
    }
    return intExpr.getTimeStamp() == ((TimeStampExpression) value).getTimeStamp();
  }

  @Override
  public Boolean visitIntervalYearConstant(IntervalYearExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof IntervalYearExpression)) {
      return false;
    }
    return intExpr.getIntervalYear() == ((IntervalYearExpression) value).getIntervalYear();
  }

  @Override
  public Boolean visitIntervalDayConstant(IntervalDayExpression intExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof IntervalDayExpression)) {
      return false;
    }
    return intExpr.getIntervalDay() == ((IntervalDayExpression) value).getIntervalDay()
            && intExpr.getIntervalMillis() == ((IntervalDayExpression) value).getIntervalMillis();
  }

  @Override
  public Boolean visitDecimal9Constant(Decimal9Expression decExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof Decimal9Expression)) {
      return false;
    }
    if (decExpr.getIntFromDecimal() != ((Decimal9Expression) value).getIntFromDecimal()) {
      return false;
    }
    if (decExpr.getScale() != ((Decimal9Expression) value).getScale()) {
      return false;
    }
    if (decExpr.getPrecision() != ((Decimal9Expression) value).getPrecision()) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitDecimal18Constant(Decimal18Expression decExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof Decimal18Expression)) {
      return false;
    }
    if (decExpr.getLongFromDecimal() != ((Decimal18Expression) value).getLongFromDecimal()) {
      return false;
    }
    if (decExpr.getScale() != ((Decimal18Expression) value).getScale()) {
      return false;
    }
    if (decExpr.getPrecision() != ((Decimal18Expression) value).getPrecision()) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitDecimal28Constant(Decimal28Expression decExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof Decimal28Expression)) {
      return false;
    }
    if (decExpr.getBigDecimal() != ((Decimal28Expression) value).getBigDecimal()) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitDecimal38Constant(Decimal38Expression decExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof Decimal38Expression)) {
      return false;
    }
    if (decExpr.getBigDecimal() != ((Decimal38Expression) value).getBigDecimal()) {
      return false;
    }
    if (!decExpr.getMajorType().equals(((Decimal38Expression) value).getMajorType())) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitVarDecimalConstant(VarDecimalExpression decExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof VarDecimalExpression)) {
      return false;
    }
    if (!decExpr.getMajorType().equals(value.getMajorType())) {
      return false;
    }
    if (!decExpr.getBigDecimal().equals(((VarDecimalExpression) value).getBigDecimal())) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitDoubleConstant(DoubleExpression dExpr, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof DoubleExpression)) {
      return false;
    }
    return dExpr.getDouble() == ((DoubleExpression) value).getDouble();
  }

  @Override
  public Boolean visitBooleanConstant(BooleanExpression e, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof BooleanExpression)) {
      return false;
    }
    return e.getBoolean() == ((BooleanExpression) value).getBoolean();
  }

  @Override
  public Boolean visitQuotedStringConstant(QuotedString e, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof QuotedString)) {
      return false;
    }
    return e.getString().equals(((QuotedString) value).getString());
  }

  @Override
  public Boolean visitNullExpression(NullExpression e, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof NullExpression)) {
      return false;
    }
    return e.getMajorType().equals(value.getMajorType());
  }

  @Override
  public Boolean visitCastExpression(CastExpression e, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof CastExpression)) {
      return false;
    }
    if (!e.getMajorType().equals(value.getMajorType())) {
      return false;
    }
    return checkChildren(e, value);
  }

  @Override
  public Boolean visitConvertExpression(ConvertExpression e, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof ConvertExpression)) {
      return false;
    }
    if (!e.getConvertFunction().equals(((ConvertExpression) value).getConvertFunction())) {
      return false;
    }
    return checkChildren(e, value);
  }

  @Override
  public Boolean visitNullConstant(TypedNullConstant e, LogicalExpression value) throws RuntimeException {
    if (!(value instanceof TypedNullConstant)) {
      return false;
    }
    return value.getMajorType().equals(e.getMajorType());
  }

  @Override
  public Boolean visitUnknown(LogicalExpression e, LogicalExpression value) throws RuntimeException {
    if (e instanceof ValueVectorReadExpression && value instanceof ValueVectorReadExpression) {
      return visitValueVectorReadExpression((ValueVectorReadExpression) e, (ValueVectorReadExpression) value);
    }
    return false;
  }

  private Boolean visitValueVectorReadExpression(ValueVectorReadExpression e, ValueVectorReadExpression value) {
    return e.getTypedFieldId().equals(value.getTypedFieldId());
  }


  private boolean checkChildren(LogicalExpression thisExpr, LogicalExpression thatExpr) {
    List<LogicalExpression> theseChildren = Lists.newArrayList(thisExpr);
    List<LogicalExpression> thoseChildren = Lists.newArrayList(thatExpr);

    if (theseChildren.size() != thoseChildren.size()) {
      return false;
    }
    for (int i = 0; i < theseChildren.size(); i++) {
      if (!theseChildren.get(i).accept(this, thoseChildren.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean checkType(LogicalExpression e1, LogicalExpression e2) {
    return e1.getMajorType().equals(e2.getMajorType());
  }
}
