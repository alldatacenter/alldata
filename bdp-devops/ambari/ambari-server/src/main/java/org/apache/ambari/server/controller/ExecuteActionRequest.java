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
package org.apache.ambari.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;

/**
 * Helper class to capture details used to create action or custom commands
 */
public class ExecuteActionRequest {
  private final String clusterName;
  private final String commandName;
  private final List<RequestResourceFilter> resourceFilters;
  private RequestOperationLevel operationLevel = null;
  private String actionName;
  private Map<String, String> parameters;
  private boolean exclusive;

  public ExecuteActionRequest(String clusterName, String commandName,
                              String actionName,
                              List<RequestResourceFilter> resourceFilters,
                              RequestOperationLevel operationLevel,
                              Map<String, String> parameters,
                              boolean exclusive) {
    this(clusterName, commandName, parameters, exclusive);
    this.actionName = actionName;
    if (resourceFilters != null) {
      this.resourceFilters.addAll(resourceFilters);
    }
    this.operationLevel = operationLevel;
  }

  /**
   * Create an ExecuteActionRequest to execute a command.
   * No filters.
   */
  public ExecuteActionRequest(String clusterName, String commandName,
                              Map<String, String> parameters,
                              boolean exclusive) {
    this.clusterName = clusterName;
    this.commandName = commandName;
    this.actionName = null;
    this.parameters = new HashMap<>();
    if (parameters != null) {
      this.parameters.putAll(parameters);
    }
    this.resourceFilters = new ArrayList<>();
    this.exclusive = exclusive;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getCommandName() {
    return commandName;
  }

  public String getActionName() {
    return actionName;
  }

  public List<RequestResourceFilter> getResourceFilters() {
    return resourceFilters;
  }

  public RequestOperationLevel getOperationLevel() {
    return operationLevel;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean isExclusive) {
    this.exclusive = isExclusive;
  }

  public Boolean isCommand() {
    return actionName == null || actionName.isEmpty();
  }

  @Override
  public synchronized String toString() {
    return new StringBuilder()
      .append("isCommand :").append(isCommand())
      .append(", action :").append(actionName)
      .append(", command :").append(commandName)
      .append(", inputs :").append(parameters)
      .append(", resourceFilters: ").append(resourceFilters)
      .append(", exclusive: ").append(exclusive)
      .append(", clusterName :").append(clusterName).toString();
  }
}
