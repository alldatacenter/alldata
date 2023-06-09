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
package org.apache.drill.exec.physical.impl.union;

import java.util.List;

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public abstract class UnionAllerTemplate implements UnionAller {

  private ImmutableList<TransferPair> transfers;

  @Override
  public final int unionRecords(int startIndex, final int recordCount, int firstOutputIndex) {
    try {
      for (int i = startIndex; i < startIndex + recordCount; i++, firstOutputIndex++) {
        doEval(i, firstOutputIndex);
      }
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }

    for (TransferPair t : transfers) {
      t.splitAndTransfer(startIndex, recordCount);
    }
    return recordCount;
  }

  @Override
  public final void setup(FragmentContext context, RecordBatch incoming, RecordBatch outgoing,
      List<TransferPair> transfers) throws SchemaChangeException{
    this.transfers = ImmutableList.copyOf(transfers);
    doSetup(context, incoming, outgoing);
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") RecordBatch incoming,
                               @Named("outgoing") RecordBatch outgoing)
                       throws SchemaChangeException;
  public abstract void doEval(@Named("inIndex") int inIndex,
                              @Named("outIndex") int outIndex)
                       throws SchemaChangeException;
}
