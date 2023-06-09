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

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.holders.RepeatedDictHolder;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.RepeatedDictVector;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

public class RepeatedDictReaderImpl extends AbstractFieldReader {

  private static final int NO_VALUES = Integer.MIN_VALUE;

  private final RepeatedDictVector container;

  private FieldReader reader;
  private int currentOffset;
  private int maxOffset;

  public RepeatedDictReaderImpl(RepeatedDictVector container) {
    super();
    this.container = container;
  }

  @Override
  public TypeProtos.MajorType getType() {
    return RepeatedDictVector.TYPE;
  }

  @Override
  public void reset() {
    super.reset();
    currentOffset = 0;
    maxOffset = 0;
    if (reader != null) {
      reader.reset();
    }
    reader = null;
  }

  @Override
  public int size() {
    return isEmpty() ? 0 : maxOffset - currentOffset;
  }

  @Override
  public void setPosition(int index) {
    if (index < 0) {
      currentOffset = NO_VALUES;
      return;
    }

    super.setPosition(index);
    RepeatedDictHolder h = new RepeatedDictHolder();
    container.getAccessor().get(index, h);
    if (h.start == h.end) {
      currentOffset = NO_VALUES;
    } else {
      currentOffset = h.start - 1;
      maxOffset = h.end - 1;
      if (reader != null) {
        reader.setPosition(currentOffset);
      }
    }
  }

  @Override
  public boolean next() {
    if (currentOffset < maxOffset) {
      currentOffset++;
      if (reader != null) {
        reader.setPosition(currentOffset);
      }
      return true;
    } else {
      currentOffset = NO_VALUES;
      return false;
    }
  }

  @Override
  public Object readObject() {
    return container.getAccessor().getObject(idx());
  }

  @Override
  public FieldReader reader() {
    if (reader == null) {
      ValueVector child = container.getDataVector();
      if (child == null) {
        reader = NullReader.INSTANCE;
      } else {
        reader = child.getReader();
      }
      reader.setPosition(currentOffset);
    }
    return reader;
  }

  public boolean isEmpty() {
    return currentOffset == NO_VALUES;
  }

  @Override
  public void copyAsValue(BaseWriter.DictWriter writer) {
    if (isEmpty()) {
      return;
    }

    ValueVector vector;
    int srcId;
    if (writer instanceof RepeatedDictWriter) {
      vector = ((RepeatedDictWriter) writer).container;
      srcId = ((RepeatedDictWriter) writer).idx();
    } else {
      vector = ((SingleDictWriter) writer).container;
      srcId = ((SingleDictWriter) writer).idx();
    }
    vector.copyEntry(srcId, container, idx());
  }

  @Override
  public String getTypeString() {
    return "ARRAY<" + reader().getTypeString() + '>';
  }
}
