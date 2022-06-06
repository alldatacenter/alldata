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
package org.apache.ambari.server.state.alert;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.agent.AlertDefinitionCommand;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertDefinitionHash} class is used to generate an MD5 hash for a
 * list of {@link AlertDefinitionEntity}s. It is used in order to represent the
 * state of a group of definitions by using
 * {@link AlertDefinitionEntity#getHash()}
 */
@Singleton
public class AlertDefinitionHash {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertDefinitionHash.class);

  /**
   * The hash returned when there are no definitions to hash.
   */
  public static final String NULL_MD5_HASH = "37a6259cc0c1dae299a7866489dff0bd";

  /**
   * DAO for retrieving {@link AlertDefinitionEntity} instances.
   */
  @Inject
  private AlertDefinitionDAO m_definitionDao;

  /**
   * Used to coerce {@link AlertDefinitionEntity} into {@link AlertDefinition}.
   */
  @Inject
  private AlertDefinitionFactory m_factory;

  /**
   * All clusters.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Used to add configurations to the {@link AlertDefinitionCommand} instances
   * so that alerts can be scheduled to run with access to the properties they
   * need.
   */
  @Inject
  private Provider<ConfigHelper> m_configHelper;

  /**
   * Due to the nature of the asynchronous events for alerts and Ambari, this
   * lock will ensure that only a single writer is writing to the
   * {@link ActionQueue}.
   */
  private ReentrantLock m_actionQueueLock = new ReentrantLock();

  /**
   * The hashes for all hosts for any cluster. The key is the hostname and the
   * value is a map between cluster name and hash.
   */
  private ConcurrentMap<String, ConcurrentMap<String, String>> m_hashes =
    new ConcurrentHashMap<>();

  /**
   * Gets a unique hash value reprssenting all of the alert definitions that
   * should be scheduled to run on a given host.
   * <p/>
   * This will not include alert definitions where the type is defined as
   * {@link SourceType#AGGREGATE} since aggregate definitions are not scheduled
   * to run on agent hosts.
   * <p/>
   * Hash values from this method are cached.
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the unique hash or {@value #NULL_MD5_HASH} if none.
   */
  public String getHash(String clusterName, String hostName) {
    ConcurrentMap<String, String> clusterMapping = m_hashes.get(hostName);
    if (null == clusterMapping) {
      clusterMapping = new ConcurrentHashMap<>();
      ConcurrentMap<String, String> temp = m_hashes.putIfAbsent(hostName, clusterMapping);
      if (temp != null) {
        clusterMapping = temp;
      }
    }

    String hash = clusterMapping.get(hostName);
    if (null != hash) {
      return hash;
    }

    hash = hash(clusterName, hostName);
    clusterMapping.put(clusterName, hash);

    return hash;
  }

  /**
   * Invalidate all cached hashes causing subsequent lookups to recalculate.
   */
  public void invalidateAll() {
    m_hashes.clear();
  }

  /**
   * Invalidates the cached hash for the specified agent host across all
   * clusters.
   *
   * @param hostName
   *          the host to invalidate the cache for (not {@code null}).
   */
  public void invalidate(String hostName) {
    m_hashes.remove(hostName);
  }

  /**
   * Invalidates the cached hash for the specified agent host in the specified
   * cluster.
   *
   * @param clusterName
   *          the name of the cluster (not {@code null}).
   * @param hostName
   *          the host to invalidate the cache for (not {@code null}).
   */
  public void invalidate(String clusterName, String hostName) {
    Map<String, String> clusterMapping = m_hashes.get(hostName);
    if (null != clusterMapping) {
      clusterMapping.remove(clusterName);
    }
  }

  /**
   * Gets whether the alert definition hash for the specified host has been
   * calculated and cached.
   *
   * @param hostName
   *          the host.
   * @return {@code true} if the hash was calculated; {@code false} otherwise.
   */
  public boolean isHashCached(String clusterName, String hostName) {
    if (null == clusterName || null == hostName) {
      return false;
    }

    Map<String, String> clusterMapping = m_hashes.get(hostName);
    if (null == clusterMapping) {
      return false;
    }

    return clusterMapping.containsKey(clusterName);
  }

  /**
   * Gets the alert definitions for the specified host. This will include the
   * following types of alert definitions:
   * <ul>
   * <li>Service/Component alerts</li>
   * <li>Service alerts where the host is a MASTER</li>
   * <li>Host alerts that are not bound to a service</li>
   * </ul>
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the alert definitions for the host, or an empty set (never
   *         {@code null}).
   */
  public List<AlertDefinition> getAlertDefinitions(String clusterName, String hostName) {
    return coerce(getAlertDefinitionEntities(clusterName, hostName));
  }

  public Map<Long, Map<Long, AlertDefinition>> getAlertDefinitions(Long hostId) throws AmbariException {
    Map<Long, Map<Long, AlertDefinition>> result = new HashMap<>();
    String hostName = m_clusters.get().getHostById(hostId).getHostName();
    for (Cluster cluster : m_clusters.get().getClustersForHost(hostName)) {
      List<AlertDefinition> alertDefinitions = getAlertDefinitions(cluster.getClusterName(), hostName);
      result.put(cluster.getClusterId(), mapById(alertDefinitions));
    }
    return result;
  }

  public Map<Long, AlertDefinition> findByServiceComponent(long clusterId, String serviceName, String componentName) {
    return mapById(coerce(m_definitionDao.findByServiceComponent(clusterId, serviceName, componentName)));
  }

  public Map<Long, AlertDefinition> findByServiceMaster(long clusterId, String... serviceName) {
    return mapById(coerce(m_definitionDao.findByServiceMaster(clusterId, Sets.newHashSet(serviceName))));
  }

  /**
   * Invalidate the hashes of any host that would be affected by the specified
   * definition. If the definition is an {@link SourceType#AGGREGATE}, this will
   * return an empty set since aggregates do not affect hosts.
   *
   * @param definition
   *          the definition to use to find the hosts to invlidate (not
   *          {@code null}).
   * @return the hosts that were invalidated, or an empty set (never
   *         {@code null}).
   */
  public Set<String> invalidateHosts(AlertDefinitionEntity definition) {
    return invalidateHosts(definition.getClusterId(),
        definition.getSourceType(),
        definition.getDefinitionName(), definition.getServiceName(),
        definition.getComponentName());
  }

  /**
   * Invalidate the hashes of any host that would be affected by the specified
   * definition. If the definition is an {@link SourceType#AGGREGATE}, this will
   * return an empty set since aggregates do not affect hosts.
   *
   * @param definition
   *          the definition to use to find the hosts to invlidate (not
   *          {@code null}).
   * @return the hosts that were invalidated, or an empty set (never
   *         {@code null}).
   */
  public Set<String> invalidateHosts(AlertDefinition definition) {
    return invalidateHosts(definition.getClusterId(),
        definition.getSource().getType(), definition.getName(),
        definition.getServiceName(), definition.getComponentName());
  }

  /**
   * Invalidate the hashes of any host that would be affected by the specified
   * definition. If the definition is an {@link SourceType#AGGREGATE}, this will
   * return an empty set since aggregates do not affect hosts.
   *
   * @param clusterId
   *          the cluster ID
   * @param definitionSourceType
   *          the type of alert definition
   * @param definitionName
   *          the definition unique name.
   * @param definitionServiceName
   *          the definition's service name.
   * @param definitionComponentName
   *          the definition's component name.
   * @return the hosts that were invalidated, or an empty set (never
   *         {@code null}).
   */
  private Set<String> invalidateHosts(long clusterId,
      SourceType definitionSourceType, String definitionName,
      String definitionServiceName, String definitionComponentName) {

    Cluster cluster = null;
    String clusterName = null;
    try {
      cluster = m_clusters.get().getClusterById(clusterId);
      if (null != cluster) {
        clusterName = cluster.getClusterName();
      }

      if (null == cluster) {
        LOG.warn("Unable to lookup cluster with ID {}", clusterId);
      }
    } catch (Exception exception) {
      LOG.error("Unable to lookup cluster with ID {}", clusterId, exception);
    }

    if (null == cluster) {
      return Collections.emptySet();
    }

    // determine which hosts in the cluster would be affected by a change
    // to the specified definition; pass in the definition source type
    // to check for AGGREGATE
    Set<String> affectedHosts = getAssociatedHosts(cluster,
        definitionSourceType, definitionName,
        definitionServiceName, definitionComponentName);

    // invalidate all returned hosts
    for (String hostName : affectedHosts) {
      invalidate(clusterName, hostName);
    }

    return affectedHosts;
  }

  /**
   * Gets the hosts that are associated with the specified definition. Each host
   * returned is expected to be capable of running the alert. A change to the
   * definition would entail contacting each returned host and invalidating
   * their current alert definitions.
   * <p/>
   * If the definition is an {@link SourceType#AGGREGATE}, this will return an
   * empty set since aggregates do not affect hosts.
   *
   * @param cluster
   * @param definitionName
   * @param definitionServiceName
   * @param definitionComponentName
   * @return a set of all associated hosts or an empty set, never {@code null}.
   */
  public Set<String> getAssociatedHosts(Cluster cluster,
      SourceType definitionSourceType, String definitionName,
      String definitionServiceName, String definitionComponentName) {

    if (definitionSourceType == SourceType.AGGREGATE) {
      return Collections.emptySet();
    }

    String clusterName = cluster.getClusterName();
    Map<String, Host> hosts = m_clusters.get().getHostsForCluster(clusterName);
    Set<String> affectedHosts = new HashSet<>();

    String ambariServiceName = RootService.AMBARI.name();
    String agentComponentName = RootComponent.AMBARI_AGENT.name();

    // intercept host agent alerts; they affect all hosts
    if (ambariServiceName.equals(definitionServiceName)
        && agentComponentName.equals(definitionComponentName)) {
      affectedHosts.addAll(hosts.keySet());
      return affectedHosts;
    }

    // ignore other AMBARI components as they are server-side only
    if (ambariServiceName.equalsIgnoreCase(definitionServiceName)) {
      return Collections.emptySet();
    }

    // find all hosts that have the matching service and component
    for (String hostName : hosts.keySet()) {
      List<ServiceComponentHost> hostComponents = cluster.getServiceComponentHosts(hostName);
      if (null == hostComponents || hostComponents.size() == 0) {
        continue;
      }

      // if a host has a matching service/component, invalidate it
      for (ServiceComponentHost component : hostComponents) {
        String serviceName = component.getServiceName();
        String componentName = component.getServiceComponentName();
        if (serviceName.equals(definitionServiceName)
            && componentName.equals(definitionComponentName)) {
          affectedHosts.add(hostName);
        }
      }
    }

    // get the service that this alert definition is associated with
    Map<String, Service> services = cluster.getServices();
    Service service = services.get(definitionServiceName);
    if (null == service) {
      LOG.warn("The alert definition {} has an unknown service of {}",
          definitionName, definitionServiceName);

      return affectedHosts;
    }

    // get all master components of the definition's service; any hosts that
    // run the master should be invalidated as well
    Map<String, ServiceComponent> components = service.getServiceComponents();
    if (null != components) {
      for (Entry<String, ServiceComponent> component : components.entrySet()) {
        if (component.getValue().isMasterComponent()) {
          Map<String, ServiceComponentHost> componentHosts = component.getValue().getServiceComponentHosts();
          if (null != componentHosts) {
            affectedHosts.addAll(componentHosts.keySet());
          }
        }
      }
    }

    return affectedHosts;
  }

  /**
   * Enqueue {@link AlertDefinitionCommand}s for every host in the cluster so
   * that they will receive a payload of alert definitions that they should be
   * running.
   * <p/>
   * This method is typically called after {@link #invalidateAll()} has caused a
   * cache invalidation of all alert definitions.
   *
   * @param clusterId
   *          the ID of the cluster.
   */
  public void enqueueAgentCommands(long clusterId) {
    String clusterName = null;
    Collection<String> hostNames;

    try {
      Cluster cluster = m_clusters.get().getClusterById(clusterId);
      clusterName = cluster.getClusterName();
      Collection<Host> hosts = cluster.getHosts();

      hostNames = new ArrayList<>(hosts.size());
      for (Host host : hosts) {
        hostNames.add(host.getHostName());
      }

      enqueueAgentCommands(cluster, clusterName, hostNames);
    } catch (AmbariException ae) {
      LOG.error("Unable to lookup cluster for alert definition commands", ae);
    }
  }

  /**
   * Enqueue {@link AlertDefinitionCommand}s for every host specified so that
   * they will receive a payload of alert definitions that they should be
   * running.
   * <p/>
   * This method is typically called after
   * {@link #invalidateHosts(AlertDefinitionEntity)} has caused a cache
   * invalidation of the alert definition hash.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param hosts
   *          the hosts to push {@link AlertDefinitionCommand}s for.
   */
  public void enqueueAgentCommands(long clusterId, Collection<String> hosts) {
    String clusterName = null;
    Cluster cluster = null;

    try {
      cluster = m_clusters.get().getClusterById(clusterId);
      clusterName = cluster.getClusterName();
    } catch (AmbariException ae) {
      LOG.error("Unable to lookup cluster for alert definition commands", ae);
    }

    enqueueAgentCommands(cluster, clusterName, hosts);
  }

  /**
   * Enqueue {@link AlertDefinitionCommand}s for every host specified so that
   * they will receive a payload of alert definitions that they should be
   * running.
   * <p/>
   * This method is typically called after
   * {@link #invalidateHosts(AlertDefinitionEntity)} has caused a cache
   * invalidation of the alert definition hash.
   *
   * @param clusterName
   *          the name of the cluster (not {@code null}).
   * @param hosts
   *          the hosts to push {@link AlertDefinitionCommand}s for.
   */
  private void enqueueAgentCommands(Cluster cluster, String clusterName, Collection<String> hosts) {
    if (null == clusterName) {
      LOG.warn("Unable to create alert definition agent commands because of a null cluster name");
      return;
    }

    if (null == hosts || hosts.size() == 0) {
      return;
    }

    try {
      m_actionQueueLock.lock();
      for (String hostName : hosts) {
        List<AlertDefinition> definitions = getAlertDefinitions(clusterName,
            hostName);

        String hash = getHash(clusterName, hostName);

        Host host = cluster.getHost(hostName);
        String publicHostName = host == null? hostName : host.getPublicHostName();
        AlertDefinitionCommand command = new AlertDefinitionCommand(
            clusterName, hostName, publicHostName, hash, definitions);

        try {
          command.addConfigs(m_configHelper.get(), cluster);
        } catch (AmbariException ae) {
          LOG.warn("Unable to add configurations to alert definition command",
              ae);
        }

        // TODO implement alert execution commands logic
        //m_actionQueue.enqueue(hostName, command);
      }
    } finally {
      m_actionQueueLock.unlock();
    }
  }

  /**
   * Calculates a unique hash value representing all of the alert definitions
   * that should be scheduled to run on a given host. Alerts of type
   * {@link SourceType#AGGREGATE} are not included in the hash since they are
   * not run on the agents.
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the unique hash or {@value #NULL_MD5_HASH} if none.
   */
  private String hash(String clusterName, String hostName) {
    Set<AlertDefinitionEntity> definitions = getAlertDefinitionEntities(
        clusterName,
        hostName);

    // no definitions found for this host, don't bother hashing
    if(definitions.isEmpty()) {
      return NULL_MD5_HASH;
    }

    // strip out all AGGREGATE types
    Iterator<AlertDefinitionEntity> iterator = definitions.iterator();
    while (iterator.hasNext()) {
      if (SourceType.AGGREGATE.equals(iterator.next().getSourceType())) {
        iterator.remove();
      }
    }

    // build the UUIDs
    List<String> uuids = new ArrayList<>(definitions.size());
    for (AlertDefinitionEntity definition : definitions) {
      uuids.add(definition.getHash());
    }

    // sort the UUIDs so that the digest is created with bytes in the same order
    Collections.sort(uuids);

    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      for (String uuid : uuids) {
        digest.update(uuid.getBytes());
      }

      byte[] hashBytes = digest.digest();
      return Hex.encodeHexString(hashBytes);
    } catch (NoSuchAlgorithmException nsae) {
      LOG.warn("Unable to calculate MD5 alert definition hash", nsae);
      return NULL_MD5_HASH;
    }
  }

  /**
   * Gets the alert definition entities for the specified host. This will include the
   * following types of alert definitions:
   * <ul>
   * <li>Service/Component alerts</li>
   * <li>Service alerts where the host is a MASTER except AGGREGATE alerts</li>
   * <li>Host alerts that are not bound to a service</li>
   * </ul>
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param hostName
   *          the host name (not {@code null}).
   * @return the alert definitions for the host, or an empty set (never
   *         {@code null}).
   */
  private Set<AlertDefinitionEntity> getAlertDefinitionEntities(
      String clusterName, String hostName) {

    Set<AlertDefinitionEntity> definitions = new HashSet<>();

    try {
      Cluster cluster = m_clusters.get().getCluster(clusterName);
      if (null == cluster) {
        return Collections.emptySet();
      }

      long clusterId = cluster.getClusterId();

      // services and components
      List<ServiceComponentHost> serviceComponents = cluster.getServiceComponentHosts(hostName);
      if (null == serviceComponents || !serviceComponents.isEmpty()) {
        if (serviceComponents != null) {
          for (ServiceComponentHost serviceComponent : serviceComponents) {
            String serviceName = serviceComponent.getServiceName();
            String componentName = serviceComponent.getServiceComponentName();

            // add all alerts for this service/component pair
            definitions.addAll(m_definitionDao.findByServiceComponent(clusterId, serviceName, componentName));
          }
        }

        // for every service, get the master components and see if the host
        // is a master
        Set<String> services = new HashSet<>();
        for (Entry<String, Service> entry : cluster.getServices().entrySet()) {
          Service service = entry.getValue();
          Map<String, ServiceComponent> components = service.getServiceComponents();
          for (Entry<String, ServiceComponent> component : components.entrySet()) {
            if (component.getValue().isMasterComponent()) {
              Map<String, ServiceComponentHost> hosts = component.getValue().getServiceComponentHosts();

              if (hosts.containsKey(hostName)) {
                services.add(service.getName());
              }
            }
          }
        }

        // add all service scoped alerts
        if (services.size() > 0) {
          definitions.addAll(m_definitionDao.findByServiceMaster(clusterId,
              services));
        }
      }

      // add any alerts not bound to a service (host level alerts)
      definitions.addAll(m_definitionDao.findAgentScoped(clusterId));
    }
    catch (ClusterNotFoundException clusterNotFound) {
      LOG.warn("Unable to get alert definitions for the missing cluster {}",
        clusterName);
      return Collections.emptySet();
    }
    catch (AmbariException ambariException) {
      LOG.error("Unable to get alert definitions", ambariException);
      return Collections.emptySet();
    }

    return definitions;
  }

  private List<AlertDefinition> coerce(Collection<AlertDefinitionEntity> entities) {
    return entities.stream()
      .map(m_factory::coerce)
      .collect(Collectors.toList());
  }

  private static Map<Long, AlertDefinition> mapById(Collection<AlertDefinition> definitions) {
    return definitions.stream()
      .collect(Collectors.toMap(AlertDefinition::getDefinitionId, Function.identity()));
  }

}
