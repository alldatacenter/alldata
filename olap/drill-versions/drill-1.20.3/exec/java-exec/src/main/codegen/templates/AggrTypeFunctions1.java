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
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/${aggrtype.className}Functions.java" />

<#include "/@includes/license.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

<#-- A utility class that is used to generate java code for aggr functions that maintain a single -->
<#-- running counter to hold the result.  This includes: MIN, MAX, SUM. -->

package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;

@SuppressWarnings("unused")

public class ${aggrtype.className}Functions {

<#list aggrtype.types as type>
<#if type.major == "Numeric">

  @FunctionTemplate(name = "${aggrtype.funcName}",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {
    @Param ${type.inputType}Holder in;
    @Workspace ${type.runningType}Holder value;
    @Workspace BigIntHolder nonNullCount;
    @Output ${type.outputType}Holder out;

    public void setup() {
      value = new ${type.runningType}Holder();
      nonNullCount = new BigIntHolder();
      nonNullCount.value = 0;
    <#if aggrtype.funcName == "sum" || aggrtype.funcName == "any_value" || aggrtype.funcName == "single_value">
      value.value = 0;
    <#elseif aggrtype.funcName == "min">
      <#if type.runningType?starts_with("Bit")>
      value.value = 1;
      <#elseif type.runningType?starts_with("Int")>
      value.value = Integer.MAX_VALUE;
      <#elseif type.runningType?starts_with("BigInt")>
      value.value = Long.MAX_VALUE;
      <#elseif type.runningType?starts_with("Float4")>
      value.value = Float.NaN;
      <#elseif type.runningType?starts_with("Float8")>
      value.value = Double.NaN;
      </#if>
    <#elseif aggrtype.funcName == "max">
      <#if type.runningType?starts_with("Bit")>
      value.value = 0;
      <#elseif type.runningType?starts_with("Int")>
      value.value = Integer.MIN_VALUE;
      <#elseif type.runningType?starts_with("BigInt")>
      value.value = Long.MIN_VALUE;
      <#elseif type.runningType?starts_with("Float4")>
      value.value = -Float.MAX_VALUE;
      <#elseif type.runningType?starts_with("Float8")>
      value.value = -Double.MAX_VALUE;
      </#if>
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
        // For min/max functions: NaN is the biggest value,
        // Infinity is the second biggest value
        // -Infinity is the smallest value
      <#if aggrtype.funcName == "min">
        <#if type.inputType?contains("Float4")>
        if(!Float.isNaN(in.value)) {
          value.value = Float.isNaN(value.value) ? in.value : Math.min(value.value, in.value);
        }
        <#elseif type.inputType?contains("Float8")>
        if(!Double.isNaN(in.value)) {
          value.value = Double.isNaN(value.value) ? in.value : Math.min(value.value, in.value);
        }
        <#else>
        value.value = Math.min(value.value, in.value);
        </#if>
      <#elseif aggrtype.funcName == "max">
        value.value = Math.max(value.value,  in.value);
      <#elseif aggrtype.funcName == "sum">
        value.value += in.value;
      <#elseif aggrtype.funcName == "count">
        value.value++;
      <#elseif aggrtype.funcName == "any_value" || aggrtype.funcName == "single_value">
        value.value = in.value;
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
        out.value = value.value;
        out.isSet = 1;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      nonNullCount.value = 0;
    <#if aggrtype.funcName == "sum" || aggrtype.funcName == "count" || aggrtype.funcName == "any_value" || aggrtype.funcName == "single_value">
      value.value = 0;
    <#elseif aggrtype.funcName == "min">
      <#if type.runningType?starts_with("Int")>
      value.value = Integer.MAX_VALUE;
      <#elseif type.runningType?starts_with("BigInt")>
      value.value = Long.MAX_VALUE;
      <#elseif type.runningType?starts_with("Float4")>
      value.value = Float.NaN;
      <#elseif type.runningType?starts_with("Float8")>
      value.value = Double.NaN;
      <#elseif type.runningType?starts_with("Bit")>
      value.value = 1;
      </#if>
    <#elseif aggrtype.funcName == "max">
      <#if type.runningType?starts_with("Int")>
      value.value = Integer.MIN_VALUE;
      <#elseif type.runningType?starts_with("BigInt")>
      value.value = Long.MIN_VALUE;
      <#elseif type.runningType?starts_with("Float4")>
      value.value = -Float.MAX_VALUE;
      <#elseif type.runningType?starts_with("Float8")>
      value.value = -Double.MAX_VALUE;
      <#elseif type.runningType?starts_with("Bit")>
      value.value = 0;
      </#if>
    </#if>
    }
  }

</#if>
</#list>
}
</#list>

