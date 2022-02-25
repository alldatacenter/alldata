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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestScheduleResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

import junit.framework.Assert;

public class RequestScheduleResourceProviderTest {

  RequestScheduleResourceProvider getResourceProvider
    (AmbariManagementController managementController) {

    Resource.Type type = Resource.Type.RequestSchedule;

    return (RequestScheduleResourceProvider)
      AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController
      );
  }

  @Test
  public void testCreateRequestSchedule() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    RequestExecutionFactory executionFactory = createNiceMock
      (RequestExecutionFactory.class);
    RequestExecution requestExecution = createNiceMock(RequestExecution.class);
    ExecutionScheduleManager executionScheduleManager = createNiceMock
      (ExecutionScheduleManager.class);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getExecutionScheduleManager()).andReturn
      (executionScheduleManager).anyTimes();
    expect(managementController.getRequestExecutionFactory()).andReturn
      (executionFactory);
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getAuthId()).andReturn(1).anyTimes();

    Capture<Cluster> clusterCapture = EasyMock.newCapture();
    Capture<Batch> batchCapture = EasyMock.newCapture();
    Capture<Schedule> scheduleCapture = EasyMock.newCapture();

    expect(executionFactory.createNew(capture(clusterCapture),
      capture(batchCapture), capture(scheduleCapture))).andReturn(requestExecution);

    replay(managementController, clusters, cluster, executionFactory,
      requestExecution, response, executionScheduleManager);

    RequestScheduleResourceProvider resourceProvider = getResourceProvider
      (managementController);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RequestScheduleResourceProvider
      .CLUSTER_NAME, "Cluster100");
    properties.put(RequestScheduleResourceProvider
      .DESCRIPTION, "some description");
    properties.put(RequestScheduleResourceProvider
      .DAY_OF_WEEK, "MON");
    properties.put(RequestScheduleResourceProvider
      .MINUTES, "2");
    properties.put(RequestScheduleResourceProvider
      .END_TIME, "2013-11-18T14:29:29-08:00");
    properties.put(RequestScheduleResourceProvider
      .DAYS_OF_MONTH, "*");

    HashSet<Map<String, Object>> batch = new HashSet<>();
    Map<String, Object> batchSettings = new HashMap<>();
    batchSettings.put(RequestScheduleResourceProvider
      .BATCH_SEPARATION_IN_SECONDS, "15");

    Map<String, Object> batchRequests = new HashMap<>();
    HashSet<Map<String, Object>> requestSet = new HashSet<>();

    Map<String, Object> request1 = new HashMap<>();
    Map<String, Object> request2 = new HashMap<>();

    request1.put(RequestScheduleResourceProvider
      .TYPE, BatchRequest.Type.PUT.name());
    request1.put(RequestScheduleResourceProvider
      .ORDER_ID, "20");
    request1.put(RequestScheduleResourceProvider
      .URI, "SomeUpdateUri");
    request1.put(RequestScheduleResourceProvider
      .BODY, "data1");

    request2.put(RequestScheduleResourceProvider
      .TYPE, BatchRequest.Type.DELETE.name());
    request2.put(RequestScheduleResourceProvider
      .ORDER_ID, "22");
    request2.put(RequestScheduleResourceProvider
      .URI, "SomeDeleteUri");

    requestSet.add(request1);
    requestSet.add(request2);

    batchRequests.put(RequestScheduleResourceProvider
      .REQUESTS, requestSet);

    batch.add(batchSettings);
    batch.add(batchRequests);

    properties.put(RequestScheduleResourceProvider
      .BATCH, batch);

    propertySet.add(properties);
    Request request = PropertyHelper.getCreateRequest(propertySet, null);
    resourceProvider.createResources(request);

    verify(managementController, clusters, cluster, executionFactory,
      requestExecution, response, executionScheduleManager);

    List<BatchRequest> testRequests = batchCapture.getValue().getBatchRequests();
    Assert.assertNotNull(testRequests);
    BatchRequest deleteReq = null;
    BatchRequest putReq = null;
    for (BatchRequest testBatchRequest : testRequests) {
      if (testBatchRequest.getType().equals(BatchRequest.Type.DELETE.name())) {
        deleteReq = testBatchRequest;
      } else {
        putReq = testBatchRequest;
      }
    }
    Assert.assertNotNull(deleteReq);
    Assert.assertNotNull(putReq);
    Assert.assertEquals("data1", putReq.getBody());
    Assert.assertNull(deleteReq.getBody());
  }

  @Test
  public void testUpdateRequestSchedule() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    final RequestExecution requestExecution = createNiceMock(RequestExecution.class);
    RequestScheduleResponse requestScheduleResponse = createNiceMock
      (RequestScheduleResponse.class);
    ExecutionScheduleManager executionScheduleManager = createNiceMock
      (ExecutionScheduleManager.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getAuthId()).andReturn(1).anyTimes();
    expect(managementController.getExecutionScheduleManager()).andReturn
      (executionScheduleManager).anyTimes();

    expect(requestExecution.getId()).andReturn(25L).anyTimes();
    expect(requestExecution.convertToResponse()).andReturn
      (requestScheduleResponse).anyTimes();
    expect(requestExecution.convertToResponseWithBody()).andReturn
      (requestScheduleResponse).anyTimes();
    expect(requestScheduleResponse.getId()).andReturn(25L).anyTimes();
    expect(requestScheduleResponse.getClusterName()).andReturn("Cluster100")
      .anyTimes();

    expect(cluster.getAllRequestExecutions()).andStubAnswer(new IAnswer<Map<Long, RequestExecution>>() {
      @Override
      public Map<Long, RequestExecution> answer() throws Throwable {
        Map<Long, RequestExecution> requestExecutionMap = new HashMap<>();
        requestExecutionMap.put(requestExecution.getId(), requestExecution);
        return requestExecutionMap;
      }
    });

    replay(managementController, clusters, cluster, requestExecution,
      response, requestScheduleResponse, executionScheduleManager);

    RequestScheduleResourceProvider resourceProvider = getResourceProvider
      (managementController);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RequestScheduleResourceProvider
      .CLUSTER_NAME, "Cluster100");
    properties.put(RequestScheduleResourceProvider
      .DESCRIPTION, "some description");
    properties.put(RequestScheduleResourceProvider
      .DAY_OF_WEEK, "MON");
    properties.put(RequestScheduleResourceProvider
      .MINUTES, "2");
    properties.put(RequestScheduleResourceProvider
      .END_TIME, "2013-11-18T14:29:29-08:00");
    properties.put(RequestScheduleResourceProvider
      .DAYS_OF_MONTH, "*");

    HashSet<Map<String, Object>> batch = new HashSet<>();
    Map<String, Object> batchSettings = new HashMap<>();
    batchSettings.put(RequestScheduleResourceProvider
      .BATCH_SEPARATION_IN_SECONDS, "15");

    Map<String, Object> batchRequests = new HashMap<>();
    HashSet<Map<String, Object>> requestSet = new HashSet<>();

    Map<String, Object> request1 = new HashMap<>();
    Map<String, Object> request2 = new HashMap<>();

    request1.put(RequestScheduleResourceProvider
      .TYPE, BatchRequest.Type.PUT.name());
    request1.put(RequestScheduleResourceProvider
      .ORDER_ID, "20");
    request1.put(RequestScheduleResourceProvider
      .URI, "SomeUpdateUri");
    request1.put(RequestScheduleResourceProvider
      .BODY, "data1");

    request2.put(RequestScheduleResourceProvider
      .TYPE, BatchRequest.Type.DELETE.name());
    request2.put(RequestScheduleResourceProvider
      .ORDER_ID, "22");
    request2.put(RequestScheduleResourceProvider
      .URI, "SomeDeleteUri");

    requestSet.add(request1);
    requestSet.add(request2);

    batchRequests.put(RequestScheduleResourceProvider
      .REQUESTS, requestSet);

    batch.add(batchSettings);
    batch.add(batchRequests);

    properties.put(RequestScheduleResourceProvider
      .BATCH, batch);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);
    Predicate predicate = new PredicateBuilder().property
      (RequestScheduleResourceProvider.CLUSTER_NAME)
      .equals("Cluster100").and().property(RequestScheduleResourceProvider
        .ID).equals(25L).toPredicate();

    resourceProvider.updateResources(request, predicate);

    verify(managementController, clusters, cluster, requestExecution,
      response, requestScheduleResponse, executionScheduleManager);
  }

  @Test
  public void testGetRequestSchedule() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    final RequestExecution requestExecution = createNiceMock(RequestExecution.class);
    RequestScheduleResponse requestScheduleResponse = createNiceMock
      (RequestScheduleResponse.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();

    expect(requestExecution.getId()).andReturn(25L).anyTimes();
    expect(requestExecution.getStatus()).andReturn(RequestExecution.Status
      .SCHEDULED.name()).anyTimes();
    expect(requestExecution.convertToResponse()).andReturn
      (requestScheduleResponse).anyTimes();
    expect(requestExecution.convertToResponseWithBody()).andReturn
      (requestScheduleResponse).anyTimes();
    expect(requestScheduleResponse.getId()).andReturn(25L).anyTimes();
    expect(requestScheduleResponse.getClusterName()).andReturn("Cluster100")
      .anyTimes();

    expect(cluster.getAllRequestExecutions()).andStubAnswer(new IAnswer<Map<Long, RequestExecution>>() {
      @Override
      public Map<Long, RequestExecution> answer() throws Throwable {
        Map<Long, RequestExecution> requestExecutionMap = new HashMap<>();
        requestExecutionMap.put(requestExecution.getId(), requestExecution);
        return requestExecutionMap;
      }
    });

    replay(managementController, clusters, cluster, requestExecution,
      response, requestScheduleResponse);

    RequestScheduleResourceProvider resourceProvider = getResourceProvider
      (managementController);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RequestScheduleResourceProvider
      .CLUSTER_NAME, "Cluster100");
    properties.put(RequestScheduleResourceProvider
      .DESCRIPTION, "some description");

    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(RequestScheduleResourceProvider
      .CLUSTER_NAME);
    propertyIds.add(RequestScheduleResourceProvider
      .ID);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    // Read by id
    Predicate predicate = new PredicateBuilder().property
      (RequestScheduleResourceProvider.CLUSTER_NAME)
      .equals("Cluster100").and().property(RequestScheduleResourceProvider
        .ID).equals(25L).toPredicate();

    Set<Resource> resources = resourceProvider.getResources(request,
      predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(25L, resources.iterator().next().getPropertyValue
      (RequestScheduleResourceProvider.ID));

    // Read all
    predicate = new PredicateBuilder().property
      (RequestScheduleResourceProvider.CLUSTER_NAME)
      .equals("Cluster100").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(25L, resources.iterator().next().getPropertyValue
      (RequestScheduleResourceProvider.ID));

    verify(managementController, clusters, cluster, requestExecution,
      response, requestScheduleResponse);
  }

  @Test
  public void testDeleteRequestSchedule() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    RequestExecution requestExecution = createNiceMock(RequestExecution.class);
    ExecutionScheduleManager executionScheduleManager = createNiceMock
      (ExecutionScheduleManager.class);

    Map<Long, RequestExecution> requestExecutionMap = new HashMap<>();
    requestExecutionMap.put(1L, requestExecution);

    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getExecutionScheduleManager()).andReturn
      (executionScheduleManager).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getAllRequestExecutions()).andReturn(requestExecutionMap);

    replay(managementController, clusters, cluster, executionScheduleManager,
      requestExecution );

    RequestScheduleResourceProvider resourceProvider = getResourceProvider
      (managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider) resourceProvider).addObserver(observer);

    Predicate predicate = new PredicateBuilder().property
      (RequestScheduleResourceProvider.CLUSTER_NAME)
      .equals("Cluster100").and().property(RequestScheduleResourceProvider
        .ID).equals(1L).toPredicate();

    resourceProvider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.RequestSchedule, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    verify(managementController, clusters, cluster, executionScheduleManager,
      requestExecution);
  }
}
