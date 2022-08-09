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

import java.util.List;

import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class StartHostTask extends TopologyHostTask {

  private final static Logger LOG = LoggerFactory.getLogger(StartHostTask.class);

  @AssistedInject
  public StartHostTask(@Assisted ClusterTopology topology, @Assisted HostRequest hostRequest, @Assisted boolean skipFailure) {
    super(topology, hostRequest);
    this.skipFailure = skipFailure;
  }

  @Override
  public Type getType() {
    return Type.START;
  }

  @Override
  public void runTask() {
    LOG.info("HostRequest: Executing START task for host: {}", hostRequest.getHostName());

    RequestStatusResponse response = clusterTopology.startHost(hostRequest.getHostName(), skipFailure);
    if (response != null) {
      // map logical install tasks to physical install tasks
      List<ShortTaskStatus> underlyingTasks = response.getTasks();
      for (ShortTaskStatus task : underlyingTasks) {

        String component = task.getRole();
        Long logicalStartTaskId = hostRequest.getLogicalTasksForTopologyTask(this).get(component);
        if (logicalStartTaskId == null) {
          LOG.info("Skipping physical start task registering, because component {} cannot be found", task.getRole());
          continue;
        }
        // for now just set on outer map
        hostRequest.registerPhysicalTaskId(logicalStartTaskId, task.getTaskId());
      }
    }

    LOG.info("HostRequest: Exiting START task for host: {}", hostRequest.getHostName());
  }
}