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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ProvisionAction;

/**
 * Represents a full cluster topology including all instance information as well as the associated
 * blueprint which provides all abstract topology information.
 */
public interface ClusterTopology {

  /**
   * Get the id of the cluster.
   *
   * @return cluster id
   */
  Long getClusterId();

  /**
   * Set the id of the cluster.
   *
   * @param clusterId cluster id
   */
  void setClusterId(Long clusterId);

  /**
   * Get the blueprint associated with the cluster.
   *
   * @return assocaited blueprint
   */
  Blueprint getBlueprint();

  /**
   * Get the cluster scoped configuration for the cluster.
   * This configuration has the blueprint cluster scoped
   * configuration set as it's parent.
   *
   * @return cluster scoped configuration
   */
  Configuration getConfiguration();

  /**
   * Get host group information.
   *
   * @return map of host group name to host group information
   */
  Map<String, HostGroupInfo> getHostGroupInfo();

  /**
   * @return all hosts in the topology
   */
  Set<String> getAllHosts();
  /**
   * Get the names of  all of host groups which contain the specified component.
   *
   * @param component  component name
   *
   * @return collection of host group names which contain the specified component
   */
  Collection<String> getHostGroupsForComponent(String component);

  /**
   * Get the name of the host group which is mapped to the specified host.
   *
   * @param hostname  host name
   *
   * @return name of the host group which is mapped to the specified host or null if
   *         no group is mapped to the host
   */
  String getHostGroupForHost(String hostname);

  /**
   * Get all hosts which are mapped to a host group which contains the specified component.
   * The host need only to be mapped to the hostgroup, not actually provisioned.
   *
   * @param component  component name
   *
   * @return collection of hosts for the specified component; will not return null
   */
  Collection<String> getHostAssignmentsForComponent(String component);

  /**
   * Update the existing topology based on the provided topology request.
   *
   * @param topologyRequest  request modifying the topology
   *
   * @throws InvalidTopologyException if the request specified invalid topology information or if
   *                                  making the requested changes would result in an invalid topology
   */
  void update(TopologyRequest topologyRequest) throws InvalidTopologyException;

  /**
   * Add a new host to the topology.
   *
   * @param hostGroupName  name of associated host group
   * @param host           name of host
   *
   * @throws InvalidTopologyException if the host being added is already registered to a different host group
   * @throws NoSuchHostGroupException if the specified host group is invalid
   */
  void addHostToTopology(String hostGroupName, String host) throws InvalidTopologyException, NoSuchHostGroupException;

  /**
   * Determine if NameNode HA is enabled.
   *
   * @return true if NameNode HA is enabled; false otherwise
   */
  boolean isNameNodeHAEnabled();

  /**
   * Determine if Yarn ResourceManager HA is enabled.
   *
   * @return true if Yarn ResourceManager HA is enabled; false otherwise
   */
  boolean isYarnResourceManagerHAEnabled();

  /**
   * Determine if the cluster is kerberos enabled.
   *
   * @return true if the cluster is kerberos enabled; false otherwise
   */
  boolean isClusterKerberosEnabled();

  /**
   * Install the specified host.
   *
   * @param hostName  host name
   * @param skipInstallTaskCreate
   * @return install response
   */
  RequestStatusResponse installHost(String hostName, boolean skipInstallTaskCreate, boolean skipFailure);

  /**
   * Start the specified host.
   *
   * @param hostName  host name
   * @return start response
   */
  RequestStatusResponse startHost(String hostName, boolean skipFailure);

  void setConfigRecommendationStrategy(ConfigRecommendationStrategy strategy);

  ConfigRecommendationStrategy getConfigRecommendationStrategy();

  /**
   * Set request provision action : INSTALL vs INSTALL_AND_START
   * @param provisionAction @ProvisionAction
   */
  void setProvisionAction(ProvisionAction provisionAction);

  ProvisionAction getProvisionAction();

  Map<String, AdvisedConfiguration> getAdvisedConfigurations();

  //todo: don't expose ambari context from this class
  AmbariContext getAmbariContext();

  /**
   * Removes host from stateful ClusterTopology
   * @param hostname
   */
  void removeHost(String hostname);

  String getDefaultPassword();

  /**
   * @return true if the given component belongs to a service that has serviceType=HCFS
   */
  boolean isComponentHadoopCompatible(String component);
}
