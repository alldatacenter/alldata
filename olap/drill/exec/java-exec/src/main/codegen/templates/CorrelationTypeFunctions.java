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



<#list corrTypes.correlationTypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/${aggrtype.className}Functions.java" />

<#include "/@includes/license.ftl" />

<#-- A utility class that is used to generate java code for corr/correlation aggr functions -->

/*
 * This class is automatically generated from CorrelationTypes.tdd using FreeMarker.
 */

package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
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
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.expr.holders.NullableFloat8Holder;
import org.apache.drill.exec.expr.holders.NullableFloat4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.Float4Holder;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")

public class ${aggrtype.className}Functions {
	static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(${aggrtype.className}Functions.class);

<#list aggrtype.types as type>

@FunctionTemplate(names = {"${aggrtype.funcName}", "${aggrtype.aliasName}"}, scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc{

  @Param ${type.inputType}Holder xIn;
  @Param ${type.inputType}Holder yIn;

  @Workspace ${type.movingAverageType}Holder xMean;
  @Workspace ${type.movingAverageType}Holder yMean;
  @Workspace ${type.movingAverageType}Holder xyMean;

  @Workspace ${type.movingAverageType}Holder xDev;
  @Workspace ${type.movingAverageType}Holder yDev;

  @Workspace ${type.movingDeviationType}Holder covar;

  @Workspace ${type.countRunningType}Holder count;
  @Output ${type.outputType}Holder out;

  public void setup() {
    xMean = new ${type.movingAverageType}Holder();
    yMean = new ${type.movingAverageType}Holder();
    xyMean = new ${type.movingDeviationType}Holder();
    xDev = new ${type.movingDeviationType}Holder();
    yDev = new ${type.movingDeviationType}Holder();
    count = new ${type.countRunningType}Holder();
    covar = new ${type.movingDeviationType}Holder();

    // Initialize the workspace variables
    xMean.value = 0;
    yMean.value = 0;
    xyMean.value = 0;
    xDev.value = 0;
    yDev.value = 0;
    count.value = 1;
    covar.value = 0;
  }

  @Override
  public void add() {
	<#if type.inputType?starts_with("Nullable")>
	  sout: {
	  if (xIn.isSet == 0 || yIn.isSet == 0) {
	   // processing nullable input and the value is null, so don't do anything...
	   break sout;
	  }
	</#if>

    // compute covariance
	double xOldMean = xMean.value, yOldMean = yMean.value;

    xMean.value += ((xIn.value - xMean.value) / count.value);
    yMean.value += ((yIn.value - yMean.value) / count.value);

    xDev.value += (xIn.value - xOldMean) * (xIn.value - xMean.value);
    yDev.value += (yIn.value - yOldMean) * (yIn.value - yMean.value);

    xyMean.value += ((xIn.value * yIn.value) - xyMean.value) / count.value;
    count.value++;
    <#if type.inputType?starts_with("Nullable")>
    } // end of sout block
    </#if>
  }

  @Override
  public void output() {
    double xVariance = (xDev.value / (count.value - 1));
    double yVariance = (yDev.value / (count.value - 1));
    double xyCovariance = (xyMean.value - (xMean.value * yMean.value));

    out.value = xyCovariance / Math.sqrt((xVariance * yVariance));
  }

  @Override
  public void reset() {
    xMean.value = 0;
    yMean.value = 0;
    xyMean.value = 0;
    count.value = 1;
    covar.value = 0;
  }
}


</#list>
}
</#list>