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
package org.apache.drill.exec.rpc;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.helper.QueryIdHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Helps to run a query and await on the results. All the inheriting sub-class
 * manages the session/connection state and submits query with respect to that
 * state. The subclass instance lifetime is per query lifetime and is not
 * re-used.
 */
public abstract class AbstractDisposableUserClientConnection implements UserClientConnection {
  private static final Logger logger =
      LoggerFactory.getLogger(AbstractDisposableUserClientConnection.class);

  protected final CountDownLatch latch = new CountDownLatch(1);

  protected volatile DrillPBError error;

  protected String queryState;

  /**
   * Wait until the query has completed or timeout is passed.
   *
   * @throws InterruptedException
   */
  public boolean await(final long timeoutMillis) throws InterruptedException {
    return latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Wait indefinitely until the query is completed. Used only in case of WebUser
   *
   * @throws Exception
   */
  public void await() throws Exception {
    latch.await();
  }

  @Override
  public void sendResult(RpcOutcomeListener<Ack> listener, QueryResult result) {

    Preconditions.checkState(result.hasQueryState());

    // Release the wait latch if the query is terminated.
    final QueryState state = result.getQueryState();
    queryState = state.toString();
    final QueryId queryId = result.getQueryId();

    if (logger.isDebugEnabled()) {
      logger.debug("Result arrived for QueryId: {} with QueryState: {}",
          QueryIdHelper.getQueryId(queryId), state);
    }

    switch (state) {
      case FAILED:
        error = result.getError(0);
        latch.countDown();
        break;
      case CANCELED:
      case COMPLETED:
        Preconditions.checkState(result.getErrorCount() == 0);
        latch.countDown();
        break;
      default:
        logger.error("Query with QueryId: {} is in unexpected state: {}", queryId, state);
    }

    // Notify the listener with ACK
    listener.success(Acks.OK, null);
  }

  /**
   * @return Any error returned in query execution.
   */
  public DrillPBError getError() {
    return error;
  }

  public String getQueryState() {
    return queryState;
  }
}