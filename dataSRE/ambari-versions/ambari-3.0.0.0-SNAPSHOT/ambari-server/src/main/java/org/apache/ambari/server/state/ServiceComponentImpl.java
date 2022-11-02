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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.ServiceComponentRecoveryChangedEvent;
import org.apache.ambari.server.events.listeners.upgrade.StackVersionListener;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceComponentImpl implements ServiceComponent {

  private final static Logger LOG =
      LoggerFactory.getLogger(ServiceComponentImpl.class);
  private final Service service;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final String componentName;
  private String displayName;
  private boolean isClientComponent;
  private boolean isMasterComponent;
  private boolean isVersionAdvertised;

  private final ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;

  private final ClusterServiceDAO clusterServiceDAO;

  private final ServiceComponentHostFactory serviceComponentHostFactory;

  private final AmbariEventPublisher eventPublisher;

  private AmbariMetaInfo ambariMetaInfo;

  private final ConcurrentMap<String, ServiceComponentHost> hostComponents = new ConcurrentHashMap<>();

  /**
   * The ID of the persisted {@link ServiceComponentDesiredStateEntity}.
   */
  private final long desiredStateEntityId;

  @Inject
  private RepositoryVersionDAO repoVersionDAO;

  @Inject
  private HostComponentStateDAO hostComponentDAO;

  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service, @Assisted String componentName,
      AmbariMetaInfo ambariMetaInfo,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      ClusterServiceDAO clusterServiceDAO, ServiceComponentHostFactory serviceComponentHostFactory,
      AmbariEventPublisher eventPublisher)
      throws AmbariException {

    this.ambariMetaInfo = ambariMetaInfo;
    this.service = service;
    this.componentName = componentName;
    this.serviceComponentDesiredStateDAO = serviceComponentDesiredStateDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceComponentHostFactory = serviceComponentHostFactory;
    this.eventPublisher = eventPublisher;

    ServiceComponentDesiredStateEntity desiredStateEntity = new ServiceComponentDesiredStateEntity();
    desiredStateEntity.setComponentName(componentName);
    desiredStateEntity.setDesiredState(State.INIT);
    desiredStateEntity.setServiceName(service.getName());
    desiredStateEntity.setClusterId(service.getClusterId());
    desiredStateEntity.setRecoveryEnabled(false);
    desiredStateEntity.setDesiredRepositoryVersion(service.getDesiredRepositoryVersion());

    updateComponentInfo();

    persistEntities(desiredStateEntity);
    desiredStateEntityId = desiredStateEntity.getId();
  }

  @Override
  public void updateComponentInfo() throws AmbariException {
    StackId stackId = service.getDesiredStackId();
    try {
      ComponentInfo compInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
          stackId.getStackVersion(), service.getName(), componentName);
      isClientComponent = compInfo.isClient();
      isMasterComponent = compInfo.isMaster();
      isVersionAdvertised = compInfo.isVersionAdvertised();
      displayName = compInfo.getDisplayName();
    } catch (ObjectNotFoundException e) {
      throw new RuntimeException("Trying to create a ServiceComponent"
          + " not recognized in stack info"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", serviceName=" + service.getName()
          + ", componentName=" + componentName
          + ", stackInfo=" + stackId.getStackId());
    }
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
      @Assisted ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity,
      AmbariMetaInfo ambariMetaInfo,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      ClusterServiceDAO clusterServiceDAO,
      HostComponentDesiredStateDAO hostComponentDesiredStateDAO,
      ServiceComponentHostFactory serviceComponentHostFactory,
      AmbariEventPublisher eventPublisher)
      throws AmbariException {
    this.service = service;
    this.serviceComponentDesiredStateDAO = serviceComponentDesiredStateDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceComponentHostFactory = serviceComponentHostFactory;
    this.eventPublisher = eventPublisher;
    this.ambariMetaInfo = ambariMetaInfo;

    desiredStateEntityId = serviceComponentDesiredStateEntity.getId();
    componentName = serviceComponentDesiredStateEntity.getComponentName();

    updateComponentInfo();

    List<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities = hostComponentDesiredStateDAO.findByIndex(
        service.getClusterId(),
        service.getName(),
        serviceComponentDesiredStateEntity.getComponentName()
    );

    Map<String, HostComponentDesiredStateEntity> mappedHostComponentDesiredStateEntitites =
        hostComponentDesiredStateEntities.stream().collect(Collectors.toMap(h -> h.getHostEntity().getHostName(),
            java.util.function.Function.identity()));
    for (HostComponentStateEntity hostComponentStateEntity : serviceComponentDesiredStateEntity.getHostComponentStateEntities()) {

      try {
        hostComponents.put(hostComponentStateEntity.getHostName(),
          serviceComponentHostFactory.createExisting(this,
            hostComponentStateEntity, mappedHostComponentDesiredStateEntitites.get(hostComponentStateEntity.getHostName())));
      } catch(ProvisionException ex) {
        StackId currentStackId = getDesiredStackId();
        LOG.error(String.format("Can not get host component info: stackName=%s, stackVersion=%s, serviceName=%s, componentName=%s, hostname=%s",
          currentStackId.getStackName(), currentStackId.getStackVersion(),
          service.getName(),serviceComponentDesiredStateEntity.getComponentName(), hostComponentStateEntity.getHostName()));
        ex.printStackTrace();
      }
    }
  }

  @Override
  public String getName() {
    return componentName;
  }

  /**
   * Get the recoveryEnabled value.
   *
   * @return true or false
   */
  @Override
  public boolean isRecoveryEnabled() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      return desiredStateEntity.isRecoveryEnabled();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may " +
              "have been previously deleted, serviceName = " + service.getName() + ", " +
              "componentName = " + componentName);
    }
    return false;
  }

  /**
   * Set the recoveryEnabled field in the entity object.
   *
   * @param recoveryEnabled - true or false
   */
  @Override
  public void setRecoveryEnabled(boolean recoveryEnabled) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting RecoveryEnabled of Component, clusterName={}, clusterId={}, serviceName={}, componentName={}, oldRecoveryEnabled={}, newRecoveryEnabled={}",
        service.getCluster().getClusterName(), service.getCluster().getClusterId(), service.getName(), getName(), isRecoveryEnabled(), recoveryEnabled);
    }

    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      desiredStateEntity.setRecoveryEnabled(recoveryEnabled);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);

      // broadcast the change
      ServiceComponentRecoveryChangedEvent event = new ServiceComponentRecoveryChangedEvent(
              getClusterId(), getClusterName(), getServiceName(), getName(), isRecoveryEnabled());
      eventPublisher.publish(event);

    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + service.getName());
    }
  }

  @Override
  public String getServiceName() {
    return service.getName();
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public long getClusterId() {
    return service.getClusterId();
  }

  @Override
  public Map<String, ServiceComponentHost> getServiceComponentHosts() {
    return new HashMap<>(hostComponents);
  }

  @Override
  public Set<String> getServiceComponentsHosts() {
    Set<String> serviceComponentsHosts = new HashSet<>();
    for (ServiceComponentHost serviceComponentHost : getServiceComponentHosts().values()) {
      serviceComponentsHosts.add(serviceComponentHost.getHostName());
    }
    return serviceComponentsHosts;
  }

  @Override
  public void addServiceComponentHosts(
      Map<String, ServiceComponentHost> hostComponents) throws AmbariException {
    // TODO validation
    for (Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      if (!entry.getKey().equals(entry.getValue().getHostName())) {
        throw new AmbariException(
            "Invalid arguments in map" + ", hostname does not match the key in map");
      }
    }

    for (ServiceComponentHost sch : hostComponents.values()) {
      addServiceComponentHost(sch);
    }
  }

  @Override
  public void addServiceComponentHost(
      ServiceComponentHost hostComponent) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      // TODO validation
      // TODO ensure host belongs to cluster
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a ServiceComponentHost to ServiceComponent, clusterName={}, clusterId={}, serviceName={}, serviceComponentName={}, hostname={}, recoveryEnabled={}",
          service.getCluster().getClusterName(), service.getCluster().getClusterId(), service.getName(), getName(), hostComponent.getHostName(), isRecoveryEnabled());
      }

      if (hostComponents.containsKey(hostComponent.getHostName())) {
        throw new AmbariException("Cannot add duplicate ServiceComponentHost" + ", clusterName="
            + service.getCluster().getClusterName() + ", clusterId="
            + service.getCluster().getClusterId() + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName() + ", hostname=" + hostComponent.getHostName()
            + ", recoveryEnabled=" + isRecoveryEnabled());
      }
      // FIXME need a better approach of caching components by host
      ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
      clusterImpl.addServiceComponentHost(hostComponent);
      hostComponents.put(hostComponent.getHostName(), hostComponent);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public ServiceComponentHost addServiceComponentHost(String hostName) throws AmbariException {
    ServiceComponentHost hostComponent = serviceComponentHostFactory.createNew(this, hostName);
    addServiceComponentHost(hostComponent);
    return hostComponent;
  }

  @Override
  public ServiceComponentHost getServiceComponentHost(String hostname)
      throws AmbariException {

    if (!hostComponents.containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(),
          getServiceName(), getName(), hostname);
    }

    return hostComponents.get(hostname);
  }

  @Override
  public State getDesiredState() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

      if (desiredStateEntity != null) {
        return desiredStateEntity.getDesiredState();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + componentName);
      }

    return null;
  }

  @Override
  public void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service, clusterName={}, clusterId={}, serviceName={}, serviceComponentName={}, oldDesiredState={}, newDesiredState={}",
        service.getCluster().getClusterName(), service.getCluster().getClusterId(), service.getName(), getName(), getDesiredState(), state);
    }

    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      desiredStateEntity.setDesiredState(state);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
    }
  }

  @Override
  public StackId getDesiredStackId() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    StackEntity stackEntity = desiredStateEntity.getDesiredStack();
    if (null != stackEntity) {
      return new StackId(stackEntity.getStackName(), stackEntity.getStackVersion());
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDesiredRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity) {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      desiredStateEntity.setDesiredRepositoryVersion(repositoryVersionEntity);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RepositoryVersionEntity getDesiredRepositoryVersion() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    return desiredStateEntity.getDesiredRepositoryVersion();
  }

  @Override
  public String getDesiredVersion() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    return desiredStateEntity.getDesiredVersion();
  }

  @Override
  public ServiceComponentResponse convertToResponse() {
    Cluster cluster = service.getCluster();
    RepositoryVersionEntity repositoryVersionEntity = getDesiredRepositoryVersion();
    StackId desiredStackId = repositoryVersionEntity.getStackId();

    ServiceComponentResponse r = new ServiceComponentResponse(getClusterId(),
        cluster.getClusterName(), service.getName(), getName(),
        desiredStackId, getDesiredState().toString(),
        getServiceComponentStateCount(), isRecoveryEnabled(), displayName,
        repositoryVersionEntity.getVersion(), getRepositoryState());

    return r;
  }

  @Override
  public String getClusterName() {
    return service.getCluster().getClusterName();
  }


  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("ServiceComponent={ serviceComponentName=").append(getName())
      .append(", recoveryEnabled=").append(isRecoveryEnabled())
      .append(", clusterName=").append(service.getCluster().getClusterName())
      .append(", clusterId=").append(service.getCluster().getClusterId())
      .append(", serviceName=").append(service.getName())
      .append(", desiredStackVersion=").append(getDesiredStackId())
      .append(", desiredState=").append(getDesiredState())
      .append(", hostcomponents=[ ");
    boolean first = true;
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!first) {
        sb.append(" , ");
      }
      first = false;
      sb.append("\n        ");
      sch.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ] }");
  }

  @Transactional
  protected void persistEntities(ServiceComponentDesiredStateEntity desiredStateEntity) {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(service.getClusterId());
    pk.setServiceName(service.getName());
    ClusterServiceEntity serviceEntity = clusterServiceDAO.findByPK(pk);

    desiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceComponentDesiredStateDAO.create(desiredStateEntity);
    serviceEntity.getServiceComponentDesiredStateEntities().add(desiredStateEntity);
    serviceEntity = clusterServiceDAO.merge(serviceEntity);
  }

  @Override
  public boolean isClientComponent() {
    return isClientComponent;
  }

  @Override
  public boolean isMasterComponent() {
    return isMasterComponent;
  }


  @Override
  public boolean isVersionAdvertised() {
    return isVersionAdvertised;
  }

  @Override
  public boolean canBeRemoved() {
    // A component can be deleted if all it's host components
    // can be removed, irrespective of the state of
    // the component itself
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!sch.canBeRemoved()) {
        LOG.warn("Found non removable hostcomponent when trying to" + " delete service component"
            + ", clusterName=" + getClusterName() + ", serviceName=" + getServiceName()
            + ", componentName=" + getName() + ", state=" + sch.getState() + ", hostname="
            + sch.getHostName());
        return false;
      }
    }
    return true;
  }

  @Override
  @Transactional
  public void deleteAllServiceComponentHosts(DeleteHostComponentStatusMetaData deleteMetaData) {
    readWriteLock.writeLock().lock();
    try {
      LOG.info("Deleting all servicecomponenthosts for component" + ", clusterName="
          + getClusterName() + ", serviceName=" + getServiceName() + ", componentName=" + getName()
          + ", recoveryEnabled=" + isRecoveryEnabled());
      for (ServiceComponentHost sch : hostComponents.values()) {
        if (!sch.canBeRemoved()) {
          deleteMetaData.setAmbariException(new AmbariException("Found non removable hostcomponent " + " when trying to delete"
              + " all hostcomponents from servicecomponent" + ", clusterName=" + getClusterName()
              + ", serviceName=" + getServiceName() + ", componentName=" + getName()
              + ", recoveryEnabled=" + isRecoveryEnabled() + ", hostname=" + sch.getHostName()));
          return;
        }
      }

      for (ServiceComponentHost serviceComponentHost : hostComponents.values()) {
        serviceComponentHost.delete(deleteMetaData);
      }

      hostComponents.clear();
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteServiceComponentHosts(String hostname, DeleteHostComponentStatusMetaData deleteMetaData) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      ServiceComponentHost sch = getServiceComponentHost(hostname);
      LOG.info("Deleting servicecomponenthost for cluster" + ", clusterName=" + getClusterName()
          + ", serviceName=" + getServiceName() + ", componentName=" + getName()
          + ", recoveryEnabled=" + isRecoveryEnabled() + ", hostname=" + sch.getHostName() + ", state=" + sch.getState());
      if (!sch.canBeRemoved()) {
        throw new AmbariException("Current host component state prohibiting component removal."
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", recoveryEnabled=" + isRecoveryEnabled()
            + ", hostname=" + sch.getHostName()
            + ", state=" + sch.getState());
      }
      sch.delete(deleteMetaData);
      hostComponents.remove(hostname);

    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  @Transactional
  public void delete(DeleteHostComponentStatusMetaData deleteMetaData) {
    readWriteLock.writeLock().lock();
    try {
      deleteAllServiceComponentHosts(deleteMetaData);
      if (deleteMetaData.getAmbariException() != null) {
        return;
      }

      ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
          desiredStateEntityId);

      serviceComponentDesiredStateDAO.remove(desiredStateEntity);

    } finally {
      readWriteLock.writeLock().unlock();
    }
  }


  /**
   * Follows this version logic:
   * <table border="1">
   *   <tr>
   *     <th>DB hostcomponent1</th>
   *     <th>DB hostcomponentN</th>
   *     <th>DB desired</th>
   *     <th>New desired</th>
   *     <th>Repo State</th>
   *   </tr>
   *   <tr>
   *     <td>v1</td>
   *     <td>v1</td>
   *     <td>UNKNOWN</td>
   *     <td>v1</td>
   *     <td>CURRENT</td>
   *   </tr>
   *   <tr>
   *     <td>v1</td>
   *     <td>v2</td>
   *     <td>UNKNOWN</td>
   *     <td>UNKNOWN</td>
   *     <td>OUT_OF_SYNC</td>
   *   </tr>
   *   <tr>
   *     <td>v1</td>
   *     <td>v2</td>
   *     <td>v2</td>
   *     <td>v2 (no change)</td>
   *     <td>OUT_OF_SYNC</td>
   *   </tr>
   *   <tr>
   *     <td>v2</td>
   *     <td>v2</td>
   *     <td>v1</td>
   *     <td>v1 (no change)</td>
   *     <td>OUT_OF_SYNC</td>
   *   </tr>
   *   <tr>
   *     <td>v2</td>
   *     <td>v2</td>
   *     <td>v2</td>
   *     <td>v2 (no change)</td>
   *     <td>CURRENT</td>
   *   </tr>
   * </table>
   */
  @Override
  @Transactional
  public void updateRepositoryState(String reportedVersion) throws AmbariException {

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    List<ServiceComponentVersionEntity> componentVersions = serviceComponentDesiredStateDAO.findVersions(
        getClusterId(), getServiceName(), getName());

    // per component, this list should be small, so iterating here isn't a big deal
    Map<String, ServiceComponentVersionEntity> map = new HashMap<>(Maps.uniqueIndex(componentVersions,
        new Function<ServiceComponentVersionEntity, String>() {
          @Override
          public String apply(ServiceComponentVersionEntity input) {
            return input.getRepositoryVersion().getVersion();
          }
      }));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Existing versions for {}/{}/{}: {}",
          getClusterName(), getServiceName(), getName(), map.keySet());
    }

    ServiceComponentVersionEntity componentVersion = map.get(reportedVersion);

    if (null == componentVersion) {
      RepositoryVersionEntity repoVersion = repoVersionDAO.findByStackAndVersion(
          getDesiredStackId(), reportedVersion);

      if (null != repoVersion) {
        componentVersion = new ServiceComponentVersionEntity();
        componentVersion.setRepositoryVersion(repoVersion);
        componentVersion.setState(RepositoryVersionState.INSTALLED);
        componentVersion.setUserName("auto-reported");

        // since we've never seen this version before, mark the component as CURRENT
        serviceComponentDesiredStateEntity.setRepositoryState(RepositoryVersionState.CURRENT);
        serviceComponentDesiredStateEntity.addVersion(componentVersion);

        serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(serviceComponentDesiredStateEntity);

        map.put(reportedVersion, componentVersion);

      } else {
        LOG.warn("There is no repository available for stack {}, version {}",
            getDesiredStackId(), reportedVersion);
      }
    }

    if (MapUtils.isNotEmpty(map)) {
      String desiredVersion = serviceComponentDesiredStateEntity.getDesiredVersion();
      RepositoryVersionEntity desiredRepositoryVersion = service.getDesiredRepositoryVersion();

      List<HostComponentStateEntity> hostComponents = hostComponentDAO.findByServiceAndComponentAndNotVersion(
          serviceComponentDesiredStateEntity.getServiceName(), serviceComponentDesiredStateEntity.getComponentName(), reportedVersion);

      LOG.debug("{}/{} reportedVersion={}, desiredVersion={}, non-matching desired count={}, repo_state={}",
          serviceComponentDesiredStateEntity.getServiceName(), serviceComponentDesiredStateEntity.getComponentName(), reportedVersion,
          desiredVersion, hostComponents.size(), serviceComponentDesiredStateEntity.getRepositoryState());

      // !!! if we are unknown, that means it's never been set.  Try to determine it.
      if (StackVersionListener.UNKNOWN_VERSION.equals(desiredVersion)) {
        if (CollectionUtils.isEmpty(hostComponents)) {
          // all host components are the same version as reported
          serviceComponentDesiredStateEntity.setDesiredRepositoryVersion(desiredRepositoryVersion);
          serviceComponentDesiredStateEntity.setRepositoryState(RepositoryVersionState.CURRENT);
        } else {
          // desired is UNKNOWN and there's a mix of versions in the host components
          serviceComponentDesiredStateEntity.setRepositoryState(RepositoryVersionState.OUT_OF_SYNC);
        }
      } else {
        if (!reportedVersion.equals(desiredVersion)) {
          serviceComponentDesiredStateEntity.setRepositoryState(RepositoryVersionState.OUT_OF_SYNC);
        } else if (CollectionUtils.isEmpty(hostComponents)) {
          serviceComponentDesiredStateEntity.setRepositoryState(RepositoryVersionState.CURRENT);
        }
      }

      serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(serviceComponentDesiredStateEntity);
    }
  }

  @Override
  public RepositoryVersionState getRepositoryState() {
    ServiceComponentDesiredStateEntity component = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (null != component) {
      return component.getRepositoryState();
    } else {
      LOG.warn("Cannot retrieve repository state on component that may have been deleted: service {}, component {}",
          service != null ? service.getName() : null, componentName);

      return null;
    }
  }


  private int getSCHCountByState(State state) {
    int count = 0;
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (sch.getState() == state) {
        count++;
      }
    }
    return count;
  }

  /**
   * Count the ServiceComponentHosts that have given state and are effectively not in maintenanceMode
   * @param state
   * @return
   */
  private int getMaintenanceOffSCHCountByState(State state) {
    int count = 0;
    for (ServiceComponentHost sch : hostComponents.values()) {
      try {
        MaintenanceState effectiveMaintenanceState = maintenanceStateHelper.getEffectiveState(sch, sch.getHost());
        if (sch.getState() == state && effectiveMaintenanceState == MaintenanceState.OFF) {
          count++;
        }
      } catch (AmbariException e) {
        e.printStackTrace();
      }
    }
    return count;
  }

  private Map <String, Integer> getServiceComponentStateCount() {
    Map <String, Integer> serviceComponentStateCountMap = new HashMap<>();
    serviceComponentStateCountMap.put("startedCount", getSCHCountByState(State.STARTED));
    serviceComponentStateCountMap.put("installedCount", getSCHCountByState(State.INSTALLED));
    serviceComponentStateCountMap.put("installedAndMaintenanceOffCount", getMaintenanceOffSCHCountByState(State.INSTALLED));
    serviceComponentStateCountMap.put("installFailedCount", getSCHCountByState(State.INSTALL_FAILED));
    serviceComponentStateCountMap.put("initCount", getSCHCountByState(State.INIT));
    serviceComponentStateCountMap.put("unknownCount", getSCHCountByState(State.UNKNOWN));
    serviceComponentStateCountMap.put("totalCount", hostComponents.size());
    return serviceComponentStateCountMap;
  }
}
