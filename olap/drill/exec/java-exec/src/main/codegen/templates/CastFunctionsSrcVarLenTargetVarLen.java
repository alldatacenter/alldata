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
<#if type.major == "SrcVarlenTargetVarlen">

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gcast/Cast${type.from}${type.to}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl.gcast;

import io.netty.buffer.ByteBuf;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")
@FunctionTemplate(name = "cast${type.to?upper_case}",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    <#if type.to == 'VarChar'>returnType = FunctionTemplate.ReturnType.STRING_CAST,</#if>
    nulls = NullHandling.NULL_IF_NULL)
public class Cast${type.from}${type.to} implements DrillSimpleFunc{

  @Param ${type.from}Holder in;
  @Param BigIntHolder length;
  @Output ${type.to}Holder out;

  public void setup() {
  }

  public void eval() {

  <#if type.to == 'VarChar'>

    //Do 1st scan to counter # of character in string.
    int charCount = org.apache.drill.exec.expr.fn.impl.StringFunctionUtil.getUTF8CharLength(in.buffer, in.start, in.end);

    //if input length <= target_type length, do nothing
    //else if target length = 0, it means cast wants all the characters in the input. Do nothing.
    //else truncate based on target_type length.
    out.buffer = in.buffer;
    out.start =  in.start;
    if (charCount <= length.value || length.value == 0 ) {
      out.end = in.end;
    } else {
      out.end = org.apache.drill.exec.expr.fn.impl.StringFunctionUtil.getUTF8CharPosition(in.buffer, in.start, in.end, (int)length.value);
    }

  <#elseif type.to == 'VarBinary'>

    //if input length <= target_type length, do nothing
    //else if target length = 0, it means cast wants all the bytes in the input. Do nothing.
    //else truncate based on target_type length.
    out.buffer = in.buffer;
    out.start =  in.start;
    if (in.end - in.start <= length.value || length.value == 0 ) {
      out.end = in.end;
    } else {
      out.end = out.start + (int) length.value;
    }
  </#if>

  }      
}

</#if> <#-- type.major -->
</#list>

