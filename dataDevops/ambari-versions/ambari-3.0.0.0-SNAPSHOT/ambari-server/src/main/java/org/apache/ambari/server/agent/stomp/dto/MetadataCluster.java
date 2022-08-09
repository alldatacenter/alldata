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

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.state.SecurityType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetadataCluster {
  private final Lock lock = new ReentrantLock();

  @JsonProperty("status_commands_to_run")
  private final Set<String> statusCommandsToRun;
  private final boolean fullServiceLevelMetadata; //this is true in case serviceLevelParams has all parameters for all services
  private SortedMap<String, MetadataServiceInfo> serviceLevelParams;
  private SortedMap<String, String> clusterLevelParams;
  private SortedMap<String, SortedMap<String,String>> agentConfigs;

  public MetadataCluster(SecurityType securityType, SortedMap<String,MetadataServiceInfo> serviceLevelParams, boolean fullServiceLevelMetadata,
                         SortedMap<String, String> clusterLevelParams, SortedMap<String, SortedMap<String,String>> agentConfigs) {
    this.statusCommandsToRun  = new HashSet<>();
    if (securityType != null) {
      this.statusCommandsToRun.add("STATUS");
    }
    this.fullServiceLevelMetadata = fullServiceLevelMetadata;
    this.serviceLevelParams = serviceLevelParams;
    this.clusterLevelParams = clusterLevelParams;
    this.agentConfigs = agentConfigs;
  }

  public static MetadataCluster emptyMetadataCluster() {
    return new MetadataCluster(null, null, false, null, null);
  }

  public static MetadataCluster serviceLevelParamsMetadataCluster(SecurityType securityType, SortedMap<String, MetadataServiceInfo> serviceLevelParams,
      boolean fullServiceLevelMetadata) {
    return new MetadataCluster(securityType, serviceLevelParams, fullServiceLevelMetadata, null, null);
  }

  public static MetadataCluster clusterLevelParamsMetadataCluster(SecurityType securityType, SortedMap<String, String> clusterLevelParams) {
    return new MetadataCluster(securityType, null, false, clusterLevelParams, null);
  }

  public Set<String> getStatusCommandsToRun() {
    return statusCommandsToRun;
  }

  public SortedMap<String, MetadataServiceInfo> getServiceLevelParams() {
    return serviceLevelParams;
  }

  public SortedMap<String, String> getClusterLevelParams() {
    return clusterLevelParams;
  }

  public SortedMap<String, SortedMap<String, String>> getAgentConfigs() {
    return agentConfigs;
  }

  public boolean isFullServiceLevelMetadata() {
    return fullServiceLevelMetadata;
  }

  public boolean updateServiceLevelParams(SortedMap<String, MetadataServiceInfo> update, boolean fullMetadataInUpdatedMap) {
    if (update != null) {
      try {
        lock.lock();
        if (this.serviceLevelParams == null) {
          this.serviceLevelParams = new TreeMap<>();
        }
        return updateMapIfNeeded(this.serviceLevelParams, update, fullMetadataInUpdatedMap);
      } finally {
        lock.unlock();
      }
    }

    return false;
  }

  public boolean updateClusterLevelParams(SortedMap<String, String> update) {
    if (update != null) {
      try {
        lock.lock();
        if (this.clusterLevelParams == null) {
          this.clusterLevelParams = new TreeMap<>();
        }
        return updateMapIfNeeded(this.clusterLevelParams, update, true);
      } finally {
        lock.unlock();
      }
    }

    return false;
  }

  private <T> boolean updateMapIfNeeded(Map<String, T> currentMap, Map<String, T> updatedMap, boolean fullMetadataInUpdatedMap) {
    boolean changed = false;
    if (fullMetadataInUpdatedMap) { // we have full metadata in updatedMap (i.e. in case of service removal we have full metadata in updatedMap)
      changed = !Objects.equals(currentMap, updatedMap);
      if (changed) {
        currentMap.clear();
        currentMap.putAll(updatedMap);
      }
    } else { // to support backward compatibility we fall back to previous version where we only added non-existing services/properties in current metadata
      for (String key : updatedMap.keySet()) {
        if (!currentMap.containsKey(key) || !currentMap.get(key).equals(updatedMap.get(key))) {
          currentMap.put(key, updatedMap.get(key));
          changed = true;
        }
      }
    }

    return changed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetadataCluster that = (MetadataCluster) o;

    return Objects.equals(statusCommandsToRun, that.statusCommandsToRun) &&
      Objects.equals(serviceLevelParams, that.serviceLevelParams) &&
      Objects.equals(clusterLevelParams, that.clusterLevelParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statusCommandsToRun, serviceLevelParams, clusterLevelParams);
  }
}
