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
package org.apache.ambari.server.events;

import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;

/**
 * The {@link InitialAlertEvent} is fired the first time that an alert is
 * received from an agent. This is different from a
 * {@link AlertStateChangeEvent} since there is no state change occurring for
 * the first alert being received.
 */
public class InitialAlertEvent extends AlertEvent {

  /**
   * The current alert, including state and history.
   */
  private final AlertCurrentEntity m_currentAlert;

  /**
   * The historical record for the initial alert.
   */
  private final AlertHistoryEntity m_history;

  /**
   * Constructor.
   *
   * @param clusterId
   *          the ID of the cluster
   * @param alert
   *          the alert that was received.
   * @param currentAlert
   *          the current alert created or updated.
   */
  public InitialAlertEvent(long clusterId, Alert alert,
      AlertCurrentEntity currentAlert) {
    super(clusterId, alert);

    m_currentAlert = currentAlert;
    m_history = currentAlert.getAlertHistory();
  }

  /**
   * Gets the current alert.
   *
   * @return the current alert.
   */
  public AlertCurrentEntity getCurrentAlert() {
    return m_currentAlert;
  }

  /**
   * Gets the newly created item in alert history.
   *
   * @return the newly created historical item.
   */
  public AlertHistoryEntity getNewHistoricalEntry() {
    return m_history;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("InitialAlertEvent{");
    buffer.append("cluserId=").append(m_clusterId);
    buffer.append(", alert=").append(m_alert);

    buffer.append("}");
    return buffer.toString();
  }
}
