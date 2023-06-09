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

import java.util.concurrent.BlockingQueue;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;

import org.apache.drill.shaded.guava.com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drill query event listener that buffers rows into a producer-consumer
 * queue. Allows rows to be received asynchronously, but processed by
 * a synchronous reader.
 * <p>
 * Query messages are transformed into events: query ID, batch,
 * EOF or error.
 */
public class BufferingQueryEventListener implements UserResultsListener {
  private static final Logger logger = LoggerFactory.getLogger(BufferingQueryEventListener.class);

  public static class QueryEvent {
    public enum Type { QUERY_ID, BATCH, EOF, ERROR }

    public final Type type;
    public QueryId queryId;
    public QueryDataBatch batch;
    public Exception error;
    public QueryState state;

    public QueryEvent(QueryId queryId) {
      this.queryId = queryId;
      this.type = Type.QUERY_ID;
    }

    public QueryEvent(Exception ex) {
      error = ex;
      type = Type.ERROR;
    }

    public QueryEvent(QueryDataBatch batch) {
      this.batch = batch;
      type = Type.BATCH;
    }

    public QueryEvent(QueryState state) {
      this.type = Type.EOF;
      this.state = state;
    }
  }

  private final BlockingQueue<QueryEvent> queue = Queues.newLinkedBlockingQueue();

  @Override
  public void queryIdArrived(QueryId queryId) {
    silentPut(new QueryEvent(queryId));
  }

  @Override
  public void submissionFailed(UserException ex) {
    silentPut(new QueryEvent(ex));
  }

  @Override
  public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
    silentPut(new QueryEvent(result));
  }

  @Override
  public void queryCompleted(QueryState state) {
    silentPut(new QueryEvent(state));
  }

  private void silentPut(QueryEvent event) {
    try {
      queue.put(event);
    } catch (InterruptedException e) {
      logger.error("Exception:", e);
    }
  }

  public QueryEvent get() {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      // Should not occur, but if it does, just pass along the error.
      return new QueryEvent(e);
    }
  }
}
