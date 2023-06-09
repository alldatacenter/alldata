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

<#list cast.types as type>
<#if type.major == "VarCharDate" || type.major == "VarBinaryDate" || type.major == "NullableVarCharDate">  <#-- Template to convert from VarChar/ VarBinary to Date, Time, TimeStamp -->

<#if type.major == "VarCharDate" || type.major == "VarBinaryDate">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}To${type.to}.java" />
<#elseif type.major == "NullableVarCharDate">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/GCast${type.from}To${type.to}.java" />
</#if>

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

import io.netty.buffer.ByteBuf;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionCostCategory;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import org.joda.time.MutableDateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DateMidnight;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
<#if type.major == "VarCharDate" || type.major == "VarBinaryDate">
@FunctionTemplate(names = {"cast${type.to?upper_case}", "${type.alias}"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL, 
  costCategory = FunctionCostCategory.COMPLEX)
public class Cast${type.from}To${type.to} implements DrillSimpleFunc {
<#elseif type.major == "NullableVarCharDate">
@FunctionTemplate(name = "castEmptyString${type.from}To${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = NullHandling.INTERNAL,
    isInternal = true)
public class GCast${type.from}To${type.to} implements DrillSimpleFunc {
</#if>

  @Param ${type.from}Holder in;
  @Output ${type.to}Holder out;
  
  public void setup() { }

  public void eval() {
    <#if type.major == "NullableVarCharDate">
    if(<#if type.from == "NullableVarChar" || type.from == "NullableVar16Char" || type.from == "NullableVarBinary">in.isSet == 0 || </#if>in.end == in.start) {
      out.isSet = 0;
      return;
    }
    out.isSet = 1;
    </#if>

    <#if type.to != "Date" && type.to != "NullableDate">
    byte[] buf = new byte[in.end - in.start];
    in.buffer.getBytes(in.start, buf, 0, in.end - in.start);
    String input = new String(buf, com.google.common.base.Charsets.UTF_8);
    </#if>

    <#if type.to == "Date" || type.to == "NullableDate">
    out.value = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.getDate(in.buffer, in.start, in.end);

    <#elseif type.to == "TimeStamp" || type.to == "NullableTimeStamp">
    java.time.LocalDateTime parsedDateTime = org.apache.drill.exec.expr.fn.impl.DateUtility.parseBest(input);
    out.value = parsedDateTime.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();

    <#elseif type.to == "Time" || type.to == "NullableTime">
    java.time.format.DateTimeFormatter f = org.apache.drill.exec.expr.fn.impl.DateUtility.getTimeFormatter();
    out.value = (int) (java.time.LocalTime.parse(input, f).atDate(java.time.LocalDate.ofEpochDay(0)).toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
    </#if>
  }
}
</#if> <#-- type.major -->
</#list>
