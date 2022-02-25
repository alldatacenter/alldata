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
package org.apache.ambari.server.state.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.listeners.alerts.AlertAggregateListener;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.events.listeners.alerts.AlertStateChangedListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.Reporting.ReportTemplate;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

import junit.framework.Assert;


/**
 * Tests the management of {@link AlertEvent}s in the system.
 */
public class AlertDataManagerTest {

  private static final String ALERT_DEFINITION = "Alert Definition 1";
  private static final String SERVICE = "HDFS";
  private static final String COMPONENT = "DATANODE";
  private static final String HOST1 = "h1";
  private static final String HOST2 = "h2";
  private static final String ALERT_LABEL = "My Label";

  private Injector m_injector;
  private OrmTestHelper m_helper;
  private Clusters m_clusters;
  private Cluster m_cluster;
  private AlertsDAO m_dao;
  private AlertDispatchDAO m_dispatchDao;
  private AlertDefinitionDAO m_definitionDao;

  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    EventBusSynchronizer.synchronizeAlertEventPublisher(m_injector);
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector);
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.getInstance(UnitOfWork.class).begin();

    m_helper = m_injector.getInstance(OrmTestHelper.class);
    m_dao = m_injector.getInstance(AlertsDAO.class);
    m_dispatchDao = m_injector.getInstance(AlertDispatchDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);

    // install YARN so there is at least 1 service installed and no
    // unexpected alerts since the test YARN service doesn't have any alerts
    m_cluster = m_helper.buildNewCluster(m_clusters, m_serviceFactory,
        m_componentFactory, m_schFactory, HOST1);

    m_helper.addHost(m_clusters, m_cluster, HOST2);
    m_helper.addHostComponent(m_cluster, HOST2, SERVICE, COMPONENT);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName(SERVICE);
      definition.setComponentName(COMPONENT);
      definition.setClusterId(m_cluster.getClusterId());
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      definition.setLabel(ALERT_LABEL);
      m_definitionDao.create(definition);
    }
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
    m_injector = null;
  }

  @Test
  public void testAlertRecords() throws AmbariException {
    Alert alert1 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1,
        AlertState.OK);
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("Component component1 is OK");
    alert1.setTimestamp(1L);
    alert1.setClusterId(m_cluster.getClusterId());

    Alert alert2 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST2,
        AlertState.CRITICAL);
    alert2.setLabel(ALERT_LABEL);
    alert2.setText("Component component2 is not OK");
    alert2.setClusterId(m_cluster.getClusterId());


    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);

    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(),
        alert1);

    AlertReceivedEvent event2 = new AlertReceivedEvent(
        m_cluster.getClusterId(),
        alert2);

    listener.onAlertEvent(event1);
    listener.onAlertEvent(event2);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrentByService(
        m_cluster.getClusterId(), SERVICE);
    assertEquals(2, allCurrent.size());

    List<AlertHistoryEntity> allHistory = m_dao.findAll(m_cluster.getClusterId());
    assertEquals(2, allHistory.size());

    AlertCurrentEntity current = m_dao.findCurrentByHostAndName(
        m_cluster.getClusterId(), HOST1, ALERT_DEFINITION);
    assertNotNull(current);
    assertEquals(HOST1, current.getAlertHistory().getHostName());
    assertEquals(ALERT_DEFINITION, current.getAlertHistory().getAlertDefinition().getDefinitionName());
    assertEquals(ALERT_LABEL, current.getAlertHistory().getAlertLabel());
    assertEquals("Component component1 is OK", current.getAlertHistory().getAlertText());
    assertEquals(current.getAlertHistory().getAlertState(), AlertState.OK);
    assertEquals(1L, current.getOriginalTimestamp().longValue());
    assertEquals(1L, current.getLatestTimestamp().longValue());

    Long currentId = current.getAlertId();
    Long historyId = current.getAlertHistory().getAlertId();

    // no new history since the state is the same
    Alert alert3 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1,
        AlertState.OK);
    alert3.setLabel(ALERT_LABEL);
    alert3.setText("Component component1 is OK");
    alert3.setTimestamp(2L);
    alert3.setClusterId(m_cluster.getClusterId());

    AlertReceivedEvent event3 = new AlertReceivedEvent(
        m_cluster.getClusterId(),
        alert3);

    listener.onAlertEvent(event3);

    current = m_dao.findCurrentByHostAndName(m_cluster.getClusterId(), HOST1,
        ALERT_DEFINITION);
    assertNotNull(current);
    assertEquals(currentId, current.getAlertId());
    assertEquals(historyId, current.getAlertHistory().getAlertId());
    assertEquals(HOST1, current.getAlertHistory().getHostName());
    assertEquals(ALERT_DEFINITION, current.getAlertHistory().getAlertDefinition().getDefinitionName());
    assertEquals(ALERT_LABEL, current.getAlertHistory().getAlertLabel());
    assertEquals("Component component1 is OK", current.getAlertHistory().getAlertText());
    assertEquals(current.getAlertHistory().getAlertState(), AlertState.OK);
    assertEquals(1L, current.getOriginalTimestamp().longValue());
    assertEquals(2L, current.getLatestTimestamp().longValue());

    allCurrent = m_dao.findCurrentByService(m_cluster.getClusterId(), SERVICE);
    assertEquals(2, allCurrent.size());

    allHistory = m_dao.findAll(m_cluster.getClusterId());
    assertEquals(2, allHistory.size());

    // change to warning
    Alert alert4 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1,
        AlertState.WARNING);
    alert4.setLabel(ALERT_LABEL);
    alert4.setText("Component component1 is about to go down");
    alert4.setTimestamp(3L);
    alert4.setClusterId(m_cluster.getClusterId());


    AlertReceivedEvent event4 = new AlertReceivedEvent(
        m_cluster.getClusterId(),
        alert4);

    listener.onAlertEvent(event4);

    current = m_dao.findCurrentByHostAndName(m_cluster.getClusterId(), HOST1,
        ALERT_DEFINITION);
    assertNotNull(current);
    assertEquals(current.getAlertId(), currentId);
    assertFalse(historyId.equals(current.getAlertHistory().getAlertId()));
    assertEquals(HOST1, current.getAlertHistory().getHostName());
    assertEquals(ALERT_DEFINITION, current.getAlertHistory().getAlertDefinition().getDefinitionName());
    assertEquals(ALERT_LABEL, current.getAlertHistory().getAlertLabel());
    assertEquals("Component component1 is about to go down", current.getAlertHistory().getAlertText());
    assertEquals(current.getAlertHistory().getAlertState(), AlertState.WARNING);
    assertEquals(3L, current.getOriginalTimestamp().longValue());
    assertEquals(3L, current.getLatestTimestamp().longValue());

    allCurrent = m_dao.findCurrentByService(m_cluster.getClusterId(), SERVICE);
    assertEquals(2, allCurrent.size());

    allHistory = m_dao.findAll(m_cluster.getClusterId());
    assertEquals(3, allHistory.size());
  }

  /**
   * Tests that {@link AlertStateChangeEvent} cause an {@link AlertNoticeEntity}
   * entry.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNotices() throws Exception {
    List<AlertNoticeEntity> notices = m_dispatchDao.findAllNotices();
    assertEquals( 0, notices.size() );

    List<AlertDefinitionEntity> definitions = m_definitionDao.findAll(m_cluster.getClusterId());
    AlertDefinitionEntity definition = definitions.get(0);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(m_cluster.getClusterId());
    history.setAlertDefinition(definition);
    history.setAlertLabel(definition.getDefinitionName());
    history.setAlertText(definition.getDefinitionName());
    history.setAlertTimestamp(System.currentTimeMillis());
    history.setHostName(HOST1);
    history.setAlertState(AlertState.OK);
    m_dao.create(history);

    List<AlertHistoryEntity> histories = m_dao.findAll(m_cluster.getClusterId());
    assertEquals(1, histories.size());

    AlertCurrentEntity currentAlert = new AlertCurrentEntity();
    currentAlert.setAlertHistory(histories.get(0));
    currentAlert.setMaintenanceState(MaintenanceState.OFF);
    currentAlert.setOriginalTimestamp(System.currentTimeMillis());
    currentAlert.setLatestTimestamp(System.currentTimeMillis());
    m_dao.create(currentAlert);

    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(
        m_cluster.getClusterId(), targets);
    group.addAlertDefinition( definitions.get(0) );
    m_dispatchDao.merge(group);

    Alert alert1 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1,
        AlertState.OK);

    AlertStateChangeEvent event = new AlertStateChangeEvent(
        m_cluster.getClusterId(), alert1,
        currentAlert, AlertState.CRITICAL, AlertFirmness.HARD);

    AlertStateChangedListener listener = m_injector.getInstance(AlertStateChangedListener.class);
    listener.onAlertEvent(event);

    notices = m_dispatchDao.findAllNotices();
    assertEquals(1, notices.size());
  }

  @Test
  public void testAggregateAlerts() throws Exception {
    // create definition
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("to_aggregate");
    definition.setLabel("My Label");
    definition.setLabel("My Description");
    definition.setServiceName(SERVICE);
    definition.setComponentName(null);
    definition.setClusterId(m_cluster.getClusterId());
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(Integer.valueOf(60));
    definition.setScope(Scope.HOST);
    definition.setSource("{\"type\" : \"SCRIPT\"}");
    definition.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(definition);

    // create aggregate of definition
    AlertDefinitionEntity aggDef = new AlertDefinitionEntity();
    aggDef.setDefinitionName("aggregate_test");
    aggDef.setServiceName(SERVICE);
    aggDef.setComponentName(null);
    aggDef.setClusterId(m_cluster.getClusterId());
    aggDef.setHash(UUID.randomUUID().toString());
    aggDef.setScheduleInterval(Integer.valueOf(60));
    aggDef.setScope(Scope.SERVICE);

    AggregateSource source = new AggregateSource();
    source.setAlertName("to_aggregate");

    // !!! type is protected
    Field field = Source.class.getDeclaredField("type");
    field.setAccessible(true);
    field.set(source, SourceType.AGGREGATE);

    Reporting reporting = new Reporting();

    ReportTemplate template = new ReportTemplate();
    template.setText("You are good {1}/{0}");
    reporting.setOk(template);

    template = new ReportTemplate();
    template.setText("Going bad {1}/{0}");
    template.setValue(Double.valueOf(0.33d));
    reporting.setWarning(template);

    template = new ReportTemplate();
    template.setText("On fire! {1}/{0}");
    template.setValue(Double.valueOf(0.66d));
    reporting.setCritical(template);

    source.setReporting(reporting);

    Gson gson = new Gson();

    aggDef.setSource(gson.toJson(source));
    aggDef.setSourceType(SourceType.AGGREGATE);
    m_definitionDao.create(aggDef);

    // add current and history across four hosts
    for (int i = 0; i < 4; i++) {
      AlertHistoryEntity history = new AlertHistoryEntity();
      history.setAlertDefinition(definition);
      history.setAlertInstance(null);
      history.setAlertLabel(definition.getLabel());
      history.setAlertState(AlertState.OK);
      history.setAlertText("OK");
      history.setAlertTimestamp(Long.valueOf(1));
      history.setClusterId(m_cluster.getClusterId());
      history.setComponentName(definition.getComponentName());
      history.setHostName("h" + (i+1));
      history.setServiceName(definition.getServiceName());
      m_dao.create(history);

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertHistory(history);
      current.setLatestText(history.getAlertText());
      current.setLatestTimestamp(Long.valueOf(1L));
      current.setOriginalTimestamp(Long.valueOf(1L));
      m_dao.merge(current);
    }

    // !!! need a synchronous op for testing
    AlertEventPublisher publisher = m_injector.getInstance(AlertEventPublisher.class);
    EventBusSynchronizer.synchronizeAlertEventPublisher(m_injector);

    final AtomicReference<Alert> ref = new AtomicReference<>();
    publisher.register(new TestListener() {
      @Override
      @Subscribe
      public void catchIt(AlertReceivedEvent event) {
        ref.set(event.getAlert());
      }
    });

    AlertAggregateListener listener = m_injector.getInstance(AlertAggregateListener.class);
    AlertDefinitionFactory factory = new AlertDefinitionFactory();

    // get the aggregate cache and test it a little bit
    AggregateDefinitionMapping aggregateMapping = m_injector.getInstance(AggregateDefinitionMapping.class);

    AlertDefinition aggregateDefinition = factory.coerce(aggDef);
    aggregateMapping.registerAggregate(m_cluster.getClusterId(),
        aggregateDefinition );

    // make sure the aggregate has the correct associations
    Assert.assertEquals(aggregateDefinition,
        aggregateMapping.getAggregateDefinitions(m_cluster.getClusterId()).get(0));

    Assert.assertEquals(definition.getDefinitionName(),
        aggregateMapping.getAlertsWithAggregates(m_cluster.getClusterId()).get(0));

    AggregateSource as = (AggregateSource) aggregateDefinition.getSource();
    AlertDefinition aggregatedDefinition = aggregateMapping.getAggregateDefinition(
        m_cluster.getClusterId(), as.getAlertName());

    assertNotNull(aggregatedDefinition);

    Alert alert = new Alert(
        definition.getDefinitionName(),
        null,
        definition.getServiceName(),
        definition.getComponentName(),
        "h1",
        AlertState.OK);

    AlertCurrentEntity current = m_dao.findCurrentByHostAndName(
        m_cluster.getClusterId(), "h1", definition.getDefinitionName());

    AlertStateChangeEvent event = new AlertStateChangeEvent(
        m_cluster.getClusterId(), alert, current, AlertState.OK, AlertFirmness.HARD);

    listener.onAlertStateChangeEvent(event);
    assertNotNull(ref.get());
    assertEquals(AlertState.OK, ref.get().getState());
    assertTrue(ref.get().getText().indexOf("0/4") > -1);

    // check if one is critical, still ok
    current.getAlertHistory().setAlertState(AlertState.CRITICAL);
    m_dao.merge(current.getAlertHistory());

    listener.onAlertStateChangeEvent(event);
    assertEquals("aggregate_test", ref.get().getName());
    assertEquals(AlertState.OK, ref.get().getState());
    assertTrue(ref.get().getText().indexOf("1/4") > -1);

    // two are either warning or critical, warning
    current = m_dao.findCurrentByHostAndName(m_cluster.getClusterId(), "h2",
        definition.getDefinitionName());

    current.getAlertHistory().setAlertState(AlertState.WARNING);
    m_dao.merge(current.getAlertHistory());

    listener.onAlertStateChangeEvent(event);
    assertEquals("aggregate_test", ref.get().getName());
    assertEquals(AlertState.WARNING, ref.get().getState());
    assertTrue(ref.get().getText().indexOf("2/4") > -1);

    // three make it critical
    current = m_dao.findCurrentByHostAndName(m_cluster.getClusterId(), "h3",
        definition.getDefinitionName());

    current.getAlertHistory().setAlertState(AlertState.CRITICAL);
    m_dao.merge(current.getAlertHistory());

    listener.onAlertStateChangeEvent(event);
    assertEquals("aggregate_test", ref.get().getName());
    assertEquals(AlertState.CRITICAL, ref.get().getState());
    assertTrue(ref.get().getText().indexOf("3/4") > -1);
  }

  /**
   * Test interface collects aggregate alert invocations
   */
  private interface TestListener {
    void catchIt(AlertReceivedEvent event);
  }
}
