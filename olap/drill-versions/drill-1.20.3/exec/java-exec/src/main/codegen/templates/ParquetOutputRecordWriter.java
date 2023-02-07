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
import org.apache.parquet.io.api.Binary;

import java.lang.Override;
import java.lang.RuntimeException;
import java.util.Arrays;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="org/apache/drill/exec/store/ParquetOutputRecordWriter.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.primitives.Ints;
import org.apache.drill.shaded.guava.com.google.common.primitives.Longs;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.parquet.ParquetTypeHelper;
import org.apache.drill.exec.store.parquet.decimal.DecimalValueWriter;
import org.apache.drill.exec.vector.*;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.io.api.Binary;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;


import org.apache.drill.common.types.TypeProtos;

import org.joda.time.DateTimeConstants;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of RecordWriter interface which exposes interface:
 *    {@link #writeHeader(List)}
 *    {@link #addField(int,String)}
 * to output the data in string format instead of implementing addField for each type holder.
 *
 * This is useful for text format writers such as CSV, TSV etc.
 *
 * NB: Source code generated using FreeMarker template ${.template_name}
 */
public abstract class ParquetOutputRecordWriter extends AbstractRecordWriter implements RecordWriter {

  /**
   * Name of nested group for Parquet's {@code LIST} type.
   * @see <a href="https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists">LIST logical type</a>
   */
  protected static final String LIST = "list";

  /**
   * Name of Parquet's {@code LIST} element type.
   * @see #LIST
   */
  protected static final String ELEMENT = "element";
  protected static final int ZERO_IDX = 0;

  private RecordConsumer consumer;
  private MessageType schema;

  public void setUp(MessageType schema, RecordConsumer consumer) {
    this.schema = schema;
    this.consumer = consumer;
  }

  protected abstract PrimitiveType getPrimitiveType(MaterializedField field);

  public abstract class BaseFieldConverter extends FieldConverter {

    public BaseFieldConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    public abstract void read();

    public abstract void read(int i);

    public abstract void consume();

    @Override
    public void writeField() throws IOException {
      read();
      consume();
    }
  }

  public class NullableFieldConverter extends FieldConverter {
    private BaseFieldConverter delegate;

    public NullableFieldConverter(int fieldId, String fieldName, FieldReader reader, BaseFieldConverter delegate) {
      super(fieldId, fieldName, reader);
      this.delegate = delegate;
    }

    @Override
    public void writeField() throws IOException {
      if (!reader.isSet()) {
        return;
      }
      consumer.startField(fieldName, fieldId);
      delegate.writeField();
      consumer.endField(fieldName, fieldId);
    }

    public void setPosition(int index) {
      delegate.setPosition(index);
    }

    public void startField() throws IOException {
      delegate.startField();
    }

    public void endField() throws IOException {
      delegate.endField();
    }
  }

  public class RequiredFieldConverter extends FieldConverter {
    private BaseFieldConverter delegate;

    public RequiredFieldConverter(int fieldId, String fieldName, FieldReader reader, BaseFieldConverter delegate) {
      super(fieldId, fieldName, reader);
      this.delegate = delegate;
    }

    @Override
    public void writeField() throws IOException {
      consumer.startField(fieldName, fieldId);
      delegate.writeField();
      consumer.endField(fieldName, fieldId);
    }

    public void setPosition(int index) {
      delegate.setPosition(index);
    }

    public void startField() throws IOException {
      delegate.startField();
    }

    public void endField() throws IOException {
      delegate.endField();
    }
  }

  public class RepeatedFieldConverter extends FieldConverter {

    private BaseFieldConverter delegate;

    public RepeatedFieldConverter(int fieldId, String fieldName, FieldReader reader, BaseFieldConverter delegate) {
      super(fieldId, fieldName, reader);
      this.delegate = delegate;
    }

    @Override
    public void writeField() throws IOException {
      // empty lists are represented by simply not starting a field, rather than starting one and putting in 0 elements
      if (reader.size() == 0) {
        return;
      }
      consumer.startField(fieldName, fieldId);
      for (int i = 0; i < reader.size(); i++) {
        delegate.read(i);
        delegate.consume();
      }
      consumer.endField(fieldName, fieldId);
    }

    @Override
    public void writeListField() {
      if (reader.size() == 0) {
        return;
      }
      consumer.startField(LIST, ZERO_IDX);
      for (int i = 0; i < reader.size(); i++) {
        consumer.startGroup();
        consumer.startField(ELEMENT, ZERO_IDX);

        delegate.read(i);
        delegate.consume();

        consumer.endField(ELEMENT, ZERO_IDX);
        consumer.endGroup();
      }
      consumer.endField(LIST, ZERO_IDX);
    }

    public void setPosition(int index) {
      delegate.setPosition(index);
    }

    public void startField() throws IOException {
      delegate.startField();
    }

    public void endField() throws IOException {
      delegate.endField();
    }
  }

<#list vv.types as type>
  <#list type.minor as minor>
    <#list vv.modes as mode>
  @Override
  public FieldConverter getNew${mode.prefix}${minor.class}Converter(int fieldId, String fieldName, FieldReader reader) {
    BaseFieldConverter converter = new ${minor.class}ParquetConverter(fieldId, fieldName, reader);
  <#if mode.prefix == "Nullable">
    return new NullableFieldConverter(fieldId, fieldName, reader, converter);
  <#elseif mode.prefix == "Repeated">
    return new RepeatedFieldConverter(fieldId, fieldName, reader, converter);
  <#else>
    return new RequiredFieldConverter(fieldId, fieldName, reader, converter);
  </#if>
  }

    </#list>

  public class ${minor.class}ParquetConverter extends BaseFieldConverter {
    private Nullable${minor.class}Holder holder = new Nullable${minor.class}Holder();
    <#if minor.class?contains("Interval")>
    private final byte[] output = new byte[12];
    <#elseif minor.class == "VarDecimal">
    private final DecimalValueWriter decimalValueWriter;
    </#if>

    public ${minor.class}ParquetConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      <#if minor.class == "VarDecimal">
      decimalValueWriter = DecimalValueWriter.
          getDecimalValueWriterForType(getPrimitiveType(reader.getField()).getPrimitiveTypeName());
      </#if>
    }

    @Override
    public void read() {
      reader.read(holder);
    }

    @Override
    public void read(int i) {
      reader.read(i, holder);
    }

    @Override
    public void consume() {
  <#if  minor.class == "TinyInt" ||
        minor.class == "UInt1" ||
        minor.class == "UInt2" ||
        minor.class == "SmallInt" ||
        minor.class == "Int" ||
        minor.class == "Time" ||
        minor.class == "Decimal9" ||
        minor.class == "UInt4">
      consumer.addInteger(holder.value);
  <#elseif
        minor.class == "Float4">
      consumer.addFloat(holder.value);
  <#elseif
        minor.class == "BigInt" ||
        minor.class == "Decimal18" ||
        minor.class == "TimeStamp" ||
        minor.class == "UInt8">
      consumer.addLong(holder.value);
  <#elseif minor.class == "Date">
      // convert from internal Drill date format to Julian Day centered around Unix Epoc
      consumer.addInteger((int) (holder.value / DateTimeConstants.MILLIS_PER_DAY));
  <#elseif
        minor.class == "Float8">
      consumer.addDouble(holder.value);
  <#elseif
        minor.class == "Bit">
      consumer.addBoolean(holder.value == 1);
  <#elseif
        minor.class == "Decimal28Sparse" ||
        minor.class == "Decimal38Sparse">
      byte[] bytes = DecimalUtility.getBigDecimalFromSparse(
              holder.buffer, holder.start, ${minor.class}Holder.nDecimalDigits, holder.scale).unscaledValue().toByteArray();
      byte[] output = new byte[ParquetTypeHelper.getLengthForMinorType(MinorType.${minor.class?upper_case})];
      if (holder.getSign(holder.start, holder.buffer)) {
        Arrays.fill(output, 0, output.length - bytes.length, (byte) -1);
      } else {
        Arrays.fill(output, 0, output.length - bytes.length, (byte) 0);
      }
      System.arraycopy(bytes, 0, output, output.length - bytes.length, bytes.length);
      consumer.addBinary(Binary.fromByteArray(output));
  <#elseif minor.class?contains("Interval")>
      <#if minor.class == "IntervalDay">
        Arrays.fill(output, 0, 4, (byte) 0);
        IntervalUtility.intToLEByteArray(holder.days, output, 4);
        IntervalUtility.intToLEByteArray(holder.milliseconds, output, 8);
      <#elseif minor.class == "IntervalYear">
        IntervalUtility.intToLEByteArray(holder.value, output, 0);
        Arrays.fill(output, 4, 8, (byte) 0);
        Arrays.fill(output, 8, 12, (byte) 0);
      <#elseif minor.class == "Interval">
        IntervalUtility.intToLEByteArray(holder.months, output, 0);
        IntervalUtility.intToLEByteArray(holder.days, output, 4);
        IntervalUtility.intToLEByteArray(holder.milliseconds, output, 8);
      </#if>
      consumer.addBinary(Binary.fromByteArray(output));
  <#elseif minor.class == "VarDecimal">
      decimalValueWriter.writeValue(consumer, holder.buffer,
          holder.start, holder.end, reader.getField().getPrecision());
  <#elseif minor.class == "VarChar" || minor.class == "Var16Char"
        || minor.class == "VarBinary">
      consumer.addBinary(Binary.fromByteBuffer(holder.buffer.nioBuffer(holder.start, holder.end - holder.start)));
  </#if>
    }

  }
  </#list>
</#list>

  private static class IntervalUtility {
    private static void intToLEByteArray(final int value, final byte[] output, final int outputIndex) {
      int shiftOrder = 0;
      for (int i = outputIndex; i < outputIndex + 4; i++) {
        output[i] = (byte) (value >> shiftOrder);
        shiftOrder += 8;
      }
    }
  }
}
