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

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/${intervaltype}Functions.java" />

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

public class ${intervaltype}Functions {

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "add", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class ${intervaltype}AddFunction implements DrillSimpleFunc {
    @Param ${intervaltype}Holder left;
    @Param ${intervaltype}Holder right;
    @Output ${intervaltype}Holder out;

        public void setup() {
        }

        public void eval() {
            <#if intervaltype == "Interval">
            out.months = left.months + right.months;
            out.days = left.days + right.days;
            out.milliseconds = left.milliseconds + right.milliseconds;
            <#elseif intervaltype == "IntervalYear">
            out.value = left.value + right.value;
            <#elseif intervaltype == "IntervalDay">
            out.days = left.days + right.days;
            long millis = (long) left.milliseconds + right.milliseconds;

            if (millis >= org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis) {
              int daysFromMillis = (int) (millis / org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);

              millis -= daysFromMillis * org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis;
              out.days += daysFromMillis;
            }
            out.milliseconds = (int) millis;
            </#if>
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "subtract", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class ${intervaltype}SubtractFunction implements DrillSimpleFunc {
    @Param ${intervaltype}Holder left;
    @Param ${intervaltype}Holder right;
    @Output ${intervaltype}Holder out;

        public void setup() {
        }

        public void eval() {
            <#if intervaltype == "Interval">
            out.months = left.months - right.months;
            out.days = left.days - right.days;
            out.milliseconds = left.milliseconds - right.milliseconds;
            <#elseif intervaltype == "IntervalYear">
            out.value = left.value - right.value;
            <#elseif intervaltype == "IntervalDay">
            out.days = left.days - right.days;
            out.milliseconds = left.milliseconds - right.milliseconds;

            int daysFromMillis = out.milliseconds/org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis;
            if (daysFromMillis != 0) {
              out.milliseconds -= org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis*daysFromMillis;
              out.days -= Math.abs(daysFromMillis);
            }

            // if milliseconds are bellow zero, substract them from the days
            if (out.milliseconds < 0 && out.days > 0) {
              out.days  -= 1;
              out.milliseconds =  org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis + out.milliseconds;
            }
            </#if>
        }
    }
    @SuppressWarnings("unused")
    @FunctionTemplate(names = {"negative", "u-", "-"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class ${intervaltype}NegateFunction implements DrillSimpleFunc {
    @Param ${intervaltype}Holder left;
    @Output ${intervaltype}Holder out;

        public void setup() {
        }

        public void eval() {
            <#if intervaltype == "Interval">
            out.months = -left.months;
            out.days = -left.days;
            out.milliseconds = -left.milliseconds;
            <#elseif intervaltype == "IntervalYear">
            out.value = -left.value;
            <#elseif intervaltype == "IntervalDay">
            out.days = -left.days;
            out.milliseconds = -left.milliseconds;
            </#if>
        }
    }
}
</#list>