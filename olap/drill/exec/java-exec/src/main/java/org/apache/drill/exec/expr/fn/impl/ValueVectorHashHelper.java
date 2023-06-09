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
package org.apache.drill.exec.expr.fn.impl;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.compile.sig.GeneratorMapping;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.compile.sig.RuntimeOverridden;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.planner.physical.HashPrelUtil;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;

import javax.inject.Named;
import java.io.IOException;

public class ValueVectorHashHelper {

  private RecordBatch recordBatch;

  private FragmentContext context;

  private TemplateClassDefinition<Hash64> TEMPLATE_DEFINITION = new TemplateClassDefinition<Hash64>(Hash64.class, Hash64Template.class);

  private static final GeneratorMapping DO_SETUP_CONSTANT = GeneratorMapping.create("doSetup" /* setup method */, "doSetup" /* eval method */, null /* reset */, null /* cleanup */);

  private static final GeneratorMapping GET_HASH_BUILD_INNER = GeneratorMapping.create("doSetup" /* setup method */, "hash64Code" /* eval method */, null /* reset */, null /* cleanup */);

  private final MappingSet GetHashIncomingBuildColMapping = new MappingSet("incomingRowIdx", null, "incoming", null, DO_SETUP_CONSTANT, GET_HASH_BUILD_INNER);

  public ValueVectorHashHelper(RecordBatch recordBatch, FragmentContext context) {
    this.recordBatch = recordBatch;
    this.context = context;
  }


  public Hash64 getHash64(LogicalExpression[] hashFieldsExp, TypedFieldId[] hashFieldIds) throws ClassTransformationException, IOException, SchemaChangeException {
    CodeGenerator<Hash64> codeGenerator = CodeGenerator.get(TEMPLATE_DEFINITION);
    codeGenerator.plainJavaCapable(true);
    //codeGenerator.saveCodeForDebugging(true);
    codeGenerator.preferPlainJava(true); // use a subclass

    ClassGenerator<Hash64> cg = codeGenerator.getRoot();
    setupBuild64Hash(cg, GetHashIncomingBuildColMapping, recordBatch, hashFieldsExp, hashFieldIds);
    Hash64 hash64 = context.getImplementationClass(codeGenerator);
    hash64.doSetup(recordBatch);
    return hash64;
  }

  private void setupBuild64Hash(ClassGenerator<Hash64> cg, MappingSet incomingMapping, VectorAccessible batch, LogicalExpression[] keyExprs, TypedFieldId[] toHashKeyFieldIds) throws SchemaChangeException {
    cg.setMappingSet(incomingMapping);
    if (keyExprs == null || keyExprs.length == 0) {
      cg.getEvalBlock()._return(JExpr.lit(0));
    }
    String seedValue = "seedValue";
    String fieldId = "fieldId";
    LogicalExpression seed = ValueExpressions.getParameterExpression(seedValue, Types.required(TypeProtos.MinorType.INT));

    LogicalExpression fieldIdParamExpr = ValueExpressions.getParameterExpression(fieldId, Types.required(TypeProtos.MinorType.INT));
    ClassGenerator.HoldingContainer fieldIdParamHolder = cg.addExpr(fieldIdParamExpr);
    int i = 0;
    for (LogicalExpression expr : keyExprs) {
      TypedFieldId targetTypeFieldId = toHashKeyFieldIds[i];
      ValueExpressions.IntExpression targetBuildFieldIdExp = new ValueExpressions.IntExpression(targetTypeFieldId.getFieldIds()[0], ExpressionPosition.UNKNOWN);

      JFieldRef targetBuildSideFieldId = cg.addExpr(targetBuildFieldIdExp, ClassGenerator.BlkCreateMode.TRUE_IF_BOUND).getValue();
      JBlock ifBlock = cg.getEvalBlock()._if(fieldIdParamHolder.getValue().eq(targetBuildSideFieldId))._then();
      cg.nestEvalBlock(ifBlock);
      LogicalExpression hashExpression = HashPrelUtil.getHash64Expression(expr, seed, true);
      LogicalExpression materializedExpr = ExpressionTreeMaterializer.materializeAndCheckErrors(hashExpression, batch, context.getFunctionRegistry());
      ClassGenerator.HoldingContainer hash = cg.addExpr(materializedExpr, ClassGenerator.BlkCreateMode.TRUE_IF_BOUND);
      ifBlock._return(hash.getValue());
      cg.unNestEvalBlock();
      i++;
    }
    cg.getEvalBlock()._return(JExpr.lit(0));
  }


  public interface Hash64 {

    public void doSetup(RecordBatch incoming) throws SchemaChangeException;

    public long hash64Code(int incomingRowIdx, int seedValue, int fieldId) throws SchemaChangeException;
  }

  public static abstract class Hash64Template implements Hash64 {
    @Override
    @RuntimeOverridden
    public abstract void doSetup(@Named("incoming") RecordBatch incoming) throws SchemaChangeException;

    @Override
    @RuntimeOverridden
    public abstract long hash64Code(@Named("incomingRowIdx") int incomingRowIdx, @Named("seedValue") int seedValue, @Named("fieldId") int fieldId) throws SchemaChangeException;
  }
}
