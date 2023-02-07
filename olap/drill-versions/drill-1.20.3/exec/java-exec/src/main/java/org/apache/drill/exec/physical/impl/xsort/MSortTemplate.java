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

import java.util.Queue;

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BaseAllocator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.hadoop.util.IndexedSortable;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Queues;

import io.netty.buffer.DrillBuf;

public abstract class MSortTemplate implements MSorter, IndexedSortable {

  private SelectionVector4 vector4;
  private SelectionVector4 aux;
  @SuppressWarnings("unused")
  private long compares;

  /**
   * Holds offsets into the SV4 of the start of each batch
   * (sorted run.)
   */
  private Queue<Integer> runStarts = Queues.newLinkedBlockingQueue();
  private FragmentContext context;

  /**
   * Controls the maximum size of batches exposed to downstream
   */
  private int desiredRecordBatchCount;

  @Override
  public void setup(FragmentContext context, BufferAllocator allocator, SelectionVector4 vector4,
                    VectorContainer hyperBatch, int outputBatchSize, int desiredBatchSize) throws SchemaChangeException{
    // we pass in the local hyperBatch since that is where we'll be reading data.
    Preconditions.checkNotNull(vector4);
    this.vector4 = vector4.createNewWrapperCurrent();
    this.context = context;
    vector4.clear();
    doSetup(context, hyperBatch, null);

    // Populate the queue with the offset in the SV4 of each
    // batch. Note that this is expensive as it requires a scan
    // of all items to be sorted: potentially millions.

    runStarts.add(0);
    int batch = 0;
    int totalCount = this.vector4.getTotalCount();
    for (int i = 0; i < totalCount; i++) {
      int newBatch = this.vector4.get(i) >>> 16;
      if (newBatch == batch) {
        continue;
      } else if (newBatch == batch + 1) {
        runStarts.add(i);
        batch = newBatch;
      } else {
        throw new UnsupportedOperationException(String.format("Missing batch. batch: %d newBatch: %d", batch, newBatch));
      }
    }

    // Create a temporary SV4 to hold the merged results.

    DrillBuf drillBuf = allocator.buffer(4 * totalCount);
    desiredRecordBatchCount = Math.min(outputBatchSize, desiredBatchSize);
    desiredRecordBatchCount = Math.min(desiredRecordBatchCount, totalCount);
    aux = new SelectionVector4(drillBuf, totalCount, desiredRecordBatchCount);
  }

  /**
   * For given recordCount how much memory does MSorter needs for its own
   * purpose. This is used in ExternalSortBatch to make decisions about whether
   * to spill or not.
   */
  public static long memoryNeeded(int recordCount) {
    // We need 4 bytes (SV4) for each record.
    // The memory allocator will round this to the next
    // power of 2.

    return BaseAllocator.nextPowerOfTwo(recordCount * 4);
  }

  /**
   * Given two regions within the selection vector 4 (a left and a right), merge
   * the two regions to produce a combined output region in the auxiliary
   * selection vector.
   */
  protected int merge(int leftStart, int rightStart, int rightEnd, int outStart) {
    int l = leftStart;
    int r = rightStart;
    int o = outStart;
    while (l < rightStart && r < rightEnd) {
      if (compare(l, r) <= 0) {
        aux.set(o++, vector4.get(l++));
      } else {
        aux.set(o++, vector4.get(r++));
      }
    }
    while (l < rightStart) {
      aux.set(o++, vector4.get(l++));
    }
    while (r < rightEnd) {
      aux.set(o++, vector4.get(r++));
    }
    assert o == outStart + (rightEnd - leftStart);
    return o;
  }

  @Override
  public SelectionVector4 getSV4() {
    return vector4;
  }

  /**
   * Merge a set of pre-sorted runs to produce a combined
   * result set. Merging is done in the selection vector, record data does
   * not move.
   * <p>
   * Runs are merge pairwise in multiple passes, providing performance
   * of O(n * m * log(n)), where n = number of runs, m = number of records
   * per run.
   */
  @Override
  public void sort() {
    while (runStarts.size() > 1) {
      int totalCount = vector4.getTotalCount();

      // check if we're cancelled/failed recently
      context.getExecutorState().checkContinue();

      int outIndex = 0;
      Queue<Integer> newRunStarts = Queues.newLinkedBlockingQueue();
      newRunStarts.add(outIndex);
      int size = runStarts.size();
      for (int i = 0; i < size / 2; i++) {
        int left = runStarts.poll();
        int right = runStarts.poll();
        Integer end = runStarts.peek();
        if (end == null) {
          end = totalCount;
        }
        outIndex = merge(left, right, end, outIndex);
        if (outIndex < vector4.getTotalCount()) {
          newRunStarts.add(outIndex);
        }
      }
      if (outIndex < totalCount) {
        copyRun(outIndex, totalCount);
      }
      SelectionVector4 tmp = aux.createNewWrapperCurrent(desiredRecordBatchCount);
      aux.clear();
      aux = vector4.createNewWrapperCurrent(desiredRecordBatchCount);
      vector4.clear();
      vector4 = tmp.createNewWrapperCurrent(desiredRecordBatchCount);
      tmp.clear();
      runStarts = newRunStarts;
    }
    aux.clear();
  }

  private void copyRun(int start, int end) {
    for (int i = start; i < end; i++) {
      aux.set(i, vector4.get(i));
    }
  }

  @Override
  public void swap(int sv0, int sv1) {
    int tmp = vector4.get(sv0);
    vector4.set(sv0, vector4.get(sv1));
    vector4.set(sv1, tmp);
  }

  @Override
  public int compare(int leftIndex, int rightIndex) {
    int sv1 = vector4.get(leftIndex);
    int sv2 = vector4.get(rightIndex);
    compares++;
    try {
      return doEval(sv1, sv2);
    } catch (SchemaChangeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void clear() {
    if (vector4 != null) {
      vector4.clear();
      vector4 = null;
    }
    if (aux != null) {
      aux.clear();
      aux = null;
    }
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") VectorContainer incoming,
                               @Named("outgoing") RecordBatch outgoing)
                       throws SchemaChangeException;
  public abstract int doEval(@Named("leftIndex") int leftIndex,
                             @Named("rightIndex") int rightIndex)
                      throws SchemaChangeException;
}
