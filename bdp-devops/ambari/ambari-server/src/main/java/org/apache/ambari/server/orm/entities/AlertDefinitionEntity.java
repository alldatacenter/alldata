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
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PreRemove;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;

/**
 * The {@link AlertDefinitionEntity} class is used to model an alert that needs
 * to run in the system. Each received alert from an agent will essentially be
 * an instance of this template.
 */
@Entity
@Table(name = "alert_definition", uniqueConstraints = @UniqueConstraint(columnNames = {
  "cluster_id", "definition_name"}))
@TableGenerator(name = "alert_definition_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "alert_definition_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "AlertDefinitionEntity.findAll", query = "SELECT ad FROM AlertDefinitionEntity ad"),
  @NamedQuery(name = "AlertDefinitionEntity.findAllInCluster", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.clusterId = :clusterId"),
  @NamedQuery(name = "AlertDefinitionEntity.findAllEnabledInCluster", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.clusterId = :clusterId AND ad.enabled = 1"),
  @NamedQuery(name = "AlertDefinitionEntity.findByName", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.definitionName = :definitionName AND ad.clusterId = :clusterId",
    hints = {
      @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
      @QueryHint(name = "eclipselink.query-results-cache.ignore-null", value = "true"),
      @QueryHint(name = "eclipselink.query-results-cache.size", value = "5000")
    }),
  @NamedQuery(name = "AlertDefinitionEntity.findByService", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.serviceName = :serviceName AND ad.clusterId = :clusterId"),
  @NamedQuery(name = "AlertDefinitionEntity.findByServiceAndComponent", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.serviceName = :serviceName AND ad.componentName = :componentName AND ad.clusterId = :clusterId"),
  @NamedQuery(name = "AlertDefinitionEntity.findByServiceMaster", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.serviceName IN :services AND ad.scope = :scope AND ad.clusterId = :clusterId AND ad.componentName IS NULL" +
      " AND ad.sourceType <> org.apache.ambari.server.state.alert.SourceType.AGGREGATE"),
  @NamedQuery(name = "AlertDefinitionEntity.findByIds", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.definitionId IN :definitionIds"),
  @NamedQuery(name = "AlertDefinitionEntity.findBySourceType", query = "SELECT ad FROM AlertDefinitionEntity ad WHERE ad.clusterId = :clusterId AND ad.sourceType = :sourceType")})
public class AlertDefinitionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_definition_id_generator")
  @Column(name = "definition_id", nullable = false, updatable = false)
  private Long definitionId;

  @Lob
  @Basic
  @Column(name = "alert_source", nullable = false, length = 32672)
  private String source;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", insertable = false, updatable = false)
  private ClusterEntity clusterEntity;

  @Column(name = "component_name", length = 255)
  private String componentName;

  @Column(name = "definition_name", nullable = false, length = 255)
  private String definitionName;

  @Column(name = "label", nullable = true, length = 255)
  private String label;

  @Column(name = "help_url", nullable = true, length = 512)
  private String helpURL;

  @Lob
  @Basic
  @Column(name = "description", nullable = true, length = 32672)
  private String description;

  @Column(name = "scope", length = 255)
  @Enumerated(value = EnumType.STRING)
  private Scope scope;

  @Column(nullable = false)
  private Integer enabled = Integer.valueOf(1);

  @Column(nullable = false, length = 64)
  private String hash;

  @Column(name = "schedule_interval", nullable = false)
  private Integer scheduleInterval;

  @Column(name = "service_name", nullable = false, length = 255)
  private String serviceName;

  @Column(name = "source_type", nullable = false, length = 255)
  @Enumerated(value = EnumType.STRING)
  private SourceType sourceType;

  @Column(name = "ignore_host", nullable = false)
  private Integer ignoreHost = Integer.valueOf(0);

  /**
   * Indicates how many sequential alerts must be received for a non-OK state
   * change to be considered correct. This value is meant to eliminate
   * false-positive notifications on alerts which are flaky.
   */
  @Column(name = "repeat_tolerance", nullable = false)
  private Integer repeatTolerance = Integer.valueOf(1);

  /**
   * If {@code 1}, then the value of {@link #repeatTolerance} is used to
   * override the global alert tolerance value.
   */
  @Column(name = "repeat_tolerance_enabled", nullable = false)
  private Short repeatToleranceEnabled = Short.valueOf((short) 0);

  /**
   * Bi-directional many-to-many association to {@link AlertGroupEntity}
   */
  @ManyToMany(mappedBy = "alertDefinitions", cascade = {CascadeType.PERSIST,
    CascadeType.MERGE, CascadeType.REFRESH})
  private Set<AlertGroupEntity> alertGroups;

  /**
   * Constructor.
   */
  public AlertDefinitionEntity() {
  }

  /**
   * Gets the unique identifier for this alert definition.
   *
   * @return the ID.
   */
  public Long getDefinitionId() {
    return definitionId;
  }

  /**
   * Sets the unique identifier for this alert definition.
   *
   * @param definitionId the ID (not {@code null}).
   */
  public void setDefinitionId(Long definitionId) {
    this.definitionId = definitionId;
  }

  /**
   * Gets the source that defines the type of alert and the alert properties.
   * This is typically a JSON structure that can be mapped to a first-class
   * object.
   *
   * @return the alert source (never {@code null}).
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the source of the alert, typically in JSON, that defines the type of
   * the alert and its properties.
   *
   * @param alertSource the alert source (not {@code null}).
   */
  public void setSource(String alertSource) {
    source = alertSource;
  }

  /**
   * Gets the ID of the cluster that this alert definition is created for. Each
   * cluster has their own set of alert definitions that are not shared with any
   * other cluster.
   *
   * @return the ID of the cluster (never {@code null}).
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * Sets the ID of the cluster that this alert definition is created for. Each
   * cluster has their own set of alert definitions that are not shared with any
   * other cluster.
   *
   * @param clusterId the ID of the cluster (not {@code null}).
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * Gets the cluster that this alert definition is a member of.
   *
   * @return
   */
  public ClusterEntity getCluster() {
    return clusterEntity;
  }

  /**
   * Sets the cluster that the alert definition is a member of.
   *
   * @param clusterEntity
   */
  public void setCluster(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  /**
   * Gets the component name that this alert is associated with, if any. Some
   * alerts are scoped at the service level and will not have a component name.
   *
   * @return the component name or {@code null} if none.
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * Sets the component name that this alert is associated with, if any. Some
   * alerts are scoped at the service level and will not have a component name.
   *
   * @param componentName the component name or {@code null} if none.
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * Gets the scope of the alert definition. The scope is defined as either
   * being for a {@link Scope#SERVICE} or {@link Scope#HOST}.
   *
   * @return the scope, or {@code null} if not defined.
   */
  public Scope getScope() {
    return scope;
  }

  /**
   * Sets the scope of the alert definition. The scope is defined as either
   * being for a {@link Scope#SERVICE} or {@link Scope#HOST}.
   *
   * @param scope the scope to set, or {@code null} for none.
   */
  public void setScope(Scope scope) {
    this.scope = scope;
  }

  /**
   * Gets the name of this alert definition. Alert definition names are unique
   * within a cluster.
   *
   * @return the name of the alert definition (never {@code null}).
   */
  public String getDefinitionName() {
    return definitionName;
  }

  /**
   * Sets the name of this alert definition. Alert definition names are unique
   * within a cluster.
   *
   * @param definitionName the name of the alert definition (not {@code null}).
   */
  public void setDefinitionName(String definitionName) {
    this.definitionName = definitionName;
  }

  /**
   * Gets whether this alert definition is enabled. Disabling an alert
   * definition will prevent agents from scheduling the alerts. No alerts will
   * be triggered and no alert data will be collected.
   *
   * @return {@code true} if this alert definition is enabled, {@code false}
   * otherwise.
   */
  public boolean getEnabled() {
    return !Objects.equals(enabled, 0);
  }

  /**
   * Sets whether this alert definition is enabled.
   *
   * @param enabled {@code true} if this alert definition is enabled, {@code false}
   *                otherwise.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled ? Integer.valueOf(1) : Integer.valueOf(0);
  }

  /**
   * Gets whether this alert definition will ignore the hosts reporting the
   * alert and combine them all into a single alert entry.
   *
   * @return {@code true} if this alert definition is to ignore hosts and
   * combine all alert instances into a single entry, {@code false}
   * otherwise.
   */
  public boolean isHostIgnored() {
    return !Objects.equals(ignoreHost, 0);
  }

  /**
   * Sets whether this alert definition will ignore the hosts reporting the
   * alert and combine them all into a single alert entry.
   *
   * @param ignoreHost {@code true} if this alert definition is to ignore hosts and
   *                   combine all alert instances into a single entry, {@code false}
   *                   otherwise.
   */
  public void setHostIgnored(boolean ignoreHost) {
    this.ignoreHost = ignoreHost ? Integer.valueOf(1) : Integer.valueOf(0);
  }

  /**
   * Gets the unique hash for the current state of this definition. If a
   * property of this definition changes, a new hash is calculated.
   *
   * @return the unique hash or {@code null} if there is none.
   */
  public String getHash() {
    return hash;
  }

  /**
   * Gets the unique hash for the current state of this definition. If a
   * property of this definition changes, a new hash is calculated.
   *
   * @param hash the unique hash to set or {@code null} for none.
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /**
   * Gets the alert trigger interval, in seconds.
   *
   * @return the interval, in seconds.
   */
  public Integer getScheduleInterval() {
    return scheduleInterval;
  }

  /**
   * Sets the alert trigger interval, in seconds.
   *
   * @param scheduleInterval the interval, in seconds.
   */
  public void setScheduleInterval(Integer scheduleInterval) {
    this.scheduleInterval = scheduleInterval;
  }

  /**
   * Gets the name of the service that this alert definition is associated with.
   * Every alert definition is associated with exactly one service.
   *
   * @return the name of the service (never {@code null}).
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Gets the name of the service that this alert definition is associated with.
   * Every alert definition is associated with exactly one service.
   *
   * @param serviceName the name of the service (not {@code null}).
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return
   */
  public SourceType getSourceType() {
    return sourceType;
  }

  /**
   * @param sourceType
   */
  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  /**
   * Gets the alert groups that this alert definition is associated with.
   *
   * @return the groups, or {@code null} if none.
   */
  public Set<AlertGroupEntity> getAlertGroups() {
    return Collections.unmodifiableSet(alertGroups);
  }

  /**
   * Sets the alert groups that this alert definition is associated with.
   *
   * @param alertGroups the groups, or {@code null} for none.
   */
  public void setAlertGroups(Set<AlertGroupEntity> alertGroups) {
    this.alertGroups = alertGroups;
  }

  /**
   * Sets a human readable label for this alert definition.
   *
   * @param label the label or {@code null} if none.
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Gets the label for this alert definition.
   *
   * @return the label or {@code null} if none.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Gets the help url for this alert.
   *
   * @return the helpURL or {@code null} if none.
   */
  public String getHelpURL() {
    return helpURL;
  }

  /**
   * Sets a help url for this alert.
   *
   * @param helpURL the helpURL or {@code null} if none.
   */
  public void setHelpURL(String helpURL) {
    this.helpURL = helpURL;
  }

  /**
   * Gets the optional description for this alert definition.
   *
   * @return the description, or {@code null} if none.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the optional description for this alert definition.
   *
   * @param description the description to set or {@code null} for none.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the number of sequential instances of an non-OK {@link AlertState}
   * must be received in order to consider the alert instance as truly being in
   * that {@link AlertState}.
   * <p/>
   * A value of 1 or less indicates that there is no tolerance and a single
   * alert will cause dispatching of alert notifications.
   *
   * @return the repeatTolerance
   */
  public int getRepeatTolerance() {
    return repeatTolerance;
  }

  /**
   * Sets the number of sequential instances of an non-OK {@link AlertState}
   * must be received in order to consider the alert instance as truly being in
   * that {@link AlertState}.
   * <p/>
   * A value of 1 or less indicates that there is no tolerance and a single
   * alert will cause dispatching of alert notifications.
   *
   * @param repeatTolerance
   *          the tolerance to set
   */
  public void setRepeatTolerance(int repeatTolerance) {
    this.repeatTolerance = repeatTolerance;
  }

  /**
   * Gets whether this definition overrides the default global tolerance value
   * specified in {@code cluster-env/alerts.repeat.tolerance}. If enabled, the
   * value from {@link #getRepeatTolerance()} should be used to calculate retry
   * tolerance.
   *
   * @return the repeatToleranceEnabled {@code true} to override the global
   *         value.
   */
  public boolean isRepeatToleranceEnabled() {
    return !Objects.equals(repeatToleranceEnabled, (short) 0);
  }

  /**
   * Sets whether this definition overrides the default global tolerance value
   * specified in {@code cluster-env/alerts.repeat.tolerance}. If enabled, the
   * value from {@link #getRepeatTolerance()} should be used to calculate retry
   * tolerance.
   *
   * @param enabled
   *          {@code true} to override the defautlt value and use the value
   *          returned from {@link #getRepeatTolerance()}.
   */
  public void setRepeatToleranceEnabled(boolean enabled) {
    repeatToleranceEnabled = enabled ? Short.valueOf((short) 1) : 0;
  }

  /**
   * Adds the specified alert group to the groups that this definition is
   * associated with. This is used to complement the JPA bidirectional
   * association.
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
   * Removes the specified alert group to the groups that this definition is
   * associated with. This is used to complement the JPA bidirectional
   * association.
   *
   * @param alertGroup
   */
  protected void removeAlertGroup(AlertGroupEntity alertGroup) {
    if (null != alertGroups && alertGroups.contains(alertGroup)) {
      alertGroups.remove(alertGroup);
    }
  }

  /**
   * Called before {@link EntityManager#remove(Object)} for this entity, removes
   * the non-owning relationship between definitions and groups.
   */
  @PreRemove
  public void preRemove() {
    if (null == alertGroups || alertGroups.size() == 0) {
      return;
    }

    Iterator<AlertGroupEntity> iterator = alertGroups.iterator();
    while (iterator.hasNext()) {
      AlertGroupEntity group = iterator.next();
      iterator.remove();

      group.removeAlertDefinition(this);
    }
  }

  /**
   * Gets the equality to another historical alert entry based on the following criteria:
   * <ul>
   * <li>{@link #definitionId}
   * <li>{@link #clusterId}
   * <li>{@link #definitionName}
   * </ul>
   * <p/>
   * However, since we're guaranteed that {@link #definitionId} is unique among persisted entities, we
   * can return the hashCode based on this value if it is set.
   * <p/>
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

    AlertDefinitionEntity that = (AlertDefinitionEntity) object;

    // use the unique ID if it exists
    if( null != definitionId ){
      return Objects.equals(definitionId, that.definitionId);
    }

    return Objects.equals(definitionId, that.definitionId) &&
        Objects.equals(clusterId, that.clusterId) &&
        Objects.equals(definitionName, that.definitionName);
  }

  /**
   * Gets a hash to uniquely identify this alert definition. Since we're
   * guaranteed that {@link #definitionId} is unique among persisted entities,
   * we can return the hashCode based on this value if it is set.
   * <p/>
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    // use the unique ID if it exists
    if( null != definitionId ){
      return definitionId.hashCode();
    }

    return Objects.hash(definitionId, clusterId, definitionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getClass().getSimpleName());
    buffer.append("{");
    buffer.append("id=").append(definitionId);
    buffer.append(", name=").append(definitionName);
    buffer.append(", serviceName=").append(serviceName);
    buffer.append(", componentName=").append(componentName);
    buffer.append(", enabled=").append(enabled);
    buffer.append(", hash=").append(hash);
    buffer.append("}");
    return buffer.toString();
  }
}
