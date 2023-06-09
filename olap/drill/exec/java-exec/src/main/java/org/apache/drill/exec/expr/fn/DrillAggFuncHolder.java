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

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.BlockType;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.record.TypedFieldId;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

class DrillAggFuncHolder extends DrillFuncHolder {

  protected String setup() {
    return meth("setup");
  }

  protected String reset() {
    return meth("reset", false);
  }

  protected String add() {
    return meth("add");
  }

  protected String output() {
    return meth("output");
  }

  protected String cleanup() {
    return meth("cleanup", false);
  }

  public DrillAggFuncHolder(
      FunctionAttributes attributes,
      FunctionInitializer initializer) {
    super(attributes, initializer);
  }

  @Override
  public boolean isNested(){
    return true;
  }

  @Override
  public boolean isAggregating() {
    return true;
  }

  @Override
  public JVar[] renderStart(ClassGenerator<?> g, HoldingContainer[] inputVariables, FieldReference fieldReference) {
    if (!g.getMappingSet().isHashAggMapping()) {  //Declare workspace vars for non-hash-aggregation.
      JVar[] workspaceJVars = declareWorkspaceVariables(g);
      generateBody(g, BlockType.SETUP, setup(), null, workspaceJVars, true);
      return workspaceJVars;
    } else {  //Declare workspace vars and workspace vectors for hash aggregation.

      JVar[] workspaceJVars = declareWorkspaceVectors(g);

      JBlock setupBlock = g.getSetupBlock();

      //Loop through all workspace vectors, to get the minimum of size of all workspace vectors.
      JVar sizeVar = setupBlock.decl(g.getModel().INT, "vectorSize", JExpr.lit(Integer.MAX_VALUE));
      JClass mathClass = g.getModel().ref(Math.class);
      for (int id = 0; id < getWorkspaceVars().length; id++) {
        if (!getWorkspaceVars()[id].isInject()) {
          setupBlock.assign(sizeVar,mathClass.staticInvoke("min").arg(sizeVar).arg(g.getWorkspaceVectors().get(getWorkspaceVars()[id]).invoke("getValueCapacity")));
        }
      }

      for (int i = 0; i < getWorkspaceVars().length; i++) {
        if (!getWorkspaceVars()[i].isInject()) {
          setupBlock.assign(workspaceJVars[i], JExpr._new(g.getHolderType(getWorkspaceVars()[i].getMajorType())));
        }
      }

      //Use for loop to initialize entries in the workspace vectors.
      JForLoop forLoop = setupBlock._for();
      JVar ivar = forLoop.init(g.getModel().INT, "drill_internal_i", JExpr.lit(0));
      forLoop.test(ivar.lt(sizeVar));
      forLoop.update(ivar.assignPlus(JExpr.lit(1)));

      JBlock subBlock = generateInitWorkspaceBlockHA(g, BlockType.SETUP, setup(), workspaceJVars, ivar);
      forLoop.body().add(subBlock);
      return workspaceJVars;
    }
  }

  @Override
  public void renderMiddle(ClassGenerator<?> g, HoldingContainer[] inputVariables, JVar[]  workspaceJVars) {
    addProtectedBlock(g, g.getBlock(BlockType.EVAL), add(), inputVariables, workspaceJVars, false);
  }

  @Override
  public HoldingContainer renderEnd(ClassGenerator<?> classGenerator, HoldingContainer[] inputVariables,
                                    JVar[] workspaceJVars, FunctionHolderExpression holderExpr) {
    HoldingContainer out = null;
    JVar internalOutput = null;
    if (getReturnType().getMinorType() != TypeProtos.MinorType.LATE) {
      out = classGenerator.declare(getReturnType(), false);
    }
    JBlock sub = new JBlock();
    if (getReturnType().getMinorType() != TypeProtos.MinorType.LATE) {
      internalOutput = sub.decl(JMod.FINAL, classGenerator.getHolderType(getReturnType()), getReturnValue().getName(), JExpr._new(classGenerator.getHolderType(getReturnType())));
    }
    classGenerator.getEvalBlock().add(sub);
    addProtectedBlock(classGenerator, sub, output(), null, workspaceJVars, false);
    if (getReturnType().getMinorType() != TypeProtos.MinorType.LATE) {
      sub.assign(out.getHolder(), internalOutput);
    }
    //hash aggregate uses workspace vectors. Initialization is done in "setup" and does not require "reset" block.
    if (!classGenerator.getMappingSet().isHashAggMapping()) {
      generateBody(classGenerator, BlockType.RESET, reset(), null, workspaceJVars, false);
    }
    generateBody(classGenerator, BlockType.CLEANUP, cleanup(), null, workspaceJVars, false);

    return out;
  }

  private JVar[] declareWorkspaceVectors(ClassGenerator<?> g) {
    JVar[] workspaceJVars = new JVar[getWorkspaceVars().length];

    for (int i = 0; i < getWorkspaceVars().length; i++) {
      if (getWorkspaceVars()[i].isInject()) {
        workspaceJVars[i] = g.declareClassField("work", g.getModel()._ref(getWorkspaceVars()[i].getType()));
        assignInjectableValue(g, workspaceJVars[i], getWorkspaceVars()[i]);
      } else {
        Preconditions.checkState(Types.isFixedWidthType(getWorkspaceVars()[i].getMajorType()), String.format("Workspace variable '%s' in aggregation function '%s' is not allowed to " +
            "have variable length type.", getWorkspaceVars()[i].getName(), getRegisteredNames()[0]));
        Preconditions.checkState(getWorkspaceVars()[i].getMajorType().getMode()==DataMode.REQUIRED, String.format("Workspace variable '%s' in aggregation function '%s' is not allowed" +
            " to have null or repeated type.", getWorkspaceVars()[i].getName(), getRegisteredNames()[0]));

        //workspaceJVars[i] = g.declareClassField("work", g.getHolderType(workspaceVars[i].majorType), JExpr._new(g.getHolderType(workspaceVars[i].majorType)));
        workspaceJVars[i] = g.declareClassField("work", g.getHolderType(getWorkspaceVars()[i].getMajorType()));

        //Declare a workspace vector for the workspace var.
        TypedFieldId typedFieldId = new TypedFieldId.Builder().finalType(getWorkspaceVars()[i].getMajorType())
            .addId(g.getWorkspaceTypes().size())
            .build();
        JVar vv  = g.declareVectorValueSetupAndMember(g.getMappingSet().getWorkspace(), typedFieldId);

        g.getWorkspaceTypes().add(typedFieldId);
        g.getWorkspaceVectors().put(getWorkspaceVars()[i], vv);
      }
    }
    return workspaceJVars;
  }

  private JBlock generateInitWorkspaceBlockHA(ClassGenerator<?> g, BlockType bt, String body, JVar[] workspaceJVars, JExpression wsIndexVariable){
    JBlock initBlock = new JBlock(true, true);
    if(!Strings.isNullOrEmpty(body) && !body.trim().isEmpty()){
      JBlock sub = new JBlock(true, true);
      addProtectedBlockHA(g, sub, body, null, workspaceJVars, wsIndexVariable);
      initBlock.directStatement(String.format("/** start %s for function %s **/ ", bt.name(), getRegisteredNames()[0]));
      initBlock.add(sub);
      initBlock.directStatement(String.format("/** end %s for function %s **/ ", bt.name(), getRegisteredNames()[0]));
    }
    return initBlock;
  }

  @Override
  protected void addProtectedBlock(ClassGenerator<?> g, JBlock sub, String body, HoldingContainer[] inputVariables, JVar[] workspaceJVars, boolean decConstantInputOnly){
    if (!g.getMappingSet().isHashAggMapping()) {
      super.addProtectedBlock(g, sub, body, inputVariables, workspaceJVars, decConstantInputOnly);
    } else {
      JExpression indexVariable = g.getMappingSet().getWorkspaceIndex();
      addProtectedBlockHA(g, sub, body, inputVariables, workspaceJVars, indexVariable);
    }
  }

  /**
   * This is customized version of "addProtectedBlock" for hash aggregation. It
   * take one additional parameter "wsIndexVariable".
   */
  private void addProtectedBlockHA(ClassGenerator<?> g, JBlock sub, String body, HoldingContainer[] inputVariables, JVar[] workspaceJVars, JExpression wsIndexVariable) {
    if (inputVariables != null) {
      if (isVarArg()) {
        declareVarArgArray(g.getModel(), sub, inputVariables);
      }
      for (int i = 0; i < inputVariables.length; i++) {
        declareInputVariable(g.getModel(), sub, inputVariables[i], i);
      }
    }

    JVar[] internalVars = new JVar[workspaceJVars.length];
    for (int i = 0; i < workspaceJVars.length; i++) {

      if (getWorkspaceVars()[i].isInject()) {
        internalVars[i] = sub.decl(g.getModel()._ref(getWorkspaceVars()[i].getType()), getWorkspaceVars()[i].getName(), workspaceJVars[i]);
        continue;
      }
      // Access workspaceVar through workspace vector.
      JInvocation getValueAccessor = g.getWorkspaceVectors().get(getWorkspaceVars()[i]).invoke("getAccessor").invoke("get");
      if (Types.usesHolderForGet(getWorkspaceVars()[i].getMajorType())) {
        sub.add(getValueAccessor.arg(wsIndexVariable).arg(workspaceJVars[i]));
      } else {
        sub.assign(workspaceJVars[i].ref("value"), getValueAccessor.arg(wsIndexVariable));
      }
      internalVars[i] = sub.decl(g.getHolderType(getWorkspaceVars()[i].getMajorType()), getWorkspaceVars()[i].getName(), workspaceJVars[i]);
    }

    Preconditions.checkNotNull(body);
    sub.directStatement(body);

    // reassign workspace variables back.
    for (int i = 0; i < workspaceJVars.length; i++) {
      sub.assign(workspaceJVars[i], internalVars[i]);

      // Injected buffers are not stored as vectors skip storing them in vectors
      if (getWorkspaceVars()[i].isInject()) {
        continue;
      }
      // Change workspaceVar through workspace vector.
      JInvocation setMeth;
      MajorType type = getWorkspaceVars()[i].getMajorType();
      if (Types.usesHolderForGet(type)) {
        setMeth = g.getWorkspaceVectors().get(getWorkspaceVars()[i]).invoke("getMutator").invoke("setSafe").arg(wsIndexVariable).arg(workspaceJVars[i]);
      } else {
        if (!Types.isFixedWidthType(type) || Types.isRepeated(type)) {
          setMeth = g.getWorkspaceVectors().get(getWorkspaceVars()[i]).invoke("getMutator").invoke("setSafe").arg(wsIndexVariable).arg(workspaceJVars[i].ref("value"));
        } else {
          setMeth = g.getWorkspaceVectors().get(getWorkspaceVars()[i]).invoke("getMutator").invoke("set").arg(wsIndexVariable).arg(workspaceJVars[i].ref("value"));
        }
      }

      sub.add(setMeth);
    }
  }

  @Override
  protected void checkNullHandling(NullHandling nullHandling) {
    checkArgument(nullHandling == NullHandling.INTERNAL,
        "An aggregate function is required to handle null input(s) on its own.");
  }
}
