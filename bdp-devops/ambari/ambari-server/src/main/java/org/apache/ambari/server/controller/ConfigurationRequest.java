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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.internal.ConfigurationResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * This class encapsulates a configuration update request.
 * The configuration properties are grouped at service level. It is assumed that
 * different components of a service don't overload same property name.
 */
public class ConfigurationRequest {

  private String clusterName;
  private String type;
  private String tag;
  private Long version;
  private String serviceConfigVersionNote;
  private Map<String, String> configs;
  private boolean selected = true;
  private Map<String, Map<String, String>> configsAttributes;
  private boolean includeProperties;

  public ConfigurationRequest() {
    configs = new HashMap<>();
    configsAttributes = new HashMap<>();
  }
  
  public ConfigurationRequest(String clusterName,
                              String type,
                              String tag,
                              Map<String, String> configs,
                              Map<String, Map<String, String>> configsAttributes) {

    this.clusterName = clusterName;
    this.configs = configs;
    this.type = type;
    this.tag = tag;
    this.configsAttributes = configsAttributes;
    this.includeProperties = (type != null && tag != null);
  }

  /**
   * @return the type
   */
  @ApiModelProperty(name = ConfigurationResourceProvider.TYPE_PROPERTY_ID)
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the versionTag
   */
  @ApiModelProperty(name = ConfigurationResourceProvider.TAG_PROPERTY_ID)
  public String getVersionTag() {
    return tag;
  }

  /**
   * @param versionTag the versionTag to set
   */
  public void setVersionTag(String versionTag) {
    this.tag = versionTag;
  }

  /**
   * @return the configs
   */
  @ApiModelProperty(name = ConfigurationResourceProvider.PROPERTIES_PROPERTY_ID)
  public Map<String, String> getProperties() {
    return configs;
  }

  /**
   * @param configs the configs to set
   */
  public void setProperties(Map<String, String> configs) {
    this.configs = configs;
  }

  /**
   * @return the clusterName
   */
  @ApiModelProperty(name = ConfigurationResourceProvider.CLUSTER_NAME)
  public String getClusterName() {
    return clusterName;
  }


  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Sets if the configuration is selected
   * @param selected <code>true</code> if the configuration is selected.
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }
  
  /**
   * Gets if the configuration is to be selected.
   * @return <code>true</code> if the configuration is selected.
   */
  @ApiModelProperty(hidden = true)
  public boolean isSelected() {
    return selected;
  }

  /**
   * Set whether properties should be included.
   *
   * @param includeProperties whether properties should be included
   */
  public void setIncludeProperties(boolean includeProperties) {
    this.includeProperties = includeProperties;
  }

  /**
   * Determine whether properties should be included.
   *
   * @return  true if properties should be included; false otherwise
   */
  public boolean includeProperties()  {
    return this.includeProperties;
  }

  /**
   * @return Attributes of configs
   */
  @ApiModelProperty(name = ConfigurationResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID)
  public Map<String, Map<String, String>> getPropertiesAttributes() {
    return configsAttributes;
  }

  public void setPropertiesAttributes(
      Map<String, Map<String, String>> configsAttributes) {
    this.configsAttributes = configsAttributes;
  }

  @ApiModelProperty(name = ConfigurationResourceProvider.VERSION_PROPERTY_ID)
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @ApiModelProperty(hidden = true)
  public String getServiceConfigVersionNote() {
    return serviceConfigVersionNote;
  }

  public void setServiceConfigVersionNote(String serviceConfigVersionNote) {
    this.serviceConfigVersionNote = serviceConfigVersionNote;
  }
}
