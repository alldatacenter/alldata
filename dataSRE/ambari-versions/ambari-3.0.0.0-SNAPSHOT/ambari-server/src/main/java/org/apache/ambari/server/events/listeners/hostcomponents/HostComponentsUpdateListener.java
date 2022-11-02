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
package org.apache.ambari.server.events.listeners.hostcomponents;

import java.util.Collections;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostComponentUpdate;
import org.apache.ambari.server.events.HostComponentsUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.StaleConfigsUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
@EagerSingleton
public class HostComponentsUpdateListener {

  private final STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private Provider<ConfigHelper> m_configHelper;

  @Inject
  public HostComponentsUpdateListener(AmbariEventPublisher ambariEventPublisher,
                                      STOMPUpdatePublisher STOMPUpdatePublisher) {
    ambariEventPublisher.register(this);
    this.STOMPUpdatePublisher = STOMPUpdatePublisher;
  }

  @Subscribe
  public void onMaintenanceStateUpdate(MaintenanceModeEvent event) {
    if (event.getServiceComponentHost() != null) {
      Long clusterId = event.getClusterId();
      ServiceComponentHost serviceComponentHost = event.getServiceComponentHost();
      String serviceName = serviceComponentHost.getServiceName();
      String componentName = serviceComponentHost.getServiceComponentName();
      String hostName = serviceComponentHost.getHostName();
      MaintenanceState maintenanceState = serviceComponentHost.getMaintenanceState();
      HostComponentUpdate hostComponentUpdate =
          HostComponentUpdate.createHostComponentMaintenanceStatusUpdate(clusterId, serviceName, hostName,
              componentName, maintenanceState);
      HostComponentsUpdateEvent hostComponentsUpdateEvent = new HostComponentsUpdateEvent(
          Collections.singletonList(hostComponentUpdate));

      STOMPUpdatePublisher.publish(hostComponentsUpdateEvent);
    }
  }

  @Subscribe
  public void onStaleConfigsStateUpdate(StaleConfigsUpdateEvent staleConfigsUpdateEvent) throws AmbariException {
    ServiceComponentHost serviceComponentHost = staleConfigsUpdateEvent.getServiceComponentHost();
    if (staleConfigsUpdateEvent.getStaleConfigs() != null) {
      boolean staleConfigs = staleConfigsUpdateEvent.getStaleConfigs();
      if (m_configHelper.get().wasStaleConfigsStatusUpdated(serviceComponentHost.getClusterId(),
          serviceComponentHost.getHost().getHostId(), serviceComponentHost.getServiceName(),
          serviceComponentHost.getServiceComponentName(), staleConfigs)) {
        STOMPUpdatePublisher.publish(new HostComponentsUpdateEvent(Collections.singletonList(
            HostComponentUpdate.createHostComponentStaleConfigsStatusUpdate(serviceComponentHost.getClusterId(),
                serviceComponentHost.getServiceName(), serviceComponentHost.getHostName(),
                serviceComponentHost.getServiceComponentName(), staleConfigs))));
      }
    }
  }
}
