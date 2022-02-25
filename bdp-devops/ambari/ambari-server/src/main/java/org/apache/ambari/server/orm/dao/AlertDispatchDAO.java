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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.AlertGroupUpdate;
import org.apache.ambari.server.api.query.JpaPredicateVisitor;
import org.apache.ambari.server.api.query.JpaSortBuilder;
import org.apache.ambari.server.controller.AlertNoticeRequest;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.events.AlertGroupsUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity_;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.Service;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertDispatchDAO} class manages the {@link AlertTargetEntity},
 * {@link AlertGroupEntity}, and the associations between them.
 */
@Singleton
public class AlertDispatchDAO {
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
   * Used to retrieve a cluster and its services when creating a default
   * {@link AlertGroupEntity} for a service.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  /**
   * Used for ensuring that the concurrent nature of the event handler methods
   * don't collide when attempting to creation alert groups for the same
   * service.
   */
  private Striped<Lock> m_locksByService = Striped.lazyWeakLock(20);

  private static final Logger LOG = LoggerFactory.getLogger(AlertDispatchDAO.class);

    /**
     * Gets an alert group with the specified ID.
     *
     * @param groupId
     *          the ID of the group to retrieve.
     * @return the group or {@code null} if none exists.
     */

  @RequiresSession
  public AlertGroupEntity findGroupById(long groupId) {
    return entityManagerProvider.get().find(AlertGroupEntity.class, groupId);
  }

  /**
   * Gets all of the alert groups for the list of IDs given.
   *
   * @param groupIds
   *          the IDs of the groups to retrieve.
   * @return the groups or an empty list (never {@code null}).
   */
  @RequiresSession
  public List<AlertGroupEntity> findGroupsById(List<Long> groupIds) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findByIds", AlertGroupEntity.class);

    query.setParameter("groupIds", groupIds);

    return daoUtils.selectList(query);
  }

  /**
   * Gets an alert target with the specified ID.
   *
   * @param targetId
   *          the ID of the target to retrieve.
   * @return the target or {@code null} if none exists.
   */
  @RequiresSession
  public AlertTargetEntity findTargetById(long targetId) {
    return entityManagerProvider.get().find(AlertTargetEntity.class, targetId);
  }

  /**
   * Gets all of the alert targets for the list of IDs given.
   *
   * @param targetIds
   *          the IDs of the targets to retrieve.
   * @return the targets or an empty list (never {@code null}).
   */
  @RequiresSession
  public List<AlertTargetEntity> findTargetsById(List<Long> targetIds) {
    TypedQuery<AlertTargetEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertTargetEntity.findByIds", AlertTargetEntity.class);

    query.setParameter("targetIds", targetIds);

    return daoUtils.selectList(query);
  }

  /**
   * Gets a notification with the specified ID.
   *
   * @param noticeId
   *          the ID of the notification to retrieve.
   * @return the notification or {@code null} if none exists.
   */
  @RequiresSession
  public AlertNoticeEntity findNoticeById(long noticeId) {
    return entityManagerProvider.get().find(AlertNoticeEntity.class, noticeId);
  }

  /**
   * Gets a notification with the specified UUID.
   *
   * @param uuid
   *          the UUID of the notification to retrieve.
   * @return the notification or {@code null} if none exists.
   */
  @RequiresSession
  public AlertNoticeEntity findNoticeByUuid(String uuid) {
    TypedQuery<AlertNoticeEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertNoticeEntity.findByUuid", AlertNoticeEntity.class);

    query.setParameter("uuid", uuid);

    return daoUtils.selectOne(query);
  }

  /**
   * Gets all {@link AlertNoticeEntity} instances that are
   * {@link NotificationState#PENDING} and not yet dispatched.
   *
   * @return the notices that are waiting to be dispatched, or an empty list
   *         (never {@code null}).
   */
  @RequiresSession
  public List<AlertNoticeEntity> findPendingNotices() {
    TypedQuery<AlertNoticeEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertNoticeEntity.findByState", AlertNoticeEntity.class);

    query.setParameter("notifyState", NotificationState.PENDING);
    return daoUtils.selectList(query);
  }

  /**
   * Gets an alert group with the specified name for the given cluster. Alert
   * group names are unique within a cluster.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param groupName
   *          the name of the group (not {@code null}).
   * @return the alert group or {@code null} if none exists.
   */
  @RequiresSession
  public AlertGroupEntity findGroupByName(long clusterId, String groupName) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findByNameInCluster", AlertGroupEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("groupName", groupName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets an alert target with the specified name. Alert target names are unique
   * across all clusters.
   *
   * @param targetName
   *          the name of the target (not {@code null}).
   * @return the alert target or {@code null} if none exists.
   */
  @RequiresSession
  public AlertTargetEntity findTargetByName(String targetName) {
    TypedQuery<AlertTargetEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertTargetEntity.findByName", AlertTargetEntity.class);

    query.setParameter("targetName", targetName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all alert groups stored in the database across all clusters.
   *
   * @return all alert groups or empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertGroupEntity> findAllGroups() {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findAll", AlertGroupEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert groups stored in the database for the specified cluster.
   *
   * @return all alert groups in the specified cluster or empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertGroupEntity> findAllGroups(long clusterId) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findAllInCluster", AlertGroupEntity.class);

    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert targets stored in the database.
   *
   * @return all alert targets or empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertTargetEntity> findAllTargets() {
    TypedQuery<AlertTargetEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertTargetEntity.findAll", AlertTargetEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all global alert targets stored in the database.
   *
   * @return all global alert targets or empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertTargetEntity> findAllGlobalTargets() {
    TypedQuery<AlertTargetEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertTargetEntity.findAllGlobal", AlertTargetEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all of the {@link AlertGroupEntity} instances that include the
   * specified alert definition. Service default groups will also be returned.
   *
   * @param definitionEntity
   *          the definition that the group must include (not {@code null}).
   * @return all alert groups that have an association with the specified
   *         definition and the definition's service default group or empty list
   *         if none exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertGroupEntity> findGroupsByDefinition(
      AlertDefinitionEntity definitionEntity) {

    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findByAssociatedDefinition", AlertGroupEntity.class);

    query.setParameter("alertDefinition", definitionEntity);
    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the default group for the specified cluster and service.
   *
   * @param clusterId
   *          the cluster that the group belongs to
   * @param serviceName
   *          the name of the service (not {@code null}).
   * @return the default group, or {@code null} if the service name is not valid
   *         for an installed service; otherwise {@code null} should not be
   *         possible.
   */
  @RequiresSession
  public AlertGroupEntity findDefaultServiceGroup(long clusterId,
      String serviceName) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findServiceDefaultGroup", AlertGroupEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all alert notifications stored in the database.
   *
   * @return all alert notifications or empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertNoticeEntity> findAllNotices() {
    TypedQuery<AlertNoticeEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertNoticeEntity.findAll", AlertNoticeEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert notifications stored in the database that match the given
   * predicate, pagination, and sorting.
   *
   * @param request
   * @return all alert notifications or empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertNoticeEntity> findAllNotices(AlertNoticeRequest request) {
    EntityManager entityManager = entityManagerProvider.get();

    // convert the Ambari predicate into a JPA predicate
    NoticePredicateVisitor visitor = new NoticePredicateVisitor();
    PredicateHelper.visit(request.Predicate, visitor);

    CriteriaQuery<AlertNoticeEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (null != jpaPredicate) {
      query.where(jpaPredicate);
    }

    // sorting
    JpaSortBuilder<AlertNoticeEntity> sortBuilder = new JpaSortBuilder<>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(request.Sort, visitor);
    query.orderBy(sortOrders);

    // pagination
    TypedQuery<AlertNoticeEntity> typedQuery = entityManager.createQuery(query);
    if (null != request.Pagination) {
      typedQuery.setFirstResult(request.Pagination.getOffset());
      typedQuery.setMaxResults(request.Pagination.getPageSize());
    }

    return daoUtils.selectList(typedQuery);
  }

  /**
   * Gets the total count of all {@link AlertNoticeEntity} rows that match the
   * specified {@link Predicate}.
   *
   * @param predicate
   *          the predicate to apply, or {@code null} for none.
   * @return the total count of rows that would be returned in a result set.
   */
  @RequiresSession
  public int getNoticesCount(Predicate predicate) {
    return 0;
  }

  /**
   * Persists new alert groups.
   *
   * @param entities
   *          the groups to persist (not {@code null}).
   */
  @Transactional
  public void createGroups(List<AlertGroupEntity> entities) {
    if (null == entities) {
      return;
    }

    List<AlertGroupUpdate> alertGroupUpdates = new ArrayList<>(entities.size());
    for (AlertGroupEntity entity : entities) {
      create(entity, false);
      alertGroupUpdates.add(new AlertGroupUpdate(entity));
    }
    AlertGroupsUpdateEvent alertGroupsUpdateEvent = new AlertGroupsUpdateEvent(alertGroupUpdates,
        UpdateEventType.CREATE);
    STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
  }

  /**
   * Persists a new alert group.
   *
   * @param group
   *          the group to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertGroupEntity group) {
    create(group, true);
  }

  /**
   * Persists a new alert group.
   *
   * @param group the group to persist (not {@code null}).
   * @param fireEvent should alert group update event to be fired
   */
  @Transactional
  public void create(AlertGroupEntity group, boolean fireEvent) {

    entityManagerProvider.get().persist(group);

    // associate the group with all alert targets
    List<AlertTargetEntity> targets = findAllGlobalTargets();
    if (!targets.isEmpty()) {
      for (AlertTargetEntity target : targets) {
        group.addAlertTarget(target);
      }
      entityManagerProvider.get().merge(group);
    }
    if (fireEvent) {
      AlertGroupsUpdateEvent alertGroupsUpdateEvent = new AlertGroupsUpdateEvent(
          Collections.singletonList(new AlertGroupUpdate(group)),
          UpdateEventType.CREATE);
      STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
    }
  }

  /**
   * Creates a default group in the specified cluster and service. If the
   * service is not valid, then this will throw an {@link AmbariException}.
   *
   * @param clusterId
   *          the cluster that the group is in.
   * @param serviceName
   *          the name of the group which is also the service name.
   */
  @Transactional
  public AlertGroupEntity createDefaultGroup(long clusterId, String serviceName)
      throws AmbariException {

    // AMBARI is a special service that we let through, otherwise we need to
    // verify that the service exists before we create the default group
    String ambariServiceName = RootService.AMBARI.name();
    if (!ambariServiceName.equals(serviceName)) {
      Cluster cluster = m_clusters.get().getClusterById(clusterId);
      Map<String, Service> services = cluster.getServices();

      if (!services.containsKey(serviceName)) {
        String message = MessageFormat.format(
            "Unable to create a default alert group for unknown service {0} in cluster {1}",
            serviceName, cluster.getClusterName());
        throw new AmbariException(message);
      }
    }

    Lock lock = m_locksByService.get(serviceName);
    lock.lock();

    try {
      AlertGroupEntity group = findDefaultServiceGroup(clusterId, serviceName);
      if (null != group) {
        return group;
      }

      group = new AlertGroupEntity();
      group.setClusterId(clusterId);
      group.setDefault(true);
      group.setGroupName(serviceName);
      group.setServiceName(serviceName);

      create(group);
      return group;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Refresh the state of the alert group from the database.
   *
   * @param alertGroup
   *          the group to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertGroupEntity alertGroup) {
    entityManagerProvider.get().refresh(alertGroup);
  }

  /**
   * Merge the speicified alert group with the existing group in the database.
   *
   * @param alertGroup
   *          the group to merge (not {@code null}).
   * @return the updated group with merged content (never {@code null}).
   */
  @Transactional
  public AlertGroupEntity merge(AlertGroupEntity alertGroup) {
    return entityManagerProvider.get().merge(alertGroup);
  }

  /**
   * Removes the specified alert group from the database.
   *
   * @param alertGroup
   *          the group to remove.
   */
  @Transactional
  public void remove(AlertGroupEntity alertGroup) {
    remove(alertGroup, true);
  }

  /**
   * Removes the specified alert group from the database.
   *
   * @param alertGroup the group to remove.
   * @param fireEvent should alert group update event to be fired.
   */
  @Transactional
  public void remove(AlertGroupEntity alertGroup, boolean fireEvent) {
    entityManagerProvider.get().remove(merge(alertGroup));
    if (fireEvent) {
      AlertGroupsUpdateEvent alertGroupsUpdateEvent = AlertGroupsUpdateEvent.deleteAlertGroupsUpdateEvent(
          Collections.singletonList(alertGroup.getGroupId()));
      STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
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
  public void removeAllGroups(long clusterId) {
    List<AlertGroupEntity> groups = findAllGroups(clusterId);
    for (AlertGroupEntity group : groups) {
      remove(group, false);
    }
    AlertGroupsUpdateEvent alertGroupsUpdateEvent = AlertGroupsUpdateEvent.deleteAlertGroupsUpdateEvent(
        groups.stream().map(AlertGroupEntity::getGroupId).collect(Collectors.toList()));
    STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
  }

  /**
   * Persists new alert targets.
   *
   * @param entities
   *          the targets to persist (not {@code null}).
   */
  @Transactional
  public void createTargets(List<AlertTargetEntity> entities) {
    if (null == entities) {
      return;
    }

    for (AlertTargetEntity entity : entities) {
      create(entity);
    }
  }

  /**
   * Creates new alert notices using the {@link EntityManager#merge(Object)}
   * method to ensure that the associated {@link AlertTargetEntity} instances
   * are also updated.
   * <p/>
   * The method returns the newly managed entities as the ones passed in will
   * not be managed.
   *
   * @param entities
   *          the targets to create (not {@code null}).
   */
  @Transactional
  public List<AlertNoticeEntity> createNotices(List<AlertNoticeEntity> entities) {
    if (null == entities || entities.isEmpty()) {
      return entities;
    }

    List<AlertNoticeEntity> managedEntities = new ArrayList<>(entities.size());
    for (AlertNoticeEntity entity : entities) {
      AlertNoticeEntity managedEntity = merge(entity);
      managedEntities.add(managedEntity);
    }

    return managedEntities;
  }

  /**
   * Persists a new alert target.
   *
   * @param alertTarget
   *          the target to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertTargetEntity alertTarget) {
    entityManagerProvider.get().persist(alertTarget);

    if (alertTarget.isGlobal()) {
      List<AlertGroupEntity> groups = findAllGroups();
      for (AlertGroupEntity group : groups) {
        group.addAlertTarget(alertTarget);
        merge(group);
        AlertGroupsUpdateEvent alertGroupsUpdateEvent = new AlertGroupsUpdateEvent(Collections.singletonList(
            new AlertGroupUpdate(group)),
            UpdateEventType.UPDATE);
        STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
      }
    }
  }

  /**
   * Refresh the state of the alert target from the database.
   *
   * @param alertTarget
   *          the target to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertTargetEntity alertTarget) {
    entityManagerProvider.get().refresh(alertTarget);
  }

  /**
   * Merge the speicified alert target with the existing target in the database.
   *
   * @param alertTarget
   *          the target to merge (not {@code null}).
   * @return the updated target with merged content (never {@code null}).
   */
  @Transactional
  public AlertTargetEntity merge(AlertTargetEntity alertTarget) {
    return entityManagerProvider.get().merge(alertTarget);
  }

  /**
   * Removes the specified alert target from the database.
   *
   * @param alertTarget
   *          the target to remove.
   */
  @Transactional
  public void remove(AlertTargetEntity alertTarget) {
    List<AlertGroupUpdate> alertGroupUpdates = new ArrayList<>();
    for (AlertGroupEntity alertGroupEntity : alertTarget.getAlertGroups()) {
      AlertGroupUpdate alertGroupUpdate = new AlertGroupUpdate(alertGroupEntity);
      alertGroupUpdate.getTargets().remove(alertTarget.getTargetId());
      alertGroupUpdates.add(alertGroupUpdate);
    }
    STOMPUpdatePublisher.publish(new AlertGroupsUpdateEvent(alertGroupUpdates, UpdateEventType.UPDATE));
    entityManagerProvider.get().remove(alertTarget);
  }

  /**
   * Persists a new notification.
   *
   * @param alertNotice
   *          the notification to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertNoticeEntity alertNotice) {
    entityManagerProvider.get().persist(alertNotice);
  }

  /**
   * Refresh the state of the notification from the database.
   *
   * @param alertNotice
   *          the notification to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertNoticeEntity alertNotice) {
    entityManagerProvider.get().refresh(alertNotice);
  }

  /**
   * Merge the specified notification with the existing target in the database.
   *
   * @param alertNotice
   *          the notification to merge (not {@code null}).
   * @return the updated notification with merged content (never {@code null}).
   */
  @Transactional
  public AlertNoticeEntity merge(AlertNoticeEntity alertNotice) {
    return entityManagerProvider.get().merge(alertNotice);
  }

  /**
   * Removes the specified notification from the database.
   *
   * @param alertNotice
   *          the notification to remove.
   */
  @Transactional
  public void remove(AlertNoticeEntity alertNotice) {
    entityManagerProvider.get().remove(alertNotice);
  }

  /**
   * Removes notifications for the specified alert definition ID. This will
   * invoke {@link EntityManager#clear()} when completed since the JPQL
   * statement will remove entries without going through the EM.
   *
   * @param definitionId
   *          the ID of the definition to remove.
   */
  @Transactional
  public void removeNoticeByDefinitionId(long definitionId) {
    LOG.info("Deleting AlertNotice entities by definition id.");
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<Integer> historyIdQuery = entityManager.createNamedQuery(
      "AlertHistoryEntity.findHistoryIdsByDefinitionId", Integer.class);
    historyIdQuery.setParameter("definitionId", definitionId);
    List<Integer> ids = daoUtils.selectList(historyIdQuery);
    // Batch delete notice
    int BATCH_SIZE = 999;
    TypedQuery<AlertNoticeEntity> noticeQuery = entityManager.createNamedQuery(
      "AlertNoticeEntity.removeByHistoryIds", AlertNoticeEntity.class);
    if (ids != null && !ids.isEmpty()) {
      for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
        int endIndex = (i + BATCH_SIZE) > ids.size() ? ids.size() : (i + BATCH_SIZE);
        List<Integer> idsSubList = ids.subList(i, endIndex);
        LOG.info("Deleting AlertNotice entity batch with history ids: " +
          idsSubList.get(0) + " - " + idsSubList.get(idsSubList.size() - 1));
        noticeQuery.setParameter("historyIds", idsSubList);
        noticeQuery.executeUpdate();
      }
    }

    entityManager.clear();
  }

  /**
   * The {@link NoticePredicateVisitor} is used to convert an Ambari
   * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class NoticePredicateVisitor extends
      JpaPredicateVisitor<AlertNoticeEntity> {

    /**
     * Constructor.
     *
     */
    public NoticePredicateVisitor() {
      super(entityManagerProvider.get(), AlertNoticeEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AlertNoticeEntity> getEntityClass() {
      return AlertNoticeEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(
        String propertyId) {
      return AlertNoticeEntity_.getPredicateMapping().get(propertyId);
    }
  }
}
