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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.holders.RepeatedMapHolder;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractRepeatedMapVector;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

@SuppressWarnings("unused")
public abstract class AbstractRepeatedMapReaderImpl<V extends AbstractRepeatedMapVector> extends AbstractFieldReader {
  protected static final int NO_VALUES = Integer.MAX_VALUE - 1;

  protected final V vector;
  protected final Map<String, FieldReader> fields = new HashMap<>();
  protected int currentOffset;
  protected int maxOffset;

  public AbstractRepeatedMapReaderImpl(V vector) {
    this.vector = vector;
  }

  @Override
  public FieldReader reader(String name) {
    FieldReader reader = fields.get(name);
    if (reader == null) {
      ValueVector child = vector.getChild(name);
      if (child == null) {
        reader = NullReader.INSTANCE;
      } else {
        reader = child.getReader();
      }
      fields.put(name, reader);
      reader.setPosition(currentOffset);
    }
    return reader;
  }

  @Override
  public void reset() {
    super.reset();
    currentOffset = 0;
    maxOffset = 0;
    for (FieldReader reader:fields.values()) {
      reader.reset();
    }
    fields.clear();
  }

  @Override
  public int size() {
    UInt4Vector.Accessor offsetsAccessor = vector.getOffsetVector().getAccessor();
    return isEmpty() ? 0 : offsetsAccessor.get(idx() + 1) - offsetsAccessor.get(idx());
  }

  @Override
  public void setPosition(int index) {
    if (index < 0 || index == NO_VALUES) {
      currentOffset = NO_VALUES;
      return;
    }

    super.setPosition(index);
    RepeatedMapHolder h = new RepeatedMapHolder();
    vector.getAccessor().get(index, h);
    if (h.start == h.end) {
      currentOffset = NO_VALUES;
    } else {
      currentOffset = h.start - 1;
      maxOffset = h.end - 1;
      setChildrenPosition(currentOffset);
    }
  }

  @Override
  public boolean next() {
    if (currentOffset < maxOffset) {
      setChildrenPosition(++currentOffset);
      return true;
    } else {
      currentOffset = NO_VALUES;
      return false;
    }
  }

  public boolean isEmpty() {
    return currentOffset == NO_VALUES;
  }

  @Override
  public Object readObject() {
    return vector.getAccessor().getObject(idx());
  }

  @Override
  public MajorType getType() {
    return vector.getField().getType();
  }

  @Override
  public Iterator<String> iterator() {
    return vector.fieldNameIterator();
  }

  @Override
  public boolean isSet() {
    return true;
  }

  void setChildrenPosition(int index) {
    for (FieldReader r : fields.values()) {
      r.setPosition(index);
    }
  }
}
