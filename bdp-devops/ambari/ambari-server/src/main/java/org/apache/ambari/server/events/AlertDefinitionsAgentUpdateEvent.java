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

package org.apache.ambari.server.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.ambari.server.agent.stomp.dto.AlertCluster;
import org.apache.ambari.server.agent.stomp.dto.Hashable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about alert definitions update. This update is specific to a single host.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDefinitionsAgentUpdateEvent extends STOMPHostEvent implements Hashable {

  private final Map<Long, AlertCluster> clusters;
  private final AlertDefinitionEventType eventType;
  private final String hostName;
  private final Long hostId;
  private String hash;

  public static AlertDefinitionsAgentUpdateEvent emptyEvent() {
    return new AlertDefinitionsAgentUpdateEvent(null, null, null, null);
  }

  public AlertDefinitionsAgentUpdateEvent(AlertDefinitionEventType eventType, Map<Long, AlertCluster> clusters, String hostName, Long hostId) {
    super(Type.ALERT_DEFINITIONS);
    this.eventType = eventType;
    this.clusters = clusters != null ? new HashMap<>(clusters) : null;
    this.hostName = hostName;
    this.hostId = hostId;
  }

  @Override
  public String getHash() {
    return hash;
  }

  @Override
  @JsonProperty("hash")
  public void setHash(String hash) {
    this.hash = hash;
  }

  @JsonProperty("hostName")
  public String getHostName() {
    return hostName;
  }

  @JsonProperty("eventType")
  public AlertDefinitionEventType getEventType() {
    return eventType;
  }

  @JsonProperty("clusters")
  public Map<Long, AlertCluster> getClusters() {
    return clusters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlertDefinitionsAgentUpdateEvent other = (AlertDefinitionsAgentUpdateEvent) o;

    return Objects.equals(eventType, other.eventType) &&
      Objects.equals(clusters, other.clusters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventType, clusters);
  }

  @Override
  public Long getHostId() {
    return hostId;
  }
}
