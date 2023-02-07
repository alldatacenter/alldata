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




<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/SumZeroFunctions.java" />

<#include "/@includes/license.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;

@SuppressWarnings("unused")

public class SumZeroFunctions {
	static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SumZeroFunctions.class);

	<#list sumzero.types as type>

	@FunctionTemplate(name = "$sum0", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
	public static class ${type.inputType}SumZero implements DrillAggFunc{

  @Param ${type.inputType}Holder in;
  @Workspace ${type.runningType}Holder value;
  @Output ${type.outputType}Holder out;
  
  
  public void setup() {
    value.value = 0;
  }
  
  @Override
  public void add() {
    value.value += in.value;
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

