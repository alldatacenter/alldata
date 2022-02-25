/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.state.Host;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

@Singleton
public class AgentSessionManager {

  private final ConcurrentMap<String, Host> registeredHosts = new ConcurrentHashMap<>(); // session ID -> host
  private final ConcurrentMap<Long, String> registeredSessionIds = new ConcurrentHashMap<>();

  public void register(String sessionId, Host host) {
    Preconditions.checkNotNull(sessionId);
    Preconditions.checkNotNull(host);
    Preconditions.checkNotNull(host.getHostId());

    String oldSessionId = registeredSessionIds.put(host.getHostId(), sessionId);
    if (oldSessionId != null) {
      registeredHosts.remove(oldSessionId);
    }
    registeredHosts.put(sessionId, host);
  }

  public boolean isRegistered(String sessionId) {
    return registeredHosts.containsKey(sessionId);
  }

  public Host getHost(String sessionId) throws HostNotRegisteredException {
    Preconditions.checkNotNull(sessionId);

    Host host = registeredHosts.get(sessionId);
    if (host != null) {
      return host;
    }

    throw HostNotRegisteredException.forSessionId(sessionId);
  }

  public String getSessionId(Long hostId) throws HostNotRegisteredException {
    Preconditions.checkNotNull(hostId);

    String sessionId = registeredSessionIds.get(hostId);
    if (sessionId != null) {
      return sessionId;
    }

    throw HostNotRegisteredException.forHostId(hostId);
  }

  public void unregisterByHost(Long hostId) {
    Preconditions.checkNotNull(hostId);

    String sessionId = registeredSessionIds.remove(hostId);
    if (sessionId != null) {
      registeredHosts.remove(sessionId);
    }
  }
}
