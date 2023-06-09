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

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;

import java.util.List;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.PartitionLimit;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps to perform limit in a partition within a record batch. Currently only
 * integer type of partition column is supported. This is mainly used for
 * Lateral/Unnest subquery where each output batch from Unnest will contain an
 * implicit column for rowId for each row.
 */
public class PartitionLimitRecordBatch extends AbstractSingleRecordBatch<PartitionLimit> {

  private static final Logger logger = LoggerFactory.getLogger(PartitionLimitRecordBatch.class);

  private final SelectionVector2 outgoingSv;
  private SelectionVector2 incomingSv;

  // Start offset of the records
  private int recordStartOffset;
  private int numberOfRecords;
  private final List<TransferPair> transfers = Lists.newArrayList();

  // Partition RowId which is currently being processed, this will help to
  // handle cases when rows for a partition id flows across 2 batches
  private int partitionId;
  private IntVector partitionColumn;

  public PartitionLimitRecordBatch(PartitionLimit popConfig, FragmentContext context, RecordBatch incoming)
      throws OutOfMemoryException {
    super(popConfig, context, incoming);
    outgoingSv = new SelectionVector2(oContext.getAllocator());
    refreshLimitState();
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
    transfers.clear();
    super.close();
  }

  @Override
  protected boolean setupNewSchema() {
    container.clear();
    transfers.clear();

    for(VectorWrapper<?> v : incoming) {
      TransferPair pair = v.getValueVector().makeTransferPair(
        container.addOrGet(v.getField(), callBack));
      transfers.add(pair);

      // Hold the transfer pair target vector for partitionColumn, since before
      // applying limit it transfer all rows from incoming to outgoing batch
      String fieldName = v.getField().getName();
      if (fieldName.equals(popConfig.getPartitionColumn())) {
        partitionColumn = (IntVector) pair.getTo();
      }
    }

    BatchSchema.SelectionVectorMode svMode = incoming.getSchema().getSelectionVectorMode();

    switch(svMode) {
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
    }

    return false;
  }

  /**
   * Gets the outcome to return from super implementation and then in case of
   * EMIT outcome it refreshes the state of operator. Refresh is done to again
   * apply limit on all the future incoming batches which will be part of next
   * record boundary.
   *
   * @param hasRemainder
   * @return - IterOutcome to send downstream
   */
  @Override
  protected IterOutcome getFinalOutcome(boolean hasRemainder) {
    IterOutcome outcomeToReturn = super.getFinalOutcome(hasRemainder);

    // EMIT outcome means leaf operator is UNNEST, hence refresh the state no matter limit is reached or not.
    if (outcomeToReturn == EMIT) {
      refreshLimitState();
    }
    return outcomeToReturn;
  }

  @Override
  protected IterOutcome doWork() {
    int inputRecordCount = incoming.getRecordCount();
    if (inputRecordCount == 0) {
      incoming.getContainer().zeroVectors();
      outgoingSv.setRecordCount(0);
      outgoingSv.setBatchActualRecordCount(0);
      container.setEmpty();
      // Release buffer for sv2 (if any)
      if (incomingSv != null) {
        incomingSv.clear();
      }
      return getFinalOutcome(false);
    }

    for (TransferPair tp : transfers) {
      tp.transfer();
    }

    // Allocate SV2 vectors for the record count size since we transfer all the vectors buffer from input record
    // batch to output record batch and later an SV2Remover copies the needed records.
    outgoingSv.allocateNew(inputRecordCount);
    limit(inputRecordCount);

    // Release memory for incoming sv (if any)
    if (incomingSv != null) {
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
   *          - number of records in incoming batch
   */
  private void limit(int inputRecordCount) {
    boolean outputAllRecords = (numberOfRecords == Integer.MIN_VALUE);

    int svIndex = 0;
    // If partitionId is not -1 that means it's set to previous batch last partitionId
    partitionId = (partitionId == -1) ? getCurrentRowId(0) : partitionId;

    for (int i = 0; i < inputRecordCount;) {
      // Get rowId from current right row
      int currentRowId = getCurrentRowId(i);

      if (partitionId == currentRowId) {
        // Check if there is any start offset set for each partition and skip those records
        if (recordStartOffset > 0) {
          --recordStartOffset;
          ++i;
          continue;
        }

        // Once the start offset records are skipped then consider rows until
        // numberOfRecords is reached for that partition
        if (outputAllRecords) {
          updateOutputSV2(svIndex++, i);
        } else if (numberOfRecords > 0) {
          updateOutputSV2(svIndex++, i);
          --numberOfRecords;
        }
        ++i;
      } else { // now a new row with different partition id is found
        refreshConfigParameter();
        partitionId = currentRowId;
      }
    }

    setOutgoingRecordCount(inputRecordCount, svIndex);
  }

  private void updateOutputSV2(int svIndex, int incomingIndex) {
    if (incomingSv != null) {
      outgoingSv.setIndex(svIndex, incomingSv.getIndex(incomingIndex));
    } else {
      outgoingSv.setIndex(svIndex, (char) incomingIndex);
    }
  }

  private int getCurrentRowId(int incomingIndex) {
    if (incomingSv != null) {
      return partitionColumn.getAccessor().get(incomingSv.getIndex(incomingIndex));
    } else {
      return partitionColumn.getAccessor().get(incomingIndex);
    }
  }

  private void setOutgoingRecordCount(int inputRecordCount, int outputCount) {
    outgoingSv.setRecordCount(outputCount);
    // Must report the actual value count as the record
    // count. This is NOT the input record count when the input
    // is an SV2.
    int inputValueCount = incoming.getContainer().getRecordCount();
    container.setRecordCount(inputValueCount);
    outgoingSv.setBatchActualRecordCount(inputValueCount);
  }

  /**
   * Reset the states for recordStartOffset, numberOfRecords and based on the
   * {@link PartitionLimit} passed to the operator. It also resets the
   * partitionId since after EMIT outcome there will be new partitionId to
   * consider. This method is called for the each EMIT outcome received no
   * matter if limit is reached or not.
   */
  private void refreshLimitState() {
    refreshConfigParameter();
    partitionId = -1;
  }

  /**
   * Only resets the recordStartOffset and numberOfRecord based on
   * {@link PartitionLimit} passed to the operator. It is explicitly called
   * after the limit for each partitionId is met or partitionId changes within
   * an EMIT boundary.
   */
  private void refreshConfigParameter() {
    // Make sure startOffset is non-negative
    recordStartOffset = Math.max(0, popConfig.getFirst());
    numberOfRecords = (popConfig.getLast() == null) ?
      Integer.MIN_VALUE : Math.max(0, popConfig.getLast()) - recordStartOffset;
  }

  @Override
  public void dump() {
    logger.error("PartitionLimitRecordBatch[container={}, popConfig={}, incomingSV={}, outgoingSV={},"
            + " recordStartOffset={}, numberOfRecords={}, partitionId={}, unionTypeEnabled={}, state={}]",
        container, popConfig, incomingSv, outgoingSv, recordStartOffset, numberOfRecords,
        partitionId, unionTypeEnabled, state);
  }
}
