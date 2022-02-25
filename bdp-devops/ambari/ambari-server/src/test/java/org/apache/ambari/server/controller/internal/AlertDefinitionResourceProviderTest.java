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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.alert.MetricSource;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * AlertDefinition tests
 */
public class AlertDefinitionResourceProviderTest {

  private AlertDefinitionDAO dao = null;
  private AlertDefinitionHash definitionHash = null;
  private AlertDefinitionFactory m_factory = new AlertDefinitionFactory();
  private Injector m_injector;

  private static String DEFINITION_UUID = UUID.randomUUID().toString();

  @Before
  public void before() {
    dao = createStrictMock(AlertDefinitionDAO.class);
    definitionHash = createNiceMock(AlertDefinitionHash.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_injector.injectMembers(m_factory);
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
  public void testGetResourcesNoPredicate() throws Exception {
    AlertDefinitionResourceProvider provider = createProvider(null);

    Request request = PropertyHelper.getReadRequest("AlertDefinition/cluster_name",
        "AlertDefinition/id");

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
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_LABEL);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();

    expect(dao.findAll(1L)).andReturn(getMockEntities());

    replay(amc, clusters, cluster, dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertDefinitionResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(expectResults ? 1 : 0, results.size());

    if(expectResults) {
      Resource r = results.iterator().next();

      Assert.assertEquals("my_def", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));

      Assert.assertEquals("Mock Label",
          r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_LABEL));
    }

    verify(amc, clusters, cluster, dao);
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
  private void testGetSingleResource(Authentication authentication) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_LABEL,
        AlertDefinitionResourceProvider.ALERT_DEF_DESCRIPTION,
        AlertDefinitionResourceProvider.ALERT_DEF_IGNORE_HOST,
        AlertDefinitionResourceProvider.ALERT_DEF_SOURCE,
        AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE,
        AlertDefinitionResourceProvider.ALERT_DEF_HELP_URL);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1")
          .and().property(AlertDefinitionResourceProvider.ALERT_DEF_ID).equals("1").toPredicate();

    expect(dao.findById(1L)).andReturn(getMockEntities().get(0));

    replay(amc, clusters, cluster, dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertDefinitionResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals("my_def", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));

    Assert.assertEquals(
        SourceType.METRIC.name(),
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE));

    Source source = getMockSource();
    String okJson = source.getReporting().getOk().getText();
    Object reporting = r.getPropertyValue("AlertDefinition/source/reporting");

    Assert.assertTrue(reporting.toString().contains(okJson));

    Assert.assertEquals("Mock Label",
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_LABEL));

    Assert.assertEquals(
        "Mock Description",
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_DESCRIPTION));

    Assert.assertEquals(
        Boolean.FALSE,
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_IGNORE_HOST));

    Assert.assertEquals(
            "http://test-help-url",
            r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_HELP_URL));

    Assert.assertNotNull(r.getPropertyValue("AlertDefinition/source/type"));
  }

  @Test
  public void testGetResourcesAssertSourceTypeAsAdministrator() throws Exception {
    testGetResourcesAssertSourceType(TestAuthenticationFactory.createAdministrator(), true);
  }

  @Test
  public void testGetResourcesAssertSourceTypeAsClusterAdministrator() throws Exception {
    testGetResourcesAssertSourceType(TestAuthenticationFactory.createClusterAdministrator(), true);
  }

  @Test
  public void testGetResourcesAssertSourceTypeAsServiceAdministrator() throws Exception {
    testGetResourcesAssertSourceType(TestAuthenticationFactory.createServiceAdministrator(), true);
  }

  @Test
  public void testGetResourcesAssertSourceTypeAsClusterUser() throws Exception {
    testGetResourcesAssertSourceType(TestAuthenticationFactory.createClusterUser(), true);
  }

  @Test
  public void testGetResourcesAssertSourceTypeAsViewUser() throws Exception {
    testGetResourcesAssertSourceType(TestAuthenticationFactory.createViewUser(99L), false);
  }

/**
   * Tests that the source structure returned has the entire set of
   * subproperties on it (such as reporting)
   *
   * @throws Exception
   */
  private void testGetResourcesAssertSourceType(Authentication authentication, boolean expectResults) throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_LABEL,
        AlertDefinitionResourceProvider.ALERT_DEF_DESCRIPTION,
        AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();

    expect(dao.findAll(1L)).andReturn(getMockEntities()).atLeastOnce();

    replay(amc, clusters, cluster, dao);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertDefinitionResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(expectResults ? 1 : 0, results.size());

    if(expectResults) {
      Resource resource = results.iterator().next();

      Assert.assertEquals("my_def",
          resource.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));

      Map<?, ?> reporting = (Map<?, ?>) resource.getPropertyValue("AlertDefinition/source/reporting");

      Assert.assertTrue(reporting.containsKey("ok"));
      Assert.assertTrue(reporting.containsKey("critical"));
    }

    verify(amc, clusters, cluster, dao);

    // make another request, this time without the source and ensure that no
    // source properties come back
    request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME);

    results = provider.getResources(request, predicate);

    if(!results.isEmpty()) {
      Resource resource = results.iterator().next();

      Assert.assertEquals(
          "my_def",
          resource.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));

      Map<?, ?> reporting = (Map<?, ?>) resource.getPropertyValue("AlertDefinition/source/reporting");

      Assert.assertNull(reporting);
    }
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
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
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Capture<AlertDefinitionEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    // creating a single definition should invalidate hosts of the definition
    expect(
        definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
      new HashSet<>()).once();

    replay(amc, clusters, cluster, dao, definitionHash);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Gson gson = m_factory.getGson();
    MetricSource source = (MetricSource)getMockSource();
    AlertDefinitionResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME,
        "HDFS");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_LABEL,
        "Mock Label (Create)");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_DESCRIPTION,
        "Mock Description (Create)");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE,
        SourceType.METRIC.name());

    // JMX
    requestProps.put("AlertDefinition/source/jmx/value",
        source.getJmxInfo().getValue().toString());
    requestProps.put("AlertDefinition/source/jmx/property_list",
        source.getJmxInfo().getPropertyList());

    // URI
    requestProps.put("AlertDefinition/source/uri/http",
        source.getUri().getHttpUri());
    requestProps.put("AlertDefinition/source/uri/https",
        source.getUri().getHttpsUri());
    requestProps.put("AlertDefinition/source/uri/https_property",
        source.getUri().getHttpsProperty());
    requestProps.put("AlertDefinition/source/uri/https_property_value",
        source.getUri().getHttpsPropertyValue());

    // reporting
    requestProps.put("AlertDefinition/source/reporting/critical/text",
        source.getReporting().getCritical().getText());
    requestProps.put("AlertDefinition/source/reporting/critical/value",
        source.getReporting().getCritical().getValue());
    requestProps.put("AlertDefinition/source/reporting/ok/text",
        source.getReporting().getOk().getText());
    requestProps.put("AlertDefinition/source/reporting/warning/text",
        source.getReporting().getWarning().getText());
    requestProps.put("AlertDefinition/source/reporting/warning/value",
        source.getReporting().getWarning().getValue());
    requestProps.put("AlertDefinition/source/reporting/units", "Gigabytes");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Assert.assertEquals(Long.valueOf(1), entity.getClusterId());
    Assert.assertNull(entity.getComponentName());
    Assert.assertEquals("my_def", entity.getDefinitionName());
    Assert.assertTrue(entity.getEnabled());
    Assert.assertNotNull(entity.getHash());
    Assert.assertEquals(Integer.valueOf(1), entity.getScheduleInterval());
    Assert.assertEquals(Scope.ANY, entity.getScope());
    Assert.assertEquals("HDFS", entity.getServiceName());
    Assert.assertEquals(SourceType.METRIC, entity.getSourceType());
    Assert.assertEquals("Mock Label (Create)", entity.getLabel());
    Assert.assertEquals("Mock Description (Create)", entity.getDescription());
    Assert.assertEquals(false, entity.isHostIgnored());

    // verify Source
    Assert.assertNotNull(entity.getSource());
    MetricSource actualSource = gson.fromJson(entity.getSource(),
        MetricSource.class);

    Assert.assertNotNull(actualSource);

    assertEquals(source.getReporting().getOk().getText(),
        actualSource.getReporting().getOk().getText());

    assertEquals(source.getReporting().getWarning().getText(),
        actualSource.getReporting().getWarning().getText());

    assertEquals(source.getReporting().getCritical().getText(),
        actualSource.getReporting().getCritical().getText());

    assertEquals("Gigabytes", actualSource.getReporting().getUnits());

    Assert.assertNotNull(source.getUri().getHttpUri());
    Assert.assertNotNull(source.getUri().getHttpsUri());

    assertEquals(source.getUri().getHttpUri(),
        actualSource.getUri().getHttpUri());

    assertEquals(source.getUri().getHttpsUri(),
        actualSource.getUri().getHttpsUri());

    verify(amc, clusters, cluster, dao);
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
  private void testUpdateResources(Authentication authentication) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).atLeastOnce();

    Capture<AlertDefinitionEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    // updateing a single definition should invalidate hosts of the definition
    expect(
        definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
      new HashSet<>()).atLeastOnce();

    replay(amc, clusters, cluster, dao, definitionHash);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    MetricSource source = (MetricSource) getMockSource();

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_LABEL, "Label");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_DESCRIPTION,"Description");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_ENABLED,
        Boolean.TRUE.toString());

    // JMX
    requestProps.put("AlertDefinition/source/jmx/value",
        source.getJmxInfo().getValue().toString());
    requestProps.put("AlertDefinition/source/jmx/property_list",
        source.getJmxInfo().getPropertyList());

    // URI
    requestProps.put("AlertDefinition/source/uri/http",
        source.getUri().getHttpUri());
    requestProps.put("AlertDefinition/source/uri/https",
        source.getUri().getHttpsUri());
    requestProps.put("AlertDefinition/source/uri/https_property",
        source.getUri().getHttpsProperty());
    requestProps.put("AlertDefinition/source/uri/https_property_value",
        source.getUri().getHttpsPropertyValue());

    // reporting
    requestProps.put("AlertDefinition/source/reporting/critical/text",
        source.getReporting().getCritical().getText());
    requestProps.put("AlertDefinition/source/reporting/critical/value",
        source.getReporting().getCritical().getValue());
    requestProps.put("AlertDefinition/source/reporting/ok/text",
        source.getReporting().getOk().getText());
    requestProps.put("AlertDefinition/source/reporting/warning/text",
        source.getReporting().getWarning().getText());
    requestProps.put("AlertDefinition/source/reporting/warning/value",
        source.getReporting().getWarning().getValue());

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    AlertDefinitionResourceProvider provider = createProvider(amc);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate p = new PredicateBuilder().property(AlertDefinitionResourceProvider.ALERT_DEF_ID).equals(
        "1").and().property(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();

    // everything is mocked, there is no DB
    entity.setDefinitionId(Long.valueOf(1));

    String oldName = entity.getDefinitionName();
    String oldHash = entity.getHash();
    Integer oldInterval = entity.getScheduleInterval();
    boolean oldEnabled = entity.getEnabled();
    boolean oldHostIgnore = entity.isHostIgnored();
    String oldSource = entity.getSource();
    String oldDescription = entity.getDescription();

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    expect(dao.merge((AlertDefinitionEntity) anyObject())).andReturn(entity).anyTimes();
    replay(dao);

    requestProps = new HashMap<>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_ID, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_LABEL, "Label 2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_DESCRIPTION, "Description 2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    // new URI
    requestProps.put("AlertDefinition/source/uri/http", source.getUri().getHttpUri() + "_foobarbaz");
    requestProps.put("AlertDefinition/source/uri/https", source.getUri().getHttpsUri()
        + "_foobarbaz");
    requestProps.put("AlertDefinition/source/uri/https_property",
        source.getUri().getHttpsProperty() + "_foobarbaz");
    requestProps.put("AlertDefinition/source/uri/https_property_value",
        source.getUri().getHttpsPropertyValue() + "_foobarbaz");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_ENABLED, Boolean.FALSE.toString());

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_IGNORE_HOST, Boolean.TRUE.toString());

    request = PropertyHelper.getUpdateRequest(requestProps, null);

    provider.updateResources(request, p);

    Assert.assertFalse(oldHash.equals(entity.getHash()));
    Assert.assertFalse(oldName.equals(entity.getDefinitionName()));
    Assert.assertFalse(oldDescription.equals(entity.getDescription()));
    Assert.assertFalse(oldInterval.equals(entity.getScheduleInterval()));
    Assert.assertFalse(oldEnabled == entity.getEnabled());
    Assert.assertFalse(oldHostIgnore == entity.isHostIgnored());
    Assert.assertFalse(oldSource.equals(entity.getSource()));
    Assert.assertTrue(entity.getSource().contains("_foobarbaz"));

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testUpdateResourcesWithNumbersAsStrings() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).atLeastOnce();

    Capture<AlertDefinitionEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    // updateing a single definition should invalidate hosts of the definition
    expect(definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
      new HashSet<>()).atLeastOnce();

    replay(amc, clusters, cluster, dao, definitionHash);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    MetricSource source = (MetricSource) getMockSource();
    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    // Ambari converts all request bodies into Map<String,String>, even they are
    // numbers - this will ensure that we can convert a string to a number
    // properly
    requestProps.put("AlertDefinition/source/reporting/critical/text",
        source.getReporting().getCritical().getText());

    requestProps.put("AlertDefinition/source/reporting/critical/value", "1234.5");

    Request request = PropertyHelper.getCreateRequest(
        Collections.singleton(requestProps), null);

    AlertDefinitionResourceProvider provider = createProvider(amc);
    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    String sourceJson = entity.getSource();
    Gson gson = new Gson();
    source = gson.fromJson(sourceJson, MetricSource.class);

    // ensure it's actually a double and NOT a string
    assertEquals(new Double(1234.5d), source.getReporting().getCritical().getValue());
    verify(amc, clusters, cluster, dao);
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
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
  public void testDeleteResources(Authentication authentication) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Capture<AlertDefinitionEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    // deleting a single definition should invalidate hosts of the definition
    expect(
        definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
      new HashSet<>()).atLeastOnce();

    replay(amc, clusters, cluster, dao, definitionHash);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AlertDefinitionResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate p = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_ID).equals("1").and().property(
            AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();
    // everything is mocked, there is no DB
    entity.setDefinitionId(Long.valueOf(1));

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    dao.remove(capture(entityCapture));
    expectLastCall();
    replay(dao);

    provider.deleteResources(new RequestImpl(null, null, null, null), p);

    AlertDefinitionEntity entity1 = entityCapture.getValue();
    Assert.assertEquals(Long.valueOf(1), entity1.getDefinitionId());

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @param amc
   * @return
   */
  private AlertDefinitionResourceProvider createProvider(AmbariManagementController amc) {
    return new AlertDefinitionResourceProvider(amc);
  }

  /**
   * @return
   */
  private List<AlertDefinitionEntity> getMockEntities() throws Exception {
    Source source = getMockSource();
    String sourceJson = new Gson().toJson(source);

    ResourceEntity clusterResourceEntity = new ResourceEntity();
    clusterResourceEntity.setId(4L);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setResource(clusterResourceEntity);

    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    entity.setClusterId(Long.valueOf(1L));
    entity.setComponentName(null);
    entity.setDefinitionId(Long.valueOf(1L));
    entity.setDefinitionName("my_def");
    entity.setLabel("Mock Label");
    entity.setDescription("Mock Description");
    entity.setEnabled(true);
    entity.setHash(DEFINITION_UUID);
    entity.setScheduleInterval(Integer.valueOf(2));
    entity.setServiceName(null);
    entity.setSourceType(SourceType.METRIC);
    entity.setSource(sourceJson);
    entity.setCluster(clusterEntity);
    entity.setHelpURL("http://test-help-url");
    return Arrays.asList(entity);
  }

  /**
   * @return
   */
  private Source getMockSource() throws Exception {
    File alertsFile = new File(
        "src/test/resources/stacks/HDP/2.0.5/services/HDFS/alerts.json");

    Assert.assertTrue(alertsFile.exists());

    Set<AlertDefinition> set = m_factory.getAlertDefinitions(alertsFile, "HDFS");
    AlertDefinition nameNodeCpu = null;
    Iterator<AlertDefinition> definitions = set.iterator();
    while (definitions.hasNext()) {
      AlertDefinition definition = definitions.next();

      if (definition.getName().equals("namenode_cpu")) {
        nameNodeCpu = definition;
      }
    }

    Assert.assertNotNull(nameNodeCpu.getSource());
    return nameNodeCpu.getSource();
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
      binder.bind(AlertDefinitionDAO.class).toInstance(dao);
      binder.bind(AlertDefinitionHash.class).toInstance(definitionHash);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}
