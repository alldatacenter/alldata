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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.stack.NoSuchStackException;

import com.google.inject.Inject;

/**
 * Create a Blueprint instance.
 */
public class BlueprintFactory {

  // Blueprints
  protected static final String BLUEPRINT_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "blueprint_name");
  protected static final String STACK_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "stack_name");
  protected static final String STACK_VERSION_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "stack_version");

  // Host Groups
  protected static final String HOST_GROUP_PROPERTY_ID = "host_groups";
  protected static final String HOST_GROUP_NAME_PROPERTY_ID = "name";
  protected static final String HOST_GROUP_CARDINALITY_PROPERTY_ID = "cardinality";

  // Host Group Components
  protected static final String COMPONENT_PROPERTY_ID ="components";
  protected static final String COMPONENT_NAME_PROPERTY_ID ="name";
  protected static final String COMPONENT_PROVISION_ACTION_PROPERTY_ID = "provision_action";

  // Configurations
  protected static final String CONFIGURATION_PROPERTY_ID = "configurations";
  protected static final String PROPERTIES_PROPERTY_ID = "properties";
  protected static final String PROPERTIES_ATTRIBUTES_PROPERTY_ID = "properties_attributes";

  protected static final String SETTINGS_PROPERTY_ID = "settings";

  private static BlueprintDAO blueprintDAO;
  private ConfigurationFactory configFactory = new ConfigurationFactory();

  private final StackFactory stackFactory;

  public BlueprintFactory() {
    this(new DefaultStackFactory());
  }

  protected BlueprintFactory(StackFactory stackFactory) {
    this.stackFactory = stackFactory;
  }

  public Blueprint getBlueprint(String blueprintName) throws NoSuchStackException {
    BlueprintEntity entity = blueprintDAO.findByName(blueprintName);
    //todo: just return null?
    return entity == null ? null : new BlueprintImpl(entity);
  }

  /**
   * Convert a map of properties to a blueprint entity.
   *
   * @param properties  property map
   * @param securityConfiguration security related properties
   * @return new blueprint entity
   */
  @SuppressWarnings("unchecked")
  public Blueprint createBlueprint(Map<String, Object> properties, SecurityConfiguration securityConfiguration) throws NoSuchStackException {
    String name = String.valueOf(properties.get(BLUEPRINT_NAME_PROPERTY_ID));
    // String.valueOf() will return "null" if value is null
    if (name.equals("null") || name.isEmpty()) {
      //todo: should throw a checked exception from here
      throw new IllegalArgumentException("Blueprint name must be provided");
    }

    Stack stack = createStack(properties);
    Collection<HostGroup> hostGroups = processHostGroups(name, stack, properties);
    Configuration configuration = configFactory.getConfiguration((Collection<Map<String, String>>)
            properties.get(CONFIGURATION_PROPERTY_ID));
    Setting setting =  SettingFactory.getSetting((Collection<Map<String, Object>>) properties.get(SETTINGS_PROPERTY_ID));

    return new BlueprintImpl(name, hostGroups, stack, configuration, securityConfiguration, setting);
  }

  protected Stack createStack(Map<String, Object> properties) throws NoSuchStackException {
    String stackName = String.valueOf(properties.get(STACK_NAME_PROPERTY_ID));
    String stackVersion = String.valueOf(properties.get(STACK_VERSION_PROPERTY_ID));
    try {
      //todo: don't pass in controller
      return stackFactory.createStack(stackName, stackVersion, AmbariServer.getController());
    } catch (ObjectNotFoundException e) {
      throw new NoSuchStackException(stackName, stackVersion);
    } catch (AmbariException e) {
      //todo:
      throw new RuntimeException("An error occurred parsing the stack information.", e);
    }
  }

  //todo: Move logic to HostGroupImpl
  @SuppressWarnings("unchecked")
  private Collection<HostGroup> processHostGroups(String bpName, Stack stack, Map<String, Object> properties) {
    Set<HashMap<String, Object>> hostGroupProps = (HashSet<HashMap<String, Object>>)
        properties.get(HOST_GROUP_PROPERTY_ID);

    if (hostGroupProps == null || hostGroupProps.isEmpty()) {
      throw new IllegalArgumentException("At least one host group must be specified in a blueprint");
    }

    Collection<HostGroup> hostGroups = new ArrayList<>();
    for (HashMap<String, Object> hostGroupProperties : hostGroupProps) {
      String hostGroupName = (String) hostGroupProperties.get(HOST_GROUP_NAME_PROPERTY_ID);
      if (hostGroupName == null || hostGroupName.isEmpty()) {
        throw new IllegalArgumentException("Every host group must include a non-null 'name' property");
      }

      HashSet<HashMap<String, String>> componentProps = (HashSet<HashMap<String, String>>)
          hostGroupProperties.get(COMPONENT_PROPERTY_ID);

      Collection<Map<String, String>> configProps = (Collection<Map<String, String>>)
          hostGroupProperties.get(CONFIGURATION_PROPERTY_ID);

      Collection<Component> components = processHostGroupComponents(stack, hostGroupName, componentProps);
      Configuration configuration = configFactory.getConfiguration(configProps);
      String cardinality = String.valueOf(hostGroupProperties.get(HOST_GROUP_CARDINALITY_PROPERTY_ID));

      HostGroup group = new HostGroupImpl(hostGroupName, bpName, stack, components, configuration, cardinality);

      hostGroups.add(group);
    }
    return hostGroups;
  }

  private Collection<Component> processHostGroupComponents(Stack stack, String groupName, HashSet<HashMap<String, String>>  componentProps) {
    if (componentProps == null || componentProps.isEmpty()) {
      throw new IllegalArgumentException("Host group '" + groupName + "' must contain at least one component");
    }

    Collection<String> stackComponentNames = getAllStackComponents(stack);
    Collection<Component> components = new ArrayList<>();

    for (HashMap<String, String> componentProperties : componentProps) {
      String componentName = componentProperties.get(COMPONENT_NAME_PROPERTY_ID);
      if (componentName == null || componentName.isEmpty()) {
        throw new IllegalArgumentException("Host group '" + groupName +
            "' contains a component with no 'name' property");
      }

      if (! stackComponentNames.contains(componentName)) {
        throw new IllegalArgumentException("The component '" + componentName + "' in host group '" +
            groupName + "' is not valid for the specified stack");
      }

      String componentProvisionAction = componentProperties.get(COMPONENT_PROVISION_ACTION_PROPERTY_ID);
      if (componentProvisionAction != null) {
        //TODO, might want to add some validation here, to only accept value enum types, rwn
        components.add(new Component(componentName, ProvisionAction.valueOf(componentProvisionAction)));
      } else {
        components.add(new Component(componentName));
      }
    }

    return components;
  }

  /**
   * Obtain all component names for the specified stack.
   *
   * @return collection of component names for the specified stack
   * @throws IllegalArgumentException if the specified stack doesn't exist
   */
  private Collection<String> getAllStackComponents(Stack stack) {
    Collection<String> allComponents = new HashSet<>();
    for (Collection<String> components: stack.getComponents().values()) {
      allComponents.addAll(components);
    }
    // currently ambari server is no a recognized component
    allComponents.add(RootComponent.AMBARI_SERVER.name());

    return allComponents;
  }


  /**
   * Static initialization.
   *
   * @param dao  blueprint data access object
   */
  @Inject
  public static void init(BlueprintDAO dao) {
    blueprintDAO   = dao;
  }

}
