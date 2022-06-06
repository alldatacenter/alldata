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

package org.apache.ambari.server.alerts;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.alerts.AmbariPerformanceRunnable.PerformanceArea;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ServerSource;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests {@link AmbariPerformanceRunnable}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ClusterControllerHelper.class, AmbariPerformanceRunnable.class,
    PerformanceArea.class })
public class AmbariPerformanceRunnableTest {

  private final static long CLUSTER_ID = 1;
  private final static String CLUSTER_NAME = "c1";

  private final static String DEFINITION_NAME = "ambari_server_performance";
  private final static String DEFINITION_SERVICE = "AMBARI";
  private final static String DEFINITION_COMPONENT = "AMBARI_SERVER";
  private final static String DEFINITION_LABEL = "Mock Definition";
  private final static int DEFINITION_INTERVAL = 1;

  private Clusters m_clusters;
  private Cluster m_cluster;
  private Injector m_injector;
  private AlertsDAO m_alertsDao;
  private AlertDefinitionDAO m_definitionDao;
  private AlertDefinitionEntity m_definition;
  private List<AlertCurrentEntity> m_currentAlerts = new ArrayList<>();
  private MockEventListener m_listener;

  private AlertEventPublisher m_eventPublisher;
  private EventBus m_synchronizedBus;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new MockModule());
    m_alertsDao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_cluster = m_injector.getInstance(Cluster.class);
    m_eventPublisher = m_injector.getInstance(AlertEventPublisher.class);
    m_listener = m_injector.getInstance(MockEventListener.class);
    m_definition = EasyMock.createNiceMock(AlertDefinitionEntity.class);

    // !!! need a synchronous op for testing
    m_synchronizedBus = new EventBus();
    Field field = AlertEventPublisher.class.getDeclaredField("m_eventBus");
    field.setAccessible(true);
    field.set(m_eventPublisher, m_synchronizedBus);

    // register mock listener
    m_synchronizedBus.register(m_listener);

    // create the cluster map
    Map<String,Cluster> clusterMap = new HashMap<>();
    clusterMap.put(CLUSTER_NAME, m_cluster);

    // mock the definition for the alert
    expect(m_definition.getDefinitionName()).andReturn(DEFINITION_NAME).atLeastOnce();
    expect(m_definition.getServiceName()).andReturn(DEFINITION_SERVICE).atLeastOnce();
    expect(m_definition.getComponentName()).andReturn(DEFINITION_COMPONENT).atLeastOnce();
    expect(m_definition.getLabel()).andReturn(DEFINITION_LABEL).atLeastOnce();
    expect(m_definition.getEnabled()).andReturn(true).atLeastOnce();
    expect(m_definition.getScheduleInterval()).andReturn(DEFINITION_INTERVAL).atLeastOnce();

    // mock the cluster
    expect(m_cluster.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();

    // mock clusters
    expect(m_clusters.getClusters()).andReturn(clusterMap).atLeastOnce();

    // mock the definition DAO
    expect(m_definitionDao.findByName(CLUSTER_ID, DEFINITION_NAME)).andReturn(
        m_definition).atLeastOnce();

    // mock the current dao
    expect(m_alertsDao.findCurrentByCluster(CLUSTER_ID)).andReturn(
        m_currentAlerts).atLeastOnce();

    // mock the factory and what it returns
    AlertDefinition definition = new AlertDefinition();
    definition.setDefinitionId(1L);
    definition.setName(DEFINITION_NAME);
    ServerSource source = new ServerSource();
    definition.setSource(source);
    AlertDefinitionFactory factory = m_injector.getInstance(AlertDefinitionFactory.class);
    expect(factory.coerce(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(definition).atLeastOnce();

    ClusterController clusterController = m_injector.getInstance(ClusterController.class);
    PowerMock.mockStatic(ClusterControllerHelper.class);
    expect(ClusterControllerHelper.getClusterController()).andReturn(clusterController);
    PowerMock.replay(ClusterControllerHelper.class);

    replay(m_definition, m_cluster, m_clusters, m_definitionDao, m_alertsDao, factory,
        clusterController);
    }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
  }

  /**
   * Tests that the event is triggerd with a status of OK if all performance
   * areas pass.
   */
  @Test
  public void testAlertFiresOKEvent() {
    // mock the entire enum so that no problems are reported
    PowerMock.mockStatic(PerformanceArea.class);
    expect(PerformanceArea.values()).andReturn(new PerformanceArea[0]);
    PowerMock.replay(PerformanceArea.class);

    // instantiate and inject mocks
    AmbariPerformanceRunnable runnable = new AmbariPerformanceRunnable(
        m_definition.getDefinitionName());

    m_injector.injectMembers(runnable);

    // run the alert
    runnable.run();

    assertEquals(1, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    List<AlertEvent> events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    AlertReceivedEvent event = (AlertReceivedEvent) events.get(0);
    Alert alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.OK, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_cluster, m_clusters, m_definitionDao);
  }

  /**
   * Tests that the event is triggerd with a status of UNKNOWN.
   */
  @Test
  public void testAlertFiresUnknownEvent() {
    // mock one area, leaving others to fail
    RequestDAO requestDAO = m_injector.getInstance(RequestDAO.class);
    expect(requestDAO.findAllRequestIds(EasyMock.anyInt(), EasyMock.anyBoolean())).andReturn(new ArrayList<>());

    replay(requestDAO);

    // instantiate and inject mocks
    AmbariPerformanceRunnable runnable = new AmbariPerformanceRunnable(
        m_definition.getDefinitionName());

    m_injector.injectMembers(runnable);

    // run the alert
    runnable.run();

    assertEquals(1,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    List<AlertEvent> events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    AlertReceivedEvent event = (AlertReceivedEvent) events.get(0);
    Alert alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.UNKNOWN, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    // verify that even though there is 1 UNKNOWN, there should also be 1 OK as
    // well
    assertTrue(alert.getText().contains("(OK)"));

    verify(m_cluster, m_clusters, m_definitionDao);
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


      PartialNiceMockBinder.newBuilder().addConfigsBindings()
          .addAlertDefinitionBinding().addLdapBindings().build().configure(binder);

      binder.bind(AlertsDAO.class).toInstance(createNiceMock(AlertsDAO.class));
      binder.bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
      binder.bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
      binder.bind(AlertDefinitionFactory.class).toInstance(createNiceMock(AlertDefinitionFactory.class));
      binder.bind(ClusterResourceProvider.class).toInstance(createNiceMock(ClusterResourceProvider.class));
      binder.bind(ClusterController.class).toInstance(createNiceMock(ClusterController.class));
      binder.bind(RequestDAO.class).toInstance(createNiceMock(RequestDAO.class));
    }
  }
}
