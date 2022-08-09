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
package org.apache.ambari.server.state.alerts;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

/**
 * Tests the {@link AlertReceivedListener}.
 */
@Category({ category.AlertTest.class})
public class AlertReceivedListenerTest {

  private static final String ALERT_DEFINITION = "alert_definition_";
  private static final String AMBARI_ALERT_DEFINITION = "ambari_server_alert";
  private static final String HOST1 = "h1";
  private static final String ALERT_LABEL = "My Label";
  private Injector m_injector;
  private AlertsDAO m_dao;
  private AlertDefinitionDAO m_definitionDao;

  private Clusters m_clusters;
  private Cluster m_cluster;

  private OrmTestHelper m_helper;
  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.getInstance(UnitOfWork.class).begin();

    m_helper = m_injector.getInstance(OrmTestHelper.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);

    m_dao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);

    EventBusSynchronizer.synchronizeAlertEventPublisher(m_injector);
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector);

    // install YARN so there is at least 1 service installed and no
    // unexpected alerts since the test YARN service doesn't have any alerts
    m_cluster = m_helper.buildNewCluster(m_clusters, m_serviceFactory, m_componentFactory,
        m_schFactory, HOST1);

    // create 5 definitions, some with HDFS and some with YARN
    for (int i = 0; i < 5; i++) {
      String serviceName = "HDFS";
      String componentName = "DATANODE";
      if (i >= 3) {
        serviceName = "YARN";
        componentName = "RESOURCEMANAGER";
      }

      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName(ALERT_DEFINITION + i);
      definition.setServiceName(serviceName);
      definition.setComponentName(componentName);
      definition.setClusterId(m_cluster.getClusterId());
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      m_definitionDao.create(definition);
    }

    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName(AMBARI_ALERT_DEFINITION);
    definition.setServiceName(RootService.AMBARI.name());
    definition.setComponentName(RootComponent.AMBARI_SERVER.name());
    definition.setClusterId(m_cluster.getClusterId());
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(Integer.valueOf(60));
    definition.setScope(Scope.SERVICE);
    definition.setSource("{\"type\" : \"SCRIPT\"}");
    definition.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(definition);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
    m_injector = null;
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testDisabledAlert() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert1 = new Alert(definitionName, null, "HDFS", componentName,
        HOST1, AlertState.OK);

    alert1.setClusterId(m_cluster.getClusterId());
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("HDFS " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // disable definition
    AlertDefinitionEntity definition = m_definitionDao.findByName(
        m_cluster.getClusterId(), definitionName);
    definition.setEnabled(false);
    m_definitionDao.merge(definition);

    // remove disabled
    m_dao.removeCurrentDisabledAlerts();
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests an invalid host is being reported in an alert.
   */
  @Test
  public void testInvalidHost() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert = new Alert(definitionName, null, "HDFS", componentName,
        HOST1, AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText("HDFS " + componentName + " is OK");
    alert.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert.setHostName("INVALID");

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testInvalidAlertDefinition() throws AmbariException {
    String componentName = "DATANODE";

    Alert alert = new Alert("missing_alert_definition_name", null, "HDFS",
        componentName, HOST1, AlertState.OK);

    alert.setLabel(ALERT_LABEL);
    alert.setText("HDFS " + componentName + " is OK");
    alert.setTimestamp(1L);

    // bad alert definition name means no current alerts
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests an invalid pairing of component to host.
   */
  @Test
  public void testInvalidServiceComponentHost() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert = new Alert(definitionName, null, "HDFS", componentName,
        HOST1, AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText("HDFS " + componentName + " is OK");
    alert.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert.setHostName("invalid_host_name");

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testMaintenanceModeSet() throws Exception {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert1 = new Alert(definitionName, null, "HDFS", componentName, HOST1,
        AlertState.CRITICAL);

    alert1.setClusterId(m_cluster.getClusterId());
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("HDFS " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    AlertCurrentEntity current = allCurrent.get(0);
    assertEquals(MaintenanceState.OFF, current.getMaintenanceState());

    // remove it
    m_dao.removeCurrentByService(m_cluster.getClusterId(), "HDFS");
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // set maintenance mode on the service
    m_cluster.getService("HDFS").setMaintenanceState(MaintenanceState.ON);

    // verify that the listener handles the event and creates the current alert
    // with the correct MM
    listener.onAlertEvent(event);

    allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    current = allCurrent.get(0);
    assertEquals(MaintenanceState.ON, current.getMaintenanceState());
  }

  /**
   * Tests that an invalid host from a host-level agent alert is rejected.
   */
  @Test
  public void testAgentAlertFromInvalidHost() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = RootService.AMBARI.name();
    String componentName = RootComponent.AMBARI_AGENT.name();

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1,
        AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(serviceName + " " + componentName + " is OK");
    alert.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert.setHostName("INVALID");

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts received
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that an alert for AMBARI/AMBARI_SERVER is always valid.
   */
  @Test
  public void testAmbariServerValidAlerts() throws AmbariException {
    String definitionName = AMBARI_ALERT_DEFINITION;
    String serviceName = RootService.AMBARI.name();
    String componentName = RootComponent.AMBARI_SERVER.name();

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1,
        AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(serviceName + " " + componentName + " is OK");
    alert.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host, invalid cluster
    alert.setHostName("INVALID");
    alert.setClusterId(null);

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify that the alert was still received
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());
  }

  /**
   * Tests that an invalid host from an invalid cluster does not trigger an
   * alert.
   */
  @Test
  public void testMissingClusterAndInvalidHost() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = RootService.AMBARI.name();
    String componentName = RootComponent.AMBARI_AGENT.name();

    Alert alert1 = new Alert(definitionName, null, serviceName, componentName, HOST1,
        AlertState.OK);

    alert1.setClusterId(m_cluster.getClusterId());
    alert1.setLabel(ALERT_LABEL);
    alert1.setText(serviceName + " " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // missing cluster, invalid host
    alert1.setClusterId(null);
    alert1.setHostName("INVALID");

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts received
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that receiving and alert with {@link AlertState#SKIPPED} does not create an entry
   * if there is currently no current alert.
   */
  @Test
  public void testSkippedAlertWithNoCurrentAlert() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1, AlertState.SKIPPED);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(serviceName + " " + componentName + " is OK");
    alert.setTimestamp(1L);

    // fire the alert, and check that nothing gets created
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that receiving and alert with {@link AlertState#SKIPPED} does not
   * create an entry if there is currently no current alert.
   */
  @Test
  public void testSkippedAlertUpdatesTimestampAndText() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";
    String text = serviceName + " " + componentName + " is OK";

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1, AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(text);
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created with the right
    // timestamp
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // check timestamp
    assertEquals(1L, (long) allCurrent.get(0).getOriginalTimestamp());
    assertEquals(1L, (long) allCurrent.get(0).getLatestTimestamp());

    // update the timestamp and the state
    alert.setState(AlertState.SKIPPED);
    alert.setTimestamp(2L);

    // we should allow updating the text if the text is provided
    text = text + " Updated";
    alert.setText(text);

    // get the current make sure the fields were updated
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1L, (long) allCurrent.get(0).getOriginalTimestamp());
    assertEquals(2L, (long) allCurrent.get(0).getLatestTimestamp());
    assertEquals(text, allCurrent.get(0).getLatestText());

    // verify that blank text does not update
    alert.setText("");
    alert.setTimestamp(3L);

    // get the current make sure the text was not updated
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1L, (long) allCurrent.get(0).getOriginalTimestamp());
    assertEquals(3L, (long) allCurrent.get(0).getLatestTimestamp());
    assertEquals(text, allCurrent.get(0).getLatestText());
  }

  /**
   * Tests that we correctly record alert occurance information.
   */
  @Test
  public void testAlertOccurrences() throws AmbariException {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";
    String text = serviceName + " " + componentName + " is OK";

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1, AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(text);
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created with the right
    // timestamp
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // check occurrences (should be 1 since it's the first)
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());

    // send OK again, then check that the value incremented
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(2, (long) allCurrent.get(0).getOccurrences());

    // now change to WARNING and check that it reset the counter
    alert.setState(AlertState.WARNING);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());

    // send another WARNING
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(2, (long) allCurrent.get(0).getOccurrences());

    // now change from WARNING to CRITICAL; because they are both non-OK states,
    // the counter should continue
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(3, (long) allCurrent.get(0).getOccurrences());
  }

  /**
   * Tests that we correctly record alert firmness depending on several factors,
   * such as {@link AlertState} and {@link SourceType}.
   */
  @Test
  public void testAlertFirmness() throws Exception {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";
    String text = serviceName + " " + componentName + " is OK";

    // start out with a critical alert to verify that all new alerts are always
    // HARD
    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1,
        AlertState.CRITICAL);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(text);
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created with the right
    // timestamp
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // check occurrences (should be 1 since it's the first)
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());

    // check that the state is HARD since it's the first alert
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // move the repeat tolerance to 2 to test out SOFT alerts
    AlertDefinitionEntity definition = allCurrent.get(0).getAlertHistory().getAlertDefinition();
    definition.setRepeatTolerance(2);
    definition.setRepeatToleranceEnabled(true);

    m_definitionDao.merge(definition);

    // change state to OK, and ensure that all OK alerts are hard
    alert.setState(AlertState.OK);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // change state to CRITICAL and verify we are soft with 1 occurrence
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.SOFT, allCurrent.get(0).getFirmness());

    // send a 2nd CRITICAL and made sure the occurrences are 2 and the firmness
    // is HARD
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(2, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());
  }

  /**
   * Tests that we correctly record alert firmness when an alert moves back and
   * forth between non-OK states (such as between {@link AlertState#WARNING} and
   * {@link AlertState#CRITICAL}). These are technically alert state changes and
   * will fire {@link AlertStateChangeEvent}s but we only want to handle them
   * when they are HARD.
   */
  @Test
  public void testAlertFirmnessWithinNonOKStates() throws Exception {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";
    String text = serviceName + " " + componentName + " is OK";

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1, AlertState.OK);
    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(text);
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created correctly
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // check occurrences (should be 1 since it's the first) and state (HARD)
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // move the repeat tolerance to 4 to test out SOFT alerts between states
    AlertDefinitionEntity definition = allCurrent.get(0).getAlertHistory().getAlertDefinition();
    definition.setRepeatTolerance(4);
    definition.setRepeatToleranceEnabled(true);
    m_definitionDao.merge(definition);

    // change state to WARNING, should still be SOFT
    alert.setState(AlertState.WARNING);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.SOFT, allCurrent.get(0).getFirmness());
    assertEquals(AlertState.WARNING, allCurrent.get(0).getAlertHistory().getAlertState());

    // change state to CRITICAL, should still be SOFT, but occurrences of non-OK
    // increases to 2
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(2, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.SOFT, allCurrent.get(0).getFirmness());
    assertEquals(AlertState.CRITICAL, allCurrent.get(0).getAlertHistory().getAlertState());

    // change state to WARNING, should still be SOFT, but occurrences of non-OK
    // increases to 3
    alert.setState(AlertState.WARNING);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(3, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.SOFT, allCurrent.get(0).getFirmness());
    assertEquals(AlertState.WARNING, allCurrent.get(0).getAlertHistory().getAlertState());

    // change state to CRITICAL, occurrences is not met, should be HARD
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(4, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());
    assertEquals(AlertState.CRITICAL, allCurrent.get(0).getAlertHistory().getAlertState());
  }

  /**
   * Tests that {@link SourceType#AGGREGATE} alerts are always HARD.
   */
  @Test
  public void testAggregateAlertFirmness() throws Exception {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("aggregate-alert-firmness-test");
    definition.setServiceName("HDFS");
    definition.setComponentName("NAMENODE");
    definition.setClusterId(m_cluster.getClusterId());
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(Integer.valueOf(60));
    definition.setScope(Scope.SERVICE);
    definition.setSource("{\"type\" : \"AGGREGATE\"}");
    definition.setSourceType(SourceType.AGGREGATE);

    // turn this up way high to ensure that we correctly short-circuit these
    // types of alerts and always consider them HARD
    definition.setRepeatTolerance(100);
    definition.setRepeatToleranceEnabled(true);

    m_definitionDao.create(definition);

    Alert alert = new Alert(definition.getDefinitionName(), null, definition.getServiceName(),
        definition.getComponentName(), HOST1, AlertState.OK);

    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText("Aggregate alerts are always HARD");
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // change state
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // change state
    alert.setState(AlertState.WARNING);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(2, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // change state
    alert.setState(AlertState.OK);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());
  }

  /**
   * Tests that we correctly record alert firmness, using the global value if
   * the definition does not override it.
   */
  @Test
  public void testAlertFirmnessUsingGlobalValue() throws Exception {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";
    String text = serviceName + " " + componentName + " is OK";

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1, AlertState.OK);
    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(text);
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // check occurrences (should be 1 since it's the first)
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // move the repeat tolerance to 2 on the definition, but leave it disabled
    // so that we still use the global
    AlertDefinitionEntity definition = allCurrent.get(0).getAlertHistory().getAlertDefinition();
    definition.setRepeatTolerance(2);
    definition.setRepeatToleranceEnabled(false);
    m_definitionDao.merge(definition);

    // change state to CRITICAL; this should make a HARD alert since the global
    // value is in use
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());
  }

  /**
   * Tests that we correctly record alert firmness, using the global value if
   * the definition does not override it.
   */
  @Test
  @SuppressWarnings("serial")
  public void testAlertFirmnessUsingGlobalValueHigherThanOverride() throws Exception {
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(m_cluster, ConfigHelper.CLUSTER_ENV, "version2",
        new HashMap<String, String>() {
          {
            put(ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, "3");
          }
        }, new HashMap<>());

    m_cluster.addDesiredConfig("user", Collections.singleton(config));

    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "HDFS";
    String componentName = "NAMENODE";
    String text = serviceName + " " + componentName + " is OK";

    Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1, AlertState.OK);
    alert.setClusterId(m_cluster.getClusterId());
    alert.setLabel(ALERT_LABEL);
    alert.setText(text);
    alert.setTimestamp(1L);

    // fire the alert, and check that the new entry was created
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
    listener.onAlertEvent(event);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // check occurrences (should be 1 since it's the first)
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());

    // change state to CRITICAL; this should make a SOFT alert since the global
    // value is 3
    alert.setState(AlertState.CRITICAL);
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(1, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.SOFT, allCurrent.get(0).getFirmness());

    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(2, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.SOFT, allCurrent.get(0).getFirmness());

    // on the 3rd time, we transition to HARD
    listener.onAlertEvent(event);
    allCurrent = m_dao.findCurrent();
    assertEquals(3, (long) allCurrent.get(0).getOccurrences());
    assertEquals(AlertFirmness.HARD, allCurrent.get(0).getFirmness());
  }

  /**
   * Tests that multiple threads can't create duplicate new alerts. This will
   * spawn several threads, each one trying to create the same alert.
   */
  @Test
  public void testMultipleNewAlertEvents() throws Exception {
    assertEquals(0, m_dao.findCurrent().size());

    final String definitionName = ALERT_DEFINITION + "1";
    List<Thread> threads = new ArrayList<>();
    final AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);

    // spawn a bunch of concurrent threasd which will try to create teh same
    // alert over and over
    for (int i = 0; i < 10; i++) {
      Thread thread = new Thread() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
          Alert alert = new Alert(definitionName, null, "HDFS", null, HOST1, AlertState.OK);
          alert.setClusterId(m_cluster.getClusterId());
          alert.setLabel(ALERT_LABEL);
          alert.setText("HDFS is OK ");
          alert.setTimestamp(System.currentTimeMillis());

          final AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(), alert);
          try {
            listener.onAlertEvent(event);
          } catch (AmbariException e) {
            e.printStackTrace();
          }
        }
      };

      threads.add(thread);
      thread.start();
    }

    // wait for threads
    for (Thread thread : threads) {
      thread.join();
    }

    assertEquals(1, m_dao.findCurrent().size());
  }
}
