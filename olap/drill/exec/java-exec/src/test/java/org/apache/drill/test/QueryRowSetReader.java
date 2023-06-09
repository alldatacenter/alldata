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
import org.apache.drill.exec.physical.resultSet.impl.PullResultSetReaderImpl;
import org.apache.drill.exec.proto.UserBitShared.QueryId;

public class QueryRowSetReader extends PullResultSetReaderImpl {

  private final QueryBatchIterator batchIter;

  public QueryRowSetReader(QueryBatchIterator batchIter) {
    super(batchIter);
    this.batchIter = batchIter;
  }

  public static QueryRowSetReader build(BufferAllocator allocator, BufferingQueryEventListener listener) {
    return new QueryRowSetReader(new QueryBatchIterator(allocator, listener));
  }

  public QueryId queryId() { return batchIter.queryId(); }
  public String queryIdString() { return batchIter.queryIdString(); }

  @Override
  public void close() {
    batchIter.close();
  }
}
