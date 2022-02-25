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

import java.util.Collection;

import org.apache.ambari.server.controller.RequestScheduleResponse;

/**
 * Request Execution is a type of resource that supports scheduling a request
 * or a group of requests for execution by the ActionManager.
 */
public interface RequestExecution {
  /**
   * Primary key of Request Execution
   * @return
   */
  Long getId();

  /**
   * Cluster name to which request schedule belongs
   * @return
   */
  String getClusterName();

  /**
   * Get the batch of requests along with batch settings
   * @return
   */
  Batch getBatch();

  /**
   * Set batch of requests and batch settings
   */
  void setBatch(Batch batch);

  /**
   * Get schedule for the execution
   * @return
   */
  Schedule getSchedule();

  /**
   * Set schedule for the execution
   */
  void setSchedule(Schedule schedule);

  /**
   * Get @RequestScheduleResponse for this Request Execution
   * @return
   */
  RequestScheduleResponse convertToResponse();

  /**
   * Persist the Request Execution and schedule
   */
  void persist();

  /**
   * Refresh entity from DB.
   */
  void refresh();

  /**
   * Delete Request Schedule entity
   */
  void delete();

  /**
   * Get status of schedule
   */
  String getStatus();

  /**
   * Set request execution description
   */
  void setDescription(String description);

  /**
   * Get description of the request execution
   */
  String getDescription();

  /**
   * Set status of the schedule
   */
  void setStatus(Status status);

  /**
   * Set datetime:status of last request that was executed
   */
  void setLastExecutionStatus(String status);

  /**
   * Set authenticated user
   */
  void setAuthenticatedUserId(Integer username);

  /**
   * Set create username
   */
  void setCreateUser(String username);

  /**
   * Set create username
   */
  void setUpdateUser(String username);

  /**
   * Get created time
   */
  String getCreateTime();

  /**
   * Get updated time
   */
  String getUpdateTime();

  /**
   * Get authenticated user
   */
  Integer getAuthenticatedUserId();

  /**
   * Get create user
   */
  String getCreateUser();

  /**
   * Get update user
   */
  String getUpdateUser();

  /**
   * Get status of the last batch of requests
   * @return
   */
  String getLastExecutionStatus();

  /**
   * Get response with request body
   */
  RequestScheduleResponse convertToResponseWithBody();

  /**
   * Get the request body for a batch request
   */
  String getRequestBody(Long batchId);

  /**
   * Get the requests IDs for the batch
   */
  Collection<Long> getBatchRequestRequestsIDs(long batchId);
  /**
   * Get batch request with specified order id
   */
  BatchRequest getBatchRequest(long batchId);

  /**
   * Updates batch request data
   * @param batchId order id of batch request
   * @param batchRequestResponse
   * @param statusOnly true if only status should be updated
   */
  void updateBatchRequest(long batchId, BatchRequestResponse batchRequestResponse, boolean statusOnly);

  /**
   * Update status and save RequestExecution
   */
  void updateStatus(Status status);

  /**
   * Status of the Request execution
   */
  enum Status {
    SCHEDULED,
    COMPLETED,
    DISABLED,
    ABORTED,
    PAUSED
  }
}
