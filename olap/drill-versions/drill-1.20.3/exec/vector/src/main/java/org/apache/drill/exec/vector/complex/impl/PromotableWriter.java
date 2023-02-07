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
package org.apache.drill.exec.vector.complex.impl;

import java.lang.reflect.Constructor;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VectorDescriptor;
import org.apache.drill.exec.vector.ZeroVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;

/**
 * This FieldWriter implementation delegates all FieldWriter API calls to an inner FieldWriter. This inner field writer
 * can start as a specific type, and this class will promote the writer to a UnionWriter if a call is made that the specifically
 * typed writer cannot handle. A new UnionVector is created, wrapping the original vector, and replaces the original vector
 * in the parent vector, which can be either an AbstractMapVector or a ListVector.
 */
public class PromotableWriter extends AbstractPromotableFieldWriter {

  private final AbstractMapVector parentContainer;
  private final ListVector listVector;
  private int position;

  private enum State {
    UNTYPED, SINGLE, UNION
  }

  private MinorType type;
  private ValueVector vector;
  private UnionVector unionVector;
  private State state;
  private FieldWriter writer;

  public PromotableWriter(ValueVector v, AbstractMapVector parentContainer) {
    super(null);
    this.parentContainer = parentContainer;
    this.listVector = null;
    init(v);
  }

  public PromotableWriter(ValueVector v, ListVector listVector) {
    super(null);
    this.listVector = listVector;
    this.parentContainer = null;
    init(v);
  }

  private void init(ValueVector v) {
    if (v instanceof UnionVector) {
      state = State.UNION;
      unionVector = (UnionVector) v;
      writer = new UnionWriter(unionVector);
    } else if (v instanceof ZeroVector) {
      state = State.UNTYPED;
    } else {
      setWriter(v);
    }
  }

  private void setWriter(ValueVector v) {
    state = State.SINGLE;
    vector = v;
    type = v.getField().getType().getMinorType();
    Class<?> writerClass = BasicTypeHelper
        .getWriterImpl(v.getField().getType().getMinorType(), v.getField().getDataMode());
    if (writerClass.equals(SingleListWriter.class)) {
      writerClass = UnionListWriter.class;
    }
    Class<? extends ValueVector> vectorClass = BasicTypeHelper.getValueVectorClass(v.getField().getType().getMinorType(), v.getField()
        .getDataMode());
    try {
      Constructor<?> constructor = null;
      for (Constructor<?> c : writerClass.getConstructors()) {
        if (c.getParameterTypes().length == 3) {
          constructor = c;
        }
      }
      if (constructor == null) {
        constructor = writerClass.getConstructor(vectorClass, AbstractFieldWriter.class);
        writer = (FieldWriter) constructor.newInstance(vector, null);
      } else {
        writer = (FieldWriter) constructor.newInstance(vector, null, true);
      }
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setPosition(int index) {
    super.setPosition(index);
    FieldWriter w = getWriter();
    if (w == null) {
      position = index;
    } else {
      w.setPosition(index);
    }
  }

  @Override
  protected FieldWriter getWriter(MinorType type) {
    if (state == State.UNION) {
      return writer;
    }
    if (state == State.UNTYPED) {
      if (type == null) {
        return null;
      }
      ValueVector v = listVector.addOrGetVector(new VectorDescriptor(Types.optional(type))).getVector();
      v.allocateNew();
      setWriter(v);
      writer.setPosition(position);
    }
    if (type != this.type) {
      return promoteToUnion(type);
    }
    return writer;
  }

  @Override
  public boolean isEmptyMap() {
    return writer.isEmptyMap();
  }

  @Override
  protected FieldWriter getWriter() {
    return getWriter(type);
  }

  private FieldWriter promoteToUnion(MinorType newType) {
    String name = vector.getField().getName();
    TransferPair tp = vector.getTransferPair(vector.getField().getType().getMinorType().name().toLowerCase(), vector.getAllocator());
    tp.transfer();
    if (parentContainer != null) {
      unionVector = parentContainer.addOrGet(name, Types.optional(MinorType.UNION), UnionVector.class);
    } else if (listVector != null) {
      unionVector = listVector.promoteToUnion();
    }
    // fix early init issue with different type lists in one union vector
    unionVector.addSubType(newType);
    unionVector.addVector(tp.getTo());
    writer = new UnionWriter(unionVector);
    writer.setPosition(idx());
    for (int i = 0; i < idx(); i++) {
      unionVector.getMutator().setType(i, vector.getField().getType().getMinorType());
    }
    vector = null;
    state = State.UNION;
    return writer;
  }

  @Override
  public void allocate() {
    getWriter().allocate();
  }

  @Override
  public void clear() {
    getWriter().clear();
  }

  @Override
  public MaterializedField getField() {
    return getWriter().getField();
  }

  @Override
  public int getValueCapacity() {
    return getWriter().getValueCapacity();
  }

  @Override
  public void close() throws Exception {
    getWriter().close();
  }
}
