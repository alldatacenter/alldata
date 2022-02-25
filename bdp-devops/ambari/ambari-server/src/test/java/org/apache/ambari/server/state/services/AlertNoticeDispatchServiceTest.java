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
package org.apache.ambari.server.state.services;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executor;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.notifications.dispatchers.AmbariSNMPDispatcher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.alert.TargetType;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests the {@link AlertNoticeDispatchService}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AmbariSNMPDispatcher.class, ManagementFactory.class })
public class AlertNoticeDispatchServiceTest extends AlertNoticeDispatchService {

  final static String ALERT_NOTICE_UUID_1 = UUID.randomUUID().toString();
  final static String ALERT_NOTICE_UUID_2 = UUID.randomUUID().toString();
  final static String ALERT_UNIQUE_TEXT = "0eeda438-2b13-4869-a416-137e35ff76e9";
  final static String HOSTNAME = "c6401.ambari.apache.org";
  final static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  private AmbariMetaInfo m_metaInfo = null;
  private DispatchFactory m_dispatchFactory = null;
  private AlertDispatchDAO m_dao = null;
  private Injector m_injector;
  private RuntimeMXBean m_runtimeMXBean;

  List<AlertDefinitionEntity> m_definitions = new ArrayList<>();
  List<AlertHistoryEntity> m_histories = new ArrayList<>();

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);
    m_dispatchFactory = createStrictMock(DispatchFactory.class);
    m_metaInfo = createNiceMock(AmbariMetaInfo.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(new MockModule());

    Assert.assertNotNull(m_injector);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("Service " + i);
      definition.setComponentName(null);
      definition.setClusterId(1L);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);

      m_definitions.add(definition);
    }


    // create 10 historical alerts for each definition, 8 OK and 2 CRIT
    calendar.clear();
    calendar.set(2014, Calendar.JANUARY, 1);

    for (AlertDefinitionEntity definition : m_definitions) {
      for (int i = 0; i < 10; i++) {
        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setServiceName(definition.getServiceName());
        history.setClusterId(1L);
        history.setAlertDefinition(definition);
        history.setAlertLabel(definition.getDefinitionName() + " " + i);
        history.setAlertText(definition.getDefinitionName() + " " + i);
        history.setAlertTimestamp(calendar.getTimeInMillis());
        history.setHostName(HOSTNAME);

        history.setAlertState(AlertState.OK);
        if (i == 0 || i == 5) {
          history.setAlertState(AlertState.CRITICAL);
        }

        // increase the days for each
        calendar.add(Calendar.DATE, 1);
        m_histories.add(history);
      }
    }

    // mock out the uptime to be a while (since most tests are not testing
    // system uptime)
    m_runtimeMXBean = EasyMock.createNiceMock(RuntimeMXBean.class);
    PowerMock.mockStatic(ManagementFactory.class);
    expect(ManagementFactory.getRuntimeMXBean()).andReturn(m_runtimeMXBean).atLeastOnce();
    PowerMock.replay(ManagementFactory.class);
    expect(m_runtimeMXBean.getUptime()).andReturn(360000L).atLeastOnce();

    replay( m_runtimeMXBean);
    }

  /**
   * Tests the parsing of the {@link AlertHistoryEntity} list into
   * {@link AlertSummaryInfo}.
   *
   * @throws Exception
   */
  @Test
  public void testAlertInfo() throws Exception {
    AlertHistoryEntity history = m_histories.get(0);
    AlertInfo alertInfo = new AlertInfo(history);
    assertEquals(history.getAlertDefinition().getLabel(), alertInfo.getAlertName());
    assertEquals(history.getAlertState(), alertInfo.getAlertState());
    assertEquals(history.getAlertText(), alertInfo.getAlertText());
    assertEquals(history.getComponentName(), alertInfo.getComponentName());
    assertEquals(history.getHostName(), alertInfo.getHostName());
    assertEquals(history.getServiceName(), alertInfo.getServiceName());

    assertEquals(false, alertInfo.hasComponentName());
    assertEquals(true, alertInfo.hasHostName());
  }

  /**
   * Tests the parsing of the {@link AlertHistoryEntity} list into
   * {@link AlertSummaryInfo}.
   *
   * @throws Exception
   */
  @Test
  public void testAlertSummaryInfo() throws Exception {
    AlertSummaryInfo alertInfo = new AlertSummaryInfo(m_histories);
    assertEquals(50, alertInfo.getAlerts().size());
    assertEquals(10, alertInfo.getAlerts("Service 1").size());
    assertEquals(10, alertInfo.getAlerts("Service 2").size());

    assertEquals(8, alertInfo.getAlerts("Service 1", "OK").size());
    assertEquals(2, alertInfo.getAlerts("Service 1", "CRITICAL").size());
    assertNull(alertInfo.getAlerts("Service 1", "WARNING"));
    assertNull(alertInfo.getAlerts("Service 1", "UNKNOWN"));

    assertEquals(5, alertInfo.getServices().size());
  }

  /**
   * Tests that the dispatcher is not called when there are no notices.
   *
   * @throws Exception
   */
  @Test
  public void testNoDispatch() throws Exception {
    EasyMock.expect(m_dao.findPendingNotices()).andReturn(
      new ArrayList<>()).once();

    // m_dispatchFactory should not be called at all
    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);
  }

  /**
   * Tests a digest dispatch for email.
   *
   * @throws Exception
   */
  @Test
  public void testDigestDispatch() throws Exception {
    MockEmailDispatcher dispatcher = new MockEmailDispatcher();
    List<AlertNoticeEntity> notices = getSingleMockNotice(dispatcher.getType());
    AlertNoticeEntity notice = notices.get(0);

    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dispatchFactory.getDispatcher("EMAIL")).andReturn(dispatcher).once();
    EasyMock.expect(m_dao.merge(notice)).andReturn(notice).atLeastOnce();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    Notification notification = dispatcher.getNotification();
    assertNotNull(notification);

    assertTrue(notification.Subject.contains("OK[1]"));
    assertTrue(notification.Subject.contains("Critical[0]"));
    assertTrue(notification.Body.contains(ALERT_UNIQUE_TEXT));
  }

  @Test
  public void testExceptionHandling() throws Exception {
    List<AlertNoticeEntity> notices = getSingleMockNotice("EMAIL");
    AlertNoticeEntity notice = notices.get(0);

    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dispatchFactory.getDispatcher("EMAIL")).andReturn(null).once();
    EasyMock.expect(m_dao.merge(notice)).andReturn(notice).atLeastOnce();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    // no exceptions should be thrown
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);
  }

  /**
   * Tests a digest dispatch for SNMP.
   *
   * @throws Exception
   */
  @Test
  public void testSingleSnmpDispatch() throws Exception {
    MockSnmpDispatcher dispatcher = new MockSnmpDispatcher();

    List<AlertNoticeEntity> notices = getSnmpMockNotices("SNMP");
    AlertNoticeEntity notice1 = notices.get(0);
    AlertNoticeEntity notice2 = notices.get(1);

    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dao.merge(notice1)).andReturn(notice1).once();
    EasyMock.expect(m_dao.merge(notice2)).andReturn(notice2).once();
    EasyMock.expect(m_dispatchFactory.getDispatcher("SNMP")).andReturn(dispatcher).atLeastOnce();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    List<Notification> notifications = dispatcher.getNotifications();
    assertEquals(2, notifications.size());
  }

  /**
   * Tests a digest dispatch for Ambari SNMP.
   *
   * @throws Exception
   */
  @Test
  public void testAmbariSnmpSingleDispatch() throws Exception {
    MockAmbariSnmpDispatcher dispatcher = new MockAmbariSnmpDispatcher();

    List<AlertNoticeEntity> notices = getSnmpMockNotices("AMBARI_SNMP");
    AlertNoticeEntity notice1 = notices.get(0);
    AlertNoticeEntity notice2 = notices.get(1);

    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dao.merge(notice1)).andReturn(notice1).once();
    EasyMock.expect(m_dao.merge(notice2)).andReturn(notice2).once();
    EasyMock.expect(m_dispatchFactory.getDispatcher("AMBARI_SNMP")).andReturn(dispatcher).atLeastOnce();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    List<Notification> notifications = dispatcher.getNotifications();
    assertEquals(2, notifications.size());
  }

  /**
   * Tests a real dispatch for Ambari SNMP.
   *
   * @throws Exception
   */
  @Test
  public void testAmbariSnmpRealDispatch() throws Exception {
    AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(8081);

    List<AlertNoticeEntity> notices = getSnmpMockNotices("AMBARI_SNMP");
    AlertNoticeEntity notice1 = notices.get(0);
    AlertNoticeEntity notice2 = notices.get(1);

    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dao.merge(notice1)).andReturn(notice1).once();
    EasyMock.expect(m_dao.merge(notice2)).andReturn(notice2).once();
    EasyMock.expect(m_dispatchFactory.getDispatcher("AMBARI_SNMP")).andReturn(dispatcher).once();
    EasyMock.expect(m_dao.findNoticeByUuid(ALERT_NOTICE_UUID_1)).andReturn(notice1).once();
    EasyMock.expect(m_dao.merge(notice1)).andReturn(notice1).once();
    EasyMock.expect(m_dao.findNoticeByUuid(ALERT_NOTICE_UUID_2)).andReturn(notice2).once();
    EasyMock.expect(m_dao.merge(notice2)).andReturn(notice2).once();
    EasyMock.replay(m_dao, m_dispatchFactory);

    // "startup" the service so that its initialization is done
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    SnmpReceiver snmpReceiver = new SnmpReceiver();

    service.runOneIteration();
    Thread.sleep(1000);

    EasyMock.verify(m_dao, m_dispatchFactory);

    List<Vector> expectedTrapVectors = new LinkedList<>();
    Vector firstVector = new Vector();
    firstVector.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(360000L)));
    firstVector.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(AmbariSNMPDispatcher.AMBARI_ALERT_TRAP_OID)));    
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_ID_OID), new Integer32(new BigDecimal(1L).intValueExact())));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_NAME_OID), new OctetString("alert-definition-1")));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_HASH_OID), new OctetString("1")));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_NAME_OID), new OctetString("Alert Definition 1")));

    Vector secondVector = new Vector(firstVector);

    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_TEXT_OID), new OctetString(ALERT_UNIQUE_TEXT)));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_STATE_OID), new Integer32(0)));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_HOST_NAME_OID), new OctetString("null")));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_SERVICE_NAME_OID), new OctetString("HDFS")));
    firstVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_COMPONENT_NAME_OID), new OctetString("null")));

    secondVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_TEXT_OID), new OctetString(ALERT_UNIQUE_TEXT + " CRITICAL")));
    secondVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_STATE_OID), new Integer32(3)));
    secondVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_HOST_NAME_OID), new OctetString("null")));
    secondVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_SERVICE_NAME_OID), new OctetString("HDFS")));
    secondVector.add(new VariableBinding(new OID(AmbariSNMPDispatcher.AMBARI_ALERT_COMPONENT_NAME_OID), new OctetString("null")));

    expectedTrapVectors.add(firstVector);
    expectedTrapVectors.add(secondVector);
    assertNotNull(snmpReceiver.receivedTrapsVectors);
    assertTrue(snmpReceiver.receivedTrapsVectors.size() == 2);
    assertEquals(expectedTrapVectors, snmpReceiver.receivedTrapsVectors);
  }

  /**
   * Tests that a failed dispatch invokes the callback to mark the UUIDs of the
   * notices as FAILED.
   *
   * @throws Exception
   */
  @Test
  public void testFailedDispatch() throws Exception {
    MockEmailDispatcher dispatcher = new MockEmailDispatcher();
    List<AlertNoticeEntity> notices = getSingleMockNotice(dispatcher.getType());
    AlertNoticeEntity notice = notices.get(0);

    // these expectations happen b/c we need to mark the notice as FAILED
    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dao.merge(notice)).andReturn(notice).once();
    EasyMock.expect(m_dao.findNoticeByUuid(ALERT_NOTICE_UUID_1)).andReturn(notice).once();
    EasyMock.expect(m_dao.merge(notice)).andReturn(notice).once();
    EasyMock.expect(m_dispatchFactory.getDispatcher(dispatcher.getType())).andReturn(dispatcher).once();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // do NOT startup the service which will force a template NPE
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    Notification notification = dispatcher.getNotification();
    assertNull(notification);
  }

  /**
   * Tests that when a dispatcher doesn't call back, the
   * {@link AlertNoticeEntity} will be put from
   * {@link NotificationState#PENDING} to {@link NotificationState#DISPATCHED}.
   *
   * @throws Exception
   */
  @Test
  public void testDispatcherWithoutCallbacks() throws Exception {
    MockNoCallbackDispatcher dispatcher = new MockNoCallbackDispatcher();
    List<AlertNoticeEntity> notices = getSingleMockNotice(dispatcher.getType());
    AlertNoticeEntity notice = notices.get(0);

    // these expectations happen b/c we need to mark the notice as FAILED
    EasyMock.expect(m_dao.findPendingNotices()).andReturn(notices).once();
    EasyMock.expect(m_dao.merge(notice)).andReturn(notice).atLeastOnce();
    EasyMock.expect(m_dispatchFactory.getDispatcher(dispatcher.getType())).andReturn(dispatcher).once();

    EasyMock.replay(m_dao, m_dispatchFactory);

    // do NOT startup the service which will force a template NPE
    AlertNoticeDispatchService service = m_injector.getInstance(AlertNoticeDispatchService.class);
    service.startUp();

    // service trigger with mock executor that blocks
    service.setExecutor(new MockExecutor());
    service.runOneIteration();

    EasyMock.verify(m_dao, m_dispatchFactory);

    Notification notification = dispatcher.getNotification();
    assertNotNull(notification);

    // the most important part of this test; ensure that notices that are
    // processed but have no callbacks are in the DISPATCHED state
    assertEquals(NotificationState.DISPATCHED, notice.getNotifyState());
  }

  /**
   * Gets a single PENDING notice.
   *
   * @return
   */
  private List<AlertNoticeEntity> getSingleMockNotice(String notificationType) {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionId(1L);
    definition.setDefinitionName("alert-definition-1");
    definition.setLabel("Alert Definition 1");

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setServiceName("HDFS");
    history.setClusterId(1L);
    history.setAlertLabel("Label");
    history.setAlertState(AlertState.OK);
    history.setAlertText(ALERT_UNIQUE_TEXT);
    history.setAlertTimestamp(System.currentTimeMillis());


    AlertTargetEntity target = new AlertTargetEntity();
    target.setTargetId(1L);
    target.setAlertStates(EnumSet.allOf(AlertState.class));
    target.setTargetName("Alert Target");
    target.setDescription("Mock Target");
    target.setNotificationType(notificationType);

    String properties = "{ \"foo\" : \"bar\" }";
    target.setProperties(properties);

    AlertNoticeEntity notice = new AlertNoticeEntity();
    notice.setUuid(ALERT_NOTICE_UUID_1);
    notice.setAlertTarget(target);
    notice.setAlertHistory(history);
    notice.setNotifyState(NotificationState.PENDING);

    ArrayList<AlertNoticeEntity> notices = new ArrayList<>();
    notices.add(notice);

    return notices;
  }

  /**
   * Gets 2 PENDING notices for SNMP or AMBARI_SNMP notificationType.
   *
   * @return
   */
  private List<AlertNoticeEntity> getSnmpMockNotices(String notificationType) {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionId(1L);
    definition.setDefinitionName("alert-definition-1");
    definition.setLabel("Alert Definition 1");

    AlertHistoryEntity history1 = new AlertHistoryEntity();
    history1.setAlertDefinition(definition);
    history1.setServiceName("HDFS");
    history1.setClusterId(1L);
    history1.setAlertLabel("Label");
    history1.setAlertState(AlertState.OK);
    history1.setAlertText(ALERT_UNIQUE_TEXT);
    history1.setAlertTimestamp(System.currentTimeMillis());

    AlertHistoryEntity history2 = new AlertHistoryEntity();
    history2.setAlertDefinition(definition);
    history2.setServiceName("HDFS");
    history2.setClusterId(1L);
    history2.setAlertLabel("Label");
    history2.setAlertState(AlertState.CRITICAL);
    history2.setAlertText(ALERT_UNIQUE_TEXT + " CRITICAL");
    history2.setAlertTimestamp(System.currentTimeMillis());

    AlertTargetEntity target = new AlertTargetEntity();
    target.setTargetId(1L);
    target.setAlertStates(EnumSet.allOf(AlertState.class));
    target.setTargetName("Alert Target");
    target.setDescription("Mock Target");
    target.setNotificationType(notificationType);

    String properties = "{ \"ambari.dispatch.snmp.version\": \"SNMPv1\", \"ambari.dispatch.snmp.port\": \"8000\"," +
                         " \"ambari.dispatch.recipients\": [\"127.0.0.1\"],\"ambari.dispatch.snmp.community\":\"\" }";
    target.setProperties(properties);

    AlertNoticeEntity notice1 = new AlertNoticeEntity();
    notice1.setUuid(ALERT_NOTICE_UUID_1);
    notice1.setAlertTarget(target);
    notice1.setAlertHistory(history1);
    notice1.setNotifyState(NotificationState.PENDING);

    AlertNoticeEntity notice2 = new AlertNoticeEntity();
    notice2.setUuid(ALERT_NOTICE_UUID_2);
    notice2.setAlertTarget(target);
    notice2.setAlertHistory(history2);
    notice2.setNotifyState(NotificationState.PENDING);

    ArrayList<AlertNoticeEntity> notices = new ArrayList<>();
    notices.add(notice1);
    notices.add(notice2);

    return notices;
  }

  /**
   * A mock dispatcher that captures the {@link Notification}.
   */
  private static final class MockEmailDispatcher implements NotificationDispatcher {

    private Notification m_notificaiton;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
      return "EMAIL";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDigestSupported() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotificationContentGenerationRequired() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatch(Notification notification) {
      m_notificaiton = notification;
    }

    @Override
    public TargetConfigurationResult validateTargetConfig(Map<String, Object> properties) {
      return null;
    }

    public Notification getNotification() {
      return m_notificaiton;
    }
  }

  /**
   * A mock dispatcher that captures the {@link Notification}.
   */
  private static class MockSnmpDispatcher implements
      NotificationDispatcher {

    private List<Notification> m_notifications = new ArrayList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
      return "SNMP";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotificationContentGenerationRequired() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDigestSupported() {
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatch(Notification notification) {
      m_notifications.add(notification);
    }

    public List<Notification> getNotifications() {
      return m_notifications;
    }

    @Override
    public TargetConfigurationResult validateTargetConfig(
        Map<String, Object> properties) {
      return null;
    }
  }

  private static final class MockAmbariSnmpDispatcher extends MockSnmpDispatcher {
    @Override
    public String getType() { return TargetType.AMBARI_SNMP.name();}
  }

  /**
   * A mock dispatcher that captures the {@link Notification}.
   */
  private static final class MockNoCallbackDispatcher implements
      NotificationDispatcher {

    private Notification m_notificaiton;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
      return "NO_CALLBACK";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotificationContentGenerationRequired() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDigestSupported() {
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatch(Notification notification) {
      m_notificaiton = notification;
    }

    @Override
    public TargetConfigurationResult validateTargetConfig(
        Map<String, Object> properties) {
      return null;
    }

    public Notification getNotification() {
      return m_notificaiton;
    }
  }

  /**
   * An {@link Executor} that calls {@link Runnable#run()} directly in the
   * current thread.
   */
  private static final class MockExecutor implements Executor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable runnable) {
      runnable.run();
    }
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     *
     */
    @Override
    public void configure(Binder binder) {
      Cluster cluster = EasyMock.createNiceMock(Cluster.class);
      PartialNiceMockBinder.newBuilder().addDBAccessorBinding().addAmbariMetaInfoBinding().addLdapBindings().build().configure(binder);

      binder.bind(AlertDispatchDAO.class).toInstance(m_dao);
      binder.bind(DispatchFactory.class).toInstance(m_dispatchFactory);
      binder.bind(AmbariMetaInfo.class).toInstance(m_metaInfo);
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
      binder.bind(AlertsDAO.class).toInstance(createNiceMock(AlertsDAO.class));

      binder.bind(AlertNoticeDispatchService.class).toInstance(new AlertNoticeDispatchService());

      EasyMock.expect(m_metaInfo.getServerVersion()).andReturn("2.0.0").anyTimes();
      EasyMock.replay(m_metaInfo);
    }
  }

  private class SnmpReceiver {
    private Snmp snmp = null;
    private Address targetAddress = GenericAddress.parse("udp:127.0.0.1/8000");
    private TransportMapping transport = null;
    public List<Vector> receivedTrapsVectors = null;
    public SnmpReceiver() throws Exception{
      transport = new DefaultUdpTransportMapping();
      snmp = new Snmp(transport);
      receivedTrapsVectors = new LinkedList<>();

      CommandResponder trapPrinter = new CommandResponder() {
        @Override
        public synchronized void processPdu(CommandResponderEvent e){
          PDU command = e.getPDU();
          if (command != null) {
            receivedTrapsVectors.add(command.getVariableBindings());
          }
        }
      };
      snmp.addNotificationListener(targetAddress, trapPrinter);
    }
  }
}
