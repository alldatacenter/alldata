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

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.selection.SelectionVector4;

public abstract class FilterTemplate4 implements Filterer {
  private SelectionVector4 outgoingSelectionVector;
  private SelectionVector4 incomingSelectionVector;
  private TransferPair[] transfers;

  @Override
  public void setup(FragmentContext context, RecordBatch incoming, RecordBatch outgoing, TransferPair[] transfers)
      throws SchemaChangeException {
    this.transfers = transfers;
    this.outgoingSelectionVector = outgoing.getSelectionVector4();
    this.incomingSelectionVector = incoming.getSelectionVector4();
    doSetup(context, incoming, outgoing);
  }

  @Override
  public void filterBatch(int recordCount){
    if (recordCount == 0) {
      return;
    }
    int outPos = 0;
    for (int i = 0; i < incomingSelectionVector.getCount(); i++) {
      int index = incomingSelectionVector.get(i);
      if (doEval(index, 0)) {
        outgoingSelectionVector.set(outPos++, index);
      }
    }
    outgoingSelectionVector.setCount(outPos);
    doTransfers();
  }

  private void doTransfers(){
    for(TransferPair t : transfers){
      t.transfer();
    }
  }

  public abstract void doSetup(@Named("context") FragmentContext context, @Named("incoming") RecordBatch incoming, @Named("outgoing") RecordBatch outgoing);
  public abstract boolean doEval(@Named("inIndex") int inIndex, @Named("outIndex") int outIndex);

  @Override
  public String toString() {
    return "FilterTemplate4[outgoingSelectionVector=" + outgoingSelectionVector
        + ", incomingSelectionVector=" + incomingSelectionVector
        + "]";
  }
}
