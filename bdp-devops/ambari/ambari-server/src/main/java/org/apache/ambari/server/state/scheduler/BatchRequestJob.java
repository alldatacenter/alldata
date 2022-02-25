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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.scheduler.AbstractLinearExecutionJob;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class BatchRequestJob extends AbstractLinearExecutionJob {
  private static final Logger LOG = LoggerFactory.getLogger(BatchRequestJob.class);

  public static final String BATCH_REQUEST_EXECUTION_ID_KEY =
    "BatchRequestJob.ExecutionId";
  public static final String BATCH_REQUEST_BATCH_ID_KEY =
    "BatchRequestJob.BatchId";
  public static final String BATCH_REQUEST_CLUSTER_NAME_KEY =
    "BatchRequestJob.ClusterName";
  public static final String BATCH_REQUEST_FAILED_TASKS_KEY =
    "BatchRequestJob.FailedTaskCount";
  public static final String BATCH_REQUEST_FAILED_TASKS_IN_CURRENT_BATCH_KEY =
    "BatchRequestJob.FailedTaskInCurrentBatchCount";
  public static final String BATCH_REQUEST_TOTAL_TASKS_KEY =
    "BatchRequestJob.TotalTaskCount";

  private final long statusCheckInterval;

  @Inject
  public BatchRequestJob(ExecutionScheduleManager executionScheduleManager,
                         @Named("statusCheckInterval") long statusCheckInterval) {
    super(executionScheduleManager);
    this.statusCheckInterval = statusCheckInterval;
  }

  @Override
  protected void doWork(Map<String, Object> properties) throws AmbariException {

    Long executionId = properties.get(BATCH_REQUEST_EXECUTION_ID_KEY) != null ?
      (Long) properties.get(BATCH_REQUEST_EXECUTION_ID_KEY) : null;
    Long batchId = properties.get(BATCH_REQUEST_BATCH_ID_KEY) != null ?
      (Long) properties.get(BATCH_REQUEST_BATCH_ID_KEY) : null;
    String clusterName = (String) properties.get(BATCH_REQUEST_CLUSTER_NAME_KEY);


    if (executionId == null || batchId == null) {
      throw new AmbariException("Unable to retrieve persisted batch request"
        + ", execution_id = " + executionId
        + ", batch_id = " + batchId);
    }

    // Aggregate tasks counts stored in the DataMap
    Map<String, Integer> taskCounts = getTaskCountProperties(properties);

    Long requestId = executionScheduleManager.executeBatchRequest
      (executionId, batchId, clusterName);

    if (requestId != null) {
      HostRoleStatus status;
      BatchRequestResponse batchRequestResponse;
      do {
        batchRequestResponse = executionScheduleManager
          .getBatchRequestResponse(requestId, clusterName);

        status = HostRoleStatus.valueOf(batchRequestResponse.getStatus());

        executionScheduleManager.updateBatchRequest(executionId, batchId,
          clusterName, batchRequestResponse, true);

        try {
          Thread.sleep(statusCheckInterval);
        } catch (InterruptedException e) {
          String message = "Job Thread interrupted";
          LOG.error(message, e);
          throw new AmbariException(message, e);
        }
      } while (!status.isCompletedState());

      // Store aggregated task status counts in the DataMap
      Map<String, Integer> aggregateCounts = addTaskCountToProperties
        (properties, taskCounts, batchRequestResponse);

      if (executionScheduleManager.hasToleranceThresholdExceeded
          (executionId, clusterName, aggregateCounts)) {

        throw new AmbariException("Task failure tolerance limit exceeded"
            + ", execution_id = " + executionId
            + ", processed batch_id = " + batchId
            + ", failed tasks in current batch = " + aggregateCounts.get(BATCH_REQUEST_FAILED_TASKS_IN_CURRENT_BATCH_KEY)
            + ", failed tasks total = " + aggregateCounts.get(BATCH_REQUEST_FAILED_TASKS_KEY)
            + ", total tasks completed = " + aggregateCounts.get(BATCH_REQUEST_TOTAL_TASKS_KEY));
      }
    }
  }

  @Override
  protected void finalizeExecution(Map<String, Object> properties)
      throws AmbariException {

    Long executionId = properties.get(BATCH_REQUEST_EXECUTION_ID_KEY) != null ?
      (Long) properties.get(BATCH_REQUEST_EXECUTION_ID_KEY) : null;
    Long batchId = properties.get(BATCH_REQUEST_BATCH_ID_KEY) != null ?
      (Long) properties.get(BATCH_REQUEST_BATCH_ID_KEY) : null;
    String clusterName = (String) properties.get(BATCH_REQUEST_CLUSTER_NAME_KEY);

    if (executionId == null || batchId == null) {
      throw new AmbariException("Unable to retrieve persisted batch request"
        + ", execution_id = " + executionId
        + ", batch_id = " + batchId);
    }

    // Check if this job has a future and update status if it doesn't
    executionScheduleManager.finalizeBatch(executionId, clusterName);

  }

  private Map<String, Integer> addTaskCountToProperties(Map<String, Object> properties,
                                        Map<String, Integer> oldCounts,
                                        BatchRequestResponse batchRequestResponse) {

    Map<String, Integer> taskCounts = new HashMap<>();

    if (batchRequestResponse != null) {
      Integer failedTasks = batchRequestResponse.getFailedTaskCount() +
        batchRequestResponse.getAbortedTaskCount() +
        batchRequestResponse.getTimedOutTaskCount();

      Integer failedCount = oldCounts.get(BATCH_REQUEST_FAILED_TASKS_KEY) + failedTasks;
      Integer totalCount = oldCounts.get(BATCH_REQUEST_TOTAL_TASKS_KEY) +
        batchRequestResponse.getTotalTaskCount();

      taskCounts.put(BATCH_REQUEST_FAILED_TASKS_KEY, failedCount);
      taskCounts.put(BATCH_REQUEST_FAILED_TASKS_IN_CURRENT_BATCH_KEY, batchRequestResponse.getFailedTaskCount());
      taskCounts.put(BATCH_REQUEST_TOTAL_TASKS_KEY, totalCount);

      properties.put(BATCH_REQUEST_FAILED_TASKS_KEY, failedCount);
      properties.put(BATCH_REQUEST_FAILED_TASKS_IN_CURRENT_BATCH_KEY, batchRequestResponse.getFailedTaskCount());
      properties.put(BATCH_REQUEST_TOTAL_TASKS_KEY, totalCount);
    }

    return taskCounts;
  }

  private Map<String, Integer> getTaskCountProperties(Map<String, Object> properties) {
    Map<String, Integer> taskCounts = new HashMap<>();
    if (properties != null) {
      Object countObj = properties.get(BATCH_REQUEST_FAILED_TASKS_KEY);
      taskCounts.put(BATCH_REQUEST_FAILED_TASKS_KEY,
        countObj != null ? Integer.parseInt(countObj.toString()) : 0);
      countObj = properties.get(BATCH_REQUEST_TOTAL_TASKS_KEY);
      taskCounts.put(BATCH_REQUEST_TOTAL_TASKS_KEY, countObj != null ?
        Integer.parseInt(countObj.toString()) : 0);
    }
    return taskCounts;
  }
}
