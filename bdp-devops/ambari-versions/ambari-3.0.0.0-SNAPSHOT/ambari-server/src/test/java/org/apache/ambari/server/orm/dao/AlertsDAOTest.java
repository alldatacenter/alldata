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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.AlertCurrentRequest;
import org.apache.ambari.server.controller.AlertHistoryRequest;
import org.apache.ambari.server.controller.internal.AlertHistoryResourceProvider;
import org.apache.ambari.server.controller.internal.AlertResourceProvider;
import org.apache.ambari.server.controller.internal.PageRequestImpl;
import org.apache.ambari.server.controller.internal.SortRequestImpl;
import org.apache.ambari.server.controller.spi.PageRequest.StartingPoint;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SortRequest.Order;
import org.apache.ambari.server.controller.spi.SortRequestProperty;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.orm.AlertDaoHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

/**
 * Tests {@link AlertsDAO}.
 */
public class AlertsDAOTest {

  final static String HOSTNAME = "c6401.ambari.apache.org";
  final static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  private Clusters m_clusters;
  private Cluster m_cluster;
  private Injector m_injector;
  private OrmTestHelper m_helper;
  private AlertsDAO m_dao;
  private AlertDefinitionDAO m_definitionDao;

  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;

  private AlertDaoHelper m_alertHelper;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.getInstance(UnitOfWork.class).begin();

    m_helper = m_injector.getInstance(OrmTestHelper.class);
    m_dao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_alertHelper = m_injector.getInstance(AlertDaoHelper.class);

    // !!! need a synchronous op for testing
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector);

    // install YARN so there is at least 1 service installed and no
    // unexpected alerts since the test YARN service doesn't have any alerts
    m_cluster = m_clusters.getClusterById(m_helper.createCluster());
    m_helper.initializeClusterWithStack(m_cluster);
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);
    m_helper.installYarnService(m_cluster, m_serviceFactory,
        m_componentFactory, m_schFactory, HOSTNAME);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("YARN");
      definition.setComponentName("Component " + i);
      definition.setClusterId(m_cluster.getClusterId());
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      m_definitionDao.create(definition);
    }

    List<AlertDefinitionEntity> definitions = m_definitionDao.findAll();
    assertNotNull(definitions);
    assertEquals(5, definitions.size());

    // create 10 historical alerts for each definition, 8 OK and 2 CRIT
    // total of 80 OK, 20 CRITICAL
    calendar.clear();
    calendar.set(2014, Calendar.JANUARY, 1);

    for (AlertDefinitionEntity definition : definitions) {
      for (int i = 0; i < 10; i++) {
        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setServiceName(definition.getServiceName());
        history.setClusterId(m_cluster.getClusterId());
        history.setAlertDefinition(definition);
        history.setAlertLabel(definition.getDefinitionName() + " " + i);
        history.setAlertText(definition.getDefinitionName() + " " + i);
        history.setAlertTimestamp(calendar.getTimeInMillis());
        history.setComponentName(definition.getComponentName());
        history.setHostName("h1");

        history.setAlertState(AlertState.OK);
        if (i == 0 || i == 5) {
          history.setAlertState(AlertState.CRITICAL);
        }

        // increase the days for each
        calendar.add(Calendar.DATE, 1);

        m_dao.create(history);
      }
    }

    // for each definition, create a current alert
    for (AlertDefinitionEntity definition : definitions) {
      List<AlertHistoryEntity> alerts = m_dao.findAll();
      AlertHistoryEntity history = null;
      for (AlertHistoryEntity alert : alerts) {
        if (definition.equals(alert.getAlertDefinition())) {
          history = alert;
        }
      }

      assertNotNull(history);

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertHistory(history);
      current.setLatestTimestamp(new Date().getTime());
      current.setOriginalTimestamp(new Date().getTime() - 10800000);
      current.setMaintenanceState(MaintenanceState.OFF);
      m_dao.create(current);
    }
  }

  /**
   *
   */
  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
    m_injector = null;
  }


  /**
   *
   */
  @Test
  public void testFindAll() {
    List<AlertHistoryEntity> alerts = m_dao.findAll(m_cluster.getClusterId());
    assertNotNull(alerts);
    assertEquals(50, alerts.size());
  }

  /**
   *
   */
  @Test
  public void testFindAllCurrent() {
    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrent();
    assertNotNull(currentAlerts);
    assertEquals(5, currentAlerts.size());
  }

  /**
   * Test looking up current alerts by definition ID.
   */
  @Test
  public void testFindCurrentByDefinitionId() throws Exception {
    // create a host
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("Foo Definition");
    definition.setServiceName("YARN");
    definition.setComponentName("NODEMANAGER");
    definition.setClusterId(m_cluster.getClusterId());
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(Integer.valueOf(60));
    definition.setScope(Scope.HOST);
    definition.setSource("{\"type\" : \"SCRIPT\"}");
    definition.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(definition);

    // history for the definition
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(m_cluster.getClusterId());
    history.setAlertDefinition(definition);
    history.setAlertLabel(definition.getDefinitionName());
    history.setAlertText(definition.getDefinitionName());
    history.setAlertTimestamp(Long.valueOf(1L));
    history.setHostName("h1");
    history.setAlertState(AlertState.OK);
    m_dao.create(history);

    // current for the history
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setOriginalTimestamp(1L);
    current.setLatestTimestamp(2L);
    current.setAlertHistory(history);
    m_dao.create(current);

    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrentByDefinitionId(definition.getDefinitionId());
    assertEquals(1, currentAlerts.size());

    // history for the definition
    AlertHistoryEntity history2 = new AlertHistoryEntity();
    history2.setServiceName(definition.getServiceName());
    history2.setClusterId(m_cluster.getClusterId());
    history2.setAlertDefinition(definition);
    history2.setAlertLabel(definition.getDefinitionName());
    history2.setAlertText(definition.getDefinitionName());
    history2.setAlertTimestamp(Long.valueOf(1L));
    history2.setHostName("h2");
    history2.setAlertState(AlertState.OK);
    m_dao.create(history);

    // current for the history
    AlertCurrentEntity current2 = new AlertCurrentEntity();
    current2.setOriginalTimestamp(1L);
    current2.setLatestTimestamp(2L);
    current2.setAlertHistory(history2);
    m_dao.create(current2);

    currentAlerts = m_dao.findCurrentByDefinitionId(definition.getDefinitionId());
    assertEquals(2, currentAlerts.size());
  }

  /**
   *
   */
  @Test
  public void testFindCurrentByService() {
    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrent();
    int currentAlertExpectedCount = currentAlerts.size();
    assertEquals(5, currentAlertExpectedCount);

    AlertCurrentEntity current = currentAlerts.get(0);
    AlertHistoryEntity history = current.getAlertHistory();

    assertNotNull(history);

    currentAlerts = m_dao.findCurrentByService(m_cluster.getClusterId(),
        history.getServiceName());

    assertNotNull(currentAlerts);
    assertEquals(currentAlertExpectedCount, currentAlerts.size());

    currentAlerts = m_dao.findCurrentByService(m_cluster.getClusterId(), "foo");

    assertNotNull(currentAlerts);
    assertEquals(0, currentAlerts.size());
  }

  /**
   * Test looking up current by a host name.
   */
  @Test
  public void testFindCurrentByHost() throws Exception {
    // create a host
    AlertDefinitionEntity hostDef = new AlertDefinitionEntity();
    hostDef.setDefinitionName("Host Alert Definition ");
    hostDef.setServiceName("YARN");
    hostDef.setComponentName(null);
    hostDef.setClusterId(m_cluster.getClusterId());
    hostDef.setHash(UUID.randomUUID().toString());
    hostDef.setScheduleInterval(Integer.valueOf(60));
    hostDef.setScope(Scope.HOST);
    hostDef.setSource("{\"type\" : \"SCRIPT\"}");
    hostDef.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(hostDef);

    // history for the definition
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(hostDef.getServiceName());
    history.setClusterId(m_cluster.getClusterId());
    history.setAlertDefinition(hostDef);
    history.setAlertLabel(hostDef.getDefinitionName());
    history.setAlertText(hostDef.getDefinitionName());
    history.setAlertTimestamp(Long.valueOf(1L));
    history.setHostName(HOSTNAME);
    history.setAlertState(AlertState.OK);
    m_dao.create(history);

    // current for the history
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setOriginalTimestamp(1L);
    current.setLatestTimestamp(2L);
    current.setAlertHistory(history);
    m_dao.create(current);

    Predicate hostPredicate = null;
    hostPredicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_HOST).equals(HOSTNAME).toPredicate();

    AlertCurrentRequest request = new AlertCurrentRequest();
    request.Predicate = hostPredicate;

    List<AlertCurrentEntity> currentAlerts = m_dao.findAll(request);
    assertNotNull(currentAlerts);
    assertEquals(1, currentAlerts.size());

    hostPredicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_HOST).equals("invalid.apache.org").toPredicate();

    request = new AlertCurrentRequest();
    request.Predicate = hostPredicate;
    currentAlerts = m_dao.findAll(request);

    assertNotNull(currentAlerts);
    assertEquals(0, currentAlerts.size());
  }

  /**
   * Tests that the Ambari {@link Predicate} can be converted and submitted to
   * JPA correctly to return a restricted result set.
   *
   * @throws Exception
   */
  @Test
  public void testAlertCurrentPredicate() throws Exception {
    AlertDefinitionEntity definition = m_definitionDao.findByName(
        m_cluster.getClusterId(), "Alert Definition 0");

    assertNotNull(definition);

    Predicate definitionIdPredicate = null;
    Predicate hdfsPredicate = null;
    Predicate yarnPredicate = null;

    definitionIdPredicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_DEFINITION_ID).equals(
        definition.getDefinitionId()).toPredicate();

    AlertCurrentRequest request = new AlertCurrentRequest();
    request.Predicate = definitionIdPredicate;

    List<AlertCurrentEntity> currentAlerts = m_dao.findAll(request);
    assertEquals(1, currentAlerts.size());

    hdfsPredicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_SERVICE).equals("HDFS").toPredicate();

    yarnPredicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_SERVICE).equals("YARN").toPredicate();

    request.Predicate = yarnPredicate;
    currentAlerts = m_dao.findAll(request);
    assertEquals(5, currentAlerts.size());

    request.Predicate = hdfsPredicate;
    currentAlerts = m_dao.findAll(request);
    assertEquals(0, currentAlerts.size());
  }

  /**
   * Tests that the Ambari sort is correctly applied to JPA quuery.
   *
   * @throws Exception
   */
  @Test
  public void testAlertCurrentSorting() throws Exception {
    AlertCurrentRequest request = new AlertCurrentRequest();

    Predicate clusterPredicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals(m_cluster.getClusterName()).toPredicate();

    request.Predicate = clusterPredicate;

    SortRequestProperty sortRequestProperty = new SortRequestProperty(AlertResourceProvider.ALERT_ID, Order.ASC);
    request.Sort = new SortRequestImpl(Collections.singletonList(sortRequestProperty));

    List<AlertCurrentEntity> currentAlerts = m_dao.findAll(request);
    assertTrue(currentAlerts.size() >= 5);
    long lastId = Long.MIN_VALUE;
    for (AlertCurrentEntity alert : currentAlerts) {
      assertTrue(lastId < alert.getAlertId());
      lastId = alert.getAlertId();
    }

    // change the sort to DESC
    sortRequestProperty = new SortRequestProperty(AlertResourceProvider.ALERT_ID, Order.DESC);
    request.Sort = new SortRequestImpl(Collections.singletonList(sortRequestProperty));

    currentAlerts = m_dao.findAll(request);
    assertTrue(currentAlerts.size() >= 5);
    lastId = Long.MAX_VALUE;
    for (AlertCurrentEntity alert : currentAlerts) {
      assertTrue(lastId > alert.getAlertId());
      lastId = alert.getAlertId();
    }
  }

  /**
   * Tests that the {@link AlertCurrentEntity} fields are updated properly when
   * a new {@link AlertHistoryEntity} is associated.
   *
   * @throws Exception
   */
  @Test
  public void testAlertCurrentUpdatesViaHistory() throws Exception {
    AlertDefinitionEntity hostDef = new AlertDefinitionEntity();
    hostDef.setDefinitionName("Host Alert Definition ");
    hostDef.setServiceName("YARN");
    hostDef.setComponentName(null);
    hostDef.setClusterId(m_cluster.getClusterId());
    hostDef.setHash(UUID.randomUUID().toString());
    hostDef.setScheduleInterval(Integer.valueOf(60));
    hostDef.setScope(Scope.HOST);
    hostDef.setSource("{\"type\" : \"SCRIPT\"}");
    hostDef.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(hostDef);

    // history for the definition
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(hostDef.getServiceName());
    history.setClusterId(m_cluster.getClusterId());
    history.setAlertDefinition(hostDef);
    history.setAlertLabel(hostDef.getDefinitionName());
    history.setAlertText(hostDef.getDefinitionName());
    history.setAlertTimestamp(Long.valueOf(1L));
    history.setHostName("h2");
    history.setAlertState(AlertState.OK);
    m_dao.create(history);

    // current for the history
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setOriginalTimestamp(1L);
    current.setLatestTimestamp(2L);
    current.setAlertHistory(history);
    m_dao.create(current);

    assertEquals(history.getAlertText(), current.getLatestText());

    history.setAlertText("foobar!");
    current.setAlertHistory(history);
    assertEquals(history.getAlertText(), current.getLatestText());
  }

  /**
   *
   */
  @Test
  public void testFindByState() {
    List<AlertState> allStates = new ArrayList<>();
    allStates.add(AlertState.OK);
    allStates.add(AlertState.WARNING);
    allStates.add(AlertState.CRITICAL);

    List<AlertHistoryEntity> history = m_dao.findAll(m_cluster.getClusterId(),
        allStates);
    assertNotNull(history);
    assertEquals(50, history.size());

    history = m_dao.findAll(m_cluster.getClusterId(),
        Collections.singletonList(AlertState.OK));
    assertNotNull(history);
    assertEquals(40, history.size());

    history = m_dao.findAll(m_cluster.getClusterId(),
        Collections.singletonList(AlertState.CRITICAL));
    assertNotNull(history);
    assertEquals(10, history.size());

    history = m_dao.findAll(m_cluster.getClusterId(),
        Collections.singletonList(AlertState.WARNING));
    assertNotNull(history);
    assertEquals(0, history.size());
  }

  /**
   *
   */
  @Test
  public void testFindByDate() {
    calendar.clear();
    calendar.set(2014, Calendar.JANUARY, 1);

    // on or after 1/1/2014
    List<AlertHistoryEntity> history = m_dao.findAll(m_cluster.getClusterId(),
        calendar.getTime(), null);

    assertNotNull(history);
    assertEquals(50, history.size());

    // on or before 1/1/2014
    history = m_dao.findAll(m_cluster.getClusterId(), null, calendar.getTime());
    assertNotNull(history);
    assertEquals(1, history.size());

    // between 1/5 and 1/10
    calendar.set(2014, Calendar.JANUARY, 5);
    Date startDate = calendar.getTime();

    calendar.set(2014, Calendar.JANUARY, 10);
    Date endDate = calendar.getTime();

    history = m_dao.findAll(m_cluster.getClusterId(), startDate, endDate);
    assertNotNull(history);
    assertEquals(6, history.size());

    // after 3/1
    calendar.set(2014, Calendar.MARCH, 5);
    history = m_dao.findAll(m_cluster.getClusterId(), calendar.getTime(), null);
    assertNotNull(history);
    assertEquals(0, history.size());

    history = m_dao.findAll(m_cluster.getClusterId(), endDate, startDate);
    assertNotNull(history);
    assertEquals(0, history.size());
  }

  @Test
  public void testFindCurrentByHostAndName() throws Exception {
    AlertCurrentEntity entity = m_dao.findCurrentByHostAndName(
        m_cluster.getClusterId(), "h2", "Alert Definition 1");
    assertNull(entity);

    entity = m_dao.findCurrentByHostAndName(m_cluster.getClusterId(), "h1",
        "Alert Definition 1");

    assertNotNull(entity);
    assertNotNull(entity.getAlertHistory());
    assertNotNull(entity.getAlertHistory().getAlertDefinition());
  }

  /**
   *
   */
  @Test
  public void testFindCurrentSummary() throws Exception {
    AlertSummaryDTO summary = m_dao.findCurrentCounts(m_cluster.getClusterId(),
        null, null);

    assertEquals(5, summary.getOkCount());

    AlertHistoryEntity h1 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        2).getAlertHistory();

    AlertHistoryEntity h2 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        3).getAlertHistory();

    AlertHistoryEntity h3 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        4).getAlertHistory();

    h1.setAlertState(AlertState.WARNING);
    m_dao.merge(h1);
    h2.setAlertState(AlertState.CRITICAL);
    m_dao.merge(h2);
    h3.setAlertState(AlertState.UNKNOWN);
    m_dao.merge(h3);

    int ok = 0;
    int warn = 0;
    int crit = 0;
    int unk = 0;
    int maintenance = 0;

    List<AlertCurrentEntity> currents = m_dao.findCurrentByCluster(m_cluster.getClusterId());
    for (AlertCurrentEntity current : currents) {
      if (current.getMaintenanceState() != MaintenanceState.OFF) {
        maintenance++;
        continue;
      }

      switch (current.getAlertHistory().getAlertState()) {
        case CRITICAL:
          crit++;
          break;
        case OK:
          ok++;
          break;
        case UNKNOWN:
          unk++;
          break;
        default:
          warn++;
          break;
      }
    }

    summary = m_dao.findCurrentCounts(m_cluster.getClusterId(), null, null);

    // !!! db-to-db compare
    assertEquals(ok, summary.getOkCount());
    assertEquals(warn, summary.getWarningCount());
    assertEquals(crit, summary.getCriticalCount());
    assertEquals(unk, summary.getUnknownCount());
    assertEquals(maintenance, summary.getMaintenanceCount());

    // !!! expected
    assertEquals(2, summary.getOkCount());
    assertEquals(1, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(1, summary.getUnknownCount());
    assertEquals(0, summary.getMaintenanceCount());

    summary = m_dao.findCurrentCounts(m_cluster.getClusterId(), "YARN", null);
    assertEquals(2, summary.getOkCount());
    assertEquals(1, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(1, summary.getUnknownCount());

    summary = m_dao.findCurrentCounts(m_cluster.getClusterId(), null, "h1");
    assertEquals(2, summary.getOkCount());
    assertEquals(1, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(1, summary.getUnknownCount());
    assertEquals(0, summary.getMaintenanceCount());

    summary = m_dao.findCurrentCounts(m_cluster.getClusterId(), "foo", null);
    assertEquals(0, summary.getOkCount());
    assertEquals(0, summary.getWarningCount());
    assertEquals(0, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(0, summary.getMaintenanceCount());

    // try out maintenance mode for all WARNINGs
    for (AlertCurrentEntity current : currents) {
      if (current.getAlertHistory().getAlertState() == AlertState.WARNING) {
        current.setMaintenanceState(MaintenanceState.ON);
        m_dao.merge(current);
      }
    }

    summary = m_dao.findCurrentCounts(m_cluster.getClusterId(), null, null);
    assertEquals(2, summary.getOkCount());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(1, summary.getUnknownCount());
    assertEquals(1, summary.getMaintenanceCount());
  }

  /**
   *
   */
  @Test
  public void testFindCurrentPerHostSummary() throws Exception {
    // Add extra host and alerts
    m_helper.addHost(m_clusters, m_cluster, "h2");
    List<AlertDefinitionEntity> definitions = m_definitionDao.findAll();
    AlertDefinitionEntity definition = definitions.get(0);
    AlertHistoryEntity h2CriticalHistory = new AlertHistoryEntity();
    h2CriticalHistory.setServiceName(definition.getServiceName());
    h2CriticalHistory.setClusterId(m_cluster.getClusterId());
    h2CriticalHistory.setAlertDefinition(definition);
    h2CriticalHistory.setAlertLabel(definition.getDefinitionName() + " h2");
    h2CriticalHistory.setAlertText(definition.getDefinitionName() + " h2");
    h2CriticalHistory.setAlertTimestamp(calendar.getTimeInMillis());
    h2CriticalHistory.setComponentName(definition.getComponentName());
    h2CriticalHistory.setHostName("h2");
    h2CriticalHistory.setAlertState(AlertState.CRITICAL);
    m_dao.create(h2CriticalHistory);
    AlertCurrentEntity h2CriticalCurrent = new AlertCurrentEntity();
    h2CriticalCurrent.setAlertHistory(h2CriticalHistory);
    h2CriticalCurrent.setLatestTimestamp(new Date().getTime());
    h2CriticalCurrent.setOriginalTimestamp(new Date().getTime() - 10800000);
    h2CriticalCurrent.setMaintenanceState(MaintenanceState.OFF);
    m_dao.create(h2CriticalCurrent);

    try {
      long clusterId = m_cluster.getClusterId();
      AlertSummaryDTO summary = m_dao.findCurrentCounts(clusterId, null, null);
      assertEquals(5, summary.getOkCount());

      AlertHistoryEntity h1 = m_dao.findCurrentByCluster(clusterId).get(2).getAlertHistory();
      AlertHistoryEntity h2 = m_dao.findCurrentByCluster(clusterId).get(3).getAlertHistory();
      AlertHistoryEntity h3 = m_dao.findCurrentByCluster(clusterId).get(4).getAlertHistory();

      h1.setAlertState(AlertState.WARNING);
      m_dao.merge(h1);
      h2.setAlertState(AlertState.CRITICAL);
      m_dao.merge(h2);
      h3.setAlertState(AlertState.UNKNOWN);
      m_dao.merge(h3);

      Map<String, AlertSummaryDTO> perHostSummary = m_dao.findCurrentPerHostCounts(clusterId);

      AlertSummaryDTO h1summary = m_dao.findCurrentCounts(clusterId, null, "h1");
      assertEquals(2, h1summary.getOkCount());
      assertEquals(1, h1summary.getWarningCount());
      assertEquals(1, h1summary.getCriticalCount());
      assertEquals(1, h1summary.getUnknownCount());
      assertEquals(0, h1summary.getMaintenanceCount());

      AlertSummaryDTO h2summary = m_dao.findCurrentCounts(clusterId, null, "h2");
      assertEquals(0, h2summary.getOkCount());
      assertEquals(0, h2summary.getWarningCount());
      assertEquals(1, h2summary.getCriticalCount());
      assertEquals(0, h2summary.getUnknownCount());
      assertEquals(0, h2summary.getMaintenanceCount());

      AlertSummaryDTO h1PerHostSummary = perHostSummary.get("h1");
      assertEquals(h1PerHostSummary.getOkCount(), h1summary.getOkCount());
      assertEquals(h1PerHostSummary.getWarningCount(), h1summary.getWarningCount());
      assertEquals(h1PerHostSummary.getCriticalCount(), h1summary.getCriticalCount());
      assertEquals(h1PerHostSummary.getUnknownCount(), h1summary.getUnknownCount());
      assertEquals(h1PerHostSummary.getMaintenanceCount(), h1summary.getMaintenanceCount());

      AlertSummaryDTO h2PerHostSummary = perHostSummary.get("h2");
      assertEquals(h2PerHostSummary.getOkCount(), h2summary.getOkCount());
      assertEquals(h2PerHostSummary.getWarningCount(), h2summary.getWarningCount());
      assertEquals(h2PerHostSummary.getCriticalCount(), h2summary.getCriticalCount());
      assertEquals(h2PerHostSummary.getUnknownCount(), h2summary.getUnknownCount());
      assertEquals(h2PerHostSummary.getMaintenanceCount(), h2summary.getMaintenanceCount());
    } finally {
      // Cleanup extra host and alerts to not effect other tests
      m_dao.remove(h2CriticalCurrent);
      m_dao.remove(h2CriticalHistory);
      m_clusters.unmapHostFromCluster("h2", m_cluster.getClusterName());
    }
  }

  /**
   *
   */
  @Test
  public void testFindCurrentHostSummary() throws Exception {
    // start out with 1 since all alerts are for a single host and are OK
    AlertHostSummaryDTO summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(0, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(1, summary.getOkCount());

    // grab 1 and change it to warning
    AlertHistoryEntity history1 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        1).getAlertHistory();

    history1.setAlertState(AlertState.WARNING);
    m_dao.merge(history1);

    // verify host changed to warning
    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(1, summary.getWarningCount());
    assertEquals(0, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(0, summary.getOkCount());

    history1.setAlertState(AlertState.CRITICAL);
    m_dao.merge(history1);

    // verify host changed to critical
    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(0, summary.getOkCount());

    // grab another and change the host so that an OK shows up
    AlertHistoryEntity history2 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        2).getAlertHistory();

    history2.setHostName(history2.getHostName() + "-foo");
    m_dao.merge(history2);

    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(1, summary.getOkCount());

    // grab another and change that host name as well
    AlertHistoryEntity history3 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        3).getAlertHistory();

    // change the name to simulate a 3rd host
    history3.setHostName(history3.getHostName() + "-bar");
    m_dao.merge(history3);

    // verify 2 hosts report OK
    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(2, summary.getOkCount());

    // grab another and change that host name and the state to UNKNOWN
    AlertHistoryEntity history4 = m_dao.findCurrentByCluster(m_cluster.getClusterId()).get(
        4).getAlertHistory();

    history4.setHostName(history4.getHostName() + "-baz");
    history4.setAlertState(AlertState.UNKNOWN);
    m_dao.merge(history3);

    // verify a new host shows up with UNKNOWN status hosts report OK
    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(1, summary.getUnknownCount());
    assertEquals(2, summary.getOkCount());

    // put 1 alert into maintenance mode
    AlertCurrentEntity current4 = m_dao.findCurrentByCluster(
        m_cluster.getClusterId()).get(4);

    current4.setMaintenanceState(MaintenanceState.ON);
    m_dao.merge(current4);

    // verify that the UNKNOWN host has moved back to OK
    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(3, summary.getOkCount());

    // put all alerts into maintenance mode
    List<AlertCurrentEntity> currents = m_dao.findCurrentByCluster(m_cluster.getClusterId());
    for (AlertCurrentEntity current : currents) {
      current.setMaintenanceState(MaintenanceState.ON);
      m_dao.merge(current);
    }

    // verify that all are OK
    summary = m_dao.findCurrentHostCounts(m_cluster.getClusterId());
    assertEquals(0, summary.getWarningCount());
    assertEquals(0, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
    assertEquals(4, summary.getOkCount());
  }

  @Test
  public void testFindAggregates() throws Exception {
    // definition
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("many_per_cluster");
    definition.setServiceName("YARN");
    definition.setComponentName(null);
    definition.setClusterId(m_cluster.getClusterId());
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(Integer.valueOf(60));
    definition.setScope(Scope.SERVICE);
    definition.setSource("{\"type\" : \"SCRIPT\"}");
    definition.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(definition);

    // history record #1 and current
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setAlertInstance(null);
    history.setAlertLabel("");
    history.setAlertState(AlertState.OK);
    history.setAlertText("");
    history.setAlertTimestamp(Long.valueOf(1L));
    history.setClusterId(m_cluster.getClusterId());
    history.setComponentName("");
    history.setHostName("h1");
    history.setServiceName("ServiceName");

    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setAlertHistory(history);
    current.setLatestTimestamp(Long.valueOf(1L));
    current.setOriginalTimestamp(Long.valueOf(1L));
    m_dao.merge(current);

    // history record #2 and current
    history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setAlertInstance(null);
    history.setAlertLabel("");
    history.setAlertState(AlertState.OK);
    history.setAlertText("");
    history.setAlertTimestamp(Long.valueOf(1L));
    history.setClusterId(m_cluster.getClusterId());
    history.setComponentName("");
    history.setHostName("h2");
    history.setServiceName("ServiceName");
    m_dao.create(history);

    current = new AlertCurrentEntity();
    current.setAlertHistory(history);
    current.setLatestTimestamp(Long.valueOf(1L));
    current.setOriginalTimestamp(Long.valueOf(1L));
    m_dao.merge(current);

    AlertSummaryDTO summary = m_dao.findAggregateCounts(
        m_cluster.getClusterId(), "many_per_cluster");
    assertEquals(2, summary.getOkCount());
    assertEquals(0, summary.getWarningCount());
    assertEquals(0, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());

    AlertCurrentEntity c = m_dao.findCurrentByHostAndName(
        m_cluster.getClusterId(),
        "h2", "many_per_cluster");
    AlertHistoryEntity h = c.getAlertHistory();
    h.setAlertState(AlertState.CRITICAL);
    m_dao.merge(h);

    summary = m_dao.findAggregateCounts(m_cluster.getClusterId(),
        "many_per_cluster");
    assertEquals(1, summary.getOkCount());
    assertEquals(0, summary.getWarningCount());
    assertEquals(1, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());

    summary = m_dao.findAggregateCounts(m_cluster.getClusterId(), "foo");
    assertEquals(0, summary.getOkCount());
    assertEquals(0, summary.getWarningCount());
    assertEquals(0, summary.getCriticalCount());
    assertEquals(0, summary.getUnknownCount());
  }

  /**
   * Tests <a
   * href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=398067">https:/
   * /bugs.eclipse.org/bugs/show_bug.cgi?id=398067</a> which causes an inner
   * entity to be stale.
   */
  @Test
  public void testJPAInnerEntityStaleness() {
    List<AlertCurrentEntity> currents = m_dao.findCurrent();
    AlertCurrentEntity current = currents.get(0);
    AlertHistoryEntity oldHistory = current.getAlertHistory();

    AlertHistoryEntity newHistory = new AlertHistoryEntity();
    newHistory.setAlertDefinition(oldHistory.getAlertDefinition());
    newHistory.setAlertInstance(oldHistory.getAlertInstance());
    newHistory.setAlertLabel(oldHistory.getAlertLabel());

    if (oldHistory.getAlertState() == AlertState.OK) {
      newHistory.setAlertState(AlertState.CRITICAL);
    } else {
      newHistory.setAlertState(AlertState.OK);
    }

    newHistory.setAlertText("New History");
    newHistory.setClusterId(oldHistory.getClusterId());
    newHistory.setAlertTimestamp(System.currentTimeMillis());
    newHistory.setComponentName(oldHistory.getComponentName());
    newHistory.setHostName(oldHistory.getHostName());
    newHistory.setServiceName(oldHistory.getServiceName());

    m_dao.create(newHistory);

    assertTrue(newHistory.getAlertId().longValue() != oldHistory.getAlertId().longValue());

    current.setAlertHistory(newHistory);
    m_dao.merge(current);

    AlertCurrentEntity newCurrent = m_dao.findCurrentByHostAndName(
        newHistory.getClusterId(),
        newHistory.getHostName(),
        newHistory.getAlertDefinition().getDefinitionName());

    assertEquals(newHistory.getAlertId(),
        newCurrent.getAlertHistory().getAlertId());

    assertEquals(newHistory.getAlertState(),
        newCurrent.getAlertHistory().getAlertState());

    newCurrent = m_dao.findCurrentById(current.getAlertId());

    assertEquals(newHistory.getAlertId(),
        newCurrent.getAlertHistory().getAlertId());

    assertEquals(newHistory.getAlertState(),
        newCurrent.getAlertHistory().getAlertState());

  }

  /**
   * Tests that maintenance mode is set correctly on notices.
   *
   * @throws Exception
   */
  @Test
  public void testMaintenanceMode() throws Exception {
    m_helper.installHdfsService(m_cluster, m_serviceFactory,
        m_componentFactory, m_schFactory, HOSTNAME);

    List<AlertCurrentEntity> currents = m_dao.findCurrent();
    for (AlertCurrentEntity current : currents) {
      m_dao.remove(current);
    }

    // create some definitions
    AlertDefinitionEntity namenode = new AlertDefinitionEntity();
    namenode.setDefinitionName("NAMENODE");
    namenode.setServiceName("HDFS");
    namenode.setComponentName("NAMENODE");
    namenode.setClusterId(m_cluster.getClusterId());
    namenode.setHash(UUID.randomUUID().toString());
    namenode.setScheduleInterval(Integer.valueOf(60));
    namenode.setScope(Scope.ANY);
    namenode.setSource("{\"type\" : \"SCRIPT\"}");
    namenode.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(namenode);

    AlertDefinitionEntity datanode = new AlertDefinitionEntity();
    datanode.setDefinitionName("DATANODE");
    datanode.setServiceName("HDFS");
    datanode.setComponentName("DATANODE");
    datanode.setClusterId(m_cluster.getClusterId());
    datanode.setHash(UUID.randomUUID().toString());
    datanode.setScheduleInterval(Integer.valueOf(60));
    datanode.setScope(Scope.HOST);
    datanode.setSource("{\"type\" : \"SCRIPT\"}");
    datanode.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(datanode);

    AlertDefinitionEntity aggregate = new AlertDefinitionEntity();
    aggregate.setDefinitionName("DATANODE_UP");
    aggregate.setServiceName("HDFS");
    aggregate.setComponentName(null);
    aggregate.setClusterId(m_cluster.getClusterId());
    aggregate.setHash(UUID.randomUUID().toString());
    aggregate.setScheduleInterval(Integer.valueOf(60));
    aggregate.setScope(Scope.SERVICE);
    aggregate.setSource("{\"type\" : \"SCRIPT\"}");
    aggregate.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(aggregate);

    // create some history
    AlertHistoryEntity nnHistory = new AlertHistoryEntity();
    nnHistory.setAlertState(AlertState.OK);
    nnHistory.setServiceName(namenode.getServiceName());
    nnHistory.setComponentName(namenode.getComponentName());
    nnHistory.setClusterId(m_cluster.getClusterId());
    nnHistory.setAlertDefinition(namenode);
    nnHistory.setAlertLabel(namenode.getDefinitionName());
    nnHistory.setAlertText(namenode.getDefinitionName());
    nnHistory.setAlertTimestamp(calendar.getTimeInMillis());
    nnHistory.setHostName(HOSTNAME);
    m_dao.create(nnHistory);

    AlertCurrentEntity nnCurrent = new AlertCurrentEntity();
    nnCurrent.setAlertHistory(nnHistory);
    nnCurrent.setLatestText(nnHistory.getAlertText());
    nnCurrent.setMaintenanceState(MaintenanceState.OFF);
    nnCurrent.setOriginalTimestamp(System.currentTimeMillis());
    nnCurrent.setLatestTimestamp(System.currentTimeMillis());
    m_dao.create(nnCurrent);

    AlertHistoryEntity dnHistory = new AlertHistoryEntity();
    dnHistory.setAlertState(AlertState.WARNING);
    dnHistory.setServiceName(datanode.getServiceName());
    dnHistory.setComponentName(datanode.getComponentName());
    dnHistory.setClusterId(m_cluster.getClusterId());
    dnHistory.setAlertDefinition(datanode);
    dnHistory.setAlertLabel(datanode.getDefinitionName());
    dnHistory.setAlertText(datanode.getDefinitionName());
    dnHistory.setAlertTimestamp(calendar.getTimeInMillis());
    dnHistory.setHostName(HOSTNAME);
    m_dao.create(dnHistory);

    AlertCurrentEntity dnCurrent = new AlertCurrentEntity();
    dnCurrent.setAlertHistory(dnHistory);
    dnCurrent.setLatestText(dnHistory.getAlertText());
    dnCurrent.setMaintenanceState(MaintenanceState.OFF);
    dnCurrent.setOriginalTimestamp(System.currentTimeMillis());
    dnCurrent.setLatestTimestamp(System.currentTimeMillis());
    m_dao.create(dnCurrent);

    AlertHistoryEntity aggregateHistory = new AlertHistoryEntity();
    aggregateHistory.setAlertState(AlertState.CRITICAL);
    aggregateHistory.setServiceName(aggregate.getServiceName());
    aggregateHistory.setComponentName(aggregate.getComponentName());
    aggregateHistory.setClusterId(m_cluster.getClusterId());
    aggregateHistory.setAlertDefinition(aggregate);
    aggregateHistory.setAlertLabel(aggregate.getDefinitionName());
    aggregateHistory.setAlertText(aggregate.getDefinitionName());
    aggregateHistory.setAlertTimestamp(calendar.getTimeInMillis());
    m_dao.create(aggregateHistory);

    AlertCurrentEntity aggregateCurrent = new AlertCurrentEntity();
    aggregateCurrent.setAlertHistory(aggregateHistory);
    aggregateCurrent.setLatestText(aggregateHistory.getAlertText());
    aggregateCurrent.setMaintenanceState(MaintenanceState.OFF);
    aggregateCurrent.setOriginalTimestamp(System.currentTimeMillis());
    aggregateCurrent.setLatestTimestamp(System.currentTimeMillis());
    m_dao.create(aggregateCurrent);

    currents = m_dao.findCurrent();
    assertEquals(3, currents.size());

    for (AlertCurrentEntity current : currents) {
      assertEquals(MaintenanceState.OFF, current.getMaintenanceState());
    }

    // turn on HDFS MM
    Service hdfs = m_clusters.getClusterById(m_cluster.getClusterId()).getService(
        "HDFS");

    hdfs.setMaintenanceState(MaintenanceState.ON);

    currents = m_dao.findCurrent();
    assertEquals(3, currents.size());
    for (AlertCurrentEntity current : currents) {
      assertEquals(MaintenanceState.ON, current.getMaintenanceState());
    }

    // turn HDFS MM off
    hdfs.setMaintenanceState(MaintenanceState.OFF);

    currents = m_dao.findCurrent();
    assertEquals(3, currents.size());
    for (AlertCurrentEntity current : currents) {
      assertEquals(MaintenanceState.OFF, current.getMaintenanceState());
    }

    // turn on host MM
    Host host = m_clusters.getHost(HOSTNAME);
    host.setMaintenanceState(m_cluster.getClusterId(), MaintenanceState.ON);

    // only NAMENODE and DATANODE should be in MM; the aggregate should not
    // since the host is in MM
    currents = m_dao.findCurrent();
    assertEquals(3, currents.size());
    for (AlertCurrentEntity current : currents) {
      if (current.getAlertHistory().getComponentName() != null) {
        assertEquals(MaintenanceState.ON, current.getMaintenanceState());
      } else {
        assertEquals(MaintenanceState.OFF, current.getMaintenanceState());
      }
    }

    // turn host MM off
    host.setMaintenanceState(m_cluster.getClusterId(), MaintenanceState.OFF);

    currents = m_dao.findCurrent();
    assertEquals(3, currents.size());
    for (AlertCurrentEntity current : currents) {
      assertEquals(MaintenanceState.OFF, current.getMaintenanceState());
    }

    // turn a component MM on
    ServiceComponentHost nnComponent = null;
    List<ServiceComponentHost> schs = m_cluster.getServiceComponentHosts(HOSTNAME);
    for (ServiceComponentHost sch : schs) {
      if ("NAMENODE".equals(sch.getServiceComponentName())) {
        sch.setMaintenanceState(MaintenanceState.ON);
        nnComponent = sch;
      }
    }

    assertNotNull(nnComponent);

    currents = m_dao.findCurrent();
    assertEquals(3, currents.size());
    for (AlertCurrentEntity current : currents) {
      if ("NAMENODE".equals(current.getAlertHistory().getComponentName())) {
        assertEquals(MaintenanceState.ON, current.getMaintenanceState());
      } else {
        assertEquals(MaintenanceState.OFF, current.getMaintenanceState());
      }
    }
  }

  /**
   * Tests that the Ambari {@link Predicate} can be converted and submitted to
   * JPA correctly to return a restricted result set.
   *
   * @throws Exception
   */
  @Test
  public void testAlertHistoryPredicate() throws Exception {
    m_helper.installHdfsService(m_cluster, m_serviceFactory,
        m_componentFactory, m_schFactory, HOSTNAME);
    m_alertHelper.populateData(m_cluster);

    Predicate clusterPredicate = null;
    Predicate hdfsPredicate = null;
    Predicate yarnPredicate = null;
    Predicate clusterAndHdfsPredicate = null;
    Predicate clusterAndHdfsAndCriticalPredicate = null;
    Predicate hdfsAndCriticalOrWarningPredicate = null;
    Predicate alertNamePredicate = null;
    Predicate historyIdPredicate = null;

    clusterPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").toPredicate();

    AlertHistoryRequest request = new AlertHistoryRequest();

    request.Predicate = clusterPredicate;
    List<AlertHistoryEntity> histories = m_dao.findAll(request);
    assertEquals(3, histories.size());

    hdfsPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME).equals("HDFS").toPredicate();

    yarnPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME).equals("YARN").toPredicate();

    clusterAndHdfsPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").and().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME).equals("HDFS").toPredicate();

    clusterAndHdfsAndCriticalPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").and().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME).equals("HDFS").and().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE).equals(
        AlertState.CRITICAL.name()).toPredicate();

    hdfsAndCriticalOrWarningPredicate = new PredicateBuilder().begin().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME).equals("HDFS").and().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE).equals(
        AlertState.CRITICAL.name()).end().or().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE).equals(
        AlertState.WARNING.name()).toPredicate();

    alertNamePredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME).equals(
        "NAMENODE").toPredicate();

    request.Predicate = hdfsPredicate;
    histories = m_dao.findAll(request);
    assertEquals(2, histories.size());

    request.Predicate = yarnPredicate;
    histories = m_dao.findAll(request);
    assertEquals(1, histories.size());

    request.Predicate = clusterAndHdfsPredicate;
    histories = m_dao.findAll(request);
    assertEquals(2, histories.size());

    request.Predicate = clusterAndHdfsAndCriticalPredicate;
    histories = m_dao.findAll(request);
    assertEquals(0, histories.size());

    request.Predicate = hdfsAndCriticalOrWarningPredicate;
    histories = m_dao.findAll(request);
    assertEquals(1, histories.size());

    request.Predicate = alertNamePredicate;
    histories = m_dao.findAll(request);
    assertEquals(1, histories.size());

    historyIdPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_ID).equals(
        histories.get(0).getAlertId()).toPredicate();

    request.Predicate = historyIdPredicate;
    histories = m_dao.findAll(request);
    assertEquals(1, histories.size());
  }

  /**
   * Tests that JPA does the pagination work for us.
   *
   * @throws Exception
   */
  @Test
  public void testAlertHistoryPagination() throws Exception {
    m_helper.installHdfsService(m_cluster, m_serviceFactory,
        m_componentFactory, m_schFactory, HOSTNAME);
    m_alertHelper.populateData(m_cluster);

    AlertHistoryRequest request = new AlertHistoryRequest();
    request.Pagination = null;

    // get back all 3
    List<AlertHistoryEntity> histories = m_dao.findAll(request);
    assertEquals(3, histories.size());

    // only the first 2
    request.Pagination = new PageRequestImpl(StartingPoint.Beginning, 2, 0,
        null, null);

    histories = m_dao.findAll(request);
    assertEquals(2, histories.size());

    // the 2nd and 3rd
    request.Pagination = new PageRequestImpl(StartingPoint.Beginning, 1, 2,
        null, null);

    histories = m_dao.findAll(request);
    assertEquals(1, histories.size());

    // none b/c we're out of index
    request.Pagination = new PageRequestImpl(StartingPoint.Beginning, 1, 3,
        null, null);

    histories = m_dao.findAll(request);
    assertEquals(0, histories.size());
  }

  /**
   * Tests that JPA does the sorting work for us.
   *
   * @throws Exception
   */
  @Test
  public void testAlertHistorySorting() throws Exception {
    m_helper.installHdfsService(m_cluster, m_serviceFactory,
        m_componentFactory, m_schFactory, HOSTNAME);
    m_alertHelper.populateData(m_cluster);

    List<SortRequestProperty> sortProperties = new ArrayList<>();
    SortRequest sortRequest = new SortRequestImpl(sortProperties);
    AlertHistoryRequest request = new AlertHistoryRequest();
    request.Sort = sortRequest;

    Predicate clusterPredicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").toPredicate();

    request.Predicate = clusterPredicate;

    sortProperties.add(new SortRequestProperty(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME, Order.ASC));

    // get back all 3
    List<AlertHistoryEntity> histories = m_dao.findAll(request);
    assertEquals(3, histories.size());

    // assert sorting ASC
    String lastServiceName = null;
    for (AlertHistoryEntity history : histories) {
      if (null == lastServiceName) {
        lastServiceName = history.getServiceName();
        continue;
      }

      String currentServiceName = history.getServiceName();
      assertTrue(lastServiceName.compareTo(currentServiceName) <= 0);
      lastServiceName = currentServiceName;
    }

    // clear and do DESC
    sortProperties.clear();
    sortProperties.add(new SortRequestProperty(
        AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME, Order.DESC));

    // get back all 3
    histories = m_dao.findAll(request);
    assertEquals(3, histories.size());

    // assert sorting DESC
    lastServiceName = null;
    for (AlertHistoryEntity history : histories) {
      if (null == lastServiceName) {
        lastServiceName = history.getServiceName();
        continue;
      }

      String currentServiceName = history.getServiceName();
      assertTrue(lastServiceName.compareTo(currentServiceName) >= 0);
      lastServiceName = currentServiceName;
    }
  }

  @Test
  public void testRemoveCurrenyByService() throws Exception {
    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrent();
    assertNotNull(currentAlerts);
    assertEquals(5, currentAlerts.size());

    // assert none removed for HDFS
    m_dao.removeCurrentByService(m_cluster.getClusterId(), "HDFS");
    currentAlerts = m_dao.findCurrent();
    assertEquals(5, currentAlerts.size());

    m_dao.removeCurrentByService(m_cluster.getClusterId(), "YARN");
    currentAlerts = m_dao.findCurrent();
    assertEquals(0, currentAlerts.size());
  }

  @Test
  public void testRemoveCurrenyByHost() throws Exception {
    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrent();
    assertNotNull(currentAlerts);
    assertEquals(5, currentAlerts.size());

    // there is no h2 host
    m_dao.removeCurrentByHost("h2");
    currentAlerts = m_dao.findCurrent();
    assertEquals(5, currentAlerts.size());

    // there is an h1 host
    m_dao.removeCurrentByHost("h1");
    currentAlerts = m_dao.findCurrent();
    assertEquals(0, currentAlerts.size());
  }

  @Test
  public void testRemoveCurrenyByComponentHost() throws Exception {
    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrent();
    assertNotNull(currentAlerts);
    assertEquals(5, currentAlerts.size());

    AlertCurrentEntity entity = m_dao.findCurrentByHostAndName(
        m_cluster.getClusterId(), "h1", "Alert Definition 1");

    assertNotNull(entity);

    m_dao.removeCurrentByServiceComponentHost(m_cluster.getClusterId(),
        entity.getAlertHistory().getServiceName(),
        entity.getAlertHistory().getComponentName(),
        entity.getAlertHistory().getHostName());

    currentAlerts = m_dao.findCurrent();
    assertEquals(4, currentAlerts.size());
  }

  @Test
  public void testRemoveCurrentDisabled() throws Exception {
    List<AlertCurrentEntity> currentAlerts = m_dao.findCurrent();
    assertNotNull(currentAlerts);
    assertEquals(5, currentAlerts.size());

    AlertDefinitionEntity definition = currentAlerts.get(0).getAlertHistory().getAlertDefinition();
    definition.setEnabled(false);
    m_definitionDao.merge(definition);

    m_dao.removeCurrentDisabledAlerts();

    currentAlerts = m_dao.findCurrent();
    assertEquals(4, currentAlerts.size());
  }
}
