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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintConfiguration;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.BlueprintSettingEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

/**
 * Blueprint implementation.
 */
public class BlueprintImpl implements Blueprint {

  private String name;
  private Map<String, HostGroup> hostGroups = new HashMap<>();
  private Stack stack;
  private Configuration configuration;
  private BlueprintValidator validator;
  private SecurityConfiguration security;
  private Setting setting;
  private List<RepositorySetting> repoSettings;

  public BlueprintImpl(BlueprintEntity entity) throws NoSuchStackException {
    this.name = entity.getBlueprintName();
    if (entity.getSecurityType() != null) {
      this.security = SecurityConfiguration.of(entity.getSecurityType(), entity.getSecurityDescriptorReference(), null);
    }

    parseStack(entity.getStack());

    // create config first because it is set as a parent on all host-group configs
    processConfiguration(entity.getConfigurations());
    parseBlueprintHostGroups(entity);
    configuration.setParentConfiguration(stack.getConfiguration(getServices()));
    validator = new BlueprintValidatorImpl(this);
    processSetting(entity.getSettings());
    processRepoSettings();
  }

  public BlueprintImpl(String name, Collection<HostGroup> groups, Stack stack, Configuration configuration,
                       SecurityConfiguration security) {
    this(name, groups, stack, configuration, security, null);
  }

  public BlueprintImpl(String name, Collection<HostGroup> groups, Stack stack, Configuration configuration,
                       SecurityConfiguration security, Setting setting) {
    this.name = name;
    this.stack = stack;
    this.security = security;

    // caller should set host group configs
    for (HostGroup hostGroup : groups) {
      hostGroups.put(hostGroup.getName(), hostGroup);
    }
    // if the parent isn't set, the stack configuration is set as the parent
    this.configuration = configuration;
    if (configuration.getParentConfiguration() == null) {
      configuration.setParentConfiguration(stack.getConfiguration(getServices()));
    }
    validator = new BlueprintValidatorImpl(this);
    this.setting = setting;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getStackName() {
    return stack.getName();
  }

  public String getStackVersion() {
    return stack.getVersion();
  }

  @Override
  public SecurityConfiguration getSecurity() {
    return security;
  }

  //todo: safe copy?
  @Override
  public Map<String, HostGroup> getHostGroups() {
    return hostGroups;
  }

  //todo: safe copy?
  @Override
  public HostGroup getHostGroup(String name) {
    return hostGroups.get(name);
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Setting getSetting() {
    return setting;
  }

  /**
   * Get all services represented in blueprint.
   *
   * @return collections of all services provided by topology
   */
  @Override
  public Collection<String> getServices() {
    Collection<String> services = new HashSet<>();
    for (HostGroup group : getHostGroups().values()) {
      services.addAll(group.getServices());
    }
    return services;
  }

  @Override
  public Collection<ServiceInfo> getServiceInfos() {
    return getServices().stream()
      .map(stack::getServiceInfo)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getComponents(String service) {
    Collection<String> components = new HashSet<>();
    for (HostGroup group : getHostGroupsForService(service)) {
      components.addAll(group.getComponents(service));
    }

    return components;
  }

  /**
   * Get whether the specified component in the service is enabled
   * for auto start.
   *
   * @param serviceName - Service name.
   * @param componentName - Component name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  @Override
  public String getRecoveryEnabled(String serviceName, String componentName) {
    Set<HashMap<String, String>> settingValue;

    if (setting == null)
      return null;

    // If component name was specified in the list of "component_settings",
    // determine if recovery_enabled is true or false and return it.
    settingValue = setting.getSettingValue(Setting.SETTING_NAME_COMPONENT_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      String name = setting.get(Setting.SETTING_NAME_NAME);
      if (StringUtils.equals(name, componentName)) {
        if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED))) {
          return setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED);
        }
      }
    }

    // If component name is not specified, look up it's service.
    settingValue = setting.getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS);
    for ( Map<String, String> setting : settingValue){
      String name = setting.get(Setting.SETTING_NAME_NAME);
      if (StringUtils.equals(name, serviceName)) {
        if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED))) {
          return setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED);
        }
      }
    }

    // If service name is not specified, look up the cluster setting.
    settingValue = setting.getSettingValue(Setting.SETTING_NAME_RECOVERY_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED))) {
        return setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED);
      }
    }

    return null;
  }

  /**
   * Get whether the specified service is enabled for credential store use.
   *
   * <pre>
   *     {@code
   *       {
   *         "service_settings" : [
   *         { "name" : "RANGER",
   *           "recovery_enabled" : "true",
   *           "credential_store_enabled" : "true"
   *         },
   *         { "name" : "HIVE",
   *           "recovery_enabled" : "true",
   *           "credential_store_enabled" : "false"
   *         },
   *         { "name" : "TEZ",
   *           "recovery_enabled" : "false"
   *         }
   *       ]
   *     }
   *   }
   * </pre>
   *
   * @param serviceName - Service name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  @Override
  public String getCredentialStoreEnabled(String serviceName) {
    if (setting == null)
      return null;

    // Look up the service and return the credential_store_enabled value.
    Set<HashMap<String, String>> settingValue = setting.getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      String name = setting.get(Setting.SETTING_NAME_NAME);
      if (StringUtils.equals(name, serviceName)) {
        if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_CREDENTIAL_STORE_ENABLED))) {
          return setting.get(Setting.SETTING_NAME_CREDENTIAL_STORE_ENABLED);
        }
        break;
      }
    }

    return null;
  }

  @Override
  public boolean shouldSkipFailure() {
    if (setting == null) {
      return false;
    }
    Set<HashMap<String, String>> settingValue = setting.getSettingValue(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      if (setting.containsKey(Setting.SETTING_NAME_SKIP_FAILURE)) {
        return setting.get(Setting.SETTING_NAME_SKIP_FAILURE).equalsIgnoreCase("true");
      }
    }
    return false;
  }

  @Override
  public Stack getStack() {
    return stack;
  }

  /**
   * Get host groups which contain a component.
   *
   * @param component   component name
   *
   * @return collection of host groups which contain the specified component
   */
  @Override
  public Collection<HostGroup> getHostGroupsForComponent(String component) {
    Collection<HostGroup> resultGroups = new HashSet<>();
    for (HostGroup group : hostGroups.values() ) {
      if (group.getComponentNames().contains(component)) {
        resultGroups.add(group);
      }
    }
    return resultGroups;
  }

  /**
   * Get host groups which contain a component for the given service.
   *
   * @param service   service name
   *
   * @return collection of host groups which contain a component of the specified service
   */
  @Override
  public Collection<HostGroup> getHostGroupsForService(String service) {
    Collection<HostGroup> resultGroups = new HashSet<>();
    for (HostGroup group : hostGroups.values() ) {
      if (group.getServices().contains(service)) {
        resultGroups.add(group);
      }
    }
    return resultGroups;
  }

  @Override
  public void validateTopology() throws InvalidTopologyException {
    validator.validateTopology();
  }

  @Override
  public BlueprintEntity toEntity() {

    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName(name);
    if (security != null) {
      if (security.getType() != null) {
        entity.setSecurityType(security.getType());
      }
      if (security.getDescriptorReference() != null) {
        entity.setSecurityDescriptorReference(security.getDescriptorReference());
      }
    }

    //todo: not using stackDAO so stackEntity.id is not set
    //todo: this is now being set in BlueprintDAO
    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName(stack.getName());
    stackEntity.setStackVersion(stack.getVersion());
    entity.setStack(stackEntity);

    createHostGroupEntities(entity);
    createBlueprintConfigEntities(entity);
    createBlueprintSettingEntities(entity);

    return entity;
  }

  /**
   * Validate blueprint configuration.
   *
   * @throws InvalidTopologyException if the blueprint configuration is invalid
   * @throws GPLLicenseNotAcceptedException ambari was configured to use gpl software, but gpl license is not accepted
   */
  @Override
  public void validateRequiredProperties() throws InvalidTopologyException, GPLLicenseNotAcceptedException {
    validator.validateRequiredProperties();
  }

  private void parseStack(StackEntity stackEntity) throws NoSuchStackException {
    try {
      //todo: don't pass in controller
      stack = new Stack(stackEntity.getStackName(), stackEntity.getStackVersion(), AmbariServer.getController());
    } catch (StackAccessException e) {
      throw new NoSuchStackException(stackEntity.getStackName(), stackEntity.getStackVersion());
    } catch (AmbariException e) {
      //todo:
      throw new RuntimeException("An error occurred parsing the stack information.", e);
    }
  }

  private Map<String, HostGroup> parseBlueprintHostGroups(BlueprintEntity entity) {
    for (HostGroupEntity hostGroupEntity : entity.getHostGroups()) {
      HostGroupImpl hostGroup = new HostGroupImpl(hostGroupEntity, getName(), stack);
      // set the bp configuration as the host group config parent
      hostGroup.getConfiguration().setParentConfiguration(configuration);
      hostGroups.put(hostGroupEntity.getName(), hostGroup);
    }
    return hostGroups;
  }

  /**
   * Process blueprint configurations.
   */
  private void processConfiguration(Collection<BlueprintConfigEntity> configs) {
    // not setting stack configuration as parent until after host groups are parsed in constructor
    configuration = new Configuration(parseConfigurations(configs),
        parseAttributes(configs), null);
  }

  /**
   * Process blueprint setting.
   *
   * @param blueprintSetting
   */
  private void processSetting(Collection<BlueprintSettingEntity> blueprintSetting) {
    if (blueprintSetting != null) {
      setting = new Setting(parseSetting(blueprintSetting));
    }
  }

  /**
   * Obtain configuration as a map of config type to corresponding properties.
   *
   * @return map of config type to map of properties
   */
  private Map<String, Map<String, String>> parseConfigurations(Collection<BlueprintConfigEntity> configs) {

    Map<String, Map<String, String>> properties = new HashMap<>();
    Gson gson = new Gson();
    for (BlueprintConfiguration config : configs) {
      String type = config.getType();
      Map<String, String> typeProperties = gson.<Map<String, String>>fromJson(
              config.getConfigData(), Map.class);
      properties.put(type, typeProperties);
    }
    return properties;
  }

  /**
   * Deserialization: Obtain setting as a map of setting name to corresponding properties.
   *
   * @return map of setting name to map of properties
   */
  private Map<String, Set<HashMap<String, String>>> parseSetting(Collection<BlueprintSettingEntity> blueprintSetting) {

    Map<String, Set<HashMap<String, String>>> properties = new HashMap<>();
    Gson gson = new Gson();
    for (BlueprintSettingEntity setting : blueprintSetting) {
      String settingName = setting.getSettingName();
      Set<HashMap<String, String>> settingProperties = gson.<Set<HashMap<String, String>>>fromJson(
              setting.getSettingData(), Set.class);
      properties.put(settingName, settingProperties);
    }
    return properties;
  }

  /**
   * Process cluster scoped configuration attributes contained in blueprint.
   *
   * @return cluster scoped property attributes contained within in blueprint
   */
  //todo: do inline with config processing
  private Map<String, Map<String, Map<String, String>>> parseAttributes(Collection<BlueprintConfigEntity> configs) {
    Map<String, Map<String, Map<String, String>>> mapAttributes =
      new HashMap<>();

    if (configs != null) {
      Gson gson = new Gson();
      for (BlueprintConfigEntity config : configs) {
        Map<String, Map<String, String>> typeAttrs =
            gson.<Map<String, Map<String, String>>>fromJson(config.getConfigAttributes(), Map.class);
        if (typeAttrs != null && !typeAttrs.isEmpty()) {
          mapAttributes.put(config.getType(), typeAttrs);
        }
      }
    }
    return mapAttributes;
  }

  /**
   * Create host group entities and add to the parent blueprint entity.
   */
  @SuppressWarnings("unchecked")
  private void createHostGroupEntities(BlueprintEntity blueprintEntity) {
    Collection<HostGroupEntity> entities = new ArrayList<>();
    for (HostGroup group : getHostGroups().values()) {
      HostGroupEntity hostGroupEntity = new HostGroupEntity();
      entities.add(hostGroupEntity);

      hostGroupEntity.setName(group.getName());
      hostGroupEntity.setBlueprintEntity(blueprintEntity);
      hostGroupEntity.setBlueprintName(getName());
      hostGroupEntity.setCardinality(group.getCardinality());

      createHostGroupConfigEntities(hostGroupEntity, group.getConfiguration());

      createComponentEntities(hostGroupEntity, group.getComponents());
    }
    blueprintEntity.setHostGroups(entities);
  }

  /**
   * Populate host group configurations.
   */
  private void createHostGroupConfigEntities(HostGroupEntity hostGroup, Configuration groupConfiguration) {
    Gson jsonSerializer = new Gson();
    Map<String, HostGroupConfigEntity> configEntityMap = new HashMap<>();
    for (Map.Entry<String, Map<String, String>> propEntry : groupConfiguration.getProperties().entrySet()) {
      String type = propEntry.getKey();
      Map<String, String> properties = propEntry.getValue();

      HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
      configEntityMap.put(type, configEntity);
      configEntity.setBlueprintName(getName());
      configEntity.setHostGroupEntity(hostGroup);
      configEntity.setHostGroupName(hostGroup.getName());
      configEntity.setType(type);
      configEntity.setConfigData(jsonSerializer.toJson(properties));
    }

    for (Map.Entry<String, Map<String, Map<String, String>>> attributesEntry : groupConfiguration.getAttributes().entrySet()) {
      String type = attributesEntry.getKey();
      Map<String, Map<String, String>> attributes = attributesEntry.getValue();

      HostGroupConfigEntity entity = configEntityMap.get(type);
      if (entity == null) {
        entity = new HostGroupConfigEntity();
        configEntityMap.put(type, entity);
        entity.setBlueprintName(getName());
        entity.setHostGroupEntity(hostGroup);
        entity.setHostGroupName(hostGroup.getName());
        entity.setType(type);
      }
      entity.setConfigAttributes(jsonSerializer.toJson(attributes));
    }
    hostGroup.setConfigurations(configEntityMap.values());
  }

  /**
    * Create component entities and add to parent host group.
    */
  @SuppressWarnings("unchecked")
  private void createComponentEntities(HostGroupEntity group, Collection<Component> components) {
    Collection<HostGroupComponentEntity> componentEntities = new HashSet<>();
    group.setComponents(componentEntities);

    for (Component component : components) {
      HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
      componentEntities.add(componentEntity);

      componentEntity.setName(component.getName());
      componentEntity.setBlueprintName(group.getBlueprintName());
      componentEntity.setHostGroupEntity(group);
      componentEntity.setHostGroupName(group.getName());

      // add provision action (if specified) to entity type
      // otherwise, just leave this column null (provision_action)
      if (component.getProvisionAction() != null) {
        componentEntity.setProvisionAction(component.getProvisionAction().toString());
      }

    }
    group.setComponents(componentEntities);
  }

  /**
   * Populate host group configurations.
   */
  private void createBlueprintConfigEntities(BlueprintEntity blueprintEntity) {
    Gson jsonSerializer = new Gson();
    Configuration config = getConfiguration();
    Map<String, BlueprintConfigEntity> configEntityMap = new HashMap<>();
    for (Map.Entry<String, Map<String, String>> propEntry : config.getProperties().entrySet()) {
      String type = propEntry.getKey();
      Map<String, String> properties = propEntry.getValue();

      BlueprintConfigEntity configEntity = new BlueprintConfigEntity();
      configEntityMap.put(type, configEntity);
      configEntity.setBlueprintName(getName());
      configEntity.setBlueprintEntity(blueprintEntity);
      configEntity.setType(type);
      configEntity.setConfigData(jsonSerializer.toJson(properties));
    }

    for (Map.Entry<String, Map<String, Map<String, String>>> attributesEntry : config.getAttributes().entrySet()) {
      String type = attributesEntry.getKey();
      Map<String, Map<String, String>> attributes = attributesEntry.getValue();

      BlueprintConfigEntity entity = configEntityMap.get(type);
      if (entity == null) {
        entity = new BlueprintConfigEntity();
        configEntityMap.put(type, entity);
        entity.setBlueprintName(getName());
        entity.setBlueprintEntity(blueprintEntity);
        entity.setType(type);
      }
      entity.setConfigAttributes(jsonSerializer.toJson(attributes));
    }
    blueprintEntity.setConfigurations(configEntityMap.values());
  }

  /**
   * Populate setting for serialization to DB.
   */
  private void createBlueprintSettingEntities(BlueprintEntity blueprintEntity) {
    Gson jsonSerializer = new Gson();
    Setting blueprintSetting = getSetting();
    if (blueprintSetting != null) {
      Map<String, BlueprintSettingEntity> settingEntityMap = new HashMap<>();
      for (Map.Entry<String, Set<HashMap<String, String>>> propEntry : blueprintSetting.getProperties().entrySet()) {
        String settingName = propEntry.getKey();
        Set<HashMap<String, String>> properties = propEntry.getValue();

        BlueprintSettingEntity settingEntity = new BlueprintSettingEntity();
        settingEntityMap.put(settingName, settingEntity);
        settingEntity.setBlueprintName(getName());
        settingEntity.setBlueprintEntity(blueprintEntity);
        settingEntity.setSettingName(settingName);
        settingEntity.setSettingData(jsonSerializer.toJson(properties));
      }
      blueprintEntity.setSettings(settingEntityMap.values());
    }
  }

  /**
   * A config type is valid if there are services related to except cluster-env and global.
   */
  @Override
  public boolean isValidConfigType(String configType) {
    if (ConfigHelper.CLUSTER_ENV.equals(configType) || "global".equals(configType)) {
      return true;
    }
    return getStack().getServicesForConfigType(configType).stream().anyMatch(getServices()::contains);
  }

  /**
   * Parse stack repo info stored in the blueprint_settings table
   * @return set of repositories
   * */
  private void processRepoSettings(){
    repoSettings = new ArrayList<>();
    if (setting != null){
      Set<HashMap<String, String>> settingValue = setting.getSettingValue(Setting.SETTING_NAME_REPOSITORY_SETTINGS);
      for (Map<String, String> setting : settingValue) {
        RepositorySetting rs = parseRepositorySetting(setting);
        repoSettings.add(rs);
      }
    }
  }

  private RepositorySetting parseRepositorySetting(Map<String, String> setting){
    RepositorySetting result = new RepositorySetting();
    result.setOperatingSystem(setting.get(RepositorySetting.OPERATING_SYSTEM));
    result.setOverrideStrategy(setting.get(RepositorySetting.OVERRIDE_STRATEGY));
    result.setRepoId(setting.get(RepositorySetting.REPO_ID));
    result.setBaseUrl(setting.get(RepositorySetting.BASE_URL));
    return result;
  }

  @Override
  public List<RepositorySetting> getRepositorySettings(){
    return repoSettings;
  }
}
