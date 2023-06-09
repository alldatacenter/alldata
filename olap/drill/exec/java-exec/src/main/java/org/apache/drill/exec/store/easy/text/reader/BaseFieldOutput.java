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
package org.apache.drill.exec.store.easy.text.reader;

import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.vector.accessor.ValueWriter;

public abstract class BaseFieldOutput implements TextOutput {

  /**
   * Width of the per-field data buffer. Fields can be larger.
   * In that case, subsequent buffers are appended to the vector
   * to form the full field.
   */
  private static final int BUFFER_LEN = 1024;

  // track which field is getting appended
  protected int currentFieldIndex = -1;
  // track chars within field
  protected int currentDataPointer;
  // track if field is still getting appended
  private boolean fieldOpen = true;
  // number of bytes written to field thus far
  protected int fieldWriteCount;
  // holds chars for a field
  protected byte[] fieldBytes;
  protected final RowSetLoader writer;
  private final boolean[] projectionMask;
  protected final int maxField;
  protected boolean fieldProjected;

  /**
   * Initialize the field output for one of three scenarios:
   * <ul>
   * <li>SELECT all: SELECT *, SELECT columns. Indicated by a non -1
   * max fields.</li>
   * <li>SELECT none: SELECT COUNT(*), etc. Indicated by a max field
   * of -1.</li>
   * <li>SELECT a, b, c indicated by a non-null projection mask that
   * identifies the indexes of the fields to be selected. In this case,
   * this constructor computes the maximum field.</li>
   * </ul>
   *
   * @param writer Row set writer that provides access to the writer for
   * each column
   * @param maxField the index of the last field to store. May be -1 if no
   * fields are to be stored. Computed if the projection mask is set
   * @param projectionMask a boolean array indicating which fields are
   * to be projected to the output. Optional
   */
  public BaseFieldOutput(RowSetLoader writer, int maxField, boolean[] projectionMask) {
    this.writer = writer;
    this.projectionMask = projectionMask;

    // If no projection mask is defined, then we want all columns
    // up to the max field, which may be -1 if we want to select
    // nothing.

    if (projectionMask == null) {
      this.maxField = maxField;
    } else {

      // Otherwise, use the projection mask to determine
      // which fields are to be projected. (The file may well
      // contain more than the projected set.)

      int end = projectionMask.length - 1;
      while (end >= 0 && ! projectionMask[end]) {
        end--;
      }
      this.maxField = end;
    }

    // If we project at least one field, allocate a buffer.

    if (maxField >= 0) {
      fieldBytes = new byte[BUFFER_LEN];
    }
  }

  /**
   * Start a new record record. Resets all pointers
   */
  @Override
  public void startRecord() {
    currentFieldIndex = -1;
    fieldOpen = false;
    writer.start();
  }

  @Override
  public void startField(int index) {
    assert index == currentFieldIndex + 1;
    currentFieldIndex = index;
    currentDataPointer = 0;
    fieldWriteCount = 0;
    fieldOpen = true;

    // Figure out if this field is projected.

    if (projectionMask == null) {
      fieldProjected = currentFieldIndex <= maxField;
    } else if (currentFieldIndex >= projectionMask.length) {
      fieldProjected = false;
    } else {
      fieldProjected = projectionMask[currentFieldIndex];
    }
  }

  @Override
  public void append(byte data) {
    if (! fieldProjected) {
      return;
    }
    if (currentDataPointer >= BUFFER_LEN - 1) {
      writeToVector();
    }

    fieldBytes[currentDataPointer++] = data;
  }

  /**
   * Write a buffer of data to the underlying vector using the
   * column writer. The buffer holds a complete or partial chunk
   * of data for the field. If this is the first data for the field,
   * write the bytes. If this is a second buffer for the same field,
   * append the bytes. The append will work if the underlying vector
   * is VarChar, it will fail if a type conversion shim is in between.
   * (This is generally OK because the previous setBytes should have
   * failed because a large int or date is not supported.)
   */
  protected void writeToVector() {
    if (!fieldProjected) {
      return;
    }
    ValueWriter colWriter = columnWriter();
    if (fieldWriteCount == 0) {
      colWriter.setBytes(fieldBytes, currentDataPointer);
    } else {
      colWriter.appendBytes(fieldBytes, currentDataPointer);
    }
    fieldWriteCount += currentDataPointer;
    currentDataPointer = 0;
  }

  protected abstract ValueWriter columnWriter();

  @Override
  public boolean endField() {
    fieldOpen = false;
    return currentFieldIndex < maxField;
  }

  @Override
  public boolean endEmptyField() {
    return endField();
  }

  @Override
  public void finishRecord() {
    if (fieldOpen) {
      endField();
    }
    writer.save();
  }

  @Override
  public long getRecordCount() {
    return writer.rowCount();
  }

  @Override
  public boolean isFull() {
    return writer.isFull();
  }
}
