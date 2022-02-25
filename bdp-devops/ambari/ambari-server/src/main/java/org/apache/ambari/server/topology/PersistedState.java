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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.internal.BaseClusterRequest;
import org.apache.ambari.server.state.Host;

/**
 * Persistence abstraction.
 */
public interface PersistedState {
  /**
   * Persist a topology request.
   *
   * @param topologyRequest  topology request to persist
   *
   * @return a persisted topology request which is a wrapper around a TopologyRequest which
   * adds an id that can be used to refer to the persisted entity
   */
  PersistedTopologyRequest persistTopologyRequest(BaseClusterRequest topologyRequest);

  /**
   * Persist a logical request.
   *
   * @param logicalRequest     logical request to persist
   * @param topologyRequestId  the id of the associated topology request
   */
  void persistLogicalRequest(LogicalRequest logicalRequest, long topologyRequestId);

  /**
   * Register a physical task with a logical task.
   *
   * @param logicalTaskId   logical task id
   * @param physicalTaskId  physical task id
   */
  void registerPhysicalTask(long logicalTaskId, long physicalTaskId);

  /**
   * Registeer a host with a host request.
   *
   * @param hostRequestId  host request id
   * @param hostName       name of host being registered
   */
  void registerHostName(long hostRequestId, String hostName);

  /**
   * Get all persisted requests.  This is used to replay all
   * requests upon ambari startup.
   *
   * @return map of cluster topology to list of logical requests
   */
  Map<ClusterTopology, List<LogicalRequest>> getAllRequests();

  void registerInTopologyHostInfo(Host host);

  /**
   * Returns provision request for a cluster
   * @param clusterId
   * @return
   */
  LogicalRequest getProvisionRequest(long clusterId);

  /**
   * Remove the given host requests (must belong to the same topology request),
   * and also the topology request if it does not have any host requests left.
   */
  void removeHostRequests(long logicalRequestId, Collection<HostRequest> hostRequests);

  /**
   * Update the status of the given host request.
   */
  void setHostRequestStatus(long hostRequestId, HostRoleStatus status, String message);
}
