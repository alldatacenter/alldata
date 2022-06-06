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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.ambari.server.orm.entities.AlertGroupEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertGroupUpdate {

  @JsonProperty("cluster_id")
  private Long clusterId;

  @JsonProperty("default")
  private Boolean defaultGroup;

  @JsonProperty("definitions")
  private List<Long> definitions;

  @JsonProperty("id")
  private Long id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("service_name")
  private String serviceName;

  @JsonProperty("targets")
  private List<Long> targets;

  public AlertGroupUpdate(AlertGroupEntity alertGroupEntity) {
    this.clusterId = alertGroupEntity.getClusterId();
    this.defaultGroup = alertGroupEntity.isDefault();
    this.definitions = alertGroupEntity.getAlertDefinitions().stream().map((al) -> al.getDefinitionId())
        .collect(Collectors.toList());
    this.id = alertGroupEntity.getGroupId();
    this.name = alertGroupEntity.getGroupName();
    this.serviceName = alertGroupEntity.getServiceName();
    this.targets = alertGroupEntity.getAlertTargets().stream().map((at) -> at.getTargetId())
        .collect(Collectors.toList());
  }

  public AlertGroupUpdate(Long id) {
    this.id = id;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public Boolean getDefaultGroup() {
    return defaultGroup;
  }

  public void setDefaultGroup(Boolean defaultGroup) {
    this.defaultGroup = defaultGroup;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Long> getDefinitions() {
    return definitions;
  }

  public void setDefinitions(List<Long> definitions) {
    this.definitions = definitions;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public List<Long> getTargets() {
    return targets;
  }

  public void setTargets(List<Long> targets) {
    this.targets = targets;
  }
}
