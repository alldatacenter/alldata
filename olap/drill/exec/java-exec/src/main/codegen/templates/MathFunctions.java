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


<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/GMathFunctions.java" />

<#include "/@includes/license.ftl" />

/*
 * This class is automatically generated from AddTypes.tdd using FreeMarker.
 */


package org.apache.drill.exec.expr.fn.impl;


import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.fn.impl.StringFunctions;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")

public class GMathFunctions{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GMathFunctions.class);
  
  private GMathFunctions(){}

  <#list mathFunc.unaryMathFunctions as func>

  <#list func.types as type>

  <#if func.funcName=="negative">
  @FunctionTemplate(names = {"negative", "u-", "-"}, scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  <#else>
  @FunctionTemplate(name = "${func.funcName}", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  </#if>
  public static class ${func.className}${type.input} implements DrillSimpleFunc {

    @Param ${type.input}Holder in;
  <#if func.funcName == 'sqrt'>
    @Output Float8Holder out;
  <#else>
    @Output ${type.outputType}Holder out;
  </#if>

    public void setup() {
    }

    public void eval() {

      <#if func.funcName=='trunc'>
      <#if type.roundingRequired ??>
      if (Double.isInfinite(in.value) || Double.isNaN(in.value)){
        out.value = <#if type.input='Float8'> Double.NaN <#else> Float.NaN </#if>;
      } else {
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(in.value).setScale(0, java.math.BigDecimal.ROUND_DOWN);
        out.value = <#if type.extraCast ??>(${type.extraCast})</#if>bd.${type.castType}Value();
      }
      <#else>
      out.value = <#if func.funcName!='sqrt'>(${type.castType})</#if> (in.value);</#if>
      <#else>
      out.value =  <#if func.funcName!='sqrt'>(${type.castType})</#if> ${func.javaFunc}(in.value);
      </#if>
    }
  }
  
  </#list>
  </#list>


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //Math functions which take two arguments (of same type). 
  //////////////////////////////////////////////////////////////////////////////////////////////////
  
  <#list mathFunc.binaryMathFunctions as func>
  <#list func.types as type>

  <#if func.aliasName == "">
  @FunctionTemplate(name = "${func.funcName}", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  <#else>
  @FunctionTemplate(names = {"${func.funcName}", "${func.aliasName}"}, scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  </#if>
  public static class ${func.className}${type.input} implements DrillSimpleFunc {

    @Param ${type.input}Holder input1;
    @Param ${type.input}Holder input2;
    @Output ${type.outputType}Holder out;

    public void setup() {
    }

    public void eval() {
	<#if func.funcName == 'div'>
	<#if type.roundingRequired ??>
    java.math.BigDecimal bdOut = java.math.BigDecimal.valueOf(input1.value ${func.javaFunc} input2.value).setScale(0, java.math.BigDecimal.ROUND_DOWN);
    out.value = bdOut.${type.castType}Value();
    <#else>
    out.value = (${type.castType}) ( input1.value ${func.javaFunc} input2.value);
    </#if>
    <#elseif func.funcName == 'mod'>
    out.value = (${type.castType}) (input2.value == 0 ? input1.value : (input1.value ${func.javaFunc} input2.value));
    <#else>
    out.value =(${type.castType}) ( input1.value ${func.javaFunc} input2.value);
    </#if>
    }
  }
  </#list>
  </#list>

  <#list mathFunc.otherMathFunctions as func>
  <#list func.types as type>

  @FunctionTemplate(name = "${func.funcName}", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  public static class ${func.className}${type.dataType} implements DrillSimpleFunc {

    @Param ${type.dataType}Holder input1;
    @Param IntHolder input2;
    @Output Float8Holder out;

    public void setup() {
    }

    public void eval() {

    <#if func.funcName=='trunc' && (type.dataType=='Float4' || type.dataType=='Float8')>
      if (Double.isInfinite(input1.value) || Double.isNaN(input1.value)){
        out.value = Double.NaN;
      } else {
        java.math.BigDecimal temp = new java.math.BigDecimal(input1.value);
        out.value = temp.setScale(input2.value, java.math.RoundingMode.${func.mode}).doubleValue();
      }
      <#else>
      java.math.BigDecimal temp = new java.math.BigDecimal(input1.value);
      out.value = temp.setScale(input2.value, java.math.RoundingMode.${func.mode}).doubleValue();
    </#if>
    }
  }
  </#list>
  </#list>
}



//////////////////////////////////////////////////////////////////////////////////////////////////
//End of GMath Functions
//////////////////////////////////////////////////////////////////////////////////////////////////




<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/ExtendedMathFunctions.java" />
<#include "/@includes/license.ftl" />

//////////////////////////////////////////////////////////////////////////////////////////////////
//Functions for Extended Math Functions
//////////////////////////////////////////////////////////////////////////////////////////////////


package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.fn.impl.StringFunctions;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
/*
 * This class is automatically generated from MathFunc.tdd using FreeMarker.
 */

@SuppressWarnings("unused")

public class ExtendedMathFunctions{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExtendedMathFunctions.class);

  private ExtendedMathFunctions(){}

//////////////////////////////////////////////////////////////////////////////////////////////////
//Unary Math functions with 1 parameter.
//////////////////////////////////////////////////////////////////////////////////////////////////

<#list mathFunc.extendedUnaryMathFunctions as func>

<#list func.types as type>

@FunctionTemplate(name = "${func.funcName}", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
public static class ${func.className}${type.input} implements DrillSimpleFunc {

  @Param ${type.input}Holder in;
  @Output ${func.outputType}Holder out;

  public void setup() {
  }

  public void eval() {
	  out.value = ${func.javaFunc}(in.value);
  }
}

</#list>
</#list>


//////////////////////////////////////////////////////////////////////////////////////////////////
//Function to handle Log with base.
//////////////////////////////////////////////////////////////////////////////////////////////////
<#list mathFunc.logBaseMathFunction as func>
<#list func.types as type>

@FunctionTemplate(name = "${func.funcName}", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
public static class ${func.className}${type.input} implements DrillSimpleFunc {

  @Param ${type.input}Holder base;
  @Param ${type.input}Holder val;
  @Output ${func.outputType}Holder out;

  public void setup() {
  }

  public void eval() {
	  out.value = ${func.javaFunc}(val.value)/ ${func.javaFunc}(base.value);
  }
}
</#list>
</#list>


}

//////////////////////////////////////////////////////////////////////////////////////////////////
//End of Extended Math Functions
//////////////////////////////////////////////////////////////////////////////////////////////////


<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/TrigoMathFunctions.java" />
<#include "/@includes/license.ftl" />

//////////////////////////////////////////////////////////////////////////////////////////////////
//Functions for Trigo Math Functions
//////////////////////////////////////////////////////////////////////////////////////////////////


package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.fn.impl.StringFunctions;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;

/*
 * This class is automatically generated from MathFunc.tdd using FreeMarker.
 */

@SuppressWarnings("unused")

public class TrigoMathFunctions{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TrigoMathFunctions.class);

  private TrigoMathFunctions(){}

  <#list mathFunc.trigoMathFunctions as func>

  <#list func.types as type>

  @FunctionTemplate(name = "${func.funcName}", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  public static class ${func.className}${type.input} implements DrillSimpleFunc {

    @Param ${type.input}Holder in;
    @Output ${func.outputType}Holder out;

    public void setup() {
    }

    public void eval() {
      out.value = ${func.javaFunc}(in.value);
    }
  }
 </#list>
 </#list>
}
