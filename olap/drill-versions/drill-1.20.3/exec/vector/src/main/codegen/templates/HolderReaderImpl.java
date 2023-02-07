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
<@pp.dropOutputFile />
<#list vv.types as type>
<#list type.minor as minor>
<#list ["", "Nullable", "Repeated"] as holderMode>
<#assign nullMode = holderMode />
<#if holderMode == "Repeated"><#assign nullMode = "Nullable" /></#if>

<#assign lowerName = minor.class?uncap_first />
<#if lowerName == "int" ><#assign lowerName = "integer" /></#if>
<#assign name = minor.class?cap_first />
<#assign javaType = (minor.javaType!type.javaType) />
<#assign friendlyType = (minor.friendlyType!minor.boxedType!type.boxedType) />
<#assign safeType=friendlyType />
<#if safeType=="byte[]"><#assign safeType="ByteArray" /></#if>
<#assign fields = minor.fields!type.fields />

<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/impl/${holderMode}${name}HolderReaderImpl.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.joda.time.Period;

// Source code generated using FreeMarker template ${.template_name}

@SuppressWarnings("unused")
public class ${holderMode}${name}HolderReaderImpl extends AbstractFieldReader {

  private ${nullMode}${name}Holder holder;
<#if holderMode == "Repeated" >
  private int index = -1;
  private ${holderMode}${name}Holder repeatedHolder;
</#if>

  public ${holderMode}${name}HolderReaderImpl(${holderMode}${name}Holder holder) {
<#if holderMode == "Repeated" >
    this.holder = new ${nullMode}${name}Holder();
    this.repeatedHolder = holder;
<#else>
    this.holder = holder;
</#if>
  }

  @Override
  public int size() {
<#if holderMode == "Repeated">
    return repeatedHolder.end - repeatedHolder.start;
<#else>
    throw new UnsupportedOperationException("You can't call size on a Holder value reader.");
</#if>
  }

  @Override
  public boolean next() {
<#if holderMode == "Repeated">
    if(index + 1 < repeatedHolder.end) {
      index++;
      repeatedHolder.vector.getAccessor().get(repeatedHolder.start + index, holder);
      return true;
    } else {
      return false;
    }
<#else>
    throw new UnsupportedOperationException("You can't call next on a single value reader.");
</#if>
  }

  @Override
  public void setPosition(int index) {
    throw new UnsupportedOperationException("You can't call next on a single value reader.");
  }

  @Override
  public MajorType getType() {
<#if name?contains("Decimal")>
    return BasicTypeHelper.getType(holder);
<#else>
  <#if holderMode == "Repeated">
    return repeatedHolder.TYPE;
  <#else>
    return holder.TYPE;
  </#if>
</#if>
  }

  @Override
  public boolean isSet() {
    <#if holderMode == "Repeated">
    return repeatedHolder.end!=this.repeatedHolder.start;
    <#elseif nullMode == "Nullable">
    return holder.isSet == 1;
    <#else>
    return true;
    </#if>
  }

<#if holderMode != "Repeated">
@Override
  public void read(${name}Holder h) {
  <#list fields as field>
    h.${field.name} = holder.${field.name};
  </#list>
  }

  @Override
  public void read(Nullable${name}Holder h) {
  <#list fields as field>
    h.${field.name} = holder.${field.name};
  </#list>
    h.isSet = isSet() ? 1 : 0;
  }

</#if>
<#if holderMode == "Repeated">
  @Override
  public ${friendlyType} read${safeType}(int index){
    repeatedHolder.vector.getAccessor().get(repeatedHolder.start + index, holder);
    ${friendlyType} value = read${safeType}();
    if (this.index > -1) {
      repeatedHolder.vector.getAccessor().get(repeatedHolder.start + this.index, holder);
    }
    return value;
  }

</#if>
  @Override
  public ${friendlyType} read${safeType}(){
<#if nullMode == "Nullable">
    if (!isSet()) {
      return null;
    }

</#if>
<#if type.major == "VarLen">
      int length = holder.end - holder.start;
      byte[] value = new byte[length];
      holder.buffer.getBytes(holder.start, value, 0, length);

<#if minor.class == "VarBinary">
      return value;
<#elseif minor.class == "VarDecimal">
      return org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromDrillBuf(holder.buffer, holder.start, holder.end - holder.start, holder.scale);
<#elseif minor.class == "Var16Char">
      return new String(value);
<#elseif minor.class == "VarChar">
      Text text = new Text();
      text.set(value);
      return text;
</#if>

<#elseif minor.class == "Interval">
      Period p = new Period();
      return p.plusMonths(holder.months).plusDays(holder.days).plusMillis(holder.milliseconds);

<#elseif minor.class == "IntervalDay">
      Period p = new Period();
      return p.plusDays(holder.days).plusMillis(holder.milliseconds);

<#elseif minor.class == "Decimal9" ||
         minor.class == "Decimal18" >
      BigInteger value = BigInteger.valueOf(holder.value);
      return new BigDecimal(value, holder.scale);

<#elseif minor.class == "Decimal28Dense" ||
         minor.class == "Decimal38Dense">
      return org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromDense(holder.buffer,
                                                                                holder.start,
                                                                                holder.nDecimalDigits,
                                                                                holder.scale,
                                                                                holder.maxPrecision,
                                                                                holder.WIDTH);

<#elseif minor.class == "Decimal28Sparse" ||
         minor.class == "Decimal38Sparse">
      return org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromSparse(holder.buffer,
                                                                                 holder.start,
                                                                                 holder.nDecimalDigits,
                                                                                 holder.scale);

<#elseif minor.class == "Bit" >
      return Boolean.valueOf(holder.value != 0);
<#elseif minor.class == "Time">
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.holder.value), ZoneOffset.UTC).toLocalTime();
<#elseif minor.class == "Date">
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.holder.value), ZoneOffset.UTC).toLocalDate();
<#elseif minor.class == "TimeStamp">
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.holder.value), ZoneOffset.UTC);
<#else>
      ${friendlyType} value = new ${friendlyType}(this.holder.value);
      return value;
</#if>
  }

  @Override
  public Object readObject() {
<#if holderMode == "Repeated" >
    List<Object> valList = Lists.newArrayList();
    for (int i = repeatedHolder.start; i < repeatedHolder.end; i++) {
      valList.add(repeatedHolder.vector.getAccessor().getObject(i));
    }
    return valList;
<#else>
    return readSingleObject();
</#if>
  }

  private Object readSingleObject() {
<#if nullMode == "Nullable">
    if (!isSet()) {
      return null;
    }
</#if>

<#if type.major == "VarLen">
      int length = holder.end - holder.start;
      byte[] value = new byte[length];
      holder.buffer.getBytes(holder.start, value, 0, length);

<#if minor.class == "VarBinary">
      return value;
<#elseif minor.class == "VarDecimal">
      return org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromDrillBuf(holder.buffer, holder.start, holder.end - holder.start, holder.scale);
<#elseif minor.class == "Var16Char">
      return new String(value);
<#elseif minor.class == "VarChar">
      Text text = new Text();
      text.set(value);
      return text;
</#if>

<#elseif minor.class == "Interval">
      Period p = new Period();
      return p.plusMonths(holder.months).plusDays(holder.days).plusMillis(holder.milliseconds);

<#elseif minor.class == "IntervalDay">
      Period p = new Period();
      return p.plusDays(holder.days).plusMillis(holder.milliseconds);

<#elseif minor.class == "Decimal9" ||
         minor.class == "Decimal18" >
      BigInteger value = BigInteger.valueOf(holder.value);
      return new BigDecimal(value, holder.scale);

<#elseif minor.class == "Decimal28Dense" ||
         minor.class == "Decimal38Dense">
      return org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromDense(holder.buffer,
                                                                                holder.start,
                                                                                holder.nDecimalDigits,
                                                                                holder.scale,
                                                                                holder.maxPrecision,
                                                                                holder.WIDTH);

<#elseif minor.class == "Decimal28Sparse" ||
         minor.class == "Decimal38Sparse">
      return org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromSparse(holder.buffer,
                                                                                 holder.start,
                                                                                 holder.nDecimalDigits,
                                                                                 holder.scale);

<#elseif minor.class == "Bit" >
      return Boolean.valueOf(holder.value != 0);
<#elseif minor.class == "Time">
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.holder.value), ZoneOffset.UTC).toLocalTime();
<#elseif minor.class == "Date">
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.holder.value), ZoneOffset.UTC).toLocalDate();
<#elseif minor.class == "TimeStamp">
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.holder.value), ZoneOffset.UTC);
<#else>
      ${friendlyType} value = new ${friendlyType}(this.holder.value);
      return value;
</#if>
  }

<#if holderMode == "Repeated">
  public void copyAsValue(${minor.class?cap_first}Writer writer) {
    Repeated${minor.class?cap_first}WriterImpl impl = (Repeated${minor.class?cap_first}WriterImpl) writer;
    impl.vector.getMutator().setSafe(impl.idx(), repeatedHolder);
  }

<#if minor.class == "VarDecimal">
  public void copyAsField(String name, MapWriter writer, int precision, int scale) {
    Repeated${minor.class?cap_first}WriterImpl impl
        = (Repeated${minor.class?cap_first}WriterImpl) writer.list(name).${lowerName}(precision, scale);
<#else>
public void copyAsField(String name, MapWriter writer) {
    Repeated${minor.class?cap_first}WriterImpl impl = (Repeated${minor.class?cap_first}WriterImpl) writer.list(name).${lowerName}();
</#if>
    impl.vector.getMutator().setSafe(impl.idx(), repeatedHolder);
  }
<#else>
  <#if !(minor.class == "Decimal9" || minor.class == "Decimal18")>
  public void copyAsValue(${minor.class?cap_first}Writer writer) {
    if (isSet()) {
      writer.write${minor.class}(<#list fields as field>holder.${field.name}<#if field_has_next>, </#if></#list>);
    }
  }

    <#if minor.class == "VarDecimal">
  public void copyAsField(String name, MapWriter writer, int precision, int scale) {
    ${minor.class?cap_first}Writer impl = writer.${lowerName}(name, precision, scale);
    <#else>
  public void copyAsField(String name, MapWriter writer) {
    ${minor.class?cap_first}Writer impl = writer.${lowerName}(name);
    </#if>
    if (isSet()) {
      impl.write${minor.class}(<#list fields as field>holder.${field.name}<#if field_has_next>,</#if></#list>);
    }
  }
  </#if>
</#if>
}
</#list>
</#list>
</#list>
