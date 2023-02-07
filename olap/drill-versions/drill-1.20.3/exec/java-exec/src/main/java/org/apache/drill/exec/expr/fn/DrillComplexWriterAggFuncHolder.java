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

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.physical.impl.aggregate.HashAggBatch;
import org.apache.drill.exec.physical.impl.aggregate.HashAggTemplate;
import org.apache.drill.exec.physical.impl.aggregate.StreamingAggBatch;
import org.apache.drill.exec.physical.impl.aggregate.StreamingAggTemplate;
import org.apache.drill.exec.record.VectorAccessibleComplexWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JMod;

public class DrillComplexWriterAggFuncHolder extends DrillAggFuncHolder {

  // Complex writer to write out complex data-types e.g. repeated maps/lists
  private JVar complexWriter;
  // The index at which to write - important when group-by is present. Implicit assumption that the output indexes
  // will be sequential starting from 0. i.e. the first group would be written at index 0, second group at index 1
  // and so on.
  private JVar writerIdx;
  private JVar lastWriterIdx;
  public DrillComplexWriterAggFuncHolder(FunctionAttributes functionAttributes, FunctionInitializer initializer) {
    super(functionAttributes, initializer);
  }

  @Override
  public boolean isComplexWriterFuncHolder() {
    return true;
  }

  @Override
  public JVar[] renderStart(ClassGenerator<?> classGenerator, HoldingContainer[] inputVariables, FieldReference fieldReference) {
    JInvocation container = classGenerator.getMappingSet().getOutgoing().invoke("getOutgoingContainer");

    complexWriter = classGenerator.declareClassField("complexWriter", classGenerator.getModel()._ref(ComplexWriter.class));

    if (classGenerator.getMappingSet().isHashAggMapping()) {
      // Default name is "col", if not passed in a reference name for the output vector.
      String refName = fieldReference == null ? "col" : fieldReference.getRootSegment().getPath();
      JClass cwClass = classGenerator.getModel().ref(VectorAccessibleComplexWriter.class);
      classGenerator.getSetupBlock().assign(complexWriter, cwClass.staticInvoke("getWriter").arg(refName).arg(container));

      return super.renderStart(classGenerator, inputVariables, fieldReference);
    } else {  //Declare workspace vars for non-hash-aggregation.
      writerIdx = classGenerator.declareClassField("writerIdx", classGenerator.getModel()._ref(int.class));
      lastWriterIdx = classGenerator.declareClassField("lastWriterIdx", classGenerator.getModel()._ref(int.class));
      //Default name is "col", if not passed in a reference name for the output vector.
      String refName = fieldReference == null ? "col" : fieldReference.getRootSegment().getPath();
      JClass cwClass = classGenerator.getModel().ref(VectorAccessibleComplexWriter.class);
      classGenerator.getSetupBlock().assign(complexWriter, cwClass.staticInvoke("getWriter").arg(refName).arg(container));
      classGenerator.getSetupBlock().assign(writerIdx, JExpr.lit(0));
      classGenerator.getSetupBlock().assign(lastWriterIdx, JExpr.lit(-1));

      JVar[] workspaceJVars = declareWorkspaceVariables(classGenerator);
      generateBody(classGenerator, ClassGenerator.BlockType.SETUP, setup(), null, workspaceJVars, true);
      return workspaceJVars;
    }
  }

  @Override
  public void renderMiddle(ClassGenerator<?> classGenerator, HoldingContainer[] inputVariables, JVar[] workspaceJVars) {

    classGenerator.getEvalBlock().directStatement(String.format("//---- start of eval portion of %s function. ----//",
        getRegisteredNames()[0]));

    JBlock sub = new JBlock(true, true);
    JClass aggBatchClass = null;

    if (classGenerator.getCodeGenerator().getDefinition() == StreamingAggTemplate.TEMPLATE_DEFINITION) {
      aggBatchClass = classGenerator.getModel().ref(StreamingAggBatch.class);
    } else if (classGenerator.getCodeGenerator().getDefinition() == HashAggTemplate.TEMPLATE_DEFINITION) {
      aggBatchClass = classGenerator.getModel().ref(HashAggBatch.class);
    }

    JExpression aggBatch = JExpr.cast(aggBatchClass, classGenerator.getMappingSet().getOutgoing());

    classGenerator.getSetupBlock().add(aggBatch.invoke("addComplexWriter").arg(complexWriter));
    // Only set the writer if there is a position change. Calling setPosition may cause underlying writers to allocate
    // new vectors, thereby, losing the previously stored values
    if (classGenerator.getMappingSet().isHashAggMapping()) {
      classGenerator.getEvalBlock().add(
          complexWriter
              .invoke("setPosition")
              .arg(classGenerator.getMappingSet().getWorkspaceIndex()));
    } else {
      JBlock condAssignCW = classGenerator.getEvalBlock()._if(lastWriterIdx.ne(writerIdx))._then();
      condAssignCW.add(complexWriter.invoke("setPosition").arg(writerIdx));
      condAssignCW.assign(lastWriterIdx, writerIdx);
    }
    sub.decl(classGenerator.getModel()._ref(ComplexWriter.class), getReturnValue().getName(), complexWriter);

    // add the subblock after the out declaration.
    classGenerator.getEvalBlock().add(sub);

    addProtectedBlock(classGenerator, sub, add(), inputVariables, workspaceJVars, false);
    classGenerator.getEvalBlock().directStatement(String.format("//---- end of eval portion of %s function. ----//",
        getRegisteredNames()[0]));
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
      internalOutput = sub.decl(JMod.FINAL, classGenerator.getHolderType(getReturnType()), getReturnValue().getName(),
          JExpr._new(classGenerator.getHolderType(getReturnType())));
    }
    classGenerator.getEvalBlock().add(sub);
    if (getReturnType().getMinorType() == TypeProtos.MinorType.LATE
        && !classGenerator.getMappingSet().isHashAggMapping()) {
      sub.assignPlus(writerIdx, JExpr.lit(1));
    }
    addProtectedBlock(classGenerator, sub, output(), null, workspaceJVars, false);
    if (getReturnType().getMinorType() != TypeProtos.MinorType.LATE) {
      sub.assign(out.getHolder(), internalOutput);
    }
    //hash aggregate uses workspace vectors. Initialization is done in "setup" and does not require "reset" block.
    if (!classGenerator.getMappingSet().isHashAggMapping()) {
      generateBody(classGenerator, ClassGenerator.BlockType.RESET, reset(), null, workspaceJVars, false);
    }
    generateBody(classGenerator, ClassGenerator.BlockType.CLEANUP, cleanup(), null, workspaceJVars, false);

    return out;
  }
}

