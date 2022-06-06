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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;

import io.swagger.annotations.ApiModelProperty;

public class HostResponse {

  private String hostname;

  private String clusterName;

  /**
   * Host IP if ipv4 interface available
   */
  private String ipv4;

  /**
   * Count of cores on Host
   */
  private long cpuCount;
  
  /**
   * Count of physical cores on Host
   */
  private long phCpuCount;

  /**
   * Os Architecture
   */
  private String osArch;

  private String osFamily;

  /**
   * OS Type
   */
  private String osType;

  /**
   * Amount of physical memory for the Host
   */
  private long totalMemBytes;

  /**
   * Disks mounted on the Host
   */
  private List<DiskInfo> disksInfo;

  /**
   * Last heartbeat timestamp from the Host
   */
  private long lastHeartbeatTime;
  
  /**
   * Last environment information
   */
  private AgentEnv lastAgentEnv;

  /**
   * Last registration timestamp for the Host
   */
  private long lastRegistrationTime;

  /**
   * Rack to which the Host belongs to
   */
  private String rackInfo;

  /**
   * Additional Host attributes
   */
  private Map<String, String> hostAttributes;

  /**
   * Version of agent running on the Host
   */
  private AgentVersion agentVersion;

  /**
   * Host Health Status
   */
  private HostHealthStatus healthStatus;

  /**
   * Recovery status
   */
  private RecoveryReport recoveryReport;

  /**
   * Summary of node recovery
   */
  private String recoverySummary = "DISABLED";
  
  /**
   * Public name.
   */
  private String publicHostname;

  /**
   * Host State
   */
  private HostState hostState;

  /**
   * Configs derived from Config groups
   */
  private Map<String, HostConfig> desiredHostConfigs;

  /**
   * Host status, calculated on host components statuses
   */
  private String status;

  private MaintenanceState maintenanceState;

  public HostResponse(String hostname, String clusterName,
                      String ipv4, int cpuCount, int phCpuCount, String osArch, String osType,
                      long totalMemBytes,
                      List<DiskInfo> disksInfo, long lastHeartbeatTime,
                      long lastRegistrationTime, String rackInfo,
                      Map<String, String> hostAttributes, AgentVersion agentVersion,
                      HostHealthStatus healthStatus, HostState hostState, String status) {
    this.hostname = hostname;
    this.clusterName = clusterName;
    this.ipv4 = ipv4;
    this.cpuCount = cpuCount;
    this.phCpuCount = phCpuCount;
    this.osArch = osArch;
    this.osType = osType;
    this.totalMemBytes = totalMemBytes;
    this.disksInfo = disksInfo;
    this.lastHeartbeatTime = lastHeartbeatTime;
    this.lastRegistrationTime = lastRegistrationTime;
    this.rackInfo = rackInfo;
    this.hostAttributes = hostAttributes;
    this.agentVersion = agentVersion;
    this.healthStatus = healthStatus;
    this.hostState = hostState;
    this.status = status;
  }

  //todo: why are we passing in empty strings for host/cluster name instead of null?
  public HostResponse(String hostname) {
    this(hostname, "", "",
      0, 0, "", "",
      0, new ArrayList<>(),
      0, 0, "",
      new HashMap<>(),
      null, null, null, null);
  }

  @ApiModelProperty(name = HostResourceProvider.HOST_NAME_PROPERTY_ID)
  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @ApiModelProperty(name = HostResourceProvider.CLUSTER_NAME_PROPERTY_ID)
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the name of the associated cluster
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @ApiModelProperty(name = HostResourceProvider.IP_PROPERTY_ID)
  public String getIpv4() {
    return ipv4;
  }

  public void setIpv4(String ipv4) {
    this.ipv4 = ipv4;
  }

  @ApiModelProperty(name = HostResourceProvider.CPU_COUNT_PROPERTY_ID)
  public long getCpuCount() {
    return cpuCount;
  }

  public void setCpuCount(long cpuCount) {
    this.cpuCount = cpuCount;
  }

  @ApiModelProperty(name = HostResourceProvider.PHYSICAL_CPU_COUNT_PROPERTY_ID)
  public long getPhCpuCount() {
    return phCpuCount;
  }

  public void setPhCpuCount(long phCpuCount) {
    this.phCpuCount = phCpuCount;
  }

  @ApiModelProperty(name = HostResourceProvider.OS_ARCH_PROPERTY_ID)
  public String getOsArch() {
    return osArch;
  }

  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  @ApiModelProperty(name = HostResourceProvider.OS_FAMILY_PROPERTY_ID)
  public String getOsFamily() {
    return osFamily;
  }

  public void setOsFamily(String osFamily) {
    this.osFamily = osFamily;
  }

  @ApiModelProperty(name = HostResourceProvider.OS_TYPE_PROPERTY_ID)
  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  @ApiModelProperty(name = HostResourceProvider.TOTAL_MEM_PROPERTY_ID)
  public long getTotalMemBytes() {
    return totalMemBytes;
  }

  public void setTotalMemBytes(long totalMemBytes) {
    this.totalMemBytes = totalMemBytes;
  }

  @ApiModelProperty(name = HostResourceProvider.DISK_INFO_PROPERTY_ID)
  public List<DiskInfo> getDisksInfo() {
    return disksInfo;
  }

  public void setDisksInfo(List<DiskInfo> disksInfo) {
    this.disksInfo = disksInfo;
  }

  @ApiModelProperty(name = HostResourceProvider.LAST_HEARTBEAT_TIME_PROPERTY_ID)
  public long getLastHeartbeatTime() {
    return lastHeartbeatTime;
  }

  public void setLastHeartbeatTime(long lastHeartbeatTime) {
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  @ApiModelProperty(name = HostResourceProvider.LAST_REGISTRATION_TIME_PROPERTY_ID)
  public long getLastRegistrationTime() {
    return lastRegistrationTime;
  }

  public void setLastRegistrationTime(long lastRegistrationTime) {
    this.lastRegistrationTime = lastRegistrationTime;
  }

  @ApiModelProperty(name = HostResourceProvider.RACK_INFO_PROPERTY_ID)
  public String getRackInfo() {
    return rackInfo;
  }

  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  @ApiModelProperty(hidden = true)
  public Map<String, String> getHostAttributes() {
    return hostAttributes;
  }

  public void setHostAttributes(Map<String, String> hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  @ApiModelProperty(hidden = true)
  public AgentVersion getAgentVersion() {
    return agentVersion;
  }

  public void setAgentVersion(AgentVersion agentVersion) {
    this.agentVersion = agentVersion;
  }

  @ApiModelProperty(name = HostResourceProvider.HOST_HEALTH_REPORT_PROPERTY_ID)
  public String getHealthReport() {
    return healthStatus.getHealthReport();
  }

  public void setHealthStatus(HostHealthStatus healthStatus) {
    this.healthStatus = healthStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostResponse other = (HostResponse) o;

    return Objects.equals(hostname, other.hostname);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hostname);
  }

  @ApiModelProperty(name = HostResourceProvider.PUBLIC_NAME_PROPERTY_ID)
  public String getPublicHostName() {
    return publicHostname;
  }

  public void setPublicHostName(String name) {
    publicHostname = name;
  }

  @ApiModelProperty(name = HostResourceProvider.STATE_PROPERTY_ID)
  public HostState getHostState() {
    return hostState;
  }

  public void setHostState(HostState hostState) {
    this.hostState = hostState;
  }

  @ApiModelProperty(name = HostResourceProvider.LAST_AGENT_ENV_PROPERTY_ID)
  public AgentEnv getLastAgentEnv() {
    return lastAgentEnv;
  }
  
  public void setLastAgentEnv(AgentEnv agentEnv) {
    lastAgentEnv = agentEnv;
  }
  
  @ApiModelProperty(name = HostResourceProvider.DESIRED_CONFIGS_PROPERTY_ID)
  public Map<String, HostConfig> getDesiredHostConfigs() {
    return desiredHostConfigs;
  }

  public void setDesiredHostConfigs(Map<String, HostConfig> desiredHostConfigs) {
    this.desiredHostConfigs = desiredHostConfigs;
  }

  @ApiModelProperty(name = HostResourceProvider.HOST_STATUS_PROPERTY_ID)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setMaintenanceState(MaintenanceState state) {
    maintenanceState = state;
  }
  
  @ApiModelProperty(name = HostResourceProvider.MAINTENANCE_STATE_PROPERTY_ID)
  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * Get the recovery summary for the host
   */
  @ApiModelProperty(name = HostResourceProvider.RECOVERY_SUMMARY_PROPERTY_ID)
  public String getRecoverySummary() {
    return recoverySummary;
  }

  /**
   * Set the recovery summary for the host
   */
  public void setRecoverySummary(String recoverySummary) {
    this.recoverySummary = recoverySummary;
  }

  /**
   * Get the detailed recovery report
   */
  @ApiModelProperty(name = HostResourceProvider.RECOVERY_REPORT_PROPERTY_ID)
  public RecoveryReport getRecoveryReport() {
    return recoveryReport;
  }

  /**
   * Set the detailed recovery report
   */
  public void setRecoveryReport(RecoveryReport recoveryReport) {
    this.recoveryReport = recoveryReport;
  }

  public interface HostResponseWrapper extends ApiModel {
    @ApiModelProperty(name = HostResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    HostResponse getHostResponse();
  }
}
