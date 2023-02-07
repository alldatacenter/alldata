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
<#list vv.modes as mode>
<#list vv.types as type>
<#list type.minor as minor>

<#assign className="GNullOp${mode.prefix}${minor.class}Holder" />

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
public class ${className} {

  @FunctionTemplate(names = {"isnull", "is null"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class IsNull implements DrillSimpleFunc {

    @Param ${mode.prefix}${minor.class}Holder input;
    @Output BitHolder out;

    public void setup() { }

    public void eval() {
    <#if mode.name == "Optional">
      out.value = (input.isSet == 0 ? 1 : 0);
    <#else>
      out.value = 0;
    </#if>
    }
  }

  @FunctionTemplate(names = {"isnotnull", "is not null"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class IsNotNull implements DrillSimpleFunc {

    @Param ${mode.prefix}${minor.class}Holder input;
    @Output BitHolder out;

    public void setup() { }

    public void eval() {
    <#if mode.name == "Optional">
      out.value = (input.isSet == 0 ? 0 : 1);
    <#else>
      out.value = 1;
    </#if>
    }
  }
}

</#list>
</#list>
</#list>