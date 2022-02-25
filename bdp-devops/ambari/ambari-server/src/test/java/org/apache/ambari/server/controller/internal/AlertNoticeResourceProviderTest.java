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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.controller.AlertNoticeRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.NotificationState;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * {@link AlertNoticeResourceProvider} tests.
 */
public class AlertNoticeResourceProviderTest {

  private AlertDispatchDAO m_dao = null;
  private Injector m_injector;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    Assert.assertNotNull(m_injector);
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testGetResourcesNoPredicateAsAdministrator() throws Exception {
    testGetResourcesNoPredicate(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesNoPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesNoPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesNoPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesNoPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesNoPredicateAsClusterUser() throws Exception {
    testGetResourcesNoPredicate(TestAuthenticationFactory.createClusterUser());
  }

  @Test
  public void testGetResourcesNoPredicateAsViewUser() throws Exception {
    testGetResourcesNoPredicate(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void testGetResourcesNoPredicate(Authentication authentication) throws Exception {
    AlertNoticeResourceProvider provider = createProvider();

    Request request = PropertyHelper.getReadRequest(
        "AlertHistory/cluster_name", "AlertHistory/id");

    expect(m_dao.findAllNotices(EasyMock.anyObject(AlertNoticeRequest.class))).andReturn(
        Collections.emptyList());

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> results = provider.getResources(request, null);
    assertEquals(0, results.size());
  }

  @Test
  public void testGetResourcesClusterPredicateAsAdministrator() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesClusterPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesClusterPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetResourcesClusterPredicateAsClusterUser() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesClusterPredicateAsViewUser() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * @throws Exception
   */
  protected void testGetResourcesClusterPredicate(Authentication authentication) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_HISTORY_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_STATE);

    Predicate predicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAllNotices(EasyMock.anyObject(AlertNoticeRequest.class))).andReturn(
        getMockEntities());

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    Clusters clusters = m_injector.getInstance(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController amc = m_injector.getInstance(AmbariManagementController.class);

    replay(m_dao, amc, clusters, cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertNoticeResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "Administrators",
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME));

    Assert.assertEquals(
        NotificationState.FAILED,
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_STATE));

    verify(m_dao, amc, clusters, cluster);
  }

  @Test
  public void testGetSingleResourceAsAdministrator() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetSingleResourceAsClusterAdministrator() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetSingleResourceAsServiceAdministrator() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetSingleResourceAsClusterUser() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetSingleResourceAsViewUser() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * @throws Exception
   */
  protected void testGetSingleResource(Authentication authentication) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_HISTORY_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_STATE);

    Predicate predicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").and().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_ID).equals("1").toPredicate();

    expect(m_dao.findAllNotices(EasyMock.anyObject(AlertNoticeRequest.class))).andReturn(
        getMockEntities());

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    Clusters clusters = m_injector.getInstance(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController amc = m_injector.getInstance(AmbariManagementController.class);

    replay(m_dao, amc, clusters, cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertNoticeResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "Administrators",
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME));

    Assert.assertEquals(NotificationState.FAILED,
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_STATE));
  }

  /**
   * @return
   */
  private AlertNoticeResourceProvider createProvider() {
    return new AlertNoticeResourceProvider(m_injector.getInstance(AmbariManagementController.class));
  }

  /**
   * @return
   */
  private List<AlertNoticeEntity> getMockEntities() throws Exception {
    ResourceEntity clusterResource = new ResourceEntity();
    clusterResource.setId(4L);

    ClusterEntity cluster = new ClusterEntity();
    cluster.setClusterName("c1");
    cluster.setClusterId(1L);
    cluster.setResource(clusterResource);

    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(1L);
    definition.setComponentName("NAMENODE");
    definition.setDefinitionName("namenode_definition");
    definition.setEnabled(true);
    definition.setServiceName("HDFS");
    definition.setCluster(cluster);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertId(1L);
    history.setAlertDefinition(definition);
    history.setClusterId(Long.valueOf(1L));
    history.setComponentName(null);
    history.setAlertText("Mock Label");
    history.setServiceName("HDFS");
    history.setAlertState(AlertState.WARNING);
    history.setAlertTimestamp(System.currentTimeMillis());

    AlertTargetEntity administrators = new AlertTargetEntity();
    administrators.setDescription("The Administrators");
    administrators.setNotificationType("EMAIL");
    administrators.setTargetName("Administrators");

    AlertNoticeEntity entity = new AlertNoticeEntity();
    entity.setAlertHistory(history);
    entity.setAlertTarget(administrators);
    entity.setNotifyState(NotificationState.FAILED);
    entity.setUuid(UUID.randomUUID().toString());
    return Arrays.asList(entity);
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
      Clusters clusters = createMock(Clusters.class);

      AmbariManagementController amc = createMock(AmbariManagementController.class);
      expect(amc.getClusters()).andReturn(clusters).anyTimes();

      binder.bind(AlertDispatchDAO.class).toInstance(m_dao);
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(AmbariManagementController.class).toInstance(amc);
      binder.bind(ActionMetadata.class);
    }
  }
}
