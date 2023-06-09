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



<#list numericTypes.numericFunctions as numericFunc>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/${numericFunc.className}Functions.java" />

<#include "/@includes/license.ftl" />

<#-- A utility class that is used to generate java code for numeric functions -->

/*
 * This class is automatically generated from NumericTypes.tdd using FreeMarker.
 */

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.NullableIntHolder;
import org.apache.drill.exec.expr.holders.SmallIntHolder;
import org.apache.drill.exec.expr.holders.NullableSmallIntHolder;
import org.apache.drill.exec.expr.holders.TinyIntHolder;
import org.apache.drill.exec.expr.holders.NullableTinyIntHolder;
import org.apache.drill.exec.expr.holders.UInt1Holder;
import org.apache.drill.exec.expr.holders.NullableUInt1Holder;
import org.apache.drill.exec.expr.holders.UInt2Holder;
import org.apache.drill.exec.expr.holders.NullableUInt2Holder;
import org.apache.drill.exec.expr.holders.UInt4Holder;
import org.apache.drill.exec.expr.holders.NullableUInt4Holder;
import org.apache.drill.exec.expr.holders.UInt8Holder;
import org.apache.drill.exec.expr.holders.NullableUInt8Holder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.record.RecordBatch;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")

public class ${numericFunc.className}Functions {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(${numericFunc.className}Functions.class);

<#list numericFunc.types as type>

<#if numericFunc.aliasName == "">
@FunctionTemplate(name = "${numericFunc.funcName}", scope = FunctionTemplate.FunctionScope.SIMPLE)
<#else>
@FunctionTemplate(names = {"${numericFunc.funcName}", "${numericFunc.aliasName}"}, scope = FunctionTemplate.FunctionScope.SIMPLE)
</#if>
public static class ${type.inputType}${numericFunc.className} implements DrillSimpleFunc{

  @Param ${type.inputType}Holder in;
  @Output ${numericFunc.outputType}Holder out;
  <#if type.intype != "numeric">
  @Workspace java.util.regex.Pattern pattern;
  @Workspace java.util.regex.Matcher matcher;
  </#if>

 public void setup() {
   <#if type.intype != "numeric">
   pattern = java.util.regex.Pattern.compile("[-+]?\\d+(\\.\\d+)?");
   matcher = pattern.matcher("");
   </#if>
 }

 public void eval() {

 <#if type.intype == "char">
 <#if type.inputType?matches("^Nullable.*")>
    if(in.isSet==0){
      out.value = 0;
      return;
    }
  </#if>
    String s = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer);
    out.value = matcher.reset(s).matches() ? 1 : 0;


  <#elseif type.intype == "numeric">
  <#if type.inputType?matches("^Nullable.*")>
    if(in.isSet==0){
      out.value = 0;
      return;
    }
  </#if>
    out.value = 1;
  </#if>

  }
}


</#list>
}
</#list>
