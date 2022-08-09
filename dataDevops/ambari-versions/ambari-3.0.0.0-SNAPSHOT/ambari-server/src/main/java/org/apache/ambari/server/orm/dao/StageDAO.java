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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.query.JpaPredicateVisitor;
import org.apache.ambari.server.api.query.JpaSortBuilder;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.orm.entities.StageEntity_;
import org.apache.ambari.server.utils.StageUtils;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class StageDAO {

  /**
   * Mapping of valid status transitions that that are driven by manual input.
   */
  private static Map<HostRoleStatus, EnumSet<HostRoleStatus>> manualTransitionMap = new HashMap<>();

  static {
    manualTransitionMap.put(HostRoleStatus.HOLDING,
        EnumSet.of(HostRoleStatus.COMPLETED, HostRoleStatus.ABORTED));

    manualTransitionMap.put(HostRoleStatus.HOLDING_FAILED,
        EnumSet.of(HostRoleStatus.PENDING, HostRoleStatus.FAILED, HostRoleStatus.ABORTED));

    manualTransitionMap.put(HostRoleStatus.HOLDING_TIMEDOUT,
        EnumSet.of(HostRoleStatus.PENDING, HostRoleStatus.TIMEDOUT, HostRoleStatus.ABORTED));

    // todo: perhaps add a CANCELED status that just affects a stage and wont
    // abort the request
    // todo: so, if I scale 10 nodes and actually provision 5 and then later
    // decide I don't want those
    // todo: additional 5 nodes I can cancel them and the corresponding request
    // will have a status of COMPLETED
  }

  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @Inject
  HostRoleCommandDAO hostRoleCommandDao;

  @RequiresSession
  public StageEntity findByPK(StageEntityPK stageEntityPK) {
    return entityManagerProvider.get().find(StageEntity.class, stageEntityPK);
  }

  @RequiresSession
  public List<StageEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), StageEntity.class);
  }

  @RequiresSession
  public long getLastRequestId() {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT max(stage.requestId) FROM StageEntity stage", Long.class);
    Long result = daoUtils.selectSingle(query);
    if (result != null) {
      return result;
    } else {
      return 0;
    }
  }

  @RequiresSession
  public StageEntity findByActionId(String actionId) {
    long[] ids = StageUtils.getRequestStage(actionId);
    StageEntityPK pk = new StageEntityPK();
    pk.setRequestId(ids[0]);
    pk.setStageId(ids[1]);
    return findByPK(pk);
  }

  @RequiresSession
  public List<StageEntity> findByRequestId(long requestId) {
    TypedQuery<StageEntity> query = entityManagerProvider.get().createQuery("SELECT stage " +
        "FROM StageEntity stage " +
        "WHERE stage.requestId=?1 " +
        "ORDER BY stage.stageId", StageEntity.class);
    return daoUtils.selectList(query, requestId);
  }

  @RequiresSession
  public List<StageEntity> findByRequestIdAndCommandStatuses(Long requestId, Collection<HostRoleStatus> statuses) {
    TypedQuery<StageEntity> query = entityManagerProvider.get().createNamedQuery(
        "StageEntity.findByRequestIdAndCommandStatuses", StageEntity.class);

    query.setParameter("requestId", requestId);
    query.setParameter("statuses", statuses);
    return daoUtils.selectList(query);
  }

  /**
   * Finds the first stage matching any of the specified statuses for every
   * request. For example, to find the first {@link HostRoleStatus#IN_PROGRESS}
   * stage for every request, pass in
   * {@link HostRoleStatus#IN_PROGRESS_STATUSES}.
   *
   * @param statuses
   *          {@link HostRoleStatus}
   * @return the list of the first matching stage for the given statuses for
   *         every request.
   */
  @RequiresSession
  public List<StageEntity> findFirstStageByStatus(Collection<HostRoleStatus> statuses) {
    TypedQuery<Object[]> query = entityManagerProvider.get().createNamedQuery(
        "StageEntity.findFirstStageByStatus", Object[].class);

    query.setParameter("statuses", statuses);

    List<Object[]> results = daoUtils.selectList(query);
    List<StageEntity> stages = new ArrayList<>();

    for (Object[] result : results) {
      StageEntityPK stagePK = new StageEntityPK();
      stagePK.setRequestId((Long) result[0]);
      stagePK.setStageId((Long) result[1]);

      StageEntity stage = findByPK(stagePK);
      stages.add(stage);
    }

    return stages;
  }

  @RequiresSession
  public Map<Long, String> findRequestContext(List<Long> requestIds) {
    Map<Long, String> resultMap = new HashMap<>();
    if (requestIds != null && !requestIds.isEmpty()) {
      TypedQuery<StageEntity> query = entityManagerProvider.get()
        .createQuery("SELECT stage FROM StageEntity stage WHERE " +
          "stage.requestId IN (SELECT DISTINCT s.requestId FROM StageEntity s " +
          "WHERE s.requestId IN ?1)", StageEntity.class);
      List<StageEntity> result = daoUtils.selectList(query, requestIds);
      if (result != null && !result.isEmpty()) {
        for (StageEntity entity : result) {
          resultMap.put(entity.getRequestId(), entity.getRequestContext());
        }
      }
    }
    return resultMap;
  }

  @RequiresSession
  public String findRequestContext(long requestId) {
    TypedQuery<String> query = entityManagerProvider.get().createQuery(
      "SELECT stage.requestContext " + "FROM StageEntity stage " +
        "WHERE stage.requestId=?1", String.class);
    String result =  daoUtils.selectOne(query, requestId);
    if (result != null) {
      return result;
    }
    else {
      return ""; // Since it is defined as empty string in the StageEntity
    }
  }

  @Transactional
  public void create(StageEntity stageEntity) {
    entityManagerProvider.get().persist(stageEntity);
  }

  @Transactional
  public StageEntity merge(StageEntity stageEntity) {
    return entityManagerProvider.get().merge(stageEntity);
  }

  @Transactional
  public void remove(StageEntity stageEntity) {
    entityManagerProvider.get().remove(merge(stageEntity));
  }

  @Transactional
  public void removeByPK(StageEntityPK stageEntityPK) {
    remove(findByPK(stageEntityPK));
  }

  /**
   * Finds all {@link org.apache.ambari.server.orm.entities.StageEntity} that match the provided
   * {@link org.apache.ambari.server.controller.spi.Predicate}. This method will make JPA do the heavy lifting
   * of providing a slice of the result set.
   *
   * @param request
   * @return
   */
  @RequiresSession
  public List<StageEntity> findAll(Request request, Predicate predicate) {
    EntityManager entityManager = entityManagerProvider.get();

    // convert the Ambari predicate into a JPA predicate
    StagePredicateVisitor visitor = new StagePredicateVisitor();
    PredicateHelper.visit(predicate, visitor);

    CriteriaQuery<StageEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (jpaPredicate != null) {
      query.where(jpaPredicate);
    }

    // sorting
    JpaSortBuilder<StageEntity> sortBuilder = new JpaSortBuilder<>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(request.getSortRequest(), visitor);
    query.orderBy(sortOrders);

    TypedQuery<StageEntity> typedQuery = entityManager.createQuery(query);

    // !!! https://bugs.eclipse.org/bugs/show_bug.cgi?id=398067
    // ensure that an associated entity with a JOIN is not stale; this causes
    // the associated StageEntity to be stale
    typedQuery.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectList(typedQuery);
  }

  /**
   * Update the given stage entity with the desired status.
   *
   * @param stage
   *          the stage entity to update
   * @param desiredStatus
   *          the desired stage status
   * @param actionManager
   *          the action manager
   *
   * @throws java.lang.IllegalArgumentException
   *           if the transition to the desired status is not a legal transition
   */
  @Transactional
  public void updateStageStatus(StageEntity stage, HostRoleStatus desiredStatus,
      ActionManager actionManager) {
    Collection<HostRoleCommandEntity> tasks = stage.getHostRoleCommands();

    HostRoleStatus currentStatus = CalculatedStatus.statusFromTaskEntities(tasks,
        stage.isSkippable()).getStatus();

    if (!isValidManualTransition(currentStatus, desiredStatus)) {
      throw new IllegalArgumentException(
          "Can not transition a stage from " + currentStatus + " to " + desiredStatus);
    }
    if (desiredStatus == HostRoleStatus.ABORTED) {
      actionManager.cancelRequest(stage.getRequestId(), "User aborted.");
    } else {
      List <HostRoleCommandEntity> hrcWithChangedStatus = new ArrayList<>();
      for (HostRoleCommandEntity hostRoleCommand : tasks) {
        HostRoleStatus hostRoleStatus = hostRoleCommand.getStatus();
        if (hostRoleStatus.equals(currentStatus)) {
          hrcWithChangedStatus.add(hostRoleCommand);
          hostRoleCommand.setStatus(desiredStatus);

          if (desiredStatus == HostRoleStatus.PENDING) {
            hostRoleCommand.setStartTime(-1L);
          }
          hostRoleCommandDao.merge(hostRoleCommand);
        }
      }
    }
  }

  /**
   *
   * @param stageEntityPK  {@link StageEntityPK}
   * @param status {@link HostRoleStatus}
   * @param displayStatus {@link HostRoleStatus}
   */
  @Transactional
  public void updateStatus(StageEntityPK stageEntityPK, HostRoleStatus status, HostRoleStatus displayStatus) {
    StageEntity stageEntity = findByPK(stageEntityPK);
    stageEntity.setStatus(status);
    stageEntity.setDisplayStatus(displayStatus);
    merge(stageEntity);
  }


  /**
   * Determine whether or not it is valid to transition from this stage status
   * to the given status.
   *
   * @param status
   *          the stage status being transitioned to
   *
   * @return true if it is valid to transition to the given stage status
   */
  private static boolean isValidManualTransition(HostRoleStatus status,
      HostRoleStatus desiredStatus) {
    EnumSet<HostRoleStatus> stageStatusSet = manualTransitionMap.get(status);
    return stageStatusSet != null && stageStatusSet.contains(desiredStatus);
  }

  /**
   * The {@link org.apache.ambari.server.orm.dao.StageDAO.StagePredicateVisitor} is used to convert an Ambari
   * {@link org.apache.ambari.server.controller.spi.Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class StagePredicateVisitor extends
      JpaPredicateVisitor<StageEntity> {

    /**
     * Constructor.
     *
     */
    public StagePredicateVisitor() {
      super(entityManagerProvider.get(), StageEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<StageEntity> getEntityClass() {
      return StageEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(
        String propertyId) {
      return StageEntity_.getPredicateMapping().get(propertyId);
    }
  }
}
