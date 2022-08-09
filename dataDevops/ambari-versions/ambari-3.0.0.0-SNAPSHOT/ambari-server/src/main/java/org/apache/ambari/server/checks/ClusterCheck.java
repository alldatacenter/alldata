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
package org.apache.ambari.server.checks;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.CheckQualification;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A base implementation of {@link UpgradeCheck} which offers injected helpers and access
 * to information about repositories and the cluster.
 */
public abstract class ClusterCheck implements UpgradeCheck {

  @Inject
  protected Provider<Clusters> clustersProvider;

  @Inject
  Provider<HostVersionDAO> hostVersionDaoProvider;

  @Inject
  Provider<RepositoryVersionDAO> repositoryVersionDaoProvider;

  @Inject
  Provider<UpgradeDAO> upgradeDaoProvider;

  @Inject
  Provider<RepositoryVersionHelper> repositoryVersionHelper;

  @Inject
  Provider<AmbariMetaInfo> ambariMetaInfo;

  @Inject
  Configuration config;

  @Inject
  Gson gson;

  @Inject
  Provider<CheckHelper> checkHelperProvider;

  private UpgradeCheckDescription m_description;

  /**
   * Constructor.
   *
   * @param description description
   */
  protected ClusterCheck(UpgradeCheckDescription description) {
    m_description = description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckDescription getCheckDescription() {
    return m_description;
  }

  /**
   * Gets the set of services that this check is associated with. If the check
   * is not associated with a particular service, then this should be an empty
   * set.
   *
   * @return a set of services which will determine whether this check is
   *         applicable.
   */
  @Override
  public Set<String> getApplicableServices() {
    return Collections.emptySet();
  }

  /**
   * Gets any additional qualifications which an upgrade check should run in
   * order to determine if it's applicable to the upgrade.
   *
   * @return a list of qualifications, or an empty list.
   */
  @Override
  public List<CheckQualification> getQualifications() {
    return Collections.emptyList();
  }

  /**
   * Gets the description of the check.
   *
   * @return the description (not {@code null}).
   */
  public UpgradeCheckDescription getDescription() {
    return m_description;
  }

  /**
   * Gets the default fail reason
   * @param upgradeCheckResult the check being performed
   * @param request           the request
   * @return the failure string
   */
  protected String getFailReason(UpgradeCheckResult upgradeCheckResult, UpgradeCheckRequest request)
      throws AmbariException {
    return getFailReason(UpgradeCheckDescription.DEFAULT, upgradeCheckResult, request);
  }

  /**
   * Gets a cluster configuration property if it exists, or {@code null}
   * otherwise.
   *
   * @param request
   *          the request (not {@code null}).
   * @param configType
   *          the configuration type, such as {@code hdfs-site} (not
   *          {@code null}).
   * @param propertyName
   *          the name of the property (not {@code null}).
   * @return the property value or {@code null} if not found.
   * @throws AmbariException
   */
  protected String getProperty(UpgradeCheckRequest request, String configType, String propertyName)
      throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    final DesiredConfig desiredConfig = desiredConfigs.get(configType);

    if (null == desiredConfig) {
      return null;
    }

    final Config config = cluster.getConfig(configType, desiredConfig.getTag());

    Map<String, String> properties = config.getProperties();
    return properties.get(propertyName);
  }

  /**
   * Gets the fail reason
   * @param key               the failure text key
   * @param UpgradeCheck the check being performed
   * @param request           the request
   * @return the failure string
   */
  protected String getFailReason(String key, UpgradeCheckResult upgradeCheckResult,
      UpgradeCheckRequest request) throws AmbariException {
    String fail = m_description.getFailureReason(key);

    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();
    if (fail.contains("{{version}}") && null != repositoryVersion) {
      fail = fail.replace("{{version}}", repositoryVersion.getVersion());
    }

    if (fail.contains("{{fails}}")) {
      LinkedHashSet<String> names = upgradeCheckResult.getFailedOn();

      // If Type=PrereqCheckType.HOST, names list is already populated
      if (getDescription().getType() == UpgradeCheckType.SERVICE) {
        Clusters clusters = clustersProvider.get();
        AmbariMetaInfo metaInfo = ambariMetaInfo.get();

        Cluster c = clusters.getCluster(request.getClusterName());
        Map<String, ServiceInfo> services = metaInfo.getServices(
            c.getDesiredStackVersion().getStackName(),
            c.getDesiredStackVersion().getStackVersion());

        LinkedHashSet<String> displays = new LinkedHashSet<>();
        for (String name : names) {
          if (services.containsKey(name)) {
            displays.add(services.get(name).getDisplayName());
          } else {
            displays.add(name);
          }
        }
        names = displays;

      }

      fail = fail.replace("{{fails}}", formatEntityList(names));
    }

    return fail;
  }

  /**
   * Formats lists of given entities to human readable form:
   * [entity1] -> {entity1} {noun}
   * [entity1, entity2] -> {entity1} and {entity2} {noun}s
   * [entity1, entity2, entity3] -> {entity1}, {entity2} and {entity3} {noun}s
   * The noun for the entities is taken from check type, it may be cluster, service or host.
   *
   * @param entities list of entities to format
   * @return formatted entity list
   */
  protected String formatEntityList(LinkedHashSet<String> entities) {
    if (entities == null || entities.isEmpty()) {
      return "";
    }

    final StringBuilder formatted = new StringBuilder(StringUtils.join(entities, ", "));
    if (entities.size() > 1) {
      formatted.replace(formatted.lastIndexOf(","), formatted.lastIndexOf(",") + 1, " and");
    }

    return formatted.toString();
  }

  /**
   * Used to represent information about a service. This class is safe to use in
   * sorted & unique collections.
   */
  static class ServiceDetail implements Comparable<ServiceDetail> {
    @JsonProperty("service_name")
    final String serviceName;

    ServiceDetail(String serviceName) {
      this.serviceName = serviceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ServiceDetail other = (ServiceDetail) obj;
      return Objects.equals(serviceName, other.serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ServiceDetail other) {
      return serviceName.compareTo(other.serviceName);
    }
  }

  /**
   * Used to represent information about a service component. This class is safe
   * to use in sorted & unique collections.
   */
  static class ServiceComponentDetail implements Comparable<ServiceComponentDetail> {
    @JsonProperty("service_name")
    final String serviceName;

    @JsonProperty("component_name")
    final String componentName;

    ServiceComponentDetail(String serviceName, String componentName) {
      this.serviceName = serviceName;
      this.componentName = componentName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(serviceName, componentName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ServiceComponentDetail other = (ServiceComponentDetail) obj;
      return Objects.equals(serviceName, other.serviceName)
          && Objects.equals(componentName, other.componentName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ServiceComponentDetail other) {
      return Comparator.comparing(
          (ServiceComponentDetail detail) -> detail.serviceName).thenComparing(
              detail -> detail.componentName).compare(this, other);
    }
  }

  /**
   * Used to represent information about a host. This class is safe to use in
   * sorted & unique collections.
   */
  static class HostDetail implements Comparable<HostDetail> {
    @JsonProperty("host_id")
    final Long hostId;

    @JsonProperty("host_name")
    final String hostName;

    HostDetail(Long hostId, String hostName) {
      this.hostId = hostId;
      this.hostName = hostName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(hostId, hostName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      HostDetail other = (HostDetail) obj;
      return Objects.equals(hostId, other.hostId) && Objects.equals(hostName, other.hostName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HostDetail other) {
      return hostName.compareTo(other.hostName);
    }
  }
}
