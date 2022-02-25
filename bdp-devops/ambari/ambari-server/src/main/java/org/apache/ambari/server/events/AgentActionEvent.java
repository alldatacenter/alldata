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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event to send action commands to agent.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AgentActionEvent extends STOMPHostEvent {

  /**
   * Host id with agent action commands will be send to.
   */
  private final Long hostId;

  @JsonProperty("actionName")
  private AgentAction agentAction;

  public AgentActionEvent(AgentAction agentAction, Long hostId) {
    super(Type.AGENT_ACTIONS);
    this.agentAction = agentAction;
    this.hostId = hostId;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  public enum AgentAction {
    RESTART_AGENT
  }
}
