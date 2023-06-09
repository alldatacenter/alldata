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
package org.apache.drill.exec.physical.impl.unnest;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.apache.drill.exec.record.BatchSchema.SelectionVectorMode.NONE;

/**
 * Contains the actual unnest operation. Unnest is a simple transfer operation in this implementation.
 * Additionally, unnest produces an implicit rowId column that allows unnest to output batches with many
 * rows of incoming data being unnested in a single call to innerNext(). Downstream blocking operators need
 * to be aware of this rowId column and include the rowId as the sort or group by key.
 * This class follows the pattern of other operators that generate code at runtime. Normally this class
 * would be abstract and have placeholders for doSetup and doEval. Unnest however, doesn't require code
 * generation so we can simply implement the code in a simple class that looks similar to the code gen
 * templates used by other operators but does not implement the doSetup and doEval methods.
 */
public class UnnestImpl implements Unnest {
  private static final Logger logger = LoggerFactory.getLogger(UnnestImpl.class);

  private ImmutableList<TransferPair> transfers;
  private SelectionVectorMode svMode;
  private RepeatedValueVector fieldToUnnest;
  private RepeatedValueVector.RepeatedAccessor accessor;
  private RecordBatch outgoing;

  private IntVector rowIdVector; // Allocated and owned by the UnnestRecordBatch
  private IntVector.Mutator rowIdVectorMutator;

  /**
   * The output batch limit starts at OUTPUT_ROW_COUNT, but may be decreased
   * if records are found to be large.
   */
  private int outputLimit = ValueVector.MAX_ROW_COUNT;



  /**
   * We maintain three indexes
   *
   *
   *
                valueIndex  0         1       2   3
                            |- - - - -|- - - -|- -|- - - -|
                            | | | | | | | | | | | | | | | |
                            |- - - - -|- - - -|- -|- - - -|
           innerValueIndex  0 1 2 3 4 0 1 2 3 0 1 0 1 2 3 |
     runningInnerValueIndex 0 1 2 3 4 5 6 7 8 9 ...
   *
   *
   *
   */
  private int valueIndex; // index in the incoming record being processed
  // The index of the array element in the unnest column at row pointed by valueIndex which is currently being
  // processed. It starts at zero and continue until InnerValueCount is reached or the batch limit is reached. It
  // allows for groups to be written across batches if we run out of space. For cases where we have finished
  // a batch on the boundary it will be set to 0
  private int innerValueIndex = 0;
  // The index in the "values" vector of the current value being processed.
  private int runningInnerValueIndex;



  @Override
  public void setUnnestField(RepeatedValueVector unnestField) {
    this.fieldToUnnest = unnestField;
    this.accessor = RepeatedValueVector.RepeatedAccessor.class.cast(unnestField.getAccessor());
  }

  @Override
  public RepeatedValueVector getUnnestField() {
    return fieldToUnnest;
  }

  @Override
  public void setOutputCount(int outputCount) {
    outputLimit = outputCount;
  }

  @Override
  public void setRowIdVector(IntVector v) {
    this.rowIdVector = v;
    this.rowIdVectorMutator = rowIdVector.getMutator();
  }

  @Override
  public final int unnestRecords(final int recordCount) {
    Preconditions.checkArgument(svMode == NONE, "Unnest does not support selection vector inputs.");

    final int initialInnerValueIndex = runningInnerValueIndex;
    int nonEmptyArray = 0;

    outer:
    {
      int outputIndex = 0; // index in the output vector that we are writing to
      final int valueCount = accessor.getValueCount();

      for (; valueIndex < valueCount; valueIndex++) {
        final int innerValueCount = accessor.getInnerValueCountAt(valueIndex);
        logger.trace("Unnest: CurrentRowId: {}, innerValueCount: {}, outputIndex: {},  output limit: {}",
            valueIndex, innerValueCount, outputIndex, outputLimit);

        if (innerValueCount > 0) {
          ++nonEmptyArray;
        }

        for (; innerValueIndex < innerValueCount; innerValueIndex++) {
          // If we've hit the batch size limit, stop and flush what we've got so far.
          if (outputIndex == outputLimit) {
            // Flush this batch.
            break outer;
          }
          try {
            // rowId starts at 1, so the value for rowId is valueIndex+1
            rowIdVectorMutator.setSafe(outputIndex, valueIndex + 1);

          } finally {
            outputIndex++;
            runningInnerValueIndex++;
          }
        }
        innerValueIndex = 0;
      }  // forevery value in the array
    }  // for every incoming record
    final int delta = runningInnerValueIndex - initialInnerValueIndex;
    logger.debug("Unnest: Finished processing current batch. [Details: LastProcessedRowIndex: {}, " +
      "RowsWithNonEmptyArrays: {}, outputIndex: {}, outputLimit: {}, TotalIncomingRecords: {}]",
      valueIndex, nonEmptyArray, delta, outputLimit, accessor.getValueCount());
    final SchemaChangeCallBack callBack = new SchemaChangeCallBack();
    for (TransferPair t : transfers) {
      t.splitAndTransfer(initialInnerValueIndex, delta);

      // Get the corresponding ValueVector in output container and transfer the data
      final ValueVector vectorWithData = t.getTo();
      final ValueVector outputVector = outgoing.getContainer().addOrGet(vectorWithData.getField(), callBack);
      Preconditions.checkState(!callBack.getSchemaChangedAndReset(), "Outgoing container doesn't have "
          + "expected ValueVector of type %s, present in TransferPair of unnest field", vectorWithData.getClass());
      vectorWithData.makeTransferPair(outputVector).transfer();
    }
    return delta;
  }

  @Override
  public final void setup(FragmentContext context, RecordBatch incoming, RecordBatch outgoing,
      List<TransferPair> transfers) throws SchemaChangeException {

    this.svMode = incoming.getSchema().getSelectionVectorMode();
    this.outgoing = outgoing;
    if (svMode == NONE) {
      this.transfers = ImmutableList.copyOf(transfers);
    } else {
      throw new UnsupportedOperationException("Unnest does not support selection vector inputs.");
    }
  }

  @Override
  public void resetGroupIndex() {
    this.valueIndex = 0;
    this.innerValueIndex = 0;
    this.runningInnerValueIndex = 0;
  }

  @Override
  public void close() {
    if (transfers != null) {
      for (TransferPair tp : transfers) {
        tp.getTo().close();
      }
      transfers = null;
    }
  }

  @Override
  public String toString() {
    return "UnnestImpl[svMode=" + svMode
        + ", outputLimit=" + outputLimit
        + ", valueIndex=" + valueIndex
        + ", innerValueIndex=" + innerValueIndex
        + ", runningInnerValueIndex=" + runningInnerValueIndex
        + "]";
  }
}
