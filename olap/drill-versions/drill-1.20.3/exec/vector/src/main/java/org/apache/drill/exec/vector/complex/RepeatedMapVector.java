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

import java.util.List;
import java.util.Map;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.holders.ComplexHolder;
import org.apache.drill.exec.expr.holders.RepeatedMapHolder;
import org.apache.drill.exec.expr.holders.RepeatedValueHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.complex.impl.NullReader;
import org.apache.drill.exec.vector.complex.impl.RepeatedMapReaderImpl;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class RepeatedMapVector extends AbstractRepeatedMapVector {

  public final static MajorType TYPE = Types.repeated(MinorType.MAP);

  private final Accessor accessor = new Accessor();
  private final Mutator mutator = new Mutator();
  private final RepeatedMapReaderImpl reader = new RepeatedMapReaderImpl(this);

  public RepeatedMapVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    super(field, allocator, callBack);
  }

  public RepeatedMapVector(MaterializedField field, UInt4Vector offsets, CallBack callBack) {
    super(field, offsets, callBack);
  }

  @Override
  public RepeatedMapReaderImpl getReader() { return reader; }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new RepeatedMapTransferPair(getField().getName(), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    return new RepeatedMapTransferPair((RepeatedMapVector) to);
  }

  MapSingleCopier makeSingularCopier(MapVector to) {
    return new MapSingleCopier(this, to);
  }

  protected static class MapSingleCopier {
    private final TransferPair[] pairs;
    public final RepeatedMapVector from;

    public MapSingleCopier(RepeatedMapVector from, MapVector to) {
      this.from = from;
      this.pairs = new TransferPair[from.size()];

      int i = 0;
      ValueVector vector;
      for (String child:from.getChildFieldNames()) {
        int preSize = to.size();
        vector = from.getChild(child);
        if (vector == null) {
          continue;
        }
        ValueVector newVector = to.addOrGet(child, vector.getField().getType(), vector.getClass());
        if (to.size() != preSize) {
          newVector.allocateNew();
        }
        pairs[i++] = vector.makeTransferPair(newVector);
      }
    }

    public void copySafe(int fromSubIndex, int toIndex) {
      for (TransferPair p : pairs) {
        p.copyValueSafe(fromSubIndex, toIndex);
      }
    }
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new RepeatedMapTransferPair(ref, allocator);
  }

  private class RepeatedMapTransferPair extends AbstractRepeatedMapTransferPair<RepeatedMapVector> {

    RepeatedMapTransferPair(String path, BufferAllocator allocator) {
      super(new RepeatedMapVector(MaterializedField.create(path, TYPE), allocator, new SchemaChangeCallBack()), false);
    }

    RepeatedMapTransferPair(RepeatedMapVector to) {
      super(to);
    }

    RepeatedMapTransferPair(RepeatedMapVector to, boolean allocate) {
      super(to, allocate);
    }
  }

  public class Accessor extends AbstractRepeatedMapVector.Accessor {
    @Override
    public Object getObject(int index) {
      List<Object> list = new JsonStringArrayList<>();
      int end = offsets.getAccessor().get(index+1);
      String fieldName;
      for (int i =  offsets.getAccessor().get(index); i < end; i++) {
        Map<String, Object> vv = Maps.newLinkedHashMap();
        for (MaterializedField field : getField().getChildren()) {
          if (!field.equals(BaseRepeatedValueVector.OFFSETS_FIELD)) {
            fieldName = field.getName();
            Object value = getChild(fieldName).getAccessor().getObject(i);
            if (value != null) {
              vv.put(fieldName, value);
            }
          }
        }
        list.add(vv);
      }
      return list;
    }

    public void get(int index, ComplexHolder holder) {
      FieldReader reader = getReader();
      reader.setPosition(index);
      holder.reader = reader;
    }

    public void get(int index, int arrayIndex, ComplexHolder holder) {
      RepeatedMapHolder h = new RepeatedMapHolder();
      get(index, h);
      int offset = h.start + arrayIndex;

      if (offset >= h.end) {
        holder.reader = NullReader.INSTANCE;
      } else {
        reader.setSinglePosition(index, arrayIndex);
        holder.reader = reader;
      }
    }
  }

  public class Mutator extends AbstractRepeatedMapVector.Mutator {
  }

  @Override
  public Accessor getAccessor() {
    return accessor;
  }

  @Override
  public Mutator getMutator() {
    return mutator;
  }

  @Override
  RepeatedValueHolder getValueHolder() {
    return new RepeatedMapHolder();
  }
}
