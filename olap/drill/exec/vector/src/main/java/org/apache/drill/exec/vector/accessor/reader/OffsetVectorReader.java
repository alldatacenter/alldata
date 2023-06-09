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
package org.apache.drill.exec.vector.accessor.reader;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.reader.BaseScalarReader.BaseFixedWidthReader;

import io.netty.buffer.DrillBuf;

/**
 * Reader for an offset vector.
 */

public class OffsetVectorReader extends BaseFixedWidthReader {

  private static final int VALUE_WIDTH = UInt4Vector.VALUE_WIDTH;

  public OffsetVectorReader(VectorAccessor offsetsAccessor) {
    vectorAccessor = offsetsAccessor;
    bufferAccessor = bufferAccessor(offsetsAccessor);
    nullStateReader = NullStateReaders.REQUIRED_STATE_READER;
  }

  @Override
  public ValueType valueType() {
    return ValueType.INTEGER;
  }

  @Override public int width() { return VALUE_WIDTH; }

  /**
   * Return the offset and length of a value encoded as a long.
   * The value is encoded to avoid the need to resolve the offset vector
   * twice per value.
   *
   * @return a long with the format:<br>
   * Upper 32 bits - offset: <tt>offset = (int) (entry >> 32)</tt><br>
   * Lower 32 bits - length: <tt>length = (int) (entry & 0xFFFF_FFFF)</tt>
   */

  public long getEntry() {
    final DrillBuf buf = bufferAccessor.buffer();
    final int readOffset = vectorIndex.offset() * VALUE_WIDTH;
    long start = buf.getInt(readOffset);
    long end = buf.getInt(readOffset + VALUE_WIDTH);
    return (start << 32) + (end - start);
  }

  @Override
  public void reposition() { }

  @Override
  public ColumnMetadata schema() { return null; }
}
