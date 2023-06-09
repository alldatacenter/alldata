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

import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector4;

public abstract class AbstractSV4Copier extends AbstractCopier {
  // Storing VectorWrapper reference instead of ValueVector[]. With EMIT outcome
  // support underlying operator
  // operator can generate multiple output batches with no schema changes which
  // will change the ValueVector[]
  // reference but not VectorWrapper reference.
  protected VectorWrapper<?>[] vvIn;
  private SelectionVector4 sv4;

  @Override
  public void setup(VectorAccessible incoming, VectorContainer outgoing) {
    super.setup(incoming, outgoing);
    sv4 = incoming.getSelectionVector4();

    int count = outgoing.getNumberOfColumns();
    vvIn = new VectorWrapper[count];

    int index = 0;
    for (VectorWrapper<?> vectorWrapper: incoming) {
      vvIn[index++] = vectorWrapper;
    }
  }

  @Override
  public void copyEntryIndirect(int inIndex, int outIndex) {
    copyEntry(sv4.get(inIndex), outIndex);
  }
}
