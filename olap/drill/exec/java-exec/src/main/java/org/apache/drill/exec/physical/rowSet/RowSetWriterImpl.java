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

import java.util.List;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractTupleWriter;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;

/**
 * Implementation of a row set writer. Only available for newly-created,
 * empty, direct, single row sets. Rewriting is not allowed, nor is writing
 * to a hyper row set.
 */
public class RowSetWriterImpl extends AbstractTupleWriter implements RowSetWriter {

  /**
   * Writer index that points to each row in the row set. The index starts at
   * the 0th row and advances one row on each increment. This allows writers to
   * start positioned at the first row. Writes happen in the current row.
   * Calling {@code next()} advances to the next position, effectively saving
   * the current row. The most recent row can be abandoned easily simply by not
   * calling {@code next()}. This means that the number of completed rows is
   * the same as the row index.
   */
  static class WriterIndexImpl implements ColumnWriterIndex {

    public enum State { OK, VECTOR_OVERFLOW, END_OF_BATCH }

    private int rowIndex = 0;
    private State state = State.OK;

    @Override
    public final int vectorIndex() { return rowIndex; }

    public final boolean next() {
      if (++rowIndex < ValueVector.MAX_ROW_COUNT) {
        return true;
      }
      // Should not call next() again once batch is full.
      assert rowIndex == ValueVector.MAX_ROW_COUNT;
      rowIndex = ValueVector.MAX_ROW_COUNT;
      state = state == State.OK ? State.END_OF_BATCH : state;
      return false;
    }

    public int size() {
      // The index always points to the next slot past the
      // end of valid rows.
      return rowIndex;
    }

    public boolean valid() { return state == State.OK; }

    public boolean hasOverflow() { return state == State.VECTOR_OVERFLOW; }

    @Override
    public final void nextElement() { }

    @Override
    public final void prevElement() { }

    @Override
    public void rollover() {
      throw new UnsupportedOperationException("Rollover not supported in the row set writer.");
    }

    @Override
    public int rowStartIndex() { return rowIndex; }

    @Override
    public ColumnWriterIndex outerIndex() { return null; }

    @Override
    public String toString() {
      return new StringBuilder()
        .append("[")
        .append(getClass().getSimpleName())
        .append(" state = ")
        .append(state)
        .append(", rowIndex = ")
        .append(rowIndex)
        .append("]")
        .toString();
    }
  }

  private final WriterIndexImpl writerIndex;
  private final ExtendableRowSet rowSet;

  protected RowSetWriterImpl(ExtendableRowSet rowSet, TupleMetadata schema, WriterIndexImpl index, List<AbstractObjectWriter> writers) {
    super(schema, writers);
    this.rowSet = rowSet;
    this.writerIndex = index;
    bindIndex(index);
    startWrite();
    startRow();
  }

  @Override
  public RowSetWriter addRow(Object...values) {
    setObject(values);
    save();
    return this;
  }

  @Override
  public RowSetWriter addSingleCol(Object value) {
    return addRow(value);
  }

  @Override
  public int rowIndex() { return writerIndex.vectorIndex(); }

  @Override
  public void save() {
    endArrayValue();
    saveRow();

    // For convenience, start a new row after each save.
    // The last (unused) row is abandoned when the batch is full.
    if (writerIndex.next()) {
      startRow();
    }
  }

  @Override
  public boolean isFull( ) { return ! writerIndex.valid(); }

  @Override
  public SingleRowSet done() {
    endWrite();
    rowSet.container().setRecordCount(writerIndex.vectorIndex());
    return rowSet;
  }

  @Override
  public int lastWriteIndex() {
    return writerIndex.vectorIndex();
  }

  @Override
  public ColumnMetadata schema() {
    // The top-level tuple (the data row) is not associated
    // with a parent column. By contrast, a map tuple is
    // associated with the column that defines the map.
    return null;
  }
}
