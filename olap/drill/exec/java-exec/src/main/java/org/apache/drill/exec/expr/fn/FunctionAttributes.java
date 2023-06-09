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

import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionCostCategory;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;

/**
 * Attributes of a function used in code generation and optimization.
 * Represents the values contained in the annotations of a Drill
 * function.
 */
public class FunctionAttributes {

  /**
   * Reference to the <code>@FunctionTemplate</code> annotation
   * instance for the function.
   */
  private final FunctionTemplate template;

  /**
   * Function names (aliases). Some functions are known by multiple
   * names.
   */
  private final String[] registeredNames;

  /**
   * Input parameters to the function. Indicated by the
   * <code>@Param</code> annotation for Drill functions.
   */
  private final ValueReference[] parameters;

  /**
   * Single return value of the function. Indicated by the
   * <code>@Output</code> annotation for Drill functions.
   */
  private final ValueReference returnValue;

  /**
   * Drill functions allow extra "workspace" variables indicated
   * by the @Workspace annotation.
   */
  private final WorkspaceReference[] workspaceVars;

  public FunctionAttributes (FunctionTemplate template,
                             ValueReference[] parameters,
                             ValueReference returnValue,
                             WorkspaceReference[] workspaceVars) {
    this.template = template;
    this.registeredNames = ((template.name().isEmpty()) ? template.names() : new String[] {template.name()});
    this.parameters = parameters;
    this.returnValue = returnValue;
    this.workspaceVars = workspaceVars;
  }

  public FunctionScope getScope() {
    return template.scope();
  }

  public FunctionTemplate.ReturnType getReturnType() {
    return template.returnType();
  }

  public FunctionTemplate.OutputWidthCalculatorType getOutputWidthCalculatorType() {
    return template.outputWidthCalculatorType();
  }

  public int variableOutputSizeEstimate() {
    return  template.outputSizeEstimate();
  }

  public NullHandling getNullHandling() {
    return template.nulls();
  }

  public boolean isBinaryCommutative() {
    return template.isBinaryCommutative();
  }

  @Deprecated
  public boolean isRandom() {
    return template.isRandom();
  }

  public boolean isDeterministic() {
    return !template.isRandom();
  }

  public String[] getRegisteredNames() {
    return registeredNames;
  }

  public ValueReference[] getParameters() {
    return parameters;
  }

  public ValueReference getReturnValue() {
    return returnValue;
  }

  public WorkspaceReference[] getWorkspaceVars() {
    return workspaceVars;
  }

  public FunctionCostCategory getCostCategory() {
    return template.costCategory();
  }

  public boolean isNiladic() {
    return template.isNiladic();
  }

  public boolean isVarArg() {
    return template.isVarArg();
  }

  public boolean isInternal() {
    return template.isInternal();
  }

  public boolean checkPrecisionRange() { return template.checkPrecisionRange(); }
}
