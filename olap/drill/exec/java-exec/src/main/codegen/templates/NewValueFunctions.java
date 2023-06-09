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

<#macro reassignHolder>
        previous.buffer = buf = buf.reallocIfNeeded(length);
        previous.buffer.setBytes(0, in.buffer, in.start, length);
        previous.end = length;
</#macro>


<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/GNewValueFunctions.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.record.RecordBatch;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
public class GNewValueFunctions {
<#list vv.types as type>
<#list type.minor as minor>
<#list vv.modes as mode>
  <#if mode.name != "Repeated">

<#if !minor.class.startsWith("Decimal28") && !minor.class.startsWith("Decimal38") && !minor.class.startsWith("Interval")>
@SuppressWarnings("unused")
@FunctionTemplate(name = "newPartitionValue", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.INTERNAL)
public static class NewValue${minor.class}${mode.prefix} implements DrillSimpleFunc {

  @Param ${mode.prefix}${minor.class}Holder in;
  @Workspace ${mode.prefix}${minor.class}Holder previous;
  @Workspace Boolean initialized;
  @Output BitHolder out;
  <#if type.major == "VarLen">
  @Inject DrillBuf buf;
  </#if>

  public void setup() {
    initialized = false;
    <#if type.major == "VarLen">
    previous.buffer = buf;
    previous.start = 0;
    </#if>
  }

  public void eval() {
  <#if mode.name == "Required">
  <#if type.major == "VarLen">
    int length = in.end - in.start;

    if (initialized) {
      if (org.apache.drill.exec.expr.fn.impl.ByteFunctionHelpers.compare(
          previous.buffer, 0, previous.end, in.buffer, in.start, in.end) == 0) {
        out.value = 0;
      } else {
        <@reassignHolder/>
        out.value = 1;
      }
    } else {
      <@reassignHolder/>
      out.value = 1;
      initialized = true;
    }
  </#if>
  <#if type.major == "Fixed" || type.major == "Bit">
    if (initialized) {
      if (in.value == previous.value) {
        out.value = 0;
      } else {
        previous.value = in.value;
        out.value = 1;
      }
    } else {
      previous.value = in.value;
      out.value = 1;
      initialized = true;
    }
  </#if>
  </#if> <#-- mode.name == "Required" -->

  <#if mode.name == "Optional">
  <#if type.major == "VarLen">
    int length = in.isSet == 0 ? 0 : in.end - in.start;

    if (initialized) {
      if (previous.isSet == 0 && in.isSet == 0) {
        out.value = 0;
      } else if (previous.isSet != 0 && in.isSet != 0 && org.apache.drill.exec.expr.fn.impl.ByteFunctionHelpers.compare(
          previous.buffer, 0, previous.end, in.buffer, in.start, in.end) == 0) {
        out.value = 0;
      } else {
        if (in.isSet == 1) {
          <@reassignHolder/>
        }
        previous.isSet = in.isSet;
        out.value = 1;
      }
    } else {
      if (in.isSet == 1) {
        <@reassignHolder/>
      }
      previous.isSet = in.isSet;
      out.value = 1;
      initialized = true;
    }
  </#if>
  <#if type.major == "Fixed" || type.major == "Bit">
    if (initialized) {
      if (in.isSet == 0 && previous.isSet == 0) {
        out.value = 0;
      } else if (in.value == previous.value) {
        out.value = 0;
      } else {
        previous.value = in.value;
        previous.isSet = in.isSet;
        out.value = 1;
      }
    } else {
      previous.value = in.value;
      previous.isSet = in.isSet;
      out.value = 1;
      initialized = true;
    }
  </#if>
  </#if> <#-- mode.name == "Optional" -->
  }
}
</#if> <#-- minor.class.startWith -->

</#if> <#-- mode.name -->
</#list>
</#list>
</#list>
}
