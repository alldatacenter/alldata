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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.expr.holders.ComplexHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.vector.BaseValueVector;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.RepeatedMapVector.MapSingleCopier;
import org.apache.drill.exec.vector.complex.impl.SingleMapReaderImpl;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Ordering;
import org.apache.drill.shaded.guava.com.google.common.primitives.Ints;

import io.netty.buffer.DrillBuf;

public class MapVector extends AbstractMapVector {

  public final static MajorType TYPE = Types.required(MinorType.MAP);

  private final SingleMapReaderImpl reader = new SingleMapReaderImpl(MapVector.this);
  private final Accessor accessor = new Accessor();
  private final Mutator mutator = new Mutator();
  private int valueCount;

  public MapVector(String path, BufferAllocator allocator, CallBack callBack) {
    this(MaterializedField.create(path, TYPE), allocator, callBack);
  }

  public MapVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    super(field, allocator, callBack);
  }

  @Override
  public FieldReader getReader() { return reader; }

  transient private MapTransferPair ephPair;
  transient private MapSingleCopier ephPair2;

  public void copyFromSafe(int fromIndex, int thisIndex, MapVector from) {
    if (ephPair == null || ephPair.from != from) {
      ephPair = (MapTransferPair) from.makeTransferPair(this);
    }
    ephPair.copyValueSafe(fromIndex, thisIndex);
  }

  public void copyFromSafe(int fromSubIndex, int thisIndex, RepeatedMapVector from) {
    if (ephPair2 == null || ephPair2.from != from) {
      ephPair2 = from.makeSingularCopier(this);
    }
    ephPair2.copySafe(fromSubIndex, thisIndex);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (MapVector) from);
  }

  @Override
  protected boolean supportsDirectRead() { return true; }

  public Iterator<String> fieldNameIterator() {
    return getChildFieldNames().iterator();
  }

  @Override
  public void setInitialCapacity(int numRecords) {
    for (ValueVector v : this) {
      v.setInitialCapacity(numRecords);
    }
  }

  @Override
  public int getBufferSize() {
    if (valueCount == 0 || size() == 0) {
      return 0;
    }
    long buffer = 0;
    for (ValueVector v : this) {
      buffer += v.getBufferSize();
    }

    return (int) buffer;
  }

  @Override
  public int getAllocatedSize() {
    int size = 0;
    for (ValueVector v : this) {
      size += v.getAllocatedSize();
    }
    return size;
  }

  @Override
  public int getBufferSizeFor(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    long bufferSize = 0;
    for (ValueVector v : this) {
      bufferSize += v.getBufferSizeFor(valueCount);
    }

    return (int) bufferSize;
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    return super.getBuffers(clear);
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new MapTransferPair(this, getField().getName(), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    return new MapTransferPair(this, (MapVector) to);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new MapTransferPair(this, ref, allocator);
  }

  protected static class MapTransferPair implements TransferPair{
    private final TransferPair[] pairs;
    private final MapVector from;
    private final MapVector to;

    public MapTransferPair(MapVector from, String path, BufferAllocator allocator) {
      this(from, new MapVector(MaterializedField.create(path, TYPE), allocator, new SchemaChangeCallBack()), false);
    }

    public MapTransferPair(MapVector from, MapVector to) {
      this(from, to, true);
    }

    protected MapTransferPair(MapVector from, MapVector to, boolean allocate) {
      this.from = from;
      this.to = to;
      this.pairs = new TransferPair[from.size()];
      this.to.ephPair = null;
      this.to.ephPair2 = null;

      int i = 0;
      ValueVector vector;
      for (String child:from.getChildFieldNames()) {
        int preSize = to.size();
        vector = from.getChild(child);
        if (vector == null) {
          continue;
        }
        //DRILL-1872: we add the child fields for the vector, looking up the field by name. For a map vector,
        // the child fields may be nested fields of the top level child. For example if the structure
        // of a child field is oa.oab.oabc then we add oa, then add oab to oa then oabc to oab.
        // But the children member of a Materialized field is a HashSet. If the fields are added in the
        // children HashSet, and the hashCode of the Materialized field includes the hash code of the
        // children, the hashCode value of oa changes *after* the field has been added to the HashSet.
        // (This is similar to what happens in ScanBatch where the children cannot be added till they are
        // read). To take care of this, we ensure that the hashCode of the MaterializedField does not
        // include the hashCode of the children but is based only on MaterializedField$key.
        ValueVector newVector = to.addOrGet(child, vector.getField().getType(), vector.getClass());
        if (allocate && to.size() != preSize) {
          newVector.allocateNew();
        }
        pairs[i++] = vector.makeTransferPair(newVector);
      }
    }

    @Override
    public void transfer() {
      for (TransferPair p : pairs) {
        p.transfer();
      }
      to.valueCount = from.valueCount;
      from.clear();
    }

    @Override
    public ValueVector getTo() {
      return to;
    }

    @Override
    public void copyValueSafe(int from, int to) {
      for (TransferPair p : pairs) {
        p.copyValueSafe(from, to);
      }
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      for (TransferPair p : pairs) {
        p.splitAndTransfer(startIndex, length);
      }
      to.getMutator().setValueCount(length);
    }
  }

  @Override
  public int getValueCapacity() {
    if (size() == 0) {
      return 0;
    }

    Ordering<ValueVector> natural = new Ordering<ValueVector>() {
      @Override
      public int compare(@Nullable ValueVector left, @Nullable ValueVector right) {
        return Ints.compare(
            Preconditions.checkNotNull(left).getValueCapacity(),
            Preconditions.checkNotNull(right).getValueCapacity()
        );
      }
    };

    return natural.min(getChildren()).getValueCapacity();
  }

  @Override
  public Accessor getAccessor() {
    return accessor;
  }

  @Override
  public void load(SerializedField metadata, DrillBuf buf) {
    List<SerializedField> fields = metadata.getChildList();
    valueCount = metadata.getValueCount();

    int bufOffset = 0;
    for (SerializedField child : fields) {
      MaterializedField fieldDef = MaterializedField.create(child);

      ValueVector vector = getChild(fieldDef.getName());
      if (vector == null) {
        // if we arrive here, we didn't have a matching vector.
        vector = BasicTypeHelper.getNewVector(fieldDef, allocator);
        putChild(fieldDef.getName(), vector);
      }
      if (child.getValueCount() == 0) {
        vector.clear();
      } else {
        vector.load(child, buf.slice(bufOffset, child.getBufferLength()));
      }
      bufOffset += child.getBufferLength();
    }

    // We should have consumed all bytes written into the buffer
    // during deserialization.

    assert bufOffset == buf.writerIndex();
  }

  @Override
  public SerializedField getMetadata() {
    SerializedField.Builder b = getField()
        .getAsBuilder()
        .setBufferLength(getBufferSize())
        .setValueCount(valueCount);


    for(ValueVector v : getChildren()) {
      b.addChild(v.getMetadata());
    }
    return b.build();
  }

  @Override
  public Mutator getMutator() {
    return mutator;
  }

  public class Accessor extends BaseValueVector.BaseAccessor {

    @Override
    public Object getObject(int index) {
      Map<String, Object> vv = new JsonStringHashMap<>();
      for (String child:getChildFieldNames()) {
        ValueVector v = getChild(child);
        // TODO(DRILL-4001):  Resolve this hack:
        // The index/value count check in the following if statement is a hack
        // to work around the current fact that RecordBatchLoader.load and
        // MapVector.load leave child vectors with a length of zero (as opposed
        // to matching the lengths of siblings and the parent map vector)
        // because they don't remove (or set the lengths of) vectors from
        // previous batches that aren't in the current batch.
        if (v != null && index < v.getAccessor().getValueCount()) {
          Object value = v.getAccessor().getObject(index);
          if (value != null) {
            vv.put(child, value);
          }
        }
      }
      return vv;
    }

    public void get(int index, ComplexHolder holder) {
      reader.setPosition(index);
      holder.reader = reader;
    }

    @Override
    public int getValueCount() {
      return valueCount;
    }
  }

  public ValueVector getVectorById(int id) {
    return getChildByOrdinal(id);
  }

  /**
   * Set the value count for the map without setting the counts for the contained
   * vectors. Use this only when the values of the contained vectors are set
   * elsewhere in the code.
   *
   * @param valueCount number of items in the map
   */

  public void setMapValueCount(int valueCount) {
    this.valueCount = valueCount;
  }

  public class Mutator extends BaseValueVector.BaseMutator {

    @Override
    public void setValueCount(int valueCount) {
      for (ValueVector v : getChildren()) {
        v.getMutator().setValueCount(valueCount);
      }
      setMapValueCount(valueCount);
    }

    @Override
    public void reset() { }

    @Override
    public void generateTestData(int values) { }
  }

  @Override
  public void clear() {
    for (ValueVector v : getChildren()) {
      v.clear();
    }
    valueCount = 0;
  }

  @Override
  public void close() {
    Collection<ValueVector> vectors = getChildren();
    for (ValueVector v : vectors) {
      v.close();
    }
    vectors.clear();
    valueCount = 0;

    super.close();
  }

  @Override
  public void toNullable(ValueVector nullableVector) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void exchange(ValueVector other) {
    super.exchange(other);
    MapVector otherMap = (MapVector) other;
    int temp = otherMap.valueCount;
    otherMap.valueCount = valueCount;
    valueCount = temp;
  }
}
