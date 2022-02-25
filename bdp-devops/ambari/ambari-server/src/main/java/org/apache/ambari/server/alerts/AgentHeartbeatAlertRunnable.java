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
package org.apache.ambari.server.alerts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.services.AmbariServerAlertService;

/**
 * The {@link AgentHeartbeatAlertRunnable} is used by the
 * {@link AmbariServerAlertService} to check agent heartbeats and fire alert
 * events when agents are not reachable.
 */
public class AgentHeartbeatAlertRunnable extends AlertRunnable {
  /**
   * Agent initializing message.
   */
  private static final String INIT_MSG = "{0} is initializing";

  /**
   * Agent healthy message.
   */
  private static final String HEALTHY_MSG = "{0} is healthy";

  /**
   * Agent waiting for status updates message.
   */
  private static final String STATUS_UPDATE_MSG = "{0} is waiting for status updates";

  /**
   * Agent is not heartbeating message.
   */
  private static final String HEARTBEAT_LOST_MSG = "{0} is not sending heartbeats";

  /**
   * Agent is not healthy message.
   */
  private static final String UNHEALTHY_MSG = "{0} is not healthy";

  /**
   * Unknown agent state message.
   */
  private static final String UNKNOWN_MSG = "{0} has an unknown state of {1}";

  /**
   * Constructor.
   *
   * @param definitionName
   */
  public AgentHeartbeatAlertRunnable(String definitionName) {
    super(definitionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  List<Alert> execute(Cluster cluster, AlertDefinitionEntity definition)
      throws AmbariException {
    long alertTimestamp = System.currentTimeMillis();

    Collection<Host> hosts = cluster.getHosts();

    List<Alert> alerts = new ArrayList<>(hosts.size());
    for (Host host : hosts) {
      String hostName = host.getHostName();

      String alertText;
      AlertState alertState = AlertState.OK;
      HostState hostState = host.getState();

      switch (hostState) {
        case INIT:
          alertText = MessageFormat.format(INIT_MSG, hostName);
          break;
        case HEALTHY:
          alertText = MessageFormat.format(HEALTHY_MSG, hostName);
          break;
        case WAITING_FOR_HOST_STATUS_UPDATES:
          alertText = MessageFormat.format(STATUS_UPDATE_MSG, hostName);
          break;
        case HEARTBEAT_LOST:
          alertState = AlertState.CRITICAL;
          alertText = MessageFormat.format(HEARTBEAT_LOST_MSG, hostName);
          break;
        case UNHEALTHY:
          alertState = AlertState.CRITICAL;
          alertText = MessageFormat.format(UNHEALTHY_MSG, hostName);
          break;
        default:
          alertState = AlertState.UNKNOWN;
          alertText = MessageFormat.format(UNKNOWN_MSG, hostName, hostState);
          break;
      }

      Alert alert = new Alert(definition.getDefinitionName(), null, definition.getServiceName(),
          definition.getComponentName(), hostName, alertState);

      alert.setLabel(definition.getLabel());
      alert.setText(alertText);
      alert.setTimestamp(alertTimestamp);
      alert.setClusterId(cluster.getClusterId());

      alerts.add(alert);
    }

    return alerts;
  }
}
