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

package org.apache.ambari.server.stack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServicePropertyInfo;
import org.apache.ambari.server.state.ThemeInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


/**
 * Service module which provides all functionality related to parsing and fully
 * resolving services from the stack definition.
 */
public class ServiceModule extends BaseModule<ServiceModule, ServiceInfo> implements Validable{
  /**
   * Corresponding service info
   */
  private ServiceInfo serviceInfo;

  /**
   * Context which provides modules access to external functionality
   */
  private StackContext stackContext;

  /**
   * Map of child configuration modules keyed by configuration type
   */
  private Map<String, ConfigurationModule> configurationModules =
    new HashMap<>();

  /**
   * Map of child component modules keyed by component name
   */
  private Map<String, ComponentModule> componentModules =
    new HashMap<>();

  /**
   * Map of themes, single value currently
   */
  private Map<String, ThemeModule> themeModules = new HashMap<>();

  /**
   * Map of quicklinks, single value currently
   */
  private Map<String, QuickLinksConfigurationModule> quickLinksConfigurationModules = new HashMap<>();

  /**
   * Encapsulates IO operations on service directory
   */
  private ServiceDirectory serviceDirectory;

  /**
   * Flag to mark a service as a common service
   */
  private boolean isCommonService;

  /**
   * validity flag
   */
  protected boolean valid = true;

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(ServiceModule.class);

  /**
   * Most of the services contain at least one config type.
   * However, there are special cases on third party stacks
   * that certain services do not have any config types.
   * */
  private boolean hasConfigs = true;

  /**
   * Constructor.
   *
   * @param stackContext      stack context which provides module access to external functionality
   * @param serviceInfo       associated service info
   * @param serviceDirectory  used for all IO interaction with service directory in stack definition
   */
  public ServiceModule(StackContext stackContext, ServiceInfo serviceInfo, ServiceDirectory serviceDirectory) {
    this(stackContext, serviceInfo, serviceDirectory, false);
  }

  /**
   * Constructor.
   *
   * @param stackContext      stack context which provides module access to external functionality
   * @param serviceInfo       associated service info
   * @param serviceDirectory  used for all IO interaction with service directory in stack definition
   * @param isCommonService   flag to mark a service as a common service
   */
  public ServiceModule(
      StackContext stackContext, ServiceInfo serviceInfo, ServiceDirectory serviceDirectory, boolean isCommonService) {
    this.serviceInfo = serviceInfo;
    this.stackContext = stackContext;
    this.serviceDirectory = serviceDirectory;
    this.isCommonService = isCommonService;

    serviceInfo.setMetricsFile(serviceDirectory.getMetricsFile(serviceInfo.getName()));
    serviceInfo.setAlertsFile(serviceDirectory.getAlertsFile());
    serviceInfo.setKerberosDescriptorFile(serviceDirectory.getKerberosDescriptorFile());
    serviceInfo.setWidgetsDescriptorFile(serviceDirectory.getWidgetsDescriptorFile(serviceInfo.getName()));
    serviceInfo.setRoleCommandOrder(serviceDirectory.getRoleCommandOrder());
    serviceInfo.setSchemaVersion(AmbariMetaInfo.SCHEMA_VERSION_2);
    serviceInfo.setServicePackageFolder(serviceDirectory.getPackageDir());
    serviceInfo.setServiceUpgradesFolder(serviceDirectory.getUpgradesDir());
    serviceInfo.setChecksFolder(serviceDirectory.getChecksDir());
    serviceInfo.setServerActionsFolder(serviceDirectory.getServerActionsDir());
    serviceInfo.setAdvisorFile(serviceDirectory.getAdvisorFile());
    serviceInfo.setAdvisorName(serviceDirectory.getAdvisorName(serviceInfo.getName()));

    populateComponentModules();
    populateConfigurationModules();
    populateThemeModules();
    populateQuickLinksConfigurationModules();

    validateServiceInfo();
  }

  @Override
  public ServiceInfo getModuleInfo() {
    return serviceInfo;
  }

  @Override
  public void resolve(
      ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    resolveInternal(parentModule, allStacks, commonServices, extensions, false);
  }

  public void resolveExplicit(
      ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    resolveInternal(parentModule, allStacks, commonServices, extensions, true);
  }

  public void resolveInternal(
      ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices,
      Map<String, ExtensionModule> extensions, boolean resolveExplicit)
      throws AmbariException {
    if (!serviceInfo.isValid() || !parentModule.isValid()) {
      return;
    }

    LOG.debug("Resolve service");

    // If resolving against parent stack service module (stack inheritance), do not merge if an
    // explicit parent is specified
    if(!StringUtils.isBlank(serviceInfo.getParent()) && !resolveExplicit) {
      return;
    }

    ServiceInfo parent = parentModule.getModuleInfo();

    if (serviceInfo.getComment() == null) {
      serviceInfo.setComment(parent.getComment());
    }
    LOG.info(String.format("Display name service/parent: %s/%s", serviceInfo.getDisplayName(), parent.getDisplayName()));
    if (serviceInfo.getDisplayName() == null) {
      serviceInfo.setDisplayName(parent.getDisplayName());
    }
    if (serviceInfo.getServiceType() == null) {
      serviceInfo.setServiceType(parent.getServiceType());
    }
    if (serviceInfo.getServiceAdvisorType() == null) {
      ServiceInfo.ServiceAdvisorType serviceAdvisorType = parent.getServiceAdvisorType();
      serviceInfo.setServiceAdvisorType(serviceAdvisorType == null ? ServiceInfo.ServiceAdvisorType.PYTHON : serviceAdvisorType);
    }
    if (serviceInfo.getVersion() == null) {
      serviceInfo.setVersion(parent.getVersion());
    }

    if (serviceInfo.getRequiredServices() == null
        || serviceInfo.getRequiredServices().size() == 0) {
      serviceInfo.setRequiredServices(parent.getRequiredServices() != null ?
          parent.getRequiredServices() :
          Collections.emptyList());
    }

    if (serviceInfo.isRestartRequiredAfterChange() == null) {
      serviceInfo.setRestartRequiredAfterChange(parent.isRestartRequiredAfterChange());
    }
    if (serviceInfo.isRestartRequiredAfterRackChange() == null) {
      serviceInfo.setRestartRequiredAfterRackChange(parent.isRestartRequiredAfterRackChange());
    }
    if (serviceInfo.isMonitoringService() == null) {
      serviceInfo.setMonitoringService(parent.isMonitoringService());
    }
    if (serviceInfo.getOsSpecifics().isEmpty() ) {
      serviceInfo.setOsSpecifics(parent.getOsSpecifics());
    }
    if (serviceInfo.getCommandScript() == null) {
      serviceInfo.setCommandScript(parent.getCommandScript());
    }
    if (serviceInfo.getServicePackageFolder() == null) {
      serviceInfo.setServicePackageFolder(parent.getServicePackageFolder());
    }
    if (serviceInfo.getServiceUpgradesFolder() == null) {
      serviceInfo.setServiceUpgradesFolder(parent.getServiceUpgradesFolder());
    }
    if (serviceInfo.getMetricsFile() == null) {
      serviceInfo.setMetricsFile(parent.getMetricsFile());
    }
    if (serviceInfo.getAlertsFile() == null) {
      serviceInfo.setAlertsFile(parent.getAlertsFile());
    }
    if (serviceInfo.getKerberosDescriptorFile() == null) {
      serviceInfo.setKerberosDescriptorFile(parent.getKerberosDescriptorFile());
    }
    if (serviceInfo.getThemesMap().isEmpty()) {
      serviceInfo.setThemesMap(parent.getThemesMap());
    }
    if (serviceInfo.getWidgetsDescriptorFile() == null) {
      serviceInfo.setWidgetsDescriptorFile(parent.getWidgetsDescriptorFile());
    }
    if (serviceInfo.getAdvisorFile() == null) {
      serviceInfo.setAdvisorFile(parent.getAdvisorFile());
    }
    if (serviceInfo.getAdvisorName() == null) {
      serviceInfo.setAdvisorName(parent.getAdvisorName());
    }

    if (serviceInfo.getRoleCommandOrder() == null) {
      serviceInfo.setRoleCommandOrder(parent.getRoleCommandOrder());
    }
    if (serviceInfo.getChecksFolder() == null) {
      serviceInfo.setChecksFolder(parent.getChecksFolder());
    }

    /*
     * Use parent's server actions if the current one does not have any.
     */
    if (serviceInfo.getServerActionsFolder() == null) {
      serviceInfo.setServerActionsFolder(parent.getServerActionsFolder());
    }

    /*
     * If current stack version does not specify the credential store information
     * for the service, then use parent definition.
     */
    if (serviceInfo.getCredentialStoreInfo() == null) {
      serviceInfo.setCredentialStoreInfo(parent.getCredentialStoreInfo());
    }

    /*
     * If current stack version does not specify the single sign-on support details for the service,
     * then use parent definition.
     */
    if (serviceInfo.getSingleSignOnInfo() == null) {
      serviceInfo.setSingleSignOnInfo(parent.getSingleSignOnInfo());
    }

    if (serviceInfo.isSelectionEmpty()) {
      serviceInfo.setSelection(parent.getSelection());
    }

    if (serviceInfo.isMaintainerEmpty()) {
      serviceInfo.setMaintainer(parent.getMaintainer());
    }

    if(null == serviceInfo.getSupportDeleteViaUIField()){
      serviceInfo.setSupportDeleteViaUI(parent.isSupportDeleteViaUI());
    }

    mergeCustomCommands(parent.getCustomCommands(), serviceInfo.getCustomCommands());
    mergeConfigDependencies(parent);
    mergeComponents(parentModule, allStacks, commonServices, extensions);
    mergeConfigurations(parentModule, allStacks, commonServices, extensions);
    mergeThemes(parentModule, allStacks, commonServices, extensions);
    mergeQuickLinksConfigurations(parentModule, allStacks, commonServices, extensions);
    mergeExcludedConfigTypes(parent);

    mergeServiceProperties(parent.getServicePropertyList());
  }

  /**
   * Merges service properties from parent into the the service properties of this this service.
   * Current properties overrides properties with same name from parent
   * @param other service properties to merge with the current service property list
   */
  private void mergeServiceProperties(List<ServicePropertyInfo> other) {
    if (!other.isEmpty()) {
      List<ServicePropertyInfo> servicePropertyList = serviceInfo.getServicePropertyList();
      List<ServicePropertyInfo> servicePropertiesToAdd = Lists.newArrayList();

      Set<String> servicePropertyNames = Sets.newTreeSet(
        Iterables.transform(servicePropertyList, new Function<ServicePropertyInfo, String>() {
          @Nullable
          @Override
          public String apply(ServicePropertyInfo serviceProperty) {
            return serviceProperty.getName();
          }
        })
      );

      for (ServicePropertyInfo otherServiceProperty : other) {
        if (!servicePropertyNames.contains(otherServiceProperty.getName()))
          servicePropertiesToAdd.add(otherServiceProperty);
      }

      List<ServicePropertyInfo> mergedServicePropertyList =
        ImmutableList.<ServicePropertyInfo>builder()
          .addAll(servicePropertyList)
          .addAll(servicePropertiesToAdd)
          .build();

      serviceInfo.setServicePropertyList(mergedServicePropertyList);

      validateServiceInfo();
    }
  }

  /**
   * Resolve common service
   * @param allStacks       all stack modules
   * @param commonServices  common service modules
   *
   * @throws AmbariException
   */
  public void resolveCommonService(Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    if(!isCommonService) {
      throw new AmbariException("Not a common service");
    }
    moduleState = ModuleState.VISITED;
    String parentString = serviceInfo.getParent();
    if(parentString != null) {
      String[] parentToks = parentString.split(StackManager.PATH_DELIMITER);
      if(parentToks.length != 3) {
        throw new AmbariException("The common service '" + serviceInfo.getName() + serviceInfo.getVersion()
            + "' extends an invalid parent: '" + parentString + "'");
      }
      if (parentToks[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
        String baseServiceKey = parentToks[1] + StackManager.PATH_DELIMITER + parentToks[2];
        ServiceModule baseService = commonServices.get(baseServiceKey);
        ModuleState baseModuleState = baseService.getModuleState();
        if (baseModuleState == ModuleState.INIT) {
          baseService.resolveCommonService(allStacks, commonServices, extensions);
        } else if (baseModuleState == ModuleState.VISITED) {
          //todo: provide more information to user about cycle
          throw new AmbariException("Cycle detected while parsing common service");
        }
        resolveExplicit(baseService, allStacks, commonServices, extensions);
      } else {
        throw new AmbariException("Common service cannot inherit from a non common service");
      }
    }
    moduleState = ModuleState.RESOLVED;
  }

  @Override
  public boolean isDeleted() {
    return serviceInfo.isDeleted();
  }

  @Override
  public String getId() {
    return serviceInfo.getName();
  }

  @Override
  public void finalizeModule() {
    finalizeChildModules(configurationModules.values());
    finalizeChildModules(componentModules.values());
    finalizeConfiguration();
    if(serviceInfo.getCommandScript() != null && ! isDeleted()) {
      stackContext.registerServiceCheck(getId());
    }
  }

  /**
   * Parse and populate child component modules.
   */
  private void populateComponentModules() {
    for (ComponentInfo component : serviceInfo.getComponents()) {
      componentModules.put(component.getName(), new ComponentModule(component));
    }
  }

  /**
   * Parse and populate child configuration modules.
   */
  private void populateConfigurationModules() {
    ConfigurationDirectory configDirectory = serviceDirectory.getConfigurationDirectory(
        serviceInfo.getConfigDir(), StackDirectory.SERVICE_PROPERTIES_FOLDER_NAME);

    if (configDirectory != null) {
      for (ConfigurationModule config : configDirectory.getConfigurationModules()) {
          ConfigurationInfo info = config.getModuleInfo();
          if (isValid()){
            setValid(config.isValid() && info.isValid());
            if (!isValid()){
              addErrors(config.getErrors());
              addErrors(info.getErrors());
            }
          }
          serviceInfo.getProperties().addAll(info.getProperties());
          serviceInfo.setTypeAttributes(config.getConfigType(), info.getAttributes());
          configurationModules.put(config.getConfigType(), config);
      }

      for (String excludedType : serviceInfo.getExcludedConfigTypes()) {
        if (! configurationModules.containsKey(excludedType)) {
          ConfigurationInfo configInfo = new ConfigurationInfo(
              Collections.emptyList(), Collections.emptyMap());
          ConfigurationModule config = new ConfigurationModule(excludedType, configInfo);

          config.setDeleted(true);
          configurationModules.put(excludedType, config);
        }
      }
    }
  }

  private void populateThemeModules() {

    if (serviceInfo.getThemesDir() == null) {
      serviceInfo.setThemesDir(StackDirectory.SERVICE_THEMES_FOLDER_NAME);
    }

    String themesDir = serviceDirectory.getAbsolutePath() + File.separator + serviceInfo.getThemesDir();

    if (serviceInfo.getThemes() != null) {
      List<ThemeInfo> themes = new ArrayList<>(serviceInfo.getThemes().size());
      for (ThemeInfo themeInfo : serviceInfo.getThemes()) {
        File themeFile = new File(themesDir + File.separator + themeInfo.getFileName());
        ThemeModule module = new ThemeModule(themeFile, themeInfo);
        if (module.isValid()) {
          themeModules.put(module.getId(), module);
          themes.add(themeInfo);
        }
        else {
          //lets not fail if theme contain errors
          LOG.error("Invalid theme {} for service {}", themeInfo.getFileName(), serviceInfo.getName());
        }
      }
      //filter out the invalid themes
      serviceInfo.setThemes(themes);
    }
  }

  /**
   * Merge theme modules.
   */
  private void mergeThemes(ServiceModule parent, Map<String, StackModule> allStacks,
                           Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions) throws AmbariException {
    Collection<ThemeModule> mergedModules = mergeChildModules(allStacks, commonServices, extensions, themeModules, parent.themeModules);

    for (ThemeModule mergedModule : mergedModules) {
      themeModules.put(mergedModule.getId(), mergedModule);
      ThemeInfo moduleInfo = mergedModule.getModuleInfo();
      if (!moduleInfo.isDeleted()) {
        serviceInfo.getThemesMap().put(moduleInfo.getFileName(), moduleInfo);
      } else {
        serviceInfo.getThemesMap().remove(moduleInfo.getFileName());
      }
    }
  }

  private void populateQuickLinksConfigurationModules(){
    if (serviceInfo.getQuickLinksConfigurationsDir() == null) {
      serviceInfo.setQuickLinksConfigurationsDir(StackDirectory.SERVICE_QUICKLINKS_CONFIGURATIONS_FOLDER_NAME);
    }

    String quickLinksConfigurationsDir = serviceDirectory.getAbsolutePath() + File.separator + serviceInfo.getQuickLinksConfigurationsDir();

    if (serviceInfo.getQuickLinksConfigurations() != null) {
      for (QuickLinksConfigurationInfo quickLinksConfigurationInfo: serviceInfo.getQuickLinksConfigurations()) {
        File file = new File(quickLinksConfigurationsDir + File.separator + quickLinksConfigurationInfo.getFileName());
        QuickLinksConfigurationModule module = new QuickLinksConfigurationModule(file, quickLinksConfigurationInfo);
        quickLinksConfigurationModules.put(module.getId(), module);
      }
    }    //Not fail if quicklinks.json file contains errors
  }

  /**
   * Merge theme modules.
   */
  private void mergeQuickLinksConfigurations(ServiceModule parent, Map<String, StackModule> allStacks,
                           Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions) throws AmbariException {
    Collection<QuickLinksConfigurationModule> mergedModules = mergeChildModules(allStacks, commonServices, extensions, quickLinksConfigurationModules, parent.quickLinksConfigurationModules);

    for (QuickLinksConfigurationModule mergedModule : mergedModules) {
      quickLinksConfigurationModules.put(mergedModule.getId(), mergedModule);
      QuickLinksConfigurationInfo moduleInfo = mergedModule.getModuleInfo();
      if (!moduleInfo.isDeleted()) {
        serviceInfo.getQuickLinksConfigurationsMap().put(moduleInfo.getFileName(), moduleInfo);
      } else {
        serviceInfo.getQuickLinksConfigurationsMap().remove(moduleInfo.getFileName());
      }
    }
  }

  /**
   * Merge excluded configs types with parent.  Child values override parent values.
   *
   * @param parent parent service module
   */

  private void mergeExcludedConfigTypes(ServiceInfo parent){
    if (serviceInfo.getExcludedConfigTypes() == null){
      serviceInfo.setExcludedConfigTypes(parent.getExcludedConfigTypes());
    } else if (parent.getExcludedConfigTypes() != null){
      Set<String> resultExcludedConfigTypes = serviceInfo.getExcludedConfigTypes();
      for (String excludedType : parent.getExcludedConfigTypes()) {
        if (!resultExcludedConfigTypes.contains(excludedType)){
          resultExcludedConfigTypes.add(excludedType);
        }
      }
      serviceInfo.setExcludedConfigTypes(resultExcludedConfigTypes);
    }

  }

  /**
   * Merge configuration dependencies with parent.  Child values override parent values.
   *
   * @param parent  parent service module
   */
  private void mergeConfigDependencies(ServiceInfo parent) {
    //currently there is no way to remove an inherited config dependency
    List<String> configDependencies = serviceInfo.getConfigDependencies();
    List<String> parentConfigDependencies = parent.getConfigDependencies() != null ?
        parent.getConfigDependencies() : Collections.emptyList();

    if (configDependencies == null) {
      serviceInfo.setConfigDependencies(parentConfigDependencies);
    } else {
      for (String parentDependency : parentConfigDependencies) {
        if (! configDependencies.contains(parentDependency)) {
          configDependencies.add(parentDependency);
        }
      }
    }
  }

  /**
   * Merge configurations with the parent configurations.
   * This will update the child configuration module set as well as the underlying info instances.
   *
   * @param parent          parent service module
   * @param allStacks       all stack modules
   * @param commonServices  common service modules
   */
  private void mergeConfigurations(
      ServiceModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    serviceInfo.getProperties().clear();
    serviceInfo.setAllConfigAttributes(new HashMap<>());

    Collection<ConfigurationModule> mergedModules = mergeChildModules(
        allStacks, commonServices, extensions, configurationModules, parent.configurationModules);
    for (ConfigurationModule module : mergedModules) {
      configurationModules.put(module.getId(), module);
      if(!module.isDeleted()) {
        serviceInfo.getProperties().addAll(module.getModuleInfo().getProperties());
        serviceInfo.setTypeAttributes(module.getConfigType(), module.getModuleInfo().getAttributes());
      }
    }
  }

  /**
   * Merge components with the parent configurations.
   * This will update the child component module set as well as the underlying info instances.
   *
   * @param parent          parent service module
   * @param allStacks       all stack modules
   * @param commonServices  common service modules
   */
  private void mergeComponents(
      ServiceModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    serviceInfo.getComponents().clear();
    Collection<ComponentModule> mergedModules = mergeChildModules(
        allStacks, commonServices, extensions, componentModules, parent.componentModules);
    componentModules.clear();
    for (ComponentModule module : mergedModules) {
      if (!module.isDeleted()){
        componentModules.put(module.getId(), module);
        serviceInfo.getComponents().add(module.getModuleInfo());
      }
    }
  }

  /**
   * Merge custom commands with the parent custom commands.
   *
   * @param parentCmds  parent custom command collection
   * @param childCmds   child custom command collection
   */
  //todo: duplicated in Component Module.  Can we use mergeChildModules?
  private void mergeCustomCommands(Collection<CustomCommandDefinition> parentCmds,
                                   Collection<CustomCommandDefinition> childCmds) {

    Collection<String> existingNames = new HashSet<>();

    for (CustomCommandDefinition childCmd : childCmds) {
      existingNames.add(childCmd.getName());
    }
    for (CustomCommandDefinition parentCmd : parentCmds) {
      if (! existingNames.contains(parentCmd.getName())) {
        childCmds.add(parentCmd);
      }
    }
  }

  /**
   * Finalize service configurations.
   * Ensure that all default type attributes are set.
   */
  private void finalizeConfiguration() {
    LOG.debug("Finalize config, number of configuration modules {}", configurationModules.size());
    hasConfigs = !(configurationModules.isEmpty());
    LOG.debug("Finalize config, hasConfigs {}", hasConfigs);

    for (ConfigurationModule config : configurationModules.values()) {
      ConfigurationInfo configInfo = config.getModuleInfo();
      configInfo.ensureDefaultAttributes();
      serviceInfo.setTypeAttributes(config.getConfigType(), configInfo.getAttributes());
    }
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  private Set<String> errorSet = new HashSet<>();

  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }

  /**
   * @return The service's directory
   */
  public ServiceDirectory getServiceDirectory() {
    return serviceDirectory;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }


  private void validateServiceInfo() {
    if (!serviceInfo.isValid()) {
      setValid(false);
      addErrors(serviceInfo.getErrors());
    }
  }

  /**
   * Whether the service is a special case where it does not include any config types
   * */
  public boolean hasConfigs(){
    return hasConfigs;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
