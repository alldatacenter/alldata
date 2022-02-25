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
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.topology.TopologyManager;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class StageResourceProviderTest {

  private StageDAO dao = null;
  private Clusters clusters = null;
  private Cluster cluster = null;
  private AmbariManagementController managementController = null;
  private Injector injector;
  private HostRoleCommandDAO hrcDao = null;
  private TopologyManager topologyManager = null;

  @Before
  public void before() {
    dao = createStrictMock(StageDAO.class);
    clusters = createStrictMock(Clusters.class);
    cluster = createStrictMock(Cluster.class);
    hrcDao = createStrictMock(HostRoleCommandDAO.class);
    topologyManager = EasyMock.createNiceMock(TopologyManager.class);

    expect(topologyManager.getStages()).andReturn(new ArrayList<>()).atLeastOnce();

    expect(hrcDao.findAggregateCounts(EasyMock.anyObject(Long.class))).andReturn(
        new HashMap<Long, HostRoleCommandStatusSummaryDTO>() {
          {
            put(0L, new HostRoleCommandStatusSummaryDTO(0, 1000L, 2500L, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                0, 0, 0));
          }
        }).anyTimes();

    replay(hrcDao, topologyManager);

    managementController = createNiceMock(AmbariManagementController.class);

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    Assert.assertNotNull(injector);
  }


  @Test
  public void testCreateResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);

    Request request = createNiceMock(Request.class);
    try {
      provider.createResources(request);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testUpdateResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);

    expect(clusters.getClusterById(anyLong())).andReturn(cluster).anyTimes();
    expect(request.getProperties()).andReturn(Collections.emptySet());

    replay(clusters, cluster, request, predicate);

    provider.updateResources(request, predicate);

    verify(clusters, cluster);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDeleteResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);
    Predicate predicate = createNiceMock(Predicate.class);
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
  }

  @Test
  public void testGetResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);

    List<StageEntity> entities = getStageEntities(HostRoleStatus.COMPLETED);

    expect(dao.findAll(request, predicate)).andReturn(entities);

    expect(clusters.getClusterById(anyLong())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    replay(dao, clusters, cluster, request, predicate);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals(100.0, resource.getPropertyValue(StageResourceProvider.STAGE_PROGRESS_PERCENT));
    Assert.assertEquals(HostRoleStatus.COMPLETED, resource.getPropertyValue(StageResourceProvider.STAGE_STATUS));
    Assert.assertEquals(HostRoleStatus.COMPLETED, resource.getPropertyValue(StageResourceProvider.STAGE_DISPLAY_STATUS));
    Assert.assertEquals(1000L, resource.getPropertyValue(StageResourceProvider.STAGE_START_TIME));
    Assert.assertEquals(2500L, resource.getPropertyValue(StageResourceProvider.STAGE_END_TIME));

    verify(dao, clusters, cluster);
  }

  @Test
  public void testGetResourcesWithRequest() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);

    Request request = createNiceMock(Request.class);
    Predicate predicate = new PredicateBuilder().property(StageResourceProvider.STAGE_REQUEST_ID).equals(1L).toPredicate();

    List<StageEntity> entities = getStageEntities(HostRoleStatus.COMPLETED);

    expect(dao.findAll(request, predicate)).andReturn(entities);

    expect(clusters.getClusterById(anyLong())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    reset(topologyManager);
    expect(topologyManager.getRequest(EasyMock.anyLong())).andReturn(null).atLeastOnce();

    replay(topologyManager, dao, clusters, cluster, request);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals(100.0, resource.getPropertyValue(StageResourceProvider.STAGE_PROGRESS_PERCENT));
    Assert.assertEquals(HostRoleStatus.COMPLETED, resource.getPropertyValue(StageResourceProvider.STAGE_STATUS));
    Assert.assertEquals(HostRoleStatus.COMPLETED, resource.getPropertyValue(StageResourceProvider.STAGE_DISPLAY_STATUS));
    Assert.assertEquals(1000L, resource.getPropertyValue(StageResourceProvider.STAGE_START_TIME));
    Assert.assertEquals(2500L, resource.getPropertyValue(StageResourceProvider.STAGE_END_TIME));

    verify(topologyManager, dao, clusters, cluster);
  }

  /**
   * Tests getting the display status of a stage which can differ from the final
   * status.
   *
   * @throws Exception
   */
  @Test
  public void testGetDisplayStatus() throws Exception {
    // clear the HRC call so that it has the correct summary fields to represent
    // 1 skipped and 1 completed task
    EasyMock.reset(hrcDao);
    expect(hrcDao.findAggregateCounts(EasyMock.anyObject(Long.class))).andReturn(
        new HashMap<Long, HostRoleCommandStatusSummaryDTO>() {
          {
            put(0L, new HostRoleCommandStatusSummaryDTO(0, 1000L, 2500L, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                0, 0, 1));
          }
        }).anyTimes();

    replay(hrcDao);

    StageResourceProvider provider = new StageResourceProvider(managementController);

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);

    // make the stage skippable so it resolves to COMPLETED even though it has a
    // skipped failure
    List<StageEntity> entities = getStageEntities(HostRoleStatus.SKIPPED_FAILED);
    entities.get(0).setSkippable(true);

    expect(dao.findAll(request, predicate)).andReturn(entities);

    expect(clusters.getClusterById(anyLong())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    replay(dao, clusters, cluster, request, predicate);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    // verify the two statuses
    Assert.assertEquals(HostRoleStatus.COMPLETED, resource.getPropertyValue(StageResourceProvider.STAGE_STATUS));
    Assert.assertEquals(HostRoleStatus.SKIPPED_FAILED, resource.getPropertyValue(StageResourceProvider.STAGE_DISPLAY_STATUS));

    verify(dao, clusters, cluster);
  }


  @Test
  @Ignore
  public void testQueryForResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);

    List<StageEntity> entities = getStageEntities(HostRoleStatus.COMPLETED);

    expect(dao.findAll(request, predicate)).andReturn(entities);

    expect(clusters.getClusterById(anyLong())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    replay(dao, clusters, cluster, request, predicate);

    QueryResponse response =  provider.queryForResources(request, predicate);

    Set<Resource> resources = response.getResources();

    Assert.assertEquals(1, resources.size());

    Assert.assertFalse(response.isSortedResponse());
    Assert.assertFalse(response.isPagedResponse());
    Assert.assertEquals(1, response.getTotalResourceCount());

    verify(dao, clusters, cluster);
  }

  @Test
  public void testUpdateStageStatus_aborted() throws Exception {
    StageResourceProvider provider = new StageResourceProvider(managementController);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    Predicate predicate = new PredicateBuilder().property(StageResourceProvider.STAGE_STAGE_ID).equals(2L).and().
        property(StageResourceProvider.STAGE_REQUEST_ID).equals(1L).toPredicate();

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(StageResourceProvider.STAGE_STATUS, HostRoleStatus.ABORTED.name());
    Request request = PropertyHelper.getUpdateRequest(requestProps, null);

    List<StageEntity> entities = getStageEntities(HostRoleStatus.HOLDING);

    expect(dao.findAll(request, predicate)).andReturn(entities);
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();

    dao.updateStageStatus(entities.get(0), HostRoleStatus.ABORTED, actionManager);
    EasyMock.expectLastCall().atLeastOnce();

    replay(dao, clusters, cluster, actionManager, managementController);

    provider.updateResources(request, predicate);

    verify(dao, clusters, cluster, actionManager, managementController);
  }

  private List<StageEntity> getStageEntities(HostRoleStatus lastTaskStatus) {
    StageEntity stage = new StageEntity();

    HostRoleCommandEntity task1 = new HostRoleCommandEntity();
    task1.setStatus(HostRoleStatus.COMPLETED);
    task1.setStartTime(1000L);
    task1.setEndTime(2000L);

    HostRoleCommandEntity task2 = new HostRoleCommandEntity();
    task2.setStatus(lastTaskStatus);
    task2.setStartTime(1500L);
    task2.setEndTime(2500L);


    Collection<HostRoleCommandEntity> tasks = new HashSet<>();
    tasks.add(task1);
    tasks.add(task2);

    stage.setHostRoleCommands(tasks);
    stage.setRequestId(1L);

    List<StageEntity> entities = new LinkedList<>();
    entities.add(stage);
    return entities;
  }

  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(StageDAO.class).toInstance(dao);
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(HostRoleCommandDAO.class).toInstance(hrcDao);
      binder.bind(AmbariManagementController.class).toInstance(managementController);
      binder.bind(ActionMetadata.class);
      binder.bind(TopologyManager.class).toInstance(topologyManager);
    }
  }
}
