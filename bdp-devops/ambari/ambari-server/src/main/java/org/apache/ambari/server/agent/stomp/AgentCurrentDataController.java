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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.stomp.dto.Hash;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.AlertDefinitionsAgentUpdateEvent;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.google.inject.Injector;

@Controller
@SendToUser("/")
@MessageMapping("/agents")
public class AgentCurrentDataController {

  private final AgentSessionManager agentSessionManager;
  private final TopologyHolder topologyHolder;
  private final MetadataHolder metadataHolder;
  private final HostLevelParamsHolder hostLevelParamsHolder;
  private final AgentConfigsHolder agentConfigsHolder;
  private final AlertDefinitionsHolder alertDefinitionsHolder;

  public AgentCurrentDataController(Injector injector) {
    agentSessionManager = injector.getInstance(AgentSessionManager.class);
    topologyHolder = injector.getInstance(TopologyHolder.class);
    metadataHolder = injector.getInstance(MetadataHolder.class);
    hostLevelParamsHolder = injector.getInstance(HostLevelParamsHolder.class);
    agentConfigsHolder = injector.getInstance(AgentConfigsHolder.class);
    alertDefinitionsHolder = injector.getInstance(AlertDefinitionsHolder.class);
  }

  @MessageMapping("/topologies")
  public TopologyUpdateEvent getCurrentTopology(Hash hash) throws AmbariException, InvalidStateTransitionException {
    return topologyHolder.getUpdateIfChanged(hash.getHash());
  }

  @MessageMapping("/metadata")
  public MetadataUpdateEvent getCurrentMetadata(Hash hash) throws AmbariException {
    return metadataHolder.getUpdateIfChanged(hash.getHash());
  }

  @MessageMapping("/alert_definitions")
  public AlertDefinitionsAgentUpdateEvent getAlertDefinitions(@Header String simpSessionId, Hash hash) throws AmbariException {
    Long hostId = agentSessionManager.getHost(simpSessionId).getHostId();
    return alertDefinitionsHolder.getUpdateIfChanged(hash.getHash(), hostId);
  }

  @MessageMapping("/configs")
  public AgentConfigsUpdateEvent getCurrentConfigs(@Header String simpSessionId, Hash hash) throws AmbariException {
    return agentConfigsHolder.getUpdateIfChanged(hash.getHash(), agentSessionManager.getHost(simpSessionId).getHostId());
  }

  @MessageMapping("/host_level_params")
  public HostLevelParamsUpdateEvent getCurrentHostLevelParams(@Header String simpSessionId, Hash hash) throws AmbariException {
    return hostLevelParamsHolder.getUpdateIfChanged(hash.getHash(), agentSessionManager.getHost(simpSessionId).getHostId());
  }

}
