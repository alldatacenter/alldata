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
import java.util.Map;

import org.apache.ambari.server.agent.stomp.StompResponse;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * Controller to Agent response data model.
 *
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RegistrationResponse extends StompResponse {
  @JsonProperty("response")
  @JsonIgnore
  private RegistrationStatus response;

  /**
   * {@link AlertDefinitionCommand}s are used to isntruct the agent as to which
   * alert definitions it needs to schedule.
   */
  @JsonProperty("alertDefinitionCommands")
  @JsonIgnore
  private List<AlertDefinitionCommand> alertDefinitionCommands = new ArrayList<>();

  /**
   * exitstatus is a code of error which was rised on server side.
   * exitstatus = 0 (OK - Default)
   * exitstatus = 1 (Registration failed because
   *                different version of agent and server)
   */
  @JsonProperty("exitstatus")
  @com.fasterxml.jackson.annotation.JsonProperty("exitstatus")
  private int exitstatus;

  /**
   * log - message, which will be printed to agents  log
   */
  @JsonProperty("log")
  @com.fasterxml.jackson.annotation.JsonProperty("log")
  private String log;

  //Response id to start with, usually zero.
  @JsonProperty("responseId")
  @com.fasterxml.jackson.annotation.JsonProperty("id")
  private long responseId;

  @JsonProperty("recoveryConfig")
  @JsonIgnore
  private RecoveryConfig recoveryConfig;

  @JsonProperty("agentConfig")
  @JsonIgnore
  private Map<String, String> agentConfig;

  @JsonProperty("statusCommands")
  @JsonIgnore
  private List<StatusCommand> statusCommands = null;

  @JsonIgnore
  public RegistrationStatus getResponseStatus() {
    return response;
  }

  public void setResponseStatus(RegistrationStatus response) {
    this.response = response;
  }

  public List<StatusCommand> getStatusCommands() {
    return statusCommands;
  }

  public void setStatusCommands(List<StatusCommand> statusCommands) {
    this.statusCommands = statusCommands;
  }

  /**
   * Gets the alert definition commands that contain the alert definitions for
   * each cluster that the host is a member of.
   *
   *          the commands, or {@code null} for none.
   */
  public List<AlertDefinitionCommand> getAlertDefinitionCommands() {
    return alertDefinitionCommands;
  }

  /**
   * Sets the alert definition commands that contain the alert definitions for
   * each cluster that the host is a member of.
   *
   * @param commands
   *          the commands, or {@code null} for none.
   */
  public void setAlertDefinitionCommands(List<AlertDefinitionCommand> commands) {
    alertDefinitionCommands = commands;
  }

  public long getResponseId() {
    return responseId;
  }

  public void setResponseId(long responseId) {
    this.responseId = responseId;
  }

  public void setExitstatus(int exitstatus) {
    this.exitstatus = exitstatus;
  }

  public void setLog(String log) {
    this.log = log;
  }

  public RecoveryConfig getRecoveryConfig() {
    return recoveryConfig;
  }

  public void setRecoveryConfig(RecoveryConfig recoveryConfig) {
    this.recoveryConfig = recoveryConfig;
  }

  public Map<String, String> getAgentConfig() {
    return agentConfig;
  }

  public void setAgentConfig(Map<String, String> agentConfig) {
    this.agentConfig = agentConfig;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("RegistrationResponse{");
    buffer.append("response=").append(response);
    buffer.append(", responseId=").append(responseId);
    buffer.append(", statusCommands=").append(statusCommands);
    buffer.append(", alertDefinitionCommands=").append(alertDefinitionCommands);
    buffer.append(", recoveryConfig=").append(recoveryConfig);
    buffer.append(", agentConfig=").append(agentConfig);
    buffer.append('}');
    return buffer.toString();
  }
}
