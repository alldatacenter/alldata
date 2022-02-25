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
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.query.JpaPredicateVisitor;
import org.apache.ambari.server.api.query.JpaSortBuilder;
import org.apache.ambari.server.cleanup.TimeBasedCleanupPolicy;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AlertCurrentRequest;
import org.apache.ambari.server.controller.AlertHistoryRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.events.AggregateAlertRecalculateEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity_;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity_;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertsDAO} class manages the {@link AlertHistoryEntity} and
 * {@link AlertCurrentEntity} instances. Each {@link AlertHistoryEntity} is
 * known as an "alert" that has been triggered and received.
 * <p/>
 * If alert caching is enabled, then updates to {@link AlertCurrentEntity} are
 * not immediately persisted to JPA. Instead, they are kept in a cache and
 * periodically flushed. This means that many queries will need to swap in the
 * cached {@link AlertCurrentEntity} with that returned from the EclipseLink JPA
 * entity manager.
 */
@Singleton
@Experimental(feature = ExperimentalFeature.ALERT_CACHING)
public class AlertsDAO implements Cleanable {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertsDAO.class);

  /**
   * A template of JPQL for getting the number of hosts in various states.
   */
  private static final String ALERT_COUNT_SQL_TEMPLATE = "SELECT NEW %s("
      + "SUM(CASE WHEN history.alertState = :okState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :warningState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :criticalState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :unknownState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN alert.maintenanceState != :maintenanceStateOff THEN 1 ELSE 0 END)) "
      + "FROM AlertCurrentEntity alert JOIN alert.alertHistory history WHERE history.clusterId = :clusterId";

  private static final String ALERT_COUNT_PER_HOST_SQL_TEMPLATE = "SELECT NEW %s("
      + "history.hostName, "
      + "SUM(CASE WHEN history.alertState = :okState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :warningState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :criticalState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :unknownState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN alert.maintenanceState != :maintenanceStateOff THEN 1 ELSE 0 END)) "
      + "FROM AlertCurrentEntity alert JOIN alert.alertHistory history WHERE history.clusterId = :clusterId GROUP BY history.hostName";

  /**
   * JPA entity manager
   */
  @Inject
  private Provider<EntityManager> m_entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils m_daoUtils;

  /**
   * Publishes alert events when particular DAO methods are called.
   */
  @Inject
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Used to lookup clusters.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Configuration.
   */
  private final Configuration m_configuration;

  /**
   * A cache of current alert information. The {@link AlertCurrentEntity}
   * instances cached are currently managed. This allows the cached instances to
   * be easiler flushed from the cache to JPA.
   * <p/>
   * This also means that the cache is holding onto a rather large map of JPA
   * entities. This could lead to OOM errors over time if the indirectly
   * referenced entity map contains more than just {@link AlertCurrentEntity}.
   */
  private LoadingCache<AlertCacheKey, AlertCurrentEntity> m_currentAlertCache = null;

  /**
   * Batch size to query the DB and use the results in an IN clause.
   */
  private static final int BATCH_SIZE = 999;

  /**
   * Constructor.
   *
   */
  @Inject
  public AlertsDAO(Configuration configuration) {
    m_configuration = configuration;

    if( m_configuration.isAlertCacheEnabled() ){
      int maximumSize = m_configuration.getAlertCacheSize();

      LOG.info("Alert caching is enabled (size={}, flushInterval={}m)", maximumSize,
          m_configuration.getAlertCacheFlushInterval());

      // construct a cache for current alerts which will prevent database hits
      // on every heartbeat
      m_currentAlertCache = CacheBuilder.newBuilder().maximumSize(
          maximumSize).build(new CacheLoader<AlertCacheKey, AlertCurrentEntity>() {
            @Override
            public AlertCurrentEntity load(AlertCacheKey key) throws Exception {
              LOG.debug("Cache miss for alert key {}, fetching from JPA", key);

              final AlertCurrentEntity alertCurrentEntity;

              long clusterId = key.getClusterId();
              String alertDefinitionName = key.getAlertDefinitionName();
              String hostName = key.getHostName();

              if (StringUtils.isEmpty(hostName)) {
                alertCurrentEntity = findCurrentByNameNoHostInternalInJPA(clusterId,
                    alertDefinitionName);
              } else {
                alertCurrentEntity = findCurrentByHostAndNameInJPA(clusterId, hostName,
                    alertDefinitionName);
              }

              if (null == alertCurrentEntity) {
                LOG.trace("Cache lookup failed for {} because the alert does not yet exist", key);
                throw new AlertNotYetCreatedException();
              }

              return alertCurrentEntity;
            }
          });
    }
  }

  /**
   * Gets an alert with the specified ID.
   *
   * @param alertId
   *          the ID of the alert to retrieve.
   * @return the alert or {@code null} if none exists.
   */
  @RequiresSession
  public AlertHistoryEntity findById(long alertId) {
    return m_entityManagerProvider.get().find(AlertHistoryEntity.class, alertId);
  }

  /**
   * Gets all alerts stored in the database across all clusters.
   *
   * @return all alerts or an empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll() {
    TypedQuery<AlertHistoryEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAll", AlertHistoryEntity.class);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @return all alerts in the specified cluster or an empty list if none exist
   *         (never {@code null}).
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId) {
    TypedQuery<AlertHistoryEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInCluster", AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster that have one
   * of the specified alert states.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param alertStates
   *          the states to match for the retrieved alerts (not {@code null}).
   * @return the alerts matching the specified states and cluster, or an empty
   *         list if none.
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId,
      List<AlertState> alertStates) {
    if (null == alertStates || alertStates.size() == 0) {
      return Collections.emptyList();
    }

    TypedQuery<AlertHistoryEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInClusterWithState",
        AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("alertStates", alertStates);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster and that fall
   * withing the specified date range. Dates are expected to be in milliseconds
   * since the epoch, normalized to UTC time.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param startDate
   *          the date that the earliest entry must occur after, normalized to
   *          UTC, or {@code null} for all entries that occur before the given
   *          end date.
   * @param endDate
   *          the date that the latest entry must occur before, normalized to
   *          UTC, or {@code null} for all entries that occur after the given
   *          start date.
   * @return the alerts matching the specified date range.
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId, Date startDate,
      Date endDate) {
    if (null == startDate && null == endDate) {
      return Collections.emptyList();
    }

    TypedQuery<AlertHistoryEntity> query = null;

    if (null != startDate && null != endDate) {
      if (startDate.after(endDate)) {
        return Collections.emptyList();
      }

      query = m_entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterBetweenDates",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("startDate", startDate.getTime());
      query.setParameter("endDate", endDate.getTime());
    } else if (null != startDate) {
      query = m_entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterAfterDate",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("afterDate", startDate.getTime());
    } else if (null != endDate) {
      query = m_entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterBeforeDate",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("beforeDate", endDate.getTime());
    }

    if (null == query) {
      return Collections.emptyList();
    }

    return m_daoUtils.selectList(query);
  }

  /**
   * Finds all {@link AlertHistoryEntity} that match the provided
   * {@link AlertHistoryRequest}. This method will make JPA do the heavy lifting
   * of providing a slice of the result set.
   *
   * @param request
   * @return
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(AlertHistoryRequest request) {
    EntityManager entityManager = m_entityManagerProvider.get();

    // convert the Ambari predicate into a JPA predicate
    HistoryPredicateVisitor visitor = new HistoryPredicateVisitor();
    PredicateHelper.visit(request.Predicate, visitor);

    CriteriaQuery<AlertHistoryEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (null != jpaPredicate) {
      query.where(jpaPredicate);
    }

    // sorting
    JpaSortBuilder<AlertHistoryEntity> sortBuilder = new JpaSortBuilder<>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(request.Sort, visitor);
    query.orderBy(sortOrders);

    // pagination
    TypedQuery<AlertHistoryEntity> typedQuery = entityManager.createQuery(query);
    if (null != request.Pagination) {
      typedQuery.setFirstResult(request.Pagination.getOffset());
      typedQuery.setMaxResults(request.Pagination.getPageSize());
    }

    return m_daoUtils.selectList(typedQuery);
  }

  /**
   * Finds all {@link AlertCurrentEntity} that match the provided
   * {@link AlertCurrentRequest}. This method will make JPA do the heavy lifting
   * of providing a slice of the result set.
   *
   * @param request
   * @return
   */
  @Transactional
  public List<AlertCurrentEntity> findAll(AlertCurrentRequest request) {
    EntityManager entityManager = m_entityManagerProvider.get();

    // convert the Ambari predicate into a JPA predicate
    CurrentPredicateVisitor visitor = new CurrentPredicateVisitor();
    PredicateHelper.visit(request.Predicate, visitor);

    CriteriaQuery<AlertCurrentEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (null != jpaPredicate) {
      query.where(jpaPredicate);
    }

    // sorting
    JpaSortBuilder<AlertCurrentEntity> sortBuilder = new JpaSortBuilder<>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(request.Sort, visitor);
    query.orderBy(sortOrders);

    // pagination
    TypedQuery<AlertCurrentEntity> typedQuery = entityManager.createQuery(query);
    if( null != request.Pagination ){
      // prevent JPA errors when -1 is passed in by accident
      int offset = request.Pagination.getOffset();
      if (offset < 0) {
        offset = 0;
      }

      typedQuery.setFirstResult(offset);
      typedQuery.setMaxResults(request.Pagination.getPageSize());
    }

    List<AlertCurrentEntity> alerts = m_daoUtils.selectList(typedQuery);

    // if caching is enabled, replace results with cached values when present
    if (m_configuration.isAlertCacheEnabled()) {
      alerts = supplementWithCachedAlerts(alerts);
    }

    return alerts;
  }

  /**
   * Gets the total count of all {@link AlertHistoryEntity} rows that match the
   * specified {@link Predicate}.
   *
   * @param predicate
   *          the predicate to apply, or {@code null} for none.
   * @return the total count of rows that would be returned in a result set.
   */
  public int getCount(Predicate predicate) {
    return 0;
  }

  /**
   * Gets the current alerts.
   *
   * @return the current alerts or an empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrent() {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findAll", AlertCurrentEntity.class);

    List<AlertCurrentEntity> alerts = m_daoUtils.selectList(query);

    // if caching is enabled, replace results with cached values when present
    if (m_configuration.isAlertCacheEnabled()) {
      alerts = supplementWithCachedAlerts(alerts);
    }

    return alerts;
  }

  /**
   * Gets a current alert with the specified ID.
   *
   * @param alertId
   *          the ID of the alert to retrieve.
   * @return the alert or {@code null} if none exists.
   */
  @RequiresSession
  public AlertCurrentEntity findCurrentById(long alertId) {
    return m_entityManagerProvider.get().find(AlertCurrentEntity.class, alertId);
  }

  /**
   * Gets the current alerts for the specified definition ID.
   *
   * @param definitionId
   *          the ID of the definition to retrieve current alerts for.
   * @return the current alerts for the definition or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByDefinitionId(long definitionId) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByDefinitionId", AlertCurrentEntity.class);

    query.setParameter("definitionId", Long.valueOf(definitionId));

    List<AlertCurrentEntity> alerts = m_daoUtils.selectList(query);

    // if caching is enabled, replace results with cached values when present
    if (m_configuration.isAlertCacheEnabled()) {
      alerts = supplementWithCachedAlerts(alerts);
    }

    return alerts;
  }

  /**
   * Gets the current alerts for a given cluster.
   *
   * @return the current alerts for the given cluster or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByCluster(long clusterId) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByCluster", AlertCurrentEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));

    List<AlertCurrentEntity> alerts = m_daoUtils.selectList(query);

    // if caching is enabled, replace results with cached values when present
    if (m_configuration.isAlertCacheEnabled()) {
      alerts = supplementWithCachedAlerts(alerts);
    }

    return alerts;
  }

  /**
   * Retrieves the summary information for a particular scope. The result is a
   * DTO since the columns are aggregated and don't fit to an entity.
   *
   * @param clusterId
   *          the cluster id
   * @param serviceName
   *          the service name. Use {@code null} to not filter on service.
   * @param hostName
   *          the host name. Use {@code null} to not filter on host.
   * @return the summary DTO
   */
  @RequiresSession
  public AlertSummaryDTO findCurrentCounts(long clusterId, String serviceName, String hostName) {
    String sql = String.format(ALERT_COUNT_SQL_TEMPLATE,
      AlertSummaryDTO.class.getName());

    StringBuilder sb = new StringBuilder(sql);

    if (null != serviceName) {
      sb.append(" AND history.serviceName = :serviceName");
    }

    if (null != hostName) {
      sb.append(" AND history.hostName = :hostName");
    }

    TypedQuery<AlertSummaryDTO> query = m_entityManagerProvider.get().createQuery(
      sb.toString(), AlertSummaryDTO.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("okState", AlertState.OK);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);

    if (null != serviceName) {
      query.setParameter("serviceName", serviceName);
    }

    if (null != hostName) {
      query.setParameter("hostName", hostName);
    }

    return m_daoUtils.selectSingle(query);
  }

  /**
   * Retrieves the summary information for all the hosts in the provided cluster.
   * The result is mapping from hostname to summary DTO.
   *
   * @param clusterId
   *          the cluster id
   * @return map from hostnames to summary DTO
   */
  @RequiresSession
  public Map<String, AlertSummaryDTO> findCurrentPerHostCounts(long clusterId) {
    String sql = String.format(ALERT_COUNT_PER_HOST_SQL_TEMPLATE, HostAlertSummaryDTO.class.getName());

    StringBuilder sb = new StringBuilder(sql);

    TypedQuery<HostAlertSummaryDTO> query = m_entityManagerProvider.get().createQuery(sb.toString(), HostAlertSummaryDTO.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("okState", AlertState.OK);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);

    Map<String, AlertSummaryDTO> map = new HashMap<>();
    List<HostAlertSummaryDTO> resultList = m_daoUtils.selectList(query);
    for (HostAlertSummaryDTO result : resultList) {
      map.put(result.getHostName(), result);
    }
    return map;
  }

  /**
   * Retrieve the summary alert information for all hosts. This is different
   * from {@link #findCurrentCounts(long, String, String)} since this will
   * return only alerts related to hosts and those values will be the total
   * number of hosts affected, not the total number of alerts.
   *
   * @param clusterId
   *          the cluster id
   * @return the summary DTO for host alerts.
   */
  @RequiresSession
  public AlertHostSummaryDTO findCurrentHostCounts(long clusterId) {
    String sql = String.format(ALERT_COUNT_PER_HOST_SQL_TEMPLATE, HostAlertSummaryDTO.class.getName());

    StringBuilder sb = new StringBuilder(sql);

    TypedQuery<HostAlertSummaryDTO> query = m_entityManagerProvider.get().createQuery(sb.toString(), HostAlertSummaryDTO.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("okState", AlertState.OK);
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);

    int okCount = 0;
    int warningCount = 0;
    int criticalCount = 0;
    int unknownCount = 0;

    List<HostAlertSummaryDTO> resultList = m_daoUtils.selectList(query);
    for (HostAlertSummaryDTO result : resultList) {
      if (result.getHostName() == null) {
        continue;
      }
      if (result.getCriticalCount() > 0) {
        criticalCount++;
      }
      else if (result.getWarningCount() > 0) {
        warningCount++;
      }
      else if (result.getUnknownCount() > 0) {
        unknownCount++;
      }
      else {
        okCount++;
      }
    }

    AlertHostSummaryDTO hostSummary = new AlertHostSummaryDTO(okCount,
            unknownCount, warningCount, criticalCount);

    return hostSummary;
  }

  /**
   * Gets the current alerts for a given service.
   *
   * @return the current alerts for the given service or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByService(long clusterId,
      String serviceName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.findByService", AlertCurrentEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("inlist", EnumSet.of(Scope.ANY, Scope.SERVICE));

    List<AlertCurrentEntity> alerts = m_daoUtils.selectList(query);

    // if caching is enabled, replace results with cached values when present
    if (m_configuration.isAlertCacheEnabled()) {
      alerts = supplementWithCachedAlerts(alerts);
    }

    return alerts;
  }

  /**
   * Locate the current alert for the provided service and alert name. This
   * method will first consult the cache if configured with
   * {@link Configuration#isAlertCacheEnabled()}.
   *
   * @param clusterId
   *          the cluster id
   * @param hostName
   *          the name of the host (not {@code null}).
   * @param alertName
   *          the name of the alert (not {@code null}).
   * @return the current record, or {@code null} if not found
   */
  public AlertCurrentEntity findCurrentByHostAndName(long clusterId, String hostName,
      String alertName) {

    if( m_configuration.isAlertCacheEnabled() ){
      AlertCacheKey key = new AlertCacheKey(clusterId, alertName, hostName);

      try {
        return m_currentAlertCache.get(key);
      } catch (ExecutionException executionException) {
        Throwable cause = executionException.getCause();
        if (!(cause instanceof AlertNotYetCreatedException)) {
          LOG.warn("Unable to retrieve alert for key {} from the cache", key);
        }
      }
    }

    return findCurrentByHostAndNameInJPA(clusterId, hostName, alertName);
  }

  /**
   * Locate the current alert for the provided service and alert name.
   *
   * @param clusterId
   *          the cluster id
   * @param hostName
   *          the name of the host (not {@code null}).
   * @param alertName
   *          the name of the alert (not {@code null}).
   * @return the current record, or {@code null} if not found
   */
  @RequiresSession
  private AlertCurrentEntity findCurrentByHostAndNameInJPA(long clusterId, String hostName,
      String alertName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.findByHostAndName", AlertCurrentEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("hostName", hostName);
    query.setParameter("definitionName", alertName);

    return m_daoUtils.selectOne(query);
  }

  /**
   * Removes alert history and current alerts for the specified alert defintiion
   * ID. This will invoke {@link EntityManager#clear()} when completed since the
   * JPQL statement will remove entries without going through the EM.
   *
   * @param definitionId
   *          the ID of the definition to remove.
   */
  @Transactional
  public void removeByDefinitionId(long definitionId) {
    EntityManager entityManager = m_entityManagerProvider.get();
    TypedQuery<AlertCurrentEntity> currentQuery = entityManager.createNamedQuery(
        "AlertCurrentEntity.removeByDefinitionId", AlertCurrentEntity.class);

    currentQuery.setParameter("definitionId", definitionId);
    currentQuery.executeUpdate();

    TypedQuery<AlertHistoryEntity> historyQuery = entityManager.createNamedQuery(
        "AlertHistoryEntity.removeByDefinitionId", AlertHistoryEntity.class);

    historyQuery.setParameter("definitionId", definitionId);
    historyQuery.executeUpdate();

    entityManager.clear();

    // if caching is enabled, invalidate the cache to force the latest values
    // back from the DB
    if (m_configuration.isAlertCacheEnabled()) {
      m_currentAlertCache.invalidateAll();
    }
  }

  /**
   * Remove a current alert whose history entry matches the specfied ID.
   *
   * @param   historyId the ID of the history entry.
   * @return  the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByHistoryId(long historyId) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.removeByHistoryId", AlertCurrentEntity.class);

    query.setParameter("historyId", historyId);
    int rowsRemoved = query.executeUpdate();

    // if caching is enabled, invalidate the cache to force the latest values
    // back from the DB
    if (m_configuration.isAlertCacheEnabled()) {
      m_currentAlertCache.invalidateAll();
    }

    return rowsRemoved;
  }

  /**
   * Remove all current alerts that are disabled.
   *
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentDisabledAlerts() {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.findDisabled", AlertCurrentEntity.class);

    int rowsRemoved = 0;
    List<AlertCurrentEntity> currentEntities = m_daoUtils.selectList(query);
    if (currentEntities != null) {
      for (AlertCurrentEntity currentEntity : currentEntities) {
        remove(currentEntity);
        rowsRemoved++;
      }
    }

    // if caching is enabled, invalidate the cache to force the latest values
    // back from the DB
    if (m_configuration.isAlertCacheEnabled()) {
      m_currentAlertCache.invalidateAll();
    }

    return rowsRemoved;
  }

  /**
   * Remove the current alert that matches the given service. This is used in
   * cases where the service was removed from the cluster.
   * <p>
   * This method will also fire an {@link AggregateAlertRecalculateEvent} in
   * order to recalculate all aggregates.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service that the current alerts are being removed
   *          for (not {@code null}).
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByService(long clusterId, String serviceName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.findByServiceName", AlertCurrentEntity.class);

    query.setParameter("serviceName", serviceName);

    int removedItems = 0;
    List<AlertCurrentEntity> currentEntities = m_daoUtils.selectList(query);
    if (currentEntities != null) {
      for (AlertCurrentEntity currentEntity : currentEntities) {
        remove(currentEntity);
        removedItems++;
      }
    }

    // if caching is enabled, invalidate the cache to force the latest values
    // back from the DB
    if (m_configuration.isAlertCacheEnabled()) {
      m_currentAlertCache.invalidateAll();
    }

    // publish the event to recalculate aggregates
    m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(clusterId));
    return removedItems;
  }

  /**
   * Remove the current alert that matches the given host. This is used in cases
   * where the host was removed from the cluster.
   * <p>
   * This method will also fire an {@link AggregateAlertRecalculateEvent} in
   * order to recalculate all aggregates.
   *
   * @param hostName
   *          the name of the host that the current alerts are being removed for
   *          (not {@code null}).
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByHost(String hostName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.findByHost", AlertCurrentEntity.class);

    query.setParameter("hostName", hostName);
    List<AlertCurrentEntity> currentEntities = m_daoUtils.selectList(query);
    int removedItems = 0;
    if (currentEntities != null) {
      for (AlertCurrentEntity currentEntity : currentEntities) {
        remove(currentEntity);
        removedItems++;
      }
    }

    // if caching is enabled, invalidate the cache to force the latest values
    // back from the DB
    if (m_configuration.isAlertCacheEnabled()) {
      m_currentAlertCache.invalidateAll();
    }

    // publish the event to recalculate aggregates for every cluster since a host could potentially have several clusters
    try {
      Map<String, Cluster> clusters = m_clusters.get().getClusters();
      for (Map.Entry<String, Cluster> entry : clusters.entrySet()) {
        m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(
            entry.getValue().getClusterId()));
      }

    } catch (Exception ambariException) {
      LOG.warn("Unable to recalcuate aggregate alerts after removing host {}", hostName);
    }

    return removedItems;
  }

  /**
   * Remove the current alert that matches the given service, component and
   * host. This is used in cases where the component was removed from the host.
   * <p>
   * This method will also fire an {@link AggregateAlertRecalculateEvent} in
   * order to recalculate all aggregates.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service that the current alerts are being removed
   *          for (not {@code null}).
   * @param componentName
   *          the name of the component that the current alerts are being
   *          removed for (not {@code null}).
   * @param hostName
   *          the name of the host that the current alerts are being removed for
   *          (not {@code null}).
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByServiceComponentHost(long clusterId, String serviceName,
      String componentName, String hostName) {

    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "AlertCurrentEntity.findByHostComponent", AlertCurrentEntity.class);

    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);
    query.setParameter("hostName", hostName);

    List<AlertCurrentEntity> currentEntities = m_daoUtils.selectList(query);
    int removedItems = 0;
    if (currentEntities != null) {
      for (AlertCurrentEntity currentEntity : currentEntities) {
        remove(currentEntity);
        removedItems++;
      }
    }

    // if caching is enabled, invalidate the cache to force the latest values
    // back from the DB
    if (m_configuration.isAlertCacheEnabled()) {
      m_currentAlertCache.invalidateAll();
    }

    // publish the event to recalculate aggregates
    m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(clusterId));

    return removedItems;
  }

  /**
   * Persists a new alert.
   *
   * @param alert
   *          the alert to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertHistoryEntity alert) {
    m_entityManagerProvider.get().persist(alert);
  }

  /**
   * Refresh the state of the alert from the database.
   *
   * @param alert
   *          the alert to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertHistoryEntity alert) {
    m_entityManagerProvider.get().refresh(alert);
  }

  /**
   * Merge the speicified alert with the existing alert in the database.
   *
   * @param alert
   *          the alert to merge (not {@code null}).
   * @return the updated alert with merged content (never {@code null}).
   */
  @Transactional
  public AlertHistoryEntity merge(AlertHistoryEntity alert) {
    return m_entityManagerProvider.get().merge(alert);
  }

  /**
   * Removes the specified alert from the database.
   *
   * @param alert
   *          the alert to remove.
   */
  @Transactional
  public void remove(AlertHistoryEntity alert) {
    alert = merge(alert);

    removeCurrentByHistoryId(alert.getAlertId());
    m_entityManagerProvider.get().remove(alert);
  }

  /**
   * Persists a new current alert.
   *
   * @param alert
   *          the current alert to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertCurrentEntity alert) {
    m_entityManagerProvider.get().persist(alert);
  }

  /**
   * Refresh the state of the current alert from the database.
   *
   * @param alert
   *          the current alert to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertCurrentEntity alert) {
    m_entityManagerProvider.get().refresh(alert);
  }

  /**
   * Merge the speicified current alert with the existing alert in the database.
   *
   * @param alert
   *          the current alert to merge (not {@code null}).
   * @return the updated current alert with merged content (never {@code null}).
   */
  @Transactional
  public AlertCurrentEntity merge(AlertCurrentEntity alert) {
    // perform the JPA merge
    alert = m_entityManagerProvider.get().merge(alert);

    // if caching is enabled, update the cache
    if( m_configuration.isAlertCacheEnabled() ){
      AlertCacheKey key = AlertCacheKey.build(alert);
      m_currentAlertCache.put(key, alert);
    }

    return alert;
  }

  /**
   * Updates the internal cache of alerts with the specified alert. Unlike
   * {@link #merge(AlertCurrentEntity)}, this is not transactional and only
   * updates the cache.
   * <p/>
   * The alert should already exist in JPA - this is mainly to update the text
   * and timestamp.
   *
   * @param alert
   *          the alert to update in the cache (not {@code null}).
   * @param updateCacheOnly
   *          if {@code true}, then only the cache is updated and not JPA.
   * @see Configuration#isAlertCacheEnabled()
   */
  public AlertCurrentEntity merge(AlertCurrentEntity alert, boolean updateCacheOnly) {
    // cache only updates
    if (updateCacheOnly) {
      AlertCacheKey key = AlertCacheKey.build(alert);

      // cache not configured, log error
      if (!m_configuration.isAlertCacheEnabled()) {
        LOG.error(
            "Unable to update a cached alert instance for {} because cached alerts are not enabled",
            key);
      } else {
        // update cache and return alert; no database work
        m_currentAlertCache.put(key, alert);
        return alert;
      }
    }

    return merge(alert);
  }

  /**
   * Removes the specified current alert from the database.
   *
   * @param alert
   *          the current alert to remove.
   */
  @Transactional
  public void remove(AlertCurrentEntity alert) {
    m_entityManagerProvider.get().remove(merge(alert));
  }

  /**
   * Finds the aggregate counts for an alert name, across all hosts.
   * @param clusterId the cluster id
   * @param alertName the name of the alert to find the aggregate
   * @return the summary data
   */
  @RequiresSession
  public AlertSummaryDTO findAggregateCounts(long clusterId, String alertName) {
    String sql = String.format(ALERT_COUNT_SQL_TEMPLATE,
        AlertSummaryDTO.class.getName());

    StringBuilder buffer = new StringBuilder(sql);
    buffer.append(" AND history.alertDefinition.definitionName = :definitionName");

    TypedQuery<AlertSummaryDTO> query = m_entityManagerProvider.get().createQuery(
        buffer.toString(), AlertSummaryDTO.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("okState", AlertState.OK);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);
    query.setParameter("definitionName", alertName);

    return m_daoUtils.selectSingle(query);
  }

  /**
   * Locate the current alert for the provided service and alert name, but when
   * host is not set ({@code IS NULL}). This method will first consult the cache
   * if configured with {@link Configuration#isAlertCacheEnabled()}.
   *
   * @param clusterId
   *          the cluster id
   * @param alertName
   *          the name of the alert
   * @return the current record, or {@code null} if not found
   */
  public AlertCurrentEntity findCurrentByNameNoHost(long clusterId, String alertName) {
    if( m_configuration.isAlertCacheEnabled() ){
      AlertCacheKey key = new AlertCacheKey(clusterId, alertName);

      try {
        return m_currentAlertCache.get(key);
      } catch (ExecutionException executionException) {
        Throwable cause = executionException.getCause();

        if (!(cause instanceof AlertNotYetCreatedException)) {
          LOG.warn("Unable to retrieve alert for key {} from, the cache", key);
        }
      }
    }

    return findCurrentByNameNoHostInternalInJPA(clusterId, alertName);
  }

  /**
   * Locate the current alert for the provided service and alert name, but when
   * host is not set ({@code IS NULL}). This method
   *
   * @param clusterId
   *          the cluster id
   * @param alertName
   *          the name of the alert
   * @return the current record, or {@code null} if not found
   */
  @RequiresSession
  private AlertCurrentEntity findCurrentByNameNoHostInternalInJPA(long clusterId, String alertName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByNameAndNoHost", AlertCurrentEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("definitionName", alertName);

    return m_daoUtils.selectOne(query);
  }

  /**
   * Writes all cached {@link AlertCurrentEntity} instances to the database and
   * clears the cache.
   */
  @Transactional
  public void flushCachedEntitiesToJPA() {
    if (!m_configuration.isAlertCacheEnabled()) {
      LOG.warn("Unable to flush cached alerts to JPA because caching is not enabled");
      return;
    }

    // capture for logging purposes
    long cachedEntityCount = m_currentAlertCache.size();

    ConcurrentMap<AlertCacheKey, AlertCurrentEntity> map = m_currentAlertCache.asMap();
    Set<Entry<AlertCacheKey, AlertCurrentEntity>> entries = map.entrySet();
    for (Entry<AlertCacheKey, AlertCurrentEntity> entry : entries) {
      merge(entry.getValue());
    }

    m_currentAlertCache.invalidateAll();

    LOG.info("Flushed {} cached alerts to the database", cachedEntityCount);
  }

  /**
   * Gets a list that is comprised of the original values replaced by any cached
   * values from {@link #m_currentAlertCache}. This method should only be
   * invoked if {@link Configuration#isAlertCacheEnabled()} is {@code true}
   *
   * @param alerts
   *          the list of alerts to iterate over and replace with cached
   *          instances.
   * @return the list of alerts from JPA combined with any cached alerts.
   */
  private List<AlertCurrentEntity> supplementWithCachedAlerts(List<AlertCurrentEntity> alerts) {
    List<AlertCurrentEntity> cachedAlerts = new ArrayList<>(alerts.size());

    for (AlertCurrentEntity alert : alerts) {
      AlertCacheKey key = AlertCacheKey.build(alert);
      AlertCurrentEntity cachedEntity = m_currentAlertCache.getIfPresent(key);
      if (null != cachedEntity) {
        alert = cachedEntity;
      }

      cachedAlerts.add(alert);
    }

    return cachedAlerts;
  }

  @Transactional
  @Override
  public long cleanup(TimeBasedCleanupPolicy policy) {
    long affectedRows = 0;
    Long clusterId = null;
    try {
      clusterId = m_clusters.get().getCluster(policy.getClusterName()).getClusterId();
      affectedRows += cleanAlertNoticesForClusterBeforeDate(clusterId, policy.getToDateInMillis());
      affectedRows += cleanAlertCurrentsForClusterBeforeDate(clusterId, policy.getToDateInMillis());
      affectedRows += cleanAlertHistoriesForClusterBeforeDate(clusterId, policy.getToDateInMillis());
    } catch (AmbariException e) {
      LOG.error("Error while looking up cluster with name: {}", policy.getClusterName(), e);
      throw new IllegalStateException(e);
    }

    return affectedRows;
  }


  /**
   * The {@link HistoryPredicateVisitor} is used to convert an Ambari
   * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class HistoryPredicateVisitor extends
      JpaPredicateVisitor<AlertHistoryEntity> {

    /**
     * Constructor.
     *
     */
    public HistoryPredicateVisitor() {
      super(m_entityManagerProvider.get(), AlertHistoryEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AlertHistoryEntity> getEntityClass() {
      return AlertHistoryEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(
        String propertyId) {
      return AlertHistoryEntity_.getPredicateMapping().get(propertyId);
    }
  }

  /**
   * The {@link CurrentPredicateVisitor} is used to convert an Ambari
   * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class CurrentPredicateVisitor extends
      JpaPredicateVisitor<AlertCurrentEntity> {

    /**
     * Constructor.
     *
     */
    public CurrentPredicateVisitor() {
      super(m_entityManagerProvider.get(), AlertCurrentEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AlertCurrentEntity> getEntityClass() {
      return AlertCurrentEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(
        String propertyId) {
      return AlertCurrentEntity_.getPredicateMapping().get(propertyId);
    }
  }

  /**
   * The {@link AlertCacheKey} class is used as a key in the cache of
   * {@link AlertCurrentEntity}.
   */
  private final static class AlertCacheKey {
    private final long m_clusterId;
    private final String m_hostName;
    private final String m_alertDefinitionName;

    /**
     * Constructor.
     *
     * @param clusterId
     * @param alertDefinitionName
     */
    private AlertCacheKey(long clusterId, String alertDefinitionName) {
      this(clusterId, alertDefinitionName, null);
    }

    /**
     * Constructor.
     *
     * @param clusterId
     * @param alertDefinitionName
     * @param hostName
     */
    private AlertCacheKey(long clusterId, String alertDefinitionName, String hostName) {
      m_clusterId = clusterId;
      m_alertDefinitionName = alertDefinitionName;
      m_hostName = hostName;
    }

    /**
     * Builds a key from an entity.
     *
     * @param current
     *          the entity to create the key for.
     * @return the key (never {@code null}).
     */
    public static AlertCacheKey build(AlertCurrentEntity current) {
      AlertHistoryEntity history = current.getAlertHistory();
      AlertCacheKey key = new AlertCacheKey(history.getClusterId(),
          history.getAlertDefinition().getDefinitionName(), history.getHostName());

      return key;
    }

    /**
     * Gets the ID of the cluster that the alert is for.
     *
     * @return the clusterId
     */
    public long getClusterId() {
      return m_clusterId;
    }

    /**
     * Gets the host name, or {@code null} if none.
     *
     * @return the hostName, or {@code null} if none.
     */
    public String getHostName() {
      return m_hostName;
    }

    /**
     * Gets the unique name of the alert definition.
     *
     * @return the alertDefinitionName
     */
    public String getAlertDefinitionName() {
      return m_alertDefinitionName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((m_alertDefinitionName == null) ? 0 : m_alertDefinitionName.hashCode());
      result = prime * result + (int) (m_clusterId ^ (m_clusterId >>> 32));
      result = prime * result + ((m_hostName == null) ? 0 : m_hostName.hashCode());
      return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      AlertCacheKey other = (AlertCacheKey) obj;

      if (m_clusterId != other.m_clusterId) {
        return false;
      }

      if (m_alertDefinitionName == null) {
        if (other.m_alertDefinitionName != null) {
          return false;
        }
      } else if (!m_alertDefinitionName.equals(other.m_alertDefinitionName)) {
        return false;
      }

      if (m_hostName == null) {
        if (other.m_hostName != null) {
          return false;
        }
      } else if (!m_hostName.equals(other.m_hostName)) {
        return false;
      }

      return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder("AlertCacheKey{");
      buffer.append("cluserId=").append(m_clusterId);
      buffer.append(", alertName=").append(m_alertDefinitionName);

      if (null != m_hostName) {
        buffer.append(", hostName=").append(m_hostName);
      }

      buffer.append("}");
      return buffer.toString();
    }
  }

  /**
   * The {@link AlertNotYetCreatedException} is used as a way to signal to the
   * {@link CacheLoader} that there is no value for the specified
   * {@link AlertCacheKey}. Because this cache doesn't understand {@code null}
   * values, we use the exception mechanism to indicate that it should be
   * created and that the {@code null} value should not be cached.
   */
  @SuppressWarnings("serial")
  private static final class AlertNotYetCreatedException extends Exception {
  }

  /**
   * Find all @AlertHistoryEntity with date before provided date.
   * @param clusterId cluster id
   * @param beforeDateMillis timestamp in millis
   * @return List<Integer> ids
   */
  private List<Integer> findAllAlertHistoryIdsBeforeDate(Long clusterId, long  beforeDateMillis) {

    EntityManager entityManager = m_entityManagerProvider.get();
    TypedQuery<Integer> alertHistoryQuery =
      entityManager.createNamedQuery("AlertHistoryEntity.findAllIdsInClusterBeforeDate", Integer.class);

    alertHistoryQuery.setParameter("clusterId", clusterId);
    alertHistoryQuery.setParameter("beforeDate", beforeDateMillis);

    return m_daoUtils.selectList(alertHistoryQuery);
  }

  /**
   * Deletes AlertNotice records in relation with AlertHistory entries older than the given date.
   *
   * @param clusterId        the identifier of the cluster the AlertNotices belong to
   * @param beforeDateMillis the date in milliseconds the
   * @return a long representing the number of affected (deleted) records
   */
  @Transactional
  int cleanAlertNoticesForClusterBeforeDate(Long clusterId, long beforeDateMillis) {
    LOG.info("Deleting AlertNotice entities before date " + new Date(beforeDateMillis));
    EntityManager entityManager = m_entityManagerProvider.get();
    List<Integer> ids = findAllAlertHistoryIdsBeforeDate(clusterId, beforeDateMillis);
    int affectedRows = 0;
    // Batch delete
    TypedQuery<AlertNoticeEntity> noticeQuery =
      entityManager.createNamedQuery("AlertNoticeEntity.removeByHistoryIds", AlertNoticeEntity.class);
    if (ids != null && !ids.isEmpty()) {
      for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
        int endIndex = (i + BATCH_SIZE) > ids.size() ? ids.size() : (i + BATCH_SIZE);
        List<Integer> idsSubList = ids.subList(i, endIndex);
        LOG.info("Deleting AlertNotice entity batch with history ids: " +
          idsSubList.get(0) + " - " + idsSubList.get(idsSubList.size() - 1));
        noticeQuery.setParameter("historyIds", idsSubList);
        affectedRows += noticeQuery.executeUpdate();
      }
    }

    return affectedRows;
  }


  /**
   * Deletes AlertCurrent records in relation with AlertHistory entries older than the given date.
   *
   * @param clusterId        the identifier of the cluster the AlertCurrents belong to
   * @param beforeDateMillis the date in milliseconds the
   * @return a long representing the number of affected (deleted) records
   */
  @Transactional
  int cleanAlertCurrentsForClusterBeforeDate(long clusterId, long beforeDateMillis) {
    LOG.info("Deleting AlertCurrent entities before date " + new Date(beforeDateMillis));
    EntityManager entityManager = m_entityManagerProvider.get();
    List<Integer> ids = findAllAlertHistoryIdsBeforeDate(clusterId, beforeDateMillis);
    int affectedRows = 0;
    TypedQuery<AlertCurrentEntity> currentQuery =
      entityManager.createNamedQuery("AlertCurrentEntity.removeByHistoryIds", AlertCurrentEntity.class);
    if (ids != null && !ids.isEmpty()) {
      for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
        int endIndex = (i + BATCH_SIZE) > ids.size() ? ids.size() : (i + BATCH_SIZE);
        List<Integer> idsSubList = ids.subList(i, endIndex);
        LOG.info("Deleting AlertCurrent entity batch with history ids: " +
          idsSubList.get(0) + " - " + idsSubList.get(idsSubList.size() - 1));
        currentQuery.setParameter("historyIds", ids.subList(i, endIndex));
        affectedRows += currentQuery.executeUpdate();
      }
    }

    return affectedRows;
  }

  /**
   * Deletes AlertHistory entries in a cluster older than the given date.
   *
   * @param clusterId        the identifier of the cluster the AlertHistory entries belong to
   * @param beforeDateMillis the date in milliseconds the
   * @return a long representing the number of affected (deleted) records
   */

  @Transactional
  int cleanAlertHistoriesForClusterBeforeDate(Long clusterId, long beforeDateMillis) {
    return executeQuery("AlertHistoryEntity.removeInClusterBeforeDate", AlertHistoryEntity.class, clusterId, beforeDateMillis);
  }

  /**
   * Utility method for executing update or delete named queries having as input parameters the cluster id and a timestamp.
   *
   * @param namedQuery the named query to be executed
   * @param entityType the type of the entity
   * @param clusterId  the cluster identifier
   * @param timestamp  timestamp
   * @return the number of rows affected by the query execution.
   */
  private int executeQuery(String namedQuery, Class entityType, long clusterId, long timestamp) {
    LOG.info("Starting: Delete/update entries older than [ {} ] for entity [{}]", timestamp, entityType);
    TypedQuery query = m_entityManagerProvider.get().createNamedQuery(namedQuery, entityType);

    query.setParameter("clusterId", clusterId);
    query.setParameter("beforeDate", timestamp);

    int affectedRows = query.executeUpdate();

    m_entityManagerProvider.get().flush();
    m_entityManagerProvider.get().clear();

    LOG.info("Completed: Delete/update entries older than [ {} ] for entity: [{}]. Number of entities deleted: [{}]",
        timestamp, entityType, affectedRows);

    return affectedRows;
  }

}
