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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.AlertHashInvalidationEvent;
import org.apache.ambari.server.events.HostsAddedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.AmbariServiceAlertDefinitions;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertHostListener} class handles {@link HostsAddedEvent} and
 * {@link HostsRemovedEvent} and ensures that {@link AlertCurrentEntity}
 * instances are properly cleaned up
 */
@Singleton
@EagerSingleton
public class AlertHostListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertHostListener.class);

  /**
   * Used for removing current alerts when a service is removed.
   */
  @Inject
  private AlertsDAO m_alertsDao;

  /**
   * Used for checking to see if definitions already exist for a cluster.
   */
  @Inject
  private AlertDefinitionDAO m_alertDefinitionDao;

  /**
   * Used to publish events when an alert definition has a lifecycle event.
   */
  @Inject
  private AmbariEventPublisher m_eventPublisher;

  /**
   * All of the {@link AlertDefinition}s that are scoped for the agents.
   */
  @Inject
  private AmbariServiceAlertDefinitions m_ambariServiceAlertDefinitions;

  /**
   * Used when a host is added to a cluster to coerce an {@link AlertDefinition}
   * into an {@link AlertDefinitionEntity}.
   */
  @Inject
  private AlertDefinitionFactory m_alertDefinitionFactory;

  /**
   * Used to prevent multiple threads from trying to create host alerts
   * simultaneously.
   */
  private Lock m_hostAlertLock = new ReentrantLock();

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertHostListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Handles the {@link HostsAddedEvent} by performing the following actions:
   * <ul>
   * <li>Ensures that all host-level alerts are loaded for the cluster. This is
   * especially useful when creating a cluster and no alerts were loaded on
   * Ambari startup</li>
   * <li>Broadcasts the {@link AlertHashInvalidationEvent} in order to push host
   * alert definitions</li>
   * </ul>
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(HostsAddedEvent event) {
    LOG.debug("Received event {}", event);

    long clusterId = event.getClusterId();

    // load the host-only alert definitions
    List<AlertDefinition> agentDefinitions = m_ambariServiceAlertDefinitions.getAgentDefinitions();
    List<AlertDefinition> serverDefinitions = m_ambariServiceAlertDefinitions.getServerDefinitions();

    List<AlertDefinition> ambariServiceDefinitions = new ArrayList<>();
    ambariServiceDefinitions.addAll(agentDefinitions);
    ambariServiceDefinitions.addAll(serverDefinitions);

    // lock to prevent multiple threads from trying to create alert
    // definitions at the same time
    m_hostAlertLock.lock();

    try {
      for (AlertDefinition agentDefinition : ambariServiceDefinitions) {
        AlertDefinitionEntity definition = m_alertDefinitionDao.findByName(
            clusterId, agentDefinition.getName());

        // this host definition does not exist, add it
        if (null == definition) {
          definition = m_alertDefinitionFactory.coerce(clusterId,
              agentDefinition);

          try {
            m_alertDefinitionDao.create(definition);
          } catch (Exception e) {
            LOG.error(
                "Unable to create an alert definition named {} in cluster {}",
                definition.getDefinitionName(), definition.getClusterId(), e);
          }
        }
      }
    } finally {
      m_hostAlertLock.unlock();
    }

    for (String hostName : event.getHostNames()) {
      AlertHashInvalidationEvent invalidationEvent = new AlertHashInvalidationEvent(
        event.getClusterId(), Collections.singletonList(hostName));

      m_eventPublisher.publish(invalidationEvent);
    }
  }

  /**
   * Handles the {@link HostsRemovedEvent} by performing the following actions:
   * <ul>
   * <li>Removes all {@link AlertCurrentEntity} for the removed hosts</li>
   * </ul>
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(HostsRemovedEvent event) {
    LOG.debug("Received event {}", event);

    // remove any current alerts for the removed hosts
    for (String hostName : event.getHostNames()) {
      m_alertsDao.removeCurrentByHost(hostName);
    }
  }
}
