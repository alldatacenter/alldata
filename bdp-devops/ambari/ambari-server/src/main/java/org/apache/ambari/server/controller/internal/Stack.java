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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackLevelConfigurationRequest;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.Configuration;

/**
 * Encapsulates stack information.
 */
public class Stack {
  /**
   * Stack name
   */
  private String name;

  /**
   * Stack version
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

  //todo: instead of all these maps from component -> * ,
  //todo: we should use a Component object with all of these attributes
  private Set<String> masterComponents = new HashSet<>();

  /**
   * Map of component to auto-deploy information
   */
  private Map<String, AutoDeployInfo> componentAutoDeployInfo =
    new HashMap<>();

  /**
   * Map of service to config type properties
   */
  private Map<String, Map<String, Map<String, ConfigProperty>>> serviceConfigurations =
    new HashMap<>();

  /**
   * Map of service to required type properties
   */
  private Map<String, Map<String, Map<String, ConfigProperty>>> requiredServiceConfigurations =
    new HashMap<>();

  /**
   * Map of service to config type properties
   */
  private Map<String, Map<String, ConfigProperty>> stackConfigurations =
    new HashMap<>();

  /**
   * Map of service to set of excluded config types
   */
  private Map<String, Set<String>> excludedConfigurationTypes =
    new HashMap<>();

  /**
   * Ambari Management Controller, used to obtain Stack definitions
   */
  private final AmbariManagementController controller;


  /**
   * Constructor.
   *
   * @param stack
   *          the stack (not {@code null}).
   * @param ambariManagementController
   *          the management controller (not {@code null}).
   * @throws AmbariException
   */
  public Stack(StackEntity stack, AmbariManagementController ambariManagementController) throws AmbariException {
    this(stack.getStackName(), stack.getStackVersion(), ambariManagementController);
  }

  public Stack(StackId stackId, AmbariManagementController ambariManagementController) throws AmbariException {
    this(stackId.getStackName(), stackId.getStackVersion(), ambariManagementController);
  }

  /**
   * Constructor.
   *
   * @param name     stack name
   * @param version  stack version
   *
   * @throws AmbariException an exception occurred getting stack information
   *                         for the specified name and version
   */
  //todo: don't pass management controller in constructor
  public Stack(String name, String version, AmbariManagementController controller) throws AmbariException {
    this.name = name;
    this.version = version;
    this.controller = controller;

    Set<StackServiceResponse> stackServices = controller.getStackServices(
        Collections.singleton(new StackServiceRequest(name, version, null)));

    for (StackServiceResponse stackService : stackServices) {
      String serviceName = stackService.getServiceName();
      parseComponents(serviceName);
      parseExcludedConfigurations(stackService);
      parseConfigurations(stackService);
      registerConditionalDependencies();
    }

    //todo: already done for each service
    parseStackConfigurations();
  }

  /**
   * Obtain stack name.
   *
   * @return stack name
   */
  public String getName() {
    return name;
  }

  /**
   * Obtain stack version.
   *
   * @return stack version
   */
  public String getVersion() {
    return version;
  }

  public StackId getStackId() {
    return new StackId(name, version);
  }

  @Override
  public String toString() {
    return "stack " + getStackId();
  }

  Map<DependencyInfo, String> getDependencyConditionalServiceMap() {
    return dependencyConditionalServiceMap;
  }

  /**
   * Get services contained in the stack.
   *
   * @return set of all services for the stack
   */
  public Set<String> getServices() {
    return serviceComponents.keySet();
  }

  /**
   * Get components contained in the stack for the specified service.
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
   *         or null if the component doesn't exist in the stack
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
   * @return an optional ServiceInfo of the given serviceName or Optional.empty() if the service doesn't exist in the stack
   */
  public Optional<ServiceInfo> getServiceInfo(String serviceName) {
    try {
      return Optional.of(controller.getAmbariMetaInfo().getService(getName(), getVersion(), serviceName));
    } catch (AmbariException e) {
      return Optional.empty();
    }
  }

  /**
   * Get all configuration types, including excluded types for the specified service.
   *
   * @param service  service name
   *
   * @return collection of all configuration types for the specified service
   */
  public Collection<String> getAllConfigurationTypes(String service) {
    return serviceConfigurations.get(service).keySet();
  }

  /**
   * Get configuration types for the specified service.
   * This doesn't include any service excluded types.
   *
   * @param service  service name
   *
   * @return collection of all configuration types for the specified service
   */
  public Collection<String> getConfigurationTypes(String service) {
    Set<String> serviceTypes = new HashSet<>(serviceConfigurations.get(service).keySet());
    serviceTypes.removeAll(getExcludedConfigurationTypes(service));

    return serviceTypes;
  }

  /**
   * Get the set of excluded configuration types for this service.
   *
   * @param service service name
   *
   * @return Set of names of excluded config types. Will not return null.
   */
  public Set<String> getExcludedConfigurationTypes(String service) {
    return excludedConfigurationTypes.containsKey(service) ?
        excludedConfigurationTypes.get(service) :
        Collections.emptySet();
  }

  /**
   * Get config properties for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   *
   * @return map of property names to values for the specified service and configuration type
   */
  public Map<String, String> getConfigurationProperties(String service, String type) {
    Map<String, String> configMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = serviceConfigurations.get(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        configMap.put(configProperty.getKey(), configProperty.getValue().getValue());
      }
    }
    return configMap;
  }

  public Map<String, ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type) {
    return serviceConfigurations.get(service).get(type);
  }

  /**
   * Get all required config properties for the specified service.
   *
   * @param service  service name
   *
   * @return collection of all required properties for the given service
   */
  public Collection<ConfigProperty> getRequiredConfigurationProperties(String service) {
    Collection<ConfigProperty> requiredConfigProperties = new HashSet<>();
    Map<String, Map<String, ConfigProperty>> serviceProperties = requiredServiceConfigurations.get(service);
    if (serviceProperties != null) {
      for (Map.Entry<String, Map<String, ConfigProperty>> typePropertiesEntry : serviceProperties.entrySet()) {
        requiredConfigProperties.addAll(typePropertiesEntry.getValue().values());
      }
    }
    return requiredConfigProperties;
  }

  /**
   * Get required config properties for the specified service which belong to the specified property type.
   *
   * @param service       service name
   * @param propertyType  property type
   *
   * @return collection of required properties for the given service and property type
   */
  public Collection<ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType) {
    Collection<ConfigProperty> matchingProperties = new HashSet<>();
    Map<String, Map<String, ConfigProperty>> requiredProperties = requiredServiceConfigurations.get(service);
    if (requiredProperties != null) {
      for (Map.Entry<String, Map<String, ConfigProperty>> typePropertiesEntry : requiredProperties.entrySet()) {
        for (ConfigProperty configProperty : typePropertiesEntry.getValue().values()) {
          if (configProperty.getPropertyTypes().contains(propertyType)) {
            matchingProperties.add(configProperty);
          }
        }

      }
    }
    return matchingProperties;
  }

  public boolean isPasswordProperty(String service, String type, String propertyName) {
    return (serviceConfigurations.containsKey(service) &&
            serviceConfigurations.get(service).containsKey(type) &&
            serviceConfigurations.get(service).get(type).containsKey(propertyName) &&
            serviceConfigurations.get(service).get(type).get(propertyName).getPropertyTypes().
                contains(PropertyInfo.PropertyType.PASSWORD));
  }

  //todo
  public Map<String, String> getStackConfigurationProperties(String type) {
    Map<String, String> configMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = stackConfigurations.get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        configMap.put(configProperty.getKey(), configProperty.getValue().getValue());
      }
    }
    return configMap;
  }

  public boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName) {
    return (serviceConfigurations.containsKey(service) &&
            serviceConfigurations.get(service).containsKey(type) &&
            serviceConfigurations.get(service).get(type).containsKey(propertyName) &&
            serviceConfigurations.get(service).get(type).get(propertyName).getPropertyTypes().
                contains(PropertyInfo.PropertyType.KERBEROS_PRINCIPAL));
  }
  /**
   * Get config attributes for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   *
   * @return  map of attribute names to map of property names to attribute values
   *          for the specified service and configuration type
   */
  public Map<String, Map<String, String>> getConfigurationAttributes(String service, String type) {
    Map<String, Map<String, String>> attributesMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = serviceConfigurations.get(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        String propertyName = configProperty.getKey();
        Map<String, String> propertyAttributes = configProperty.getValue().getAttributes();
        if (propertyAttributes != null) {
          for (Map.Entry<String, String> propertyAttribute : propertyAttributes.entrySet()) {
            String attributeName = propertyAttribute.getKey();
            String attributeValue = propertyAttribute.getValue();
            if (attributeValue != null) {
              Map<String, String> attributes = attributesMap.get(attributeName);
              if (attributes == null) {
                  attributes = new HashMap<>();
                  attributesMap.put(attributeName, attributes);
              }
              attributes.put(propertyName, attributeValue);
            }
          }
        }
      }
    }
    return attributesMap;
  }

  //todo:
  public Map<String, Map<String, String>> getStackConfigurationAttributes(String type) {
    Map<String, Map<String, String>> attributesMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = stackConfigurations.get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        String propertyName = configProperty.getKey();
        Map<String, String> propertyAttributes = configProperty.getValue().getAttributes();
        if (propertyAttributes != null) {
          for (Map.Entry<String, String> propertyAttribute : propertyAttributes.entrySet()) {
            String attributeName = propertyAttribute.getKey();
            String attributeValue = propertyAttribute.getValue();
            Map<String, String> attributes = attributesMap.get(attributeName);
            if (attributes == null) {
              attributes = new HashMap<>();
              attributesMap.put(attributeName, attributes);
            }
            attributes.put(propertyName, attributeValue);
          }
        }
      }
    }
    return attributesMap;
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
   * Obtain the service name which corresponds to the specified configuration.
   *
   * @param config  configuration type
   *
   * @return name of service which corresponds to the specified configuration type
   */
  public String getServiceForConfigType(String config) {
    for (Map.Entry<String, Map<String, Map<String, ConfigProperty>>> entry : serviceConfigurations.entrySet()) {
      Map<String, Map<String, ConfigProperty>> typeMap = entry.getValue();
      String serviceName = entry.getKey();
      if (typeMap.containsKey(config) && !getExcludedConfigurationTypes(serviceName).contains(config)) {
        return serviceName;
      }
    }
    throw new IllegalArgumentException(
        "Specified configuration type is not associated with any service: " + config);
  }

  public List<String> getServicesForConfigType(String config) {
    List<String> serviceNames = new ArrayList<>();
    for (Map.Entry<String, Map<String, Map<String, ConfigProperty>>> entry : serviceConfigurations.entrySet()) {
      Map<String, Map<String, ConfigProperty>> typeMap = entry.getValue();
      String serviceName = entry.getKey();
      if (typeMap.containsKey(config) && !getExcludedConfigurationTypes(serviceName).contains(config)) {
        serviceNames.add(serviceName);
      }
    }
    return serviceNames;
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

  public boolean isMasterComponent(String component) {
    return masterComponents.contains(component);
  }

  public Configuration getConfiguration(Collection<String> services) {
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    Map<String, Map<String, String>> properties = new HashMap<>();

    for (String service : services) {
      Collection<String> serviceConfigTypes = getConfigurationTypes(service);
      for (String type : serviceConfigTypes) {
        Map<String, String> typeProps = properties.get(type);
        if (typeProps == null) {
          typeProps = new HashMap<>();
          properties.put(type, typeProps);
        }
        typeProps.putAll(getConfigurationProperties(service, type));

        Map<String, Map<String, String>> stackTypeAttributes = getConfigurationAttributes(service, type);
        if (!stackTypeAttributes.isEmpty()) {
          if (! attributes.containsKey(type)) {
            attributes.put(type, new HashMap<>());
          }
          Map<String, Map<String, String>> typeAttributes = attributes.get(type);
          for (Map.Entry<String, Map<String, String>> attribute : stackTypeAttributes.entrySet()) {
            String attributeName = attribute.getKey();
            Map<String, String> attributeProps = typeAttributes.get(attributeName);
            if (attributeProps == null) {
              attributeProps = new HashMap<>();
              typeAttributes.put(attributeName, attributeProps);
            }
            attributeProps.putAll(attribute.getValue());
          }
        }
      }
    }
    return new Configuration(properties, attributes);
  }

  public Configuration getConfiguration() {
    Map<String, Map<String, Map<String, String>>> stackAttributes = new HashMap<>();
    Map<String, Map<String, String>> stackConfigs = new HashMap<>();

    for (String service : getServices()) {
      for (String type : getAllConfigurationTypes(service)) {
        Map<String, String> typeProps = stackConfigs.get(type);
        if (typeProps == null) {
          typeProps = new HashMap<>();
          stackConfigs.put(type, typeProps);
        }
        typeProps.putAll(getConfigurationProperties(service, type));

        Map<String, Map<String, String>> stackTypeAttributes = getConfigurationAttributes(service, type);
        if (!stackTypeAttributes.isEmpty()) {
          if (! stackAttributes.containsKey(type)) {
            stackAttributes.put(type, new HashMap<>());
          }
          Map<String, Map<String, String>> typeAttrs = stackAttributes.get(type);
          for (Map.Entry<String, Map<String, String>> attribute : stackTypeAttributes.entrySet()) {
            String attributeName = attribute.getKey();
            Map<String, String> attributes = typeAttrs.get(attributeName);
            if (attributes == null) {
              attributes = new HashMap<>();
              typeAttrs.put(attributeName, attributes);
            }
            attributes.putAll(attribute.getValue());
          }
        }
      }
    }
    return new Configuration(stackConfigs, stackAttributes);
  }

  /**
   * @return default configuration of the stack, with some updates necessary so that the config can be applied
   * (eg. some properties need a unit to be appended)
   */
  public Configuration getValidDefaultConfig() {
    Configuration config = getDefaultConfig();
    UnitUpdater.updateUnits(config, this);
    return config;
  }

  public Configuration getDefaultConfig() {
    Configuration config = getConfiguration();
    config.getProperties().values().forEach(
      each -> each.values().removeIf(Objects::isNull)
    );
    return config;
  }


  /**
   * Parse components for the specified service from the stack definition.
   *
   * @param service  service name
   *
   * @throws AmbariException an exception occurred getting components from the stack definition
   */
  private void parseComponents(String service) throws AmbariException{
    Collection<String> componentSet = new HashSet<>();

    Set<StackServiceComponentResponse> components = controller.getStackComponents(
        Collections.singleton(new StackServiceComponentRequest(name, version, service, null)));

    // stack service components
    for (StackServiceComponentResponse component : components) {
      String componentName = component.getComponentName();
      componentSet.add(componentName);
      componentService.put(componentName, service);
      String cardinality = component.getCardinality();
      if (cardinality != null) {
        cardinalityRequirements.put(componentName, cardinality);
      }
      AutoDeployInfo autoDeploy = component.getAutoDeploy();
      if (autoDeploy != null) {
        componentAutoDeployInfo.put(componentName, autoDeploy);
      }

      // populate component dependencies
      //todo: remove usage of AmbariMetaInfo
      Collection<DependencyInfo> componentDependencies = controller.getAmbariMetaInfo().getComponentDependencies(
          name, version, service, componentName);

      if (componentDependencies != null && ! componentDependencies.isEmpty()) {
        dependencies.put(componentName, componentDependencies);
      }
      if (component.isMaster()) {
        masterComponents.add(componentName);
      }
    }
    serviceComponents.put(service, componentSet);
  }

  /**
   * Parse configurations for the specified service from the stack definition.
   *
   * @param stackService  service to parse the stack configuration for
   *
   * @throws AmbariException an exception occurred getting configurations from the stack definition
   */
  private void parseConfigurations(StackServiceResponse stackService) throws AmbariException {
    String service = stackService.getServiceName();
    Map<String, Map<String, ConfigProperty>> mapServiceConfig = new HashMap<>();
    Map<String, Map<String, ConfigProperty>> mapRequiredServiceConfig = new HashMap<>();


    serviceConfigurations.put(service, mapServiceConfig);
    requiredServiceConfigurations.put(service, mapRequiredServiceConfig);

    Set<StackConfigurationResponse> serviceConfigs = controller.getStackConfigurations(
        Collections.singleton(new StackConfigurationRequest(name, version, service, null)));
    Set<StackConfigurationResponse> stackLevelConfigs = controller.getStackLevelConfigurations(
        Collections.singleton(new StackLevelConfigurationRequest(name, version, null)));
    serviceConfigs.addAll(stackLevelConfigs);

    // shouldn't have any required properties in stack level configuration
    for (StackConfigurationResponse config : serviceConfigs) {
      ConfigProperty configProperty = new ConfigProperty(config);
      String type = configProperty.getType();

      Map<String, ConfigProperty> mapTypeConfig = mapServiceConfig.get(type);
      if (mapTypeConfig == null) {
        mapTypeConfig = new HashMap<>();
        mapServiceConfig.put(type, mapTypeConfig);
      }

      mapTypeConfig.put(config.getPropertyName(), configProperty);
      if (config.isRequired()) {
        Map<String, ConfigProperty> requiredTypeConfig = mapRequiredServiceConfig.get(type);
        if (requiredTypeConfig == null) {
          requiredTypeConfig = new HashMap<>();
          mapRequiredServiceConfig.put(type, requiredTypeConfig);
        }
        requiredTypeConfig.put(config.getPropertyName(), configProperty);
      }
    }

    // So far we added only config types that have properties defined
    // in stack service definition. Since there might be config types
    // with no properties defined we need to add those separately
    Set<String> configTypes = stackService.getConfigTypes().keySet();
    for (String configType: configTypes) {
      if (!mapServiceConfig.containsKey(configType)) {
        mapServiceConfig.put(configType, Collections.emptyMap());
      }
    }
  }

  private void parseStackConfigurations () throws AmbariException {

    Set<StackConfigurationResponse> stackLevelConfigs = controller.getStackLevelConfigurations(
        Collections.singleton(new StackLevelConfigurationRequest(name, version, null)));

    for (StackConfigurationResponse config : stackLevelConfigs) {
      ConfigProperty configProperty = new ConfigProperty(config);
      String type = configProperty.getType();

      Map<String, ConfigProperty> mapTypeConfig = stackConfigurations.get(type);
      if (mapTypeConfig == null) {
        mapTypeConfig = new HashMap<>();
        stackConfigurations.put(type, mapTypeConfig);
      }

      mapTypeConfig.put(config.getPropertyName(),
          configProperty);
    }
  }

  /**
   * Obtain the excluded configuration types from the StackServiceResponse
   *
   * @param stackServiceResponse the response object associated with this stack service
   */
  private void parseExcludedConfigurations(StackServiceResponse stackServiceResponse) {
    excludedConfigurationTypes.put(stackServiceResponse.getServiceName(), stackServiceResponse.getExcludedConfigTypes());
  }

  /**
   * Register conditional dependencies.
   */
  //todo: This information should be specified in the stack definition.
  void registerConditionalDependencies() {
    dbDependencyInfo.put("MYSQL_SERVER", "global/hive_database");
  }

  /**
   * Contains a configuration property's value and attributes.
   */
  public static class ConfigProperty {
    private ValueAttributesInfo propertyValueAttributes = null;
    private String name;
    private String value;
    private Map<String, String> attributes;
    private Set<PropertyInfo.PropertyType> propertyTypes;
    private String type;
    private Set<PropertyDependencyInfo> dependsOnProperties =
      Collections.emptySet();

    public ConfigProperty(StackConfigurationResponse config) {
      this.name = config.getPropertyName();
      this.value = config.getPropertyValue();
      this.attributes = config.getPropertyAttributes();
      this.propertyTypes = config.getPropertyType();
      this.type = normalizeType(config.getType());
      this.dependsOnProperties = config.getDependsOnProperties();
      this.propertyValueAttributes = config.getPropertyValueAttributes();
    }

    public ConfigProperty(String type, String name, String value) {
      this.type = type;
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getType() {
      return type;
    }

    public Set<PropertyInfo.PropertyType> getPropertyTypes() {
      return propertyTypes;
    }

    public void setPropertyTypes(Set<PropertyInfo.PropertyType> propertyTypes) {
      this.propertyTypes = propertyTypes;
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
    }

    Set<PropertyDependencyInfo> getDependsOnProperties() {
      return this.dependsOnProperties;
    }

    private String normalizeType(String type) {
      //strip .xml from type
      if (type.endsWith(".xml")) {
        type = type.substring(0, type.length() - 4);
      }
      return type;
    }

    public ValueAttributesInfo getPropertyValueAttributes() {
      return propertyValueAttributes;
    }
  }
}
