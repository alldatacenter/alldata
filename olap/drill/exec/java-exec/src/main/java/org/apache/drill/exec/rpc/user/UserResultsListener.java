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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.rpc.ConnectionThrottle;

public interface UserResultsListener {

  /**
   * QueryId is available. Called when a query is successfully submitted to the
   * server.
   *
   * @param queryId
   *          sent by the server along {@link org.apache.drill.exec.rpc.Acks.OK
   *          Acks.OK}
   */
  void queryIdArrived(QueryId queryId);

  /**
   * The query has failed. Most likely called when the server returns a FAILED
   * query state. Can also be called if
   * {@link #dataArrived(QueryDataBatch, ConnectionThrottle) dataArrived()}
   * throws an exception
   *
   * @param ex exception describing the cause of the failure
   */
  void submissionFailed(UserException ex);

  /**
   * A {@link org.apache.drill.exec.proto.beans.QueryData QueryData} message was received
   * @param result data batch received
   * @param throttle connection throttle
   */
  void dataArrived(QueryDataBatch result, ConnectionThrottle throttle);

  /**
   * The query has completed (successful completion or cancellation). The
   * listener will not receive any other data or result message. Called when the
   * server returns a terminal-non failing- state (COMPLETED or CANCELLED)
   */
  void queryCompleted(QueryState state);
}
