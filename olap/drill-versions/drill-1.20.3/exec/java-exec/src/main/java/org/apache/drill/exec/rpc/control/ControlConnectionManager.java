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
package org.apache.drill.exec.rpc.control;

import org.apache.drill.exec.proto.BitControl.BitControlHandshake;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.BasicClient;
import org.apache.drill.exec.rpc.ReconnectingConnection;

/**
 * Maintains connection between two particular bits.
 */
public abstract class ControlConnectionManager extends ReconnectingConnection<ControlConnection, BitControlHandshake>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControlConnectionManager.class);

  public ControlConnectionManager(DrillbitEndpoint localEndpoint, DrillbitEndpoint remoteEndpoint) {
    super(
        BitControlHandshake.newBuilder()
            .setRpcVersion(ControlRpcConfig.RPC_VERSION)
            .setEndpoint(localEndpoint)
            .build(),
        remoteEndpoint.getAddress(),
        remoteEndpoint.getControlPort());
  }

  @Override
  protected abstract BasicClient<?, ControlConnection, BitControlHandshake, ?> getNewClient();
}
