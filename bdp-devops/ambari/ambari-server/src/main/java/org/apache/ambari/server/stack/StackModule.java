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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradePack;
import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.BulkCommandDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RefreshCommand;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;


/**
 * Stack module which provides all functionality related to parsing and fully
 * resolving stacks from the stack definition.
 *
 * <p>
 * Each stack node is identified by name and version, contains service and configuration
 * child nodes and may extend a single parent stack.
 * </p>
 *
 * <p>
 * Resolution of a stack is a depth first traversal up the inheritance chain where each stack node
 * calls resolve on its parent before resolving itself.  After the parent resolve call returns, all
 * ancestors in the inheritance tree are fully resolved.  The act of resolving the stack includes
 * resolution of the configuration and services children of the stack as well as merging of other stack
 * state with the fully resolved parent.
 * </p>
 *
 * <p>
 * Configuration child node resolution involves merging configuration types, properties and attributes
 * with the fully resolved parent.
 * </p>
 *
 * <p>
 * Because a service may explicitly extend another service in a stack outside of the inheritance tree,
 * service child node resolution involves a depth first resolution of the stack associated with the
 * services explicit parent, if any.  This follows the same steps defined above fore stack node
 * resolution.  After the services explicit parent is fully resolved, the services state is merged
 * with it's parent.
 * </p>
 *
 * <p>
 * If a cycle in a stack definition is detected, an exception is thrown from the resolve call.
 * </p>
 *
 */
public class StackModule extends BaseModule<StackModule, StackInfo> implements Validable {

  /**
   * Context which provides access to external functionality
   */
  private StackContext stackContext;

  /**
   * Map of child configuration modules keyed by configuration type
   */
  private Map<String, ConfigurationModule> configurationModules = new HashMap<>();

  /**
   * Map of child service modules keyed by service name
   */
  private Map<String, ServiceModule> serviceModules = new HashMap<>();

  /**
   * Map of linked extension modules keyed by extension name + version
   */
  private Map<String, ExtensionModule> extensionModules = new HashMap<>();

  /**
   * Corresponding StackInfo instance
   */
  private StackInfo stackInfo;

  /**
   * Encapsulates IO operations on stack directory
   */
  private StackDirectory stackDirectory;

  /**
   * Stack id which is in the form stackName:stackVersion
   */
  private String id;

  /**
   * validity flag
   */
  protected boolean valid = true;

  /**
   * file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackModule.class);

  /**
   * Constructor.
   * @param stackDirectory  represents stack directory
   * @param stackContext    general stack context
   */
  public StackModule(StackDirectory stackDirectory, StackContext stackContext) {
    this.stackDirectory = stackDirectory;
    this.stackContext = stackContext;
    stackInfo = new StackInfo();
    populateStackInfo();
  }

  public Map<String, ServiceModule> getServiceModules() {
	  return serviceModules;
  }

  public Map<String, ExtensionModule> getExtensionModules() {
	  return extensionModules;
  }

  /**
   * Fully resolve the stack. See stack resolution description in the class documentation.
   * If the stack has a parent, this stack will be merged against its fully resolved parent
   * if one is specified.Merging applies to all stack state including child service and
   * configuration modules.  Services may extend a service in another version in the
   * same stack hierarchy or may explicitly extend a service in a stack in a different
   * hierarchy.
   *
   * @param parentModule   not used.  Each stack determines its own parent since stacks don't
   *                       have containing modules
   * @param allStacks      all stacks modules contained in the stack definition
   * @param commonServices all common services specified in the stack definition
   * @param extensions     all extension modules contained in the stack definition
   *
   * @throws AmbariException if an exception occurs during stack resolution
   */
  @Override
  public void resolve(
      StackModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    moduleState = ModuleState.VISITED;
    LOG.info(String.format("Resolve: %s:%s", stackInfo.getName(), stackInfo.getVersion()));
    String parentVersion = stackInfo.getParentStackVersion();
    mergeServicesWithExplicitParent(allStacks, commonServices, extensions);
    addExtensionServices();

    // merge with parent version of same stack definition
    if (parentVersion != null) {
      mergeStackWithParent(parentVersion, allStacks, commonServices, extensions);
    }

    for (ExtensionInfo extension : stackInfo.getExtensions()) {
      String extensionKey = extension.getName() + StackManager.PATH_DELIMITER + extension.getVersion();
      ExtensionModule extensionModule = extensions.get(extensionKey);
      if (extensionModule == null) {
        throw new AmbariException("Extension '" + stackInfo.getName() + ":" + stackInfo.getVersion() +
                        "' specifies an extension " + extensionKey + " that doesn't exist");
      }
      mergeStackWithExtension(extensionModule, allStacks, commonServices, extensions);
    }

    processUpgradePacks();
    processRepositories();
    processPropertyDependencies();
    validateBulkCommandComponents(allStacks);
    moduleState = ModuleState.RESOLVED;
  }

  @Override
  public StackInfo getModuleInfo() {
    return stackInfo;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void finalizeModule() {
    finalizeChildModules(serviceModules.values());
    finalizeChildModules(configurationModules.values());

    // This needs to be merged during the finalize to avoid the RCO from services being inherited by the children stacks
    // The RCOs from a service should only be inherited through the service.
    for (ServiceModule module : serviceModules.values()) {
      mergeRoleCommandOrder(module);
    }

    // Generate list of services that have no config types
    List<String> servicesWithNoConfigs = new ArrayList<>();
    for(ServiceModule serviceModule: serviceModules.values()){
      if (!serviceModule.hasConfigs()){
        servicesWithNoConfigs.add(serviceModule.getId());
      }
    }
    stackInfo.setServicesWithNoConfigs(servicesWithNoConfigs);
  }

  /**
   * Get the associated stack directory.
   *
   * @return associated stack directory
   */
  public StackDirectory getStackDirectory() {
    return stackDirectory;
  }

  /**
   * Merge the stack with its parent.
   *
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   * @param parentVersion  version of the stacks parent
   *
   * @throws AmbariException if an exception occurs merging with the parent
   */
  private void mergeStackWithParent(
      String parentVersion, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {

    String parentStackKey = stackInfo.getName() + StackManager.PATH_DELIMITER + parentVersion;
    StackModule parentStack = allStacks.get(parentStackKey);

    if (parentStack == null) {
      throw new AmbariException("Stack '" + stackInfo.getName() + ":" + stackInfo.getVersion() +
          "' specifies a parent that doesn't exist");
    }

    resolveStack(parentStack, allStacks, commonServices, extensions);
    mergeConfigurations(parentStack, allStacks, commonServices, extensions);
    mergeRoleCommandOrder(parentStack);

    // grab stack level kerberos_preconfigure.json from parent stack
    if (stackInfo.getKerberosDescriptorPreConfigurationFileLocation() == null) {
      stackInfo.setKerberosDescriptorPreConfigurationFileLocation(parentStack.getModuleInfo().getKerberosDescriptorPreConfigurationFileLocation());
    }

    mergeServicesWithParent(parentStack, allStacks, commonServices, extensions);
  }

  /**
   * Merge the stack with one of its linked extensions.
   *
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs merging with the parent
   */
  private void mergeStackWithExtension(
		  ExtensionModule extension, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {

  }

  /**
   * Merge child services with parent stack.
   *
   * @param parentStack    parent stack module
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs merging the child services with the parent stack
   */
  private void mergeServicesWithParent(
      StackModule parentStack, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    stackInfo.getServices().clear();
    Collection<ServiceModule> mergedModules = mergeChildModules(
        allStacks, commonServices, extensions, serviceModules, parentStack.serviceModules);

    List<String> removedServices = new ArrayList<>();

    for (ServiceModule module : mergedModules) {
      if (module.isDeleted()){
        removedServices.add(module.getId());
      } else {
        serviceModules.put(module.getId(), module);
        stackInfo.getServices().add(module.getModuleInfo());
      }
    }
    stackInfo.setRemovedServices(removedServices);
  }

  /**
   * Merge services with their explicitly specified parent if one has been specified.
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs while merging child services with their explicit parents
   */
  private void mergeServicesWithExplicitParent(
      Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions) throws AmbariException {
    for (ServiceModule service : serviceModules.values()) {
      ServiceInfo serviceInfo = service.getModuleInfo();
      String parent = serviceInfo.getParent();
      if (parent != null) {
        mergeServiceWithExplicitParent(service, parent, allStacks, commonServices, extensions);
      }
    }
  }

  /**
   * Merge a service with its explicitly specified parent.
   * @param service          the service to merge
   * @param parent           the explicitly specified parent service
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs merging a service with its explicit parent
   */
  private void mergeServiceWithExplicitParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {

    LOG.info(String.format("Merge service %s with explicit parent: %s", service.getModuleInfo().getName(), parent));
    if(isCommonServiceParent(parent)) {
      mergeServiceWithCommonServiceParent(service, parent, allStacks, commonServices, extensions);
    } else if(isExtensionServiceParent(parent)) {
      mergeServiceWithExtensionServiceParent(service, parent, allStacks, commonServices, extensions);
    } else {
      mergeServiceWithStackServiceParent(service, parent, allStacks, commonServices, extensions);
    }
  }

  /**
   * Check if parent is common service
   * @param parent  Parent string
   * @return true: if parent is common service, false otherwise
   */
  private boolean isCommonServiceParent(String parent) {
    return parent != null
        && !parent.isEmpty()
        && parent.split(StackManager.PATH_DELIMITER)[0].equalsIgnoreCase(StackManager.COMMON_SERVICES);
  }

  /**
   * Check if parent is extension service
   * @param parent  Parent string
   * @return true: if parent is extension service, false otherwise
   */
  private boolean isExtensionServiceParent(String parent) {
    return parent != null
        && !parent.isEmpty()
        && parent.split(StackManager.PATH_DELIMITER)[0].equalsIgnoreCase(StackManager.EXTENSIONS);
  }

  private void addExtensionServices() throws AmbariException {
    for (ExtensionModule extension : extensionModules.values()) {
      for (Map.Entry<String, ServiceModule> entry : extension.getServiceModules().entrySet()) {
        serviceModules.put(entry.getKey(), entry.getValue());
      }
      stackInfo.addExtension(extension.getModuleInfo());
    }
  }

  /**
   * Merge a service with its explicitly specified common service as parent.
   * Parent: common-services/<serviceName>/<serviceVersion>
   * Common Services Lookup Key: <serviceName>/<serviceVersion>
   * Example:
   *  Parent: common-services/HDFS/2.1.0.2.0
   *  Key: HDFS/2.1.0.2.0
   *
   * @param service          the service to merge
   * @param parent           the explicitly specified common service as parent
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services specified in the stack definition
   * @throws AmbariException
   */
  private void mergeServiceWithCommonServiceParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    ServiceInfo serviceInfo = service.getModuleInfo();
    String[] parentToks = parent.split(StackManager.PATH_DELIMITER);
    if(parentToks.length != 3 || !parentToks[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }

    String baseServiceKey = parentToks[1] + StackManager.PATH_DELIMITER + parentToks[2];
    ServiceModule baseService = commonServices.get(baseServiceKey);
    if (baseService == null) {
      setValid(false);
      stackInfo.setValid(false);
      String error = "The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'";
      addError(error);
      stackInfo.addError(error);
    } else {
      if (baseService.isValid()) {
        service.resolveExplicit(baseService, allStacks, commonServices, extensions);
      } else {
        setValid(false);
        stackInfo.setValid(false);
        addErrors(baseService.getErrors());
        stackInfo.addErrors(baseService.getErrors());
      }
    }
  }

  /**
   * Merge a service with its explicitly specified extension service as parent.
   * Parent: extensions/<extensionName>/<extensionVersion>/<serviceName>
   * Example:
   *  Parent: extensions/EXT_TEST/1.0/CUSTOM_SERVICE
   *
   * @param service          the service to merge
   * @param parent           the explicitly specified extension as parent
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services
   * @param extensions       all extensions
   * @throws AmbariException
   */
  private void mergeServiceWithExtensionServiceParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    ServiceInfo serviceInfo = service.getModuleInfo();
    String[] parentToks = parent.split(StackManager.PATH_DELIMITER);
    if(parentToks.length != 4 || !parentToks[0].equalsIgnoreCase(StackManager.EXTENSIONS)) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }

    String extensionKey = parentToks[1] + StackManager.PATH_DELIMITER + parentToks[2];
    ExtensionModule extension = extensions.get(extensionKey);

    if (extension == null || !extension.isValid()) {
      setValid(false);
      addError("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'");
    } else {
      resolveExtension(extension, allStacks, commonServices, extensions);
      ServiceModule parentService = extension.getServiceModules().get(parentToks[3]);
      if (parentService == null || !parentService.isValid()) {
        setValid(false);
        addError("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
            + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'");
      } else {
        service.resolve(parentService, allStacks, commonServices, extensions);
      }
    }
  }

  /**
   * Merge a service with its explicitly specified stack service as parent.
   * Parent: <stackName>/<stackVersion>/<serviceName>
   * Stack Lookup Key: <stackName>/<stackVersion>
   * Example:
   *  Parent: HDP/2.0.6/HDFS
   *  Key: HDP/2.0.6
   *
   * @param service          the service to merge
   * @param parent           the explicitly specified stack service as parent
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services
   * @param extensions       all extensions
   * @throws AmbariException
   */
  private void mergeServiceWithStackServiceParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    ServiceInfo serviceInfo = service.getModuleInfo();
    String[] parentToks = parent.split(StackManager.PATH_DELIMITER);
    if(parentToks.length != 3 || parentToks[0].equalsIgnoreCase(StackManager.EXTENSIONS) || parentToks[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }

    String baseStackKey = parentToks[0] + StackManager.PATH_DELIMITER + parentToks[1];
    StackModule baseStack = allStacks.get(baseStackKey);
    if (baseStack == null) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a service in a non-existent stack: '" + baseStackKey + "'");
    }

    resolveStack(baseStack, allStacks, commonServices, extensions);

    ServiceModule baseService = baseStack.serviceModules.get(parentToks[2]);
    if (baseService == null) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'");
      }
    service.resolveExplicit(baseService, allStacks, commonServices, extensions);
  }

  /**
   * Populate the stack module and info from the stack definition.
   */
  private void populateStackInfo() {
    stackInfo.setName(stackDirectory.getStackDirName());
    stackInfo.setVersion(stackDirectory.getName());

    id = String.format("%s:%s", stackInfo.getName(), stackInfo.getVersion());

    LOG.debug("Adding new stack to known stacks, stackName = {}, stackVersion = {}", stackInfo.getName(), stackInfo.getVersion());

    //todo: give additional thought on handling missing metainfo.xml
    StackMetainfoXml smx = stackDirectory.getMetaInfoFile();
    if (smx != null) {
      if (!smx.isValid()) {
        stackInfo.setValid(false);
        stackInfo.addErrors(smx.getErrors());
      }
      stackInfo.setMinJdk(smx.getMinJdk());
      stackInfo.setMaxJdk(smx.getMaxJdk());
      stackInfo.setActive(smx.getVersion().isActive());
      stackInfo.setParentStackVersion(smx.getExtends());
      stackInfo.setRcoFileLocation(stackDirectory.getRcoFilePath());
      stackInfo.setKerberosDescriptorPreConfigurationFileLocation(stackDirectory.getKerberosDescriptorPreconfigureFilePath());
      stackInfo.setUpgradesFolder(stackDirectory.getUpgradesDir());
      stackInfo.setUpgradePacks(stackDirectory.getUpgradePacks());
      stackInfo.setConfigUpgradePack(stackDirectory.getConfigUpgradePack());
      stackInfo.setRoleCommandOrder(stackDirectory.getRoleCommandOrder());
      stackInfo.setReleaseVersionClass(smx.getVersion().getReleaseVersion());
      stackInfo.setLibraryClassLoader(stackDirectory.getLibraryClassLoader());
      populateConfigurationModules();
    }

    try {
      //configurationModules
      RepositoryXml rxml = stackDirectory.getRepoFile();
      if (rxml != null && !rxml.isValid()) {
        stackInfo.setValid(false);
        stackInfo.addErrors(rxml.getErrors());
      }
      // Read the service and available configs for this stack
      populateServices();

      if (!stackInfo.isValid()) {
        setValid(false);
        addErrors(stackInfo.getErrors());
      }

      //todo: shouldn't blindly catch Exception, re-evaluate this.
    } catch (Exception e) {
      String error = "Exception caught while populating services for stack: " +
          stackInfo.getName() + "-" + stackInfo.getVersion();
      setValid(false);
      stackInfo.setValid(false);
      addError(error);
      stackInfo.addError(error);
      LOG.error(error);
    }
  }

  /**
   * Populate the child services.
   */
  private void populateServices()throws AmbariException {
    for (ServiceDirectory serviceDir : stackDirectory.getServiceDirectories()) {
      populateService(serviceDir);
    }
  }

  /**
   * Populate a child service.
   *
   * @param serviceDirectory the child service directory
   */
  private void populateService(ServiceDirectory serviceDirectory)  {
    Collection<ServiceModule> serviceModules = new ArrayList<>();
    // unfortunately, we allow multiple services to be specified in the same metainfo.xml,
    // so we can't move the unmarshal logic into ServiceModule
    ServiceMetainfoXml metaInfoXml = serviceDirectory.getMetaInfoFile();
    if (!metaInfoXml.isValid()){
      stackInfo.setValid(metaInfoXml.isValid());
      setValid(metaInfoXml.isValid());
      stackInfo.addErrors(metaInfoXml.getErrors());
      addErrors(metaInfoXml.getErrors());
      return;
    }
    List<ServiceInfo> serviceInfos = metaInfoXml.getServices();

    for (ServiceInfo serviceInfo : serviceInfos) {
      ServiceModule serviceModule = new ServiceModule(stackContext, serviceInfo, serviceDirectory);
      serviceModules.add(serviceModule);
      if (!serviceModule.isValid()) {
        stackInfo.setValid(false);
        setValid(false);
        stackInfo.addErrors(serviceModule.getErrors());
        addErrors(serviceModule.getErrors());
      }
    }
    addServices(serviceModules);
  }

  /**
   * Populate the child configurations.
   */
  private void populateConfigurationModules() {
    //todo: can't exclude types in stack config
    ConfigurationDirectory configDirectory = stackDirectory.getConfigurationDirectory(
        StackDirectory.SERVICE_CONFIG_FOLDER_NAME, StackDirectory.SERVICE_PROPERTIES_FOLDER_NAME);

    if (configDirectory != null) {
      for (ConfigurationModule config : configDirectory.getConfigurationModules()) {
        if (stackInfo.isValid()){
          stackInfo.setValid(config.isValid());
          stackInfo.addErrors(config.getErrors());
        }
        stackInfo.getProperties().addAll(config.getModuleInfo().getProperties());
        stackInfo.setConfigTypeAttributes(config.getConfigType(), config.getModuleInfo().getAttributes());
        configurationModules.put(config.getConfigType(), config);
      }
    }
  }

  /**
   * Merge configurations with the parent configurations.
   *
   * @param parent  parent stack module
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   */
  private void mergeConfigurations(
      StackModule parent, Map<String,StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    stackInfo.getProperties().clear();
    stackInfo.setAllConfigAttributes(new HashMap<>());

    Collection<ConfigurationModule> mergedModules = mergeChildModules(
        allStacks, commonServices, extensions, configurationModules, parent.configurationModules);
    for (ConfigurationModule module : mergedModules) {
      if(!module.isDeleted()){
        configurationModules.put(module.getId(), module);
        stackInfo.getProperties().addAll(module.getModuleInfo().getProperties());
        stackInfo.setConfigTypeAttributes(module.getConfigType(), module.getModuleInfo().getAttributes());
      }
    }
  }

  /**
   * Resolve another stack module.
   *
   * @param stackToBeResolved  stack module to be resolved
   * @param allStacks          all stack modules in stack definition
   * @param commonServices     all common services specified in the stack definition
   * @throws AmbariException if unable to resolve the stack
   */
  private void resolveStack(
          StackModule stackToBeResolved, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
          throws AmbariException {
    if (stackToBeResolved.getModuleState() == ModuleState.INIT) {
      stackToBeResolved.resolve(null, allStacks, commonServices, extensions);
    } else if (stackToBeResolved.getModuleState() == ModuleState.VISITED) {
      //todo: provide more information to user about cycle
      throw new AmbariException("Cycle detected while parsing stack definition");
    }
    if (!stackToBeResolved.isValid() || (stackToBeResolved.getModuleInfo() != null && !stackToBeResolved.getModuleInfo().isValid())) {
      setValid(stackToBeResolved.isValid());
      stackInfo.setValid(stackToBeResolved.stackInfo.isValid());
      addErrors(stackToBeResolved.getErrors());
      stackInfo.addErrors(stackToBeResolved.getErrors());
    }
  }

  /**
   * Resolve an extension module.
   *
   * @param extension              extension module to be resolved
   * @param allStacks              all stack modules in stack definition
   * @param commonServices         all common services
   * @param extensions             all extensions
   * @throws AmbariException if unable to resolve the stack
   */
  private void resolveExtension(
          ExtensionModule extension, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
          throws AmbariException {
    if (extension.getModuleState() == ModuleState.INIT) {
	  extension.resolve(null, allStacks, commonServices, extensions);
    } else if (extension.getModuleState() == ModuleState.VISITED) {
      //todo: provide more information to user about cycle
      throw new AmbariException("Cycle detected while parsing extension definition");
    }
    if (!extension.isValid() || (extension.getModuleInfo() != null && !extension.getModuleInfo().isValid())) {
      setValid(false);
      addError("Stack includes an invalid extension: " + extension.getModuleInfo().getName());
    }
  }

  /**
   * Add a child service module to the stack.
   *
   * @param service  service module to add
   */
  private void addService(ServiceModule service) {
    ServiceInfo serviceInfo = service.getModuleInfo();
    Object previousValue = serviceModules.put(service.getId(), service);
    if (previousValue == null) {
      stackInfo.getServices().add(serviceInfo);
    }
  }

  /**
   * Add child service modules to the stack.
   *
   * @param services  collection of service modules to add
   */
  private void addServices(Collection<ServiceModule> services) {
    for (ServiceModule service : services) {
      addService(service);
    }
  }

  /**
   * Process <depends-on></depends-on> properties
   */
  private void processPropertyDependencies() {

    // Stack-definition has 'depends-on' relationship specified.
    // We have a map to construct the 'depended-by' relationship.
    Map<PropertyDependencyInfo, Set<PropertyDependencyInfo>> dependedByMap =
      new HashMap<>();

    // Go through all service-configs and gather the reversed 'depended-by'
    // relationship into map. Since we do not have the reverse {@link PropertyInfo},
    // we have to loop through service-configs again later.
    for (ServiceModule serviceModule : serviceModules.values()) {

      Map<String, Map<String, String>> componentRefreshCommandsMap = new HashMap();

      for (PropertyInfo pi : serviceModule.getModuleInfo().getProperties()) {

        for (PropertyDependencyInfo pdi : pi.getDependsOnProperties()) {
          String type = ConfigHelper.fileNameToConfigType(pi.getFilename());
          String name = pi.getName();
          PropertyDependencyInfo propertyDependency =
            new PropertyDependencyInfo(type, name);
          if (dependedByMap.keySet().contains(pdi)) {
            dependedByMap.get(pdi).add(propertyDependency);
          } else {
            Set<PropertyDependencyInfo> newDependenciesSet =
              new HashSet<>();
            newDependenciesSet.add(propertyDependency);
            dependedByMap.put(pdi, newDependenciesSet);
          }
        }

        // set refresh commands
        if (pi.getSupportedRefreshCommands() != null && pi.getSupportedRefreshCommands().size() > 0) {
          String type = ConfigHelper.fileNameToConfigType(pi.getFilename());
          String propertyName = type + "/" + pi.getName();

          Map<String, String> refreshCommandPropertyMap = componentRefreshCommandsMap.get(propertyName);

          for (RefreshCommand refreshCommand : pi.getSupportedRefreshCommands()) {
            String componentName = refreshCommand.getComponentName();
            if (refreshCommandPropertyMap == null) {
              refreshCommandPropertyMap = new HashMap<>();
              componentRefreshCommandsMap.put(propertyName, refreshCommandPropertyMap);
            }
            refreshCommandPropertyMap.put(componentName, refreshCommand.getCommand());
          }

        }

      }

      stackInfo.getRefreshCommandConfiguration().addRefreshCommands(componentRefreshCommandsMap);
    }

    // Go through all service-configs again and set their 'depended-by' if necessary.
    for (ServiceModule serviceModule : serviceModules.values()) {
      addDependedByProperties(dependedByMap, serviceModule.getModuleInfo().getProperties());
    }
    // Go through all stack-configs again and set their 'depended-by' if necessary.
    addDependedByProperties(dependedByMap, stackInfo.getProperties());
  }

  /**
   * Add dependendByProperties to property info's
   * @param dependedByMap Map containing the 'depended-by' relationships
   * @param properties properties to check against dependedByMap
   */
  private void addDependedByProperties(Map<PropertyDependencyInfo, Set<PropertyDependencyInfo>> dependedByMap,
                                  Collection<PropertyInfo> properties) {
    for (PropertyInfo pi : properties) {
      String type = ConfigHelper.fileNameToConfigType(pi.getFilename());
      String name = pi.getName();
      Set<PropertyDependencyInfo> set =
        dependedByMap.remove(new PropertyDependencyInfo(type, name));
      if (set != null) {
        pi.getDependedByProperties().addAll(set);
      }
    }
  }

  /**
   * Process upgrade packs associated with the stack.
   * @throws AmbariException if unable to fully process the upgrade packs
   */
  private void processUpgradePacks() throws AmbariException {
    if (stackInfo.getUpgradePacks() == null) {
      return;
    }

    for (UpgradePack pack : stackInfo.getUpgradePacks().values()) {
      List<UpgradePack> servicePacks = new ArrayList<>();
      for (ServiceModule module : serviceModules.values()) {
        File upgradesFolder = module.getModuleInfo().getServiceUpgradesFolder();
        if (upgradesFolder != null) {
          UpgradePack servicePack = getServiceUpgradePack(pack, upgradesFolder);
          if (servicePack != null) {
            servicePacks.add(servicePack);
          }
        }
      }
      if (servicePacks.size() > 0) {
        LOG.info("Merging service specific upgrades for pack: " + pack.getName());
        mergeUpgradePack(pack, servicePacks);
      }
    }

    ConfigUpgradePack configPack = stackInfo.getConfigUpgradePack();
    if (configPack == null) {
      return;
    }

    for (ServiceModule module : serviceModules.values()) {
      File upgradesFolder = module.getModuleInfo().getServiceUpgradesFolder();
      if (upgradesFolder != null) {
        mergeConfigUpgradePack(configPack, upgradesFolder);
      }
    }
  }

  /**
   * Attempts to merge, into the stack config upgrade, all the config upgrades
   * for any service which specifies its own upgrade.
   */
  private void mergeConfigUpgradePack(ConfigUpgradePack pack, File upgradesFolder) throws AmbariException {
    File stackFolder = new File(upgradesFolder, stackInfo.getName());
    File versionFolder = new File(stackFolder, stackInfo.getVersion());
    File serviceConfig = new File(versionFolder, StackDefinitionDirectory.CONFIG_UPGRADE_XML_FILENAME_PREFIX);
    if (!serviceConfig.exists()) {
      return;
    }

    try {
      ConfigUpgradePack serviceConfigPack = unmarshaller.unmarshal(ConfigUpgradePack.class, serviceConfig);
      pack.services.addAll(serviceConfigPack.services);
    }
    catch (Exception e) {
      throw new AmbariException("Unable to parse service config upgrade file at location: " + serviceConfig.getAbsolutePath(), e);
    }
  }

  /**
   * Returns the upgrade pack for a service if it exists, otherwise returns null
   */
  private UpgradePack getServiceUpgradePack(UpgradePack pack, File upgradesFolder) throws AmbariException {
    File stackFolder = new File(upgradesFolder, stackInfo.getName());
    File versionFolder = new File(stackFolder, stackInfo.getVersion());
    // !!! relies on the service upgrade pack filename being named the exact same
    File servicePackFile = new File(versionFolder, pack.getName() + ".xml");

    LOG.info("Service folder: " + servicePackFile.getAbsolutePath());
    if (servicePackFile.exists()) {
      return parseServiceUpgradePack(pack, servicePackFile);
    } else {
      UpgradePack child = findServiceUpgradePack(pack, stackFolder);

      return null == child ? null : parseServiceUpgradePack(pack, child);
    }
  }

  /**
   * Attempts to merge, into the stack upgrade, all the upgrades
   * for any service which specifies its own upgrade.
   */
  private void mergeUpgradePack(UpgradePack pack, List<UpgradePack> servicePacks) throws AmbariException {

    List<Grouping> originalGroups = pack.getAllGroups();
    Map<String, List<Grouping>> allGroupMap = new HashMap<>();
    for (Grouping group : originalGroups) {
      List<Grouping> list = new ArrayList<>();
      list.add(group);
      allGroupMap.put(group.name, list);
    }

    for (UpgradePack servicePack : servicePacks) {
      for (Grouping group : servicePack.getAllGroups()) {

        /*
         !!! special case where the service pack is targeted for any version.  When
         a service UP targets to run after another group, check to make sure that the
         base UP contains the group.
         */
        if (servicePack.isAllTarget() && !allGroupMap.keySet().contains(group.addAfterGroup)) {
          LOG.warn("Service Upgrade Pack specified after-group of {}, but that is not found in {}",
              group.addAfterGroup, StringUtils.join(allGroupMap.keySet(), ','));
          continue;
        }

        if (allGroupMap.containsKey(group.name)) {
          List<Grouping> list = allGroupMap.get(group.name);
          Grouping first = list.get(0);
          if (!first.getClass().equals(group.getClass())) {
            throw new AmbariException("Expected class: " + first.getClass() + " instead of " + group.getClass());
          }
          /* If the current group doesn't specify an "after entry" and the first group does
             then the current group should be added first.  The first group in the list should
             never be ordered relative to any other group. */
          if (group.addAfterGroupEntry == null && first.addAfterGroupEntry != null) {
            list.add(0, group);
          }
          else {
            list.add(group);
          }
        } else {
          List<Grouping> list = new ArrayList<>();
          list.add(group);
          allGroupMap.put(group.name, list);
        }
      }
    }

    Map<String, Grouping> mergedGroupMap = new HashMap<>();
    for (String key : allGroupMap.keySet()) {
      Iterator<Grouping> iterator = allGroupMap.get(key).iterator();
      Grouping group = iterator.next();
      if (iterator.hasNext()) {
        group.merge(iterator);
      }
      mergedGroupMap.put(key, group);
    }

    orderGroups(originalGroups, mergedGroupMap);
  }

  /**
   * Orders the upgrade groups.  All new groups specified in a service's upgrade file must
   * specify after which group they should be placed in the upgrade order.
   */
  private void orderGroups(List<Grouping> groups, Map<String, Grouping> mergedGroupMap) throws AmbariException {
    Map<String, List<Grouping>> skippedGroups = new HashMap<>();

    for (Map.Entry<String, Grouping> entry : mergedGroupMap.entrySet()) {
      Grouping group = entry.getValue();

      if (!groups.contains(group)) {
        boolean added = addGrouping(groups, group);
        if (added) {
          addSkippedGroup(groups, skippedGroups, group);
        } else {
          List<Grouping> tmp = null;

          // store the group until later
          if (skippedGroups.containsKey(group.addAfterGroup)) {
            tmp = skippedGroups.get(group.addAfterGroup);
          } else {
            tmp = new ArrayList<>();
            skippedGroups.put(group.addAfterGroup, tmp);
          }
          tmp.add(group);
        }
      }
    }

    if (!skippedGroups.isEmpty()) {
      throw new AmbariException("Missing groups: " + skippedGroups.keySet());
    }
  }

  /**
   * Adds the group provided if the group which it should come after has been added.
   */
  private boolean addGrouping(List<Grouping> groups, Grouping group) throws AmbariException {
    if (group.addAfterGroup == null) {
      throw new AmbariException("Group " + group.name + " needs to specify which group it should come after");
    }
    else {
      // Check the current services, if the "after" service is there then add these
      for (int index = groups.size() - 1; index >= 0; index--) {
        String name = groups.get(index).name;
        if (name.equals(group.addAfterGroup)) {
          groups.add(index + 1, group);
          LOG.debug("Added group/after: {}/{}", group.name, group.addAfterGroup);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Adds any groups which have been previously skipped if the group which they should come
   * after have been added.
   */
  private void addSkippedGroup(List<Grouping> groups, Map<String, List<Grouping>> skippedGroups, Grouping groupJustAdded) throws AmbariException {
    if (skippedGroups.containsKey(groupJustAdded.name)) {
      List<Grouping> groupsToAdd = skippedGroups.remove(groupJustAdded.name);
      for (Grouping group : groupsToAdd) {
        boolean added = addGrouping(groups, group);
        if (added) {
          addSkippedGroup(groups, skippedGroups, group);
        } else {
          throw new AmbariException("Failed to add group " + group.name);
        }
      }
    }
  }

  /**
   * Finds an upgrade pack that:
   * <ul>
   *   <li>Is found in the $SERVICENAME/upgrades/$STACKNAME folder</li>
   *   <li>Matches the same {@link UpgradeType} as the {@code base} upgrade pack</li>
   *   <li>Has the {@link UpgradePack#getTarget()} value equals to "*"</li>
   *   <li>Has the {@link UpgradePack#getTargetStack()} value equals to "*"</li>
   * </ul>
   * This method will not attempt to resolve the "most correct" upgrade pack.  For this
   * feature to work, there should be only one upgrade pack per type.  If more specificity
   * is required, then follow the convention of $SERVICENAME/upgrades/$STACKNAME/$STACKVERSION/$BASE_FILE_NAME.xml
   *
   * @param base the base upgrade pack for a stack
   * @param upgradeStackDirectory service directory that contains stack upgrade files.
   * @return an upgrade pack that matches {@code base}
   */
  private UpgradePack findServiceUpgradePack(UpgradePack base, File upgradeStackDirectory) {
    if (!upgradeStackDirectory.exists() || !upgradeStackDirectory.isDirectory()) {
      return null;
    }

    File[] upgradeFiles = upgradeStackDirectory.listFiles(StackDirectory.XML_FILENAME_FILTER);
    if (0 == upgradeFiles.length) {
      return null;
    }

    for (File f : upgradeFiles) {
      try {
        UpgradePack upgradePack = unmarshaller.unmarshal(UpgradePack.class, f);

        // !!! if the type is the same and the target is "*", then it's good to merge
        if (upgradePack.isAllTarget() && upgradePack.getType() == base.getType()) {
          return upgradePack;
        }

      } catch (Exception e) {
        LOG.warn("File {} does not appear to be an upgrade pack and will be skipped ({})",
            f.getAbsolutePath(), e.getMessage());
      }
    }

    return null;
  }

  /**
   * Parses the service specific upgrade file and merges the none order elements
   * (prerequisite check and processing sections).
   */
  private UpgradePack parseServiceUpgradePack(UpgradePack parent, File serviceFile) throws AmbariException {
    UpgradePack pack = null;
    try {
      pack = unmarshaller.unmarshal(UpgradePack.class, serviceFile);
    }
    catch (Exception e) {
      throw new AmbariException("Unable to parse service upgrade file at location: " + serviceFile.getAbsolutePath(), e);
    }

    return parseServiceUpgradePack(parent, pack);
  }

  /**
   * Places prerequisite checks and processing objects onto the parent upgrade pack.
   *
   * @param parent  the parent upgrade pack
   * @param child   the parsed child upgrade pack
   * @return the child upgrade pack
   */
  private UpgradePack parseServiceUpgradePack(UpgradePack parent, UpgradePack child) {
    parent.mergePrerequisiteChecks(child);
    parent.mergeProcessing(child);

    return child;
  }


  /**
   * Process repositories associated with the stack.
   * @throws AmbariException if unable to fully process the stack repositories
   */
  private void processRepositories() throws AmbariException {
    List<RepositoryInfo> stackRepos = Collections.emptyList();
    RepositoryXml rxml = stackDirectory.getRepoFile();

    if (null != rxml) {
      stackInfo.setRepositoryXml(rxml);

      LOG.debug("Adding repositories to stack, stackName={}, stackVersion={}, repoFolder={}",
        stackInfo.getName(), stackInfo.getVersion(), stackDirectory.getRepoDir());

      stackRepos = rxml.getRepositories();


      stackInfo.getRepositories().addAll(stackRepos);
    }

    LOG.debug("Process service custom repositories");
    Collection<RepositoryInfo> serviceRepos = getUniqueServiceRepos(stackRepos);
    stackInfo.getRepositories().addAll(serviceRepos);

    if (null != rxml && null != rxml.getLatestURI() && stackRepos.size() > 0) {
      registerRepoUpdateTask(rxml);
    }
  }

  private void registerRepoUpdateTask(RepositoryXml rxml) {
    String latest = rxml.getLatestURI();
    if (StringUtils.isBlank(latest)) {
      return;
    }

    URI uri = getURI(this, latest);

    if (null == uri) {
      LOG.warn("Could not determine how to load stack {}-{} latest definition for {}",
          stackInfo.getName(), stackInfo.getVersion(), latest);
      return;
    }

    stackContext.registerRepoUpdateTask(uri, this);
  }

  /**
   * @param module
   *          the stack module
   * @param uriString
   *          the uri string
   * @return  a repo URI, even if it is relative-file based
   */
  public static URI getURI(StackModule module, String uriString) {

    URI uri = null;
    if (uriString.startsWith("http")) {
      try {
        uri = new URI(uriString);
      } catch (URISyntaxException e) {
        // should be logged later
      }
    } else if ('.' == uriString.charAt(0)) {
      uri = new File(module.getStackDirectory().getRepoDir(), uriString).toURI();
    } else {
      uri = new File(uriString).toURI();
    }

    return uri;
  }

  /**
   * Gets the service repos with duplicates filtered out. A service repo is considered duplicate if:
   * <ul>
   *   <li>It has the same name as a stack repo</li>
   *   <li>It has the same id as another service repo</li>
   * </ul>
   * Duplicate repo url's only results in warnings in the log. Duplicates are checked per os type, so e.g. the same repo
   * can exsist for centos5 and centos6.
   * @param stackRepos the list of stack repositories
   * @return the service repos with duplicates filtered out.
   */
   @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
     comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
   private Collection<RepositoryInfo> getUniqueServiceRepos(List<RepositoryInfo> stackRepos) {
    List<RepositoryInfo> serviceRepos = getAllServiceRepos();
    ImmutableListMultimap<String, RepositoryInfo> serviceReposByOsType = Multimaps.index(serviceRepos, RepositoryInfo.GET_OSTYPE_FUNCTION);
    ImmutableListMultimap<String, RepositoryInfo> stackReposByOsType = Multimaps.index(stackRepos, RepositoryInfo.GET_OSTYPE_FUNCTION);
     Map<String, RepositoryInfo> uniqueServiceRepos = new HashMap<>();

    // Uniqueness is checked for each os type
    for (String osType: serviceReposByOsType.keySet()) {
      List<RepositoryInfo> stackReposForOsType = stackReposByOsType.containsKey(osType) ? stackReposByOsType.get(osType) : Collections.emptyList();
      List<RepositoryInfo> serviceReposForOsType = serviceReposByOsType.get(osType);
      Set<String> stackRepoNames = ImmutableSet.copyOf(Lists.transform(stackReposForOsType, RepositoryInfo.GET_REPO_NAME_FUNCTION));
      Set<String> stackRepoUrls = ImmutableSet.copyOf(Lists.transform(stackReposForOsType, RepositoryInfo.SAFE_GET_BASE_URL_FUNCTION));
      Set<String> duplicateServiceRepoNames = findDuplicates(serviceReposForOsType, RepositoryInfo.GET_REPO_NAME_FUNCTION);
      Set<String> duplicateServiceRepoUrls = findDuplicates(serviceReposForOsType, RepositoryInfo.SAFE_GET_BASE_URL_FUNCTION);

      for (RepositoryInfo repo: serviceReposForOsType) {
        // These cases only generate warnings
        if (stackRepoUrls.contains(repo.getBaseUrl())) {
          LOG.warn("Service repo has a base url that is identical to that of a stack repo: {}", repo);
        }
        else if (duplicateServiceRepoUrls.contains(repo.getBaseUrl())) {
          LOG.warn("Service repo has a base url that is identical to that of another service repo: {}", repo);
        }
        // These cases cause the repo to be disregarded
        if (stackRepoNames.contains(repo.getRepoName())) {
          LOG.warn("Discarding service repository with the same name as one of the stack repos: {}", repo);
        }
        else if (duplicateServiceRepoNames.contains(repo.getRepoName())) {
          LOG.warn("Discarding service repository with duplicate name and different content: {}", repo);
        }
        else {
          String key = repo.getOsType() + "-" + repo.getRepoName() + "-" + repo.getRepoId();
          if(uniqueServiceRepos.containsKey(key)) {
            uniqueServiceRepos.get(key).getApplicableServices().addAll(repo.getApplicableServices());
          } else {
            uniqueServiceRepos.put(key, repo);
          }
        }
      }
    }
    return uniqueServiceRepos.values();
  }

  /**
   * Finds duplicate repository infos. Duplicateness is checked on the property specified in the keyExtractor.
   * Items that are equal don't count as duplicate, only differing items with the same key
   * @param input the input list
   * @param keyExtractor a function to that returns the property to be checked
   * @return a set containing the keys of duplicates
   */
  private static Set<String> findDuplicates(List<RepositoryInfo> input, Function<RepositoryInfo, String> keyExtractor) {
    ListMultimap<String, RepositoryInfo> itemsByKey = Multimaps.index(input, keyExtractor);
    Set<String> duplicates = new HashSet<>();
    for (Map.Entry<String, Collection<RepositoryInfo>> entry: itemsByKey.asMap().entrySet()) {
      if (entry.getValue().size() > 1) {
        Set<RepositoryInfo> differingItems = new HashSet<>();
        differingItems.addAll(entry.getValue());
        if (differingItems.size() > 1) {
          duplicates.add(entry.getKey());
        }
      }
    }
    return duplicates;
  }

  /**
   * Returns all service repositories for a given stack
   * @return a list of service repo definitions
   */
  private List<RepositoryInfo> getAllServiceRepos() {
    List<RepositoryInfo> repos = new ArrayList<>();
    for (ServiceModule sm: serviceModules.values()) {
      ServiceDirectory sd = sm.getServiceDirectory();
      if (sd instanceof StackServiceDirectory) {
        StackServiceDirectory ssd = (StackServiceDirectory) sd;
        RepositoryXml serviceRepoXml = ssd.getRepoFile();
        if (null != serviceRepoXml) {
          List<RepositoryInfo> serviceRepos = serviceRepoXml.getRepositories();
          for(RepositoryInfo serviceRepo : serviceRepos) {
            serviceRepo.getApplicableServices().add(sm.getId());
          }
          repos.addAll(serviceRepos);
          if (null != serviceRepoXml.getLatestURI()) {
            registerRepoUpdateTask(serviceRepoXml);
          }
        }
      }
    }
    return repos;
  }



  /**
   * Merge role command order with the parent stack
   *
   * @param parentStack parent stack
   */

  private void mergeRoleCommandOrder(StackModule parentStack) {
    stackInfo.getRoleCommandOrder().merge(parentStack.stackInfo.getRoleCommandOrder());
  }

  /**
   * Merge role command order with the service
   *
   * @param service    service
   */
  private void mergeRoleCommandOrder(ServiceModule service) {
    if (service.getModuleInfo().getRoleCommandOrder() == null) {
      return;
    }

    stackInfo.getRoleCommandOrder().merge(service.getModuleInfo().getRoleCommandOrder(), true);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Role Command Order for {}-{} service {}", stackInfo.getName(), stackInfo.getVersion(), service.getModuleInfo().getName());
      stackInfo.getRoleCommandOrder().printRoleCommandOrder(LOG);
    }
  }

  /**
   * Validate the component defined in the bulkCommand section is defined for the service
   * This needs to happen after the stack is resolved
   * */
  private void validateBulkCommandComponents(Map<String, StackModule> allStacks){
    if (null != stackInfo) {
      String currentStackId = stackInfo.getName() + StackManager.PATH_DELIMITER + stackInfo.getVersion();
      LOG.debug("Validate bulk command components for: {}", currentStackId);
      StackModule currentStack = allStacks.get(currentStackId);
      if (null != currentStack){
        for (ServiceModule serviceModule : currentStack.getServiceModules().values()) {
          ServiceInfo service = serviceModule.getModuleInfo();
          for(ComponentInfo component: service.getComponents()){
            BulkCommandDefinition bcd = component.getBulkCommandDefinition();
            if (null != bcd && null != bcd.getMasterComponent()){
              String name = bcd.getMasterComponent();
              ComponentInfo targetComponent = service.getComponentByName(name);
              if (null == targetComponent){
                String serviceName = service.getName();
                LOG.error(
                    String.format("%s bulk command section for service %s in stack %s references a component %s which doesn't exist.",
                        component.getName(), serviceName, currentStackId, name));
              }
            }
          }
        }
      }
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

  @Override
  public void addErrors(Collection<String> errors) {
    errorSet.addAll(errors);
  }

}
