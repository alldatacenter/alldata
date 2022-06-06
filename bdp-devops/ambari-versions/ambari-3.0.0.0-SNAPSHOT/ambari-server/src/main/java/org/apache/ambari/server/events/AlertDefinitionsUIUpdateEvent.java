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

import org.apache.ambari.server.agent.stomp.dto.AlertCluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDefinitionsUIUpdateEvent extends STOMPEvent {

  private final Map<Long, AlertCluster> clusters;
  private final AlertDefinitionEventType eventType;

  protected AlertDefinitionsUIUpdateEvent(Type type, AlertDefinitionEventType eventType,
                                          Map<Long, AlertCluster> clusters) {
    super(type);
    this.eventType = eventType;
    this.clusters = clusters != null ? new HashMap<>(clusters) : null;
  }

  public AlertDefinitionsUIUpdateEvent(AlertDefinitionEventType eventType,
                                       Map<Long, AlertCluster> clusters) {
    this(Type.UI_ALERT_DEFINITIONS, eventType, clusters);
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

    AlertDefinitionsUIUpdateEvent that = (AlertDefinitionsUIUpdateEvent) o;

    if (clusters != null ? !clusters.equals(that.clusters) : that.clusters != null) return false;
    return eventType == that.eventType;
  }

  @Override
  public int hashCode() {
    int result = clusters != null ? clusters.hashCode() : 0;
    result = 31 * result + eventType.hashCode();
    return result;
  }
}
