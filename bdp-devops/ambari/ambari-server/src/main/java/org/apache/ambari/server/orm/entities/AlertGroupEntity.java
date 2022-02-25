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
package org.apache.ambari.server.orm.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 * The {@link AlertGroupEntity} class manages the logical groupings of
 * {@link AlertDefinitionEntity}s in order to easily define what alerts an
 * {@link AlertTargetEntity} should be notified on.
 */
@Entity
@Table(
    name = "alert_group",
    uniqueConstraints = @UniqueConstraint(columnNames = { "cluster_id", "group_name" }))
@TableGenerator(
    name = "alert_group_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "alert_group_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(
        name = "AlertGroupEntity.findAll",
        query = "SELECT alertGroup FROM AlertGroupEntity alertGroup"),
    @NamedQuery(
        name = "AlertGroupEntity.findAllInCluster",
        query = "SELECT alertGroup FROM AlertGroupEntity alertGroup WHERE alertGroup.clusterId = :clusterId"),
    @NamedQuery(
        name = "AlertGroupEntity.findByNameInCluster",
        query = "SELECT alertGroup FROM AlertGroupEntity alertGroup WHERE alertGroup.groupName = :groupName AND alertGroup.clusterId = :clusterId"),
    @NamedQuery(
        name = "AlertGroupEntity.findByAssociatedDefinition",
        query = "SELECT alertGroup FROM AlertGroupEntity alertGroup WHERE :alertDefinition MEMBER OF alertGroup.alertDefinitions"),
    @NamedQuery(
        name = "AlertGroupEntity.findServiceDefaultGroup",
        query = "SELECT alertGroup FROM AlertGroupEntity alertGroup WHERE alertGroup.clusterId = :clusterId AND alertGroup.serviceName = :serviceName AND alertGroup.isDefault = 1"),
    @NamedQuery(
        name = "AlertGroupEntity.findByIds",
        query = "SELECT alertGroup FROM AlertGroupEntity alertGroup WHERE alertGroup.groupId IN :groupIds") })
public class AlertGroupEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_group_id_generator")
  @Column(name = "group_id", nullable = false, updatable = false)
  private Long groupId;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "group_name", nullable = false, length = 255)
  private String groupName;

  @Column(name = "is_default", nullable = false)
  private Integer isDefault = Integer.valueOf(0);

  @Column(name = "service_name", nullable = true, length = 255)
  private String serviceName;

  /**
   * Bi-directional many-to-many association to {@link AlertDefinitionEntity}
   */
  @ManyToMany(cascade = CascadeType.MERGE)
  @JoinTable(
      name = "alert_grouping",
      joinColumns = { @JoinColumn(name = "group_id", nullable = false) },
      inverseJoinColumns = { @JoinColumn(name = "definition_id", nullable = false) })
  private Set<AlertDefinitionEntity> alertDefinitions;

  /**
   * Unidirectional many-to-many association to {@link AlertTargetEntity}
   */
  @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE, CascadeType.REFRESH })
  @JoinTable(
      name = "alert_group_target",
      joinColumns = { @JoinColumn(name = "group_id", nullable = false) },
      inverseJoinColumns = { @JoinColumn(name = "target_id", nullable = false) })
  private Set<AlertTargetEntity> alertTargets;

  /**
   * Gets the unique ID of this grouping of alerts.
   *
   * @return the ID (never {@code null}).
   */
  public Long getGroupId() {
    return groupId;
  }

  /**
   * Sets the unique ID of this grouping of alerts.
   *
   * @param groupId
   *          the ID (not {@code null}).
   */
  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  /**
   * Gets the ID of the cluster that this alert group is a part of.
   *
   * @return the ID (never {@code null}).
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * Sets the ID of the cluster that this alert group is a part of.
   *
   * @param clusterId
   *          the ID of the cluster (not {@code null}).
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * Gets the name of the grouping of alerts. Group names are unique in a given
   * cluster.
   *
   * @return the group name (never {@code null}).
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets the name of this grouping of alerts. Group names are unique in a given
   * cluster.
   *
   * @param groupName
   *          the name of the group (not {@code null}).
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Gets whether this is a default group and is mostly read-only. Default
   * groups cannot have their alert definition groupings changed. New alert
   * definitions are automtaically added to the default group that belongs to
   * the service of that definition.
   *
   * @return {@code true} if this is a default group, {@code false} otherwise.
   */
  public boolean isDefault() {
    return isDefault == 0 ? false : true;
  }

  /**
   * Sets whether this is a default group and is immutable.
   *
   * @param isDefault
   *          {@code true} to make this group immutable.
   */
  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault == false ? 0 : 1;
  }

  /**
   * Gets the name of the service. This is only applicable when
   * {@link #isDefault()} is {@code true}.
   *
   * @return the service that this is the default group for, or {@code null} if
   *         this is not a default group.
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set the service name. This is only applicable when {@link #isDefault()} is
   * {@code true}.
   *
   * @param serviceName
   *          the service that this is the default group for, or {@code null} if
   *          this is not a default group.
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Gets all of the alert definitions that are a part of this grouping.
   *
   * @return the alert definitions or an empty set if none (never {@code null).
   */
  public Set<AlertDefinitionEntity> getAlertDefinitions() {
    if (null == alertDefinitions) {
      alertDefinitions = new HashSet<>();
    }

    return Collections.unmodifiableSet(alertDefinitions);
  }

  /**
   * Set all of the alert definitions that are part of this alert group.
   *
   * @param alertDefinitions
   *          the definitions, or {@code null} for none.
   */
  public void setAlertDefinitions(Set<AlertDefinitionEntity> alertDefinitions) {
    if (null != this.alertDefinitions) {
      for (AlertDefinitionEntity definition : this.alertDefinitions) {
        definition.removeAlertGroup(this);
      }
    }

    this.alertDefinitions = alertDefinitions;

    if (null != alertDefinitions) {
      for (AlertDefinitionEntity definition : alertDefinitions) {
        definition.addAlertGroup(this);
      }
    }
  }

  /**
   * Adds the specified definition to the definitions that this group will
   * dispatch to.
   *
   * @param definition
   *          the definition to add (not {@code null}).
   */
  public void addAlertDefinition(AlertDefinitionEntity definition) {
    if (null == alertDefinitions) {
      alertDefinitions = new HashSet<>();
    }

    alertDefinitions.add(definition);
    definition.addAlertGroup(this);
  }

  /**
   * Removes the specified definition from the definitions that this group will
   * dispatch to.
   *
   * @param definition
   *          the definition to remove (not {@code null}).
   */
  public void removeAlertDefinition(AlertDefinitionEntity definition) {
    if (null != alertDefinitions) {
      alertDefinitions.remove(definition);
    }

    definition.removeAlertGroup(this);
  }

  /**
   * Gets an immutable set of the targets that will receive notifications for
   * alert definitions in this group.
   *
   * @return the targets that will be dispatch to for alerts in this group, or
   *         an empty set if there are none (never {@code null}).
   */
  public Set<AlertTargetEntity> getAlertTargets() {
    if( null == alertTargets ) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(alertTargets);
  }

  /**
   * Adds the specified target to the targets that this group will dispatch to.
   *
   * @param alertTarget
   *          the target to add (not {@code null}).
   */
  public void addAlertTarget(AlertTargetEntity alertTarget) {
    if (null == alertTargets) {
      alertTargets = new HashSet<>();
    }

    alertTargets.add(alertTarget);
    alertTarget.addAlertGroup(this);
  }

  /**
   * Removes the specified target from the targets that this group will dispatch
   * to.
   *
   * @param alertTarget
   *          the target to remove (not {@code null}).
   */
  public void removeAlertTarget(AlertTargetEntity alertTarget) {
    if (null != alertTargets) {
      alertTargets.remove(alertTarget);
    }

    alertTarget.removeAlertGroup(this);
  }

  /**
   * Sets all of the targets that will receive notifications for alert
   * definitions in this group.
   *
   * @param alertTargets
   *          the targets, or {@code null} if there are none.
   */
  public void setAlertTargets(Set<AlertTargetEntity> alertTargets) {
    // for any existing associations, remove "this" from those associations
    if (null != this.alertTargets) {
      // make a copy to prevent ConcurrentModificiationExceptions
      Set<AlertTargetEntity> copyOfAssociatedTargets = new HashSet<>(this.alertTargets);
      for (AlertTargetEntity target : copyOfAssociatedTargets) {
        target.removeAlertGroup(this);
      }
    }

    // update all new targets to reflect "this" as an associated group
    if (null != alertTargets) {
      for (AlertTargetEntity target : alertTargets) {
        target.addAlertGroup(this);
      }
    }

    // update reference
    this.alertTargets = alertTargets;
  }

  /**
   *
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    AlertGroupEntity that = (AlertGroupEntity) object;

    // use the unique ID if it exists
    if( null != groupId ){
      return Objects.equals(groupId, that.groupId);
    }

    return Objects.equals(groupId, that.groupId) &&
        Objects.equals(clusterId, that.clusterId) &&
        Objects.equals(groupName, that.groupName) &&
        Objects.equals(serviceName, that.serviceName);
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    // use the unique ID if it exists
    if( null != groupId ){
      return groupId.hashCode();
    }

    return Objects.hash(groupId, clusterId, groupName, serviceName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getClass().getSimpleName());
    buffer.append("{");
    buffer.append("id=").append(groupId);
    buffer.append(", name=").append(groupName);
    buffer.append(", default=").append(isDefault);
    buffer.append("}");
    return buffer.toString();
  }
}
