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

package org.apache.ambari.server.scheduler;

import static org.apache.ambari.server.state.scheduler.RequestExecution.Status.ABORTED;
import static org.apache.ambari.server.state.scheduler.RequestExecution.Status.PAUSED;
import static org.apache.ambari.server.state.scheduler.RequestExecution.Status.SCHEDULED;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.security.authorization.internal.InternalTokenClientFilter;
import org.apache.ambari.server.security.authorization.internal.InternalTokenStorage;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.BatchRequestJob;
import org.apache.ambari.server.state.scheduler.BatchRequestResponse;
import org.apache.ambari.server.state.scheduler.BatchSettings;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.apache.ambari.server.utils.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.quartz.CronExpression;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.CsrfProtectionFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/**
 * This class handles scheduling request execution for managed clusters
 */
@Singleton
public class ExecutionScheduleManager {
  private static final Logger LOG = LoggerFactory.getLogger
    (ExecutionScheduleManager.class);

  private final InternalTokenStorage tokenStorage;
  private ActionDBAccessor actionDBAccessor;
  private final Gson gson;
  private final Clusters clusters;
  ExecutionScheduler executionScheduler;
  Configuration configuration;

  private volatile boolean schedulerAvailable = false;
  protected static final String BATCH_REQUEST_JOB_PREFIX = "BatchRequestJob";
  protected static final String REQUEST_EXECUTION_TRIGGER_PREFIX =
    "RequestExecution";
  protected static final String DEFAULT_API_PATH = "api/v1";

  public static final String USER_ID_HEADER = "X-Authenticated-User-ID";

  protected Client ambariClient;
  protected WebResource ambariWebResource;

  protected static final String REQUESTS_STATUS_KEY = "request_status";
  protected static final String REQUESTS_ID_KEY = "id";
  protected static final String REQUESTS_FAILED_TASKS_KEY = "failed_task_count";
  protected static final String REQUESTS_ABORTED_TASKS_KEY = "aborted_task_count";
  protected static final String REQUESTS_TIMEDOUT_TASKS_KEY = "timed_out_task_count";
  protected static final String REQUESTS_TOTAL_TASKS_KEY = "task_count";

  protected static final Pattern CONTAINS_API_VERSION_PATTERN = Pattern.compile("^/?" + DEFAULT_API_PATH+ ".*");

  @Inject
  public ExecutionScheduleManager(Configuration configuration,
                                  ExecutionScheduler executionScheduler,
                                  InternalTokenStorage tokenStorage,
                                  Clusters clusters,
                                  ActionDBAccessor actionDBAccessor,
                                  Gson gson) {
    this.configuration = configuration;
    this.executionScheduler = executionScheduler;
    this.tokenStorage = tokenStorage;
    this.clusters = clusters;
    this.actionDBAccessor = actionDBAccessor;
    this.gson = gson;

    try {
      buildApiClient();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

  protected void buildApiClient() throws NoSuchAlgorithmException, KeyManagementException {

    Client client;

    String pattern;
    String url;

    if (configuration.getApiSSLAuthentication()) {
      pattern = "https://localhost:%s/";
      url = String.format(pattern, configuration.getClientSSLApiPort());

      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }


      }};

      //Create SSL context
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new SecureRandom());

      //Install all trusting cert SSL context for jersey client
      ClientConfig config = new DefaultClientConfig();
      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
        new HostnameVerifier() {
          @Override
          public boolean verify( String s, SSLSession sslSession ) {
            return true;
          }
        },
        sc
      ));

      client = Client.create(config);

    } else {
      client = Client.create();
      pattern = "http://localhost:%s/";
      url = String.format(pattern, configuration.getClientApiPort());
    }

    this.ambariClient = client;
    this.ambariWebResource = client.resource(url);

    //Install auth filters
    ClientFilter csrfFilter = new CsrfProtectionFilter("RequestSchedule");
    ClientFilter tokenFilter = new InternalTokenClientFilter(tokenStorage);
    ambariClient.addFilter(csrfFilter);
    ambariClient.addFilter(tokenFilter);

  }

  /**
   * Start Execution scheduler
   */
  public void start() {
    LOG.info("Starting scheduler");
    try {
      executionScheduler.startScheduler(configuration.getExecutionSchedulerStartDelay());
      schedulerAvailable = true;

    } catch (AmbariException e) {
      LOG.warn("Unable to start scheduler. No recurring tasks will be " +
        "scheduled.");
    }
  }

  /**
   * Stop execution scheduler
   */
  public void stop() {
    LOG.info("Stopping scheduler");
    schedulerAvailable = false;
    try {
      executionScheduler.stopScheduler();
    } catch (AmbariException e) {
      LOG.warn("Unable to stop scheduler. No new recurring tasks will be " +
        "scheduled.");
    }
  }

  /**
   * Is Execution scheduler available for accepting jobs?
   * @return
   */
  public boolean isSchedulerAvailable() {
    return schedulerAvailable;
  }

  /**
   * Add trigger for a job to the scheduler
   * @param trigger
   */
  public void scheduleJob(Trigger trigger) {
    LOG.debug("Scheduling job: {}", trigger.getJobKey());
    if (isSchedulerAvailable()) {
      try {
        executionScheduler.scheduleJob(trigger);
      } catch (SchedulerException e) {
        LOG.error("Unable to add trigger for execution job: " + trigger
          .getJobKey(), e);
      }
    } else {
      LOG.error("Scheduler unavailable, cannot schedule jobs.");
    }
  }

  /**
   * Find out by how much did a schedule misfire and decide whether to continue
   * based on configuration
   * @param jobExecutionContext
   * @return
   */
  public boolean continueOnMisfire(JobExecutionContext jobExecutionContext) {
    if (jobExecutionContext != null) {
      Date scheduledTime = jobExecutionContext.getScheduledFireTime();
      Long diff = DateUtils.getDateDifferenceInMinutes(scheduledTime);
      return (diff < configuration.getExecutionSchedulerMisfireToleration());
    }
    return true;
  }


  private long getFirstJobOrderId(RequestExecution requestExecution) throws AmbariException {
    Long firstBatchOrderId = null;
    Batch batch = requestExecution.getBatch();
    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null) {
        Collections.sort(batchRequests);
        ListIterator<BatchRequest> iterator = batchRequests.listIterator();
        firstBatchOrderId = iterator.next().getOrderId();
      }
    }
    if (firstBatchOrderId == null) {
      throw new AmbariException("Can't schedule RequestExecution with no batches");
    }
    return firstBatchOrderId;
  }

  /**
   * Persist jobs based on the request batch and create trigger for the first
   * job
   * @param requestExecution
   * @throws AmbariException
   */
  public void scheduleAllBatches(RequestExecution requestExecution) throws AmbariException {
    Long firstBatchOrderId = getFirstJobOrderId(requestExecution);
    scheduleBatch(requestExecution, firstBatchOrderId);
  }

  /**
   * Persist jobs based on the request batches staring from the defined batch and create trigger for the first
   * job
   * @param requestExecution
   * @throws AmbariException
   */
  public void scheduleBatch(RequestExecution requestExecution, long startingBatchOrderId)
    throws AmbariException {

    if (!isSchedulerAvailable()) {
      throw new AmbariException("Scheduler unavailable.");
    }

    // Check if scheduler is running, if not start immediately before scheduling jobs
    try {
      if (!executionScheduler.isSchedulerStarted()) {
        executionScheduler.startScheduler(null);
      }
    } catch (SchedulerException e) {
      LOG.error("Unable to determine scheduler state.", e);
      throw new AmbariException("Scheduler unavailable.");
    }
    LOG.debug("Scheduling jobs starting from " + startingBatchOrderId);

    // Create and persist jobs based on batches
    JobDetail firstJobDetail = persistBatch(requestExecution, startingBatchOrderId);

    if (firstJobDetail == null) {
      throw new AmbariException("Unable to schedule jobs. firstJobDetail = "
        + firstJobDetail);
    }

    Integer failedCount = countFailedTasksBeforeStartingBatch(requestExecution, startingBatchOrderId);

    // Create a cron trigger for the first batch job
    // If no schedule is specified create simple trigger to fire right away
    Schedule schedule = requestExecution.getSchedule();

    if (schedule != null) {
      String triggerExpression = schedule.getScheduleExpression();

      Date startDate = null;
      Date endDate = null;
      try {
        String startTime = schedule.getStartTime();
        String endTime = schedule.getEndTime();
        startDate = startTime != null && !startTime.isEmpty() ?
          DateUtils.convertToDate(startTime) : new Date();
        endDate = endTime != null && !endTime.isEmpty() ?
          DateUtils.convertToDate(endTime) : null;
      } catch (ParseException e) {
        LOG.error("Unable to parse startTime / endTime.", e);
      }

      Trigger trigger = newTrigger()
          .withIdentity(REQUEST_EXECUTION_TRIGGER_PREFIX + "-" +
            requestExecution.getId(), ExecutionJob.LINEAR_EXECUTION_TRIGGER_GROUP)
          .withSchedule(cronSchedule(triggerExpression)
            .withMisfireHandlingInstructionFireAndProceed())
          .forJob(firstJobDetail)
          .usingJobData(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY, failedCount)
          .startAt(startDate)
          .endAt(endDate)
          .build();

      try {
        executionScheduler.scheduleJob(trigger);
        LOG.debug("Scheduled trigger next fire time: {}", trigger.getNextFireTime());
      } catch (SchedulerException e) {
        LOG.error("Unable to schedule request execution.", e);
        throw new AmbariException(e.getMessage());
      }

    } else {
      // Create trigger for immediate job execution
      Trigger trigger = newTrigger()
        .forJob(firstJobDetail)
        .withIdentity(REQUEST_EXECUTION_TRIGGER_PREFIX + "-" +
          requestExecution.getId(), ExecutionJob.LINEAR_EXECUTION_TRIGGER_GROUP)
        .withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
        .usingJobData(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY, failedCount)
        .startNow()
        .build();

      try {
        executionScheduler.scheduleJob(trigger);
        LOG.debug("Scheduled trigger next fire time: {}", trigger.getNextFireTime());
      } catch (SchedulerException e) {
        LOG.error("Unable to schedule request execution.", e);
        throw new AmbariException(e.getMessage());
      }
    }
  }

  private Integer countFailedTasksBeforeStartingBatch(RequestExecution requestExecution, long startingBatchOrderId) throws AmbariException {
    int result = 0;
    Batch batch = requestExecution.getBatch();
    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null) {
        Collections.sort(batchRequests);
        for (BatchRequest batchRequest : batchRequests) {
          if (batchRequest.getOrderId() >= startingBatchOrderId) break;

          if (batchRequest.getRequestId() != null) {
            BatchRequestResponse batchRequestResponse = getBatchRequestResponse(batchRequest.getRequestId(), requestExecution.getClusterName());
            if (batchRequestResponse != null) {
               result += batchRequestResponse.getFailedTaskCount() +
                         batchRequestResponse.getAbortedTaskCount() +
                         batchRequestResponse.getTimedOutTaskCount();
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Creates and stores the chain of BatchRequestJobs - quartz jobs - in order from the last order Id to the startingBatchOrderId
   * @return the quartz job that corresponds to startingBatchOrderId
   */
  private JobDetail persistBatch(RequestExecution requestExecution, long startingBatchOrderId)
    throws  AmbariException {

    Batch batch = requestExecution.getBatch();
    JobDetail jobDetail = null;

    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null) {
        Collections.sort(batchRequests);
        ListIterator<BatchRequest> iterator = batchRequests.listIterator(batchRequests.size());
        String nextJobName = null;
        long nextBatchOrderId = Integer.MAX_VALUE/2;
        while (nextBatchOrderId != startingBatchOrderId && iterator.hasPrevious()) {
          BatchRequest batchRequest = iterator.previous();

          String jobName = getJobName(requestExecution.getId(),
            batchRequest.getOrderId());

          Integer separationSeconds = requestExecution.getBatch()
            .getBatchSettings().getBatchSeparationInSeconds();

          // Create Job and store properties to get next batch request details
          jobDetail = newJob(BatchRequestJob.class)
            .withIdentity(jobName, ExecutionJob.LINEAR_EXECUTION_JOB_GROUP)
            .usingJobData(ExecutionJob.NEXT_EXECUTION_JOB_NAME_KEY, nextJobName)
            .usingJobData(ExecutionJob.NEXT_EXECUTION_JOB_GROUP_KEY,
              ExecutionJob.LINEAR_EXECUTION_JOB_GROUP)
            .usingJobData(BatchRequestJob.BATCH_REQUEST_EXECUTION_ID_KEY,
              requestExecution.getId())
            .usingJobData(BatchRequestJob.BATCH_REQUEST_BATCH_ID_KEY,
              batchRequest.getOrderId())
            .usingJobData(BatchRequestJob.BATCH_REQUEST_CLUSTER_NAME_KEY,
              requestExecution.getClusterName())
            .usingJobData(BatchRequestJob.NEXT_EXECUTION_SEPARATION_SECONDS,
              separationSeconds != null ? separationSeconds : 0)
            .storeDurably()
            .build();

          try {
            executionScheduler.addJob(jobDetail);
          } catch (SchedulerException e) {
            LOG.error("Failed to add job detail. " + batchRequest, e);
          }

          nextJobName = jobName;
          nextBatchOrderId = batchRequest.getOrderId();
        }
      }
    }
    return jobDetail;
  }

  protected String getJobName(Long executionId, Long orderId) {
    return BATCH_REQUEST_JOB_PREFIX + "-" + executionId + "-" +
      orderId;
  }

  /**
   * Pause/resume/abort request schedule and related jobs and triggers
   * @param requestExecution
   */
  public void updateBatchSchedule(RequestExecution requestExecution)
    throws AmbariException {
    BatchRequest activeBatch = calculateActiveBatch(requestExecution);
    if (activeBatch == null) {
      LOG.warn("Ignoring RequestExecution status update since all batches has been executed");
      return;
    }
    if (requestExecution.getStatus().equals(SCHEDULED.name())) {
      scheduleBatch(requestExecution, activeBatch.getOrderId());
    } else if (requestExecution.getStatus().equals(PAUSED.name()) ||
               requestExecution.getStatus().equals(ABORTED.name())) {
      LOG.info("Request execution status changed to " + requestExecution.getStatus() + " for request schedule "
        + requestExecution.getId() + ". Deleting related jobs.");
      deleteJobs(requestExecution, activeBatch.getOrderId());
      Collection<Long> requestIDsToAbort = requestExecution.getBatchRequestRequestsIDs(activeBatch.getOrderId());
      for (Long requestId : requestIDsToAbort) {
        //might be null if the request is for not long running job
        if (requestId == null) continue;
        abortRequestById(requestExecution, requestId);
      }
    }
  }

  /**
   * Iterate through the batches and find the first one with not completed status, if all were completed return null
   * @param requestExecution
   * @return
   */
  private BatchRequest calculateActiveBatch(RequestExecution requestExecution) {
    BatchRequest result = null;
    Batch batch = requestExecution.getBatch();
    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null) {
        Collections.sort(batchRequests);
        ListIterator<BatchRequest> iterator = batchRequests.listIterator();
        do {
          result = iterator.next();
        } while (iterator.hasNext() && result.getStatus() != null &&
                 HostRoleStatus.getCompletedStates().contains(HostRoleStatus.valueOf(result.getStatus())) &&
                 !HostRoleStatus.ABORTED.name().equals(result.getStatus()));
      }
    }

    if (result != null && result.getStatus() != null &&
      HostRoleStatus.getCompletedStates().contains(HostRoleStatus.valueOf(result.getStatus())) &&
      !HostRoleStatus.ABORTED.name().equals(result.getStatus())) {
      return null;
    }

    return result;
  }

  /**
   * Validate if schedule expression is a valid Cron schedule
   * @param schedule
   * @return
   */
  public void validateSchedule(Schedule schedule) throws AmbariException {
    Date startDate = null;
    Date endDate = null;
    if (!schedule.isEmpty()) {
      if (schedule.getStartTime() != null && !schedule.getStartTime().isEmpty()) {
        try {
          startDate = DateUtils.convertToDate(schedule.getStartTime());
        } catch (ParseException pe) {
          throw new AmbariException("Start time in invalid format. startTime "
            + "= " + schedule.getStartTime() + ", Allowed format = "
            + DateUtils.ALLOWED_DATE_FORMAT);
        }
      }
      if (schedule.getEndTime() != null && !schedule.getEndTime().isEmpty()) {
        try {
          endDate = DateUtils.convertToDate(schedule.getEndTime());
        } catch (ParseException pe) {
          throw new AmbariException("End time in invalid format. endTime "
            + "= " + schedule.getEndTime() + ", Allowed format = "
            + DateUtils.ALLOWED_DATE_FORMAT);
        }
      }
      if (endDate != null) {
        if (endDate.before(new Date())) {
          throw new AmbariException("End date should be in the future. " +
            "endDate = " + endDate);
        }
        if (startDate != null && endDate.before(startDate)) {
          throw new AmbariException("End date cannot be before start date. " +
            "startDate = " + startDate + ", endDate = " + endDate);
        }
      }
      String cronExpression = schedule.getScheduleExpression();
      if (cronExpression != null && !cronExpression.trim().isEmpty()) {
        if (!CronExpression.isValidExpression(cronExpression)) {
          throw new AmbariException("Invalid non-empty cron expression " +
            "provided. " + cronExpression);
        }
      }
    }
  }

  /**
   * Delete all jobs and triggers if possible.
   * @throws AmbariException
   */
  public void deleteAllJobs(RequestExecution requestExecution) throws AmbariException {
    Long firstBatchOrderId = getFirstJobOrderId(requestExecution);
    deleteJobs(requestExecution, firstBatchOrderId);
  }

  /**
   * Delete all jobs and triggers if possible.
   * @throws AmbariException
   */
  public void deleteJobs(RequestExecution requestExecution, Long startingBatchOrderId) throws AmbariException {
    if (!isSchedulerAvailable()) {
      throw new AmbariException("Scheduler unavailable.");
    }

    // Delete all jobs for this request execution
    Batch batch = requestExecution.getBatch();
    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null) {
        Collections.sort(batchRequests);
        for (BatchRequest batchRequest : batchRequests) {
          //skip all before starting batch
          if (batchRequest.getOrderId() < startingBatchOrderId) continue;

          String jobName = getJobName(requestExecution.getId(),
            batchRequest.getOrderId());

          LOG.debug("Deleting Job, jobName = {}", jobName);

          try {
            executionScheduler.deleteJob(JobKey.jobKey(jobName,
              ExecutionJob.LINEAR_EXECUTION_JOB_GROUP));
          } catch (SchedulerException e) {
            LOG.warn("Unable to delete job, " + jobName, e);
            throw new AmbariException(e.getMessage());
          }
        }
      }
    }
  }

  /**
   * Execute a Batch request and return request id if the server responds with
   * a request id for long running operations.
   * @return request id
   * @throws AmbariException
   */
  public Long executeBatchRequest(long executionId,
                                  long batchId,
                                  String clusterName) throws AmbariException {

    String type = null;
    String uri = null;
    String body = null;

    try {
      RequestExecution requestExecution = clusters.getCluster(clusterName).getAllRequestExecutions().get(executionId);
      BatchRequest batchRequest = requestExecution.getBatchRequest(batchId);
      type = batchRequest.getType();
      uri = batchRequest.getUri();

      body = requestExecution.getRequestBody(batchId);

      BatchRequestResponse batchRequestResponse = performApiRequest(uri, body, type, requestExecution.getAuthenticatedUserId());

      updateBatchRequest(executionId, batchId, clusterName, batchRequestResponse, false);

      if (batchRequestResponse.getRequestId() != null) {
        actionDBAccessor.setSourceScheduleForRequest(batchRequestResponse.getRequestId(), executionId);
      }

      batchRequest.setRequestId(batchRequestResponse.getRequestId());

      return batchRequestResponse.getRequestId();
    } catch (Exception e) {
      throw new AmbariException("Exception occurred while performing request", e);
    }

  }

  /**
   * Get status of a long running operation
   * @return
   * @throws AmbariException
   */
  public BatchRequestResponse getBatchRequestResponse(Long requestId, String clusterName)
    throws AmbariException {

    StrBuilder sb = new StrBuilder();
    sb.append(DEFAULT_API_PATH)
      .append("/clusters/")
      .append(clusterName)
      .append("/requests/")
      .append(requestId);

    return performApiGetRequest(sb.toString(), true);

  }

  protected RequestStatus abortRequestById(RequestExecution requestExecution, Long requestId) throws AmbariException {
    LOG.debug("Aborting request " + requestId);
    ResourceProvider provider =
      AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Request);

    Map<String, Object> properties = new HashMap<>();
    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, requestExecution.getClusterName());
    properties.put(RequestResourceProvider.REQUEST_ABORT_REASON_PROPERTY_ID, "Request execution status changed to " + requestExecution.getStatus());
    properties.put(RequestResourceProvider.REQUEST_ID_PROPERTY_ID, Long.toString(requestId));
    properties.put(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID, HostRoleStatus.ABORTED.name());

    org.apache.ambari.server.controller.spi.Request request = new RequestImpl(Collections.emptySet(),
        Collections.singleton(properties), Collections.emptyMap(), null);

    Predicate predicate =  new PredicateBuilder()
        .property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID)
        .equals(requestExecution.getClusterName()).and()
        .property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID)
        .equals(Long.toString(requestId)).toPredicate();

    try{
      return provider.updateResources(request, predicate);
    } catch (Exception e) {
     throw new AmbariException("Error while aborting the request.", e);
    }
  }

  private BatchRequestResponse convertToBatchRequestResponse(ClientResponse clientResponse) {
    BatchRequestResponse batchRequestResponse = new BatchRequestResponse();
    int retCode = clientResponse.getStatus();

    batchRequestResponse.setReturnCode(retCode);

    String responseString = clientResponse.getEntity(String.class);
    LOG.debug("Processing API response: status={}, body={}", retCode, responseString);
    Map<String, Object> httpResponseMap;
    try {
      httpResponseMap = gson.<Map<String, Object>>fromJson(responseString, Map.class);
      LOG.debug("Processing responce as JSON");
    } catch (JsonSyntaxException e) {
      LOG.debug("Response is not valid JSON object. Recording as is");
      httpResponseMap = new HashMap<>();
      httpResponseMap.put("message", responseString);
    }


    if (retCode < 300) {
      if (httpResponseMap == null) {
        //Empty response on successful scenario
        batchRequestResponse.setStatus(HostRoleStatus.COMPLETED.toString());
        return batchRequestResponse;
      }

      Map requestMap = null;
      Object requestMapObject = httpResponseMap.get("Requests");
      if (requestMapObject instanceof Map) {
        requestMap = (Map) requestMapObject;
      }

      if (requestMap != null) {
        batchRequestResponse.setRequestId((
          (Double) requestMap.get(REQUESTS_ID_KEY)).longValue());
        //TODO fix different names for field
        String status = null;
        if (requestMap.get(REQUESTS_STATUS_KEY) != null) {
          status = requestMap.get(REQUESTS_STATUS_KEY).toString();
        }
        if (requestMap.get("status") != null) {
          status = requestMap.get("status").toString();
        }

        if (requestMap.get(REQUESTS_ABORTED_TASKS_KEY) != null) {
          batchRequestResponse.setAbortedTaskCount(
            ((Double) requestMap.get(REQUESTS_ABORTED_TASKS_KEY)).intValue());
        }
        if (requestMap.get(REQUESTS_FAILED_TASKS_KEY) != null) {
          batchRequestResponse.setFailedTaskCount(
            ((Double) requestMap.get(REQUESTS_FAILED_TASKS_KEY)).intValue());
        }
        if (requestMap.get(REQUESTS_TIMEDOUT_TASKS_KEY) != null) {
          batchRequestResponse.setTimedOutTaskCount(
            ((Double) requestMap.get(REQUESTS_TIMEDOUT_TASKS_KEY)).intValue());
        }
        if (requestMap.get(REQUESTS_TOTAL_TASKS_KEY) != null) {
          batchRequestResponse.setTotalTaskCount(
            ((Double) requestMap.get(REQUESTS_TOTAL_TASKS_KEY)).intValue());
        }
        batchRequestResponse.setStatus(status);
      }

    } else {
      //unsuccessful response
      batchRequestResponse.setReturnMessage((String) httpResponseMap.get("message"));
      batchRequestResponse.setStatus(HostRoleStatus.FAILED.toString());
    }

    return batchRequestResponse;
  }

  public String getBatchRequestStatus(Long executionId, String clusterName) throws AmbariException {
    Cluster cluster = clusters.getCluster(clusterName);
    RequestExecution requestExecution = cluster.getAllRequestExecutions().get(executionId);

    if (requestExecution == null) {
      throw new AmbariException("Unable to find request schedule with id = "
          + executionId);
    }

    return requestExecution.getStatus();
  }

  public void updateBatchRequest(long executionId, long batchId, String clusterName,
                                 BatchRequestResponse batchRequestResponse,
                                 boolean statusOnly) throws AmbariException {

    Cluster cluster = clusters.getCluster(clusterName);
    RequestExecution requestExecution = cluster.getAllRequestExecutions().get(executionId);

    if (requestExecution == null) {
      throw new AmbariException("Unable to find request schedule with id = "
        + executionId);
    }

    requestExecution.updateBatchRequest(batchId, batchRequestResponse, statusOnly);
  }

  protected BatchRequestResponse performUriRequest(String url, String body, String method) {
    ClientResponse response;
    try {
      response = ambariClient.resource(url).entity(body).method(method, ClientResponse.class);
    } catch (UniformInterfaceException e) {
      response = e.getResponse();
    }
    //Don't read response entity for logging purposes, it can be read only once from http stream

    return convertToBatchRequestResponse(response);
  }

  protected BatchRequestResponse performApiGetRequest(String relativeUri, boolean queryAllFields) {
    WebResource webResource = extendApiResource(ambariWebResource, relativeUri);
    if (queryAllFields) {
      webResource = webResource.queryParam("fields", "*");
    }
    ClientResponse response;
    try {
      response = webResource.get(ClientResponse.class);
    } catch (UniformInterfaceException e) {
      response = e.getResponse();
    }
    return convertToBatchRequestResponse(response);
  }

  protected BatchRequestResponse performApiRequest(String relativeUri, String body, String method, Integer userId) {
    ClientResponse response;
    try {
      response = extendApiResource(ambariWebResource, relativeUri)
          .header(USER_ID_HEADER, userId).method(method, ClientResponse.class, body);
    } catch (UniformInterfaceException e) {
      response = e.getResponse();
    }

    return convertToBatchRequestResponse(response);
  }

  /**
   * Check if the allowed threshold for failed tasks has exceeded.
   * This needs to be an absolute value of tasks.
   * @param executionId
   * @param clusterName
   * @param taskCounts
   * @return
   * @throws AmbariException
   */
  public boolean hasToleranceThresholdExceeded(Long executionId,
      String clusterName, Map<String, Integer> taskCounts) throws AmbariException {

    Cluster cluster = clusters.getCluster(clusterName);
    RequestExecution requestExecution = cluster.getAllRequestExecutions().get(executionId);

    if (requestExecution == null) {
      throw new AmbariException("Unable to find request schedule with id = "
        + executionId);
    }

    BatchSettings batchSettings = requestExecution.getBatch().getBatchSettings();

    boolean result = false;
    if (batchSettings != null) {
      if (batchSettings.getTaskFailureToleranceLimit() != null) {
        result = taskCounts.get(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY) > batchSettings.getTaskFailureToleranceLimit();
      }
      if (batchSettings.getTaskFailureToleranceLimitPerBatch() != null) {
        result = result || taskCounts.get(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_IN_CURRENT_BATCH_KEY) >
            batchSettings.getTaskFailureToleranceLimitPerBatch();
      }
    }

    return result;
  }

  /**
   * Marks Request Schedule as COMPLETED, if:
   * No triggers exist for the first job in the chain OR
   * If the trigger will never fire again.
   *
   * @param executionId
   * @param clusterName
   * @throws AmbariException
   */
  public void finalizeBatch(long executionId, String clusterName)
    throws AmbariException {

    Cluster cluster = clusters.getCluster(clusterName);
    RequestExecution requestExecution = cluster.getAllRequestExecutions().get(executionId);

    if (requestExecution == null) {
      throw new AmbariException("Unable to find request schedule with id = "
        + executionId);
    }

    Batch batch = requestExecution.getBatch();
    BatchRequest firstBatchRequest = null;

    if (batch != null) {
      List<BatchRequest> batchRequests = batch.getBatchRequests();
      if (batchRequests != null && batchRequests.size() > 0) {
        Collections.sort(batchRequests);
        firstBatchRequest = batchRequests.get(0);
      }
    }

    boolean markCompleted = false;

    if (firstBatchRequest != null) {
      String jobName = getJobName(executionId, firstBatchRequest.getOrderId());
      JobKey jobKey = JobKey.jobKey(jobName, ExecutionJob.LINEAR_EXECUTION_JOB_GROUP);
      JobDetail jobDetail;
      try {
        jobDetail = executionScheduler.getJobDetail(jobKey);
      } catch (SchedulerException e) {
        LOG.warn("Unable to retrieve job details from scheduler. job: " + jobKey);
        e.printStackTrace();
        return;
      }

      if (jobDetail != null) {
        try {
          List<? extends Trigger> triggers = executionScheduler.getTriggersForJob(jobKey);
          if (triggers != null && triggers.size() > 0) {
            if (triggers.size() > 1) {
              throw new AmbariException("Too many triggers defined for job. " +
                "job: " + jobKey);
            }

            Trigger trigger = triggers.get(0);
            // Note: If next fire time is in the past, it could be a misfire
            // If final fire time is null, means it is a forever running job
            if (!trigger.mayFireAgain() ||
                (trigger.getFinalFireTime() != null &&
                  !DateUtils.isFutureTime(trigger.getFinalFireTime()))) {
              markCompleted = true;
            }
          } else {
            // No triggers for job
            markCompleted = true;
          }
        } catch (SchedulerException e) {
          LOG.warn("Unable to retrieve triggers for job: " + jobKey);
          e.printStackTrace();
          return;
        }
      }
    }

    if (markCompleted) {
      requestExecution.updateStatus(RequestExecution.Status.COMPLETED);
    }
  }

  /**
   * Returns the absolute web resource with {@link #DEFAULT_API_PATH}
   * @param webResource Ambari WebResource as provided by the client {@link #ambariWebResource}
   * @param relativeUri relative request URI
   * @return  Extended WebResource
   */
  protected WebResource extendApiResource(WebResource webResource, String relativeUri) {
    WebResource result = webResource;
    if (StringUtils.isNotEmpty(relativeUri) && !CONTAINS_API_VERSION_PATTERN.matcher(relativeUri).matches()) {
      result = webResource.path(DEFAULT_API_PATH);
    }
    return result.path(relativeUri);
  }

  /**
   * Checks if scheduled request should be auto paused and updates the status to PAUSED if it does.
   * For now the condition is following: the current status is SCHEDULED,
   * it's the first batch and the pauseAfterFirstBatch flag is TRUE
   */
  public void pauseAfterBatchIfNeeded(long executionId, long batchId, String clusterName) throws AmbariException {
    Cluster cluster = clusters.getCluster(clusterName);
    RequestExecution requestExecution = cluster.getAllRequestExecutions().get(executionId);

    if (requestExecution == null) {
      throw new AmbariException("Unable to find request schedule with id = "
        + executionId);
    }

    Batch batch = requestExecution.getBatch();
    if (batch != null) {
      BatchSettings batchSettings = batch.getBatchSettings();
      if (batchSettings != null) {
        if (SCHEDULED.name().equals(requestExecution.getStatus()) && getFirstJobOrderId(requestExecution) == batchId &&
            batchSettings.isPauseAfterFirstBatch()) {
          LOG.info("Auto pausing the scheduled request after first batch. Scheduled request ID : " + executionId);
          requestExecution.updateStatus(PAUSED);
        }
      }
    }
  }
}

