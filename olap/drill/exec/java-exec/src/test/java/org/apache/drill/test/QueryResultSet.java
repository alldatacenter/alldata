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

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.test.BufferingQueryEventListener.QueryEvent;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;

/**
 * Returns query results as an iterator over row sets. Provides
 * a very easy way for tests to work with query data using the
 * row set tools.
 */
public class QueryResultSet {
  private final BufferingQueryEventListener listener;
  private boolean eof;
  private int recordCount;
  private int batchCount;
  private QueryId queryId;
  @SuppressWarnings("unused")
  private QueryState state;
  final RecordBatchLoader loader;

  public QueryResultSet(BufferingQueryEventListener listener, BufferAllocator allocator) {
    this.listener = listener;
    loader = new RecordBatchLoader(allocator);
  }

  /**
   * Return the next batch of data as a row set. The first batch is usually
   * empty as it carries only schema.
   *
   * @return the next batch as a row set, or null if EOF
   * @throws Exception on a server error
   */
  public DirectRowSet next() throws Exception {
    if (eof) {
      return null;
    }
    while (true) {
      QueryEvent event = listener.get();
      switch (event.type)
      {
      case BATCH:
        batchCount++;
        recordCount += event.batch.getHeader().getRowCount();
        loader.load(event.batch.getHeader().getDef(), event.batch.getData());
        event.batch.release();
        return DirectRowSet.fromVectorAccessible(loader.allocator(), loader);

      case EOF:
        state = event.state;
        eof = true;
        return null;

      case ERROR:
        state = event.state;
        eof = true;
        throw event.error;

      case QUERY_ID:
        queryId = event.queryId;
        continue;

      default:
        throw new IllegalStateException("Unexpected event: " + event.type);
      }
    }
  }

  public QueryId queryId() { return queryId; }
  public int recordCount() { return recordCount; }
  public int batchCount() { return batchCount; }

  public void close() {
    try {
      while (! eof) {
        RowSet rowSet = next();
        if (rowSet != null) {
          rowSet.clear();
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      loader.clear();
    }
  }
}
