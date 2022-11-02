/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent.stomp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyComponent;
import org.apache.ambari.server.agent.stomp.dto.TopologyHost;
import org.apache.ambari.server.agent.stomp.dto.TopologyUpdateHandlingReport;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.events.ClusterComponentsRepoChangedEvent;
import org.apache.ambari.server.events.TopologyAgentUpdateEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TopologyHolder extends AgentClusterDataHolder<TopologyUpdateEvent> {

  private final static Logger LOG = LoggerFactory.getLogger(TopologyHolder.class);

  @Inject
  private AmbariManagementControllerImpl ambariManagementController;

  @Inject
  private Clusters clusters;

  @Inject
  private StackAdvisorHelper stackAdvisorHelper;

  @Inject
  public TopologyHolder(AmbariEventPublisher ambariEventPublisher) {
    ambariEventPublisher.register(this);
  }

  @Override
  public TopologyUpdateEvent getUpdateIfChanged(String agentHash) throws AmbariException {
    TopologyUpdateEvent topologyUpdateEvent = super.getUpdateIfChanged(agentHash);
    prepareAgentTopology(topologyUpdateEvent);
    return topologyUpdateEvent;
  }

  /**
   * Is used during agent registering to provide base info about clusters topology.
   * @return filled TopologyUpdateEvent with info about all components and hosts in all clusters
   */
  @Override
  public TopologyUpdateEvent getCurrentData() throws AmbariException {
    TreeMap<String, TopologyCluster> topologyClusters = new TreeMap<>();
    for (Cluster cl : clusters.getClusters().values()) {
      Collection<Host> clusterHosts = cl.getHosts();
      Set<TopologyComponent> topologyComponents = new HashSet<>();
      Set<TopologyHost> topologyHosts = new HashSet<>();
      for (Host host : clusterHosts) {
        topologyHosts.add(new TopologyHost(host.getHostId(), host.getHostName(),
            host.getRackInfo(), host.getIPv4()));
      }
      for (Service service : cl.getServices().values()) {
        for (ServiceComponent component : service.getServiceComponents().values()) {
          Map<String, ServiceComponentHost> componentsMap = component.getServiceComponentHosts();
          if (!componentsMap.isEmpty()) {

            //TODO will be a need to change to multi-instance usage
            ServiceComponentHost sch = componentsMap.entrySet().iterator().next().getValue();

            Set<String> hostNames = cl.getHosts(sch.getServiceName(), sch.getServiceComponentName());
            Set<Long> hostOrderIds = clusterHosts.stream()
              .filter(h -> hostNames.contains(h.getHostName()))
              .map(Host::getHostId)
              .collect(Collectors.toSet());
            Set<String> hostOrderNames = clusterHosts.stream()
              .filter(h -> hostNames.contains(h.getHostName()))
              .map(Host::getHostName)
              .collect(Collectors.toSet());
            String serviceName = sch.getServiceName();
            String componentName = sch.getServiceComponentName();

            TopologyComponent topologyComponent = TopologyComponent.newBuilder()
                .setComponentName(sch.getServiceComponentName())
                .setServiceName(sch.getServiceName())
                .setHostIdentifiers(hostOrderIds, hostOrderNames)
                .setComponentLevelParams(ambariManagementController.getTopologyComponentLevelParams(cl.getClusterId(), serviceName,
                    componentName, cl.getSecurityType()))
                .setCommandParams(ambariManagementController.getTopologyCommandParams(cl.getClusterId(), serviceName, componentName, sch))
                .build();
            topologyComponents.add(topologyComponent);
          }
        }
      }
      topologyClusters.put(Long.toString(cl.getClusterId()),
          new TopologyCluster(topologyComponents, topologyHosts));
    }
    return new TopologyUpdateEvent(topologyClusters, UpdateEventType.CREATE);
  }

  @Override
  public boolean updateData(TopologyUpdateEvent update) throws AmbariException {
    boolean changed = super.updateData(update);
    if (changed) {
      // it is not allowed to change existent update event before arriving to listener and converting to json
      // so it is better to create copy
      TopologyUpdateEvent copiedUpdate = update.deepCopy();
      TopologyAgentUpdateEvent topologyAgentUpdateEvent = new TopologyAgentUpdateEvent(copiedUpdate.getClusters(),
        copiedUpdate.getHash(),
        copiedUpdate.getEventType()
      );
      prepareAgentTopology(topologyAgentUpdateEvent);
      LOG.debug("Publishing Topology Agent Update Event hash={}", topologyAgentUpdateEvent.getHash());
      STOMPUpdatePublisher.publish(topologyAgentUpdateEvent);
    }

    return changed;
  }

  @Override
  protected boolean handleUpdate(TopologyUpdateEvent update) throws AmbariException {
    TopologyUpdateHandlingReport report = new TopologyUpdateHandlingReport();
    UpdateEventType eventType = update.getEventType();
    for (Map.Entry<String, TopologyCluster> updatedCluster : update.getClusters().entrySet()) {
      String clusterId = updatedCluster.getKey();
      TopologyCluster cluster = updatedCluster.getValue();
      if (getData().getClusters().containsKey(clusterId)) {
        if (eventType.equals(UpdateEventType.DELETE) &&
            CollectionUtils.isEmpty(cluster.getTopologyComponents()) &&
            CollectionUtils.isEmpty(cluster.getTopologyHosts())) {
          getData().getClusters().remove(clusterId);
          report.mappingWasChanged();
        } else {
          getData().getClusters().get(clusterId).update(
              update.getClusters().get(clusterId).getTopologyComponents(),
              update.getClusters().get(clusterId).getTopologyHosts(),
              eventType, report);
        }
      } else {
        if (eventType.equals(UpdateEventType.UPDATE)) {
          getData().getClusters().put(clusterId, cluster);
          report.mappingWasChanged();
        } else {
          throw new ClusterNotFoundException(Long.parseLong(clusterId));
        }
      }
    }
    stackAdvisorHelper.clearCaches(report.getUpdatedHostNames());
    return report.wasChanged();
  }

  private void prepareAgentTopology(TopologyUpdateEvent topologyUpdateEvent) {
    if (topologyUpdateEvent.getClusters() != null) {
      for (TopologyCluster topologyCluster : topologyUpdateEvent.getClusters().values()) {
        if (CollectionUtils.isNotEmpty(topologyCluster.getTopologyComponents())) {
          for (TopologyComponent topologyComponent : topologyCluster.getTopologyComponents()) {
            topologyComponent.setHostNames(new HashSet<>());
            topologyComponent.setPublicHostNames(new HashSet<>());
            topologyComponent.setLastComponentState(null);
          }
        }
        if (topologyUpdateEvent.getEventType().equals(UpdateEventType.DELETE)
            && CollectionUtils.isNotEmpty(topologyCluster.getTopologyHosts())) {
          for (TopologyHost topologyHost : topologyCluster.getTopologyHosts()) {
            topologyHost.setHostName(null);
          }
        }
      }
    }
  }

  @Override
  protected TopologyUpdateEvent getEmptyData() {
    return TopologyUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onClusterComponentsRepoUpdate(ClusterComponentsRepoChangedEvent clusterComponentsRepoChangedEvent) throws AmbariException {
    Long clusterId = clusterComponentsRepoChangedEvent.getClusterId();

    TopologyCluster topologyCluster = new TopologyCluster();
    topologyCluster.setTopologyComponents(getTopologyComponentRepos(clusterId));
    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();
    topologyUpdates.put(Long.toString(clusterId), topologyCluster);
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(topologyUpdates, UpdateEventType.UPDATE);
    updateData(topologyUpdateEvent);
  }

  private Set<TopologyComponent> getTopologyComponentRepos(Long clusterId) throws AmbariException {
    Set<TopologyComponent> topologyComponents = new HashSet<>();
    Cluster cl = clusters.getCluster(clusterId);
    for (Service service : cl.getServices().values()) {
      for (ServiceComponent component : service.getServiceComponents().values()) {
        Map<String, ServiceComponentHost> componentsMap = component.getServiceComponentHosts();
        if (!componentsMap.isEmpty()) {

          //TODO will be a need to change to multi-instance usage
          ServiceComponentHost sch = componentsMap.entrySet().iterator().next().getValue();

          String serviceName = sch.getServiceName();
          String componentName = sch.getServiceComponentName();

          TopologyComponent topologyComponent = TopologyComponent.newBuilder()
              .setComponentName(sch.getServiceComponentName())
              .setServiceName(sch.getServiceName())
              .setCommandParams(ambariManagementController.getTopologyCommandParams(cl.getClusterId(), serviceName, componentName, sch))
              .setComponentLevelParams(ambariManagementController.getTopologyComponentLevelParams(clusterId,
                  serviceName, componentName, cl.getSecurityType()))
              .build();
          topologyComponents.add(topologyComponent);
        }
      }
    }
    return topologyComponents;
  }
}
