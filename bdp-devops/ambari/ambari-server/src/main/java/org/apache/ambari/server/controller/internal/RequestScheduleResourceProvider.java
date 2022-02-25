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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestScheduleRequest;
import org.apache.ambari.server.controller.RequestScheduleResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.BatchSettings;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class RequestScheduleResourceProvider extends AbstractControllerResourceProvider {
  private static final Logger LOG = LoggerFactory.getLogger
    (RequestScheduleResourceProvider.class);

  public static final String REQUEST_SCHEDULE = "RequestSchedule";
  public static final String BATCH_SETTINGS = "batch_settings";
  public static final String BATCH_REQUESTS = "batch_requests";

  public static final String ID_PROPERTY_ID = "id";
  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String DESCRIPTION_PROPERTY_ID = "description";
  public static final String STATUS_PROPERTY_ID = "status";
  public static final String LAST_EXECUTION_STATUS_PROPERTY_ID = "last_execution_status";
  public static final String BATCH_PROPERTY_ID = "batch";
  public static final String SCHEDULE_PROPERTY_ID = "schedule";
  public static final String CREATE_USER_PROPERTY_ID = "create_user";
  public static final String AUTHENTICATED_USER_PROPERTY_ID = "authenticated_user";
  public static final String UPDATE_USER_PROPERTY_ID = "update_user";
  public static final String CREATE_TIME_PROPERTY_ID = "create_time";
  public static final String UPDATE_TIME_PROPERTY_ID = "update_time";

  public static final String BATCH_SEPARATION_IN_SECONDS_PROPERTY_ID = "batch_separation_in_seconds";
  public static final String TASK_FAILURE_TOLERANCE_PROPERTY_ID = "task_failure_tolerance";
  public static final String TASK_FAILURE_TOLERANCE_PER_BATCH_PROPERTY_ID = "task_failure_tolerance_per_batch";
  public static final String TASK_FAILURE_TOLERANCE_LIMIT_PROPERTY_ID = "task_failure_tolerance_limit";
  public static final String REQUESTS_PROPERTY_ID = "requests";
  public static final String PAUSE_AFTER_FIRST_BATCH_PROPERTY_ID = "pause_after_first_batch";

  public static final String TYPE_PROPERTY_ID = "type";
  public static final String URI_PROPERTY_ID = "uri";
  public static final String ORDER_ID_PROPERTY_ID = "order_id";
  public static final String REQUEST_TYPE_PROPERTY_ID = "request_type";
  public static final String REQUEST_URI_PROPERTY_ID = "request_uri";
  public static final String REQUEST_BODY_PROPERTY_ID = "request_body";
  public static final String REQUEST_STATUS_PROPERTY_ID = "request_status";
  public static final String RETURN_CODE_PROPERTY_ID = "return_code";
  public static final String RESPONSE_MESSAGE_PROPERTY_ID = "response_message";

  public static final String DAYS_OF_MONTH_PROPERTY_ID = "days_of_month";
  public static final String MINUTES_PROPERTY_ID = "minutes";
  public static final String HOURS_PROPERTY_ID = "hours";
  public static final String YEAR_PROPERTY_ID = "year";
  public static final String DAY_OF_WEEK_PROPERTY_ID = "day_of_week";
  public static final String MONTH_PROPERTY_ID = "month";
  public static final String START_TIME_PROPERTY_ID = "startTime";
  public static final String START_TIME_SNAKE_CASE_PROPERTY_ID = "start_time";
  public static final String END_TIME_PROPERTY_ID = "endTime";
  public static final String END_TIME_SNAKE_CASE_PROPERTY_ID = "end_time";


  public static final String ID = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, ID_PROPERTY_ID);
  public static final String CLUSTER_NAME = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, CLUSTER_NAME_PROPERTY_ID);
  public static final String DESCRIPTION = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, DESCRIPTION_PROPERTY_ID);
  public static final String STATUS = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, STATUS_PROPERTY_ID);
  public static final String LAST_EXECUTION_STATUS = PropertyHelper.getPropertyId(REQUEST_SCHEDULE,
          LAST_EXECUTION_STATUS_PROPERTY_ID);
  public static final String BATCH = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, BATCH_PROPERTY_ID);
  public static final String SCHEDULE = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, SCHEDULE_PROPERTY_ID);
  public static final String CREATE_USER = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, CREATE_USER_PROPERTY_ID);
  public static final String AUTHENTICATED_USER = PropertyHelper.getPropertyId(REQUEST_SCHEDULE,
          AUTHENTICATED_USER_PROPERTY_ID);
  public static final String UPDATE_USER = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, UPDATE_USER_PROPERTY_ID);
  public static final String CREATE_TIME = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, CREATE_TIME_PROPERTY_ID);
  public static final String UPDATE_TIME = PropertyHelper.getPropertyId(REQUEST_SCHEDULE, UPDATE_TIME_PROPERTY_ID);

  public static final String BATCH_SEPARATION_IN_SECONDS = PropertyHelper.getPropertyId(BATCH_SETTINGS,
          BATCH_SEPARATION_IN_SECONDS_PROPERTY_ID);
  public static final String TASK_FAILURE_TOLERANCE = PropertyHelper.getPropertyId(BATCH_SETTINGS,
          TASK_FAILURE_TOLERANCE_PROPERTY_ID);
  public static final String TASK_FAILURE_TOLERANCE_PER_BATCH = PropertyHelper.getPropertyId(BATCH_SETTINGS, TASK_FAILURE_TOLERANCE_PER_BATCH_PROPERTY_ID);
  public static final String REQUESTS = PropertyHelper.getPropertyId(null, REQUESTS_PROPERTY_ID);

  public static final String PAUSE_AFTER_FIRST_BATCH = PropertyHelper.getPropertyId(BATCH_SETTINGS, PAUSE_AFTER_FIRST_BATCH_PROPERTY_ID);

  public static final String TYPE = PropertyHelper.getPropertyId(null, TYPE_PROPERTY_ID);
  public static final String URI = PropertyHelper.getPropertyId(null, URI_PROPERTY_ID);
  public static final String ORDER_ID = PropertyHelper.getPropertyId(null, ORDER_ID_PROPERTY_ID);
  public static final String BODY = PropertyHelper.getPropertyId(null, RequestBodyParser.REQUEST_BLOB_TITLE);

  public static final String DAYS_OF_MONTH = PropertyHelper.getPropertyId(SCHEDULE, DAYS_OF_MONTH_PROPERTY_ID);
  public static final String MINUTES = PropertyHelper.getPropertyId(SCHEDULE, MINUTES_PROPERTY_ID);
  public static final String HOURS = PropertyHelper.getPropertyId(SCHEDULE, HOURS_PROPERTY_ID);
  public static final String YEAR = PropertyHelper.getPropertyId(SCHEDULE, YEAR_PROPERTY_ID);
  public static final String DAY_OF_WEEK = PropertyHelper.getPropertyId(SCHEDULE, DAY_OF_WEEK_PROPERTY_ID);
  public static final String MONTH = PropertyHelper.getPropertyId(SCHEDULE, MONTH_PROPERTY_ID);
  public static final String START_TIME = PropertyHelper.getPropertyId(SCHEDULE, START_TIME_PROPERTY_ID);
  public static final String END_TIME = PropertyHelper.getPropertyId(SCHEDULE, END_TIME_PROPERTY_ID);

  /**
   * The key property ids for a RequestSchedule resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, CLUSTER_NAME)
      .put(Resource.Type.RequestSchedule, ID)
      .build();

  /**
   * The property ids for a RequestSchedule resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
    ID,
    CLUSTER_NAME,
    DESCRIPTION,
    STATUS,
    LAST_EXECUTION_STATUS,
    BATCH,
    SCHEDULE,
    CREATE_USER,
    AUTHENTICATED_USER,
    UPDATE_USER,
    CREATE_TIME,
    UPDATE_TIME,
    BATCH_SEPARATION_IN_SECONDS,
    TASK_FAILURE_TOLERANCE,
    TASK_FAILURE_TOLERANCE_PER_BATCH,
    PAUSE_AFTER_FIRST_BATCH,
    REQUESTS,
    TYPE,
    URI,
    ORDER_ID,
    BODY,
    DAYS_OF_MONTH,
    MINUTES,
    HOURS,
    YEAR,
    DAY_OF_WEEK,
    MONTH,
    START_TIME,
    END_TIME);

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */

  protected RequestScheduleResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.RequestSchedule, propertyIds, keyPropertyIds, managementController);
  }


  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    final Set<RequestScheduleRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequestScheduleRequest(propertyMap));
    }

    Set<RequestScheduleResponse> responses =
      createResources(new Command<Set<RequestScheduleResponse>>() {
      @Override
      public Set<RequestScheduleResponse> invoke() throws AmbariException {
        return createRequestSchedules(requests);
      }
    });

    notifyCreate(Resource.Type.RequestSchedule, request);

    Set<Resource> associatedResources = new HashSet<>();
    for (RequestScheduleResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RequestSchedule);
      resource.setProperty(ID, response.getId());
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    final Set<RequestScheduleRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequestScheduleRequest(propertyMap));
    }

    Set<RequestScheduleResponse> responses =
      getResources(new Command<Set<RequestScheduleResponse>>() {
        @Override
        public Set<RequestScheduleResponse> invoke() throws AmbariException {
          return getRequestSchedules(requests);
        }
      });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    for (RequestScheduleResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RequestSchedule);

      setResourceProperty(resource, ID,
        response.getId(), requestedIds);
      setResourceProperty(resource, CLUSTER_NAME,
        response.getClusterName(), requestedIds);
      setResourceProperty(resource, DESCRIPTION,
        response.getDescription(), requestedIds);
      setResourceProperty(resource, STATUS,
        response.getStatus(), requestedIds);
      setResourceProperty(resource, LAST_EXECUTION_STATUS,
        response.getLastExecutionStatus(), requestedIds);
      setResourceProperty(resource, BATCH,
        response.getBatch(), requestedIds);
      setResourceProperty(resource, SCHEDULE,
        response.getSchedule(), requestedIds);
      setResourceProperty(resource, CREATE_USER,
        response.getCreateUser(), requestedIds);
      setResourceProperty(resource, AUTHENTICATED_USER,
        response.getAuthenticatedUserId(), requestedIds);
      setResourceProperty(resource, CREATE_TIME,
        response.getCreateTime(), requestedIds);
      setResourceProperty(resource, UPDATE_USER,
        response.getUpdateUser(), requestedIds);
      setResourceProperty(resource, UPDATE_TIME,
        response.getUpdateTime(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  /**
   * Currently unsupported operation. Since strong guarantees are required
   * that no jobs are currently running.
   * @param request    the request object which defines the set of properties
   *                   for the resources to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are updated
   *
   * @return
   * @throws SystemException
   * @throws UnsupportedPropertyException
   * @throws NoSuchResourceException
   * @throws NoSuchParentResourceException
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RequestScheduleRequest> requests = new
      HashSet<>();

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequestScheduleRequest(propertyMap));
      }

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          updateRequestSchedule(requests);
          return null;
        }
      });
    }

    notifyUpdate(Resource.Type.RequestSchedule, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final RequestScheduleRequest requestScheduleRequest =
        getRequestScheduleRequest(propertyMap);

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          deleteRequestSchedule(requestScheduleRequest);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.RequestSchedule, predicate);

    return getRequestStatus(null);
  }

  private synchronized void deleteRequestSchedule(RequestScheduleRequest request)
    throws AmbariException {

    if (request.getId() == null) {
      throw new AmbariException("Id is a required field.");
    }

    Clusters clusters = getManagementController().getClusters();

    Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ClusterNotFoundException e) {
      throw new ParentObjectNotFoundException(
        "Attempted to delete a request schedule from a cluster which doesn't "
          + "exist", e);
    }

    RequestExecution requestExecution =
      cluster.getAllRequestExecutions().get(request.getId());

    if (requestExecution == null) {
      throw new AmbariException("Request Schedule not found "
        + ", clusterName = " + request.getClusterName()
        + ", description = " + request.getDescription()
        + ", id = " + request.getId());
    }

    String username = getManagementController().getAuthName();

    LOG.info("Disabling Request Schedule "
      + ", clusterName = " + request.getClusterName()
      + ", id = " + request.getId()
      + ", user = " + username);

    // Delete all jobs and triggers
    getManagementController().getExecutionScheduleManager()
      .deleteAllJobs(requestExecution);

    requestExecution.updateStatus(RequestExecution.Status.DISABLED);
  }

  private synchronized void updateRequestSchedule
    (Set<RequestScheduleRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters clusters = getManagementController().getClusters();

    for (RequestScheduleRequest request : requests) {

      validateRequest(request);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
          "Attempted to add a request schedule to a cluster which doesn't " +
            "exist", e);
      }

      if (request.getId() == null) {
        throw new AmbariException("Id is a required parameter.");
      }

      RequestExecution requestExecution =
        cluster.getAllRequestExecutions().get(request.getId());

      if (requestExecution == null) {
        throw new AmbariException("Request Schedule not found "
          + ", clusterName = " + request.getClusterName()
          + ", description = " + request.getDescription()
          + ", id = " + request.getId());
      }

      String username = getManagementController().getAuthName();
      Integer userId = getManagementController().getAuthId();

      if (request.getDescription() != null) {
        requestExecution.setDescription(request.getDescription());
      }

      if (request.getSchedule() != null) {
        requestExecution.setSchedule(request.getSchedule());
      }

      if (request.getStatus() != null) {
        //TODO status changes graph
        if (!isValidRequestScheduleStatus(request.getStatus())) {
          throw new AmbariException("Request Schedule status not valid"
            + ", clusterName = " + request.getClusterName()
            + ", description = " + request.getDescription()
            + ", id = " + request.getId());
        }
        requestExecution.setStatus(RequestExecution.Status.valueOf(request.getStatus()));
      }
      requestExecution.setUpdateUser(username);
      requestExecution.setAuthenticatedUserId(userId);

      LOG.info("Persisting updated Request Schedule "
        + ", clusterName = " + request.getClusterName()
        + ", description = " + request.getDescription()
        + ", status = " + request.getStatus()
        + ", user = " + username);

      requestExecution.persist();

      // Update schedule for the batch
      getManagementController().getExecutionScheduleManager()
        .updateBatchSchedule(requestExecution);
    }
  }

  private synchronized Set<RequestScheduleResponse> createRequestSchedules
    (Set<RequestScheduleRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Set<RequestScheduleResponse> responses = new
      HashSet<>();

    Clusters clusters = getManagementController().getClusters();
    RequestExecutionFactory requestExecutionFactory =
      getManagementController().getRequestExecutionFactory();

    for (RequestScheduleRequest request : requests) {

      validateRequest(request);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
          "Attempted to add a request schedule to a cluster which doesn't " +
            "exist", e);
      }

      String username = getManagementController().getAuthName();
      Integer userId = getManagementController().getAuthId();

      RequestExecution requestExecution = requestExecutionFactory.createNew
        (cluster, request.getBatch(), request.getSchedule());

      requestExecution.setCreateUser(username);
      requestExecution.setUpdateUser(username);
      requestExecution.setAuthenticatedUserId(userId);
      requestExecution.setStatus(RequestExecution.Status.SCHEDULED);

      LOG.info("Persisting new Request Schedule "
        + ", clusterName = " + request.getClusterName()
        + ", description = " + request.getDescription()
        + ", user = " + username);

      requestExecution.persist();
      cluster.addRequestExecution(requestExecution);

      // Setup batch schedule
      getManagementController().getExecutionScheduleManager()
        .scheduleAllBatches(requestExecution);

      RequestScheduleResponse response = new RequestScheduleResponse
        (requestExecution.getId(), requestExecution.getClusterName(),
          requestExecution.getDescription(), requestExecution.getStatus(),
          requestExecution.getLastExecutionStatus(),
          requestExecution.getBatch(), request.getSchedule(),
          requestExecution.getCreateUser(), requestExecution.getCreateTime(),
          requestExecution.getUpdateUser(), requestExecution.getUpdateTime(),
          requestExecution.getAuthenticatedUserId());

      responses.add(response);
    }

    return responses;
  }

  private void validateRequest(RequestScheduleRequest request) throws AmbariException {
    if (request.getClusterName() == null) {
      throw new IllegalArgumentException("Cluster name is required.");
    }
    Schedule schedule = request.getSchedule();
    if (schedule != null) {
      getManagementController().getExecutionScheduleManager()
        .validateSchedule(schedule);
    }
    Batch batch = request.getBatch();
    if (batch != null && !batch.getBatchRequests().isEmpty()) {
      // Verify requests can be ordered
      HashSet<Long> orderIdSet = new HashSet<>();
      for (BatchRequest batchRequest : batch.getBatchRequests()) {
        if (batchRequest.getOrderId() == null) {
          throw new AmbariException("No order id provided for batch request. " +
            "" + batchRequest);
        }
        if (orderIdSet.contains(batchRequest.getOrderId())) {
          throw new AmbariException("Duplicate order id provided for batch " +
            "request. " + batchRequest);
        }
        orderIdSet.add(batchRequest.getOrderId());
      }
    }
  }

  private synchronized Set<RequestScheduleResponse> getRequestSchedules
    (Set<RequestScheduleRequest> requests) throws AmbariException {

    Set<RequestScheduleResponse> responses = new
      HashSet<>();

    if (requests != null) {
      for (RequestScheduleRequest request : requests) {
        if (request.getClusterName() == null) {
          LOG.warn("Cluster name is a required field.");
          continue;
        }

        Cluster cluster = getManagementController().getClusters().getCluster
          (request.getClusterName());

        Map<Long, RequestExecution> allRequestExecutions =
          cluster.getAllRequestExecutions();

        // Find by id
        if (request.getId() != null) {
          RequestExecution requestExecution = allRequestExecutions.get
            (request.getId());
          if (requestExecution != null) {
            responses.add(requestExecution.convertToResponseWithBody());
          }
          continue;
        }
        // Find by status
        if (request.getStatus() != null) {
          for (RequestExecution requestExecution : allRequestExecutions.values()) {
            if (requestExecution.getStatus().equals(request.getStatus())) {
              responses.add(requestExecution.convertToResponse());
            }
          }
          continue;
        }
        // TODO: Find by status of Batch Request(s) and start time greater than requested time

        // Select all
        for (RequestExecution requestExecution : allRequestExecutions.values()) {
          responses.add(requestExecution.convertToResponse());
        }
      }
    }

    return responses;
  }

  private boolean isValidRequestScheduleStatus(String giveStatus) {
    for (RequestExecution.Status status : RequestExecution.Status.values()) {
      if (status.name().equalsIgnoreCase(giveStatus)) {
        return true;
      }
    }
    return false;
  }

  private RequestScheduleRequest getRequestScheduleRequest(Map<String, Object> properties) {
    Object idObj = properties.get(ID);
    Long id = null;
    if (idObj != null)  {
      id = idObj instanceof Long ? (Long) idObj :
        Long.parseLong((String) idObj);
    }

    RequestScheduleRequest requestScheduleRequest = new RequestScheduleRequest(
      id,
      (String) properties.get(CLUSTER_NAME),
      (String) properties.get(DESCRIPTION),
      (String) properties.get(STATUS),
      null,
      null);

    Batch batch = new Batch();
    BatchSettings batchSettings = new BatchSettings();
    List<BatchRequest> batchRequests = new ArrayList<>();

    Object batchObject = properties.get(BATCH);
    if (batchObject != null && batchObject instanceof HashSet<?>) {
      try {
        HashSet<Map<String, Object>> batchMap = (HashSet<Map<String, Object>>) batchObject;

        for (Map<String, Object> batchEntry : batchMap) {
          if (batchEntry != null) {
            for (Map.Entry<String, Object> batchMapEntry : batchEntry.entrySet()) {
              if (batchMapEntry.getKey().equals
                  (TASK_FAILURE_TOLERANCE)) {
                batchSettings.setTaskFailureToleranceLimit(Integer.valueOf
                  ((String) batchMapEntry.getValue()));
              }  else if (batchMapEntry.getKey().equals
                  (TASK_FAILURE_TOLERANCE_PER_BATCH)) {
                batchSettings.setTaskFailureToleranceLimitPerBatch(Integer.valueOf
                    ((String) batchMapEntry.getValue()));
              } else if (batchMapEntry.getKey().equals
                  (BATCH_SEPARATION_IN_SECONDS)) {
                batchSettings.setBatchSeparationInSeconds(Integer.valueOf
                  ((String) batchMapEntry.getValue()));
              } else if (batchMapEntry.getKey().equals
                  (PAUSE_AFTER_FIRST_BATCH)) {
                batchSettings.setPauseAfterFirstBatch(Boolean.valueOf
                    ((String) batchMapEntry.getValue()));
              } else if (batchMapEntry.getKey().equals
                  (REQUESTS)) {
                HashSet<Map<String, Object>> requestSet =
                  (HashSet<Map<String, Object>>) batchMapEntry.getValue();

                for (Map<String, Object> requestEntry : requestSet) {
                  if (requestEntry != null) {
                    BatchRequest batchRequest = new BatchRequest();
                    for (Map.Entry<String, Object> requestMapEntry :
                        requestEntry.entrySet()) {
                      if (requestMapEntry.getKey()
                                 .equals(TYPE)) {
                        batchRequest.setType(BatchRequest.Type.valueOf
                          ((String) requestMapEntry.getValue()));
                      } else if (requestMapEntry.getKey()
                                 .equals(URI)) {
                        batchRequest.setUri(
                          (String) requestMapEntry.getValue());
                      } else if (requestMapEntry.getKey()
                                .equals(ORDER_ID)) {
                        batchRequest.setOrderId(Long.parseLong(
                          (String) requestMapEntry.getValue()));
                      } else if (requestMapEntry.getKey()
                                .equals(BODY)) {
                        batchRequest.setBody(
                          (String) requestMapEntry.getValue());
                      }
                    }
                    batchRequests.add(batchRequest);
                  }
                }
              }
            }
          }
        }

        batch.getBatchRequests().addAll(batchRequests);
        batch.setBatchSettings(batchSettings);

      } catch (Exception e) {
        LOG.warn("Request Schedule batch json is unparseable. " +
          batchObject, e);
      }
    }

    requestScheduleRequest.setBatch(batch);

    Schedule schedule = new Schedule();
    for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
      if (propertyEntry.getKey().equals(DAY_OF_WEEK)) {
        schedule.setDayOfWeek((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(DAYS_OF_MONTH)) {
        schedule.setDaysOfMonth((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(END_TIME)) {
        schedule.setEndTime((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(HOURS)) {
        schedule.setHours((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(MINUTES)) {
        schedule.setMinutes((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(MONTH)) {
        schedule.setMonth((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(START_TIME)) {
        schedule.setStartTime((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(YEAR)) {
        schedule.setYear((String) propertyEntry.getValue());
      }
    }

    if (!schedule.isEmpty()) {
      requestScheduleRequest.setSchedule(schedule);
    }

    return requestScheduleRequest;
  }
}
