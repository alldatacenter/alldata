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
import org.apache.drill.exec.vector.ValueVector;

import static org.apache.drill.exec.physical.impl.svremover.AbstractCopier.allocateOutgoing;

public class GenericCopier implements Copier {
  private ValueVector[] vvOut;
  private ValueVector[] vvIn;

  private VectorContainer outgoing;

  @Override
  public void setup(VectorAccessible incoming, VectorContainer outgoing) {
    this.outgoing = outgoing;

    final int count = outgoing.getNumberOfColumns();

    vvIn = new ValueVector[count];
    vvOut = new ValueVector[count];

    {
      int index = 0;

      for (VectorWrapper<?> vectorWrapper: incoming) {
        vvIn[index] = vectorWrapper.getValueVector();
        index++;
      }
    }

    for (int index = 0; index < count; index++) {
      vvOut[index] = outgoing.getValueVector(index).getValueVector();
    }
  }

  @Override
  public int copyRecords(int index, int recordCount) {
    allocateOutgoing(outgoing, recordCount);
    return insertRecords(0, index, recordCount);
  }

  @Override
  public int appendRecord(int index) {
    int outgoingPosition = outgoing.getRecordCount();
    for (int vectorIndex = 0; vectorIndex < vvIn.length; vectorIndex++) {
      vvOut[vectorIndex].copyEntry(outgoingPosition, vvIn[vectorIndex], index);
    }
    outgoingPosition++;
    updateCounts(outgoingPosition);
    return outgoingPosition;
  }

  @Override
  public int appendRecords(int index, int recordCount) {
    return insertRecords(outgoing.getRecordCount(), index, recordCount);
  }

  private int insertRecords(int outgoingPosition, int startIndex, int recordCount) {
    final int endIndex = startIndex + recordCount;

    for (int index = startIndex; index < endIndex; index++, outgoingPosition++) {
      for (int vectorIndex = 0; vectorIndex < vvIn.length; vectorIndex++) {
        vvOut[vectorIndex].copyEntry(outgoingPosition, vvIn[vectorIndex], index);
      }
    }

    updateCounts(outgoingPosition);
    return outgoingPosition;
  }

  private void updateCounts(int numRecords) {
    outgoing.setRecordCount(numRecords);

    for (int vectorIndex = 0; vectorIndex < vvOut.length; vectorIndex++) {
      vvOut[vectorIndex].getMutator().setValueCount(numRecords);
    }
  }
}
