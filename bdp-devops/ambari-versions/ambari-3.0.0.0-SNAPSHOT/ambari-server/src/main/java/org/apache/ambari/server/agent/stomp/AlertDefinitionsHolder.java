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

import static org.apache.ambari.server.events.AlertDefinitionEventType.CREATE;
import static org.apache.ambari.server.events.AlertDefinitionEventType.DELETE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.AlertCluster;
import org.apache.ambari.server.events.AlertDefinitionEventType;
import org.apache.ambari.server.events.AlertDefinitionsAgentUpdateEvent;
import org.apache.ambari.server.events.HostsAddedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.listeners.alerts.AlertDefinitionsUIUpdateListener;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.alert.AlertHelper;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

@Singleton
public class AlertDefinitionsHolder extends AgentHostDataHolder<AlertDefinitionsAgentUpdateEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(AlertDefinitionsHolder.class);

  @Inject
  private Provider<AlertDefinitionHash> helper;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  private AlertDefinitionDAO alertDefinitionDAO;

  @Inject
  private AlertHelper alertHelper;

  @Inject
  private AlertDefinitionFactory alertDefinitionFactory;

  @Inject
  public AlertDefinitionsHolder(AmbariEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  @Override
  protected AlertDefinitionsAgentUpdateEvent getCurrentData(Long hostId) throws AmbariException {
    Map<Long, AlertCluster> result = new TreeMap<>();
    Map<Long, Map<Long, AlertDefinition>> alertDefinitions = helper.get().getAlertDefinitions(hostId);
    String hostName = clusters.get().getHostById(hostId).getHostName();
    long count = 0;
    for (Map.Entry<Long, Map<Long, AlertDefinition>> e : alertDefinitions.entrySet()) {
      Long clusterId = e.getKey();
      Map<Long, AlertDefinition> definitionMap = e.getValue();

      AlertDefinitionEntity ambariStaleAlert = alertDefinitionDAO.findByName(clusterId, AlertDefinitionsUIUpdateListener.AMBARI_STALE_ALERT_NAME);
      Integer staleIntervalMultiplier = alertHelper.getWaitFactorMultiplier(alertDefinitionFactory.coerce(ambariStaleAlert));

      result.put(clusterId, new AlertCluster(definitionMap, hostName, staleIntervalMultiplier));
      count += definitionMap.size();
    }
    LOG.info("Loaded {} alert definitions for {} clusters for host {}", count, result.size(), hostName);
    return new AlertDefinitionsAgentUpdateEvent(CREATE, result, hostName, hostId);
  }

  public AlertDefinitionsAgentUpdateEvent getDeleteCluster(Long clusterId, Long hostId) throws AmbariException {
    Map<Long, AlertCluster> result = new TreeMap<>();
    result.put(clusterId, AlertCluster.emptyAlertCluster());
    return new AlertDefinitionsAgentUpdateEvent(DELETE, result, null, hostId);
  }

  @Override
  protected AlertDefinitionsAgentUpdateEvent getEmptyData() {
    return AlertDefinitionsAgentUpdateEvent.emptyEvent();
  }

  @Override
  protected AlertDefinitionsAgentUpdateEvent handleUpdate(AlertDefinitionsAgentUpdateEvent current, AlertDefinitionsAgentUpdateEvent update) throws AmbariException {
    Map<Long, AlertCluster> updateClusters = update.getClusters();
    if (updateClusters.isEmpty()) {
      return null;
    }
    AlertDefinitionsAgentUpdateEvent result = null;

    Long hostId = update.getHostId();
    boolean changed = false;
    Map<Long, AlertCluster> existingClusters = current.getClusters();
    Map<Long, AlertCluster> mergedClusters = new HashMap<>();

    switch (update.getEventType()) {
      case UPDATE:
      case DELETE:
        if (!existingClusters.keySet().containsAll(updateClusters.keySet())) {
          LOG.info("Unknown clusters in update, perhaps cluster was removed previously");
        }
        for (Map.Entry<Long, AlertCluster> e : existingClusters.entrySet()) {
          Long clusterId = e.getKey();
          if (!updateClusters.containsKey(clusterId)) {
            mergedClusters.put(clusterId, e.getValue());
          }
        }
        for (Map.Entry<Long, AlertCluster> e : updateClusters.entrySet()) {
          Long clusterId = e.getKey();
          if (existingClusters.containsKey(clusterId)) {
            if (update.getEventType().equals(DELETE) && CollectionUtils.isEmpty(e.getValue().getAlertDefinitions())) {
              changed = true;
            } else {
              AlertCluster mergedCluster = existingClusters.get(e.getKey()).handleUpdate(update.getEventType(), e.getValue());
              if (mergedCluster != null) {
                mergedClusters.put(clusterId, mergedCluster);
                changed = true;
              }
            }
          } else {
            mergedClusters.put(clusterId, e.getValue());
            changed = true;
          }
        }
        LOG.debug("Handled {} of alerts for {} cluster(s) on host with id {}, changed = {}", update.getEventType(), updateClusters.size(), hostId, changed);
        break;
      case CREATE:
        if (!Sets.intersection(existingClusters.keySet(), updateClusters.keySet()).isEmpty()) {
          throw new AmbariException("Existing clusters in create");
        }
        mergedClusters.putAll(existingClusters);
        mergedClusters.putAll(updateClusters);
        LOG.debug("Handled {} of alerts for {} cluster(s)", update.getEventType(), updateClusters.size());
        changed = true;
        break;
      default:
        LOG.warn("Unhandled event type {}", update.getEventType());
        break;
    }
    if (changed) {
      result = new AlertDefinitionsAgentUpdateEvent(CREATE, mergedClusters, current.getHostName(), hostId);
    }
    return result;
  }

  @Subscribe
  public void onHostToClusterAssign(HostsAddedEvent hostsAddedEvent) throws AmbariException {
    Long clusterId = hostsAddedEvent.getClusterId();
    for (String hostName : hostsAddedEvent.getHostNames()) {
      Long hostId = clusters.get().getHost(hostName).getHostId();
      Map<Long, AlertCluster> existingClusters = getData(hostId).getClusters();
      if (!existingClusters.containsKey(clusterId)) {
        existingClusters.put(clusterId, new AlertCluster(new HashMap<>(), hostName));
      }
    }
  }

  @Subscribe
  public void onHostsRemoved(HostsRemovedEvent event) {
    for (Long hostId : event.getHostIds()) {
      onHostRemoved(hostId);
    }
  }

  private void safelyUpdateData(AlertDefinitionsAgentUpdateEvent event) {
    try {
      updateData(event);
    } catch (AmbariException e) {
      LOG.warn(String.format("Failed to %s alert definitions for host %s", event.getEventType(), event.getHostName()), e);
    }
  }

  private void safelyResetData(Long hostId) {
    try {
      resetData(hostId);
    } catch (AmbariException e) {
      LOG.warn(String.format("Failed to reset alert definitions for host with id %s", hostId), e);
    }
  }

  public void provideAlertDefinitionAgentUpdateEvent(AlertDefinitionEventType eventType, Long clusterId,
                                                     Map<Long, AlertDefinition> alertDefinitions, String hostName) throws AmbariException {
    Long hostId = clusters.get().getHost(hostName).getHostId();
    Map<Long, AlertCluster> update = Collections.singletonMap(clusterId, new AlertCluster(alertDefinitions, hostName));
    AlertDefinitionsAgentUpdateEvent event = new AlertDefinitionsAgentUpdateEvent(eventType, update, hostName, hostId);
    safelyUpdateData(event);
  }

  public void provideStaleAlertDefinitionUpdateEvent(AlertDefinitionEventType eventType, Long clusterId,
                                                     Integer staleIntervalMultiplier, String hostName) throws AmbariException {
    Long hostId = clusters.get().getHost(hostName).getHostId();
    Map<Long, AlertCluster> update = Collections.singletonMap(clusterId, new AlertCluster(Collections.emptyMap(), hostName, staleIntervalMultiplier));
    AlertDefinitionsAgentUpdateEvent event = new AlertDefinitionsAgentUpdateEvent(eventType, update, hostName, hostId);
    safelyUpdateData(event);
  }
}
