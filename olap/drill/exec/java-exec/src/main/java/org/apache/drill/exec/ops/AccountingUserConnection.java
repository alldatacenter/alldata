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
package org.apache.drill.exec.ops;

import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.UserClientConnection;

/**
 * Wrapper around a {@link UserClientConnection} that tracks the status of batches
 * sent to User.
 */
public class AccountingUserConnection {
  private final UserClientConnection connection;
  private final SendingAccountor sendingAccountor;
  private final RpcOutcomeListener<Ack> statusHandler;

  public AccountingUserConnection(UserClientConnection connection, SendingAccountor sendingAccountor,
      RpcOutcomeListener<Ack> statusHandler) {
    this.connection = connection;
    this.sendingAccountor = sendingAccountor;
    this.statusHandler = statusHandler;
  }

  public void sendData(QueryDataPackage data) {
    sendingAccountor.increment();
    connection.sendData(statusHandler, data);
  }
}
