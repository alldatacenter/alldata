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

import io.swagger.annotations.ApiModelProperty;

/**
 * Used to respond to GET requests for actions
 */
public class ActionResponse {
  
  private String actionName;
  private String actionType;
  private String inputs;
  private String targetService;
  private String targetComponent;
  private String description;
  private String targetType;
  private String defaultTimeout;

  public ActionResponse(String actionName, String actionType, String inputs,
      String targetService, String targetComponent, String description, String targetType,
      String defaultTimeout) {
    setActionName(actionName);
    setActionType(actionType);
    setInputs(inputs);
    setTargetService(targetService);
    setTargetComponent(targetComponent);
    setDescription(description);
    setTargetType(targetType);
    setDefaultTimeout(defaultTimeout);
  }

  @ApiModelProperty(name = ActionRequest.ACTION_NAME)
  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  @ApiModelProperty(name = ActionRequest.ACTION_TYPE)
  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
    this.actionType = actionType;
  }

  @ApiModelProperty(name = ActionRequest.INPUTS)
  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  @ApiModelProperty(name = ActionRequest.TARGET_SERVICE)
  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  @ApiModelProperty(name = ActionRequest.TARGET_COMPONENT)
  public String getTargetComponent() {
    return targetComponent;
  }

  public void setTargetComponent(String targetComponent) {
    this.targetComponent = targetComponent;
  }

  @ApiModelProperty(name = ActionRequest.DESCRIPTION)
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  @ApiModelProperty(name = ActionRequest.TARGET_TYPE)
  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  @ApiModelProperty(name = ActionRequest.DEFAULT_TIMEOUT)
  public String getDefaultTimeout() {
    return defaultTimeout;
  }

  public void setDefaultTimeout(String defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ActionResponse that = (ActionResponse) o;

    if (actionName != null ?
        !actionName.equals(that.actionName) : that.actionName != null) {
      return false;
    }

    if (actionType != null ?
        !actionType.equals(that.actionType) : that.actionType != null) {
      return false;
    }

    if (description != null ?
        !description.equals(that.description) : that.description != null) {
      return false;
    }

    if (inputs != null ?
        !inputs.equals(that.inputs) : that.inputs != null) {
      return false;
    }

    if (targetService != null ?
        !targetService.equals(that.targetService) : that.targetService != null) {
      return false;
    }

    if (targetComponent != null ?
        !targetComponent.equals(that.targetComponent) : that.targetComponent != null) {
      return false;
    }

    if (targetType != null ?
        !targetType.equals(that.targetType) : that.targetType != null) {
      return false;
    }

    if (defaultTimeout != null ?
        !defaultTimeout.equals(that.defaultTimeout) : that.defaultTimeout != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = 31 + (actionName != null ? actionName.hashCode() : 0);
    result = result + (actionType != null ? actionType.hashCode() : 0);
    result = result + (inputs != null ? inputs.hashCode() : 0);
    result = result + (description != null ? description.hashCode() : 0);
    result = result + (targetService != null ? targetService.hashCode() : 0);
    result = result + (targetComponent != null ? targetComponent.hashCode() : 0);
    result = result + (targetType != null ? targetType.hashCode() : 0);
    result = result + (defaultTimeout != null ? defaultTimeout.hashCode() : 0);
    return result;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ActionResponseSwagger {
    @ApiModelProperty(name = "Actions")
    ActionResponse getActionResponse();
  }
}
