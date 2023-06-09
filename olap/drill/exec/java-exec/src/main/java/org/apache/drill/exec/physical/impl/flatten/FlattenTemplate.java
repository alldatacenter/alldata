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
package org.apache.drill.exec.physical.impl.flatten;

import java.util.List;

import javax.inject.Named;

import org.apache.drill.exec.exception.OversizedAllocationException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FlattenTemplate implements Flattener {
  private static final Logger logger = LoggerFactory.getLogger(FlattenTemplate.class);

  private static final int OUTPUT_ROW_COUNT = ValueVector.MAX_ROW_COUNT;

  private ImmutableList<TransferPair> transfers;
  private SelectionVectorMode svMode;
  private RepeatedValueVector fieldToFlatten;
  private RepeatedValueVector.RepeatedAccessor accessor;
  private int valueIndex;

  /**
   * The output batch limit starts at OUTPUT_ROW_COUNT, but may be decreased
   * if records are found to be large.
   */
  private int outputLimit = OUTPUT_ROW_COUNT;

  // this allows for groups to be written between batches if we run out of space, for cases where we have finished
  // a batch on the boundary it will be set to 0
  private int innerValueIndex = -1;
  private int currentInnerValueIndex;

  @Override
  public void setFlattenField(RepeatedValueVector flattenField) {
    this.fieldToFlatten = flattenField;
    this.accessor = RepeatedValueVector.RepeatedAccessor.class.cast(flattenField.getAccessor());
  }

  @Override
  public RepeatedValueVector getFlattenField() {
    return fieldToFlatten;
  }

  @Override
  public void setOutputCount(int outputCount) {
    outputLimit = outputCount;
  }

  @Override
  public final int flattenRecords(final int recordCount, final int firstOutputIndex,
      final Flattener.Monitor monitor) {
    switch (svMode) {
      case FOUR_BYTE:
        throw new UnsupportedOperationException("Flatten does not support selection vector inputs.");

      case TWO_BYTE:
        throw new UnsupportedOperationException("Flatten does not support selection vector inputs.");

      case NONE:
        if (innerValueIndex == -1) {
          innerValueIndex = 0;
        }

        final int initialInnerValueIndex = currentInnerValueIndex;
        // restore state to local stack
        int valueIndexLocal = valueIndex;
        int innerValueIndexLocal = innerValueIndex;
        int currentInnerValueIndexLocal = currentInnerValueIndex;
        outer: {
          int outputIndex = firstOutputIndex;
          int recordsThisCall = 0;
          final int valueCount = accessor.getValueCount();
          for ( ; valueIndexLocal < valueCount; valueIndexLocal++) {
            final int innerValueCount = accessor.getInnerValueCountAt(valueIndexLocal);
            for ( ; innerValueIndexLocal < innerValueCount; innerValueIndexLocal++) {
              // If we've hit the batch size limit, stop and flush what we've got so far.
              if (recordsThisCall == outputLimit) {
                // Flush this batch.
                break outer;
              }

              try {
                doEval(valueIndexLocal, outputIndex);
              } catch (OversizedAllocationException ex) {
                // unable to flatten due to a soft buffer overflow. split the batch here and resume execution.
                logger.debug("Reached allocation limit. Splitting the batch at input index: {} - inner index: {} - current completed index: {}",
                    valueIndexLocal, innerValueIndexLocal, currentInnerValueIndexLocal);

                /*
                 * TODO
                 * We can't further reduce the output limits here because it won't have
                 * any effect. The vectors have already gotten large, and there's currently
                 * no way to reduce their size. Ideally, we could reduce the outputLimit,
                 * and reduce the size of the currently used vectors.
                 */
                break outer;
              } catch (SchemaChangeException e) {
                throw new UnsupportedOperationException(e);
              }
              outputIndex++;
              currentInnerValueIndexLocal++;
              ++recordsThisCall;
            }
            innerValueIndexLocal = 0;
          }
        }
        // save state to heap
        valueIndex = valueIndexLocal;
        innerValueIndex = innerValueIndexLocal;
        currentInnerValueIndex = currentInnerValueIndexLocal;
        // transfer the computed range
        final int delta = currentInnerValueIndexLocal - initialInnerValueIndex;
        for (TransferPair t : transfers) {
          t.splitAndTransfer(initialInnerValueIndex, delta);
        }
        return delta;

      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public final void setup(FragmentContext context, RecordBatch incoming, RecordBatch outgoing, List<TransferPair> transfers)  throws SchemaChangeException{

    this.svMode = incoming.getSchema().getSelectionVectorMode();
    switch (svMode) {
      case FOUR_BYTE:
        throw new UnsupportedOperationException("Flatten does not support selection vector inputs.");
      case TWO_BYTE:
        throw new UnsupportedOperationException("Flatten does not support selection vector inputs.");
      default:
    }
    this.transfers = ImmutableList.copyOf(transfers);
    doSetup(context, incoming, outgoing);
  }

  @Override
  public void resetGroupIndex() {
    valueIndex = 0;
    currentInnerValueIndex = 0;
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") RecordBatch incoming,
                               @Named("outgoing") RecordBatch outgoing) throws SchemaChangeException;
  public abstract boolean doEval(@Named("inIndex") int inIndex,
                                 @Named("outIndex") int outIndex) throws SchemaChangeException;

  @Override
  public String toString() {
    return "FlattenTemplate[svMode=" + svMode
        + ", fieldToFlatten=" + fieldToFlatten
        + ", valueIndex=" + valueIndex
        + ", outputLimit=" + outputLimit
        + ", innerValueIndex=" + innerValueIndex
        + ", currentInnerValueIndex=" + currentInnerValueIndex
        + "]";
  }
}
