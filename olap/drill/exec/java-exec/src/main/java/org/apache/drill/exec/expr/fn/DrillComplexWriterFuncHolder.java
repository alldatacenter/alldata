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
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.physical.impl.project.ProjectRecordBatch;
import org.apache.drill.exec.record.VectorAccessibleComplexWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JVar;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;

public class DrillComplexWriterFuncHolder extends DrillSimpleFuncHolder {

  public DrillComplexWriterFuncHolder(FunctionAttributes functionAttributes, FunctionInitializer initializer) {
    super(functionAttributes, initializer);
  }

  @Override
  public boolean isComplexWriterFuncHolder() {
    return true;
  }

  @Override
  protected HoldingContainer generateEvalBody(ClassGenerator<?> classGenerator, HoldingContainer[] inputVariables, String body,
                                              JVar[] workspaceJVars, FunctionHolderExpression holderExpr) {

    FieldReference fieldReference = holderExpr.getFieldReference();
    classGenerator.getEvalBlock().directStatement(String.format("//---- start of eval portion of %s function. ----//", getRegisteredNames()[0]));

    JBlock sub = new JBlock(true, true);
    JBlock topSub = sub;

    JVar complexWriter = classGenerator.declareClassField("complexWriter", classGenerator.getModel()._ref(ComplexWriter.class));


    JInvocation container = classGenerator.getMappingSet().getOutgoing().invoke("getOutgoingContainer");

    //Default name is "col", if not passed in a reference name for the output vector.
    String refName = fieldReference == null ? "col" : fieldReference.getRootSegment().getPath();

    JClass cwClass = classGenerator.getModel().ref(VectorAccessibleComplexWriter.class);
    classGenerator.getSetupBlock().assign(complexWriter, cwClass.staticInvoke("getWriter").arg(refName).arg(container));

    JClass projBatchClass = classGenerator.getModel().ref(ProjectRecordBatch.class);
    JExpression projBatch = JExpr.cast(projBatchClass, classGenerator.getMappingSet().getOutgoing());

    classGenerator.getSetupBlock().add(projBatch.invoke("addComplexWriter").arg(complexWriter));


    classGenerator.getEvalBlock().add(complexWriter.invoke("setPosition").arg(classGenerator.getMappingSet().getValueWriteIndex()));

    sub.decl(classGenerator.getModel()._ref(ComplexWriter.class), getReturnValue().getName(), complexWriter);

    // add the subblock after the out declaration.
    classGenerator.getEvalBlock().add(topSub);

    addProtectedBlock(classGenerator, sub, body, inputVariables, workspaceJVars, false);


//    JConditional jc = classGenerator.getEvalBlock()._if(complexWriter.invoke("ok").not());

//    jc._then().add(complexWriter.invoke("reset"));
    //jc._then().directStatement("System.out.println(\"debug : write ok fail!, inIndex = \" + inIndex);");
//    jc._then()._return(JExpr.FALSE);

    //jc._else().directStatement("System.out.println(\"debug : write successful, inIndex = \" + inIndex);");

    classGenerator.getEvalBlock().directStatement(String.format("//---- end of eval portion of %s function. ----//", getRegisteredNames()[0]));

    return null;
  }

  @Override
  protected void checkNullHandling(NullHandling nullHandling) {
    checkArgument(nullHandling == NullHandling.INTERNAL,
        "Function with @Output of type 'org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter'" +
            " is required to handle null input(s) on its own.");
  }
}
