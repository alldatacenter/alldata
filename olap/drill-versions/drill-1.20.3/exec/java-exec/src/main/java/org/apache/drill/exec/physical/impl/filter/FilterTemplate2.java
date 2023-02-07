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
package org.apache.drill.exec.physical.impl.filter;

import javax.inject.Named;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.selection.SelectionVector2;

public abstract class FilterTemplate2 implements Filterer {
  private SelectionVector2 outgoingSelectionVector;
  private SelectionVector2 incomingSelectionVector;
  private SelectionVectorMode svMode;
  private TransferPair[] transfers;
  private RecordBatch outgoing;

  @Override
  public void setup(FragmentContext context, RecordBatch incoming,
      RecordBatch outgoing, TransferPair[] transfers) throws SchemaChangeException {
    this.transfers = transfers;
    this.outgoingSelectionVector = outgoing.getSelectionVector2();
    this.svMode = incoming.getSchema().getSelectionVectorMode();
    this.outgoing = outgoing;

    switch(svMode) {
    case NONE:
      break;
    case TWO_BYTE:
      incomingSelectionVector = incoming.getSelectionVector2();
      break;
    default:
      // SV4 is handled in FilterTemplate4
      throw new UnsupportedOperationException();
    }
    doSetup(context, incoming, outgoing);
  }

  private void doTransfers() {
    for (TransferPair t : transfers) {
      t.transfer();
    }
  }

  @Override
  public void filterBatch(int recordCount) throws SchemaChangeException {
    if (recordCount == 0) {
      outgoingSelectionVector.setRecordCount(0);
      outgoingSelectionVector.setBatchActualRecordCount(0);
      outgoing.getContainer().setEmpty();
      return;
    }
    if (! outgoingSelectionVector.allocateNewSafe(recordCount)) {
      throw new OutOfMemoryException("Unable to allocate filter batch");
    }

    switch(svMode) {
    case NONE:
      // Set the actual recordCount in outgoing selection vector to help SVRemover copy the entire
      // batch if possible at once rather than row-by-row
      outgoingSelectionVector.setBatchActualRecordCount(recordCount);
      filterBatchNoSV(recordCount);
      doTransfers();
      outgoing.getContainer().setRecordCount(recordCount);
      break;
    case TWO_BYTE:
      // Set the actual recordCount in outgoing selection vector to help SVRemover copy the entire
      // batch if possible at once rather than row-by-row
      int actualRowCount = incomingSelectionVector.getBatchActualRecordCount();
      outgoingSelectionVector.setBatchActualRecordCount(actualRowCount);
      filterBatchSV2(recordCount);
      doTransfers();
      outgoing.getContainer().setRecordCount(actualRowCount);
      break;
    default:
      throw new UnsupportedOperationException();
    }
  }

  private void filterBatchSV2(int recordCount) throws SchemaChangeException {
    int svIndex = 0;
    for (int i = 0; i < recordCount; i++) {
      int index = incomingSelectionVector.getIndex(i);
      if (doEval(index, 0)) {
        outgoingSelectionVector.setIndex(svIndex, index);
        svIndex++;
      }
    }
    outgoingSelectionVector.setRecordCount(svIndex);
  }

  private void filterBatchNoSV(int recordCount) throws SchemaChangeException {
    int svIndex = 0;
    for (int i = 0; i < recordCount; i++) {
      if (doEval(i, 0)) {
        outgoingSelectionVector.setIndex(svIndex, (char)i);
        svIndex++;
      }
    }
    outgoingSelectionVector.setRecordCount(svIndex);
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") RecordBatch incoming,
                               @Named("outgoing") RecordBatch outgoing)
                       throws SchemaChangeException;
  public abstract boolean doEval(@Named("inIndex") int inIndex,
                                 @Named("outIndex") int outIndex)
                          throws SchemaChangeException;

  @Override
  public String toString() {
    return "FilterTemplate2[outgoingSelectionVector=" + outgoingSelectionVector
        + ", incomingSelectionVector=" + incomingSelectionVector
        + ", svMode=" + svMode
        + "]";
  }
}
