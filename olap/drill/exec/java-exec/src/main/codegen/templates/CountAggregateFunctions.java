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
import java.lang.Override;

<@pp.dropOutputFile />

<#-- A utility class that is used to generate java code for count aggregate functions -->

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/CountFunctions.java" />

<#include "/@includes/license.ftl" />


package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")

public class CountFunctions {
  <#list countAggrTypes.countFunctionsInput as inputType>
  @FunctionTemplate(name = "count", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class ${inputType}CountFunction implements DrillAggFunc {

    @Param ${inputType}Holder in;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    @Override
    public void setup() {
	    value = new BigIntHolder();
      value.value = 0;
    }

    @Override
    public void add() {
      <#if inputType?starts_with("Nullable")>
        if (in.isSet == 1) {
          value.value++;
        }
      <#else>
        value.value++;
      </#if>
    }

    @Override
    public void output() {
      out.value = value.value;
    }

    @Override
    public void reset() {
      value.value = 0;
    }
  }
  </#list>
}