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

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * The {@link AlertServiceComponentHostListener} handles event relating to the
 * disabling of an alert definition.
 */
@EagerSingleton
public class AlertServiceComponentHostListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertServiceComponentHostListener.class);

  /**
   * Used for deleting the alert notices when a definition is disabled.
   */
  @Inject
  private AlertsDAO m_alertsDao = null;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertServiceComponentHostListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Removes any {@link AlertCurrentEntity} for the given service, component and
   * host.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(ServiceComponentUninstalledEvent event) {
    LOG.debug("Received event {}", event);

    long clusterId = event.getClusterId();
    String serviceName = event.getServiceName();
    String componentName = event.getComponentName();
    String hostName = event.getHostName();

    m_alertsDao.removeCurrentByServiceComponentHost(clusterId, serviceName, componentName,
        hostName);
  }
}
