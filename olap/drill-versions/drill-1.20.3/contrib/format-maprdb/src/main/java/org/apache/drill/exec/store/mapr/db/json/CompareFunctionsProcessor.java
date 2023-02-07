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
package org.apache.drill.exec.store.mapr.db.json;

import static com.mapr.db.rowcol.DBValueBuilderImpl.KeyValueBuilder;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions.BooleanExpression;
import org.apache.drill.common.expression.ValueExpressions.DateExpression;
import org.apache.drill.common.expression.ValueExpressions.Decimal28Expression;
import org.apache.drill.common.expression.ValueExpressions.Decimal38Expression;
import org.apache.drill.common.expression.ValueExpressions.DoubleExpression;
import org.apache.drill.common.expression.ValueExpressions.FloatExpression;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.ValueExpressions.VarDecimalExpression;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.joda.time.LocalTime;
import org.ojai.Value;
import org.ojai.types.ODate;
import org.ojai.types.OTime;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import com.mapr.db.util.SqlHelper;

import org.ojai.types.OTimestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

class CompareFunctionsProcessor extends AbstractExprVisitor<Boolean, LogicalExpression, RuntimeException> {

  private String functionName;
  private Boolean success;
  protected Value value;
  protected SchemaPath path;

  public CompareFunctionsProcessor(String functionName) {
    this.functionName = functionName;
    this.success = false;
    this.value = null;
  }

  public static boolean isCompareFunction(String functionName) {
    return COMPARE_FUNCTIONS_TRANSPOSE_MAP.keySet().contains(functionName);
  }

  @Override
  public Boolean visitUnknown(LogicalExpression e, LogicalExpression valueArg) throws RuntimeException {
    return false;
  }

  /**
   * Converts specified function call to be pushed into maprDB JSON scan.
   *
   * @param call function call to be pushed
   * @return CompareFunctionsProcessor instance which contains converted function call
   */
  public static CompareFunctionsProcessor process(FunctionCall call) {
    return processWithEvaluator(call, new CompareFunctionsProcessor(call.getName()));
  }

  /**
   * Converts specified function call to be pushed into maprDB JSON scan.
   * For the case when timestamp value is used, it is converted to UTC timezone
   * before converting to {@link OTimestamp} instance.
   *
   * @param call function call to be pushed
   * @return CompareFunctionsProcessor instance which contains converted function call
   */
  public static CompareFunctionsProcessor processWithTimeZoneOffset(FunctionCall call) {
    CompareFunctionsProcessor processor = new CompareFunctionsProcessor(call.getName()) {
      @Override
      protected boolean visitTimestampExpr(SchemaPath path, TimeStampExpression valueArg) {
        // converts timestamp value from local time zone to UTC since the record reader
        // reads the timestamp in local timezone if the readTimestampWithZoneOffset flag is enabled
        Instant localInstant = Instant.ofEpochMilli(valueArg.getTimeStamp());
        ZonedDateTime utcZonedDateTime = localInstant.atZone(ZoneId.of("UTC"));
        ZonedDateTime convertedZonedDateTime = utcZonedDateTime.withZoneSameLocal(ZoneId.systemDefault());
        long timeStamp = convertedZonedDateTime.toInstant().toEpochMilli();

        this.value = KeyValueBuilder.initFrom(new OTimestamp(timeStamp));
        this.path = path;
        return true;
      }
    };
    return processWithEvaluator(call, processor);
  }

  private static CompareFunctionsProcessor processWithEvaluator(FunctionCall call, CompareFunctionsProcessor evaluator) {
    String functionName = call.getName();
    LogicalExpression nameArg = call.arg(0);
    LogicalExpression valueArg = call.argCount() >= 2 ? call.arg(1) : null;

    if (VALUE_EXPRESSION_CLASSES.contains(nameArg.getClass())) {
      LogicalExpression swapArg = valueArg;
      valueArg = nameArg;
      nameArg = swapArg;
      evaluator.functionName = COMPARE_FUNCTIONS_TRANSPOSE_MAP.get(functionName);
    }
    if (nameArg != null) {
      evaluator.success = nameArg.accept(evaluator, valueArg);
    }

    return evaluator;
  }

  public boolean isSuccess() {
    // TODO Auto-generated method stub
    return success;
  }

  public SchemaPath getPath() {
    return path;
  }

  public Value getValue() {
    return value;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Override
  public Boolean visitSchemaPath(SchemaPath path, LogicalExpression valueArg) throws RuntimeException {
    // If valueArg is null, this might be a IS NULL/IS NOT NULL type of query
    if (valueArg == null) {
      this.path = path;
      return true;
    }

    if (valueArg instanceof QuotedString) {
      this.value = SqlHelper.decodeStringAsValue(((QuotedString) valueArg).value);
      this.path = path;
      return true;
    }

    if (valueArg instanceof IntExpression) {
      this.value = KeyValueBuilder.initFrom(((IntExpression)valueArg).getInt());
      this.path = path;
      return true;
    }

    if (valueArg instanceof FloatExpression) {
      this.value = KeyValueBuilder.initFrom(((FloatExpression)valueArg).getFloat());
      this.path = path;
      return true;
    }

    if (valueArg instanceof BooleanExpression) {
      this.value = KeyValueBuilder.initFrom(((BooleanExpression)valueArg).getBoolean());
      this.path = path;
      return true;
    }

    if (valueArg instanceof Decimal28Expression) {
      this.value = KeyValueBuilder.initFrom(((Decimal28Expression)valueArg).getBigDecimal());
      this.path = path;
      return true;
    }

    if (valueArg instanceof Decimal38Expression) {
      this.value = KeyValueBuilder.initFrom(((Decimal38Expression)valueArg).getBigDecimal());
      this.path = path;
      return true;
    }

    if (valueArg instanceof DoubleExpression) {
      this.value = KeyValueBuilder.initFrom(((DoubleExpression)valueArg).getDouble());
      this.path = path;
      return true;
    }

    if (valueArg instanceof LongExpression) {
      this.value = KeyValueBuilder.initFrom(((LongExpression)valueArg).getLong());
      this.path = path;
      return true;
    }

    if (valueArg instanceof DateExpression) {
      long d = ((DateExpression)valueArg).getDate();
      final long MILLISECONDS_IN_A_DAY  = (long)1000 * 60 * 60 * 24;
      int daysSinceEpoch = (int)(d / MILLISECONDS_IN_A_DAY);
      this.value = KeyValueBuilder.initFrom(ODate.fromDaysSinceEpoch(daysSinceEpoch));
      this.path = path;
      return true;
    }

    if (valueArg instanceof TimeExpression) {
      int t = ((TimeExpression)valueArg).getTime();
      LocalTime lT = LocalTime.fromMillisOfDay(t);
      this.value = KeyValueBuilder.initFrom(new OTime(lT.getHourOfDay(), lT.getMinuteOfHour(), lT.getSecondOfMinute(), lT.getMillisOfSecond()));
      this.path = path;
      return true;
    }

    // MaprDB does not support decimals completely, therefore double value is used.
    // See com.mapr.db.impl.ConditionImpl.is(FieldPath path, QueryCondition.Op op, BigDecimal value) method
    if (valueArg instanceof VarDecimalExpression) {
      this.value = KeyValueBuilder.initFrom(((VarDecimalExpression) valueArg).getBigDecimal().doubleValue());
      this.path = path;
      return true;
    }

    if (valueArg instanceof TimeStampExpression) {
      return visitTimestampExpr(path, (TimeStampExpression) valueArg);
    }
    return false;
  }

  protected boolean visitTimestampExpr(SchemaPath path, TimeStampExpression valueArg) {
    this.value = KeyValueBuilder.initFrom(new OTimestamp(valueArg.getTimeStamp()));
    this.path = path;
    return true;
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
        .add(VarDecimalExpression.class)
        .build();
  }

  private static final ImmutableMap<String, String> COMPARE_FUNCTIONS_TRANSPOSE_MAP;
  static {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    COMPARE_FUNCTIONS_TRANSPOSE_MAP = builder
     // unary functions
        .put(FunctionNames.IS_NOT_NULL, FunctionNames.IS_NOT_NULL)
        .put("isNotNull", "isNotNull")
        .put("is not null", "is not null")
        .put(FunctionNames.IS_NULL, FunctionNames.IS_NULL)
        .put("isNull", "isNull")
        .put("is null", "is null")
        // binary functions
        .put("like", "like")
        .put(FunctionNames.EQ, FunctionNames.EQ)
        .put(FunctionNames.NE, FunctionNames.NE)
        .put(FunctionNames.GE, FunctionNames.LE)
        .put(FunctionNames.GT, FunctionNames.LT)
        .put(FunctionNames.LE, FunctionNames.GE)
        .put(FunctionNames.LT, FunctionNames.GT)
        .build();
  }

}
