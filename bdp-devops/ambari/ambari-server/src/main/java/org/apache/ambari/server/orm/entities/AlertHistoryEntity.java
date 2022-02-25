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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.AlertState;

/**
 * The {@link AlertHistoryEntity} class is an instance of an alert state change
 * that was received. Instances are only stored in the history if there is a
 * state change event. Subsequent alerts that are received for the same state
 * update the timestamps in {@link AlertNoticeEntity} but do not receive a new
 * {@link AlertHistoryEntity}.
 */
@Entity
@Table(name = "alert_history")
@TableGenerator(name = "alert_history_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "alert_history_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "AlertHistoryEntity.findAll", query = "SELECT alertHistory FROM AlertHistoryEntity alertHistory"),
  @NamedQuery(name = "AlertHistoryEntity.findAllInCluster", query = "SELECT alertHistory FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId"),
  @NamedQuery(name = "AlertHistoryEntity.findAllInClusterWithState", query = "SELECT alertHistory FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId AND alertHistory.alertState IN :alertStates"),
  @NamedQuery(name = "AlertHistoryEntity.findAllInClusterBetweenDates", query = "SELECT alertHistory FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId AND alertHistory.alertTimestamp BETWEEN :startDate AND :endDate"),
  @NamedQuery(name = "AlertHistoryEntity.findAllInClusterBeforeDate", query = "SELECT alertHistory FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId AND alertHistory.alertTimestamp <= :beforeDate"),
  @NamedQuery(name = "AlertHistoryEntity.findAllIdsInClusterBeforeDate", query = "SELECT alertHistory.alertId FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId AND alertHistory.alertTimestamp <= :beforeDate"),
  @NamedQuery(name = "AlertHistoryEntity.findAllInClusterAfterDate", query = "SELECT alertHistory FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId AND alertHistory.alertTimestamp >= :afterDate"),
  @NamedQuery(name = "AlertHistoryEntity.removeByDefinitionId", query = "DELETE FROM AlertHistoryEntity alertHistory WHERE alertHistory.alertDefinitionId = :definitionId"),
  @NamedQuery(name = "AlertHistoryEntity.removeInClusterBeforeDate", query = "DELETE FROM AlertHistoryEntity alertHistory WHERE alertHistory.clusterId = :clusterId AND alertHistory.alertTimestamp <= :beforeDate"),
  @NamedQuery(name = "AlertHistoryEntity.findHistoryIdsByDefinitionId", query = "SELECT alertHistory.alertId FROM AlertHistoryEntity alertHistory WHERE alertHistory.alertDefinitionId = :definitionId ORDER BY alertHistory.alertId")
})
public class AlertHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_history_id_generator")
  @Column(name = "alert_id", nullable = false, updatable = false)
  private Long alertId;

  @Column(name = "alert_instance", length = 255)
  private String alertInstance;

  @Column(name = "alert_label", length = 1024)
  private String alertLabel;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "alert_state", nullable = false, length = 255)
  private AlertState alertState;

  @Lob
  @Column(name = "alert_text")
  private String alertText;

  @Column(name = "alert_timestamp", nullable = false)
  private Long alertTimestamp;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "component_name", length = 255)
  private String componentName;

  @Column(name = "host_name", length = 255)
  private String hostName;

  @Column(name = "service_name", nullable = false, length = 255)
  private String serviceName;

  /**
   * Unidirectional many-to-one association to {@link AlertDefinitionEntity}
   */
  @ManyToOne
  @JoinColumn(name = "alert_definition_id", nullable = false)
  private AlertDefinitionEntity alertDefinition;

  @Column(name = "alert_definition_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long alertDefinitionId;

  /**
   * Constructor.
   */
  public AlertHistoryEntity() {
  }

  /**
   * Gets the unique ID for this alert instance.
   *
   * @return the unique ID (never {@code null}).
   */
  public Long getAlertId() {
    return alertId;
  }

  /**
   * Sets the unique ID for this alert instance.
   *
   * @param alertId
   *          the unique ID (not {@code null}).
   */
  public void setAlertId(Long alertId) {
    this.alertId = alertId;
  }

  /**
   * Gets the instance identifier, if any, for this alert instance.
   *
   * @return the instance ID or {@code null} if none.
   */
  public String getAlertInstance() {
    return alertInstance;
  }

  /**
   * Sets the instance identifier, if any, for this alert instance.
   *
   * @param alertInstance
   *          the instance ID or {@code null} if none.
   */
  public void setAlertInstance(String alertInstance) {
    this.alertInstance = alertInstance;
  }

  /**
   * Gets the label for this alert instance. The label is typically an
   * abbreviated form of the alert text.
   *
   * @return the alert instance label or {@code null} if none.
   * @see #getAlertText()
   */
  public String getAlertLabel() {
    return alertLabel;
  }

  /**
   * Sets the label for this alert instance.
   *
   * @param alertLabel
   *          the label or {@code null} if none.
   */
  public void setAlertLabel(String alertLabel) {
    this.alertLabel = alertLabel;
  }

  /**
   * Gets the state of this alert instance.
   *
   * @return the alert state (never {@code null}).
   */
  public AlertState getAlertState() {
    return alertState;
  }

  /**
   * Sets the state of this alert instance.
   *
   * @param alertState
   *          the alert state (not {@code null}).
   */
  public void setAlertState(AlertState alertState) {
    this.alertState = alertState;
  }

  /**
   * Gets the text of the alert instance.
   *
   * @return the text of the alert instance or {@code null} if none.
   */
  public String getAlertText() {
    return alertText;
  }

  /**
   * Sets the text of the alert instance.
   *
   * @param alertText
   *          the text, or {@code null} if none.
   */
  public void setAlertText(String alertText) {
    this.alertText = alertText;
  }

  /**
   * Gets the time that the alert instace was received. This will be the value,
   * in milliseconds, since the UNIX/Java epoch, represented in UTC time.
   *
   * @return the time of the alert instance (never {@code null}).
   */
  public Long getAlertTimestamp() {
    return alertTimestamp;
  }

  /**
   * Sets the time that the alert instace was received. This should be the
   * value, in milliseconds, since the UNIX/Java epoch, represented in UTC time.
   *
   * @param alertTimestamp
   *          the time of the alert instance (not {@code null}).
   */
  public void setAlertTimestamp(Long alertTimestamp) {
    this.alertTimestamp = alertTimestamp;
  }

  /**
   * Gets the ID of the cluster that this alert is associated with.
   *
   * @return the ID of the cluster for the server that this alert is for (never
   *         {@code null}).
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * Sets the ID of the cluster that this alert is associated with.
   *
   * @param clusterId
   *          the ID of the cluster for the server that this alert is for (never
   *          {@code null}).
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * Gets the name of the component, if any, that this alert instance is for.
   * Some alerts, such as those that are scoped for the entire service, do not
   * have component names.
   *
   * @return the name of the component, or {@code null} for none.
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * Sets the name of the component that this alert instance is associated with.
   * Component names are not required if the alert definition is scoped for a
   * service. If specified, there is always a 1:1 mapping between alert
   * definitions and components.
   *
   * @param componentName
   *          the name of the component, or {@code null} if none.
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * Gets the name of the host that the alert is for. Some alerts do not run
   * against hosts, such as aggregate alert definitions, so this may be
   * {@code null}.
   *
   * @return the name of the host or {@code null} if none.
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets the name of the host that the alert is for.
   *
   * @param hostName
   *          the name of the host or {@code null} if none.
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets the name of the service that the alert is defined for.
   *
   * @return the name of the service (never {@code null}).
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets the name of the service that the alert is defined for. Every alert
   * definition is related to exactly 1 service.
   *
   * @param serviceName
   *          the name of the service (not {@code null}).
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Gets the associated alert definition for this alert instance. The alert
   * definition can be used to retrieve global information about an alert such
   * as the interval and the name.
   *
   * @return the alert definition (never {@code null}).
   */
  public AlertDefinitionEntity getAlertDefinition() {
    return alertDefinition;
  }

  /**
   * Sets the associated alert definition for this alert instance.
   *
   * @param alertDefinition
   *          the alert definition (not {@code null}).
   */
  public void setAlertDefinition(AlertDefinitionEntity alertDefinition) {
    this.alertDefinition = alertDefinition;
    alertDefinitionId = alertDefinition.getDefinitionId();
  }

  /**
   * Get parent alert definition id
   * @return definition id
   */
  public Long getAlertDefinitionId() {
    return alertDefinitionId;
  }

  /**
   * Set parent alert definition id
   * @param alertDefinitionId definition id
   */
  public void setAlertDefinitionId(Long alertDefinitionId) {
    this.alertDefinitionId = alertDefinitionId;
  }

  /**
   * Gets the equality to another historical alert entry based on the following criteria:
   * <ul>
   * <li>{@link #alertId}
   * <li>{@link #clusterId}
   * <li>{@link #alertInstance}
   * <li>{@link #alertLabel}
   * <li>{@link #alertState}
   * <li>{@link #alertText}
   * <li>{@link #alertTimestamp}
   * <li>{@link #serviceName}
   * <li>{@link #componentName}
   * <li>{@link #hostName}
   * <li>{@link #alertDefinition}
   * </ul>
   * <p/>
   * However, since we're guaranteed that {@link #alertId} is unique among persisted entities, we
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

    AlertHistoryEntity that = (AlertHistoryEntity) object;

    // use the unique ID if it exists
    if( null != alertId ){
      return Objects.equals(alertId, that.alertId);
    }

    return Objects.equals(alertId, that.alertId) &&
        Objects.equals(clusterId, that.clusterId) &&
        Objects.equals(alertInstance, that.alertInstance) &&
        Objects.equals(alertLabel, that.alertLabel) &&
        Objects.equals(alertState, that.alertState) &&
        Objects.equals(alertText, that.alertText) &&
        Objects.equals(alertTimestamp, that.alertTimestamp) &&
        Objects.equals(serviceName, that.serviceName) &&
        Objects.equals(componentName, that.componentName) &&
        Objects.equals(hostName, that.hostName) &&
        Objects.equals(alertDefinition, that.alertDefinition);
  }

  /**
   * Gets a hash to uniquely identify this historical alert instance. Since historical entries
   * have no real uniqueness properties, we need to rely on a combination of the fields of this
   * entity.
   * <p/>
   * However, since we're guaranteed that {@link #alertId} is unique among persisted entities, we
   * can return the hashCode based on this value if it is set.
   * <p/>
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    // use the unique ID if it exists
    if( null != alertId ){
      return alertId.hashCode();
    }

    return Objects.hash(alertId, clusterId, alertInstance, alertLabel, alertState, alertText,
        alertTimestamp, serviceName, componentName, hostName, alertDefinition);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getClass().getSimpleName());
    buffer.append("{");
    buffer.append("id=").append(alertId);
    buffer.append(", serviceName=").append(serviceName);
    buffer.append(", componentName=").append(componentName);
    buffer.append(", state=").append(alertState);
    buffer.append(", label=").append(alertLabel);
    buffer.append("}");
    return buffer.toString();
  }

  public int getAlertDefinitionHash() {
    return this.getAlertDefinition().hashCode();
  }
}
