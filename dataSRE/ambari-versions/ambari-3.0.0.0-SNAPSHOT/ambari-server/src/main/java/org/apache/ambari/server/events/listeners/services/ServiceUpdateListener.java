/**
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
package org.apache.ambari.server.events.listeners.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostComponentUpdate;
import org.apache.ambari.server.events.HostComponentsUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
@EagerSingleton
public class ServiceUpdateListener {

  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  public ServiceUpdateListener(STOMPUpdatePublisher STOMPUpdatePublisher, AmbariEventPublisher ambariEventPublisher) {
    STOMPUpdatePublisher.registerAPI(this);
    ambariEventPublisher.register(this);

    this.STOMPUpdatePublisher = STOMPUpdatePublisher;
  }

  @Subscribe
  public void onHostComponentUpdate(HostComponentsUpdateEvent event) throws AmbariException {
    Map<Long, Set<String>> clustersServices = new HashMap<>();
    for (HostComponentUpdate hostComponentUpdate : event.getHostComponentUpdates()) {
      Long clusterId = hostComponentUpdate.getClusterId();
      String serviceName = hostComponentUpdate.getServiceName();
      clustersServices.computeIfAbsent(clusterId, c -> new HashSet<>()).add(serviceName);
    }
    for (Map.Entry<Long, Set<String>> clusterServices : clustersServices.entrySet()) {
      Long clusterId = clusterServices.getKey();
      String clusterName = m_clusters.get().getClusterById(clusterId).getClusterName();
      for (String serviceName : clusterServices.getValue()) {
        STOMPUpdatePublisher.publish(new ServiceUpdateEvent(clusterName, null, serviceName, null,
            true));
      }
    }
  }

  @Subscribe
  public void onMaintenanceStateUpdate(MaintenanceModeEvent event) throws AmbariException {
    if (event.getService() == null) {
      return;
    }
    Long clusterId = event.getClusterId();
    String clusterName = m_clusters.get().getClusterById(clusterId).getClusterName();
    String serviceName = event.getService().getName();

    MaintenanceState maintenanceState = event.getMaintenanceState();

    STOMPUpdatePublisher.publish(new ServiceUpdateEvent(clusterName, maintenanceState, serviceName, null,
        false));
  }
}
