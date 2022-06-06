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
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.CollectionPresentationUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Widget tests
 */
public class WidgetResourceProviderTest {

  private WidgetDAO dao = null;
  private Injector m_injector;

  @Before
  public void before() {
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator());

    dao = createStrictMock(WidgetDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    WidgetResourceProvider provider = createProvider(null);

    Request request = PropertyHelper.getReadRequest("Widgets/id");

    Set<Resource> results = provider.getResources(request, null);

    assertEquals(0, results.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
      WidgetResourceProvider.WIDGET_ID_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_TIME_CREATED_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_DESCRIPTION_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID,
      WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getClusterById(1L)).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID).equals("c1")
          .and().property(WidgetResourceProvider.WIDGET_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID).equals("username").toPredicate();

    expect(dao.findById(1L)).andReturn(getMockEntities("CLUSTER").get(0));

    replay(amc, clusters, cluster, dao);

    WidgetResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    Assert.assertEquals("GAUGE", r.getPropertyValue(WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID));
    Assert.assertEquals("CLUSTER", r.getPropertyValue(WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID));
    Assert.assertEquals("username", r.getPropertyValue(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID));
    Assert.assertEquals("widget name", r.getPropertyValue(WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID));
    Object metrics = r.getPropertyValue(WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID);
    Assert.assertEquals("[{\"widget_id\":\"metrics/jvm/HeapMemoryUsed\"," +
            "\"host_component_criteria\":\"host_components/metrics/dfs/FSNamesystem/HAState\\u003dactive\"," +
            "\"service_name\":\"HDFS\",\"component_name\":\"NAMENODE\"," +
            "\"name\":\"java.lang:type\\u003dMemory.HeapMemoryUsage[used]\",\"category\":\"\"}," +
            "{\"widget_id\":\"metrics/jvm/HeapMemoryMax\"," +
            "\"host_component_criteria\":\"host_components/metrics/dfs/FSNamesystem/HAState\\u003dactive\"," +
            "\"service_name\":\"HDFS\",\"component_name\":\"NAMENODE\"," +
            "\"name\":\"java.lang:type\\u003dMemory.HeapMemoryUsage[max]\"," +
            "\"category\":\"\"}]", r.getPropertyValue(WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID));
    Assert.assertEquals("[{\"name\":\"NameNode Heap\"," +
            "\"value\":\"${java.lang:type\\u003dMemory.HeapMemoryUsage[used] / " +
            "java.lang:type\\u003dMemory.HeapMemoryUsage[max]}\"}]",
            r.getPropertyValue(WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID));
    Assert.assertEquals("{\"name\":\"value\"}", r.getPropertyValue(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourceOfOtherUser() throws Exception {
    Request request = PropertyHelper.getReadRequest(
            WidgetResourceProvider.WIDGET_ID_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_TIME_CREATED_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_DESCRIPTION_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID,
            WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getClusterById(1L)).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    Predicate predicate = new PredicateBuilder().property(
            WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID).equals("c1")
            .and().property(WidgetResourceProvider.WIDGET_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID).equals("username").toPredicate();

    expect(dao.findById(1L)).andReturn(getMockEntities("USER").get(0));

    replay(amc, clusters, cluster, dao);

    WidgetResourceProvider provider = createProvider(amc);

    try {
      Set<Resource> results = provider.getResources(request, predicate);
    } catch (AccessDeniedException ex) {
      //Expected exception
      Assert.assertEquals("User must be author of the widget or widget must have cluster scope", ex.getMessage());
    }

  }


  /**
   * @throws Exception
   */

  @Test
  public void testCreateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Capture<WidgetEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    replay(amc, clusters, cluster, dao);

    WidgetResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID, "widget name");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID, "GAUGE");
    requestProps.put(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID, "admin");
    requestProps.put(WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID, "USER");
    Set<Map<String, String>> testSet = new LinkedHashSet<>();
    Map<String, String> testMap = new HashMap<>();
    testMap.put("name","value");
    testMap.put("name2","value2");
    testSet.add(testMap);
    requestProps.put(WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property1", "value1");
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property2", "value2");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    RequestStatus requestStatus = provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    WidgetEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Assert.assertEquals(1, requestStatus.getAssociatedResources().size());
    Assert.assertEquals(Long.valueOf(1), entity.getClusterId());
    Assert.assertEquals("USER", entity.getScope());
    Assert.assertEquals("widget name", entity.getWidgetName());
    Assert.assertEquals(null, entity.getDefaultSectionName());
    Assert.assertEquals("GAUGE", entity.getWidgetType());
    Assert.assertEquals("admin", entity.getAuthor());
    Assert.assertEquals("[{\"name\":\"value\",\"name2\":\"value2\"}]", entity.getMetrics());
    Assert.assertEquals("[{\"name\":\"value\",\"name2\":\"value2\"}]", entity.getWidgetValues());
    Assert.assertEquals("{\"property2\":\"value2\",\"property1\":\"value1\"}", entity.getProperties());

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testUpdateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).atLeastOnce();

    Capture<WidgetEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    replay(amc, clusters, cluster, dao);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID, "widget name");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID, "GAUGE");
    requestProps.put(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID, "admin");
    requestProps.put(WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID, "USER");
    Set<Map<String, String>> testSet = new LinkedHashSet<>();
    Map<String, String> testMap = new HashMap<>();
    testMap.put("name","value");
    testMap.put("name2","value2");
    testSet.add(testMap);
    requestProps.put(WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property1", "value1");
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property2", "value2");

    Request request = PropertyHelper.getCreateRequest(
        Collections.singleton(requestProps), null);

    WidgetResourceProvider provider = createProvider(amc);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    WidgetEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate predicate = new PredicateBuilder().property(
            WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID).equals("c1")
            .and().property(WidgetResourceProvider.WIDGET_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID).equals("username").toPredicate();

    // everything is mocked, there is no DB
    entity.setId(Long.valueOf(1));

    String oldMetrics = entity.getMetrics();
    String oldProperties = entity.getProperties();
    String oldName = entity.getWidgetName();

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    expect(dao.merge((WidgetEntity) anyObject())).andReturn(entity).anyTimes();
    replay(dao);

    requestProps = new HashMap<>();
    requestProps.put(WidgetResourceProvider.WIDGET_ID_PROPERTY_ID, "1");
    requestProps.put(WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID, "widget name2");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID, "GAUGE");
    requestProps.put(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID, "admin");
    requestProps.put(WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID, "USER");
    testSet = new LinkedHashSet<>();
    testMap = new HashMap<>();
    testMap.put("name","new_value");
    testMap.put("new_name","new_value2");
    testSet.add(testMap);
    requestProps.put(WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property1", "new_value1");
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/new_property", "new_value2");

    request = PropertyHelper.getUpdateRequest(requestProps, null);

    provider.updateResources(request, predicate);

    Assert.assertFalse(oldName.equals(entity.getWidgetName()));
    Assert.assertFalse(oldMetrics.equals(entity.getMetrics()));
    Assert.assertFalse(oldProperties.equals(entity.getProperties()));
    Assert.assertEquals("[{\"name\":\"new_value\",\"new_name\":\"new_value2\"}]",entity.getMetrics());
    // Depends on hashing, string representation can be different
    Assert.assertTrue(CollectionPresentationUtils.isJsonsEquals("{\"new_property\":\"new_value2\",\"property1\":\"new_value1\"}", entity.getProperties()));
    Assert.assertEquals("widget name2",entity.getWidgetName());
    Assert.assertEquals(null,entity.getDefaultSectionName());

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDeleteResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Capture<WidgetEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    replay(amc, clusters, cluster, dao);

    WidgetResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID, "widget name");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID, "GAUGE");
    requestProps.put(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID, "admin");
    requestProps.put(WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID, "USER");
    Set<Map<String, String>> testSet = new LinkedHashSet<>();
    Map<String, String> testMap = new HashMap<>();
    testMap.put("name","value");
    testMap.put("name2","value2");
    testSet.add(testMap);
    requestProps.put(WidgetResourceProvider.WIDGET_METRICS_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_VALUES_PROPERTY_ID, testSet);
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property1", "value1");
    requestProps.put(WidgetResourceProvider.WIDGET_PROPERTIES_PROPERTY_ID+"/property2", "value2");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    WidgetEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate predicate = new PredicateBuilder().property(
            WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID).equals("c1")
            .and().property(WidgetResourceProvider.WIDGET_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID).equals("username").toPredicate();

    // everything is mocked, there is no DB
    entity.setId(Long.valueOf(1));

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    dao.remove(capture(entityCapture));
    expectLastCall();
    replay(dao);

    provider.deleteResources(request, predicate);

    WidgetEntity entity1 = entityCapture.getValue();
    Assert.assertEquals(Long.valueOf(1), entity1.getId());

    verify(amc, clusters, cluster, dao);
  }

  @Test
  public void testScopePrivilegeCheck() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createServiceOperator());

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getResourceId()).andReturn(Long.valueOf(1)).atLeastOnce();

    Capture<WidgetEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    replay(amc, clusters, cluster, dao);
    PowerMock.replayAll();

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetResourceProvider.WIDGET_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_NAME_PROPERTY_ID, "widget name");
    requestProps.put(WidgetResourceProvider.WIDGET_WIDGET_TYPE_PROPERTY_ID, "GAUGE");
    requestProps.put(WidgetResourceProvider.WIDGET_AUTHOR_PROPERTY_ID, "admin");
    requestProps.put(WidgetResourceProvider.WIDGET_SCOPE_PROPERTY_ID, "CLUSTER");
    Request request = PropertyHelper.getCreateRequest(
            Collections.singleton(requestProps), null);

    try {
      WidgetResourceProvider widgetResourceProvider = createProvider(amc);
      widgetResourceProvider.createResources(request);
    } catch (AccessDeniedException ex) {
      //Expected exception
    }

  }

  /**
   * @param amc
   * @return
   */
  private WidgetResourceProvider createProvider(AmbariManagementController amc) {
    return new WidgetResourceProvider(amc);
  }

  /**
   * @return
   */
  private List<WidgetEntity> getMockEntities(String scope) throws Exception {

    WidgetEntity widgetEntity = new WidgetEntity();
    widgetEntity.setClusterId(Long.valueOf(1L));
    widgetEntity.setWidgetName("widget name");
    widgetEntity.setWidgetType("GAUGE");
    widgetEntity.setAuthor("username");
    widgetEntity.setScope(scope);
    widgetEntity.setDefaultSectionName("default_section_name");
    widgetEntity.setDescription("Description");
    widgetEntity.setMetrics("[{\"widget_id\":\"metrics/jvm/HeapMemoryUsed\"," +
            "\"host_component_criteria\":\"host_components/metrics/dfs/FSNamesystem/HAState\\u003dactive\"," +
            "\"service_name\":\"HDFS\",\"component_name\":\"NAMENODE\"," +
            "\"name\":\"java.lang:type\\u003dMemory.HeapMemoryUsage[used]\",\"category\":\"\"}," +
            "{\"widget_id\":\"metrics/jvm/HeapMemoryMax\"," +
            "\"host_component_criteria\":\"host_components/metrics/dfs/FSNamesystem/HAState\\u003dactive\"," +
            "\"service_name\":\"HDFS\",\"component_name\":\"NAMENODE\"," +
            "\"name\":\"java.lang:type\\u003dMemory.HeapMemoryUsage[max]\"," +
            "\"category\":\"\"}]");
    widgetEntity.setWidgetValues("[{\"name\":\"NameNode Heap\"," +
            "\"value\":\"${java.lang:type\\u003dMemory.HeapMemoryUsage[used] / " +
            "java.lang:type\\u003dMemory.HeapMemoryUsage[max]}\"}]");
    widgetEntity.setProperties("{\"name\":\"value\"}");
    return Arrays.asList(widgetEntity);
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
      binder.bind(WidgetDAO.class).toInstance(dao);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
              EasyMock.createNiceMock(Cluster.class));
      binder.bind(CredentialStoreService.class).toInstance(
        EasyMock.createNiceMock(CredentialStoreService.class)
      );
    }
  }
}
