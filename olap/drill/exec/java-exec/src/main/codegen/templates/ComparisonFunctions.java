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
/**
  NOTE: ComparisonFunctions.java does not contain/generate all comparison
  functions.  DecimalFunctions.java and DateIntervalFunctions.java contain
  some.
*/
<#-- TODO:  Refactor comparison code from DecimalFunctions.java and
     DateIntervalFunctions.java into here (to eliminate duplicate template code
     and so that ComparisonFunctions actually has all comparison functions. -->


<@pp.dropOutputFile />

<#macro intervalCompareBlock leftType rightType leftMonths leftDays leftMillis rightMonths rightDays rightMillis output>

        org.joda.time.MutableDateTime leftDate  =
            new org.joda.time.MutableDateTime(1970, 1, 1, 0, 0, 0, 0, org.joda.time.DateTimeZone.UTC);
        org.joda.time.MutableDateTime rightDate =
            new org.joda.time.MutableDateTime(1970, 1, 1, 0, 0, 0, 0, org.joda.time.DateTimeZone.UTC);

        // Left and right date have the same starting point (epoch), add the interval period and compare the two
        leftDate.addMonths(${leftMonths});
        leftDate.addDays(${leftDays});
        leftDate.add(${leftMillis});

        rightDate.addMonths(${rightMonths});
        rightDate.addDays(${rightDays});
        rightDate.add(${rightMillis});

        long leftMS  = leftDate.getMillis();
        long rightMS = rightDate.getMillis();

        ${output} = leftMS < rightMS ? -1 : (leftMS > rightMS ? 1 : 0);

</#macro>


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


<#-- macro compareBlock: Generates block handling comparison, including NULL. -->

<#-- Parameters: >
<#-- - mode: selects case of comparison code -->
<#-- - leftType: name of left argument's type  (e.g., NullableFloat4)  -->
<#-- - rightType: name of right argument's type  -->
<#-- - output: output variable name -->
<#-- - nullCompare: whether to generate null-comparison code -->
<#-- - nullComparesHigh:  whether NULL compares as the highest value or the lowest
       value -->

<#macro compareBlock mode leftType rightType output nullCompare nullComparesHigh>
     outside:
      {
        <@compareNullsSubblock leftType=leftType rightType=rightType
          output="out.value" breakTarget="outside" nullCompare=true nullComparesHigh=nullComparesHigh />
    // NaN is the biggest possible value, and NaN == NaN
    <#if mode == "primitive">
      if(Double.isNaN(left.value) && Double.isNaN(right.value)) {
        ${output} = 0;
      } else if (!Double.isNaN(left.value) && Double.isNaN(right.value)) {
        ${output} = -1;
      } else if (Double.isNaN(left.value) && !Double.isNaN(right.value)) {
        ${output} = 1;
      } else {
        ${output} = left.value < right.value ? -1 : (left.value == right.value ? 0 : 1);
      }

    <#elseif mode == "varString">
      ${output} = org.apache.drill.exec.expr.fn.impl.ByteFunctionHelpers.compare(
          left.buffer, left.start, left.end, right.buffer, right.start, right.end );
    <#elseif mode == "intervalNameThis">
      <@intervalCompareBlock leftType=leftType rightType=rightType
        leftMonths ="left.months"  leftDays ="left.days"  leftMillis ="left.milliseconds"
        rightMonths="right.months" rightDays="right.days" rightMillis="right.milliseconds"
        output="${output}"/>
    <#elseif mode == "intervalDay">
      <@intervalCompareBlock leftType=leftType rightType=rightType
        leftMonths ="0" leftDays ="left.days"  leftMillis ="left.milliseconds"
        rightMonths="0" rightDays="right.days" rightMillis="right.milliseconds"
        output="${output}"/>
       <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>

      } // outside
</#macro>


<#-- 1.  For each group of cross-comparable types: -->
<#list comparisonTypesMain.typeGroups as typeGroup>

<#-- 2.  For each pair of (cross-comparable) types in group: -->
<#list typeGroup.comparables as leftTypeBase>
<#list typeGroup.comparables as rightTypeBase>

<#-- Generate one file for each pair of base types (includes Nullable cases). -->
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/GCompare${leftTypeBase}Vs${rightTypeBase}.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.expr.fn.impl.ByteFunctionHelpers;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import javax.inject.Inject;
import io.netty.buffer.DrillBuf;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

@SuppressWarnings("unused")
public class GCompare${leftTypeBase}Vs${rightTypeBase} {

<#-- 3.  For each combination of Nullable vs. not (of given non-nullable types):  -->
<#list ["${leftTypeBase}", "Nullable${leftTypeBase}"] as leftType >
<#list ["${rightTypeBase}", "Nullable${rightTypeBase}"] as rightType >


  <#-- Comparison function for sorting and grouping relational operators
       (not for comparison expression operators (=, <, etc.)). -->
  @FunctionTemplate(name = FunctionGenerationHelper.COMPARE_TO_NULLS_HIGH,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.INTERNAL)
  public static class GCompare${leftType}Vs${rightType}NullHigh implements DrillSimpleFunc {

    @Param ${leftType}Holder left;
    @Param ${rightType}Holder right;
    @Output IntHolder out;

    public void setup() {}

    public void eval() {
      <@compareBlock mode=typeGroup.mode leftType=leftType rightType=rightType
                     output="out.value" nullCompare=true nullComparesHigh=true />
    }
  }

  <#-- Comparison function for sorting and grouping relational operators
       (not for comparison expression operators (=, <, etc.)). -->
  @FunctionTemplate(name = FunctionGenerationHelper.COMPARE_TO_NULLS_LOW,
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.INTERNAL)
  public static class GCompare${leftType}Vs${rightType}NullLow implements DrillSimpleFunc {

    @Param ${leftType}Holder left;
    @Param ${rightType}Holder right;
    @Output IntHolder out;

    public void setup() {}

    public void eval() {
      <@compareBlock mode=typeGroup.mode leftType=leftType rightType=rightType
                     output="out.value" nullCompare=true nullComparesHigh=false />
    }
  }

</#list>
</#list> <#-- 3. Nullable combinations -->



  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
       not for sorting and grouping relational operators. -->
  @FunctionTemplate(names = {FunctionNames.LT, "<"},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class LessThan${leftTypeBase}Vs${rightTypeBase} implements DrillSimpleFunc {

    @Param ${leftTypeBase}Holder left;
    @Param ${rightTypeBase}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {

    <#if typeGroup.mode == "primitive">
      // NaN is the biggest possible value, and NaN == NaN
      if (Double.isNaN(left.value) || ( Double.isNaN(left.value) && Double.isNaN(right.value))) {
        out.value=0;
      } else if (Double.isNaN(right.value) && !Double.isNaN(left.value)) {
        out.value = 1;
      } else {
        out.value = left.value < right.value ? 1 : 0;
      }
    <#elseif typeGroup.mode == "varString"
        || typeGroup.mode == "intervalNameThis" || typeGroup.mode == "intervalDay" >
      int cmp;
      <@compareBlock mode=typeGroup.mode leftType=leftTypeBase rightType=rightTypeBase
                     output="cmp" nullCompare=false nullComparesHigh=false />
      out.value = cmp == -1 ? 1 : 0;
    <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
       not for sorting and grouping relational operators. -->
  @FunctionTemplate(names = {FunctionNames.LE, "<="},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class LessThanEq${leftTypeBase}Vs${rightTypeBase} implements DrillSimpleFunc {

    @Param ${leftTypeBase}Holder left;
    @Param ${rightTypeBase}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {

    <#if typeGroup.mode == "primitive">
      // NaN is the biggest possible value, and NaN == NaN
      if (Double.isNaN(right.value)){
        out.value = 1;
      } else if (!Double.isNaN(right.value) && Double.isNaN(left.value)) {
        out.value = 0;
      } else {
        out.value = left.value <= right.value ? 1 : 0;
      }
    <#elseif typeGroup.mode == "varString"
        || typeGroup.mode == "intervalNameThis" || typeGroup.mode == "intervalDay" >
      int cmp;
      <@compareBlock mode=typeGroup.mode leftType=leftTypeBase rightType=rightTypeBase
                     output="cmp" nullCompare=false nullComparesHigh=false />
      out.value = cmp < 1 ? 1 : 0;
    <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
       not for sorting and grouping relational operators. -->
  @FunctionTemplate(names = {FunctionNames.GT, ">"},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class GreaterThan${leftTypeBase}Vs${rightTypeBase} implements DrillSimpleFunc {

    @Param ${leftTypeBase}Holder left;
    @Param ${rightTypeBase}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {

    <#if typeGroup.mode == "primitive">
      // NaN is the biggest possible value, and NaN == NaN
      if (Double.isNaN(right.value) || ( Double.isNaN(left.value) && Double.isNaN(right.value))) {
        out.value = 0;
      } else if (Double.isNaN(left.value) && !Double.isNaN(right.value)) {
        out.value = 1;
      } else {
        out.value = left.value > right.value ? 1 : 0;
      }
    <#elseif typeGroup.mode == "varString"
        || typeGroup.mode == "intervalNameThis" || typeGroup.mode == "intervalDay" >
      int cmp;
      <@compareBlock mode=typeGroup.mode leftType=leftTypeBase rightType=rightTypeBase
                     output="cmp" nullCompare=false nullComparesHigh=false />
      out.value = cmp == 1 ? 1 : 0;
    <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
       not for sorting and grouping relational operators. -->
  @FunctionTemplate(names = {FunctionNames.GE, ">="},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class GreaterThanEq${leftTypeBase}Vs${rightTypeBase} implements DrillSimpleFunc {

    @Param ${leftTypeBase}Holder left;
    @Param ${rightTypeBase}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {

    <#if typeGroup.mode == "primitive">
      // NaN is the biggest possible value, and NaN == NaN
      if (Double.isNaN(left.value)){
        out.value=1;
      } else if (!Double.isNaN(left.value) && Double.isNaN(right.value)) {
        out.value = 0;
      } else {
        out.value = left.value >= right.value ? 1 : 0;
      }

    <#elseif typeGroup.mode == "varString"
        || typeGroup.mode == "intervalNameThis" || typeGroup.mode == "intervalDay" >
      int cmp;
      <@compareBlock mode=typeGroup.mode leftType=leftTypeBase rightType=rightTypeBase
                     output="cmp" nullCompare=false nullComparesHigh=false />
      out.value = cmp > -1 ? 1 : 0;
    <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
       not for sorting and grouping relational operators. -->
  @FunctionTemplate(names = {FunctionNames.EQ, "==", "="},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class Equals${leftTypeBase}Vs${rightTypeBase} implements DrillSimpleFunc {

    @Param ${leftTypeBase}Holder left;
    @Param ${rightTypeBase}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {

    <#if typeGroup.mode == "primitive">
      // NaN is the biggest possible value, and NaN == NaN
      if (Double.isNaN(left.value) && Double.isNaN(right.value)) {
        out.value = 1;
      } else {
        out.value = left.value == right.value ? 1 : 0;
      }
    <#elseif typeGroup.mode == "varString" >
      out.value = org.apache.drill.exec.expr.fn.impl.ByteFunctionHelpers.equal(
          left.buffer, left.start, left.end, right.buffer, right.start, right.end);
    <#elseif typeGroup.mode == "intervalNameThis" || typeGroup.mode == "intervalDay" >
      int cmp;
      <@compareBlock mode=typeGroup.mode leftType=leftTypeBase rightType=rightTypeBase
                     output="cmp" nullCompare=false nullComparesHigh=false />
      out.value = cmp == 0 ? 1 : 0;
    <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>
    }
  }

  <#-- Comparison function for comparison expression operator (=, &lt;, etc.),
       not for sorting and grouping relational operators. -->
  @FunctionTemplate(names = {FunctionNames.NE, "<>", "!="},
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = NullHandling.NULL_IF_NULL)
  public static class NotEquals${leftTypeBase}Vs${rightTypeBase} implements DrillSimpleFunc {

    @Param ${leftTypeBase}Holder left;
    @Param ${rightTypeBase}Holder right;
    @Output BitHolder out;

    public void setup() {}

    public void eval() {

    <#if typeGroup.mode == "primitive">
      // NaN is the biggest possible value, and NaN == NaN
      if (Double.isNaN(left.value) && Double.isNaN(right.value)) {
        out.value = 0;
      } else {
        out.value = left.value != right.value ? 1 : 0;
      }
    <#elseif typeGroup.mode == "varString"
          || typeGroup.mode == "intervalNameThis" || typeGroup.mode == "intervalDay" >
      int cmp;
      <@compareBlock mode=typeGroup.mode leftType=leftTypeBase rightType=rightTypeBase
                       output="cmp" nullCompare=false nullComparesHigh=false />
        out.value = cmp == 0 ? 0 : 1;
    <#-- TODO:  Refactor other comparison code to here. -->
    <#else>
      ${mode_HAS_BAD_VALUE}
    </#if>
    }
  }
}
</#list> <#-- 2.  Pair of types-->
</#list>
</#list> <#-- 1. Group -->
