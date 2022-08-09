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
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.BulkCommandDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;

import io.swagger.annotations.ApiModelProperty;

/**
 * Stack service component response.
 */
public class StackServiceComponentResponse {
  /**
   * stack name
   */
  private String stackName;

  /**
   * stack version
   */
  private String stackVersion;

  /**
   * service name
   */
  private String serviceName;

  /**
   * component name
   */
  private String componentName;

  /**
   * component display name
   */
  private String componentDisplayName;

  /**
   * component category
   */
  private String componentCategory;

  /**
   * is component a client component
   */
  private boolean isClient;

  /**
   * is component a master component
   */
  private boolean isMaster;

  /**
   * cardinality requirement
   */
  private String cardinality;

  /**
   * does the component need to advertise a version
   */
  private boolean versionAdvertised;

  /**
   * Whether the component can be decommissioned.
   * */
  private String decommissionAllowed;

  /**
   * Whether the component supports rolling restart.
   * */
  private boolean rollingRestartSupported;

  /**
   * auto deploy information
   */
  private AutoDeployInfo autoDeploy;

  /**
   * The names of the custom commands defined for the component.
   */
  private List<String> customCommands;

  /**
   * The names of the custom commands defined for the component which are hidden=false
   */
  private List<String> visibleCustomCommands;

  /**
   * Enabled for auto start or not.
   */
  private boolean recoveryEnabled;

  private String bulkCommandsDisplayName;
  private String bulkCommandMasterComponentName;
  private boolean hasBulkCommands;

  /**
   * Whether the component can be reassigned to a different node.
   * */
  private String reassignAllowed;

  /**
   * @see ComponentInfo#componentType
   */
  private String componentType;

  /**
   * Constructor.
   *
   * @param component
   *          the component to generate the response from (not {@code null}).
   */
  public StackServiceComponentResponse(ComponentInfo component) {
    componentName = component.getName();
    componentDisplayName = component.getDisplayName();
    componentCategory = component.getCategory();
    isClient = component.isClient();
    isMaster = component.isMaster();
    cardinality = component.getCardinality();
    versionAdvertised = component.isVersionAdvertised();
    decommissionAllowed = component.getDecommissionAllowed();
    autoDeploy = component.getAutoDeploy();
    recoveryEnabled = component.isRecoveryEnabled();
    hasBulkCommands = componentHasBulkCommands(component);
    bulkCommandsDisplayName = getBulkCommandsDisplayName(component);
    bulkCommandMasterComponentName = getBulkCommandsMasterComponentName(component);
    reassignAllowed = component.getReassignAllowed();
    rollingRestartSupported = component.getRollingRestartSupported();
    componentType = component.getComponentType();

    // the custom command names defined for this component
    List<CustomCommandDefinition> definitions = component.getCustomCommands();
    if (null == definitions || definitions.size() == 0) {
      customCommands = Collections.emptyList();
      visibleCustomCommands = Collections.emptyList();
    } else {
      customCommands = new ArrayList<>(definitions.size());
      visibleCustomCommands = new ArrayList<>();
      for (CustomCommandDefinition command : definitions) {
        customCommands.add(command.getName());
        if(!command.isHidden()) {
          visibleCustomCommands.add(command.getName());
        }
      }
    }
  }

  /**
   * Get bulk command master component name
   *
   * @param component
   *          the component to generate the response from (not {@code null}).
   * @return master component name
   */
  private String getBulkCommandsMasterComponentName(ComponentInfo component) {
    BulkCommandDefinition o = component.getBulkCommandDefinition();
    if (o == null) {
      return "";
    } else {
      return o.getMasterComponent();
    }
  }

  /**
   * Get the display name shown on the user interface which bulk commands are grouped under
   *
   * @param component
   *          the component to generate the response from (not {@code null}).
   * @return display name of the bulk command group
   */
  private String getBulkCommandsDisplayName(ComponentInfo component) {
    BulkCommandDefinition o = component.getBulkCommandDefinition();
    if (o == null) {
      return "";
    } else {
      return o.getDisplayName();
    }
  }

  /**
   * Determine if the component has bulk commands
   *
   * @param component
   *          the component to generate the response from (not {@code null}).
   * @return boolean
   */
  private boolean componentHasBulkCommands(ComponentInfo component) {
    BulkCommandDefinition o = component.getBulkCommandDefinition();
    if (o == null) {
      return false;
    } else {
      return o.getDisplayName() != null && !o.getDisplayName().trim().isEmpty();
    }
  }

  /**
   * Get stack name.
   *
   * @return stack name
   */
  @ApiModelProperty(name = "stack_name")
  public String getStackName() {
    return stackName;
  }

  /**
   * Set stack name.
   *
   * @param stackName  stack name
   */
  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  /**
   * Get stack version.
   *
   * @return stack version
   */
  @ApiModelProperty(name = "stack_version")
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * Set stack version.
   *
   * @param stackVersion  stack version
   */
  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  /**
   * Get service name.
   *
   * @return service name
   */
  @ApiModelProperty(name = "service_name")
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set service name.
   *
   * @param serviceName  service name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Get component name.
   *
   * @return component name
   */
  @ApiModelProperty(name = "component_name")
  public String getComponentName() {
    return componentName;
  }

  /**
   * Set component name.
   *
   * @param componentName  component name
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * Get component display name.
   *
   * @return component display name
   */

  @ApiModelProperty(name = "display_name")
  public String getComponentDisplayName() {
    return componentDisplayName;
  }

  /**
   * Set component display name.
   *
   * @param componentDisplayName  component display name
   */
  public void setComponentDisplayName(String componentDisplayName) {
    this.componentDisplayName = componentDisplayName;
  }

  /**
   * Get component category.
   *
   * @return component category
   */
  @ApiModelProperty(name = "component_category")
  public String getComponentCategory() {
    return componentCategory;
  }

  /**
   * Set the component category.
   *
   * @param componentCategory  component category
   */
  public void setComponentCategory(String componentCategory) {
    this.componentCategory = componentCategory;
  }

  /**
   * Determine whether the component is a client component.
   *
   * @return whether the component is a client component
   */
  @ApiModelProperty(name = "is_client")
  public boolean isClient() {
    return isClient;
  }

  /**
   * Set whether the component is a client component.
   *
   * @param isClient whether the component is a client
   */
  public void setClient(boolean isClient) {
    this.isClient = isClient;
  }

  /**
   * Determine whether the component is a master component.
   *
   * @return whether the component is a master component
   */
  @ApiModelProperty(name = "is_master")
  public boolean isMaster() {
    return isMaster;
  }

  /**
   * Set whether the component is a master component.
   *
   * @param isMaster whether the component is a master
   */
  public void setMaster(boolean isMaster) {
    this.isMaster = isMaster;
  }

  /**
   * Get cardinality requirement of component.
   *
   * @return component cardinality requirement
   */
  @ApiModelProperty(name = "cardinality")
  public String getCardinality() {
    return cardinality;
  }

  /**
   * Set component cardinality requirement.
   *
   * @param cardinality cardinality requirement
   */
  public void setCardinality(String cardinality) {
    this.cardinality = cardinality;
  }


  /**
   * Get whether the components needs to advertise a version.
   *
   * @return Whether the components needs to advertise a version
   */
  @ApiModelProperty(name = "advertise_version")
  public boolean isVersionAdvertised() {
    return versionAdvertised;
  }

  /**
   * Set whether the component needs to advertise a version.
   *
   * @param versionAdvertised whether the component needs to advertise a version
   */
  public void setVersionAdvertised(boolean versionAdvertised) {
    this.versionAdvertised = versionAdvertised;
  }

  /**
   * Get whether the components can be decommissioned.
   *
   * @return Whether the components can be decommissioned
   */
  @ApiModelProperty(name = "decommission_allowed")
  public boolean isDecommissionAlllowed() {
    if (decommissionAllowed != null && decommissionAllowed.equals("true")) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Set whether the component can be decommissioned.
   *
   * @param decommissionAllowed whether the component can be decommissioned
   */
  public void setDecommissionAllowed(String decommissionAllowed) {
    this.decommissionAllowed = decommissionAllowed;
  }

  /**
   * Get whether the component supports rolling restart
   *
   * @return whether the component supports rolling restart
   */
  @ApiModelProperty(name = "rollingRestartSupported")
  public boolean isRollingRestartSupported(){
    return rollingRestartSupported;
  }

  /**
   * Get whether the components can be reassigned.
   *
   * @return Whether the components can be reassigned
   */
  @ApiModelProperty(name = "reassign_allowed")
  public boolean isReassignAlllowed() {
    if (reassignAllowed != null && reassignAllowed.equals("true")) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Set whether the component can be reassigned.
   *
   * @param reassignAllowed whether the component can be reassigned
   */
  public void setReassignAllowed(String reassignAllowed) {
    this.reassignAllowed = reassignAllowed;
  }

  /**
   * Get whether auto start is enabled.
   *
   * @return True or false.
   */
  @ApiModelProperty(name = "recovery_enabled")
  public boolean isRecoveryEnabled() {
    return recoveryEnabled;
  }

  /**
   * Set whether auto start is enabled.
   *
   * @param recoveryEnabled True or false.
   */
  public void setRecoveryEnabled(boolean recoveryEnabled) {
    this.recoveryEnabled = recoveryEnabled;
  }


  /**
   * Get auto deploy information.
   *
   * @return auto deploy information
   */
  @ApiModelProperty(hidden = true)
  public AutoDeployInfo getAutoDeploy() {
    return autoDeploy;
  }

  /**
   * Set auto deploy information.
   *
   * @param autoDeploy auto deploy info
   */
  public void setAutoDeploy(AutoDeployInfo autoDeploy) {
    this.autoDeploy = autoDeploy;
  }

  /**
   * Gets the names of all of the custom commands for this component.
   *
   * @return the commands or an empty list (never {@code null}).
   */
  public List<String> getCustomCommands() {
    return customCommands;
  }

  @ApiModelProperty(name = "custom_commands")
  public List<String> getVisibleCustomCommands() {
    return visibleCustomCommands;
  }

  @ApiModelProperty(name = "has_bulk_commands_definition")
  public boolean hasBulkCommands(){
    return hasBulkCommands;
  }

  public String getBulkCommandsDisplayName(){
    return bulkCommandsDisplayName == null ? "":bulkCommandsDisplayName;
  }

  @ApiModelProperty(name = "bulk_commands_master_component_namen")
  public String getBulkCommandsMasterComponentName(){
    return bulkCommandMasterComponentName == null ? "":bulkCommandMasterComponentName;
  }

  public String getComponentType() {
    return componentType;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface StackServiceComponentResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "StackServiceComponents")
    public StackServiceComponentResponse getStackServiceComponentResponse();
  }
}
