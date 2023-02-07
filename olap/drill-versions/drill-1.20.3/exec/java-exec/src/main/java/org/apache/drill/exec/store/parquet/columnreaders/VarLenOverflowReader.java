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
package org.apache.drill.exec.store.parquet.columnreaders;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import java.nio.ByteBuffer;

import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.BatchSizingMemoryUtil;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.FieldOverflowDefinition;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.FieldOverflowState;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.FieldOverflowStateContainer;

/**
 * This class is responsible for processing serialized overflow data (generated in a previous batch); this way
 * overflow data becomes an input source and is thus a) efficiently re-loaded into the current
 * batch ValueVector and b) subjected to the same batching constraints rules.
 */
public final class VarLenOverflowReader extends VarLenAbstractEntryReader {
  private final FieldOverflowStateContainer fieldOverflowContainer;
  private final boolean isNullable;
  private final FieldOverflowStateImpl overflowState;

  /**
   * CTOR.
   * @param buffer byte buffer for data buffering (within CPU cache)
   * @param containerCallback container callback
   * @param fieldOverflowContainer field overflow container
   * @param entry reusable bulk entry object
   */
  VarLenOverflowReader(ByteBuffer buffer,
    VarLenColumnBulkEntry entry,
    VarLenColumnBulkInputCallback containerCallback,
    FieldOverflowStateContainer fieldOverflowContainer) {

    super(buffer, entry, containerCallback);

    this.fieldOverflowContainer = fieldOverflowContainer;
    this.isNullable = fieldOverflowContainer.overflowDef.field.isNullable();

    // Initialize the overflow state object
    initOverflowStateIfNeeded();

    // By now the overflow state object should be initialized
    overflowState = (FieldOverflowStateImpl) fieldOverflowContainer.overflowState;
  }

  /** {@inheritDoc} */
  @Override
  VarLenColumnBulkEntry getEntry(int valuesToRead) {

    if (getRemainingOverflowData() == 0) {
      return null; // overflow data fully committed
    }

    final int[] valueLengths = entry.getValuesLength();
    final FieldOverflowDefinition overflowDef = fieldOverflowContainer.overflowDef;
    final OverflowDataCache overflowDataCache = overflowState.overflowDataCache;
    final int maxDataSize = VarLenBulkPageReader.BUFF_SZ;

    // Flush the cache across batches
    if (overflowState.currValueIdx == overflowState.numCommittedValues) {
      overflowDataCache.flush();
    }

    // load some overflow data for processing
    final int maxValues = Math.min(entry.getMaxEntries(), valuesToRead);
    final int numAvailableValues = overflowDataCache.load(overflowState.currValueIdx, maxValues);
    Preconditions.checkState(numAvailableValues > 0, "Number values to read [%s] should be greater than zero", numAvailableValues);

    final int firstValueDataOffset = getDataBufferStartOffset() + adjustDataOffset(overflowState.currValueIdx);
    int totalDataLen = 0;
    int currValueIdx = overflowState.currValueIdx;
    int idx = 0;
    int numNulls = 0;

    for ( ; idx < numAvailableValues; idx++, currValueIdx++) {
      // Is this value defined?
      if (!isNullable || overflowDataCache.getNullable(currValueIdx) == 1) {
        final int dataLen = overflowDataCache.getDataLength(currValueIdx);

        if ((totalDataLen + dataLen) > maxDataSize) {
          break;
        }

        totalDataLen += dataLen;
        valueLengths[idx] = dataLen;

      } else {
        valueLengths[idx] = -1;
        ++numNulls;
      }
    }

    // We encountered a large value or no overflow data; need special handling
    if (idx == 0) {
      final int dataLen = overflowDataCache.getDataLength(currValueIdx);
      return handleLargeEntry(maxDataSize, firstValueDataOffset, dataLen);
    }

    // Update the next overflow value index to be processed
    overflowState.currValueIdx = currValueIdx;

    // Now set the bulk entry
    entry.set(firstValueDataOffset, totalDataLen, idx, idx - numNulls, overflowDef.buffer);

    return entry;
  }

  /**
   * @return remaining overflow data (total-overflow-data - (committed + returned-within-current-batch))
   */
  int getRemainingOverflowData() {
    return overflowState.getRemainingOverflowData();
  }

  private VarLenColumnBulkEntry handleLargeEntry(int maxDataSize, int firstValueDataOffset, int totalDataLen) {

    final FieldOverflowDefinition overflowDef  = fieldOverflowContainer.overflowDef;
    final FieldOverflowStateImpl overflowState = (FieldOverflowStateImpl) fieldOverflowContainer.overflowState;
    final int[] valueLengths = entry.getValuesLength();

    // Is there enough memory to handle this large value?
    if (batchMemoryConstraintsReached(isNullable ? 1 : 0, 4, totalDataLen)) {
      entry.set(0, 0, 0, 0); // no data to be consumed
      return entry;
    }

    // Register the length
    valueLengths[0] = totalDataLen;

    // We already have all the information we need
    entry.set(firstValueDataOffset, totalDataLen, 1, 1, overflowDef.buffer);

    // Update the current value index
    overflowState.currValueIdx++;
    return entry;
  }

  void initOverflowStateIfNeeded() {
    // An overflow happened in the previous batch; this is the first time we are trying to
    // consume this overflow data (in some cases, several batches are needed if somehow the
    // number-of-records-per-batch becomes small).
    if (fieldOverflowContainer.overflowState == null) {
      fieldOverflowContainer.overflowState = new FieldOverflowStateImpl(buffer, fieldOverflowContainer.overflowDef);
    }
  }

  private int adjustDataOffset(int valueIdx) {
    // The overflow definition stores offsets without adjustment (the offsets still refer to the original
    // buffer). We need to perform a minor transformation to compute the correct offset within the new buffer:
    // adjusted-offset(value-idx) = offset(value-i) - offset(value-0)
    int firstOffset, targetOffset;

    if (!isNullable) {
      firstOffset = fieldOverflowContainer.overflowDef.buffer.getInt(0);
      targetOffset = fieldOverflowContainer.overflowDef.buffer.getInt(valueIdx * 4);

    } else {
      final int numOverflowValues = fieldOverflowContainer.overflowDef.numValues;
      firstOffset = fieldOverflowContainer.overflowDef.buffer.getInt(numOverflowValues);
      targetOffset = fieldOverflowContainer.overflowDef.buffer.getInt(numOverflowValues + valueIdx * 4);
    }

    return targetOffset - firstOffset;
  }

  private int getDataBufferStartOffset() {
    if (!isNullable) {
      // <num-values+1 offsets><data>
      return (fieldOverflowContainer.overflowDef.numValues + 1) * 4;

    } else {
      // <num-values nullable bytes><num-values+1 offsets><data>
      return fieldOverflowContainer.overflowDef.numValues + (fieldOverflowContainer.overflowDef.numValues + 1) * 4;
    }
  }

// ----------------------------------------------------------------------------
// Inner Data Structure
// ----------------------------------------------------------------------------

  /** Allows overflow reader to maintain overflow data state */
  final static class FieldOverflowStateImpl implements FieldOverflowState {
    /**
     * The number of overflow values consumed by previous batches; this means that if a new overflow
     * happens, then we should return uncommitted values (un-consumed)
     */
    private int numCommittedValues;
    /** Next value index to be processed */
    private int currValueIdx;
    /** A heap cache to accelerate loading of overflow data into bulk entries */
    private final OverflowDataCache overflowDataCache;

    private FieldOverflowStateImpl(ByteBuffer buffer, FieldOverflowDefinition overflowDef) {
      overflowDataCache = new OverflowDataCache(buffer, overflowDef);
    }

    /** {@inheritDoc} */
    @Override
    public void onNewBatchValuesConsumed(int numValues) {
      if (numCommittedValues < overflowDataCache.overflowDef.numValues) {
        numCommittedValues += numValues;
        currValueIdx = numCommittedValues;
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOverflowDataFullyConsumed() {
      return numCommittedValues == overflowDataCache.overflowDef.numValues;
    }

    /**
     * @return remaining overflow data (total-overflow-data - (committed + returned-within-current-batch))
     */
    int getRemainingOverflowData() {
      assert currValueIdx <= overflowDataCache.overflowDef.numValues;

      return overflowDataCache.overflowDef.numValues - currValueIdx;
    }

  }

  /** Enable us to reuse cached overflow data across calls */
  final static class OverflowDataCache {
    /** Cache format: [<nullvalue>*][<offset>*]; the last "-1" is to account for the extra offset to retrieve
     * since each value requires two offsets  */
    private static final int MAX_NUM_VALUES = (VarLenBulkPageReader.BUFF_SZ / (BatchSizingMemoryUtil.INT_VALUE_WIDTH + 1)) - 1;

    /** Byte buffer for CPU caching */
    private final byte[] bufferArray;
    /** Overflow definition */
    private final FieldOverflowDefinition overflowDef;
    /** Whether this column is optional */
    final boolean isNullable;
    /** start index of the first cached overflow data */
    private int firstCachedValueIdx;
    /** number of cached values */
    private int numCachedValues;

    private OverflowDataCache(ByteBuffer buffer, FieldOverflowDefinition overflowDef) {
      this.bufferArray = buffer.array();
      this.overflowDef = overflowDef;
      this.isNullable = this.overflowDef.field.isNullable();
      this.firstCachedValueIdx = -1;
      this.numCachedValues = -1;
    }

    private void flush() {
      this.firstCachedValueIdx = -1;
      this.numCachedValues = -1;
    }

    private int load(int currentValueIdx, int maxValues) {
      if (currentValueIdx >= overflowDef.numValues) {
        throw new RuntimeException();
      }
      assert currentValueIdx < overflowDef.numValues;

      if (numCachedValues > 0
       && currentValueIdx >= lowerBound()
       && currentValueIdx <= upperBound()) {

        return upperBound() - currentValueIdx + 1;
      }

      // Either cache is empty or the requested values are not in the cache
      loadInternal(currentValueIdx, maxValues);

      return numCachedValues;
    }

    /**
     * @return the lowest overflow value index in the cache
     */
    private int lowerBound() {
      return firstCachedValueIdx;
    }

    /**
     * @return the highest overflow value index in the cache
     */
    private int upperBound() {
      return firstCachedValueIdx + numCachedValues - 1;
    }

    private byte getNullable(int valueIdx) {
      assert isNullable;
      assert valueIdx >= lowerBound();
      assert valueIdx <= upperBound();

      // We need to map the overflow value index to the buffer array representation
      final int cacheIdx = (valueIdx - lowerBound()) * BatchSizingMemoryUtil.BYTE_VALUE_WIDTH;
      return bufferArray[cacheIdx];
    }

    private int getDataLength(int valueIdx) {
      assert valueIdx >= lowerBound();
      assert valueIdx <= upperBound();

      // We need to map the overflow value index to the buffer array representation
      int cacheIdx1, cacheIdx2;

      if (!isNullable) {
        cacheIdx1 = (valueIdx - lowerBound()) * BatchSizingMemoryUtil.INT_VALUE_WIDTH;
      } else {
        cacheIdx1 = numCachedValues +
          (valueIdx - lowerBound()) * BatchSizingMemoryUtil.INT_VALUE_WIDTH;
      }
      cacheIdx2 = cacheIdx1 + BatchSizingMemoryUtil.INT_VALUE_WIDTH;

      return VarLenOverflowReader.getInt(bufferArray, cacheIdx2) - VarLenOverflowReader.getInt(bufferArray, cacheIdx1);
    }

    private void loadInternal(int targetIdx, int maxValues) {
      // We need to load a new batch of overflow data (bits and offsets)
      firstCachedValueIdx = targetIdx;

      final int remaining = remaining();
      assert remaining > 0;

      final int maxValuesToLoad = Math.min(MAX_NUM_VALUES, maxValues);
      numCachedValues           = Math.min(remaining, maxValuesToLoad);

      // Let us load the nullable & offsets data (the actual data doesn't have to be loaded)
      loadNullable();
      loadOffsets();
    }

    void loadNullable() {
      if (!isNullable) {
        return; // NOOP
      }

      overflowDef.buffer.getBytes(firstCachedValueIdx, bufferArray, 0, numCachedValues);
    }

    void loadOffsets() {
      int sourceIdx, targetIdx;

      if (!isNullable) {
        sourceIdx = firstCachedValueIdx * BatchSizingMemoryUtil.INT_VALUE_WIDTH;
        targetIdx = 0;

      } else {
        sourceIdx = overflowDef.numValues + (firstCachedValueIdx * BatchSizingMemoryUtil.INT_VALUE_WIDTH);
        targetIdx = numCachedValues;
      }

      // We always get one extra value as to compute the length of value-i we need offset-i and offset-i+1
      overflowDef.buffer.getBytes(sourceIdx, bufferArray, targetIdx, (numCachedValues + 1) * BatchSizingMemoryUtil.INT_VALUE_WIDTH);
    }

    private int remaining() {
      return overflowDef.numValues - firstCachedValueIdx;
    }
  }

}
