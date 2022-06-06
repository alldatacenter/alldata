/**
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

import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Host info with updated parameter. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostUpdateEvent extends STOMPEvent {

  @JsonProperty("cluster_name")
  private String clusterName;

  @JsonProperty("host_name")
  private String hostName;

  @JsonProperty("host_status")
  private String hostStatus;

  @JsonProperty("host_state")
  private HostState hostState;

  @JsonProperty("last_heartbeat_time")
  private Long lastHeartbeatTime;

  @JsonProperty("maintenance_state")
  private MaintenanceState maintenanceState;

  @JsonProperty("alerts_summary")
  private AlertSummaryDTO alertsSummary;

  public HostUpdateEvent(String clusterName, String hostName, String hostStatus, HostState hostState,
                         Long lastHeartbeatTime, MaintenanceState maintenanceState, AlertSummaryDTO alertsSummary) {
    super(Type.HOST);
    this.clusterName = clusterName;
    this.hostName = hostName;
    this.hostStatus = hostStatus;
    this.hostState = hostState;
    this.lastHeartbeatTime = lastHeartbeatTime;
    this.maintenanceState = maintenanceState;
    this.alertsSummary = alertsSummary;
  }

  public static HostUpdateEvent createHostStatusUpdate(String clusterName, String hostName, String hostStatus,
                                                       Long lastHeartbeatTime) {
    return new HostUpdateEvent(clusterName, hostName, hostStatus, null, lastHeartbeatTime, null, null);
  }

  public static HostUpdateEvent createHostStateUpdate(String clusterName, String hostName, HostState hostState,
                                                       Long lastHeartbeatTime) {
    return new HostUpdateEvent(clusterName, hostName, null, hostState, lastHeartbeatTime, null, null);
  }

  public static HostUpdateEvent createHostMaintenanceStatusUpdate(String clusterName, String hostName,
                                                                  MaintenanceState maintenanceState,
                                                                  AlertSummaryDTO alertsSummary) {
    return new HostUpdateEvent(clusterName, hostName, null, null, null, maintenanceState, alertsSummary);
  }

  public static HostUpdateEvent createHostAlertsUpdate(String clusterName, String hostName,
                                                       AlertSummaryDTO alertsSummary) {
    return new HostUpdateEvent(clusterName, hostName, null, null, null, null, alertsSummary);
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getHostStatus() {
    return hostStatus;
  }

  public void setHostStatus(String hostStatus) {
    this.hostStatus = hostStatus;
  }

  public Long getLastHeartbeatTime() {
    return lastHeartbeatTime;
  }

  public void setLastHeartbeatTime(Long lastHeartbeatTime) {
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  public void setMaintenanceState(MaintenanceState maintenanceState) {
    this.maintenanceState = maintenanceState;
  }

  public AlertSummaryDTO getAlertsSummary() {
    return alertsSummary;
  }

  public void setAlertsSummary(AlertSummaryDTO alertsSummary) {
    this.alertsSummary = alertsSummary;
  }

  public HostState getHostState() {
    return hostState;
  }

  public void setHostState(HostState hostState) {
    this.hostState = hostState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostUpdateEvent that = (HostUpdateEvent) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (hostStatus != null ? !hostStatus.equals(that.hostStatus) : that.hostStatus != null) return false;
    if (hostState != that.hostState) return false;
    if (lastHeartbeatTime != null ? !lastHeartbeatTime.equals(that.lastHeartbeatTime) : that.lastHeartbeatTime != null)
      return false;
    if (maintenanceState != that.maintenanceState) return false;
    return alertsSummary != null ? alertsSummary.equals(that.alertsSummary) : that.alertsSummary == null;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (hostStatus != null ? hostStatus.hashCode() : 0);
    result = 31 * result + (hostState != null ? hostState.hashCode() : 0);
    result = 31 * result + (lastHeartbeatTime != null ? lastHeartbeatTime.hashCode() : 0);
    result = 31 * result + (maintenanceState != null ? maintenanceState.hashCode() : 0);
    result = 31 * result + (alertsSummary != null ? alertsSummary.hashCode() : 0);
    return result;
  }
}
