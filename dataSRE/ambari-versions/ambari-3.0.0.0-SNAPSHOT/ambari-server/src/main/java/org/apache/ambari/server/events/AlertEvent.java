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

import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.state.Alert;

/**
 * The {@link AlertEvent} class is the base for all events related to alerts.
 */
public abstract class AlertEvent {

  protected long m_clusterId;
  protected Alert m_alert;
  protected List<Alert> m_alerts;

  /**
   * Constructor.
   *
   * @param clusterId
   * @param alert
   */
  public AlertEvent(long clusterId, Alert alert) {
    m_clusterId = clusterId;
    m_alert = alert;
  }

  public AlertEvent(List<Alert> m_alerts) {
    this.m_alerts = m_alerts;
  }

  /**
   * Gets the cluster ID that the alert belongs to.
   *
   * @return the ID of the cluster.
   */
  public long getClusterId() {
    return m_clusterId;
  }

  /**
   * Gets the alert that this event is created for.
   *
   * @return the alert (never {@code null}).
   */
  public Alert getAlert(){
    return m_alert;
  }

  /**
   * Gets list of alerts this event was created for
   * @return
   */
  public List<Alert> getAlerts() {
    if (m_alerts != null) {
      return m_alerts;
    } else {
      return Collections.singletonList(m_alert);
    }
  }
}
