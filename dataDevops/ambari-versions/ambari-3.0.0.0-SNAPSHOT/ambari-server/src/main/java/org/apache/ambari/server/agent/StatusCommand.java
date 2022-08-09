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
import java.util.Map;

import org.apache.ambari.server.state.State;

import com.google.gson.annotations.SerializedName;

/**
 * Command to report the status of a list of services in roles.
 */
public class StatusCommand extends AgentCommand {

  public StatusCommand() {
    super(AgentCommandType.STATUS_COMMAND);
  }

  @SerializedName("clusterName")
  @com.fasterxml.jackson.annotation.JsonProperty("clusterName")
  private String clusterName;

  @SerializedName("serviceName")
  @com.fasterxml.jackson.annotation.JsonProperty("serviceName")
  private String serviceName;

  @SerializedName("role")
  private String role;

  @SerializedName("componentName")
  @com.fasterxml.jackson.annotation.JsonProperty("componentName")
  private String componentName;

  @SerializedName("configurations")
  @com.fasterxml.jackson.annotation.JsonProperty("configurations")
  private Map<String, Map<String, String>> configurations;

  @SerializedName("configurationAttributes")
  @com.fasterxml.jackson.annotation.JsonProperty("configurationAttributes")
  private Map<String, Map<String, Map<String, String>>> configurationAttributes;

  @SerializedName("commandParams")
  @com.fasterxml.jackson.annotation.JsonProperty("commandParams")
  private Map<String, String> commandParams = new HashMap<>();

  @SerializedName("hostLevelParams")
  @com.fasterxml.jackson.annotation.JsonProperty("hostLevelParams")
  private Map<String, String> hostLevelParams = new HashMap<>();

  @SerializedName("hostname")
  @com.fasterxml.jackson.annotation.JsonProperty("hostname")
  private String hostname = null;

  @SerializedName("payloadLevel")
  @com.fasterxml.jackson.annotation.JsonProperty("payloadLevel")
  private StatusCommandPayload payloadLevel = StatusCommandPayload.DEFAULT;

  @SerializedName("desiredState")
  @com.fasterxml.jackson.annotation.JsonProperty("desiredState")
  private State desiredState;

  @SerializedName("hasStaleConfigs")
  @com.fasterxml.jackson.annotation.JsonProperty("hasStaleConfigs")
  private Boolean hasStaleConfigs;

  @SerializedName("executionCommandDetails")
  @com.fasterxml.jackson.annotation.JsonProperty("executionCommandDetails")
  private ExecutionCommand executionCommand;

  public ExecutionCommand getExecutionCommand() {
    return executionCommand;
  }

  public void setExecutionCommand(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(State desiredState) {
    this.desiredState = desiredState;
  }

  public Boolean getHasStaleConfigs() {
    return hasStaleConfigs;
  }

  public void setHasStaleConfigs(Boolean hasStaleConfigs) {
    this.hasStaleConfigs = hasStaleConfigs;
  }

  public StatusCommandPayload getPayloadLevel() {
    return payloadLevel;
  }

  public void setPayloadLevel(StatusCommandPayload payloadLevel) {
    this.payloadLevel = payloadLevel;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  /**
   * Sets both the {@code componentName} and the {@code role}. Status commands
   * use the {@code componentName}, while execution commands use the
   * {@code role}. It's simpler for the Python to just worry about {@code role},
   * so this ensures that both are set.
   *
   * @param componentName
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
    role = componentName;
  }

  public Map<String, Map<String, String>> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Map<String, Map<String, String>> configurations) {
    this.configurations = configurations;
  }

  public Map<String, Map<String, Map<String, String>>> getConfigurationAttributes() {
    return configurationAttributes;
  }

  public void setConfigurationAttributes(Map<String, Map<String, Map<String, String>>> configurationAttributes) {
    this.configurationAttributes = configurationAttributes;
  }

  public Map<String, String> getHostLevelParams() {
    return hostLevelParams;
  }

  public void setHostLevelParams(Map<String, String> params) {
    hostLevelParams = params;
  }

  public Map<String, String> getCommandParams() {
    return commandParams;
  }

  public void setCommandParams(Map<String, String> commandParams) {
    this.commandParams = commandParams;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getHostname() {
    return hostname;
  }

  public String getRole() {
    return role;
  }

  public enum StatusCommandPayload {
    // The minimal payload for status, agent adds necessary details
    MINIMAL,
    // default payload - backward compatible
    DEFAULT,
    // has enough details to construct START or INSTALL commands
    EXECUTION_COMMAND
  }
}
