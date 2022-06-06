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
package org.apache.ambari.server.events.listeners.hosts;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.HostStateUpdateEvent;
import org.apache.ambari.server.events.HostStatusUpdateEvent;
import org.apache.ambari.server.events.HostUpdateEvent;
import org.apache.ambari.server.events.InitialAlertEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.commons.lang.StringUtils;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
@EagerSingleton
public class HostUpdateListener {

  private Map<Long, Map<String, HostUpdateEvent>> hosts = new HashMap<>();

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private AlertsDAO alertsDAO;

  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  private Provider<StackAdvisorHelper> stackAdvisorHelperProvider;

  @Inject
  public HostUpdateListener(AmbariEventPublisher ambariEventPublisher, AlertEventPublisher m_alertEventPublisher) {
    ambariEventPublisher.register(this);
    m_alertEventPublisher.register(this);
  }

  @Subscribe
  public void onHostStatusUpdate(HostStatusUpdateEvent event) throws AmbariException {
    String hostName = event.getHostName();
    Long lastHeartbeatTime = m_clusters.get().getHost(hostName).getLastHeartbeatTime();

    for (Cluster cluster : m_clusters.get().getClustersForHost(hostName)) {
      Long clusterId = cluster.getClusterId();

      // retrieve state from cache
      HostUpdateEvent hostUpdateEvent = retrieveHostUpdateFromCache(clusterId, hostName);

      if (hostUpdateEvent.getHostStatus().equals(event.getHostStatus())) {
        continue;
      } else {
        hostUpdateEvent.setHostStatus(event.getHostStatus());
      }
      hostUpdateEvent.setLastHeartbeatTime(lastHeartbeatTime);

      STOMPUpdatePublisher.publish(HostUpdateEvent.createHostStatusUpdate(hostUpdateEvent.getClusterName(),
          hostUpdateEvent.getHostName(),
          hostUpdateEvent.getHostStatus(),
          hostUpdateEvent.getLastHeartbeatTime()));
      stackAdvisorHelperProvider.get().clearCaches(hostName);
    }
  }

  @Subscribe
  public void onHostStateUpdate(HostStateUpdateEvent event) throws AmbariException {
    String hostName = event.getHostName();
    Long lastHeartbeatTime = m_clusters.get().getHost(hostName).getLastHeartbeatTime();

    for (Cluster cluster : m_clusters.get().getClustersForHost(hostName)) {
      Long clusterId = cluster.getClusterId();

      // retrieve state from cache
      HostUpdateEvent hostUpdateEvent = retrieveHostUpdateFromCache(clusterId, hostName);

      if (hostUpdateEvent.getHostState().equals(event.getHostState())) {
        continue;
      } else {
        hostUpdateEvent.setHostState(event.getHostState());
      }
      hostUpdateEvent.setLastHeartbeatTime(lastHeartbeatTime);

      STOMPUpdatePublisher.publish(HostUpdateEvent.createHostStateUpdate(hostUpdateEvent.getClusterName(),
          hostUpdateEvent.getHostName(),
          hostUpdateEvent.getHostState(),
          hostUpdateEvent.getLastHeartbeatTime()));
    }
    stackAdvisorHelperProvider.get().clearCaches(hostName);
  }

  @Subscribe
  public void onAlertsHostUpdate(AlertEvent event) throws AmbariException {
    String hostName;
    if (!(event instanceof AlertStateChangeEvent) && !(event instanceof InitialAlertEvent)) {
      return;
    } else if (event instanceof AlertStateChangeEvent) {
      hostName = ((AlertStateChangeEvent) event).getNewHistoricalEntry().getHostName();
    } else {
      hostName = ((InitialAlertEvent) event).getNewHistoricalEntry().getHostName();
    }
    if (StringUtils.isEmpty(hostName)) {
      return;
    }

    Long clusterId = event.getClusterId();

    // retrieve state from cache
    HostUpdateEvent hostUpdateEvent = retrieveHostUpdateFromCache(clusterId, hostName);

    // change alerts counters
    AlertSummaryDTO summary = alertsDAO.findCurrentCounts(clusterId, null, hostName);
    if (hostUpdateEvent.getAlertsSummary().equals(summary)) {
      return;
    }
    hostUpdateEvent.setAlertsSummary(summary);

    STOMPUpdatePublisher.publish(HostUpdateEvent.createHostAlertsUpdate(hostUpdateEvent.getClusterName(),
        hostName, summary));
  }

  @Subscribe
  public void onMaintenanceStateUpdate(MaintenanceModeEvent event) throws AmbariException {
    Long clusterId = event.getClusterId();

    if (event.getHost() != null || event.getServiceComponentHost() != null) {
      String hostName = event.getHost() == null ? event.getServiceComponentHost().getHostName() : event.getHost().getHostName();

      HostUpdateEvent hostUpdateEvent = retrieveHostUpdateFromCache(clusterId, hostName);

      AlertSummaryDTO summary = alertsDAO.findCurrentCounts(clusterId, null, hostName);

      if (hostUpdateEvent.getAlertsSummary().equals(summary)) {
        return;
      }
      hostUpdateEvent.setAlertsSummary(summary);
      if (event.getHost() != null) {
        MaintenanceState maintenanceState = event.getMaintenanceState();
        hostUpdateEvent.setMaintenanceState(maintenanceState);

        STOMPUpdatePublisher.publish(HostUpdateEvent.createHostMaintenanceStatusUpdate(hostUpdateEvent.getClusterName(),
            hostName, maintenanceState, summary));
      } else {
        STOMPUpdatePublisher.publish(HostUpdateEvent.createHostAlertsUpdate(hostUpdateEvent.getClusterName(),
            hostName, summary));
      }
      stackAdvisorHelperProvider.get().clearCaches(hostName);
    } else if (event.getService()!= null) {
      String serviceName = event.getService().getName();
      for (String hostName : m_clusters.get().getCluster(clusterId).getService(serviceName).getServiceHosts()) {
        HostUpdateEvent hostUpdateEvent = retrieveHostUpdateFromCache(clusterId, hostName);

        AlertSummaryDTO summary = alertsDAO.findCurrentCounts(clusterId, null, hostName);

        if (hostUpdateEvent.getAlertsSummary().equals(summary)) {
          continue;
        }
        hostUpdateEvent.setAlertsSummary(summary);

        STOMPUpdatePublisher.publish(HostUpdateEvent.createHostAlertsUpdate(hostUpdateEvent.getClusterName(),
            hostName, summary));
        stackAdvisorHelperProvider.get().clearCaches(hostName);
      }
    }
  }

  private HostUpdateEvent retrieveHostUpdateFromCache(Long clusterId, String hostName) throws AmbariException {
    HostUpdateEvent hostUpdateEvent;
    if (hosts.containsKey(clusterId) && hosts.get(clusterId).containsKey(hostName)) {
      hostUpdateEvent = hosts.get(clusterId).get(hostName);
    } else {
      hostUpdateEvent = createHostUpdateEvent(clusterId, hostName);
      if (!hosts.containsKey(clusterId)) {
        hosts.put(clusterId, new HashMap<>());
      }
      hosts.get(clusterId).put(hostName, hostUpdateEvent);
    }

    return hostUpdateEvent;
  }

  private HostUpdateEvent createHostUpdateEvent(Long clusterId, String hostName) throws AmbariException {
    String clusterName = m_clusters.get().getClusterById(clusterId).getClusterName();
    Host host = m_clusters.get().getHost(hostName);

    AlertSummaryDTO summary = alertsDAO.findCurrentCounts(clusterId, null, hostName);

    return new HostUpdateEvent(clusterName, hostName, host.getStatus(), host.getState(),  host.getLastHeartbeatTime(),
        host.getMaintenanceState(clusterId), summary);
  }
}
