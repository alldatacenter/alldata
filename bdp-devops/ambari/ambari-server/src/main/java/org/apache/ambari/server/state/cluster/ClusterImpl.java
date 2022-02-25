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

package org.apache.ambari.server.state.cluster;

import static java.util.stream.Collectors.toList;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ConfigGroupNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariSessionManager;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.AmbariEvent.AmbariEventType;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.ClusterEvent;
import org.apache.ambari.server.events.ClusterProvisionedEvent;
import org.apache.ambari.server.events.ConfigsUpdateEvent;
import org.apache.ambari.server.events.jpa.EntityManagerCacheInvalidationEvent;
import org.apache.ambari.server.events.jpa.JPAEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.JPAEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.logging.LockFactory;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.state.BlueprintProvisioningState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ClusterHealthReport;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.topology.STOMPComponentsDeleteHandler;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;

public class ClusterImpl implements Cluster {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterImpl.class);
  private static final Logger configChangeLog = LoggerFactory.getLogger("configchange");

  /**
   * Prefix for cluster session attributes name.
   */
  private static final String CLUSTER_SESSION_ATTRIBUTES_PREFIX = "cluster_session_attributes:";

  @Inject
  private Clusters clusters;

  private StackId desiredStackVersion;

  private final ConcurrentSkipListMap<String, Service> services = new ConcurrentSkipListMap<>();

  /**
   * [ Config Type -> [ Config Version Tag -> Config ] ]
   */
  private final ConcurrentMap<String, ConcurrentMap<String, Config>> allConfigs = new ConcurrentHashMap<>();

  /**
   * [ ServiceName -> [ ServiceComponentName -> [ HostName -> [ ... ] ] ] ]
   */
  private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, ServiceComponentHost>>> serviceComponentHosts = new ConcurrentHashMap<>();

  /**
   * [ HostName -> [ ... ] ]
   */
  private final ConcurrentMap<String, List<ServiceComponentHost>> serviceComponentHostsByHost = new ConcurrentHashMap<>();

  /**
   * Map of existing config groups
   */
  private final Map<Long, ConfigGroup> clusterConfigGroups = new ConcurrentHashMap<>();

  /**
   * Map of Request schedules for this cluster
   */
  private final Map<Long, RequestExecution> requestExecutions = new ConcurrentHashMap<>();

  private final ReadWriteLock clusterGlobalLock;

  /**
   * The unique ID of the {@link @ClusterEntity}.
   */
  private final long clusterId;

  private String clusterName;

  @Inject
  private ClusterDAO clusterDAO;

  @Inject
  private ClusterStateDAO clusterStateDAO;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ConfigFactory configFactory;

  @Inject
  private LockFactory lockFactory;

  @Inject
  private HostConfigMappingDAO hostConfigMappingDAO;

  @Inject
  private ConfigGroupFactory configGroupFactory;

  @Inject
  private RequestExecutionFactory requestExecutionFactory;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private AmbariManagementController controller;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  @Inject
  private AlertDefinitionDAO alertDefinitionDAO;

  @Inject
  private AlertDispatchDAO alertDispatchDAO;

  @Inject
  private UpgradeDAO upgradeDAO;

  @Inject
  private AmbariSessionManager sessionManager;

  @Inject
  private TopologyRequestDAO topologyRequestDAO;

  @Inject
  private STOMPComponentsDeleteHandler STOMPComponentsDeleteHandler;

  @Inject
  private HostLevelParamsHolder hostLevelParamsHolder;

  /**
   * Data access object used for looking up stacks from the database.
   */
  @Inject
  private StackDAO stackDAO;

  private volatile Multimap<String, String> serviceConfigTypes;

  /**
   * Used to publish events relating to cluster CRUD operations and to receive
   * information about cluster operations.
   */
  private AmbariEventPublisher eventPublisher;

  /**
   * Used for broadcasting {@link JPAEvent}s.
   */
  @Inject
  private JPAEventPublisher jpaEventPublisher;

  /**
   * Used for getting instances of {@link RoleCommand} for this cluster.
   */
  @Inject
  private RoleCommandOrderProvider roleCommandOrderProvider;

  /**
   * Used to create instances of {@link UpgradeContext} with injected
   * dependencies. The {@link UpgradeContext} is used to populate the command
   * with upgrade information on the command/role maps if the upgrade is
   * suspended.
   */
  @Inject
  private UpgradeContextFactory upgradeContextFactory;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;

  /**
   * A simple cache for looking up {@code cluster-env} properties for a cluster.
   * This map is changed whenever {{cluster-env}} is changed and we receive a
   * {@link ClusterConfigChangedEvent}.
   */
  private Map<String, String> m_clusterPropertyCache = new ConcurrentHashMap<>();

  @Inject
  public ClusterImpl(@Assisted ClusterEntity clusterEntity, Injector injector,
      AmbariEventPublisher eventPublisher)
      throws AmbariException {

    clusterId = clusterEntity.getClusterId();
    clusterName = clusterEntity.getClusterName();

    injector.injectMembers(this);

    clusterGlobalLock = lockFactory.newReadWriteLock("clusterGlobalLock");

    loadStackVersion();
    loadServices();
    loadServiceHostComponents();

    // cache configurations before loading configuration groups
    cacheConfigurations();
    loadConfigGroups();

    loadRequestExecutions();

    if (desiredStackVersion != null && !StringUtils.isEmpty(desiredStackVersion.getStackName()) && !
      StringUtils.isEmpty(desiredStackVersion.getStackVersion())) {
      loadServiceConfigTypes();
    }

    // register to receive stuff
    eventPublisher.register(this);
    this.eventPublisher = eventPublisher;
  }

  private void loadServiceConfigTypes() throws AmbariException {
    try {
      serviceConfigTypes = collectServiceConfigTypesMapping();
    } catch (AmbariException e) {
      LOG.error("Cannot load stack info:", e);
      throw e;
    }
    LOG.info("Service config types loaded: {}", serviceConfigTypes);
  }

  /**
   * Construct config type to service name mapping
   * @throws AmbariException when stack or its part not found
   */
  private Multimap<String, String> collectServiceConfigTypesMapping() throws AmbariException {
    Multimap<String, String> serviceConfigTypes = HashMultimap.create();

    Map<String, ServiceInfo> serviceInfoMap = null;
    try {
      serviceInfoMap = ambariMetaInfo.getServices(desiredStackVersion.getStackName(), desiredStackVersion.getStackVersion());
    } catch (ParentObjectNotFoundException e) {
      LOG.error("Service config versioning disabled due to exception: ", e);
      return serviceConfigTypes;
    }
    for (Entry<String, ServiceInfo> entry : serviceInfoMap.entrySet()) {
      String serviceName = entry.getKey();
      ServiceInfo serviceInfo = entry.getValue();
      Set<String> configTypes = serviceInfo.getConfigTypeAttributes().keySet();
      for (String configType : configTypes) {
        serviceConfigTypes.put(serviceName, configType);
      }
    }

    return serviceConfigTypes;
  }

  /**
   * Make sure we load all the service host components.
   * We need this for live status checks.
   */
  private void loadServiceHostComponents() {
    for (Entry<String, Service> serviceKV : services.entrySet()) {
      /* get all the service component hosts **/
      Service service = serviceKV.getValue();
      if (!serviceComponentHosts.containsKey(service.getName())) {
        serviceComponentHosts.put(service.getName(), new ConcurrentHashMap<>());
      }

      for (Entry<String, ServiceComponent> svcComponent : service.getServiceComponents().entrySet()) {
        ServiceComponent comp = svcComponent.getValue();
        String componentName = svcComponent.getKey();
        if (!serviceComponentHosts.get(service.getName()).containsKey(componentName)) {
          serviceComponentHosts.get(service.getName()).put(componentName, new ConcurrentHashMap<>());
        }

        // Get Service Host Components
        for (Entry<String, ServiceComponentHost> svchost : comp.getServiceComponentHosts().entrySet()) {
          String hostname = svchost.getKey();
          ServiceComponentHost svcHostComponent = svchost.getValue();
          if (!serviceComponentHostsByHost.containsKey(hostname)) {
            serviceComponentHostsByHost.put(hostname, new CopyOnWriteArrayList<>());
          }

          List<ServiceComponentHost> compList = serviceComponentHostsByHost.get(hostname);
          compList.add(svcHostComponent);

          if (!serviceComponentHosts.get(service.getName()).get(componentName).containsKey(
              hostname)) {
            serviceComponentHosts.get(service.getName()).get(componentName).put(hostname,
                svcHostComponent);
          }
        }
      }
    }
  }

  private void loadServices() {
    ClusterEntity clusterEntity = getClusterEntity();
    if (CollectionUtils.isEmpty(clusterEntity.getClusterServiceEntities())) {
      return;
    }

    for (ClusterServiceEntity serviceEntity : clusterEntity.getClusterServiceEntities()) {
      StackId stackId = getCurrentStackVersion();
      try {
        if (ambariMetaInfo.getService(stackId.getStackName(),
          stackId.getStackVersion(), serviceEntity.getServiceName()) != null) {
          services.put(serviceEntity.getServiceName(),
            serviceFactory.createExisting(this, serviceEntity));
        }
      } catch (AmbariException e) {
        LOG.error(String.format(
          "Can not get service info: stackName=%s, stackVersion=%s, serviceName=%s",
          stackId.getStackName(), stackId.getStackVersion(),
          serviceEntity.getServiceName()));
      }
    }
  }

  private void loadConfigGroups() {
    ClusterEntity clusterEntity = getClusterEntity();
    if (!clusterEntity.getConfigGroupEntities().isEmpty()) {
      for (ConfigGroupEntity configGroupEntity : clusterEntity.getConfigGroupEntities()) {
        clusterConfigGroups.put(configGroupEntity.getGroupId(),
            configGroupFactory.createExisting(this, configGroupEntity));
      }
    }
  }

  private void loadRequestExecutions() {
    ClusterEntity clusterEntity = getClusterEntity();
    if (!clusterEntity.getRequestScheduleEntities().isEmpty()) {
      for (RequestScheduleEntity scheduleEntity : clusterEntity.getRequestScheduleEntities()) {
        requestExecutions.put(scheduleEntity.getScheduleId(),
            requestExecutionFactory.createExisting(this, scheduleEntity));
      }
    }
  }

  @Override
  public void addConfigGroup(ConfigGroup configGroup) throws AmbariException {
    String hostList = "";
    if(LOG.isDebugEnabled()) {
      if (configGroup.getHosts() != null) {
        for (Host host : configGroup.getHosts().values()) {
          hostList += host.getHostName() + ", ";
        }
      }
    }

    LOG.debug("Adding a new Config group, clusterName = {}, groupName = {}, tag = {} with hosts {}",
      getClusterName(), configGroup.getName(), configGroup.getTag(), hostList);

    if (clusterConfigGroups.containsKey(configGroup.getId())) {
      // The loadConfigGroups will load all groups to memory
      LOG.debug("Config group already exists, clusterName = {}, groupName = {}, groupId = {}, tag = {}",
        getClusterName(), configGroup.getName(), configGroup.getId(), configGroup.getTag());
    } else {
      clusterConfigGroups.put(configGroup.getId(), configGroup);
    }
  }

  @Override
  public Map<Long, ConfigGroup> getConfigGroups() {
    return Collections.unmodifiableMap(clusterConfigGroups);
  }

  @Override
  public Map<Long, ConfigGroup> getConfigGroupsByHostname(String hostname)
    throws AmbariException {
    Map<Long, ConfigGroup> configGroups = new HashMap<>();

    for (Entry<Long, ConfigGroup> groupEntry : clusterConfigGroups.entrySet()) {
      Long id = groupEntry.getKey();
      ConfigGroup group = groupEntry.getValue();
      for (Host host : group.getHosts().values()) {
        if (StringUtils.equals(hostname, host.getHostName())) {
          configGroups.put(id, group);
          break;
        }
      }
    }
    return configGroups;
  }

  @Override
  public ConfigGroup getConfigGroupsById(Long configId) {
    return clusterConfigGroups.get(configId);
  }

  @Override
  public Map<Long, ConfigGroup> getConfigGroupsByServiceName(String serviceName) {
    Map<Long, ConfigGroup> configGroups = new HashMap<>();

    for (Entry<Long, ConfigGroup> groupEntry : clusterConfigGroups.entrySet()) {
      Long id = groupEntry.getKey();
      ConfigGroup group = groupEntry.getValue();
      if (StringUtils.equals(serviceName, group.getServiceName())) {
        configGroups.put(id, group);
      }
    }
    return configGroups;
  }

  @Override
  public void addRequestExecution(RequestExecution requestExecution) throws AmbariException {
    LOG.info("Adding a new request schedule" + ", clusterName = " + getClusterName() + ", id = "
        + requestExecution.getId() + ", description = " + requestExecution.getDescription());

    if (requestExecutions.containsKey(requestExecution.getId())) {
      LOG.debug("Request schedule already exists, clusterName = {}, id = {}, description = {}",
        getClusterName(), requestExecution.getId(), requestExecution.getDescription());
    } else {
      requestExecutions.put(requestExecution.getId(), requestExecution);
    }
  }

  @Override
  public Map<Long, RequestExecution> getAllRequestExecutions() {
    return Collections.unmodifiableMap(requestExecutions);
  }

  @Override
  public void deleteRequestExecution(Long id) throws AmbariException {
    RequestExecution requestExecution = requestExecutions.get(id);
    if (requestExecution == null) {
      throw new AmbariException("Request schedule does not exists, " + "id = " + id);
    }
    LOG.info("Deleting request schedule" + ", clusterName = " + getClusterName() + ", id = "
        + requestExecution.getId() + ", description = " + requestExecution.getDescription());

    requestExecution.delete();
    requestExecutions.remove(id);
  }

  @Override
  public void deleteConfigGroup(Long id) throws AmbariException {
    ConfigGroup configGroup = clusterConfigGroups.get(id);
    if (configGroup == null) {
      throw new ConfigGroupNotFoundException(getClusterName(), id.toString());
    }

    LOG.debug("Deleting Config group, clusterName = {}, groupName = {}, groupId = {}, tag = {}",
      getClusterName(), configGroup.getName(), configGroup.getId(), configGroup.getTag());

    configGroup.delete();
    clusterConfigGroups.remove(id);

    configHelper.updateAgentConfigs(Collections.singleton(configGroup.getClusterName()));
  }

  public ServiceComponentHost getServiceComponentHost(String serviceName,
      String serviceComponentName, String hostname) throws AmbariException {
    if (!serviceComponentHosts.containsKey(serviceName)
        || !serviceComponentHosts.get(serviceName).containsKey(
            serviceComponentName)
        || !serviceComponentHosts.get(serviceName).get(serviceComponentName).containsKey(
            hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(),
          serviceName, serviceComponentName, hostname);
    }
    return serviceComponentHosts.get(serviceName).get(serviceComponentName).get(hostname);
  }

  @Override
  public List<ServiceComponentHost> getServiceComponentHosts() {
    List<ServiceComponentHost> serviceComponentHosts = new ArrayList<>();
    if (!serviceComponentHostsByHost.isEmpty()) {
      for (List<ServiceComponentHost> schList : serviceComponentHostsByHost.values()) {
        serviceComponentHosts.addAll(schList);
      }
    }
    return Collections.unmodifiableList(serviceComponentHosts);
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public void setClusterName(String clusterName) {
    String oldName = null;
    ClusterEntity clusterEntity = getClusterEntity();
    oldName = clusterEntity.getClusterName();
    clusterEntity.setClusterName(clusterName);

    // RollbackException possibility if UNIQUE constraint violated
    clusterEntity = clusterDAO.merge(clusterEntity);
    clusters.updateClusterName(oldName, clusterName);
    this.clusterName = clusterName;

    // if the name changed, fire an event
    if (!StringUtils.equals(oldName, clusterName)) {
      ClusterEvent clusterNameChangedEvent = new ClusterEvent(AmbariEventType.CLUSTER_RENAME, clusterId);
      eventPublisher.publish(clusterNameChangedEvent);
    }
  }

  @Override
  public Long getResourceId() {
    ClusterEntity clusterEntity = getClusterEntity();

    ResourceEntity resourceEntity = clusterEntity.getResource();
    if (resourceEntity == null) {
      LOG.warn(
          "There is no resource associated with this cluster:\n\tCluster Name: {}\n\tCluster ID: {}",
          getClusterName(), getClusterId());
      return null;
    } else {
      return resourceEntity.getId();
    }
  }

  @Override
  @Transactional
  public void addServiceComponentHosts(Collection<ServiceComponentHost> serviceComponentHosts) throws AmbariException {
    for (ServiceComponentHost serviceComponentHost : serviceComponentHosts) {
      Service service = getService(serviceComponentHost.getServiceName());
      ServiceComponent serviceComponent = service.getServiceComponent(serviceComponentHost.getServiceComponentName());
      serviceComponent.addServiceComponentHost(serviceComponentHost);
    }
  }

  public void addServiceComponentHost(ServiceComponentHost svcCompHost)
      throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Trying to add component {} of service {} on {} to the cache",
          svcCompHost.getServiceComponentName(), svcCompHost.getServiceName(),
          svcCompHost.getHostName());
    }

    final String hostname = svcCompHost.getHostName();
    final String serviceName = svcCompHost.getServiceName();
    final String componentName = svcCompHost.getServiceComponentName();

    Set<Cluster> cs = clusters.getClustersForHost(hostname);

    boolean clusterFound = false;
    Iterator<Cluster> iter = cs.iterator();
    while (iter.hasNext()) {
      Cluster c = iter.next();
      if (c.getClusterId() == getClusterId()) {
        clusterFound = true;
        break;
      }
    }

    if (!clusterFound) {
      throw new AmbariException("Host does not belong this cluster"
          + ", hostname=" + hostname + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId());
    }

    if (!serviceComponentHosts.containsKey(serviceName)) {
      serviceComponentHosts.put(serviceName, new ConcurrentHashMap<>());
    }

    if (!serviceComponentHosts.get(serviceName).containsKey(componentName)) {
      serviceComponentHosts.get(serviceName).put(componentName, new ConcurrentHashMap<>());
    }

    if (serviceComponentHosts.get(serviceName).get(componentName).containsKey(
        hostname)) {
      throw new AmbariException("Duplicate entry for ServiceComponentHost"
          + ", serviceName=" + serviceName + ", serviceComponentName"
          + componentName + ", hostname= " + hostname);
    }

    if (!serviceComponentHostsByHost.containsKey(hostname)) {
      serviceComponentHostsByHost.put(hostname, new CopyOnWriteArrayList<>());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a new ServiceComponentHost, clusterName={}, clusterId={}, serviceName={}, serviceComponentName{}, hostname= {}",
        getClusterName(), getClusterId(), serviceName, componentName, hostname);
    }

    serviceComponentHosts.get(serviceName).get(componentName).put(hostname,
      svcCompHost);

    serviceComponentHostsByHost.get(hostname).add(svcCompHost);
  }

  @Override
  public void removeServiceComponentHost(ServiceComponentHost svcCompHost)
    throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(
          "Trying to remove component {} of service {} on {} from the cache",
          svcCompHost.getServiceComponentName(), svcCompHost.getServiceName(),
          svcCompHost.getHostName());
    }

    final String hostname = svcCompHost.getHostName();
    final String serviceName = svcCompHost.getServiceName();
    final String componentName = svcCompHost.getServiceComponentName();
    Set<Cluster> cs = clusters.getClustersForHost(hostname);

    boolean clusterFound = false;
    Iterator<Cluster> iter = cs.iterator();
    while (iter.hasNext()) {
      Cluster c = iter.next();
      if (c.getClusterId() == getClusterId()) {
        clusterFound = true;
        break;
      }
    }

    if (!clusterFound) {
      throw new AmbariException("Host does not belong this cluster"
          + ", hostname=" + hostname + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId());
    }

    if (!serviceComponentHosts.containsKey(serviceName)
        || !serviceComponentHosts.get(serviceName).containsKey(componentName)
        || !serviceComponentHosts.get(serviceName).get(componentName).containsKey(
            hostname)) {
      throw new AmbariException("Invalid entry for ServiceComponentHost"
          + ", serviceName=" + serviceName + ", serviceComponentName"
          + componentName + ", hostname= " + hostname);
    }

    if (!serviceComponentHostsByHost.containsKey(hostname)) {
      throw new AmbariException("Invalid host entry for ServiceComponentHost"
          + ", serviceName=" + serviceName + ", serviceComponentName"
          + componentName + ", hostname= " + hostname);
    }

    ServiceComponentHost schToRemove = null;
    for (ServiceComponentHost sch : serviceComponentHostsByHost.get(hostname)) {
      if (sch.getServiceName().equals(serviceName)
          && sch.getServiceComponentName().equals(componentName)
          && sch.getHostName().equals(hostname)) {
        schToRemove = sch;
        break;
      }
    }

    if (schToRemove == null) {
      LOG.warn("Unavailable in per host cache. ServiceComponentHost"
        + ", serviceName=" + serviceName
        + ", serviceComponentName" + componentName
        + ", hostname= " + hostname);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Removing a ServiceComponentHost, clusterName={}, clusterId={}, serviceName={}, serviceComponentName{}, hostname= {}",
        getClusterName(), getClusterId(), serviceName, componentName, hostname);
    }

    serviceComponentHosts.get(serviceName).get(componentName).remove(hostname);
    if (schToRemove != null) {
      serviceComponentHostsByHost.get(hostname).remove(schToRemove);
    }
  }

  @Override
  public long getClusterId() {
    // Add cluster creates the managed entity before creating the Cluster
    // instance so id would not be null.
    return clusterId;
  }

  @Override
  public List<ServiceComponentHost> getServiceComponentHosts(String hostname) {
    List<ServiceComponentHost> serviceComponentHosts = serviceComponentHostsByHost.get(hostname);
    if (null != serviceComponentHosts) {
      return new CopyOnWriteArrayList<>(serviceComponentHosts);
    }

    return new ArrayList<>();
  }

  @Override
  public Map<String, Set<String>> getServiceComponentHostMap(Set<String> hostNames, Set<String> serviceNames) {
    Map<String, Set<String>> componentHostMap = new TreeMap<>();

    Collection<Host> hosts = getHosts();

    if(hosts != null) {
      for (Host host : hosts) {
        String hostname = host.getHostName();

        // If this host is not filtered out, continue processing
        if ((hostNames == null) || hostNames.contains(hostname)) {
          List<ServiceComponentHost> serviceComponentHosts = getServiceComponentHosts(hostname);

          if (serviceComponentHosts != null) {
            for (ServiceComponentHost sch : serviceComponentHosts) {
              // If the service for this ServiceComponentHost is not filtered out, continue processing
              if ((serviceNames == null) || serviceNames.contains(sch.getServiceName())) {
                String component = sch.getServiceComponentName();
                Set<String> componentHosts = componentHostMap.get(component);

                if (componentHosts == null) {
                  componentHosts = new TreeSet<>();
                  componentHostMap.put(component, componentHosts);
                }

                componentHosts.add(hostname);
              }
            }
          }
        }
      }
    }

    return componentHostMap;
  }

  @Override
  public List<ServiceComponentHost> getServiceComponentHosts(String serviceName, String componentName) {
    ArrayList<ServiceComponentHost> foundItems = new ArrayList<>();

    ConcurrentMap<String, ConcurrentMap<String, ServiceComponentHost>> foundByService = serviceComponentHosts.get(
        serviceName);
    if (foundByService != null) {
      if (componentName == null) {
        for (Map<String, ServiceComponentHost> foundByComponent : foundByService.values()) {
          foundItems.addAll(foundByComponent.values());
        }
      } else if (foundByService.containsKey(componentName)) {
        foundItems.addAll(foundByService.get(componentName).values());
      }
    }

    return foundItems;
  }

  @Override
  public void addService(Service service) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a new Service, clusterName={}, clusterId={}, serviceName={}", getClusterName(), getClusterId(), service.getName());
    }
    services.put(service.getName(), service);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service addService(String serviceName, RepositoryVersionEntity repositoryVersion) throws AmbariException {
    if (services.containsKey(serviceName)) {
      String message = MessageFormat.format("The {0} service already exists in {1}", serviceName,
          getClusterName());

      throw new AmbariException(message);
    }

    @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES)
    Service service = serviceFactory.createNew(this, serviceName, repositoryVersion);
    addService(service);

    return service;
  }

  @Override
  public Service getService(String serviceName) throws AmbariException {
    Service service = services.get(serviceName);
    if (null == service) {
      throw new ServiceNotFoundException(getClusterName(), serviceName);
    }

    return service;
  }

  @Override
  public Map<String, Service> getServices() {
    return new HashMap<>(services);
  }

  @Override
  public Service getServiceByComponentName(String componentName) throws AmbariException {
    for (Service service : services.values()) {
      for (ServiceComponent component : service.getServiceComponents().values()) {
        if (component.getName().equals(componentName)) {
          return service;
        }
      }
    }

    throw new ServiceNotFoundException(getClusterName(), "component: " + componentName);
  }


  @Override
  public StackId getDesiredStackVersion() {
    return desiredStackVersion;
  }

  @Override
  public void setDesiredStackVersion(StackId stackId) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Changing DesiredStackVersion of Cluster, clusterName={}, clusterId={}, currentDesiredStackVersion={}, newDesiredStackVersion={}",
          getClusterName(), getClusterId(), desiredStackVersion, stackId);
      }

      desiredStackVersion = stackId;
      StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

      ClusterEntity clusterEntity = getClusterEntity();

      clusterEntity.setDesiredStack(stackEntity);
      clusterEntity = clusterDAO.merge(clusterEntity);

      loadServiceConfigTypes();
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public StackId getCurrentStackVersion() {
    ClusterEntity clusterEntity = getClusterEntity();

    ClusterStateEntity clusterStateEntity = clusterEntity.getClusterStateEntity();
    if (clusterStateEntity != null) {
      StackEntity currentStackEntity = clusterStateEntity.getCurrentStack();
      return new StackId(currentStackEntity);
    }

    return null;
  }

  @Override
  public State getProvisioningState() {
    State provisioningState = null;
    ClusterEntity clusterEntity = getClusterEntity();
    provisioningState = clusterEntity.getProvisioningState();

    if (null == provisioningState) {
      provisioningState = State.INIT;
    }

    return provisioningState;
  }

  @Override
  public void setProvisioningState(State provisioningState) {
    ClusterEntity clusterEntity = getClusterEntity();
    clusterEntity.setProvisioningState(provisioningState);
    clusterEntity = clusterDAO.merge(clusterEntity);
  }

  private boolean setBlueprintProvisioningState(BlueprintProvisioningState blueprintProvisioningState) {
    boolean updated = false;
    for (Service s : getServices().values()) {
      for (ServiceComponent sc : s.getServiceComponents().values()) {
        if (!sc.isClientComponent()) {
          for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            HostComponentDesiredStateEntity desiredStateEntity = sch.getDesiredStateEntity();
            if (desiredStateEntity.getBlueprintProvisioningState() != blueprintProvisioningState) {
              desiredStateEntity.setBlueprintProvisioningState(blueprintProvisioningState);
              hostComponentDesiredStateDAO.merge(desiredStateEntity);
              updated = true;
            }
          }
        }
      }
    }
    return updated;
  }

  @Override
  public SecurityType getSecurityType() {
    SecurityType securityType = null;
    ClusterEntity clusterEntity = getClusterEntity();
    securityType = clusterEntity.getSecurityType();

    if (null == securityType) {
      securityType = SecurityType.NONE;
    }

    return securityType;
  }

  @Override
  public void setSecurityType(SecurityType securityType) {
    ClusterEntity clusterEntity = getClusterEntity();
    clusterEntity.setSecurityType(securityType);
    clusterEntity = clusterDAO.merge(clusterEntity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public List<Host> transitionHostsToInstalling(RepositoryVersionEntity repoVersionEntity,
      VersionDefinitionXml versionDefinitionXml, boolean forceInstalled) throws AmbariException {


    // the hosts to return so that INSTALL commands can be generated for them
    final List<Host> hostsRequiringInstallation;

    clusterGlobalLock.writeLock().lock();
    try {

      // get this once for easy lookup later
      Map<String, Host> hosts = clusters.getHostsForCluster(getClusterName());
      hostsRequiringInstallation = new ArrayList<>(hosts.size());

      // for every host, either create or update the host version to the right
      // state - starting with STATE
      Collection<HostEntity> hostEntities = getClusterEntity().getHostEntities();

      for (HostEntity hostEntity : hostEntities) {

        // start with INSTALLING
        RepositoryVersionState state = RepositoryVersionState.INSTALLING;
        if (forceInstalled) {
          state = RepositoryVersionState.INSTALLED;
        }

        // is this host version not required b/c of versionable components
        Host host = hosts.get(hostEntity.getHostName());
        if (!host.hasComponentsAdvertisingVersions(desiredStackVersion)) {
          state = RepositoryVersionState.NOT_REQUIRED;
        }

        // if the repository is still required, check against the repo type
        if (state != RepositoryVersionState.NOT_REQUIRED) {
          if (repoVersionEntity.getType() != RepositoryType.STANDARD) {
            // does the host gets a different repo state based on VDF and repo
            // type
            boolean hostRequiresRepository = false;
            ClusterVersionSummary clusterSummary = versionDefinitionXml.getClusterSummary(this,
                ambariMetaInfo);
            Set<String> servicesInUpgrade = clusterSummary.getAvailableServiceNames();

            List<ServiceComponentHost> schs = getServiceComponentHosts(hostEntity.getHostName());
            for (ServiceComponentHost serviceComponentHost : schs) {
              String serviceName = serviceComponentHost.getServiceName();
              if (servicesInUpgrade.contains(serviceName)) {
                hostRequiresRepository = true;
                break;
              }
            }

            // if not required, then move onto the next host
            if (!hostRequiresRepository) {
              state = RepositoryVersionState.NOT_REQUIRED;
            }
          }
        }

        // last check if it's still required - check for MM
        if (state != RepositoryVersionState.NOT_REQUIRED) {
          if (host.getMaintenanceState(clusterId) != MaintenanceState.OFF) {
            state = RepositoryVersionState.OUT_OF_SYNC;
          }
        }

        // now that the correct state is determdined for the host version,
        // either update or create it
        HostVersionEntity hostVersionEntity = null;
        Collection<HostVersionEntity> hostVersions = hostEntity.getHostVersionEntities();
        for (HostVersionEntity existingHostVersion : hostVersions) {
          if (Objects.equals(existingHostVersion.getRepositoryVersion().getId(), repoVersionEntity.getId())) {
            hostVersionEntity = existingHostVersion;
            break;
          }
        }

        if (null == hostVersionEntity) {
          hostVersionEntity = new HostVersionEntity(hostEntity, repoVersionEntity, state);
          hostVersionDAO.create(hostVersionEntity);

          // bi-directional association update
          hostVersions.add(hostVersionEntity);
          hostDAO.merge(hostEntity);
        } else {
          hostVersionEntity.setState(state);
          hostVersionEntity = hostVersionDAO.merge(hostVersionEntity);
        }

        LOG.info("Created host version for {}, state={}, repository version={} (repo_id={})",
            hostVersionEntity.getHostName(), hostVersionEntity.getState(),
            repoVersionEntity.getVersion(), repoVersionEntity.getId());

        if (state == RepositoryVersionState.INSTALLING) {
          hostsRequiringInstallation.add(host);
        }
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

    return hostsRequiringInstallation;
  }

  @Override
  @Transactional
  public void setCurrentStackVersion(StackId stackId) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

      ClusterEntity clusterEntity = getClusterEntity();
      ClusterStateEntity clusterStateEntity = clusterStateDAO.findByPK(
          clusterEntity.getClusterId());
      if (clusterStateEntity == null) {
        clusterStateEntity = new ClusterStateEntity();
        clusterStateEntity.setClusterId(clusterEntity.getClusterId());
        clusterStateEntity.setCurrentStack(stackEntity);
        clusterStateEntity.setClusterEntity(clusterEntity);
        clusterStateDAO.create(clusterStateEntity);
        clusterStateEntity = clusterStateDAO.merge(clusterStateEntity);
        clusterEntity.setClusterStateEntity(clusterStateEntity);
        clusterEntity = clusterDAO.merge(clusterEntity);
      } else {
        clusterStateEntity.setCurrentStack(stackEntity);
        clusterStateEntity = clusterStateDAO.merge(clusterStateEntity);
        clusterEntity = clusterDAO.merge(clusterEntity);
      }
    } catch (RollbackException e) {
      LOG.warn("Unable to set version " + stackId + " for cluster "
          + getClusterName());
      throw new AmbariException("Unable to set" + " version=" + stackId
          + " for cluster " + getClusterName(), e);
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public Map<String, Config> getConfigsByType(String configType) {
    clusterGlobalLock.readLock().lock();
    try {
      if (!allConfigs.containsKey(configType)) {
        return null;
      }

      return Collections.unmodifiableMap(allConfigs.get(configType));
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public Config getConfig(String configType, String versionTag) {
    clusterGlobalLock.readLock().lock();
    try {
      if (!allConfigs.containsKey(configType)
          || !allConfigs.get(configType).containsKey(versionTag)) {
        return null;
      }
      return allConfigs.get(configType).get(versionTag);
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public List<Config> getLatestConfigsWithTypes(Collection<String> types) {
    return clusterDAO.getLatestConfigurationsWithTypes(clusterId, getDesiredStackVersion(), types)
      .stream()
      .map(clusterConfigEntity -> configFactory.createExisting(this, clusterConfigEntity))
      .collect(toList());
  }

  @Override
  public Config getConfigByVersion(String configType, Long configVersion) {
    clusterGlobalLock.readLock().lock();
    try {
      if (!allConfigs.containsKey(configType)) {
        return null;
      }

      for (Map.Entry<String, Config> entry : allConfigs.get(configType).entrySet()) {
        if (entry.getValue().getVersion().equals(configVersion)) {
          return entry.getValue();
        }
      }

      return null;
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public void addConfig(Config config) {
    if (config.getType() == null || config.getType().isEmpty()) {
      throw new IllegalArgumentException("Config type cannot be empty");
    }

    clusterGlobalLock.writeLock().lock();
    try {
      if (!allConfigs.containsKey(config.getType())) {
        allConfigs.put(config.getType(), new ConcurrentHashMap<>());
      }

      allConfigs.get(config.getType()).put(config.getTag(), config);
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public Collection<Config> getAllConfigs() {
    clusterGlobalLock.readLock().lock();
    try {
      List<Config> list = new ArrayList<>();
      for (Entry<String, ConcurrentMap<String, Config>> entry : allConfigs.entrySet()) {
        list.addAll(entry.getValue().values());
      }
      return Collections.unmodifiableList(list);
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public ClusterResponse convertToResponse()
    throws AmbariException {
    String clusterName = getClusterName();
    Map<String, Host> hosts = clusters.getHostsForCluster(clusterName);

    return new ClusterResponse(getClusterId(), clusterName,
        getProvisioningState(), getSecurityType(), hosts.keySet(),
        hosts.size(), getDesiredStackVersion().getStackId(),
        getClusterHealthReport(hosts));
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("Cluster={ clusterName=").append(getClusterName()).append(", clusterId=").append(
        getClusterId()).append(", desiredStackVersion=").append(
            desiredStackVersion.getStackId()).append(", services=[ ");
    boolean first = true;
    for (Service s : services.values()) {
      if (!first) {
        sb.append(" , ");
      }
      first = false;
      sb.append("\n    ");
      s.debugDump(sb);
      sb.append(' ');
    }
    sb.append(" ] }");
    lockFactory.debugDump(sb);
  }

  @Override
  @Transactional
  public void refresh() {
    clusterGlobalLock.writeLock().lock();
    try {
      ClusterEntity clusterEntity = getClusterEntity();
      clusterDAO.refresh(clusterEntity);
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  @Transactional
  public void deleteAllServices() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      LOG.info("Deleting all services for cluster" + ", clusterName="
        + getClusterName());
      for (Service service : services.values()) {
        if (!service.canBeRemoved()) {
          throw new AmbariException(
              "Found non removable service when trying to"
                  + " all services from cluster" + ", clusterName="
                  + getClusterName() + ", serviceName=" + service.getName());
        }
      }

      DeleteHostComponentStatusMetaData deleteMetaData = new DeleteHostComponentStatusMetaData();
      for (Service service : services.values()) {
        deleteService(service, deleteMetaData);
        STOMPComponentsDeleteHandler.processDeleteByMetaDataException(deleteMetaData);
      }
      STOMPComponentsDeleteHandler.processDeleteCluster(getClusterId());
      services.clear();
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteService(String serviceName, DeleteHostComponentStatusMetaData deleteMetaData)
    throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      Service service = getService(serviceName);
      LOG.info("Deleting service for cluster" + ", clusterName="
          + getClusterName() + ", serviceName=" + service.getName());
      // FIXME check dependencies from meta layer
      if (!service.canBeRemoved()) {
        deleteMetaData.setAmbariException(new AmbariException("Could not delete service from cluster"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + service.getName()));
        return;
      }
      deleteService(service, deleteMetaData);
      services.remove(serviceName);

    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  /**
   * Deletes the specified service also removes references to it from {@link this.serviceComponentHosts}
   * and references to ServiceComponentHost objects that belong to the service from {@link this.serviceComponentHostsByHost}
   * <p>
   *   Note: This method must be called only with write lock acquired.
   * </p>
   * @param service the service to be deleted
   * @throws AmbariException
   * @see   ServiceComponentHost
   */
  private void deleteService(Service service, DeleteHostComponentStatusMetaData deleteMetaData) {
    final String serviceName = service.getName();

    service.delete(deleteMetaData);
    if (deleteMetaData.getAmbariException() != null) {
      return;
    }

    serviceComponentHosts.remove(serviceName);

    for (List<ServiceComponentHost> serviceComponents: serviceComponentHostsByHost.values()){
      Iterables.removeIf(serviceComponents, new Predicate<ServiceComponentHost>() {
        @Override
        public boolean apply(ServiceComponentHost serviceComponentHost) {
          return serviceComponentHost.getServiceName().equals(serviceName);
        }
      });
    }
  }

  @Override
  public boolean canBeRemoved() {
    clusterGlobalLock.readLock().lock();
    try {
      boolean safeToRemove = true;
      for (Service service : services.values()) {
        if (!service.canBeRemoved()) {
          safeToRemove = false;
          LOG.warn("Found non removable service" + ", clusterName="
              + getClusterName() + ", serviceName=" + service.getName());
        }
      }
      return safeToRemove;
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      refresh();
      deleteAllServices();
      resetHostVersions();

      refresh(); // update one-to-many clusterServiceEntities
      removeEntities();
      allConfigs.clear();
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    long clusterId = getClusterId();
    alertDefinitionDAO.removeAll(clusterId);
    alertDispatchDAO.removeAllGroups(clusterId);
    upgradeDAO.removeAll(clusterId);
    topologyRequestDAO.removeAll(clusterId);
    clusterDAO.removeByPK(clusterId);
  }

  private void resetHostVersions() {
    for (HostVersionEntity hostVersionEntity : hostVersionDAO.findByCluster(getClusterName())) {
      if (!hostVersionEntity.getState().equals(RepositoryVersionState.NOT_REQUIRED)) {
        hostVersionEntity.setState(RepositoryVersionState.NOT_REQUIRED);
        hostVersionDAO.merge(hostVersionEntity);
      }
    }
  }

  @Override
  public ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs) throws AmbariException {
    return addDesiredConfig(user, configs, null);
  }

  @Override
  public ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs, String serviceConfigVersionNote) throws AmbariException {
    if (null == user) {
      throw new NullPointerException("User must be specified.");
    }

    clusterGlobalLock.writeLock().lock();
    try {
      if (configs == null) {
        return null;
      }

      Iterator<Config> configIterator = configs.iterator();

      while (configIterator.hasNext()) {
        Config config = configIterator.next();
        if (config == null) {
          configIterator.remove();
          continue;
        }
        Config currentDesired = getDesiredConfigByType(config.getType());

        // do not set if it is already the current
        if (null != currentDesired
            && currentDesired.getTag().equals(config.getTag())) {
          configIterator.remove();
        }
      }

      ServiceConfigVersionResponse serviceConfigVersionResponse = applyConfigs(
          configs, user, serviceConfigVersionNote);

      return serviceConfigVersionResponse;
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  /**
   * Gets all versions of the desired configurations for the cluster.
   * @return a map of type-to-configuration information.
   */
  @Override
  public Map<String, Set<DesiredConfig>> getAllDesiredConfigVersions() {
    return getDesiredConfigs(true, true);
  }


  @Override
  public Map<String, DesiredConfig> getDesiredConfigs() {
    return getDesiredConfigs(true);
  }

  @Override
  public Map<String, DesiredConfig> getDesiredConfigs(boolean cachedConfigEntities) {
    Map<String, Set<DesiredConfig>> activeConfigsByType = getDesiredConfigs(false, cachedConfigEntities);
    return Maps.transformEntries(
        activeConfigsByType,
        new Maps.EntryTransformer<String, Set<DesiredConfig>, DesiredConfig>() {
          @Override
          public DesiredConfig transformEntry(@Nullable String key, @Nullable Set<DesiredConfig> value) {
            return value.iterator().next();
          }
        });
  }

  /**
   * Gets desired configurations for the cluster.
   * @param allVersions specifies if all versions of the desired configurations to be returned
   *                    or only the active ones. It is expected that there is one and only one active
   *                    desired configuration per config type.
   * @param cachedConfigEntities retrieves cluster config entities from the cache if true, otherwise from the DB directly.
   * @return a map of type-to-configuration information.
   */
  private Map<String, Set<DesiredConfig>> getDesiredConfigs(boolean allVersions, boolean cachedConfigEntities) {
    clusterGlobalLock.readLock().lock();
    try {
      Map<String, Set<DesiredConfig>> map = new HashMap<>();
      Collection<String> types = new HashSet<>();
      Collection<ClusterConfigEntity> entities;
      if (cachedConfigEntities) {
        entities = getClusterEntity().getClusterConfigEntities();
      } else {
        entities = clusterDAO.getEnabledConfigs(clusterId);
      }
      for (ClusterConfigEntity configEntity : entities) {
        if (allVersions || configEntity.isSelected()) {
          DesiredConfig desiredConfig = new DesiredConfig();
          desiredConfig.setServiceName(null);
          desiredConfig.setTag(configEntity.getTag());

          if (!allConfigs.containsKey(configEntity.getType())) {
            LOG.error("An inconsistency exists for configuration {}", configEntity.getType());
            continue;
          }

          Map<String, Config> configMap = allConfigs.get(configEntity.getType());
          if(!configMap.containsKey(configEntity.getTag())) {
            LOG.error("An inconsistency exists for the configuration {} with tag {}",
                configEntity.getType(), configEntity.getTag());

            continue;
          }

          Config config = configMap.get(configEntity.getTag());
          desiredConfig.setVersion(config.getVersion());

          Set<DesiredConfig> configs = map.get(configEntity.getType());
          if (configs == null) {
            configs = new HashSet<>();
          }

          configs.add(desiredConfig);

          map.put(configEntity.getType(), configs);
          types.add(configEntity.getType());
        }
      }

      // TODO AMBARI-10679, need efficient caching from hostId to hostName...
      Map<Long, String> hostIdToName = new HashMap<>();

      if (!map.isEmpty()) {
        Map<String, List<HostConfigMapping>> hostMappingsByType =
          hostConfigMappingDAO.findSelectedHostsByTypes(clusterId, types);

        for (Entry<String, Set<DesiredConfig>> entry : map.entrySet()) {
          List<DesiredConfig.HostOverride> hostOverrides = new ArrayList<>();
          for (HostConfigMapping mappingEntity : hostMappingsByType.get(entry.getKey())) {

            if (!hostIdToName.containsKey(mappingEntity.getHostId())) {
              HostEntity hostEntity = hostDAO.findById(mappingEntity.getHostId());
              hostIdToName.put(mappingEntity.getHostId(), hostEntity.getHostName());
            }

            hostOverrides.add(new DesiredConfig.HostOverride(
                hostIdToName.get(mappingEntity.getHostId()), mappingEntity.getVersion()));
          }

          for (DesiredConfig c: entry.getValue()) {
            c.setHostOverrides(hostOverrides);
          }
        }
      }

      return map;
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }



  @Override
  public ServiceConfigVersionResponse createServiceConfigVersion(
      String serviceName, String user, String note, ConfigGroup configGroup) throws AmbariException {

    // Create next service config version
    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();

    clusterGlobalLock.writeLock().lock();
    try {
      ClusterEntity clusterEntity = getClusterEntity();
      // set config group
      if (configGroup != null) {
        serviceConfigEntity.setGroupId(configGroup.getId());
        Collection<Config> configs = configGroup.getConfigurations().values();
        List<ClusterConfigEntity> configEntities = new ArrayList<>(
            configs.size());
        for (Config config : configs) {
          configEntities.add(
              clusterDAO.findConfig(getClusterId(), config.getType(), config.getTag()));
        }

        serviceConfigEntity.setClusterConfigEntities(configEntities);
      } else {
        List<ClusterConfigEntity> configEntities = getClusterConfigEntitiesByService(serviceName);
        serviceConfigEntity.setClusterConfigEntities(configEntities);
      }
      Map<String, Collection<String>> changedConfigs = configHelper.getChangedConfigTypes(this, serviceConfigEntity,
          configGroup == null ? null : configGroup.getId(),
          clusterId,
          serviceName);

      long nextServiceConfigVersion = serviceConfigDAO.findNextServiceConfigVersion(clusterId,
          serviceName);

      // get the correct stack ID to use when creating the service config
      StackEntity stackEntity = clusterEntity.getDesiredStack();
      Service service = services.get(serviceName);
      if (null != service) {
        StackId serviceStackId = service.getDesiredStackId();
        stackEntity = stackDAO.find(serviceStackId);
      }

      serviceConfigEntity.setServiceName(serviceName);
      serviceConfigEntity.setClusterEntity(clusterEntity);
      serviceConfigEntity.setVersion(nextServiceConfigVersion);
      serviceConfigEntity.setUser(user);
      serviceConfigEntity.setNote(note);
      serviceConfigEntity.setStack(stackEntity);

      serviceConfigDAO.create(serviceConfigEntity);
      List<String> groupHostNames = null;
      if (configGroup != null) {
        if (MapUtils.isNotEmpty(configGroup.getHosts())) {
          groupHostNames = configGroup.getHosts().entrySet().stream().map(h -> h.getValue().getHostName())
              .collect(Collectors.toList());
        }
        serviceConfigEntity.setHostIds(new ArrayList<>(configGroup.getHosts().keySet()));
        serviceConfigEntity = serviceConfigDAO.merge(serviceConfigEntity);
      }
      STOMPUpdatePublisher.publish(new ConfigsUpdateEvent(serviceConfigEntity,
          configGroup == null ? null : configGroup.getName(), groupHostNames, changedConfigs.keySet()));
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

    String configGroupName = configGroup == null ? ServiceConfigVersionResponse.DEFAULT_CONFIG_GROUP_NAME : configGroup.getName();
    configChangeLog.info("(configchange) Creating config version. cluster: '{}', changed by: '{}', " +
            "service_name: '{}', config_group: '{}', config_group_id: '{}', version: '{}', create_timestamp: '{}', note: '{}'",
        getClusterName(), user, serviceName, configGroupName,
        configGroup == null ? "null" : configGroup.getId(), serviceConfigEntity.getVersion(), serviceConfigEntity.getCreateTimestamp(),
        serviceConfigEntity.getNote());

    ServiceConfigVersionResponse response = new ServiceConfigVersionResponse(
        serviceConfigEntity, configGroupName);

    return response;
  }

  @Override
  public String getServiceForConfigTypes(Collection<String> configTypes) {
    List<String> serviceNames = configTypes.stream()
      .map(this::getServiceByConfigType)
      .filter(Objects::nonNull)
      .collect(toList());
    boolean allTheSame = new HashSet<>(serviceNames).size() <= 1;
    if (!allTheSame) {
      throw new IllegalArgumentException(String.format(
        "Config types: %s should belong to a single installed service. But they belong to: %s", configTypes, serviceNames));
    }
    return serviceNames.isEmpty() ? null : serviceNames.get(0);
  }

  public List<String> serviceNameByConfigType(String configType) {
    return serviceConfigTypes.entries().stream()
      .filter(entry -> StringUtils.equals(entry.getValue(), configType))
      .map(entry -> entry.getKey())
      .collect(toList());
  }

  @Override
  public String getServiceByConfigType(String configType) {
    return serviceNameByConfigType(configType).stream()
      .filter(this::isServiceInstalled)
      .findFirst()
      .orElse(null);
  }

  private boolean isServiceInstalled(String serviceName) {
    return services.get(serviceName) != null;
  }

  @Override
  public ServiceConfigVersionResponse setServiceConfigVersion(String serviceName, Long version, String user, String note) throws AmbariException {
    if (null == user) {
      throw new NullPointerException("User must be specified.");
    }

    clusterGlobalLock.writeLock().lock();
    try {
      ServiceConfigVersionResponse serviceConfigVersionResponse = applyServiceConfigVersion(
          serviceName, version, user, note);
      return serviceConfigVersionResponse;
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public Map<String, Collection<ServiceConfigVersionResponse>> getActiveServiceConfigVersions() {
    clusterGlobalLock.readLock().lock();
    try {
      Map<String, Collection<ServiceConfigVersionResponse>> map = new HashMap<>();

      Set<ServiceConfigVersionResponse> responses = getActiveServiceConfigVersionSet();
      for (ServiceConfigVersionResponse response : responses) {
        if (map.get(response.getServiceName()) == null) {
          map.put(response.getServiceName(), new ArrayList<>());
        }
        map.get(response.getServiceName()).add(response);
      }
      return map;
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public List<ServiceConfigVersionResponse> getServiceConfigVersions() {
    clusterGlobalLock.readLock().lock();
    try {
      List<ServiceConfigVersionResponse> serviceConfigVersionResponses = new ArrayList<>();

      List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getServiceConfigs(getClusterId());

      // Gather for each service in each config group the active service config response  as we
      // iterate through all service config responses
      Map<String, Map<String, ServiceConfigVersionResponse>> activeServiceConfigResponses = new HashMap<>();

      for (ServiceConfigEntity serviceConfigEntity : serviceConfigs) {
        ServiceConfigVersionResponse serviceConfigVersionResponse = convertToServiceConfigVersionResponse(serviceConfigEntity);

        Map<String, ServiceConfigVersionResponse> activeServiceConfigResponseGroups = activeServiceConfigResponses.get(serviceConfigVersionResponse.getServiceName());

        if (activeServiceConfigResponseGroups == null) {
          Map<String, ServiceConfigVersionResponse> serviceConfigGroups = new HashMap<>();
          activeServiceConfigResponses.put(serviceConfigVersionResponse.getServiceName(), serviceConfigGroups);

          activeServiceConfigResponseGroups = serviceConfigGroups;
        }

        // the active config within a group
        ServiceConfigVersionResponse activeServiceConfigResponse = activeServiceConfigResponseGroups.get(serviceConfigVersionResponse.getGroupName());

        if (activeServiceConfigResponse == null && !ServiceConfigVersionResponse.DELETED_CONFIG_GROUP_NAME.equals(serviceConfigVersionResponse.getGroupName())) {
          // service config version with deleted group should always be marked is not current
          activeServiceConfigResponseGroups.put(serviceConfigVersionResponse.getGroupName(), serviceConfigVersionResponse);
          activeServiceConfigResponse = serviceConfigVersionResponse;
        }
        if (serviceConfigEntity.getGroupId() == null) {
          if (serviceConfigVersionResponse.getCreateTime() > activeServiceConfigResponse.getCreateTime()) {
            activeServiceConfigResponseGroups.put(serviceConfigVersionResponse.getGroupName(), serviceConfigVersionResponse);
          }
        }
        else if (clusterConfigGroups != null && clusterConfigGroups.containsKey(serviceConfigEntity.getGroupId())){
          if (serviceConfigVersionResponse.getVersion() > activeServiceConfigResponse.getVersion()) {
            activeServiceConfigResponseGroups.put(serviceConfigVersionResponse.getGroupName(), serviceConfigVersionResponse);
          }
        }

        serviceConfigVersionResponse.setIsCurrent(false);
        serviceConfigVersionResponses.add(getServiceConfigVersionResponseWithConfig(serviceConfigVersionResponse, serviceConfigEntity));
      }

      for (Map<String, ServiceConfigVersionResponse> serviceConfigVersionResponseGroup: activeServiceConfigResponses.values()) {
        for (ServiceConfigVersionResponse serviceConfigVersionResponse : serviceConfigVersionResponseGroup.values()) {
          serviceConfigVersionResponse.setIsCurrent(true);
        }
      }

      return serviceConfigVersionResponses;
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  private Set<ServiceConfigVersionResponse> getActiveServiceConfigVersionSet() {
    Set<ServiceConfigVersionResponse> responses = new HashSet<>();
    List<ServiceConfigEntity> activeServiceConfigVersions = getActiveServiceConfigVersionEntities();

    for (ServiceConfigEntity lastServiceConfig : activeServiceConfigVersions) {
      ServiceConfigVersionResponse response = convertToServiceConfigVersionResponse(lastServiceConfig);
      response.setIsCurrent(true); //mark these as current, as they are
      responses.add(response);
    }
    return responses;
  }

  private List<ServiceConfigEntity> getActiveServiceConfigVersionEntities() {

    List<ServiceConfigEntity> activeServiceConfigVersions = new ArrayList<>();
    //for services
    activeServiceConfigVersions.addAll(serviceConfigDAO.getLastServiceConfigs(getClusterId()));
    //for config groups
    if (clusterConfigGroups != null) {
      activeServiceConfigVersions.addAll(
        serviceConfigDAO.getLastServiceConfigVersionsForGroups(clusterConfigGroups.keySet()));
    }

    return activeServiceConfigVersions;
  }

  @Override
  public List<ServiceConfigVersionResponse> getActiveServiceConfigVersionResponse(String serviceName) {
    clusterGlobalLock.readLock().lock();
    try {
      List<ServiceConfigEntity> activeServiceConfigVersionEntities = new ArrayList<>();
      List<ServiceConfigVersionResponse> activeServiceConfigVersionResponses = new ArrayList<>();
      activeServiceConfigVersionEntities.addAll(serviceConfigDAO.getLastServiceConfigsForService(getClusterId(), serviceName));
      for (ServiceConfigEntity serviceConfigEntity : activeServiceConfigVersionEntities) {
        ServiceConfigVersionResponse serviceConfigVersionResponse = getServiceConfigVersionResponseWithConfig(convertToServiceConfigVersionResponse(serviceConfigEntity), serviceConfigEntity);
        serviceConfigVersionResponse.setIsCurrent(true);
        activeServiceConfigVersionResponses.add(serviceConfigVersionResponse);
      }
      return activeServiceConfigVersionResponses;
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  /**
   * Adds Configuration data to the serviceConfigVersionResponse
   * @param serviceConfigVersionResponse
   * @param serviceConfigEntity
   * @return serviceConfigVersionResponse
   */
  private ServiceConfigVersionResponse getServiceConfigVersionResponseWithConfig(ServiceConfigVersionResponse serviceConfigVersionResponse, ServiceConfigEntity serviceConfigEntity) {
    serviceConfigVersionResponse.setConfigurations(new ArrayList<>());
    List<ClusterConfigEntity> clusterConfigEntities = serviceConfigEntity.getClusterConfigEntities();
    for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
      Config config = allConfigs.get(clusterConfigEntity.getType()).get(
          clusterConfigEntity.getTag());

      serviceConfigVersionResponse.getConfigurations().add(
          new ConfigurationResponse(getClusterName(), config));
    }
    return serviceConfigVersionResponse;
  }


  @RequiresSession
  ServiceConfigVersionResponse getActiveServiceConfigVersion(String serviceName) {
    ServiceConfigEntity lastServiceConfig = serviceConfigDAO.getLastServiceConfig(getClusterId(), serviceName);
    if (lastServiceConfig == null) {
      LOG.debug("No service config version found for service {}", serviceName);
      return null;
    }
    return convertToServiceConfigVersionResponse(lastServiceConfig);
  }

  @RequiresSession
  ServiceConfigVersionResponse convertToServiceConfigVersionResponse(ServiceConfigEntity serviceConfigEntity) {
    Long groupId = serviceConfigEntity.getGroupId();

    String groupName;
    if (groupId != null) {
      ConfigGroup configGroup = null;
      if (clusterConfigGroups != null) {
        configGroup = clusterConfigGroups.get(groupId);
      }

      if (configGroup != null) {
        groupName = configGroup.getName();
      } else {
        groupName = ServiceConfigVersionResponse.DELETED_CONFIG_GROUP_NAME;
      }
    } else {
      groupName = ServiceConfigVersionResponse.DEFAULT_CONFIG_GROUP_NAME;
    }

    ServiceConfigVersionResponse serviceConfigVersionResponse = new ServiceConfigVersionResponse(
        serviceConfigEntity, groupName);

    return serviceConfigVersionResponse;
  }

  @Transactional
  ServiceConfigVersionResponse applyServiceConfigVersion(String serviceName, Long serviceConfigVersion, String user,
                                 String serviceConfigVersionNote) throws AmbariException {
    ServiceConfigEntity serviceConfigEntity = serviceConfigDAO.findByServiceAndVersion(serviceName, serviceConfigVersion);
    if (serviceConfigEntity == null) {
      throw new ObjectNotFoundException("Service config version with serviceName={} and version={} not found");
    }

    String configGroupName = null;
    // disable all configs related to service
    if (serviceConfigEntity.getGroupId() == null) {
      // Here was fixed bug with entity changes revert. More you can find here AMBARI-21173.
      // This issue reproduces only if you are changing same entity in first and second loop.
      // In that case eclipselink will revert changes to cached, if entity has fluchGroup and it
      // needs to be refreshed. Actually we don't need to change same antities in few steps, so i
      // decided to filter out. duplicates and do not change them. It will be better for performance and bug will be fixed.
      Collection<String> configTypes = serviceConfigTypes.get(serviceName);
      List<ClusterConfigEntity> enabledConfigs = clusterDAO.getEnabledConfigsByTypes(clusterId, configTypes);
      List<ClusterConfigEntity> serviceConfigEntities = serviceConfigEntity.getClusterConfigEntities();
      ArrayList<ClusterConfigEntity> duplicatevalues = new ArrayList<>(serviceConfigEntities);
      duplicatevalues.retainAll(enabledConfigs);

      for (ClusterConfigEntity enabledConfig : enabledConfigs) {
        if (!duplicatevalues.contains(enabledConfig)) {
          enabledConfig.setSelected(false);
          clusterDAO.merge(enabledConfig);
        }
      }

      for (ClusterConfigEntity configEntity : serviceConfigEntities) {
        if (!duplicatevalues.contains(configEntity)) {
          configEntity.setSelected(true);
          clusterDAO.merge(configEntity);
        }
      }
    } else {
      Long configGroupId = serviceConfigEntity.getGroupId();
      ConfigGroup configGroup = clusterConfigGroups.get(configGroupId);
      if (configGroup != null) {
        configGroupName = configGroup.getName();
        Map<String, Config> groupDesiredConfigs = new HashMap<>();
        for (ClusterConfigEntity entity : serviceConfigEntity.getClusterConfigEntities()) {
          Config config = allConfigs.get(entity.getType()).get(entity.getTag());
          groupDesiredConfigs.put(config.getType(), config);
        }
        configGroup.setConfigurations(groupDesiredConfigs);

        Map<Long, Host> groupDesiredHosts = new HashMap<>();
        if (serviceConfigEntity.getHostIds() != null) {
          for (Long hostId : serviceConfigEntity.getHostIds()) {
            Host host = clusters.getHostById(hostId);
            if (host != null) {
              groupDesiredHosts.put(hostId, host);
            } else {
              LOG.warn("Host with id {} doesn't exist anymore, skipping", hostId);
            }
          }
        }
        configGroup.setHosts(groupDesiredHosts);
      } else {
        throw new IllegalArgumentException("Config group {} doesn't exist");
      }
    }
    Map<String, Collection<String>> changedConfigs = configHelper.getChangedConfigTypes(this, serviceConfigEntity,
        serviceConfigEntity.getGroupId(), clusterId, serviceName);

    ClusterEntity clusterEntity = getClusterEntity();
    long nextServiceConfigVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterEntity.getClusterId(), serviceName);

    ServiceConfigEntity serviceConfigEntityClone = new ServiceConfigEntity();
    serviceConfigEntityClone.setCreateTimestamp(System.currentTimeMillis());
    serviceConfigEntityClone.setUser(user);
    serviceConfigEntityClone.setServiceName(serviceName);
    serviceConfigEntityClone.setClusterEntity(clusterEntity);
    serviceConfigEntityClone.setStack(serviceConfigEntity.getStack());
    serviceConfigEntityClone.setClusterConfigEntities(serviceConfigEntity.getClusterConfigEntities());
    serviceConfigEntityClone.setClusterId(serviceConfigEntity.getClusterId());
    serviceConfigEntityClone.setHostIds(serviceConfigEntity.getHostIds());
    serviceConfigEntityClone.setGroupId(serviceConfigEntity.getGroupId());
    serviceConfigEntityClone.setNote(serviceConfigVersionNote);
    serviceConfigEntityClone.setVersion(nextServiceConfigVersion);

    List<String> groupHostNames = null;
    if (CollectionUtils.isNotEmpty(serviceConfigEntity.getHostIds())) {
      groupHostNames = getHosts().stream()
          .filter(h -> serviceConfigEntity.getHostIds().contains(h.getHostId()))
          .map(h -> h.getHostName()).collect(Collectors.toList());
    }

    serviceConfigDAO.create(serviceConfigEntityClone);
    STOMPUpdatePublisher.publish(new ConfigsUpdateEvent(serviceConfigEntityClone,
        configGroupName,
        groupHostNames,
        changedConfigs.keySet()));

    return convertToServiceConfigVersionResponse(serviceConfigEntityClone);
  }

  @Transactional
  ServiceConfigVersionResponse applyConfigs(Set<Config> configs, String user, String serviceConfigVersionNote) throws AmbariException{

    List<ClusterConfigEntity> appliedConfigs = new ArrayList<>();
    String serviceName = getServiceForConfigTypes(configs.stream().map(Config::getType).collect(toList()));
    // update the selected flag for every config type
    ClusterEntity clusterEntity = getClusterEntity();
    Collection<ClusterConfigEntity> clusterConfigs = clusterEntity.getClusterConfigEntities();
    for (Config config: configs) {
      for (ClusterConfigEntity clusterConfigEntity : clusterConfigs) {
        // unset for this config type
        if (StringUtils.equals(clusterConfigEntity.getType(), config.getType())) {
          clusterConfigEntity.setSelected(false);

          // unless both the tag and type match, then enable it
          if (StringUtils.equals(clusterConfigEntity.getTag(), config.getTag())) {
            appliedConfigs.add(clusterConfigEntity);
            clusterConfigEntity.setSelected(true);
          }
        }
      }
    }

    clusterEntity = clusterDAO.merge(clusterEntity);

    if (serviceName == null) {
      ArrayList<String> configTypes = new ArrayList<>();
      for (Config config: configs) {
        configTypes.add(config.getType());
      }

      STOMPUpdatePublisher.publish(new ConfigsUpdateEvent(this, appliedConfigs));
      LOG.error("No service found for config types '{}', service config version not created", configTypes);
      return null;
    } else {
      return createServiceConfigVersion(serviceName, user, serviceConfigVersionNote);
    }

  }

  private ServiceConfigVersionResponse createServiceConfigVersion(String serviceName, String user,
                                                                  String serviceConfigVersionNote) throws AmbariException {
    //create next service config version
    return createServiceConfigVersion(serviceName, user, serviceConfigVersionNote, null);
  }

  private List<ClusterConfigEntity> getClusterConfigEntitiesByService(String serviceName) {
    Collection<String> configTypes = serviceConfigTypes.get(serviceName);
    return clusterDAO.getEnabledConfigsByTypes(getClusterId(), new ArrayList<>(configTypes));
  }

  @Override
  public Config getDesiredConfigByType(String configType) {
    ClusterConfigEntity config = clusterDAO.findEnabledConfigByType(getClusterId(), configType);
    if (null == config) {
      return null;
    }

    return getConfig(configType, config.getTag());
  }

  @Override
  public boolean isConfigTypeExists(String configType) {
    ClusterConfigEntity config = clusterDAO.findEnabledConfigByType(getClusterId(), configType);
    return null != config;
  }

  @Override
  public Map<Long, Map<String, DesiredConfig>> getHostsDesiredConfigs(Collection<Long> hostIds) {

    if (hostIds == null || hostIds.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<HostConfigMapping> mappingEntities =
        hostConfigMappingDAO.findSelectedByHosts(hostIds);

    Map<Long, Map<String, DesiredConfig>> desiredConfigsByHost = new HashMap<>();

    for (Long hostId : hostIds) {
      desiredConfigsByHost.put(hostId, new HashMap<>());
    }

    for (HostConfigMapping mappingEntity : mappingEntities) {
      DesiredConfig desiredConfig = new DesiredConfig();
      desiredConfig.setTag(mappingEntity.getVersion());
      desiredConfig.setServiceName(mappingEntity.getServiceName());

      desiredConfigsByHost.get(mappingEntity.getHostId()).put(mappingEntity.getType(), desiredConfig);
    }

    return desiredConfigsByHost;
  }

  @Override
  public Map<Long, Map<String, DesiredConfig>> getAllHostsDesiredConfigs() {

    Collection<Long> hostIds;
    try {
      hostIds = clusters.getHostIdsForCluster(clusterName).keySet();
    } catch (AmbariException ignored) {
      return Collections.emptyMap();
    }

    return getHostsDesiredConfigs(hostIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getNextConfigVersion(String type) {
    return clusterDAO.findNextConfigVersion(clusterId, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<ServiceComponentHostEvent, String> processServiceComponentHostEvents(ListMultimap<String, ServiceComponentHostEvent> eventMap) {
    clusterGlobalLock.readLock().lock();

    try {
      return processServiceComponentHostEventsInSingleTransaction(eventMap);
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  /**
   * Bulk handle service component host events, wrapping all handling in a
   * single transaction. This allows
   * {@link #processServiceComponentHostEvents(ListMultimap)} to lock around the
   * AoP {@link Transactional} annotation so that the lock is not released
   * before the transaction is committed.
   *
   * @param eventMap
   * @return the events which failed to be processed.
   */
  @Transactional
  protected Map<ServiceComponentHostEvent, String> processServiceComponentHostEventsInSingleTransaction(
      ListMultimap<String, ServiceComponentHostEvent> eventMap) {
    Map<ServiceComponentHostEvent, String> failedEvents = new HashMap<>();

    for (Entry<String, ServiceComponentHostEvent> entry : eventMap.entries()) {
      String serviceName = entry.getKey();
      ServiceComponentHostEvent event = entry.getValue();
      String serviceComponentName = event.getServiceComponentName();

      // server-side events either don't have a service name or are AMBARI;
      // either way they are not handled by this method since it expects a
      // real service and component
      if (StringUtils.isBlank(serviceName) || RootService.AMBARI.name().equals(serviceName)) {
        continue;
      }

      if (StringUtils.isBlank(serviceComponentName)) {
        continue;
      }

      try {
        Service service = getService(serviceName);
        ServiceComponent serviceComponent = service.getServiceComponent(serviceComponentName);
        ServiceComponentHost serviceComponentHost = serviceComponent.getServiceComponentHost(
            event.getHostName());
        serviceComponentHost.handleEvent(event);
      } catch (ServiceNotFoundException e) {
        String message = String.format(
            "ServiceComponentHost lookup exception. Service not found for Service: %s. Error: %s",
            serviceName, e.getMessage());
        LOG.error(message);
        failedEvents.put(event, message);
      } catch (ServiceComponentNotFoundException e) {
        String message = String.format(
            "ServiceComponentHost lookup exception. Service Component not found for Service: %s, Component: %s. Error: %s",
            serviceName, serviceComponentName, e.getMessage());
        LOG.error(message);
        failedEvents.put(event, message);
      } catch (ServiceComponentHostNotFoundException e) {
        String message = String.format(
            "ServiceComponentHost lookup exception. Service Component Host not found for Service: %s, Component: %s, Host: %s. Error: %s",
            serviceName, serviceComponentName, event.getHostName(), e.getMessage());
        LOG.error(message);
        failedEvents.put(event, message);
      } catch (AmbariException e) {
        String message = String.format("ServiceComponentHost lookup exception %s", e.getMessage());
        LOG.error(message);
        failedEvents.put(event, message);
      } catch (InvalidStateTransitionException e) {
        LOG.error("Invalid transition ", e);

        boolean isFailure = true;

        Enum<?> currentState = e.getCurrentState();
        Enum<?> failedEvent = e.getEvent();

        // skip adding this as a failed event, to work around stack ordering
        // issues with Hive
        if (currentState == State.STARTED &&
            failedEvent == ServiceComponentHostEventType.HOST_SVCCOMP_START){
          isFailure = false;
          LOG.warn(
              "The start request for {} is invalid since the component is already started. Ignoring the request.",
              serviceComponentName);
        }

        // unknown hosts should be able to be put back in progress and let the
        // action scheduler fail it; don't abort the entire stage just because
        // this happens
        if (currentState == State.UNKNOWN
            && failedEvent == ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS) {
          isFailure = false;
          LOG.warn("The host {} is in an unknown state; attempting to put {} back in progress.",
              event.getHostName(),
              serviceComponentName);
        }

        // fail the event, causing it to automatically abort
        if (isFailure) {
          failedEvents.put(event, String.format("Invalid transition. %s", e.getMessage()));
        }
      }
    }

    return failedEvents;
  }

  /**
   * @param serviceName name of the service
   * @param componentName name of the component
   * @return the set of hosts for the provided service and component
   */
  @Override
  public Set<String> getHosts(String serviceName, String componentName) {
    Map<String, Service> clusterServices = getServices();

    if (!clusterServices.containsKey(serviceName)) {
      return Collections.emptySet();
    }

    Service service = clusterServices.get(serviceName);
    Map<String, ServiceComponent> components = service.getServiceComponents();

    if (!components.containsKey(componentName) ||
            components.get(componentName).getServiceComponentHosts().size() == 0) {
      return Collections.emptySet();
    }

    return components.get(componentName).getServiceComponentHosts().keySet();
  }

  @Override
  public Host getHost(final String hostName) {
    if (StringUtils.isEmpty(hostName)) {
      return null;
    }

    Collection<Host> hosts = getHosts();
    if(hosts != null) {
      for (Host host : hosts) {
        String hostString = host.getHostName();
        if(hostName.equalsIgnoreCase(hostString)) {
          return host;
        }
      }
    }
    return null;
  }

  @Override
  public Host getHost(final Long hostId) {
    if (hostId == null) {
      return null;
    }

    Collection<Host> hosts = getHosts();
    if(hosts != null) {
      for (Host host : hosts) {
        if(hostId.equals(host.getHostId())) {
          return host;
        }
      }
    }
    return null;
  }

  @Override
  public Collection<Host> getHosts() {
    return clusters.getHostsForCluster(clusterName).values();
  }

  private ClusterHealthReport getClusterHealthReport(
      Map<String, Host> clusterHosts) throws AmbariException {

    int staleConfigsHosts = 0;
    int maintenanceStateHosts = 0;

    int healthyStateHosts = 0;
    int unhealthyStateHosts = 0;
    int initStateHosts = 0;
    int healthyStatusHosts = 0;

    int unhealthyStatusHosts = 0;
    int unknownStatusHosts = 0;
    int alertStatusHosts = 0;
    int heartbeatLostStateHosts = 0;

    // look this up once so it can be reused in the loop for every SCH
    Map<String, DesiredConfig> desiredConfigs = getDesiredConfigs();

    Collection<Host> hosts = clusterHosts.values();
    Iterator<Host> iterator = hosts.iterator();
    //TODO to version in sch
    List<Long> hostIds = hosts.stream().map(Host::getHostId).collect(Collectors.toList());
    List<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities =
        hostIds.isEmpty() ? Collections.EMPTY_LIST : hostComponentDesiredStateDAO.findByHostsAndCluster(hostIds, clusterId);
    Map<Long, Map<String, HostComponentDesiredStateEntity>> mappedHostIds = hostComponentDesiredStateEntities.stream().collect(
        Collectors.groupingBy(HostComponentDesiredStateEntity::getHostId,
            Collectors.toMap(HostComponentDesiredStateEntity::getComponentName, Function.identity())
        )
    );
    while (iterator.hasNext()) {
      Host host = iterator.next();
      String hostName = host.getHostName();

      switch (host.getState()) {
        case HEALTHY:
          healthyStateHosts++;
          break;
        case UNHEALTHY:
          unhealthyStateHosts++;
          break;
        case INIT:
          initStateHosts++;
          break;
        case HEARTBEAT_LOST:
          heartbeatLostStateHosts++;
          break;
      }

      switch (HostHealthStatus.HealthStatus.valueOf(host.getStatus())) {
        case HEALTHY:
          healthyStatusHosts++;
          break;
        case UNHEALTHY:
          unhealthyStatusHosts++;
          break;
        case UNKNOWN:
          unknownStatusHosts++;
          break;
        case ALERT:
          alertStatusHosts++;
          break;
      }

      boolean staleConfig = false;
      boolean maintenanceState = false;

      if (serviceComponentHostsByHost.containsKey(hostName)) {
        Map<String, HostComponentDesiredStateEntity> componentsStates = mappedHostIds.get(host.getHostId());
        for (ServiceComponentHost sch : serviceComponentHostsByHost.get(hostName)) {
          HostComponentDesiredStateEntity componentState = componentsStates == null ? null :
              componentsStates.get(sch.getServiceComponentName());
          if (componentState != null) {
            staleConfig = staleConfig || configHelper.isStaleConfigs(sch, desiredConfigs, componentState);
          } else {
            staleConfig = staleConfig || configHelper.isStaleConfigs(sch, desiredConfigs);
          }
          maintenanceState = maintenanceState ||
            maintenanceStateHelper.getEffectiveState(sch) != MaintenanceState.OFF;
        }
      }

      if (staleConfig) {
        staleConfigsHosts++;
      }
      if (maintenanceState) {
        maintenanceStateHosts++;
      }
    }

    ClusterHealthReport chr = new ClusterHealthReport();
    chr.setAlertStatusHosts(alertStatusHosts);
    chr.setHealthyStateHosts(healthyStateHosts);
    chr.setUnknownStatusHosts(unknownStatusHosts);
    chr.setUnhealthyStatusHosts(unhealthyStatusHosts);
    chr.setUnhealthyStateHosts(unhealthyStateHosts);
    chr.setStaleConfigsHosts(staleConfigsHosts);
    chr.setMaintenanceStateHosts(maintenanceStateHosts);
    chr.setInitStateHosts(initStateHosts);
    chr.setHeartbeatLostStateHosts(heartbeatLostStateHosts);
    chr.setHealthyStatusHosts(healthyStatusHosts);

    return chr;
  }

  @Override
  public boolean checkPermission(PrivilegeEntity privilegeEntity, boolean readOnly) {
    ClusterEntity clusterEntity = getClusterEntity();
    ResourceEntity resourceEntity = clusterEntity.getResource();
    if (resourceEntity != null) {
      Integer permissionId = privilegeEntity.getPermission().getId();
      // CLUSTER.USER or CLUSTER.ADMINISTRATOR for the given cluster resource.
      if (privilegeEntity.getResource().equals(resourceEntity)) {
        if ((readOnly && permissionId.equals(PermissionEntity.CLUSTER_USER_PERMISSION))
            || permissionId.equals(PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void addSessionAttributes(Map<String, Object> attributes) {
    if (attributes != null && !attributes.isEmpty()) {
      Map<String, Object>  sessionAttributes = new HashMap<>(getSessionAttributes());
      sessionAttributes.putAll(attributes);
      setSessionAttributes(attributes);
    }
  }

  @Override
  public void setSessionAttribute(String key, Object value){
    if (key != null && !key.isEmpty()) {
      Map<String, Object> sessionAttributes = new HashMap<>(getSessionAttributes());
      sessionAttributes.put(key, value);
      setSessionAttributes(sessionAttributes);
    }
  }

  @Override
  public void removeSessionAttribute(String key) {
    if (key != null && !key.isEmpty()) {
      Map<String, Object> sessionAttributes = new HashMap<>(getSessionAttributes());
      sessionAttributes.remove(key);
      setSessionAttributes(sessionAttributes);
    }
  }

  @Override
  public Map<String, Object> getSessionAttributes() {
    Map<String, Object>  attributes =
        (Map<String, Object>) getSessionManager().getAttribute(getClusterSessionAttributeName());

    return attributes == null ? Collections.emptyMap() : attributes;
  }

  /**
   * Get the associated session manager.
   *
   * @return the session manager
   */
  protected AmbariSessionManager getSessionManager() {
    return sessionManager;
  }

  /**
   * Set the map of session attributes for this cluster.
   * <p/>
   * This is a private method so that it may be used as a utility for add and update operations.
   *
   * @param sessionAttributes the map of session attributes for this cluster; never null
   */
  private void setSessionAttributes(Map<String, Object> sessionAttributes) {
    getSessionManager().setAttribute(getClusterSessionAttributeName(), sessionAttributes);
  }

  /**
   * Generates and returns the cluster-specific attribute name to use to set and get cluster-specific
   * session attributes.
   *
   * @return the name of the cluster-specific session attribute
   */
  private String getClusterSessionAttributeName() {
    return CLUSTER_SESSION_ATTRIBUTES_PREFIX + getClusterName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void applyLatestConfigurations(StackId stackId, String serviceName) {
    clusterGlobalLock.writeLock().lock();

    try {
      // grab all of the configurations and hash them so we can easily update them when picking and choosing only those from the service
      ClusterEntity clusterEntity = getClusterEntity();
      Collection<ClusterConfigEntity> configEntities = clusterEntity.getClusterConfigEntities();
      ImmutableMap<Object, ClusterConfigEntity> clusterConfigEntityMap = Maps.uniqueIndex(
          configEntities, Functions.identity());

      // find the latest configurations for the service
      Set<String> configTypesForService = new HashSet<>();
      List<ServiceConfigEntity> latestServiceConfigs = serviceConfigDAO.getLastServiceConfigsForService(
          getClusterId(), serviceName);

      // process the current service configurations
      for (ServiceConfigEntity serviceConfig : latestServiceConfigs) {
        List<ClusterConfigEntity> latestConfigs = serviceConfig.getClusterConfigEntities();
        for( ClusterConfigEntity latestConfig : latestConfigs ){
          // grab the hash'd entity from the map so we're working with the right one
          latestConfig = clusterConfigEntityMap.get(latestConfig);

          // add the config type to our list for tracking later on
          configTypesForService.add(latestConfig.getType());

          // un-select the latest configuration for the service
          LOG.debug("Disabling configuration {} with tag {}", latestConfig.getType(), latestConfig.getTag());
          latestConfig.setSelected(false);
        }
      }

      // get the latest configurations for the given stack which we're going to make active
      Collection<ClusterConfigEntity> latestConfigsByStack = clusterDAO.getLatestConfigurations(
          clusterId, stackId);

      // set the service configuration for the specified stack to the latest
      for (ClusterConfigEntity latestConfigByStack : latestConfigsByStack) {
        // since we're iterating over all configuration types, only work with those that are for our service
        if (!configTypesForService.contains(latestConfigByStack.getType())) {
          continue;
        }

        // pull the correct latest mapping for the stack out of the cached map
        // from the cluster entity
        ClusterConfigEntity entity = clusterConfigEntityMap.get(latestConfigByStack);
        entity.setSelected(true);

        LOG.info("Setting {} with version tag {} created on {} to selected for stack {}",
            entity.getType(), entity.getTag(), new Date(entity.getTimestamp()),
          stackId
        );
      }

      // since the entities which were modified came from the cluster entity's
      // list to begin with, we can just save them right back - no need for a
      // new collection since the entity instances were modified directly
      clusterEntity = clusterDAO.merge(clusterEntity, true);

      cacheConfigurations();

      LOG.info(
          "Applied latest configurations for {} on stack {}. The the following types were modified: {}",
          serviceName, stackId, StringUtils.join(configTypesForService, ','));

    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

    // publish an event to instruct entity managers to clear cached instances of
    // ClusterEntity immediately - it takes EclipseLink about 1000ms to update
    // the L1 caches of other threads and the action scheduler could act upon
    // stale data
    EntityManagerCacheInvalidationEvent event = new EntityManagerCacheInvalidationEvent();
    jpaEventPublisher.publish(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<PropertyInfo.PropertyType, Set<String>> getConfigPropertiesTypes(String configType){
    return getConfigPropertiesTypes(configType, getCurrentStackVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<PropertyInfo.PropertyType, Set<String>> getConfigPropertiesTypes(String configType, StackId stackId) {
    try {
      StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      return stackInfo.getConfigPropertiesTypes(configType);
    } catch (AmbariException ignored) {
    }
    return new HashMap<>();
  }

  /**
   * Removes all configurations associated with the specified stack for the
   * specified service. The caller should make sure the cluster global write
   * lock is acquired.
   *
   * @param stackId
   *          the stack to remove configurations for (not {@code null}).
   * @param serviceName
   *          the service name (not {@code null}).
   * @see #clusterGlobalLock
   */
  @Transactional
  void removeAllConfigsForStack(StackId stackId, String serviceName) {
    ClusterEntity clusterEntity = getClusterEntity();

    // make sure the entity isn't stale in the current unit of work.
    clusterDAO.refresh(clusterEntity);

    long clusterId = clusterEntity.getClusterId();

    // keep track of any types removed for logging purposes
    Set<String> removedConfigurationTypes = new HashSet<>();

    // this will keep track of cluster config mappings that need removal
    // since there is no relationship between configs and their mappings, we
    // have to do it manually
    List<ClusterConfigEntity> removedClusterConfigs = new ArrayList<>(50);
    Collection<ClusterConfigEntity> allClusterConfigEntities = clusterEntity.getClusterConfigEntities();
    Collection<ServiceConfigEntity> allServiceConfigEntities = clusterEntity.getServiceConfigEntities();

    // get the service configs only for the service
    List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getServiceConfigsForServiceAndStack(
        clusterId, stackId, serviceName);

    // remove all service configurations and associated configs
    for (ServiceConfigEntity serviceConfig : serviceConfigs) {
      for (ClusterConfigEntity configEntity : serviceConfig.getClusterConfigEntities()) {
        removedConfigurationTypes.add(configEntity.getType());

        allClusterConfigEntities.remove(configEntity);
        clusterDAO.removeConfig(configEntity);
        removedClusterConfigs.add(configEntity);
      }

      serviceConfig.getClusterConfigEntities().clear();
      serviceConfigDAO.remove(serviceConfig);
      allServiceConfigEntities.remove(serviceConfig);
    }

    clusterEntity.setClusterConfigEntities(allClusterConfigEntities);
    clusterEntity = clusterDAO.merge(clusterEntity);

    LOG.info("Removed the following configuration types for {} on stack {}: {}", serviceName,
        stackId, StringUtils.join(removedConfigurationTypes, ','));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeConfigurations(StackId stackId, String serviceName) {
    clusterGlobalLock.writeLock().lock();
    try {
      removeAllConfigsForStack(stackId, serviceName);
      cacheConfigurations();
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  /**
   * Caches all of the {@link ClusterConfigEntity}s in {@link #allConfigs}.
   */
  private void cacheConfigurations() {
    clusterGlobalLock.writeLock().lock();
    try {
      ClusterEntity clusterEntity = getClusterEntity();
      allConfigs.clear();

      if (!clusterEntity.getClusterConfigEntities().isEmpty()) {
        for (ClusterConfigEntity entity : clusterEntity.getClusterConfigEntities()) {

          if (!allConfigs.containsKey(entity.getType())) {
            allConfigs.put(entity.getType(), new ConcurrentHashMap<>());
          }

          Config config = configFactory.createExisting(this, entity);

          allConfigs.get(entity.getType()).put(entity.getTag(), config);
        }
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  private void loadStackVersion() {
    desiredStackVersion = new StackId(getClusterEntity().getDesiredStack());

    if (!StringUtils.isEmpty(desiredStackVersion.getStackName())
        && !StringUtils.isEmpty(desiredStackVersion.getStackVersion())) {
      try {
        loadServiceConfigTypes();
      } catch (AmbariException e) {
        // TODO recheck wrapping exception here, required for lazy loading after
        // invalidation
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Returns whether this cluster was provisioned by a Blueprint or not.
   * @return true if the cluster was deployed with a Blueprint otherwise false.
   */
  @Override
  public boolean isBluePrintDeployed() {

    List<TopologyRequestEntity> topologyRequests = topologyRequestDAO.findByClusterId(getClusterId());

    // Iterate through the topology requests associated with this cluster and look for PROVISION request
    for (TopologyRequestEntity topologyRequest: topologyRequests) {
      TopologyRequest.Type requestAction = TopologyRequest.Type.valueOf(topologyRequest.getAction());
      if (requestAction == TopologyRequest.Type.PROVISION) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets the {@link ClusterEntity} for this {@link Cluster} from the
   * {@link EntityManager} cache.
   *
   * @return
   */
  @Override
  public ClusterEntity getClusterEntity() {
    return clusterDAO.findById(clusterId);
  }

  /**
  * Returns the number of hosts that form the cluster.
  *
  *  @return number of hosts that form the cluster
  */
  @Override
  public int getClusterSize() {
    return clusters.getClusterSize(clusterName);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeEntity getUpgradeInProgress() {
    ClusterEntity clusterEntity = getClusterEntity();
    return clusterEntity.getUpgradeEntity();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void setUpgradeEntity(UpgradeEntity upgradeEntity) throws AmbariException {
    try {
      ClusterEntity clusterEntity = getClusterEntity();
      clusterEntity.setUpgradeEntity(upgradeEntity);
      clusterDAO.merge(clusterEntity);
    } catch (RollbackException e) {
      throw new AmbariException("Unable to update the associated upgrade with the cluster", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUpgradeSuspended() {
    UpgradeEntity upgrade = getUpgradeInProgress();
    if (null != upgrade) {
      return upgrade.isSuspended();
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getClusterProperty(String propertyName, String defaultValue) {
    String cachedValue = m_clusterPropertyCache.get(propertyName);
    if (null != cachedValue) {
      return cachedValue;
    }

    // start with the default
    cachedValue = defaultValue;

    Config clusterEnv = getDesiredConfigByType(ConfigHelper.CLUSTER_ENV);
    if (null != clusterEnv) {
      Map<String, String> clusterEnvProperties = clusterEnv.getProperties();
      if (clusterEnvProperties.containsKey(propertyName)) {
        String value = clusterEnvProperties.get(propertyName);
        if (null != value) {
          cachedValue = value;
        }
      }
    }

    // cache the value and return it
    m_clusterPropertyCache.put(propertyName, cachedValue);
    return cachedValue;
  }

  /**
   * Gets whether the specified cluster property is already cached.
   *
   * @param propertyName
   *          the property to check.
   * @return {@code true} if the property is cached.
   */
  boolean isClusterPropertyCached(String propertyName) {
    return m_clusterPropertyCache.containsKey(propertyName);
  }

  /**
   * Handles {@link ClusterConfigChangedEvent} which means that the
   * {{cluster-env}} may have changed.
   *
   * @param event
   *          the change event.
   */
  @Subscribe
  public void handleClusterEnvConfigChangedEvent(ClusterConfigChangedEvent event) {
    if (!StringUtils.equals(event.getConfigType(), ConfigHelper.CLUSTER_ENV)) {
      return;
    }

    m_clusterPropertyCache.clear();
  }

  @Subscribe
  public void onClusterProvisioned(ClusterProvisionedEvent event) {
    if (event.getClusterId() == getClusterId()) {
      LOG.info("Removing temporary configurations after successful deployment of cluster id={} name={}", getClusterId(), getClusterName());
      for (Map.Entry<String, Set<String>> e : BlueprintConfigurationProcessor.TEMPORARY_PROPERTIES_FOR_CLUSTER_DEPLOYMENT.entrySet()) {
        try {
          configHelper.updateConfigType(this, getCurrentStackVersion(), controller,
            e.getKey(), Collections.emptyMap(), e.getValue(),
            "internal", "Removing temporary configurations after successful deployment"
          );
          LOG.info("Removed temporary configurations: {} / {}", e.getKey(), e.getValue());
        } catch (AmbariException ex) {
          LOG.warn("Failed to remove temporary configurations: {} / {}", e.getKey(), e.getValue(), ex);
        }
      }
      changeBlueprintProvisioningState(BlueprintProvisioningState.FINISHED);
    }
  }

  private void changeBlueprintProvisioningState(BlueprintProvisioningState newState) {
    boolean updated = setBlueprintProvisioningState(newState);
    if (updated) {
      try {
        //host level params update
        hostLevelParamsHolder.updateAllHosts();
      } catch (AmbariException e) {
        LOG.error("Topology update failed after setting blueprint provision state to {}", newState, e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrderProvider.getRoleCommandOrder(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addSuspendedUpgradeParameters(Map<String, String> commandParams,
      Map<String, String> roleParams) {

    // build some command params from the upgrade, including direction,
    // type, version, etc
    UpgradeEntity suspendedUpgrade = getUpgradeInProgress();
    if( null == suspendedUpgrade ){
      LOG.warn(
          "An upgrade is not currently suspended. The command and role parameters will not be modified.");

      return;
    }

    UpgradeContext upgradeContext = upgradeContextFactory.create(this, suspendedUpgrade);
    commandParams.putAll(upgradeContext.getInitializedCommandParameters());

    // suspended goes in role params
    roleParams.put(KeyNames.UPGRADE_SUSPENDED, Boolean.TRUE.toString().toLowerCase());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Map<String, String>> getComponentVersionMap() {
    Map<String, Map<String, String>> componentVersionMap = new HashMap<>();

    for (Service service : getServices().values()) {
      Map<String, String> componentMap = new HashMap<>();
      for (ServiceComponent component : service.getServiceComponents().values()) {
        // skip components which don't advertise a version
        if (!component.isVersionAdvertised()) {
          continue;
        }

        // if the repo isn't resolved, then we can't trust the version
        if (!component.getDesiredRepositoryVersion().isResolved()) {
          continue;
        }

        componentMap.put(component.getName(), component.getDesiredVersion());
      }

      if (!componentMap.isEmpty()) {
        componentVersionMap.put(service.getName(), componentMap);
      }
    }

    return componentVersionMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ClusterInformation buildClusterInformation() {
    SecurityType securityType = getSecurityType();
    Map<String, Set<String>> topology = new HashMap<>();
    List<ServiceComponentHost> serviceComponentHosts = getServiceComponentHosts();
    for (ServiceComponentHost serviceComponentHost : serviceComponentHosts) {
      String hash = serviceComponentHost.getServiceName() + "/"
          + serviceComponentHost.getServiceComponentName();

      Set<String> hosts = topology.get(hash);
      if (null == hosts) {
        hosts = Sets.newTreeSet();
        topology.put(hash, hosts);
      }

      hosts.add(serviceComponentHost.getHostName());
    }

    Map<String, Map<String, String>> configurations = new HashMap<>();
    Map<String, DesiredConfig> desiredConfigs = getDesiredConfigs();
    for (Map.Entry<String, DesiredConfig> desiredConfigEntry : desiredConfigs.entrySet()) {
      String configType = desiredConfigEntry.getKey();
      DesiredConfig desiredConfig = desiredConfigEntry.getValue();
      Config clusterConfig = getConfig(configType, desiredConfig.getTag());
      configurations.put(configType, clusterConfig.getProperties());
    }

    Map<String, Service> clusterServices = getServices();
    Map<String, RepositoryVersion> clusterServiceVersions = new HashMap<>();
    if (null != clusterServices) {
      for (Map.Entry<String, Service> serviceEntry : clusterServices.entrySet()) {
        Service service = serviceEntry.getValue();
        RepositoryVersionEntity desiredRepositoryEntity = service.getDesiredRepositoryVersion();
        RepositoryVersion desiredRepositoryVersion = desiredRepositoryEntity.getRepositoryVersion();

        clusterServiceVersions.put(serviceEntry.getKey(), desiredRepositoryVersion);
      }
    }

    return new ClusterInformation(getClusterName(), securityType == SecurityType.KERBEROS,
        configurations, topology, clusterServiceVersions);
  }
}
