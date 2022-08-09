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
package org.apache.ambari.spi.upgrade;

import java.util.Map;

import org.apache.ambari.spi.RepositoryVersion;

/**
 * The {@link UpgradeInformation} class contains information about a running
 * upgrade or downgrade.
 */
public class UpgradeInformation {

  /**
   * {@code true} if the direction is upgrade, {@code false} if it is a
   * downgrade.
   */
  private final boolean m_isUpgrade;

  /**
   * The orchestration type of the upgrade.
   */
  private final UpgradeType m_upgradeType;

  /**
   * The target version of the upgrade or downgrade.
   */
  private final RepositoryVersion m_targetVersion;

  /**
   * The source version of every service in the upgrade.
   */
  private final Map<String, RepositoryVersion> m_sourceVersions;

  /**
   * The target version of every service in the upgrade.
   */
  private final Map<String, RepositoryVersion> m_targetVersions;

  /**
   * Constructor.
   *
   * @param isUpgrade
   *          {@code true} if this is an upgrade, {@code false} otherwise.
   * @param upgradeType
   *          the orchestration type of the upgrade.
   * @param targetVersion
   *          the target version for all services and components in the upgrade
   *          or downgrade. If this is an upgrade, then this is the version tha
   *          all services are moving to. If this is a downgrade, then this is
   *          the version that all services are coming back from.
   * @param sourceVersions
   *          the versions that all services and components are coming from.
   * @param targetVersions
   *          the versions that all services and components in the upgrade are
   *          moving to.
   */
  public UpgradeInformation(boolean isUpgrade, UpgradeType upgradeType,
      RepositoryVersion targetVersion, Map<String, RepositoryVersion> sourceVersions,
      Map<String, RepositoryVersion> targetVersions) {
    m_isUpgrade = isUpgrade;
    m_upgradeType = upgradeType;
    m_targetVersion = targetVersion;
    m_sourceVersions = sourceVersions;
    m_targetVersions = targetVersions;
  }

  /**
   * {@code true} if this is an upgrade, {@code false} otherwise.
   *
   * @return the upgrade direction.
   */
  public boolean isUpgrade() {
    return m_isUpgrade;
  }

  /**
   * The orchestration type of the upgrade.
   *
   * @return the orchestration type.
   */
  public UpgradeType getUpgradeType() {
    return m_upgradeType;
  }

  /**
   * The target version for all services and components in the upgrade or
   * downgrade. If this is an upgrade, then this is the version tha all services
   * are moving to. If this is a downgrade, then this is the version that all
   * services are coming back from.
   *
   * @return the target versions for all services.
   */
  public RepositoryVersion getRepositoryVersion() {
    return m_targetVersion;
  }

  /**
   * The versions that all services and components are coming from.
   *
   * @return the source versions of all services.
   */
  public Map<String, RepositoryVersion> getSourceVersions() {
    return m_sourceVersions;
  }

  /**
   * The versions that all services and components in the upgrade are moving to.
   *
   * @return the target versions for all services.
   */
  public Map<String, RepositoryVersion> getTargetVersions() {
    return m_targetVersions;
  }
}
