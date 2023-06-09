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



<#list aggrtypes2.aggrtypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/${aggrtype.className}Functions.java" />

<#include "/@includes/license.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

<#-- A utility class that is used to generate java code for aggr functions that maintain a sum -->
<#-- and a running count.  For now, this includes: AVG. -->


package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;

@SuppressWarnings("unused")

public class ${aggrtype.className}Functions {
	static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(${aggrtype.className}Functions.class);
 
<#list aggrtype.types as type>
<#if type.major == "Numeric">

@FunctionTemplate(name = "${aggrtype.funcName}", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)

public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc{

  @Param ${type.inputType}Holder in;
  @Workspace ${type.sumRunningType}Holder sum;
  @Workspace ${type.countRunningType}Holder count;
  @Workspace BigIntHolder nonNullCount;
  @Output ${type.outputType}Holder out;

  public void setup() {
  	sum = new ${type.sumRunningType}Holder();  
    count = new ${type.countRunningType}Holder();  
  	nonNullCount = new BigIntHolder();
  	nonNullCount.value = 0;
    sum.value = 0;
    count.value = 0;
  }
  
  @Override
  public void add() {
	  <#if type.inputType?starts_with("Nullable")>
	    sout: {
	    if (in.isSet == 0) {
		    // processing nullable input and the value is null, so don't do anything...
		    break sout;
	    } 
	  </#if>
    nonNullCount.value = 1;
	  <#if aggrtype.funcName == "avg">
 	    sum.value += in.value;
 	    count.value++;
	  <#else>
	  // TODO: throw an error ? 
	  </#if>
	<#if type.inputType?starts_with("Nullable")>
    } // end of sout block
	</#if>
  }

  @Override
  public void output() {
    if (nonNullCount.value > 0) {
      out.value = sum.value / ((double) count.value);
      out.isSet = 1;
    } else {
      out.isSet = 0;
    }
  }

  @Override
  public void reset() {
    nonNullCount.value = 0;
    sum.value = 0;
    count.value = 0;
  }

 }

</#if>
</#list>
}
</#list>

