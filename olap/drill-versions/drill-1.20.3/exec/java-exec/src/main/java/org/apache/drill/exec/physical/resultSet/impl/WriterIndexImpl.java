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
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;

/**
 * Writer index that points to each row in the row set. The index starts at
 * the 0th row and advances one row on each increment. This allows writers to
 * start positioned at the first row. Writes happen in the current row.
 * Calling <tt>next()</tt> advances to the next position, effectively saving
 * the current row. The most recent row can be abandoned easily simply by not
 * calling <tt>next()</tt>. This means that the number of completed rows is
 * the same as the row index.
 * <p>
 * The writer index enforces the row count limit for a new batch. The
 * limit is set by the result set loader and can vary from batch to batch
 * if the client chooses in order to adjust the row count based on actual
 * data size.
 */

class WriterIndexImpl implements ColumnWriterIndex {

  private final ResultSetLoader rsLoader;
  private int rowIndex;

  public WriterIndexImpl(ResultSetLoader rsLoader) {
    this.rsLoader = rsLoader;
  }

  @Override
  public int vectorIndex() { return rowIndex; }

  @Override
  public int rowStartIndex() { return rowIndex; }

  public boolean next() {
    if (++rowIndex < rsLoader.targetRowCount()) {
      return true;
    } else {
      // Should not call next() again once batch is full.
      rowIndex = rsLoader.targetRowCount();
      return false;
    }
  }

  public int skipRows(int requestedCount) {

    // Determine the number of rows that can be skipped. Note that since the
    // batch must have no columns, there is no need to observe the normal
    // batch size limit.

    int skipped = Math.min(requestedCount,
        ValueVector.MAX_ROW_COUNT - rowIndex);

    // Update the row index with the skip count.

    rowIndex += skipped;

    // Tell the client how many rows were actually skipped.
    // The client can make more requests in later batches to get
    // the full amount.

    return skipped;
  }

  public int size() {

    // The index always points to the next slot past the
    // end of valid rows.

    return rowIndex;
  }

  public boolean valid() { return rowIndex < rsLoader.maxBatchSize(); }

  @Override
  public void rollover() {

    // The top level index always rolls over to 0 --
    // the first row position in the new vectors.

    reset();
  }

  public void reset() { rowIndex = 0; }

  @Override
  public void nextElement() { }

  @Override
  public void prevElement() { }

  @Override
  public ColumnWriterIndex outerIndex() { return null; }

  @Override
  public String toString() {
    return new StringBuilder()
      .append("[")
      .append(getClass().getSimpleName())
      .append(" rowIndex = ")
      .append(rowIndex)
      .append("]")
      .toString();
  }
}
