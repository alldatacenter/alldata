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
import java.util.SortedMap;

import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.agent.stomp.dto.MetadataCluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains update info about metadata for all clusters. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataUpdateEvent extends STOMPEvent implements Hashable {

  /**
   * Id used to send parameters common to all clusters.
   */
  private static final String AMBARI_LEVEL_CLUSTER_ID = "-1";

  /**
   * Actual version hash.
   */
  private String hash;

  private final UpdateEventType eventType;

  /**
   * Map of metadatas for each cluster by cluster ids.
   */
  @JsonProperty("clusters")
  private final SortedMap<String, MetadataCluster> metadataClusters;

  private MetadataUpdateEvent() {
    super(Type.METADATA);
    metadataClusters = null;
    eventType = null;
  }

  public MetadataUpdateEvent(SortedMap<String, MetadataCluster> metadataClusters,
                             SortedMap<String, String> ambariLevelParams,
                             SortedMap<String, SortedMap<String, String>> metadataAgentConfigs,
                             UpdateEventType eventType) {
    super(Type.METADATA);
    this.metadataClusters = metadataClusters;
    if (ambariLevelParams != null) {
      this.metadataClusters.put(AMBARI_LEVEL_CLUSTER_ID, new MetadataCluster(null, null, false, ambariLevelParams, metadataAgentConfigs));
    }
    this.eventType = eventType;
  }

  public Map<String, MetadataCluster> getMetadataClusters() {
    return metadataClusters;
  }

  @Override
  public String getHash() {
    return hash;
  }

  @Override
  public void setHash(String hash) {
    this.hash = hash;
  }

  public UpdateEventType getEventType() {
    return eventType;
  }

  public static MetadataUpdateEvent emptyUpdate() {
    return new MetadataUpdateEvent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetadataUpdateEvent that = (MetadataUpdateEvent) o;

    return metadataClusters != null ? metadataClusters.equals(that.metadataClusters) : that.metadataClusters == null;
  }

  @Override
  public int hashCode() {
    return metadataClusters != null ? metadataClusters.hashCode() : 0;
  }
}
