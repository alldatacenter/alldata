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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * The {@link AlertCurrentEntity} class represents the most recently received an
 * alert data for a given instance. This class always has an associated matching
 * {@link AlertHistoryEntity} that defines the actual data of the alert.
 * <p/>
 * There will only ever be a single entity for each given
 * {@link AlertDefinitionEntity}.
 */
@Entity
@Table(name = "alert_current")
@TableGenerator(name = "alert_current_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "alert_current_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "AlertCurrentEntity.findAll", query = "SELECT alert FROM AlertCurrentEntity alert"),
  @NamedQuery(name = "AlertCurrentEntity.findByCluster", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.clusterId = :clusterId"),
  @NamedQuery(name = "AlertCurrentEntity.findByDefinitionId", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertDefinition.definitionId = :definitionId"),
  @NamedQuery(name = "AlertCurrentEntity.findByService", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.clusterId = :clusterId AND alert.alertHistory.serviceName = :serviceName AND alert.alertHistory.alertDefinition.scope IN :inlist"),
  @NamedQuery(name = "AlertCurrentEntity.findByHostAndName", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.clusterId = :clusterId AND alert.alertHistory.alertDefinition.definitionName = :definitionName AND alert.alertHistory.hostName = :hostName"),
  @NamedQuery(name = "AlertCurrentEntity.findByNameAndNoHost", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.clusterId = :clusterId AND alert.alertHistory.alertDefinition.definitionName = :definitionName AND alert.alertHistory.hostName IS NULL"),
  @NamedQuery(name = "AlertCurrentEntity.findByHostComponent", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.serviceName = :serviceName AND alert.alertHistory.componentName = :componentName AND alert.alertHistory.hostName = :hostName"),
  @NamedQuery(name = "AlertCurrentEntity.findByHost", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.hostName = :hostName"),
  @NamedQuery(name = "AlertCurrentEntity.findByServiceName", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertHistory.serviceName = :serviceName"),
  @NamedQuery(name = "AlertCurrentEntity.findDisabled", query = "SELECT alert FROM AlertCurrentEntity alert WHERE alert.alertDefinition.enabled = 0"),
  @NamedQuery(name = "AlertCurrentEntity.removeByHistoryId", query = "DELETE FROM AlertCurrentEntity alert WHERE alert.historyId = :historyId"),
  // The remove queries can be handled by a simpler JPQL query,
  // however, MySQL gtid enforce policy gets violated due to creation and
  // deletion of TEMP table in the same transaction
  @NamedQuery(name = "AlertCurrentEntity.removeByHistoryIds", query = "DELETE FROM AlertCurrentEntity alert WHERE alert.historyId IN :historyIds"),
  @NamedQuery(name = "AlertCurrentEntity.removeByDefinitionId", query = "DELETE FROM AlertCurrentEntity alert WHERE alert.definitionId = :definitionId")
})
public class AlertCurrentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_current_id_generator")
  @Column(name = "alert_id", nullable = false, updatable = false)
  private Long alertId;

  @Column(name = "history_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long historyId;

  @Column(name = "definition_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long definitionId;

  @Column(name = "latest_timestamp", nullable = false)
  private Long latestTimestamp;

  @Column(name = "maintenance_state", length = 255)
  @Enumerated(value = EnumType.STRING)
  private MaintenanceState maintenanceState = MaintenanceState.OFF;

  @Column(name = "original_timestamp", nullable = false)
  private Long originalTimestamp;

  @Lob
  @Column(name = "latest_text")
  private String latestText = null;

  /**
   * The number of occurrences of this alert in its current state. States which
   * are not {@link AlertState#OK} are aggregated such that transitioning
   * between these states should not reset this value. For example, if an alert
   * bounces between {@link AlertState#WARNING} and {@link AlertState#CRITICAL},
   * then it will not reset this value.
   *
   */
  @Column(name="occurrences", nullable=false)
  private Long occurrences = Long.valueOf(1);

  @Column(name = "firmness", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private AlertFirmness firmness = AlertFirmness.HARD;

  /**
   * Unidirectional one-to-one association to {@link AlertHistoryEntity}
   */
  @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
  @JoinColumn(name = "history_id", unique = true, nullable = false)
  private AlertHistoryEntity alertHistory;

  /**
   * Unidirectional one-to-one association to {@link AlertDefinitionEntity}
   */
  @OneToOne
  @JoinColumn(name = "definition_id", unique = false, nullable = false)
  private AlertDefinitionEntity alertDefinition;

  /**
   * Constructor.
   */
  public AlertCurrentEntity() {
  }

  /**
   * Gets the unique ID for this current alert.
   *
   * @return the ID (never {@code null}).
   */
  public Long getAlertId() {
    return alertId;
  }

  /**
   * Sets the unique ID for this current alert.
   *
   * @param alertId
   *          the ID (not {@code null}).
   */
  public void setAlertId(Long alertId) {
    this.alertId = alertId;
  }

  /**
   * Get the related alert history id.
   * @return history id
   */
  public Long getHistoryId() {
    return historyId;
  }

  /**
   * Set the related history id.
   * @param historyId historyId
   */
  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  /**
   * Get parent alert definition id
   * @return definition id
   */
  public Long getDefinitionId() {
    return definitionId;
  }

  /**
   * Set the parent alert definition id
   * @param definitionId definition id
   */
  public void setDefinitionId(Long definitionId) {
    this.definitionId = definitionId;
  }

  /**
   * Gets the time, in millis, that the last instance of this alert state was
   * received.
   *
   * @return the time of the most recently received alert data for this instance
   *         (never {@code null}).
   */
  public Long getLatestTimestamp() {
    return latestTimestamp;
  }

  /**
   * Sets the time, in millis, that the last instance of this alert state was
   * received.
   *
   * @param latestTimestamp
   *          the time of the most recently received alert data for this
   *          instance (never {@code null}).
   */
  public void setLatestTimestamp(Long latestTimestamp) {
    this.latestTimestamp = latestTimestamp;
  }

  /**
   * Gets the current maintenance state for the alert.
   *
   * @return the current maintenance state (never {@code null}).
   */
  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * Sets the current maintenance state for the alert.
   *
   * @param maintenanceState
   *          the state to set (not {@code null}).
   */
  public void setMaintenanceState(MaintenanceState maintenanceState) {
    this.maintenanceState = maintenanceState;
  }

  /**
   * Gets the time, in milliseconds, when the alert was first received with the
   * current state.
   *
   * @return the time of the first instance of this alert.
   */
  public Long getOriginalTimestamp() {
    return originalTimestamp;
  }

  /**
   * Sets the time, in milliseconds, when the alert was first received with the
   * current state.
   *
   * @param originalTimestamp
   *          the time of the first instance of this alert (not {@code null}).
   */
  public void setOriginalTimestamp(Long originalTimestamp) {
    this.originalTimestamp = originalTimestamp;
  }

  /**
   * Gets the latest text for this alert.  History will not get a new record on
   * update when the state is the same, but the text may be changed.  For example,
   * CPU utilization includes the usage in the text and should be available.
   */
  public String getLatestText() {
    return latestText;
  }

  /**
   * Sets the latest text.  {@link #getLatestText()}
   */
  public void setLatestText(String text) {
    latestText = text;
  }

  /**
   * Gets the number of occurrences of this alert in its current state. States
   * which are not {@link AlertState#OK} are aggregated such that transitioning
   * between these states should not reset this value. For example, if an alert
   * bounces between {@link AlertState#WARNING} and {@link AlertState#CRITICAL},
   * then it will not reset this value.
   *
   * @return the number of occurrences.
   */
  public Long getOccurrences() {
    return occurrences;
  }

  /**
   * Sets the number of occurrences for this alert instance.
   *
   * @param occurrences
   *          the occurrences.
   * @see #getOccurrences()
   *
   */
  public void setOccurrences(long occurrences) {
    this.occurrences = occurrences;
  }

  /**
   * Gets the firmness of the alert, indicating whether or not it could be a
   * potential false positive.
   *
   * @return the alert firmness.
   */
  public AlertFirmness getFirmness() {
    return firmness;
  }

  /**
   * Sets the firmness of the alert, indicating whether or not it could be a
   * potential false positive.
   *
   * @param firmness
   *          the firmness (not {@code null}).
   */
  public void setFirmness(AlertFirmness firmness) {
    this.firmness = firmness;
  }

  /**
   * Gets the associated {@link AlertHistoryEntity} entry for this current alert
   * instance.
   *
   * @return the most recently received history entry (never {@code null}).
   */
  public AlertHistoryEntity getAlertHistory() {
    return alertHistory;
  }

  /**
   * Sets the associated {@link AlertHistoryEntity} entry for this current alert
   * instance. This will update the internal fields of this current alert with
   * those from the alertHistory.
   *
   * @param alertHistory
   *          the most recently received history entry (not {@code null}).
   */
  public void setAlertHistory(AlertHistoryEntity alertHistory) {
    this.alertHistory = alertHistory;
    historyId = alertHistory.getAlertId();
    alertDefinition = alertHistory.getAlertDefinition();
    definitionId = alertHistory.getAlertDefinitionId();
    latestText = alertHistory.getAlertText();
  }

  /**
   * Gets the equality to another alert based on the following criteria:
   * <ul>
   * <li>{@link #alertId}
   * <li>{@link #alertHistory}
   * </ul>
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

    AlertCurrentEntity that = (AlertCurrentEntity) object;
    EqualsBuilder equalsBuilder = new EqualsBuilder();

    equalsBuilder.append(alertId, that.alertId);
    equalsBuilder.append(alertHistory, that.alertHistory);
    return equalsBuilder.isEquals();
  }

  /**
   * Generates a hash for the current alert based on the following criteria:
   * <ul>
   * <li>{@link #alertId}
   * <li>{@link #alertHistory}
   * </ul>
   * <p/>
   * For new alerts, the associated {@link AlertHistoryEntity} may not be
   * persisted yet. This will rely on the
   * {@link AlertHistoryEntity#equals(Object)} and
   * {@link AlertHistoryEntity#hashCode()} being correct.
   * <p/>
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(alertId, alertHistory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("AlertCurrentEntity{");
    buffer.append("alertId=").append(alertId);
    if( null != alertDefinition) {
      buffer.append(", name=").append(alertDefinition.getDefinitionName());
    }

    if (null != alertHistory) {
      buffer.append(", state=").append(alertHistory.getAlertState());
    }

    buffer.append(", latestTimestamp=").append(latestTimestamp);

    buffer.append("}");
    return buffer.toString();
  }
}
