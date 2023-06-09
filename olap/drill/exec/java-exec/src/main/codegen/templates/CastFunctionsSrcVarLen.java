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

<#macro doError>
  { 
    byte[] buf = new byte[in.end - in.start];
    in.buffer.getBytes(in.start, buf, 0, in.end - in.start);  
    throw new NumberFormatException(new String(buf, com.google.common.base.Charsets.UTF_8));
  }  
</#macro>

<#list cast.types as type>
<#if type.major == "SrcVarlen">

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}${type.to}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
@FunctionTemplate(name = "cast${type.to?upper_case}", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
public class Cast${type.from}${type.to} implements DrillSimpleFunc{

  @Param ${type.from}Holder in;
  @Output ${type.to}Holder out;

  public void setup() {}

  public void eval() {
    <#if type.to == "Float4" || type.to == "Float8">
      
      byte[] buf = new byte[in.end - in.start];
      in.buffer.getBytes(in.start, buf, 0, in.end - in.start);
    
      //TODO: need capture format exception, and issue SQLERR code.
      out.value = ${type.javaType}.parse${type.parse}(new String(buf, com.google.common.base.Charsets.UTF_8));
      
    <#elseif type.to=="Int" >
      out.value = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.varTypesToInt(in.start, in.end, in.buffer);
    
    <#elseif type.to == "BigInt">
      out.value = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.varTypesToLong(in.start, in.end, in.buffer);
    </#if>
  }
}

</#if> <#-- type.major -->
</#list>

