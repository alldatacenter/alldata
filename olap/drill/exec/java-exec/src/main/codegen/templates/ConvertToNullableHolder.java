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
<#list vv.types as type>
<#list type.minor as minor>

<#assign className="GConvertToNullable${minor.class}Holder" />

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/${className}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.*;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@FunctionTemplate(name = "convertToNullable${minor.class?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    <#if minor.class.startsWith("Decimal")>
    returnType = FunctionTemplate.ReturnType.DECIMAL_MAX_SCALE,
    <#elseif minor.class.startsWith("Var")>
    returnType = FunctionTemplate.ReturnType.SAME_IN_OUT_LENGTH,
    </#if>
    nulls = FunctionTemplate.NullHandling.INTERNAL,
    isInternal = true)
public class ${className} implements DrillSimpleFunc {

  @Param ${minor.class}Holder input;
  @Output Nullable${minor.class}Holder output;

  public void setup() { }

  public void eval() {
    output.isSet = 1;
<#if type.major != "VarLen">
  <#if (minor.class == "Interval")>
    output.months = input.months;
    output.days = input.days;
    output.milliseconds = input.milliseconds;
  <#elseif (minor.class == "IntervalDay")>
    output.days = input.days;
    output.milliseconds = input.milliseconds;
  <#elseif minor.class.startsWith("Decimal")>
    output.scale = input.scale;
    output.precision = input.precision;
    <#if minor.class.startsWith("Decimal28") || minor.class.startsWith("Decimal38")>
    output.setSign(input.getSign(input.start, input.buffer), output.start, output.buffer);
    output.start = input.start;
    output.buffer = input.buffer;
    <#else>
    output.value = input.value;
    </#if>
  <#elseif (type.width > 8)>
    output.start = input.start;
    output.buffer = input.buffer;
  <#else>
    output.value = input.value;
  </#if>
<#else>
    output.start = input.start;
    output.end = input.end;
    output.buffer = input.buffer;
</#if>
  }
}
</#list>
</#list>