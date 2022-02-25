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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.NotificationState;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * The {@link AlertNoticeEntity} class represents the need to dispatch a
 * notification to an {@link AlertTargetEntity}. There are three
 * {@link NotificationState}s that a single notice can exist in. These instances
 * are persisted indefinitely for historical reference.
 *
 */
@Entity
@Table(name = "alert_notice")
@TableGenerator(name = "alert_notice_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "alert_notice_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "AlertNoticeEntity.findAll", query = "SELECT notice FROM AlertNoticeEntity notice"),
  @NamedQuery(name = "AlertNoticeEntity.findByState", query = "SELECT notice FROM AlertNoticeEntity notice WHERE notice.notifyState = :notifyState  ORDER BY  notice.notificationId"),
  @NamedQuery(name = "AlertNoticeEntity.findByUuid", query = "SELECT notice FROM AlertNoticeEntity notice WHERE notice.uuid = :uuid"),
  @NamedQuery(name = "AlertNoticeEntity.findByHistoryIds", query = "SELECT notice FROM AlertNoticeEntity notice WHERE notice.historyId IN :historyIds"),
  // The remove query can be handled by a simpler JPQL query,
  // however, MySQL gtid enforce policy gets violated due to creation and
  // deletion of TEMP table in the same transaction
  @NamedQuery(name = "AlertNoticeEntity.removeByHistoryIds", query = "DELETE FROM AlertNoticeEntity notice WHERE notice.historyId IN :historyIds")
})
public class AlertNoticeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_notice_id_generator")
  @Column(name = "notification_id", nullable = false, updatable = false)
  private Long notificationId;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "notify_state", nullable = false, length = 255)
  private NotificationState notifyState;

  @Basic
  @Column(nullable = false, length = 64, unique = true)
  private String uuid;

  /**
   * Bi-directional many-to-one association to {@link AlertHistoryEntity}.
   */
  @ManyToOne
  @JoinColumn(name = "history_id", nullable = false)
  private AlertHistoryEntity alertHistory;

  @Column(name = "history_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long historyId;

  /**
   * Bi-directional many-to-one association to {@link AlertTargetEntity}
   */
  @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.REFRESH })
  @JoinColumn(name = "target_id", nullable = false)
  private AlertTargetEntity alertTarget;

  /**
   * Constructor.
   */
  public AlertNoticeEntity() {
  }

  /**
   * Gets the unique ID for this alert notice.
   *
   * @return the ID (never {@code null}).
   */
  public Long getNotificationId() {
    return notificationId;
  }

  /**
   * Sets the unique ID for this alert notice.
   *
   * @param notificationId
   *          the ID (not {@code null}).
   */
  public void setNotificationId(Long notificationId) {
    this.notificationId = notificationId;
  }

  /**
   * Gets the notification state for this alert notice. Alert notices are
   * pending until they are processed, after which they will either be
   * successful or failed.
   *
   * @return the notification state (never {@code null}).
   */
  public NotificationState getNotifyState() {
    return notifyState;
  }

  /**
   * Sets the notification state for this alert notice.
   *
   * @param notifyState
   *          the notification state (not {@code null}).
   */
  public void setNotifyState(NotificationState notifyState) {
    this.notifyState = notifyState;
  }

  /**
   * Gets the unique ID for this alert notice.
   *
   * @return the unique ID (never {@code null}).
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Sets the unique ID for this alert notice.
   *
   * @param uuid
   *          the unique ID (not {@code null}).
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets the associated alert history entry for this alert notice.
   *
   * @return the historical event that traiggered this notice's creation (never
   *         {@code null}).
   */
  public AlertHistoryEntity getAlertHistory() {
    return alertHistory;
  }

  /**
   * Sets the associated alert history entry for this alert notice.
   *
   * @param alertHistory
   *          the historical event that traiggered this notice's creation (not
   *          {@code null}).
   */
  public void setAlertHistory(AlertHistoryEntity alertHistory) {
    this.alertHistory = alertHistory;
    historyId = alertHistory.getAlertId();
  }

  /**
   * Get parent AlertHistory id
   * @return history id
   */
  public Long getHistoryId() {
    return historyId;
  }

  /**
   * Set parent Alert History id
   * @param historyId history id
   */
  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  /**
   * Gets the intended audience for the notification.
   *
   * @return the recipient of this notification (never {@code null}).
   */
  public AlertTargetEntity getAlertTarget() {
    return alertTarget;
  }

  /**
   * Sets the intended audience for the notification.
   *
   * @param alertTarget
   *          the recipient of this notification (not {@code null}).
   */
  public void setAlertTarget(AlertTargetEntity alertTarget) {
    this.alertTarget = alertTarget;
    alertTarget.addAlertNotice(this);
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

    AlertNoticeEntity that = (AlertNoticeEntity) object;

    EqualsBuilder equalsBuilder = new EqualsBuilder();
    equalsBuilder.append(uuid, that.uuid);
    return equalsBuilder.isEquals();
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    int result = null != uuid ? uuid.hashCode() : 0;
    return result;
  }

}
