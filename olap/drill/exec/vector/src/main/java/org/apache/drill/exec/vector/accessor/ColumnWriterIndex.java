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
package org.apache.drill.exec.vector.accessor;

/**
 * A Drill record batch consists of a variety of vectors, including maps and lists.
 * Each vector is written independently. A reader may skip some values in each row
 * if no values appear for those columns.
 * <p>
 * This index provides a single view of the "current row" or "current array index"
 * across a set of vectors. Each writer consults this index to determine:
 * <ul>
 * <li>The position to which to write a value.</li>
 * <li>Whether the write position is beyond the "last write" position, which
 * would require filling in any "missing" values.</li>
 * </ul>
 */

public interface ColumnWriterIndex {

  /**
   * Index of the first entry for the current row
   * @return index of the first entry for the current row
   */

  int rowStartIndex();

  /**
   * Current row or array index.
   * @return row or array index
   */

  int vectorIndex();

  /**
   * Increment the index for an array.
   * For arrays, writing (or saving) one value automatically
   * moves to the next value. Ignored for non-element indexes.
   */

  void nextElement();

  /**
   * Decrement the index for an array. Used exclusively for
   * appending bytes to a VarChar, Var16Char or VarBytes
   * column. Assumed to be followed by another call
   * to nextElement().
   */

  void prevElement();

  /**
   * When handling overflow, the index must be reset so that the current row
   * starts at the start of the vector. Relative offsets must be preserved.
   * (That is, if the current write position for an array is four greater than
   * the start, then that offset must now be reset to four from the start of
   * the vector.)
   */

  void rollover();

  /**
   * If this index represents a repeat level, return the index of the
   * next higher repeat level.
   *
   * @return the outer repeat level index, if any
   */

  ColumnWriterIndex outerIndex();
}
