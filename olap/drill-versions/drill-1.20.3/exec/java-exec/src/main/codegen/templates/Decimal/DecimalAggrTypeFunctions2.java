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
import org.apache.drill.exec.expr.annotations.Workspace;

<@pp.dropOutputFile />



<#list decimalaggrtypes2.aggrtypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/Decimal${aggrtype.className}Functions.java" />

<#include "/@includes/license.ftl" />

<#-- A utility class that is used to generate java code for aggr functions for decimal data type that maintain a single -->
<#-- running counter to hold the result. This includes: AVG. -->

/*
 * This class is automatically generated from AggrTypeFunctions1.tdd using FreeMarker.
 */

package org.apache.drill.exec.expr.fn.impl.gaggr;

<#include "/@includes/vv_imports.ftl" />

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.record.RecordBatch;
import io.netty.buffer.ByteBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")

public class Decimal${aggrtype.className}Functions {
<#list aggrtype.types as type>

@FunctionTemplate(name = "${aggrtype.funcName}",
                  scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                  returnType = FunctionTemplate.ReturnType.DECIMAL_AVG_AGGREGATE)
public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {

  @Param ${type.inputType}Holder in;
  @Inject DrillBuf buffer;
  @Workspace ObjectHolder value;
  @Workspace ${type.countRunningType}Holder count;
  @Workspace IntHolder outputScale;
  @Output ${type.outputType}Holder out;

  public void setup() {
    value = new ObjectHolder();
    value.obj = java.math.BigDecimal.ZERO;
    count = new ${type.countRunningType}Holder();
    count.value = 0;
    outputScale = new IntHolder();
    outputScale.value = Integer.MIN_VALUE;
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
    count.value++;
    java.math.BigDecimal currentValue = org.apache.drill.exec.util.DecimalUtility
        .getBigDecimalFromDrillBuf(in.buffer, in.start, in.end - in.start, in.scale);
    value.obj = ((java.math.BigDecimal)(value.obj)).add(currentValue);
    if (outputScale.value == Integer.MIN_VALUE) {
      outputScale.value = in.scale;
    }
	<#if type.inputType?starts_with("Nullable")>
    } // end of sout block
	</#if>
  }

  @Override
  public void output() {
    if (count.value > 0) {
      out.isSet = 1;
      out.start  = 0;
      out.scale = Math.max(outputScale.value, 6);
      java.math.BigDecimal average = ((java.math.BigDecimal) value.obj)
            .divide(java.math.BigDecimal.valueOf(count.value), out.scale, java.math.BigDecimal.ROUND_HALF_UP);
      out.precision = org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision();
      byte[] bytes = average.unscaledValue().toByteArray();
      int len = bytes.length;
      out.buffer = buffer = buffer.reallocIfNeeded(len);
      out.buffer.setBytes(0, bytes);
      out.end = len;
    } else {
      out.isSet = 0;
    }
  }

  @Override
  public void reset() {
    value = new ObjectHolder();
    value.obj = java.math.BigDecimal.ZERO;
    count = new ${type.countRunningType}Holder();
    count.value = 0;
    outputScale = new IntHolder();
    outputScale.value = Integer.MIN_VALUE;
  }
}

</#list>
}
</#list>

