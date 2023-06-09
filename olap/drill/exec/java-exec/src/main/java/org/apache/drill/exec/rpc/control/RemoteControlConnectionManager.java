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

import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.BasicClient;

public class RemoteControlConnectionManager extends ControlConnectionManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RemoteControlConnectionManager.class);

  private final ControlConnectionConfig config;
  private final DrillbitEndpoint remoteEndpoint;

  public RemoteControlConnectionManager(ControlConnectionConfig config, DrillbitEndpoint
    localEndpoint, DrillbitEndpoint remoteEndpoint) {
    super(localEndpoint, remoteEndpoint);
    this.config = config;
    this.remoteEndpoint = remoteEndpoint;
  }

  @Override
  protected BasicClient<?, ControlConnection, BitControl.BitControlHandshake, ?> getNewClient() {
    return new ControlClient(config, remoteEndpoint, new CloseHandlerCreator());
  }
}
