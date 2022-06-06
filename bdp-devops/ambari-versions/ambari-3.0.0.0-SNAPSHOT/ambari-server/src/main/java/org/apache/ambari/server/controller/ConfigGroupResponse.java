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
package org.apache.ambari.server.controller;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ConfigGroupResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public class ConfigGroupResponse {
  private Long id;
  private String clusterName;
  private String groupName;
  private String tag;
  private String description;
  private Set<Map<String, Object>> hosts;
  private Set<Map<String, Object>> configVersions;
  private Set<Map<String, Object>> versionTags = new HashSet<>();

  public ConfigGroupResponse(Long id, String clusterName,
          String groupName, String tag, String description,
          Set<Map<String, Object>> hosts,
          Set<Map<String, Object>> configVersions) {
    this.id = id;
    this.clusterName = clusterName;
    this.groupName = groupName;
    this.tag = tag;
    this.description = description;
    this.hosts = hosts;
    this.configVersions = configVersions;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.ID_PROPERTY_ID)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.CLUSTER_NAME_PROPERTY_ID)
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.GROUP_NAME_PROPERTY_ID)
  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.TAG_PROPERTY_ID)
  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.DESCRIPTION_PROPERTY_ID)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.HOSTS_PROPERTY_ID)
  public Set<Map<String, Object>> getHosts() {
    return hosts;
  }

  public void setHosts(Set<Map<String, Object>> hosts) {
    this.hosts = hosts;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.DESIRED_CONFIGS_PROPERTY_ID)
  public Set<Map<String, Object>> getConfigurations() {
    return configVersions;
  }

  public void setConfigurations(Set<Map<String, Object>> configurations) {
    this.configVersions = configurations;
  }

  @ApiModelProperty(name = ConfigGroupResourceProvider.VERSION_TAGS_PROPERTY_ID)
  public Set<Map<String, Object>> getVersionTags() {
    return versionTags;
  }

  public void setVersionTags(Set<Map<String, Object>> versionTags) {
    this.versionTags = versionTags;
  }

  public interface ConfigGroupWrapper extends ApiModel {
    @ApiModelProperty(name = ConfigGroupResourceProvider.CONFIG_GROUP)
    ConfigGroupResponse getConfigGroup();
  }
}
