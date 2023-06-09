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
package org.apache.drill.exec.physical.impl.xsort;

import org.apache.drill.exec.ops.OperatorStats;

public class SortMetrics {

  private int peakBatchCount = -1;
  private int inputRecordCount = 0;
  private int inputBatchCount = 0; // total number of batches received so far

  /**
   * Sum of the total number of bytes read from upstream.
   * This is the raw memory bytes, not actual data bytes.
   */

  private long totalInputBytes;

  /**
   * Tracks the minimum amount of remaining memory for use
   * in populating an operator metric.
   */

  private long minimumBufferSpace;
  private OperatorStats stats;
  private int spillCount;
  private int mergeCount;
  private long writeBytes;

  public SortMetrics(OperatorStats stats) {
    assert stats != null;
    this.stats = stats;
  }

  public void updateInputMetrics(int rowCount, long batchSize) {
    inputRecordCount += rowCount;
    inputBatchCount++;
    totalInputBytes += batchSize;
  }

  public void updateMemory(long freeMem) {

    if (minimumBufferSpace == 0) {
      minimumBufferSpace = freeMem;
    } else {
      minimumBufferSpace = Math.min(minimumBufferSpace, freeMem);
    }
    stats.setLongStat(ExternalSortBatch.Metric.MIN_BUFFER, minimumBufferSpace);
  }

  public int getInputRowCount() { return inputRecordCount; }
  public long getInputBatchCount() { return inputBatchCount; }
  public long getInputBytes() { return totalInputBytes; }

  public void updatePeakBatches(int bufferedBatchCount) {
    if (peakBatchCount < bufferedBatchCount) {
      peakBatchCount = bufferedBatchCount;
      stats.setLongStat(ExternalSortBatch.Metric.PEAK_BATCHES_IN_MEMORY, peakBatchCount);
    }
  }

  public void incrMergeCount() {
    stats.addLongStat(ExternalSortBatch.Metric.MERGE_COUNT, 1);
    mergeCount++;
  }

  public void incrSpillCount() {
    stats.addLongStat(ExternalSortBatch.Metric.SPILL_COUNT, 1);
    spillCount++;
  }

  public void updateWriteBytes(long writeBytes) {
    stats.setDoubleStat(ExternalSortBatch.Metric.SPILL_MB,
        writeBytes / 1024.0D / 1024.0);
    this.writeBytes = writeBytes;
  }

  public int getSpillCount() { return spillCount; }
  public int getMergeCount() { return mergeCount; }
  public long getWriteBytes() { return writeBytes; }
  public int getPeakBatchCount() { return peakBatchCount; }
}
