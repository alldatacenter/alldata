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

<#if type.major == "VarCharDecimalComplex" || type.major == "NullableVarCharDecimalComplex">  <#-- Cast function template for conversion from VarChar to VarDecimal -->

<#if type.major == "VarCharDecimalComplex">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}${type.to}.java"/>
<#elseif type.major == "NullableVarCharDecimalComplex">
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/CastEmptyString${type.from}To${type.to}.java"/>
</#if>

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

<#include "/@includes/vv_imports.ftl" />

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.expr.annotations.Workspace;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")
<#if type.major == "VarCharDecimalComplex">
@FunctionTemplate(name = "cast${type.to?upper_case}",
                  scope = FunctionTemplate.FunctionScope.SIMPLE,
                  returnType = FunctionTemplate.ReturnType.DECIMAL_CAST,
                  nulls = NullHandling.NULL_IF_NULL)
public class Cast${type.from}${type.to} implements DrillSimpleFunc {
<#elseif type.major == "NullableVarCharDecimalComplex">
@FunctionTemplate(name = "castEmptyString${type.from}To${type.to?upper_case}",
                  scope = FunctionTemplate.FunctionScope.SIMPLE,
                  returnType = FunctionTemplate.ReturnType.DECIMAL_CAST,
                  nulls = NullHandling.INTERNAL)
public class CastEmptyString${type.from}To${type.to} implements DrillSimpleFunc {
</#if>
  @Param ${type.from}Holder in;
  @Inject DrillBuf buffer;
  @Param IntHolder precision;
  @Param IntHolder scale;

  @Output ${type.to}Holder out;

  public void setup() {
  }

  public void eval() {
    <#if type.major == "NullableVarCharDecimalComplex">
    // Check if the input is null or empty string
    if (<#if type.from == "NullableVarChar"> in.isSet == 0 || </#if> in.end == in.start) {
      out.isSet = 0;
      return;
    }
    out.isSet = 1;
    </#if>

    out.start  = 0;

    out.scale = scale.value;
    out.precision = precision.value;

    byte[] buf = new byte[in.end - in.start];
    in.buffer.getBytes(in.start, buf, 0, in.end - in.start);
    String s = new String(buf, com.google.common.base.Charsets.UTF_8);
    java.math.BigDecimal bd = new java.math.BigDecimal(s);

    org.apache.drill.exec.util.DecimalUtility.checkValueOverflow(bd, precision.value, scale.value);

    bd = bd.setScale(scale.value, java.math.RoundingMode.HALF_UP);

    byte[] bytes = bd.unscaledValue().toByteArray();
    int len = bytes.length;
    out.buffer = buffer = buffer.reallocIfNeeded(len);
    out.buffer.setBytes(out.start, bytes);
    out.end = out.start + len;
  }
}
</#if> <#-- type.major -->
</#list>
