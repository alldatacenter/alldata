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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.RollbackException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.events.HostsAddedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.RequestOperationLevelDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.TopologyHostInfoDAO;
import org.apache.ambari.server.orm.dao.TopologyLogicalTaskDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.RetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClustersImpl implements Clusters {

  private static final Logger LOG = LoggerFactory.getLogger(ClustersImpl.class);

  private ConcurrentHashMap<String, Cluster> clustersByName = null;
  private ConcurrentHashMap<Long, Cluster> clustersById = null;
  private ConcurrentHashMap<String, Host> hostsByName = null;
  private ConcurrentHashMap<Long, Host> hostsById = null;
  private ConcurrentHashMap<String, Set<Cluster>> hostClustersMap = null;
  private ConcurrentHashMap<String, Set<Host>> clusterHostsMap1 = null;

  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private HostVersionDAO hostVersionDAO;
  @Inject
  private HostStateDAO hostStateDAO;
  @Inject
  private ResourceTypeDAO resourceTypeDAO;
  @Inject
  private RequestOperationLevelDAO requestOperationLevelDAO;
  @Inject
  private HostConfigMappingDAO hostConfigMappingDAO;
  @Inject
  private ServiceConfigDAO serviceConfigDAO;
  @Inject
  private ClusterFactory clusterFactory;
  @Inject
  private HostFactory hostFactory;
  @Inject
  private SecurityHelper securityHelper;
  @Inject
  private TopologyLogicalTaskDAO topologyLogicalTaskDAO;
  @Inject
  private TopologyHostInfoDAO topologyHostInfoDAO;
  @Inject
  private TopologyManager topologyManager;
  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  /**
   * Data access object for stacks.
   */
  @Inject
  private StackDAO stackDAO;

  /**
   * Used to publish events relating to cluster CRUD operations.
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  @Inject
  private Provider<TopologyHolder> m_topologyHolder;

  @Inject
  private Provider<AgentConfigsHolder> m_agentConfigsHolder;

  @Inject
  private Provider<MetadataHolder> m_metadataHolder;

  @Inject
  private Provider<AmbariManagementControllerImpl> m_ambariManagementController;

  @Inject
  public ClustersImpl(ClusterDAO clusterDAO, ClusterFactory clusterFactory, HostDAO hostDAO,
      HostFactory hostFactory) {

    this.clusterDAO = clusterDAO;
    this.clusterFactory = clusterFactory;
    this.hostDAO = hostDAO;
    this.hostFactory = hostFactory;
  }

  /**
   * Gets the internal clusters-by-name map, ensuring that the relevant data has been previously initialized.
   *
   * @return a map of the requested data
   */
  private ConcurrentHashMap<String, Cluster> getClustersByName() {
    if (clustersByName == null) {
      safelyLoadClustersAndHosts();
    }
    return clustersByName;
  }

  /**
   * Gets the internal clusters-by-id map, ensuring that the relevant data has been previously initialized.
   *
   * @return a map of the requested data
   */
  private ConcurrentHashMap<Long, Cluster> getClustersById() {
    if (clustersById == null) {
      safelyLoadClustersAndHosts();
    }
    return clustersById;
  }

  /**
   * Gets the internal hosts-by-name map, ensuring that the relevant data has been previously initialized.
   *
   * @return a map of the requested data
   */
  private ConcurrentHashMap<String, Host> getHostsByName() {
    if (hostsByName == null) {
      safelyLoadClustersAndHosts();
    }
    return hostsByName;
  }

  /**
   * Gets the internal hosts-by-id map, ensuring that the relevant data has been previously initialized.
   *
   * @return a map of the requested data
   */
  private ConcurrentHashMap<Long, Host> getHostsById() {
    if (hostsById == null) {
      safelyLoadClustersAndHosts();
    }
    return hostsById;
  }

  /**
   * Gets the internal host/clusters map, ensuring that the relevant data has been previously initialized.
   *
   * @return a map of the requested data
   */
  private ConcurrentHashMap<String, Set<Cluster>> getHostClustersMap() {
    if (hostClustersMap == null) {
      safelyLoadClustersAndHosts();
    }
    return hostClustersMap;
  }

  /**
   * Gets the internal cluster/hosts map, ensuring that the relevant data has been previously initialized.
   *
   * @return a map of the requested data
   */
  private ConcurrentHashMap<String, Set<Host>> getClusterHostsMap() {
    if (clusterHostsMap1 == null) {
      safelyLoadClustersAndHosts();
    }
    return clusterHostsMap1;
  }

  /**
   * Safely initializes all of the in-memory state collections that this class.
   * <p>
   * This method is synchronized so that the data can be loaded only if needed.  If most than one of
   * the relevant get methods are concurrently invoked, this method should ensure that {@link #loadClustersAndHosts()}
   * will be entered once.  Subsequent calls will bypass {@link #loadClustersAndHosts()} since the
   * relevant variables will no longer be null.
   *
   * @see #getClustersByName()
   * @see #getClustersById()
   * @see #getHostsByName()
   * @see #getHostsById()
   * @see #getClusterHostsMap()
   * @see #getHostClustersMap()
   */
  synchronized private void safelyLoadClustersAndHosts() {
    if (clustersByName == null || clustersById == null ||
        hostsByName == null || hostsById == null ||
        hostClustersMap == null || clusterHostsMap1 == null) {
      loadClustersAndHosts();
    }
  }

  /**
   * Initializes all of the in-memory state collections that this class
   * unfortunately uses.
   * <p>
   * This method should be called only once, when the data is first needed.
   * <p/>
   * Because some of these stateful initializations may actually reference this
   * {@link Clusters} instance, we must do this after the object has been
   * instantiated and injected.
   */
  private void loadClustersAndHosts() {
    LOG.info("Initializing cluster and host data.");

    ConcurrentHashMap<String, Cluster> clustersByNameTemp = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Cluster> clustersByIdTemp = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Host> hostsByNameTemp = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Host> hostsByIdTemp = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Set<Cluster>> hostClustersMapTemp = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Set<Host>> clusterHostsMap1Temp = new ConcurrentHashMap<>();

    List<HostEntity> hostEntities = hostDAO.findAll();
    for (HostEntity hostEntity : hostEntities) {
      Host host = hostFactory.create(hostEntity);
      hostsByNameTemp.put(hostEntity.getHostName(), host);
      hostsByIdTemp.put(hostEntity.getHostId(), host);
    }
    hostsByName = hostsByNameTemp;
    hostsById = hostsByIdTemp;

    for (ClusterEntity clusterEntity : clusterDAO.findAll()) {
      Cluster currentCluster = clusterFactory.create(clusterEntity);
      clustersByNameTemp.put(clusterEntity.getClusterName(), currentCluster);
      clustersByIdTemp.put(currentCluster.getClusterId(), currentCluster);
      clusterHostsMap1Temp.put(currentCluster.getClusterName(), Collections.newSetFromMap(new ConcurrentHashMap<>()));
    }
    clustersByName = clustersByNameTemp;
    clustersById = clustersByIdTemp;

    for (HostEntity hostEntity : hostEntities) {
      Set<Cluster> cSet = Collections.newSetFromMap(new ConcurrentHashMap<Cluster, Boolean>());
      hostClustersMapTemp.put(hostEntity.getHostName(), cSet);

      Host host = getHostsByName().get(hostEntity.getHostName());
      for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
        clusterHostsMap1Temp.get(clusterEntity.getClusterName()).add(host);
        cSet.add(clustersByName.get(clusterEntity.getClusterName()));
      }
    }
    hostClustersMap = hostClustersMapTemp;
    clusterHostsMap1 = clusterHostsMap1Temp;
    // init host configs
    for (Long hostId : hostsById.keySet()) {
      try {
        m_agentConfigsHolder.get().initializeDataIfNeeded(hostId, true);
      } catch (AmbariException e) {
        LOG.error("Agent configs initialization was failed", e);
      }
    }
  }

  @Override
  public void addCluster(String clusterName, StackId stackId)
    throws AmbariException {
    addCluster(clusterName, stackId, null);
  }

  @Override
  public void addCluster(String clusterName, StackId stackId, SecurityType securityType)
      throws AmbariException {
    Cluster cluster = null;

    if (getClustersByName().containsKey(clusterName)) {
      throw new DuplicateResourceException(
          "Attempted to create a Cluster which already exists" + ", clusterName=" + clusterName);
    }

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    // retrieve new cluster id
    // add cluster id -> cluster mapping into clustersById
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(clusterName);
    clusterEntity.setDesiredStack(stackEntity);
    clusterEntity.setResource(resourceEntity);
    if (securityType != null) {
      clusterEntity.setSecurityType(securityType);
    }

    try {
      clusterDAO.create(clusterEntity);
    } catch (RollbackException e) {
      LOG.warn("Unable to create cluster " + clusterName, e);
      throw new AmbariException("Unable to create cluster " + clusterName, e);
    }

    cluster = clusterFactory.create(clusterEntity);
    getClustersByName().put(clusterName, cluster);
    getClustersById().put(cluster.getClusterId(), cluster);
    getClusterHostsMap().put(clusterName,
        Collections.newSetFromMap(new ConcurrentHashMap<>()));

    cluster.setCurrentStackVersion(stackId);

    TreeMap<String, TopologyCluster> addedClusters = new TreeMap<>();
    TopologyCluster addedCluster = new TopologyCluster();
    addedClusters.put(Long.toString(cluster.getClusterId()), addedCluster);
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(addedClusters,
        UpdateEventType.UPDATE);
    m_topologyHolder.get().updateData(topologyUpdateEvent);
    m_metadataHolder.get().updateData(m_ambariManagementController.get().getClusterMetadata(cluster));
  }

  @Override
  public Cluster getCluster(String clusterName)
      throws AmbariException {
    Cluster cluster = null;
    if (clusterName != null) {
      cluster = getClustersByName().get(clusterName);
    }
    if (null == cluster) {
      throw new ClusterNotFoundException(clusterName);
    }
    RetryHelper.addAffectedCluster(cluster);
    return cluster;
  }

  @Override
  public Cluster getCluster(Long clusterId)
    throws AmbariException {
    Cluster cluster = null;
    if (clusterId != null) {
      cluster = getClustersById().get(clusterId);
    }
    if (null == cluster) {
      throw new ClusterNotFoundException(clusterId);
    }
    RetryHelper.addAffectedCluster(cluster);
    return cluster;
  }

  @Override
  public Cluster getClusterById(long id) throws AmbariException {
    ConcurrentHashMap<Long, Cluster> clustersById = getClustersById();
    Cluster cluster = clustersById.get(id);
    if (null == cluster) {
      throw new ClusterNotFoundException("clusterID=" + id);
    }

    return clustersById.get(id);
  }

  @Override
  public List<Host> getHosts() {
    return new ArrayList<>(getHostsByName().values());
  }

  @Override
  public Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException {
    Set<Cluster> clusters = getHostClustersMap().get(hostname);
    if(clusters == null){
      throw new HostNotFoundException(hostname);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Looking up clusters for hostname, hostname={}, mappedClusters={}", hostname, clusters.size());
    }
    return Collections.unmodifiableSet(clusters);

  }

  @Override
  public Host getHost(String hostname) throws AmbariException {
    Host host = getHostsByName().get(hostname);
    if (null == host) {
      throw new HostNotFoundException(hostname);
    }

    return host;
  }

  @Override
  public boolean hostExists(String hostname){
    return getHostsByName().containsKey(hostname);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isHostMappedToCluster(long clusterId, String hostName) {
    Set<Cluster> clusters = getHostClustersMap().get(hostName);
    for (Cluster cluster : clusters) {
      if (clusterId == cluster.getClusterId()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Host getHostById(Long hostId) throws AmbariException {
    if (!getHostsById().containsKey(hostId)) {
      throw new HostNotFoundException("Host Id = " + hostId);
    }

    return getHostsById().get(hostId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateHostMappings(Host host) {
    Long hostId = host.getHostId();

    if (null != hostId) {
      getHostsById().put(hostId, host);
      // init host configs
      try {
        m_agentConfigsHolder.get().initializeDataIfNeeded(hostId, true);
      } catch (AmbariException e) {
        LOG.error("Agent configs initialization was failed for host with id %s", hostId, e);
      }
    }
  }

  /**
   * Register a host by creating a {@link HostEntity} object in the database and setting its state to
   * {@link HostState#INIT}. This does not add the host the cluster.
   * @param hostname Host to be added
   * @throws AmbariException
   */
  @Override
  public void addHost(String hostname) throws AmbariException {
    if (getHostsByName().containsKey(hostname)) {
      throw new AmbariException(MessageFormat.format("Duplicate entry for Host {0}", hostname));
    }

    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname);
    hostEntity.setClusterEntities(new ArrayList<>());

    // not stored to DB
    Host host = hostFactory.create(hostEntity);
    host.setAgentVersion(new AgentVersion(""));
    List<DiskInfo> emptyDiskList = new CopyOnWriteArrayList<>();
    host.setDisksInfo(emptyDiskList);
    host.setHealthStatus(new HostHealthStatus(HealthStatus.UNKNOWN, ""));
    host.setHostAttributes(new ConcurrentHashMap<>());
    host.setState(HostState.INIT);

    // the hosts by ID map is updated separately since the host has not yet
    // been persisted yet - the below event is what causes the persist
    getHostsByName().put(hostname, host);
    getHostsById().put(host.getHostId(), host);

    getHostClustersMap().put(hostname,
        Collections.newSetFromMap(new ConcurrentHashMap<>()));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a host to Clusters, hostname={}", hostname);
    }
  }

  @Override
  public void updateHostWithClusterAndAttributes(
      Map<String, Set<String>> hostClusters,
      Map<String, Map<String, String>> hostAttributes) throws AmbariException {

    if (null == hostClusters || hostClusters.isEmpty()) {
      return;
    }

    Map<String, Host> hostMap = getHostsMap(hostClusters.keySet());

    Map<String, Set<String>> clusterHosts = new HashMap<>();
    for (Map.Entry<String, Set<String>> hostClustersEntry : hostClusters.entrySet()) {
      Set<String> hostClusterNames = hostClustersEntry.getValue();
      String hostname = hostClustersEntry.getKey();

      // populate attributes
      Host host = hostMap.get(hostname);
      Map<String, String> attributes = hostAttributes.get(hostname);
      if (attributes != null && !attributes.isEmpty()) {
        host.setHostAttributes(attributes);
      }

      // create cluster to hosts map
      for (String clusterName : hostClusterNames) {
        if (clusterName != null && !clusterName.isEmpty()) {
          if (!clusterHosts.containsKey(clusterName)) {
            clusterHosts.put(clusterName, new HashSet<>());
          }
          clusterHosts.get(clusterName).add(hostname);
        }
      }
    }

    for (Map.Entry<String, Set<String>> clusterHostsEntry : clusterHosts.entrySet()) {
      Set<String> clusterHostsNames = clusterHostsEntry.getValue();
      String clusterName = clusterHostsEntry.getKey();
      mapAndPublishHostsToCluster(clusterHostsNames, clusterName);
    }
  }

  private Map<String, Host> getHostsMap(Collection<String> hostSet) throws
      HostNotFoundException {

    Map<String, Host> hostMap = new HashMap<>();
    Host host = null;
    for (String hostName : hostSet) {
      if (null != hostName) {
          host= getHostsByName().get(hostName);
        if (host == null) {
          throw new HostNotFoundException(hostName);
        }
      } else {
        throw new HostNotFoundException(hostName);
      }

      hostMap.put(hostName, host);
    }

    return hostMap;
  }

  /**
   *  For each host, attempts to map it to the cluster, and apply the cluster's current version to the host.
   * @param hostnames Collection of host names
   * @param clusterName Cluster name
   * @throws AmbariException
   */
  @Override
  public void mapAndPublishHostsToCluster(Set<String> hostnames, String clusterName) throws AmbariException {
    for (String hostname : hostnames) {
      mapHostToCluster(hostname, clusterName);
    }
    publishAddingHostsToCluster(hostnames, clusterName);
    getCluster(clusterName).refresh();
  }

  private void publishAddingHostsToCluster(Set<String> hostnames, String clusterName) throws AmbariException {
    HostsAddedEvent event = new HostsAddedEvent(getCluster(clusterName).getClusterId(), hostnames);
    eventPublisher.publish(event);
  }

  /**
   * Attempts to map the host to the cluster via clusterhostmapping table if not already present, and add a host_version
   * record for the cluster's currently applied (stack, version) if not already present.
   * @param hostname Host name
   * @param clusterName Cluster name
   * @throws AmbariException May throw a DuplicateResourceException.
   */
  @Override
  public void mapHostToCluster(String hostname, String clusterName)
      throws AmbariException {

    Host host = getHost(hostname);
    Cluster cluster = getCluster(clusterName);
    ConcurrentHashMap<String, Set<Cluster>> hostClustersMap = getHostClustersMap();

    // check to ensure there are no duplicates
    for (Cluster c : hostClustersMap.get(hostname)) {
      if (c.getClusterName().equals(clusterName)) {
        throw new DuplicateResourceException("Attempted to create a host which already exists: clusterName=" +
          clusterName + ", hostName=" + hostname);
      }
    }

    long clusterId = cluster.getClusterId();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Mapping host {} to cluster {} (id={})", hostname, clusterName,
        clusterId);
    }

    mapHostClusterEntities(hostname, clusterId);
    hostClustersMap.get(hostname).add(cluster);
    getClusterHostsMap().get(clusterName).add(host);
  }

  @Transactional
  void mapHostClusterEntities(String hostName, Long clusterId) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);

    hostEntity.getClusterEntities().add(clusterEntity);
    clusterEntity.getHostEntities().add(hostEntity);

    clusterDAO.merge(clusterEntity);
    hostDAO.merge(hostEntity);
  }

  @Override
  public Map<String, Cluster> getClusters() {
    return Collections.unmodifiableMap(getClustersByName());
  }

  @Override
  public void updateClusterName(String oldName, String newName) {
    ConcurrentHashMap<String, Cluster> clusters = getClustersByName();
    clusters.put(newName, clusters.remove(oldName));

    ConcurrentHashMap<String, Set<Host>> clusterHostsMap = getClusterHostsMap();
    clusterHostsMap.put(newName, clusterHostsMap.remove(oldName));

    //TODO metadata update
  }


  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("Clusters=[ ");
    boolean first = true;
    for (Cluster c : getClustersByName().values()) {
      if (!first) {
        sb.append(" , ");
      }
      first = false;
      sb.append("\n  ");
      c.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ]");
  }

  @Override
  public Map<String, Host> getHostsForCluster(String clusterName) {
    Map<String, Host> hosts = new HashMap<>();
    for (Host h : getClusterHostsMap().get(clusterName)) {
      hosts.put(h.getHostName(), h);
    }

    return hosts;
  }

  @Override
  public Map<Long, Host> getHostIdsForCluster(String clusterName)
      throws AmbariException {
    Map<Long, Host> hosts = new HashMap<>();

    for (Host h : getClusterHostsMap().get(clusterName)) {
      HostEntity hostEntity = hostDAO.findByName(h.getHostName());
      hosts.put(hostEntity.getHostId(), h);
    }

    return hosts;
  }

  @Override
  public void deleteCluster(String clusterName)
      throws AmbariException {
    Cluster cluster = getCluster(clusterName);
    if (!cluster.canBeRemoved()) {
      throw new AmbariException("Could not delete cluster" + ", clusterName=" + clusterName);
    }

    LOG.info("Deleting cluster " + cluster.getClusterName());
    cluster.delete();

    // clear maps
    for (Set<Cluster> clusterSet : getHostClustersMap().values()) {
      clusterSet.remove(cluster);
    }
    getClusterHostsMap().remove(cluster.getClusterName());
    getClustersByName().remove(clusterName);
  }

  @Override
  public void unmapHostFromCluster(String hostname, String clusterName) throws AmbariException {
    final Cluster cluster = getCluster(clusterName);
    Host host = getHost(hostname);

    unmapHostFromClusters(host, Sets.newHashSet(cluster));

    cluster.refresh();
  }

  @Transactional
  void unmapHostFromClusters(Host host, Set<Cluster> clusters) throws AmbariException {
    HostEntity hostEntity = null;

    if (clusters.isEmpty()) {
      return;
    }

    String hostname = host.getHostName();
    hostEntity = hostDAO.findByName(hostname);

    for (Cluster cluster : clusters) {
      long clusterId = cluster.getClusterId();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Unmapping host {} from cluster {} (id={})", hostname, cluster.getClusterName(),
            clusterId);
      }

      unmapHostClusterEntities(hostname, cluster.getClusterId());

      getHostClustersMap().get(hostname).remove(cluster);
      getClusterHostsMap().get(cluster.getClusterName()).remove(host);
    }

    deleteConfigGroupHostMapping(hostEntity.getHostId());

    // Remove mapping of principals to the unmapped host
    kerberosKeytabPrincipalDAO.removeByHost(hostEntity.getHostId());
  }

  @Transactional
  void unmapHostClusterEntities(String hostName, long clusterId) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);

    hostEntity.getClusterEntities().remove(clusterEntity);
    clusterEntity.getHostEntities().remove(hostEntity);

    hostDAO.merge(hostEntity);
    clusterDAO.merge(clusterEntity, true);
  }

  @Transactional
  void deleteConfigGroupHostMapping(Long hostId) throws AmbariException {
    // Remove Config group mapping
    for (Cluster cluster : getClustersByName().values()) {
      for (ConfigGroup configGroup : cluster.getConfigGroups().values()) {
        configGroup.removeHost(hostId);
      }
    }
  }

  /***
   * Delete a host entirely from the cluster and all database tables, except
   * AlertHistory. If the host is not found, throws
   * {@link org.apache.ambari.server.HostNotFoundException}.
   *
   * @param hostname
   * @throws AmbariException
   */
  @Override
  public void deleteHost(String hostname) throws AmbariException {
    // unmapping hosts from a cluster modifies the collections directly; keep
    // a copy of this to ensure that we can pass in the original set of
    // clusters that the host belonged to to the host removal event
    Set<Cluster> clusters = getHostClustersMap().get(hostname);
    if (clusters == null) {
      throw new HostNotFoundException(hostname);
    }

    deleteHostEntityRelationships(hostname);
  }

  @Override
  public void publishHostsDeletion(Set<Long> hostIds, Set<String> hostNames) throws AmbariException {
    // Publish the event, using the original list of clusters that the host
    // belonged to
    HostsRemovedEvent event = new HostsRemovedEvent(hostNames, hostIds);
    eventPublisher.publish(event);
  }

  /***
   * Deletes all of the JPA relationships between a host and other entities.
   * This method will not fire {@link HostsRemovedEvent} since it is performed
   * within an {@link Transactional} and the event must fire after the
   * transaction is successfully committed.
   *
   * @param hostname
   * @throws AmbariException
   */
  @Transactional
  void deleteHostEntityRelationships(String hostname) throws AmbariException {
    if (!getHostsByName().containsKey(hostname)) {
      throw new HostNotFoundException("Could not find host " + hostname);
    }

    HostEntity entity = hostDAO.findByName(hostname);

    if (entity == null) {
      return;
    }

    // Remove from all clusters in the cluster_host_mapping table.
    // This will also remove from kerberos_principal_hosts, hostconfigmapping,
    // and configgrouphostmapping
    Set<Cluster> clusters = getHostClustersMap().get(hostname);
    Set<Long> clusterIds = Sets.newHashSet();
    for (Cluster cluster : clusters) {
      clusterIds.add(cluster.getClusterId());
    }

    Host host = getHostsByName().get(hostname);
    unmapHostFromClusters(host, clusters);
    hostDAO.refresh(entity);

    hostVersionDAO.removeByHostName(hostname);

    // Remove blueprint tasks before hostRoleCommands
    // TopologyLogicalTask owns the OneToOne relationship but Cascade is on
    // HostRoleCommandEntity
    if (entity.getHostRoleCommandEntities() != null) {
      for (HostRoleCommandEntity hrcEntity : entity.getHostRoleCommandEntities()) {
        TopologyLogicalTaskEntity topologyLogicalTaskEnity = hrcEntity.getTopologyLogicalTaskEntity();
        if (topologyLogicalTaskEnity != null) {
          topologyLogicalTaskDAO.remove(topologyLogicalTaskEnity);
          hrcEntity.setTopologyLogicalTaskEntity(null);
        }
      }
    }

    topologyManager.removeHostRequests(hostname);

    entity.setHostStateEntity(null);
    hostStateDAO.removeByHostId(entity.getHostId());
    hostConfigMappingDAO.removeByHostId(entity.getHostId());
    serviceConfigDAO.removeHostFromServiceConfigs(entity.getHostId());
    requestOperationLevelDAO.removeByHostId(entity.getHostId());
    topologyHostInfoDAO.removeByHost(entity);

    // Remove from dictionaries
    getHostsByName().remove(hostname);
    getHostsById().remove(entity.getHostId());

    hostDAO.remove(entity);

    // Note, if the host is still heartbeating, then new records will be
    // re-inserted
    // into the hosts and hoststate tables
  }

  @Override
  public boolean checkPermission(String clusterName, boolean readOnly) {
    Cluster cluster = findCluster(clusterName);

    return (cluster == null && readOnly) || checkPermission(cluster, readOnly);
  }

  @Override
  public void addSessionAttributes(String name, Map<String, Object> attributes) {
    Cluster cluster = findCluster(name);
    if (cluster != null) {
      cluster.addSessionAttributes(attributes);
    }
  }

  @Override
  public Map<String, Object> getSessionAttributes(String name) {
    Cluster cluster = findCluster(name);
    return cluster == null ? Collections.emptyMap() : cluster.getSessionAttributes();
  }

  /**
   * Returns the number of hosts that form the cluster identified by the given name.
   *
   * @param clusterName the name that identifies the cluster
   * @return number of hosts that form the cluster
   */
  @Override
  public int getClusterSize(String clusterName) {
    int hostCount = 0;
    ConcurrentHashMap<String, Set<Host>> clusterHostsMap = getClusterHostsMap();
    Set<Host> hosts = clusterHostsMap.get(clusterName);
    if (null != hosts) {
      hostCount = clusterHostsMap.get(clusterName).size();
    }

    return hostCount;
  }

  // ----- helper methods ---------------------------------------------------

  /**
   * Find the cluster for the given name.
   *
   * @param name  the cluster name
   *
   * @return the cluster for the given name; null if the cluster can not be found
   */
  protected Cluster findCluster(String name) {
    Cluster cluster = null;
    try {
      cluster = name == null ? null : getCluster(name);
    } catch (AmbariException e) {
      // do nothing
    }
    return cluster;
  }

  /**
   * Determine whether or not access to the given cluster resource should be allowed based
   * on the privileges of the current user.
   *
   * @param cluster   the cluster
   * @param readOnly  indicate whether or not this check is for a read only operation
   *
   * @return true if the access to this cluster is allowed
   */
  private boolean checkPermission(Cluster cluster, boolean readOnly) {
    for (GrantedAuthority grantedAuthority : securityHelper.getCurrentAuthorities()) {
      if (grantedAuthority instanceof AmbariGrantedAuthority) {

        AmbariGrantedAuthority authority       = (AmbariGrantedAuthority) grantedAuthority;
        PrivilegeEntity        privilegeEntity = authority.getPrivilegeEntity();
        Integer                permissionId    = privilegeEntity.getPermission().getId();

        // admin has full access
        if (permissionId.equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION)) {
          return true;
        }
        if (cluster != null) {
          if (cluster.checkPermission(privilegeEntity, readOnly)) {
            return true;
          }
        }
      }
    }
    // TODO : should we log this?
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void invalidate(Cluster cluster) {
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());
    Cluster currentCluster = clusterFactory.create(clusterEntity);
    getClustersByName().put(clusterEntity.getClusterName(), currentCluster);
    getClustersById().put(currentCluster.getClusterId(), currentCluster);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void invalidateAllClusters() {
    if(clustersByName != null) {
      Collection<Cluster> clusters = clustersByName.values();
      for (Cluster cluster : clusters) {
        invalidate(cluster);
      }
    }
  }
}
