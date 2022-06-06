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

import org.apache.ambari.server.agent.stomp.StompResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;

/**
 * Controller to Agent response data model.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HeartBeatResponse extends StompResponse {

  @SerializedName("responseId")
  @com.fasterxml.jackson.annotation.JsonProperty("id")
  private long responseId;

  @SerializedName("executionCommands")
  @JsonIgnore
  private List<ExecutionCommand> executionCommands = new ArrayList<>();

  @SerializedName("statusCommands")
  @JsonIgnore
  private List<StatusCommand> statusCommands = new ArrayList<>();

  @SerializedName("cancelCommands")
  @JsonIgnore
  private List<CancelCommand> cancelCommands = new ArrayList<>();

  /**
   * {@link AlertDefinitionCommand}s are used to isntruct the agent as to which
   * alert definitions it needs to schedule. A {@code null} value here indicates
   * that no data is to be sent and no change is required on the agent. This is
   * different from sending an empty list where the empty list would instruct
   * the agent to abandon all alert definitions that are scheduled.
   */
  @SerializedName("alertDefinitionCommands")
  @JsonIgnore
  private List<AlertDefinitionCommand> alertDefinitionCommands = null;

  /**
   * {@link AlertExecutionCommand}s are used to execute an alert job
   * immediately.
   */
  @SerializedName("alertExecutionCommands")
  @JsonIgnore
  private List<AlertExecutionCommand> alertExecutionCommands = null;

  @SerializedName("registrationCommand")
  @JsonIgnore
  private RegistrationCommand registrationCommand;

  @SerializedName("restartAgent")
  @com.fasterxml.jackson.annotation.JsonProperty("restartAgent")
  private Boolean restartAgent = null;

  @SerializedName("hasMappedComponents")
  @JsonIgnore
  private boolean hasMappedComponents = false;

  @SerializedName("hasPendingTasks")
  @JsonIgnore
  private boolean hasPendingTasks = false;

  @SerializedName("recoveryConfig")
  @JsonIgnore
  private RecoveryConfig recoveryConfig;

  @SerializedName("clusterSize")
  @JsonIgnore
  private int clusterSize = -1;

  public long getResponseId() {
    return responseId;
  }

  public void setResponseId(long responseId) {
    this.responseId=responseId;
  }

  public List<ExecutionCommand> getExecutionCommands() {
    return executionCommands;
  }

  public void setExecutionCommands(List<ExecutionCommand> executionCommands) {
    this.executionCommands = executionCommands;
  }

  public List<StatusCommand> getStatusCommands() {
    return statusCommands;
  }

  public void setStatusCommands(List<StatusCommand> statusCommands) {
    this.statusCommands = statusCommands;
  }

  public List<CancelCommand> getCancelCommands() {
    return cancelCommands;
  }

  public void setCancelCommands(List<CancelCommand> cancelCommands) {
    this.cancelCommands = cancelCommands;
  }

  public RegistrationCommand getRegistrationCommand() {
    return registrationCommand;
  }

  public void setRegistrationCommand(RegistrationCommand registrationCommand) {
    this.registrationCommand = registrationCommand;
  }

  /**
   * Get the recovery configuration settings for this host. The configuration is set whenever
   * any value changes. The agent uses this information to update the values on it's side.
   *
   * @return Null if nothing changed since the last update, else updated configuration.
   */
  public RecoveryConfig getRecoveryConfig() {
    return recoveryConfig;
  }

  /**
   * Set the recovery configuration. Set only when one or more recovery values change. This
   * is to avoid sending the configuration to the agent during every heartbeat.
   *
   * @param recoveryConfig
   */
  public void setRecoveryConfig(RecoveryConfig recoveryConfig) {
    this.recoveryConfig = recoveryConfig;
  }

  /**
   * Gets the alert definition commands that contain the alert definitions for
   * each cluster that the host is a member of.
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

  public Boolean isRestartAgent() {
    return restartAgent;
  }

  public void setRestartAgent(Boolean restartAgent) {
    this.restartAgent = restartAgent;
  }

  public boolean hasMappedComponents() {
    return hasMappedComponents;
  }

  public void setHasMappedComponents(boolean hasMappedComponents) {
    this.hasMappedComponents = hasMappedComponents;
  }

  public boolean hasPendingTasks() {
    return hasPendingTasks;
  }

  public void setHasPendingTasks(boolean hasPendingTasks) {
    this.hasPendingTasks = hasPendingTasks;
  }

  public void addExecutionCommand(ExecutionCommand execCmd) {
    executionCommands.add(execCmd);
  }

  public void addStatusCommand(StatusCommand statCmd) {
    statusCommands.add(statCmd);
  }

  public void addCancelCommand(CancelCommand cancelCmd) {
    cancelCommands.add(cancelCmd);
  }

  public void addAlertDefinitionCommand(AlertDefinitionCommand command) {
    // commands are added here when they are taken off the queue; there should
    // be no thread contention and thus no worry about locks for the null check
    if (null == alertDefinitionCommands) {
      alertDefinitionCommands = new ArrayList<>();
    }

    alertDefinitionCommands.add(command);
  }

  public void addAlertExecutionCommand(AlertExecutionCommand command) {
    // commands are added here when they are taken off the queue; there should
    // be no thread contention and thus no worry about locks for the null check
    if (null == alertExecutionCommands) {
      alertExecutionCommands = new ArrayList<>();
    }

    alertExecutionCommands.add(command);
  }

  public void setClusterSize(int clusterSize) {
    this.clusterSize = clusterSize;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("HeartBeatResponse{");
    buffer.append("responseId=").append(responseId);
    buffer.append(", executionCommands=").append(executionCommands);
    buffer.append(", statusCommands=").append(statusCommands);
    buffer.append(", cancelCommands=").append(cancelCommands);
    buffer.append(", alertDefinitionCommands=").append(alertDefinitionCommands);
    buffer.append(", registrationCommand=").append(registrationCommand);
    buffer.append(", restartAgent=").append(restartAgent);
    buffer.append(", recoveryConfig=").append(recoveryConfig);
    buffer.append('}');
    return buffer.toString();
  }
}
