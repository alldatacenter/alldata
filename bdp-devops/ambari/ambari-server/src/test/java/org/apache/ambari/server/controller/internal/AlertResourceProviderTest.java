/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMORY_DRIVER;
import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMORY_URL;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.query.render.AlertStateSummary;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer.AlertDefinitionSummary;
import org.apache.ambari.server.api.query.render.AlertSummaryRenderer;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AlertCurrentRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.PageRequest.StartingPoint;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
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
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.MaintenanceState;
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
 * Test the AlertResourceProvider class
 */
public class AlertResourceProviderTest {

  private static final Long ALERT_VALUE_ID = 1000L;
  private static final String ALERT_VALUE_LABEL = "My Label";
  private static final Long ALERT_VALUE_TIMESTAMP = 1L;
  private static final String ALERT_VALUE_TEXT = "My Text";
  private static final String ALERT_VALUE_COMPONENT = "component";
  private static final String ALERT_VALUE_HOSTNAME = "host";
  private static final String ALERT_VALUE_SERVICE = "service";

  private AlertsDAO m_dao;
  private Injector m_injector;
  private AmbariManagementController m_amc;

  @Before
  public void before() throws Exception {
    m_dao = EasyMock.createNiceMock(AlertsDAO.class);

    m_injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(
        new MockModule()));

    m_amc = m_injector.getInstance(AmbariManagementController.class);

    Cluster cluster = EasyMock.createMock(Cluster.class);
    Clusters clusters = m_injector.getInstance(Clusters.class);

    expect(m_amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster(capture(EasyMock.<String>newCapture()))).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getClusterProperty(ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, "1")).andReturn("1").atLeastOnce();

    replay(m_amc, clusters, cluster);
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetClusterAsAdministrator() throws Exception {
    testGetCluster(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetClusterAsClusterAdministrator() throws Exception {
    testGetCluster(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetClusterAsClusterUser() throws Exception {
    testGetCluster(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetClusterAsViewOnlyUser() throws Exception {
    testGetCluster(TestAuthenticationFactory.createViewUser(99L));
  }

  private void testGetCluster(Authentication authentication) throws Exception {
    expect(m_dao.findAll(capture(EasyMock.<AlertCurrentRequest>newCapture()))).andReturn(getClusterMockEntities()).anyTimes();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));

    verify(m_dao);
  }

  /**
   * Test for service
   */
  @Test
  public void testGetServiceAsAdministrator() throws Exception {
    testGetService(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetServiceAsClusterAdministrator() throws Exception {
    testGetService(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetServiceAsClusterUser() throws Exception {
    testGetService(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetServiceAsViewOnlyUser() throws Exception {
    testGetService(TestAuthenticationFactory.createViewUser(99L));
  }

  private void testGetService(Authentication authentication) throws Exception {
    expect(m_dao.findAll(capture(EasyMock.<AlertCurrentRequest>newCapture()))).andReturn(
        getClusterMockEntities()).anyTimes();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").and()
        .property(AlertResourceProvider.ALERT_SERVICE).equals(ALERT_VALUE_SERVICE).toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    assertEquals(ALERT_VALUE_SERVICE, r.getPropertyValue(AlertResourceProvider.ALERT_SERVICE));

    verify(m_dao);
  }

  /**
   * Test for service
   */
  @Test
  public void testGetHostAsAdministrator() throws Exception {
    testGetHost(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetHostAsClusterAdministrator() throws Exception {
    testGetHost(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetHostAsClusterUser() throws Exception {
    testGetHost(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetHostAsViewOnlyUser() throws Exception {
    testGetHost(TestAuthenticationFactory.createViewUser(99L));
  }

  private void testGetHost(Authentication authentication) throws Exception {
    expect(m_dao.findAll(capture(EasyMock.<AlertCurrentRequest>newCapture()))).andReturn(
        getClusterMockEntities()).anyTimes();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").and()
        .property(AlertResourceProvider.ALERT_HOST).equals(ALERT_VALUE_HOSTNAME).toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    assertEquals(ALERT_VALUE_HOSTNAME, r.getPropertyValue(AlertResourceProvider.ALERT_HOST));

    verify(m_dao);
  }


  @Test
  public void testGetClusterSummaryAsAdministrator() throws Exception {
    testGetClusterSummary(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetClusterSummaryAsClusterAdministrator() throws Exception {
    testGetClusterSummary(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetClusterSummaryAsClusterUser() throws Exception {
    testGetClusterSummary(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetClusterSummaryAsViewOnlyUser() throws Exception {
    testGetClusterSummary(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * Tests that the {@link AlertSummaryRenderer} correctly transforms the alert
   * data.
   *
   * @throws Exception
   */
  private void testGetClusterSummary(Authentication authentication) throws Exception {
    expect(m_dao.findAll(capture(EasyMock.<AlertCurrentRequest>newCapture()))).andReturn(
        getMockEntitiesManyStates()).anyTimes();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID, AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL, AlertResourceProvider.ALERT_STATE,
        AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    verify(m_dao);

    AlertSummaryRenderer renderer = new AlertSummaryRenderer();
    ResultImpl result = new ResultImpl(true);
    TreeNode<Resource> resources = result.getResultTree();

    AtomicInteger alertResourceId = new AtomicInteger(1);
    for (Resource resource : results) {
      resources.addChild(resource, "Alert " + alertResourceId.getAndIncrement());
    }

    Result summary = renderer.finalizeResult(result);
    Assert.assertNotNull(summary);

    // pull out the alerts_summary child set by the renderer
    TreeNode<Resource> summaryResultTree = summary.getResultTree();
    TreeNode<Resource> summaryResources = summaryResultTree.getChild("alerts_summary");

    Resource summaryResource = summaryResources.getObject();
    AlertStateSummary alertStateSummary = (AlertStateSummary) summaryResource.getPropertyValue("alerts_summary");

    Assert.assertEquals(10, alertStateSummary.Ok.Count);
    Assert.assertEquals(2, alertStateSummary.Warning.Count);
    Assert.assertEquals(1, alertStateSummary.Critical.Count);
    Assert.assertEquals(3, alertStateSummary.Unknown.Count);
  }

  @Test
  public void testGetClusterGroupedSummaryAsAdministrator() throws Exception {
    testGetClusterGroupedSummary(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetClusterGroupedSummaryAsClusterAdministrator() throws Exception {
    testGetClusterGroupedSummary(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetClusterGroupedSummaryAsClusterUser() throws Exception {
    testGetClusterGroupedSummary(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetClusterGroupedSummaryAsViewOnlyUser() throws Exception {
    testGetClusterGroupedSummary(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * Tests that the {@link AlertSummaryGroupedRenderer} correctly transforms the
   * alert data.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void testGetClusterGroupedSummary(Authentication authentication) throws Exception {
    expect(m_dao.findAll(capture(EasyMock.<AlertCurrentRequest>newCapture()))).andReturn(
        getMockEntitiesManyStates()).anyTimes();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID, AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL, AlertResourceProvider.ALERT_STATE,
        AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP,
        AlertResourceProvider.ALERT_TEXT);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    verify(m_dao);

    AlertSummaryGroupedRenderer renderer = new AlertSummaryGroupedRenderer();
    ResultImpl result = new ResultImpl(true);
    TreeNode<Resource> resources = result.getResultTree();

    AtomicInteger alertResourceId = new AtomicInteger(1);
    for (Resource resource : results) {
      resources.addChild(resource, "Alert " + alertResourceId.getAndIncrement());
    }

    Result groupedSummary = renderer.finalizeResult(result);
    Assert.assertNotNull(groupedSummary);

    // pull out the alerts_summary child set by the renderer
    TreeNode<Resource> summaryResultTree = groupedSummary.getResultTree();
    TreeNode<Resource> summaryResources = summaryResultTree.getChild("alerts_summary_grouped");

    Resource summaryResource = summaryResources.getObject();
    List<AlertDefinitionSummary> summaryList = (List<AlertDefinitionSummary>) summaryResource.getPropertyValue("alerts_summary_grouped");
    assertEquals(4, summaryList.size());

    AlertDefinitionSummary nnSummary = null;
    AlertDefinitionSummary rmSummary = null;
    AlertDefinitionSummary hiveSummary = null;
    AlertDefinitionSummary flumeSummary = null;

    for (AlertDefinitionSummary summary : summaryList) {
      if (summary.Name.equals("hdfs_namenode")) {
        nnSummary = summary;
      } else if (summary.Name.equals("yarn_resourcemanager")) {
        rmSummary = summary;
      } else if (summary.Name.equals("hive_server")) {
        hiveSummary = summary;
      } else if (summary.Name.equals("flume_handler")) {
        flumeSummary = summary;
      }
    }

    Assert.assertNotNull(nnSummary);
    Assert.assertNotNull(rmSummary);
    Assert.assertNotNull(hiveSummary);
    Assert.assertNotNull(flumeSummary);

    Assert.assertEquals(10, nnSummary.State.Ok.Count);
    Assert.assertEquals(ALERT_VALUE_TEXT, nnSummary.State.Ok.AlertText);
    Assert.assertEquals(0, nnSummary.State.Warning.Count);
    Assert.assertEquals(0, nnSummary.State.Critical.Count);
    Assert.assertEquals(0, nnSummary.State.Unknown.Count);

    Assert.assertEquals(0, rmSummary.State.Ok.Count);
    Assert.assertEquals(2, rmSummary.State.Warning.Count);
    Assert.assertEquals(ALERT_VALUE_TEXT, rmSummary.State.Warning.AlertText);
    Assert.assertEquals(0, rmSummary.State.Critical.Count);
    Assert.assertEquals(0, rmSummary.State.Unknown.Count);

    Assert.assertEquals(0, hiveSummary.State.Ok.Count);
    Assert.assertEquals(0, hiveSummary.State.Warning.Count);
    Assert.assertEquals(1, hiveSummary.State.Critical.Count);
    Assert.assertEquals(ALERT_VALUE_TEXT, hiveSummary.State.Critical.AlertText);
    Assert.assertEquals(0, hiveSummary.State.Unknown.Count);

    Assert.assertEquals(0, flumeSummary.State.Ok.Count);
    Assert.assertEquals(0, flumeSummary.State.Warning.Count);
    Assert.assertEquals(0, flumeSummary.State.Critical.Count);
    Assert.assertEquals(3, flumeSummary.State.Unknown.Count);
    Assert.assertEquals(ALERT_VALUE_TEXT, flumeSummary.State.Unknown.AlertText);
  }

  @Test
  public void testGetClusterGroupedSummaryMaintenanceCountsAsAdministrator() throws Exception {
    testGetClusterGroupedSummaryMaintenanceCounts(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetClusterGroupedSummaryMaintenanceCountsAsClusterAdministrator() throws Exception {
    testGetClusterGroupedSummaryMaintenanceCounts(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetClusterGroupedSummaryMaintenanceCountsAsClusterUser() throws Exception {
    testGetClusterGroupedSummaryMaintenanceCounts(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetClusterGroupedSummaryMaintenanceCountsAsViewOnlyUser() throws Exception {
    testGetClusterGroupedSummaryMaintenanceCounts(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * Tests that the {@link AlertSummaryGroupedRenderer} correctly transforms the
   * alert data when it has maintenance mode alerts.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void testGetClusterGroupedSummaryMaintenanceCounts(Authentication authentication) throws Exception {
    // turn on MM for all alerts in the WARNING state
    List<AlertCurrentEntity> currents = getMockEntitiesManyStates();
    for (AlertCurrentEntity current : currents) {
      if (current.getAlertHistory().getAlertState() == AlertState.WARNING) {
        current.setMaintenanceState(MaintenanceState.ON);
      }
    }

    expect(m_dao.findAll(capture(EasyMock.<AlertCurrentRequest>newCapture()))).andReturn(
        currents).anyTimes();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL, AlertResourceProvider.ALERT_STATE,
        AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    verify(m_dao);

    AlertSummaryGroupedRenderer renderer = new AlertSummaryGroupedRenderer();
    ResultImpl result = new ResultImpl(true);
    TreeNode<Resource> resources = result.getResultTree();

    AtomicInteger alertResourceId = new AtomicInteger(1);
    for (Resource resource : results) {
      resources.addChild(resource, "Alert " + alertResourceId.getAndIncrement());
    }

    Result groupedSummary = renderer.finalizeResult(result);
    Assert.assertNotNull(groupedSummary);

    // pull out the alerts_summary child set by the renderer
    TreeNode<Resource> summaryResultTree = groupedSummary.getResultTree();
    TreeNode<Resource> summaryResources = summaryResultTree.getChild("alerts_summary_grouped");

    Resource summaryResource = summaryResources.getObject();
    List<Object> summaryList = (List<Object>) summaryResource.getPropertyValue("alerts_summary_grouped");
    assertEquals(4, summaryList.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testResponseIsPaginated() throws Exception {
    expect(m_dao.findAll(EasyMock.anyObject(AlertCurrentRequest.class))).andReturn(
        getClusterMockEntities()).atLeastOnce();

    expect(m_dao.getCount(EasyMock.anyObject(Predicate.class))).andReturn(0).atLeastOnce();

    replay(m_dao);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    Set<String> requestProperties = new HashSet<>();
    requestProperties.add(AlertResourceProvider.ALERT_ID);
    requestProperties.add(AlertResourceProvider.ALERT_DEFINITION_NAME);

    Request request = PropertyHelper.getReadRequest(requestProperties);

    Predicate predicate = new PredicateBuilder().property(AlertResourceProvider.ALERT_CLUSTER_NAME).equals(
        "c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    QueryResponse response = provider.queryForResources(request, predicate);

    // since the request didn't have paging, then this should be false
    assertFalse(response.isPagedResponse());

    // add a paged request
    PageRequest pageRequest = new PageRequestImpl(StartingPoint.Beginning, 5, 10, predicate, null);
    request = PropertyHelper.getReadRequest(requestProperties, null, null, pageRequest, null);
    response = provider.queryForResources(request, predicate);

    // now the request has paging
    assertTrue(response.isPagedResponse());

    verify(m_dao);
  }

  /**
   * @return
   */
  private AlertResourceProvider createProvider() {
    return new AlertResourceProvider(m_amc);
  }

  /**
   * @return
   */
  private List<AlertCurrentEntity> getClusterMockEntities() throws Exception {
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setAlertId(Long.valueOf(1000L));
    current.setHistoryId(ALERT_VALUE_ID);
    current.setLatestTimestamp(Long.valueOf(1L));
    current.setOriginalTimestamp(Long.valueOf(2L));

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertId(ALERT_VALUE_ID);
    history.setAlertInstance(null);
    history.setAlertLabel(ALERT_VALUE_LABEL);
    history.setAlertState(AlertState.OK);
    history.setAlertText(ALERT_VALUE_TEXT);
    history.setAlertTimestamp(ALERT_VALUE_TIMESTAMP);
    history.setClusterId(Long.valueOf(1L));
    history.setComponentName(ALERT_VALUE_COMPONENT);
    history.setHostName(ALERT_VALUE_HOSTNAME);
    history.setServiceName(ALERT_VALUE_SERVICE);

    ResourceEntity clusterResourceEntity = new ResourceEntity();
    clusterResourceEntity.setId(4L);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(2L);
    clusterEntity.setResource(clusterResourceEntity);

    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setCluster(clusterEntity);

    history.setAlertDefinition(definition);
    current.setAlertHistory(history);

    return Arrays.asList(current);
  }

  /**
   * Gets a bunch of alerts with various values for state and timestamp.
   *
   * @return
   */
  private List<AlertCurrentEntity> getMockEntitiesManyStates() throws Exception {
    // yesterday
    AtomicLong timestamp = new AtomicLong(System.currentTimeMillis() - 86400000);
    AtomicLong alertId = new AtomicLong(1);

    int ok = 10;
    int warning = 2;
    int critical = 1;
    int unknown = 3;
    int total = ok + warning + critical + unknown;

    List<AlertCurrentEntity> currents = new ArrayList<>(total);

    for (int i = 0; i < total; i++) {
      AlertState state = AlertState.OK;
      String service = "HDFS";
      String component = "NAMENODE";
      String definitionName = "hdfs_namenode";

      if (i >= ok && i < ok + warning) {
        state = AlertState.WARNING;
        service = "YARN";
        component = "RESOURCEMANAGER";
        definitionName = "yarn_resourcemanager";
      } else if (i >= ok + warning & i < ok + warning + critical) {
        state = AlertState.CRITICAL;
        service = "HIVE";
        component = "HIVE_SERVER";
        definitionName = "hive_server";
      } else if (i >= ok + warning + critical) {
        state = AlertState.UNKNOWN;
        service = "FLUME";
        component = "FLUME_HANDLER";
        definitionName = "flume_handler";
      }

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertId(alertId.getAndIncrement());
      current.setOriginalTimestamp(timestamp.getAndAdd(10000));
      current.setLatestTimestamp(timestamp.getAndAdd(10000));
      current.setLatestText(ALERT_VALUE_TEXT);

      AlertHistoryEntity history = new AlertHistoryEntity();
      history.setAlertId(alertId.getAndIncrement());
      history.setAlertInstance(null);
      history.setAlertLabel(ALERT_VALUE_LABEL);
      history.setAlertState(state);
      history.setAlertText(ALERT_VALUE_TEXT);
      history.setAlertTimestamp(current.getOriginalTimestamp());
      history.setClusterId(Long.valueOf(1L));
      history.setComponentName(component);
      history.setHostName(ALERT_VALUE_HOSTNAME);
      history.setServiceName(service);

      ResourceEntity clusterResourceEntity = new ResourceEntity();
      clusterResourceEntity.setId(4L);

      ClusterEntity clusterEntity = new ClusterEntity();
      clusterEntity.setClusterId(2L);
      clusterEntity.setResource(clusterResourceEntity);

      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionId(Long.valueOf(i));
      definition.setDefinitionName(definitionName);
      definition.setCluster(clusterEntity);
      history.setAlertDefinition(definition);
      current.setAlertHistory(history);
      currents.add(current);
    }

    return currents;
  }


  /**
   *
   */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(EntityManager.class).toInstance(EasyMock.createMock(EntityManager.class));
      binder.bind(AlertsDAO.class).toInstance(m_dao);
      binder.bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementController.class));
      binder.bind(DBAccessor.class).to(DBAccessorImpl.class);

      Clusters clusters = EasyMock.createNiceMock(Clusters.class);
      Configuration configuration = EasyMock.createNiceMock(Configuration.class);

      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(Configuration.class).toInstance(configuration);

      expect(configuration.getDatabaseUrl()).andReturn(JDBC_IN_MEMORY_URL).anyTimes();
      expect(configuration.getDatabaseDriver()).andReturn(JDBC_IN_MEMORY_DRIVER).anyTimes();
      expect(configuration.getDatabaseUser()).andReturn("sa").anyTimes();
      expect(configuration.getDatabasePassword()).andReturn("").anyTimes();
      expect(configuration.getAlertEventPublisherCorePoolSize()).andReturn(Integer.valueOf(Configuration.ALERTS_EXECUTION_SCHEDULER_THREADS_CORE_SIZE.getDefaultValue())).anyTimes();
      expect(configuration.getAlertEventPublisherMaxPoolSize()).andReturn(Integer.valueOf(Configuration.ALERTS_EXECUTION_SCHEDULER_THREADS_MAX_SIZE.getDefaultValue())).anyTimes();
      expect(configuration.getAlertEventPublisherWorkerQueueSize()).andReturn(Integer.valueOf(Configuration.ALERTS_EXECUTION_SCHEDULER_WORKER_QUEUE_SIZE.getDefaultValue())).anyTimes();
      expect(configuration.getMasterKeyLocation()).andReturn(new File("/test")).anyTimes();
      expect(configuration.getTemporaryKeyStoreRetentionMinutes()).andReturn(2l).anyTimes();
      expect(configuration.isActivelyPurgeTemporaryKeyStore()).andReturn(true).anyTimes();
      expect(configuration.getDatabaseSchema()).andReturn(Configuration.DEFAULT_DERBY_SCHEMA).anyTimes();
      replay(configuration);
    }
  }
}
