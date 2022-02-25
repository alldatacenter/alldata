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

package org.apache.ambari.server.api.services;

import static org.apache.ambari.server.controller.spi.Resource.InternalType.Component;
import static org.apache.ambari.server.controller.spi.Resource.InternalType.HostComponent;
import static org.apache.ambari.server.controller.utilities.PropertyHelper.AGGREGATE_FUNCTION_IDENTIFIERS;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.MpackRequest;
import org.apache.ambari.server.controller.MpackResponse;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.customactions.ActionDefinitionManager;
import org.apache.ambari.server.events.AlertDefinitionDisabledEvent;
import org.apache.ambari.server.events.AlertDefinitionRegistrationEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.AmbariServiceAlertDefinitions;
import org.apache.ambari.server.mpack.MpackManager;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ScriptSource;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * ServiceInfo responsible getting information about cluster.
 */
@Singleton
public class AmbariMetaInfo {

  /**
   * This string is used in placeholder in places that are common for
   * all operating systems or in situations where os type is not important.
   */
  public static final String ANY_OS = "any";
  /**
   * Version of XML files with support of custom services and custom commands
   */
  public static final String SCHEMA_VERSION_2 = "2.0";

  /**
   * The filename for a Kerberos descriptor file at either the stack or service level
   */
  public static final String KERBEROS_DESCRIPTOR_FILE_NAME = "kerberos.json";

  /**
   * The filename for a Widgets descriptor file at either the stack or service level
   */
  public static final String WIDGETS_DESCRIPTOR_FILE_NAME = "widgets.json";

  private final static Logger LOG = LoggerFactory.getLogger(AmbariMetaInfo.class);


  // all the supported OS'es
  @Inject
  private OsFamily osFamily;

  /**
   * ALL_SUPPORTED_OS is dynamically generated list from loaded families from os_family.json
   * Instead of append values here, please, add new families in json for tests and production
   */
  private List<String> ALL_SUPPORTED_OS;

  private final ActionDefinitionManager adManager = new ActionDefinitionManager();
  private String serverVersion = "undefined";

  private File stackRoot;
  private File commonServicesRoot;
  private File extensionsRoot;
  private File serverVersionFile;
  private File commonWidgetsDescriptorFile;
  private File customActionRoot;

  private String commonKerberosDescriptorFileLocation;
  Map<String, VersionDefinitionXml> versionDefinitions = null;

  private File mpacksV2Staging;


  /**
   * Alert Definition DAO used to merge stack definitions into the database.
   */
  @Inject
  private AlertDefinitionDAO alertDefinitionDao;

  /**
   * A factory that assists in the creation of {@link AlertDefinition} and
   * {@link AlertDefinitionEntity}.
   */
  @Inject
  private AlertDefinitionFactory alertDefinitionFactory;

  /**
   * All of the {@link AlertDefinition}s that are scoped for the agents.
   */
  @Inject
  private AmbariServiceAlertDefinitions ambariServiceAlertDefinitions;

  /**
   * Publishes the following events:
   * <ul>
   * <li>{@link AlertDefinitionRegistrationEvent} when new alerts are merged
   * from the stack</li>
   * <li>{@link AlertDefinitionDisabledEvent} when existing definitions are
   * disabled after being removed from the current stack</li>
   * </ul>
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  /**
   * KerberosDescriptorFactory used to create KerberosDescriptor instances
   */
  @Inject
  private KerberosDescriptorFactory kerberosDescriptorFactory;

  /**
   * KerberosServiceDescriptorFactory used to create KerberosServiceDescriptor instances
   */
  @Inject
  private KerberosServiceDescriptorFactory kerberosServiceDescriptorFactory;

  /**
   * Factory for injecting {@link StackManager} instances.
   */
  @Inject
  private StackManagerFactory stackManagerFactory;

  /**
   * Singleton instance of the stack manager.
   */
  private StackManager stackManager;

  /**
   * Factory for injecting {@link MpackManager} instances.
   */
  @Inject
  private MpackManagerFactory mpackManagerFactory;

  /**
  * Singleton instance of mpack manager
  */
  private MpackManager mpackManager;


  private Configuration conf;

  /**
   * Ambari Meta Info Object
   *
   * @param conf Configuration API to be used.
   * @throws Exception
   */
  @Inject
  public AmbariMetaInfo(Configuration conf) throws Exception {
    this.conf = conf;
    String stackPath = conf.getMetadataPath();
    stackRoot = new File(stackPath);

    String commonServicesPath = conf.getCommonServicesPath();
    if(commonServicesPath != null && !commonServicesPath.isEmpty()) {
      commonServicesRoot = new File(commonServicesPath);
    }

    String extensionsPath = conf.getExtensionsPath();
    if (extensionsPath != null && !extensionsPath.isEmpty()) {
      extensionsRoot = new File(extensionsPath);
    }

    String serverVersionFilePath = conf.getServerVersionFilePath();
    serverVersionFile = new File(serverVersionFilePath);

    customActionRoot = new File(conf.getCustomActionDefinitionPath());


    commonKerberosDescriptorFileLocation = new File(conf.getResourceDirPath(), KERBEROS_DESCRIPTOR_FILE_NAME).getAbsolutePath();
    commonWidgetsDescriptorFile = new File(conf.getResourceDirPath(), WIDGETS_DESCRIPTOR_FILE_NAME);

    String mpackV2StagingPath = conf.getMpacksV2StagingPath();
    mpacksV2Staging = new File(mpackV2StagingPath);

  }

  /**
   * Initialize the Ambari Meta Info
   *
   * @throws Exception throws exception if not able to parse the Meta data.
   */
  @Inject
  public void init() throws Exception {
    // Need to be initialized before all actions
    ALL_SUPPORTED_OS = new ArrayList<>(osFamily.os_list());

    readServerVersion();

    stackManager = stackManagerFactory.create(stackRoot, commonServicesRoot, extensionsRoot,
        osFamily, false);

    mpackManager = mpackManagerFactory.create(mpacksV2Staging, stackRoot);

    getCustomActionDefinitions(customActionRoot);
  }

  /**
   * Obtain the underlying stack manager.
   * @return stack manager
   */
  public StackManager getStackManager() {
    return stackManager;
  }

  /**
   * Obtain the underlying mpack manager.
   * @return mpack manager
   */
  public MpackManager getMpackManager() {
    return mpackManager;
  }

  /**
   * Get components by service
   *
   * @param stackName     stack name
   * @param version       stack version
   * @param serviceName   service name
   * @return List of ComponentInfo objects
   * @throws AmbariException
   */
  public List<ComponentInfo> getComponentsByService(String stackName, String version, String serviceName)
      throws AmbariException {

    ServiceInfo service;
    try {
      service = getService(stackName, version, serviceName);
    } catch (StackAccessException e) {
      throw new ParentObjectNotFoundException("Parent Service resource doesn't exist. stackName=" +
          stackName + ", stackVersion=" + version + ", serviceName=" + serviceName);
    }
    return service.getComponents();
  }

  public ComponentInfo getComponent(String stackName, String version, String serviceName,
                                    String componentName) throws AmbariException {

    ComponentInfo component = getService(stackName, version, serviceName).getComponentByName(componentName);

    if (component == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", componentName=" + componentName);
    }
    return component;
  }

  /**
   * Get all dependencies for a component.
   *
   * @param stackName  stack name
   * @param version    stack version
   * @param service    service name
   * @param component  component name
   *
   * @return List of dependencies for the given component
   * @throws AmbariException if unable to obtain the dependencies for the component
   */
  public List<DependencyInfo> getComponentDependencies(String stackName, String version,
                                                       String service, String component)
                                                       throws AmbariException {
    ComponentInfo componentInfo;
    try {
      componentInfo = getComponent(stackName, version, service, component);
    } catch (StackAccessException e) {
      throw new ParentObjectNotFoundException("Parent Component resource doesn't exist", e);
    }
    return componentInfo.getDependencies();
  }

  /**
   * Obtain a specific dependency by name.
   *
   * @param stackName       stack name
   * @param version         stack version
   * @param service         service name
   * @param component       component name
   * @param dependencyName  dependency component name
   *
   * @return the requested dependency
   * @throws AmbariException if unable to obtain the requested dependency
   */
  public DependencyInfo getComponentDependency(String stackName, String version, String service,
                                               String component, String dependencyName) throws AmbariException {

    DependencyInfo foundDependency = null;
    List<DependencyInfo> componentDependencies = getComponentDependencies(
      stackName, version, service, component);
    Iterator<DependencyInfo> iter = componentDependencies.iterator();
    while (foundDependency == null && iter.hasNext()) {
      DependencyInfo dependency = iter.next();
      if (dependencyName.equals(dependency.getComponentName())) {
        foundDependency = dependency;
      }
    }
    if (foundDependency == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion= " + version
          + ", stackService=" + service
          + ", stackComponent= " + component
          + ", dependency=" + dependencyName);
    }

    return foundDependency;
  }

  /**
   * Gets repositories that are keyed by operating system type.
   * @param stackName the stack name
   * @param version   the stack version
   * @return
   * @throws AmbariException
   */
  public Map<String, List<RepositoryInfo>> getRepository(String stackName,
                                                         String version) throws AmbariException {
    StackInfo stack = getStack(stackName, version);
    List<RepositoryInfo> repository = stack.getRepositories();

    Map<String, List<RepositoryInfo>> reposResult = new HashMap<>();
    for (RepositoryInfo repo : repository) {
      if (!reposResult.containsKey(repo.getOsType())) {
        reposResult.put(repo.getOsType(), new ArrayList<>());
      }
      reposResult.get(repo.getOsType()).add(repo);
    }
    return reposResult;
  }

  public List<RepositoryInfo> getRepositories(String stackName,
                                              String version, String osType) throws AmbariException {

    StackInfo stack = getStack(stackName, version);
    List<RepositoryInfo> repositories = stack.getRepositories();

    List<RepositoryInfo> repositoriesResult = new ArrayList<>();
    for (RepositoryInfo repository : repositories) {
      if (repository.getOsType().equals(osType)) {
        repositoriesResult.add(repository);
      }
    }
    return repositoriesResult;
  }

  public RepositoryInfo getRepository(String stackName,
                                      String version, String osType, String repoId) throws AmbariException {

    List<RepositoryInfo> repositories = getRepositories(stackName, version, osType);

    if (repositories.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType
          + ", repoId=" + repoId);
    }

    RepositoryInfo repoResult = null;
    for (RepositoryInfo repository : repositories) {
      if (repository.getRepoId().equals(repoId)) {
        repoResult = repository;
      }
    }
    if (repoResult == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion= " + version
          + ", osType=" + osType
          + ", repoId= " + repoId);
    }
    return repoResult;
  }

  /*
   * function for given a stack name and version, is it a supported stack
   */
  public boolean isSupportedStack(String stackName, String version) {
    try {
      // thrown an exception if the stack doesn't exist
      getStack(stackName, version);
      return true;
    } catch (AmbariException e) {
      return false;
    }
  }

  /*
   * support isValidService(), isValidComponent for a given stack/version
   */
  public boolean isValidService(String stackName, String version, String serviceName){

    try {
      getService(stackName, version, serviceName);
      return true;
    } catch (AmbariException e) {
      return false;
    }
  }

  /*
   * support isValidService(), isValidComponent for a given stack/version
   */
  public boolean isValidServiceComponent(String stackName, String version,
                                         String serviceName, String componentName) {
    try {
      getService(stackName, version, serviceName).getComponentByName(componentName);
      return true;
    } catch (AmbariException e) {
      return false;
    }
  }

  /**
   * Get the name of a service given the component name.
   *
   * @param stackName     the stack name
   * @param version       the stack version
   * @param componentName the component name
   * @return the service name
   * @throws AmbariException
   */
  public String getComponentToService(String stackName, String version,
                                      String componentName) throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Looking for service for component, stackName={}, stackVersion={}, componentName={}", stackName, version, componentName);
    }
    Map<String, ServiceInfo> services = getServices(stackName, version);
    String retService = null;
    if (services == null || services.isEmpty()) {
      return retService;
    }
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      ComponentInfo vu = entry.getValue().getComponentByName(componentName);
      if(vu != null){
        retService = entry.getKey();
        break;
      }
    }
    return retService;
  }

  /**
   * Given a stack name and version return all the services with info
   *
   * @param stackName the stack name
   * @param version   the version of the stack
   * @return the information of abt various services that are supported in the stack
   * @throws AmbariException
   */
  public Map<String, ServiceInfo> getServices(String stackName, String version) throws AmbariException {

    Map<String, ServiceInfo> servicesInfoResult = new HashMap<>();

    Collection<ServiceInfo> services;
    StackInfo stack;
    try {
      stack = getStack(stackName, version);
    } catch (StackAccessException e) {
      throw new ParentObjectNotFoundException("Parent Stack Version resource doesn't exist", e);
    }

    services = stack.getServices();
    if (services != null) {
      for (ServiceInfo service : services) {
        servicesInfoResult.put(service.getName(), service);
      }
    }
    return servicesInfoResult;
  }

  /**
   * Convenience method to use stack id instead of separate name and version.
   * @param service
   *            the service business object
   * @return  the service info instance defined from the stack for the business object
   * @throws AmbariException
   */
  public ServiceInfo getService(Service service) throws AmbariException {
    StackId stackId = service.getDesiredStackId();
    return getService(stackId.getStackName(), stackId.getStackVersion(), service.getName());
  }

  public ServiceInfo getService(String stackName, String version, String serviceName) throws AmbariException {
    ServiceInfo service = getStack(stackName, version).getService(serviceName);

    if (service == null) {
      throw new StackAccessException("stackName=" + stackName + ", stackVersion=" +
                                     version + ", serviceName=" + serviceName);
    }

    return service;
  }

  public boolean isServiceRemovedInStack(String stackName, String version, String serviceName) throws AmbariException{
    StackInfo stack = getStack(stackName, version);
    List<String> removedServices = stack.getRemovedServices();
    return removedServices.contains(serviceName);
  }

  public boolean isServiceWithNoConfigs(String stackName, String version, String serviceName) throws AmbariException{
    StackInfo stack = getStack(stackName, version);
    List<String> servicesWithNoConfigs = stack.getServicesWithNoConfigs();
    return servicesWithNoConfigs.contains(serviceName);
  }

  public Collection<String> getMonitoringServiceNames(String stackName, String version)
    throws AmbariException{

    List<String> monitoringServices = new ArrayList<>();
    for (ServiceInfo service : getServices(stackName, version).values()) {
      if ((service.isMonitoringService() != null) && service.isMonitoringService()) {
        monitoringServices.add(service.getName());
      }
    }
    return monitoringServices;
  }

  public Set<String> getRestartRequiredServicesNames(String stackName, String version)
    throws AmbariException{

    HashSet<String> needRestartServices = new HashSet<>();
    Collection<ServiceInfo> serviceInfos = getServices(stackName, version).values();

    for (ServiceInfo service : serviceInfos) {

      Boolean restartRequiredAfterChange = service.isRestartRequiredAfterChange();

      if (restartRequiredAfterChange != null && restartRequiredAfterChange) {
        needRestartServices.add(service.getName());
      }
    }
    return needRestartServices;
  }

  /**
   * Get the set of names of services which require restart when host rack information is changed.
   *
   * @param stackName  the stack name
   * @param version    the stack version
   *
   * @return the set of services which require restart when host rack information is changed
   *
   * @throws AmbariException if the service set can not be acquired
   */
  public Set<String> getRackSensitiveServicesNames(String stackName, String version)
      throws AmbariException {

    HashSet<String> needRestartServices = new HashSet<>();

    Collection<ServiceInfo> serviceInfos = getServices(stackName, version).values();

    for (ServiceInfo service : serviceInfos) {

      Boolean restartRequiredAfterRackChange = service.isRestartRequiredAfterRackChange();

      if (restartRequiredAfterRackChange != null && restartRequiredAfterRackChange) {
        needRestartServices.add(service.getName());
      }
    }
    return needRestartServices;
  }

  public Collection<StackInfo> getStacks() {
    return stackManager.getStacks();
  }

  /**
   * Calls the registerMpack method from mpackManager to support a POST /mpacks request
   * @param mpackRequest
   * @return MpackResponse
   * @throws IOException
   * @throws ResourceAlreadyExistsException
   */
  public MpackResponse registerMpack(MpackRequest mpackRequest) throws IOException, ResourceAlreadyExistsException {
    if (versionDefinitions != null) {
      versionDefinitions.clear();
    }
    return mpackManager.registerMpack(mpackRequest);
  }

  /**
   * Gets the module information for given mpack.
   * @param mpackId
   * @return List of Modules.
   */
  public List<Module> getModules(Long mpackId) {
    return mpackManager.getModules(mpackId);
  }


  public Collection<StackInfo> getStacks(String stackName) throws AmbariException {
    Collection<StackInfo> stacks = stackManager.getStacks(stackName);

    if (stacks.isEmpty()) {
      throw new StackAccessException("stackName=" + stackName);
    }

    return stacks;
  }

  /**
   * Convenience method to get stack info from a stack id
   * @param stackId
   *            the stack id
   * @return  the stack info
   * @throws AmbariException
   */
  public StackInfo getStack(StackId stackId) throws AmbariException {
    return getStack(stackId.getStackName(), stackId.getStackVersion());
  }

  public StackInfo getStack(String stackName, String version) throws AmbariException {
    StackInfo stackInfoResult = stackManager.getStack(stackName, version);

    if (stackInfoResult == null) {
      throw new StackAccessException("Stack " + stackName + " " + version + " is not found in Ambari metainfo");
    }

    return stackInfoResult;
  }

  public List<String> getStackParentVersions(String stackName, String version) {
    List<String> parents = new ArrayList<>();
    try {
      StackInfo stackInfo = getStack(stackName, version);
      String parentVersion = stackInfo.getParentStackVersion();
      if (parentVersion != null) {
        parents.add(parentVersion);
        parents.addAll(getStackParentVersions(stackName, parentVersion));
      }
    } catch (AmbariException e) {
      // parent was not found.
    }
    return parents;
  }

  public Collection<ExtensionInfo> getExtensions() {
    return stackManager.getExtensions();
  }

  public Collection<ExtensionInfo> getExtensions(String extensionName) throws AmbariException {
    Collection<ExtensionInfo> extensions = stackManager.getExtensions(extensionName);

    if (extensions.isEmpty()) {
      throw new StackAccessException("extensionName=" + extensionName);
    }

    return extensions;
  }

  public ExtensionInfo getExtension(String extensionName, String version) throws AmbariException {
    ExtensionInfo result = stackManager.getExtension(extensionName, version);

    if (result == null) {
      throw new StackAccessException("Extension " + extensionName + " " + version + " is not found in Ambari metainfo");
    }

    return result;
  }

  public Set<PropertyInfo> getServiceProperties(String stackName, String version, String serviceName)
      throws AmbariException {

    return new HashSet<>(getService(stackName, version, serviceName).getProperties());
  }

  public Set<PropertyInfo> getStackProperties(String stackName, String version)
      throws AmbariException {

    return new HashSet<>(getStack(stackName, version).getProperties());
  }

  public Set<PropertyInfo> getPropertiesByName(String stackName, String version, String serviceName, String propertyName)
      throws AmbariException {

    Set<PropertyInfo> properties = serviceName == null ?
      getStackProperties(stackName, version)
      : getServiceProperties(stackName, version, serviceName);

    if (properties.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", propertyName=" + propertyName);
    }

    Set<PropertyInfo> propertyResult = new HashSet<>();

    for (PropertyInfo property : properties) {
      if (property.getName().equals(propertyName)) {
        propertyResult.add(property);
      }
    }

    if (propertyResult.isEmpty()) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", propertyName=" + propertyName);
    }

    return propertyResult;
  }

  public Set<PropertyInfo> getStackPropertiesByName(String stackName, String version, String propertyName)
      throws AmbariException {
    Set<PropertyInfo> properties = getStackProperties(stackName, version);

    if (properties.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", propertyName=" + propertyName);
    }

    Set<PropertyInfo> propertyResult = new HashSet<>();

    for (PropertyInfo property : properties) {
      if (property.getName().equals(propertyName)) {
        propertyResult.add(property);
      }
    }

    if (propertyResult.isEmpty()) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", propertyName=" + propertyName);
    }

    return propertyResult;
  }

  /**
   * Lists operatingsystems supported by stack
   */
  public Set<OperatingSystemInfo> getOperatingSystems(String stackName, String version)
      throws AmbariException {

    Set<OperatingSystemInfo> operatingSystems = new HashSet<>();
    StackInfo stack = getStack(stackName, version);
    List<RepositoryInfo> repositories = stack.getRepositories();
    for (RepositoryInfo repository : repositories) {
      operatingSystems.add(new OperatingSystemInfo(repository.getOsType()));
    }

    return operatingSystems;
  }

  public OperatingSystemInfo getOperatingSystem(String stackName, String version, String osType)
      throws AmbariException {

    Set<OperatingSystemInfo> operatingSystems = getOperatingSystems(stackName, version);

    if (operatingSystems.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType);
    }

    OperatingSystemInfo resultOperatingSystem = null;

    for (OperatingSystemInfo operatingSystem : operatingSystems) {
      if (operatingSystem.getOsType().equals(osType)) {
        resultOperatingSystem = operatingSystem;
        break;
      }
    }

    if (resultOperatingSystem == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType);
    }

    return resultOperatingSystem;
  }

  private void readServerVersion() throws Exception {
    File versionFile = serverVersionFile;
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    Scanner scanner = new Scanner(versionFile);
    serverVersion = scanner.useDelimiter("\\Z").next();
    scanner.close();
  }

  private void getCustomActionDefinitions(File customActionDefinitionRoot) throws JAXBException, AmbariException {
    if (customActionDefinitionRoot != null) {
      LOG.debug("Loading custom action definitions from {}", customActionDefinitionRoot.getAbsolutePath());

      if (customActionDefinitionRoot.exists() && customActionDefinitionRoot.isDirectory()) {
        adManager.readCustomActionDefinitions(customActionDefinitionRoot);
      } else {
        LOG.debug("No action definitions found at {}", customActionDefinitionRoot.getAbsolutePath());
      }
    }
  }

  /**
   * Get all action definitions
   */
  public List<ActionDefinition> getAllActionDefinition(){
    return adManager.getAllActionDefinition();
  }

  /**
   * Get action definitions based on the supplied name
   */
  public ActionDefinition getActionDefinition(String name){
    return adManager.getActionDefinition(name);
  }


  /**
   * Used for test purposes
   */
  public void addActionDefinition(ActionDefinition ad) throws AmbariException {
    adManager.addActionDefinition(ad);
  }

  public String getServerVersion() {
    return serverVersion;
  }

  public boolean isOsSupported(String osType) {
    return ALL_SUPPORTED_OS.contains(osType);
  }


  public File getStackRoot() {
    return stackRoot;
  }

  public File getExtensionsRoot() {
    return extensionsRoot;
  }

  /**
   * Return metrics for a stack service.
   */
  public Map<String, Map<String, List<MetricDefinition>>> getServiceMetrics(String stackName,
            String stackVersion, String serviceName) throws AmbariException {

    ServiceInfo svc = getService(stackName, stackVersion, serviceName);

    if (null == svc.getMetricsFile() || !svc.getMetricsFile().exists()) {
      LOG.debug("Metrics file for {}/{}/{} not found.", stackName, stackVersion, serviceName);
      return null;
    }

    Map<String, Map<String, List<MetricDefinition>>> map = svc.getMetrics();

    // check for cached
    if (null == map) {
      // data layout:
      // "DATANODE" -> "Component" -> [ MetricDefinition, MetricDefinition, ... ]
      //           \-> "HostComponent" -> [ MetricDefinition, ... ]
      Type type = new TypeToken<Map<String, Map<String, List<MetricDefinition>>>>(){}.getType();

      Gson gson = new Gson();

      try {
        map = gson.fromJson(new FileReader(svc.getMetricsFile()), type);

        svc.setMetrics(processMetricDefinition(map));

      } catch (Exception e) {
        LOG.error ("Could not read the metrics file", e);
        throw new AmbariException("Could not read metrics file", e);
      }
    }

    return map;
  }

  /**
   * Add aggregate function support for all stack defined metrics.
   *
   * Refactor Namenode RPC metrics for different kinds of ports.
   */
  private Map<String, Map<String, List<MetricDefinition>>> processMetricDefinition(
    Map<String, Map<String, List<MetricDefinition>>> metricMap) {

    if (!metricMap.isEmpty()) {
        // For every Component
      for (Map.Entry<String, Map<String, List<MetricDefinition>>> componentMetricDefEntry : metricMap.entrySet()) {
        String componentName = componentMetricDefEntry.getKey();
        // For every Component / HostComponent category
        for (Map.Entry<String, List<MetricDefinition>> metricDefEntry : componentMetricDefEntry.getValue().entrySet()) {
          //For every metric definition
          for (MetricDefinition metricDefinition : metricDefEntry.getValue()) {
            for (Map.Entry<String, Map<String, Metric>> metricByCategory : metricDefinition.getMetricsByCategory().entrySet()) {
              Iterator<Map.Entry<String, Metric>> iterator = metricByCategory.getValue().entrySet().iterator();
              Map<String, Metric> newMetricsToAdd = new HashMap<>();

              while (iterator.hasNext()) {
                Map.Entry<String, Metric> metricEntry = iterator.next();
                // Process Namenode rpc metrics
                Map<String, Metric> processedMetrics = PropertyHelper.processRpcMetricDefinition(metricDefinition.getType(),
                  componentName, metricEntry.getKey(), metricEntry.getValue());
                if (processedMetrics != null) {
                  iterator.remove(); // Remove current metric entry
                  newMetricsToAdd.putAll(processedMetrics);
                } else {
                  processedMetrics = Collections.singletonMap(metricEntry.getKey(), metricEntry.getValue());
                }

                // NOTE: Only Component aggregates for AMS supported for now.
                if (metricDefinition.getType().equals("ganglia") &&
                  (metricDefEntry.getKey().equals(Component.name()) ||
                    metricDefEntry.getKey().equals(HostComponent.name()))) {
                  for (Map.Entry<String, Metric> processedMetric : processedMetrics.entrySet()) {
                    newMetricsToAdd.putAll(getAggregateFunctionMetrics(processedMetric.getKey(),
                      processedMetric.getValue()));
                  }
                }
              }
              metricByCategory.getValue().putAll(newMetricsToAdd);
            }
          }
        }
      }
    }

    return metricMap;
  }

  private Map<String, Metric> getAggregateFunctionMetrics(String metricName, Metric currentMetric) {
    Map<String, Metric> newMetrics = new HashMap<>();
    if (!PropertyHelper.hasAggregateFunctionSuffix(currentMetric.getName())) {
      // For every function id
      for (String identifierToAdd : AGGREGATE_FUNCTION_IDENTIFIERS) {
        String newMetricKey = metricName + identifierToAdd;
        Metric newMetric = new Metric(
          currentMetric.getName() + identifierToAdd,
          currentMetric.isPointInTime(),
          currentMetric.isTemporal(),
          currentMetric.isAmsHostMetric(),
          currentMetric.getUnit()
        );
        newMetrics.put(newMetricKey, newMetric);
      }
    }

    return newMetrics;
  }

  /**
   * Gets the metrics for a Role (component).
   * @return the list of defined metrics.
   */
  public List<MetricDefinition> getMetrics(String stackName, String stackVersion,
      String serviceName, String componentName, String metricType)
      throws AmbariException {

    Map<String, Map<String, List<MetricDefinition>>> map = getServiceMetrics(
      stackName, stackVersion, serviceName);

    if (map != null && map.containsKey(componentName)) {
      if (map.get(componentName).containsKey(metricType)) {
        return map.get(componentName).get(metricType);
      }
    }

	  return null;
  }

  /**
   * Gets the alert definitions for the specified stack and service.
   *
   * @param stackName
   *          the stack name
   * @param stackVersion
   *          the stack version
   * @param serviceName
   *          the service name
   * @return the alert definitions for a stack or an empty list if none (never
   *         {@code null}).
   * @throws AmbariException
   */
  public Set<AlertDefinition> getAlertDefinitions(String stackName, String stackVersion,
                                                  String serviceName) throws AmbariException {

    ServiceInfo svc = getService(stackName, stackVersion, serviceName);
    return getAlertDefinitions(svc);
  }
  /**
   * Gets the alert definitions for the specified stack and service.
   *
   * @param service
   *          the service name
   * @return the alert definitions for a stack or an empty list if none (never
   *         {@code null}).
   * @throws AmbariException
   */
  public Set<AlertDefinition> getAlertDefinitions(ServiceInfo service)
      throws AmbariException {
    File alertsFile = service.getAlertsFile();

    if (null == alertsFile || !alertsFile.exists()) {
      LOG.debug("Alerts file for {}/{} not found.", service.getSchemaVersion(),
          service.getName());

      return Collections.emptySet();
    }

    return alertDefinitionFactory.getAlertDefinitions(alertsFile,
            service.getName());
  }

  /**
   * Returns new definitions for the merge
   *
   * @param definitions List of the definitions
   * @param clusterId Cluster ID
   * @param mappedEntities Mapped entities
   * @return Entities to merge
   */
  private List<AlertDefinitionEntity> getDefinitionsForMerge(List<AlertDefinition> definitions, long clusterId,
                                                             Map<String, AlertDefinitionEntity> mappedEntities ) {
    List<AlertDefinitionEntity> definitionsForMerge = new ArrayList<>();

    for (AlertDefinition definition: definitions) {
      AlertDefinitionEntity entity = mappedEntities.get(definition.getName());

      // no entity means this is new; create a new entity
      if (null == entity) {
        entity = alertDefinitionFactory.coerce(clusterId, definition);
        definitionsForMerge.add(entity);
      }
    }
    return definitionsForMerge;
  }

  /**
   * Compares the alert definitions defined on the stack with those in the
   * database and merges any new or updated definitions. This method will first
   * determine the services that are installed on each cluster to prevent alert
   * definitions from undeployed services from being shown.
   * <p/>
   * This method will also detect "agent" alert definitions, which are
   * definitions that should be run on agent hosts but are not associated with a
   * service.
   *
   * @param clusters all clusters
   * @param updateScriptPaths whether existing script-based alerts should be updated
   *        with possibly new paths from the stack definition
   */
  public void reconcileAlertDefinitions(Clusters clusters, boolean updateScriptPaths)  throws AmbariException {
    Map<String, Cluster> clusterMap = clusters.getClusters();
    if (null == clusterMap || clusterMap.size() == 0) {
      return;
    }

    // for every cluster
    for (Cluster cluster : clusterMap.values()) {
       reconcileAlertDefinitions(cluster, updateScriptPaths);
    }

  }

  /**
   * Compares the alert definitions defined on the stack with those in the
   * database and merges any new or updated definitions. This method will first
   * determine the services that are installed on each cluster to prevent alert
   * definitions from undeployed services from being shown.
   * <p/>
   * This method will also detect "agent" alert definitions, which are
   * definitions that should be run on agent hosts but are not associated with a
   * service.
   *
   * @param cluster cluster
   * @param updateScriptPaths whether existing script-based alerts should be updated
   *        with possibly new paths from the stack definition
   */
  public void reconcileAlertDefinitions(Cluster cluster, boolean updateScriptPaths) throws AmbariException {
      if (null == cluster) {
        return;
      }

      long clusterId = cluster.getClusterId();
      Map<String, ServiceInfo> stackServiceMap = new HashMap<>();
      Map<String, ComponentInfo> stackComponentMap = new HashMap<>();

      // for every service installed in that cluster, get the service MetaInfo
      // and off of that the alert definitions
      List<AlertDefinition> stackDefinitions = new ArrayList<>(50);

      for (Service service : cluster.getServices().values()) {
        ServiceInfo stackService = getService(service.getDesiredStackId().getStackName(),
            service.getDesiredStackId().getStackVersion(), service.getName());

        if (null == stackService) {
          continue;
        }

        stackServiceMap.put(stackService.getName(), stackService);
        List<ComponentInfo> components = stackService.getComponents();
        for (ComponentInfo component : components) {
          stackComponentMap.put(component.getName(), component);
        }

        // get all alerts defined on the stack for each cluster service
        Set<AlertDefinition> serviceDefinitions = getAlertDefinitions(stackService);
        stackDefinitions.addAll(serviceDefinitions);
      }

      List<AlertDefinitionEntity> persist = new ArrayList<>();
      List<AlertDefinitionEntity> entities = alertDefinitionDao.findAll(clusterId);

      // create a map of the entities for fast extraction
      Map<String, AlertDefinitionEntity> mappedEntities = new HashMap<>(100);
      for (AlertDefinitionEntity entity : entities) {
        mappedEntities.put(entity.getDefinitionName(), entity);
      }

      // for each stack definition, see if it exists and compare if it does
      for( AlertDefinition stackDefinition : stackDefinitions ){
        AlertDefinitionEntity entity = mappedEntities.get(stackDefinition.getName());

        // no entity means this is new; create a new entity
        if (null == entity) {
          entity = alertDefinitionFactory.coerce(clusterId, stackDefinition);
          persist.add(entity);
          continue;
        }

        // definitions from the stack that are altered will not be overwritten;
        // use the REST APIs to modify them instead
        AlertDefinition databaseDefinition = alertDefinitionFactory.coerce(entity);
        if (!stackDefinition.deeplyEquals(databaseDefinition)) {
          if (updateScriptPaths) {
            Source databaseSource = databaseDefinition.getSource();
            Source stackSource = stackDefinition.getSource();
            if (databaseSource.getType() == SourceType.SCRIPT && stackSource.getType() == SourceType.SCRIPT) {
              ScriptSource databaseScript = (ScriptSource) databaseSource;
              ScriptSource stackScript = (ScriptSource) stackSource;
              String oldPath = databaseScript.getPath();
              String newPath = stackScript.getPath();
              if (!Objects.equals(oldPath, newPath)) {
                databaseScript.setPath(newPath);
                entity = alertDefinitionFactory.mergeSource(databaseScript, entity);
                persist.add(entity);

                LOG.info("Updating script path for the alert named {} from '{}' to '{}'",
                  stackDefinition.getName(), oldPath, newPath
                );
              }
            }
          } else {
            LOG.debug("The alert named {} has been modified from the stack definition and will not be merged",
              stackDefinition.getName());
          }
        }
      }

      // ambari agent host-only alert definitions
      persist.addAll(getDefinitionsForMerge(ambariServiceAlertDefinitions.getAgentDefinitions(), clusterId, mappedEntities));

      // ambari server host-only alert definitions
      persist.addAll(getDefinitionsForMerge(ambariServiceAlertDefinitions.getServerDefinitions(), clusterId, mappedEntities));

      // persist any new or updated definition
      for (AlertDefinitionEntity entity : persist) {
        LOG.debug("Merging Alert Definition {} into the database", entity.getDefinitionName());
        alertDefinitionDao.createOrUpdate(entity);
      }

      // all definition resolved; publish their registration
      for (AlertDefinitionEntity def : alertDefinitionDao.findAll(cluster.getClusterId())) {
        AlertDefinition realDef = alertDefinitionFactory.coerce(def);
        AlertDefinitionRegistrationEvent event = new AlertDefinitionRegistrationEvent(cluster.getClusterId(), realDef);

        eventPublisher.publish(event);
      }

      // for every definition, determine if the service and the component are
      // still valid; if they are not, disable them - this covers the case
      // with STORM/REST_API where that component was removed from the
      // stack but still exists in the database - we disable the alert to
      // preserve historical references
      List<AlertDefinitionEntity> definitions = alertDefinitionDao.findAllEnabled(clusterId);
      List<AlertDefinitionEntity> definitionsToDisable = new ArrayList<>();

      for (AlertDefinitionEntity definition : definitions) {
        String serviceName = definition.getServiceName();
        String componentName = definition.getComponentName();

        // the AMBARI service is special, skip it here
        if (RootService.AMBARI.name().equals(serviceName)) {
          continue;
        }

        if (!stackServiceMap.containsKey(serviceName)) {

           LOG.info( "The {} service has been marked as deleted for cluster {}, disabling alert {}",
              serviceName, cluster.getClusterName(), definition.getDefinitionName());

          definitionsToDisable.add(definition);
        } else if (null != componentName && !stackComponentMap.containsKey(componentName)) {

          StackId stackId = cluster.getService(serviceName).getDesiredStackId();
          LOG.info( "The {} component {} has been marked as deleted for stack {}, disabling alert {}",
              serviceName, componentName, stackId, definition.getDefinitionName());

          definitionsToDisable.add(definition);
        }
      }

      // disable definitions and fire the event
      for (AlertDefinitionEntity definition : definitionsToDisable) {
        definition.setEnabled(false);
        alertDefinitionDao.merge(definition);
        eventPublisher.publish(new AlertDefinitionDisabledEvent(clusterId, definition.getDefinitionId(),
            definition.getDefinitionName()));
      }
  }

  /**
   * Get all upgrade packs available for a stack.
   *
   * @param stackName the stack name
   * @param stackVersion the stack version
   * @return a map of upgrade packs, keyed by the name of the upgrade pack
   */
  public Map<String, UpgradePack> getUpgradePacks(String stackName, String stackVersion) {
    try {
      StackInfo stack = getStack(stackName, stackVersion);
      return stack.getUpgradePacks() == null ?
          Collections.emptyMap() : stack.getUpgradePacks();

    } catch (AmbariException e) {
      LOG.debug("Cannot load upgrade packs for non-existent stack {}-{}", stackName, stackVersion, e);
    }

    return Collections.emptyMap();
  }

  /**
   * Get all upgrade config pack if it is available for a stack.
   *
   * @param stackName the stack name
   * @param stackVersion the stack version
   * @return config upgrade pack for stack or null if it is
   * not defined for stack
   */
  public ConfigUpgradePack getConfigUpgradePack(String stackName, String stackVersion) {
    try {
      StackInfo stack = getStack(stackName, stackVersion);
      return stack.getConfigUpgradePack();
    } catch (AmbariException e) {
      LOG.debug("Cannot load config upgrade pack for non-existent stack {}-{}", stackName, stackVersion, e);
      return null;
    }
  }

  /**
   * Gets the fully compiled Kerberos descriptor for the relevant stack and version.
   * <p/>
   * All of the kerberos.json files from the specified stack (and version) are read, parsed and
   * complied into a complete Kerberos descriptor hierarchy.
   *
   * @param stackName               a String declaring the stack name
   * @param stackVersion            a String declaring the stack version
   * @param includePreconfigureData a Boolean value indicating whether to include the pre-configuration
   *                                data (<code>true</code>), or not (<code>false</code>)
   * @return a new complete KerberosDescriptor, or null if no Kerberos descriptor information is available
   * @throws AmbariException if an error occurs reading or parsing the stack's kerberos.json files
   */
  public KerberosDescriptor getKerberosDescriptor(String stackName, String stackVersion, boolean includePreconfigureData) throws AmbariException {
    StackInfo stackInfo = getStack(stackName, stackVersion);

    KerberosDescriptor kerberosDescriptor = readKerberosDescriptorFromFile(getCommonKerberosDescriptorFileLocation());

    if (kerberosDescriptor == null) {
      LOG.warn("Couldn't read common Kerberos descriptor with path {%s}", getCommonKerberosDescriptorFileLocation());
      kerberosDescriptor = new KerberosDescriptor();
    }
    // Read in the stack-level Kerberos descriptor pre-configuration data
    if (includePreconfigureData) {
      KerberosDescriptor preConfigureKerberosDescriptor = readKerberosDescriptorFromFile(stackInfo.getKerberosDescriptorPreConfigurationFileLocation());

      if (preConfigureKerberosDescriptor != null) {
        // Ensure the all services to be pre-configured are flagged appropriately.
        Map<String, KerberosServiceDescriptor> serviceDescriptors = preConfigureKerberosDescriptor.getServices();
        if (serviceDescriptors != null) {
          for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors.values()) {
            serviceDescriptor.setPreconfigure(true);
          }
        }
        kerberosDescriptor.update(preConfigureKerberosDescriptor);
      }
    }

    // Read in the individual service-level Kerberos descriptors
    Map<String, ServiceInfo> services = getServices(stackName, stackVersion);

    if (services != null) {
      for (ServiceInfo service : services.values()) {
        KerberosServiceDescriptor[] serviceDescriptors = getKerberosDescriptor(service);

        if (serviceDescriptors != null) {
          for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors) {
            // Add the service-level Kerberos descriptor to kerberosDescriptor. If the service was
            // previously added (possible from the pre-configure data), update the existing service-level
            // Kerberos descriptor with this one.
            kerberosDescriptor.putService(serviceDescriptor);
          }
        }
      }
    }

    return kerberosDescriptor;
  }

  /**
   * Gets the path to the common Kerberos descriptor file
   *
   * @return a String containing the path to the common Kerberos descriptor file
   */
  protected String getCommonKerberosDescriptorFileLocation() {
    return commonKerberosDescriptorFileLocation;
  }

  /**
   * Gets the requested service-level Kerberos descriptor(s)
   * <p/>
   * An array of descriptors are returned since the kerberos.json in a service directory may contain
   * descriptor details for one or more services.
   *
   * @param serviceInfo a ServiceInfo declaring the stack name, version, a service (directory) name
   *                    details
   * @return an array of KerberosServiceDescriptors, or null if the relevant service (directory)
   * does not contain Kerberos descriptor details
   * @throws AmbariException if an error occurs reading or parsing the service's kerberos.json files
   */
  public KerberosServiceDescriptor[] getKerberosDescriptor(ServiceInfo serviceInfo) throws AmbariException {

    KerberosServiceDescriptor[] kerberosServiceDescriptors = null;
    File kerberosFile = (serviceInfo == null) ? null : serviceInfo.getKerberosDescriptorFile();

    if (kerberosFile != null) {
      try {
        kerberosServiceDescriptors = kerberosServiceDescriptorFactory.createInstances(kerberosFile);
      } catch (Exception e) {
        LOG.error("Could not read the kerberos descriptor file", e);
        throw new AmbariException("Could not read kerberos descriptor file", e);
      }
    }

    return kerberosServiceDescriptors;
  }

  /* Return ambari.properties from configuration API. This is to avoid
  changing interface impls that do not use injection or use partial
  injection like Stack Advisor Commands */
  public Map<String, String> getAmbariServerProperties() {
    return conf.getAmbariProperties();
  }

  /**
   * Ensures that the map of version definition files is populated
   */
  private synchronized void ensureVersionDefinitions() {
    if (null != versionDefinitions) {
      if(versionDefinitions.size() > 0)
        return;
    }

    versionDefinitions = new HashMap<>();

    for (StackInfo stack : getStacks()) {
      if (stack.isActive() && stack.isValid()) {
        for (VersionDefinitionXml definition : stack.getVersionDefinitions()) {
          versionDefinitions.put(String.format("%s-%s-%s", stack.getName(),
            stack.getVersion(), definition.release.version), definition);
        }
        
        try {
          // !!! check for a "latest-vdf" one.  This will be used for the default if one is not found.
          VersionDefinitionXml xml = stack.getLatestVersionDefinition();

          if (null == xml) {
            // !!! "latest-vdf" was not found, use the stack.  this is the last-ditch effort
            xml = VersionDefinitionXml.build(stack);
          }

          versionDefinitions.put(String.format("%s-%s", stack.getName(), stack.getVersion()), xml);
        } catch (Exception e) {
          LOG.warn("Could not make a stack VDF for {}-{}: {}",
              stack.getName(), stack.getVersion(), e.getMessage());
        }
      } else {
        StackId stackId = new StackId(stack);
        if (!stack.isValid()) {
          LOG.info("Stack {} is not valid, skipping VDF: {}", stackId, StringUtils.join(stack.getErrors(), "; "));
        } else if (!stack.isActive()) {
          LOG.info("Stack {} is not active, skipping VDF", stackId);
        }
      }

    }
  }

  /**
   * @param versionDefinitionId the version definition id
   * @return the definition, or {@code null} if not found
   */
  public VersionDefinitionXml getVersionDefinition(String versionDefinitionId) {
    ensureVersionDefinitions();

    return versionDefinitions.get(versionDefinitionId);
  }

  /**
   * @return the version definitions found for stacks
   */
  public Map<String, VersionDefinitionXml> getVersionDefinitions() {
    ensureVersionDefinitions();

    return versionDefinitions;
  }


  /**
   * Reads a Kerberos descriptor from the specified file path.
   *
   * @param fileLocation the path to the file
   * @return a KerberosDescriptor or <code>null</code>, if no path is specified
   * @throws AmbariException if an error occurs reading or parsing the Kerberos descriptor file
   */
  KerberosDescriptor readKerberosDescriptorFromFile(String fileLocation) throws AmbariException {
    if (!StringUtils.isEmpty(fileLocation)) {
      File file = new File(fileLocation);

      if (file.canRead()) {
        try {
          return kerberosDescriptorFactory.createInstance(file);
        } catch (IOException e) {
          throw new AmbariException(String.format("Failed to parse Kerberos descriptor file %s",
              file.getAbsolutePath()), e);
        }
      } else {
        throw new AmbariException(String.format("Unable to read Kerberos descriptor file %s",
            file.getAbsolutePath()));
      }
    } else {
      LOG.debug("Missing path to Kerberos descriptor, returning null");
    }

    return null;
  }

  public File getCommonWidgetsDescriptorFile() {
    return commonWidgetsDescriptorFile;
  }

  /***
   * Remove Mpack from the mpackMap and stackMap which is used to power the Mpack and Stack APIs.
   * Stack should be removed from stackMap only if it points to the mpack that is being removed.
   * @param mpackEntity
   * @param stackEntity
   * @throws IOException
   */
  public void removeMpack(MpackEntity mpackEntity, StackEntity stackEntity) throws IOException {
    if (versionDefinitions != null) {
      versionDefinitions.clear();
    }
    boolean stackDelete = mpackManager.removeMpack(mpackEntity, stackEntity);

    if (stackDelete) {
      stackManager.removeStack(stackEntity);
    }
  }

    /*
   * Fetch all mpacks from mpackMap
   * @return all mpacks from mpackMap - in memory data structure
   */
  public Collection<Mpack> getMpacks() {
    if (mpackManager.getMpackMap() != null) {
      return mpackManager.getMpackMap().values();
    }
    return Collections.emptySet();
  }

  /***
   * Fetch a particular mpack based on mpackid
   * @return a single mpack
   */
  public Mpack getMpack(Long mpackId) {
    if (mpackManager.getMpackMap() != null && mpackManager.getMpackMap().containsKey(mpackId)) {
      return mpackManager.getMpackMap().get(mpackId);
    }
    return null;
  }
}
