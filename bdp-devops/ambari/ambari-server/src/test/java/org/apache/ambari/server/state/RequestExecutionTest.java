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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.RequestScheduleResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
import org.apache.ambari.server.orm.entities.RequestScheduleBatchRequestEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.BatchRequestResponse;
import org.apache.ambari.server.state.scheduler.BatchSettings;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

import junit.framework.Assert;

public class RequestExecutionTest {
  private Injector injector;
  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private AmbariMetaInfo metaInfo;
  private RequestExecutionFactory requestExecutionFactory;
  private RequestScheduleDAO requestScheduleDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    requestExecutionFactory = injector.getInstance(RequestExecutionFactory.class);
    requestScheduleDAO = injector.getInstance(RequestScheduleDAO.class);

    clusterName = "foo";
    clusters.addCluster(clusterName, new StackId("HDP-0.1"));
    cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);
    clusters.addHost("h1");
    clusters.addHost("h2");
    clusters.addHost("h3");
    Assert.assertNotNull(clusters.getHost("h1"));
    Assert.assertNotNull(clusters.getHost("h2"));
    Assert.assertNotNull(clusters.getHost("h3"));
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Transactional
  RequestExecution createRequestSchedule() throws Exception {
    Batch batches = new Batch();
    Schedule schedule = new Schedule();

    BatchSettings batchSettings = new BatchSettings();
    batchSettings.setTaskFailureToleranceLimit(10);
    batchSettings.setTaskFailureToleranceLimitPerBatch(2);
    batches.setBatchSettings(batchSettings);

    List<BatchRequest> batchRequests = new ArrayList<>();
    BatchRequest batchRequest1 = new BatchRequest();
    batchRequest1.setOrderId(10L);
    batchRequest1.setType(BatchRequest.Type.DELETE);
    batchRequest1.setUri("testUri1");

    BatchRequest batchRequest2 = new BatchRequest();
    batchRequest2.setOrderId(12L);
    batchRequest2.setType(BatchRequest.Type.POST);
    batchRequest2.setUri("testUri2");
    batchRequest2.setBody("testBody");

    batchRequests.add(batchRequest1);
    batchRequests.add(batchRequest2);

    batches.getBatchRequests().addAll(batchRequests);

    schedule.setMinutes("10");
    schedule.setEndTime("2014-01-01 00:00:00");

    RequestExecution requestExecution = requestExecutionFactory.createNew
      (cluster, batches, schedule);

    requestExecution.setStatus(RequestExecution.Status.SCHEDULED);
    requestExecution.setDescription("Test Schedule");

    requestExecution.persist();
    cluster.addRequestExecution(requestExecution);
    return requestExecution;
  }

  @Test
  public void testCreateRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
      (requestExecution.getId());

    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(requestExecution.getBatch().getBatchSettings()
      .getTaskFailureToleranceLimit(), scheduleEntity.getBatchTolerationLimit());
    Assert.assertEquals(requestExecution.getBatch().getBatchSettings()
      .getTaskFailureToleranceLimitPerBatch(), scheduleEntity.getBatchTolerationLimitPerBatch());
    Assert.assertEquals(scheduleEntity.getRequestScheduleBatchRequestEntities().size(), 2);
    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();
    Assert.assertNotNull(batchRequestEntities);
    RequestScheduleBatchRequestEntity reqEntity1 = null;
    RequestScheduleBatchRequestEntity reqEntity2 = null;
    for (RequestScheduleBatchRequestEntity reqEntity : batchRequestEntities) {
      if (reqEntity.getRequestUri().equals("testUri1")) {
        reqEntity1 = reqEntity;
      } else if (reqEntity.getRequestUri().equals("testUri2")) {
        reqEntity2 = reqEntity;
      }
    }
    Assert.assertNotNull(reqEntity1);
    Assert.assertNotNull(reqEntity2);
    Assert.assertEquals(Long.valueOf(10L), reqEntity1.getBatchId());
    Assert.assertEquals(Long.valueOf(12L), reqEntity2.getBatchId());
    Assert.assertEquals(BatchRequest.Type.DELETE.name(), reqEntity1.getRequestType());
    Assert.assertEquals(BatchRequest.Type.POST.name(), reqEntity2.getRequestType());
    Assert.assertEquals(requestExecution.getSchedule().getMinutes(),
      scheduleEntity.getMinutes());
    Assert.assertEquals(requestExecution.getSchedule().getEndTime(),
      scheduleEntity.getEndTime());
  }

  @Test
  public void testUpdateRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);
    Long id = requestExecution.getId();
    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById(id);
    Assert.assertNotNull(scheduleEntity);

    // Read from DB
    requestExecution = requestExecutionFactory.createExisting(cluster,
      scheduleEntity);

    // Remove host and add host
    Batch batches = new Batch();

    List<BatchRequest> batchRequests = new ArrayList<>();
    BatchRequest batchRequest1 = new BatchRequest();
    batchRequest1.setOrderId(10L);
    batchRequest1.setType(BatchRequest.Type.PUT);
    batchRequest1.setUri("testUri3");

    BatchRequest batchRequest2 = new BatchRequest();
    batchRequest2.setOrderId(12L);
    batchRequest2.setType(BatchRequest.Type.POST);
    batchRequest2.setUri("testUri4");
    batchRequest2.setBody("testBody");

    batchRequests.add(batchRequest1);
    batchRequests.add(batchRequest2);

    batches.getBatchRequests().addAll(batchRequests);


    requestExecution.setBatch(batches);

    // Change schedule
    requestExecution.getSchedule().setHours("11");

    // Save
    requestExecution.persist();

    scheduleEntity = requestScheduleDAO.findById(id);
    Assert.assertNotNull(scheduleEntity);
    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();
    Assert.assertNotNull(batchRequestEntities);
    RequestScheduleBatchRequestEntity reqEntity1 = null;
    RequestScheduleBatchRequestEntity reqEntity2 = null;
    for (RequestScheduleBatchRequestEntity reqEntity : batchRequestEntities) {
      if (reqEntity.getRequestUri().equals("testUri3")) {
        reqEntity1 = reqEntity;
      } else if (reqEntity.getRequestUri().equals("testUri4")) {
        reqEntity2 = reqEntity;
      }
    }
    Assert.assertNotNull(reqEntity1);
    Assert.assertNotNull(reqEntity2);
    Assert.assertEquals(Long.valueOf(10L), reqEntity1.getBatchId());
    Assert.assertEquals(Long.valueOf(12L), reqEntity2.getBatchId());
    Assert.assertEquals(BatchRequest.Type.PUT.name(), reqEntity1.getRequestType());
    Assert.assertEquals(BatchRequest.Type.POST.name(), reqEntity2.getRequestType());
    Assert.assertEquals("11", scheduleEntity.getHours());
  }

  @Test
  public void testGetRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
      (requestExecution.getId());
    Assert.assertNotNull(scheduleEntity);

    Assert.assertNotNull(cluster.getAllRequestExecutions().get
      (requestExecution.getId()));

    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(requestExecution.getBatch().getBatchSettings()
      .getTaskFailureToleranceLimit(), scheduleEntity.getBatchTolerationLimit());
    Assert.assertEquals(requestExecution.getBatch().getBatchSettings()
      .getTaskFailureToleranceLimitPerBatch(), scheduleEntity.getBatchTolerationLimitPerBatch());
    Assert.assertEquals(scheduleEntity.getRequestScheduleBatchRequestEntities().size(), 2);
    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();
    Assert.assertNotNull(batchRequestEntities);
    RequestScheduleBatchRequestEntity reqEntity1 = null;
    RequestScheduleBatchRequestEntity reqEntity2 = null;
    for (RequestScheduleBatchRequestEntity reqEntity : batchRequestEntities) {
      if (reqEntity.getRequestUri().equals("testUri1")) {
        reqEntity1 = reqEntity;
      } else if (reqEntity.getRequestUri().equals("testUri2")) {
        reqEntity2 = reqEntity;
      }
    }
    Assert.assertNotNull(reqEntity1);
    Assert.assertNotNull(reqEntity2);
    Assert.assertEquals(Long.valueOf(10L), reqEntity1.getBatchId());
    Assert.assertEquals(Long.valueOf(12L), reqEntity2.getBatchId());
    Assert.assertEquals(BatchRequest.Type.DELETE.name(), reqEntity1.getRequestType());
    Assert.assertEquals(BatchRequest.Type.POST.name(), reqEntity2.getRequestType());
    Assert.assertEquals(requestExecution.getSchedule().getMinutes(),
      scheduleEntity.getMinutes());
    Assert.assertEquals(requestExecution.getSchedule().getEndTime(),
      scheduleEntity.getEndTime());
  }

  @Test
  public void testDeleteRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);

    Long id = requestExecution.getId();
    cluster.deleteRequestExecution(id);

    Assert.assertNull(requestScheduleDAO.findById(id));
    Assert.assertNull(cluster.getAllRequestExecutions().get(id));
  }

  @Test
  public void testGetRequestScheduleWithRequestBody() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);
    Assert.assertNotNull(cluster.getAllRequestExecutions().get
      (requestExecution.getId()));

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
      (requestExecution.getId());
    Assert.assertNotNull(scheduleEntity);

    // Default Read
    requestExecution = requestExecutionFactory.createExisting(cluster,
      scheduleEntity);

    BatchRequest postBatchRequest = null;
    List<BatchRequest> batchRequests = requestExecution.getBatch()
      .getBatchRequests();
    Assert.assertNotNull(batchRequests);
    for (BatchRequest batchRequest : batchRequests) {
      if (batchRequest.getType().equals(BatchRequest.Type.POST.name())) {
        postBatchRequest = batchRequest;
      }
    }
    Assert.assertNotNull(postBatchRequest);
    // Not read by default
    Assert.assertNotNull(postBatchRequest.getBody());
    Assert.assertEquals("testBody", postBatchRequest.getBody());

    RequestScheduleResponse requestScheduleResponse = requestExecution
      .convertToResponseWithBody();

    Assert.assertNotNull(requestScheduleResponse);

    batchRequests = requestExecution.getBatch().getBatchRequests();
    Assert.assertNotNull(batchRequests);
    for (BatchRequest batchRequest : batchRequests) {
      if (batchRequest.getType().equals(BatchRequest.Type.POST.name())) {
        postBatchRequest = batchRequest;
      }
    }
    Assert.assertNotNull(postBatchRequest);
    // Request Body loaded lazily
    Assert.assertNotNull(postBatchRequest.getBody());
  }

  @Test
  public void testUpdateStatus() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);
    Assert.assertNotNull(cluster.getAllRequestExecutions().get
      (requestExecution.getId()));

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
      (requestExecution.getId());
    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(RequestExecution.Status.SCHEDULED.name(),
      scheduleEntity.getStatus());

    requestExecution.updateStatus(RequestExecution.Status.COMPLETED);

    scheduleEntity = requestScheduleDAO.findById(requestExecution.getId());
    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(RequestExecution.Status.COMPLETED.name(),
      scheduleEntity.getStatus());
  }

  @Test
  public void testUpdateBatchRequest() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);
    Assert.assertNotNull(cluster.getAllRequestExecutions().get
      (requestExecution.getId()));

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById(requestExecution.getId());
    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(RequestExecution.Status.SCHEDULED.name(), scheduleEntity.getStatus());

    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();

    Assert.assertNotNull(batchRequestEntities);
    Assert.assertEquals(2, batchRequestEntities.size());

    BatchRequestResponse batchRequestResponse = new BatchRequestResponse();
    batchRequestResponse.setRequestId(1L);
    batchRequestResponse.setReturnCode(200);
    batchRequestResponse.setReturnMessage("test");
    batchRequestResponse.setStatus("IN_PROGRESS");

    requestExecution.updateBatchRequest(10L, batchRequestResponse, false);

    scheduleEntity = requestScheduleDAO.findById(requestExecution.getId());
    RequestScheduleBatchRequestEntity testEntity = null;

    for (RequestScheduleBatchRequestEntity entity :
        scheduleEntity.getRequestScheduleBatchRequestEntities()) {
      if (entity.getBatchId().equals(10L)) {
        testEntity = entity;
      }
    }

    Assert.assertNotNull(testEntity);
    Assert.assertEquals(200, testEntity.getReturnCode().intValue());
    Assert.assertEquals("test", testEntity.getReturnMessage());
    Assert.assertEquals("IN_PROGRESS", testEntity.getRequestStatus());
  }
}
