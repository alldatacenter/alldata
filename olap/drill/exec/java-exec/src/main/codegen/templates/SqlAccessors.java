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
import java.lang.Override;

<@pp.dropOutputFile />
<#list vv.types as type>
<#list type.minor as minor>
<#list ["", "Nullable"] as mode>
<#assign name = mode + minor.class?cap_first />
<#assign javaType = (minor.javaType!type.javaType) />
<#assign friendlyType = (minor.friendlyType!minor.boxedType!type.boxedType) />
<#-- Class returned by ResultSet.getObject(...): -->
<#assign jdbcObjectClass = minor.jdbcObjectClass ! friendlyType />

<@pp.changeOutputFile name="/org/apache/drill/exec/vector/accessor/${name}Accessor.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.accessor;

<#include "/@includes/vv_imports.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
public class ${name}Accessor extends AbstractSqlAccessor {
 <#if mode == "Nullable">
  private static final MajorType TYPE = Types.optional(MinorType.${minor.class?upper_case});
 <#else>
  private static final MajorType TYPE = Types.required(MinorType.${minor.class?upper_case});
 </#if>

  private final ${name}Vector.Accessor ac;

  public ${name}Accessor(${name}Vector vector) {
    this.ac = vector.getAccessor();
  }

  @Override
  public MajorType getType() {
    return TYPE;
  };

  @Override
  public boolean isNull(int index) {
   <#if mode == "Nullable">
    return ac.isNull(index);
   <#else>
    return false;
   </#if>
  }

 <#if minor.class != "VarChar" && minor.class != "TimeStamp"
   && minor.class != "Time" && minor.class != "Date">
  <#-- Types whose class for JDBC getObject(...) is same as class from getObject
       on vector. -->

  @Override
  public Class<?> getObjectClass() {
    return ${jdbcObjectClass}.class;
  }

  @Override
  public Object getObject(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    return ac.getObject(index);
  }
 </#if>

 <#if type.major == "VarLen">

  @Override
  public InputStream getStream(int index) {
    <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    ${name}Holder h = new ${name}Holder();
    ac.get(index, h);
    return new ByteBufInputStream(h.buffer.slice(h.start, h.end));
  }

  @Override
  public byte[] getBytes(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    return ac.get(index);
  }

  <#switch minor.class>

    <#case "VarBinary">

    @Override
    public String getString(int index) {
     <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
     </#if>
      byte [] b = ac.get(index);
      return DrillStringUtils.toBinaryString(b);
    }
      <#break>

    <#case "VarDecimal">

    @Override
    public String getString(int index) {
      <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
      </#if>
      BigDecimal bd = getBigDecimal(index);
      return bd.toString();
    }

    @Override
    public BigDecimal getBigDecimal(int index) {
    <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
    </#if>
      return ac.getObject(index);
    }
      <#break>

    <#case "VarChar">

    @Override
    public Class<?> getObjectClass() {
      return String.class;
    }

    @Override
    public String getObject(int index) {
     <#if mode == "Nullable">
       if (ac.isNull(index)) {
         return null;
       }
     </#if>
       return getString(index);
    }

    @Override
    public InputStreamReader getReader(int index) {
     <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
     </#if>
      return new InputStreamReader(getStream(index), Charsets.UTF_8);
    }

    @Override
    public String getString(int index) {
     <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
     </#if>
      return new String(getBytes(index), Charsets.UTF_8);
    }
      <#break>

    <#case "Var16Char">

    @Override
    public InputStreamReader getReader(int index) {
     <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
     </#if>
      return new InputStreamReader(getStream(index), Charsets.UTF_16);
    }

    @Override
    public String getString(int index) {
     <#if mode == "Nullable">
      if (ac.isNull(index)) {
        return null;
      }
     </#if>
      return new String(getBytes(index), Charsets.UTF_16);
    }
      <#break>

    <#default>
    This is uncompilable code

  </#switch>

 <#else> <#-- VarLen -->

  <#if minor.class == "TimeStampTZ">

  @Override
  public Class<?> getObjectClass() {
    return Timestamp.class;
  }

  @Override
  public Object getObject(int index) {
    return getTimestamp(index);
  }

  @Override
  public Timestamp getTimestamp(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    return new Timestamp(ac.getObject(index).getMillis());
  }

  <#elseif minor.class == "Interval" || minor.class == "IntervalDay" || minor.class == "IntervalYear">

  @Override
  public String getString(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    return String.valueOf(ac.getAsStringBuilder(index));
  }

  <#elseif minor.class.contains("Decimal")>

  @Override
  public BigDecimal getBigDecimal(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
      return ac.getObject(index);
  }
  <#elseif minor.class == "Date">

  @Override
  public Class<?> getObjectClass() {
    return Date.class;
  }

  @Override
  public Object getObject(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    return getDate(index);
  }

  @Override
  public Date getDate(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    org.joda.time.LocalDate date = new org.joda.time.LocalDate(ac.get(index), org.joda.time.DateTimeZone.UTC);
    // Use "toDate()" to get java.util.Date object with exactly the same year the same year, month and day as Joda date.
    // See more in Javadoc for "LocalDate#toDate()"
    return new Date(date.toDate().getTime());
  }

  <#elseif minor.class == "TimeStamp">

  @Override
  public Class<?> getObjectClass() {
    return Timestamp.class;
  }

  @Override
  public Object getObject(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    return getTimestamp(index);
  }

  @Override
  public Timestamp getTimestamp(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    org.joda.time.LocalDateTime dateTime = new org.joda.time.LocalDateTime(ac.get(index), org.joda.time.DateTimeZone.UTC);
    // use "toDate()" to get java.util.Date object with exactly the same fields as this Joda date-time.
    // See more in Javadoc for "LocalDateTime#toDate()"
    return new Timestamp(dateTime.toDate().getTime());
  }

  <#elseif minor.class == "Time">

  @Override
  public Class<?> getObjectClass() {
    return Time.class;
  }

  @Override
  public Object getObject(int index) {
    return getTime(index);
  }

  @Override
  public Time getTime(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return null;
    }
   </#if>
    org.joda.time.LocalTime time = new org.joda.time.LocalTime(ac.get(index), org.joda.time.DateTimeZone.UTC);
    // use "toDateTimeToday()"  and "getMillis()" to get the local milliseconds from the Java epoch of 1970-01-01T00:00:00
    return new TimePrintMillis(time.toDateTimeToday().getMillis());
  }

  <#else>

  @Override
  public ${javaType} get${javaType?cap_first}(int index) {
    return ac.get(index);
  }
  </#if>

  <#if minor.class == "Bit" >
  public boolean getBoolean(int index) {
   <#if mode == "Nullable">
    if (ac.isNull(index)) {
      return false;
    }
   </#if>
   return 1 == ac.get(index);
  }
 </#if>
 </#if> <#-- not VarLen -->
}
</#list>
</#list>
</#list>
