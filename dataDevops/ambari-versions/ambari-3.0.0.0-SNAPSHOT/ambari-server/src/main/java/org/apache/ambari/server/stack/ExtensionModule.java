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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.ExtensionMetainfoXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension module which provides all functionality related to parsing and fully
 * resolving extensions from the extension definition.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 *
 * <p>
 * Each extension node is identified by name and version, contains service
 * child nodes and may extend a single parent extension.
 * </p>
 *
 * <p>
 * Resolution of a extension is a depth first traversal up the inheritance chain where each extension node
 * calls resolve on its parent before resolving itself.  After the parent resolve call returns, all
 * ancestors in the inheritance tree are fully resolved.  The act of resolving the extension includes
 * resolution of the services children of the extension as well as merging of other extension
 * state with the fully resolved parent.
 * </p>
 *
 * <p>
 * Because a service may explicitly extend another service in a extension outside of the inheritance tree,
 * service child node resolution involves a depth first resolution of the extension associated with the
 * services explicit parent, if any.  This follows the same steps defined above fore extension node
 * resolution.  After the services explicit parent is fully resolved, the services state is merged
 * with it's parent.
 * </p>
 *
 * <p>
 * If a cycle in a extension definition is detected, an exception is thrown from the resolve call.
 * </p>
 *
 */
public class ExtensionModule extends BaseModule<ExtensionModule, ExtensionInfo> implements Validable {

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
   * Corresponding ExtensionInfo instance
   */
  private ExtensionInfo extensionInfo;

  /**
   * Encapsulates IO operations on extension directory
   */
  private ExtensionDirectory extensionDirectory;

  /**
   * Extension id which is in the form extensionName:extensionVersion
   */
  private String id;

  /**
   * validity flag
   */
  protected boolean valid = true;

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(ExtensionModule.class);

  /**
   * Constructor.
   * @param extensionDirectory  represents extension directory
   * @param stackContext    general stack context
   */
  public ExtensionModule(ExtensionDirectory extensionDirectory, StackContext stackContext) {
    this.extensionDirectory = extensionDirectory;
    this.stackContext = stackContext;
    this.extensionInfo = new ExtensionInfo();
    populateExtensionInfo();
  }

  public Map<String, ServiceModule> getServiceModules() {
	  return serviceModules;
  }

  /**
   * Fully resolve the extension. See extension resolution description in the class documentation.
   * If the extension has a parent, this extension will be merged against its fully resolved parent
   * if one is specified. Merging applies to all extension state including child service and
   * configuration modules.  Services may extend a service in another version in the
   * same extension hierarchy or may explicitly extend a service in a different
   * hierarchy.
   *
   * @param parentModule   not used.  Each extension determines its own parent since extensions don't
   *                       have containing modules
   * @param allStacks      all stacks modules contained in the stack definition
   * @param commonServices all common services
   * @param extensions     all extensions
   *
   * @throws AmbariException if an exception occurs during extension resolution
   */
  @Override
  public void resolve(
      ExtensionModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    moduleState = ModuleState.VISITED;
    checkExtensionName(allStacks);

    String parentVersion = extensionInfo.getParentExtensionVersion();
    mergeServicesWithExplicitParent(allStacks, commonServices, extensions);
    // merge with parent version of same extension definition
    if (parentVersion != null) {
      mergeExtensionWithParent(parentVersion, allStacks, commonServices, extensions);
    }
    moduleState = ModuleState.RESOLVED;
  }

  @Override
  public ExtensionInfo getModuleInfo() {
    return extensionInfo;
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
  }

  /**
   * Get the associated extension directory.
   *
   * @return associated extension directory
   */
  public ExtensionDirectory getExtensionDirectory() {
    return extensionDirectory;
  }

  /**
   * Merge the extension with its parent.
   *
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   * @param parentVersion  version of the extensions parent
   *
   * @throws AmbariException if an exception occurs merging with the parent
   */
  private void mergeExtensionWithParent(
      String parentVersion, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {

    String parentExtensionKey = extensionInfo.getName() + StackManager.PATH_DELIMITER + parentVersion;
    ExtensionModule parentExtension = extensions.get(parentExtensionKey);

    if (parentExtension == null) {
      throw new AmbariException("Extension '" + extensionInfo.getName() + ":" + extensionInfo.getVersion() +
          "' specifies a parent that doesn't exist");
    }

    resolveExtension(parentExtension, allStacks, commonServices, extensions);
    /*mergeConfigurations(parentStack, allStacks, commonServices);
    mergeRoleCommandOrder(parentStack);*/

    /*if (extensionInfo.getStackHooksFolder() == null) {
      extensionInfo.setStackHooksFolder(parentStack.getModuleInfo().getStackHooksFolder());
    }

    if (extensionInfo.getKerberosDescriptorFileLocation() == null) {
      extensionInfo.setKerberosDescriptorFileLocation(parentStack.getModuleInfo().getKerberosDescriptorFileLocation());
    }

    if (extensionInfo.getWidgetsDescriptorFileLocation() == null) {
      extensionInfo.setWidgetsDescriptorFileLocation(parentStack.getModuleInfo().getWidgetsDescriptorFileLocation());
    }*/

    mergeServicesWithParent(parentExtension, allStacks, commonServices, extensions);
  }

  /**
   * Merge child services with parent extension.
   *
   * @param parentExtension    parent extension module
   * @param allStacks          all stacks in stack definition
   * @param commonServices     all common services
   * @param extensions         all extensions
   *
   * @throws AmbariException if an exception occurs merging the child services with the parent extension
   */
  private void mergeServicesWithParent(
      ExtensionModule parentExtension, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    extensionInfo.getServices().clear();

    LOG.info("***Merging extension services with parent: " + parentExtension.getId());

    Collection<ServiceModule> mergedModules = mergeChildModules(
        allStacks, commonServices, extensions, serviceModules, parentExtension.serviceModules);
    for (ServiceModule module : mergedModules) {
      if(!module.isDeleted()){
        serviceModules.put(module.getId(), module);
        extensionInfo.getServices().add(module.getModuleInfo());
      }
    }
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
    if(isCommonServiceParent(parent)) {
      LOG.info("merging with common service: " + service.getModuleInfo().getName());
      mergeServiceWithCommonServiceParent(service, parent, allStacks, commonServices, extensions);
      LOG.info("display name: " + service.getModuleInfo().getDisplayName());
    } else {
      throw new AmbariException("The service '" + service.getModuleInfo().getName() + "' in extension '" + extensionInfo.getName() + ":"
          + extensionInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }
  }

  /**
   * @param allStacks        all stacks specified in the stack definition
   *
   * @throws AmbariException if the extension name is the same as any of the stacks
   */
  private void checkExtensionName(Map<String, StackModule> allStacks)
      throws AmbariException {

    String name = extensionInfo.getName();
    for (StackModule stack : allStacks.values()) {
      String stackName = stack.getModuleInfo().getName();
      if (name.equals(stackName)) {
        throw new AmbariException("The extension '" + name + "' has a name which matches a stack name");
      }
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
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in extension '" + extensionInfo.getName() + ":"
          + extensionInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }

    String baseServiceKey = parentToks[1] + StackManager.PATH_DELIMITER + parentToks[2];
    ServiceModule baseService = commonServices.get(baseServiceKey);
    if (baseService == null) {
      setValid(false);
      extensionInfo.setValid(false);
      String error = "The service '" + serviceInfo.getName() + "' in extension '" + extensionInfo.getName() + ":"
          + extensionInfo.getVersion() + "' extends a non-existent service: '" + parent + "'";
      addError(error);
      extensionInfo.addError(error);
    } else {
      if (baseService.isValid()) {
        service.resolveExplicit(baseService, allStacks, commonServices, extensions);
      } else {
        setValid(false);
        extensionInfo.setValid(false);
        addErrors(baseService.getErrors());
        extensionInfo.addErrors(baseService.getErrors());
      }
    }
  }

  /**
   * Populate the extension module and info from the extension definition.
   */
  private void populateExtensionInfo() {
    extensionInfo.setName(extensionDirectory.getExtensionDirName());
    extensionInfo.setVersion(extensionDirectory.getName());

    id = String.format("%s:%s", extensionInfo.getName(), extensionInfo.getVersion());

    LOG.debug("Adding new extension to known extensions, extensionName = {}, extensionVersion = {}", extensionInfo.getName(), extensionInfo.getVersion());


    //todo: give additional thought on handling missing metainfo.xml
    ExtensionMetainfoXml emx = extensionDirectory.getMetaInfoFile();
    if (emx != null) {
      if (!emx.isValid()) {
        extensionInfo.setValid(false);
        extensionInfo.addErrors(emx.getErrors());
      }
      extensionInfo.setParentExtensionVersion(emx.getExtends());
      extensionInfo.setStacks(emx.getStacks());
      extensionInfo.setExtensions(emx.getExtensions());
      extensionInfo.setActive(emx.getVersion().isActive());
      extensionInfo.setAutoLink(emx.isAutoLink());
    }

    try {
      // Read the service for this extension
      populateServices();
      if (!extensionInfo.isValid()) {
        setValid(false);
        addErrors(extensionInfo.getErrors());
      }

      //todo: shouldn't blindly catch Exception, re-evaluate this.
    } catch (Exception e) {
      String error = "Exception caught while populating services for extension: " +
          extensionInfo.getName() + "-" + extensionInfo.getVersion();
      setValid(false);
      extensionInfo.setValid(false);
      addError(error);
      extensionInfo.addError(error);
      LOG.error(error);
    }
  }

  /**
   * Populate the child services.
   */
  private void populateServices()throws AmbariException {
    for (ServiceDirectory serviceDir : extensionDirectory.getServiceDirectories()) {
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
      extensionInfo.setValid(metaInfoXml.isValid());
      setValid(metaInfoXml.isValid());
      extensionInfo.addErrors(metaInfoXml.getErrors());
      addErrors(metaInfoXml.getErrors());
      return;
    }
    List<ServiceInfo> serviceInfos = metaInfoXml.getServices();

    for (ServiceInfo serviceInfo : serviceInfos) {
      ServiceModule serviceModule = new ServiceModule(stackContext, serviceInfo, serviceDirectory);
      serviceModules.add(serviceModule);
      if (!serviceModule.isValid()){
        extensionInfo.setValid(false);
        setValid(false);
        extensionInfo.addErrors(serviceModule.getErrors());
        addErrors(serviceModule.getErrors());
      }
    }
    addServices(serviceModules);
  }

  /**
   * Resolve another extension module.
   *
   * @param parentExtension    extension module to be resolved
   * @param allStacks          all stack modules in stack definition
   * @param commonServices     all common services specified in the stack definition
   * @param extensions         all extensions
   * @throws AmbariException if unable to resolve the extension
   */
  private void resolveExtension(
          ExtensionModule parentExtension, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions)
          throws AmbariException {
    if (parentExtension.getModuleState() == ModuleState.INIT) {
	  parentExtension.resolve(null, allStacks, commonServices, extensions);
    } else if (parentExtension.getModuleState() == ModuleState.VISITED) {
      //todo: provide more information to user about cycle
      throw new AmbariException("Cycle detected while parsing extension definition");
    }
    if (!parentExtension.isValid() || (parentExtension.getModuleInfo() != null && !parentExtension.getModuleInfo().isValid())) {
      setValid(parentExtension.isValid());
      extensionInfo.setValid(parentExtension.extensionInfo.isValid());
      addErrors(parentExtension.getErrors());
      extensionInfo.addErrors(parentExtension.getErrors());
    }
  }

  /**
   * Add a child service module to the extension.
   *
   * @param service  service module to add
   */
  private void addService(ServiceModule service) {
    ServiceInfo serviceInfo = service.getModuleInfo();
    Object previousValue = serviceModules.put(service.getId(), service);
    if (previousValue == null) {
      extensionInfo.getServices().add(serviceInfo);
    }
  }

  /**
   * Add child service modules to the extension.
   *
   * @param services  collection of service modules to add
   */
  private void addServices(Collection<ServiceModule> services) {
    for (ServiceModule service : services) {
      addService(service);
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
  public Collection<String> getErrors() {
    return errorSet;
  }

  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }

}
