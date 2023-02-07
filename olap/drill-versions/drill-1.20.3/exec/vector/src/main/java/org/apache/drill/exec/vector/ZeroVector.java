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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.impl.NullReader;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

public class ZeroVector implements ValueVector {
  public final static ZeroVector INSTANCE = new ZeroVector();

  private final MaterializedField field = MaterializedField.create("[DEFAULT]", Types.LATE_BIND_TYPE);

  private final TransferPair defaultPair = new TransferPair() {
    @Override
    public void transfer() { }

    @Override
    public void splitAndTransfer(int startIndex, int length) { }

    @Override
    public ValueVector getTo() {
      return ZeroVector.this;
    }

    @Override
    public void copyValueSafe(int from, int to) { }
  };

  private final Accessor defaultAccessor = new Accessor() {
    @Override
    public Object getObject(int index) { return null; }

    @Override
    public int getValueCount() { return 0; }

    @Override
    public boolean isNull(int index) { return true; }
  };

  private final Mutator defaultMutator = new Mutator() {
    @Override
    public void setValueCount(int valueCount) { }

    @Override
    public void reset() { }

    @Override
    public void generateTestData(int values) { }

    @Override
    public void exchange(Mutator other) { }
  };

  public ZeroVector() { }

  @Override
  public void close() { }

  @Override
  public void clear() { }

  @Override
  public MaterializedField getField() { return field; }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return defaultPair;
  }

  @Override
  public UserBitShared.SerializedField getMetadata() {
    return getField()
        .getAsBuilder()
        .setBufferLength(getBufferSize())
        .setValueCount(getAccessor().getValueCount())
        .build();
  }

  @Override
  public Iterator<ValueVector> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public int getBufferSize() { return 0; }

  @Override
  public int getAllocatedSize() { return 0; }

  @Override
  public int getBufferSizeFor(final int valueCount) { return 0; }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    return new DrillBuf[0];
  }

  @Override
  public void allocateNew() throws OutOfMemoryException {
    allocateNewSafe();
  }

  @Override
  public boolean allocateNewSafe() { return true; }

  @Override
  public BufferAllocator getAllocator() {
    throw new UnsupportedOperationException("Tried to get allocator from ZeroVector");
  }

  @Override
  public void setInitialCapacity(int numRecords) { }

  @Override
  public int getValueCapacity() { return 0; }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) { return defaultPair; }

  @Override
  public TransferPair makeTransferPair(ValueVector target) { return defaultPair; }

  @Override
  public Accessor getAccessor() { return defaultAccessor; }

  @Override
  public Mutator getMutator() { return defaultMutator; }

  @Override
  public FieldReader getReader() { return NullReader.INSTANCE; }

  @Override
  public void load(UserBitShared.SerializedField metadata, DrillBuf buffer) { }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) { }

  @Override
  public void exchange(ValueVector other) { }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {}

  @Override
  public int getPayloadByteCount(int valueCount) { return 0; }

  @Override
  public void toNullable(ValueVector nullableVector) {
    throw new UnsupportedOperationException();
  }
}
