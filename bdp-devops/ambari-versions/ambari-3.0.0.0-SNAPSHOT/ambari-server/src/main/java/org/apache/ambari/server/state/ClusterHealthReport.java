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

package org.apache.ambari.server.state;

import org.codehaus.jackson.annotate.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Cluster Health Report (part of Clusters API response)
 */
public class ClusterHealthReport {

  private static final String HOST_STALE_CONFIG = "Host/stale_config";
  private static final String HOST_MAINTENANCE_STATE = "Host/maintenance_state";
  private static final String HOST_HOST_STATE_HEALTHY = "Host/host_state/HEALTHY";
  private static final String HOST_HOST_STATE_UNHEALTHY = "Host/host_state/UNHEALTHY";
  private static final String HOST_HOST_STATE_INIT = "Host/host_state/INIT";
  private static final String HOST_HOST_STATUS_HEALTHY = "Host/host_status/HEALTHY";
  private static final String HOST_HOST_STATUS_UNHEALTHY = "Host/host_status/UNHEALTHY";
  private static final String HOST_HOST_STATUS_UNKNOWN = "Host/host_status/UNKNOWN";
  private static final String HOST_HOST_STATUS_ALERT = "Host/host_status/ALERT";
  private static final String HOST_HOST_STATE_HEARTBEAT_LOST = "Host/host_state/HEARTBEAT_LOST";

  private int staleConfigsHosts;
  private int maintenanceStateHosts;

  private int healthyStateHosts;
  private int unhealthyStateHosts;
  private int heartbeatLostStateHosts;
  private int initStateHosts;

  private int healthyStatusHosts;
  private int unhealthyStatusHosts;
  private int unknownStatusHosts;
  private int alertStatusHosts;

  /**
   * @return number of hosts having stale_config set to true
   */
  @JsonProperty(HOST_STALE_CONFIG)
  @ApiModelProperty(name = HOST_STALE_CONFIG)
  public int getStaleConfigsHosts() {
    return staleConfigsHosts;
  }

  /**
   * @param staleConfigsHosts number of hosts having stale_config set to true
   */
  public void setStaleConfigsHosts(int staleConfigsHosts) {
    this.staleConfigsHosts = staleConfigsHosts;
  }

  /**
   * @return number of hosts having maintenance state on
   */
  @JsonProperty(HOST_MAINTENANCE_STATE)
  @ApiModelProperty(name = HOST_MAINTENANCE_STATE)
  public int getMaintenanceStateHosts() {
    return maintenanceStateHosts;
  }

  /**
   * @param maintenanceStateHosts number of hosts having maintenance state on
   */
  public void setMaintenanceStateHosts(int maintenanceStateHosts) {
    this.maintenanceStateHosts = maintenanceStateHosts;
  }

  /**
   * @return number of hosts having host state HEALTHY
   */
  @JsonProperty(HOST_HOST_STATE_HEALTHY)
  @ApiModelProperty(name = HOST_HOST_STATE_HEALTHY)
  public int getHealthyStateHosts() {
    return healthyStateHosts;
  }

  /**
   * @param healthyStateHosts number of hosts having host state HEALTHY
   */
  public void setHealthyStateHosts(int healthyStateHosts) {
    this.healthyStateHosts = healthyStateHosts;
  }

  /**
   * @return number of hosts having host state UNHEALTHY
   */
  @JsonProperty(HOST_HOST_STATE_UNHEALTHY)
  @ApiModelProperty(name = HOST_HOST_STATE_UNHEALTHY)
  public int getUnhealthyStateHosts() {
    return unhealthyStateHosts;
  }

  /**
   * @param unhealthyStateHosts number of hosts having host state UNHEALTHY
   */
  public void setUnhealthyStateHosts(int unhealthyStateHosts) {
    this.unhealthyStateHosts = unhealthyStateHosts;
  }

  /**
   * @return number of hosts having host state INIT
   */
  @JsonProperty(HOST_HOST_STATE_INIT)
  @ApiModelProperty(name = HOST_HOST_STATE_INIT)
  public int getInitStateHosts() {
    return initStateHosts;
  }

  /**
   * @param initStateHosts number of hosts having host state INIT
   */
  public void setInitStateHosts(int initStateHosts) {
    this.initStateHosts = initStateHosts;
  }

  /**
   * @return number of hosts having host status HEALTHY
   */
  @JsonProperty(HOST_HOST_STATUS_HEALTHY)
  @ApiModelProperty(name = HOST_HOST_STATUS_HEALTHY)
  public int getHealthyStatusHosts() {
    return healthyStatusHosts;
  }

  /**
   * @param healthyStatusHosts number of hosts having host status HEALTHY
   */
  public void setHealthyStatusHosts(int healthyStatusHosts) {
    this.healthyStatusHosts = healthyStatusHosts;
  }

  /**
   * @return number of hosts having host status UNHEALTHY
   */
  @JsonProperty(HOST_HOST_STATUS_UNHEALTHY)
  @ApiModelProperty(name = HOST_HOST_STATUS_UNHEALTHY)
  public int getUnhealthyStatusHosts() {
    return unhealthyStatusHosts;
  }

  /**
   * @param unhealthyStatusHosts number of hosts having host status UNHEALTHY
   */
  public void setUnhealthyStatusHosts(int unhealthyStatusHosts) {
    this.unhealthyStatusHosts = unhealthyStatusHosts;
  }

  /**
   * @return number of hosts having host status UNKNOWN
   */
  @JsonProperty(HOST_HOST_STATUS_UNKNOWN)
  @ApiModelProperty(name = HOST_HOST_STATUS_UNKNOWN)
  public int getUnknownStatusHosts() {
    return unknownStatusHosts;
  }

  /**
   * @param unknownStatusHosts number of hosts having host status UNKNOWN
   */
  public void setUnknownStatusHosts(int unknownStatusHosts) {
    this.unknownStatusHosts = unknownStatusHosts;
  }

  /**
   * @return number of hosts having host status ALERT
   */
  @JsonProperty(HOST_HOST_STATUS_ALERT)
  @ApiModelProperty(name = HOST_HOST_STATUS_ALERT)
  public int getAlertStatusHosts() {
    return alertStatusHosts;
  }

  /**
   * @param alertStatusHosts number of hosts having host status ALERT
   */
  public void setAlertStatusHosts(int alertStatusHosts) {
    this.alertStatusHosts = alertStatusHosts;
  }

  /**
   * @return number of hosts having host status HEARTBEAT_LOST
   */
  @JsonProperty(HOST_HOST_STATE_HEARTBEAT_LOST)
  @ApiModelProperty(name = HOST_HOST_STATE_HEARTBEAT_LOST)
  public int getHeartbeatLostStateHosts() {
    return heartbeatLostStateHosts;
  }

  /**
   * @param heartbeatLostStateHosts number of hosts
   *                                having host status HEARTBEAT_LOST
   */
  public void setHeartbeatLostStateHosts(int heartbeatLostStateHosts) {
    this.heartbeatLostStateHosts = heartbeatLostStateHosts;
  }

  public ClusterHealthReport() {

  }

  @Override
  public String toString() {
    return "ClusterHealthReport{" +
      "staleConfigsHosts=" + staleConfigsHosts +
      ", maintenanceStateHosts=" + maintenanceStateHosts +
      ", healthyStateHosts=" + healthyStateHosts +
      ", unhealthyStateHosts=" + unhealthyStateHosts +
      ", heartbeatLostStateHosts=" + heartbeatLostStateHosts +
      ", initStateHosts=" + initStateHosts +
      ", healthyStatusHosts=" + healthyStatusHosts +
      ", unhealthyStatusHosts=" + unhealthyStatusHosts +
      ", unknownStatusHosts=" + unknownStatusHosts +
      ", alertStatusHosts=" + alertStatusHosts +
      '}';
  }
}





