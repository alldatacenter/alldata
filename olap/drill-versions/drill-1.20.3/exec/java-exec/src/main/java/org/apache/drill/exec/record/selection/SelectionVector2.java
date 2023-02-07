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
package org.apache.drill.exec.record.selection;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.DeadBuf;

import io.netty.buffer.DrillBuf;

/**
 * A selection vector that fronts, at most, 64K values.
 * The selection vector is used for two cases:
 * <ol>
 * <li>To create a list of values retained by a filter.</li>
 * <li>To provide a redirection level for sorted
 * batches.</li>
 * </ol>
 */

public class SelectionVector2 implements AutoCloseable {

  public static final int RECORD_SIZE = 2;

  private final BufferAllocator allocator;
  // Indicates number of indexes stored in the SV2 buffer which may be less than actual number of rows stored in
  // RecordBatch container owning this SV2 instance
  private int recordCount;
  // Indicates actual number of rows in the RecordBatch container which owns this SV2 instance
  private int batchActualRecordCount = -1;
  private DrillBuf buffer = DeadBuf.DEAD_BUFFER;

  public SelectionVector2(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  /**
   * Create a selection vector with the given buffer. The selection vector
   * increments the buffer's reference count, talking ownership of the buffer.
   *
   * @param allocator allocator used to allocate the buffer
   * @param buf the buffer containing the selection vector's data
   * @param count the number of values in the selection vector
   */

  public SelectionVector2(BufferAllocator allocator, DrillBuf buf, int count) {
    this.allocator = allocator;
    buffer = buf;
    buffer.retain(1);
    recordCount = count;
  }

  public SelectionVector2(BufferAllocator allocator, DrillBuf buf, int count, int actualRecordCount) {
    this(allocator, buf, count);
    this.batchActualRecordCount = actualRecordCount;
  }

  public int getCount() {
    return recordCount;
  }

  public DrillBuf getBuffer() {
    return getBuffer(true);
  }

  public DrillBuf getBuffer(boolean clear) {
    DrillBuf bufferHandle = buffer;

    if (clear) {
      /* Increment the ref count for this buffer */
      bufferHandle.retain(1);

      /* We are passing ownership of the buffer to the
       * caller. clear the buffer from within our selection vector
       */
      clear();
    }

    return bufferHandle;
  }

  public void setBuffer(DrillBuf bufferHandle) {
    /* clear the existing buffer */
    clear();

    buffer = bufferHandle;
    buffer.retain(1);
  }

  public char getIndex(int index) {
    return buffer.getChar(index * RECORD_SIZE);
  }

  public long getDataAddr() {
    return buffer.memoryAddress();
  }

  public void setIndex(int index, int value) {
    buffer.setChar(index * RECORD_SIZE, value);
  }

  public boolean allocateNewSafe(int size) {
    try {
      allocateNew(size);
    } catch (OutOfMemoryException e) {
      return false;
    }
    return true;
  }

  public void allocateNew(int size) {
    clear();
    buffer = allocator.buffer(size * RECORD_SIZE);
  }

  @Override
  public SelectionVector2 clone() {
    SelectionVector2 newSV = new SelectionVector2(allocator);
    newSV.recordCount = recordCount;
    newSV.batchActualRecordCount = batchActualRecordCount;
    newSV.buffer = buffer;

    /* Since buffer and newSV.buffer essentially point to the
     * same buffer, if we don't do a retain() on the newSV's
     * buffer, it might get freed.
     */
    newSV.buffer.retain(1);
    clear();
    return newSV;
  }

  public void clear() {
    if (buffer != null && buffer != DeadBuf.DEAD_BUFFER) {
      buffer.release();
      buffer = DeadBuf.DEAD_BUFFER;
      recordCount = 0;
      batchActualRecordCount = -1;
    }
  }

  public void setRecordCount(int recordCount){
    this.recordCount = recordCount;
  }

  public boolean canDoFullTransfer() {
    return recordCount == batchActualRecordCount;
  }

  public void setBatchActualRecordCount(int actualRecordCount) {
    this.batchActualRecordCount = actualRecordCount;
  }

  public int getBatchActualRecordCount() {
    return batchActualRecordCount;
  }

  @Override
  public void close() {
    clear();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("[SV2: recs=");
    buf.append(recordCount);
    buf.append(" - ");
    int n = Math.min(20, recordCount);
    for (int i = 0; i < n; i++) {
      if (i > 0) { buf.append("," ); }
      buf.append((int) getIndex(i));
    }
    if (recordCount > n) {
      buf.append("...");
      buf.append((int) getIndex(recordCount-1));
    }
    buf.append("]");
    return buf.toString();
  }
}
