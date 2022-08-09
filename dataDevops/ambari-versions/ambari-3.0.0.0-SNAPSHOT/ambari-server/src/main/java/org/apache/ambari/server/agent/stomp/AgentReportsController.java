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
package org.apache.ambari.server.agent.stomp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.agent.AgentReportsProcessor;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.CommandStatusAgentReport;
import org.apache.ambari.server.agent.ComponentStatus;
import org.apache.ambari.server.agent.ComponentStatusAgentReport;
import org.apache.ambari.server.agent.ComponentVersionAgentReport;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.HostStatusAgentReport;
import org.apache.ambari.server.agent.stomp.dto.AckReport;
import org.apache.ambari.server.agent.stomp.dto.CommandStatusReports;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReport;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReports;
import org.apache.ambari.server.agent.stomp.dto.ComponentVersionReports;
import org.apache.ambari.server.agent.stomp.dto.HostStatusReport;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.google.inject.Injector;

@Controller
@SendToUser("/")
@MessageMapping("/reports")
public class AgentReportsController {
  private static final Logger LOG = LoggerFactory.getLogger(AgentReportsController.class);

  @Autowired
  private Provider<DefaultMessageEmitter> defaultMessageEmitterProvider;

  private final HeartBeatHandler hh;
  private final AgentSessionManager agentSessionManager;
  private final AgentReportsProcessor agentReportsProcessor;

  public AgentReportsController(Injector injector) {
    hh = injector.getInstance(HeartBeatHandler.class);
    agentSessionManager = injector.getInstance(AgentSessionManager.class);
    agentReportsProcessor = injector.getInstance(AgentReportsProcessor.class);
  }

  @MessageMapping("/component_version")
  public ReportsResponse handleComponentVersionReport(@Header String simpSessionId, ComponentVersionReports message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {

    agentReportsProcessor.addAgentReport(new ComponentVersionAgentReport(hh,
        agentSessionManager.getHost(simpSessionId).getHostName(), message));
    return new ReportsResponse();
  }

  @MessageMapping("/component_status")
  public ReportsResponse handleComponentReportStatus(@Header String simpSessionId, ComponentStatusReports message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {
    List<ComponentStatus> statuses = new ArrayList<>();
    for (Map.Entry<String, List<ComponentStatusReport>> clusterReport : message.getComponentStatusReports().entrySet()) {
      for (ComponentStatusReport report : clusterReport.getValue()) {
        ComponentStatus componentStatus = new ComponentStatus();
        componentStatus.setClusterId(report.getClusterId());
        componentStatus.setComponentName(report.getComponentName());
        componentStatus.setServiceName(report.getServiceName());
        componentStatus.setStatus(report.getStatus());
        statuses.add(componentStatus);
      }
    }

    agentReportsProcessor.addAgentReport(new ComponentStatusAgentReport(hh,
        agentSessionManager.getHost(simpSessionId).getHostName(), statuses));
    return new ReportsResponse();
  }

  @MessageMapping("/commands_status")
  public ReportsResponse handleCommandReportStatus(@Header String simpSessionId, CommandStatusReports message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {
    List<CommandReport> statuses = new ArrayList<>();
    for (Map.Entry<String, List<CommandReport>> clusterReport : message.getClustersComponentReports().entrySet()) {
      statuses.addAll(clusterReport.getValue());
    }

    agentReportsProcessor.addAgentReport(new CommandStatusAgentReport(hh,
        agentSessionManager.getHost(simpSessionId).getHostName(), statuses));
    return new ReportsResponse();
  }

  @MessageMapping("/host_status")
  public ReportsResponse handleHostReportStatus(@Header String simpSessionId, HostStatusReport message) throws AmbariException {
    agentReportsProcessor.addAgentReport(new HostStatusAgentReport(hh,
        agentSessionManager.getHost(simpSessionId).getHostName(), message));
    return new ReportsResponse();
  }

  @MessageMapping("/alerts_status")
  public ReportsResponse handleAlertsStatus(@Header String simpSessionId, Alert[] message) throws AmbariException {
    String hostName = agentSessionManager.getHost(simpSessionId).getHostName();
    List<Alert> alerts = Arrays.asList(message);
    LOG.debug("Handling {} alerts status for host {}", alerts.size(), hostName);
    hh.getHeartbeatProcessor().processAlerts(hostName, alerts);
    return new ReportsResponse();
  }

  @MessageMapping("/responses")
  public ReportsResponse handleReceiveReport(@Header String simpSessionId, AckReport ackReport) throws HostNotRegisteredException {
    Long hostId = agentSessionManager.getHost(simpSessionId).getHostId();
    LOG.debug("Handling agent receive report for execution message with messageId {}, status {}, reason {}",
        ackReport.getMessageId(), ackReport.getStatus(), ackReport.getReason());
    defaultMessageEmitterProvider.get().processReceiveReport(hostId, ackReport);
    return new ReportsResponse();
  }

}
