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

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/G${type}ToChar.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

<#include "/@includes/vv_imports.ftl" />

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")
@FunctionTemplate(name = "to_char", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
public class G${type}ToChar implements DrillSimpleFunc {

    @Param  ${type}Holder left;
    @Param  VarCharHolder right;
    @Inject DrillBuf buffer;
    @Workspace org.joda.time.MutableDateTime temp;
    @Workspace org.joda.time.format.DateTimeFormatter format;
    @Output VarCharHolder out;

    public void setup() {
        temp = new org.joda.time.MutableDateTime(0, org.joda.time.DateTimeZone.UTC);
        buffer = buffer.reallocIfNeeded(100);

        // Get the desired output format and create a DateTimeFormatter
        byte[] buf = new byte[right.end - right.start];
        right.buffer.getBytes(right.start, buf, 0, right.end - right.start);
        String input = new String(buf, com.google.common.base.Charsets.UTF_8);
        format = org.joda.time.format.DateTimeFormat.forPattern(input);
    }

    public void eval() {
        temp.setMillis(left.value);

        // print current value in the desired format
        String str = format.print(temp);

        out.buffer = buffer;
        out.start = 0;
        out.end = Math.min(100, str.length()); // truncate if target type has length smaller than that of input's string
        out.buffer.setBytes(0, str.substring(0,out.end).getBytes());
    }
}
</#list>