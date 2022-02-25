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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.AlertState;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link AlertTargetEntity} class represents audience that will receive
 * dispatches when an alert is triggered.
 */
@Entity
@Table(name = "alert_target")
@TableGenerator(
    name = "alert_target_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "alert_target_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(
        name = "AlertTargetEntity.findAll",
        query = "SELECT alertTarget FROM AlertTargetEntity alertTarget"),
    @NamedQuery(
        name = "AlertTargetEntity.findAllGlobal",
        query = "SELECT alertTarget FROM AlertTargetEntity alertTarget WHERE alertTarget.isGlobal = 1"),
    @NamedQuery(
        name = "AlertTargetEntity.findByName",
        query = "SELECT alertTarget FROM AlertTargetEntity alertTarget WHERE alertTarget.targetName = :targetName"),
    @NamedQuery(
        name = "AlertTargetEntity.findByIds",
        query = "SELECT alertTarget FROM AlertTargetEntity alertTarget WHERE alertTarget.targetId IN :targetIds") })
public class AlertTargetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_target_id_generator")
  @Column(name = "target_id", nullable = false, updatable = false)
  private Long targetId;

  @Column(length = 1024)
  private String description;

  @Column(name = "notification_type", nullable = false, length = 64)
  private String notificationType;

  @Lob
  @Basic
  @Column(length = 32672)
  private String properties;

  @Column(name = "target_name", unique = true, nullable = false, length = 255)
  private String targetName;

  @Column(name = "is_global", nullable = false, length = 1)
  private Short isGlobal = Short.valueOf((short) 0);

  @Column(name = "is_enabled", nullable = false, length = 1)
  private Short isEnabled = Short.valueOf((short) 1);

  /**
   * Bi-directional many-to-many association to {@link AlertGroupEntity}
   */
  @ManyToMany(
      fetch = FetchType.EAGER,
      mappedBy = "alertTargets",
      cascade = { CascadeType.MERGE, CascadeType.REFRESH })
  private Set<AlertGroupEntity> alertGroups;

  /**
   * Gets the alert states that this target will be notified for. If this is
   * either empty or {@code null}, then it is implied that all alert states are
   * of interest to the target. A target without an alert states does not make
   * sense which is why the absence of states implies all states.
   */
  @Enumerated(value = EnumType.STRING)
  @ElementCollection(targetClass = AlertState.class)
  @CollectionTable(name = "alert_target_states", joinColumns = @JoinColumn(name = "target_id"))
  @Column(name = "alert_state")
  private Set<AlertState> alertStates = EnumSet.allOf(AlertState.class);

  /**
   * Bi-directional one-to-many association to {@link AlertNoticeEntity}.
   */
  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "alertTarget")
  private List<AlertNoticeEntity> alertNotices;

  /**
   * Gets the unique ID of this alert target.
   *
   * @return the ID of the target (never {@code null}).
   */
  public Long getTargetId() {
    return targetId;
  }

  /**
   * Sets the unique ID of this alert target.
   *
   * @param targetId
   *          the ID of the alert target (not {@code null}).
   */
  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  /**
   * Gets the description of this alert target.
   *
   * @return the description or {@code null} if none.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description for this alert target.
   *
   * @param description
   *          the description or {@code null} for none.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return
   */
  public String getNotificationType() {
    return notificationType;
  }

  /**
   * @param notificationType
   */
  public void setNotificationType(String notificationType) {
    this.notificationType = notificationType;
  }

  /**
   * @return
   */
  public String getProperties() {
    return properties;
  }

  /**
   * @param properties
   */
  public void setProperties(String properties) {
    this.properties = properties;
  }

  /**
   * Gets the name of this alert target.
   *
   * @return the alert target name (never {@code null}).
   */
  public String getTargetName() {
    return targetName;
  }

  /**
   * Gets whether the alert target is a global target and will receive
   * notifications triggered for any alert group.
   *
   * @return the isGlobal {@code} if the target is global.
   */
  public boolean isGlobal() {
    return isGlobal == 0 ? false : true;
  }

  /**
   * Sets whether the alert target is a global target and will receive
   * notifications triggered for any alert group.
   *
   * @param isGlobal
   *          {@code} if the target is global.
   */
  public void setGlobal(boolean isGlobal) {
    this.isGlobal = isGlobal ? (short) 1 : (short) 0;
  }

  /**
   * Gets whether the alert target is enabled. Targets which are not enabled
   * will not receive notifications.
   *
   * @return the {@code true} if the target is enabled.
   */
  public boolean isEnabled() {
    return isEnabled == 0 ? false : true;
  }

  /**
   * Sets whether the alert target is enabled. Targets which are not enabled
   * will not receive notifications.
   *
   * @param isEnabled
   *          {@code} if the target is enabled.
   */
  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled ? (short) 1 : (short) 0;
  }

  /**
   * Gets the alert states that will cause a triggered alert to be sent to this
   * target. A target may be associated with a group where an alert has changed
   * state, but if that new state is not of interest to the target, it will not
   * be sent.
   *
   * @return the set of alert states or {@code null} or empty to imply all.
   */
  public Set<AlertState> getAlertStates() {
    return alertStates;
  }

  /**
   * Sets the alert states that will cause a triggered alert to be sent to this
   * target. A target may be associated with a group where an alert has changed
   * state, but if that new state is not of interest to the target, it will not
   * be sent.
   *
   * @param alertStates
   *          the set of alert states or {@code null} or empty to imply all.
   */
  public void setAlertStates(Set<AlertState> alertStates) {
    this.alertStates = alertStates;
  }

  /**
   * Sets the name of this alert target.
   *
   * @param targetName
   *          the name (not {@code null}).
   */
  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  /**
   * Gets an immutable set of the alert groups that this target is associated
   * with.
   *
   * @return the groups that will send to this target when an alert in that
   *         group is received, or an empty set for none.
   */
  public Set<AlertGroupEntity> getAlertGroups() {
    if (null == alertGroups) {
      return Collections.emptySet();
    }

    return ImmutableSet.copyOf(alertGroups);
  }

  /**
   * Sets all of the groups that are associated with this target.
   *
   * @param alertGroups
   *          the groups, or {@code null} if there are none.
   */
  public void setAlertGroups(Set<AlertGroupEntity> alertGroups) {
    Set<AlertGroupEntity> groups = getAlertGroups();
    for (AlertGroupEntity group : groups) {
      group.removeAlertTarget(this);
    }

    this.alertGroups = alertGroups;

    if (null != alertGroups) {
      for (AlertGroupEntity group : alertGroups) {
        group.addAlertTarget(this);
      }
    }
  }

  /**
   * Adds the specified alert group to the groups that this target is associated
   * with. This is used to complement the JPA bidirectional association.
   *
   * @param alertGroup
   */
  protected void addAlertGroup(AlertGroupEntity alertGroup) {
    if (null == alertGroups) {
      alertGroups = new HashSet<>();
    }

    alertGroups.add(alertGroup);
  }

  /**
   * Removes the specified alert group to the groups that this target is
   * associated with. This is used to complement the JPA bidirectional
   * association.
   *
   * @param alertGroup
   */
  protected void removeAlertGroup(AlertGroupEntity alertGroup) {
    if (null != alertGroups) {
      alertGroups.remove(alertGroup);
    }
  }

  /**
   * Adds the specified notice to the notices that have been sent out for this
   * target.
   *
   * @param notice
   *          the notice.
   */
  protected void addAlertNotice(AlertNoticeEntity notice) {
    if (null == alertNotices) {
      alertNotices = new ArrayList<>();
    }

    alertNotices.add(notice);
  }

  public List<AlertNoticeEntity> getAlertNotices() {
    return alertNotices;
  }

  public void setAlertNotices(List<AlertNoticeEntity> alertNotices) {
    this.alertNotices = alertNotices;
  }

  /**
   * Called before {@link EntityManager#remove(Object)} for this entity, removes
   * the non-owning relationship between targets and groups.
   */
  @PreRemove
  public void preRemove() {
    Set<AlertGroupEntity> groups = getAlertGroups();
    if (!groups.isEmpty()) {
      for (AlertGroupEntity group : groups) {
        group.removeAlertTarget(this);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    AlertTargetEntity that = (AlertTargetEntity) object;

    // use the unique ID if it exists
    if( null != targetId ){
      return Objects.equals(targetId, that.targetId);
    }

    return Objects.equals(targetId, that.targetId) &&
        Objects.equals(targetName, that.targetName) &&
        Objects.equals(notificationType, that.notificationType) &&
        Objects.equals(isEnabled, that.isEnabled) &&
        Objects.equals(description, that.description) &&
        Objects.equals(isGlobal, that.isGlobal);
    }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    // use the unique ID if it exists
    if (null != targetId) {
      return targetId.hashCode();
    }

    return Objects.hash(targetId, targetName, notificationType, isEnabled, description, isGlobal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getClass().getSimpleName());
    buffer.append("{");
    buffer.append("id=").append(targetId);
    buffer.append(", name=").append(targetName);
    buffer.append("}");
    return buffer.toString();
  }
}
