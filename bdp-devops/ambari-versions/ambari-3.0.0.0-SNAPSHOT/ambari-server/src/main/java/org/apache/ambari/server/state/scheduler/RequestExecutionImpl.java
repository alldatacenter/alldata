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
package org.apache.ambari.server.state.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.RequestScheduleResponse;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleBatchRequestDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleBatchRequestEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class RequestExecutionImpl implements RequestExecution {
  private Cluster cluster;
  private Batch batch;
  private Schedule schedule;
  private RequestScheduleEntity requestScheduleEntity;
  private volatile boolean isPersisted = false;

  @Inject
  private Gson gson;
  @Inject
  private Clusters clusters;
  @Inject
  private RequestScheduleDAO requestScheduleDAO;
  @Inject
  private RequestScheduleBatchRequestDAO batchRequestDAO;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private HostDAO hostDAO;

  private static final Logger LOG = LoggerFactory.getLogger(RequestExecutionImpl.class);
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  @AssistedInject
  public RequestExecutionImpl(@Assisted("cluster") Cluster cluster,
                              @Assisted("batch") Batch batch,
                              @Assisted("schedule") @Nullable Schedule schedule,
                              Injector injector) {
    this.cluster = cluster;
    this.batch = batch;
    this.schedule = schedule;
    injector.injectMembers(this);

    // Initialize the Entity object
    // Batch Hosts is initialized on persist
    requestScheduleEntity = new RequestScheduleEntity();
    requestScheduleEntity.setClusterId(cluster.getClusterId());

    updateBatchSettings();

    updateSchedule();
  }

  @AssistedInject
  public RequestExecutionImpl(@Assisted Cluster cluster,
                              @Assisted RequestScheduleEntity requestScheduleEntity,
                              Injector injector) {
    this.cluster = cluster;
    injector.injectMembers(this);

    this.requestScheduleEntity = requestScheduleEntity;

    batch = new Batch();
    schedule = new Schedule();

    BatchSettings batchSettings = new BatchSettings();
    batchSettings.setBatchSeparationInSeconds(requestScheduleEntity.getBatchSeparationInSeconds());
    batchSettings.setTaskFailureToleranceLimit(requestScheduleEntity.getBatchTolerationLimit());
    batchSettings.setTaskFailureToleranceLimitPerBatch(requestScheduleEntity.getBatchTolerationLimitPerBatch());
    batchSettings.setPauseAfterFirstBatch(requestScheduleEntity.isPauseAfterFirstBatch());

    batch.setBatchSettings(batchSettings);

    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      requestScheduleEntity.getRequestScheduleBatchRequestEntities();
    if (batchRequestEntities != null) {
      for (RequestScheduleBatchRequestEntity batchRequestEntity :
          batchRequestEntities) {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setOrderId(batchRequestEntity.getBatchId());
        batchRequest.setRequestId(batchRequestEntity.getRequestId());
        if (batchRequestEntity.getRequestBody() != null) {
          batchRequest.setBody(new String(batchRequestEntity.getRequestBody()));
        }
        batchRequest.setType(BatchRequest.Type.valueOf(batchRequestEntity.getRequestType()));
        batchRequest.setUri(batchRequestEntity.getRequestUri());
        batchRequest.setStatus(batchRequestEntity.getRequestStatus());
        batchRequest.setReturnCode(batchRequestEntity.getReturnCode());
        batchRequest.setResponseMsg(batchRequestEntity.getReturnMessage());
        batch.getBatchRequests().add(batchRequest);
      }
    }

    schedule.setDayOfWeek(requestScheduleEntity.getDayOfWeek());
    schedule.setDaysOfMonth(requestScheduleEntity.getDaysOfMonth());
    schedule.setMinutes(requestScheduleEntity.getMinutes());
    schedule.setHours(requestScheduleEntity.getHours());
    schedule.setMonth(requestScheduleEntity.getMonth());
    schedule.setYear(requestScheduleEntity.getYear());
    schedule.setStartTime(requestScheduleEntity.getStartTime());
    schedule.setEndTime(requestScheduleEntity.getEndTime());

    //if all values are nulls set the general scheduler to null
    if (schedule.getDayOfWeek() == null && schedule.getDaysOfMonth() == null &&
          schedule.getMinutes() == null && schedule.getHours() == null &&
          schedule.getMonth() == null && schedule.getYear() == null &&
          schedule.getStartTime() == null && schedule.getEndTime() == null) {
      schedule = null;
    }
    isPersisted = true;
  }

  @Override
  public Long getId() {
    return requestScheduleEntity.getScheduleId();
  }

  @Override
  public String getClusterName() {
    return cluster.getClusterName();
  }

  @Override
  public Batch getBatch() {
    return batch;
  }

  @Override
  public void setBatch(Batch batch) {
    this.batch = batch;
  }

  @Override
  public Schedule getSchedule() {
    return schedule;
  }

  @Override
  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  @Override
  public RequestScheduleResponse convertToResponse() {
    readWriteLock.readLock().lock();
    try{
      RequestScheduleResponse response = new RequestScheduleResponse(
        getId(), getClusterName(), getDescription(), getStatus(),
        getLastExecutionStatus(), getBatch(), getSchedule(),
        requestScheduleEntity.getCreateUser(),
        DateUtils.convertToReadableTime(requestScheduleEntity.getCreateTimestamp()),
        requestScheduleEntity.getUpdateUser(),
        DateUtils.convertToReadableTime(requestScheduleEntity.getUpdateTimestamp()),
        requestScheduleEntity.getAuthenticatedUserId()
      );
      return response;
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void persist() {
    readWriteLock.writeLock().lock();
    try {
      if (!isPersisted) {
        persistEntities();
        refresh();
        cluster.refresh();
        isPersisted = true;
      } else {
        saveIfPersisted();
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void refresh() {
    readWriteLock.writeLock().lock();
    try{
      if (isPersisted) {
        RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
          (requestScheduleEntity.getScheduleId());
        requestScheduleDAO.refresh(scheduleEntity);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void delete() {
    readWriteLock.writeLock().lock();
    try {
      if (isPersisted) {
        batchRequestDAO.removeByScheduleId(requestScheduleEntity.getScheduleId());
        requestScheduleDAO.remove(requestScheduleEntity);
        cluster.refresh();
        isPersisted = false;
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public String getStatus() {
    return requestScheduleEntity.getStatus();
  }

  @Override
  public void setDescription(String description) {
    requestScheduleEntity.setDescription(description);
  }

  @Override
  public String getDescription() {
    return requestScheduleEntity.getDescription();
  }

  /**
   * Persist @RequestScheduleEntity with @RequestScheduleBatchHostEntity
   */
  @Transactional
  void persistEntities() {
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());
    requestScheduleEntity.setClusterEntity(clusterEntity);
    requestScheduleEntity.setCreateTimestamp(System.currentTimeMillis());
    requestScheduleEntity.setUpdateTimestamp(System.currentTimeMillis());
    requestScheduleDAO.create(requestScheduleEntity);

    persistRequestMapping();
  }

  @Transactional
  void persistRequestMapping() {
    // Delete existing mappings to support updates
    if (isPersisted) {
      batchRequestDAO.removeByScheduleId(requestScheduleEntity.getScheduleId());
      requestScheduleEntity.getRequestScheduleBatchRequestEntities().clear();
    }

    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null) {
        Collections.sort(batchRequests);
        for (BatchRequest batchRequest : batchRequests) {
          RequestScheduleBatchRequestEntity batchRequestEntity = new
            RequestScheduleBatchRequestEntity();
          batchRequestEntity.setBatchId(batchRequest.getOrderId());
          batchRequestEntity.setRequestId(batchRequest.getRequestId());
          batchRequestEntity.setScheduleId(requestScheduleEntity.getScheduleId());
          batchRequestEntity.setRequestScheduleEntity(requestScheduleEntity);
          batchRequestEntity.setRequestType(batchRequest.getType());
          batchRequestEntity.setRequestUri(batchRequest.getUri());
          batchRequestEntity.setRequestBody(batchRequest.getBody());
          batchRequestEntity.setReturnCode(batchRequest.getReturnCode());
          batchRequestEntity.setReturnMessage(batchRequest.getResponseMsg());
          batchRequestEntity.setRequestStatus(batchRequest.getStatus());
          batchRequestDAO.create(batchRequestEntity);
          requestScheduleEntity.getRequestScheduleBatchRequestEntities().add
            (batchRequestEntity);
          requestScheduleDAO.merge(requestScheduleEntity);
        }
      }
    }


  }

  @Transactional
  void saveIfPersisted() {
    if (isPersisted) {
      requestScheduleEntity.setUpdateTimestamp(System.currentTimeMillis());
      // Update the Entity object with new settings
      updateBatchSettings();
      updateSchedule();
      // Persist schedule and settings
      requestScheduleDAO.merge(requestScheduleEntity);
      // Persist batches of hosts
      persistRequestMapping();
    }
  }

  private void updateBatchSettings() {
    if (batch != null) {
      BatchSettings settings = batch.getBatchSettings();
      if (settings != null) {
        requestScheduleEntity.setBatchSeparationInSeconds(settings.getBatchSeparationInSeconds());
        requestScheduleEntity.setBatchTolerationLimit(settings.getTaskFailureToleranceLimit());
        requestScheduleEntity.setBatchTolerationLimitPerBatch(settings.getTaskFailureToleranceLimitPerBatch());
        requestScheduleEntity.setPauseAfterFirstBatch(settings.isPauseAfterFirstBatch());
      }
    }
  }

  private void updateSchedule() {
    if (schedule != null) {
      requestScheduleEntity.setMinutes(schedule.getMinutes());
      requestScheduleEntity.setHours(schedule.getHours());
      requestScheduleEntity.setDaysOfMonth(schedule.getDaysOfMonth());
      requestScheduleEntity.setDayOfWeek(schedule.getDayOfWeek());
      requestScheduleEntity.setMonth(schedule.getMonth());
      requestScheduleEntity.setYear(schedule.getYear());
      requestScheduleEntity.setStartTime(schedule.getStartTime());
      requestScheduleEntity.setEndTime(schedule.getEndTime());
    }
  }

  @Override
  public void setStatus(Status status) {
    requestScheduleEntity.setStatus(status.name());
  }

  @Override
  public void setLastExecutionStatus(String status) {
    requestScheduleEntity.setLastExecutionStatus(status);
  }

  @Override
  public void setAuthenticatedUserId(Integer username) {
    requestScheduleEntity.setAuthenticatedUserId(username);
  }

  @Override
  public void setCreateUser(String username) {
    requestScheduleEntity.setCreateUser(username);
  }

  @Override
  public void setUpdateUser(String username) {
    requestScheduleEntity.setUpdateUser(username);
  }

  @Override
  public String getCreateTime() {
    return DateUtils.convertToReadableTime
      (requestScheduleEntity.getCreateTimestamp());
  }

  @Override
  public String getUpdateTime() {
    return DateUtils.convertToReadableTime
      (requestScheduleEntity.getUpdateTimestamp());
  }

  @Override
  public Integer getAuthenticatedUserId() {
    return requestScheduleEntity.getAuthenticatedUserId();
  }

  @Override
  public String getCreateUser() {
    return requestScheduleEntity.getCreateUser();
  }

  @Override
  public String getUpdateUser() {
    return requestScheduleEntity.getUpdateUser();
  }

  @Override
  public String getLastExecutionStatus() {
    return requestScheduleEntity.getLastExecutionStatus();
  }

  @Override
  public RequestScheduleResponse convertToResponseWithBody() {
    readWriteLock.readLock().lock();
    try{
      RequestScheduleResponse response = convertToResponse();
      Batch batch = response.getBatch();
      if (batch != null) {
        List<BatchRequest> batchRequests = batch.getBatchRequests();
        if (batchRequests != null) {
          for (BatchRequest batchRequest : batchRequests) {
            batchRequest.setBody(getRequestBody(batchRequest.getOrderId()));
          }
        }
      }
      return response;
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public String getRequestBody(Long batchId) {
    String body = null;
    if (requestScheduleEntity != null) {
      Collection<RequestScheduleBatchRequestEntity> requestEntities =
        requestScheduleEntity.getRequestScheduleBatchRequestEntities();
      if (requestEntities != null) {
        for (RequestScheduleBatchRequestEntity requestEntity : requestEntities) {
          if (requestEntity.getBatchId().equals(batchId)) {
            body = requestEntity.getRequestBodyAsString();
          }
        }
      }
    }
    return body;
  }

  @Override
  public Collection<Long> getBatchRequestRequestsIDs(long batchId) {
    Collection<Long> requestIDs = new ArrayList<>();
    if (requestScheduleEntity != null) {
      Collection<RequestScheduleBatchRequestEntity> requestEntities =
        requestScheduleEntity.getRequestScheduleBatchRequestEntities();
      if (requestEntities != null) {
        requestIDs.addAll(requestEntities.stream()
          .filter(requestEntity -> requestEntity.getBatchId().equals(batchId))
          .map(RequestScheduleBatchRequestEntity::getRequestId)
          .collect(Collectors.toList()));
      }
    }
    return requestIDs;
  }

  @Override
  public BatchRequest getBatchRequest(long batchId) {
    for (BatchRequest batchRequest : batch.getBatchRequests()) {
      if (batchId == batchRequest.getOrderId()) {
        return batchRequest;
      }
    }
    return null;
  }

  @Override
  public void updateBatchRequest(long batchId,
                                 BatchRequestResponse batchRequestResponse,
                                 boolean statusOnly) {

    RequestScheduleBatchRequestEntity batchRequestEntity = null;

    for (RequestScheduleBatchRequestEntity entity :
        requestScheduleEntity.getRequestScheduleBatchRequestEntities()) {
      if (entity.getBatchId() == batchId
          && entity.getScheduleId() == requestScheduleEntity.getScheduleId()) {
        batchRequestEntity = entity;
      }
    }

    // Rare race condition when batch request finished during pausing the request execution,
    //in this case the job details will be deleted,
    //so we mark it as not completed because otherwise the job detail will be lost
    //and the whole Request Execution status will not be set to COMPLETED at the end.
    if (Status.PAUSED.name().equals(getStatus()) && HostRoleStatus.COMPLETED.name().equals(batchRequestResponse.getStatus())) {
      batchRequestResponse.setStatus(HostRoleStatus.ABORTED.name());
    }

    if (batchRequestEntity != null) {
      batchRequestEntity.setRequestStatus(batchRequestResponse.getStatus());

      if (!statusOnly) {
        batchRequestEntity.setReturnCode(batchRequestResponse.getReturnCode());
        batchRequestEntity.setRequestId(batchRequestResponse.getRequestId());
        batchRequestEntity.setReturnMessage(batchRequestResponse.getReturnMessage());
      }

      batchRequestDAO.merge(batchRequestEntity);
    }

    BatchRequest batchRequest = getBatchRequest(batchId);

    batchRequest.setStatus(batchRequestResponse.getStatus());

    if (!statusOnly) {
      batchRequest.setReturnCode(batchRequestResponse.getReturnCode());
      batchRequest.setResponseMsg(batchRequestResponse.getReturnMessage());
    }

    setLastExecutionStatus(batchRequestResponse.getStatus());
    requestScheduleDAO.merge(requestScheduleEntity);
  }

  @Override
  @Transactional
  public void updateStatus(Status status) {
    setStatus(status);
    if (isPersisted) {
      requestScheduleEntity.setUpdateTimestamp(System.currentTimeMillis());
      requestScheduleDAO.merge(requestScheduleEntity);
    } else {
      LOG.warn("Updated status in memory, since Request Schedule is not " +
        "persisted.");
    }
  }

}
