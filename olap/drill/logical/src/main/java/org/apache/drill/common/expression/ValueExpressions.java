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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;

public class ValueExpressions {

  public static LogicalExpression getBigInt(long l){
    return new LongExpression(l);
  }

  public static LogicalExpression getInt(int i){
    return new IntExpression(i, ExpressionPosition.UNKNOWN);
  }

  public static LogicalExpression getFloat8(double d){
    return new DoubleExpression(d, ExpressionPosition.UNKNOWN);
  }
  public static LogicalExpression getFloat4(float f){
    return new FloatExpression(f, ExpressionPosition.UNKNOWN);
  }

  public static LogicalExpression getBit(boolean b){
    return new BooleanExpression(Boolean.toString(b), ExpressionPosition.UNKNOWN);
  }

  public static LogicalExpression getChar(String s, int precision){
    return new QuotedString(s, precision, ExpressionPosition.UNKNOWN);
  }

  public static LogicalExpression getDate(GregorianCalendar date) {
    return new org.apache.drill.common.expression.ValueExpressions.DateExpression(date.getTimeInMillis());
  }

  public static LogicalExpression getDate(long milliSecond){
    return new org.apache.drill.common.expression.ValueExpressions.DateExpression(milliSecond);
  }

  public static LogicalExpression getTime(GregorianCalendar time) {
    int millis = time.get(GregorianCalendar.HOUR_OF_DAY) * 60 * 60 * 1000 +
                 time.get(GregorianCalendar.MINUTE) * 60 * 1000 +
                 time.get(GregorianCalendar.SECOND) * 1000 +
                 time.get(GregorianCalendar.MILLISECOND);

    return new TimeExpression(millis);
  }

  public static LogicalExpression getTime(int milliSeconds) {
    return new TimeExpression(milliSeconds);
  }

  public static LogicalExpression getTimeStamp(GregorianCalendar date) {
    return new org.apache.drill.common.expression.ValueExpressions.TimeStampExpression(date.getTimeInMillis());
  }

  public static LogicalExpression getTimeStamp(long milliSeconds) {
    return new org.apache.drill.common.expression.ValueExpressions.TimeStampExpression(milliSeconds);
  }

  public static LogicalExpression getIntervalYear(int months) {
    return new IntervalYearExpression(months);
  }

  public static LogicalExpression getIntervalDay(long intervalInMillis) {
      return new IntervalDayExpression(intervalInMillis);
  }

  public static LogicalExpression getVarDecimal(BigDecimal input, int precision, int scale) {
    return new VarDecimalExpression(input, precision, scale, ExpressionPosition.UNKNOWN);
  }

  public static LogicalExpression getNumericExpression(String sign, String s, ExpressionPosition ep) {
    String numStr = (sign == null) ? s : sign+s;
    try {
      int a = Integer.parseInt(numStr);
      return new IntExpression(a, ep);
    } catch (Exception e) { }

    try {
      long l = Long.parseLong(numStr);
      return new LongExpression(l, ep);
    } catch (Exception e) { }

    try {
      double d = Double.parseDouble(numStr);
      return new DoubleExpression(d, ep);
    } catch (Exception e) { }

    throw new IllegalArgumentException(String.format("Unable to parse string %s as integer or floating point number.",
        numStr));
  }

  public static LogicalExpression getParameterExpression(String name, MajorType type) {
    return new ParameterExpression(name, type, ExpressionPosition.UNKNOWN);
  }

  protected static abstract class ValueExpression<V> extends LogicalExpressionBase {
    public final V value;

    protected ValueExpression(String value, ExpressionPosition pos) {
      super(pos);
      this.value = parseValue(value);
    }

    protected abstract V parseValue(String s);

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class BooleanExpression extends ValueExpression<Boolean> {

    public static final BooleanExpression TRUE = new BooleanExpression("true", ExpressionPosition.UNKNOWN);
    public static final BooleanExpression FALSE = new BooleanExpression("false", ExpressionPosition.UNKNOWN);

    public BooleanExpression(String value, ExpressionPosition pos) {
      super(value, pos);
    }

    @Override
    protected Boolean parseValue(String s) {
      return Boolean.parseBoolean(s);
    }

    @Override
    public MajorType getMajorType() {
      return Types.REQUIRED_BIT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitBooleanConstant(this, value);
    }

    public boolean getBoolean() {
      return value;
    }
  }

  public static class FloatExpression extends LogicalExpressionBase {
    private final float f;

    private static final MajorType FLOAT_CONSTANT = Types.required(MinorType.FLOAT4);

    public FloatExpression(float f, ExpressionPosition pos) {
      super(pos);
      this.f = f;
    }

    public float getFloat() {
      return f;
    }

    @Override
    public MajorType getMajorType() {
      return FLOAT_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitFloatConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class IntExpression extends LogicalExpressionBase {

    private static final MajorType INT_CONSTANT = Types.required(MinorType.INT);

    private final int i;

    public IntExpression(int i, ExpressionPosition pos) {
      super(pos);
      this.i = i;
    }

    public int getInt() {
      return i;
    }

    @Override
    public MajorType getMajorType() {
      return INT_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitIntConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class Decimal9Expression extends LogicalExpressionBase {

    private final int decimal;
    private final int scale;
    private final int precision;

    public Decimal9Expression(BigDecimal input, ExpressionPosition pos) {
      super(pos);
      this.scale = input.scale();
      this.precision = input.precision();
      this.decimal = input.setScale(scale, BigDecimal.ROUND_HALF_UP).intValue();
    }

    public int getIntFromDecimal() {
      return decimal;
    }

    public int getScale() {
      return scale;
    }

    public int getPrecision() {
      return precision;
    }

    @Override
    public MajorType getMajorType() {
      return MajorType.newBuilder().setMinorType(MinorType.DECIMAL9).setScale(scale).setPrecision(precision).setMode(DataMode.REQUIRED).build();
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitDecimal9Constant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class Decimal18Expression extends LogicalExpressionBase {

    private final long decimal;
    private final int scale;
    private final int precision;

    public Decimal18Expression(BigDecimal input, ExpressionPosition pos) {
      super(pos);
      this.scale = input.scale();
      this.precision = input.precision();
      this.decimal = input.setScale(scale, BigDecimal.ROUND_HALF_UP).longValue();
    }

    public long getLongFromDecimal() {
      return decimal;
    }

    public int getScale() {
      return scale;
    }

    public int getPrecision() {
      return precision;
    }

    @Override
    public MajorType getMajorType() {
      return MajorType.newBuilder().setMinorType(MinorType.DECIMAL18).setScale(scale).setPrecision(precision).setMode(DataMode.REQUIRED).build();
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitDecimal18Constant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class Decimal28Expression extends LogicalExpressionBase {

    private final BigDecimal bigDecimal;

    public Decimal28Expression(BigDecimal input, ExpressionPosition pos) {
      super(pos);
      this.bigDecimal = input;
    }

    public BigDecimal getBigDecimal() {
      return bigDecimal;
    }

    @Override
    public MajorType getMajorType() {
      return MajorType.newBuilder().setMinorType(MinorType.DECIMAL28SPARSE).setScale(bigDecimal.scale()).setPrecision(bigDecimal.precision()).setMode(DataMode.REQUIRED).build();
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitDecimal28Constant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class Decimal38Expression extends LogicalExpressionBase {

    private final BigDecimal bigDecimal;

    public Decimal38Expression(BigDecimal input, ExpressionPosition pos) {
      super(pos);
      this.bigDecimal = input;
    }

    public BigDecimal getBigDecimal() {
      return bigDecimal;
    }

    @Override
    public MajorType getMajorType() {
      return MajorType.newBuilder().setMinorType(MinorType.DECIMAL38SPARSE).setScale(bigDecimal.scale()).setPrecision(bigDecimal.precision()).setMode(DataMode.REQUIRED).build();
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitDecimal38Constant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class VarDecimalExpression extends LogicalExpressionBase {

    private final BigDecimal bigDecimal;
    private final int precision;
    private final int scale;

    public VarDecimalExpression(BigDecimal input, int precision, int scale, ExpressionPosition pos) {
      super(pos);
      this.bigDecimal = input;
      this.precision = precision;
      this.scale = scale;
    }

    public BigDecimal getBigDecimal() {
      return bigDecimal;
    }

    @Override
    public MajorType getMajorType() {
      return MajorType
          .newBuilder()
          .setMinorType(MinorType.VARDECIMAL)
          .setScale(scale)
          .setPrecision(precision)
          .setMode(DataMode.REQUIRED)
          .build();
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitVarDecimalConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class DoubleExpression extends LogicalExpressionBase {
    private final double d;

    private static final MajorType DOUBLE_CONSTANT = Types.required(MinorType.FLOAT8);

    public DoubleExpression(double d, ExpressionPosition pos) {
      super(pos);
      this.d = d;
    }

    public double getDouble() {
      return d;
    }

    @Override
    public MajorType getMajorType() {
      return DOUBLE_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitDoubleConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class LongExpression extends LogicalExpressionBase {

    private static final MajorType LONG_CONSTANT = Types.required(MinorType.BIGINT);

    private final long l;

    public LongExpression(long l) {
      this(l, ExpressionPosition.UNKNOWN);
    }

    public LongExpression(long l, ExpressionPosition pos) {
      super(pos);
      this.l = l;
    }

    public long getLong() {
      return l;
    }

    @Override
    public MajorType getMajorType() {
      return LONG_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitLongConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class DateExpression extends LogicalExpressionBase {

    private static final MajorType DATE_CONSTANT = Types.required(MinorType.DATE);

    private final long dateInMillis;

    public DateExpression(long l) {
      this(l, ExpressionPosition.UNKNOWN);
    }

    public DateExpression(long dateInMillis, ExpressionPosition pos) {
      super(pos);
      this.dateInMillis = dateInMillis;
    }

    public long getDate() {
      return dateInMillis;
    }

    @Override
    public MajorType getMajorType() {
      return DATE_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitDateConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class TimeExpression extends LogicalExpressionBase {

    private static final MajorType TIME_CONSTANT = Types.required(MinorType.TIME);

    private final int timeInMillis;

    public TimeExpression(int timeInMillis) {
      this(timeInMillis, ExpressionPosition.UNKNOWN);
    }

    public TimeExpression(int timeInMillis, ExpressionPosition pos) {
      super(pos);
      this.timeInMillis = timeInMillis;
    }

    public int getTime() {
      return timeInMillis;
    }

    @Override
    public MajorType getMajorType() {
      return TIME_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitTimeConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class TimeStampExpression extends LogicalExpressionBase {

    private static final MajorType TIMESTAMP_CONSTANT = Types.required(MinorType.TIMESTAMP);

    private final long timeInMillis;

    public TimeStampExpression(long timeInMillis) {
      this(timeInMillis, ExpressionPosition.UNKNOWN);
    }

    public TimeStampExpression(long timeInMillis, ExpressionPosition pos) {
      super(pos);
      this.timeInMillis = timeInMillis;
    }

    public long getTimeStamp() {
      return timeInMillis;
    }

    @Override
    public MajorType getMajorType() {
      return TIMESTAMP_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitTimeStampConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class IntervalYearExpression extends LogicalExpressionBase {

    private static final MajorType INTERVALYEAR_CONSTANT = Types.required(MinorType.INTERVALYEAR);

    private final int months;

    public IntervalYearExpression(int months) {
      this(months, ExpressionPosition.UNKNOWN);
    }

    public IntervalYearExpression(int months, ExpressionPosition pos) {
      super(pos);
      this.months = months;
    }

    public int getIntervalYear() {
      return months;
    }

    @Override
    public MajorType getMajorType() {
      return INTERVALYEAR_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitIntervalYearConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class IntervalDayExpression extends LogicalExpressionBase {

    private static final MajorType INTERVALDAY_CONSTANT = Types.required(MinorType.INTERVALDAY);
    private static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;

    private final int days;
    private final int millis;

    public IntervalDayExpression(long intervalInMillis) {
      this((int) (intervalInMillis / MILLIS_IN_DAY), (int) (intervalInMillis % MILLIS_IN_DAY), ExpressionPosition.UNKNOWN);
    }

    public IntervalDayExpression(int days, int millis, ExpressionPosition pos) {
      super(pos);
      this.days = days;
      this.millis = millis;
    }

    public int getIntervalDay() {
      return days;
    }

    public int getIntervalMillis() {
        return millis;
    }

    @Override
    public MajorType getMajorType() {
      return INTERVALDAY_CONSTANT;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitIntervalDayConstant(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return Collections.emptyIterator();
    }
  }

  public static class QuotedString extends ValueExpression<String> {

    public static final QuotedString EMPTY_STRING = new QuotedString("", 0, ExpressionPosition.UNKNOWN);

    private final int precision;

    public QuotedString(String value, int precision, ExpressionPosition pos) {
      super(value, pos);
      this.precision = precision;
    }

    public String getString() {
      return value;
    }

    @Override
    protected String parseValue(String s) {
      return s;
    }

    @Override
    public MajorType getMajorType() {
      return Types.withPrecision(MinorType.VARCHAR, DataMode.REQUIRED, precision);
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitQuotedStringConstant(this, value);
    }
  }

  /**
   * Identifies method parameter based on given name and type.
   */
  public static class ParameterExpression extends LogicalExpressionBase {

    private final String name;
    private final MajorType type;

    protected ParameterExpression(String name, MajorType type, ExpressionPosition pos) {
      super(pos);
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    @Override
    public MajorType getMajorType() {
      return type;
    }

    @Override
    public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
      return visitor.visitParameter(this, value);
    }

    @Override
    public Iterator<LogicalExpression> iterator() {
      return ImmutableList.<LogicalExpression>of().iterator();
    }
  }
}
