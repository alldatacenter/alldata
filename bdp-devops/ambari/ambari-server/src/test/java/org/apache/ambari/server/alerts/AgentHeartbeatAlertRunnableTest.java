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

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests {@link AgentHeartbeatAlertRunnable}.
 */
public class AgentHeartbeatAlertRunnableTest {

  private final static long CLUSTER_ID = 1;
  private final static String CLUSTER_NAME = "c1";
  private final static String HOSTNAME = "c6401.ambari.apache.org";

  private final static String DEFINITION_NAME = "ambari_server_agent_heartbeat";
  private final static String DEFINITION_SERVICE = "AMBARI";
  private final static String DEFINITION_COMPONENT = "AMBARI_SERVER";
  private final static String DEFINITION_LABEL = "Mock Definition";

  private Clusters m_clusters;
  private Cluster m_cluster;
  private Host m_host;
  private Injector m_injector;
  private AlertDefinitionDAO m_definitionDao;
  private AlertDefinitionEntity m_definition;
  private MockEventListener m_listener;

  private AlertEventPublisher m_eventPublisher;
  private EventBus m_synchronizedBus;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new MockModule());
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_cluster = m_injector.getInstance(Cluster.class);
    m_eventPublisher = m_injector.getInstance(AlertEventPublisher.class);
    m_listener = m_injector.getInstance(MockEventListener.class);
    m_definition = EasyMock.createNiceMock(AlertDefinitionEntity.class);
    m_host = EasyMock.createNiceMock(Host.class);

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

    // create the host map
    Map<String, Host> hostMap = new HashMap<>();
    hostMap.put(HOSTNAME, m_host);

    // mock the definition for the alert
    expect(m_definition.getDefinitionName()).andReturn(DEFINITION_NAME).atLeastOnce();
    expect(m_definition.getServiceName()).andReturn(DEFINITION_SERVICE).atLeastOnce();
    expect(m_definition.getComponentName()).andReturn(DEFINITION_COMPONENT).atLeastOnce();
    expect(m_definition.getLabel()).andReturn(DEFINITION_LABEL).atLeastOnce();
    expect(m_definition.getEnabled()).andReturn(true).atLeastOnce();

    // mock the host state
    expect(m_host.getState()).andReturn(HostState.HEALTHY);

    // mock the cluster
    expect(m_cluster.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();
    expect(m_cluster.getHosts()).andReturn(Collections.singleton(m_host)).atLeastOnce();

    // mock clusters
    expect(m_clusters.getClusters()).andReturn(clusterMap).atLeastOnce();

    // mock the definition DAO
    expect(m_definitionDao.findByName(CLUSTER_ID, DEFINITION_NAME)).andReturn(
        m_definition).atLeastOnce();

    EasyMock.replay(m_definition, m_host, m_cluster, m_clusters,
        m_definitionDao);
    }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
  }

  @Test
  public void testHealthyHostAlert(){
    // precondition that no events were fired
    assertEquals(0,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    AgentHeartbeatAlertRunnable runnable = new AgentHeartbeatAlertRunnable(
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
    assertEquals(AlertState.OK, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_definition, m_host, m_cluster, m_clusters,
        m_definitionDao);
  }

  @Test
  public void testLostHeartbeatAlert() {
    EasyMock.reset(m_host);
    expect(m_host.getState()).andReturn(HostState.HEARTBEAT_LOST).atLeastOnce();
    replay(m_host);

    // precondition that no events were fired
    assertEquals(0,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    AgentHeartbeatAlertRunnable runnable = new AgentHeartbeatAlertRunnable(
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
    assertEquals(AlertState.CRITICAL, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_definition, m_host, m_cluster, m_clusters, m_definitionDao);
  }

  @Test
  public void testUnhealthyHostAlert() {
    EasyMock.reset(m_host);
    expect(m_host.getState()).andReturn(HostState.UNHEALTHY).atLeastOnce();
    replay(m_host);

    // precondition that no events were fired
    assertEquals(0,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    AgentHeartbeatAlertRunnable runnable = new AgentHeartbeatAlertRunnable(
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
    assertEquals(AlertState.CRITICAL, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_definition, m_host, m_cluster, m_clusters, m_definitionDao);
  }

  /**
   *
   */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      PartialNiceMockBinder.newBuilder().addConfigsBindings()
          .addAlertDefinitionBinding().addLdapBindings().build().configure(binder);
    }
  }
}
