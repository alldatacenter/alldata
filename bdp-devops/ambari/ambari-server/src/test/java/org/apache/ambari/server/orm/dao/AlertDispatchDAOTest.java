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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.AlertNoticeRequest;
import org.apache.ambari.server.controller.internal.AlertNoticeResourceProvider;
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
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.ServiceComponentFactory;
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
 * Tests {@link AlertDispatchDAO}.
 */
public class AlertDispatchDAOTest {

  private final static String HOSTNAME = "c6401.ambari.apache.org";

  private Clusters m_clusters;
  private Cluster m_cluster;
  private Injector m_injector;
  private AlertDispatchDAO m_dao;
  private AlertDefinitionDAO m_definitionDao;
  private AlertsDAO m_alertsDao;
  private OrmTestHelper m_helper;

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


    m_dao = m_injector.getInstance(AlertDispatchDAO.class);
    m_alertsDao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_helper = m_injector.getInstance(OrmTestHelper.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_alertHelper = m_injector.getInstance(AlertDaoHelper.class);


    // !!! need a synchronous op for testing
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector);

    m_cluster = m_clusters.getClusterById(m_helper.createCluster());
    m_helper.initializeClusterWithStack(m_cluster);
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }

  private void initTestData() throws Exception {
    Set<AlertTargetEntity> targets = createTargets(1);

    for (int i = 0; i < 2; i++) {
      AlertGroupEntity group = new AlertGroupEntity();
      group.setDefault(false);
      group.setGroupName("Group Name " + i);
      group.setClusterId(m_cluster.getClusterId());
      for (AlertTargetEntity alertTarget : targets) {
        group.addAlertTarget(alertTarget);
      }

      m_dao.create(group);
    }
  }

  /**
   *
   */
  @Test
  public void testFindTargets() throws Exception {
    initTestData();
    // find all targets
    List<AlertTargetEntity> targets = m_dao.findAllTargets();
    assertNotNull(targets);
    assertEquals(1, targets.size());

    // find by ids
    List<Long> ids = new ArrayList<>();
    ids.add(targets.get(0).getTargetId());
    ids.add(99999L);

    targets = m_dao.findTargetsById(ids);
    assertEquals(1, targets.size());

    //find by name
    AlertTargetEntity target = targets.get(0);

    AlertTargetEntity actual = m_dao.findTargetByName(target.getTargetName());
    assertEquals(target, actual);
  }

  /**
   *
   */
  @Test
  public void testCreateAndFindAllGlobalTargets() throws Exception {
    List<AlertTargetEntity> targets = m_dao.findAllGlobalTargets();
    assertNotNull(targets);
    assertEquals(0, targets.size());

    AlertTargetEntity target  = m_helper.createGlobalAlertTarget();
    m_helper.createGlobalAlertTarget();
    m_helper.createGlobalAlertTarget();

    targets = m_dao.findAllGlobalTargets();
    assertTrue(target.isGlobal());
    assertEquals(3, targets.size());

    m_dao.findTargetByName(target.getTargetName());
    assertTrue( target.isGlobal() );
  }

  /**
   *
   */
  @Test
  public void testFindGroups() throws Exception {
    initTestData();
    // find all
    List<AlertGroupEntity> groups = m_dao.findAllGroups();
    assertNotNull(groups);
    assertEquals(2, groups.size());

    //find by name
    AlertGroupEntity group = groups.get(1);

    AlertGroupEntity actual = m_dao.findGroupByName(group.getClusterId(),
            group.getGroupName());

    assertEquals(group, actual);

    //find by id
    List<Long> ids = new ArrayList<>();
    ids.add(groups.get(0).getGroupId());
    ids.add(groups.get(1).getGroupId());
    ids.add(99999L);

    groups = m_dao.findGroupsById(ids);
    assertEquals(2, groups.size());

    // find default group
    for (AlertGroupEntity alertGroupEntity : groups) {
      assertFalse(alertGroupEntity.isDefault());
    }

    Cluster cluster = m_helper.buildNewCluster(m_clusters, m_serviceFactory,
            m_componentFactory, m_schFactory, HOSTNAME);

    AlertGroupEntity hdfsGroup = m_dao.findDefaultServiceGroup(
            cluster.getClusterId(), "HDFS");

    assertNotNull(hdfsGroup);
    assertTrue(hdfsGroup.isDefault());
  }

  /**
   *
   */
  @Test
  public void testCreateUpdateRemoveGroup() throws Exception {
    // create group
    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), targets);
    AlertGroupEntity actual = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    assertEquals(group.getGroupName(), actual.getGroupName());
    assertEquals(group.isDefault(), actual.isDefault());
    assertEquals(group.getAlertTargets(), actual.getAlertTargets());
    assertEquals(group.getAlertDefinitions(), actual.getAlertDefinitions());

    // update group
    AlertGroupEntity group1 = m_helper.createAlertGroup(
            m_cluster.getClusterId(), null);

    String groupName = group1.getGroupName();

    group1 = m_dao.findGroupById(group1.getGroupId());
    group1.setGroupName(groupName + "FOO");
    group1.setDefault(true);

    m_dao.merge(group1);
    group = m_dao.findGroupById(group1.getGroupId());

    assertEquals(groupName + "FOO", group1.getGroupName());
    assertEquals(true, group1.isDefault());
    assertEquals(0, group1.getAlertDefinitions().size());
    assertEquals(0, group1.getAlertTargets().size());

    group1.addAlertTarget(target);
    m_dao.merge(group);

    group1 = m_dao.findGroupById(group1.getGroupId());
    assertEquals(targets, group1.getAlertTargets());

    // delete group
    m_dao.remove(group);
    group = m_dao.findGroupById(group.getGroupId());
    assertNull(group);

    target = m_dao.findTargetById(target.getTargetId());
    assertNotNull(target);
    assertEquals(1, m_dao.findAllTargets().size());
  }

  /**
   *
   */
  @Test
  public void testCreateAndRemoveTarget() throws Exception {
    // create target
    int targetCount = m_dao.findAllTargets().size();

    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), targets);

    AlertTargetEntity actual = m_dao.findTargetById(target.getTargetId());
    assertNotNull(actual);

    assertEquals(target.getTargetName(), actual.getTargetName());
    assertEquals(target.getDescription(), actual.getDescription());
    assertEquals(target.getNotificationType(), actual.getNotificationType());
    assertEquals(target.getProperties(), actual.getProperties());
    assertEquals(false, actual.isGlobal());

    assertNotNull(actual.getAlertGroups());
    Iterator<AlertGroupEntity> iterator = actual.getAlertGroups().iterator();
    AlertGroupEntity actualGroup = iterator.next();

    assertEquals(group, actualGroup);

    assertEquals(targetCount + 1, m_dao.findAllTargets().size());

    // remove target
    m_dao.remove(target);

    target = m_dao.findTargetById(target.getTargetId());
    assertNull(target);

  }

  /**
   *
   */
  @Test
  public void testGlobalTargetAssociations() throws Exception {
    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertEquals(0, group.getAlertTargets().size());

    AlertTargetEntity target = m_helper.createGlobalAlertTarget();
    assertTrue(target.isGlobal());

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertEquals(1, group.getAlertTargets().size());

    List<AlertGroupEntity> groups = m_dao.findAllGroups();
    target = m_dao.findTargetById(target.getTargetId());
    assertEquals(groups.size(), target.getAlertGroups().size());

    m_dao.remove(target);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertEquals(0, group.getAlertTargets().size());
  }

  /**
   * Tests that a newly created group is correctly associated with all global
   * targets.
   */
  @Test
  public void testGlobalTargetAssociatedWithNewGroup() throws Exception {
    AlertTargetEntity target1 = m_helper.createGlobalAlertTarget();
    AlertTargetEntity target2 = m_helper.createGlobalAlertTarget();
    assertTrue(target1.isGlobal());
    assertTrue(target2.isGlobal());

    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertEquals(2, group.getAlertTargets().size());

    Iterator<AlertTargetEntity> iterator = group.getAlertTargets().iterator();
    AlertTargetEntity groupTarget1 = iterator.next();
    AlertTargetEntity groupTarget2 = iterator.next();

    assertTrue(groupTarget1.isGlobal());
    assertTrue(groupTarget2.isGlobal());
  }

  /**
  *
  */
  @Test
  public void testDeleteTargetWithNotices() throws Exception {
    AlertTargetEntity target = m_helper.createAlertTarget();

    List<AlertDefinitionEntity> definitions = createDefinitions();
    AlertDefinitionEntity definition = definitions.get(0);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(m_cluster.getClusterId());
    history.setAlertDefinition(definition);
    history.setAlertLabel("Label");
    history.setAlertState(AlertState.OK);
    history.setAlertText("Alert Text");
    history.setAlertTimestamp(System.currentTimeMillis());
    m_alertsDao.create(history);

    AlertNoticeEntity notice = new AlertNoticeEntity();
    notice.setUuid(UUID.randomUUID().toString());
    notice.setAlertTarget(target);
    notice.setAlertHistory(history);
    notice.setNotifyState(NotificationState.PENDING);
    m_dao.create(notice);

    notice = m_dao.findNoticeById(notice.getNotificationId());
    assertEquals(target.getTargetId(), notice.getAlertTarget().getTargetId());

    //new org.apache.derby.tools.dblook(new String[]{"-d", Configuration.JDBC_IN_MEMORY_URL, "-verbose", "-o", "/tmp/1.ddl"});

    target = m_dao.findTargetById(target.getTargetId());
    m_dao.remove(target);
    notice = m_dao.findNoticeById(notice.getNotificationId());
    assertNull(notice);
  }

  /**
   *
   */
  @Test
  public void testDeleteAssociatedTarget() throws Exception {
    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), targets);
    assertEquals(1, group.getAlertTargets().size());

    target = m_dao.findTargetById(target.getTargetId());
    m_dao.refresh(target);

    assertNotNull(target);
    assertEquals(1, target.getAlertGroups().size());

    m_dao.remove(target);
    target = m_dao.findTargetById(target.getTargetId());
    assertNull(target);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    assertEquals(0, group.getAlertTargets().size());
  }

  /**
   * Tests finding groups by a definition ID that they are associatd with.
   *
   * @throws Exception
   */
  @Test
  public void testFindGroupsByDefinition() throws Exception {
    List<AlertDefinitionEntity> definitions = createDefinitions();
    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    for (AlertDefinitionEntity definition : definitions) {
      group.addAlertDefinition(definition);
    }

    m_dao.merge(group);

    group = m_dao.findGroupByName(m_cluster.getClusterId(), group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    // assert that the definition is now part of 2 groups (the default group
    // and the newly associated group from above)
    for (AlertDefinitionEntity definition : definitions) {
      List<AlertGroupEntity> groups = m_dao.findGroupsByDefinition(definition);
      assertEquals(2, groups.size());
    }
  }

  /**
   * Tests finding groups by a definition ID that they are associatd with in
   * order to get any targets associated with that group. This exercises the
   * bi-directional
   *
   * @throws Exception
   */
  @Test
  public void testFindTargetsViaGroupsByDefinition() throws Exception {
    initTestData();
    List<AlertDefinitionEntity> definitions = createDefinitions();
    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    AlertDefinitionEntity definition = definitions.get(0);
    group.addAlertDefinition(definition);

    m_dao.merge(group);

    List<AlertTargetEntity> targets = m_dao.findAllTargets();
    AlertTargetEntity target = targets.get(0);
    Set<AlertTargetEntity> setTargets = Collections.singleton(target);

    group.setAlertTargets(setTargets);
    m_dao.merge(group);

    List<AlertGroupEntity> groups = m_dao.findGroupsByDefinition(definition);
    assertEquals(2, groups.size());

    group = groups.get(groups.indexOf(group));
    assertEquals(1, group.getAlertTargets().size());
    assertEquals(target.getTargetId(),
        group.getAlertTargets().iterator().next().getTargetId());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindNoticeByUuid() throws Exception {
    List<AlertDefinitionEntity> definitions = createDefinitions();
    AlertDefinitionEntity definition = definitions.get(0);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(m_cluster.getClusterId());
    history.setAlertDefinition(definition);
    history.setAlertLabel("Label");
    history.setAlertState(AlertState.OK);
    history.setAlertText("Alert Text");
    history.setAlertTimestamp(System.currentTimeMillis());
    m_alertsDao.create(history);

    AlertTargetEntity target = m_helper.createAlertTarget();

    AlertNoticeEntity notice = new AlertNoticeEntity();
    notice.setUuid(UUID.randomUUID().toString());
    notice.setAlertTarget(target);
    notice.setAlertHistory(history);
    notice.setNotifyState(NotificationState.PENDING);
    m_dao.create(notice);

    AlertNoticeEntity actual = m_dao.findNoticeByUuid(notice.getUuid());
    assertEquals(notice.getNotificationId(), actual.getNotificationId());
    assertNull(m_dao.findNoticeByUuid("DEADBEEF"));
  }


  /**
   * Tests that the Ambari {@link Predicate} can be converted and submitted to
   * JPA correctly to return a restricted result set.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticePredicate() throws Exception {
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);
    m_helper.installHdfsService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);
    m_helper.installYarnService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);

    m_alertHelper.populateData(m_cluster);

    Predicate clusterPredicate = null;
    Predicate hdfsPredicate = null;
    Predicate yarnPredicate = null;
    Predicate adminPredicate = null;
    Predicate adminOrOperatorPredicate = null;
    Predicate pendingPredicate = null;
    Predicate noticeIdPredicate = null;

    clusterPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").toPredicate();

    hdfsPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME).equals("HDFS").toPredicate();

    yarnPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME).equals("YARN").toPredicate();

    adminPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME).equals(
        "Administrators").toPredicate();

    adminOrOperatorPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME).equals(
        "Administrators").or().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME).equals(
        "Operators").toPredicate();

    pendingPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_STATE).equals(
        NotificationState.PENDING.name()).toPredicate();

    AlertNoticeRequest request = new AlertNoticeRequest();
    request.Predicate = clusterPredicate;

    List<AlertNoticeEntity> notices = m_dao.findAllNotices(request);
    assertEquals(3, notices.size());

    request.Predicate = hdfsPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(2, notices.size());

    request.Predicate = yarnPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    request.Predicate = adminPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(2, notices.size());

    request.Predicate = adminOrOperatorPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(3, notices.size());

    request.Predicate = pendingPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    noticeIdPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_ID).equals(
        notices.get(0).getNotificationId()).toPredicate();

    request.Predicate = noticeIdPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());
  }

  /**
   * Tests that JPA does the pagination work for us.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticePagination() throws Exception {
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);
    m_helper.installHdfsService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);
    m_helper.installYarnService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);

    m_alertHelper.populateData(m_cluster);

    AlertNoticeRequest request = new AlertNoticeRequest();
    request.Pagination = null;

    // get back all 3
    List<AlertNoticeEntity> notices = m_dao.findAllNotices(request);
    assertEquals(3, notices.size());

    // only the first 2
    request.Pagination = new PageRequestImpl(StartingPoint.Beginning, 2, 0,
        null, null);

    notices = m_dao.findAllNotices(request);
    assertEquals(2, notices.size());

    // the 2nd and 3rd
    request.Pagination = new PageRequestImpl(StartingPoint.Beginning, 1, 2,
        null, null);

    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    // none b/c we're out of index
    request.Pagination = new PageRequestImpl(StartingPoint.Beginning, 1, 3,
        null, null);

    notices = m_dao.findAllNotices(request);
    assertEquals(0, notices.size());
  }

  /**
   * Tests that JPA does the sorting work for us.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticeSorting() throws Exception {
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);
    m_helper.installHdfsService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);
    m_helper.installYarnService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);

    m_alertHelper.populateData(m_cluster);

    List<SortRequestProperty> sortProperties = new ArrayList<>();
    SortRequest sortRequest = new SortRequestImpl(sortProperties);

    AlertNoticeRequest request = new AlertNoticeRequest();
    request.Sort = sortRequest;

    Predicate clusterPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").toPredicate();

    request.Predicate = clusterPredicate;

    sortProperties.add(new SortRequestProperty(
        AlertNoticeResourceProvider.ALERT_NOTICE_ID, Order.ASC));

    // get back all 3
    List<AlertNoticeEntity> notices = m_dao.findAllNotices(request);
    assertEquals(3, notices.size());

    // assert sorting ASC
    long lastId = 0L;
    for (AlertNoticeEntity notice : notices) {
      if (lastId == 0L) {
        lastId = notice.getNotificationId();
        continue;
      }

      long currentId = notice.getNotificationId();
      assertTrue(lastId < currentId);
      lastId = currentId;
    }

    // clear and do DESC
    sortProperties.clear();
    sortProperties.add(new SortRequestProperty(
        AlertNoticeResourceProvider.ALERT_NOTICE_ID, Order.DESC));

    notices = m_dao.findAllNotices(request);
    assertEquals(3, notices.size());

    // assert sorting DESC
    lastId = 0L;
    for (AlertNoticeEntity notice : notices) {
      if (lastId == 0L) {
        lastId = notice.getNotificationId();
        continue;
      }

      long currentId = notice.getNotificationId();
      assertTrue(lastId > currentId);
      lastId = currentId;
    }
  }

  /**
   * Tests that when creating a new {@link AlertDefinitionEntity}, if the group
   * for its service does not exist, then it will be created.
   */
  @Test
  public void testDefaultGroupAutomaticCreation() throws Exception {
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);
    m_helper.installHdfsService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);
    //m_helper.installYarnService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);

    AlertGroupEntity hdfsGroup = m_dao.findDefaultServiceGroup(
            m_cluster.getClusterId(), "HDFS");

    // remove the HDFS default group
    m_dao.remove(hdfsGroup);
    hdfsGroup = m_dao.findDefaultServiceGroup(m_cluster.getClusterId(), "HDFS");
    assertNull(hdfsGroup);

    AlertDefinitionEntity datanodeProcess = new AlertDefinitionEntity();
    datanodeProcess.setClusterId(m_cluster.getClusterId());
    datanodeProcess.setDefinitionName("datanode_process");
    datanodeProcess.setServiceName("HDFS");
    datanodeProcess.setComponentName("DATANODE");
    datanodeProcess.setHash(UUID.randomUUID().toString());
    datanodeProcess.setScheduleInterval(60);
    datanodeProcess.setScope(Scope.SERVICE);
    datanodeProcess.setSource("{\"type\" : \"SCRIPT\"}");
    datanodeProcess.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(datanodeProcess);

    // the group should be created and should be default
    hdfsGroup = m_dao.findDefaultServiceGroup(m_cluster.getClusterId(), "HDFS");
    assertNotNull(hdfsGroup);
    assertTrue(hdfsGroup.isDefault());
  }

  /**
   * Tests that when creating a new {@link AlertDefinitionEntity}, if the group
   * for its service does not exist, then it will not be created if the service
   * is invalid.
   */
  @Test(expected = AmbariException.class)
  public void testDefaultGroupInvalidServiceNoCreation() throws Exception {
    initTestData();
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);
    m_helper.installHdfsService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);
    //m_helper.installYarnService(m_cluster, m_serviceFactory, m_componentFactory, m_schFactory, HOSTNAME);

    assertEquals(3, m_dao.findAllGroups().size());

    // create a definition with an invalid service
    AlertDefinitionEntity datanodeProcess = new AlertDefinitionEntity();
    datanodeProcess.setClusterId(m_cluster.getClusterId());
    datanodeProcess.setDefinitionName("datanode_process");
    datanodeProcess.setServiceName("INVALID");
    datanodeProcess.setComponentName("DATANODE");
    datanodeProcess.setHash(UUID.randomUUID().toString());
    datanodeProcess.setScheduleInterval(60);
    datanodeProcess.setScope(Scope.SERVICE);
    datanodeProcess.setSource("{\"type\" : \"SCRIPT\"}");
    datanodeProcess.setSourceType(SourceType.SCRIPT);

    try {
      m_definitionDao.create(datanodeProcess);
    } finally {
      // assert no group was added
      assertEquals(3, m_dao.findAllGroups().size());
    }
  }

  /**
   * @return
   */
  private List<AlertDefinitionEntity> createDefinitions() throws Exception {
    // add a host to the cluster
    m_helper.addHost(m_clusters, m_cluster, HOSTNAME);

    // install YARN (which doesn't have any alerts defined in the test JSON)
    // so that the definitions get created correctly
    m_helper.installYarnService(m_cluster, m_serviceFactory,
        m_componentFactory, m_schFactory, HOSTNAME);

    List<AlertDefinitionEntity> alertDefinitionEntities = new ArrayList<>();

    for (int i = 0; i < 2; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("YARN");
      definition.setComponentName(null);
      definition.setClusterId(m_cluster.getClusterId());
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      m_definitionDao.create(definition);
      alertDefinitionEntities.add(definition);
    }

    return alertDefinitionEntities;
  }

  /**
   * @return
   * @throws Exception
   */
  private Set<AlertTargetEntity> createTargets(int numberOfTargets) throws Exception {
    Set<AlertTargetEntity> targets = new HashSet<>();
    for (int i = 0; i < numberOfTargets; i++) {
      AlertTargetEntity target = new AlertTargetEntity();
      target.setDescription("Target Description " + i);
      target.setNotificationType("EMAIL");
      target.setProperties("Target Properties " + i);
      target.setTargetName("Target Name " + i);
      m_dao.create(target);
      targets.add(target);
    }

    return targets;
  }

  /**
   *
   */
  @Test
  public void testGroupDefinitions() throws Exception {
    List<AlertDefinitionEntity> definitions = createDefinitions();

    AlertGroupEntity group = m_helper.createAlertGroup(
            m_cluster.getClusterId(), null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    for (AlertDefinitionEntity definition : definitions) {
      group.addAlertDefinition(definition);
    }

    m_dao.merge(group);

    group = m_dao.findGroupByName(m_cluster.getClusterId(), group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    for (AlertDefinitionEntity definition : definitions) {
      assertTrue(group.getAlertDefinitions().contains(definition));
    }

    m_definitionDao.refresh(definitions.get(0));
    m_definitionDao.remove(definitions.get(0));
    definitions.remove(0);

    group = m_dao.findGroupByName(m_cluster.getClusterId(), group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    for (AlertDefinitionEntity definition : definitions) {
      assertTrue(group.getAlertDefinitions().contains(definition));
    }
  }

  /**
   * Tests that updating JPA associations concurrently doesn't lead to Concu
   *
   * @throws Exception
   */
  @Test
  public void testConcurrentGroupModification() throws Exception {
    createDefinitions();

    AlertGroupEntity group = m_helper.createAlertGroup(m_cluster.getClusterId(), null);
    final Set<AlertTargetEntity> targets = createTargets(100);

    group.setAlertTargets(targets);
    group = m_dao.merge(group);

    final class AlertGroupWriterThread extends Thread {
      private AlertGroupEntity group;

      /**
       * {@inheritDoc}
       */
      @Override
      public void run() {
        for (int i = 0; i < 1000; i++) {
          group.setAlertTargets(new HashSet<>(targets));
        }
      }
    }

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      AlertGroupWriterThread thread = new AlertGroupWriterThread();
      threads.add(thread);

      thread.group = group;
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }
}
