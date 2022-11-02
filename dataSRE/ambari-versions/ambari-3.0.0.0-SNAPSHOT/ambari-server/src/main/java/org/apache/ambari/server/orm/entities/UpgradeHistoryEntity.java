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
package org.apache.ambari.server.orm.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The {@link UpgradeHistoryEntity} represents the version history of components
 * participating in an upgrade or a downgrade.
 */
@Entity
@Table(
    name = "upgrade_history",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "upgrade_id", "component_name", "service_name" }))
@TableGenerator(
    name = "upgrade_history_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_history_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(
        name = "UpgradeHistoryEntity.findAll",
        query = "SELECT upgradeHistory FROM UpgradeHistoryEntity upgradeHistory"),
    @NamedQuery(
        name = "UpgradeHistoryEntity.findByUpgradeId",
        query = "SELECT upgradeHistory FROM UpgradeHistoryEntity upgradeHistory WHERE upgradeHistory.upgradeId = :upgradeId")
})
public class UpgradeHistoryEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_history_id_generator")
  private Long id;

  @Column(name = "upgrade_id", nullable = false, insertable = false, updatable = false)
  private Long upgradeId;

  @JoinColumn(name = "upgrade_id", nullable = false)
  private UpgradeEntity upgrade;

  @Column(name = "service_name", nullable = false, insertable = true, updatable = true)
  private String serviceName;

  @Column(name = "component_name", nullable = false, insertable = true, updatable = true)
  private String componentName;

  @ManyToOne
  @JoinColumn(name = "from_repo_version_id", unique = false, nullable = false, insertable = true, updatable = true)
  private RepositoryVersionEntity fromRepositoryVersion = null;

  @ManyToOne
  @JoinColumn(name = "target_repo_version_id", unique = false, nullable = false, insertable = true, updatable = true)
  private RepositoryVersionEntity targetRepositoryVersion = null;

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the ID of the upgrade associated with this historical entry.
   *
   * @return the upgrade ID (never {@code null}).
   */
  public Long getUpgradeId() {
    return upgradeId;
  }

  /**
   * @return
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * @param componentName
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * Gets the repository that the upgrade is coming from.
   *
   * @return the repository that the upgrade is coming from (not {@code null}).
   */
  public RepositoryVersionEntity getFromReposistoryVersion() {
    return fromRepositoryVersion;
  }

  /**
   * Sets the repository that the services in the upgrade are CURRENT on.
   *
   * @param repositoryVersionEntity
   *          the repository entity (not {@code null}).
   */
  public void setFromRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity) {
    fromRepositoryVersion = repositoryVersionEntity;
  }

  /**
   * Gets the target repository version for this upgrade.
   *
   * @return the target repository for the services in the upgrade (not
   *         {@code null}).
   */
  public RepositoryVersionEntity getTargetRepositoryVersion() {
    return targetRepositoryVersion;
  }

  /**
   * Gets the version of the target repository.
   *
   * @return the target version string (never {@code null}).
   * @see #getTargetRepositoryVersion()
   */
  public String getTargetVersion() {
    return targetRepositoryVersion.getVersion();
  }

  /**
   * Sets the target repository of the upgrade.
   *
   * @param repositoryVersionEntity
   *          the target repository (not {@code null}).
   */
  public void setTargetRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity) {
    targetRepositoryVersion = repositoryVersionEntity;
  }

  /**
   * Sets the associated upgrade entity.
   *
   * @param upgrade
   */
  public void setUpgrade(UpgradeEntity upgrade) {
    this.upgrade = upgrade;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UpgradeHistoryEntity that = (UpgradeHistoryEntity) o;
    return new EqualsBuilder()
        .append(id, that.id)
        .append(upgradeId, that.upgradeId)
        .append(serviceName, that.serviceName)
        .append(componentName, that.componentName)
        .isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(id, upgradeId, serviceName, componentName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("upgradeId", upgradeId)
        .add("serviceName", serviceName)
        .add("componentName", componentName)
        .add("from", fromRepositoryVersion)
        .add("to", targetRepositoryVersion).toString();
  }
}
