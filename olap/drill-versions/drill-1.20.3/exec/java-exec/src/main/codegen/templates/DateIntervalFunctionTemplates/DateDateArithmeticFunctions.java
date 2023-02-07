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

<#list dateIntervalFunc.dates as type>

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/G${type}Arithmetic.java" />

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

@SuppressWarnings("unused")
public class G${type}Arithmetic {
@SuppressWarnings("unused")

@FunctionTemplate(names = {"date_diff", "subtract", "date_sub"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
public static class G${type}Difference implements DrillSimpleFunc {

    @Param  ${type}Holder left;
    @Param  ${type}Holder right;
    @Output IntervalDayHolder out;

    public void setup() {
    }

    public void eval() {
        <#if type == "Time">
        out.milliseconds = left.value - right.value;
        <#elseif type == "Date">
        out.days = (int) ((left.value - right.value) / org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
        <#elseif type == "TimeStamp">
        long difference = (left.value - right.value);
        out.milliseconds = (int) (difference % org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
        out.days = (int) (difference / org.apache.drill.exec.vector.DateUtilities.daysToStandardMillis);
        </#if>
    }
}
}
</#list>
