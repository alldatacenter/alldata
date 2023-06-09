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
<#if type.major == "VarCharInterval" || type.major == "NullableVarCharInterval">  <#-- Template to convert from VarChar to Interval, IntervalYear, IntervalDay -->

<#if type.major == "VarCharInterval">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}To${type.to}.java" />
<#elseif type.major == "NullableVarCharInterval">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/CastEmptyString${type.from}To${type.to}.java" />
</#if>

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

import io.netty.buffer.ByteBuf;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
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
<#if type.major == "VarCharInterval">
@FunctionTemplate(name = "cast${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = NullHandling.NULL_IF_NULL)
public class Cast${type.from}To${type.to} implements DrillSimpleFunc {
<#elseif type.major == "NullableVarCharInterval">
@FunctionTemplate(name = "castEmptyString${type.from}To${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = NullHandling.INTERNAL,
    isInternal = true)
public class CastEmptyString${type.from}To${type.to} implements DrillSimpleFunc {
</#if>

  @Param ${type.from}Holder in;
  @Output ${type.to}Holder out;

  public void setup() {
  }

  public void eval() {
    <#if type.major == "NullableVarCharInterval">
    if (<#if type.from == "NullableVarChar" || type.from == "NullableVar16Char" || type.from == "NullableVarBinary">in.isSet == 0 || </#if>in.end == in.start) {
      out.isSet = 0;
      return;
    }
    out.isSet = 1;
    </#if>

    byte[] buf = new byte[in.end - in.start];
    in.buffer.getBytes(in.start, buf, 0, in.end - in.start);
    String input = new String(buf, com.google.common.base.Charsets.UTF_8);

    // Parse the ISO format
    org.joda.time.Period period = org.joda.time.Period.parse(input);

    <#if type.to == "Interval" || type.to == "NullableInterval">
    out.months       = period.getYears() * org.apache.drill.exec.vector.DateUtilities.yearsToMonths + period.getMonths();

    out.days         = period.getDays();

    out.milliseconds = period.getHours() * org.apache.drill.exec.vector.DateUtilities.hoursToMillis +
                       period.getMinutes() * org.apache.drill.exec.vector.DateUtilities.minutesToMillis +
                       period.getSeconds() * org.apache.drill.exec.vector.DateUtilities.secondsToMillis +
                       period.getMillis();

    <#elseif type.to == "IntervalDay" || type.to == "NullableIntervalDay">
    out.days         = period.getDays();

    long millis = period.getHours() * (long) org.apache.drill.exec.vector.DateUtilities.hoursToMillis +
                  period.getMinutes() * (long) org.apache.drill.exec.vector.DateUtilities.minutesToMillis +
                  period.getSeconds() * (long) org.apache.drill.exec.vector.DateUtilities.secondsToMillis +
                  period.getMillis();

    if (millis >= org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis) {
      int daysFromMillis = (int) (millis / org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
      millis -= daysFromMillis * org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis;
      out.days += daysFromMillis;
    }

    out.milliseconds = (int) millis;

    <#elseif type.to == "IntervalYear" || type.to == "NullableIntervalYear">
    out.value = period.getYears() * org.apache.drill.exec.vector.DateUtilities.yearsToMonths + period.getMonths();
    </#if>
  }
}
</#if> <#-- type.major -->
</#list>
