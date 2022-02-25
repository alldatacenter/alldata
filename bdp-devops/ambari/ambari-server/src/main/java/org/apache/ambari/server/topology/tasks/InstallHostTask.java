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
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class InstallHostTask extends TopologyHostTask {

  private final static Logger LOG = LoggerFactory.getLogger(InstallHostTask.class);

  @AssistedInject
  public InstallHostTask(@Assisted ClusterTopology topology, @Assisted HostRequest hostRequest, @Assisted boolean skipFailure) {
    super(topology, hostRequest);
    this.skipFailure = skipFailure;
  }

  @Override
  public Type getType() {
    return Type.INSTALL;
  }

  @Override
  public void runTask() {
    LOG.info("HostRequest: Executing INSTALL task for host: {}", hostRequest.getHostName());
    boolean skipInstallTaskCreate = clusterTopology.getProvisionAction().equals(ProvisionAction.START_ONLY);
    RequestStatusResponse response = clusterTopology.installHost(hostRequest.getHostName(), skipInstallTaskCreate, skipFailure);
    if(response != null) {
      // map logical install tasks to physical install tasks
      List<ShortTaskStatus> underlyingTasks = response.getTasks();
      for (ShortTaskStatus task : underlyingTasks) {

        String component = task.getRole();
        Long logicalInstallTaskId = hostRequest.getLogicalTasksForTopologyTask(this).get(component);
        if (logicalInstallTaskId == null) {
          LOG.info("Skipping physical install task registering, because component {} cannot be found", task.getRole());
          continue;
        }
        //todo: for now only one physical task per component
        long taskId = task.getTaskId();
        hostRequest.registerPhysicalTaskId(logicalInstallTaskId, taskId);
      }
    }

    LOG.info("HostRequest: Exiting INSTALL task for host: {}", hostRequest.getHostName());
  }
}