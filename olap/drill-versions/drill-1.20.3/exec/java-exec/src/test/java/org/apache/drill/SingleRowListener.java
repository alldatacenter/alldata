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
package org.apache.drill;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;

/**
 * Result listener that is set up to receive a single row. Useful for queries
 * such with a count(*) or limit 1. The abstract method {@link #rowArrived(QueryDataBatch)} provides
 * the means for a derived class to get the expected record's data.
 */
public abstract class SingleRowListener implements UserResultsListener {
  private final CountDownLatch latch = new CountDownLatch(1); // used to wait for completion
  private final AtomicInteger nRows = new AtomicInteger(0); // counts rows received
  private QueryState queryState = null; // last received QueryState, if any
  private final List<DrillPBError> errorList = new LinkedList<>(); // all errors ever received
  private Exception exception = null; // the exception captured from a submission failure

  @Override
  public void queryIdArrived(final QueryId queryId) {
  }

  @Override
  public void submissionFailed(final UserException ex) {
    exception = ex;
    synchronized(errorList) {
      errorList.add(ex.getOrCreatePBError(false));
    }
    latch.countDown();
  }

  @Override
  public void queryCompleted(QueryState state) {
    queryState = state;
    try {
      cleanup();
    } finally {
      latch.countDown();
    }
  }

  @Override
  public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
    final QueryData queryData = result.getHeader();
    if (result.hasData()) {
      final int nRows = this.nRows.addAndGet(queryData.getRowCount());
      if (nRows > 1) {
        throw new IllegalStateException("Expected exactly one row, but got " + nRows);
      }

      rowArrived(result);
    }

    result.release();
  }

  /**
   * Get the last known QueryState.
   *
   * @return the query state; may be null if no query state has been received
   */
  public QueryState getQueryState() {
    return queryState;
  }

  /**
   * Get an immutable copy of the list of all errors received so far.
   *
   * @return list of errors received
   */
  public List<DrillPBError> getErrorList() {
    synchronized(errorList) {
      return Collections.unmodifiableList(errorList);
    }
  }

  /**
   * A record has arrived and is ready for access.
   *
   * <p>Derived classes provide whatever implementation they require here to access
   * the record's data.
   *
   * @param queryDataBatch result batch holding the row
   */
  protected abstract void rowArrived(QueryDataBatch queryDataBatch);

  /**
   * Wait for the completion of this query; receiving a record or an error will both cause the
   * query to be considered complete
   *
   * @throws Exception if there was any kind of problem
   */
  public void waitForCompletion() throws Exception {
    latch.await();
    if (exception != null) {
      throw new RuntimeException("Query submission failed", exception);
    }
  }

  /**
   * Clean up any resources used.
   *
   * <p>Derived classes may use this to free things like allocators or files that were used to
   * record data received in resultArrived().
   */
  public void cleanup() {
  }
}
