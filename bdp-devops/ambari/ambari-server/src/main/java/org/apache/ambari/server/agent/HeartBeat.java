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

package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.state.Alert;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 *
 * Data model for Ambari Agent to send heartbeat to Ambari Server.
 *
 */

public class HeartBeat {

  @com.fasterxml.jackson.annotation.JsonProperty("id")
  private long responseId = -1;

  @com.fasterxml.jackson.annotation.JsonProperty("staleAlerts")
  private List<StaleAlert> staleAlerts = new ArrayList<>();

  private long timestamp;
  private String hostname;
  List<CommandReport> reports = new ArrayList<>();
  List<ComponentStatus> componentStatus = new ArrayList<>();
  private List<DiskInfo> mounts = new ArrayList<>();
  HostStatus nodeStatus;
  private AgentEnv agentEnv = null;
  private List<Alert> alerts = null;
  private RecoveryReport recoveryReport;
  private long recoveryTimestamp = -1;

  public long getResponseId() {
    return responseId;
  }

  public void setResponseId(long responseId) {
    this.responseId=responseId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getHostname() {
    return hostname;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Timestamp when the recovery values were last updated.
   *
   * @return - Timestamp.
   */
  public long getRecoveryTimestamp() {
    return recoveryTimestamp;
  }

  /**
   * Set the timestamp when the recovery values were last updated.
   *
   * @param recoveryTimestamp
   */
  public void setRecoveryTimestamp(long recoveryTimestamp) {
    this.recoveryTimestamp = recoveryTimestamp;
  }

  @JsonProperty("reports")
  @com.fasterxml.jackson.annotation.JsonProperty("reports")
  public List<CommandReport> getReports() {
    return reports;
  }

  @JsonProperty("reports")
  @com.fasterxml.jackson.annotation.JsonProperty("reports")
  public void setReports(List<CommandReport> reports) {
    this.reports = reports;
  }

  public HostStatus getNodeStatus() {
    return nodeStatus;
  }

  public void setNodeStatus(HostStatus nodeStatus) {
    this.nodeStatus = nodeStatus;
  }

  public RecoveryReport getRecoveryReport() {
    return recoveryReport;
  }

  public void setRecoveryReport(RecoveryReport recoveryReport) {
    this.recoveryReport = recoveryReport;
  }

  public AgentEnv getAgentEnv() {
    return agentEnv;
  }

  public void setAgentEnv(AgentEnv env) {
    agentEnv = env;
  }

  @JsonProperty("componentStatus")
  @com.fasterxml.jackson.annotation.JsonProperty("componentStatus")
  public List<ComponentStatus> getComponentStatus() {
    return componentStatus;
  }

  @JsonProperty("componentStatus")
  @com.fasterxml.jackson.annotation.JsonProperty("componentStatus")
  public void setComponentStatus(List<ComponentStatus> componentStatus) {
    this.componentStatus = componentStatus;
  }

  @JsonProperty("mounts")
  @com.fasterxml.jackson.annotation.JsonProperty("mounts")
  public List<DiskInfo> getMounts() {
    return mounts;
  }

  @JsonProperty("mounts")
  @com.fasterxml.jackson.annotation.JsonProperty("mounts")
  public void setMounts(List<DiskInfo> mounts) {
    this.mounts = mounts;
  }

  public List<Alert> getAlerts() {
    return alerts;
  }

  public void setAlerts(List<Alert> alerts) {
    this.alerts = alerts;
  }

  public List<StaleAlert> getStaleAlerts() {
    return staleAlerts;
  }

  @Override
  public String toString() {
    return "HeartBeat{" +
            "responseId=" + responseId +
            ", timestamp=" + timestamp +
            ", hostname='" + hostname + '\'' +
            ", reports=" + reports +
            ", componentStatus=" + componentStatus +
            ", nodeStatus=" + nodeStatus +
            ", recoveryReport=" + recoveryReport +
            '}';
  }
}
