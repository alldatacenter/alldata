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

import java.util.List;

import org.apache.ambari.server.controller.internal.HostResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public class HostRequest implements ApiModel {

  private String hostname;
  private String publicHostname;
  private String clusterName; // CREATE/UPDATE
  private String rackInfo;
  private List<ConfigurationRequest> desiredConfigs; // UPDATE
  private String maintenanceState; // UPDATE
  private String blueprint;
  private String hostGroup;

  public HostRequest(String hostname, String clusterName) {
    this.hostname = hostname;
    this.clusterName = clusterName;
  }

  @ApiModelProperty(name = HostResourceProvider.HOST_NAME_PROPERTY_ID)
  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @ApiModelProperty(hidden = true)
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @ApiModelProperty(name = HostResourceProvider.RACK_INFO_PROPERTY_ID)
  public String getRackInfo() {
    return rackInfo;
  }
  
  public void setRackInfo(String info) {
    rackInfo = info;
  }

  @ApiModelProperty(name = HostResourceProvider.PUBLIC_NAME_PROPERTY_ID)
  public String getPublicHostName() {
    return publicHostname;
  }
  
  public void setPublicHostName(String name) {
    publicHostname = name;
  }
  
  public void setDesiredConfigs(List<ConfigurationRequest> request) {
    desiredConfigs = request;
  }

  @ApiModelProperty(name = HostResourceProvider.DESIRED_CONFIGS_PROPERTY_ID)
  public List<ConfigurationRequest> getDesiredConfigs() {
    return desiredConfigs;
  }

  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  @ApiModelProperty(name = HostResourceProvider.MAINTENANCE_STATE_PROPERTY_ID)
  public String getMaintenanceState() {
    return maintenanceState;
  }

  public void setBlueprintName(String blueprintName) {
    blueprint = blueprintName;
  }

  @ApiModelProperty(name = HostResourceProvider.BLUEPRINT_PROPERTY_ID)
  public String getBlueprintName() {
    return blueprint;
  }

  public void setHostGroupName(String hostGroupName) {
    hostGroup = hostGroupName;
  }

  @ApiModelProperty(name = HostResourceProvider.HOST_GROUP_PROPERTY_ID)
  public String getHostGroupName() {
    return hostGroup;
  }

  @Override
  public String toString() {
    return "{ hostname=" + hostname + ", clusterName=" + clusterName + " }";
  }
}
