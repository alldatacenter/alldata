/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.events;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.MessageDestinationIsNotDefinedException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.commons.lang.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class DefaultMessageEmitter extends MessageEmitter {
  private final Map<STOMPEvent.Type, String> DEFAULT_DESTINATIONS =
      Collections.unmodifiableMap(new HashMap<STOMPEvent.Type, String>(){{
        put(STOMPEvent.Type.ALERT, "/events/alerts");
        put(STOMPEvent.Type.ALERT_GROUP, "/events/alert_group");
        put(STOMPEvent.Type.METADATA, "/events/metadata");
        put(STOMPEvent.Type.HOSTLEVELPARAMS, "/host_level_params");
        put(STOMPEvent.Type.UI_TOPOLOGY, "/events/ui_topologies");
        put(STOMPEvent.Type.AGENT_TOPOLOGY, "/events/topologies");
        put(STOMPEvent.Type.AGENT_CONFIGS, "/configs");
        put(STOMPEvent.Type.CONFIGS, "/events/configs");
        put(STOMPEvent.Type.HOSTCOMPONENT, "/events/hostcomponents");
        put(STOMPEvent.Type.NAMEDTASK, "/events/tasks");
        put(STOMPEvent.Type.REQUEST, "/events/requests");
        put(STOMPEvent.Type.SERVICE, "/events/services");
        put(STOMPEvent.Type.HOST, "/events/hosts");
        put(STOMPEvent.Type.COMMAND, "/commands");
        put(STOMPEvent.Type.ALERT_DEFINITIONS, "/alert_definitions");
        put(STOMPEvent.Type.UI_ALERT_DEFINITIONS, "/events/alert_definitions");
        put(STOMPEvent.Type.UPGRADE, "/events/upgrade");
        put(STOMPEvent.Type.AGENT_ACTIONS, "/agent_actions");
        put(STOMPEvent.Type.ENCRYPTION_KEY_UPDATE, "/events/encryption_key");
  }});
  public static final Set<STOMPEvent.Type> DEFAULT_AGENT_EVENT_TYPES =
      Collections.unmodifiableSet(new HashSet<STOMPEvent.Type>(Arrays.asList(
        STOMPEvent.Type.METADATA,
        STOMPEvent.Type.HOSTLEVELPARAMS,
        STOMPEvent.Type.AGENT_TOPOLOGY,
        STOMPEvent.Type.AGENT_CONFIGS,
        STOMPEvent.Type.COMMAND,
        STOMPEvent.Type.ALERT_DEFINITIONS,
        STOMPEvent.Type.AGENT_ACTIONS,
        STOMPEvent.Type.ENCRYPTION_KEY_UPDATE
  )));
  public static final Set<STOMPEvent.Type> DEFAULT_API_EVENT_TYPES =
      Collections.unmodifiableSet(new HashSet<STOMPEvent.Type>(Arrays.asList(
        STOMPEvent.Type.ALERT,
        STOMPEvent.Type.ALERT_GROUP,
        STOMPEvent.Type.METADATA,
        STOMPEvent.Type.UI_TOPOLOGY,
        STOMPEvent.Type.CONFIGS,
        STOMPEvent.Type.HOSTCOMPONENT,
        STOMPEvent.Type.NAMEDTASK,
        STOMPEvent.Type.REQUEST,
        STOMPEvent.Type.SERVICE,
        STOMPEvent.Type.HOST,
        STOMPEvent.Type.UI_ALERT_DEFINITIONS,
        STOMPEvent.Type.UPGRADE
  )));

  public DefaultMessageEmitter(AgentSessionManager agentSessionManager, SimpMessagingTemplate simpMessagingTemplate,
                               AmbariEventPublisher ambariEventPublisher, int retryCount, int retryInterval) {
    super(agentSessionManager, simpMessagingTemplate, ambariEventPublisher, retryCount, retryInterval);
  }

  @Override
  public void emitMessage(STOMPEvent event) throws AmbariException {
    if (StringUtils.isEmpty(getDestination(event))) {
      throw new MessageDestinationIsNotDefinedException(event.getType());
    }
    if (event instanceof STOMPHostEvent) {
      STOMPHostEvent hostUpdateEvent = (STOMPHostEvent) event;
      if (hostUpdateEvent.getType().equals(STOMPEvent.Type.COMMAND)) {
        emitMessageRetriable((ExecutionCommandEvent) hostUpdateEvent);
      } else {
        emitMessageToHost(hostUpdateEvent);
      }
    } else {
      emitMessageToAll(event);
    }
  }

  @Override
  protected String getDestination(STOMPEvent stompEvent) {
    return stompEvent.completeDestination(DEFAULT_DESTINATIONS.get(stompEvent.getType()));
  }
}
