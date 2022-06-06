/**
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
package org.apache.ambari.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link ClusterInformation} class is used to pass the state of the cluster
 * as simple primitive values and collections. It contains the following types of information:
 * <ul>
 * <li>The name of a cluster
 * <li>The current desired configurations of a cluster
 * <li>The hosts where services and components are installed
 * <li>The security state of the cluster
 * </ul>
 */
public class ClusterInformation {

  /**
   * The cluster's current configurations.
   */
  private final Map<String, Map<String, String>> m_configurations;

  /**
   * The name of the cluster.
   */
  private final String m_clusterName;

  /**
   * {@code true} if the cluster is kerberized.
   */
  private final boolean m_isKerberosEnabled;

  /**
   * A simple representation of the cluster topology where the key is the
   * combination of service/component and the value is the set of hosts.
   */
  private final Map<String, Set<String>> m_topology;

  /**
   * The current version of every service in the cluster.
   */
  private final Map<String, RepositoryVersion> m_serviceVersions;

  /**
   * Constructor.
   *
   * @param clusterName
   *          the name of the cluster.
   * @param isKerberosEnabled
   *          {@code true} if the cluster is Kerberized.
   * @param configurations
   *          a mapping of configuration type (such as foo-site) to the specific
   *          configurations (such as http.port : 8080).
   * @param topology
   *          a mapping of the cluster topology where the key is a combination
   *          of service / component and the value is the hosts where it is
   *          installed.
   * @param serviceVersions
   *          the current repository version for every service in the cluster.
   */
  public ClusterInformation(String clusterName, boolean isKerberosEnabled,
      Map<String, Map<String, String>> configurations, Map<String, Set<String>> topology,
      Map<String, RepositoryVersion> serviceVersions) {
    m_configurations = configurations;
    m_clusterName = clusterName;
    m_isKerberosEnabled = isKerberosEnabled;
    m_topology = topology;
    m_serviceVersions = serviceVersions;
  }

  /**
   * Gets the cluster name.
   *
   * @return the cluster name.
   */
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * Gets whether the cluster is Kerberized.
   *
   * @return {@code true} if the cluster is Kerberized.
   */
  public boolean isKerberosEnabled() {
    return m_isKerberosEnabled;
  }

  /**
   * Gets any hosts where the matching service and component are installed.
   *
   * @param serviceName
   *          the service name
   * @param componentName
   *          the component name
   * @return the set of hosts where the component is installed, or an empty set.
   */
  public Set<String> getHosts(String serviceName, String componentName) {
    Set<String> hosts = m_topology.get(serviceName + "/" + componentName);
    if (null == hosts) {
      hosts = new HashSet<>();
    }

    return hosts;
  }

  /**
   * Gets the configuration properties for the specified type. If the type does
   * not exist, this will return {@code null}.
   *
   * @param configurationType
   *          the configuration type to retrieve.
   * @return the property name and value pairs for the configuration type, or
   *         {@code null} if no configuration type exists.
   */
  public Map<String, String> getConfigurationProperties(String configurationType) {
    return m_configurations.get(configurationType);
  }

  /**
   * Gets a configuration value given the type and property name.
   *
   * @param configurationType
   *          the configuration type, such as foo-site.
   * @param propertyName
   *          the property name, such as http.port
   * @return the property value, or {@code null} if it does not exist.
   */
  public String getConfigurationProperty(String configurationType, String propertyName) {
    Map<String, String> configType = getConfigurationProperties(configurationType);
    if (null == configType) {
      return null;
    }

    return configType.get(propertyName);
  }

  /**
   * Gets the {@link RepositoryVersion} for the given service which represents
   * the service's current version in the cluster.
   *
   * @param serviceName
   *          the service name.
   * @return the repository version information for the given service.
   */
  public RepositoryVersion getServiceRepositoryVersion(String serviceName) {
    return m_serviceVersions.get(serviceName);
  }

  /**
   * Gets the services installed in the cluster.
   *
   * @return the services in the cluster.
   */
  public Set<String> getServices() {
    return m_serviceVersions.keySet();
  }
}
