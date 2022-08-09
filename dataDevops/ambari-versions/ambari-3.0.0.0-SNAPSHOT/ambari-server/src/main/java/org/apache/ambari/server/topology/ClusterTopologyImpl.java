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

package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.ALL_COMPONENTS;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;
import static org.apache.ambari.server.state.ServiceInfo.HADOOP_COMPATIBLE_FS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a cluster topology.
 * Topology includes the the associated blueprint, cluster configuration and hostgroup -> host mapping.
 */
public class ClusterTopologyImpl implements ClusterTopology {

  private Long clusterId;

  //todo: currently topology is only associated with a single bp
  //todo: this will need to change to allow usage of multiple bp's for the same cluster
  //todo: for example: provision using bp1 and scale using bp2
  private Blueprint blueprint;
  private Configuration configuration;
  private ConfigRecommendationStrategy configRecommendationStrategy;
  private ProvisionAction provisionAction = ProvisionAction.INSTALL_AND_START;
  private Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
  private final AmbariContext ambariContext;
  private final String defaultPassword;

  private final static Logger LOG = LoggerFactory.getLogger(ClusterTopologyImpl.class);


  //todo: will need to convert all usages of hostgroup name to use fully qualified name (BP/HG)
  //todo: for now, restrict scaling to the same BP
  public ClusterTopologyImpl(AmbariContext ambariContext, TopologyRequest topologyRequest) throws InvalidTopologyException {
    this.clusterId = topologyRequest.getClusterId();
    // provision cluster currently requires that all hostgroups have same BP so it is ok to use root level BP here
    this.blueprint = topologyRequest.getBlueprint();
    this.configuration = topologyRequest.getConfiguration();
    if (topologyRequest instanceof ProvisionClusterRequest) {
      this.defaultPassword = ((ProvisionClusterRequest) topologyRequest).getDefaultPassword();
    } else {
      this.defaultPassword = null;
    }

    registerHostGroupInfo(topologyRequest.getHostGroupInfo());

    this.ambariContext = ambariContext;
  }

  @Override
  public void update(TopologyRequest topologyRequest) throws InvalidTopologyException {
    registerHostGroupInfo(topologyRequest.getHostGroupInfo());
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  @Override
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfoMap;
  }

  //todo: do we want to return groups with no requested hosts?
  @Override
  public Collection<String> getHostGroupsForComponent(String component) {
    Collection<String> resultGroups = new ArrayList<>();
    for (HostGroup group : getBlueprint().getHostGroups().values() ) {
      if (group.getComponentNames().contains(component)) {
        resultGroups.add(group.getName());
      }
    }
    return resultGroups;
  }

  @Override
  public String getHostGroupForHost(String hostname) {
    for (HostGroupInfo groupInfo : hostGroupInfoMap.values() ) {
      if (groupInfo.getHostNames().contains(hostname)) {
        // a host can only be associated with a single host group
        return groupInfo.getHostGroupName();
      }
    }
    return null;
  }

  //todo: host info?
  @Override
  public void addHostToTopology(String hostGroupName, String host) throws InvalidTopologyException, NoSuchHostGroupException {
    if (blueprint.getHostGroup(hostGroupName) == null) {
      throw new NoSuchHostGroupException("Attempted to add host to non-existing host group: " + hostGroupName);
    }

    // check for host duplicates
    String groupContainsHost = getHostGroupForHost(host);
    // in case of reserved host, hostgroup will already contain host
    if (groupContainsHost != null && ! hostGroupName.equals(groupContainsHost)) {
      throw new InvalidTopologyException(String.format(
          "Attempted to add host '%s' to hostgroup '%s' but it is already associated with hostgroup '%s'.",
          host, hostGroupName, groupContainsHost));
    }

    synchronized(hostGroupInfoMap) {
      HostGroupInfo existingHostGroupInfo = hostGroupInfoMap.get(hostGroupName);
      if (existingHostGroupInfo == null) {
        throw new RuntimeException(String.format("An attempt was made to add host '%s' to an unregistered hostgroup '%s'",
            host, hostGroupName));
      }
      // ok to add same host multiple times to same group
      existingHostGroupInfo.addHost(host);

      LOG.info("ClusterTopologyImpl.addHostTopology: added host = " + host + " to host group = " + existingHostGroupInfo.getHostGroupName());
    }
  }

  @Override
  public Set<String> getAllHosts() {
    return hostGroupInfoMap.values().stream().flatMap(hg -> hg.getHostNames().stream()).collect(toSet());
  }

  @Override
  public Collection<String> getHostAssignmentsForComponent(String component) {
    //todo: ordering requirements?
    Collection<String> hosts = new ArrayList<>();
    Collection<String> hostGroups = getHostGroupsForComponent(component);
    for (String group : hostGroups) {
      HostGroupInfo hostGroupInfo = getHostGroupInfo().get(group);
      if (hostGroupInfo != null) {
        hosts.addAll(hostGroupInfo.getHostNames());
      } else {
        LOG.warn("HostGroup {} not found, when checking for hosts for component {}", group, component);
      }
    }
    return hosts;
  }

  @Override
  public boolean isNameNodeHAEnabled() {
    return isNameNodeHAEnabled(configuration.getFullProperties());
  }

  public static boolean isNameNodeHAEnabled(Map<String, Map<String, String>> configurationProperties) {
    return configurationProperties.containsKey("hdfs-site") &&
           (configurationProperties.get("hdfs-site").containsKey("dfs.nameservices") ||
            configurationProperties.get("hdfs-site").containsKey("dfs.internal.nameservices"));
  }

  @Override
  public boolean isYarnResourceManagerHAEnabled() {
    return isYarnResourceManagerHAEnabled(configuration.getFullProperties());
  }

  /**
   * Static convenience function to determine if Yarn ResourceManager HA is enabled
   * @param configProperties configuration properties for this cluster
   * @return true if Yarn ResourceManager HA is enabled
   *         false if Yarn ResourceManager HA is not enabled
   */
  static boolean isYarnResourceManagerHAEnabled(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("yarn-site") && configProperties.get("yarn-site").containsKey("yarn.resourcemanager.ha.enabled")
      && configProperties.get("yarn-site").get("yarn.resourcemanager.ha.enabled").equals("true");
  }

  @Override
  public boolean isClusterKerberosEnabled() {
    return ambariContext.isClusterKerberosEnabled(getClusterId());
  }

  @Override
  public RequestStatusResponse installHost(String hostName, boolean skipInstallTaskCreate, boolean skipFailure) {
    try {
      String hostGroupName = getHostGroupForHost(hostName);
      HostGroup hostGroup = this.blueprint.getHostGroup(hostGroupName);

      Collection<String> skipInstallForComponents = new ArrayList<>();
      if (skipInstallTaskCreate) {
        skipInstallForComponents.add(ALL_COMPONENTS);
      } else {
        // get the set of components that are marked as START_ONLY for this hostgroup
        skipInstallForComponents.addAll(hostGroup.getComponentNames(ProvisionAction.START_ONLY));
      }

      Collection<String> dontSkipInstallForComponents = hostGroup.getComponentNames(INSTALL_ONLY);
      dontSkipInstallForComponents.addAll(hostGroup.getComponentNames(INSTALL_AND_START));

      return ambariContext.installHost(hostName, ambariContext.getClusterName(getClusterId()),
        skipInstallForComponents, dontSkipInstallForComponents, skipFailure);
    } catch (AmbariException e) {
      LOG.error("Cannot get cluster name for clusterId = " + getClusterId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public RequestStatusResponse startHost(String hostName, boolean skipFailure) {
    try {
      String hostGroupName = getHostGroupForHost(hostName);
      HostGroup hostGroup = this.blueprint.getHostGroup(hostGroupName);

      // get the set of components that are marked as INSTALL_ONLY
      // for this hostgroup
      Collection<String> installOnlyComponents =
        hostGroup.getComponentNames(ProvisionAction.INSTALL_ONLY);

      return ambariContext.startHost(hostName, ambariContext.getClusterName(getClusterId()), installOnlyComponents, skipFailure);
    } catch (AmbariException e) {
      LOG.error("Cannot get cluster name for clusterId = " + getClusterId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setConfigRecommendationStrategy(ConfigRecommendationStrategy strategy) {
    this.configRecommendationStrategy = strategy;
  }

  @Override
  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return this.configRecommendationStrategy;
  }

  @Override
  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  @Override
  public void setProvisionAction(ProvisionAction provisionAction) {
    this.provisionAction = provisionAction;
  }

  @Override
  public Map<String, AdvisedConfiguration> getAdvisedConfigurations() {
    return this.advisedConfigurations;
  }

  @Override
  public AmbariContext getAmbariContext() {
    return ambariContext;
  }

  @Override
  public void removeHost(String hostname) {
    for(Map.Entry<String,HostGroupInfo> entry : hostGroupInfoMap.entrySet()) {
      entry.getValue().removeHost(hostname);
    }
  }

  @Override
  public String getDefaultPassword() {
    return defaultPassword;
  }

  @Override
  public boolean isComponentHadoopCompatible(String component) {
    return blueprint.getServiceInfos().stream()
      .filter(service -> service.getComponentByName(component) != null)
      .findFirst()
      .map(service -> HADOOP_COMPATIBLE_FS.equals(service.getServiceType()))
      .orElse(false);
  }

  private void registerHostGroupInfo(Map<String, HostGroupInfo> requestedHostGroupInfoMap) throws InvalidTopologyException {
    LOG.debug("Registering requested host group information for {} hostgroups", requestedHostGroupInfoMap.size());
    checkForDuplicateHosts(requestedHostGroupInfoMap);

    for (HostGroupInfo requestedHostGroupInfo : requestedHostGroupInfoMap.values()) {
      String hostGroupName = requestedHostGroupInfo.getHostGroupName();

      //todo: doesn't support using a different blueprint for update (scaling)
      HostGroup baseHostGroup = getBlueprint().getHostGroup(hostGroupName);

      if (baseHostGroup == null) {
        throw new IllegalArgumentException("Invalid host_group specified: " + hostGroupName +
            ".  All request host groups must have a corresponding host group in the specified blueprint");
      }
      //todo: split into two methods
      HostGroupInfo currentHostGroupInfo = hostGroupInfoMap.get(hostGroupName);
      if (currentHostGroupInfo == null) {
        // blueprint host group config
        Configuration bpHostGroupConfig = baseHostGroup.getConfiguration();
        // parent config is BP host group config but with parent set to topology cluster scoped config
        Configuration parentConfiguration = new Configuration(bpHostGroupConfig.getProperties(),
            bpHostGroupConfig.getAttributes(), getConfiguration());

        requestedHostGroupInfo.getConfiguration().setParentConfiguration(parentConfiguration);
        hostGroupInfoMap.put(hostGroupName, requestedHostGroupInfo);
      } else {
        // Update.  Either add hosts or increment request count
        if (!requestedHostGroupInfo.getHostNames().isEmpty()) {
          try {
            // this validates that hosts aren't already registered with groups
            addHostsToTopology(requestedHostGroupInfo);
          } catch (NoSuchHostGroupException e) {
            //todo
            throw new InvalidTopologyException("Attempted to add hosts to unknown host group: " + hostGroupName);
          }
        } else {
          currentHostGroupInfo.setRequestedCount(
              currentHostGroupInfo.getRequestedHostCount() + requestedHostGroupInfo.getRequestedHostCount());
        }
        //todo: throw exception in case where request attempts to modify HG configuration in scaling operation
      }
    }
  }

  private void addHostsToTopology(HostGroupInfo hostGroupInfo) throws InvalidTopologyException, NoSuchHostGroupException {
    for (String host: hostGroupInfo.getHostNames()) {
      registerRackInfo(hostGroupInfo, host);
      addHostToTopology(hostGroupInfo.getHostGroupName(), host);
    }
  }

  private void registerRackInfo(HostGroupInfo hostGroupInfo, String host) {
    synchronized (hostGroupInfoMap) {
      HostGroupInfo cachedHGI = hostGroupInfoMap.get(hostGroupInfo.getHostGroupName());
      if (null != cachedHGI) {
        cachedHGI.addHostRackInfo(host, hostGroupInfo.getHostRackInfo().get(host));
      }
    }
  }


  private void checkForDuplicateHosts(Map<String, HostGroupInfo> groupInfoMap) throws InvalidTopologyException {
    Set<String> hosts = new HashSet<>();
    Set<String> duplicates = new HashSet<>();
    for (HostGroupInfo group : groupInfoMap.values()) {
      // check for duplicates within the new groups
      Collection<String> groupHosts = group.getHostNames();
      Collection<String> groupHostsCopy = new HashSet<>(group.getHostNames());
      groupHostsCopy.retainAll(hosts);
      duplicates.addAll(groupHostsCopy);
      hosts.addAll(groupHosts);

      // check against existing groups
      for (String host : groupHosts) {
        if (getHostGroupForHost(host) != null) {
          duplicates.add(host);
        }
      }
    }
    if (! duplicates.isEmpty()) {
      throw new InvalidTopologyException("The following hosts are mapped to multiple host groups: " + duplicates + "." +
        " Be aware that host names are converted to lowercase, case differences do not matter in Ambari deployments.");
    }
  }
}
