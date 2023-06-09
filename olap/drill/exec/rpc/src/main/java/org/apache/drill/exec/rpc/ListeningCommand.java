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

import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;

public abstract class ListeningCommand<T extends MessageLite, C extends RemoteConnection,
  E extends EnumLite, M extends MessageLite> implements RpcCommand<T, C, E, M> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ListeningCommand.class);

  private final RpcOutcomeListener<T> listener;

  public ListeningCommand(RpcOutcomeListener<T> listener) {
    this.listener = listener;
  }

  public abstract void doRpcCall(RpcOutcomeListener<T> outcomeListener, C connection);

  @Override
  public void connectionAvailable(C connection) {

    doRpcCall(listener, connection);
  }

  @Override
  public void connectionSucceeded(C connection) {
    connectionAvailable(connection);
  }

  @Override
  public void connectionFailed(FailureType type, Throwable t) {
    listener.failed(RpcException.mapException(
        String.format("Command failed while establishing connection.  Failure type %s.", type), t));
  }

  @Override
  public RpcOutcomeListener<T> getOutcomeListener() {
    return listener;
  }

}
