<#macro copyright>
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

// This class is generated using Freemarker and the ${.template_name} template.
</#macro>
<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/accessor/ColumnAccessors.java" />
<#macro getType drillType label>
    @Override
    public ValueType valueType() {
  <#if label == "Int">
      return ValueType.INTEGER;
  <#elseif drillType == "VarChar" || drillType == "Var16Char">
      return ValueType.STRING;
  <#elseif drillType == "VarDecimal">
      return ValueType.DECIMAL;
  <#else>
      return ValueType.${label?upper_case};
  </#if>
    }
  <#if drillType == "Date" || drillType == "Time" || drillType == "TimeStamp">

    @Override
    public ValueType extendedType() {
    <#if drillType == "Date">
      return ValueType.DATE;
    <#elseif drillType == "Time">
      return ValueType.TIME;
    <#elseif drillType == "TimeStamp">
      return ValueType.TIMESTAMP;
    <#else>
      <#-- Should not be necessary. -->
      return valueType();
    </#if>
    }
  </#if>
</#macro>
<#macro build types vectorType accessorType>
  <#if vectorType == "Repeated">
    <#assign fnPrefix = "Array" />
    <#assign classType = "Element" />
  <#else>
    <#assign fnPrefix = vectorType />
    <#assign classType = "Scalar" />
  </#if>
  <#if vectorType == "Required">
    <#assign vectorPrefix = "" />
  <#else>
    <#assign vectorPrefix = vectorType />
  </#if>
  public static void define${fnPrefix}${accessorType}s(
      Class<? extends Base${classType}${accessorType}> ${accessorType?lower_case}s[]) {
  <#list types as type>
  <#list type.minor as minor>
    <#assign drillType=minor.class>
    <#assign notyet=minor.accessorDisabled!type.accessorDisabled!false>
    <#if ! notyet>
    <#assign typeEnum=drillType?upper_case>
    ${accessorType?lower_case}s[MinorType.${typeEnum}.ordinal()] = ${vectorPrefix}${drillType}Column${accessorType}.class;
    </#if>
  </#list>
  </#list>
  }
</#macro>
<#macro writeBuf buffer drillType minor putType doCast >
  <#if varWidth>
      ${buffer}.setBytes(offset, value, 0, len);
  <#elseif drillType == "Decimal9">
      ${buffer}.setInt(offset,
        DecimalUtility.getDecimal9FromBigDecimal(value,
            type.getScale()));
  <#elseif drillType == "Decimal18">
      ${buffer}.setLong(offset,
          DecimalUtility.getDecimal18FromBigDecimal(value,
              type.getScale()));
  <#elseif drillType == "Decimal38Sparse">
      <#-- Hard to optimize this case. Just use the available tools. -->
      DecimalUtility.getSparseFromBigDecimal(value, ${buffer},
          offset, type.getScale(), 6);
  <#elseif drillType == "Decimal28Sparse">
      <#-- Hard to optimize this case. Just use the available tools. -->
      DecimalUtility.getSparseFromBigDecimal(value, ${buffer},
          offset, type.getScale(), 5);
  <#elseif drillType == "IntervalYear">
      ${buffer}.setInt(offset,
          value.getYears() * 12 + value.getMonths());
  <#elseif drillType == "IntervalDay">
      ${buffer}.setInt(offset, value.getDays());
      ${buffer}.setInt(offset + ${minor.millisecondsOffset}, DateUtilities.periodToMillis(value));
  <#elseif drillType == "Interval">
      ${buffer}.setInt(offset, DateUtilities.periodToMonths(value));
      ${buffer}.setInt(offset + ${minor.daysOffset}, value.getDays());
      ${buffer}.setInt(offset + ${minor.millisecondsOffset}, DateUtilities.periodToMillis(value));
  <#elseif drillType == "Float4">
      ${buffer}.setInt(offset, Float.floatToRawIntBits((float) value));
  <#elseif drillType == "Float8">
      ${buffer}.setLong(offset, Double.doubleToRawLongBits(value));
  <#elseif drillType == "Bit">
      ${buffer}.setByte(offset, (byte)(value & 0x01));
  <#else>
      ${buffer}.set${putType?cap_first}(offset, <#if doCast>(${putType}) </#if>value);
  </#if>
</#macro>
<@copyright />

package org.apache.drill.exec.vector.accessor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.vector.DateUtilities;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.*;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.accessor.reader.BaseScalarReader.BaseVarWidthReader;
import org.apache.drill.exec.vector.accessor.reader.BaseScalarReader.BaseFixedWidthReader;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessor;
import org.apache.drill.exec.vector.accessor.writer.AbstractFixedWidthWriter.BaseFixedWidthWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractFixedWidthWriter.BaseIntWriter;
import org.apache.drill.exec.vector.accessor.writer.BaseVarWidthWriter;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;

import io.netty.buffer.DrillBuf;

import org.joda.time.Period;

/**
 * Basic accessors for most Drill vector types and modes. Each class has a bare-bones
 * accessors that converts from the "native" Drill type to the vectors. Many classes
 * also have "convenience" methods that convert from other Java types.
 * <p>
 * Writers work only with single vectors. Readers work with either single
 * vectors or a "hyper vector": a collection of vectors indexed together.
 * The details are hidden behind the {@link RowIndex} interface. If the reader
 * accesses a single vector, then the mutator is cached at bind time. However,
 * if the reader works with a hyper vector, then the vector is null at bind
 * time and must be retrieved for each row (since the vector differs row-by-
 * row.)
 */

public class ColumnAccessors {
  public static final LocalDateTime LOCAL_EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

<#list vv.types as type>
  <#list type.minor as minor>
    <#assign drillType=minor.class>
    <#if drillType == "Bit">
      <#-- Bit is special, handled outside of codegen. -->
      <#continue>
    </#if>
    <#assign javaType=minor.javaType!type.javaType>
    <#assign accessorType=minor.accessorType!type.accessorType!minor.friendlyType!javaType>
    <#assign label=minor.accessorLabel!type.accessorLabel!accessorType?capitalize>
    <#assign notyet=minor.accessorDisabled!type.accessorDisabled!false>
    <#if notyet>
      <#continue>
    </#if>
   <#assign cast=minor.accessorCast!minor.accessorCast!type.accessorCast!"none">
    <#assign friendlyType=minor.friendlyType!"">
    <#if accessorType=="BigDecimal">
      <#assign label="Decimal">
    </#if>
    <#assign varWidth = drillType == "VarChar" || drillType == "Var16Char" ||
                        drillType == "VarBinary" || drillType == "VarDecimal" />
    <#assign decimal = drillType == "Decimal9" || drillType == "Decimal18" ||
                       drillType == "Decimal28Sparse" || drillType == "Decimal38Sparse" ||
                       drillType == "VarDecimal" />
    <#assign intType = drillType == "TinyInt" || drillType == "SmallInt" || drillType == "Int" ||
                       drillType == "Bit" || drillType == "UInt1" || drillType = "UInt2" />
    <#if varWidth>
      <#assign accessorType = "byte[]">
      <#assign label = "Bytes">
      <#assign putArgs = ", final int len">
    <#else>
      <#assign putArgs = "">
    </#if>
    <#if javaType == "char">
      <#assign putType = "short" />
      <#assign doCast = true />
    <#else>
      <#assign putType = javaType />
      <#assign doCast = (cast == "set") />
    </#if>
  //------------------------------------------------------------------------
  // ${drillType} readers and writers

    <#if varWidth>
  public static class ${drillType}ColumnReader extends BaseVarWidthReader {

    <#else>
  public static class ${drillType}ColumnReader extends BaseFixedWidthReader {

    private static final int VALUE_WIDTH = ${drillType}Vector.VALUE_WIDTH;

    </#if>
    <#if decimal>
    private MajorType type;

    @Override
    public void bindVector(ColumnMetadata schema, VectorAccessor va) {
      super.bindVector(schema, va);
      <#if decimal>
      type = va.type();
      </#if>
    }

    </#if>
    <@getType drillType label />
    <#if ! varWidth>

    @Override public int width() { return VALUE_WIDTH; }
    </#if>

    @Override
    public ${accessorType} get${label}() {
    <#assign getObject ="getObject"/>
    <#assign indexVar = ""/>
      final DrillBuf buf = bufferAccessor.buffer();
    <#if ! varWidth>
      final int readOffset = vectorIndex.offset();
      <#assign getOffset = "readOffset * VALUE_WIDTH">
    </#if>
    <#if varWidth>
      final long entry = offsetsReader.getEntry();
      return buf.unsafeGetMemory((int) (entry >> 32), (int) (entry & 0xFFFF_FFFF));
    <#elseif drillType == "Decimal9">
      return DecimalUtility.getBigDecimalFromPrimitiveTypes(
          buf.getInt(${getOffset}),
          type.getScale());
    <#elseif drillType == "Decimal18">
      return DecimalUtility.getBigDecimalFromPrimitiveTypes(
          buf.getLong(${getOffset}),
          type.getScale());
    <#elseif drillType == "IntervalYear">
      <#-- For Java 8:
      final int value = buf.getInt(${getOffset});
      final int years  = (value / DateUtilities.yearsToMonths);
      final int months = (value % DateUtilities.yearsToMonths);
      return Period.of(years, months, 0); -->
      return DateUtilities.fromIntervalYear(
          buf.getInt(${getOffset}));
    <#elseif drillType == "IntervalDay">
      final int offset = ${getOffset};
      <#-- Show stopper for Java 8 date/time: There is no class
           that is equivalent to a Joda Period. -->
      return DateUtilities.fromIntervalDay(
          buf.getInt(offset),
          buf.getInt(offset + ${minor.millisecondsOffset}));
    <#elseif drillType == "Interval">
      final int offset = ${getOffset};
      return DateUtilities.fromInterval(
          buf.getInt(offset),
          buf.getInt(offset + ${minor.daysOffset}),
          buf.getInt(offset + ${minor.millisecondsOffset}));
    <#elseif drillType == "Decimal28Sparse" || drillType == "Decimal38Sparse">
      return DecimalUtility.getBigDecimalFromSparse(buf, ${getOffset},
          ${minor.nDecimalDigits}, type.getScale());
    <#elseif drillType == "Decimal28Dense" || drillType == "Decimal38Dense">
      return DecimalUtility.getBigDecimalFromDense(buf, ${getOffset},
          ${minor.nDecimalDigits}, type.getScale(),
          ${minor.maxPrecisionDigits}, VALUE_WIDTH);
    <#elseif drillType == "UInt1">
      return buf.getByte(${getOffset}) & 0xFF;
    <#elseif drillType == "UInt2">
      return buf.getShort(${getOffset}) & 0xFFFF;
    <#elseif drillType == "UInt4">
      // Should be the following:
      // return ((long) buf.unsafeGetInt(${getOffset})) & 0xFFFF_FFFF;
      // else, the unsigned values of 32 bits are mapped to negative.
      return buf.getInt(${getOffset});
    <#elseif drillType == "Float4">
      return Float.intBitsToFloat(buf.getInt(${getOffset}));
    <#elseif drillType == "Float8">
      return Double.longBitsToDouble(buf.getLong(${getOffset}));
    <#elseif drillType == "Bit">
      return buf.getByte(${getOffset});
    <#else>
      return buf.get${putType?cap_first}(${getOffset});
    </#if>
    }
    <#if drillType == "VarChar">

    @Override
    public String getString() {
      return new String(getBytes(${indexVar}), Charsets.UTF_8);
    }
    <#elseif drillType == "Var16Char">

    @Override
    public String getString() {
      return new String(getBytes(${indexVar}), Charsets.UTF_16);
    }
    <#elseif drillType == "VarDecimal">

    @Override
    public BigDecimal getDecimal() {
      final byte[] bytes = getBytes();
      BigInteger unscaledValue = bytes.length == 0 ? BigInteger.ZERO : new BigInteger(bytes);
      return new BigDecimal(unscaledValue, type.getScale());
    }
    <#elseif drillType == "Float4">

    @Override
    public double getDouble() {
      return getFloat();
    }
    <#elseif drillType == "Date">

    @Override
    public final LocalDate getDate() {
      return DateUtilities.fromDrillDate(getLong());
    }
    <#elseif drillType == "Time">

    @Override
    public final LocalTime getTime() {
      return DateUtilities.fromDrillTime(getInt());
    }
    <#elseif drillType == "TimeStamp">

    @Override
    public final Instant getTimestamp() {
      return DateUtilities.fromDrillTimestamp(getLong());
    }
    </#if>
  }

    <#if varWidth>
  public static class ${drillType}ColumnWriter extends BaseVarWidthWriter {
    <#else>
      <#if intType>
  public static class ${drillType}ColumnWriter extends BaseIntWriter {
      <#else>
  public static class ${drillType}ColumnWriter extends BaseFixedWidthWriter {
      </#if>

    private static final int VALUE_WIDTH = ${drillType}Vector.VALUE_WIDTH;
    </#if>

    private final ${drillType}Vector vector;
    <#if drillType == "VarDecimal">
    private int precision;
    private int scale;
    <#elseif decimal>
    private MajorType type;
    </#if>

    public ${drillType}ColumnWriter(final ValueVector vector) {
    <#if varWidth>
      super(((${drillType}Vector) vector).getOffsetVector());
    </#if>
    <#if drillType == "VarDecimal">
      // VarDecimal requires a scale. If not set, assume 0
      MajorType type = vector.getField().getType();
      precision = type.hasPrecision() ? type.getPrecision() : Types.maxPrecision(type.getMinorType());
      scale = type.hasScale() ? type.getScale() : 0;
    <#elseif decimal>
      type = vector.getField().getType();
    </#if>
      this.vector = (${drillType}Vector) vector;
    }

    @Override public BaseDataValueVector vector() { return vector; }

     <#if ! varWidth>
    @Override public int width() { return VALUE_WIDTH; }

    </#if>
      <@getType drillType label />

    @Override
    public final void set${label}(final ${accessorType} value${putArgs}) {
    <#-- Must compute the write offset first; can't be inline because the
         writeOffset() function has a side effect of possibly changing the buffer
         address (bufAddr). -->
    <#if varWidth>
      final int offset = prepareWrite(len);
    <#else>
      final int offset = prepareWrite() * VALUE_WIDTH;
    </#if>
      <@writeBuf "drillBuf", drillType minor putType doCast />
    <#if varWidth>
      offsetsWriter.setNextOffset(offset + len);
    </#if>
      vectorIndex.nextElement();
    }
    <#if ! varWidth>

    public final void write${label}(final DrillBuf buf, final ${accessorType} value) {
      final int offset = 0;
      <@writeBuf "buf", drillType minor putType doCast />
      buf.writerIndex(VALUE_WIDTH);
    }
    </#if>
    <#if drillType == "VarChar" || drillType == "Var16Char" || drillType == "VarBinary">

    @Override
    public final void appendBytes(final byte[] value, final int len) {
      vectorIndex.prevElement();
      final int offset = prepareAppend(len);
      drillBuf.setBytes(offset, value, 0, len);
      offsetsWriter.reviseOffset(offset + len);
      vectorIndex.nextElement();
    }
    </#if>
    <#if drillType == "VarChar">

    @Override
    public final void setString(final String value) {
      final byte bytes[] = value.getBytes(Charsets.UTF_8);
      setBytes(bytes, bytes.length);
    }
    <#elseif drillType == "Var16Char">

    @Override
    public final void setString(final String value) {
      final byte bytes[] = value.getBytes(Charsets.UTF_16);
      setBytes(bytes, bytes.length);
    }
    <#elseif drillType == "BigInt">

    @Override
    public final void setInt(final int value) {
      setLong(value);
    }

    @Override
    public final void setFloat(final float value) {
      // Does not catch overflow from
      // double. See Math.round for details.
      setLong(Math.round(value));
    }

    @Override
    public final void setDouble(final double value) {
      // Does not catch overflow from
      // double. See Math.round for details.
      setLong(Math.round(value));
    }

    @Override
    public final void setDecimal(final BigDecimal value) {
      try {
        // Catches long overflow.
        setLong(value.longValueExact());
      } catch (ArithmeticException e) {
        throw InvalidConversionError.writeError(schema(), value, e);
      }
    }
    <#elseif drillType == "Float8">

    @Override
    public final void setInt(final int value) {
      setDouble(value);
    }

    @Override
    public final void setLong(final long value) {
      setDouble(value);
    }

    @Override
    public final void setFloat(final float value) {
      setDouble(value);
    }

    @Override
    public final void setDecimal(final BigDecimal value) {
      setDouble(value.doubleValue());
    }
    <#elseif drillType == "Float4">

    @Override
    public final void setInt(final int value) {
      setFloat(value);
    }

    @Override
    public final void setLong(final long value) {
      setFloat(value);
    }

    @Override
    public final void setDouble(final double value) {
      setFloat((float) value);
    }

    @Override
    public final void setDecimal(final BigDecimal value) {
      setFloat(value.floatValue());
    }
    <#elseif decimal>

    @Override
    public final void setInt(final int value) {
      setDecimal(BigDecimal.valueOf(value));
    }

    @Override
    public final void setLong(final long value) {
      setDecimal(BigDecimal.valueOf(value));
    }

    @Override
    public final void setDouble(final double value) {
      setDecimal(BigDecimal.valueOf(value));
    }

    @Override
    public final void setFloat(final float value) {
      setDecimal(BigDecimal.valueOf(value));
    }
      <#if drillType == "VarDecimal">

    @Override
    public final void setDecimal(final BigDecimal value) {
      try {
        final BigDecimal rounded = value.setScale(scale, RoundingMode.HALF_UP);
        DecimalUtility.checkValueOverflow(rounded, precision, scale);
        final byte[] barr = rounded.unscaledValue().toByteArray();
        setBytes(barr,  barr.length);
      } catch (ArithmeticException e) {
        throw new InvalidConversionError("Decimal conversion failed for " + value, e);
      }
    }
      </#if>
    <#elseif drillType == "Date">

    @Override
    public final void setDate(final LocalDate value) {
      setLong(DateUtilities.toDrillDate(value));
    }
    <#elseif drillType == "Time">

    @Override
    public final void setTime(final LocalTime value) {
      setInt(DateUtilities.toDrillTime(value));
    }
    <#elseif drillType == "TimeStamp">

    @Override
    public final void setTimestamp(final Instant value) {
      setLong(DateUtilities.toDrillTimestamp(value));
    }
    </#if>
    <#if ! intType>

    @Override
    public final void setValue(final Object value) {
      <#if drillType == "VarChar">
      setString((String) value);
      <#elseif drillType = "Date">
      setDate((LocalDate) value);
      <#elseif drillType = "Time">
      setTime((LocalTime) value);
      <#elseif drillType = "TimeStamp">
      setTimestamp((Instant) value);
      <#elseif putArgs != "">
      throw new InvalidConversionError("Generic object not supported for type ${drillType}, "
          + "set${label}(${accessorType}${putArgs})");
      <#else>
      if (value != null) {
        set${label}((${accessorType}) value);
      }
      </#if>
    }
    </#if>

    <#-- Default value logic is a bit convoluted because we want to reuse the same
         (complex) template here to generate both the set-value code and the set-default
         code. This means we need a (temporary) DrillBuf for most cases except those
         where a byte array value is directly available. -->
    @Override
    public final void setDefaultValue(final Object value) {
    <#if drillType == "VarBinary">
      emptyValue = (byte[]) value;
    <#elseif drillType == "VarChar">
      emptyValue = ((String) value).getBytes(Charsets.UTF_8);
    <#elseif drillType == "Var16Char">
      emptyValue = ((String) value).getBytes(Charsets.UTF_16);
    <#elseif drillType == "VarDecimal">
      final BigDecimal rounded = ((BigDecimal) value).setScale(scale, RoundingMode.HALF_UP);
      DecimalUtility.checkValueOverflow(rounded, precision, scale);
      emptyValue = rounded.unscaledValue().toByteArray();
    <#else>
      try (DrillBuf buf = vector.getAllocator().buffer(VALUE_WIDTH)) {
      <#if drillType = "Date">
        writeLong(buf, DateUtilities.toDrillDate((LocalDate) value));
      <#elseif drillType = "Time">
        writeInt(buf, DateUtilities.toDrillTime((LocalTime) value));
      <#elseif drillType = "TimeStamp">
        writeLong(buf, DateUtilities.toDrillTimestamp((Instant) value));
      <#elseif putArgs != "">
        throw new InvalidConversionError("Generic object not supported for type ${drillType}, "
            + "set${label}(${accessorType}${putArgs})");
      <#else>
        write${label}(buf, (${accessorType}) value);
      </#if>
        emptyValue = new byte[VALUE_WIDTH];
        buf.getBytes(0, emptyValue);
      }
    </#if>
    }

    @Override
    public final void copy(ColumnReader from) {
      ${drillType}ColumnReader source = (${drillType}ColumnReader) from;
      final DrillBuf sourceBuf = source.buffer();
    <#if varWidth>
      final long entry = source.getEntry();
      final int sourceOffset = (int) (entry >> 32);
      final int len = (int) (entry & 0xFFFF_FFFF);
      final int destOffset = prepareWrite(len);
      drillBuf.setBytes(destOffset, sourceBuf, sourceOffset, len);
      offsetsWriter.setNextOffset(destOffset + len);
    <#else>
      final int sourceOffset = source.offsetIndex() * VALUE_WIDTH;
      final int destOffset = prepareWrite() * VALUE_WIDTH;
      drillBuf.setBytes(destOffset, sourceBuf, sourceOffset, VALUE_WIDTH);
    </#if>
      vectorIndex.nextElement();
    }
  }

  </#list>
</#list>
}
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/accessor/ColumnAccessorUtils.java" />
<@copyright />

package org.apache.drill.exec.vector.accessor;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.*;
import org.apache.drill.exec.vector.accessor.reader.BaseScalarReader;
import org.apache.drill.exec.vector.accessor.reader.BitColumnReader;
import org.apache.drill.exec.vector.accessor.writer.BaseScalarWriter;
import org.apache.drill.exec.vector.accessor.writer.BitColumnWriter;

public class ColumnAccessorUtils {

  private ColumnAccessorUtils() { }

<@build vv.types "Required" "Reader" />

<@build vv.types "Required" "Writer" />
}
