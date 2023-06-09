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
package org.apache.drill.exec.vector;

import io.netty.buffer.DrillBuf;

/**
 * A bulk input entry enables us to process potentially multiple VL values in one shot (especially for very
 * small values); please refer to {@link org.apache.drill.exec.vector.VarBinaryVector.BulkInput}.
 *
 * <p><b>Format -</b>
 * <ul>
 * <li>data: [&lt;val1&gt;&lt;val2&gt;&lt;val3&gt;..]
 * <li>value lengths: [&lt;val1-len&gt;&lt;val2-len&gt;&lt;val3-len&gt;..]
 * </ul>
 *
 * <p><b>NOTE - </b>Bulk entries are immutable
 */
public interface VarLenBulkEntry {
  /**
   * @return total length of this bulk entry data
   */
  int getTotalLength();
  /**
   * @return data start offset
   */
  int getDataStartOffset();
  /**
   * @return true if this entry's data is backed by an array
   */
  boolean arrayBacked();
  /**
   * @return byte buffer containing the data; the data is located within buffer[start-offset, length-1] where
   * buffer is the byte array returned by this method
   */
  byte[] getArrayData();
  /**
   * @return {@link DrillBuf} containing the data; the data is located within [start-offset, length-1]
   */
   DrillBuf getData();
  /**
   * @return length table (one per VL value)
   */
  int[] getValuesLength();
  /**
   * @return number of values (including null values)
   */
  int getNumValues();
  /**
   * @return number of non-null values
   */
  int getNumNonNullValues();
  /**
   * @return true if this bulk entry has nulls; false otherwise
   */
  boolean hasNulls();
}