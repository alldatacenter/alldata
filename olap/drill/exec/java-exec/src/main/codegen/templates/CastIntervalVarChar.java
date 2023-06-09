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

<#if type.major == "IntervalVarChar">  <#-- Template to convert from Interval, IntervalYear, IntervalDay to VarChar -->

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}To${type.to}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

<#include "/@includes/vv_imports.ftl" />

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionCostCategory;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import org.joda.time.MutableDateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DateMidnight;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
@FunctionTemplate(name = "cast${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    returnType = FunctionTemplate.ReturnType.STRING_CAST,
    nulls = NullHandling.NULL_IF_NULL)
public class Cast${type.from}To${type.to} implements DrillSimpleFunc {

  @Param ${type.from}Holder in;
  @Param BigIntHolder len;
  @Inject DrillBuf buffer;
  @Output ${type.to}Holder out;

  public void setup() {
    buffer = buffer.reallocIfNeeded(${type.bufferLength});
  }

  public void eval() {

      int years  = (in.months / org.apache.drill.exec.vector.DateUtilities.yearsToMonths);
      int months = (in.months % org.apache.drill.exec.vector.DateUtilities.yearsToMonths);

      long millis = in.milliseconds;

      long hours  = millis / (org.apache.drill.exec.vector.DateUtilities.hoursToMillis);
      millis     = millis % (org.apache.drill.exec.vector.DateUtilities.hoursToMillis);

      long minutes = millis / (org.apache.drill.exec.vector.DateUtilities.minutesToMillis);
      millis      = millis % (org.apache.drill.exec.vector.DateUtilities.minutesToMillis);

      long seconds = millis / (org.apache.drill.exec.vector.DateUtilities.secondsToMillis);
      millis      = millis % (org.apache.drill.exec.vector.DateUtilities.secondsToMillis);

      String yearString = (Math.abs(years) == 1) ? " year " : " years ";
      String monthString = (Math.abs(months) == 1) ? " month " : " months ";
      String dayString = (Math.abs(in.days) == 1) ? " day " : " days ";


      StringBuilder str = new StringBuilder().
                            append(years).append(yearString).
                            append(months).append(monthString).
                            append(in.days).append(dayString).
                            append(hours).append(":").
                            append(minutes).append(":").
                            append(seconds).append(".").
                            append(millis);
      out.buffer = buffer;
      out.start = 0;
      out.end = Math.min((int)len.value, str.length()); // truncate if target type has length smaller than that of input's string
      out.buffer.setBytes(0, String.valueOf(str.substring(0,out.end)).getBytes());
  }
}

<#elseif type.major == "IntervalYearVarChar">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}To${type.to}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

<#include "/@includes/vv_imports.ftl" />

import io.netty.buffer.ByteBuf;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionCostCategory;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import org.joda.time.MutableDateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DateMidnight;

@SuppressWarnings("unused")
@FunctionTemplate(name = "cast${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    returnType = FunctionTemplate.ReturnType.STRING_CAST,
    nulls = NullHandling.NULL_IF_NULL)
public class Cast${type.from}To${type.to} implements DrillSimpleFunc {

  @Param ${type.from}Holder in;
  @Param BigIntHolder len;
  @Inject DrillBuf buffer;
  @Output ${type.to}Holder out;

  public void setup() {
    buffer = buffer.reallocIfNeeded((int) len.value);
  }

  public void eval() {
      int years  = (in.value / org.apache.drill.exec.vector.DateUtilities.yearsToMonths);
      int months = (in.value % org.apache.drill.exec.vector.DateUtilities.yearsToMonths);

      String yearString = (Math.abs(years) == 1) ? " year " : " years ";
      String monthString = (Math.abs(months) == 1) ? " month " : " months ";


      StringBuilder str = new StringBuilder().append(years).append(yearString).append(months).append(monthString);

      out.buffer = buffer;
      out.start = 0;
      out.end = Math.min((int)len.value, str.length()); // truncate if target type has length smaller than that of input's string
      out.buffer.setBytes(0, String.valueOf(str.substring(0,out.end)).getBytes());
  }
}

<#elseif type.major == "IntervalDayVarChar">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}To${type.to}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;





import io.netty.buffer.ByteBuf;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionCostCategory;
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

@SuppressWarnings("unused")
@FunctionTemplate(name = "cast${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    returnType = FunctionTemplate.ReturnType.STRING_CAST,
    nulls = NullHandling.NULL_IF_NULL,
    costCategory = FunctionCostCategory.COMPLEX)
public class Cast${type.from}To${type.to} implements DrillSimpleFunc {

  @Param ${type.from}Holder in;
  @Param BigIntHolder len;
  @Inject DrillBuf buffer;
  @Output ${type.to}Holder out;

  public void setup() {
    buffer = buffer.reallocIfNeeded((int) len.value);
  }

  public void eval() {
      long millis = in.milliseconds;

      long hours  = millis / (org.apache.drill.exec.vector.DateUtilities.hoursToMillis);
      millis     = millis % (org.apache.drill.exec.vector.DateUtilities.hoursToMillis);

      long minutes = millis / (org.apache.drill.exec.vector.DateUtilities.minutesToMillis);
      millis      = millis % (org.apache.drill.exec.vector.DateUtilities.minutesToMillis);

      long seconds = millis / (org.apache.drill.exec.vector.DateUtilities.secondsToMillis);
      millis      = millis % (org.apache.drill.exec.vector.DateUtilities.secondsToMillis);

      String dayString = (Math.abs(in.days) == 1) ? " day " : " days ";


      StringBuilder str = new StringBuilder().
                            append(in.days).append(dayString).
                            append(hours).append(":").
                            append(minutes).append(":").
                            append(seconds).append(".").
                            append(millis);


      out.buffer = buffer;
      out.start = 0;
      out.end = Math.min((int)len.value, str.length()); // truncate if target type has length smaller than that of input's string
      out.buffer.setBytes(0, String.valueOf(str.substring(0,out.end)).getBytes());
  }
}
</#if> <#-- type.major -->
</#list>