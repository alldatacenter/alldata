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
package org.apache.ambari.spi.upgrade;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.net.HttpURLConnectionProvider;

/**
 * Represents a request to run the upgrade checks before an upgrade begins.
 */
public class UpgradeCheckRequest {
  private final ClusterInformation m_clusterInformation;
  private final UpgradeType m_upgradeType;
  private boolean m_revert = false;
  private final RepositoryVersion m_targetRepositoryVersion;
  private final Map<String,String> m_checkConfigurations;
  private final HttpURLConnectionProvider m_httpURLConnectionProvider;

  /**
   * Used for tracking results during a check request.
   */
  private Map<UpgradeCheckDescription, UpgradeCheckStatus> m_results = new HashMap<>();

  /**
   * Constructor.
   *
   * @param clusterInformation
   *          the name of a cluster along with its hosts, services, and
   *          topology.
   * @param upgradeType
   *          the type of the upgrade.
   * @param targetRepositoryVersion
   *          the target repository version for the upgrade.
   * @param checkConfigurations
   *          any configurations specified in the upgrade pack which can be used
   *          to when
   * @param httpURLConnectionProvider
   *          provides a mechanism for an {@link UpgradeCheck} to make URL
   *          requests while using Ambari's truststore and configured stream
   *          timeout settings.
   */
  public UpgradeCheckRequest(ClusterInformation clusterInformation, UpgradeType upgradeType,
      RepositoryVersion targetRepositoryVersion, Map<String, String> checkConfigurations,
      HttpURLConnectionProvider httpURLConnectionProvider) {
    m_clusterInformation = clusterInformation;
    m_upgradeType = upgradeType;
    m_targetRepositoryVersion = targetRepositoryVersion;
    m_checkConfigurations = checkConfigurations;
    m_httpURLConnectionProvider = httpURLConnectionProvider;
  }

  /**
   * Gets information about the cluster's current state, such as configurations,
   * topology, and security.
   *
   * @return information about the cluster.
   */
  public ClusterInformation getClusterInformation() {
    return m_clusterInformation;
  }

  /**
   * Gets the name of the cluster from the {@link ClusterInformation} instance.
   *
   * @return  the cluster name.
   */
  public String getClusterName() {
    return m_clusterInformation.getClusterName();
  }

  public UpgradeType getUpgradeType() {
    return m_upgradeType;
  }

  /**
   * Gets the target repository version for the upgrade.
   *
   * @return  the target for the upgrade.
   */
  public RepositoryVersion getTargetRepositoryVersion() {
    return m_targetRepositoryVersion;
  }

  /**
   * @param revert
   *          {@code true} if the check is for a patch reversion
   */
  public void setRevert(boolean revert) {
    m_revert = revert;
  }

  /**
   * @return if the check is for a patch reversion
   */
  public boolean isRevert() {
    return m_revert;
  }

  /**
   * Gets any configurations defined in the upgrade pack which can be passed into the checks.
   *
   * @return  configurations defined in the upgrade pack, if any.
   */
  public Map<String, String> getCheckConfigurations() {
    return m_checkConfigurations;
  }

  /**
   * Sets the result of a check.
   * @param description the description
   * @param status      the status result
   */
  public void addResult(UpgradeCheckDescription description, UpgradeCheckStatus status) {
    m_results.put(description, status);
  }

  /**
   * Gets the result of a check of the supplied description
   * @param description the description
   * @return the return value, or {@code null} if it has not been run
   */
  public UpgradeCheckStatus getResult(UpgradeCheckDescription description) {
    return m_results.get(description);
  }

  /**
   * Gets a class which can construct {@link HttpURLConnection} instances which
   * are backed by Ambari's cookie store, truststore, and timeout settings.
   *
   * @return the httpURLConnectionProvider an instance of the provider.
   */
  public HttpURLConnectionProvider getHttpURLConnectionProvider() {
    return m_httpURLConnectionProvider;
  }
}
