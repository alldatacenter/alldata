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
package org.apache.drill.exec.vector;

import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JVar;

public class CopyUtil {

  public static void generateCopies(ClassGenerator<?> g, VectorAccessible batch, boolean hyper) {
    // we have parallel ids for each value vector so we don't actually have to deal with managing the ids at all.
    int fieldId = 0;

    JExpression inIndex = JExpr.direct("inIndex");
    JExpression outIndex = JExpr.direct("outIndex");
    for (VectorWrapper<?> vv : batch) {
      String copyMethod;
      if (!Types.isFixedWidthType(vv.getField().getType()) ||
           Types.isRepeated(vv.getField().getType()) ||
           Types.isComplex(vv.getField().getType())) {
        copyMethod = "copyFromSafe";
      } else {
        copyMethod = "copyFrom";
      }
      g.rotateBlock();
      TypedFieldId inFieldId = new TypedFieldId.Builder().finalType(vv.getField().getType())
          .hyper(vv.isHyper())
          .addId(fieldId)
          .build();
      JVar inVV = g.declareVectorValueSetupAndMember("incoming", inFieldId);
      TypedFieldId outFieldId = new TypedFieldId.Builder().finalType(vv.getField().getType())
          .hyper(false)
          .addId(fieldId)
          .build();
      JVar outVV = g.declareVectorValueSetupAndMember("outgoing", outFieldId);

      if (hyper) {
        g.getEvalBlock().add(outVV
                        .invoke(copyMethod)
                        .arg(inIndex.band(JExpr.lit((int) Character.MAX_VALUE)))
                        .arg(outIndex)
                        .arg(inVV.component(inIndex.shrz(JExpr.lit(16)))));
      } else {
        g.getEvalBlock().add(outVV.invoke(copyMethod).arg(inIndex).arg(outIndex).arg(inVV));
      }

      g.rotateBlock();
      fieldId++;
    }
  }
}
