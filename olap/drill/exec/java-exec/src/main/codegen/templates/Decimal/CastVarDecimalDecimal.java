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
<#if type.major == "VarDecimalToDecimal">

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}${type.to}.java" />

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
import org.apache.drill.exec.expr.annotations.Workspace;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

import java.nio.ByteBuffer;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
@FunctionTemplate(name = "cast${type.to?upper_case}",
                  scope = FunctionTemplate.FunctionScope.SIMPLE,
                  returnType = FunctionTemplate.ReturnType.DECIMAL_CAST,
                  nulls = NullHandling.NULL_IF_NULL)
public class Cast${type.from}${type.to} implements DrillSimpleFunc {

  @Param ${type.from}Holder in;
  <#if type.to.endsWith("Sparse") || type.to.endsWith("Dense")>
  @Inject DrillBuf buffer;
  </#if>
  @Param IntHolder precision;
  @Param IntHolder scale;
  @Output ${type.to}Holder out;

  public void setup() {
  <#if type.to.endsWith("Sparse") || type.to.endsWith("Dense")>
    int size = ${type.arraySize} * (org.apache.drill.exec.util.DecimalUtility.INTEGER_SIZE);
    buffer = buffer.reallocIfNeeded(size);
  </#if>
  }

  public void eval() {
    out.scale = scale.value;
    out.precision = precision.value;
    java.math.BigDecimal bd =
        org.apache.drill.exec.util.DecimalUtility
            .getBigDecimalFromDrillBuf(in.buffer, in.start, in.end - in.start, in.scale)
                .setScale(scale.value, java.math.RoundingMode.HALF_UP)
                .round(new java.math.MathContext(precision.value, java.math.RoundingMode.HALF_UP));

  <#if type.to.endsWith("Decimal9")>
    out.value = org.apache.drill.exec.util.DecimalUtility.getDecimal9FromBigDecimal(bd, out.scale);
  <#elseif type.to.endsWith("Decimal18")>
    out.value = org.apache.drill.exec.util.DecimalUtility.getDecimal18FromBigDecimal(bd, out.scale);
  <#elseif type.to.endsWith("Sparse")>
    out.start = 0;
    out.buffer = buffer;
    org.apache.drill.exec.util.DecimalUtility
        .getSparseFromBigDecimal(bd, out.buffer, out.start, out.scale, out.nDecimalDigits);
  </#if>
  }
}
</#if>
</#list>
