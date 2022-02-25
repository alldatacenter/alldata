/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events.listeners.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.api.stomp.NamedTasksSubscriptions;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.events.NamedTaskUpdateEvent;
import org.apache.ambari.server.events.RequestUpdateEvent;
import org.apache.ambari.server.events.TaskCreateEvent;
import org.apache.ambari.server.events.TaskUpdateEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.events.publishers.TaskEventPublisher;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * The {@link TaskStatusListener} is used to constantly update status of running Stages and Requests
 * {@link TaskUpdateEvent} listens for all incoming events. These events are fired when either host role commands are created/updated
 * This listener maintains map of all running tasks, stages and requests
 */
@Singleton
@EagerSingleton
public class TaskStatusListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(TaskStatusListener.class);

  /**
   * Maps task id to its {@link HostRoleCommand} Object.
   * Map has entries of all tasks of all active(ongoing) requests
   * NOTE: Partial loading of tasks for any request may lead to incorrect update of the request status
   */
  private Map<Long,HostRoleCommand> activeTasksMap = new ConcurrentHashMap<>();

  /**
   * Maps all ongoing request id to its {@link ActiveRequest}
   */
  private Map<Long, ActiveRequest> activeRequestMap = new ConcurrentHashMap<>();

  /**
   * Maps {@link StageEntityPK} of all ongoing requests to its {@link ActiveStage}
   * with updated {@link ActiveStage#status} and {@link ActiveStage#displayStatus}.
   */
  private Map<StageEntityPK, ActiveStage> activeStageMap = new ConcurrentHashMap<>();

  private StageDAO stageDAO;

  private RequestDAO requestDAO;

  private STOMPUpdatePublisher STOMPUpdatePublisher;

  private NamedTasksSubscriptions namedTasksSubscriptions;

  @Inject
  public TaskStatusListener(TaskEventPublisher taskEventPublisher, StageDAO stageDAO, RequestDAO requestDAO,
                            STOMPUpdatePublisher STOMPUpdatePublisher, NamedTasksSubscriptions namedTasksSubscriptions) {
    this.stageDAO = stageDAO;
    this.requestDAO = requestDAO;
    this.STOMPUpdatePublisher = STOMPUpdatePublisher;
    this.namedTasksSubscriptions = namedTasksSubscriptions;
    taskEventPublisher.register(this);
  }

  public Map<Long,HostRoleCommand> getActiveTasksMap() {
    return activeTasksMap;
  }

  public Map<Long, ActiveRequest> getActiveRequestMap() {
    return activeRequestMap;
  }

  public Map<StageEntityPK, ActiveStage> getActiveStageMap() {
    return activeStageMap;
  }

  /**
   * On receiving task update event, update related entries of the running request, stage and task in the maps
   * Event containing newly created tasks is expected to contain complete set of all tasks for a request
   * @param event Consumes {@link TaskUpdateEvent}.
   */
  @Subscribe
  public void onTaskUpdateEvent(TaskUpdateEvent event) {
    LOG.debug("Received task update event {}", event);
    List<HostRoleCommand> hostRoleCommandListAll = event.getHostRoleCommands();
    List<HostRoleCommand>  hostRoleCommandWithReceivedStatus =  new ArrayList<>();
    Set<StageEntityPK> stagesWithReceivedTaskStatus = new HashSet<>();
    Set<Long> requestIdsWithReceivedTaskStatus =  new HashSet<>();
    Set<RequestUpdateEvent> requestsToPublish = new HashSet<>();
    Set<NamedTaskUpdateEvent> namedTasksToPublish = new HashSet<>();

    for (HostRoleCommand hostRoleCommand : hostRoleCommandListAll) {
      Long reportedTaskId = hostRoleCommand.getTaskId();
      HostRoleCommand activeTask =  activeTasksMap.get(reportedTaskId);
      if (activeTask == null) {
        LOG.error(String.format("Received update for a task %d which is not being tracked as running task", reportedTaskId));
      } else  {
        hostRoleCommandWithReceivedStatus.add(hostRoleCommand);
        StageEntityPK stageEntityPK = new StageEntityPK();
        stageEntityPK.setRequestId(hostRoleCommand.getRequestId());
        stageEntityPK.setStageId(hostRoleCommand.getStageId());
        stagesWithReceivedTaskStatus.add(stageEntityPK);
        requestIdsWithReceivedTaskStatus.add(hostRoleCommand.getRequestId());

        NamedTaskUpdateEvent namedTaskUpdateEvent = new NamedTaskUpdateEvent(hostRoleCommand);
        if (namedTasksSubscriptions.checkTaskId(reportedTaskId)
            && !namedTaskUpdateEvent.equals(new NamedTaskUpdateEvent(activeTasksMap.get(reportedTaskId)))) {
          namedTasksToPublish.add(namedTaskUpdateEvent);
        }

        // unsubscribe on complete (no any update will be sent anyway)
        if (hostRoleCommand.getStatus().equals(HostRoleStatus.COMPLETED)) {
          namedTasksSubscriptions.removeTaskId(reportedTaskId);
        }

        if (!activeTasksMap.get(reportedTaskId).getStatus().equals(hostRoleCommand.getStatus())) {
          // Ignore requests not related to any cluster. "requests" topic is used for cluster requests only.
          Long clusterId = activeRequestMap.get(hostRoleCommand.getRequestId()).getClusterId();
          if (clusterId != null && clusterId != -1) {
            Set<RequestUpdateEvent.HostRoleCommand> hostRoleCommands = new HashSet<>();
            hostRoleCommands.add(new RequestUpdateEvent.HostRoleCommand(hostRoleCommand.getTaskId(),
                hostRoleCommand.getRequestId(),
                hostRoleCommand.getStatus(),
                hostRoleCommand.getHostName()));
            requestsToPublish.add(new RequestUpdateEvent(hostRoleCommand.getRequestId(),
                activeRequestMap.get(hostRoleCommand.getRequestId()).getStatus(), hostRoleCommands));
          } else {
            LOG.debug("No STOMP request update event was fired for host component status change due no cluster related, " +
                    "request id: {}, role: {}, role command: {}, host: {}, task id: {}, old state: {}, new state: {}",
                hostRoleCommand.getRequestId(),
                hostRoleCommand.getRole(),
                hostRoleCommand.getRoleCommand(),
                hostRoleCommand.getHostName(),
                hostRoleCommand.getTaskId(),
                activeTasksMap.get(reportedTaskId).getStatus(),
                hostRoleCommand.getStatus());
          }
        }
      }
    }
    updateActiveTasksMap(hostRoleCommandWithReceivedStatus);
    Boolean didAnyStageStatusUpdated = updateActiveStagesStatus(stagesWithReceivedTaskStatus, hostRoleCommandListAll);
    // Presumption: If there is no update in any of the running stage's status
    // then none of the running request status needs to be updated
    if (didAnyStageStatusUpdated) {
      updateActiveRequestsStatus(requestIdsWithReceivedTaskStatus, stagesWithReceivedTaskStatus);
    }
    for (RequestUpdateEvent requestToPublish : requestsToPublish) {
      STOMPUpdatePublisher.publish(requestToPublish);
    }
    for (NamedTaskUpdateEvent namedTaskUpdateEvent : namedTasksToPublish) {
      LOG.info(String.format("NamedTaskUpdateEvent with id %s will be send", namedTaskUpdateEvent.getId()));
      STOMPUpdatePublisher.publish(namedTaskUpdateEvent);
    }
  }

  /**
   * On receiving task create event, create entries in the running request, stage and task in the maps
   * @param event Consumes {@link TaskCreateEvent}.
   */
  @Subscribe
  public void onTaskCreateEvent(TaskCreateEvent event) {
    LOG.debug("Received task create event {}", event);
    List<HostRoleCommand> hostRoleCommandListAll = event.getHostRoleCommands();

    for (HostRoleCommand hostRoleCommand : hostRoleCommandListAll) {
      activeTasksMap.put(hostRoleCommand.getTaskId(), hostRoleCommand);
      addStagePK(hostRoleCommand);
      addRequestId(hostRoleCommand);
    }
  }


  /**
   * update changed host role command status
   * @param hostRoleCommandWithReceivedStatus list of host role commands reported
   */
  private void updateActiveTasksMap(List<HostRoleCommand> hostRoleCommandWithReceivedStatus) {
    for (HostRoleCommand hostRoleCommand : hostRoleCommandWithReceivedStatus) {
      Long taskId = hostRoleCommand.getTaskId();
      activeTasksMap.put(taskId , hostRoleCommand);
    }
  }


  /**
   * Adds new {@link StageEntityPK} to be tracked as running stage in {@link #activeStageMap}
   * @param hostRoleCommand newly created {@link HostRoleCommand} in {@link #activeTasksMap}
   */
  private void addStagePK(HostRoleCommand hostRoleCommand) {
    StageEntityPK stageEntityPK = new StageEntityPK();
    stageEntityPK.setRequestId(hostRoleCommand.getRequestId());
    stageEntityPK.setStageId(hostRoleCommand.getStageId());
    if (activeStageMap.containsKey(stageEntityPK)) {
      activeStageMap.get(stageEntityPK).addTaskId(hostRoleCommand.getTaskId());
    } else {
      StageEntity stageEntity = stageDAO.findByPK(stageEntityPK);
      // Stage entity of the hostrolecommand should be persisted before publishing task create event
      assert stageEntity != null;
      Map<Role, Float> successFactors = new HashMap<>();
      Collection<RoleSuccessCriteriaEntity> roleSuccessCriteriaEntities = stageEntity.getRoleSuccessCriterias();
      for (RoleSuccessCriteriaEntity successCriteriaEntity : roleSuccessCriteriaEntities) {
        successFactors.put(successCriteriaEntity.getRole(), successCriteriaEntity.getSuccessFactor().floatValue());
      }
      Set<Long> taskIdSet =  Sets.newHashSet(hostRoleCommand.getTaskId());

      ActiveStage reportedStage = new ActiveStage(stageEntity.getStatus(), stageEntity.getDisplayStatus(),
          successFactors, stageEntity.isSkippable(), taskIdSet);
      activeStageMap.put(stageEntityPK, reportedStage);
    }
  }

  /**
   * update and persist all changed stage status
   * @param stagesWithReceivedTaskStatus set of stages that has received task status
   * @param hostRoleCommandListAll list of all task updates received from agent
   * @return  <code>true</code> if any of the stage has changed it's existing status;
   *          <code>false</code> otherwise
   */
  private Boolean updateActiveStagesStatus(final Set<StageEntityPK> stagesWithReceivedTaskStatus, List<HostRoleCommand> hostRoleCommandListAll) {
    Boolean didAnyStageStatusUpdated = Boolean.FALSE;
    for (StageEntityPK reportedStagePK : stagesWithReceivedTaskStatus) {
      if (activeStageMap.containsKey(reportedStagePK)) {
        Boolean didStatusChange = updateStageStatus(reportedStagePK, hostRoleCommandListAll);
        if (didStatusChange) {
          ActiveStage reportedStage = activeStageMap.get(reportedStagePK);
          stageDAO.updateStatus(reportedStagePK, reportedStage.getStatus(), reportedStage.getDisplayStatus());
          didAnyStageStatusUpdated = Boolean.TRUE;
        }
      } else {
        LOG.error(String.format("Received update for a task whose stage is not being tracked as running stage: %s", reportedStagePK.toString()));
      }

    }
    return didAnyStageStatusUpdated;
  }

  /**
   * Adds new request id to be tracked as running request in {@link #activeRequestMap}
   * @param hostRoleCommand newly created {@link HostRoleCommand} in {@link #activeTasksMap}
   */
  private void addRequestId(HostRoleCommand hostRoleCommand) {
    Long requestId = hostRoleCommand.getRequestId();
    StageEntityPK stageEntityPK = new StageEntityPK();
    stageEntityPK.setRequestId(hostRoleCommand.getRequestId());
    stageEntityPK.setStageId(hostRoleCommand.getStageId());
    if (activeRequestMap.containsKey(requestId)) {
      activeRequestMap.get(requestId).addStageEntityPK(stageEntityPK);
    } else {
      RequestEntity requestEntity = requestDAO.findByPK(requestId);
      // Request entity of the hostrolecommand should be persisted before publishing task create event
      assert requestEntity != null;
      Set<StageEntityPK> stageEntityPKs =  Sets.newHashSet(stageEntityPK);
      ActiveRequest request = new ActiveRequest(requestEntity.getStatus(),requestEntity.getDisplayStatus(),
          stageEntityPKs, requestEntity.getClusterId());
      activeRequestMap.put(requestId, request);
    }
  }


  /**
   * update and persist changed request status
   * @param requestIdsWithReceivedTaskStatus set of request ids that has received tasks status
   * @param stagesWithChangedTaskStatus set of stages that have received tasks with changed status
   */
  private void updateActiveRequestsStatus(final Set<Long> requestIdsWithReceivedTaskStatus, Set<StageEntityPK> stagesWithChangedTaskStatus) {
    for (Long reportedRequestId : requestIdsWithReceivedTaskStatus) {
      if (activeRequestMap.containsKey(reportedRequestId)) {
        ActiveRequest request =  activeRequestMap.get(reportedRequestId);
        Boolean didStatusChange = updateRequestStatus(reportedRequestId, stagesWithChangedTaskStatus);
        if (didStatusChange) {
          requestDAO.updateStatus(reportedRequestId, request.getStatus(), request.getDisplayStatus());
        }
        if (request.isCompleted() && isAllTasksCompleted(reportedRequestId)) {
          // Request is considered ton have been finished if request status and all of it's tasks status are completed
          // in that case, request and it's stages
          // and tasks should no longer be tracked as active(running)
          removeRequestStageAndTasks(reportedRequestId);
        }
      } else {
        LOG.error(String.format("Received update for a task whose request %d is not being tracked as running request", reportedRequestId));
      }

    }
  }

  /**
   *
   * @param requestId request Id
   * @return  <code>false</code> if any of the task belonging to requestId has incomplete status
   *          <code>true</code> otherwise
   */
  private Boolean isAllTasksCompleted(Long requestId) {
    Boolean result = Boolean.TRUE;
    for (Map.Entry<Long, HostRoleCommand> entry : activeTasksMap.entrySet()) {
      if (entry.getValue().getRequestId() == requestId && !entry.getValue().getStatus().isCompletedState()) {
        result = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Removes entries from {@link #activeTasksMap},{@link #activeStageMap} and {@link #activeRequestMap}
   * @param requestId request id whose entry and it's stage and task entries is to be removed
   */
  private void removeRequestStageAndTasks(Long requestId) {
    removeTasks(requestId);
    removeStages(requestId);
    removeRequest(requestId);
  }


  /**
   * Filters list of {@link Stage} to list of {@link StageEntityPK}
   * @param requestID requestId
   * @return  list of StageEntityPK
   */
  private List<StageEntityPK> getAllStageEntityPKForRequest(final Long requestID) {
    Predicate<StageEntityPK> predicate = new Predicate<StageEntityPK>() {
      @Override
      public boolean apply(StageEntityPK stageEntityPK) {
        return stageEntityPK.getRequestId().equals(requestID);
      }
    };
    return  FluentIterable.from(activeStageMap.keySet())
        .filter(predicate)
        .toList();
  }



  /**
   * Returns the computed status of the stage from the status of it's host role commands
   * @param stagePK {@link StageEntityPK} primary key for the stage entity
   * @param hostRoleCommandListAll list of all hrc received whose status has been received from agent
   * @return {@link Boolean} <code>TRUE</code> if status of the given stage changed.
   */
  private Boolean updateStageStatus(final StageEntityPK stagePK, List<HostRoleCommand> hostRoleCommandListAll) {
    Boolean didAnyStatusChanged = Boolean.FALSE;
    ActiveStage reportedStage = activeStageMap.get(stagePK);
    HostRoleStatus stageCurrentStatus = reportedStage.getStatus();
    HostRoleStatus stageCurrentDisplayStatus = reportedStage.getDisplayStatus();


    // if stage is already marked to be completed then do not calculate reported status from host role commands
    // Presumption: There will be no status transition of the host role command from one completed state to another
    if (!stageCurrentDisplayStatus.isCompletedState() || !stageCurrentStatus.isCompletedState()) {
      Map<HostRoleStatus, Integer> receivedTaskStatusCount = CalculatedStatus.calculateStatusCountsForTasks(hostRoleCommandListAll, stagePK);
      HostRoleStatus statusFromPartialSet = CalculatedStatus.calculateSummaryStatusFromPartialSet(receivedTaskStatusCount, reportedStage.getSkippable());
      HostRoleStatus displayStatusFromPartialSet = CalculatedStatus.calculateSummaryStatusFromPartialSet(receivedTaskStatusCount, Boolean.FALSE);
      if (statusFromPartialSet == HostRoleStatus.PENDING || displayStatusFromPartialSet == HostRoleStatus.PENDING) {
        Function<Long,HostRoleCommand> transform = new Function<Long,HostRoleCommand>(){
          @Override
          public HostRoleCommand apply(Long taskId) {
            return activeTasksMap.get(taskId);
          }
        };

        List<HostRoleCommand> activeHostRoleCommandsOfStage = FluentIterable.from(reportedStage.getTaskIds())
            .transform(transform).toList();
        Map<HostRoleStatus, Integer> statusCount = CalculatedStatus.calculateStatusCountsForTasks(activeHostRoleCommandsOfStage);
        if (displayStatusFromPartialSet == HostRoleStatus.PENDING) {
          // calculate and get new display status of the stage as per the new status of received host role commands
          HostRoleStatus display_status = CalculatedStatus.calculateSummaryDisplayStatus(statusCount, activeHostRoleCommandsOfStage.size(), reportedStage.getSkippable());
          if (display_status != stageCurrentDisplayStatus) {
            reportedStage.setDisplayStatus(display_status);
            didAnyStatusChanged = Boolean.TRUE;
          }

        } else {
          reportedStage.setDisplayStatus(displayStatusFromPartialSet);
          didAnyStatusChanged = Boolean.TRUE;
        }

        if (statusFromPartialSet == HostRoleStatus.PENDING) {
          // calculate status of the stage as per the new status of received host role commands
          HostRoleStatus status = CalculatedStatus.calculateStageStatus(activeHostRoleCommandsOfStage, statusCount, reportedStage.getSuccessFactors(), reportedStage.getSkippable());
          if (status != stageCurrentStatus) {
            reportedStage.setStatus(status);
            didAnyStatusChanged = Boolean.TRUE;
          }
        } else {
          reportedStage.setDisplayStatus(displayStatusFromPartialSet);
          didAnyStatusChanged = Boolean.TRUE;
        }
      } else {
        reportedStage.setStatus(statusFromPartialSet);
        reportedStage.setDisplayStatus(displayStatusFromPartialSet);
        didAnyStatusChanged = Boolean.TRUE;
      }
    }

    return didAnyStatusChanged;
  }

  /**
   *
   * @param requestId {@link Request} whose status is to be updated
   * @param stagesWithChangedTaskStatus set of stages that have received tasks with changed status
   * @return {Boolean} <code>TRUE</code> if request status has changed from existing
   */
  private Boolean updateRequestStatus (final Long requestId, Set<StageEntityPK> stagesWithChangedTaskStatus) {
    Boolean didStatusChanged = Boolean.FALSE;
    ActiveRequest request = activeRequestMap.get(requestId);
    HostRoleStatus requestCurrentStatus = request.getStatus();
    HostRoleStatus requestCurrentDisplayStatus = request.getDisplayStatus();

    if (!requestCurrentDisplayStatus.isCompletedState() || !requestCurrentStatus.isCompletedState()) {
      List <ActiveStage>  activeStagesWithChangesTaskStatus = new ArrayList<>();
      for (StageEntityPK stageEntityPK:stagesWithChangedTaskStatus) {
        if (requestId.equals(stageEntityPK.getRequestId())) {
          ActiveStage activeStage = activeStageMap.get(stageEntityPK);
          activeStagesWithChangesTaskStatus.add(activeStage);
        }
      }


      Map<CalculatedStatus.StatusType,Map<HostRoleStatus, Integer>> stageStatusCountFromPartialSet = CalculatedStatus.calculateStatusCountsForStage(activeStagesWithChangesTaskStatus);
      HostRoleStatus statusFromPartialSet = CalculatedStatus.calculateSummaryStatusFromPartialSet(stageStatusCountFromPartialSet.get(CalculatedStatus.StatusType.STATUS), Boolean.FALSE);
      HostRoleStatus displayStatusFromPartialSet = CalculatedStatus.calculateSummaryStatusFromPartialSet(stageStatusCountFromPartialSet.get(CalculatedStatus.StatusType.DISPLAY_STATUS), Boolean.FALSE);

      if (statusFromPartialSet == HostRoleStatus.PENDING || displayStatusFromPartialSet == HostRoleStatus.PENDING) {
        List <ActiveStage> allActiveStages = new ArrayList<>();
        for (StageEntityPK stageEntityPK:request.getStageEntityPks()) {
          ActiveStage activeStage = activeStageMap.get(stageEntityPK);
          allActiveStages.add(activeStage);
        }
        Map<CalculatedStatus.StatusType,Map<HostRoleStatus, Integer>> stageStatusCount = CalculatedStatus.calculateStatusCountsForStage(allActiveStages);

        if (displayStatusFromPartialSet == HostRoleStatus.PENDING) {
          // calculate and get new display status of the stage as per the new status of received host role commands

          HostRoleStatus display_status = CalculatedStatus.calculateSummaryDisplayStatus(stageStatusCount.get(CalculatedStatus.StatusType.DISPLAY_STATUS), allActiveStages.size(), false);
          if (display_status != requestCurrentDisplayStatus) {
            request.setDisplayStatus(display_status);
            didStatusChanged = Boolean.TRUE;
          }

        } else {
          request.setDisplayStatus(displayStatusFromPartialSet);
          didStatusChanged = Boolean.TRUE;
        }

        if (statusFromPartialSet == HostRoleStatus.PENDING) {
          // calculate status of the stage as per the new status of received host role commands
          HostRoleStatus status = CalculatedStatus.calculateSummaryStatus(stageStatusCount.get(CalculatedStatus.StatusType.STATUS), allActiveStages.size(), false);
          if (status != requestCurrentStatus) {
            request.setStatus(status);
            didStatusChanged = Boolean.TRUE;
          }
        } else {
          request.setDisplayStatus(displayStatusFromPartialSet);
          didStatusChanged = Boolean.TRUE;
        }
      } else {
        request.setStatus(statusFromPartialSet);
        request.setDisplayStatus(displayStatusFromPartialSet);
        didStatusChanged = Boolean.TRUE;
      }
    }

    return didStatusChanged;
  }


  /**
   * Removes list of {@link HostRoleCommand} entries from {@link #activeTasksMap}
   * @param requestId request id
   */
  private void removeTasks(Long requestId) {
    Iterator<Map.Entry<Long, HostRoleCommand>> iter = activeTasksMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<Long, HostRoleCommand> entry = iter.next();
      HostRoleCommand hrc = entry.getValue();
      if (hrc.getRequestId() == requestId) {
        if (!hrc.getStatus().isCompletedState()) {
          LOG.error(String.format("Task %d should have been completed before being removed from running task cache(activeTasksMap)", hrc.getTaskId()));
        }
        iter.remove();
      }
    }
  }


  /**
   * Removes list of {@link StageEntityPK} entries from {@link #activeStageMap}
   * @param requestId request Id
   */
  private void removeStages(Long requestId) {
    List <StageEntityPK> stageEntityPKs = getAllStageEntityPKForRequest(requestId);
    for (StageEntityPK stageEntityPK: stageEntityPKs) {
      activeStageMap.remove(stageEntityPK);
    }
  }


  /**
   * Removes request id from {@link #activeRequestMap}
   * @param requestId request Id
   */
  private void removeRequest(Long requestId) {
    activeRequestMap.remove(requestId);
  }


  /**
   * This class stores {@link Request#status} and {@link Request#displayStatus} information
   * This information is cached for all running {@link Request} at {@link #activeRequestMap}
   */
  protected class ActiveRequest {
    private HostRoleStatus status;
    private HostRoleStatus displayStatus;
    private Set <StageEntityPK> stageEntityPks;
    private Long clusterId;

    public ActiveRequest(HostRoleStatus status, HostRoleStatus displayStatus, Set<StageEntityPK> stageEntityPks,
                         Long clusterId) {
      this.status = status;
      this.displayStatus = displayStatus;
      this.stageEntityPks = stageEntityPks;
      this.clusterId = clusterId;
    }

    public HostRoleStatus getStatus() {
      return status;
    }

    public void setStatus(HostRoleStatus status) {
      this.status = status;
    }

    public HostRoleStatus getDisplayStatus() {
      return displayStatus;
    }

    public void setDisplayStatus(HostRoleStatus displayStatus) {
      this.displayStatus = displayStatus;
    }

    public Boolean isCompleted() {
      return status.isCompletedState() && displayStatus.isCompletedState();
    }

    public Set <StageEntityPK> getStageEntityPks() {
      return stageEntityPks;
    }

    public void addStageEntityPK(StageEntityPK stageEntityPK) {
      stageEntityPks.add(stageEntityPK);
    }

    public Long getClusterId() {
      return clusterId;
    }

    public void setClusterId(Long clusterId) {
      this.clusterId = clusterId;
    }
  }

  /**
   * This class stores information needed to determine {@link Stage#status} and {@link Stage#displayStatus}
   * This information is cached for all {@link Stage} of all running {@link Request} at {@link #activeStageMap}
   */
  public class ActiveStage {
    private HostRoleStatus status;
    private HostRoleStatus displayStatus;
    private Boolean skippable;
    private Set <Long> taskIds;

    //Map of roles to successFactors for this stage. Default is 1 i.e. 100%
    private Map<Role, Float> successFactors = new HashMap<>();

    public ActiveStage(HostRoleStatus status, HostRoleStatus displayStatus,
                       Map<Role, Float> successFactors, Boolean skippable, Set<Long> taskIds) {
      this.status = status;
      this.displayStatus = displayStatus;
      this.successFactors =  successFactors;
      this.skippable = skippable;
      this.taskIds = taskIds;
    }

    public HostRoleStatus getStatus() {
      return status;
    }

    public void setStatus(HostRoleStatus status) {
      this.status = status;
    }

    public HostRoleStatus getDisplayStatus() {
      return displayStatus;
    }

    public void setDisplayStatus(HostRoleStatus displayStatus) {
      this.displayStatus = displayStatus;
    }

    public Boolean getSkippable() {
      return skippable;
    }

    public void setSkippable(Boolean skippable) {
      this.skippable = skippable;
    }

    public Map<Role, Float> getSuccessFactors() {
      return successFactors;
    }

    public void setSuccessFactors(Map<Role, Float> successFactors) {
      this.successFactors = successFactors;
    }

    public Set <Long> getTaskIds() {
      return taskIds;
    }

    public void addTaskId(Long taskId) {
      taskIds.add(taskId);
    }

  }
}
