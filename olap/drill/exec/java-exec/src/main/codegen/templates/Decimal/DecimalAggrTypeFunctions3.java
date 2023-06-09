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

<#list decimalaggrtypes3.aggrtypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/Decimal${aggrtype.className}Functions.java" />

<#include "/@includes/license.ftl" />

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

<#-- A utility class that is used to generate java code for aggr functions such as stddev, variance -->

package org.apache.drill.exec.expr.fn.impl.gaggr;

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.*;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;

@SuppressWarnings("unused")

public class Decimal${aggrtype.className}Functions {
<#list aggrtype.types as type>

  <#if aggrtype.aliasName == "">
  @FunctionTemplate(name = "${aggrtype.funcName}",
  <#else>
  @FunctionTemplate(names = {"${aggrtype.funcName}", "${aggrtype.aliasName}"},
  </#if>
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_AVG_AGGREGATE)
  public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {
    @Param ${type.inputType}Holder in;
    @Inject DrillBuf buffer;
    @Workspace ObjectHolder avg;
    @Workspace ObjectHolder dev;
    @Workspace BigIntHolder count;
    @Workspace IntHolder scale;
    @Output ${type.outputType}Holder out;
    @Workspace BigIntHolder nonNullCount;

    public void setup() {
      avg = new ObjectHolder();
      dev = new ObjectHolder();
      count = new BigIntHolder();
      scale = new IntHolder();
      scale.value = Integer.MIN_VALUE;
      // Initialize the workspace variables
      avg.obj = java.math.BigDecimal.ZERO;
      dev.obj = java.math.BigDecimal.ZERO;
      count.value = 1;
      nonNullCount = new BigIntHolder();
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

      if (scale.value == Integer.MIN_VALUE) {
        scale.value = Math.max(in.scale, 6);
      }

      // Welford's approach to compute standard deviation
      // avg.value += ((in.value - temp) / count.value);
      // dev.value += (in.value - temp) * (in.value - avg.value);
      java.math.BigDecimal temp = (java.math.BigDecimal) avg.obj;
      java.math.BigDecimal input = org.apache.drill.exec.util.DecimalUtility
          .getBigDecimalFromDrillBuf(in.buffer, in.start, in.end - in.start, in.scale);
      avg.obj = ((java.math.BigDecimal) avg.obj)
          .add(input.subtract(temp)
                  .divide(java.math.BigDecimal.valueOf(count.value),
                      new java.math.MathContext(
                          org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision(),
                          java.math.RoundingMode.HALF_UP)));
      dev.obj = ((java.math.BigDecimal) dev.obj)
          .add(input.subtract(temp).multiply(input.subtract(((java.math.BigDecimal) avg.obj))));
      count.value++;
    <#if type.inputType?starts_with("Nullable")>
      } // end of sout block
    </#if>
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.isSet = 1;
      <#if aggrtype.funcName == "stddev_pop">
        if (count.value > 1) {
          // out.value = Math.sqrt((dev.value / (count.value - 1)));
          java.math.BigDecimal result =
              org.apache.drill.exec.util.DecimalUtility.sqrt(((java.math.BigDecimal) dev.obj)
                  .setScale(scale.value, java.math.RoundingMode.HALF_UP)
                  .divide(java.math.BigDecimal.valueOf(count.value - 1),
                      java.math.RoundingMode.HALF_UP),
                  scale.value);
      <#elseif aggrtype.funcName == "var_pop">
        if (count.value  > 1) {
          // out.value = (dev.value / (count.value - 1));
          java.math.BigDecimal result =
              ((java.math.BigDecimal) dev.obj)
                  .setScale(scale.value, java.math.RoundingMode.HALF_UP)
                  .divide(java.math.BigDecimal.valueOf(count.value - 1),
                      java.math.RoundingMode.HALF_UP);
      <#elseif aggrtype.funcName == "stddev_samp">
        if (count.value  > 2) {
          // out.value = Math.sqrt((dev.value / (count.value - 2)));
          java.math.BigDecimal result =
              org.apache.drill.exec.util.DecimalUtility.sqrt(((java.math.BigDecimal) dev.obj)
                  .setScale(scale.value, java.math.RoundingMode.HALF_UP)
                  .divide(java.math.BigDecimal.valueOf(count.value - 2),
                      java.math.RoundingMode.HALF_UP),
                  scale.value);
      <#elseif aggrtype.funcName == "var_samp">
        if (count.value > 2) {
          // out.value = (dev.value / (count.value - 2));
          java.math.BigDecimal result =
              ((java.math.BigDecimal) dev.obj)
                  .setScale(scale.value, java.math.RoundingMode.HALF_UP)
                  .divide(java.math.BigDecimal.valueOf(count.value - 2),
                      java.math.RoundingMode.HALF_UP);
      </#if>
          out.scale = scale.value;
          result = result.setScale(out.scale, java.math.RoundingMode.HALF_UP);
          out.start  = 0;
          out.precision = org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision();
          org.apache.drill.exec.util.DecimalUtility.checkValueOverflow(result, out.precision, out.scale);
          byte[] bytes = result.unscaledValue().toByteArray();
          int len = bytes.length;
          out.buffer = buffer = buffer.reallocIfNeeded(len);
          out.buffer.setBytes(0, bytes);
          out.end = len;
        } else {
          out.start = 0;
          out.end = 0;
          out.buffer = buffer;
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      avg.obj = java.math.BigDecimal.ZERO;
      dev.obj = java.math.BigDecimal.ZERO;
      count.value = 1;
      nonNullCount.value = 0;
    }
  }

</#list>
}
</#list>
