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
package org.apache.ambari.server.topology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.AlertDefinitionsHolder;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyComponent;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.state.Clusters;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class STOMPComponentsDeleteHandler {

  @Inject
  private Provider<TopologyHolder> m_topologyHolder;

  @Inject
  private Provider<AgentConfigsHolder> agentConfigsHolder;

  @Inject
  private Provider<MetadataHolder> metadataHolder;

  @Inject
  private Provider<HostLevelParamsHolder> hostLevelParamsHolder;

  @Inject
  private Provider<AlertDefinitionsHolder> alertDefinitionsHolder;

  @Inject
  private Clusters clusters;

  public void processDeleteByMetaDataException(DeleteHostComponentStatusMetaData metaData) throws AmbariException {
    if (metaData.getAmbariException() != null) {
      processDeleteByMetaData(metaData);
      throw metaData.getAmbariException();
    }
  }
  public void processDeleteByMetaData(DeleteHostComponentStatusMetaData metaData) throws AmbariException {
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(
        createUpdateFromDeleteByMetaData(metaData),
        UpdateEventType.DELETE
    );
    m_topologyHolder.get().updateData(topologyUpdateEvent);

    // on components remove we should remove appropriate metadata/configs/hostlevelparams on agents
    updateNonTopologyAgentInfo(metaData.getRemovedHostComponents().stream().map(hc -> hc.getHostId()).collect(Collectors.toSet()),
        null);
  }

  public void processDeleteCluster(Long clusterId) throws AmbariException {
    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();
    topologyUpdates.put(Long.toString(clusterId), new TopologyCluster(null, null));
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(
        topologyUpdates,
        UpdateEventType.DELETE
    );
    m_topologyHolder.get().updateData(topologyUpdateEvent);

    // on cluster remove we should remove appropriate metadata/configs/hostlevelparams on agents
    updateNonTopologyAgentInfo(clusters.getCluster(clusterId).getHosts().stream().map(h -> h.getHostId()).collect(Collectors.toSet()),
        clusterId);
  }

  private void updateNonTopologyAgentInfo(Set<Long> changedHosts, Long clusterId) throws AmbariException {

    for (Long hostId : changedHosts) {
      if (clusterId != null) {
        alertDefinitionsHolder.get().updateData(alertDefinitionsHolder.get().getDeleteCluster(clusterId, hostId));
        agentConfigsHolder.get().updateData(agentConfigsHolder.get().getCurrentDataExcludeCluster(hostId, clusterId));
        hostLevelParamsHolder.get().updateData(hostLevelParamsHolder.get().getCurrentDataExcludeCluster(hostId, clusterId));
      } else {
        agentConfigsHolder.get().updateData(agentConfigsHolder.get().getCurrentData(hostId));
        hostLevelParamsHolder.get().updateData(hostLevelParamsHolder.get().getCurrentData(hostId));
      }
    }
    if (clusterId != null) {
      metadataHolder.get().updateData(metadataHolder.get().getDeleteMetadata(clusterId));
    } else {
      metadataHolder.get().updateData(metadataHolder.get().getCurrentData());
    }
  }

  public TreeMap<String, TopologyCluster> createUpdateFromDeleteByMetaData(DeleteHostComponentStatusMetaData metaData) {
    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();

    for (DeleteHostComponentStatusMetaData.HostComponent hostComponent : metaData.getRemovedHostComponents()) {
      TopologyComponent deletedComponent = TopologyComponent.newBuilder()
          .setComponentName(hostComponent.getComponentName())
          .setServiceName(hostComponent.getServiceName())
          .setVersion(hostComponent.getVersion())
          .setHostIdentifiers(new HashSet<>(Arrays.asList(hostComponent.getHostId())),
              new HashSet<>(Arrays.asList(hostComponent.getHostName())))
          .setLastComponentState(hostComponent.getLastComponentState())
          .build();

      String clusterId = hostComponent.getClusterId();
      if (!topologyUpdates.containsKey(clusterId)) {
        topologyUpdates.put(clusterId, new TopologyCluster());
      }

      if (!topologyUpdates.get(clusterId).getTopologyComponents().contains(deletedComponent)) {
        topologyUpdates.get(clusterId).addTopologyComponent(deletedComponent);
      } else {
        topologyUpdates.get(clusterId).getTopologyComponents()
            .stream().filter(t -> t.equals(deletedComponent))
            .forEach(t -> t.addHostName(hostComponent.getHostName()));
        topologyUpdates.get(clusterId).getTopologyComponents()
            .stream().filter(t -> t.equals(deletedComponent))
            .forEach(t -> t.addHostId(hostComponent.getHostId()));
      }
    }

    return topologyUpdates;
  }
}
