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



<#list aggrtypes1.aggrtypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/${aggrtype.className}ComplexFunctions.java" />

<#include "/@includes/license.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

<#-- A utility class that is used to generate java code for aggr functions that maintain a single -->
<#-- running counter to hold the result.  This includes: ANY_VALUE. -->

package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.MapUtility;
import org.apache.drill.exec.vector.complex.writer.*;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.*;

@SuppressWarnings("unused")

public class ${aggrtype.className}ComplexFunctions {

<#list aggrtype.types as type>
<#if type.major == "Complex">

  @FunctionTemplate(name = "${aggrtype.funcName}",
                  <#if type.major == "VarDecimal">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_AVG_AGGREGATE,
                  </#if>
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {
    @Param ${type.inputType}Holder inHolder;
    @Workspace BigIntHolder nonNullCount;
    @Output ComplexWriter writer;

    public void setup() {
      nonNullCount = new BigIntHolder();
      nonNullCount.value = 0;
    }

    @Override
    public void add() {
    <#if type.inputType?starts_with("Nullable")>
      sout: {
      if (inHolder.isSet == 0) {
      // processing nullable input and the value is null, so don't do anything...
      break sout;
      }
    </#if>
    <#if aggrtype.funcName == "single_value">
      if (nonNullCount.value > 0) {
        throw org.apache.drill.common.exceptions.UserException.functionError()
            .message("Input for single_value function has more than one row")
            .build();
      }
    </#if>
    <#if aggrtype.funcName == "any_value" || aggrtype.funcName == "single_value">
      <#if type.runningType?starts_with("Map")>
      if (nonNullCount.value == 0) {
        org.apache.drill.exec.expr.fn.impl.MappifyUtility.createMap(inHolder.reader, writer, "${aggrtype.funcName}");
      }
      <#elseif type.runningType?starts_with("RepeatedMap")>
      if (nonNullCount.value == 0) {
        org.apache.drill.exec.expr.fn.impl.MappifyUtility.createRepeatedMapOrList(inHolder.reader, writer, "${aggrtype.funcName}");
      }
      <#elseif type.runningType?starts_with("List")>
      if (nonNullCount.value == 0) {
        org.apache.drill.exec.expr.fn.impl.MappifyUtility.createList(inHolder.reader, writer, "${aggrtype.funcName}");
      }
      <#elseif type.runningType?starts_with("RepeatedList")>
      if (nonNullCount.value == 0) {
        org.apache.drill.exec.expr.fn.impl.MappifyUtility.createRepeatedMapOrList(inHolder.reader, writer, "${aggrtype.funcName}");
      }
      <#elseif type.runningType?starts_with("Repeated")>
      if (nonNullCount.value == 0) {
        org.apache.drill.exec.expr.fn.impl.MappifyUtility.createList(inHolder.reader, writer, "${aggrtype.funcName}");
      }
      </#if>
    </#if>
      nonNullCount.value = 1;
    <#if type.inputType?starts_with("Nullable")>
      } // end of sout block
    </#if>
    }

    @Override
    public void output() {
      //Do nothing since the complex writer takes care of everything!
    }

    @Override
    public void reset() {
    <#if aggrtype.funcName == "any_value" || aggrtype.funcName == "single_value">
      nonNullCount.value = 0;
    </#if>
    }
  }
</#if>
</#list>
}
</#list>