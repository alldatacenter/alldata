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

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;

public interface Host extends Comparable {

  HostEntity getHostEntity();

  /**
   * @return the hostName
   */
  String getHostName();

  /**
   * @return host id
   */
  Long getHostId();

  /**
   * @return the currentPingPort
   */
  Integer getCurrentPingPort();

  /**
   * @param currentPingPort the currentPingPort to set
   */
  void setCurrentPingPort(Integer currentPingPort);

  /**
   * Gets the public-facing host name.
   */
  void setPublicHostName(String hostName);

  /**
   * Sets the public-facing host name.
   */
  String getPublicHostName();

  /**
   * IPv4 assigned to the Host
   * @return the ip or null if no IPv4 interface
   */
  String getIPv4();

  /**
   * @param ip the ip to set
   */
  void setIPv4(String ip);

  /**
   * IPv6 assigned to the Host
   * @return the ip or null if no IPv6 interface
   */
  String getIPv6();

  /**
   * @param ip the ip to set
   */
  void setIPv6(String ip);

  /**
   * @return the cpuCount
   */
  int getCpuCount();

  /**
   * @param cpuCount the cpuCount to set
   */
  void setCpuCount(int cpuCount);

  /**
   * @return the physical cpu cores
   */
  int getPhCpuCount();

  /**
   * @param phCpuCount the physical cpu cores to set
   */
  void setPhCpuCount(int phCpuCount);

  /**
   * Get the Amount of physical memory for the Host.
   * @return the totalMemBytes
   */
  long getTotalMemBytes();

  /**
   * Set the Amount of physical memory for the Host.
   * @param totalMemBytes the totalMemBytes to set
   */
  void setTotalMemBytes(long totalMemBytes);

  /**
   * Get the Amount of available memory for the Host.
   * In most cases, available should be same as total unless
   * the agent on the host is configured to not use all
   * available memory
   * @return the availableMemBytes
   */
  long getAvailableMemBytes();

  /**
   * Set the Amount of available memory for the Host.
   * @param availableMemBytes the availableMemBytes to set
   */
  void setAvailableMemBytes(long availableMemBytes);

  /**
   * Get the OS Architecture.
   * i386, x86_64, etc.
   * @return the osArch
   */
  String getOsArch();

  /**
   * @param osArch the osArch to set
   */
  void setOsArch(String osArch);

  /**
   * Get the General OS information.
   * uname -a, /etc/*-release dump
   * @return the osInfo
   */
  String getOsInfo();

  /**
   * @param osInfo the osInfo to set
   */
  void setOsInfo(String osInfo);

  /**
   * Get the OS Type: RHEL6/CentOS6/...
   * Defined and match-able OS type
   * @return the osType
   */
  String getOsType();

  /**
   * Get the os Family:
   * redhat6: for centos6, rhel6, oraclelinux6 ..
   * ubuntu12 : for ubuntu12
   * suse11: for sles11, suse11 ..
   * suse12: for suse12, sles12 ..
   * @return the osFamily
   */
  String getOsFamily();

  /**
   * Gets the os family from host attributes
   * @param hostAttributes host attributes
   * @return the os family for host
   */
  String getOsFamily(Map<String, String> hostAttributes);

  String getOSFamilyFromHostAttributes(Map<String, String> hostAttributes);

  /**
   * @param osType the osType to set
   */
  void setOsType(String osType);

  /**
   * Get information on disks available on the host.
   * @return the disksInfo
   */
  List<DiskInfo> getDisksInfo();

  /**
   * @param disksInfo the disksInfo to set
   */
  void setDisksInfo(List<DiskInfo> disksInfo);

  /**
   * @return the healthStatus
   */
  HostHealthStatus getHealthStatus();

  /**
   * Gets the health status from host attributes
   * @param hostStateEntity host attributes
   * @return the health status
   */
  HostHealthStatus getHealthStatus(HostStateEntity hostStateEntity);

  /**
   * Get detailed recovery report for the host
   * @return
   */
  RecoveryReport getRecoveryReport();

  /**
   * Set detailed recovery report for the host
   */
  void setRecoveryReport(RecoveryReport recoveryReport);

  /**
   * @param healthStatus the healthStatus to set
   */
  void setHealthStatus(HostHealthStatus healthStatus);

  /**
   * Get additional host attributes
   * For example, public/hostname/IP for AWS
   * @return the hostAttributes
   */
  Map<String, String> getHostAttributes();

  /**
   * Gets host attributes from host entity
   * @param hostEntity host entity
   * @return the host attributes
   */
  Map<String, String> getHostAttributes(HostEntity hostEntity);

  /**
   * @param hostAttributes the hostAttributes to set
   */
  void setHostAttributes(Map<String, String> hostAttributes);

  /**
   * @return the rackInfo
   */
  String getRackInfo();

  /**
   * @param rackInfo the rackInfo to set
   */
  void setRackInfo(String rackInfo);

  /**
   * Last time the host registered with the Ambari Server
   * ( Unix timestamp )
   * @return the lastRegistrationTime
   */
  long getLastRegistrationTime();

  /**
   * @param lastRegistrationTime the lastRegistrationTime to set
   */
  void setLastRegistrationTime(long lastRegistrationTime);

  /**
   * Time the Ambari Agent was started.
   * ( Unix timestamp )
   * @return the lastOnAgentStartRegistrationTime
   */
  long getLastAgentStartTime();

  /**
   * @param lastAgentStartTime the lastAgentStartTime to set
   */
  void setLastAgentStartTime(long lastAgentStartTime);

  /**
   * Last time the Ambari Server received a heartbeat from the Host
   * ( Unix timestamp )
   * @return the lastHeartbeatTime
   */
  long getLastHeartbeatTime();

  /**
   * @param lastHeartbeatTime the lastHeartbeatTime to set
   */
  void setLastHeartbeatTime(long lastHeartbeatTime);

  /**
   * Sets the latest agent environment that arrived in a heartbeat.
   */
  void setLastAgentEnv(AgentEnv env);

  /**
   * Gets the latest agent environment that arrived in a heartbeat.
   */
  AgentEnv getLastAgentEnv();

  /**
   * Version of the Ambari Agent running on the host
   * @return the agentVersion
   */
  AgentVersion getAgentVersion();

  /**
   * Gets version of the ambari agent running on the host.
   * @param hostStateEntity host state entity
   * @return the agentVersion
   */
  AgentVersion getAgentVersion(HostStateEntity hostStateEntity);

  /**
   * @param agentVersion the agentVersion to set
   */
  void setAgentVersion(AgentVersion agentVersion);

  /**
   * Get Current Host State
   * @return HostState
   */
  HostState getState();

  /**
   * Set the State of the Host
   * @param state Host State
   */
  void setState(HostState state);

  /**
   * Set state of host's state machine.
   * @param state
   */
  void setStateMachineState(HostState state);

  /**
   * Get the prefix path of all logs
   * @return prefix
   */
  String getPrefix();

  /**
   * Set the prefix path of all logs of the host
   * @param prefix the prefix path to set
   */
  void setPrefix(String prefix);

  /**
   * Send an event to the Host's StateMachine
   * @param event HostEvent
   * @throws InvalidStateTransitionException
   */
  void handleEvent(HostEvent event)
      throws InvalidStateTransitionException;

  /**
   * Get time spent in the current state i.e. the time since last state change.
   * @return Time spent in current state.
   */
  long getTimeInState();

  /**
   * @param timeInState the timeInState to set
   */
  void setTimeInState(long timeInState);

  /**
   * Get Current Host Status
   * @return String
   */
  String getStatus();

  /**
   * Set the Status of the Host
   * @param status Host Status
   */
  void setStatus(String status);

  HostResponse convertToResponse();

  void importHostInfo(HostInfo hostInfo);

  /**
   * Adds a desired configuration to the host instance.
   * @param clusterId the cluster id that the config applies to
   * @param selected <code>true</code> if the configuration is selected.  Applies
   *    only to remove the override, otherwise this value should always be <code>true</code>.
   * @param user the user making the change for audit purposes
   * @param config the configuration object
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  boolean addDesiredConfig(long clusterId, boolean selected, String user, Config config);

  /**
   * Gets all the selected configurations for the host.
   * return a map of type-to-{@link DesiredConfig} instances.
   */
  Map<String, DesiredConfig> getDesiredConfigs(long clusterId);

  /**
   * Get the desired configurations for the host including overrides
   *
   * @param cluster
   * @param clusterDesiredConfigs
   *          the desired configurations for the cluster. Obtaining these can be
   *          expensive and since this method operates on hosts, it could be
   *          called 1,000's of times when generating host responses. Therefore,
   *          the caller should build these once and pass them in. If
   *          {@code null}, then this method will retrieve them at runtime,
   *          incurring a performance penality.
   * @return
   * @throws AmbariException
   */
  Map<String, HostConfig> getDesiredHostConfigs(Cluster cluster,
      Map<String, DesiredConfig> clusterDesiredConfigs) throws AmbariException;

  /**
   * Sets the maintenance state for the host.
   * @param clusterId the cluster id
   * @param state the state
   */
  void setMaintenanceState(long clusterId, MaintenanceState state);

  /**
   * @param clusterId the cluster id
   * @return the maintenance state
   */
  MaintenanceState getMaintenanceState(long clusterId);

  /**
   * Get all of the HostVersionEntity objects for the host.
   * @return
   */
  List<HostVersionEntity> getAllHostVersions();

  /**
   * Gets whether this host has components which advertise their version.
   *
   * @param stackId
   *          the version of the stack to use when checking version
   *          advertise-ability.
   * @return {@code true} if at least 1 component on this host advertises its
   *         version.
   * @throws AmbariException
   *           if there is a problem retrieving the component from the stack.
   * @see ComponentInfo#isVersionAdvertised()
   */
  boolean hasComponentsAdvertisingVersions(StackId stackId) throws AmbariException;

  void calculateHostStatus(Long clusterId) throws AmbariException;

  /**
   * Gets whether all host components whose desired repository version matches
   * the repository version specified have reported the correct version and are
   * no longer upgrading.
   *
   * @param repositoryVersion
   *          the repository version to check for (not {@code null}).
   * @return {@code true} if all components on this host have checked in with
   *         the correct version if their desired repository matches the one
   *         specified.
   *
   * @throws AmbariException
   */
  boolean isRepositoryVersionCorrect(RepositoryVersionEntity repositoryVersion)
      throws AmbariException;
}
