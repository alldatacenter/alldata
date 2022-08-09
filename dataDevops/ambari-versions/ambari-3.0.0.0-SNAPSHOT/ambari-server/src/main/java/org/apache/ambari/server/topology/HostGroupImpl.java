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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;

import com.google.gson.Gson;

/**
 * Host Group implementation.
 */
public class HostGroupImpl implements HostGroup {

  /**
   * host group name
   */
  private String name;

  /**
   * blueprint name
   */
  private String blueprintName;

  /**
   * components contained in the host group
   */
  private Map<String, Component> components = new HashMap<>();

  /**
   * map of service to components for the host group
   */
  private Map<String, Set<String>> componentsForService = new HashMap<>();

  /**
   * configuration
   */
  private Configuration configuration = null;

  private boolean containsMasterComponent = false;

  private Stack stack;

  private String cardinality = "NOT SPECIFIED";

  public HostGroupImpl(HostGroupEntity entity, String blueprintName, Stack stack) {
    this.name = entity.getName();
    this.cardinality = entity.getCardinality();
    this.blueprintName = blueprintName;
    this.stack = stack;

    parseComponents(entity);
    parseConfigurations(entity);
  }

  public HostGroupImpl(String name, String bpName, Stack stack, Collection<Component> components, Configuration configuration, String cardinality) {
    this.name = name;
    this.blueprintName = bpName;
    this.stack = stack;

    // process each component
    for (Component component : components) {
      addComponent(component.getName(), component.getProvisionAction());
    }

    this.configuration = configuration;
    if (cardinality != null && ! cardinality.equals("null")) {
      this.cardinality = cardinality;
    }
  }


  @Override
  public String getName() {
    return name;
  }

  //todo: currently not qualifying host group name
  @Override
  public String getFullyQualifiedName() {
    return String.format("%s:%s", blueprintName, getName());
  }

  //todo: currently not qualifying host group name
  public static String formatAbsoluteName(String bpName, String hgName) {
    return String.format("%s:%s", bpName, hgName);
  }

  @Override
  public Collection<Component> getComponents() {
    return components.values();
  }

  @Override
  public Collection<String> getComponentNames() {
    return components.keySet();
  }

  @Override
  public Collection<String> getComponentNames(ProvisionAction provisionAction) {
    Set<String> setOfComponentNames = new HashSet<>();
    for (String componentName : components.keySet()) {
      Component component = components.get(componentName);
      if ( (component.getProvisionAction() != null) && (component.getProvisionAction() == provisionAction) ) {
        setOfComponentNames.add(componentName);
      }
    }

    return setOfComponentNames;
  }


  /**
   * Get the services which are deployed to this host group.
   *
   * @return collection of services which have components in this host group
   */
  @Override
  public Collection<String> getServices() {
    return componentsForService.keySet();
  }

  /**
   * Add a component to the host group.
   *
   * @param component  component to add
   *
   * @return true if component was added; false if component already existed
   */
  @Override
  public boolean addComponent(String component) {
    return this.addComponent(component, null);
  }

  /**
   * Add a component with the specified provision action to the
   *   host group.
   *
   * @param component  component name
   * @param provisionAction provision action for this component
   *
   * @return true if component was added; false if component already existed
   */
  @Override
  public boolean addComponent(String component, ProvisionAction provisionAction) {
    boolean added;
    if (!components.containsKey(component)) {
      components.put(component, new Component(component, provisionAction));
      added = true;
    } else {
      added = false;
    }

    if (stack.isMasterComponent(component)) {
      containsMasterComponent = true;
    }
    if (added) {
      String service = stack.getServiceForComponent(component);
      if (service != null) {
        // an example of a component without a service in the stack is AMBARI_SERVER
        Set<String> serviceComponents = componentsForService.get(service);
        if (serviceComponents == null) {
          serviceComponents = new HashSet<>();
          componentsForService.put(service, serviceComponents);
        }
        serviceComponents.add(component);
      }
    }
    return added;
  }

  /**
   * Get the components for the specified service which are associated with the host group.
   *
   * @param service  service name
   *
   * @return set of component names
   */
  @Override
  public Collection<String> getComponents(String service) {
    return componentsForService.containsKey(service) ?
      new HashSet<>(componentsForService.get(service)) :
        Collections.emptySet();
  }

  /**
   * Get this host groups configuration.
   *
   * @return configuration instance
   */
  @Override
  public Configuration getConfiguration() {

    return configuration;
  }

  /**
   * Get the associated blueprint name.
   *
   * @return  associated blueprint name
   */
  @Override
  public String getBlueprintName() {
    return blueprintName;
  }

  @Override
  public boolean containsMasterComponent() {
    return containsMasterComponent;
  }

  @Override
  public Stack getStack() {
    return stack;
  }

  @Override
  public String getCardinality() {
    return cardinality;
  }

  /**
   * Parse component information.
   */
  private void parseComponents(HostGroupEntity entity) {
    for (HostGroupComponentEntity componentEntity : entity.getComponents() ) {
      if (componentEntity.getProvisionAction() != null) {
        addComponent(componentEntity.getName(), ProvisionAction.valueOf(componentEntity.getProvisionAction()));
      } else {
        addComponent(componentEntity.getName());
      }


    }
  }

  /**
   * Parse host group configurations.
   */
  //todo: use ConfigurationFactory
  private void parseConfigurations(HostGroupEntity entity) {
    Map<String, Map<String, String>> config = new HashMap<>();
    Gson jsonSerializer = new Gson();
    for (HostGroupConfigEntity configEntity : entity.getConfigurations()) {
      String type = configEntity.getType();
      Map<String, String> typeProperties = config.get(type);
      if ( typeProperties == null) {
        typeProperties = new HashMap<>();
        config.put(type, typeProperties);
      }
      Map<String, String> propertyMap =  jsonSerializer.<Map<String, String>>fromJson(
          configEntity.getConfigData(), Map.class);

      if (propertyMap != null) {
        typeProperties.putAll(propertyMap);
      }
    }
    //todo: parse attributes
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    configuration = new Configuration(config, attributes);
  }
  @Override
  public String toString(){
       return  name;
  }
}
