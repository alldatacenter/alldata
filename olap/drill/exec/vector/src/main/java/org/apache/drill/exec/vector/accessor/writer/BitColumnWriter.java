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

import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ValueType;

/**
 * Specialized writer for bit columns. Bits are packed 8 per byte.
 * Rather than duplicate that logic here, this writer just delegates
 * to the vector's own mutator.
 */

public class BitColumnWriter extends AbstractFixedWidthWriter {

  private final BitVector vector;
  private final BitVector.Mutator mutator;
  private int defaultValue;

  public BitColumnWriter(final ValueVector vector) {
    this.vector = (BitVector) vector;
    mutator = this.vector.getMutator();
  }

  @Override public BaseDataValueVector vector() { return vector; }

  @Override public int width() { return BitVector.VALUE_WIDTH; }

  @Override
  public ValueType valueType() {
    return ValueType.BOOLEAN;
  }

  protected int prepareWrite() {

    // "Fast path" for the normal case of no fills, no overflow.
    // This is the only bounds check we want to do for the entire
    // set operation.

    // This is performance critical code; every operation counts.
    // Please be thoughtful when changing the code.

    final int writeIndex = vectorIndex.vectorIndex();
    prepareWrite(writeIndex);

    // Track the last write location for zero-fill use next time around.

    lastWriteIndex = writeIndex;
    return writeIndex;
  }

  private void prepareWrite(int writeIndex) {
    final int byteIndex = writeIndex >> 3;
    if (byteIndex >= capacity) {

      // Bit vector can never overflow

      resize(byteIndex);
    }
    if (lastWriteIndex + 1 < writeIndex) {
      fillEmpties(writeIndex);
    }
    lastWriteIndex = writeIndex;
  }

  @Override
  public void setValueCount(int valueCount) {
    prepareWrite(valueCount);
    mutator.setValueCount(valueCount);
  }

  /**
   * Fill empties. This is required because the allocated memory is not
   * zero-filled.
   */

  @Override
  protected final void fillEmpties(final int writeIndex) {
    for (int dest = lastWriteIndex + 1; dest < writeIndex; dest++) {
      mutator.set(dest, defaultValue);
    }
  }

  @Override
  public final void setBoolean(final boolean value) {
    mutator.set(prepareWrite(), value ? 1 : 0);
    vectorIndex.nextElement();
  }

  @Override
  public void setInt(final int value) {
    setBoolean(value != 0);
  }

  @Override
  public void setValue(Object value) {
    setInt((Integer) value);
  }

  @Override
  public final void setDefaultValue(final Object value) {
    defaultValue = ((Boolean) value) ? 1 : 0;
  }

  @Override
  public void copy(ColumnReader from) {
    ScalarReader source = (ScalarReader) from;
    setInt(source.getInt());
  }
}
