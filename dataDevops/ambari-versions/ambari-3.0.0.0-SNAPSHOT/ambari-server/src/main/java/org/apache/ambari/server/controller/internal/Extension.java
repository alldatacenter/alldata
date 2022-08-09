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

package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.topology.Cardinality;

/**
 * Encapsulates extension information.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
public class Extension {
  /**
   * Extension name
   */
  private String name;

  /**
   * Extension version
   */
  private String version;

  /**
   * Map of service name to components
   */
  private Map<String, Collection<String>> serviceComponents =
    new HashMap<>();

  /**
   * Map of component to service
   */
  private Map<String, String> componentService = new HashMap<>();

  /**
   * Map of component to dependencies
   */
  private Map<String, Collection<DependencyInfo>> dependencies =
    new HashMap<>();

  /**
   * Map of dependency to conditional service
   */
  private Map<DependencyInfo, String> dependencyConditionalServiceMap =
    new HashMap<>();

  /**
   * Map of database component name to configuration property which indicates whether
   * the database in to be managed or if it is an external non-managed instance.
   * If the value of the config property starts with 'New', the database is determined
   * to be managed, otherwise it is non-managed.
   */
  private Map<String, String> dbDependencyInfo = new HashMap<>();

  /**
   * Map of component to required cardinality
   */
  private Map<String, String> cardinalityRequirements = new HashMap<>();

  /**
   * Map of component to auto-deploy information
   */
  private Map<String, AutoDeployInfo> componentAutoDeployInfo =
    new HashMap<>();

  /**
   * Ambari Management Controller, used to obtain Extension definitions
   */
  private final AmbariManagementController controller;


  /**
   * Constructor.
   *
   * @param extension
   *          the extension (not {@code null}).
   * @param ambariManagementController
   *          the management controller (not {@code null}).
   * @throws AmbariException
   */
  public Extension(ExtensionEntity extension, AmbariManagementController ambariManagementController) throws AmbariException {
    this(extension.getExtensionName(), extension.getExtensionVersion(), ambariManagementController);
  }

  /**
   * Constructor.
   *
   * @param name     extension name
   * @param version  extension version
   *
   * @throws AmbariException an exception occurred getting extension information
   *                         for the specified name and version
   */
  //todo: don't pass management controller in constructor
  public Extension(String name, String version, AmbariManagementController controller) throws AmbariException {
    this.name = name;
    this.version = version;
    this.controller = controller;
  }

  /**
   * Obtain extension name.
   *
   * @return extension name
   */
  public String getName() {
    return name;
  }

  /**
   * Obtain extension version.
   *
   * @return extension version
   */
  public String getVersion() {
    return version;
  }


  Map<DependencyInfo, String> getDependencyConditionalServiceMap() {
    return dependencyConditionalServiceMap;
  }

  /**
   * Get services contained in the extension.
   *
   * @return collection of all services for the extension
   */
  public Collection<String> getServices() {
    return serviceComponents.keySet();
  }

  /**
   * Get components contained in the extension for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the specified service
   */
  public Collection<String> getComponents(String service) {
    return serviceComponents.get(service);
  }

  /**
   * Get all service components
   *
   * @return map of service to associated components
   */
  public Map<String, Collection<String>> getComponents() {
    Map<String, Collection<String>> serviceComponents = new HashMap<>();
    for (String service : getServices()) {
      Collection<String> components = new HashSet<>();
      components.addAll(getComponents(service));
      serviceComponents.put(service, components);
    }
    return serviceComponents;
  }

  /**
   * Get info for the specified component.
   *
   * @param component  component name
   *
   * @return component information for the requested component
   *         or null if the component doesn't exist in the extension
   */
  public ComponentInfo getComponentInfo(String component) {
    ComponentInfo componentInfo = null;
    String service = getServiceForComponent(component);
    if (service != null) {
      try {
        componentInfo = controller.getAmbariMetaInfo().getComponent(
            getName(), getVersion(), service, component);
      } catch (AmbariException e) {
        // just return null if component doesn't exist
      }
    }
    return componentInfo;
  }

  /**
   * Get the service for the specified component.
   *
   * @param component  component name
   *
   * @return service name that contains tha specified component
   */
  public String getServiceForComponent(String component) {
    return componentService.get(component);
  }

  /**
   * Get the names of the services which contains the specified components.
   *
   * @param components collection of components
   *
   * @return collection of services which contain the specified components
   */
  public Collection<String> getServicesForComponents(Collection<String> components) {
    Set<String> services = new HashSet<>();
    for (String component : components) {
      services.add(getServiceForComponent(component));
    }

    return services;
  }

  /**
   * Return the dependencies specified for the given component.
   *
   * @param component  component to get dependency information for
   *
   * @return collection of dependency information for the specified component
   */
  //todo: full dependency graph
  public Collection<DependencyInfo> getDependenciesForComponent(String component) {
    return dependencies.containsKey(component) ? dependencies.get(component) :
        Collections.emptySet();
  }

  /**
   * Get the service, if any, that a component dependency is conditional on.
   *
   * @param dependency  dependency to get conditional service for
   *
   * @return conditional service for provided component or null if dependency
   *         is not conditional on a service
   */
  public String getConditionalServiceForDependency(DependencyInfo dependency) {
    return dependencyConditionalServiceMap.get(dependency);
  }

  public String getExternalComponentConfig(String component) {
    return dbDependencyInfo.get(component);
  }

  /**
   * Obtain the required cardinality for the specified component.
   */
  public Cardinality getCardinality(String component) {
    return new Cardinality(cardinalityRequirements.get(component));
  }

  /**
   * Obtain auto-deploy information for the specified component.
   */
  public AutoDeployInfo getAutoDeployInfo(String component) {
    return componentAutoDeployInfo.get(component);
  }
}
