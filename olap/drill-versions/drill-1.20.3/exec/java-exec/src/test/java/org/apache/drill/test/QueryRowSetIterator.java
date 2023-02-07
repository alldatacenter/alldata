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

import java.util.Iterator;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetFormatter;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;

/**
 * Converts an incoming set of record batches into an iterator over a
 * set of row sets. Primarily for testing.
 */
public class QueryRowSetIterator implements Iterator<DirectRowSet>, Iterable<DirectRowSet> {
  private final QueryBatchIterator batchIter;

  public QueryRowSetIterator(BufferAllocator allocator, BufferingQueryEventListener listener) {
    batchIter = new QueryBatchIterator(allocator, listener);
  }

  public QueryId queryId() { return batchIter.queryId(); }
  public String queryIdString() { return batchIter.queryIdString(); }
  public QueryState finalState() { return batchIter.finalState(); }
  public int batchCount() { return batchIter.batchCount(); }
  public int rowCount() { return batchIter.rowCount(); }

  @Override
  public boolean hasNext() {
    return batchIter.next();
  }

  @Override
  public DirectRowSet next() {
    return DirectRowSet.fromContainer(batchIter.batch());
  }

  public void printAll() {
    for (DirectRowSet rowSet : this) {
      RowSetFormatter.print(rowSet);
      rowSet.clear();
    }
  }

  @Override
  public Iterator<DirectRowSet> iterator() {
    return this;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
