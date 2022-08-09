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
package org.apache.ambari.server.actionmanager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;

public interface ActionDBAccessor {

  /**
   * Given an action id of the form requestId-stageId, retrieve the Stage
   */
  Stage getStage(String actionId);

  /**
   * Get all stages associated with a single request id
   */
  List<Stage> getAllStages(long requestId);

  /**
   * Gets the request entity by id.  Will not load the entire
   * Request/Stage/HostRoleCommand object hierarchy.
   */
  RequestEntity getRequestEntity(long requestId);

  /**
   * Get request object by id.  USE WITH CAUTION!  This method will
   * result in loading the full object hierarchy, and can be expensive to
   * construct.
   * @param requestId the request id
   * @return the Request instance, with all Stages and HostRoleCommands constructed
   *        from the database
   */
  Request getRequest(long requestId);

  /**
   * Abort all outstanding operations associated with the given request. This
   * method uses the {@link HostRoleStatus#SCHEDULED_STATES} to determine which
   * {@link HostRoleCommand} instances to abort.
   *
   * Returns the list of the aborted operations.
   */
  Collection<HostRoleCommandEntity> abortOperation(long requestId);

  /**
   * Mark the task as to have timed out
   */
  void timeoutHostRole(String host, long requestId, long stageId, String role);

  /**
   * Mark the task as to have timed out
   */
  void timeoutHostRole(String host, long requestId, long stageId, String role,
                       boolean skipSupported, boolean hostUnknownState);

  /**
   * Returns the next stage which is in-progress for every in-progress request
   * in the system. Since stages are always synchronous, there is no reason to
   * return more than the most recent stage per request. Returning every single
   * stage in the requesrt would be extremely inffecient and wasteful. However,
   * since requests can run in parallel, this method must return the most recent
   * stage for every request. The results will be sorted by request ID.
   * <p/>
   * Use {@link #getCommandsInProgressCount()} in order to determine if there
   * are stages that are in progress before getting the stages from this method.
   *
   * @see HostRoleStatus#IN_PROGRESS_STATUSES
   */
  List<Stage> getFirstStageInProgressPerRequest();

  /**
   * Returns all the pending stages in a request, including queued and not-queued. A stage is
   * considered in progress if it is in progress for any host.
   * <p/>
   * The results will be sorted by stage ID making this call
   * expensive in some scenarios. Use {@link #getCommandsInProgressCount()} in
   * order to determine if there are stages that are in progress before getting
   * the stages from this method.
   *
   * @see HostRoleStatus#IN_PROGRESS_STATUSES
   */
  List<Stage> getStagesInProgressForRequest(Long requestId);

  /**
   * Gets the number of commands in progress.
   *
   * @return the number of commands in progress.
   */
  int getCommandsInProgressCount();

  /**
   * Persists all tasks for a given request
   *
   * @param request
   *          request object
   */
  void persistActions(Request request) throws AmbariException;

  void startRequest(long requestId);

  void endRequest(long requestId);

  /**
   * Updates request with link to source schedule
   */
  void setSourceScheduleForRequest(long requestId, long scheduleId);

  /**
   * Update tasks according to command reports
   */
  void updateHostRoleStates(Collection<CommandReport> reports);

  /**
   * For the given host, update all the tasks based on the command report
   */
  void updateHostRoleState(String hostname, long requestId,
                           long stageId, String role, CommandReport report);

  /**
   * Mark the task as to have been aborted
   */
  void abortHostRole(String host, long requestId, long stageId, String role);

  /**
   * Mark the task as to have been aborted. Reason should be specified manually.
   */
  void abortHostRole(String host, long requestId, long stageId,
                     String role, String reason);

  /**
   * Return the last persisted Request ID as seen when the DBAccessor object
   * was initialized.
   * Value should remain unchanged through the lifetime of the object instance.
   *
   * @return Request Id seen at init time
   */
  long getLastPersistedRequestIdWhenInitialized();

  /**
   * Bulk update scheduled commands
   */
  void bulkHostRoleScheduled(Stage s, List<ExecutionCommand> commands);

  /**
   * Bulk abort commands
   */
  void bulkAbortHostRole(Stage s, Map<ExecutionCommand, String> commands);

  /**
   * Updates scheduled stage.
   */
  void hostRoleScheduled(Stage s, String hostname, String roleStr);

  /**
   * Given a request id, get all the tasks that belong to this request
   */
  List<HostRoleCommand> getRequestTasks(long requestId);

  /**
   * Given a list of request ids, get all the tasks that belong to these requests
   */
  List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds);

  /**
   * Given a list of task ids, get all the host role commands
   */
  Collection<HostRoleCommand> getTasks(Collection<Long> taskIds);

  /**
   * Get a List of host role commands where the host, role and status are as specified
   */
  List<HostRoleCommand> getTasksByHostRoleAndStatus(String hostname, String role, HostRoleStatus status);

  /**
   * Get a List of host role commands where the role and status are as specified
   */
  List<HostRoleCommand> getTasksByRoleAndStatus(String role, HostRoleStatus status);

  /**
   * Gets the host role command corresponding to the task id
   */
  HostRoleCommand getTask(long taskId);

  /**
   * Get first or last maxResults requests that are in the specified status
   *
   * @param status
   *          Desired request status
   * @param maxResults
   *          maximal number of returned id's
   * @param ascOrder
   *          defines sorting order for database query result
   * @return First or last maxResults request id's if ascOrder is true or false,
   *         respectively
   */
  List<Long> getRequestsByStatus(RequestStatus status, int maxResults, boolean ascOrder);

  /**
   * Gets request contexts associated with the list of request id
   */
  Map<Long, String> getRequestContext(List<Long> requestIds);

  /**
   * Gets the request context associated with the request id
   */
  String getRequestContext(long requestId);

  /**
   * Gets request objects by ids
   */
  List<Request> getRequests(Collection<Long> requestIds);

  /**
   * Resubmits a series of tasks
   * @param taskIds
   */
  void resubmitTasks(List<Long> taskIds);



}
