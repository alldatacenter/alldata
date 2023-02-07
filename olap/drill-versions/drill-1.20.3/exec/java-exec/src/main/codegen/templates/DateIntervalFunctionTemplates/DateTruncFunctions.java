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
import org.apache.drill.exec.expr.annotations.Workspace;

<@pp.dropOutputFile />

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/GDateTimeTruncateFunctions.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;

import io.netty.buffer.ByteBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

public class GDateTimeTruncateFunctions {

<#list dateIntervalFunc.truncInputTypes as type> <#-- Start InputType Loop -->

  /**
   * This class merely act as a placeholder so that Calcite allows 'trunc('truncationUnit', col)'
   * function in SQL.
   */
  @SuppressWarnings("unused")
  @FunctionTemplate(name = "date_trunc", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  public static class G${type}DummyDateTrunc implements DrillSimpleFunc {
    @Param  VarCharHolder left;
    @Param  ${type}Holder right;
    @Output ${type}Holder out;

    @Override
    public void setup() { }

    @Override
    public void eval() { }
  }
  <#list dateIntervalFunc.truncUnits as toUnit> <#-- Start UnitType Loop -->

    <#-- Filter out unsupported combinations -->
    <#if !(type == "Time" && toUnit == "Week") &&
      !(type == "Interval" && toUnit == "Week") &&
      !(type == "IntervalDay" && toUnit == "Week") &&
      !(type == "IntervalYear" && toUnit == "Week")
    > <#-- Truncate by Week not supported for Time input type -->

  @SuppressWarnings("unused")
  @FunctionTemplate(names = "date_trunc_${toUnit}", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  public static class G${type}${toUnit}DateTrunc implements DrillSimpleFunc {
    @Param  ${type}Holder right;
    @Output ${type}Holder out;
    @Workspace org.joda.time.MutableDateTime dateTime;
    <#if toUnit == "Quarter" || toUnit == "Decade" || toUnit == "Century" || toUnit == "Millennium">
    @Workspace org.joda.time.MutableDateTime dateTime2;
    </#if>

    public void setup() {
      dateTime = new org.joda.time.MutableDateTime(org.joda.time.DateTimeZone.UTC);
      <#if toUnit == "Quarter" || toUnit == "Decade" || toUnit == "Century" || toUnit == "Millennium">
      dateTime2 = new org.joda.time.MutableDateTime(org.joda.time.DateTimeZone.UTC);
      </#if>
    }

    public void eval() {
      <#if type == "Time"> <#-- Start InputType -->
        <#if toUnit == "Hour"> <#-- Start UnitType -->
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().hourOfDay());
      out.value = (int) dateTime.getMillis();
        <#elseif toUnit == "Minute">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().minuteOfHour());
      out.value = (int) dateTime.getMillis();
        <#elseif toUnit == "Second">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().secondOfMinute());
      out.value = (int) dateTime.getMillis();
        <#else>
        <#-- For all other units truncate the whole thing -->
      out.value = 0;
        </#if> <#-- End UnitType -->
      <#elseif type == "Date">
        <#if toUnit == "Second" || toUnit == "Minute" || toUnit == "Hour" || toUnit == "Day"> <#-- Start UnitType -->
      // No truncation as there is no time part in date
      out.value = right.value;
        <#elseif toUnit == "Year">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().year());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Month">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().monthOfYear());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Week">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().weekOfWeekyear());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Quarter">
      dateTime.setMillis(right.value);
      int month = dateTime.getMonthOfYear();
      dateTime.setRounding(dateTime.getChronology().year());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.months(), ((month-1)/3)*3);
      out.value = dateTime2.getMillis();
        <#elseif toUnit == "Decade">
      dateTime.setMillis(right.value);
      int year = dateTime.getYear();
      dateTime.setRounding(dateTime.getChronology().centuryOfEra());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.years(), ((year%100)/10)*10);
      out.value = dateTime2.getMillis();
        <#elseif toUnit == "Century">
      dateTime.setMillis(right.value);
      dateTime.add(org.joda.time.DurationFieldType.years(), -1);
      dateTime.setRounding(dateTime.getChronology().centuryOfEra());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.years(), 1);
      out.value = dateTime2.getMillis();
        <#elseif toUnit == "Millennium">
      dateTime.setMillis(right.value);
      int year = dateTime.getYear();
      dateTime.setRounding(dateTime.getChronology().era());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.years(), ((year-1)/1000)*1000);
      out.value = dateTime2.getMillis();
        </#if> <#-- End UnitType -->
      <#elseif type == "TimeStamp">
        <#if toUnit == "Year"> <#--  Start UnitType -->
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().year());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Month">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().monthOfYear());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Day">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().dayOfMonth());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Hour">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().hourOfDay());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Minute">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().minuteOfHour());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Second">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().secondOfMinute());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Week">
      dateTime.setMillis(right.value);
      dateTime.setRounding(dateTime.getChronology().weekOfWeekyear());
      out.value = dateTime.getMillis();
        <#elseif toUnit == "Quarter">
      dateTime.setMillis(right.value);
      int month = dateTime.getMonthOfYear();
      dateTime.setRounding(dateTime.getChronology().year());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.months(), ((month-1)/3)*3);
      out.value = dateTime2.getMillis();
        <#elseif toUnit == "Decade">
      dateTime.setMillis(right.value);
      int year = dateTime.getYear();
      dateTime.setRounding(dateTime.getChronology().centuryOfEra());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.years(), ((year%100)/10)*10);
      out.value = dateTime2.getMillis();
        <#elseif toUnit == "Century">
      dateTime.setMillis(right.value);
      dateTime.add(org.joda.time.DurationFieldType.years(), -1);
      dateTime.setRounding(dateTime.getChronology().centuryOfEra());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.years(), 1);
      out.value = dateTime2.getMillis();
        <#elseif toUnit == "Millennium">
      dateTime.setMillis(right.value);
      int year = dateTime.getYear();
      dateTime.setRounding(dateTime.getChronology().era());
      dateTime2.setMillis(dateTime.getMillis());
      dateTime2.add(org.joda.time.DurationFieldType.years(), ((year-1)/1000)*1000);
      out.value = dateTime2.getMillis();
        </#if> <#-- End UnitType -->
      <#elseif type == "Interval">
        <#if toUnit == "Second"> <#--  Start UnitType -->
      out.months = right.months;
      out.days = right.days;
      out.milliseconds = (right.milliseconds/(org.apache.drill.exec.vector.DateUtilities.secondsToMillis))*
          (org.apache.drill.exec.vector.DateUtilities.secondsToMillis);
        <#elseif toUnit == "Minute">
      out.months = right.months;
      out.days = right.days;
      out.milliseconds = (right.milliseconds/(org.apache.drill.exec.vector.DateUtilities.minutesToMillis))*
          (org.apache.drill.exec.vector.DateUtilities.minutesToMillis);
        <#elseif toUnit == "Hour">
      out.months = right.months;
      out.days = right.days;
      out.milliseconds =
          (right.milliseconds/(org.apache.drill.exec.vector.DateUtilities.hoursToMillis))*
              (org.apache.drill.exec.vector.DateUtilities.hoursToMillis);
        <#elseif toUnit == "Day">
      out.months = right.months;
      out.days = right.days;
      out.milliseconds = 0;
        <#elseif toUnit == "Month">
      out.months = right.months;
      out.days = 0;
      out.milliseconds = 0;
        <#elseif toUnit == "Quarter">
      out.months = (right.months / 3) * 3;
      out.days = 0;
      out.milliseconds = 0;
        <#elseif toUnit == "Year">
      out.months = (right.months / 12) * 12;
      out.days = 0;
      out.milliseconds = 0;
        <#elseif toUnit == "Decade">
      out.months = (right.months / 120) * 120;
      out.days = 0;
      out.milliseconds = 0;
        <#elseif toUnit == "Century">
      out.months = (right.months / 1200) * 1200;
      out.days = 0;
      out.milliseconds = 0;
        <#elseif toUnit == "Millennium">
      out.months = (right.months / 12000) * 12000;
      out.days = 0;
      out.milliseconds = 0;
        </#if> <#-- End UnitType -->
      <#elseif type == "IntervalDay">
        <#if toUnit == "Second"> <#--  Start UnitType -->
      out.days = right.days;
      out.milliseconds = (right.milliseconds/(org.apache.drill.exec.vector.DateUtilities.secondsToMillis))*
        (org.apache.drill.exec.vector.DateUtilities.secondsToMillis);
        <#elseif toUnit == "Minute">
      out.days = right.days;
      out.milliseconds = (right.milliseconds/(org.apache.drill.exec.vector.DateUtilities.minutesToMillis))*
          (org.apache.drill.exec.vector.DateUtilities.minutesToMillis);
        <#elseif toUnit == "Hour">
      out.days = right.days;
      out.milliseconds =
          (right.milliseconds/(org.apache.drill.exec.vector.DateUtilities.hoursToMillis))*
              (org.apache.drill.exec.vector.DateUtilities.hoursToMillis);
        <#elseif toUnit == "Day">
      out.days = right.days;
      out.milliseconds = 0;
        <#elseif toUnit == "Month" || toUnit == "Quarter" || toUnit == "Year" || toUnit == "Decade" || toUnit == "Century" || toUnit == "Millennium">
      out.days = 0;
      out.milliseconds = 0;
        </#if> <#-- End UnitType -->
      <#elseif type == "IntervalYear">
        <#if toUnit == "Second" || toUnit == "Minute" || toUnit == "Hour" || toUnit == "Day"> <#--  Start UnitType -->
      out.value = right.value;
        <#elseif toUnit == "Month">
      out.value = right.value;
        <#elseif toUnit == "Quarter">
      out.value = (right.value / 3) * 3;
        <#elseif toUnit == "Year">
      out.value = (right.value / 12) * 12;
        <#elseif toUnit == "Decade">
      out.value = (right.value / 120) * 120;
        <#elseif toUnit == "Century">
      out.value = (right.value / 1200) * 1200;
        <#elseif toUnit == "Millennium">
      out.value = (right.value / 12000) * 12000;
        </#if> <#-- End UnitType -->
      </#if> <#-- End InputType -->
    }
}
   </#if> <#-- Filter out unsupported combinations -->
  </#list> <#-- End UnitType Loop -->
</#list> <#-- End InputType Loop -->
}