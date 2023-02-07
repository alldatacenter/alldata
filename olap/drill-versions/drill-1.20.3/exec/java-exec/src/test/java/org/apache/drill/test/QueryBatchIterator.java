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
package org.apache.drill.test;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.resultSet.impl.PullResultSetReaderImpl.UpstreamSource;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.test.BufferingQueryEventListener.QueryEvent;

/**
 * Iterator over batches returned from a query. Uses a listener to obtain
 * the serialized batches, then decodes them into value vectors held
 * in a vector container - the same structure as used in the query
 * executor. This format allows us to use the "row set" classes on the
 * output of the query.
 */
public class QueryBatchIterator implements UpstreamSource, AutoCloseable {

  private enum State { START, RUN, RETAIN, EOF }

  private final BufferingQueryEventListener listener;
  private final RecordBatchLoader loader;
  private State state = State.START;
  private boolean retainData;
  private QueryId queryId;
  private QueryState queryState;
  private int schemaVersion;
  private int recordCount;
  private int batchCount;

  public QueryBatchIterator(BufferAllocator allocator, BufferingQueryEventListener listener) {
    this.listener = listener;
    this.loader = new RecordBatchLoader(allocator);
  }

  public QueryId queryId() { return queryId; }
  public String queryIdString() { return QueryIdHelper.getQueryId(queryId); }
  public QueryState finalState() { return queryState; }
  public int batchCount() { return batchCount; }
  public int rowCount() { return recordCount; }

  @Override
  public boolean next() {
    retainData = false;
    if (state == State.EOF) {
      return false;
    }
    while (true) {
      QueryEvent event = listener.get();
      queryState = event.state;
      switch (event.type) {
        case BATCH:

          // Skip over null batches
          if (loadBatch(event)) {
            return true;
          }
          break;
        case EOF:
          state = State.EOF;
          return false;
        case ERROR:
          state = State.EOF;
          if (event.error instanceof UserException) {
            throw (UserException) event.error;
          } else {
            throw new RuntimeException(event.error);
          }
        case QUERY_ID:
          queryId = event.queryId;
          break;
        default:
          throw new IllegalStateException("Unexpected event: " + event.type);
      }
    }
  }

  private boolean loadBatch(QueryEvent event) {
    batchCount++;
    recordCount += event.batch.getHeader().getRowCount();
    QueryDataBatch inputBatch = Preconditions.checkNotNull(event.batch);

    // Unload the batch and convert to a row set.
    loader.load(inputBatch.getHeader().getDef(), inputBatch.getData());
    inputBatch.release();
    VectorContainer batch = loader.getContainer();
    batch.setRecordCount(loader.getRecordCount());

    // Null results? Drill will return a single batch with no rows
    // and no columns even if the scan (or other) operator returns
    // no batches at all. For ease of testing, simply map this null
    // result set to a null output row set that says "nothing at all
    // was returned." Note that this is different than an empty result
    // set which has a schema, but no rows.
    if (batch.getRecordCount() == 0 && batch.getNumberOfColumns() == 0) {
      release();
      return false;
    }

    if (state == State.START || batch.isSchemaChanged()) {
      schemaVersion++;
    }
    state = State.RUN;
    return true;
  }

  @Override
  public int schemaVersion() { return schemaVersion; }

  @Override
  public VectorContainer batch() { return loader.getContainer(); }

  @Override
  public SelectionVector2 sv2() { return null; }

  @Override
  public void release() {
    loader.clear();
  }

  public void retainData() {
    retainData = true;
  }

  @Override
  public void close() {
    if (!retainData) {
      release();
    }

    // Consume any pending input
    while (state != State.EOF) {
      QueryEvent event = listener.get();
      if (event.type == QueryEvent.Type.EOF) {
        state = State.EOF;
      }
    }
  }
}
