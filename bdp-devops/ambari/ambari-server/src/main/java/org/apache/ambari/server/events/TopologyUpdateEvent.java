/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.events;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about clusters topology update. This update will be sent to all subscribed recipients.
 * Is used to messaging to UI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopologyUpdateEvent extends STOMPEvent implements Hashable {

  /**
   * Map of clusters topologies by cluster ids.
   */
  @JsonProperty("clusters")
  private final SortedMap<String, TopologyCluster> clusters;

  /**
   * Actual version hash.
   */
  private String hash;

  /**
   * Type of update, is used to differ full current topology (CREATE), adding new or update existing topology
   * elements (UPDATE) and removing existing topology elements (DELETE).
   */
  private final UpdateEventType eventType;

  public TopologyUpdateEvent(SortedMap<String, TopologyCluster> clusters, UpdateEventType eventType) {
    this(Type.UI_TOPOLOGY, clusters, null, eventType);
  }

  public TopologyUpdateEvent(Type type, SortedMap<String, TopologyCluster> clusters, String hash, UpdateEventType eventType) {
    super(type);
    this.clusters = clusters;
    this.hash = hash;
    this.eventType = eventType;
  }

  public SortedMap<String, TopologyCluster> getClusters() {
    return clusters;
  }

  public TopologyUpdateEvent deepCopy() {
    SortedMap<String, TopologyCluster> copiedClusters = new TreeMap<>();
    for (Map.Entry<String, TopologyCluster> topologyClusterEntry : getClusters().entrySet()) {
      copiedClusters.put(topologyClusterEntry.getKey(), topologyClusterEntry.getValue().deepCopyCluster());
    }
    TopologyUpdateEvent copiedEvent = new TopologyUpdateEvent(copiedClusters, getEventType());
    copiedEvent.setHash(getHash());
    return copiedEvent;
  }

  public UpdateEventType getEventType() {
    return eventType;
  }

  @Override
  public String getHash() {
    return hash;
  }

  @Override
  public void setHash(String hash) {
    this.hash = hash;
  }

  public static TopologyUpdateEvent emptyUpdate() {
    return new TopologyUpdateEvent(null, null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyUpdateEvent that = (TopologyUpdateEvent) o;

    return Objects.equals(eventType, that.eventType) &&
      Objects.equals(clusters, that.clusters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusters, eventType);
  }
}
