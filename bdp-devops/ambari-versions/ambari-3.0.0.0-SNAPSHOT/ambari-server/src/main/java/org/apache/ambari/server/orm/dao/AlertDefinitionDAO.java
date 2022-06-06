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
package org.apache.ambari.server.orm.dao;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.AlertGroupUpdate;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.internal.AlertDefinitionResourceProvider;
import org.apache.ambari.server.events.AlertDefinitionChangedEvent;
import org.apache.ambari.server.events.AlertDefinitionDeleteEvent;
import org.apache.ambari.server.events.AlertDefinitionRegistrationEvent;
import org.apache.ambari.server.events.AlertGroupsUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertDefinitionDAO} class is used to manage the persistence and
 * retrieval of {@link AlertDefinitionEntity} instances.
 */
@Singleton
public class AlertDefinitionDAO {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertDefinitionDAO.class);

  /**
   * JPA entity manager
   */
  @Inject
  private Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  /**
   * Alert history DAO.
   */
  @Inject
  private AlertsDAO alertsDao;

  /**
   * Alert dispatch DAO.
   */
  @Inject
  private AlertDispatchDAO dispatchDao;

  /**
   * Publishes the following events:
   * <ul>
   * <li>{@link AlertDefinitionRegistrationEvent} when new alerts are merged
   * from the stack or created from the {@link AlertDefinitionResourceProvider}</li>
   * <li>{@link AlertDefinitionChangedEvent} when alerts are updated.</li>
   * <li>{@link AlertDefinitionDeleteEvent} when alerts are removed</li>
   * </ul>
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  /**
   * A factory that assists in the creation of {@link AlertDefinition} and
   * {@link AlertDefinitionEntity}.
   */
  @Inject
  private AlertDefinitionFactory alertDefinitionFactory;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  /**
   * Gets an alert definition with the specified ID.
   *
   * @param definitionId
   *          the ID of the definition to retrieve.
   * @return the alert definition or {@code null} if none exists.
   */
  @RequiresSession
  public AlertDefinitionEntity findById(long definitionId) {
    return entityManagerProvider.get().find(AlertDefinitionEntity.class,
        definitionId);
  }

  /**
   * Gets an alert definition with the specified name. Alert definition names
   * are unique within a cluster.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param definitionName
   *          the name of the definition (not {@code null}).
   * @return the alert definition or {@code null} if none exists.
   */
  @RequiresSession
  public AlertDefinitionEntity findByName(long clusterId, String definitionName) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByName", AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("definitionName", definitionName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all alert definitions stored in the database.
   *
   * @return all alert definitions or an empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findAll() {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findAll", AlertDefinitionEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions stored in the database.
   *
   * @return all alert definitions or empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findAll(long clusterId) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findAllInCluster", AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all enabled alert definitions stored in the database for the specified
   * cluster.
   *
   * @return all enabled alert definitions or empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findAllEnabled(long clusterId) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findAllEnabledInCluster",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    return daoUtils.selectList(query);
  }

  /**
   * Gets all of the alert definitions for the list of IDs given.
   *
   * @param definitionIds
   *          the IDs of the definitions to retrieve.
   * @return the definition or an empty list (never {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findByIds(List<Long> definitionIds) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByIds", AlertDefinitionEntity.class);

    query.setParameter("definitionIds", definitionIds);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions for the given service in the specified cluster.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service.
   *
   * @return all alert definitions for the service or empty list if none exist
   *         (never {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findByService(long clusterId,
      String serviceName) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByService", AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions for the specified services that do not have a
   * component and do not belong to AGGREGATE source type. These definitions are assumed to be run on the master hosts.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param services
   *          the services to match on.
   *
   * @return all alert definitions for the services or empty list if none exist
   *         (never {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findByServiceMaster(long clusterId,
      Set<String> services) {
    if (null == services || services.size() == 0) {
      return Collections.emptyList();
    }

    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByServiceMaster",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("services", services);
    query.setParameter("scope", Scope.SERVICE);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions that are not bound to a particular service. An
   * example of this type of definition is a host capacity alert.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service (not {@code null}).
   * @param componentName
   *          the name of the service component (not {@code null}).
   * @return all alert definitions that are not bound to a service or an empty
   *         list (never {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findByServiceComponent(long clusterId,
      String serviceName, String componentName) {
    if (null == serviceName || null == componentName) {
      return Collections.emptyList();
    }

    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByServiceAndComponent",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions that are not bound to a particular service. An
   * example of this type of definition is a host capacity alert.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @return all alert definitions that are not bound to a service or an empty
   *         list (never {@code null}).
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findAgentScoped(long clusterId) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByServiceAndComponent",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);

    query.setParameter("serviceName",
        RootService.AMBARI.name());

    query.setParameter("componentName",
        RootComponent.AMBARI_AGENT.name());

    return daoUtils.selectList(query);
  }

  /**
   * @return all definitions with the given sourceType
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findBySourceType(Long clusterId, SourceType sourceType) {
    return daoUtils.selectList(
      entityManagerProvider.get()
        .createNamedQuery("AlertDefinitionEntity.findBySourceType", AlertDefinitionEntity.class)
        .setParameter("clusterId", clusterId)
        .setParameter("sourceType", sourceType));
  }

  /**
   * Persists a new alert definition, also creating the associated
   * {@link AlertGroupEntity} relationship for the definition's service default
   * group. Fires an {@link AlertDefinitionRegistrationEvent}.
   *
   * @param alertDefinition
   *          the definition to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertDefinitionEntity alertDefinition)
      throws AmbariException {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(alertDefinition);

    AlertGroupEntity group = dispatchDao.findDefaultServiceGroup(alertDefinition.getClusterId(),
        alertDefinition.getServiceName());

    if (null == group) {
      // create the default alert group for the new service; this MUST be done
      // before adding definitions so that they are properly added to the
      // default group
      String serviceName = alertDefinition.getServiceName();
      group = dispatchDao.createDefaultGroup(alertDefinition.getClusterId(), serviceName);
    }

    group.addAlertDefinition(alertDefinition);
    AlertGroupsUpdateEvent alertGroupsUpdateEvent = new AlertGroupsUpdateEvent(Collections.singletonList(
        new AlertGroupUpdate(group)),
        UpdateEventType.UPDATE);
    STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
    dispatchDao.merge(group);

    // publish the alert definition registration
    AlertDefinition coerced = alertDefinitionFactory.coerce(alertDefinition);
    if (null != coerced) {
      AlertDefinitionRegistrationEvent event = new AlertDefinitionRegistrationEvent(
          alertDefinition.getClusterId(), coerced);
      eventPublisher.publish(event);
    } else {
      LOG.warn("Unable to broadcast alert registration event for {}",
          alertDefinition.getDefinitionName());
    }

    entityManager.refresh(alertDefinition);
  }

  /**
   * Refresh the state of the alert definition from the database.
   *
   * @param alertDefinition
   *          the definition to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertDefinitionEntity alertDefinition) {
    entityManagerProvider.get().refresh(alertDefinition);
  }

  /**
   * Merge the speicified alert definition with the existing definition in the
   * database. Fires an {@link AlertDefinitionChangedEvent}.
   *
   * @param alertDefinition
   *          the definition to merge (not {@code null}).
   * @return the updated definition with merged content (never {@code null}).
   */
  @Transactional
  public AlertDefinitionEntity merge(AlertDefinitionEntity alertDefinition) {
    AlertDefinitionEntity entity = entityManagerProvider.get().merge(alertDefinition);

    AlertDefinition definition = alertDefinitionFactory.coerce(entity);

    AlertDefinitionChangedEvent event = new AlertDefinitionChangedEvent(
        alertDefinition.getClusterId(), definition);

    eventPublisher.publish(event);

    return entity;
  }

  /**
   * Creates or updates the specified entity. This method will check
   * {@link AlertDefinitionEntity#getDefinitionId()} in order to determine
   * whether the entity should be created or merged.
   *
   * @param alertDefinition
   *          the definition to create or update (not {@code null}).
   */
  public void createOrUpdate(AlertDefinitionEntity alertDefinition)
      throws AmbariException {
    if (null == alertDefinition.getDefinitionId()) {
      create(alertDefinition);
    } else {
      merge(alertDefinition);
    }
  }

  /**
   * Removes the specified alert definition and all related history and
   * associations from the database. Fires an {@link AlertDefinitionDeleteEvent}
   * .
   *
   * @param alertDefinition
   *          the definition to remove.
   */
  @Transactional
  public void remove(AlertDefinitionEntity alertDefinition) {
    dispatchDao.removeNoticeByDefinitionId(alertDefinition.getDefinitionId());
    alertsDao.removeByDefinitionId(alertDefinition.getDefinitionId());

    EntityManager entityManager = entityManagerProvider.get();

    alertDefinition = findById(alertDefinition.getDefinitionId());
    if (null != alertDefinition) {
      entityManager.remove(alertDefinition);

      // publish the alert definition removal
      AlertDefinition coerced = alertDefinitionFactory.coerce(alertDefinition);
      if (null != coerced) {
        AlertDefinitionDeleteEvent event = new AlertDefinitionDeleteEvent(
                alertDefinition.getClusterId(), coerced);

        eventPublisher.publish(event);
      } else {
        LOG.warn("Unable to broadcast alert removal event for {}",
                alertDefinition.getDefinitionName());
      }
    }
  }

  /**
   * Removes all {@link AlertDefinitionEntity} that are associated with the
   * specified cluster ID.
   *
   * @param clusterId
   *          the cluster ID.
   */
  @Transactional
  public void removeAll(long clusterId) {
    List<AlertDefinitionEntity> definitions = findAll(clusterId);
    for (AlertDefinitionEntity definition : definitions) {
      remove(definition);
    }
  }
}
