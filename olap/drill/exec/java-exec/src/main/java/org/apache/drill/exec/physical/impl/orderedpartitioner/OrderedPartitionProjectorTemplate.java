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
package org.apache.drill.exec.physical.impl.orderedpartitioner;

import java.util.List;

import javax.inject.Named;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.IntVector;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OrderedPartitionProjectorTemplate implements OrderedPartitionProjector {
  static final Logger logger = LoggerFactory.getLogger(OrderedPartitionProjectorTemplate.class);

  private ImmutableList<TransferPair> transfers;
  private int partitions;
  private SelectionVectorMode svMode;
  private RecordBatch outBatch;
  private IntVector partitionValues;

  public OrderedPartitionProjectorTemplate() throws SchemaChangeException{
  }

  private int getPartition(int index) {
    //TODO replace this with binary search
    int partitionIndex = 0;
    try {
      while (partitionIndex < partitions - 1 && doEval(index, partitionIndex) >= 0) {
        partitionIndex++;
      }
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
    return partitionIndex;
  }

  @Override
  public final int projectRecords(final int recordCount, int firstOutputIndex) {
    final int countN = recordCount;
    int counter = 0;
    for (int i = 0; i < countN; i++, firstOutputIndex++) {
      int partition = getPartition(i);
      partitionValues.getMutator().setSafe(i, partition);
      counter++;
    }
    for(TransferPair t : transfers){
        t.transfer();
    }
    return counter;
  }

  @Override
  public final void setup(FragmentContext context, VectorAccessible incoming, RecordBatch outgoing, List<TransferPair> transfers,
                          VectorContainer partitionVectors, int partitions, SchemaPath outputField)  throws SchemaChangeException{

    this.svMode = incoming.getSchema().getSelectionVectorMode();
    this.outBatch = outgoing;
    partitionValues = (IntVector) outBatch.getValueAccessorById(IntVector.class, outBatch.getValueVectorId(outputField).getFieldIds()).getValueVector();
    switch(svMode){
    case FOUR_BYTE:
    case TWO_BYTE:
      throw new UnsupportedOperationException("Selection vector not supported");
    default:
    }
    this.transfers = ImmutableList.copyOf(transfers);
    this.partitions = partitions;
    doSetup(context, incoming, outgoing, partitionVectors);
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") VectorAccessible incoming,
                               @Named("outgoing") RecordBatch outgoing,
                               @Named("partitionVectors") VectorContainer partitionVectors)
                       throws SchemaChangeException;
  public abstract int doEval(@Named("inIndex") int inIndex,
                             @Named("partitionIndex") int partitionIndex)
                      throws SchemaChangeException;
}
