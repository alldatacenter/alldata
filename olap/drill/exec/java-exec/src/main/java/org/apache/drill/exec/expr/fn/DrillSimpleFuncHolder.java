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
package org.apache.drill.exec.expr.fn;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.BlockType;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.vector.ValueHolderHelper;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;

/**
 * Definition of a Drill function defined using the
 * <code>@FunctionTemplate</code> annotation of the class which
 * implements the function. Simple functions have
 * input parameters as defined by member variables annotated
 * with the <code>@Param</code> annotation.
 */
public class DrillSimpleFuncHolder extends DrillFuncHolder {

  private final String drillFuncClass;
  // each function should be wrapped unique class loader associated with its jar
  // to prevent classpath collisions during loading and unloading jars
  private final ClassLoader classLoader;

  public DrillSimpleFuncHolder(FunctionAttributes functionAttributes, FunctionInitializer initializer) {
    super(functionAttributes, initializer);
    drillFuncClass = checkNotNull(initializer.getClassName());
    classLoader = checkNotNull(initializer.getClassLoader());
  }

  private String setupBody() {
    return meth("setup", false);
  }

  private String evalBody() {
    return meth("eval");
  }

  private String resetBody() {
    return meth("reset", false);
  }

  private String cleanupBody() {
    return meth("cleanup", false);
  }

  @Override
  public boolean isNested() {
    return false;
  }

  public DrillSimpleFunc createInterpreter() throws Exception {
    return (DrillSimpleFunc)Class.forName(drillFuncClass, true, classLoader).newInstance();
  }

  /**
   * Render the various code blocks for a simple function.
   *
   * @param classGenerator code generator
   * @param inputVariables value holder references for each input value. Variables
   * are in the same order as the declared function parameters
   * @param workspaceJVars internal working variables as declared with the
   * <code>@Workspece</code> annotations. These are simple variables, not
   * Drill value holders
   * @param holderExpr the function call expression
   */
  @Override
  public HoldingContainer renderEnd(ClassGenerator<?> classGenerator, HoldingContainer[] inputVariables,
                                    JVar[] workspaceJVars, FunctionHolderExpression holderExpr) {

    // If the function's annotation specifies a parameter has to be constant
    // expression, but the HoldingContainer for the argument is not, then raise
    // exception.
    for (int i = 0; i < inputVariables.length; i++) {
      if (getAttributeParameter(i).isConstant() && !inputVariables[i].isConstant()) {
        throw new DrillRuntimeException(String.format(
            "The argument '%s' of Function '%s' has to be constant!",
            getAttributeParameter(i).getName(), this.getRegisteredNames()[0]));
      }
    }
    // Inline code from the function's setup block
    generateBody(classGenerator, BlockType.SETUP, setupBody(), inputVariables, workspaceJVars, true);

    // Generate the wrapper, and inline code for, the function's eval block
    HoldingContainer c = generateEvalBody(classGenerator, inputVariables, evalBody(), workspaceJVars, holderExpr);

    // Generate code for an aggregate functions reset block
    generateBody(classGenerator, BlockType.RESET, resetBody(), null, workspaceJVars, false);

    // Inline code from the function's cleanup() method
    generateBody(classGenerator, BlockType.CLEANUP, cleanupBody(), null, workspaceJVars, false);
    return c;
  }

  /**
   * Generate the eval block for a simple function, including the null-handling wrapper,
   * if requested.
   *
   * @see {@link #generateBody()}
   */
  protected HoldingContainer generateEvalBody(ClassGenerator<?> g, HoldingContainer[] inputVariables, String body,
                                              JVar[] workspaceJVars, FunctionHolderExpression holderExpr) {

    g.getEvalBlock().directStatement(String.format(
        "//---- start of eval portion of %s function. ----//", getRegisteredNames()[0]));

    JBlock sub = new JBlock(true, true);
    JBlock topSub = sub;
    HoldingContainer out = null;
    MajorType returnValueType = getReturnType();

    // add outside null handling if it is defined.
    // If defined, the actual eval is in the "else" block of the
    // null checks.
    if (getNullHandling() == NullHandling.NULL_IF_NULL) {
      JExpression e = null;
      for (HoldingContainer v : inputVariables) {
        if (v.isOptional()) {
          JExpression isNullExpr;
          if (v.isReader()) {
            isNullExpr = JOp.cond(v.getHolder().invoke("isSet"), JExpr.lit(1), JExpr.lit(0));
          } else {
            isNullExpr = v.getIsSet();
          }
          if (e == null) {
            e = isNullExpr;
          } else {
            e = e.mul(isNullExpr);
          }
        }
      }

      if (e != null) {
        // if at least one expression must be checked, set up the conditional.
        returnValueType = getReturnType().toBuilder().setMode(DataMode.OPTIONAL).build();
        out = g.declare(returnValueType);
        e = e.eq(JExpr.lit(0));
        JConditional jc = sub._if(e);
        jc._then().assign(out.getIsSet(), JExpr.lit(0));
        sub = jc._else();
      } else if (holderExpr.getMajorType().getMode() == DataMode.OPTIONAL) {
        returnValueType = getReturnType().toBuilder().setMode(DataMode.OPTIONAL).build();
      }
    }

    if (out == null) {
      out = g.declare(returnValueType);
    }

    // add the subblock after the out declaration.
    g.getEvalBlock().add(topSub);

    JVar internalOutput = sub.decl(JMod.FINAL, g.getHolderType(returnValueType),
        getReturnValue().getName(), JExpr._new(g.getHolderType(returnValueType)));

    // Generate function body from source, including fields rendered as
    // block local vars.
    addProtectedBlock(g, sub, body, inputVariables, workspaceJVars, false);

    // Copy the output from the internal output parameter to the
    // one known in the outer scope.
    List<String> holderFields = ValueHolderHelper.getHolderParams(returnValueType);
    for (String holderField : holderFields) {
      sub.assign(out.f(holderField), internalOutput.ref(holderField));
    }

    if (sub != topSub) {
      sub.assign(out.f("isSet"), JExpr.lit(1));  // Assign null if NULL_IF_NULL mode
    }

    g.getEvalBlock().directStatement(String.format(
        "//---- end of eval portion of %s function. ----//", getRegisteredNames()[0]));

    return out;
  }
}
