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
package org.apache.drill.exec.rpc.user;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.BaseRpcOutcomeListener;
import org.apache.drill.exec.rpc.user.UserClient.UserToBitConnection;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.RpcBus;
import org.apache.drill.exec.rpc.RpcConnectionHandler;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the future management of query submissions.  This entails a
 * potential race condition.  Normal ordering is:
 * <ul>
 *   <li>1.  Submit query to be executed. </li>
 *   <li>2.  Receive QueryHandle for buffer management. </li>
 *   <li>3.  Start receiving results batches for query. </li>
 * </ul>
 * However, 3 could potentially occur before 2.   Because of that, we need to
 * handle this case and then do a switcheroo.
 */
public class QueryResultHandler {
  private static final Logger logger =
      LoggerFactory.getLogger(QueryResultHandler.class);

  /**
   * Current listener for results, for each active query.
   * <p>
   *   Concurrency:  Access by SubmissionLister for query-ID message vs.
   *   access by batchArrived is not otherwise synchronized.
   * </p>
   */
  private final ConcurrentMap<QueryId, UserResultsListener> queryIdToResultsListenersMap =
      Maps.newConcurrentMap();

  public RpcOutcomeListener<QueryId> getWrappedListener(UserResultsListener resultsListener) {
    return new SubmissionListener(resultsListener);
  }

  public RpcConnectionHandler<UserToBitConnection> getWrappedConnectionHandler(
      RpcConnectionHandler<UserToBitConnection> handler) {
    return new ChannelClosedHandler(handler);
  }

  /**
   * Maps internal low-level API protocol to {@link UserResultsListener}-level API protocol.
   * handles data result messages
   */
  public void resultArrived(ByteBuf pBody) throws RpcException {
    QueryResult queryResult = RpcBus.get(pBody, QueryResult.PARSER);

    QueryId queryId = queryResult.getQueryId();
    QueryState queryState = queryResult.getQueryState();

    if (logger.isDebugEnabled()) {
      logger.debug("resultArrived: queryState: {}, queryId = {}", queryState,
          QueryIdHelper.getQueryId(queryId));
    }

    assert queryResult.hasQueryState() : "received query result without QueryState";

    boolean isFailureResult = QueryState.FAILED == queryState;
    // CANCELED queries are handled the same way as COMPLETED
    boolean isTerminalResult;
    switch (queryState) {
      case FAILED:
      case CANCELED:
      case COMPLETED:
        isTerminalResult = true;
        break;
      default:
        logger.error("Unexpected/unhandled QueryState " + queryState
          + " (for query " + queryId +  ")");
        isTerminalResult = false;
        break;
    }

    assert isFailureResult || queryResult.getErrorCount() == 0
      : "Error count for the query batch is non-zero but QueryState != FAILED";

    UserResultsListener resultsListener = newUserResultsListener(queryId);

    try {
      if (isFailureResult) {

        // Failure case--pass on via submissionFailed(...).
        resultsListener.submissionFailed(new UserRemoteException(queryResult.getError(0)));
        // Note: Listener is removed in finally below.
      } else if (isTerminalResult) {

        // A successful completion/canceled case--pass on via resultArrived
        try {
          resultsListener.queryCompleted(queryState);
        } catch (Exception e) {
          resultsListener.submissionFailed(UserException.systemError(e).build(logger));
        }
      } else {
        logger.warn("queryState {} was ignored", queryState);
      }
    } finally {
      if (isTerminalResult) {
        // TODO:  What exactly are we checking for?  How should we really check
        // for it?
        if ((! (resultsListener instanceof BufferingResultsListener)
          || ((BufferingResultsListener) resultsListener).output != null)) {
          queryIdToResultsListenersMap.remove(queryId, resultsListener);
        }
      }
    }
  }

  /**
   * Maps internal low-level API protocol to {@link UserResultsListener}-level API protocol.
   * handles query data messages
   */
  public void batchArrived(ConnectionThrottle throttle,
                            ByteBuf pBody, ByteBuf dBody) throws RpcException {
    QueryData queryData = RpcBus.get(pBody, QueryData.PARSER);
    // Current batch coming in.
    DrillBuf drillBuf = (DrillBuf) dBody;
    QueryDataBatch batch = new QueryDataBatch(queryData, drillBuf);

    QueryId queryId = queryData.getQueryId();

    if (logger.isDebugEnabled()) {
      logger.debug("batchArrived: queryId = {}", QueryIdHelper.getQueryId(queryId));
    }
    logger.trace("batchArrived: batch = {}", batch);

    UserResultsListener resultsListener = newUserResultsListener(queryId);

    // A data case--pass on via dataArrived
    try {
      resultsListener.dataArrived(batch, throttle);
      // That releases batch if successful.
    } catch (Exception e) {
      batch.release();
      resultsListener.submissionFailed(UserException.systemError(e).build(logger));
    }
  }

  /**
   * Return {@link UserResultsListener} associated with queryId. Will create a
   * new {@link BufferingResultsListener} if no listener found.
   *
   * @param queryId
   *          queryId we are getting the listener for
   * @return {@link UserResultsListener} associated with queryId
   */
  private UserResultsListener newUserResultsListener(QueryId queryId) {
    UserResultsListener resultsListener = queryIdToResultsListenersMap.get(queryId);
    logger.trace("For QueryId [{}], retrieved results listener {}", queryId, resultsListener);
    if (null == resultsListener) {

      // WHO?? didn't get query ID response and set submission listener yet,
      // so install a buffering listener for now
      BufferingResultsListener bl = new BufferingResultsListener();
      resultsListener = queryIdToResultsListenersMap.putIfAbsent(queryId, bl);
      // If we had a successful insertion, use that reference.  Otherwise, just
      // throw away the new buffering listener.
      if (null == resultsListener) {
        resultsListener = bl;
      }
    }
    return resultsListener;
  }

  private static class BufferingResultsListener implements UserResultsListener {

    private final ConcurrentLinkedQueue<QueryDataBatch> results = Queues.newConcurrentLinkedQueue();
    private volatile UserException ex;
    private volatile QueryState queryState;
    private volatile UserResultsListener output;
    private volatile ConnectionThrottle throttle;

    public boolean transferTo(UserResultsListener l) {
      synchronized (this) {
        output = l;
        for (QueryDataBatch r : results) {
          l.dataArrived(r, throttle);
        }
        if (ex != null) {
          l.submissionFailed(ex);
          return true;
        } else if (queryState != null) {
          l.queryCompleted(queryState);
          return true;
        }

        return false;
      }
    }

    @Override
    public void queryCompleted(QueryState state) {
      assert queryState == null;
      this.queryState = state;
      synchronized (this) {
        if (output != null) {
          output.queryCompleted(state);
        }
      }
    }

    @Override
    public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
      this.throttle = throttle;

      synchronized (this) {
        if (output == null) {
          this.results.add(result);
        } else {
          output.dataArrived(result, throttle);
        }
      }
    }

    @Override
    public void submissionFailed(UserException ex) {
      assert queryState == null;
      // there is one case when submissionFailed() is called even though the
      // query didn't fail on the server side
      // it happens when UserResultsListener.batchArrived() throws an exception
      // that will be passed to
      // submissionFailed() by QueryResultHandler.dataArrived()
      queryState = QueryState.FAILED;
      synchronized (this) {
        if (output == null) {
          this.ex = ex;
        } else{
          output.submissionFailed(ex);
        }
      }
    }

    @Override
    public void queryIdArrived(QueryId queryId) { }
  }

  private class SubmissionListener extends BaseRpcOutcomeListener<QueryId> {

    private final UserResultsListener resultsListener;
    private final AtomicBoolean isTerminal = new AtomicBoolean(false);

    public SubmissionListener(UserResultsListener resultsListener) {
      this.resultsListener = resultsListener;
    }

    @Override
    public void failed(RpcException ex) {
      if (!isTerminal.compareAndSet(false, true)) {
        logger.warn("Received multiple responses to run query request.");
        return;
      }

      // Although query submission failed, results might have arrived for this query.
      // However, the results could not be transferred to this resultListener because
      // there is no query id mapped to this resultListener. Look out for the warning
      // message from ChannelClosedHandler in the client logs.
      // TODO(DRILL-4586)
      resultsListener.submissionFailed(UserException.systemError(ex)
          .addContext("Query submission to Drillbit failed.")
          .build(logger));
    }

    @Override
    public void success(QueryId queryId, ByteBuf buf) {
      if (!isTerminal.compareAndSet(false, true)) {
        logger.warn("Received multiple responses to run query request.");
        return;
      }

      resultsListener.queryIdArrived(queryId);
      if (logger.isDebugEnabled()) {
        logger.debug("Received QueryId {} successfully. Adding results listener {}.",
          QueryIdHelper.getQueryId(queryId), resultsListener);
      }
      UserResultsListener oldListener =
          queryIdToResultsListenersMap.putIfAbsent(queryId, resultsListener);

      // We need to deal with the situation where we already received results by
      // the time we got the query id back.  In that case, we'll need to
      // transfer the buffering listener over, grabbing a lock against reception
      // of additional results during the transition.
      if (oldListener != null) {
        logger.debug("Unable to place user results listener, buffering listener was already in place.");
        if (oldListener instanceof BufferingResultsListener) {
          boolean all = ((BufferingResultsListener) oldListener).transferTo(this.resultsListener);
          // simply remove the buffering listener if we already have the last response.
          if (all) {
            queryIdToResultsListenersMap.remove(queryId);
          } else {
            boolean replaced = queryIdToResultsListenersMap.replace(queryId, oldListener, resultsListener);
            if (!replaced) {
              throw new IllegalStateException(); // TODO: Say what the problem is!
            }
          }
        } else {
          throw new IllegalStateException("Trying to replace a non-buffering User Results listener.");
        }
      }
    }

    @Override
    public void interrupted(InterruptedException ex) {
      if (!isTerminal.compareAndSet(false, true)) {
        logger.warn("Received multiple responses to run query request.");
        return;
      }

      // TODO(DRILL-4586)
      resultsListener.submissionFailed(UserException.systemError(ex)
          .addContext("The client had been asked to wait as the Drillbit is potentially being over-utilized." +
              " But the client was interrupted while waiting.")
          .build(logger));
    }
  }

  /**
   * When a {@link UserToBitConnection connection} to a server is successfully
   * created, this handler adds a listener to that connection that listens to
   * connection closure. If the connection is closed, all active
   * {@link UserResultsListener result listeners} are failed.
   */
  private class ChannelClosedHandler implements RpcConnectionHandler<UserToBitConnection> {

    private final RpcConnectionHandler<UserToBitConnection> parentHandler;

    public ChannelClosedHandler(RpcConnectionHandler<UserToBitConnection> parentHandler) {
      this.parentHandler = parentHandler;
    }

    @Override
    public void connectionSucceeded(UserToBitConnection connection) {
      connection.getChannel().closeFuture().addListener(
          new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future)
                throws Exception {
              for (UserResultsListener listener : queryIdToResultsListenersMap.values()) {
                listener.submissionFailed(UserException.connectionError()
                    .message("Connection %s closed unexpectedly. Drillbit down?",
                        connection.getName())
                    .build(logger));
                if (listener instanceof BufferingResultsListener) {
                  // the appropriate listener will be failed by SubmissionListener#failed
                  logger.warn("Buffering listener failed before results were transferred to the actual listener.");
                }
              }
            }
          });
      parentHandler.connectionSucceeded(connection);
    }

    @Override
    public void connectionFailed(FailureType type, Throwable t) {
      parentHandler.connectionFailed(type, t);
    }
  }
}
