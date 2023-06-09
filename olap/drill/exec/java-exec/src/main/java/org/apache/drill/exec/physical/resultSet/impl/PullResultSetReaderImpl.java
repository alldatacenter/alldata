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

import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.resultSet.PullResultSetReader;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * <h4>Protocol</h4>
 * <ol>
 * <li>Create an instance, passing in a
 *     {@link UpstreamSource} to provide batches and optional
 *     selection vector.</li>
 * <li>For each incoming batch:
 *   <ol>
 *   <li>Call {@link #start()} to attach the batch. The associated
 *       {@link BatchAccessor} reports if the schema has changed.</li>
 *   <li>Call {@link #reader()} to obtain a reader.</li>
 *   <li>Iterate over the batch using the reader.</li>
 *   <li>Call {@link #release()} to free the memory for the
 *       incoming batch. Or, to call {@link #detach()} to keep
 *       the batch memory.</li>
 *   </ol>
 * <li>Call {@link #close()} after all batches are read.</li>
 * </ol>
 */
public class PullResultSetReaderImpl implements PullResultSetReader {

  public interface UpstreamSource  extends PushResultSetReaderImpl.UpstreamSource {
    boolean next();
    void release();
  }

  @VisibleForTesting
  protected enum State {
      START,
      PENDING,
      BATCH,
      DETACHED,
      EOF,
      CLOSED
  }

  private final PushResultSetReaderImpl baseReader;
  private final UpstreamSource source;
  private State state = State.START;
  private RowSetReader rowSetReader;

  public PullResultSetReaderImpl(UpstreamSource source) {
    this.baseReader = new PushResultSetReaderImpl(source);
    this.source = source;
  }

  @Override
  public TupleMetadata schema() {
    switch (state) {
      case CLOSED:
        return null;
      case START:
        if (!next()) {
          return null;
        }
        state = State.PENDING;
        break;
      default:
    }
    return rowSetReader.tupleSchema();
  }

  @Override
  public boolean next() {
    switch (state) {
      case PENDING:
        state = State.BATCH;
        return true;
      case BATCH:
        source.release();
        break;
      case CLOSED:
        throw new IllegalStateException("Reader is closed");
      case EOF:
        return false;
      case START:
        break;
      default:
        source.release();
    }
    if (!source.next()) {
      state = State.EOF;
      return false;
    }

    rowSetReader = baseReader.start();
    state = State.BATCH;
    return true;
  }

  @Override
  public int schemaVersion() { return source.schemaVersion(); }

  @Override
  public RowSetReader reader() {
    Preconditions.checkState(state == State.BATCH, "Not in batch-ready state.");
    return rowSetReader;
  }

  @Override
  public void close() {
    source.release();
    state = State.CLOSED;
  }

  @VisibleForTesting
  protected State state() { return state; }
}
