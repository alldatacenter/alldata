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
package org.apache.drill.exec.physical.impl.TopN;

import io.netty.buffer.DrillBuf;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PriorityQueueTemplate implements PriorityQueue {
  private static final Logger logger = LoggerFactory.getLogger(PriorityQueueTemplate.class);

  /**
   * The estimated maximum queue size used with allocating the SV4
   * for the queue. If the queue is larger, then a) we should probably
   * be using a sort instead of top N, and b) the code will automatically
   * grow the SV4 as needed up to the max supported size.
   */
  public static final int EST_MAX_QUEUE_SIZE = 4000;

  // This holds the min heap of the record indexes. Heapify condition is based on actual record though. Only records
  // meeting the heap condition have their indexes in this heap. Actual record are stored inside the hyperBatch. Since
  // hyperBatch contains ValueVectors from all the incoming batches, the indexes here consider both BatchNumber and
  // RecordNumber.
  private SelectionVector4 heapSv4;
  private SelectionVector4 finalSv4; //This is for final sorted output

  // This stores the actual incoming record batches
  private ExpandableHyperContainer hyperBatch;
  private BufferAllocator allocator;

  // Limit determines the number of record to output and hold in queue.
  private int limit;
  private int queueSize;
  private int batchCount;
  private boolean hasSv2;

  @Override
  public void init(int limit, BufferAllocator allocator,  boolean hasSv2) throws SchemaChangeException {
    this.limit = limit;
    this.allocator = allocator;
    // It's allocating memory to store (limit+1) indexes. When first limit number of record indexes are stored then all
    // the other record indexes are kept at (limit+1) and evaluated with the root element of heap to determine if
    // this new element will reside in heap or not.
    final DrillBuf drillBuf = allocator.buffer(4 * (limit + 1));
    // Heap is a SelectionVector4 since it stores indexes for record relative to their batches.
    heapSv4 = new SelectionVector4(drillBuf, limit, Character.MAX_VALUE);
    this.hasSv2 = hasSv2;
  }

  @Override
  public void resetQueue(VectorContainer container, SelectionVector4 v4) throws SchemaChangeException {
    assert container.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.FOUR_BYTE;
    BatchSchema schema = container.getSchema();
    VectorContainer newContainer = new VectorContainer();
    for (MaterializedField field : schema) {
      int[] ids = container.getValueVectorId(SchemaPath.getSimplePath(field.getName())).getFieldIds();
      newContainer.add(container.getValueAccessorById(field.getValueClass(), ids).getValueVectors());
    }
    newContainer.buildSchema(BatchSchema.SelectionVectorMode.FOUR_BYTE);
    // Cleanup before recreating hyperbatch and sv4.
    cleanup();
    hyperBatch = new ExpandableHyperContainer(newContainer);
    batchCount = hyperBatch.iterator().next().getValueVectors().length;
    final DrillBuf drillBuf = allocator.buffer(4 * (limit + 1));
    heapSv4 = new SelectionVector4(drillBuf, limit, Character.MAX_VALUE);
    // Reset queue size (most likely to be set to limit).
    queueSize = 0;
    for (int i = 0; i < v4.getTotalCount(); i++) {
      heapSv4.set(i, v4.get(i));
      ++queueSize;
    }
    v4.clear();
    doSetup(hyperBatch, null);
  }

  @Override
  public void add(RecordBatchData batch) throws SchemaChangeException{
    Stopwatch watch = Stopwatch.createStarted();
    if (hyperBatch == null) {
      hyperBatch = new ExpandableHyperContainer(batch.getContainer());
    } else {
      hyperBatch.addBatch(batch.getContainer());
    }

    doSetup(hyperBatch, null); // may not need to do this every time

    int count = 0;
    SelectionVector2 sv2 = null;
    if (hasSv2) {
      sv2 = batch.getSv2();
    }
    // Will only be called until queueSize has reached the limit which means it has seen limit number of records in
    // one or many batches. For each new record siftUp (or heapify) to adjust min heap property is called.
    for (; queueSize < limit && count < batch.getRecordCount();  count++) {
      heapSv4.set(queueSize, batchCount, hasSv2 ? sv2.getIndex(count) : count);
      queueSize++;
      siftUp();
    }

    // For all the other records which fall beyond limit, it compares them with the root element in the current heap
    // and perform heapify if need be. Note: Even though heapSv4 stores only limit+1 indexes but in hyper batch we
    // are still keeping all the records unless purge is called.
    for (; count < batch.getRecordCount(); count++) {
      heapSv4.set(limit, batchCount, hasSv2 ? sv2.getIndex(count) : count);
      if (compare(limit, 0) < 0) {
        swap(limit, 0);
        siftDown();
      }
    }
    batchCount++;
    if (hasSv2) {
      sv2.clear();
    }
    logger.debug("Took {} us to add {} records", watch.elapsed(TimeUnit.MICROSECONDS), count);
  }

  @Override
  public void generate() {
    Stopwatch watch = Stopwatch.createStarted();
    final DrillBuf drillBuf = allocator.buffer(4 * queueSize);
    finalSv4 = new SelectionVector4(drillBuf, queueSize, EST_MAX_QUEUE_SIZE);
    for (int i = queueSize - 1; i >= 0; i--) {
      finalSv4.set(i, pop());
    }
    logger.debug("Took {} us to generate output of {}", watch.elapsed(TimeUnit.MICROSECONDS), finalSv4.getTotalCount());
  }

  @Override
  public VectorContainer getHyperBatch() {
    return hyperBatch;
  }

  @Override
  public SelectionVector4 getSv4() {
    return heapSv4;
  }

  @Override
  public SelectionVector4 getFinalSv4() {
    return finalSv4;
  }

  @Override
  public void cleanup() {
    if (heapSv4 != null) {
      heapSv4.clear();
      heapSv4 = null;
    }
    if (hyperBatch != null) {
      hyperBatch.clear();
      hyperBatch = null;
    }
    if (finalSv4 != null) {
      finalSv4.clear();
      finalSv4 = null;
    }
    batchCount = 0;
  }

  /**
   * When cleanup is called then heapSv4 is cleared and set to null and is only initialized during init call. Hence
   * this is used to determine if priority queue is initialized or not.
   * @return - true - queue is still initialized
   *           false - queue is not yet initialized and before using queue init should be called
   */
  @Override
  public boolean isInitialized() {
    return (heapSv4 != null);
  }

  /**
   * Perform Heapify for the record stored at index which was added as leaf node in the array. The new record is
   * compared with the record stored at parent index. Since the new record index will flow up in the array hence the
   * name siftUp
   * @throws SchemaChangeException
   */
  private void siftUp() throws SchemaChangeException {
    int p = queueSize - 1;
    while (p > 0) {
      if (compare(p, (p - 1) / 2) > 0) {
        swap(p, (p - 1) / 2);
        p = (p - 1) / 2;
      } else {
        break;
      }
    }
  }

  /**
   * Compares the record stored at the index of 0th index element of heapSv4 (or root element) with the record
   * stored at index of limit index element of heapSv4 (or new element). If the root element is greater than new element
   * then new element is discarded else root element is replaced with new element and again heapify is performed on
   * new root element.
   * This is done for all the records which are seen after the queue is filled with limit number of record indexes.
   * @throws SchemaChangeException
   */
  private void siftDown() throws SchemaChangeException {
    int p = 0;
    int next;
    while (p * 2 + 1 < queueSize) {
      if (p * 2 + 2 >= queueSize) {
        next = p * 2 + 1;
      } else {
        if (compare(p * 2 + 1, p * 2 + 2) >= 0) {
          next = p * 2 + 1;
        } else {
          next = p * 2 + 2;
        }
      }
      if (compare(p, next) < 0) {
        swap(p, next);
        p = next;
      } else {
        break;
      }
    }
  }

  /**
   * Pop the root element which holds the minimum value in heap. In this case root element will be the index of
   * record with minimum value. After extracting the root element it swaps the root element with last element in
   * heapSv4 and does heapify (by calling siftDown) again.
   * @return - Index for
   */
  public int pop() {
    int value = heapSv4.get(0);
    swap(0, queueSize - 1);
    queueSize--;
    try {
      siftDown();
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
    return value;
  }

  public void swap(int sv0, int sv1) {
    int tmp = heapSv4.get(sv0);
    heapSv4.set(sv0, heapSv4.get(sv1));
    heapSv4.set(sv1, tmp);
  }

  public int compare(int leftIndex, int rightIndex) throws SchemaChangeException {
    int sv1 = heapSv4.get(leftIndex);
    int sv2 = heapSv4.get(rightIndex);
    return doEval(sv1, sv2);
  }

  /**
   * Stores the reference to the hyperBatch container which holds all the records across incoming batches in it. This
   * is used in doEval function to compare records in this hyper batch at given indexes.
   * @param incoming - reference to hyperBatch
   * @param outgoing - null
   * @throws SchemaChangeException
   */
  public abstract void doSetup(@Named("incoming") VectorContainer incoming,
                               @Named("outgoing") RecordBatch outgoing)
                       throws SchemaChangeException;

  /**
   * Evaluates the value of record at leftIndex and rightIndex w.r.t min heap condition. It is used in
   * {@link PriorityQueueTemplate#compare(int, int)} method while Heapifying the queue.
   * @param leftIndex
   * @param rightIndex
   * @return
   * @throws SchemaChangeException
   */
  public abstract int doEval(@Named("leftIndex") int leftIndex,
                             @Named("rightIndex") int rightIndex)
                      throws SchemaChangeException;
}
