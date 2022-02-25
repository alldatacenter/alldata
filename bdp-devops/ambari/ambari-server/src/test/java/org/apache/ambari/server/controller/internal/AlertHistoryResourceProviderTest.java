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

import org.apache.ambari.server.controller.AlertHistoryRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
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
 * {@link AlertHistoryResourceProvider} tests.
 */
public class AlertHistoryResourceProviderTest {

  private AlertsDAO m_dao = null;
  private Injector m_injector;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertsDAO.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_injector.injectMembers(this);
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
  private  void testGetResourcesNoPredicate(Authentication authentication) throws Exception {
    AlertHistoryResourceProvider provider = createProvider();

    Request request = PropertyHelper.getReadRequest(
        "AlertHistory/cluster_name", "AlertHistory/id");

    expect(m_dao.findAll(EasyMock.anyObject(AlertHistoryRequest.class))).andReturn(
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
  private void testGetResourcesClusterPredicate(Authentication authentication) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_ID,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_COMPONENT_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_HOSTNAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE);

    Predicate predicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAll(EasyMock.anyObject(AlertHistoryRequest.class))).andReturn(
        getMockEntities());

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    Clusters clusters = m_injector.getInstance(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController amc = m_injector.getInstance(AmbariManagementController.class);

    replay(m_dao, amc, clusters, cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertHistoryResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "namenode_definition",
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME));

    Assert.assertEquals(AlertState.WARNING,
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_STATE));

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
  public void testGetSingleResource(Authentication authentication) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_ID,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_COMPONENT_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_HOSTNAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE);

    Predicate predicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").and().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_ID).equals("1").toPredicate();

    expect(m_dao.findAll(EasyMock.anyObject(AlertHistoryRequest.class))).andReturn(
        getMockEntities());

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    Clusters clusters = m_injector.getInstance(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController amc = m_injector.getInstance(AmbariManagementController.class);

    replay(m_dao, amc, clusters, cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertHistoryResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "namenode_definition",
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME));

    Assert.assertEquals(AlertState.WARNING,
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_STATE));
  }

  /**
   * @return
   */
  private AlertHistoryResourceProvider createProvider() {
    return new AlertHistoryResourceProvider(m_injector.getInstance(AmbariManagementController.class));
  }

  /**
   * @return
   */
  private List<AlertHistoryEntity> getMockEntities() throws Exception {
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

    AlertHistoryEntity entity = new AlertHistoryEntity();
    entity.setAlertId(1L);
    entity.setAlertDefinition(definition);
    entity.setClusterId(Long.valueOf(1L));
    entity.setComponentName(null);
    entity.setAlertText("Mock Label");
    entity.setServiceName("HDFS");
    entity.setAlertState(AlertState.WARNING);
    entity.setAlertTimestamp(System.currentTimeMillis());
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

      binder.bind(AlertsDAO.class).toInstance(m_dao);
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(AmbariManagementController.class).toInstance(amc);
      binder.bind(ActionMetadata.class);
    }
  }
}
