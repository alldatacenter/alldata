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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.vector.IntVector;

import java.util.Map;

public class BatchSizePredictorImpl implements BatchSizePredictor {
  private RecordBatch batch;
  private double fragmentationFactor;
  private double safetyFactor;

  private long batchSize;
  private int numRecords;
  private boolean updatedStats;
  private boolean hasData;

  public BatchSizePredictorImpl(final RecordBatch batch,
                                final double fragmentationFactor,
                                final double safetyFactor) {
    this.batch = Preconditions.checkNotNull(batch);
    this.fragmentationFactor = fragmentationFactor;
    this.safetyFactor = safetyFactor;
  }

  @Override
  public long getBatchSize() {
    Preconditions.checkState(updatedStats);
    return hasData? batchSize: 0;
  }

  @Override
  public int getNumRecords() {
    Preconditions.checkState(updatedStats);
    return hasData? numRecords: 0;
  }

  @Override
  public boolean hadDataLastTime() {
    return hasData;
  }

  @Override
  public void updateStats() {
    final RecordBatchSizer batchSizer = new RecordBatchSizer(batch);
    numRecords = batchSizer.rowCount();
    updatedStats = true;
    hasData = numRecords > 0;

    if (hasData) {
      batchSize = getBatchSizeEstimate(batch);
    }
  }

  @Override
  public long predictBatchSize(int desiredNumRecords, boolean reserveHash) {
    Preconditions.checkState(hasData);
    // Safety factor can be multiplied at the end since these batches are coming from exchange operators, so no excess value vector doubling
    return computeMaxBatchSize(batchSize,
      numRecords,
      desiredNumRecords,
      fragmentationFactor,
      safetyFactor,
      reserveHash);
  }

  public static long computeValueVectorSize(long numRecords, long byteSize) {
    long naiveSize = numRecords * byteSize;
    return roundUpToPowerOf2(naiveSize);
  }

  public static long computeValueVectorSize(long numRecords, long byteSize, double safetyFactor) {
    long naiveSize = RecordBatchSizer.multiplyByFactor(numRecords * byteSize, safetyFactor);
    return roundUpToPowerOf2(naiveSize);
  }

  public static long roundUpToPowerOf2(long num) {
    Preconditions.checkArgument(num >= 1);
    return num == 1 ? 1 : Long.highestOneBit(num - 1) << 1;
  }

  public static long computeMaxBatchSizeNoHash(final long incomingBatchSize,
                                         final int incomingNumRecords,
                                         final int desiredNumRecords,
                                         final double fragmentationFactor,
                                         final double safetyFactor) {
    long maxBatchSize = computePartitionBatchSize(incomingBatchSize, incomingNumRecords, desiredNumRecords);
    // Multiple by fragmentation factor
    return RecordBatchSizer.multiplyByFactors(maxBatchSize, fragmentationFactor, safetyFactor);
  }

  public static long computeMaxBatchSize(final long incomingBatchSize,
                                         final int incomingNumRecords,
                                         final int desiredNumRecords,
                                         final double fragmentationFactor,
                                         final double safetyFactor,
                                         final boolean reserveHash) {
    long size = computeMaxBatchSizeNoHash(incomingBatchSize,
      incomingNumRecords,
      desiredNumRecords,
      fragmentationFactor,
      safetyFactor);

    if (!reserveHash) {
      return size;
    }

    long hashSize = desiredNumRecords * ((long) IntVector.VALUE_WIDTH);
    hashSize = RecordBatchSizer.multiplyByFactors(hashSize, fragmentationFactor);

    return size + hashSize;
  }

  public static long computePartitionBatchSize(final long incomingBatchSize,
                                               final int incomingNumRecords,
                                               final int desiredNumRecords) {
    return (long) Math.ceil((((double) incomingBatchSize) /
      ((double) incomingNumRecords)) *
      ((double) desiredNumRecords));
  }

  public static long getBatchSizeEstimate(final RecordBatch recordBatch) {
    final RecordBatchSizer sizer = new RecordBatchSizer(recordBatch);
    long size = 0L;

    for (Map.Entry<String, RecordBatchSizer.ColumnSize> column : sizer.columns().entrySet()) {
      size += computeValueVectorSize(recordBatch.getRecordCount(), column.getValue().getStdNetOrNetSizePerEntry());
    }

    return size;
  }

  public static class Factory implements BatchSizePredictor.Factory {
    public static final Factory INSTANCE = new Factory();

    private Factory() {
    }

    @Override
    public BatchSizePredictor create(final RecordBatch batch,
                                     final double fragmentationFactor,
                                     final double safetyFactor) {
      return new BatchSizePredictorImpl(batch, fragmentationFactor, safetyFactor);
    }
  }
}
