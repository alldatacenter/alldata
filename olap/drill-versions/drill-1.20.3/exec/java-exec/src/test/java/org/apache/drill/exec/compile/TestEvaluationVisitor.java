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
package org.apache.drill.exec.compile;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.parser.LogicalExpressionParser;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.EvaluationVisitor;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.ValueVectorWriteExpression;
import org.apache.drill.exec.physical.impl.project.Projector;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

public class TestEvaluationVisitor extends BaseTest {

  @Test
  public void testEvaluation() {
    EvaluationVisitor v = new EvaluationVisitor();
    CodeGenerator<?> g = CodeGenerator.get(Projector.TEMPLATE_DEFINITION, null);
    SchemaPath path = (SchemaPath) LogicalExpressionParser.parse("a.b[4][2].c[6]");

    TypedFieldId id = TypedFieldId.newBuilder()
      .addId(1)
      .addId(3)
      .remainder(path.getRootSegment())
      .intermediateType(Types.optional(MinorType.MAP))
      .finalType(Types.repeated(MinorType.MAP))
      .hyper(true)
      .withIndex()
      .build();

    ValueVectorReadExpression e = new ValueVectorReadExpression(id);

    TypedFieldId outId = TypedFieldId.newBuilder()
        .addId(1)
        .finalType(Types.repeated(MinorType.MAP))
        .intermediateType(Types.repeated(MinorType.MAP))
        .build();
    ValueVectorWriteExpression e2 = new ValueVectorWriteExpression(outId, e, true);

    v.addExpr(e2,  g.getRoot());
  }

}
