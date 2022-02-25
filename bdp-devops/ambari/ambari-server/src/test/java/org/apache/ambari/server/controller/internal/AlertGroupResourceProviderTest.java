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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.controller.AlertDefinitionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertTarget;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.alert.TargetType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
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
 * {@link AlertGroupResourceProvider} tests.
 */
public class AlertGroupResourceProviderTest {

  private static final Long ALERT_GROUP_ID = Long.valueOf(28);
  private static final String ALERT_GROUP_NAME = "Important Alerts";
  private static final long ALERT_GROUP_CLUSTER_ID = 1L;
  private static final String ALERT_GROUP_CLUSTER_NAME = "c1";

  private static final Long ALERT_TARGET_ID = Long.valueOf(28);
  private static final String ALERT_TARGET_NAME = "The Administrators";
  private static final String ALERT_TARGET_DESC = "Admins and Others";
  private static final String ALERT_TARGET_TYPE = TargetType.EMAIL.name();

  private static final Long ALERT_DEF_ID = 10L;
  private static final String ALERT_DEF_NAME = "Mock Definition";
  private static final String ALERT_DEF_LABEL = "Mock Label";
  private static final String ALERT_DEF_DESCRIPTION = "Mock Description";

  private static String DEFINITION_UUID = UUID.randomUUID().toString();

  private AlertDispatchDAO m_dao;
  private AlertDefinitionDAO m_definitionDao;
  private Injector m_injector;

  private AmbariManagementController m_amc;
  private Clusters m_clusters;
  private Cluster m_cluster;

  @Before
  public void before() throws Exception {
    m_dao = createMock(AlertDispatchDAO.class);
    m_definitionDao = createMock(AlertDefinitionDAO.class);

    m_amc = createMock(AmbariManagementController.class);
    m_clusters = createMock(Clusters.class);
    m_cluster = createMock(Cluster.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    assertNotNull(m_injector);

    expect(m_amc.getClusters()).andReturn(m_clusters).anyTimes();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(m_cluster).anyTimes();
    expect(m_clusters.getClusterById(1L)).andReturn(m_cluster).anyTimes();
    expect(m_cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(m_cluster.getResourceId()).andReturn(4L).anyTimes();
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
  private void testGetResourcesNoPredicate(Authentication authentication) throws Exception {
    AlertGroupResourceProvider provider = createProvider(m_amc);

    Request request = PropertyHelper.getReadRequest("AlertGroup/cluster_name",
        "AlertGroup/id");

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> results = provider.getResources(request, null);

    assertEquals(0, results.size());
  }

  @Test
  public void testGetResourcesClusterPredicateAsAdministrator() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testGetResourcesClusterPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testGetResourcesClusterPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testGetResourcesClusterPredicateAsClusterUser() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createClusterUser(), true);
  }

  @Test
  public void testGetResourcesClusterPredicateAsViewUser() throws Exception {
    testGetResourcesClusterPredicate(TestAuthenticationFactory.createViewUser(99L), false);
  }

  /**
   * @throws Exception
   */
  private void testGetResourcesClusterPredicate(Authentication authentication, boolean expectResults) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertGroupResourceProvider.ALERT_GROUP_ID,
        AlertGroupResourceProvider.ALERT_GROUP_NAME,
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        AlertGroupResourceProvider.ALERT_GROUP_DEFAULT);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAllGroups(ALERT_GROUP_CLUSTER_ID)).andReturn(
        getMockEntities());

    replay(m_amc, m_clusters, m_cluster, m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(expectResults ? 1 : 0, results.size());

    if(expectResults) {
      Resource r = results.iterator().next();

      assertEquals(ALERT_GROUP_NAME,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_NAME));

      assertEquals(ALERT_GROUP_ID,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_ID));

      assertEquals(ALERT_GROUP_CLUSTER_NAME,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME));

      // verify definitions do not come back when not requested
      assertNull(r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_DEFINITIONS));

      // verify alerts do not come back when not requested
      assertNull(r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_TARGETS));
    }

    verify(m_amc, m_clusters, m_cluster, m_dao);
  }

  @Test
  public void testGetResourcesAllPropertiesAsAdministrator() throws Exception {
    testGetResourcesAllProperties(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testGetResourcesAllPropertiesAsClusterAdministrator() throws Exception {
    testGetResourcesAllProperties(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testGetResourcesAllPropertiesAsServiceAdministrator() throws Exception {
    testGetResourcesAllProperties(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testGetResourcesAllPropertiesAsClusterUser() throws Exception {
    testGetResourcesAllProperties(TestAuthenticationFactory.createClusterUser(), true);
  }

  @Test
  public void testGetResourcesAllPropertiesAsViewUser() throws Exception {
    testGetResourcesAllProperties(TestAuthenticationFactory.createViewUser(99L), false);
  }

  /**
   * @throws Exception
   */
  private void testGetResourcesAllProperties(Authentication authentication, boolean expectResults) throws Exception {
    Request request = PropertyHelper.getReadRequest();

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAllGroups(ALERT_GROUP_CLUSTER_ID)).andReturn(
        getMockEntities());

    replay(m_amc, m_clusters, m_cluster, m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(expectResults ? 1 : 0, results.size());

    if(expectResults) {
      Resource r = results.iterator().next();

      assertEquals(ALERT_GROUP_NAME,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_NAME));

      assertEquals(ALERT_GROUP_ID,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_ID));

      assertEquals(ALERT_GROUP_CLUSTER_NAME,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME));


      // verify definitions and targets come back when requested
      List<AlertDefinitionResponse> definitions = (List<AlertDefinitionResponse>) r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_DEFINITIONS);
      List<?> targets = (List<?>) r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_TARGETS);

      assertNotNull(definitions);
      assertEquals(1, definitions.size());
      assertEquals(ALERT_DEF_NAME, definitions.get(0).getName());
      assertEquals(SourceType.METRIC, definitions.get(0).getSourceType());
      assertNotNull(targets);
      assertEquals(1, targets.size());
    }

    verify(m_amc, m_clusters, m_cluster, m_dao);
  }

  @Test
  public void testGetSingleResourceAsAdministrator() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testGetSingleResourceAsClusterAdministrator() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testGetSingleResourceAsServiceAdministrator() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testGetSingleResourceAsClusterUser() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createClusterUser(), true);
  }

  @Test(expected = AuthorizationException.class)
  public void testGetSingleResourceAsViewUser() throws Exception {
    testGetSingleResource(TestAuthenticationFactory.createViewUser(99L), false);
  }
  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void testGetSingleResource(Authentication authentication, boolean expectResults) throws Exception {
    Request request = PropertyHelper.getReadRequest();

    AmbariManagementController amc = createMock(AmbariManagementController.class);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    expect(m_dao.findGroupById(ALERT_GROUP_ID.longValue())).andReturn(
        getMockEntities().get(0));

    expect(amc.getClusters()).andReturn(m_clusters).atLeastOnce();

    replay(amc, m_dao, m_clusters, m_cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(expectResults ? 1 : 0, results.size());

    if(expectResults) {
      Resource r = results.iterator().next();

      assertEquals(ALERT_GROUP_NAME,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_NAME));

      assertEquals(ALERT_GROUP_ID,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_ID));

      assertEquals(ALERT_GROUP_CLUSTER_NAME,
          r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME));

      // verify definitions and targets are returned on single instances
      List<AlertDefinitionResponse> definitions = (List<AlertDefinitionResponse>) r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_DEFINITIONS);
      List<AlertTarget> targets = (List<AlertTarget>) r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_TARGETS);

      assertNotNull(definitions);
      assertNotNull(targets);

      assertEquals(1, definitions.size());
      assertEquals(ALERT_DEF_NAME, definitions.get(0).getName());
      assertEquals(SourceType.METRIC, definitions.get(0).getSourceType());

      assertEquals(1, targets.size());
      assertEquals(ALERT_TARGET_NAME, targets.get(0).getName());
    }

    verify(amc, m_dao);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsClusterUser() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsViewUser() throws Exception {
    testCreateResources(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * @throws Exception
   */
  public void testCreateResources(Authentication authentication) throws Exception {
    Capture<List<AlertGroupEntity>> listCapture = EasyMock.newCapture();

    // the definition IDs to associate with the group
    List<Long> definitionIds = new ArrayList<>();
    definitionIds.add(ALERT_DEF_ID);

    // the target IDs to associate with the group
    List<Long> targetIds = new ArrayList<>();
    targetIds.add(ALERT_TARGET_ID);

    // definition entities to return from DAO
    List<AlertDefinitionEntity> definitionEntities = new ArrayList<>();
    definitionEntities.addAll(getMockDefinitions());

    // target entities to return from DAO
    List<AlertTargetEntity> targetEntities = new ArrayList<>();
    targetEntities.addAll(getMockTargets());

    // expect create group
    m_dao.createGroups(capture(listCapture));
    expectLastCall().once();

    // expect target entity lookup for association
    expect(m_dao.findTargetsById(EasyMock.eq(targetIds))).andReturn(
        targetEntities).times(1);

    // expect definition entity lookup for association
    expect(m_definitionDao.findByIds(definitionIds)).andReturn(
        definitionEntities).times(1);

    replay(m_amc, m_clusters, m_cluster, m_dao, m_definitionDao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME,
        ALERT_GROUP_NAME);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        ALERT_GROUP_CLUSTER_NAME);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_DEFINITIONS,
        definitionIds);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_TARGETS, targetIds);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    assertTrue(listCapture.hasCaptured());
    AlertGroupEntity entity = listCapture.getValue().get(0);
    assertNotNull(entity);

    assertEquals(ALERT_GROUP_NAME, entity.getGroupName());
    assertEquals(ALERT_GROUP_CLUSTER_ID,
        entity.getClusterId().longValue());

    verify(m_amc, m_clusters, m_cluster, m_dao, m_definitionDao);
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsClusterUser() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsViewUser() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void testUpdateResources(Authentication authentication) throws Exception {
    Capture<AlertGroupEntity> entityCapture = EasyMock.newCapture();

    // the definition IDs to associate with the group
    List<Long> definitionIds = new ArrayList<>();
    definitionIds.add(ALERT_DEF_ID);

    // the target IDs to associate with the group
    List<Long> targetIds = new ArrayList<>();
    targetIds.add(ALERT_TARGET_ID);

    // definition entities to return from DAO
    List<AlertDefinitionEntity> definitionEntities = new ArrayList<>();
    definitionEntities.addAll(getMockDefinitions());

    // target entities to return from DAO
    List<AlertTargetEntity> targetEntities = new ArrayList<>();
    targetEntities.addAll(getMockTargets());

    m_dao.createGroups(EasyMock.anyObject());
    expectLastCall().times(1);

    AlertGroupEntity group = new AlertGroupEntity();
    expect(m_dao.findGroupById(ALERT_GROUP_ID)).andReturn(
        group).times(1);

    expect(m_dao.merge(capture(entityCapture))).andReturn(group).once();

    // expect target entity lookup for association
    expect(m_dao.findTargetsById(EasyMock.eq(targetIds))).andReturn(
        targetEntities).once();

    // expect definition entity lookup for association
    expect(m_definitionDao.findByIds(definitionIds)).andReturn(
        definitionEntities).once();

    replay(m_amc, m_clusters, m_cluster, m_dao, m_definitionDao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);
    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME,
        ALERT_GROUP_NAME);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        ALERT_GROUP_CLUSTER_NAME);

    Request request = PropertyHelper.getCreateRequest(
        Collections.singleton(requestProps), null);

    provider.createResources(request);

    // create new properties, and include the ID since we're not going through
    // a service layer which would add it for us automatically
    requestProps = new HashMap<>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_ID,
        ALERT_GROUP_ID.toString());

    String newName = ALERT_GROUP_NAME + " Foo";
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME, newName);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_DEFINITIONS,
        definitionIds);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_TARGETS, targetIds);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    request = PropertyHelper.getUpdateRequest(requestProps, null);
    provider.updateResources(request, predicate);

    assertTrue(entityCapture.hasCaptured());

    AlertGroupEntity entity = entityCapture.getValue();
    assertEquals(newName, entity.getGroupName());
    verify(m_amc, m_clusters, m_cluster, m_dao, m_definitionDao);
  }

  @Test
  public void testUpdateDefaultGroupAsAdministrator() throws Exception {
     testUpdateDefaultGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateDefaultGroupAsClusterAdministrator() throws Exception {
    testUpdateDefaultGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateDefaultGroupAsServiceAdministrator() throws Exception {
    testUpdateDefaultGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateDefaultGroupAsClusterUser() throws Exception {
    testUpdateDefaultGroup(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateDefaultGroupAsViewUser() throws Exception {
    testUpdateDefaultGroup(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * Tests that updating a default group doesn't change read-only properties
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private  void testUpdateDefaultGroup(Authentication authentication) throws Exception {
    Capture<AlertGroupEntity> entityCapture = EasyMock.newCapture();

    // the definition IDs to associate with the group
    List<Long> definitionIds = new ArrayList<>();
    definitionIds.add(ALERT_DEF_ID);

    // the target IDs to associate with the group
    List<Long> targetIds = new ArrayList<>();
    targetIds.add(ALERT_TARGET_ID);

    // definition entities to return from DAO
    List<AlertDefinitionEntity> definitionEntities = new ArrayList<>();
    definitionEntities.addAll(getMockDefinitions());

    // target entities to return from DAO
    List<AlertTargetEntity> newTargetEntities = new ArrayList<>();
    newTargetEntities.addAll(getMockTargets());

    Set<AlertTargetEntity> mockTargets2 = getMockTargets();
    AlertTargetEntity target2 = mockTargets2.iterator().next();
    target2.setTargetId(29L);

    newTargetEntities.add(target2);

    AlertGroupEntity group = new AlertGroupEntity();
    group.setDefault(true);
    group.setClusterId(1L);
    group.setGroupName(ALERT_GROUP_NAME);
    group.setAlertDefinitions(getMockDefinitions());
    group.setAlertTargets(getMockTargets());

    expect(m_dao.findGroupById(ALERT_GROUP_ID)).andReturn(group).times(1);
    expect(m_dao.merge(capture(entityCapture))).andReturn(group).once();

    // expect target entity lookup for association
    List<Long> newTargets = Arrays.asList(28L, 29L);
    expect(m_dao.findTargetsById(EasyMock.eq(newTargets))).andReturn(
        newTargetEntities).once();

    replay(m_dao, m_definitionDao, m_amc, m_clusters, m_cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);

    // create new properties, and include the ID since we're not going through
    // a service layer which would add it for us automatically
    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_ID,
        ALERT_GROUP_ID.toString());

    // try to change the name (it should not work)
    String newName = ALERT_GROUP_NAME + " Foo";
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME, newName);

    // try to change the definitions (it should not work)
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_DEFINITIONS,
        new ArrayList<Long>());

    // try to change the targets (it should work)
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_TARGETS,
        newTargets);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    Request request = PropertyHelper.getUpdateRequest(requestProps, null);
    provider.updateResources(request, predicate);

    assertTrue(entityCapture.hasCaptured());

    AlertGroupEntity entity = entityCapture.getValue();
    assertEquals(ALERT_GROUP_NAME, entity.getGroupName());
    assertEquals(2, entity.getAlertTargets().size());
    assertEquals(1, entity.getAlertDefinitions().size());

    verify(m_dao, m_definitionDao);
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsClusterUser() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsViewUser() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * @throws Exception
   */
  private void testDeleteResources(Authentication authentication) throws Exception {
    Capture<AlertGroupEntity> entityCapture = EasyMock.newCapture();
    Capture<List<AlertGroupEntity>> listCapture = EasyMock.newCapture();

    m_dao.createGroups(capture(listCapture));
    expectLastCall();

    replay(m_amc, m_clusters, m_cluster, m_dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME,
        ALERT_GROUP_NAME);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        ALERT_GROUP_CLUSTER_NAME);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    assertTrue(listCapture.hasCaptured());
    AlertGroupEntity entity = listCapture.getValue().get(0);
    assertNotNull(entity);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    // everything is mocked, there is no DB
    entity.setGroupId(ALERT_GROUP_ID);

    resetToStrict(m_dao);
    expect(m_dao.findGroupById(ALERT_GROUP_ID.longValue())).andReturn(entity).anyTimes();
    m_dao.remove(capture(entityCapture));
    expectLastCall();
    replay(m_dao);

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    AlertGroupEntity entity1 = entityCapture.getValue();
    assertEquals(ALERT_GROUP_ID, entity1.getGroupId());

    verify(m_amc, m_clusters, m_cluster, m_dao);
  }

  @Test
  public void testDeleteDefaultGroupAsAdministrator() throws Exception {
    testDeleteDefaultGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteDefaultGroupAsClusterAdministrator() throws Exception {
    testDeleteDefaultGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteDefaultGroupAsServiceAdministrator() throws Exception {
    testDeleteDefaultGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteDefaultGroupAsClusterUser() throws Exception {
    testDeleteDefaultGroup(TestAuthenticationFactory.createClusterUser());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteDefaultGroupAsViewUser() throws Exception {
    testDeleteDefaultGroup(TestAuthenticationFactory.createViewUser(99L));
  }

  /**
   * Tests that a default group cannot be deleted via the resource provider.
   *
   * @throws Exception
   */
  private void testDeleteDefaultGroup(Authentication authentication) throws Exception {
    AlertGroupEntity group = new AlertGroupEntity();
    group.setGroupId(ALERT_GROUP_ID);
    group.setDefault(true);
    group.setGroupName(ALERT_GROUP_NAME);
    group.setAlertDefinitions(getMockDefinitions());
    group.setAlertTargets(getMockTargets());

    resetToStrict(m_dao);
    expect(m_dao.findGroupById(ALERT_GROUP_ID)).andReturn(group).anyTimes();

    replay(m_dao, m_amc, m_clusters, m_cluster);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertGroupResourceProvider provider = createProvider(m_amc);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
    verify(m_dao, m_amc);
  }

  /**
   * @param amc
   * @return
   */
  private AlertGroupResourceProvider createProvider(
      AmbariManagementController amc) {

    return new AlertGroupResourceProvider(amc);
  }

  /**
   * @return
   */
  private List<AlertGroupEntity> getMockEntities() throws Exception {
    AlertGroupEntity entity = new AlertGroupEntity();
    entity.setGroupId(ALERT_GROUP_ID);
    entity.setGroupName(ALERT_GROUP_NAME);
    entity.setClusterId(ALERT_GROUP_CLUSTER_ID);
    entity.setDefault(false);

    entity.setAlertTargets(getMockTargets());
    entity.setAlertDefinitions(getMockDefinitions());
    return Arrays.asList(entity);
  }

  /**
   * Gets some mock {@link AlertDefinitionEntity} instances.
   *
   * @return
   * @throws Exception
   */
  private Set<AlertDefinitionEntity> getMockDefinitions() throws Exception {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    entity.setClusterId(Long.valueOf(1L));
    entity.setComponentName(null);
    entity.setDefinitionId(ALERT_DEF_ID);
    entity.setDefinitionName(ALERT_DEF_NAME);
    entity.setLabel(ALERT_DEF_LABEL);
    entity.setDescription(ALERT_DEF_DESCRIPTION);
    entity.setEnabled(true);
    entity.setHash(DEFINITION_UUID);
    entity.setScheduleInterval(Integer.valueOf(2));
    entity.setServiceName(null);
    entity.setSourceType(SourceType.METRIC);
    entity.setSource("{\"type\" : \"METRIC\"}");

    Set<AlertDefinitionEntity> definitions = new HashSet<>();
    definitions.add(entity);

    return definitions;
  }

  /**
   * Gets some mock {@link AlertTargetEntity} instances.
   *
   * @return
   */
  private Set<AlertTargetEntity> getMockTargets() throws Exception {
    AlertTargetEntity entity = new AlertTargetEntity();
    entity.setTargetId(ALERT_TARGET_ID);
    entity.setDescription(ALERT_TARGET_DESC);
    entity.setTargetName(ALERT_TARGET_NAME);
    entity.setNotificationType(ALERT_TARGET_TYPE);

    Set<AlertTargetEntity> targets = new HashSet<>();
    targets.add(entity);

    return targets;
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
      binder.bind(AlertDispatchDAO.class).toInstance(m_dao);
      binder.bind(AlertDefinitionDAO.class).toInstance(m_definitionDao);
      binder.bind(Clusters.class).toInstance(m_clusters);
      binder.bind(Cluster.class).toInstance(m_cluster);
      binder.bind(ActionMetadata.class);
    }
  }
}
