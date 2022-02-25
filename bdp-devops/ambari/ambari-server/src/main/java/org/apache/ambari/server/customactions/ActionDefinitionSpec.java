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

package org.apache.ambari.server.customactions;

public class ActionDefinitionSpec {
  private String actionName;
  private String actionType;
  private String inputs;
  private String targetService;
  private String targetComponent;
  private String description;
  private String targetType;
  private String defaultTimeout;
  private String permissions;

  public String getTargetComponent() {
    return targetComponent;
  }

  public void setTargetComponent(String targetComponent) {
    this.targetComponent = targetComponent;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getDefaultTimeout() {
    return defaultTimeout;
  }

  public void setDefaultTimeout(String defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
    this.actionType = actionType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getPermissions() {
    return permissions;
  }

  public void setPermissions(String permissions) {
    this.permissions = permissions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((actionName == null) ? 0 : actionName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ActionDefinitionSpec other = (ActionDefinitionSpec) obj;
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("ActionDefinition:")
        .append(" actionName: ").append(actionName)
        .append(" actionType: ").append(actionType)
        .append(" inputs: ").append(inputs)
        .append(" description: ").append(description)
        .append(" targetService: ").append(targetService)
        .append(" targetComponent: ").append(targetComponent)
        .append(" defaultTimeout: ").append(defaultTimeout)
        .append(" targetType: ").append(targetType)
        .append(" permissions: ").append(permissions)
        .toString();
  }
}
