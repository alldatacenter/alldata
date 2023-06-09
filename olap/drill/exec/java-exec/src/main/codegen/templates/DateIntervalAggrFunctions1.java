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



<#list aggrtypes1.aggrtypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/${aggrtype.className}DateTypeFunctions.java" />

<#include "/@includes/license.ftl" />


<#-- A utility class that is used to generate java code for aggr functions for Date, Time, Interval types -->
<#--  that maintain a single running counter to hold the result.  This includes: MIN, MAX, SUM, COUNT. -->

package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")

public class ${aggrtype.className}DateTypeFunctions {

<#list aggrtype.types as type>
<#if type.major == "Date">
@FunctionTemplate(name = "${aggrtype.funcName}", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {

  @Param ${type.inputType}Holder in;
  @Workspace ${type.runningType}Holder value;
  @Workspace BigIntHolder nonNullCount;
  @Output ${type.outputType}Holder out;

  public void setup() {
	  value = new ${type.runningType}Holder();
    nonNullCount = new BigIntHolder();
    nonNullCount.value = 0;
    <#if type.runningType == "Interval">
    value.months = ${type.initialValue};
    value.days= ${type.initialValue};
    value.milliseconds = ${type.initialValue};
    <#elseif type.runningType == "IntervalDay">
    value.days= ${type.initialValue};
    value.milliseconds = ${type.initialValue};
    <#else>
    value.value = ${type.initialValue};
    </#if>
  }

  @Override
  public void add() {
	  <#if type.inputType?starts_with("Nullable")>
	    sout: {
	    if (in.isSet == 0) {
		    // processing nullable input and the value is null, so don't do anything...
		    break sout;
	    }
	  </#if>
    <#if aggrtype.funcName == "single_value">
      if (nonNullCount.value > 0) {
        throw org.apache.drill.common.exceptions.UserException.functionError()
            .message("Input for single_value function has more than one row")
            .build();
      }
    </#if>
    nonNullCount.value = 1;
	  <#if aggrtype.funcName == "min">

    <#if type.outputType?ends_with("Interval")>

    long inMS = (long) in.months * org.apache.drill.exec.vector.DateUtilities.monthsToMillis+
                       in.days * (org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis) +
                       in.milliseconds;

    value.value = Math.min(value.value, inMS);

    <#elseif type.outputType?ends_with("IntervalDay")>
    long inMS = (long) in.days * (org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis) +
                       in.milliseconds;

    value.value = Math.min(value.value, inMS);


    <#else>
    value.value = Math.min(value.value, in.value);
    </#if>
	  <#elseif aggrtype.funcName == "max">
    <#if type.outputType?ends_with("Interval")>
    long inMS = (long) in.months * org.apache.drill.exec.vector.DateUtilities.monthsToMillis+
                       in.days * (org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis) +
                       in.milliseconds;

    value.value = Math.max(value.value, inMS);
    <#elseif type.outputType?ends_with("IntervalDay")>
    long inMS = (long) in.days * (org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis) +
                       in.milliseconds;

    value.value = Math.max(value.value, inMS);
    <#else>
    value.value = Math.max(value.value, in.value);
    </#if>

	  <#elseif aggrtype.funcName == "sum">
    <#if type.outputType?ends_with("Interval")>
    value.days += in.days;
    value.months += in.months;
    value.milliseconds += in.milliseconds;
    <#elseif type.outputType?ends_with("IntervalDay")>
    value.days += in.days;
    value.milliseconds += in.milliseconds;
    <#else>
	    value.value += in.value;
    </#if>
	  <#elseif aggrtype.funcName == "count">
	    value.value++;
    <#elseif aggrtype.funcName == "any_value" || aggrtype.funcName == "single_value">
      <#if type.outputType?ends_with("Interval")>
        value.days = in.days;
        value.months = in.months;
        value.milliseconds = in.milliseconds;
      <#elseif type.outputType?ends_with("IntervalDay")>
        value.days = in.days;
        value.milliseconds = in.milliseconds;
      <#else>
        value.value = in.value;
      </#if>
    <#else>
	  // TODO: throw an error ?
	  </#if>
	<#if type.inputType?starts_with("Nullable")>
    } // end of sout block
	</#if>
  }

  @Override
  public void output() {
    if (nonNullCount.value > 0) {
      out.isSet = 1;
      <#if aggrtype.funcName == "max" || aggrtype.funcName == "min">
      <#if type.outputType?ends_with("Interval")>
      out.months = (int) (value.value / org.apache.drill.exec.vector.DateUtilities.monthsToMillis);
      value.value = value.value % org.apache.drill.exec.vector.DateUtilities.monthsToMillis;
      out.days = (int) (value.value / org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
      out.milliseconds = (int) (value.value % org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
      <#elseif type.outputType?ends_with("IntervalDay")>
      out.days = (int) (value.value / org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
      out.milliseconds = (int) (value.value % org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
      <#else>
      out.value = value.value;
      </#if>
      <#else>
      <#if type.outputType?ends_with("Interval")>
      out.months = value.months;
      out.days = value.days;
      out.milliseconds = value.milliseconds;
      <#elseif type.outputType?ends_with("IntervalDay")>
      out.days = value.days;
      out.milliseconds = value.milliseconds;
      <#else>
      out.value = value.value;
      </#if>
      </#if>
    } else {
      out.isSet = 0;
    }
  }

  @Override
  public void reset() {
    nonNullCount.value = 0;
    <#if type.runningType == "Interval">
    value.months = ${type.initialValue};
    value.days= ${type.initialValue};
    value.milliseconds = ${type.initialValue};
    <#elseif type.runningType == "IntervalDay">
    value.days= ${type.initialValue};
    value.milliseconds = ${type.initialValue};
    <#else>
    value.value = ${type.initialValue};
    </#if>
  }

 }

</#if>
</#list>
}
</#list>

