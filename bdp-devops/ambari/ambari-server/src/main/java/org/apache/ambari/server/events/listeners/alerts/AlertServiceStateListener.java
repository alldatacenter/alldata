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

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertServiceStateListener} class handles
 * {@link ServiceInstalledEvent} and {@link ServiceRemovedEvent} and ensures
 * that {@link AlertDefinitionEntity} and {@link AlertGroupEntity} instances are
 * correctly populated or cleaned up.
 */
@Singleton
@EagerSingleton
public class AlertServiceStateListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertServiceStateListener.class);

  /**
   * Services metainfo; injected lazily as a {@link Provider} since JPA is not
   * fully initialized when this singleton is eagerly instantiated. See
   * {@link AmbariServer#main(String[])} and the ordering of
   * {@link ControllerModule} and {@link GuiceJpaInitializer}.
   */
  @Inject
  private Provider<AmbariMetaInfo> m_metaInfoProvider;

  /**
   * Used when a service is installed to read alert definitions from the stack
   * and coerce them into {@link AlertDefinitionEntity}.
   */
  @Inject
  private AlertDefinitionFactory m_alertDefinitionFactory;

  /**
   * Used when a service is installed to insert a default
   * {@link AlertGroupEntity} into the database.
   */
  @Inject
  private AlertDispatchDAO m_alertDispatchDao;

  /**
   * Used when a service is installed to insert {@link AlertDefinitionEntity}
   * into the database or when a service is removed to delete the definition.
   */
  @Inject
  private AlertDefinitionDAO m_definitionDao;

  /**
   * Used to retrieve a cluster using clusterId from event.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Used for ensuring that the concurrent nature of the event handler methods
   * don't collide when attempting to perform operations on the same service.
   */
  private Striped<Lock> m_locksByService = Striped.lazyWeakLock(20);

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertServiceStateListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Handles service installed events by populating the database with all known
   * alert definitions for the newly installed service and creates the service's
   * default alert group.
   *
   * @param event
   *          the published event being handled (not {@code null}).
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(ServiceInstalledEvent event) {
    LOG.debug("Received event {}", event);

    long clusterId = event.getClusterId();
    String stackName = event.getStackName();
    String stackVersion = event.getStackVersion();
    String serviceName = event.getServiceName();

    Lock lock = m_locksByService.get(serviceName);
    lock.lock();

    try {
      // create the default alert group for the new service if absent; this MUST
      // be done before adding definitions so that they are properly added to the
      // default group
      if (null == m_alertDispatchDao.findDefaultServiceGroup(clusterId, serviceName)) {
        try {
          m_alertDispatchDao.createDefaultGroup(clusterId, serviceName);
        } catch (AmbariException ambariException) {
          LOG.error("Unable to create a default alert group for {}",
            event.getServiceName(), ambariException);
        }
      }

      // populate alert definitions for the new service from the database, but
      // don't worry about sending down commands to the agents; the host
      // components are not yet bound to the hosts so we'd have no way of knowing
      // which hosts are invalidated; do that in another impl
      try {
        Set<AlertDefinition> alertDefinitions = m_metaInfoProvider.get().getAlertDefinitions(
            stackName, stackVersion, serviceName);

        for (AlertDefinition definition : alertDefinitions) {
          AlertDefinitionEntity entity = m_alertDefinitionFactory.coerce(
              clusterId,
              definition);

          m_definitionDao.create(entity);
        }
      } catch (AmbariException ae) {
        String message = MessageFormat.format(
            "Unable to populate alert definitions from the database during installation of {0}",
            serviceName);
        LOG.error(message, ae);
      }
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Removes all alert data associated with the removed serviced.
   *
   * @param event
   *          the published event being handled (not {@code null}).
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(ServiceRemovedEvent event) {
    LOG.debug("Received event {}", event);

    try {
      m_clusters.get().getClusterById(event.getClusterId());
    } catch (AmbariException e) {
      LOG.warn("Unable to retrieve cluster with id {}", event.getClusterId());
      return;
    }

    String serviceName = event.getServiceName();
    Lock lock = m_locksByService.get(serviceName);
    lock.lock();

    try {
      List<AlertDefinitionEntity> definitions = m_definitionDao.findByService(event.getClusterId(),
          event.getServiceName());

      for (AlertDefinitionEntity definition : definitions) {
        try {
          m_definitionDao.remove(definition);
        } catch (Exception exception) {
          LOG.error("Unable to remove alert definition {}", definition.getDefinitionName(),
              exception);
        }
      }

      // remove the default group for the service
      AlertGroupEntity group = m_alertDispatchDao.findGroupByName(event.getClusterId(),
          event.getServiceName());

      if (null != group && group.isDefault()) {
        try {
          m_alertDispatchDao.remove(group);
        } catch (Exception exception) {
          LOG.error("Unable to remove default alert group {}", group.getGroupName(), exception);
        }
      }
    } finally {
      lock.unlock();
    }
  }
}
