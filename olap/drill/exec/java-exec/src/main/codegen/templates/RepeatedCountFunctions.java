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

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/RepeatedCountFunctions.java" />

<#include "/@includes/license.ftl" />
package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
public class RepeatedCountFunctions {

  private RepeatedCountFunctions() {
  }

  <#list countAggrTypes.countFunctionsInput as type>
    <#if type?starts_with("Repeated")>
  @FunctionTemplate(name = "repeated_count", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class RepeatedCount${type} implements DrillSimpleFunc {

    @Param
    ${type}Holder input;
    @Output
    IntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      out.value = input.reader.size();
    }
  }

    </#if>
  </#list>
}
