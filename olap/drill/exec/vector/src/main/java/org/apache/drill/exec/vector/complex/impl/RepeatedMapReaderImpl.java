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

import org.apache.drill.exec.expr.holders.RepeatedMapHolder;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;

@SuppressWarnings("unused")
public class RepeatedMapReaderImpl extends AbstractRepeatedMapReaderImpl<RepeatedMapVector> {

  public RepeatedMapReaderImpl(RepeatedMapVector vector) {
    super(vector);
  }

  @Override
  public FieldReader reader() {
    if (isEmpty()) {
      return NullReader.INSTANCE;
    }

    setChildrenPosition(currentOffset);
    return new SingleLikeRepeatedMapReaderImpl(vector, this);
  }

  public void setSinglePosition(int index, int childIndex) {
    super.setPosition(index);
    RepeatedMapHolder h = new RepeatedMapHolder();
    vector.getAccessor().get(index, h);
    if (h.start == h.end) {
      currentOffset = NO_VALUES;
    } else {
      int singleOffset = h.start + childIndex;
      assert singleOffset < h.end;
      currentOffset = singleOffset;
      maxOffset = singleOffset + 1;
      setChildrenPosition(singleOffset);
    }
  }

  @Override
  public MaterializedField getField() {
    return vector.getField();
  }

  @Override
  public void copyAsValue(MapWriter writer) {
    if (isEmpty()) {
      return;
    }
    RepeatedMapWriter impl = (RepeatedMapWriter) writer;
    impl.container.copyFromSafe(idx(), impl.idx(), vector);
  }

  public void copyAsValueSingle(MapWriter writer) {
    if (isEmpty()) {
      return;
    }
    SingleMapWriter impl = (SingleMapWriter) writer;
    impl.container.copyFromSafe(currentOffset, impl.idx(), vector);
  }

  @Override
  public void copyAsField(String name, MapWriter writer) {
    if (isEmpty()) {
      return;
    }
    RepeatedMapWriter impl = (RepeatedMapWriter) writer.map(name);
    impl.container.copyFromSafe(idx(), impl.idx(), vector);
  }
}
