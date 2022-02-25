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
package org.apache.ambari.server.agent.stomp;

import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.agent.stomp.dto.MetadataCluster;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.events.AmbariPropertiesChangedEvent;
import org.apache.ambari.server.events.ClusterComponentsRepoChangedEvent;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.events.ServiceCredentialStoreUpdateEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class MetadataHolder extends AgentClusterDataHolder<MetadataUpdateEvent> {

  @Inject
  private AmbariManagementControllerImpl ambariManagementController;

  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  public MetadataHolder(AmbariEventPublisher ambariEventPublisher) {
    ambariEventPublisher.register(this);
  }

  @Override
  public MetadataUpdateEvent getCurrentData() throws AmbariException {
    return ambariManagementController.getClustersMetadata();
  }

  public MetadataUpdateEvent getDeleteMetadata(Long clusterId) throws AmbariException {
    TreeMap<String, MetadataCluster> clusterToRemove = new TreeMap<>();
    if (clusterId != null) {
      clusterToRemove.put(Long.toString(clusterId), MetadataCluster.emptyMetadataCluster());
    }
    MetadataUpdateEvent deleteEvent = new MetadataUpdateEvent(clusterToRemove, null , null,
        UpdateEventType.DELETE);
    return deleteEvent;
  }

  @Override
  protected boolean handleUpdate(MetadataUpdateEvent update) throws AmbariException {
    boolean changed = false;
    UpdateEventType eventType = update.getEventType();
    if (MapUtils.isNotEmpty(update.getMetadataClusters())) {
      for (Map.Entry<String, MetadataCluster> metadataClusterEntry : update.getMetadataClusters().entrySet()) {
        MetadataCluster updatedCluster = metadataClusterEntry.getValue();
        String clusterId = metadataClusterEntry.getKey();
        Map<String, MetadataCluster> clusters = getData().getMetadataClusters();
        if (clusters.containsKey(clusterId)) {
          if (eventType.equals(UpdateEventType.DELETE)) {
            getData().getMetadataClusters().remove(clusterId);
            changed = true;
          } else {
            MetadataCluster cluster = clusters.get(clusterId);
            if (cluster.updateClusterLevelParams(updatedCluster.getClusterLevelParams())) {
              changed = true;
            }
            if (cluster.updateServiceLevelParams(updatedCluster.getServiceLevelParams(), updatedCluster.isFullServiceLevelMetadata())) {
              changed = true;
            }
            if (CollectionUtils.isNotEmpty(updatedCluster.getStatusCommandsToRun())
                && !cluster.getStatusCommandsToRun().containsAll(updatedCluster.getStatusCommandsToRun())) {
              cluster.getStatusCommandsToRun().addAll(updatedCluster.getStatusCommandsToRun());
              changed = true;
            }
          }
        } else {
          if (eventType.equals(UpdateEventType.UPDATE)) {
            clusters.put(clusterId, updatedCluster);
            changed = true;
          } else {
            throw new ClusterNotFoundException(Long.parseLong(clusterId));
          }
        }
      }
    }
    return changed;
  }

  @Override
  protected MetadataUpdateEvent getEmptyData() {
    return MetadataUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onServiceCreate(ServiceInstalledEvent serviceInstalledEvent) throws AmbariException {
    Cluster cluster = m_clusters.get().getCluster(serviceInstalledEvent.getClusterId());
    updateData(ambariManagementController.getClusterMetadataOnServiceInstall(cluster, serviceInstalledEvent.getServiceName()));
  }

  @Subscribe
  public void onClusterComponentsRepoUpdate(ClusterComponentsRepoChangedEvent clusterComponentsRepoChangedEvent) throws AmbariException {
    Cluster cluster = m_clusters.get().getCluster(clusterComponentsRepoChangedEvent.getClusterId());
    updateData(ambariManagementController.getClusterMetadataOnRepoUpdate(cluster));
  }

  @Subscribe
  public void onServiceCredentialStoreUpdate(ServiceCredentialStoreUpdateEvent serviceCredentialStoreUpdateEvent) throws AmbariException {
    Cluster cluster = m_clusters.get().getCluster(serviceCredentialStoreUpdateEvent.getClusterId());
    updateData(ambariManagementController.getClusterMetadataOnServiceCredentialStoreUpdate(cluster, serviceCredentialStoreUpdateEvent.getServiceName()));
  }

  @Subscribe
  public void onAmbariPropertiesChange(AmbariPropertiesChangedEvent event) throws AmbariException {
    updateData(ambariManagementController.getClustersMetadata());
  }
}
