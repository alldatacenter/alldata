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
package org.apache.ambari.server.state.configgroup;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.controller.internal.ConfigurationResourceProvider;
import org.apache.ambari.server.logging.LockFactory;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupConfigMappingDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntityPK;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ConfigGroupImpl implements ConfigGroup {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigGroupImpl.class);

  private Cluster cluster;
  private ConcurrentMap<Long, Host> m_hosts;
  private ConcurrentMap<String, Config> m_configurations;
  private String configGroupName;
  private String serviceName;
  private long configGroupId;

  /**
   * This lock is required to prevent inconsistencies in internal state between
   * {@link #m_hosts} and the entities stored by the {@link ConfigGroupEntity}.
   */
  private final ReadWriteLock hostLock;

  /**
   * A label for {@link #hostLock} to use with the {@link LockFactory}.
   */
  private static final String hostLockLabel = "configurationGroupHostLock";

  private final ConfigGroupDAO configGroupDAO;

  private final ConfigGroupConfigMappingDAO configGroupConfigMappingDAO;

  private final ConfigGroupHostMappingDAO configGroupHostMappingDAO;

  private final HostDAO hostDAO;

  private final ClusterDAO clusterDAO;

  private final ConfigFactory configFactory;

  @AssistedInject
  public ConfigGroupImpl(@Assisted("cluster") Cluster cluster,
      @Assisted("serviceName") @Nullable String serviceName, @Assisted("name") String name,
      @Assisted("tag") String tag, @Assisted("description") String description,
      @Assisted("configs") Map<String, Config> configurations,
      @Assisted("hosts") Map<Long, Host> hosts, Clusters clusters, ConfigFactory configFactory,
      ClusterDAO clusterDAO, HostDAO hostDAO, ConfigGroupDAO configGroupDAO,
      ConfigGroupConfigMappingDAO configGroupConfigMappingDAO,
      ConfigGroupHostMappingDAO configGroupHostMappingDAO, LockFactory lockFactory)
      throws AmbariException {

    this.configFactory = configFactory;
    this.clusterDAO = clusterDAO;
    this.hostDAO = hostDAO;
    this.configGroupDAO = configGroupDAO;
    this.configGroupConfigMappingDAO = configGroupConfigMappingDAO;
    this.configGroupHostMappingDAO = configGroupHostMappingDAO;

    hostLock = lockFactory.newReadWriteLock(hostLockLabel);

    this.cluster = cluster;
    this.serviceName = serviceName;
    configGroupName = name;

    ConfigGroupEntity configGroupEntity = new ConfigGroupEntity();
    configGroupEntity.setClusterId(cluster.getClusterId());
    configGroupEntity.setGroupName(name);
    configGroupEntity.setTag(tag);
    configGroupEntity.setDescription(description);
    configGroupEntity.setServiceName(serviceName);

    m_hosts = hosts == null ? new ConcurrentHashMap<>()
        : new ConcurrentHashMap<>(hosts);

    m_configurations = configurations == null ? new ConcurrentHashMap<>()
        : new ConcurrentHashMap<>(configurations);

    // save the entity and grab the ID
    persist(configGroupEntity);
    configGroupId = configGroupEntity.getGroupId();
  }

  @AssistedInject
  public ConfigGroupImpl(@Assisted Cluster cluster, @Assisted ConfigGroupEntity configGroupEntity,
      Clusters clusters, ConfigFactory configFactory,
      ClusterDAO clusterDAO, HostDAO hostDAO, ConfigGroupDAO configGroupDAO,
      ConfigGroupConfigMappingDAO configGroupConfigMappingDAO,
      ConfigGroupHostMappingDAO configGroupHostMappingDAO, LockFactory lockFactory) {

    this.configFactory = configFactory;
    this.clusterDAO = clusterDAO;
    this.hostDAO = hostDAO;
    this.configGroupDAO = configGroupDAO;
    this.configGroupConfigMappingDAO = configGroupConfigMappingDAO;
    this.configGroupHostMappingDAO = configGroupHostMappingDAO;

    hostLock = lockFactory.newReadWriteLock(hostLockLabel);

    this.cluster = cluster;
    configGroupId = configGroupEntity.getGroupId();
    configGroupName = configGroupEntity.getGroupName();
    serviceName = configGroupEntity.getServiceName();

    m_configurations = new ConcurrentHashMap<>();
    m_hosts = new ConcurrentHashMap<>();

    // Populate configs
    for (ConfigGroupConfigMappingEntity configMappingEntity : configGroupEntity.getConfigGroupConfigMappingEntities()) {
      Config config = cluster.getConfig(configMappingEntity.getConfigType(),
        configMappingEntity.getVersionTag());

      if (config != null) {
        m_configurations.put(config.getType(), config);
      } else {
        LOG.warn("Unable to find config mapping {}/{} for config group in cluster {}",
            configMappingEntity.getConfigType(), configMappingEntity.getVersionTag(),
            cluster.getClusterName());
      }
    }

    // Populate Hosts
    for (ConfigGroupHostMappingEntity hostMappingEntity : configGroupEntity.getConfigGroupHostMappingEntities()) {
      try {
        Host host = clusters.getHost(hostMappingEntity.getHostname());
        HostEntity hostEntity = hostMappingEntity.getHostEntity();
        if (host != null && hostEntity != null) {
          m_hosts.put(hostEntity.getHostId(), host);
        }
      } catch (Exception e) {
        LOG.warn("Host {} seems to be deleted but Config group {} mapping " +
          "still exists !", hostMappingEntity.getHostname(), configGroupName);
        LOG.debug("Host seems to be deleted but Config group mapping still exists !", e);
      }
    }
  }

  @Override
  public Long getId() {
    return configGroupId;
  }

  @Override
  public String getName() {
    return configGroupName;
  }

  @Override
  public void setName(String name) {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    configGroupEntity.setGroupName(name);
    configGroupDAO.merge(configGroupEntity);

    configGroupName = name;
  }

  @Override
  public String getClusterName() {
    return cluster.getClusterName();
  }

  @Override
  public String getTag() {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    return configGroupEntity.getTag();
  }

  @Override
  public void setTag(String tag) {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    configGroupEntity.setTag(tag);
    configGroupDAO.merge(configGroupEntity);
  }

  @Override
  public String getDescription() {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    return configGroupEntity.getDescription();
  }

  @Override
  public void setDescription(String description) {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    configGroupEntity.setDescription(description);
    configGroupDAO.merge(configGroupEntity);
  }

  @Override
  public Map<Long, Host> getHosts() {
    return Collections.unmodifiableMap(m_hosts);
  }

  @Override
  public Map<String, Config> getConfigurations() {
    return Collections.unmodifiableMap(m_configurations);
  }

  /**
   * Helper method to recreate host mapping
   * @param hosts
   */
  @Override
  public void setHosts(Map<Long, Host> hosts) {
    hostLock.writeLock().lock();
    try {
      // persist enitites in a transaction first, then update internal state
      replaceHostMappings(hosts);
      m_hosts = new ConcurrentHashMap<>(hosts);
    } finally {
      hostLock.writeLock().unlock();
    }
  }

  /**
   * Helper method to recreate configs mapping
   */
  @Override
  public void setConfigurations(Map<String, Config> configurations) throws AmbariException {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    ClusterEntity clusterEntity = configGroupEntity.getClusterEntity();

    // only update the internal state after the configurations have been
    // persisted
    persistConfigMapping(clusterEntity, configGroupEntity, configurations);
    m_configurations = new ConcurrentHashMap<>(configurations);
  }

  @Override
  public void removeHost(Long hostId) throws AmbariException {
    hostLock.writeLock().lock();
    try {
      Host host = m_hosts.get(hostId);
      if (null == host) {
        return;
      }

      String hostName = host.getHostName();
      LOG.info("Removing host (id={}, name={}) from config group", host.getHostId(), hostName);

      try {
        // remove the entities first, then update internal state
        removeConfigGroupHostEntity(host);
        m_hosts.remove(hostId);
      } catch (Exception e) {
        LOG.error("Failed to delete config group host mapping for cluster {} and host {}",
            cluster.getClusterName(), hostName, e);

        throw new AmbariException(e.getMessage());
      }
    } finally {
      hostLock.writeLock().unlock();
    }
  }

  /**
   * Removes the {@link ConfigGroupHostMappingEntity} for the specified host
   * from this configuration group.
   *
   * @param host
   *          the host to remove.
   */
  @Transactional
  void removeConfigGroupHostEntity(Host host) {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    ConfigGroupHostMappingEntityPK hostMappingEntityPK = new ConfigGroupHostMappingEntityPK();
    hostMappingEntityPK.setHostId(host.getHostId());
    hostMappingEntityPK.setConfigGroupId(configGroupId);

    ConfigGroupHostMappingEntity configGroupHostMapping = configGroupHostMappingDAO.findByPK(
        hostMappingEntityPK);

    configGroupHostMappingDAO.remove(configGroupHostMapping);

    configGroupEntity.getConfigGroupHostMappingEntities().remove(configGroupHostMapping);
    configGroupEntity = configGroupDAO.merge(getConfigGroupEntity());
  }

  /**
   * @param configGroupEntity
   */
  private void persist(ConfigGroupEntity configGroupEntity) throws AmbariException {
    persistEntities(configGroupEntity);
    cluster.refresh();
  }

  /**
   * Persist Config group with host mapping and configurations
   *
   * @throws Exception
   */
  @Transactional
  void persistEntities(ConfigGroupEntity configGroupEntity) throws AmbariException {
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());
    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setTimestamp(System.currentTimeMillis());
    configGroupDAO.create(configGroupEntity);

    configGroupId = configGroupEntity.getGroupId();

    persistConfigMapping(clusterEntity, configGroupEntity, m_configurations);
    replaceHostMappings(m_hosts);
  }

  /**
   * Replaces all existing host mappings with the new collection of hosts.
   */
  @Transactional
  void replaceHostMappings(Map<Long, Host> hosts) {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();

    // Delete existing mappings and create new ones
    configGroupHostMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
    configGroupEntity.setConfigGroupHostMappingEntities(new HashSet<>());

    if (hosts != null && !hosts.isEmpty()) {
      configGroupEntity = persistHostMapping(hosts.values(), configGroupEntity);
    }
  }

  /**
   * Adds the collection of hosts to the configuration group.
   */
  @Transactional
  ConfigGroupEntity persistHostMapping(Collection<Host> hosts,
      ConfigGroupEntity configGroupEntity) {
    for (Host host : hosts) {
      HostEntity hostEntity = hostDAO.findById(host.getHostId());
      if (hostEntity != null) {
        ConfigGroupHostMappingEntity hostMappingEntity = new ConfigGroupHostMappingEntity();
        hostMappingEntity.setHostId(hostEntity.getHostId());
        hostMappingEntity.setHostEntity(hostEntity);
        hostMappingEntity.setConfigGroupEntity(configGroupEntity);
        hostMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
        configGroupEntity.getConfigGroupHostMappingEntities().add(hostMappingEntity);
        configGroupHostMappingDAO.create(hostMappingEntity);
      } else {
        LOG.warn(
            "The host {} has been removed from the cluster and cannot be added to the configuration group {}",
            host.getHostName(), configGroupName);
      }
    }

    return configGroupDAO.merge(configGroupEntity);
  }

  /**
   * Persist config group config mapping and create configs if not in DB
   *
   * @param clusterEntity
   * @throws Exception
   */
  @Transactional
  void persistConfigMapping(ClusterEntity clusterEntity, ConfigGroupEntity configGroupEntity,
      Map<String, Config> configurations) throws AmbariException {
    configGroupConfigMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
    configGroupEntity.setConfigGroupConfigMappingEntities(new HashSet<>());

    if (configurations != null && !configurations.isEmpty()) {
      for (Entry<String, Config> entry : configurations.entrySet()) {
        Config config = entry.getValue();
        ClusterConfigEntity clusterConfigEntity = clusterDAO.findConfig
          (cluster.getClusterId(), config.getType(), config.getTag());

        if (clusterConfigEntity == null) {
          String serviceName = getServiceName();
          Service service = cluster.getService(serviceName);

          config = configFactory.createNew(service.getDesiredStackId(), cluster, config.getType(),
              config.getTag(), config.getProperties(), config.getPropertiesAttributes());

          entry.setValue(config);

          clusterConfigEntity = clusterDAO.findConfig(cluster.getClusterId(), config.getType(),
              config.getTag());
        }

        ConfigGroupConfigMappingEntity configMappingEntity =
          new ConfigGroupConfigMappingEntity();

        configMappingEntity.setTimestamp(System.currentTimeMillis());
        configMappingEntity.setClusterId(clusterEntity.getClusterId());
        configMappingEntity.setClusterConfigEntity(clusterConfigEntity);
        configMappingEntity.setConfigGroupEntity(configGroupEntity);
        configMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
        configMappingEntity.setConfigType(clusterConfigEntity.getType());
        configMappingEntity.setVersionTag(clusterConfigEntity.getTag());
        configGroupConfigMappingDAO.create(configMappingEntity);
        configGroupEntity.getConfigGroupConfigMappingEntities().add
          (configMappingEntity);

        configGroupEntity = configGroupDAO.merge(configGroupEntity);
      }
    }
  }

  @Override
  @Transactional
  public void delete() {
    configGroupConfigMappingDAO.removeAllByGroup(configGroupId);
    configGroupHostMappingDAO.removeAllByGroup(configGroupId);
    configGroupDAO.removeByPK(configGroupId);
    cluster.refresh();
  }

  @Override
  public void addHost(Host host) throws AmbariException {
    hostLock.writeLock().lock();
    try {
      if (m_hosts.containsKey(host.getHostId())) {
        String message = String.format(
            "Host %s is already associated with the configuration group %s", host.getHostName(),
            configGroupName);

        throw new DuplicateResourceException(message);
      }

      // ensure that we only update the in-memory structure if the merge was
      // successful
      ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
      persistHostMapping(Collections.singletonList(host), configGroupEntity);
      m_hosts.putIfAbsent(host.getHostId(), host);
    } finally {
      hostLock.writeLock().unlock();
    }
  }

  @Override
  public ConfigGroupResponse convertToResponse() throws AmbariException {
    Set<Map<String, Object>> hostnames = new HashSet<>();
    for (Host host : m_hosts.values()) {
      Map<String, Object> hostMap = new HashMap<>();
      hostMap.put("host_name", host.getHostName());
      hostnames.add(hostMap);
    }

    Set<Map<String, Object>> configObjMap = new HashSet<>();

    for (Config config : m_configurations.values()) {
      Map<String, Object> configMap = new HashMap<>();
      configMap.put(ConfigurationResourceProvider.TYPE, config.getType());
      configMap.put(ConfigurationResourceProvider.TAG, config.getTag());
      configObjMap.add(configMap);
    }

    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    ConfigGroupResponse configGroupResponse = new ConfigGroupResponse(
        configGroupEntity.getGroupId(), cluster.getClusterName(),
        configGroupEntity.getGroupName(), configGroupEntity.getTag(),
        configGroupEntity.getDescription(), hostnames, configObjMap);
    return configGroupResponse;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  public void setServiceName(String serviceName) {
    ConfigGroupEntity configGroupEntity = getConfigGroupEntity();
    configGroupEntity.setServiceName(serviceName);
    configGroupDAO.merge(configGroupEntity);

    this.serviceName = serviceName;
  }

  /**
   * Gets the {@link ConfigGroupEntity} by it's ID from the JPA cache.
   *
   * @return the entity.
   */
  private ConfigGroupEntity getConfigGroupEntity() {
    return configGroupDAO.findById(configGroupId);
  }
}
