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
package org.apache.ambari.server.events.listeners.upgrade;

import static org.easymock.EasyMock.createNiceMock;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 *
 */
public class AlertMaintenanceModeListenerTest {

  private static final String HOSTNAME = "c6401.ambari.apache.org";
  private static final String SERVICE = "HDFS";
  private static final String COMPONENT = "NAMENODE";
  private static final Long CLUSTER_ID = 1L;
  private static final String DEFINITION_NAME = "definition_name";

  @Inject
  private AmbariEventPublisher m_eventPublisher;

  @Inject
  private AlertsDAO m_alertsDAO;

  private Injector injector;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(
        new MockModule()));

    // !!! need a synchronous op for testing
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    injector.injectMembers(this);
  }

  /**
   * Tests that only the host alert has its maintenance mode changed.
   *
   * @throws Exception
   */
  @Test
  public void testHostMaintenanceMode() throws Exception {
    List<AlertCurrentEntity> alerts = getMockAlerts("HOST");

    AlertCurrentEntity hostAlert = alerts.get(0);
    AlertCurrentEntity serviceAlert = alerts.get(1);
    AlertCurrentEntity componentAlert = alerts.get(2);

    EasyMock.expect(hostAlert.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    hostAlert.setMaintenanceState(MaintenanceState.ON);
    EasyMock.expectLastCall().once();

    EasyMock.expect(m_alertsDAO.merge(hostAlert)).andReturn(hostAlert).once();

    Host host = EasyMock.createNiceMock(Host.class);
    EasyMock.expect(host.getHostName()).andReturn(HOSTNAME).atLeastOnce();

    STOMPUpdatePublisher stompUpdatePublisher = injector.getInstance(STOMPUpdatePublisher.class);
    Capture<AlertUpdateEvent> alertUpdateEventCapture = EasyMock.newCapture();
    stompUpdatePublisher.publish(EasyMock.capture(alertUpdateEventCapture));

    EasyMock.replay(hostAlert, serviceAlert, componentAlert, host, m_alertsDAO, stompUpdatePublisher);

    MaintenanceModeEvent hostEvent = new MaintenanceModeEvent(MaintenanceState.ON, 1 /* cluster id */, host);
    m_eventPublisher.publish(hostEvent);

    EasyMock.verify(hostAlert, serviceAlert, componentAlert, host, m_alertsDAO);

    AlertUpdateEvent alertUpdateEvent = alertUpdateEventCapture.getValue();
    Assert.assertNotNull(alertUpdateEvent);
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().size());
    Assert.assertTrue(alertUpdateEvent.getSummaries().containsKey(CLUSTER_ID));
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().get(CLUSTER_ID).size());
    Assert.assertTrue(alertUpdateEvent.getSummaries().get(CLUSTER_ID).containsKey(DEFINITION_NAME));
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().get(CLUSTER_ID).get(DEFINITION_NAME).State.Ok.MaintenanceCount);
  }

  /**
   * Tests that only the service alert has its maintenance mode changed.
   */
  @Test
  public void testServiceMaintenanceMode() throws Exception {
    List<AlertCurrentEntity> alerts = getMockAlerts("SERVICE");

    AlertCurrentEntity hostAlert = alerts.get(0);
    AlertCurrentEntity serviceAlert = alerts.get(1);
    AlertCurrentEntity componentAlert = alerts.get(2);

    EasyMock.expect(serviceAlert.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    serviceAlert.setMaintenanceState(MaintenanceState.ON);
    EasyMock.expectLastCall().once();

    EasyMock.expect(m_alertsDAO.merge(serviceAlert)).andReturn(serviceAlert).once();

    Service service = EasyMock.createNiceMock(Service.class);
    EasyMock.expect(service.getName()).andReturn(SERVICE).atLeastOnce();

    STOMPUpdatePublisher stompUpdatePublisher = injector.getInstance(STOMPUpdatePublisher.class);
    Capture<AlertUpdateEvent> alertUpdateEventCapture = EasyMock.newCapture();
    stompUpdatePublisher.publish(EasyMock.capture(alertUpdateEventCapture));

    EasyMock.replay(hostAlert, serviceAlert, componentAlert, service, m_alertsDAO, stompUpdatePublisher);

    MaintenanceModeEvent serviceEvent = new MaintenanceModeEvent(MaintenanceState.ON, service);
    m_eventPublisher.publish(serviceEvent);

    EasyMock.verify(hostAlert, serviceAlert, componentAlert, service, m_alertsDAO);

    AlertUpdateEvent alertUpdateEvent = alertUpdateEventCapture.getValue();
    Assert.assertNotNull(alertUpdateEvent);
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().size());
    Assert.assertTrue(alertUpdateEvent.getSummaries().containsKey(CLUSTER_ID));
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().get(CLUSTER_ID).size());
    Assert.assertTrue(alertUpdateEvent.getSummaries().get(CLUSTER_ID).containsKey(DEFINITION_NAME));
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().get(CLUSTER_ID).get(DEFINITION_NAME).State.Ok.MaintenanceCount);
  }

  @Test
  public void testComponentMaintenanceMode() throws Exception {
    List<AlertCurrentEntity> alerts = getMockAlerts("SCH");

    AlertCurrentEntity hostAlert = alerts.get(0);
    AlertCurrentEntity serviceAlert = alerts.get(1);
    AlertCurrentEntity componentAlert = alerts.get(2);

    EasyMock.expect(componentAlert.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    componentAlert.setMaintenanceState(MaintenanceState.ON);
    EasyMock.expectLastCall().once();

    EasyMock.expect(m_alertsDAO.merge(componentAlert)).andReturn(componentAlert).once();

    ServiceComponentHost serviceComponentHost = EasyMock.createNiceMock(ServiceComponentHost.class);
    EasyMock.expect(serviceComponentHost.getHostName()).andReturn(HOSTNAME).atLeastOnce();
    EasyMock.expect(serviceComponentHost.getServiceName()).andReturn(SERVICE).atLeastOnce();
    EasyMock.expect(serviceComponentHost.getServiceComponentName()).andReturn(COMPONENT).atLeastOnce();

    STOMPUpdatePublisher stompUpdatePublisher = injector.getInstance(STOMPUpdatePublisher.class);
    Capture<AlertUpdateEvent> alertUpdateEventCapture = EasyMock.newCapture();
    stompUpdatePublisher.publish(EasyMock.capture(alertUpdateEventCapture));

    EasyMock.replay(hostAlert, serviceAlert, componentAlert, serviceComponentHost, m_alertsDAO, stompUpdatePublisher);

    MaintenanceModeEvent serviceComponentHostEvent = new MaintenanceModeEvent(MaintenanceState.ON,
        serviceComponentHost);

    m_eventPublisher.publish(serviceComponentHostEvent);

    EasyMock.verify(hostAlert, serviceAlert, componentAlert, serviceComponentHost, m_alertsDAO);

    AlertUpdateEvent alertUpdateEvent = alertUpdateEventCapture.getValue();
    Assert.assertNotNull(alertUpdateEvent);
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().size());
    Assert.assertTrue(alertUpdateEvent.getSummaries().containsKey(CLUSTER_ID));
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().get(CLUSTER_ID).size());
    Assert.assertTrue(alertUpdateEvent.getSummaries().get(CLUSTER_ID).containsKey(DEFINITION_NAME));
    Assert.assertEquals(1, alertUpdateEvent.getSummaries().get(CLUSTER_ID).get(DEFINITION_NAME).State.Ok.MaintenanceCount);
  }

  private List<AlertCurrentEntity> getMockAlerts(String testType) {
    AlertCurrentEntity hostAlert = EasyMock.createStrictMock(AlertCurrentEntity.class);
    AlertCurrentEntity serviceAlert = EasyMock.createStrictMock(AlertCurrentEntity.class);
    AlertCurrentEntity componentAlert = EasyMock.createStrictMock(AlertCurrentEntity.class);

    AlertHistoryEntity hostHistory = EasyMock.createStrictMock(AlertHistoryEntity.class);
    AlertHistoryEntity serviceHistory = EasyMock.createStrictMock(AlertHistoryEntity.class);
    AlertHistoryEntity componentHistory = EasyMock.createStrictMock(AlertHistoryEntity.class);

    AlertDefinitionEntity alertDefinition = EasyMock.createStrictMock(AlertDefinitionEntity.class);

    EasyMock.expect(hostAlert.getAlertHistory()).andReturn(hostHistory).atLeastOnce();
    EasyMock.expect(serviceAlert.getAlertHistory()).andReturn(serviceHistory).atLeastOnce();
    EasyMock.expect(componentAlert.getAlertHistory()).andReturn(componentHistory).atLeastOnce();

    EasyMock.expect(hostHistory.getHostName()).andReturn(HOSTNAME).atLeastOnce();
    EasyMock.expect(hostHistory.getServiceName()).andReturn(null).atLeastOnce();
    EasyMock.expect(hostHistory.getComponentName()).andReturn(null).atLeastOnce();
    EasyMock.expect(hostHistory.getAlertDefinition()).andReturn(alertDefinition).atLeastOnce();
    EasyMock.expect(hostHistory.getAlertState()).andReturn(AlertState.OK).atLeastOnce();
    EasyMock.expect(hostHistory.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();

    EasyMock.expect(serviceHistory.getHostName()).andReturn(null).atLeastOnce();
    EasyMock.expect(serviceHistory.getServiceName()).andReturn(SERVICE).atLeastOnce();
    EasyMock.expect(serviceHistory.getComponentName()).andReturn(null).atLeastOnce();
    EasyMock.expect(serviceHistory.getAlertDefinition()).andReturn(alertDefinition).atLeastOnce();
    EasyMock.expect(serviceHistory.getAlertState()).andReturn(AlertState.OK).atLeastOnce();
    EasyMock.expect(serviceHistory.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();

    EasyMock.expect(alertDefinition.getDefinitionId()).andReturn(1L).atLeastOnce();
    EasyMock.expect(alertDefinition.getDefinitionName()).andReturn(DEFINITION_NAME).atLeastOnce();

    if (testType.equals("SCH")) {
      EasyMock.expect(componentHistory.getHostName()).andReturn(HOSTNAME).atLeastOnce();
      EasyMock.expect(componentHistory.getServiceName()).andReturn(SERVICE).atLeastOnce();
      EasyMock.expect(componentHistory.getComponentName()).andReturn(COMPONENT).atLeastOnce();
    } else {
      EasyMock.expect(componentHistory.getHostName()).andReturn(null).atLeastOnce();
      EasyMock.expect(componentHistory.getServiceName()).andReturn(null).atLeastOnce();
      EasyMock.expect(componentHistory.getComponentName()).andReturn(COMPONENT).atLeastOnce();
    }
    EasyMock.expect(componentHistory.getAlertDefinition()).andReturn(alertDefinition).atLeastOnce();
    EasyMock.expect(componentHistory.getAlertState()).andReturn(AlertState.OK).atLeastOnce();
    EasyMock.expect(componentHistory.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();

    List<AlertCurrentEntity> currentAlerts = new ArrayList<>();
    currentAlerts.add(hostAlert);
    currentAlerts.add(serviceAlert);
    currentAlerts.add(componentAlert);

    EasyMock.expect(m_alertsDAO.findCurrent()).andReturn(currentAlerts).atLeastOnce();

    EasyMock.replay(hostHistory, serviceHistory, componentHistory, alertDefinition);
    return currentAlerts;
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      Cluster cluster = EasyMock.createNiceMock(Cluster.class);
      binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
      binder.bind(AlertsDAO.class).toInstance(createNiceMock(AlertsDAO.class));
      binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
      binder.bind(STOMPUpdatePublisher.class).toInstance(createNiceMock(STOMPUpdatePublisher.class));
    }
  }
}
