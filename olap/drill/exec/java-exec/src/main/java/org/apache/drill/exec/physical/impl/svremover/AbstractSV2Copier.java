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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.vector.ValueVector;

public abstract class AbstractSV2Copier extends AbstractCopier {
  protected ValueVector[] vvIn;
  private SelectionVector2 sv2;
  protected List<TransferPair> transferPairs = new ArrayList<>();

  @Override
  public void setup(VectorAccessible incoming, VectorContainer outgoing) {
    super.setup(incoming, outgoing);
    sv2 = incoming.getSelectionVector2();

    final int count = outgoing.getNumberOfColumns();
    vvIn = new ValueVector[count];

    int index = 0;
    for (VectorWrapper<?> vectorWrapper: incoming) {
      vvIn[index++] = vectorWrapper.getValueVector();
    }
  }

  @Override
  public void copyEntryIndirect(int inIndex, int outIndex) {
    copyEntry(sv2.getIndex(inIndex), outIndex);
  }

  @Override
  public int copyRecords(int index, int recordCount) {
    if (sv2.canDoFullTransfer()) {
      for (TransferPair pair : transferPairs) {
        pair.transfer();
      }
      updateCounts(recordCount);
      return recordCount;
    }

    return super.copyRecords(index, recordCount);
  }
}
