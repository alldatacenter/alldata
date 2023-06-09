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
package org.apache.drill.exec.vector;

import io.netty.buffer.DrillBuf;

import java.util.Set;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.record.MaterializedField;


public abstract class BaseDataValueVector extends BaseValueVector {

  protected final static byte[] emptyByteArray = new byte[0]; // Nullable vectors use this

  protected DrillBuf data;

  public BaseDataValueVector(MaterializedField field, BufferAllocator allocator) {
    super(field, allocator);
    data = allocator.getEmpty();
  }

  /**
   * Core of vector allocation. Given a new size (which must be a power of two), allocate
   * the new buffer, copy the current values, and leave the unused parts garbage-filled.
   *
   * @param newAllocationSize new buffer size as a power of two
   * @return the new buffer
   */

  public abstract DrillBuf reallocRaw(int newAllocationSize);

  @Override
  public void clear() {
    if (data != null) {
      data.release();
    }
    data = allocator.getEmpty();
    super.clear();
  }

  @Override
  public void close() {
    clear();
    if (data != null) {
      data.release();
      data = null;
    }
    super.close();
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    DrillBuf[] out;
    if (getBufferSize() == 0) {
      out = new DrillBuf[0];
    } else {
      out = new DrillBuf[]{data};
      data.readerIndex(0);
      if (clear) {
        data.retain(1);
      }
    }
    if (clear) {
      clear();
    }
    return out;
  }

  @Override
  public int getBufferSize() {
    if (getAccessor().getValueCount() == 0) {
      return 0;
    }
    return data.writerIndex();
  }

  @Override
  public int getAllocatedSize() {
    return data.capacity();
  }

  public DrillBuf getBuffer() { return data; }

  /**
   * This method has a similar effect of allocateNew() without actually clearing and reallocating
   * the value vector. The purpose is to move the value vector to a "mutate" state
   */
  public void reset() {}

  @Override
  public void exchange(ValueVector other) {

    // Exchange the data buffers

    BaseDataValueVector target = (BaseDataValueVector) other;
    DrillBuf temp = data;
    data = target.data;
    target.data = temp;
    getReader().reset();
    getMutator().exchange(target.getMutator());
    // No state in an Accessor to reset
  }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    BufferLedger ledger = data.getLedger();
    if (ledger != null) {
      ledgers.add(ledger);
    }
  }
}
