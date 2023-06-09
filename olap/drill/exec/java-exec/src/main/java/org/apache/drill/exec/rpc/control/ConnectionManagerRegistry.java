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

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.exec.rpc.BitRpcUtility;

public class ConnectionManagerRegistry implements Iterable<ControlConnectionManager> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectionManagerRegistry.class);

  private final ConcurrentMap<DrillbitEndpoint, ControlConnectionManager> registry = Maps.newConcurrentMap();

  private final ControlConnectionConfig config;

  private DrillbitEndpoint localEndpoint;

  public ConnectionManagerRegistry(ControlConnectionConfig config) {
    this.config = config;
  }

  public ControlConnectionManager getConnectionManager(DrillbitEndpoint remoteEndpoint) {
    assert localEndpoint != null :
        "DrillbitEndpoint must be set before a connection manager can be retrieved";

    final boolean isLocalServer = BitRpcUtility.isLocalControlServer(localEndpoint, remoteEndpoint);
    ControlConnectionManager m = registry.get(remoteEndpoint);
    if (m == null) {
      m = (isLocalServer) ? new LocalControlConnectionManager(config, remoteEndpoint)
                    : new RemoteControlConnectionManager(config, localEndpoint, remoteEndpoint);
      final ControlConnectionManager m2 = registry.putIfAbsent(remoteEndpoint, m);
      if (m2 != null) {
        m = m2;
      }
    }

    return m;
  }

  void setLocalEndpoint(final DrillbitEndpoint endpoint) {
    this.localEndpoint = endpoint;
  }

  @Override
  public Iterator<ControlConnectionManager> iterator() {
    return registry.values().iterator();
  }

}
