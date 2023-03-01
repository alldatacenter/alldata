/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.client.factory;

import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.impl.grpc.ShuffleServerGrpcClient;
import org.apache.uniffle.common.ClientType;
import org.apache.uniffle.common.ShuffleServerInfo;

public class ShuffleServerClientFactory {

  private Map<String, Map<ShuffleServerInfo, ShuffleServerClient>> clients;

  private ShuffleServerClientFactory() {
    clients = Maps.newConcurrentMap();
  }

  private static class LazyHolder {
    static final ShuffleServerClientFactory INSTANCE = new ShuffleServerClientFactory();
  }

  public static ShuffleServerClientFactory getInstance() {
    return LazyHolder.INSTANCE;
  }

  private ShuffleServerClient createShuffleServerClient(String clientType, ShuffleServerInfo shuffleServerInfo) {
    if (clientType.equalsIgnoreCase(ClientType.GRPC.name())) {
      return new ShuffleServerGrpcClient(shuffleServerInfo.getHost(), shuffleServerInfo.getPort());
    } else {
      throw new UnsupportedOperationException("Unsupported client type " + clientType);
    }
  }

  public synchronized ShuffleServerClient getShuffleServerClient(
      String clientType, ShuffleServerInfo shuffleServerInfo) {
    clients.putIfAbsent(clientType, Maps.newConcurrentMap());
    Map<ShuffleServerInfo, ShuffleServerClient> serverToClients = clients.get(clientType);
    if (serverToClients.get(shuffleServerInfo) == null) {
      serverToClients.put(shuffleServerInfo, createShuffleServerClient(clientType, shuffleServerInfo));
    }
    return serverToClients.get(shuffleServerInfo);
  }

  // Only for tests
  public synchronized void cleanupCache() {
    clients.values().stream().flatMap(x -> x.values().stream()).forEach(ShuffleServerClient::close);
    this.clients = Maps.newConcurrentMap();
  }
}
