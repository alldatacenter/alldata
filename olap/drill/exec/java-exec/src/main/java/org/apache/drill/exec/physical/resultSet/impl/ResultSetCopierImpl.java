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

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.resultSet.PullResultSetReader;
import org.apache.drill.exec.physical.resultSet.ResultSetCopier;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class ResultSetCopierImpl implements ResultSetCopier {

  private enum State {
    START,
    NO_SCHEMA,
    BETWEEN_BATCHES,
    BATCH_ACTIVE,
    NEW_SCHEMA,
    SCHEMA_PENDING,
    END_OF_INPUT,
    CLOSED
  }

  private class CopyAll {

     public void copy() {
      while (!rowWriter.isFull() && rowReader.next()) {
        copyColumns();
      }
    }

    public boolean hasMore() {
      return rowReader.hasNext();
    }
  }

  private static class CopyPair {
    protected final ColumnWriter writer;
    protected final ColumnReader reader;

    protected CopyPair(ColumnWriter writer, ColumnReader reader) {
      this.writer = writer;
      this.reader = reader;
    }
  }

  // Input state

  private int currentSchemaVersion = -1;
  private final PullResultSetReader resultSetReader;
  protected RowSetReader rowReader;

  // Output state

  private final BufferAllocator allocator;
  private final ResultSetOptionBuilder writerOptions;
  private ResultSetLoader resultSetWriter;
  private RowSetLoader rowWriter;

  // Copy state

  private State state;
  private CopyPair[] projection;
  private CopyAll activeCopy;

  public ResultSetCopierImpl(BufferAllocator allocator, PullResultSetReader source) {
    this(allocator, source, new ResultSetOptionBuilder());
  }

  public ResultSetCopierImpl(BufferAllocator allocator, PullResultSetReader source,
      ResultSetOptionBuilder outputOptions) {
    this.allocator = allocator;
    resultSetReader = source;
    writerOptions = outputOptions;
    writerOptions.vectorCache(new ResultVectorCacheImpl(allocator));
    state = State.START;
  }

  @Override
  public void startOutputBatch() {
    if (state == State.START) {

      // No schema yet. Defer real batch start until we see an input
      // batch.
      state = State.NO_SCHEMA;
      return;
    }
    Preconditions.checkState(state == State.BETWEEN_BATCHES || state == State.SCHEMA_PENDING);
    if (state == State.SCHEMA_PENDING) {

      // We have a pending new schema. Create new writers to match.
      createProjection();
    }
    resultSetWriter.startBatch();
    state = State.BATCH_ACTIVE;
    if (isCopyPending()) {

      // Resume copying if a copy is active.
      copyBlock();
    }
  }

  @Override
  public boolean nextInputBatch() {
    if (state == State.END_OF_INPUT) {
      return false;
    }
    Preconditions.checkState(state == State.NO_SCHEMA || state == State.NEW_SCHEMA ||
                             state == State.BATCH_ACTIVE,
        "Can only start input while in an output batch");
    Preconditions.checkState(!isCopyPending(),
        "Finish the pending copy before changing input");

    if (!resultSetReader.next()) {
      state = State.END_OF_INPUT;
      return false;
    }
    rowReader = resultSetReader.reader();

    if (state == State.BATCH_ACTIVE) {

      // If no schema change, we are ready to copy.
      if (currentSchemaVersion == resultSetReader.schemaVersion()) {
        return true;
      }

      // The schema has changed. Handle it now or later.
      if (hasOutputRows()) {

        // Output batch has rows. Can't switch and bind inputs
        // until current batch is sent downstream.
        state = State.NEW_SCHEMA;
        return true;
      }
    }

    // The schema changed: first schema, or a change while a bath
    // is active, but is empty.
    if (state == State.NO_SCHEMA) {
      state = State.BATCH_ACTIVE;
    } else {

      // Discard the unused empty batch
      harvest().zeroVectors();
    }
    createProjection();
    resultSetWriter.startBatch();

    // Stay in the current state.
    return true;
  }

  private void createProjection() {
    if (resultSetWriter != null) {

      // Need to build a new writer. Close this one. Doing so
      // will tear down the whole show. But, the vector cache will
      // ensure that the new writer reuses any matching vectors from
      // the prior batch to provide vector persistence as Drill expects.
      resultSetWriter.close();
    }
    TupleMetadata schema = resultSetReader.schema();
    writerOptions.readerSchema(schema);
    resultSetWriter = new ResultSetLoaderImpl(allocator, writerOptions.build());
    rowWriter = resultSetWriter.writer();
    currentSchemaVersion = resultSetReader.schemaVersion();

    int colCount = schema.size();
    projection = new CopyPair[colCount];
    for (int i = 0; i < colCount; i++) {
      projection[i] = new CopyPair(
          rowWriter.column(i).writer(),
          rowReader.column(i).reader());
    }
  }

  @Override
  public boolean hasOutputRows() {
    switch (state) {
    case BATCH_ACTIVE:
    case NEW_SCHEMA:
      return resultSetWriter.hasRows();
    default:
      return false;
    }
  }

  @Override
  public boolean isOutputFull() {
    switch (state) {
    case BATCH_ACTIVE:
      return rowWriter.isFull();
    case NEW_SCHEMA:
    case END_OF_INPUT:
      return true;
    default:
      return false;
    }
  }

  protected void verifyWritable() {
    Preconditions.checkState(state != State.NEW_SCHEMA,
        "Must harvest current batch to flush for new schema.");
    Preconditions.checkState(state == State.BATCH_ACTIVE,
        "Start an output batch before copying");
    Preconditions.checkState(!isCopyPending(),
        "Resume the in-flight copy before copying another");
    Preconditions.checkState(!rowWriter.isFull(),
        "Output batch is full; harvest before adding more");
  }

  @Override
  public boolean copyNextRow() {
    verifyWritable();
    if (!rowReader.next()) {
      return false;
    }
    copyColumns();
    return true;
  }

  @Override
  public void copyRow(int posn) {
    verifyWritable();
    rowReader.setPosition(posn);
    copyColumns();
  }

  private final void copyColumns() {
    rowWriter.start();
    for (CopyPair pair : projection) {
      pair.writer.copy(pair.reader);
    }
    rowWriter.save();
  }

  @Override
  public void copyAllRows() {
    verifyWritable();
    activeCopy = new CopyAll();
    copyBlock();
  }

  private void copyBlock() {
    activeCopy.copy();
    if (! activeCopy.hasMore()) {
      activeCopy = null;
    }
  }

  @Override
  public boolean isCopyPending() {
    return activeCopy != null && activeCopy.hasMore();
  }

  @Override
  public VectorContainer harvest() {
    Preconditions.checkState(state == State.BATCH_ACTIVE || state == State.NEW_SCHEMA ||
                             state == State.END_OF_INPUT);
    VectorContainer output = resultSetWriter.harvest();
    state = (state == State.BATCH_ACTIVE)
        ? State.BETWEEN_BATCHES : State.SCHEMA_PENDING;
    return output;
  }

  @Override
  public void close() {
    if (state == State.CLOSED) {
      return;
    }
    if (resultSetWriter != null) {
      resultSetWriter.close();
      resultSetWriter = null;
      rowWriter = null;
    }
    resultSetReader.close();
    rowReader = null;

    state = State.CLOSED;
  }
}
