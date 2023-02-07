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

<#assign entries = drillDataType.map + drillOI.map />
<#list entries as entry>
<#if entry.needOIForDrillType == true>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/hive/Drill${entry.drillType}${entry.hiveOI}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.hive;

import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers;
import org.apache.drill.exec.expr.holders.*;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.HiveVarchar;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.io.ByteWritable;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.hive.serde2.io.HiveDecimalWritable;
import org.apache.hadoop.hive.serde2.io.HiveVarcharWritable;
import org.apache.hadoop.hive.serde2.io.ShortWritable;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.*;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoFactory;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class Drill${entry.drillType}${entry.hiveOI} {
<#assign seq = ["Required", "Optional"]>
<#list seq as mode>

  public static class ${mode} extends AbstractDrillPrimitiveObjectInspector implements ${entry.hiveOI} {
    public ${mode}() {
      super(TypeInfoFactory.${entry.hiveType?lower_case}TypeInfo);
    }

<#if entry.drillType == "VarChar" && entry.hiveType == "VARCHAR">
    @Override
    public HiveVarcharWritable getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarCharHolder h = (NullableVarCharHolder)o;
    <#else>
      final VarCharHolder h = (VarCharHolder)o;
    </#if>
      final HiveVarcharWritable valW = new HiveVarcharWritable();
      valW.set(StringFunctionHelpers.toStringFromUTF8(h.start, h.end, h.buffer), HiveVarchar.MAX_VARCHAR_LENGTH);
      return valW;
    }

    @Override
    public HiveVarchar getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarCharHolder h = (NullableVarCharHolder)o;
    <#else>
      final VarCharHolder h = (VarCharHolder)o;
    </#if>
      final String s = StringFunctionHelpers.toStringFromUTF8(h.start, h.end, h.buffer);
      return new HiveVarchar(s, HiveVarchar.MAX_VARCHAR_LENGTH);
    }
<#elseif entry.drillType == "VarChar" && entry.hiveType == "STRING">
    @Override
    public Text getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarCharHolder h = (NullableVarCharHolder)o;
    <#else>
      final VarCharHolder h = (VarCharHolder)o;
    </#if>
      return new Text(StringFunctionHelpers.toStringFromUTF8(h.start, h.end, h.buffer));
    }

    @Override
    public String getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarCharHolder h = (NullableVarCharHolder) o;
    <#else>
      final VarCharHolder h = (VarCharHolder)o;
    </#if>
      return StringFunctionHelpers.toStringFromUTF8(h.start, h.end, h.buffer);
    }
<#elseif entry.drillType == "VarBinary">
    @Override
    public BytesWritable getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarBinaryHolder h = (NullableVarBinaryHolder) o;
    <#else>
      final VarBinaryHolder h = (VarBinaryHolder) o;
    </#if>
      final byte[] buf = new byte[h.end-h.start];
      h.buffer.getBytes(h.start, buf, 0, h.end-h.start);
      return new BytesWritable(buf);
    }

    @Override
    public byte[] getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarBinaryHolder h = (NullableVarBinaryHolder) o;
    <#else>
      final VarBinaryHolder h = (VarBinaryHolder) o;
    </#if>
      final byte[] buf = new byte[h.end-h.start];
      h.buffer.getBytes(h.start, buf, 0, h.end-h.start);
      return buf;
    }
<#elseif entry.drillType == "Bit">
    @Override
    public boolean get(Object o) {
    <#if mode == "Optional">
      return ((NullableBitHolder)o).value == 0 ? false : true;
    <#else>
      return ((BitHolder)o).value == 0 ? false : true;
    </#if>
    }

    @Override
    public BooleanWritable getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      return new BooleanWritable(((NullableBitHolder)o).value == 0 ? false : true);
    <#else>
      return new BooleanWritable(((BitHolder)o).value == 0 ? false : true);
    </#if>
    }

    @Override
    public Boolean getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      return Boolean.valueOf(((NullableBitHolder)o).value != 0);
    <#else>
      return Boolean.valueOf(((BitHolder)o).value != 0);
    </#if>
    }
<#elseif entry.drillType == "VarDecimal">
    @Override
    public HiveDecimal getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarDecimalHolder h = (NullableVarDecimalHolder) o;
    <#else>
      final VarDecimalHolder h = (VarDecimalHolder) o;
    </#if>
      return HiveDecimal.create(DecimalUtility.getBigDecimalFromDrillBuf(h.buffer, h.start, h.end - h.start, h.scale));
    }

    @Override
    public HiveDecimalWritable getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableVarDecimalHolder h = (NullableVarDecimalHolder) o;
    <#else>
      final VarDecimalHolder h = (VarDecimalHolder) o;
    </#if>
      return new HiveDecimalWritable(
          HiveDecimal.create(DecimalUtility.getBigDecimalFromDrillBuf(h.buffer, h.start, h.end - h.start, h.scale)));
    }

<#elseif entry.drillType == "TimeStamp">
    @Override
    public ${entry.javaType} getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableTimeStampHolder h = (NullableTimeStampHolder) o;
    <#else>
      final TimeStampHolder h = (TimeStampHolder) o;
    </#if>
    <#if entry.javaType == "org.apache.hadoop.hive.common.type.Timestamp">
      return ${entry.javaType}.ofEpochMilli(h.value);
    <#else>
      org.joda.time.LocalDateTime dateTime = new org.joda.time.LocalDateTime(h.value, org.joda.time.DateTimeZone.UTC);
      // use "toDate()" to get java.util.Date object with exactly the same fields as this Joda date-time.
      // See more in Javadoc for "LocalDateTime#toDate()"
      return new ${entry.javaType}(dateTime.toDate().getTime());
    </#if>
    }

    @Override
    public ${entry.writableType} getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableTimeStampHolder h = (NullableTimeStampHolder) o;
    <#else>
      final TimeStampHolder h = (TimeStampHolder) o;
    </#if>
    <#if entry.javaType == "org.apache.hadoop.hive.common.type.Timestamp">
      return new ${entry.writableType}(${entry.javaType}.ofEpochMilli(h.value));
    <#else>
      org.joda.time.LocalDateTime dateTime = new org.joda.time.LocalDateTime(h.value, org.joda.time.DateTimeZone.UTC);
      // use "toDate()" to get java.util.Date object with exactly the same fields as this Joda date-time.
      // See more in Javadoc for "LocalDateTime#toDate()"
      return new ${entry.writableType}(new ${entry.javaType}(dateTime.toDate().getTime()));
    </#if>
    }

<#elseif entry.drillType == "Date">
    @Override
    public ${entry.javaType} getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableDateHolder h = (NullableDateHolder) o;
    <#else>
      final DateHolder h = (DateHolder) o;
    </#if>
    <#if entry.javaType == "org.apache.hadoop.hive.common.type.Date">
      return org.apache.hadoop.hive.common.type.Date.ofEpochMilli(h.value);
    <#else>
      org.joda.time.LocalDate localDate = new org.joda.time.LocalDate(h.value, org.joda.time.DateTimeZone.UTC);
      // Use "toDate()" to get java.util.Date object with exactly the same year the same year, month and day as Joda date.
      // See more in Javadoc for "LocalDate#toDate()"
      return new ${entry.javaType}(localDate.toDate().getTime());
    </#if>
    }

    @Override
    public ${entry.writableType} getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final NullableDateHolder h = (NullableDateHolder) o;
    <#else>
      final DateHolder h = (DateHolder) o;
    </#if>
    <#if entry.javaType == "org.apache.hadoop.hive.common.type.Date">
      return new ${entry.writableType}(org.apache.hadoop.hive.common.type.Date.ofEpochMilli(h.value));
    <#else>
      org.joda.time.LocalDate localDate = new org.joda.time.LocalDate(h.value, org.joda.time.DateTimeZone.UTC);
      // Use "toDate()" to get java.util.Date object with exactly the same year the same year, month and day as Joda date.
      // See more in Javadoc for "LocalDate#toDate()"
      return new ${entry.writableType}(new ${entry.javaType}(localDate.toDate().getTime()));
    </#if>
    }

<#else>
    @Override
    public ${entry.javaType} get(Object o) {
    <#if mode == "Optional">
      return ((Nullable${entry.drillType}Holder) o).value;
    <#else>
      return ((${entry.drillType}Holder) o).value;
    </#if>
    }

<#if entry.drillType == "Int">
    @Override
    public Integer getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
    </#if>
      return Integer.valueOf(get(o));
    }
<#else>
    @Override
    public ${entry.javaType?cap_first} getPrimitiveJavaObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      return new ${entry.javaType?cap_first}(((Nullable${entry.drillType}Holder) o).value);
    <#else>
      return new ${entry.javaType?cap_first}(((${entry.drillType}Holder) o).value);
    </#if>
    }
</#if>

    @Override
    public ${entry.javaType?cap_first}Writable getPrimitiveWritableObject(Object o) {
    <#if mode == "Optional">
      if (o == null) {
        return null;
      }
      final Nullable${entry.drillType}Holder h = (Nullable${entry.drillType}Holder) o;
    <#else>
      final ${entry.drillType}Holder h = (${entry.drillType}Holder) o;
    </#if>
      return new ${entry.javaType?cap_first}Writable(h.value);
    }
</#if>
  }
</#list>
}
</#if>
</#list>

