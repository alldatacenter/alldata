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

<#-- TODO:  Refactor comparison code from here into ComparisonFunctions (to
     eliminate duplicate template code and so that ComparisonFunctions actually
     as all comparison functions. -->

<#macro compareNullsSubblock leftType rightType output breakTarget nullCompare nullComparesHigh>
  <#if nullCompare>
    <#if nullComparesHigh>
      <#assign leftNullResult = 1> <#-- if only L is null and nulls are high, then "L > R" (1) -->
      <#assign rightNullResult = -1>
    <#else>
      <#assign leftNullResult = -1> <#-- if only L is null and nulls are low, then "L < R" (-1) -->
      <#assign rightNullResult = 1>
    </#if>

    <#if leftType?starts_with("Nullable")>
      <#if rightType?starts_with("Nullable")>
        <#-- Both are nullable. -->
        if ( left.isSet == 0 ) {
          if ( right.isSet == 0 ) {
            <#-- Both are null--result is "L = R". -->
            ${output} = 0;
            break ${breakTarget};
          } else {
            <#-- Only left is null--result is "L < R" or "L > R" per null ordering. -->
            ${output} = ${leftNullResult};
            break ${breakTarget};
          }
        } else if ( right.isSet == 0 ) {
          <#-- Only right is null--result is "L > R" or "L < R" per null ordering. -->
          ${output} = ${rightNullResult};
          break ${breakTarget};
        }
      <#else>
        <#-- Left is nullable but right is not. -->
        if ( left.isSet == 0 ) {
          <#-- Only left is null--result is "L < R" or "L > R" per null ordering. -->
          ${output} = ${leftNullResult};
          break ${breakTarget};
        }
      </#if>
    <#elseif rightType?starts_with("Nullable")>
      <#-- Left is not nullable but right is. -->
      if ( right.isSet == 0 ) {
        <#-- Only right is null--result is "L > R" or "L < R" per null ordering. -->
        ${output} = ${rightNullResult};
        break ${breakTarget};
      }
    </#if>
  </#if>

</#macro>


<#macro compareBlock leftType rightType absCompare output nullCompare nullComparesHigh>
         outside:
          {
            <@compareNullsSubblock leftType=leftType rightType=rightType output=output breakTarget="outside" nullCompare=nullCompare nullComparesHigh=nullComparesHigh />

            ${output} = org.apache.drill.exec.util.DecimalUtility.compareSparseBytes(left.buffer, left.start, left.getSign(left.start, left.buffer),
                            left.scale, left.precision, right.buffer,
                            right.start, right.getSign(right.start, right.buffer), right.precision,
                            right.scale, left.WIDTH, left.nDecimalDigits, ${absCompare});

          } // outside
</#macro>

<#macro varCompareBlock leftType rightType absCompare output nullCompare nullComparesHigh>
      outside:
      {
        <@compareNullsSubblock leftType = leftType rightType=rightType output = output breakTarget = "outside" nullCompare = nullCompare nullComparesHigh=nullComparesHigh />

        ${output} = org.apache.drill.exec.util.DecimalUtility.compareVarLenBytes(left.buffer, left.start, left.end, left.scale, right.buffer, right.start, right.end, right.scale, ${absCompare});
      } // outside
</#macro>

<#-- For each DECIMAL... type (in DecimalTypes.tdd) ... -->
<#list comparisonTypesDecimal.decimalTypes as type>

<#if type.name.endsWith("VarDecimal")>

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/${type.name}Functions.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

<#include "/@includes/vv_imports.ftl" />

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public class ${type.name}Functions {

<#list ["Subtract", "Add", "Multiply", "Divide", "Mod"] as functionName>
  @FunctionTemplate(name = "${functionName?lower_case}",
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                  <#if functionName == "Subtract" || functionName == "Add">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_ADD_SCALE,
                  <#elseif functionName == "Multiply">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_SUM_SCALE,
                  <#elseif functionName == "Divide">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_DIV_SCALE,
                  <#elseif functionName == "Mod">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_MOD_SCALE,
                  </#if>
                    nulls = NullHandling.NULL_IF_NULL,
                    checkPrecisionRange = true)
  public static class ${type.name}${functionName}Function implements DrillSimpleFunc {
    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Inject DrillBuf buffer;
    @Output ${type.name}Holder result;

    public void setup() {
    }

    public void eval() {
      result.start = 0;

      java.math.BigDecimal leftInput =
          org.apache.drill.exec.util.DecimalUtility
              .getBigDecimalFromDrillBuf(left.buffer, left.start, left.end - left.start, left.scale);
      java.math.BigDecimal rightInput =
          org.apache.drill.exec.util.DecimalUtility
              .getBigDecimalFromDrillBuf(right.buffer, right.start, right.end - right.start, right.scale);
      org.apache.drill.exec.planner.types.decimal.DrillBaseComputeScalePrecision typeInference =
      <#if functionName == "Subtract" || functionName == "Add">
          new org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionAddFunction(
      <#elseif functionName == "Multiply">
          new org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionMulFunction(
      <#elseif functionName == "Divide">
          new org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionDivideFunction(
      <#elseif functionName == "Mod">
          new org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionModFunction(
      </#if>
              left.precision, left.scale,
              right.precision, right.scale);

      result.scale = typeInference.getOutputScale();
      result.precision = typeInference.getOutputPrecision();

      java.math.BigDecimal opResult =
      <#if functionName == "Subtract" || functionName == "Add"
          || functionName == "Multiply"|| functionName == "Divide">
          leftInput.${functionName?lower_case}(rightInput,
      <#elseif functionName == "Mod">
        leftInput.remainder(rightInput,
      </#if>
              new java.math.MathContext(result.precision, java.math.RoundingMode.HALF_UP))
            .setScale(result.scale, java.math.BigDecimal.ROUND_HALF_UP);

      org.apache.drill.exec.util.DecimalUtility.checkValueOverflow(opResult, result.precision, result.scale);

      byte[] bytes = opResult.unscaledValue().toByteArray();
      int len = bytes.length;
      result.buffer = buffer = buffer.reallocIfNeeded(len);
      result.buffer.setBytes(0, bytes);
      result.end = len;
    }
  }

</#list>

<#list ["Abs", "Ceil", "Floor", "Trunc", "Round", "Negative"] as functionName>
  <#if functionName == "Ceil">
  @FunctionTemplate(names = {"ceil", "ceiling"},
  <#elseif functionName == "Trunc">
  @FunctionTemplate(names = {"trunc", "truncate"},
  <#elseif functionName == "Negative">
  @FunctionTemplate(names = {"negative", "u-", "-"},
  <#else>
  @FunctionTemplate(name = "${functionName?lower_case}",
  </#if>
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                  <#if functionName == "Abs" || functionName == "Negative">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_MAX_SCALE,
                  <#elseif functionName == "Ceil" || functionName == "Floor"
                      || functionName == "Trunc" || functionName == "Round">
                    returnType = FunctionTemplate.ReturnType.DECIMAL_ZERO_SCALE,
                  </#if>
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}${functionName}Function implements DrillSimpleFunc {
    @Param  ${type.name}Holder in;
    @Output ${type.name}Holder result;
    @Inject DrillBuf buffer;

    public void setup() {
    }

    public void eval() {
      result.start = 0;
      result.precision = in.precision;

      java.math.BigDecimal opResult =
          org.apache.drill.exec.util.DecimalUtility
              .getBigDecimalFromDrillBuf(in.buffer, in.start, in.end - in.start, in.scale)
          <#if functionName == "Abs">
                  .abs();
      result.scale = in.scale;
          <#elseif functionName == "Negative">
                  .negate();
      result.scale = in.scale;
          <#elseif functionName == "Ceil">
                  .setScale(0, java.math.BigDecimal.ROUND_CEILING);
          <#elseif functionName == "Floor">
                  .setScale(0, java.math.BigDecimal.ROUND_FLOOR);
          <#elseif functionName == "Trunc">
                  .setScale(0, java.math.BigDecimal.ROUND_DOWN);
          <#elseif functionName == "Round">
                  .setScale(0, java.math.BigDecimal.ROUND_HALF_UP);
          </#if>

      byte[] bytes = opResult.unscaledValue().toByteArray();
      int len = bytes.length;
      result.buffer = buffer = buffer.reallocIfNeeded(len);
      result.buffer.setBytes(0, bytes);
      result.end = len;
    }
  }

</#list>
  @FunctionTemplate(name = "sign",
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}SignFunction implements DrillSimpleFunc {
    @Param ${type.name}Holder in;
    @Output IntHolder out;

    public void setup() {}

    public void eval() {
      // TODO: optimize to get only bytes that corresponds to sign.
      // Should be taken into account case when leading zero bytes are stored in buff.
      java.math.BigDecimal bd =
          org.apache.drill.exec.util.DecimalUtility
              .getBigDecimalFromDrillBuf(in.buffer, in.start, in.end - in.start, in.scale);
      out.value = bd.signum();
    }
  }

  @FunctionTemplate(names = {"trunc", "truncate"},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_SET_SCALE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}TruncateScaleFunction implements DrillSimpleFunc {
    @Param  ${type.name}Holder left;
    @Param  IntHolder right;
    @Output ${type.name}Holder result;
    @Inject DrillBuf buffer;

    public void setup() {
    }

    public void eval() {
      result.start = 0;
      result.scale = Math.max(right.value, 0);
      result.precision = left.precision;
      java.math.BigDecimal opResult =
          org.apache.drill.exec.util.DecimalUtility
              .getBigDecimalFromDrillBuf(left.buffer, left.start, left.end - left.start, left.scale)
                  .setScale(result.scale, java.math.BigDecimal.ROUND_DOWN);
      byte[] bytes = opResult.unscaledValue().toByteArray();
      int len = bytes.length;
      result.buffer = buffer = buffer.reallocIfNeeded(len);
      result.buffer.setBytes(0, bytes);
      result.end = len;
    }
  }

  @FunctionTemplate(name = "round",
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_SET_SCALE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}RoundScaleFunction implements DrillSimpleFunc {
    @Param  ${type.name}Holder left;
    @Param  IntHolder right;
    @Output ${type.name}Holder result;
    @Inject DrillBuf buffer;

    public void setup() {
    }

    public void eval() {
      result.scale = Math.max(right.value, 0);
      result.precision = left.precision;
      result.start = 0;
      java.math.BigDecimal bd =
          org.apache.drill.exec.util.DecimalUtility
              .getBigDecimalFromDrillBuf(left.buffer, left.start, left.end - left.start, left.scale)
                  .setScale(result.scale, java.math.BigDecimal.ROUND_HALF_UP);
      byte[] bytes = bd.unscaledValue().toByteArray();
      int len = bytes.length;
      result.buffer = buffer = buffer.reallocIfNeeded(len);
      result.buffer.setBytes(0, bytes);
      result.end = len;
    }
  }

  <#-- Handle 2 x 2 combinations of nullable and non-nullable arguments. -->
  <#list ["Nullable${type.name}", "${type.name}"] as leftType >
  <#list ["Nullable${type.name}", "${type.name}"] as rightType >

  <#-- Comparison function for sorting and grouping relational operators
       (not for comparison expression operators (=, <, etc.)). -->
  @FunctionTemplate(name = FunctionGenerationHelper.COMPARE_TO_NULLS_HIGH,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_MAX_SCALE,
                    nulls = NullHandling.INTERNAL)
  public static class GCompare${leftType}Vs${rightType}NullHigh implements DrillSimpleFunc {

    @Param ${leftType}Holder left;
    @Param ${rightType}Holder right;
    @Output IntHolder out;

    public void setup() {}

    public void eval() {
      <@varCompareBlock leftType = leftType rightType = rightType absCompare="false" output = "out.value" nullCompare = true nullComparesHigh = true />
    }
  }



  <#-- Comparison function for sorting and grouping relational operators
        (not for comparison expression operators (=, <, etc.)). -->
  @FunctionTemplate(name = FunctionGenerationHelper.COMPARE_TO_NULLS_LOW,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    returnType = FunctionTemplate.ReturnType.DECIMAL_MAX_SCALE,
                    nulls = NullHandling.INTERNAL)
  public static class GCompare${leftType}Vs${rightType}NullLow implements DrillSimpleFunc {

    @Param ${leftType}Holder left;
    @Param ${rightType}Holder right;
    @Output IntHolder out;

    public void setup() {}

    public void eval() {
      <@varCompareBlock leftType = leftType rightType = rightType absCompare = "false" output = "out.value" nullCompare = true nullComparesHigh = false />
    }
  }

  </#list>
  </#list>

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
         not for sorting and grouping relational operators.) -->
  @FunctionTemplate(name = FunctionGenerationHelper.LT,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}LessThan implements DrillSimpleFunc {

    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Output BitHolder out;
    public void setup() {}

    public void eval() {
      int cmp;
      <@varCompareBlock leftType = "leftType" rightType = "rightType" absCompare = "false" output = "cmp" nullCompare = false nullComparesHigh = false />
      out.value = cmp == -1 ? 1 : 0;
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
         not for sorting and grouping relational operators.) -->
  @FunctionTemplate(name = FunctionGenerationHelper.LE,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}LessThanEq implements DrillSimpleFunc {

    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {
      int cmp;
      <@varCompareBlock leftType = "leftType" rightType = "rightType" absCompare = "false" output = "cmp" nullCompare = false nullComparesHigh = false />
      out.value = cmp < 1 ? 1 : 0;
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
         not for sorting and grouping relational operators.) -->
  @FunctionTemplate(name = FunctionGenerationHelper.GT,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}GreaterThan implements DrillSimpleFunc {

    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {
      int cmp;
      <@varCompareBlock leftType = "leftType" rightType = "rightType" absCompare = "false" output = "cmp" nullCompare = false nullComparesHigh = false />
      out.value = cmp == 1 ? 1 : 0;
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
         not for sorting and grouping relational operators.) -->
  @FunctionTemplate(name = FunctionGenerationHelper.GE,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}GreaterThanEq implements DrillSimpleFunc {

    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {
      int cmp;
      <@varCompareBlock leftType = "leftType" rightType = "rightType" absCompare = "false" output = "cmp" nullCompare = false nullComparesHigh = false />
      out.value = cmp > -1 ? 1 : 0;
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
         not for sorting and grouping relational operators.) -->
  @FunctionTemplate(name = FunctionGenerationHelper.EQ,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}Equal implements DrillSimpleFunc {

    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {
      int cmp;
      <@varCompareBlock leftType = "leftType" rightType = "rightType" absCompare = "false" output = "cmp" nullCompare = false nullComparesHigh = false />
      out.value = cmp == 0 ? 1 : 0;
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
         not for sorting and grouping relational operators.) -->
  @FunctionTemplate(name = FunctionGenerationHelper.NE,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class ${type.name}NotEqual implements DrillSimpleFunc {

    @Param ${type.name}Holder left;
    @Param ${type.name}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {
      int cmp;
      <@varCompareBlock leftType = "leftType" rightType = "rightType" absCompare = "false" output = "cmp" nullCompare = false nullComparesHigh = false />
      out.value = cmp != 0 ? 1 : 0;
    }
  }
}
</#if>

</#list>
