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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests {@link ComponentVersionAlertRunnable}.
 */
public class ComponentVersionAlertRunnableTest extends EasyMockSupport {

  private final static long CLUSTER_ID = 1;
  private final static String CLUSTER_NAME = "c1";
  private final static String HOSTNAME_1 = "c6401.ambari.apache.org";
  private final static String HOSTNAME_2 = "c6402.ambari.apache.org";

  private final static String EXPECTED_VERSION = "2.6.0.0-1234";
  private final static String WRONG_VERSION = "9.9.9.9-9999";

  private final static String DEFINITION_NAME = "ambari_server_component_version";
  private final static String DEFINITION_SERVICE = "AMBARI";
  private final static String DEFINITION_COMPONENT = "AMBARI_SERVER";
  private final static String DEFINITION_LABEL = "Mock Definition";

  private Clusters m_clusters;
  private Cluster m_cluster;
  private Injector m_injector;
  private AlertDefinitionDAO m_definitionDao;
  private AlertDefinitionEntity m_definition;
  private MockEventListener m_listener;
  private AmbariMetaInfo m_metaInfo;

  private AlertEventPublisher m_eventPublisher;
  private EventBus m_synchronizedBus;

  private Collection<Host> m_hosts;
  private Map<String, List<ServiceComponentHost>> m_hostComponentMap = new HashMap<>();
  private StackId m_desidredStackId;

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
    m_definition = createNiceMock(AlertDefinitionEntity.class);
    m_metaInfo = m_injector.getInstance(AmbariMetaInfo.class);

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

    // hosts
    m_hosts = new ArrayList<>();
    Host host1 = createNiceMock(Host.class);
    Host host2 = createNiceMock(Host.class);
    expect(host1.getHostName()).andReturn(HOSTNAME_1).atLeastOnce();
    expect(host2.getHostName()).andReturn(HOSTNAME_2).atLeastOnce();
    m_hosts.add(host1);
    m_hosts.add(host2);

    m_hostComponentMap.put(HOSTNAME_1, new ArrayList<>());
    m_hostComponentMap.put(HOSTNAME_2, new ArrayList<>());

    // desired stack
    m_desidredStackId = createNiceMock(StackId.class);
    expect(m_desidredStackId.getStackName()).andReturn("SOME-STACK").atLeastOnce();
    expect(m_desidredStackId.getStackVersion()).andReturn("STACK-VERSION").atLeastOnce();

    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersionEntity.getVersion()).andReturn(EXPECTED_VERSION).anyTimes();

    // services
    Service service = createNiceMock(Service.class);
    expect(service.getDesiredRepositoryVersion()).andReturn(repositoryVersionEntity).atLeastOnce();

    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    expect(serviceComponent.getDesiredStackId()).andReturn(m_desidredStackId).atLeastOnce();
    expect(service.getServiceComponent(EasyMock.anyString())).andReturn(serviceComponent).atLeastOnce();

    // components
    ServiceComponentHost sch1_1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost sch1_2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost sch2_1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost sch2_2 = createNiceMock(ServiceComponentHost.class);

    expect(sch1_1.getServiceName()).andReturn("FOO").atLeastOnce();
    expect(sch1_1.getServiceComponentName()).andReturn("FOO_COMPONENT").atLeastOnce();
    expect(sch1_1.getVersion()).andReturn(EXPECTED_VERSION).atLeastOnce();
    expect(sch1_2.getServiceName()).andReturn("BAR").atLeastOnce();
    expect(sch1_2.getServiceComponentName()).andReturn("BAR_COMPONENT").atLeastOnce();
    expect(sch1_2.getVersion()).andReturn(EXPECTED_VERSION).atLeastOnce();
    expect(sch2_1.getServiceName()).andReturn("FOO").atLeastOnce();
    expect(sch2_1.getServiceComponentName()).andReturn("FOO_COMPONENT").atLeastOnce();
    expect(sch2_1.getVersion()).andReturn(EXPECTED_VERSION).atLeastOnce();
    expect(sch2_2.getServiceName()).andReturn("BAZ").atLeastOnce();
    expect(sch2_2.getServiceComponentName()).andReturn("BAZ_COMPONENT").atLeastOnce();
    expect(sch2_2.getVersion()).andReturn(EXPECTED_VERSION).atLeastOnce();

    m_hostComponentMap.get(HOSTNAME_1).add(sch1_1);
    m_hostComponentMap.get(HOSTNAME_1).add(sch1_2);
    m_hostComponentMap.get(HOSTNAME_2).add(sch2_1);
    m_hostComponentMap.get(HOSTNAME_2).add(sch2_2);

    // mock the definition for the alert
    expect(m_definition.getDefinitionName()).andReturn(DEFINITION_NAME).atLeastOnce();
    expect(m_definition.getServiceName()).andReturn(DEFINITION_SERVICE).atLeastOnce();
    expect(m_definition.getComponentName()).andReturn(DEFINITION_COMPONENT).atLeastOnce();
    expect(m_definition.getLabel()).andReturn(DEFINITION_LABEL).atLeastOnce();
    expect(m_definition.getEnabled()).andReturn(true).atLeastOnce();

    // mock the cluster
    expect(m_cluster.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();
    expect(m_cluster.getHosts()).andReturn(m_hosts).atLeastOnce();
    expect(m_cluster.getService(EasyMock.anyString())).andReturn(service).atLeastOnce();

    // mock clusters
    expect(m_clusters.getClusters()).andReturn(clusterMap).atLeastOnce();

    // mock the definition DAO
    expect(m_definitionDao.findByName(CLUSTER_ID, DEFINITION_NAME)).andReturn(
        m_definition).atLeastOnce();

    m_metaInfo.init();
    EasyMock.expectLastCall().anyTimes();

    // expect the cluster host mapping
    expect(m_cluster.getServiceComponentHosts(HOSTNAME_1)).andReturn(
        m_hostComponentMap.get(HOSTNAME_1)).once();
    expect(m_cluster.getServiceComponentHosts(HOSTNAME_2)).andReturn(
        m_hostComponentMap.get(HOSTNAME_2)).once();

    // expect the component from metainfo
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).atLeastOnce();
    expect(m_metaInfo.getComponent(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
        EasyMock.anyString())).andReturn(componentInfo).atLeastOnce();
    }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
  }

  /**
   * Tests that the alert is SKIPPED when there is an upgrade in progress.
   */
  @Test
  public void testUpgradeInProgress() throws Exception {
    UpgradeEntity upgrade = createNiceMock(UpgradeEntity.class);
    expect(upgrade.getDirection()).andReturn(Direction.UPGRADE).atLeastOnce();
    expect(m_cluster.getUpgradeInProgress()).andReturn(upgrade).once();

    replayAll();

    m_metaInfo.init();

    // precondition that no events were fired
    assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    ComponentVersionAlertRunnable runnable = new ComponentVersionAlertRunnable(
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
    assertEquals(AlertState.SKIPPED, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());
  }

  /**
   * Tests the alert that fires when all components are reporting correct
   * versions.
   */
  @Test
  public void testAllComponentVersionsCorrect() throws Exception {
    replayAll();

    m_metaInfo.init();

    // precondition that no events were fired
    assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    ComponentVersionAlertRunnable runnable = new ComponentVersionAlertRunnable(
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

    verifyAll();
  }

  /**
   * Tests that the alert which fires when there is a mismatch is a WARNING.
   */
  @Test
  public void testomponentVersionMismatch() throws Exception {
    // reset expectation so that it returns a wrong version
    ServiceComponentHost sch = m_hostComponentMap.get(HOSTNAME_1).get(0);
    EasyMock.reset(sch);
    expect(sch.getServiceName()).andReturn("FOO").atLeastOnce();
    expect(sch.getServiceComponentName()).andReturn("FOO_COMPONENT").atLeastOnce();
    expect(sch.getVersion()).andReturn(WRONG_VERSION).atLeastOnce();

    replayAll();

    m_metaInfo.init();

    // precondition that no events were fired
    assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    ComponentVersionAlertRunnable runnable = new ComponentVersionAlertRunnable(
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
    assertEquals(AlertState.WARNING, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verifyAll();
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
      Cluster cluster = createNiceMock(Cluster.class);

      PartialNiceMockBinder.newBuilder(ComponentVersionAlertRunnableTest.this).addConfigsBindings()
          .addDBAccessorBinding()
          .addFactoriesInstallBinding()
          .addAmbariMetaInfoBinding()
          .addLdapBindings()
          .build().configure(binder);

      binder.bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
    }
  }
}
