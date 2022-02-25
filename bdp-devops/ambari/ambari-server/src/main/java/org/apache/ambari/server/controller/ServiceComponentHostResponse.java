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

import java.util.Map;

import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.UpgradeState;

import io.swagger.annotations.ApiModelProperty;

public class ServiceComponentHostResponse implements ApiModel {

  private String clusterName; // REF
  private String serviceName;
  private String componentName;
  private String displayName;
  private String publicHostname;
  private String hostname;
  // type -> desired config
  private Map<String, HostConfig> actualConfigs;
  private String liveState;
  private String version;
  private String desiredStackVersion;
  private String desiredRepositoryVersion;
  private String desiredState;
  private boolean staleConfig = false;
  private boolean reloadConfig = false;
  private String adminState = null;
  private String maintenanceState = null;
  private UpgradeState upgradeState = UpgradeState.NONE;

  public ServiceComponentHostResponse(String clusterName, String serviceName, String componentName,
      String displayName, String hostname, String publicHostname, String liveState, String version,
      String desiredState, String desiredStackVersion, String desiredRepositoryVersion,
      HostComponentAdminState adminState) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.displayName = displayName;
    this.hostname = hostname;
    this.publicHostname = publicHostname;
    this.liveState = liveState;
    this.version = version;
    this.desiredState = desiredState;
    this.desiredStackVersion = desiredStackVersion;
    this.desiredRepositoryVersion = desiredRepositoryVersion;
    if (adminState != null) {
      this.adminState = adminState.name();
    }
  }

  /**
   * @return the serviceName
   */
  @ApiModelProperty(name = HostComponentResourceProvider.SERVICE_NAME_PROPERTY_ID)
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName the serviceName to set
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return the componentName
   */
  @ApiModelProperty(name = HostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID)
  public String getComponentName() {
    return componentName;
  }

  /**
   * @param componentName the componentName to set
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @return the displayName
   */
  @ApiModelProperty(name = HostComponentResourceProvider.DISPLAY_NAME_PROPERTY_ID)
  public String getDisplayName() {
    return displayName;
  }

  /**
   * @return the hostname
   */
  @ApiModelProperty(name = HostComponentResourceProvider.HOST_NAME_PROPERTY_ID)
  public String getHostname() {
    return hostname;
  }

  /**
   * @param hostname the hostname to set
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * @return the public hostname
   */
  @ApiModelProperty(name = HostComponentResourceProvider.PUBLIC_HOST_NAME_PROPERTY_ID)
  public String getPublicHostname() {
    return publicHostname;
  }

  /**
   * @param publicHostname the public hostname to set
   */
  public void setPublicHostname(String publicHostname) {
    this.publicHostname = publicHostname;
  }

  /**
   * @return the liveState
   */
  @ApiModelProperty(name = HostComponentResourceProvider.STATE_PROPERTY_ID)
  public String getLiveState() {
    return liveState;
  }

  /**
   * @param liveState the liveState to set
   */
  public void setLiveState(String liveState) {
    this.liveState = liveState;
  }

  /**
   * @return the version
   */
  @ApiModelProperty(name = HostComponentResourceProvider.VERSION_PROPERTY_ID)
  public String getVersion() {
    return version;
  }

  /**
   * @return the desiredState
   */
  @ApiModelProperty(name = HostComponentResourceProvider.DESIRED_STATE_PROPERTY_ID)
  public String getDesiredState() {
    return desiredState;
  }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  /**
   * @return the desiredStackVersion
   */
  @ApiModelProperty(name = HostComponentResourceProvider.DESIRED_STACK_ID_PROPERTY_ID)
  public String getDesiredStackVersion() {
    return desiredStackVersion;
  }

  /**
   * @param desiredStackVersion the desiredStackVersion to set
   */
  public void setDesiredStackVersion(String desiredStackVersion) {
    this.desiredStackVersion = desiredStackVersion;
  }

  /**
   * Gets the desired repository of the component.
   *
   * @return the desired repository.
   */
  @ApiModelProperty(name = HostComponentResourceProvider.DESIRED_REPOSITORY_VERSION_PROPERTY_ID)
  public String getDesiredRepositoryVersion() {
    return desiredRepositoryVersion;
  }

  /**
   * @return the clusterName
   */

  @ApiModelProperty(name = HostComponentResourceProvider.CLUSTER_NAME_PROPERTY_ID)
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
   * @return the admin state of the host component
   */
  @ApiModelProperty(name = HostComponentResourceProvider.DESIRED_ADMIN_STATE_PROPERTY_ID, hidden = true)
  public String getAdminState() {
    return adminState;
  }

  /**
   * @param adminState of the host component
   */
  public void setAdminState(String adminState) {
    this.adminState = adminState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceComponentHostResponse that =
        (ServiceComponentHostResponse) o;

    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }
    if (serviceName != null ?
        !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }
    if (componentName != null ?
        !componentName.equals(that.componentName) : that.componentName != null) {
      return false;
    }
    if (hostname != null ?
        !hostname.equals(that.hostname) : that.hostname != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 71 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 71 * result + (hostname != null ? hostname.hashCode() : 0);
    return result;
  }

  /**
   * @return the actual configs
   */
  @ApiModelProperty(name = HostComponentResourceProvider.ACTUAL_CONFIGS_PROPERTY_ID)
  public Map<String, HostConfig> getActualConfigs() {
    return actualConfigs;
  }

  /**
   * @param configs the actual configs
   */
  public void setActualConfigs(Map<String, HostConfig> configs) {
    actualConfigs = configs;
  }

  /**
   * @return if the configs are stale
   */
  @ApiModelProperty(name = HostComponentResourceProvider.STALE_CONFIGS_PROPERTY_ID)
  public boolean isStaleConfig() {
    return staleConfig;
  }

  /**
   * @param stale
   */
  public void setStaleConfig(boolean stale) {
    staleConfig = stale;
  }

  /**
   * @return true if configs are reloadable without RESTART command
   */
  @ApiModelProperty(name = HostComponentResourceProvider.RELOAD_CONFIGS_PROPERTY_ID)
  public boolean isReloadConfig() {
    return reloadConfig;
  }

  /**
   * @param reloadConfig
   */
  public void setReloadConfig(boolean reloadConfig) {
    this.reloadConfig = reloadConfig;
  }

  /**
   * @return the maintenance state
   */
  @ApiModelProperty(name = HostComponentResourceProvider.MAINTENANCE_STATE_PROPERTY_ID)
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * @param state the maintenance state
   */
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  /**
   * @param state  the upgrade state
   */
  public void setUpgradeState(UpgradeState state) {
    upgradeState = state;
  }

  /**
   * @return the upgrade state
   */
  @ApiModelProperty(name = HostComponentResourceProvider.UPGRADE_STATE_PROPERTY_ID)
  public UpgradeState getUpgradeState() {
    return upgradeState;
  }

}
