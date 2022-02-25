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
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

import junit.framework.Assert;

/**
 * Tests {@link AlertDefinitionDAO} for interacting with
 * {@link AlertDefinitionEntity}.
 */
public class AlertDefinitionDAOTest {

  static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  Injector injector;
  Long clusterId;
  AlertDefinitionDAO dao;
  AlertsDAO alertsDao;
  AlertDispatchDAO dispatchDao;
  OrmTestHelper helper;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
//    LoggerFactory.getLogger("eclipselink").
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(UnitOfWork.class).begin();

    dispatchDao = injector.getInstance(AlertDispatchDAO.class);
    dao = injector.getInstance(AlertDefinitionDAO.class);
    alertsDao = injector.getInstance(AlertsDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();

    // create required default groups
    helper.createDefaultAlertGroups(clusterId);

    // create 8 HDFS alerts
    int i = 0;
    for (; i < 8; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("HDFS");
      definition.setComponentName(null);
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      dao.create(definition);
    }

    // create 2 HDFS with components
    for (; i < 10; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("HDFS");

      if (i == 9) {
        definition.setComponentName("NAMENODE");
      } else {
        definition.setComponentName("DATANODE");
      }

      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      dao.create(definition);
    }

    // create 2 host scoped
    for (; i < 12; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("OOZIE");
      definition.setComponentName("OOZIE_SERVER");
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.HOST);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      dao.create(definition);
    }

    // create 3 agent alerts
    for (; i < 15; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName(RootService.AMBARI.name());
      definition.setComponentName(RootComponent.AMBARI_AGENT.name());
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.HOST);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      dao.create(definition);
    }
  }

  @After
  public void teardown() throws Exception {
    injector.getInstance(UnitOfWork.class).end();

    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }

  /**
   *
   */
  @Test
  public void testFindByName() {
    List<AlertDefinitionEntity> definitions = dao.findAll();
    assertNotNull(definitions);
    AlertDefinitionEntity definition = definitions.get(2);
    AlertDefinitionEntity retrieved = dao.findByName(
        definition.getClusterId(), definition.getDefinitionName());

    assertEquals(definition, retrieved);
  }

  /**
   *
   */
  @Test
  public void testFindAll() {
    List<AlertDefinitionEntity> definitions = dao.findAll();
    assertNotNull(definitions);
    assertEquals(15, definitions.size());
  }

  /**
  *
  */
  @Test
  public void testFindAllEnabled() {
    List<AlertDefinitionEntity> definitions = dao.findAll();
    assertNotNull(definitions);
    assertEquals(15, definitions.size());

    List<AlertDefinitionEntity> enabledDefinitions = dao.findAllEnabled(clusterId);
    assertNotNull(enabledDefinitions);
    assertEquals(definitions.size(), enabledDefinitions.size());

    enabledDefinitions.get(0).setEnabled(false);
    dao.merge(enabledDefinitions.get(0));

    enabledDefinitions = dao.findAllEnabled(clusterId);
    assertNotNull(enabledDefinitions);
    assertEquals(definitions.size() - 1, enabledDefinitions.size());
  }

  /**
   *
   */
  @Test
  public void testFindById() {
    List<AlertDefinitionEntity> definitions = dao.findAll();
    assertNotNull(definitions);
    AlertDefinitionEntity definition = definitions.get(2);
    AlertDefinitionEntity retrieved = dao.findById(definition.getDefinitionId());
    assertEquals(definition, retrieved);
  }

  /**
  *
  */
  @Test
  public void testFindByIds() {
    List<AlertDefinitionEntity> definitions = dao.findAll();
    List<Long> ids = new ArrayList<>();
    ids.add(definitions.get(0).getDefinitionId());
    ids.add(definitions.get(1).getDefinitionId());
    ids.add(99999L);

    definitions = dao.findByIds(ids);
    assertEquals(2, definitions.size());
  }

  /**
   *
   */
  @Test
  public void testFindByService() {
    List<AlertDefinitionEntity> definitions = dao.findByService(clusterId,
        "HDFS");

    assertNotNull(definitions);
    assertEquals(10, definitions.size());

    definitions = dao.findByService(clusterId, "YARN");
    assertNotNull(definitions);
    assertEquals(0, definitions.size());
  }

  /**
   *
   */
  @Test
  public void testFindByServiceComponent() {
    List<AlertDefinitionEntity> definitions = dao.findByServiceComponent(
      clusterId, "OOZIE", "OOZIE_SERVER");

    assertNotNull(definitions);
    assertEquals(2, definitions.size());
  }

  /**
   *
   */
  @Test
  public void testFindAgentScoped() {
    List<AlertDefinitionEntity> definitions = dao.findAgentScoped(clusterId);
    assertNotNull(definitions);
    assertEquals(3, definitions.size());
  }

  @Test
  public void testRefresh() {
  }

  @Test
  public void testCreate() {
  }

  @Test
  public void testMerge() {
  }

  @Test
  public void testRemove() throws Exception {
    AlertDefinitionEntity definition = helper.createAlertDefinition(clusterId);
    definition = dao.findById(definition.getDefinitionId());
    assertNotNull(definition);
    dao.remove(definition);
    definition = dao.findById(definition.getDefinitionId());
    assertNull(definition);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCascadeDelete() throws Exception {
    AlertDefinitionEntity definition = helper.createAlertDefinition(clusterId);

    AlertGroupEntity group = helper.createAlertGroup(clusterId, null);
    group.addAlertDefinition(definition);
    dispatchDao.merge(group);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(clusterId);
    history.setAlertDefinition(definition);
    history.setAlertLabel("Label");
    history.setAlertState(AlertState.OK);
    history.setAlertText("Alert Text");
    history.setAlertTimestamp(calendar.getTimeInMillis());
    alertsDao.create(history);

    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setAlertHistory(history);
    current.setLatestTimestamp(new Date().getTime());
    current.setOriginalTimestamp(new Date().getTime() - 10800000);
    current.setMaintenanceState(MaintenanceState.OFF);
    alertsDao.create(current);

    AlertNoticeEntity notice = new AlertNoticeEntity();
    notice.setAlertHistory(history);
    notice.setAlertTarget(helper.createAlertTarget());
    notice.setNotifyState(NotificationState.PENDING);
    notice.setUuid(UUID.randomUUID().toString());
    dispatchDao.create(notice);

    group = dispatchDao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertDefinitions());
    assertEquals(1, group.getAlertDefinitions().size());

    history = alertsDao.findById(history.getAlertId());
    assertNotNull(history);

    current = alertsDao.findCurrentById(current.getAlertId());
    assertNotNull(current);
    assertNotNull(current.getAlertHistory());

    notice = dispatchDao.findNoticeById(notice.getNotificationId());
    assertNotNull(notice);
    assertNotNull(notice.getAlertHistory());
    assertNotNull(notice.getAlertTarget());

    // delete the definition
    definition = dao.findById(definition.getDefinitionId());
    dao.refresh(definition);
    dao.remove(definition);

    notice = dispatchDao.findNoticeById(notice.getNotificationId());
    assertNull(notice);

    current = alertsDao.findCurrentById(current.getAlertId());
    assertNull(current);

    history = alertsDao.findById(history.getAlertId());
    assertNull(history);

    group = dispatchDao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertDefinitions());
    assertEquals(0, group.getAlertDefinitions().size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCascadeDeleteForCluster() throws Exception {
    AlertDefinitionEntity definition = helper.createAlertDefinition(clusterId);
    definition = dao.findById(definition.getDefinitionId());
    dao.refresh(definition);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    clusterDAO.refresh(clusterEntity);

    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getClusterById(clusterId);
    cluster.delete();

    assertNull(clusterDAO.findById(clusterId));
    assertNull(dao.findById(definition.getDefinitionId()));

    assertEquals(0, dispatchDao.findAllGroups(clusterId).size());
  }

  @Test
  public void testNestedClusterEntity() throws Exception {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("nested-cluster-entity-test");
    definition.setServiceName("HDFS");
    definition.setComponentName(null);
    definition.setClusterId(clusterId);
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(60);
    definition.setScope(Scope.SERVICE);
    definition.setSource("{\"type\" : \"SCRIPT\"}");
    definition.setSourceType(SourceType.SCRIPT);
    dao.create(definition);

    definition = dao.findById(definition.getDefinitionId());
    assertNotNull(definition.getCluster());
    assertEquals(clusterId, definition.getCluster().getClusterId());
  }

  @Test
  public void testBatchDeleteOfNoticeEntities() throws Exception {
    AlertDefinitionEntity definition = helper.createAlertDefinition(clusterId);

    AlertGroupEntity group = helper.createAlertGroup(clusterId, null);
    group.addAlertDefinition(definition);
    dispatchDao.merge(group);

    // Add 1000+ notice entities
    for (int i = 0; i < 1500; i++) {
      AlertHistoryEntity history = new AlertHistoryEntity();
      history.setServiceName(definition.getServiceName());
      history.setClusterId(clusterId);
      history.setAlertDefinition(definition);
      history.setAlertLabel("Label");
      history.setAlertState(AlertState.OK);
      history.setAlertText("Alert Text");
      history.setAlertTimestamp(calendar.getTimeInMillis());
      alertsDao.create(history);

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertHistory(history);
      current.setLatestTimestamp(new Date().getTime());
      current.setOriginalTimestamp(new Date().getTime() - 10800000);
      current.setMaintenanceState(MaintenanceState.OFF);
      alertsDao.create(current);

      AlertNoticeEntity notice = new AlertNoticeEntity();
      notice.setAlertHistory(history);
      notice.setAlertTarget(helper.createAlertTarget());
      notice.setNotifyState(NotificationState.PENDING);
      notice.setUuid(UUID.randomUUID().toString());
      dispatchDao.create(notice);
    }

    group = dispatchDao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertDefinitions());
    assertEquals(1, group.getAlertDefinitions().size());

    List<AlertHistoryEntity> historyEntities = alertsDao.findAll();
    assertEquals(1500, historyEntities.size());

    List<AlertCurrentEntity> currentEntities = alertsDao.findCurrentByDefinitionId(definition.getDefinitionId());
    assertNotNull(currentEntities);
    assertEquals(1500, currentEntities.size());

    List<AlertNoticeEntity> noticeEntities = dispatchDao.findAllNotices();
    Assert.assertEquals(1500, noticeEntities.size());

    // delete the definition
    definition = dao.findById(definition.getDefinitionId());
    dao.refresh(definition);
    dao.remove(definition);

    List<AlertNoticeEntity> notices = dispatchDao.findAllNotices();
    assertTrue(notices.isEmpty());

    currentEntities = alertsDao.findCurrentByDefinitionId(definition.getDefinitionId());
    assertTrue(currentEntities == null || currentEntities.isEmpty());
    historyEntities = alertsDao.findAll();
    assertTrue(historyEntities == null || historyEntities.isEmpty());

    group = dispatchDao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertDefinitions());
    assertEquals(0, group.getAlertDefinitions().size());
  }
}
