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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.collections.Predicate;
import org.apache.ambari.server.collections.PredicateUtils;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.internal.AmbariServerLDAPConfigurationHandler;
import org.apache.ambari.server.controller.internal.AmbariServerSSOConfigurationHandler;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceCredentialStoreUpdateEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ServiceImpl implements Service {
  private final Lock lock = new ReentrantLock();
  private ServiceDesiredStateEntityPK serviceDesiredStateEntityPK;
  private ClusterServiceEntityPK serviceEntityPK;

  private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

  private final Cluster cluster;
  private final ConcurrentMap<String, ServiceComponent> components = new ConcurrentHashMap<>();
  private boolean isClientOnlyService;
  private boolean isCredentialStoreSupported;
  private boolean isCredentialStoreRequired;
  private final boolean ssoIntegrationSupported;
  private final Predicate ssoEnabledTest;
  private final boolean ldapIntegrationSupported;
  private final Predicate ldapEnabledTest;
  private final boolean ssoRequiresKerberos;
  private final Predicate kerberosEnabledTest;
  private AmbariMetaInfo ambariMetaInfo;
  private AtomicReference<MaintenanceState> maintenanceState = new AtomicReference<>();

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  @Inject
  private AmbariManagementController ambariManagementController;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private AmbariServerSSOConfigurationHandler ambariServerSSOConfigurationHandler;

  @Inject
  private AmbariServerLDAPConfigurationHandler ambariServerLDAPConfigurationHandler;

  private final ClusterServiceDAO clusterServiceDAO;
  private final ServiceDesiredStateDAO serviceDesiredStateDAO;
  private final ClusterDAO clusterDAO;
  private final ServiceComponentFactory serviceComponentFactory;

  /**
   * Used to publish events relating to service CRUD operations.
   */
  private final AmbariEventPublisher eventPublisher;

  /**
   * The name of the service.
   */
  private final String serviceName;
  private final String displayName;

  @AssistedInject
  ServiceImpl(@Assisted Cluster cluster, @Assisted String serviceName,
              @Assisted RepositoryVersionEntity desiredRepositoryVersion, ClusterDAO clusterDAO,
              ClusterServiceDAO clusterServiceDAO, ServiceDesiredStateDAO serviceDesiredStateDAO,
              ServiceComponentFactory serviceComponentFactory, AmbariMetaInfo ambariMetaInfo,
              AmbariEventPublisher eventPublisher) throws AmbariException {
    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceDesiredStateDAO = serviceDesiredStateDAO;
    this.serviceComponentFactory = serviceComponentFactory;
    this.eventPublisher = eventPublisher;
    this.serviceName = serviceName;
    this.ambariMetaInfo = ambariMetaInfo;

    ClusterServiceEntity serviceEntity = new ClusterServiceEntity();
    serviceEntity.setClusterId(cluster.getClusterId());
    serviceEntity.setServiceName(serviceName);
    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setServiceName(serviceName);
    serviceDesiredStateEntity.setClusterId(cluster.getClusterId());
    serviceDesiredStateEntity.setDesiredRepositoryVersion(desiredRepositoryVersion);
    serviceDesiredStateEntityPK = getServiceDesiredStateEntityPK(serviceDesiredStateEntity);
    serviceEntityPK = getServiceEntityPK(serviceEntity);

    serviceDesiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);

    StackId stackId = desiredRepositoryVersion.getStackId();

    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);

    displayName = sInfo.getDisplayName();
    isClientOnlyService = sInfo.isClientOnlyService();
    isCredentialStoreSupported = sInfo.isCredentialStoreSupported();
    isCredentialStoreRequired = sInfo.isCredentialStoreRequired();
    ssoIntegrationSupported = sInfo.isSingleSignOnSupported();
    ssoEnabledTest = compileSsoEnabledPredicate(sInfo);
    ssoRequiresKerberos = sInfo.isKerberosRequiredForSingleSignOnIntegration();
    kerberosEnabledTest = compileKerberosEnabledPredicate(sInfo);

    if (ssoIntegrationSupported && ssoRequiresKerberos && (kerberosEnabledTest == null)) {
      LOG.warn("The service, {}, requires Kerberos to be enabled for SSO integration support; " +
              "however, the kerberosEnabledTest specification has not been specified in the metainfo.xml file. " +
              "Automated SSO integration will not be allowed for this service.",
          serviceName);
    }

    ldapIntegrationSupported = sInfo.isLdapSupported();
    ldapEnabledTest = StringUtils.isNotBlank(sInfo.getLdapEnabledTest()) ? PredicateUtils.fromJSON(sInfo.getLdapEnabledTest()) : null;

    persist(serviceEntity);
  }

  @AssistedInject
  ServiceImpl(@Assisted Cluster cluster, @Assisted ClusterServiceEntity serviceEntity,
              ClusterDAO clusterDAO, ClusterServiceDAO clusterServiceDAO,
              ServiceDesiredStateDAO serviceDesiredStateDAO,
              ServiceComponentFactory serviceComponentFactory, AmbariMetaInfo ambariMetaInfo,
              AmbariEventPublisher eventPublisher) throws AmbariException {
    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceDesiredStateDAO = serviceDesiredStateDAO;
    this.serviceComponentFactory = serviceComponentFactory;
    this.eventPublisher = eventPublisher;
    serviceName = serviceEntity.getServiceName();
    this.ambariMetaInfo = ambariMetaInfo;

    ServiceDesiredStateEntity serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
    serviceDesiredStateEntityPK = getServiceDesiredStateEntityPK(serviceDesiredStateEntity);
    serviceEntityPK = getServiceEntityPK(serviceEntity);

    if (!serviceEntity.getServiceComponentDesiredStateEntities().isEmpty()) {
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity
          : serviceEntity.getServiceComponentDesiredStateEntities()) {
        try {
          components.put(serviceComponentDesiredStateEntity.getComponentName(),
              serviceComponentFactory.createExisting(this,
                  serviceComponentDesiredStateEntity));
        } catch (ProvisionException ex) {
          StackId stackId = new StackId(serviceComponentDesiredStateEntity.getDesiredStack());
          LOG.error(String.format("Can not get component info: stackName=%s, stackVersion=%s, serviceName=%s, componentName=%s",
              stackId.getStackName(), stackId.getStackVersion(),
              serviceEntity.getServiceName(), serviceComponentDesiredStateEntity.getComponentName()));
          ex.printStackTrace();
        }
      }
    }

    StackId stackId = getDesiredStackId();
    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), getName());
    isClientOnlyService = sInfo.isClientOnlyService();
    isCredentialStoreSupported = sInfo.isCredentialStoreSupported();
    isCredentialStoreRequired = sInfo.isCredentialStoreRequired();
    displayName = sInfo.getDisplayName();
    ssoIntegrationSupported = sInfo.isSingleSignOnSupported();
    ssoEnabledTest = compileSsoEnabledPredicate(sInfo);
    ssoRequiresKerberos = sInfo.isKerberosRequiredForSingleSignOnIntegration();
    kerberosEnabledTest = compileKerberosEnabledPredicate(sInfo);

    if (ssoIntegrationSupported && ssoRequiresKerberos && (kerberosEnabledTest == null)) {
      LOG.warn("The service, {}, requires Kerberos to be enabled for SSO integration support; " +
              "however, the kerberosEnabledTest specification has not been specified in the metainfo.xml file. " +
              "Automated SSO integration will not be allowed for this service.",
          serviceName);
    }

    ldapIntegrationSupported = sInfo.isLdapSupported();
    ldapEnabledTest = StringUtils.isNotBlank(sInfo.getLdapEnabledTest()) ? PredicateUtils.fromJSON(sInfo.getLdapEnabledTest()) : null;
  }


  /***
   * Refresh Service info due to current stack
   * @throws AmbariException
   */
  @Override
  public void updateServiceInfo() throws AmbariException {
    try {
      ServiceInfo serviceInfo = ambariMetaInfo.getService(this);

      isClientOnlyService = serviceInfo.isClientOnlyService();
      isCredentialStoreSupported = serviceInfo.isCredentialStoreSupported();
      isCredentialStoreRequired = serviceInfo.isCredentialStoreRequired();

    } catch (ObjectNotFoundException e) {
      throw new RuntimeException("Trying to create a ServiceInfo"
          + " not recognized in stack info"
          + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName()
          + ", stackInfo=" + getDesiredStackId().getStackName());
    }
  }

  @Override
  public String getName() {
    return serviceName;
  }

  @Override
  public String getDisplayName() {
    return StringUtils.isBlank(displayName) ? serviceName : displayName;
  }

  @Override
  public long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public Map<String, ServiceComponent> getServiceComponents() {
    return new HashMap<>(components);
  }

  @Override
  public Set<String> getServiceHosts() {
    Set<String> hostNames = new HashSet<>();
    for (ServiceComponent serviceComponent : getServiceComponents().values()) {
      hostNames.addAll(serviceComponent.getServiceComponentsHosts());
    }
    return hostNames;
  }

  @Override
  public void addServiceComponents(
      Map<String, ServiceComponent> components) throws AmbariException {
    for (ServiceComponent sc : components.values()) {
      addServiceComponent(sc);
    }
  }

  @Override
  public void addServiceComponent(ServiceComponent component) throws AmbariException {
    if (components.containsKey(component.getName())) {
      throw new AmbariException("Cannot add duplicate ServiceComponent"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + component.getName());
    }

    components.put(component.getName(), component);
  }

  @Override
  public ServiceComponent addServiceComponent(String serviceComponentName)
      throws AmbariException {
    ServiceComponent component = serviceComponentFactory.createNew(this, serviceComponentName);
    addServiceComponent(component);
    return component;
  }

  @Override
  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException {
    ServiceComponent serviceComponent = components.get(componentName);
    if (null == serviceComponent) {
      throw new ServiceComponentNotFoundException(cluster.getClusterName(),
          getName(), componentName);
    }

    return serviceComponent;
  }

  @Override
  public State getDesiredState() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    return serviceDesiredStateEntity.getDesiredState();
  }

  @Override
  public void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service, clusterName={}, clusterId={}, serviceName={}, oldDesiredState={}, newDesiredState={}",
          cluster.getClusterName(), cluster.getClusterId(), getName(), getDesiredState(), state);
    }

    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredState(state);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StackId getDesiredStackId() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();

    if (null == serviceDesiredStateEntity) {
      return null;
    } else {
      StackEntity desiredStackEntity = serviceDesiredStateEntity.getDesiredStack();
      return new StackId(desiredStackEntity);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RepositoryVersionEntity getDesiredRepositoryVersion() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    return serviceDesiredStateEntity.getDesiredRepositoryVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void setDesiredRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity) {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredRepositoryVersion(repositoryVersionEntity);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);

    Collection<ServiceComponent> components = getServiceComponents().values();
    for (ServiceComponent component : components) {
      component.setDesiredRepositoryVersion(repositoryVersionEntity);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RepositoryVersionState getRepositoryState() {
    if (components.isEmpty()) {
      return RepositoryVersionState.NOT_REQUIRED;
    }

    List<RepositoryVersionState> states = new ArrayList<>();
    for (ServiceComponent component : components.values()) {
      states.add(component.getRepositoryState());
    }

    return RepositoryVersionState.getAggregateState(states);
  }

  @Override
  public ServiceResponse convertToResponse() {
    RepositoryVersionEntity desiredRespositoryVersion = getDesiredRepositoryVersion();
    StackId desiredStackId = desiredRespositoryVersion.getStackId();

    Map<String, Map<String, String>> existingConfigurations;

    try {
      existingConfigurations = configHelper.calculateExistingConfigurations(ambariManagementController, cluster);
    } catch (AmbariException e) {
      LOG.warn("Failed to get the existing configurations for the cluster.  Predicate calculations may not be correct due to missing data.");
      existingConfigurations = Collections.emptyMap();
    }

    ServiceResponse r = new ServiceResponse(cluster.getClusterId(), cluster.getClusterName(),
        getName(), desiredStackId, desiredRespositoryVersion.getVersion(), getRepositoryState(),
        getDesiredState().toString(), isCredentialStoreSupported(), isCredentialStoreEnabled(),
        ssoIntegrationSupported, isSsoIntegrationDesired(), isSsoIntegrationEnabled(existingConfigurations),
        isKerberosRequiredForSsoIntegration(), isKerberosEnabled(existingConfigurations), ldapIntegrationSupported,isLdapIntegrationEnabeled(existingConfigurations), isLdapIntegrationDesired());

    r.setDesiredRepositoryVersionId(desiredRespositoryVersion.getId());

    r.setMaintenanceState(getMaintenanceState().name());
    return r;
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  /**
   * Get a true or false value specifying whether
   * credential store is supported by this service.
   *
   * @return true or false
   */
  @Override
  public boolean isCredentialStoreSupported() {
    return isCredentialStoreSupported;
  }

  /**
   * Get a true or false value specifying whether
   * credential store is required by this service.
   *
   * @return true or false
   */
  @Override
  public boolean isCredentialStoreRequired() {
    return isCredentialStoreRequired;
  }


  /**
   * Get a true or false value specifying whether
   * credential store use is enabled for this service.
   *
   * @return true or false
   */
  @Override
  public boolean isCredentialStoreEnabled() {
    ServiceDesiredStateEntity desiredStateEntity = getServiceDesiredStateEntity();

    if (desiredStateEntity != null) {
      return desiredStateEntity.isCredentialStoreEnabled();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getName());
    }
    return false;
  }


  /**
   * Set a true or false value specifying whether this
   * service is to be enabled for credential store use.
   *
   * @param credentialStoreEnabled - true or false
   */
  @Override
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting CredentialStoreEnabled of Service, clusterName={}, clusterId={}, serviceName={}, oldCredentialStoreEnabled={}, newCredentialStoreEnabled={}",
          cluster.getClusterName(), cluster.getClusterId(), getName(), isCredentialStoreEnabled(), credentialStoreEnabled);
    }

    ServiceDesiredStateEntity desiredStateEntity = getServiceDesiredStateEntity();

    if (desiredStateEntity != null) {
      ServiceCredentialStoreUpdateEvent serviceCredentialStoreUpdateEvent = null;
      //create event only if the value changed
      if (desiredStateEntity.isCredentialStoreEnabled() != credentialStoreEnabled) {
        StackId stackId = getDesiredStackId();
        serviceCredentialStoreUpdateEvent =
            new ServiceCredentialStoreUpdateEvent(getClusterId(), stackId.getStackName(),
                stackId.getStackVersion(), getName());
      }
      desiredStateEntity.setCredentialStoreEnabled(credentialStoreEnabled);
      desiredStateEntity = serviceDesiredStateDAO.merge(desiredStateEntity);

      //publish event after the value has changed
      if (serviceCredentialStoreUpdateEvent != null) {
        eventPublisher.publish(serviceCredentialStoreUpdateEvent);
      }
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getName());
    }
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("Service={ serviceName=").append(getName())
        .append(", clusterName=").append(cluster.getClusterName())
        .append(", clusterId=").append(cluster.getClusterId())
        .append(", desiredStackVersion=").append(getDesiredStackId())
        .append(", desiredState=").append(getDesiredState())
        .append(", components=[ ");
    boolean first = true;
    for (ServiceComponent sc : components.values()) {
      if (!first) {
        sb.append(" , ");
      }
      first = false;
      sb.append("\n      ");
      sc.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ] }");
  }

  /**
   *
   */
  private void persist(ClusterServiceEntity serviceEntity) {
    persistEntities(serviceEntity);

    // publish the service installed event
    StackId stackId = getDesiredStackId();
    cluster.addService(this);

    ServiceInstalledEvent event = new ServiceInstalledEvent(getClusterId(), stackId.getStackName(),
        stackId.getStackVersion(), getName());

    eventPublisher.publish(event);
  }

  @Transactional
  void persistEntities(ClusterServiceEntity serviceEntity) {
    long clusterId = cluster.getClusterId();
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    serviceEntity.setClusterEntity(clusterEntity);
    clusterServiceDAO.create(serviceEntity);
    clusterEntity.getClusterServiceEntities().add(serviceEntity);
    clusterDAO.merge(clusterEntity);
    clusterServiceDAO.merge(serviceEntity);
  }


  @Override
  public boolean canBeRemoved() {
    //
    // A service can be deleted if all it's components
    // can be removed, irrespective of the state of
    // the service itself.
    //
    for (ServiceComponent sc : components.values()) {
      if (!sc.canBeRemoved()) {
        LOG.warn("Found non removable component when trying to delete service" + ", clusterName="
            + cluster.getClusterName() + ", serviceName=" + getName() + ", componentName="
            + sc.getName());
        return false;
      }
    }
    return true;
  }

  @Transactional
  void deleteAllServiceConfigs() throws AmbariException {
    long clusterId = getClusterId();
    ServiceConfigEntity lastServiceConfigEntity = serviceConfigDAO.findMaxVersion(clusterId, getName());
    // de-select every configuration from the service
    if (lastServiceConfigEntity != null) {
      for (ClusterConfigEntity serviceConfigEntity : lastServiceConfigEntity.getClusterConfigEntities()) {
        LOG.info("Disabling and unmapping configuration {}", serviceConfigEntity);
        serviceConfigEntity.setSelected(false);
        serviceConfigEntity.setUnmapped(true);
        clusterDAO.merge(serviceConfigEntity);
      }
    }

    LOG.info("Deleting all configuration associations for {} on cluster {}", getName(), cluster.getClusterName());

    List<ServiceConfigEntity> serviceConfigEntities =
        serviceConfigDAO.findByService(cluster.getClusterId(), getName());

    for (ServiceConfigEntity serviceConfigEntity : serviceConfigEntities) {
      // unmapping all service configs
      for (ClusterConfigEntity clusterConfigEntity : serviceConfigEntity.getClusterConfigEntities()) {
        if (!clusterConfigEntity.isUnmapped()) {
          LOG.info("Unmapping configuration {}", clusterConfigEntity);
          clusterConfigEntity.setUnmapped(true);
          clusterDAO.merge(clusterConfigEntity);
        }
      }
      // Only delete the historical version information and not original
      // config data
      serviceConfigDAO.remove(serviceConfigEntity);
    }
  }

  void deleteAllServiceConfigGroups() throws AmbariException {
    for (Long configGroupId : cluster.getConfigGroupsByServiceName(serviceName).keySet()) {
      cluster.deleteConfigGroup(configGroupId);
    }
  }

  @Override
  @Transactional
  public void deleteAllComponents(DeleteHostComponentStatusMetaData deleteMetaData) {
    lock.lock();
    try {
      LOG.info("Deleting all components for service" + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName());
      // FIXME check dependencies from meta layer
      for (ServiceComponent component : components.values()) {
        if (!component.canBeRemoved()) {
          deleteMetaData.setAmbariException(new AmbariException("Found non removable component when trying to"
              + " delete all components from service" + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName() + ", componentName=" + component.getName()));
          return;
        }
      }

      for (ServiceComponent serviceComponent : components.values()) {
        serviceComponent.delete(deleteMetaData);
        if (deleteMetaData.getAmbariException() != null) {
          return;
        }
      }

      components.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void deleteServiceComponent(String componentName, DeleteHostComponentStatusMetaData deleteMetaData)
      throws AmbariException {
    lock.lock();
    try {
      ServiceComponent component = getServiceComponent(componentName);
      LOG.info("Deleting servicecomponent for cluster" + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName() + ", componentName=" + componentName);
      // FIXME check dependencies from meta layer
      if (!component.canBeRemoved()) {
        throw new AmbariException("Could not delete component from cluster"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName()
            + ", componentName=" + componentName);
      }

      component.delete(deleteMetaData);
      components.remove(componentName);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isClientOnlyService() {
    return isClientOnlyService;
  }

  @Override
  @Transactional
  public void delete(DeleteHostComponentStatusMetaData deleteMetaData) {
    List<Component> components = getComponents(); // XXX temporal coupling, need to call this BEFORE deletingAllComponents
    deleteAllComponents(deleteMetaData);
    if (deleteMetaData.getAmbariException() != null) {
      return;
    }

    StackId stackId = getDesiredStackId();
    try {
      deleteAllServiceConfigs();
      deleteAllServiceConfigGroups();

      removeEntities();
    } catch (AmbariException e) {
      deleteMetaData.setAmbariException(e);
      return;
    }

    // publish the service removed event
    if (null == stackId) {
      return;
    }

    ServiceRemovedEvent event = new ServiceRemovedEvent(getClusterId(), stackId.getStackName(),
        stackId.getStackVersion(), getName(), components);

    eventPublisher.publish(event);
  }

  private List<Component> getComponents() {
    List<Component> result = new ArrayList<>();
    for (ServiceComponent component : getServiceComponents().values()) {
      for (ServiceComponentHost host : component.getServiceComponentHosts().values()) {
        result.add(new Component(host.getHostName(), getName(), component.getName(), host.getHost().getHostId()));
      }
    }
    return result;
  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    serviceDesiredStateDAO.removeByPK(serviceDesiredStateEntityPK);
    clusterServiceDAO.removeByPK(serviceEntityPK);
  }

  @Override
  public void setMaintenanceState(MaintenanceState state) {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setMaintenanceState(state);
    maintenanceState.set(serviceDesiredStateDAO.merge(serviceDesiredStateEntity).getMaintenanceState());

    // broadcast the maintenance mode change
    MaintenanceModeEvent event = new MaintenanceModeEvent(state, this);
    eventPublisher.publish(event);
  }

  @Override
  public MaintenanceState getMaintenanceState() {
    if (maintenanceState.get() == null) {
      maintenanceState.set(getServiceDesiredStateEntity().getMaintenanceState());
    }
    return maintenanceState.get();
  }

  @Override
  public boolean isKerberosEnabled() {
    if (kerberosEnabledTest != null) {
      Map<String, Map<String, String>> existingConfigurations;

      try {
        existingConfigurations = configHelper.calculateExistingConfigurations(ambariManagementController, cluster);
      } catch (AmbariException e) {
        LOG.warn("Failed to get the existing configurations for the cluster.  Predicate calculations may not be correct due to missing data.");
        existingConfigurations = Collections.emptyMap();
      }
      return isKerberosEnabled(existingConfigurations);
    }

    return false;
  }

  @Override
  public boolean isKerberosEnabled(Map<String, Map<String, String>> configurations) {
    return kerberosEnabledTest != null && kerberosEnabledTest.evaluate(configurations);
  }


  private ClusterServiceEntityPK getServiceEntityPK(ClusterServiceEntity serviceEntity) {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(serviceEntity.getClusterId());
    pk.setServiceName(serviceEntity.getServiceName());
    return pk;
  }

  private ServiceDesiredStateEntityPK getServiceDesiredStateEntityPK(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    ServiceDesiredStateEntityPK pk = new ServiceDesiredStateEntityPK();
    pk.setClusterId(serviceDesiredStateEntity.getClusterId());
    pk.setServiceName(serviceDesiredStateEntity.getServiceName());
    return pk;
  }

  // Refresh the cached reference on setters
  private ServiceDesiredStateEntity getServiceDesiredStateEntity() {
    return serviceDesiredStateDAO.findByPK(serviceDesiredStateEntityPK);
  }

  private Predicate compileSsoEnabledPredicate(ServiceInfo sInfo) {
    if (StringUtils.isNotBlank(sInfo.getSingleSignOnEnabledTest())) {
      if (StringUtils.isNotBlank(sInfo.getSingleSignOnEnabledConfiguration())) {
        LOG.warn("Both <ssoEnabledTest> and <enabledConfiguration> have been declared within <sso> for {}; using <ssoEnabledTest>", serviceName);
      }
      return PredicateUtils.fromJSON(sInfo.getSingleSignOnEnabledTest());
    } else if (StringUtils.isNotBlank(sInfo.getSingleSignOnEnabledConfiguration())) {
      LOG.warn("Only <enabledConfiguration> have been declared  within <sso> for {}; converting its value to an equals predicate", serviceName);
      final String equalsPredicateJson = "{\"equals\": [\"" + sInfo.getSingleSignOnEnabledConfiguration() + "\", \"true\"]}";
      return PredicateUtils.fromJSON(equalsPredicateJson);
    }
    return null;
  }

  private Predicate compileKerberosEnabledPredicate(ServiceInfo sInfo) {
    if (StringUtils.isNotBlank(sInfo.getKerberosEnabledTest())) {
      return PredicateUtils.fromJSON(sInfo.getKerberosEnabledTest());
    }
    return null;
  }

  private boolean isSsoIntegrationDesired() {
    return ambariServerSSOConfigurationHandler.getSSOEnabledServices().contains(serviceName);
  }

  private boolean isSsoIntegrationEnabled(Map<String, Map<String, String>> existingConfigurations) {
    return ssoIntegrationSupported && ssoEnabledTest != null && ssoEnabledTest.evaluate(existingConfigurations);
  }

  private boolean isKerberosRequiredForSsoIntegration() {
    return ssoRequiresKerberos;
  }

  private boolean isLdapIntegrationEnabeled(Map<String, Map<String, String>> existingConfigurations) {
    return ldapIntegrationSupported && ldapEnabledTest != null && ldapEnabledTest.evaluate(existingConfigurations);
  }

  private boolean isLdapIntegrationDesired() {
    return ambariServerLDAPConfigurationHandler.getLDAPEnabledServices().contains(serviceName);
  }
}
