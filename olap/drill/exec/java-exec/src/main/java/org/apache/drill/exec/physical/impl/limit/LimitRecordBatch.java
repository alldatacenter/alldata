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
package org.apache.drill.exec.physical.impl.limit;

import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.Limit;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessibleUtilities;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;

public class LimitRecordBatch extends AbstractSingleRecordBatch<Limit> {
  private static final Logger logger = LoggerFactory.getLogger(LimitRecordBatch.class);

  private final SelectionVector2 outgoingSv;
  private SelectionVector2 incomingSv;

  // Start offset of the records
  private int recordStartOffset;
  private int numberOfRecords;
  private boolean first = true;
  private final List<TransferPair> transfers = Lists.newArrayList();

  public LimitRecordBatch(Limit popConfig, FragmentContext context, RecordBatch incoming)
      throws OutOfMemoryException {
    super(popConfig, context, incoming);
    outgoingSv = new SelectionVector2(oContext.getAllocator());
    refreshLimitState();
  }

  @Override
  public IterOutcome innerNext() {
    if (!first && !needMoreRecords(numberOfRecords)) {
      outgoingSv.setRecordCount(0);
      incoming.cancel();
      IterOutcome upStream = next(incoming);

      while (upStream == IterOutcome.OK || upStream == IterOutcome.OK_NEW_SCHEMA) {
        // Clear the memory for the incoming batch
        VectorAccessibleUtilities.clear(incoming);

        // clear memory for incoming sv (if any)
        if (incomingSv != null) {
          incomingSv.clear();
        }
        upStream = next(incoming);
      }
      // If EMIT that means leaf operator is UNNEST, in this case refresh the limit states and return EMIT.
      if (upStream == EMIT) {
        // Clear the memory for the incoming batch
        VectorAccessibleUtilities.clear(incoming);

        // clear memory for incoming sv (if any)
        if (incomingSv != null) {
          incomingSv.clear();
        }

        refreshLimitState();
        return upStream;
      }
      // other leaf operator behave as before.
      return NONE;
    }
    return super.innerNext();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    return outgoingSv;
  }

  @Override
  public int getRecordCount() {
    return outgoingSv.getCount();
  }

  @Override
  public void close() {
    outgoingSv.clear();
    super.close();
  }

  @Override
  protected boolean setupNewSchema() {
    container.clear();
    transfers.clear();

    for (final VectorWrapper<?> v : incoming) {
      final TransferPair pair = v.getValueVector().makeTransferPair(
        container.addOrGet(v.getField(), callBack));
      transfers.add(pair);
    }

    final BatchSchema.SelectionVectorMode svMode = incoming.getSchema().getSelectionVectorMode();

    switch (svMode) {
      case NONE:
        break;
      case TWO_BYTE:
        this.incomingSv = incoming.getSelectionVector2();
        break;
      default:
        throw new UnsupportedOperationException();
    }

    if (container.isSchemaChanged()) {
      container.buildSchema(BatchSchema.SelectionVectorMode.TWO_BYTE);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Gets the outcome to return from super implementation and then in case of
   * EMIT outcome it refreshes the state of operator. Refresh is done to again
   * apply limit on all the future incoming batches which will be part of next
   * record boundary.
   *
   * @return - IterOutcome to send downstream
   */
  @Override
  protected IterOutcome getFinalOutcome(boolean hasRemainder) {
    final IterOutcome outcomeToReturn = super.getFinalOutcome(hasRemainder);

    // EMIT outcome means leaf operator is UNNEST, hence refresh the state no matter limit is reached or not.
    if (outcomeToReturn == EMIT) {
      refreshLimitState();
    }
    return outcomeToReturn;
  }

  @Override
  protected IterOutcome doWork() {
    if (first) {
      first = false;
    }
    final int inputRecordCount = incoming.getRecordCount();
    if (inputRecordCount == 0) {
      setOutgoingRecordCount(0);
      container.setEmpty();
      return getFinalOutcome(false);
    }

    for (final TransferPair tp : transfers) {
      tp.transfer();
    }
    // Check if current input record count is less than start offset. If yes then adjust the start offset since we
    // have to ignore all these records and return empty batch.
    if (inputRecordCount <= recordStartOffset) {
      recordStartOffset -= inputRecordCount;
      setOutgoingRecordCount(0);
      container.setEmpty();
    } else {
      // Allocate SV2 vectors for the record count size since we transfer all the vectors buffer from input record
      // batch to output record batch and later an SV2Remover copies the needed records.
      outgoingSv.allocateNew(inputRecordCount);
      limit(inputRecordCount);
    }

    // clear memory for incoming sv (if any)
    if (incomingSv != null) {
      int incomingCount = incomingSv.getBatchActualRecordCount();
      outgoingSv.setBatchActualRecordCount(incomingCount);
      container.setRecordCount(incomingCount);
      incomingSv.clear();
    }

    return getFinalOutcome(false);
  }

  /**
   * limit call when incoming batch has number of records more than the start
   * offset such that it can produce some output records. After first call of
   * this method recordStartOffset should be 0 since we have already skipped the
   * required number of records as part of first incoming record batch.
   *
   * @param inputRecordCount
   *          number of records in incoming batch
   */
  private void limit(int inputRecordCount) {
    int endRecordIndex;

    if (numberOfRecords == Integer.MIN_VALUE) {
      endRecordIndex = inputRecordCount;
    } else {
      endRecordIndex = Math.min(inputRecordCount, recordStartOffset + numberOfRecords);
      numberOfRecords -= Math.max(0, endRecordIndex - recordStartOffset);
    }

    int svIndex = 0;
    for (int i = recordStartOffset; i < endRecordIndex; svIndex++, i++) {
      if (incomingSv != null) {
        outgoingSv.setIndex(svIndex, incomingSv.getIndex(i));
      } else {
        outgoingSv.setIndex(svIndex, (char) i);
      }
    }
    outgoingSv.setRecordCount(svIndex);
    outgoingSv.setBatchActualRecordCount(inputRecordCount);
    // Actual number of values in the container; not the number in
    // the SV. Set record count, not value count. Value count is
    // carried over from input vectors.
    container.setRecordCount(inputRecordCount);
    // Update the start offset
    recordStartOffset = 0;
  }

  private void setOutgoingRecordCount(int outputCount) {
    outgoingSv.setRecordCount(outputCount);
    outgoingSv.setBatchActualRecordCount(outputCount);
  }

  /**
   * Method which returns if more output records are needed from LIMIT operator.
   * When numberOfRecords is set to {@link Integer#MIN_VALUE} that means there
   * is no end bound on LIMIT, so get all the records past start offset.
   *
   * @return - true - more output records is expected.
   *           false - limit bound is reached and no more record is expected
   */
  private boolean needMoreRecords(int recordsToRead) {
    boolean readMore = true;

    Preconditions.checkState(recordsToRead == Integer.MIN_VALUE || recordsToRead >= 0,
      String.format("Invalid value of numberOfRecords %d inside LimitRecordBatch", recordsToRead));

    // Above check makes sure that either numberOfRecords has no bound or if it has bounds then either we have read
    // all the records or still left to read some.
    // Below check just verifies if there is bound on numberOfRecords and we have read all of it.
    if (recordsToRead == 0) {
      readMore = false;
    }
    return readMore;
  }

  /**
   * Reset the states for recordStartOffset and numberOfRecords based on the
   * popConfig passed to the operator. This method is called for the outcome
   * EMIT no matter if limit is reached or not.
   */
  private void refreshLimitState() {
    // Make sure startOffset is non-negative
    recordStartOffset = Math.max(0, popConfig.getFirst());
    numberOfRecords = (popConfig.getLast() == null) ?
      Integer.MIN_VALUE : Math.max(0, popConfig.getLast()) - recordStartOffset;
    first = true;
  }

  @Override
  public void dump() {
    logger.error("LimitRecordBatch[container={}, offset={}, numberOfRecords={}, incomingSV={}, outgoingSV={}]",
        container, recordStartOffset, numberOfRecords, incomingSv, outgoingSv);
  }
}
