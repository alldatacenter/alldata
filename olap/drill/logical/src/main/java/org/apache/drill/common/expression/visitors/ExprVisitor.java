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
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.NullExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedFieldExpr;
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
import org.apache.drill.common.expression.ValueExpressions.ParameterExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.ValueExpressions.VarDecimalExpression;

public interface ExprVisitor<T, VAL, EXCEP extends Exception> {
  T visitFunctionCall(FunctionCall call, VAL value) throws EXCEP;
  T visitFunctionHolderExpression(FunctionHolderExpression holder, VAL value) throws EXCEP;
  T visitIfExpression(IfExpression ifExpr, VAL value) throws EXCEP;
  T visitBooleanOperator(BooleanOperator call, VAL value) throws EXCEP;
  T visitSchemaPath(SchemaPath path, VAL value) throws EXCEP;
  T visitIntConstant(IntExpression intExpr, VAL value) throws EXCEP;
  T visitFloatConstant(FloatExpression fExpr, VAL value) throws EXCEP;
  T visitLongConstant(LongExpression intExpr, VAL value) throws EXCEP;
  T visitDateConstant(DateExpression intExpr, VAL value) throws EXCEP;
  T visitTimeConstant(TimeExpression intExpr, VAL value) throws EXCEP;
  T visitTimeStampConstant(TimeStampExpression intExpr, VAL value) throws EXCEP;
  T visitIntervalYearConstant(IntervalYearExpression intExpr, VAL value) throws EXCEP;
  T visitIntervalDayConstant(IntervalDayExpression intExpr, VAL value) throws EXCEP;
  T visitDecimal9Constant(Decimal9Expression decExpr, VAL value) throws EXCEP;
  T visitDecimal18Constant(Decimal18Expression decExpr, VAL value) throws EXCEP;
  T visitDecimal28Constant(Decimal28Expression decExpr, VAL value) throws EXCEP;
  T visitDecimal38Constant(Decimal38Expression decExpr, VAL value) throws EXCEP;
  T visitVarDecimalConstant(VarDecimalExpression decExpr, VAL value) throws EXCEP;
  T visitDoubleConstant(DoubleExpression dExpr, VAL value) throws EXCEP;
  T visitBooleanConstant(BooleanExpression e, VAL value) throws EXCEP;
  T visitQuotedStringConstant(QuotedString e, VAL value) throws EXCEP;
  T visitNullConstant(TypedNullConstant e, VAL value) throws EXCEP;
  T visitNullExpression(NullExpression e, VAL value) throws EXCEP;
  T visitUnknown(LogicalExpression e, VAL value) throws EXCEP;
  T visitCastExpression(CastExpression e, VAL value) throws EXCEP;
  T visitConvertExpression(ConvertExpression e, VAL value) throws EXCEP;
  T visitParameter(ParameterExpression e, VAL value) throws EXCEP;
  T visitTypedFieldExpr(TypedFieldExpr e, VAL value) throws EXCEP;
  T visitAnyValueExpression(AnyValueExpression e, VAL value) throws EXCEP;
}
