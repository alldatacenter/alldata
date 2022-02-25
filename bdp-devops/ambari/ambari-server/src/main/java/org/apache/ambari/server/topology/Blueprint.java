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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.ServiceInfo;

/**
 * Blueprint representation.
 */
public interface Blueprint {

  /**
   * Get the name of the blueprint.
   *
   * @return blueprint name
   */
  String getName();

  /**
   * Get the hot groups contained in the blueprint.
   * @return map of host group name to host group
   */
  Map<String, HostGroup> getHostGroups();

  /**
   * Get a hostgroup specified by name.
   *
   * @param name  name of the host group to get
   *
   * @return the host group with the given name or null
   */
  HostGroup getHostGroup(String name);

  /**
   * Get the Blueprint cluster scoped configuration.
   * The blueprint cluster scoped configuration has the stack
   * configuration with the config types associated with the blueprint
   * set as it's parent.
   *
   * @return blueprint cluster scoped configuration
   */
  Configuration getConfiguration();

  /**
   * Get the Blueprint cluster scoped setting.
   * The blueprint cluster scoped setting has the setting properties
   * with the setting names associated with the blueprint.
   *
   * @return blueprint cluster scoped setting
   */
  Setting getSetting();

  /**
   * Get all of the services represented in the blueprint.
   *
   * @return collection of all represented service names
   */
  Collection<String> getServices();

  /**
  * @return collection of all service infos
   */
  Collection<ServiceInfo> getServiceInfos();

  /**
   * Get the components that are included in the blueprint for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the service.  Will not return null.
   */
  Collection<String> getComponents(String service);

  /**
   * Get whether a component is enabled for auto start.
   *
   * @param serviceName - Service name.
   * @param componentName - Component name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  String getRecoveryEnabled(String serviceName, String componentName);

  /**
   * Get whether a service is enabled for credential store use.
   *
   * @param serviceName - Service name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  String getCredentialStoreEnabled(String serviceName);

  /**
   * Check if auto skip failure is enabled.
   * @return true if enabled, otherwise false.
   */
  boolean shouldSkipFailure();

  /**
   * Get the stack associated with the blueprint.
   *
   * @return associated stack
   */
  Stack getStack();

  /**
   * Get the host groups which contain components for the specified service.
   *
   * @param service  service name
   *
   * @return collection of host groups containing components for the specified service;
   *         will not return null
   */
  Collection<HostGroup> getHostGroupsForService(String service);

  /**
   * Get the host groups which contain the give component.
   *
   * @param component  component name
   *
   * @return collection of host groups containing the specified component; will not return null
   */
  Collection<HostGroup> getHostGroupsForComponent(String component);

  SecurityConfiguration getSecurity();

  /**
   * Validate the blueprint topology.
   *
   * @throws InvalidTopologyException if the topology is invalid
   */
  void validateTopology() throws InvalidTopologyException;

  /**
   * Validate that the blueprint contains all of the required properties.
   *
   * @throws InvalidTopologyException if the blueprint doesn't contain all required properties
   */
  void validateRequiredProperties() throws InvalidTopologyException, GPLLicenseNotAcceptedException;

  /**
   *
   * A config type is valid if there are services related to except cluster-env and global.
   * @param configType
   * @return
   */
  boolean isValidConfigType(String configType);

  /**
   * Obtain the blueprint as an entity.
   *
   * @return entity representation of the blueprint
   */
  BlueprintEntity toEntity();

  List<RepositorySetting> getRepositorySettings();
}
