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
package org.apache.drill.exec.physical.rowSet;

import org.apache.drill.exec.vector.accessor.TupleReader;

/**
 * Reader for all types of row sets: those with or without
 * a selection vector. Iterates over "bare" row sets in row
 * order. Iterates over selection-vector based row sets in
 * selection vector order.
 */

public interface RowSetReader extends TupleReader {

  /**
   * Total number of rows in the row set.
   * @return total number of rows
   */
  int rowCount();

  /**
   * Convenience method which whether the next call to {@link #next()}
   * will succeed. Purely optional.
   *
   * @return <tt>true</tt> if there is another record to read,
   * <tt>false</tt> if not
   */
  boolean hasNext();

  /**
   * Advance to the next position. If the underlying row set has
   * a selection vector, then moves one position in the selection
   * vector, and to whichever data record is indexed.
   *
   * @return <tt>true</tt> if another row is available,
   * <tt>false</tt> if all rows have been read
   */
  boolean next();

  /**
   * Gets the read position within the row set. If the row set has
   * a selection vector, this is the position in that vector; the
   * actual record location will likely differ. Use
   * {@link #offset()} to get the actual row index.
   *
   * @return current iteration position
   */
  int logicalIndex();

  /**
   * Sets the iteration position. If the row set has a selection
   * vector, this sets the index within that vector. The index must
   * be from -1 to the {@link #rowCount()} - 1. Set the value to one
   * less than the position to be read in the next call to
   * {@link #next()}. An index of -1 means before the first row.
   *
   * @param index the desired index position
   */
  void setPosition(int index);

  /**
   * Reset the position to before the first row. Convenient method
   * which is the same as <tt>setPosition(-1)</tt>.
   */
  void rewind();

  /**
   * Batch index: 0 for a single batch, batch for the current
   * row is a hyper-batch.
   * @return index of the batch for the current row
   */
  int hyperVectorIndex();

  /**
   * The index of the underlying row which may be indexed by an
   * SV2 or SV4.
   *
   * @return index of the underlying row
   */
  int offset();

  /**
   * Bind the reader to a new batch of data. The vectors are
   * unchanged, but the buffers are different. Assumes the schema
   * has not changed: the columns and selection vector mode remain
   * unchanged; only the buffers changed. If the schema changes,
   * discard this reader and rebuild a new one.
   */
  void newBatch();
}
