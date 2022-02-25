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
package org.apache.ambari.server.agent.stomp.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.events.AlertDefinitionEventType;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertCluster {

  private static final Logger LOG = LoggerFactory.getLogger(AlertCluster.class);

  private final Map<Long, AlertDefinition> alertDefinitions;
  private final String hostName;
  private Integer staleIntervalMultiplier;

  public AlertCluster(AlertDefinition alertDefinition, String hostName, Integer staleIntervalMultiplier) {
    this(Collections.singletonMap(alertDefinition.getDefinitionId(), alertDefinition), hostName, staleIntervalMultiplier);
  }

  public AlertCluster(Map<Long, AlertDefinition> alertDefinitions, String hostName, Integer staleIntervalMultiplier) {
    this.alertDefinitions = new HashMap<>(alertDefinitions);
    this.hostName = hostName;
    this.staleIntervalMultiplier = staleIntervalMultiplier;
  }

  public AlertCluster(AlertDefinition alertDefinition, String hostName) {
    this(Collections.singletonMap(alertDefinition.getDefinitionId(), alertDefinition), hostName, null);
  }

  public AlertCluster(Map<Long, AlertDefinition> alertDefinitions, String hostName) {
    this.alertDefinitions = new HashMap<>(alertDefinitions);
    this.hostName = hostName;
    this.staleIntervalMultiplier = null;
  }

  private AlertCluster() {
    alertDefinitions = null;
    hostName = null;
  }

  @JsonProperty("staleIntervalMultiplier")
  public Integer getStaleIntervalMultiplier() {
    return staleIntervalMultiplier;
  }

  @JsonProperty("alertDefinitions")
  public Collection<AlertDefinition> getAlertDefinitions() {
    return alertDefinitions == null ? Collections.emptyList() : alertDefinitions.values();
  }

  @JsonProperty("hostName")
  public String getHostName() {
    return hostName;
  }

  public AlertCluster handleUpdate(AlertDefinitionEventType eventType, AlertCluster update) {
    boolean changed = false;

    AlertCluster mergedCluster = null;
    Map<Long, AlertDefinition> mergedDefinitions = new HashMap<>();
    Integer mergedStaleIntervalMultiplier = null;
    switch (eventType) {
      case CREATE:
        // FIXME should clear map first?
      case UPDATE:
        for (Map.Entry<Long, AlertDefinition> alertDefinitionEntry : alertDefinitions.entrySet()) {
          Long definitionId = alertDefinitionEntry.getKey();
          if (!update.alertDefinitions.containsKey(definitionId)) {
            mergedDefinitions.put(definitionId, alertDefinitionEntry.getValue());
          } else {
            AlertDefinition newDefinition = update.alertDefinitions.get(definitionId);
            AlertDefinition oldDefinition = alertDefinitionEntry.getValue();
            if (!oldDefinition.deeplyEquals(newDefinition)) {
              changed = true;
            }
            mergedDefinitions.put(definitionId, newDefinition);
          }
        }
        if (addNewAlertDefinitions(update, mergedDefinitions)) {
          changed = true;
        }
        if (update.getStaleIntervalMultiplier() != null
            && !update.getStaleIntervalMultiplier().equals(staleIntervalMultiplier)) {
          mergedStaleIntervalMultiplier = update.getStaleIntervalMultiplier();
          changed = true;
        } else {
          mergedStaleIntervalMultiplier = staleIntervalMultiplier;
        }
        LOG.debug("Handled {} of {} alerts, changed = {}", eventType, update.alertDefinitions.size(), changed);
        break;
      case DELETE:
        for (Map.Entry<Long, AlertDefinition> alertDefinitionEntry : alertDefinitions.entrySet()) {
          Long definitionId = alertDefinitionEntry.getKey();
          if (!update.alertDefinitions.containsKey(definitionId)) {
            mergedDefinitions.put(definitionId, alertDefinitionEntry.getValue());
          } else {
            changed = true;
          }
        }
        mergedStaleIntervalMultiplier = staleIntervalMultiplier;
        LOG.debug("Handled {} of {} alerts", eventType, update.alertDefinitions.size());
        break;
      default:
        LOG.warn("Unhandled event type {}", eventType);
        break;
    }
    if (changed) {
      mergedCluster = new AlertCluster(mergedDefinitions, hostName, mergedStaleIntervalMultiplier);
    }
    return mergedCluster;
  }

  /**
   * Add each alert definitions from the update event which are not included in mergedDefinitions
   * @return true if there was such an alert definition
   */
  private boolean addNewAlertDefinitions(AlertCluster update, Map<Long, AlertDefinition> mergedDefinitions) {
    boolean hasNew = false;
    for (Map.Entry<Long, AlertDefinition> each : update.alertDefinitions.entrySet()) {
      if (!mergedDefinitions.containsKey(each.getKey())) {
        mergedDefinitions.put(each.getKey(), each.getValue());
        hasNew = true;
      }
    }
    return hasNew;
  }

  public static AlertCluster emptyAlertCluster() {
    return new AlertCluster();
  }
}
