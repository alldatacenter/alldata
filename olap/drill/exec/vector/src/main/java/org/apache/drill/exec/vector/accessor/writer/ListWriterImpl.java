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
package org.apache.drill.exec.vector.accessor.writer;

import java.lang.reflect.Array;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.UInt1ColumnWriter;
import org.apache.drill.exec.vector.complex.ListVector;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * List writer, which is basically an array writer, with the addition
 * that each list element can be null. Lists never auto-increment
 * their indexes since the contents of lists can change dynamically,
 * and auto-increment is meaningful only for scalar arrays.
 */

public class ListWriterImpl extends ObjectArrayWriter {

  private final ListVector vector;
  private final UInt1ColumnWriter isSetWriter;

  public ListWriterImpl(ColumnMetadata schema, ListVector vector, AbstractObjectWriter memberWriter) {
    super(schema, vector.getOffsetVector(), memberWriter);
    this.vector = vector;
    isSetWriter = new UInt1ColumnWriter(vector.getBitsVector());
    elementIndex = new ArrayElementWriterIndex();
  }

  @Override
  public void bindIndex(ColumnWriterIndex index) {
    super.bindIndex(index);
    isSetWriter.bindIndex(index);
  }

  @VisibleForTesting
  public ListVector vector() { return vector; }

  @Override
  public void setNull(boolean isNull) {
    if (elementIndex.arraySize() > 0 && isNull) {
      throw new IllegalStateException();
    }
    isSetWriter.setInt(isNull ? 0 : 1);
  }

  @Override
  public void startWrite() {
    super.startWrite();
    isSetWriter.startWrite();
  }

  @Override
  public void startRow() {
    super.startRow();
    isSetWriter.startRow();
  }

  @Override
  public void endArrayValue() {

    // Do the shim save first: it requires state which is reset
    // in the super call.

    if (elementIndex.arraySize() > 0) {
      setNull(false);
    }
    super.endArrayValue();
  }

  @Override
  public void restartRow() {
    super.restartRow();
    isSetWriter.restartRow();
  }

  @Override
  public void saveRow() {
    super.saveRow();
    isSetWriter.saveRow();
  }

  @Override
  public void preRollover() {
    super.preRollover();
    isSetWriter.preRollover();
  }

  @Override
  public void postRollover() {
    super.postRollover();
    isSetWriter.postRollover();
  }

  @Override
  public void endWrite() {
    isSetWriter.endWrite();
    super.endWrite();
  }

  @Override
  public void setObject(Object array) {
    setNull(array == null);
    if (array == null) {
      return;
    }
    final int size = Array.getLength(array);
    for (int i = 0; i < size; i++) {
      final Object value = Array.get(array, i);
      if (value != null) {
        elementObjWriter.setObject(value);
      } else if (elementObjWriter.nullable()) {
        elementObjWriter.setNull();
      }
      save();
    }
  }
}
