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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.stomp.dto.ComponentVersionReports;
import org.apache.ambari.server.agent.stomp.dto.HostStatusReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.AgentActionEvent;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.EncryptionKeyUpdateEvent;
import org.apache.ambari.server.events.HostRegisteredEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.alert.AlertHelper;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


/**
 * This class handles the heartbeats coming from the agent, passes on the information
 * to other modules and processes the queue to send heartbeat response.
 */
@Singleton
public class HeartBeatHandler {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(HeartBeatHandler.class);

  private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
  private final Clusters clusterFsm;
  private final Encryptor<AgentConfigsUpdateEvent> encryptor;
  private HeartbeatMonitor heartbeatMonitor;
  private HeartbeatProcessor heartbeatProcessor;

  @Inject
  private Configuration config;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private AgentSessionManager agentSessionManager;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private AlertHelper alertHelper;

  private Map<String, Long> hostResponseIds = new ConcurrentHashMap<>();

  private Map<String, HeartBeatResponse> hostResponses = new ConcurrentHashMap<>();

  @Inject
  public HeartBeatHandler(Clusters fsm, ActionManager am, @Named("AgentConfigEncryptor") Encryptor<AgentConfigsUpdateEvent> encryptor,
                          Injector injector) {
    this.clusterFsm = fsm;
    this.encryptor = encryptor;
    this.heartbeatMonitor = new HeartbeatMonitor(fsm, am, 60000, injector);
    this.heartbeatProcessor = new HeartbeatProcessor(fsm, am, heartbeatMonitor, injector); //TODO modify to match pattern
    injector.injectMembers(this);
  }

  public void start() {
    heartbeatProcessor.startAsync();
    heartbeatMonitor.start();
  }

  void setHeartbeatMonitor(HeartbeatMonitor heartbeatMonitor) {
    this.heartbeatMonitor = heartbeatMonitor;
  }

  public void setHeartbeatProcessor(HeartbeatProcessor heartbeatProcessor) {
    this.heartbeatProcessor = heartbeatProcessor;
  }

  public HeartbeatProcessor getHeartbeatProcessor() {
    return heartbeatProcessor;
  }

  public HeartBeatResponse handleHeartBeat(HeartBeat heartbeat)
      throws AmbariException {
    long now = System.currentTimeMillis();
    if (heartbeat.getAgentEnv() != null && heartbeat.getAgentEnv().getHostHealth() != null) {
      heartbeat.getAgentEnv().getHostHealth().setServerTimeStampAtReporting(now);
    }

    String hostname = heartbeat.getHostname();
    Long currentResponseId = hostResponseIds.get(hostname);
    HeartBeatResponse response;

    if (currentResponseId == null) {
      //Server restarted, or unknown host.
      LOG.error("CurrentResponseId unknown for " + hostname + " - send register command");
      return createRegisterCommand();
    }

    LOG.debug("Received heartbeat from host, hostname={}, currentResponseId={}, receivedResponseId={}", hostname, currentResponseId, heartbeat.getResponseId());

    response = new HeartBeatResponse();
    Host hostObject;
    try {
      hostObject = clusterFsm.getHost(hostname);
    } catch (HostNotFoundException e) {
      LOG.error("Host: {} not found. Agent is still heartbeating.", hostname);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Host associated with the agent heratbeat might have been " +
            "deleted", e);
      }
      // For now return empty response with only response id.
      return response;
    }

    if (heartbeat.getResponseId() == currentResponseId - 1) {
      HeartBeatResponse heartBeatResponse = hostResponses.get(hostname);

      LOG.warn("Old responseId={} received form host {} - response was lost - returning cached response with responseId={}",
        heartbeat.getResponseId(),
        hostname,
        heartBeatResponse.getResponseId());

      return heartBeatResponse;
    } else if (heartbeat.getResponseId() != currentResponseId) {
      LOG.error("Error in responseId sequence - received responseId={} from host {} - sending agent restart command with responseId={}",
        heartbeat.getResponseId(),
        hostname,
        currentResponseId);

      return createRestartCommand(currentResponseId);
    }
    response.setResponseId(++currentResponseId);

    if (hostObject.getState().equals(HostState.HEARTBEAT_LOST)) {
      // After loosing heartbeat agent should reregister
      LOG.warn("Host {} is in HEARTBEAT_LOST state - sending register command", hostname);
      STOMPUpdatePublisher.publish(new AgentActionEvent(AgentActionEvent.AgentAction.RESTART_AGENT,
          hostObject.getHostId()));
      return createRegisterCommand();
    }

    hostResponseIds.put(hostname, currentResponseId);
    hostResponses.put(hostname, response);

    // If the host is waiting for component status updates, notify it
    if (hostObject.getState().equals(HostState.WAITING_FOR_HOST_STATUS_UPDATES)) {
      try {
        LOG.debug("Got component status updates for host {}", hostname);
        hostObject.handleEvent(new HostStatusUpdatesReceivedEvent(hostname, now));
      } catch (InvalidStateTransitionException e) {
        LOG.warn("Failed to notify the host {} about component status updates", hostname, e);
      }
    }
    if (heartbeat.getRecoveryReport() != null) {
      RecoveryReport rr = heartbeat.getRecoveryReport();
      processRecoveryReport(rr, hostname);
    }

    if (CollectionUtils.isNotEmpty(heartbeat.getStaleAlerts())) {
      alertHelper.addStaleAlerts(hostObject.getHostId(), heartbeat.getStaleAlerts());
    }

    try {
      hostObject.handleEvent(new HostHealthyHeartbeatEvent(hostname, now,
          heartbeat.getAgentEnv(), heartbeat.getMounts()));
    } catch (InvalidStateTransitionException ex) {
      LOG.warn("Asking agent to re-register due to " + ex.getMessage(), ex);
      hostObject.setState(HostState.INIT);
      return createRegisterCommand();
    }

    heartbeatProcessor.addHeartbeat(heartbeat);

    // Send commands if node is active
    if (hostObject.getState().equals(HostState.HEALTHY)) {
      annotateResponse(hostname, response);
    }

    return response;
  }

  public void handleComponentReportStatus(List<ComponentStatus> componentStatuses, String hostname) throws AmbariException {
    heartbeatProcessor.processStatusReports(componentStatuses, hostname);
    heartbeatProcessor.processHostStatus(componentStatuses, null, hostname);
  }

  public void handleCommandReportStatus(List<CommandReport> reports, String hostname) throws AmbariException {
    heartbeatProcessor.processCommandReports(reports, hostname, System.currentTimeMillis());
    heartbeatProcessor.processHostStatus(null, reports, hostname);
  }

  public void handleHostReportStatus(HostStatusReport hostStatusReport, String hostname) throws AmbariException {
    Host host = clusterFsm.getHost(hostname);
    try {
      host.handleEvent(new HostHealthyHeartbeatEvent(hostname, System.currentTimeMillis(),
          hostStatusReport.getAgentEnv(), hostStatusReport.getMounts()));
    } catch (InvalidStateTransitionException ex) {
      LOG.warn("Asking agent to re-register due to " + ex.getMessage(), ex);
      host.setState(HostState.INIT);
      agentSessionManager.unregisterByHost(host.getHostId());
    }
  }

  public void handleComponentVersionReports(ComponentVersionReports componentVersionReports, String hostname) throws AmbariException {
    heartbeatProcessor.processVersionReports(componentVersionReports, hostname);
  }

  protected void processRecoveryReport(RecoveryReport recoveryReport, String hostname) throws AmbariException {
    LOG.debug("Received recovery report: {}", recoveryReport);
    Host host = clusterFsm.getHost(hostname);
    host.setRecoveryReport(recoveryReport);
  }

  public String getOsType(String os, String osRelease) {
    String osType = "";
    if (os != null) {
      osType = os;
    }
    if (osRelease != null) {
      String[] release = DOT_PATTERN.split(osRelease);
      if (release.length > 0) {
        osType += release[0];
      }
    }
    return osType.toLowerCase();
  }

  public HeartBeatResponse createRegisterCommand() {
    HeartBeatResponse response = new HeartBeatResponse();
    RegistrationCommand regCmd = new RegistrationCommand();
    response.setResponseId(0);
    response.setRegistrationCommand(regCmd);
    return response;
  }

  protected HeartBeatResponse createRestartCommand(Long currentResponseId) {
    HeartBeatResponse response = new HeartBeatResponse();
    response.setRestartAgent(true);
    response.setResponseId(currentResponseId);
    return response;
  }

  public RegistrationResponse handleRegistration(Register register)
      throws InvalidStateTransitionException, AmbariException {
    String hostname = register.getHostname();
    int currentPingPort = register.getCurrentPingPort();
    long now = System.currentTimeMillis();

    String agentVersion = register.getAgentVersion();
    String serverVersion = ambariMetaInfo.getServerVersion();
    if (!VersionUtils.areVersionsEqual(serverVersion, agentVersion, true)) {
      LOG.warn("Received registration request from host with non compatible"
          + " agent version"
          + ", hostname=" + hostname
          + ", agentVersion=" + agentVersion
          + ", serverVersion=" + serverVersion);
      throw new AmbariException("Cannot register host with non compatible"
          + " agent version"
          + ", hostname=" + hostname
          + ", agentVersion=" + agentVersion
          + ", serverVersion=" + serverVersion);
    }

    String agentOsType = getOsType(register.getHardwareProfile().getOS(),
        register.getHardwareProfile().getOSRelease());
    LOG.info("agentOsType = "+agentOsType );
    if (!ambariMetaInfo.isOsSupported(agentOsType)) {
      LOG.warn("Received registration request from host with not supported"
          + " os type"
          + ", hostname=" + hostname
          + ", serverOsType=" + config.getServerOsType()
          + ", agentOsType=" + agentOsType);
      throw new AmbariException("Cannot register host with not supported"
          + " os type"
          + ", hostname=" + hostname
          + ", serverOsType=" + config.getServerOsType()
          + ", agentOsType=" + agentOsType);
    }

    Host hostObject;
    try {
      hostObject = clusterFsm.getHost(hostname);
    } catch (HostNotFoundException ex) {
      clusterFsm.addHost(hostname);
      hostObject = clusterFsm.getHost(hostname);
    }

    // Resetting host state
    hostObject.setStateMachineState(HostState.INIT);

    // Set ping port for agent
    hostObject.setCurrentPingPort(currentPingPort);

    // Save the prefix of the log file paths
    hostObject.setPrefix(register.getPrefix());

    alertHelper.clearStaleAlerts(hostObject.getHostId());

    hostObject.handleEvent(new HostRegistrationRequestEvent(hostname,
        null != register.getPublicHostname() ? register.getPublicHostname() : hostname,
        new AgentVersion(register.getAgentVersion()), now, register.getHardwareProfile(),
        register.getAgentEnv(), register.getAgentStartTime()));

    // publish the event
    HostRegisteredEvent event = new HostRegisteredEvent(hostname, hostObject.getHostId());
    ambariEventPublisher.publish(event);

    if (config.shouldEncryptSensitiveData()) {
      EncryptionKeyUpdateEvent encryptionKeyUpdateEvent = new EncryptionKeyUpdateEvent(encryptor.getEncryptionKey());
      STOMPUpdatePublisher.publish(encryptionKeyUpdateEvent);
    }

    RegistrationResponse response = new RegistrationResponse();

    Long requestId = 0L;
    hostResponseIds.put(hostname, requestId);
    response.setResponseId(requestId);
    return response;
  }

  /**
   * Annotate the response with some housekeeping details.
   * hasMappedComponents - indicates if any components are mapped to the host
   * hasPendingTasks - indicates if any tasks are pending for the host (they may not be sent yet)
   * clusterSize - indicates the number of hosts that form the cluster
   * @param hostname
   * @param response
   * @throws org.apache.ambari.server.AmbariException
   */
  private void annotateResponse(String hostname, HeartBeatResponse response) throws AmbariException {
    for (Cluster cl : clusterFsm.getClustersForHost(hostname)) {
      response.setClusterSize(cl.getClusterSize());

      List<ServiceComponentHost> scHosts = cl.getServiceComponentHosts(hostname);
      if (scHosts != null && scHosts.size() > 0) {
        response.setHasMappedComponents(true);
        break;
      }
    }
  }

  /**
   * Response contains information about HDP Stack in use
   * @param clusterName
   * @return @ComponentsResponse
   * @throws org.apache.ambari.server.AmbariException
   */
  public ComponentsResponse handleComponents(String clusterName)
      throws AmbariException {
    ComponentsResponse response = new ComponentsResponse();

    Cluster cluster = clusterFsm.getCluster(clusterName);

    Map<String, Map<String, String>> componentsMap = new HashMap<>();

    for (org.apache.ambari.server.state.Service service : cluster.getServices().values()) {
      componentsMap.put(service.getName(), new HashMap<>());

      for (ServiceComponent component : service.getServiceComponents().values()) {
        StackId stackId = component.getDesiredStackId();

        ComponentInfo componentInfo = ambariMetaInfo.getComponent(
            stackId.getStackName(), stackId.getStackVersion(), service.getName(), component.getName());

        componentsMap.get(service.getName()).put(component.getName(), componentInfo.getCategory());
      }
    }

    response.setClusterName(clusterName);
    response.setComponents(componentsMap);

    return response;
  }

  public void stop() {
    heartbeatMonitor.shutdown();
    heartbeatProcessor.stopAsync();
  }
}
