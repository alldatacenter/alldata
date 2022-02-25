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
package org.apache.ambari.server.events.listeners.alerts;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.events.AlertDefinitionDisabledEvent;
import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * The {@link AlertDefinitionDisabledListener} handles event relating to the
 * disabling of an alert definition.
 */
@EagerSingleton
public class AlertDefinitionDisabledListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertDefinitionDisabledListener.class);

  /**
   * Used for deleting the alert notices when a definition is disabled.
   */
  @Inject
  private AlertsDAO m_alertsDao = null;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  /**
   * Constructor.
   *
   * @param publisher
   *          the publisher to register this listener with (not {@code null}).
   */
  @Inject
  public AlertDefinitionDisabledListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Removes any {@link AlertCurrentEntity} instance associated with the
   * specified alert definition.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(AlertDefinitionDisabledEvent event) {
    LOG.debug("Received event {}", event);

    m_alertsDao.removeCurrentDisabledAlerts();

    // send API STOMP alert update
    Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> alertUpdates = new HashMap<>();
    alertUpdates.put(event.getClusterId(), AlertSummaryGroupedRenderer.generateEmptySummary(event.getDefinitionId(),
        event.getDefinitionName()));
    STOMPUpdatePublisher.publish(new AlertUpdateEvent(alertUpdates));
  }
}
