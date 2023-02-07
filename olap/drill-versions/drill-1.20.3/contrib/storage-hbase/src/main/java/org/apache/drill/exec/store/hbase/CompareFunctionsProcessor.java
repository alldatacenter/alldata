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
package org.apache.drill.exec.store.hbase;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.ConvertExpression;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions.BooleanExpression;
import org.apache.drill.common.expression.ValueExpressions.DateExpression;
import org.apache.drill.common.expression.ValueExpressions.DoubleExpression;
import org.apache.drill.common.expression.ValueExpressions.FloatExpression;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.ValueExpressions.TimeExpression;
import org.apache.drill.common.expression.ValueExpressions.TimeStampExpression;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.hadoop.hbase.util.Order;
import org.apache.hadoop.hbase.util.PositionedByteRange;
import org.apache.hadoop.hbase.util.SimplePositionedMutableByteRange;

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PrefixFilter;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

public class CompareFunctionsProcessor extends AbstractExprVisitor<Boolean, LogicalExpression, RuntimeException> {

  // to check that function name starts with convert_from disregarding the case and has encoding after
  private static final Pattern convertFromPattern = Pattern.compile(String.format("^%s(.+)", ConvertExpression.CONVERT_FROM), Pattern.CASE_INSENSITIVE);

  private byte[] value;
  private boolean success;
  private final boolean isEqualityFn;
  private SchemaPath path;
  private String functionName;
  private boolean sortOrderAscending;

  // Fields for row-key prefix comparison
  // If the query is on row-key prefix, we cannot use a standard template to identify startRow, stopRow and filter
  // Hence, we use these local variables(set depending upon the encoding type in user query)
  private boolean isRowKeyPrefixComparison;
  private byte[] rowKeyPrefixStartRow;
  private byte[] rowKeyPrefixStopRow;
  private Filter rowKeyPrefixFilter;

  public static boolean isCompareFunction(String functionName) {
    return COMPARE_FUNCTIONS_TRANSPOSE_MAP.keySet().contains(functionName);
  }

  public static CompareFunctionsProcessor createFunctionsProcessorInstance(FunctionCall call, boolean nullComparatorSupported) {
    String functionName = call.getName();
    CompareFunctionsProcessor evaluator = new CompareFunctionsProcessor(functionName);

    return createFunctionsProcessorInstanceInternal(call, nullComparatorSupported, evaluator);
  }

  protected static <T extends CompareFunctionsProcessor> T createFunctionsProcessorInstanceInternal(FunctionCall call,
                                                                                                    boolean nullComparatorSupported,
                                                                                                    T evaluator) {
    LogicalExpression nameArg = call.arg(0);
    LogicalExpression valueArg = call.argCount() >= 2 ? call.arg(1) : null;
    if (valueArg != null) { // binary function
      if (VALUE_EXPRESSION_CLASSES.contains(nameArg.getClass())) {
        LogicalExpression swapArg = valueArg;
        valueArg = nameArg;
        nameArg = swapArg;
        evaluator.setFunctionName(COMPARE_FUNCTIONS_TRANSPOSE_MAP.get(evaluator.getFunctionName()));
      }
      evaluator.setSuccess(nameArg.accept(evaluator, valueArg));
    } else if (nullComparatorSupported && call.arg(0) instanceof SchemaPath) {
      evaluator.setSuccess(true);
      evaluator.setPath((SchemaPath) nameArg);
    }

    return evaluator;
  }

  public CompareFunctionsProcessor(String functionName) {
    this.success = false;
    this.functionName = functionName;
    this.isEqualityFn = COMPARE_FUNCTIONS_TRANSPOSE_MAP.containsKey(functionName)
        && COMPARE_FUNCTIONS_TRANSPOSE_MAP.get(functionName).equals(functionName);
    this.isRowKeyPrefixComparison = false;
    this.sortOrderAscending = true;
  }

  public byte[] getValue() {
    return value;
  }

  public boolean isSuccess() {
    return success;
  }

  protected void setSuccess(boolean success) {
    this.success = success;
  }

  public SchemaPath getPath() {
    return path;
  }

  protected void setPath(SchemaPath path) {
    this.path = path;
  }

  public String getFunctionName() {
    return functionName;
  }

  protected void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  public boolean isRowKeyPrefixComparison() {
  return isRowKeyPrefixComparison;
  }

  public byte[] getRowKeyPrefixStartRow() {
  return rowKeyPrefixStartRow;
  }

  public byte[] getRowKeyPrefixStopRow() {
  return rowKeyPrefixStopRow;
  }

  public Filter getRowKeyPrefixFilter() {
  return rowKeyPrefixFilter;
  }

  public boolean isSortOrderAscending() {
    return sortOrderAscending;
  }

  protected void setSortOrderAscending(boolean sortOrderAscending) {
    this.sortOrderAscending = sortOrderAscending;
  }

  @Override
  public Boolean visitCastExpression(CastExpression e, LogicalExpression valueArg) throws RuntimeException {
    if (e.getInput() instanceof CastExpression || e.getInput() instanceof SchemaPath) {
      return e.getInput().accept(this, valueArg);
    }
    return false;
  }

  @Override
  public Boolean visitConvertExpression(ConvertExpression e, LogicalExpression valueArg) throws RuntimeException {
    if (ConvertExpression.CONVERT_FROM.equals(e.getConvertFunction())) {

      String encodingType = e.getEncodingType();
      int prefixLength;

      // Handle scan pruning in the following scenario:
      // The row-key is a composite key and the CONVERT_FROM() function has byte_substr() as input function which is
      // querying for the first few bytes of the row-key(start-offset 1)
      // Example WHERE clause:
      // CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'DATE_EPOCH_BE') < DATE '2015-06-17'
      if (e.getInput() instanceof FunctionCall) {

        // We can prune scan range only for big-endian encoded data
        if (!encodingType.endsWith("_BE")) {
          return false;
        }

        FunctionCall call = (FunctionCall)e.getInput();
        String functionName = call.getName();
        if (!functionName.equalsIgnoreCase("byte_substr")) {
          return false;
        }

        LogicalExpression nameArg = call.arg(0);
        LogicalExpression valueArg1 = call.argCount() >= 2 ? call.arg(1) : null;
        LogicalExpression valueArg2 = call.argCount() >= 3 ? call.arg(2) : null;

        if (!(nameArg instanceof SchemaPath)
            || (valueArg1 == null) || !(valueArg1 instanceof IntExpression)
            || (valueArg2 == null) || !(valueArg2 instanceof IntExpression)) {
          return false;
        }

        boolean isRowKey = ((SchemaPath) nameArg).getRootSegmentPath().equals(DrillHBaseConstants.ROW_KEY);
        int offset = ((IntExpression) valueArg1).getInt();

        if (!isRowKey || offset != 1) {
          return false;
        }

        this.path = (SchemaPath) nameArg;
        prefixLength = ((IntExpression) valueArg2).getInt();
        this.isRowKeyPrefixComparison = true;
        return visitRowKeyPrefixConvertExpression(e, prefixLength, valueArg);
      }

      if (e.getInput() instanceof SchemaPath) {
        ByteBuf bb = null;
        switch (encodingType) {
          case "INT_BE":
          case "INT":
          case "UINT_BE":
          case "UINT":
          case "UINT4_BE":
          case "UINT4":
            if (valueArg instanceof IntExpression
                && (isEqualityFn || encodingType.startsWith("U"))) {
              bb = newByteBuf(4, encodingType.endsWith("_BE"));
              bb.writeInt(((IntExpression) valueArg).getInt());
            }
            break;
          case "BIGINT_BE":
          case "BIGINT":
          case "UINT8_BE":
          case "UINT8":
            if (valueArg instanceof LongExpression
                && (isEqualityFn || encodingType.startsWith("U"))) {
              bb = newByteBuf(8, encodingType.endsWith("_BE"));
              bb.writeLong(((LongExpression) valueArg).getLong());
            }
            break;
          case "FLOAT":
            if (valueArg instanceof FloatExpression && isEqualityFn) {
              bb = newByteBuf(4, true);
              bb.writeFloat(((FloatExpression) valueArg).getFloat());
            }
            break;
          case "DOUBLE":
            if (valueArg instanceof DoubleExpression && isEqualityFn) {
              bb = newByteBuf(8, true);
              bb.writeDouble(((DoubleExpression) valueArg).getDouble());
            }
            break;
          case "TIME_EPOCH":
          case "TIME_EPOCH_BE":
            if (valueArg instanceof TimeExpression) {
              bb = newByteBuf(8, encodingType.endsWith("_BE"));
              bb.writeLong(((TimeExpression) valueArg).getTime());
            }
            break;
          case "DATE_EPOCH":
          case "DATE_EPOCH_BE":
            if (valueArg instanceof DateExpression) {
              bb = newByteBuf(8, encodingType.endsWith("_BE"));
              bb.writeLong(((DateExpression) valueArg).getDate());
            }
            break;
          case "BOOLEAN_BYTE":
            if (valueArg instanceof BooleanExpression) {
              bb = newByteBuf(1, false /* does not matter */);
              bb.writeByte(((BooleanExpression) valueArg).getBoolean() ? 1 : 0);
            }
            break;
          case "DOUBLE_OB":
          case "DOUBLE_OBD":
            if (valueArg instanceof DoubleExpression) {
              bb = newByteBuf(9, true);
              PositionedByteRange br = new SimplePositionedMutableByteRange(bb.array(), 0, 9);
              if (encodingType.endsWith("_OBD")) {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeFloat64(br,
                  ((DoubleExpression) valueArg).getDouble(), Order.DESCENDING);
                this.sortOrderAscending = false;
              } else {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeFloat64(br,
                  ((DoubleExpression) valueArg).getDouble(), Order.ASCENDING);
              }
            }
            break;
          case "FLOAT_OB":
          case "FLOAT_OBD":
            if (valueArg instanceof FloatExpression) {
              bb = newByteBuf(5, true);
              PositionedByteRange br = new SimplePositionedMutableByteRange(bb.array(), 0, 5);
              if (encodingType.endsWith("_OBD")) {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeFloat32(br,
                  ((FloatExpression) valueArg).getFloat(), Order.DESCENDING);
                this.sortOrderAscending = false;
              } else {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeFloat32(br,
                          ((FloatExpression) valueArg).getFloat(), Order.ASCENDING);
              }
            }
            break;
          case "BIGINT_OB":
          case "BIGINT_OBD":
            if (valueArg instanceof LongExpression) {
              bb = newByteBuf(9, true);
              PositionedByteRange br = new SimplePositionedMutableByteRange(bb.array(), 0, 9);
              if (encodingType.endsWith("_OBD")) {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeInt64(br,
                          ((LongExpression) valueArg).getLong(), Order.DESCENDING);
                this.sortOrderAscending = false;
              } else {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeInt64(br,
                  ((LongExpression) valueArg).getLong(), Order.ASCENDING);
              }
            }
            break;
          case "INT_OB":
          case "INT_OBD":
            if (valueArg instanceof IntExpression) {
              bb = newByteBuf(5, true);
              PositionedByteRange br = new SimplePositionedMutableByteRange(bb.array(), 0, 5);
              if (encodingType.endsWith("_OBD")) {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeInt32(br,
                  ((IntExpression) valueArg).getInt(), Order.DESCENDING);
                this.sortOrderAscending = false;
              } else {
                org.apache.hadoop.hbase.util.OrderedBytes.encodeInt32(br,
                          ((IntExpression) valueArg).getInt(), Order.ASCENDING);
              }
            }
            break;
          case "UTF8":
            // let visitSchemaPath() handle this.
            return e.getInput().accept(this, valueArg);
          default:
            bb = getByteBuf(valueArg, encodingType);
        }

        if (bb != null) {
          this.value = bb.array();
          this.path = (SchemaPath)e.getInput();
          return true;
        }
      }
    }
    return false;
  }

  protected ByteBuf getByteBuf(LogicalExpression valueArg, String encodingType) {
    return null;
  }

  private Boolean visitRowKeyPrefixConvertExpression(ConvertExpression e,
                                                     int prefixLength, LogicalExpression valueArg) {
    String encodingType = e.getEncodingType();
    rowKeyPrefixStartRow = HConstants.EMPTY_START_ROW;
    rowKeyPrefixStopRow = HConstants.EMPTY_START_ROW;
    rowKeyPrefixFilter = null;

    if ((encodingType.compareTo("UINT4_BE") == 0)
        || (encodingType.compareTo("UINT_BE") == 0)) {
      if (prefixLength != 4) {
        throw new RuntimeException("Invalid length(" + prefixLength + ") of row-key prefix");
      }

      int val;
      if (!(valueArg instanceof IntExpression)) {
        return false;
      }

      val = ((IntExpression) valueArg).getInt();

      // For TIME_EPOCH_BE/BIGINT_BE encoding, the operators that we push-down are =, <>, <, <=, >, >=
      switch (functionName) {
        case FunctionNames.EQ:
          rowKeyPrefixFilter = new PrefixFilter(ByteBuffer.allocate(4).putInt(val).array());
          rowKeyPrefixStartRow = ByteBuffer.allocate(4).putInt(val).array();
          rowKeyPrefixStopRow = ByteBuffer.allocate(4).putInt(val + 1).array();
          return true;
        case FunctionNames.GE:
          rowKeyPrefixStartRow = ByteBuffer.allocate(4).putInt(val).array();
          return true;
        case FunctionNames.GT:
          rowKeyPrefixStartRow = ByteBuffer.allocate(4).putInt(val + 1).array();
          return true;
        case FunctionNames.LE:
          rowKeyPrefixStopRow = ByteBuffer.allocate(4).putInt(val + 1).array();
          return true;
        case FunctionNames.LT:
          rowKeyPrefixStopRow = ByteBuffer.allocate(4).putInt(val).array();
          return true;
      }

      return false;
    }

    if ((encodingType.compareTo("TIMESTAMP_EPOCH_BE") == 0)
        || (encodingType.compareTo("TIME_EPOCH_BE") == 0)
        || (encodingType.compareTo("UINT8_BE") == 0)) {

      if (prefixLength != 8) {
        throw new RuntimeException("Invalid length(" + prefixLength + ") of row-key prefix");
      }

      long val;
      if (encodingType.compareTo("TIME_EPOCH_BE") == 0) {
        if (!(valueArg instanceof TimeExpression)) {
          return false;
        }

        val = ((TimeExpression) valueArg).getTime();
      } else if (encodingType.compareTo("UINT8_BE") == 0) {
        if (!(valueArg instanceof LongExpression)) {
          return false;
        }

        val = ((LongExpression) valueArg).getLong();
      } else if (encodingType.compareTo("TIMESTAMP_EPOCH_BE") == 0) {
        if (!(valueArg instanceof TimeStampExpression)) {
          return false;
        }

        val = ((TimeStampExpression) valueArg).getTimeStamp();
      } else {
        // Should not reach here.
        return false;
      }

      // For TIME_EPOCH_BE/BIGINT_BE encoding, the operators that we push-down are =, <>, <, <=, >, >=
      switch (functionName) {
        case FunctionNames.EQ:
          rowKeyPrefixFilter = new PrefixFilter(ByteBuffer.allocate(8).putLong(val).array());
          rowKeyPrefixStartRow = ByteBuffer.allocate(8).putLong(val).array();
          rowKeyPrefixStopRow = ByteBuffer.allocate(8).putLong(val + 1).array();
          return true;
        case FunctionNames.GE:
          rowKeyPrefixStartRow = ByteBuffer.allocate(8).putLong(val).array();
          return true;
        case FunctionNames.GT:
          rowKeyPrefixStartRow = ByteBuffer.allocate(8).putLong(val + 1).array();
          return true;
        case FunctionNames.LE:
          rowKeyPrefixStopRow = ByteBuffer.allocate(8).putLong(val + 1).array();
          return true;
        case FunctionNames.LT:
          rowKeyPrefixStopRow = ByteBuffer.allocate(8).putLong(val).array();
          return true;
      }

      return false;
    }

    if (encodingType.compareTo("DATE_EPOCH_BE") == 0) {
      if (!(valueArg instanceof DateExpression)) {
        return false;
      }

      if (prefixLength != 8) {
        throw new RuntimeException("Invalid length(" + prefixLength + ") of row-key prefix");
      }

      final long MILLISECONDS_IN_A_DAY = 1000 * 60 * 60 * 24;
      long dateToSet;
      // For DATE encoding, the operators that we push-down are =, <>, <, <=, >, >=
      switch (functionName) {
        case FunctionNames.EQ:
          long startDate = ((DateExpression) valueArg).getDate();
          rowKeyPrefixStartRow = ByteBuffer.allocate(8).putLong(startDate).array();
          long stopDate = ((DateExpression) valueArg).getDate() + MILLISECONDS_IN_A_DAY;
          rowKeyPrefixStopRow = ByteBuffer.allocate(8).putLong(stopDate).array();
          return true;
        case FunctionNames.GE:
          dateToSet = ((DateExpression) valueArg).getDate();
          rowKeyPrefixStartRow = ByteBuffer.allocate(8).putLong(dateToSet).array();
          return true;
        case FunctionNames.GT:
          dateToSet = ((DateExpression) valueArg).getDate() + MILLISECONDS_IN_A_DAY;
          rowKeyPrefixStartRow = ByteBuffer.allocate(8).putLong(dateToSet).array();
          return true;
        case FunctionNames.LE:
          dateToSet = ((DateExpression) valueArg).getDate() + MILLISECONDS_IN_A_DAY;
          rowKeyPrefixStopRow = ByteBuffer.allocate(8).putLong(dateToSet).array();
          return true;
        case FunctionNames.LT:
          dateToSet = ((DateExpression) valueArg).getDate();
          rowKeyPrefixStopRow = ByteBuffer.allocate(8).putLong(dateToSet).array();
          return true;
      }

      return false;
    }

    return false;
  }

  @Override
  public Boolean visitUnknown(LogicalExpression e, LogicalExpression valueArg) throws RuntimeException {
    return false;
  }

  @Override
  public Boolean visitSchemaPath(SchemaPath path, LogicalExpression valueArg) throws RuntimeException {
    if (valueArg instanceof QuotedString) {
      this.value = ((QuotedString) valueArg).value.getBytes(Charsets.UTF_8);
      this.path = path;
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitFunctionCall(FunctionCall call, LogicalExpression valueArg) {
    Matcher matcher = convertFromPattern.matcher(call.getName());
    if (matcher.find()) {
      // convert function call to ConvertExpression
      ConvertExpression convert = new ConvertExpression(
          ConvertExpression.CONVERT_FROM, matcher.group(1),
          call.arg(0), call.getPosition());
      return visitConvertExpression(convert, valueArg);
    }
    return false;
  }

  protected static ByteBuf newByteBuf(int size, boolean bigEndian) {
    return Unpooled.wrappedBuffer(new byte[size])
        .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
        .writerIndex(0);
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
        .put(FunctionNames.LIKE, FunctionNames.LIKE)
        .put(FunctionNames.EQ, FunctionNames.EQ)
        .put(FunctionNames.NE, FunctionNames.NE)
        .put(FunctionNames.GE, FunctionNames.LE)
        .put(FunctionNames.GT, FunctionNames.LT)
        .put(FunctionNames.LE, FunctionNames.GE)
        .put(FunctionNames.LT, FunctionNames.GT)
        .build();
  }
}
