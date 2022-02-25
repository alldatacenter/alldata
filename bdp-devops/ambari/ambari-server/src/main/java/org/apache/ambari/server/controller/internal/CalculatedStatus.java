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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.events.listeners.tasks.TaskStatusListener;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * Status of a request resource, calculated from a set of tasks or stages.
 */
public class CalculatedStatus {

  /**
   * The calculated overall status.
   */
  private final HostRoleStatus status;

  /**
   * The display status.
   */
  private final HostRoleStatus displayStatus;

  /**
   * The calculated percent complete.
   */
  private final double percent;

  /**
   * A status which represents a COMPLETED state at 100%
   */
  public static final CalculatedStatus COMPLETED = new CalculatedStatus(HostRoleStatus.COMPLETED,
      HostRoleStatus.COMPLETED, 100.0);

  /**
   * A status which represents a PENDING state at 0%
   */
  public static final CalculatedStatus PENDING = new CalculatedStatus(HostRoleStatus.PENDING,
      HostRoleStatus.PENDING, 0.0);

  /**
   * A status which represents an ABORTED state at -1%
   */
  public static final CalculatedStatus ABORTED = new CalculatedStatus(HostRoleStatus.ABORTED, HostRoleStatus.ABORTED, -1);

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param status   the calculated overall status
   * @param percent  the calculated percent complete
   */
  private CalculatedStatus(HostRoleStatus status, double percent) {
    this(status, null, percent);
  }

  /**
   * Overloaded constructor that allows to set the display status if required.
   *
   * @param status   the calculated overall status
   * @param displayStatus the calculated display status
   * @param percent  the calculated percent complete
   */
  private CalculatedStatus(HostRoleStatus status, HostRoleStatus displayStatus, double percent) {
    this.status  = status;
    this.displayStatus  = displayStatus;
    this.percent = percent;
  }


  // ----- CalculatedStatus --------------------------------------------------

  /**
   * Get the calculated status.
   *
   * @return the status
   */
  public HostRoleStatus getStatus() {
    return status;
  }

  /**
   * Get the calculated display status. The display_status field is used as
   * a hint for UI. It's effective only on UpgradeGroup level.
   * We should expose it for the following states:
   * SKIPPED_FAILED
   * FAILED
   *
   * @return the display status
   */
  public HostRoleStatus getDisplayStatus() {
    return displayStatus;
  }

  /**
   * Get the calculated percent complete.
   *
   * @return the percent complete
   */
  public double getPercent() {
    return percent;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Factory method to create a calculated status.  Calculate request status from the given
   * collection of task entities.
   *
   * @param tasks      the collection of task entities
   * @param skippable  true if a single failed status should NOT result in an overall failed status
   *
   * @return a calculated status
   */
  public static CalculatedStatus statusFromTaskEntities(Collection<HostRoleCommandEntity> tasks, boolean skippable) {

    int size = tasks.size();

    Map<HostRoleStatus, Integer> taskStatusCounts = CalculatedStatus.calculateTaskEntityStatusCounts(tasks);

    HostRoleStatus status = calculateSummaryStatus(taskStatusCounts, size, skippable);

    double progressPercent = calculateProgressPercent(taskStatusCounts, size);

    return new CalculatedStatus(status, progressPercent);
  }

  /**
   * Factory method to create a calculated status.  Calculate request status from the given
   * collection of stage entities.
   *
   * @param stages  the collection of stage entities
   *
   * @return a calculated status
   */
  public static CalculatedStatus statusFromStageEntities(Collection<StageEntity> stages) {
    Collection<HostRoleStatus> stageStatuses = new HashSet<>();
    Collection<HostRoleCommandEntity> tasks = new HashSet<>();

    for (StageEntity stage : stages) {
      // get all the tasks for the stage
      Collection<HostRoleCommandEntity> stageTasks = stage.getHostRoleCommands();

      // calculate the stage status from the task status counts
      HostRoleStatus stageStatus =
          calculateSummaryStatus(calculateTaskEntityStatusCounts(stageTasks), stageTasks.size(), stage.isSkippable());

      stageStatuses.add(stageStatus);

      // keep track of all of the tasks for all stages
      tasks.addAll(stageTasks);
    }

    // calculate the overall status from the stage statuses
    HostRoleStatus status = calculateSummaryStatusOfUpgrade(calculateStatusCounts(stageStatuses), stageStatuses.size());

    // calculate the progress from the task status counts
    double progressPercent = calculateProgressPercent(calculateTaskEntityStatusCounts(tasks), tasks.size());

    return new CalculatedStatus(status, progressPercent);
  }

  /**
   * Factory method to create a calculated status.  Calculate request status from the given
   * collection of stages.
   *
   * @param stages  the collection of stages
   *
   * @return a calculated status
   */
  public static CalculatedStatus statusFromStages(Collection<Stage> stages) {

    Collection<HostRoleStatus> stageStatuses = new HashSet<>();
    Collection<HostRoleCommand> tasks = new HashSet<>();

    for (Stage stage : stages) {
      // get all the tasks for the stage
      Collection<HostRoleCommand> stageTasks = stage.getOrderedHostRoleCommands();

      // calculate the stage status from the task status counts
      HostRoleStatus stageStatus =
          calculateSummaryStatus(calculateTaskStatusCounts(stageTasks), stageTasks.size(), stage.isSkippable());

      stageStatuses.add(stageStatus);

      // keep track of all of the tasks for all stages
      tasks.addAll(stageTasks);
    }

    // calculate the overall status from the stage statuses
    HostRoleStatus status = calculateSummaryStatusOfUpgrade(calculateStatusCounts(stageStatuses), stageStatuses.size());

    // calculate the progress from the task status counts
    double progressPercent = calculateProgressPercent(calculateTaskStatusCounts(tasks), tasks.size());

    return new CalculatedStatus(status, progressPercent);
  }

  /**
   * Returns counts of tasks that are in various states.
   *
   * @param hostRoleStatuses  the collection of tasks
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<HostRoleStatus, Integer> calculateStatusCounts(Collection<HostRoleStatus> hostRoleStatuses) {
    Map<HostRoleStatus, Integer> counters = new HashMap<>();
    // initialize
    for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
      counters.put(hostRoleStatus, 0);
    }
    // calculate counts
    for (HostRoleStatus status : hostRoleStatuses) {
      // count tasks where isCompletedState() == true as COMPLETED
      // but don't count tasks with COMPLETED status twice
      if (status.isCompletedState() && status != HostRoleStatus.COMPLETED) {
        // Increase total number of completed tasks;
        counters.put(HostRoleStatus.COMPLETED, counters.get(HostRoleStatus.COMPLETED) + 1);
      }
      // Increment counter for particular status
      counters.put(status, counters.get(status) + 1);
    }

    // We overwrite the value to have the sum converged
    counters.put(HostRoleStatus.IN_PROGRESS,
        hostRoleStatuses.size() -
            counters.get(HostRoleStatus.COMPLETED) -
            counters.get(HostRoleStatus.QUEUED) -
            counters.get(HostRoleStatus.PENDING));

    return counters;
  }

  /**
   * Returns counts of tasks that are in various states.
   *
   * @param hostRoleCommands  collection of beans {@link HostRoleCommand}
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<HostRoleStatus, Integer> calculateStatusCountsForTasks(Collection<HostRoleCommand> hostRoleCommands) {
    Map<HostRoleStatus, Integer> counters = new HashMap<>();
    // initialize
    for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
      counters.put(hostRoleStatus, 0);
    }
    // calculate counts
    for (HostRoleCommand hrc : hostRoleCommands) {
      // count tasks where isCompletedState() == true as COMPLETED
      // but don't count tasks with COMPLETED status twice
      if (hrc.getStatus().isCompletedState() && hrc.getStatus() != HostRoleStatus.COMPLETED) {
        // Increase total number of completed tasks;
        counters.put(HostRoleStatus.COMPLETED, counters.get(HostRoleStatus.COMPLETED) + 1);
      }
      // Increment counter for particular status
      counters.put(hrc.getStatus(), counters.get(hrc.getStatus()) + 1);
    }

    // We overwrite the value to have the sum converged
    counters.put(HostRoleStatus.IN_PROGRESS,
        hostRoleCommands.size() -
            counters.get(HostRoleStatus.COMPLETED) -
            counters.get(HostRoleStatus.QUEUED) -
            counters.get(HostRoleStatus.PENDING));

    return counters;
  }

  /**
   * Returns map for counts of stages that are in various states.
   *
   * @param stages  collection of beans {@link org.apache.ambari.server.events.listeners.tasks.TaskStatusListener.ActiveStage}
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<StatusType,Map<HostRoleStatus, Integer>> calculateStatusCountsForStage(Collection<TaskStatusListener.ActiveStage> stages) {

    Map<StatusType,Map<HostRoleStatus, Integer>> counters = new HashMap<>();
    for (StatusType statusType : StatusType.values()) {
      Map <HostRoleStatus, Integer> statusMap = new HashMap<>();
      counters.put(statusType,statusMap);
      // initialize
      for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
        statusMap.put(hostRoleStatus, 0);
      }
      for (TaskStatusListener.ActiveStage stage : stages) {
        // count tasks where isCompletedState() == true as COMPLETED
        // but don't count tasks with COMPLETED status twice
        HostRoleStatus status;
        if (statusType == StatusType.DISPLAY_STATUS) {
          status = stage.getDisplayStatus();
        } else {
          status = stage.getStatus();
        }
        if (status.isCompletedState() && status != HostRoleStatus.COMPLETED) {
          // Increase total number of completed tasks;
          statusMap.put(HostRoleStatus.COMPLETED, statusMap.get(HostRoleStatus.COMPLETED) + 1);
        }

        // Increment counter for particular status
        statusMap.put(status, statusMap.get(status) + 1);
      }
      statusMap.put(HostRoleStatus.IN_PROGRESS,
          stages.size() -
              statusMap.get(HostRoleStatus.COMPLETED) -
              statusMap.get(HostRoleStatus.QUEUED) -
              statusMap.get(HostRoleStatus.PENDING));
    }
    return counters;
  }


  /**
   * Returns counts of tasks that are in various states.
   *
   * @param hostRoleCommands  collection of beans {@link HostRoleCommand}
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<HostRoleStatus, Integer> calculateStatusCountsForTasks(Collection<HostRoleCommand> hostRoleCommands, StageEntityPK stage) {
    Map<HostRoleStatus, Integer> counters = new HashMap<>();
    List<HostRoleCommand> hostRoleCommandsOfStage = new ArrayList<>();
    // initialize
    for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
      counters.put(hostRoleStatus, 0);
    }
    // calculate counts
    for (HostRoleCommand hrc : hostRoleCommands) {
      if (stage.getStageId() == hrc.getStageId() && stage.getRequestId() == hrc.getRequestId()) {
        // count tasks where isCompletedState() == true as COMPLETED
        // but don't count tasks with COMPLETED status twice
        if (hrc.getStatus().isCompletedState() && hrc.getStatus() != HostRoleStatus.COMPLETED) {
          // Increase total number of completed tasks;
          counters.put(HostRoleStatus.COMPLETED, counters.get(HostRoleStatus.COMPLETED) + 1);
        }

        // Increment counter for particular status
        counters.put(hrc.getStatus(), counters.get(hrc.getStatus()) + 1);

        hostRoleCommandsOfStage.add(hrc);
      }
    }

    // We overwrite the value to have the sum converged
    counters.put(HostRoleStatus.IN_PROGRESS,
        hostRoleCommandsOfStage.size() -
            counters.get(HostRoleStatus.COMPLETED) -
            counters.get(HostRoleStatus.QUEUED) -
            counters.get(HostRoleStatus.PENDING));

    return counters;
  }

  /**
   * Returns counts of task entities that are in various states.
   *
   * @param tasks  the collection of task entities
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<HostRoleStatus, Integer> calculateTaskEntityStatusCounts(Collection<HostRoleCommandEntity> tasks) {
    Collection<HostRoleStatus> hostRoleStatuses = new LinkedList<>();

    for (HostRoleCommandEntity hostRoleCommand : tasks) {
      hostRoleStatuses.add(hostRoleCommand.getStatus());
    }
    return calculateStatusCounts(hostRoleStatuses);
  }

  /**
   * Return counts of task statuses.
   * @param stageDto  the map of stage-to-summary value objects
   * @param stageIds  the stage ids to consider from the value objects
   * @return the map of status to counts
   */
  public static Map<HostRoleStatus, Integer> calculateTaskStatusCounts(
      Map<Long, HostRoleCommandStatusSummaryDTO> stageDto, Set<Long> stageIds) {

    List<HostRoleStatus> status = new ArrayList<>();

    for (Long stageId : stageIds) {
      if (!stageDto.containsKey(stageId)) {
        continue;
      }

      HostRoleCommandStatusSummaryDTO dto = stageDto.get(stageId);

      status.addAll(dto.getTaskStatuses());
    }

    return calculateStatusCounts(status);
  }

  /**
   * Calculates the status for specified request by id.
   * @param s_hostRoleCommandDAO is used to retrieve the map of stage-to-summary value objects
   * @param topologyManager topology manager
   * @param requestId the request id
   * @return the calculated status
   */
  public static CalculatedStatus statusFromRequest(HostRoleCommandDAO s_hostRoleCommandDAO,
                                                   TopologyManager topologyManager, Long requestId) {
    Map<Long, HostRoleCommandStatusSummaryDTO> summary = s_hostRoleCommandDAO.findAggregateCounts(requestId);

    // get summaries from TopologyManager for logical requests
    summary.putAll(topologyManager.getStageSummaries(requestId));

    // summary might be empty due to delete host have cleared all
    // HostRoleCommands or due to hosts haven't registered yet with the cluster
    // when the cluster is provisioned with a Blueprint
    final CalculatedStatus status;
    LogicalRequest logicalRequest = topologyManager.getRequest(requestId);
    if (summary.isEmpty() && null != logicalRequest) {
      // In this case, it appears that there are no tasks but this is a logical
      // topology request, so it's a matter of hosts simply not registering yet
      // for tasks to be created ==> status = PENDING.
      // For a new LogicalRequest there should be at least one HostRequest,
      // while if they were removed already ==> status = COMPLETED.
      if (logicalRequest.getHostRequests().isEmpty()) {
        status = CalculatedStatus.COMPLETED;
      } else {
        status = CalculatedStatus.PENDING;
      }
    } else {
      // there are either tasks or this is not a logical request, so do normal
      // status calculations
      status = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());
    }
    return status;
  }

  /**
   * Calculates the overall status of an upgrade. If there are no tasks, then a
   * status of {@link HostRoleStatus#COMPLETED} is returned.
   *
   * @param stageDto
   *          the map of stage-to-summary value objects
   * @param stageIds
   *          the stage ids to consider from the value objects
   * @return the calculated status
   */
  public static CalculatedStatus statusFromStageSummary(Map<Long, HostRoleCommandStatusSummaryDTO> stageDto,
      Set<Long> stageIds) {

    // if either are empty, then we have no tasks and therefore no status - we
    // should return COMPLETED. This can happen if someone removes all tasks but
    // leaves the stages and request
    if (stageDto.isEmpty() || stageIds.isEmpty()) {
      return COMPLETED;
    }

    Collection<HostRoleStatus> stageStatuses = new HashSet<>();
    Collection<HostRoleStatus> stageDisplayStatuses = new HashSet<>();
    Collection<HostRoleStatus> taskStatuses = new ArrayList<>();

    for (Long stageId : stageIds) {
      if (!stageDto.containsKey(stageId)) {
        continue;
      }

      HostRoleCommandStatusSummaryDTO summary = stageDto.get(stageId);

      int total = summary.getTaskTotal();
      boolean skip = summary.isStageSkippable();
      Map<HostRoleStatus, Integer> counts = calculateStatusCounts(summary.getTaskStatuses());
      HostRoleStatus stageStatus = calculateSummaryStatus(counts, total, skip);
      HostRoleStatus stageDisplayStatus = calculateSummaryDisplayStatus(counts, total, skip);

      stageStatuses.add(stageStatus);
      stageDisplayStatuses.add(stageDisplayStatus);
      taskStatuses.addAll(summary.getTaskStatuses());
    }

    // calculate the overall status from the stage statuses
    Map<HostRoleStatus, Integer> counts = calculateStatusCounts(stageStatuses);
    Map<HostRoleStatus, Integer> displayCounts = calculateStatusCounts(stageDisplayStatuses);

    HostRoleStatus status = calculateSummaryStatusOfUpgrade(counts, stageStatuses.size());
    HostRoleStatus displayStatus = calculateSummaryDisplayStatus(displayCounts, stageDisplayStatuses.size(), false);

    double progressPercent = calculateProgressPercent(calculateStatusCounts(taskStatuses), taskStatuses.size());

    return new CalculatedStatus(status, displayStatus, progressPercent);
  }

  /**
   * Returns counts of tasks that are in various states.
   *
   * @param tasks  the collection of tasks
   *
   * @return a map of counts of tasks keyed by the task status
   */
  private static Map<HostRoleStatus, Integer> calculateTaskStatusCounts(Collection<HostRoleCommand> tasks) {
    Collection<HostRoleStatus> hostRoleStatuses = new LinkedList<>();

    for (HostRoleCommand hostRoleCommand : tasks) {
      hostRoleStatuses.add(hostRoleCommand.getStatus());
    }
    return calculateStatusCounts(hostRoleStatuses);
  }

  /**
   * Calculate the percent complete based on the given status counts.
   *
   * @param counters  counts of resources that are in various states
   * @param total     total number of resources in request
   *
   * @return the percent complete for the stage
   */
  private static double calculateProgressPercent(Map<HostRoleStatus, Integer> counters, double total) {
    return total == 0 ? 0 :
        ((counters.get(HostRoleStatus.QUEUED)              * 0.09 +
          counters.get(HostRoleStatus.IN_PROGRESS)         * 0.35 +
          counters.get(HostRoleStatus.HOLDING)             * 0.35 +
          counters.get(HostRoleStatus.HOLDING_FAILED)      * 0.35 +
          counters.get(HostRoleStatus.HOLDING_TIMEDOUT)    * 0.35 +
          counters.get(HostRoleStatus.COMPLETED)) / total) * 100.0;
  }

  /**
   * Calculate overall status of a stage or upgrade based on the given status counts.
   *
   * @param counters   counts of resources that are in various states
   * @param total      total number of resources in request. NOTE, completed tasks may be counted twice.
   * @param skippable  true if a single failed status should NOT result in an overall failed status return
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  public static HostRoleStatus calculateSummaryStatus(Map<HostRoleStatus, Integer> counters,
      int total, boolean skippable) {

    // when there are 0 tasks, return COMPLETED
    if (total == 0) {
      return HostRoleStatus.COMPLETED;
    }

    if (counters.get(HostRoleStatus.PENDING) == total) {
      return HostRoleStatus.PENDING;
    }

    // By definition, any tasks in a future stage must be held in a PENDING status.
    if (counters.get(HostRoleStatus.HOLDING) > 0 || counters.get(HostRoleStatus.HOLDING_FAILED) > 0 || counters.get(HostRoleStatus.HOLDING_TIMEDOUT) > 0) {
      return counters.get(HostRoleStatus.HOLDING) > 0 ? HostRoleStatus.HOLDING :
      counters.get(HostRoleStatus.HOLDING_FAILED) > 0 ? HostRoleStatus.HOLDING_FAILED :
      HostRoleStatus.HOLDING_TIMEDOUT;
    }

    // Because tasks are not skippable, guaranteed to be FAILED
    if (counters.get(HostRoleStatus.FAILED) > 0 && !skippable) {
      return HostRoleStatus.FAILED;
    }

    // Because tasks are not skippable, guaranteed to be TIMEDOUT
    if (counters.get(HostRoleStatus.TIMEDOUT) > 0  && !skippable) {
      return HostRoleStatus.TIMEDOUT;
    }

    int numActiveTasks = counters.get(HostRoleStatus.PENDING) + counters.get(HostRoleStatus.QUEUED) + counters.get(HostRoleStatus.IN_PROGRESS);
    // ABORTED only if there are no other active tasks
    if (counters.get(HostRoleStatus.ABORTED) > 0 && numActiveTasks == 0) {
      return HostRoleStatus.ABORTED;
    }

    if (counters.get(HostRoleStatus.COMPLETED) == total) {
      return HostRoleStatus.COMPLETED;
    }

    return HostRoleStatus.IN_PROGRESS;
  }

  /**
   *
   * @param counters counts of resources that are in various states
   * @param skippable {Boolean} <code>TRUE<code/> if failure of any of the task should not fail the stage
   * @return {@link HostRoleStatus}
   */
  public static HostRoleStatus calculateSummaryStatusFromPartialSet(Map<HostRoleStatus, Integer> counters,
                                                      boolean skippable) {

    HostRoleStatus status = HostRoleStatus.PENDING;
    // By definition, any tasks in a future stage must be held in a PENDING status.
    if (counters.get(HostRoleStatus.HOLDING) > 0 || counters.get(HostRoleStatus.HOLDING_FAILED) > 0 || counters.get(HostRoleStatus.HOLDING_TIMEDOUT) > 0) {
      status =  counters.get(HostRoleStatus.HOLDING) > 0 ? HostRoleStatus.HOLDING :
          counters.get(HostRoleStatus.HOLDING_FAILED) > 0 ? HostRoleStatus.HOLDING_FAILED :
              HostRoleStatus.HOLDING_TIMEDOUT;
    }

    // Because tasks are not skippable, guaranteed to be FAILED
    if (counters.get(HostRoleStatus.FAILED) > 0 && !skippable) {
      status = HostRoleStatus.FAILED;
    }

    // Because tasks are not skippable, guaranteed to be TIMEDOUT
    if (counters.get(HostRoleStatus.TIMEDOUT) > 0  && !skippable) {
      status = HostRoleStatus.TIMEDOUT;
    }

    int inProgressTasks =  counters.get(HostRoleStatus.QUEUED) + counters.get(HostRoleStatus.IN_PROGRESS);
    if (inProgressTasks > 0) {
      status = HostRoleStatus.IN_PROGRESS;
    }

    return status;
  }


  /**
   *
   * @param hostRoleCommands list of {@link HostRoleCommand} for a stage
   * @param counters counts of resources that are in various states
   * @param successFactors Map of roles to their successfactor for a stage
   * @param skippable {Boolean} <code>TRUE<code/> if failure of any of the task should not fail the stage
   * @return {@link HostRoleStatus} based on success factor
   */
  public static HostRoleStatus calculateStageStatus(List <HostRoleCommand> hostRoleCommands, Map<HostRoleStatus, Integer> counters, Map<Role, Float> successFactors,
                                                    boolean skippable) {

    // when there are 0 tasks, return COMPLETED
    int total = hostRoleCommands.size();
    if (total == 0) {
      return HostRoleStatus.COMPLETED;
    }

    if (counters.get(HostRoleStatus.PENDING) == total) {
      return HostRoleStatus.PENDING;
    }

    // By definition, any tasks in a future stage must be held in a PENDING status.
    if (counters.get(HostRoleStatus.HOLDING) > 0 || counters.get(HostRoleStatus.HOLDING_FAILED) > 0 || counters.get(HostRoleStatus.HOLDING_TIMEDOUT) > 0) {
      return counters.get(HostRoleStatus.HOLDING) > 0 ? HostRoleStatus.HOLDING :
          counters.get(HostRoleStatus.HOLDING_FAILED) > 0 ? HostRoleStatus.HOLDING_FAILED :
              HostRoleStatus.HOLDING_TIMEDOUT;
    }


    if (counters.get(HostRoleStatus.FAILED) > 0 && !skippable) {
      Set<Role> rolesWithFailedTasks = getRolesOfFailedTasks(hostRoleCommands);
      Boolean didStageFailed = didStageFailed(hostRoleCommands, rolesWithFailedTasks, successFactors);
      if (didStageFailed) return HostRoleStatus.FAILED;
    }


    if (counters.get(HostRoleStatus.TIMEDOUT) > 0  && !skippable) {
      Set<Role> rolesWithTimedOutTasks = getRolesOfTimedOutTasks(hostRoleCommands);
      Boolean didStageFailed = didStageFailed(hostRoleCommands, rolesWithTimedOutTasks, successFactors);
      if (didStageFailed) return HostRoleStatus.TIMEDOUT;
    }

    int numActiveTasks = counters.get(HostRoleStatus.PENDING) + counters.get(HostRoleStatus.QUEUED) + counters.get(HostRoleStatus.IN_PROGRESS);

    if (numActiveTasks > 0) {
      return HostRoleStatus.IN_PROGRESS;
    } else if (counters.get(HostRoleStatus.ABORTED) > 0) {
      Set<Role> rolesWithTimedOutTasks = getRolesOfAbortedTasks(hostRoleCommands);
      Boolean didStageFailed = didStageFailed(hostRoleCommands, rolesWithTimedOutTasks, successFactors);
      if (didStageFailed) return HostRoleStatus.ABORTED;
    }

    return HostRoleStatus.COMPLETED;
  }

  /**
   *  Get all {@link Role} any of whose tasks is in {@link HostRoleStatus#FAILED}
   * @param hostRoleCommands list of {@link HostRoleCommand}
   * @return Set of {@link Role}
   */
  protected static Set<Role> getRolesOfFailedTasks(List <HostRoleCommand> hostRoleCommands) {
    return getRolesOfTasks(hostRoleCommands, HostRoleStatus.FAILED);
  }

  /**
   *  Get all {@link Role} any of whose tasks is in {@link HostRoleStatus#TIMEDOUT}
   * @param hostRoleCommands list of {@link HostRoleCommand}
   * @return Set of {@link Role}
   */
  protected static Set<Role> getRolesOfTimedOutTasks(List <HostRoleCommand> hostRoleCommands) {
    return getRolesOfTasks(hostRoleCommands, HostRoleStatus.TIMEDOUT);
  }

  /**
   *  Get all {@link Role} any of whose tasks is in {@link HostRoleStatus#ABORTED}
   * @param hostRoleCommands list of {@link HostRoleCommand}
   * @return Set of {@link Role}
   */
  protected static Set<Role> getRolesOfAbortedTasks(List <HostRoleCommand> hostRoleCommands) {
    return getRolesOfTasks(hostRoleCommands, HostRoleStatus.ABORTED);
  }

  /**
   * Get all {@link Role} any of whose tasks are in given {@code status}
   * @param hostRoleCommands list of {@link HostRoleCommand}
   * @param status {@link HostRoleStatus}
   * @return Set of {@link Role}
   */
  protected static Set<Role> getRolesOfTasks(List <HostRoleCommand> hostRoleCommands, final HostRoleStatus status) {

    Predicate<HostRoleCommand> predicate = new Predicate<HostRoleCommand>() {
      @Override
      public boolean apply(HostRoleCommand hrc) {
        return hrc.getStatus() ==  status;
      }
    };

    Function<HostRoleCommand, Role> transform = new Function<HostRoleCommand, Role>() {
      @Override
      public Role apply(HostRoleCommand hrc) {
        return hrc.getRole();
      }
    };
    return FluentIterable.from(hostRoleCommands)
        .filter(predicate)
        .transform(transform)
        .toSet();
  }

  /**
   *
   * @param hostRoleCommands list of {@link HostRoleCommand} for a stage
   * @param roles  set of roles to be checked for meeting success criteria
   * @param successFactors  map of role to it's success factor
   * @return {Boolean} <code>TRUE</code> if stage failed due to hostRoleCommands of any role not meeting success criteria
   */
  protected static Boolean didStageFailed(List<HostRoleCommand> hostRoleCommands, Set<Role> roles, Map<Role, Float> successFactors) {
    Boolean isFailed = Boolean.FALSE;
    for (Role role: roles) {
      List <HostRoleCommand> hostRoleCommandsOfRole = getHostRoleCommandsOfRole(hostRoleCommands, role);
      List <HostRoleCommand> failedHostRoleCommands =  getFailedHostRoleCommands(hostRoleCommandsOfRole);
      float successRatioForRole = (hostRoleCommandsOfRole.size() - failedHostRoleCommands.size())/hostRoleCommandsOfRole.size();
      Float successFactorForRole =  successFactors.get(role) == null ? 1.0f : successFactors.get(role);
      if (successRatioForRole  < successFactorForRole) {
        isFailed = Boolean.TRUE;
        break;
      }
    }
    return isFailed;
  }

  /**
   *
   * @param hostRoleCommands list of {@link HostRoleCommand}
   * @param role {@link Role}
   * @return list of {@link HostRoleCommand} that belongs to {@link Role}
   */
  protected static List<HostRoleCommand> getHostRoleCommandsOfRole(List <HostRoleCommand> hostRoleCommands, final Role role) {
    Predicate<HostRoleCommand> predicate = new Predicate<HostRoleCommand>() {
      @Override
      public boolean apply(HostRoleCommand hrc) {
        return hrc.getRole() ==  role;
      }
    };
    return FluentIterable.from(hostRoleCommands)
        .filter(predicate)
        .toList();
  }

  /**
   *
   * @param hostRoleCommands list of {@link HostRoleCommand}
   * @return list of {@link HostRoleCommand} with failed status
   */
  protected static List<HostRoleCommand> getFailedHostRoleCommands(List <HostRoleCommand> hostRoleCommands) {
    Predicate<HostRoleCommand> predicate = new Predicate<HostRoleCommand>() {
      @Override
      public boolean apply(HostRoleCommand hrc) {
        return hrc.getStatus().isFailedAndNotSkippableState();
      }
    };
    return FluentIterable.from(hostRoleCommands)
        .filter(predicate)
        .toList();
  }


  /**
   * Calculate overall status from collection of statuses
   * @param hostRoleStatuses list of all stage's {@link HostRoleStatus}
   * @return overall status of a request
   */
  public static HostRoleStatus getOverallStatusForRequest (Collection<HostRoleStatus> hostRoleStatuses) {
    Map<HostRoleStatus, Integer> statusCount = calculateStatusCounts(hostRoleStatuses);
    return calculateSummaryStatus(statusCount, hostRoleStatuses.size(), false);
  }

  /**
   * Calculate overall display status from collection of statuses
   * @param hostRoleStatuses list of all stage's {@link HostRoleStatus}
   * @return overall display status of a request
   */
  public static HostRoleStatus getOverallDisplayStatusForRequest (Collection<HostRoleStatus> hostRoleStatuses) {
    Map<HostRoleStatus, Integer> statusCount = calculateStatusCounts(hostRoleStatuses);
    return calculateSummaryDisplayStatus(statusCount, hostRoleStatuses.size(), false);
  }


  /**
   * Calculate overall status of an upgrade.
   *
   * @param counters   counts of resources that are in various states
   * @param total      total number of resources in request
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  protected static HostRoleStatus calculateSummaryStatusOfUpgrade(
      Map<HostRoleStatus, Integer> counters, int total) {
    return calculateSummaryStatus(counters, total, false);
  }

  /**
   * Calculate an overall display status based on the given status counts.
   *
   * @param counters   counts of resources that are in various states
   * @param total      total number of resources in request
   * @param skippable  true if a single failed status should NOT result in an overall failed status return
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  public static HostRoleStatus calculateSummaryDisplayStatus(
      Map<HostRoleStatus, Integer> counters, int total, boolean skippable) {
    return counters.get(HostRoleStatus.FAILED) > 0 ? HostRoleStatus.FAILED:
           counters.get(HostRoleStatus.TIMEDOUT) > 0 ? HostRoleStatus.TIMEDOUT:
           counters.get(HostRoleStatus.SKIPPED_FAILED) > 0 ? HostRoleStatus.SKIPPED_FAILED :
           calculateSummaryStatus(counters, total, skippable);
  }

  /**
   * kind of {@link HostRoleStatus} persisted by {@link Stage} and {@link Request}
   */
  public enum StatusType {
    STATUS("status"),
    DISPLAY_STATUS("display_status");
    private String value;

    StatusType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
