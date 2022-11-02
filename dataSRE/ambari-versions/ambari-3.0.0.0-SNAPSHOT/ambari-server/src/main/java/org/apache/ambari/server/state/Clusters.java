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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;

/**
 * Single entity that tracks all clusters and hosts that are managed
 * by the Ambari server
 */
public interface Clusters {

  /**
   * Add a new Cluster
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param stackId
   *          the stack for the cluster (not {@code null}).
   */
  void addCluster(String clusterName, StackId stackId)
      throws AmbariException;

  /**
   * Add a new cluster
   * @param clusterName
   *          the cluster name (not {@code null}).
   * @param stackId
   *          the stack for the cluster (not {@code null}).
   * @param securityType
   *          the cluster will be created with this security type.
   * @throws AmbariException
   */
  void addCluster(String clusterName, StackId stackId, SecurityType securityType)
    throws AmbariException;

  /**
   * Gets the Cluster given the cluster name
   * @param clusterName Name of the Cluster to retrieve
   * @return  <code>Cluster</code> identified by the given name
   */
  Cluster getCluster(String clusterName)
      throws AmbariException;

  /**
   * Gets the Cluster given the cluster id
   * @param clusterId Id of the Cluster to retrieve
   * @return  <code>Cluster</code> identified by the given id
   */
  Cluster getCluster(Long clusterId)
    throws AmbariException;

  /**
   * Get all clusters
   * @return <code>Map</code> of clusters with cluster name as key
   */
  Map<String, Cluster> getClusters();

  /**
   * Get all hosts being tracked by the Ambari server
   * @return <code>List</code> of <code>Host</code>
   */
  List<Host> getHosts();

  /**
   * Returns all the cluster names for this hostname
   * @param hostname
   * @return List of cluster names
   * @throws AmbariException
   */
  Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException;


  /**
   * Get a Host object managed by this server
   * @param hostname Name of the host requested
   * @return Host object
   * @throws AmbariException
   */
  Host getHost(String hostname) throws AmbariException;

  /**
   * Check if host exists
   * @param hostname Name of the host requested
   * @return is host exists
   */
  boolean hostExists(String hostname);

  /**
   * Gets whether the specified cluster has a mapping for the specified host.
   *
   * @param clusterId the cluster ID
   * @param hostName
   *          the host (not {@code null}).
   * @return {@code true} if the host belongs to the cluster, {@code false}
   *         otherwise.
   */
  boolean isHostMappedToCluster(long clusterId, String hostName);

  /**
   * Get a Host object managed by this server
   * @param hostId Host Id from the {@link org.apache.ambari.server.orm.entities.HostEntity} objecty
   * @return Host object
   * @throws AmbariException
   */
  Host getHostById(Long hostId) throws AmbariException;

  /**
   * Updates the internal mappings of hosts using the specified host.
   *
   * @param host
   *          the host to update the internal mappings for.
   */
  void updateHostMappings(Host host);

  /**
   * Add a Host object to be managed by this server
   * @param hostname Host to be added
   * @throws AmbariException
   */
  void addHost(String hostname) throws AmbariException;

  /**
   * Map host to the given cluster.
   * A host can belong to multiple clusters
   * @param hostname
   * @param clusterName
   * @throws AmbariException
   */
  void mapHostToCluster(String hostname, String clusterName)
      throws AmbariException;

  /**
   * Maps a set of hosts to the given cluster
   * @param hostnames
   * @param clusterName
   * @throws AmbariException
   */
  void mapAndPublishHostsToCluster(Set<String> hostnames, String clusterName)
      throws AmbariException;

  /**
   * Updates the name of the cluster
   * @param oldName
   * @param newName
   * @throws AmbariException
   */
  void updateClusterName(String oldName, String newName);

  /**
   * Gets the cluster using the id.
   * @param id The identifier associated with the cluster
   * @return <code>Cluster</code> identified by the identifier
   * @throws AmbariException
   */
  Cluster getClusterById(long id) throws AmbariException;

  /**
   * Produces a debug dump into the supplied string buffer
   * @param sb The string buffer to add the debug dump to
   */
  void debugDump(StringBuilder sb);

  /**
   * Gets all the hosts associated with the cluster
   * @param clusterName The name of the cluster
   * @return <code>Map</code> containing host name and <code>Host</code>
   */
  Map<String, Host> getHostsForCluster(String clusterName);

  /**
   * Gets all the host Ids associated with the cluster
   * @param clusterName The name of the cluster
   * @return <code>Map</code> containing host id and <code>Host</code>
   * @throws AmbariException
   */
  Map<Long, Host> getHostIdsForCluster(String clusterName)
      throws AmbariException;

  /**
   * Deletes the cluster identified by the name
   * @param clusterName The name of the cluster
   * @throws AmbariException
   */
  void deleteCluster(String clusterName)
      throws AmbariException;

  /**
   * Update the host set for clusters and the host attributes associated with the hosts
   * @param hostsClusters
   * @param hostAttributes
   * @throws AmbariException
   */
  void updateHostWithClusterAndAttributes(
      Map<String, Set<String>> hostsClusters, Map<String, Map<String, String>> hostAttributes)
      throws AmbariException;

  /**
   * Removes a host from a cluster.  Inverts {@link #mapHostToCluster(String, String)
   * @param hostname
   * @param clusterName
   */
  void unmapHostFromCluster(String hostname, String clusterName)
      throws AmbariException;

  /**
   * Removes a host.  Inverts {@link #addHost(String)}
   * @param hostname
   */
  void deleteHost(String hostname) throws AmbariException;

  /**
   * Publish event set of hosts were removed
   */
  void publishHostsDeletion(Set<Long> hostIds, Set<String> hostNames)
      throws AmbariException;

  /**
   * Determine whether or not access to the cluster resource identified
   * by the given cluster name should be allowed based on the permissions
   * granted to the current user.
   *
   * @param clusterName  the cluster name
   * @param readOnly     indicate whether or not this check is for a read only operation
   *
   * @return true if access to the cluster is allowed
   */
  boolean checkPermission(String clusterName, boolean readOnly);

  /**
   * Add the given map of attributes to the session for the cluster identified by the given name.
   *
   * @param name        the cluster name
   * @param attributes  the session attributes
   */
  void addSessionAttributes(String name, Map<String, Object> attributes);

  /**
   * Get the map of session attributes for the cluster identified by the given name.
   *
   * @param name  the cluster name
   *
   * @return the map of session attributes for the cluster; never null
   */
  Map<String, Object> getSessionAttributes(String name);

  /**
   * Returns the number of hosts that form the cluster identified by the given name.
   * @param clusterName the name that identifies the cluster
   * @return  number of hosts that form the cluster
   */
  int getClusterSize(String clusterName);

  /**
   * Invalidates the specified cluster by retrieving it from the database and
   * refreshing all of the internal stateful collections.
   *
   * @param cluster
   *          the cluster to invalidate and refresh (not {@code null}).
   */
  void invalidate(Cluster cluster);

  /**
   * Invalidates all clusters by retrieving each from the database and refreshing all of its internal
   * stateful collections.
   */
  void invalidateAllClusters();
}
