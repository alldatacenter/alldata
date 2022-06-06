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

import java.util.Objects;
import java.util.SortedMap;

import org.apache.ambari.server.agent.stomp.dto.ClusterConfigs;
import org.apache.ambari.server.agent.stomp.dto.Hashable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about configs update for one host. This update will be sent to single host only.
 * Host can be identified by AgentConfigsUpdateEvent#hostName.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentConfigsUpdateEvent extends STOMPHostEvent implements Hashable {

  /**
   * Actual version hash.
   */
  private String hash;

  private Long timestamp;

  /**
   * Host identifier.
   */
  private final Long hostId;

  /**
   * Configs grouped by cluster id as keys.
   */
  @JsonProperty("clusters")
  private final SortedMap<String, ClusterConfigs> clustersConfigs;

  public AgentConfigsUpdateEvent(Long hostId, SortedMap<String, ClusterConfigs> clustersConfigs) {
    super(Type.AGENT_CONFIGS);
    this.hostId = hostId;
    this.clustersConfigs = clustersConfigs;
    this.timestamp = System.currentTimeMillis();
  }

  @Override
  public String getHash() {
    return hash;
  }

  @Override
  public void setHash(String hash) {
    this.hash = hash;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  public SortedMap<String, ClusterConfigs> getClustersConfigs() {
    return clustersConfigs;
  }

  public static AgentConfigsUpdateEvent emptyUpdate() {
    return new AgentConfigsUpdateEvent(null, null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AgentConfigsUpdateEvent that = (AgentConfigsUpdateEvent) o;

    return Objects.equals(hostId, that.hostId) &&
      Objects.equals(clustersConfigs, that.clustersConfigs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostId, clustersConfigs);
  }
}
