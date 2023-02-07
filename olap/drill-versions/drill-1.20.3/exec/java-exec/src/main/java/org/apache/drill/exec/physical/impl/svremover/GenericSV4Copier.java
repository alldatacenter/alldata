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
package org.apache.drill.exec.physical.impl.svremover;

import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.ValueVector;

public class GenericSV4Copier extends AbstractSV4Copier {

  public GenericSV4Copier(RecordBatch incomingBatch, VectorContainer outputContainer,
                          SchemaChangeCallBack callBack) {
    for (VectorWrapper<?> vv : incomingBatch) {
      ValueVector v = vv.getValueVectors()[0];
      v.makeTransferPair(outputContainer.addOrGet(v.getField(), callBack));
    }
  }

  @Override
  public void copyEntry(int inIndex, int outIndex) {
    int inOffset = inIndex & 0xFFFF;
    int inVector = inIndex >>> 16;
    for (int i = 0;  i < vvIn.length;  i++) {
      ValueVector[] vectorsFromIncoming = vvIn[i].getValueVectors();
      vvOut[i].copyEntry(outIndex, vectorsFromIncoming[inVector], inOffset);
    }
  }
}