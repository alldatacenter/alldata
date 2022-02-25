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

import static org.easymock.EasyMock.anyLong;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * WidgetLayout tests
 */
public class WidgetLayoutResourceProviderTest {

  private WidgetLayoutDAO dao = null;
  private WidgetDAO widgetDAO = null;
  private Injector m_injector;

  @Before
  public void before() {
    dao = createStrictMock(WidgetLayoutDAO.class);
    widgetDAO = createStrictMock(WidgetDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    WidgetLayoutResourceProvider provider = createProvider(null);

    Request request = PropertyHelper.getReadRequest("WidgetLayouts/id");

    Set<Resource> results = provider.getResources(request, null);

    assertEquals(0, results.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID,
        WidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getClusterById(1L)).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID).equals("c1")
          .and().property(WidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID).equals("username").toPredicate();

    expect(dao.findById(1L)).andReturn(getMockEntities().get(0));

    replay(amc, clusters, cluster, dao);

    WidgetLayoutResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    Assert.assertEquals("section0", r.getPropertyValue(WidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID));
    Assert.assertEquals("CLUSTER", r.getPropertyValue(WidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID));
    Assert.assertEquals("username", r.getPropertyValue(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID));
    Assert.assertEquals("displ_name", r.getPropertyValue(WidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID));
    Assert.assertEquals("layout name0", r.getPropertyValue(WidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID));

    Assert.assertEquals("[]", r.getPropertyValue(WidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID).toString());
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

    Capture<WidgetLayoutEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    WidgetEntity widgetEntity = new WidgetEntity();
    widgetEntity.setId(1l);
    widgetEntity.setListWidgetLayoutUserWidgetEntity(new ArrayList<>());
    expect(widgetDAO.findById(anyLong())).andReturn(widgetEntity).anyTimes();

    replay(amc, clusters, cluster, dao, widgetDAO);

    WidgetLayoutResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID, "layout_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID, "display_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID, "section_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID, "admin");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID, "CLUSTER");
    Set<Map<String, String>> widgetsInfo = new LinkedHashSet<>();
    Map<String, String> widget = new HashMap<>();
    widget.put("id","1");
    widgetsInfo.add(widget);
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID, widgetsInfo);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    RequestStatus requestStatus = provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    WidgetLayoutEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Assert.assertEquals(1, requestStatus.getAssociatedResources().size());
    Assert.assertEquals(Long.valueOf(1), entity.getClusterId());
    Assert.assertEquals("CLUSTER", entity.getScope());
    Assert.assertEquals("layout_name", entity.getLayoutName());
    Assert.assertEquals("display_name", entity.getDisplayName());
    Assert.assertEquals("section_name", entity.getSectionName());
    Assert.assertEquals("admin", entity.getUserName());
    Assert.assertNotNull(entity.getListWidgetLayoutUserWidgetEntity());
    Assert.assertNotNull(entity.getListWidgetLayoutUserWidgetEntity().get(0));
    Assert.assertNotNull(entity.getListWidgetLayoutUserWidgetEntity().get(0).
            getWidget().getListWidgetLayoutUserWidgetEntity());

    verify(amc, clusters, cluster, dao, widgetDAO);
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

    Capture<WidgetLayoutEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    WidgetEntity widgetEntity = new WidgetEntity();
    widgetEntity.setId(1L);
    widgetEntity.setListWidgetLayoutUserWidgetEntity(new ArrayList<>());
    WidgetEntity widgetEntity2 = new WidgetEntity();
    widgetEntity2.setId(2L);
    widgetEntity2.setListWidgetLayoutUserWidgetEntity(new ArrayList<>());
    expect(widgetDAO.findById(1L)).andReturn(widgetEntity).atLeastOnce();

    replay(amc, clusters, cluster, dao, widgetDAO);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID, "layout_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID, "display_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID, "section_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID, "admin");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID, "CLUSTER");
    Set<Map<String, String>> widgetsInfo = new LinkedHashSet<>();
    Map<String, String> widget = new HashMap<>();
    widget.put("id","1");
    widgetsInfo.add(widget);
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID, widgetsInfo);

    Request request = PropertyHelper.getCreateRequest(
        Collections.singleton(requestProps), null);

    WidgetLayoutResourceProvider provider = createProvider(amc);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    WidgetLayoutEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate predicate = new PredicateBuilder().property(
            WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID).equals("c1")
            .and().property(WidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID).equals("username").toPredicate();

    // everything is mocked, there is no DB
    entity.setId(Long.valueOf(1));

    String oldLayoutName = entity.getLayoutName();
    String oldScope = entity.getScope();

    resetToStrict(dao, widgetDAO);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    expect(dao.mergeWithFlush((WidgetLayoutEntity) anyObject())).andReturn(entity).anyTimes();
    expect(widgetDAO.merge(widgetEntity)).andReturn(widgetEntity).anyTimes();
    expect(widgetDAO.findById(2L)).andReturn(widgetEntity2).anyTimes();
    replay(dao, widgetDAO);

    requestProps = new HashMap<>();
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID, "layout_name_new");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID, "USER");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID, "1");
    widget.put("id","2");
    widgetsInfo.add(widget);
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID, widgetsInfo);

    request = PropertyHelper.getUpdateRequest(requestProps, null);

    provider.updateResources(request, predicate);

    Assert.assertFalse(oldLayoutName.equals(entity.getLayoutName()));
    Assert.assertFalse(oldScope.equals(entity.getScope()));

    verify(amc, clusters, cluster, dao, widgetDAO);
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

    Capture<WidgetLayoutEntity> entityCapture = EasyMock.newCapture();
    dao.create(capture(entityCapture));
    expectLastCall();

    replay(amc, clusters, cluster, dao);

    WidgetLayoutResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID, "c1");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID, "layout_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID, "display_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID, "section_name");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID, "admin");
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID, "CLUSTER");
    Set widgetsInfo = new LinkedHashSet();
    requestProps.put(WidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID, widgetsInfo);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    WidgetLayoutEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate predicate = new PredicateBuilder().property(
            WidgetLayoutResourceProvider.WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID).equals("c1")
            .and().property(WidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID).equals("1")
            .and().property(WidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID).equals("username").toPredicate();

    // everything is mocked, there is no DB
    entity.setId(Long.valueOf(1));

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    dao.remove(capture(entityCapture));
    expectLastCall();
    replay(dao);

    provider.deleteResources(request, predicate);

    WidgetLayoutEntity entity1 = entityCapture.getValue();
    Assert.assertEquals(Long.valueOf(1), entity1.getId());

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @param amc
   * @return
   */
  private WidgetLayoutResourceProvider createProvider(AmbariManagementController amc) {
    return new WidgetLayoutResourceProvider(amc);
  }

  /**
   * @return
   */
  private List<WidgetLayoutEntity> getMockEntities() throws Exception {

    WidgetLayoutEntity widgetLayoutEntity = new WidgetLayoutEntity();
    widgetLayoutEntity.setClusterId(Long.valueOf(1L));
    widgetLayoutEntity.setLayoutName("layout name0");
    widgetLayoutEntity.setSectionName("section0");
    widgetLayoutEntity.setUserName("username");
    widgetLayoutEntity.setScope("CLUSTER");
    widgetLayoutEntity.setDisplayName("displ_name");
    List<WidgetLayoutUserWidgetEntity> layoutUserWidgetEntityList = new LinkedList<>();
    widgetLayoutEntity.setListWidgetLayoutUserWidgetEntity(layoutUserWidgetEntityList);
    return Arrays.asList(widgetLayoutEntity);
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
      binder.bind(WidgetLayoutDAO.class).toInstance(dao);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
      binder.bind(WidgetDAO.class).toInstance(widgetDAO);
    }
  }
}
