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

<#list intervalNumericTypes.interval as intervaltype>
<#list intervalNumericTypes.numeric as numerictype>

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/${intervaltype}${numerictype}Functions.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

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

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

public class ${intervaltype}${numerictype}Functions {

<#-- Macro block to multiply interval data type with integers -->
<#macro intervalIntegerMultiplyBlock left right temp out intervaltype>
    <#if intervaltype == "Interval">
    ${out}.months = (int) (${left}.months * ${right}.value);
    ${out}.days = (int) (${left}.days * ${right}.value);
    ${out}.milliseconds = (int) (${left}.milliseconds * ${right}.value);
    <#elseif intervaltype == "IntervalYear">
    ${out}.months = (int) (${left}.value * ${right}.value);
    <#elseif intervaltype == "IntervalDay">
    ${out}.days = (int) (${left}.days * ${right}.value);
    ${out}.milliseconds = (int) (${left}.milliseconds * ${right}.value);
    </#if>
</#macro>

<#-- Macro block to multiply and divide interval data type with integers, floating point values -->
<#macro intervalNumericArithmeticBlock left right temp op out intervaltype>
    double fractionalMonths = 0;
    double fractionalDays = 0;
    double fractionalMillis = 0;

    <#if intervaltype == "Interval">
    fractionalMonths = (${left}.months ${op} (double) ${right}.value);
    fractionalDays = (${left}.days ${op} (double) ${right}.value);
    fractionalMillis = (${left}.milliseconds ${op} (double) ${right}.value);
    <#elseif intervaltype == "IntervalYear">
    fractionalMonths = (${left}.value ${op} (double) ${right}.value);
    <#elseif intervaltype == "IntervalDay">
    fractionalDays = (${left}.days ${op} ${right}.value);
    fractionalMillis = (${left}.milliseconds ${op} (double) ${right}.value);
    </#if>

    ${out}.months = (int) fractionalMonths;

    // Transfer fractional part to days
    fractionalMonths = fractionalMonths - (long) fractionalMonths;
    fractionalDays += fractionalMonths * org.apache.drill.exec.vector.DateUtilities.monthToStandardDays;
    ${out}.days = (int) fractionalDays;

    // Transfer fractional part to millis
    fractionalDays = fractionalDays - (long) fractionalDays;
    fractionalMillis += fractionalDays * org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis;

    ${out}.milliseconds = (int) fractionalMillis;
</#macro>

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "multiply", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class ${intervaltype}${numerictype}MultiplyFunction implements DrillSimpleFunc {
    @Param ${intervaltype}Holder left;
    @Param ${numerictype}Holder right;
    @Output IntervalHolder out;

        public void setup() {
        }

        public void eval() {
            <#if numerictype == "Int" || numerictype == "BigInt">
            <@intervalIntegerMultiplyBlock left="left" right="right" temp = "temp" out = "out" intervaltype=intervaltype />
            <#elseif numerictype == "Float4" || numerictype == "Float8">
            <@intervalNumericArithmeticBlock left="left" right="right" temp = "temp" op = "*" out = "out" intervaltype=intervaltype />
            </#if>
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "multiply", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class ${numerictype}${intervaltype}MultiplyFunction implements DrillSimpleFunc {
    @Param ${numerictype}Holder right;
    @Param ${intervaltype}Holder left;
    @Output IntervalHolder out;

        public void setup() {
        }

        public void eval() {
            <#if numerictype == "Int" || numerictype == "BigInt">
            <@intervalIntegerMultiplyBlock left="left" right="right" temp = "temp" out = "out" intervaltype=intervaltype />
            <#elseif numerictype == "Float4" || numerictype == "Float8">
            <@intervalNumericArithmeticBlock left="left" right="right" temp = "temp" op = "*" out = "out" intervaltype=intervaltype />
            </#if>
        }
    }

  @SuppressWarnings("unused")
  @FunctionTemplate(names = {"divide", "div"<#if numerictype == "Int">, "/int"</#if>},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${intervaltype}${numerictype}DivideFunction implements DrillSimpleFunc {
    @Param ${intervaltype}Holder left;
    @Param ${numerictype}Holder right;
    @Output IntervalHolder out;

    public void setup() {
    }

    public void eval() {
      <@intervalNumericArithmeticBlock left="left" right="right" temp = "temp" op = "/" out = "out" intervaltype=intervaltype />
    }
  }
}
</#list>
</#list>
