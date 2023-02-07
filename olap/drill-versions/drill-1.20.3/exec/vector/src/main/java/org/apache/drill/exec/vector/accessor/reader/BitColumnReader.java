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

import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.reader.BaseScalarReader.BaseFixedWidthReader;

/**
 * Specialized reader for bit columns. Bits are packed 8 per byte.
 * Rather than duplicate that logic here, this reader just delegates
 * to the vector's own accessor.
 */
public class BitColumnReader extends BaseFixedWidthReader {

  @Override
  public ValueType valueType() {
    return ValueType.BOOLEAN;
  }

  @Override public int width() { return BitVector.VALUE_WIDTH; }

  @Override
  public boolean getBoolean() {
    final BitVector.Accessor accessor = ((BitVector) vectorAccessor.vector()).getAccessor();
    final int readOffset = vectorIndex.offset();
    return accessor.get(readOffset) != 0;
  }

  @Override
  public int getInt() {
    return getBoolean() ? 1 : 0;
  }
}
