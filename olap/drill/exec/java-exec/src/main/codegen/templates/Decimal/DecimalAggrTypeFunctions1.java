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

<#list decimalaggrtypes1.aggrtypes as aggrtype>
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/gaggr/Decimal${aggrtype.className}Functions.java" />

<#include "/@includes/license.ftl" />

<#-- A utility class that is used to generate java code for aggr functions for decimal data type that maintain a single -->
<#-- running counter to hold the result. This includes: MIN, MAX, SUM, $SUM0. -->

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
import org.apache.drill.exec.vector.complex.writer.*;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.*;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import io.netty.buffer.ByteBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")

public class Decimal${aggrtype.className}Functions {

  <#list aggrtype.types as type>
  <#if aggrtype.funcName.contains("sum")>
  @FunctionTemplate(name = "${aggrtype.funcName}",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_SUM_AGGREGATE)
  public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {
    @Param ${type.inputType}Holder in;
    @Inject DrillBuf buffer;
    @Workspace ObjectHolder value;
    @Workspace IntHolder outputScale;
    @Output ${type.outputType}Holder out;
    @Workspace BigIntHolder nonNullCount;

    public void setup() {
      value = new ObjectHolder();
      value.obj = java.math.BigDecimal.ZERO;
      outputScale = new IntHolder();
      outputScale.value = Integer.MIN_VALUE;
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
      java.math.BigDecimal currentValue = org.apache.drill.exec.util.DecimalUtility
          .getBigDecimalFromDrillBuf(in.buffer, in.start, in.end - in.start, in.scale);
      value.obj = ((java.math.BigDecimal) value.obj).add(currentValue);
      if (outputScale.value == Integer.MIN_VALUE) {
        outputScale.value = in.scale;
      }
      org.apache.drill.exec.util.DecimalUtility.checkValueOverflow((java.math.BigDecimal) value.obj,
          org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision(), outputScale.value);
      <#if type.inputType?starts_with("Nullable")>
      } // end of sout block
      </#if>
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.isSet = 1;
        out.start  = 0;
        out.scale = outputScale.value;
        out.precision =
            org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision();
        value.obj = ((java.math.BigDecimal) value.obj).setScale(out.scale, java.math.BigDecimal.ROUND_HALF_UP);
        byte[] bytes = ((java.math.BigDecimal) value.obj).unscaledValue().toByteArray();
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
      outputScale = new IntHolder();
      outputScale.value = Integer.MIN_VALUE;
      nonNullCount.value = 0;
    }
  }
  <#elseif aggrtype.funcName.contains("any_value") || aggrtype.funcName.contains("single_value")>
  @FunctionTemplate(name = "${aggrtype.funcName}",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_AGGREGATE)
  public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {
    @Param ${type.inputType}Holder in;
    @Inject DrillBuf buffer;
    @Workspace ObjectHolder value;
    @Workspace IntHolder scale;
    @Workspace IntHolder precision;
    @Output ${type.outputType}Holder out;
    @Workspace BigIntHolder nonNullCount;

    public void setup() {
      value = new ObjectHolder();
      value.obj = java.math.BigDecimal.ZERO;
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
      if (nonNullCount.value == 0) {
        value.obj=org.apache.drill.exec.util.DecimalUtility
            .getBigDecimalFromDrillBuf(in.buffer,in.start,in.end-in.start,in.scale);
        scale.value = in.scale;
        precision.value = in.precision;
      <#if aggrtype.funcName.contains("single_value")>
      } else {
        throw org.apache.drill.common.exceptions.UserException.functionError()
            .message("Input for single_value function has more than one row")
            .build();
      </#if>
      }
      nonNullCount.value = 1;
      <#if type.inputType?starts_with("Nullable")>
      } // end of sout block
      </#if>
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.isSet = 1;
        byte[] bytes = ((java.math.BigDecimal)value.obj).unscaledValue().toByteArray();
        int len = bytes.length;
        out.start  = 0;
        out.buffer = buffer = buffer.reallocIfNeeded(len);
        out.buffer.setBytes(0, bytes);
        out.end = len;
        out.scale = scale.value;
        out.precision = precision.value;
    } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      scale.value = 0;
      precision.value = 0;
      value.obj = null;
      nonNullCount.value = 0;
    }
  }
  <#elseif aggrtype.funcName == "max" || aggrtype.funcName == "min">

  @FunctionTemplate(name = "${aggrtype.funcName}",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_AGGREGATE)
  public static class ${type.inputType}${aggrtype.className} implements DrillAggFunc {
    @Param ${type.inputType}Holder in;
    @Inject DrillBuf buffer;
    @Workspace IntHolder scale;
    @Workspace IntHolder precision;
    @Workspace ObjectHolder tempResult;
    @Output ${type.outputType}Holder out;
    @Workspace BigIntHolder nonNullCount;
    @Inject DrillBuf buf;

    public void setup() {
      tempResult = new ObjectHolder();
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
      org.apache.drill.exec.expr.fn.impl.DrillByteArray tmp = (org.apache.drill.exec.expr.fn.impl.DrillByteArray) tempResult.obj;
      <#if aggrtype.funcName == "max">
      int cmp = 0;
      if (tmp != null) {
        cmp = org.apache.drill.exec.util.DecimalUtility
            .compareVarLenBytes(in.buffer, in.start, in.end, in.scale,
                tmp.getBytes(), scale.value, false);
      } else {
        cmp = 1;
        tmp = new org.apache.drill.exec.expr.fn.impl.DrillByteArray();
        tempResult.obj = tmp;
      }

      if (cmp > 0) {
        int inputLength = in.end - in.start;
        if (tmp.getLength() >= inputLength) {
          in.buffer.getBytes(in.start, tmp.getBytes(), 0, inputLength);
          tmp.setLength(inputLength);
        } else {
          byte[] tempArray = new byte[in.end - in.start];
          in.buffer.getBytes(in.start, tempArray, 0, in.end - in.start);
          tmp.setBytes(tempArray);
        }
        scale.value = in.scale;
        precision.value = in.precision;
      }
      <#elseif aggrtype.funcName == "min">
      int cmp = 0;
      if (tmp != null) {
        cmp = org.apache.drill.exec.util.DecimalUtility
            .compareVarLenBytes(in.buffer, in.start, in.end, in.scale,
                tmp.getBytes(), scale.value, false);
      } else {
        cmp = -1;
        tmp = new org.apache.drill.exec.expr.fn.impl.DrillByteArray();
        tempResult.obj = tmp;
      }

      if (cmp < 0) {
        int inputLength = in.end - in.start;
        if (tmp.getLength() >= inputLength) {
          in.buffer.getBytes(in.start, tmp.getBytes(), 0, inputLength);
          tmp.setLength(inputLength);
        } else {
        byte[] tempArray = new byte[in.end - in.start];
          in.buffer.getBytes(in.start, tempArray, 0, in.end - in.start);
          tmp.setBytes(tempArray);
        }
        scale.value = in.scale;
        precision.value = in.precision;
      }
      </#if>
      <#if type.inputType?starts_with("Nullable")>
      } // end of sout block
      </#if>
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.isSet = 1;
        org.apache.drill.exec.expr.fn.impl.DrillByteArray tmp = (org.apache.drill.exec.expr.fn.impl.DrillByteArray) tempResult.obj;
        buf = buf.reallocIfNeeded(tmp.getLength());
        buf.setBytes(0, tmp.getBytes(), 0, tmp.getLength());
        out.start = 0;
        out.end = tmp.getLength();
        out.buffer = buf;

        out.scale = scale.value;
        out.precision = precision.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      scale.value = 0;
      precision.value = 0;
      tempResult.obj = null;
      nonNullCount.value = 0;
    }
  }
  </#if>
</#list>
}
</#list>
