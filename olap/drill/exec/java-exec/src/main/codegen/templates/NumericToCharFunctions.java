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

<#list numericTypes.numeric as type>

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

import java.text.NumberFormat;
import java.text.DecimalFormat;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
@FunctionTemplate(name = "to_char",
                  scope = FunctionTemplate.FunctionScope.SIMPLE,
                  nulls = NullHandling.NULL_IF_NULL)
public class G${type}ToChar implements DrillSimpleFunc {

  @Param  ${type}Holder left;
  @Param  VarCharHolder right;
  @Inject DrillBuf buffer;
  @Workspace java.text.NumberFormat outputFormat;
  @Output VarCharHolder out;

  public void setup() {
    buffer = buffer.reallocIfNeeded(100);
    byte[] buf = new byte[right.end - right.start];
    right.buffer.getBytes(right.start, buf, 0, right.end - right.start);
    String inputFormat = new String(buf);
    outputFormat = new java.text.DecimalFormat(inputFormat);
  }

  public void eval() {
    <#if type == "VarDecimal">
    java.math.BigDecimal bigDecimal = org.apache.drill.exec.util.DecimalUtility.getBigDecimalFromDrillBuf(left.buffer, left.start, left.end - left.start, left.scale);
    String str = outputFormat.format(bigDecimal);
    <#else>
    String str =  outputFormat.format(left.value);
    </#if>
    out.buffer = buffer;
    out.start = 0;
    out.end = Math.min(100, str.length()); // truncate if target type has length smaller than that of input's string
    out.buffer.setBytes(0, str.substring(0, out.end).getBytes());
  }
}
</#list>
