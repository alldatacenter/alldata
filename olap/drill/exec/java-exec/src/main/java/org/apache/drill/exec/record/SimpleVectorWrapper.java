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
package org.apache.drill.exec.record;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.FieldIdUtil;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class SimpleVectorWrapper<T extends ValueVector> implements VectorWrapper<T>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SimpleVectorWrapper.class);

  private T vector;

  public SimpleVectorWrapper(T v) {
    this.vector = v;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<T> getVectorClass() {
    return (Class<T>) vector.getClass();
  }

  @Override
  public MaterializedField getField() {
    return vector.getField();
  }

  @Override
  public T getValueVector() {
    return vector;
  }

  @Override
  public T[] getValueVectors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isHyper() {
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public VectorWrapper<T> cloneAndTransfer(BufferAllocator allocator) {
    TransferPair tp = vector.getTransferPair(allocator);
    tp.transfer();
    return new SimpleVectorWrapper<T>((T) tp.getTo());
  }

  @Override
  public void clear() {
    vector.clear();
  }

  public static <T extends ValueVector> SimpleVectorWrapper<T> create(T v) {
    return new SimpleVectorWrapper<T>(v);
  }


  @Override
  public VectorWrapper<?> getChildWrapper(int[] ids) {
    if (ids.length == 1) {
      return this;
    }

    ValueVector vector = this.vector;
    for (int i = 1; i < ids.length; i++) {
      final AbstractMapVector mapLike = AbstractMapVector.class.cast(vector);
      if (mapLike == null) {
        return null;
      }
      vector = mapLike.getChildByOrdinal(ids[i]);
    }

    return new SimpleVectorWrapper<>(vector);
  }

  @Override
  public TypedFieldId getFieldIdIfMatches(int id, SchemaPath expectedPath) {
    return FieldIdUtil.getFieldId(getValueVector(), id, expectedPath, false);
  }

  @Override
  public void transfer(VectorWrapper<?> destination) {
    Preconditions.checkArgument(destination instanceof SimpleVectorWrapper);
    Preconditions.checkArgument(getField().getType().equals(destination.getField().getType()));
    vector.makeTransferPair(((SimpleVectorWrapper<?>)destination).vector).transfer();
  }

  @Override
  public String toString() {
    if (vector == null) {
      return "null";
    } else {
      return vector.toString();
    }
  }

}
