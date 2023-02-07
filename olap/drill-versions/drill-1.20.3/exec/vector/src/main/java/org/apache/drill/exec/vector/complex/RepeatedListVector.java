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
package org.apache.drill.exec.vector.complex;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import io.netty.buffer.DrillBuf;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.holders.ComplexHolder;
import org.apache.drill.exec.expr.holders.RepeatedListHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.vector.AddOrGetResult;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VectorDescriptor;
import org.apache.drill.exec.vector.complex.impl.NullReader;
import org.apache.drill.exec.vector.complex.impl.RepeatedListReaderImpl;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

public class RepeatedListVector extends AbstractContainerVector
    implements RepeatedValueVector {

  protected static class DelegateRepeatedVector extends BaseRepeatedValueVector {

    private final RepeatedListAccessor accessor = new RepeatedListAccessor();
    private final RepeatedListMutator mutator = new RepeatedListMutator();
    private final EmptyValuePopulator emptyPopulator;
    private transient DelegateTransferPair ephPair;

    public class RepeatedListAccessor extends BaseRepeatedValueVector.BaseRepeatedAccessor {

      @Override
      public Object getObject(int index) {
        final List<Object> list = new JsonStringArrayList<>();
        final int start = offsets.getAccessor().get(index);
        final int until = offsets.getAccessor().get(index+1);
        for (int i = start; i < until; i++) {
          list.add(vector.getAccessor().getObject(i));
        }
        return list;
      }

      public void get(int index, RepeatedListHolder holder) {
        assert index <= getValueCapacity();
        holder.start = getOffsetVector().getAccessor().get(index);
        holder.end = getOffsetVector().getAccessor().get(index+1);
      }

      public void get(int index, ComplexHolder holder) {
        final FieldReader reader = getReader();
        reader.setPosition(index);
        holder.reader = reader;
      }

      public void get(int index, int arrayIndex, ComplexHolder holder) {
        final RepeatedListHolder listHolder = new RepeatedListHolder();
        get(index, listHolder);
        int offset = listHolder.start + arrayIndex;
        if (offset >= listHolder.end) {
          holder.reader = NullReader.INSTANCE;
        } else {
          FieldReader r = getDataVector().getReader();
          r.setPosition(offset);
          holder.reader = r;
        }
      }
    }

    public class RepeatedListMutator extends BaseRepeatedValueVector.BaseRepeatedMutator {

      public int add(int index) {
        final int curEnd = getOffsetVector().getAccessor().get(index+1);
        getOffsetVector().getMutator().setSafe(index + 1, curEnd + 1);
        return curEnd;
      }

      @Override
      public void startNewValue(int index) {
        emptyPopulator.populate(index+1);
        super.startNewValue(index);
      }

      @Override
      public void setValueCount(int valueCount) {
        emptyPopulator.populate(valueCount);
        super.setValueCount(valueCount);
      }
    }

    public class DelegateTransferPair extends BaseRepeatedValueVectorTransferPair<DelegateRepeatedVector> {

      public DelegateTransferPair(DelegateRepeatedVector target) {
        super(target);
      }

      @Override
      public void copyValueSafe(int srcIndex, int destIndex) {
        final RepeatedListHolder holder = new RepeatedListHolder();
        getAccessor().get(srcIndex, holder);
        target.emptyPopulator.populate(destIndex+1);
        copyValueSafe(destIndex, holder.start, holder.end);
      }
    }

    public DelegateRepeatedVector(String path, BufferAllocator allocator) {
      this(MaterializedField.create(path, TYPE), allocator);
    }

    public DelegateRepeatedVector(MaterializedField field, BufferAllocator allocator) {
      super(field, allocator);
      emptyPopulator = new EmptyValuePopulator(getOffsetVector());
    }

    @Override
    public void allocateNew() throws OutOfMemoryException {
      if (!allocateNewSafe()) {
        throw new OutOfMemoryException();
      }
    }

    @Override
    public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
      return makeTransferPair(new DelegateRepeatedVector(ref, allocator));
    }

    @Override
    public TransferPair makeTransferPair(ValueVector target) {
      return new DelegateTransferPair(DelegateRepeatedVector.class.cast(target));
    }

    @Override
    public RepeatedListAccessor getAccessor() { return accessor; }

    @Override
    public RepeatedListMutator getMutator() { return mutator; }

    @Override
    public FieldReader getReader() {
      throw new UnsupportedOperationException();
    }

    public void copyFromSafe(int fromIndex, int thisIndex, DelegateRepeatedVector from) {
      if (ephPair == null || ephPair.target != from) {
        ephPair = DelegateTransferPair.class.cast(from.makeTransferPair(this));
      }
      ephPair.copyValueSafe(fromIndex, thisIndex);
    }

    @Override
    public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
      copyFromSafe(fromIndex, toIndex, (DelegateRepeatedVector) from);
    }
  }

  protected class RepeatedListTransferPair implements TransferPair {
    private final TransferPair delegate;

    public RepeatedListTransferPair(TransferPair delegate) {
      this.delegate = delegate;
    }

    @Override
    public void transfer() {
      delegate.transfer();
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      delegate.splitAndTransfer(startIndex, length);
    }

    @Override
    public ValueVector getTo() {
      final DelegateRepeatedVector delegateVector = DelegateRepeatedVector.class.cast(delegate.getTo());
      return new RepeatedListVector(getField(), allocator, callBack, delegateVector);
    }

    @Override
    public void copyValueSafe(int from, int to) {
      delegate.copyValueSafe(from, to);
    }
  }

  public RepeatedListVector(String path, BufferAllocator allocator, CallBack callBack) {
    this(MaterializedField.create(path, TYPE), allocator, callBack);
  }

  public RepeatedListVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    this(field, allocator, callBack, new DelegateRepeatedVector(field, allocator));
  }

  public final static MajorType TYPE = Types.repeated(MinorType.LIST);
  private final RepeatedListReaderImpl reader = new RepeatedListReaderImpl(null, this);
  private final DelegateRepeatedVector delegate;

  protected RepeatedListVector(MaterializedField field, BufferAllocator allocator, CallBack callBack, DelegateRepeatedVector delegate) {
    super(field, allocator, callBack);
    this.delegate = Preconditions.checkNotNull(delegate);

    final List<MaterializedField> children = Lists.newArrayList(field.getChildren());
    final int childSize = children.size();
    assert childSize < 3;
    final boolean hasChild = childSize > 0;
    if (hasChild) {
      // the last field is data field
      final MaterializedField child = children.get(childSize-1);
      addOrGetVector(VectorDescriptor.create(child));
    }
  }

  @Override
  public RepeatedListReaderImpl getReader() { return reader; }

  @Override
  public DelegateRepeatedVector.RepeatedListAccessor getAccessor() {
    return delegate.getAccessor();
  }

  @Override
  public DelegateRepeatedVector.RepeatedListMutator getMutator() {
    return delegate.getMutator();
  }

  @Override
  public UInt4Vector getOffsetVector() {
    return delegate.getOffsetVector();
  }

  @Override
  public ValueVector getDataVector() {
    return delegate.getDataVector();
  }

  @Override
  public void allocateNew() throws OutOfMemoryException {
    delegate.allocateNew();
  }

  @Override
  public boolean allocateNewSafe() {
    return delegate.allocateNewSafe();
  }

  @Override
  public <T extends ValueVector> AddOrGetResult<T> addOrGetVector(VectorDescriptor descriptor) {
    final AddOrGetResult<T> result = delegate.addOrGetVector(descriptor);
    if (result.isCreated() && callBack != null) {
      callBack.doWork();
    }
    this.field = delegate.getField();
    return result;
  }

  public void setChildVector(ValueVector childVector) {
    delegate.setChildVector(childVector);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public int getBufferSize() {
    return delegate.getBufferSize();
  }

  @Override
  public int getAllocatedSize() {
    return delegate.getAllocatedSize();
  }

  @Override
  public int getBufferSizeFor(final int valueCount) {
    return delegate.getBufferSizeFor(valueCount);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new RepeatedListTransferPair(delegate.getTransferPair(allocator));
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new RepeatedListTransferPair(delegate.getTransferPair(ref, allocator));
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    final RepeatedListVector target = RepeatedListVector.class.cast(to);
    return new RepeatedListTransferPair(delegate.makeTransferPair(target.delegate));
  }

  @Override
  public int getValueCapacity() {
    return delegate.getValueCapacity();
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    return delegate.getBuffers(clear);
  }

  @Override
  public void load(SerializedField metadata, DrillBuf buf) {
    delegate.load(metadata, buf);
  }

  @Override
  public SerializedField getMetadata() {
    return delegate.getMetadata();
  }

  @Override
  public Iterator<ValueVector> iterator() {
    return delegate.iterator();
  }

  @Override
  public void setInitialCapacity(int numRecords) {
    delegate.setInitialCapacity(numRecords);
  }

  /**
   * @deprecated
   *   prefer using {@link #addOrGetVector(org.apache.drill.exec.vector.VectorDescriptor)} instead.
   */
  @Deprecated
  @Override
  public <T extends ValueVector> T addOrGet(String name, MajorType type, Class<T> clazz) {
    final AddOrGetResult<T> result = addOrGetVector(VectorDescriptor.create(type));
    return result.getVector();
  }

  @Override
  public <T extends ValueVector> T getChild(String name, Class<T> clazz) {
    if (name != null) {
      return null;
    }
    return typeify(delegate.getDataVector(), clazz);
  }

  public void allocateNew(int valueCount, int innerValueCount) {
    clear();
    getOffsetVector().allocateNew(valueCount + 1);
    getOffsetVector().getMutator().setSafe(0, 0);
    getMutator().reset();
  }

  public void allocateOffsetsNew(int groupCount) {
    getOffsetVector().allocateNew(groupCount + 1);
    getOffsetVector().zeroVector();
  }

  @Override
  public VectorWithOrdinal getChildVectorWithOrdinal(String name) {
    if (name != null) {
      return null;
    }
    return new VectorWithOrdinal(delegate.getDataVector(), 0);
  }

  public void copyFromSafe(int fromIndex, int thisIndex, RepeatedListVector from) {
    delegate.copyFromSafe(fromIndex, thisIndex, from.delegate);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (RepeatedListVector) from);
  }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    delegate.collectLedgers(ledgers);
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }
    return delegate.getPayloadByteCount(valueCount);
  }

  @Override
  public void exchange(ValueVector other) {
    delegate.exchange(((RepeatedListVector) other).delegate);
  }

  @Override
  public void toNullable(ValueVector nullableVector) {
    throw new UnsupportedOperationException();
  }
}
