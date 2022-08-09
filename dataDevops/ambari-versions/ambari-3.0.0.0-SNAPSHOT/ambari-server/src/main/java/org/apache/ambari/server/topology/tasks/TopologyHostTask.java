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
package org.apache.ambari.server.topology.tasks;

import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class TopologyHostTask implements TopologyTask {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyHostTask.class);

  ClusterTopology clusterTopology;
  HostRequest hostRequest;
  boolean skipFailure;

  public TopologyHostTask(ClusterTopology topology, HostRequest hostRequest) {
    this.clusterTopology = topology;
    this.hostRequest = hostRequest;
  }

  public HostRequest getHostRequest() {
    return hostRequest;
  }

  /**
   * Run with an InternalAuthenticationToken as when running these tasks we might not have any active security context.
   */
  @Override
  public void run() {
    Authentication savedAuthContext = SecurityContextHolder.getContext().getAuthentication();
    try {
      InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken(TopologyManager.INTERNAL_AUTH_TOKEN);
      authenticationToken.setAuthenticated(true);
      SecurityContextHolder.getContext().setAuthentication(authenticationToken);
      runTask();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(savedAuthContext);
    }
  }

  public abstract void runTask();

}
