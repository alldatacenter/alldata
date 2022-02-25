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

import java.util.List;

import org.apache.ambari.server.state.Alert;

/**
 * The {@link AlertReceivedEvent} is fired when an {@link Alert} is received or
 * generated.
 */
public final class AlertReceivedEvent extends AlertEvent {

  /**
   * Constructor.
   *
   * @param clusterId
   * @param alert
   */
  public AlertReceivedEvent(long clusterId, Alert alert) {
    super(clusterId, alert);
  }

  public AlertReceivedEvent(List<Alert> alerts) {
    super(alerts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("AlertReceivedEvent{");
    buffer.append("cluserId=").append(m_clusterId);
    buffer.append(", alerts=").append(getAlerts());
    buffer.append("}");
    return buffer.toString();
  }
}
