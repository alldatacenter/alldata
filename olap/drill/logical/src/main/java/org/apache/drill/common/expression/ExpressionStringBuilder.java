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

import java.math.BigDecimal;
import java.util.List;

import org.apache.drill.common.expression.IfExpression.IfCondition;
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
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.joda.time.Period;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public class ExpressionStringBuilder extends AbstractExprVisitor<Void, StringBuilder, RuntimeException>{

  static final ExpressionStringBuilder INSTANCE = new ExpressionStringBuilder();

  public static String toString(LogicalExpression expr) {
    StringBuilder sb = new StringBuilder();
    expr.accept(INSTANCE, sb);
    return sb.toString();
  }

  public static void toString(LogicalExpression expr, StringBuilder sb) {
    expr.accept(INSTANCE, sb);
  }

  public static String escapeSingleQuote(String input) {
    return input.replaceAll("(['\\\\])", "\\\\$1");
  }

  public static String escapeBackTick(String input) {
    return input.replaceAll("([`\\\\])", "\\\\$1");
  }

  @Override
  public Void visitFunctionCall(FunctionCall call, StringBuilder sb) throws RuntimeException {
    List<LogicalExpression> args = call.args();
    sb.append(call.getName());
    sb.append("(");
    for (int i = 0; i < args.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      args.get(i).accept(this, sb);
    }
    sb.append(") ");
    return null;
  }

  @Override
  public Void visitBooleanOperator(BooleanOperator op, StringBuilder sb) throws RuntimeException {
    return visitFunctionCall(op, sb);
  }

  @Override
  public Void visitFunctionHolderExpression(FunctionHolderExpression holder, StringBuilder sb) throws RuntimeException {
    ImmutableList<LogicalExpression> args = holder.args;
    sb.append(holder.getName());
    sb.append("(");
    for (int i = 0; i < args.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      args.get(i).accept(this, sb);
    }
    sb.append(") ");
    return null;
  }

  @Override
  public Void visitIfExpression(IfExpression ifExpr, StringBuilder sb) throws RuntimeException {

    // serialize the if expression
    sb.append(" ( ");
    IfCondition c = ifExpr.ifCondition;
    sb.append("if (");
    c.condition.accept(this, sb);
    sb.append(" ) then (");
    c.expression.accept(this, sb);
    sb.append(" ) ");

    sb.append(" else (");
    ifExpr.elseExpression.accept(this, sb);
    sb.append(" ) ");
    sb.append(" end ");
    sb.append(" ) ");
    return null;
  }

  @Override
  public Void visitSchemaPath(SchemaPath path, StringBuilder sb) throws RuntimeException {
    PathSegment seg = path.getRootSegment();
    if (seg.isArray()) {
      throw new IllegalStateException("Drill doesn't currently support top level arrays");
    }
    sb.append('`');
    sb.append(escapeBackTick(seg.getNameSegment().getPath()));
    sb.append('`');

    while ( (seg = seg.getChild()) != null) {
      if (seg.isNamed()) {
        sb.append('.');
        sb.append('`');
        sb.append(escapeBackTick(seg.getNameSegment().getPath()));
        sb.append('`');
      } else {
        sb.append('[');
        sb.append(seg.getArraySegment().getIndex());
        sb.append(']');
      }
    }
    return null;
  }

  @Override
  public Void visitLongConstant(LongExpression lExpr, StringBuilder sb) throws RuntimeException {
    sb.append(lExpr.getLong());
    return null;
  }

  @Override
  public Void visitDateConstant(DateExpression lExpr, StringBuilder sb) throws RuntimeException {
    sb.append("cast( ");
    sb.append(lExpr.getDate());
    sb.append(" as DATE)");
    return null;
  }

  @Override
  public Void visitTimeConstant(TimeExpression lExpr, StringBuilder sb) throws RuntimeException {
    sb.append("cast( ");
    sb.append(lExpr.getTime());
    sb.append(" as TIME)");
    return null;
  }

  @Override
  public Void visitTimeStampConstant(TimeStampExpression lExpr, StringBuilder sb) throws RuntimeException {
    sb.append("cast( ");
    sb.append(lExpr.getTimeStamp());
    sb.append(" as TIMESTAMP)");
    return null;
  }

  @Override
  public Void visitIntervalYearConstant(IntervalYearExpression lExpr, StringBuilder sb) throws RuntimeException {
    sb.append("cast( '");
    sb.append(Period.months(lExpr.getIntervalYear()).toString());
    sb.append("' as INTERVALYEAR)");
    return null;
  }

  @Override
  public Void visitIntervalDayConstant(IntervalDayExpression lExpr, StringBuilder sb) throws RuntimeException {
    sb.append("cast( '");
    sb.append(Period.days(lExpr.getIntervalDay()).plusMillis(lExpr.getIntervalMillis()).toString());
    sb.append("' as INTERVALDAY)");
    return null;
  }

  @Override
  public Void visitDecimal9Constant(Decimal9Expression decExpr, StringBuilder sb) throws RuntimeException {
    BigDecimal value = new BigDecimal(decExpr.getIntFromDecimal());
    sb.append((value.setScale(decExpr.getScale())).toString());
    return null;
  }

  @Override
  public Void visitDecimal18Constant(Decimal18Expression decExpr, StringBuilder sb) throws RuntimeException {
    BigDecimal value = new BigDecimal(decExpr.getLongFromDecimal());
    sb.append((value.setScale(decExpr.getScale())).toString());
    return null;
  }

  @Override
  public Void visitDecimal28Constant(Decimal28Expression decExpr, StringBuilder sb) throws RuntimeException {
    sb.append(decExpr.toString());
    return null;
  }

  @Override
  public Void visitDecimal38Constant(Decimal38Expression decExpr, StringBuilder sb) throws RuntimeException {
    sb.append(decExpr.getBigDecimal().toString());
    return null;
  }

  @Override
  public Void visitVarDecimalConstant(VarDecimalExpression decExpr, StringBuilder sb) throws RuntimeException {
    sb.append(decExpr.getBigDecimal().toString());
    return null;
  }

  @Override
  public Void visitDoubleConstant(DoubleExpression dExpr, StringBuilder sb) throws RuntimeException {
    sb.append(dExpr.getDouble());
    return null;
  }

  @Override
  public Void visitBooleanConstant(BooleanExpression e, StringBuilder sb) throws RuntimeException {
    sb.append(e.getBoolean());
    return null;
  }

  @Override
  public Void visitQuotedStringConstant(QuotedString e, StringBuilder sb) throws RuntimeException {
    sb.append("'");
    sb.append(escapeSingleQuote(e.value));
    sb.append("'");
    return null;
  }

  @Override
  public Void visitConvertExpression(ConvertExpression e, StringBuilder sb) throws RuntimeException {
    sb.append(e.getConvertFunction()).append("(");
    e.getInput().accept(this, sb);
    sb.append(", '").append(e.getEncodingType()).append("')");
    return null;
  }

  @Override
  public Void visitAnyValueExpression(AnyValueExpression e, StringBuilder sb) throws RuntimeException {
    sb.append("any(");
    e.getInput().accept(this, sb);
    sb.append(")");
    return null;
  }

  @Override
  public Void visitCastExpression(CastExpression e, StringBuilder sb) throws RuntimeException {
    MajorType mt = e.getMajorType();

    sb.append("cast( (");
    e.getInput().accept(this, sb);
    sb.append(" ) as ");
    sb.append(mt.getMinorType().name());

    switch(mt.getMinorType()) {
    case FLOAT4:
    case FLOAT8:
    case BIT:
    case INT:
    case TINYINT:
    case SMALLINT:
    case BIGINT:
    case UINT1:
    case UINT2:
    case UINT4:
    case UINT8:
    case DATE:
    case TIMESTAMP:
    case TIMESTAMPTZ:
    case TIME:
    case INTERVAL:
    case INTERVALDAY:
    case INTERVALYEAR:
      // do nothing else.
      break;
    case VAR16CHAR:
    case VARBINARY:
    case VARCHAR:
    case FIXED16CHAR:
    case FIXEDBINARY:
    case FIXEDCHAR:

      // add size in parens
      sb.append("(");
      sb.append(mt.getPrecision());
      sb.append(")");
      break;
    case DECIMAL9:
    case DECIMAL18:
    case DECIMAL28DENSE:
    case DECIMAL28SPARSE:
    case DECIMAL38DENSE:
    case DECIMAL38SPARSE:
    case VARDECIMAL:
      // add scale and precision
      sb.append("(");
      sb.append(mt.getPrecision());
      sb.append(", ");
      sb.append(mt.getScale());
      sb.append(")");
      break;
    default:
      throw new UnsupportedOperationException(String.format("Unable to convert cast expression %s into string.", e));
    }
    sb.append(" )");
    return null;
  }

  @Override
  public Void visitFloatConstant(FloatExpression fExpr, StringBuilder sb) throws RuntimeException {
    sb.append(fExpr.getFloat());
    return null;
  }

  @Override
  public Void visitIntConstant(IntExpression intExpr, StringBuilder sb) throws RuntimeException {
    sb.append(intExpr.getInt());
    return null;
  }

  @Override
  public Void visitNullConstant(TypedNullConstant e, StringBuilder sb) throws RuntimeException {
    sb.append("NULL");
    return null;
  }

  @Override
  public Void visitNullExpression(NullExpression e, StringBuilder sb) throws RuntimeException {
    sb.append("NULL");
    return null;
  }

  @Override
  public Void visitUnknown(LogicalExpression e, StringBuilder sb) {
    sb.append(e.toString());
    return null;
  }
}
