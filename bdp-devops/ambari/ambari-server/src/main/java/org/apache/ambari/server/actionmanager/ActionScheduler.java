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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.CancelCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.jpa.EntityManagerCacheInvalidationEvent;
import org.apache.ambari.server.events.listeners.tasks.TaskStatusListener;
import org.apache.ambari.server.events.publishers.AgentCommandsPublisher;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.JPAEventPublisher;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandPair;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.serveraction.ServerActionExecutor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.UnitOfWork;


/**
 * This class encapsulates the action scheduler thread.
 * Action schedule frequently looks at action database and determines if
 * there is an action that can be scheduled.
 */
@Singleton
class ActionScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ActionScheduler.class);

  public static final String FAILED_TASK_ABORT_REASONING =
    "Server considered task failed and automatically aborted it";

  @Inject
  private RoleCommandOrderProvider roleCommandOrderProvider;

  @Inject
  private UnitOfWork unitOfWork;

  @Inject
  private Clusters clusters;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private HostsMap hostsMap;

  @Inject
  private Configuration configuration;

  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Used for turning instances of {@link HostRoleCommandEntity} into
   * {@link HostRoleCommand}.
   */
  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  /**
   * Used for retrieving {@link HostRoleCommandEntity} instances which need to
   * be cancelled.
   */
  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private AgentCommandsPublisher agentCommandsPublisher;

  /**
   * The current thread's reference to the {@link EntityManager}.
   */
  volatile EntityManager threadEntityManager;

  private final long actionTimeout;
  private final long sleepTime;
  private volatile boolean shouldRun = true;
  private Thread schedulerThread = null;
  private final ActionDBAccessor db;
  private short maxAttempts = 2;
  private final JPAEventPublisher jpaPublisher;
  private boolean taskTimeoutAdjustment = true;
  private final Object wakeupSyncObject = new Object();
  private final ServerActionExecutor serverActionExecutor;

  private final Set<Long> requestsInProgress = new HashSet<>();

  /**
   * Contains request ids that have been scheduled to be cancelled,
   * but are not cancelled yet
   */
  private final Set<Long> requestsToBeCancelled =
    Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

  /**
   * Maps request IDs to reasoning for cancelling request.
   * Map is NOT synchronized, so any access to it should synchronize on
   * requestsToBeCancelled object
   */
  private final Map<Long, String> requestCancelReasons =
    new HashMap<>();

  /**
   * true if scheduler should run ASAP.
   * We need this flag to avoid sleep in situations, when
   * we receive awake() request during running a scheduler iteration.
   */
  private boolean activeAwakeRequest = false;

  private AtomicBoolean taskStatusLoaded = new AtomicBoolean();

  //Cache for clusterHostinfo, key - stageId-requestId
  private Cache<String, Map<String, Set<String>>> clusterHostInfoCache;
  private Cache<String, Map<String, String>> commandParamsStageCache;
  private Cache<String, Map<String, String>> hostParamsStageCache;

  /**
   * Guice-injected Constructor.
   *
   * @param sleepTime
   * @param actionTimeout
   * @param db
   * @param jpaPublisher
   */
  @Inject
  public ActionScheduler(@Named("schedulerSleeptime") long sleepTime,
                         @Named("actionTimeout") long actionTimeout, ActionDBAccessor db,
                         JPAEventPublisher jpaPublisher) {

    this.sleepTime = sleepTime;
    this.actionTimeout = actionTimeout;
    this.db = db;

    this.jpaPublisher = jpaPublisher;
    this.jpaPublisher.register(this);

    serverActionExecutor = new ServerActionExecutor(db, sleepTime);

    initializeCaches();
  }

  /**
   * Unit Test Constructor.
   * @param sleepTimeMilliSec
   * @param actionTimeoutMilliSec
   * @param db
   * @param fsmObject
   * @param maxAttempts
   * @param hostsMap
   * @param unitOfWork
   * @param ambariEventPublisher
   * @param configuration
   * @param entityManagerProvider
   * @param hostRoleCommandDAO
   * @param hostRoleCommandFactory
   * @param roleCommandOrderProvider
   */
  protected ActionScheduler(long sleepTimeMilliSec, long actionTimeoutMilliSec, ActionDBAccessor db,
                            Clusters fsmObject, int maxAttempts, HostsMap hostsMap,
                            UnitOfWork unitOfWork, AmbariEventPublisher ambariEventPublisher,
                            Configuration configuration, Provider<EntityManager> entityManagerProvider,
                            HostRoleCommandDAO hostRoleCommandDAO, HostRoleCommandFactory hostRoleCommandFactory,
                            RoleCommandOrderProvider roleCommandOrderProvider, AgentCommandsPublisher agentCommandsPublisher) {

    sleepTime = sleepTimeMilliSec;
    actionTimeout = actionTimeoutMilliSec;
    this.db = db;
    clusters = fsmObject;
    this.maxAttempts = (short) maxAttempts;
    this.hostsMap = hostsMap;
    this.unitOfWork = unitOfWork;
    this.ambariEventPublisher = ambariEventPublisher;
    this.configuration = configuration;
    this.entityManagerProvider = entityManagerProvider;
    this.hostRoleCommandDAO = hostRoleCommandDAO;
    this.hostRoleCommandFactory = hostRoleCommandFactory;
    jpaPublisher = null;
    this.roleCommandOrderProvider = roleCommandOrderProvider;
    this.agentCommandsPublisher = agentCommandsPublisher;

    serverActionExecutor = new ServerActionExecutor(db, sleepTime);
    initializeCaches();
  }

  /**
   * Unit Test Constructor.
   *
   * @param sleepTimeMilliSec
   * @param actionTimeoutMilliSec
   * @param db
   * @param fsmObject
   * @param maxAttempts
   * @param hostsMap
   * @param unitOfWork
   * @param ambariEventPublisher
   * @param configuration
   * @param hostRoleCommandDAO
   * @param hostRoleCommandFactory
   */
  protected ActionScheduler(long sleepTimeMilliSec, long actionTimeoutMilliSec, ActionDBAccessor db,
                            Clusters fsmObject, int maxAttempts, HostsMap hostsMap,
                            UnitOfWork unitOfWork, AmbariEventPublisher ambariEventPublisher,
                            Configuration configuration, Provider<EntityManager> entityManagerProvider,
                            HostRoleCommandDAO hostRoleCommandDAO, HostRoleCommandFactory hostRoleCommandFactory,
                            AgentCommandsPublisher agentCommandsPublisher) {

    this(sleepTimeMilliSec, actionTimeoutMilliSec, db, fsmObject, maxAttempts, hostsMap, unitOfWork,
            ambariEventPublisher, configuration, entityManagerProvider, hostRoleCommandDAO, hostRoleCommandFactory,
            null, agentCommandsPublisher);
  }

  /**
   * Initializes the caches.
   */
  private void initializeCaches() {
    clusterHostInfoCache = CacheBuilder.newBuilder().
      expireAfterAccess(5, TimeUnit.MINUTES).
      build();

    commandParamsStageCache = CacheBuilder.newBuilder().
      expireAfterAccess(5, TimeUnit.MINUTES).
      build();

    hostParamsStageCache = CacheBuilder.newBuilder().
      expireAfterAccess(5, TimeUnit.MINUTES).
      build();
  }

  public void start() {
    schedulerThread = new Thread(this, "ambari-action-scheduler");
    schedulerThread.start();

    // Start up the ServerActionExecutor. Since it is directly related to the ActionScheduler it
    // should be started and stopped along with it.
    serverActionExecutor.start();
  }

  public void stop() {
    shouldRun = false;
    schedulerThread.interrupt();

    // Stop the ServerActionExecutor. Since it is directly related to the ActionScheduler it should
    // be started and stopped along with it.
    serverActionExecutor.stop();
  }

  /**
   * Should be called from another thread when we want scheduler to
   * make a run ASAP (for example, to process desired configs of SCHs).
   * The method is guaranteed to return quickly.
   */
  public void awake() {
    synchronized (wakeupSyncObject) {
      activeAwakeRequest = true;
      wakeupSyncObject.notify();
    }
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        synchronized (wakeupSyncObject) {
          if (!activeAwakeRequest) {
            wakeupSyncObject.wait(sleepTime);
          }
          activeAwakeRequest = false;
        }

        doWork();

      } catch (InterruptedException ex) {
        LOG.warn("Scheduler thread is interrupted going to stop", ex);
        shouldRun = false;
      } catch (Exception ex) {
        LOG.warn("Exception received", ex);
        requestsInProgress.clear();
      } catch (Throwable t) {
        LOG.warn("ERROR", t);
        requestsInProgress.clear();
      }
    }
  }

  public void doWork() throws AmbariException {
    try {
      unitOfWork.begin();

      // grab a reference to this UnitOfWork's EM
      threadEntityManager = entityManagerProvider.get();

      // The first thing to do is to abort requests that are cancelled
      processCancelledRequestsList();

      // !!! getting the stages in progress could be a very expensive call due
      // to the join being used; there's no need to make it if there are
      // no commands in progress
      if (db.getCommandsInProgressCount() == 0) {
        // Nothing to do
        if (LOG.isDebugEnabled()) {
          LOG.debug("There are no stages currently in progress.");
        }

        return;
      }

      Set<Long> runningRequestIds = new HashSet<>();
      List<Stage> firstStageInProgressPerRequest = db.getFirstStageInProgressPerRequest();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Scheduler wakes up");
        LOG.debug("Processing {} in progress stages", firstStageInProgressPerRequest.size());
      }

      publishInProgressTasks(firstStageInProgressPerRequest);

      if (firstStageInProgressPerRequest.isEmpty()) {
        // Nothing to do
        if (LOG.isDebugEnabled()) {
          LOG.debug("There are no stages currently in progress.");
        }

        return;
      }

      int i_stage = 0;

      // filter the stages in progress down to those which can be scheduled in
      // parallel
      List<Stage> stages = filterParallelPerHostStages(firstStageInProgressPerRequest);

      boolean exclusiveRequestIsGoing = false;
      // This loop greatly depends on the fact that order of stages in
      // a list does not change between invocations
      for (Stage stage : stages) {
        // Check if we can process this stage in parallel with another stages
        i_stage++;
        long requestId = stage.getRequestId();
        LOG.debug("==> STAGE_i = {}(requestId={},StageId={})", i_stage, requestId, stage.getStageId());

        RequestEntity request = db.getRequestEntity(requestId);

        if (request.isExclusive()) {
          if (runningRequestIds.size() > 0) {
            // As a result, we will wait until any previous stages are finished
            LOG.debug("Stage requires exclusive execution, but other requests are already executing. Stopping for now");
            break;
          }
          exclusiveRequestIsGoing = true;
        }

        if (runningRequestIds.contains(requestId)) {
          // We don't want to process different stages from the same request in parallel
          LOG.debug("==> We don't want to process different stages from the same request in parallel");
          continue;
        } else {
          runningRequestIds.add(requestId);
          if (!requestsInProgress.contains(requestId)) {
            requestsInProgress.add(requestId);
            db.startRequest(requestId);
          }
        }

        // Commands that will be scheduled in current scheduler wakeup
        List<ExecutionCommand> commandsToSchedule = new ArrayList<>();
        Multimap<Long, AgentCommand> commandsToEnqueue = ArrayListMultimap.create();

        Map<String, RoleStats> roleStats =
          processInProgressStage(stage, commandsToSchedule, commandsToEnqueue);

        // Check if stage is failed
        boolean failed = false;
        for (Map.Entry<String, RoleStats> entry : roleStats.entrySet()) {

          String role = entry.getKey();
          RoleStats stats = entry.getValue();

          if (LOG.isDebugEnabled()) {
            LOG.debug("Stats for role: {}, stats={}", role, stats);
          }

          // only fail the request if the role failed and the stage is not
          // skippable
          if (stats.isRoleFailed() && !stage.isSkippable()) {
            LOG.warn("{} failed, request {} will be aborted", role, request.getRequestId());

            failed = true;
            break;
          }
        }

        if (!failed) {
          // Prior stage may have failed and it may need to fail the whole request
          failed = hasPreviousStageFailed(stage);
        }

        if (failed) {
          LOG.error("Operation completely failed, aborting request id: {}", stage.getRequestId());
          cancelHostRoleCommands(stage.getOrderedHostRoleCommands(), FAILED_TASK_ABORT_REASONING);
          abortOperationsForStage(stage);
          return;
        }

        List<ExecutionCommand> commandsToStart = new ArrayList<>();
        List<ExecutionCommand> commandsToUpdate = new ArrayList<>();

        //Schedule what we have so far


        for (ExecutionCommand cmd : commandsToSchedule) {
          processHostRole(request, stage, cmd, commandsToStart, commandsToUpdate);
        }

        LOG.debug("==> Commands to start: {}", commandsToStart.size());
        LOG.debug("==> Commands to update: {}", commandsToUpdate.size());

        //Multimap is analog of Map<Object, List<Object>> but allows to avoid nested loop
        ListMultimap<String, ServiceComponentHostEvent> eventMap = formEventMap(stage, commandsToStart);
        Map<ExecutionCommand, String> commandsToAbort = new HashMap<>();
        if (!eventMap.isEmpty()) {
          LOG.debug("==> processing {} serviceComponentHostEvents...", eventMap.size());
          Cluster cluster = clusters.getCluster(stage.getClusterName());
          if (cluster != null) {
            Map<ServiceComponentHostEvent, String> failedEvents = cluster.processServiceComponentHostEvents(eventMap);

            if (failedEvents.size() > 0) {
              LOG.error("==> {} events failed.", failedEvents.size());
            }

            for (Iterator<ExecutionCommand> iterator = commandsToUpdate.iterator(); iterator.hasNext(); ) {
              ExecutionCommand cmd = iterator.next();
              for (ServiceComponentHostEvent event : failedEvents.keySet()) {
                if (StringUtils.equals(event.getHostName(), cmd.getHostname()) &&
                  StringUtils.equals(event.getServiceComponentName(), cmd.getRole())) {
                  iterator.remove();
                  commandsToAbort.put(cmd, failedEvents.get(event));
                  break;
                }
              }
            }
          } else {
            LOG.warn("There was events to process but cluster {} not found", stage.getClusterName());
          }
        }

        LOG.debug("==> Scheduling {} tasks...", commandsToUpdate.size());
        db.bulkHostRoleScheduled(stage, commandsToUpdate);

        if (commandsToAbort.size() > 0) { // Code branch may be a bit slow, but is extremely rarely used
          LOG.debug("==> Aborting {} tasks...", commandsToAbort.size());
          // Build a list of HostRoleCommands
          List<Long> taskIds = new ArrayList<>();
          for (ExecutionCommand command : commandsToAbort.keySet()) {
            taskIds.add(command.getTaskId());
          }
          Collection<HostRoleCommand> hostRoleCommands = db.getTasks(taskIds);

          cancelHostRoleCommands(hostRoleCommands, FAILED_TASK_ABORT_REASONING);
          db.bulkAbortHostRole(stage, commandsToAbort);
        }

        LOG.debug("==> Adding {} tasks to queue...", commandsToUpdate.size());
        for (ExecutionCommand cmd : commandsToUpdate) {
          // Do not queue up server actions; however if we encounter one, wake up the ServerActionExecutor
          if (Role.AMBARI_SERVER_ACTION.name().equals(cmd.getRole())) {
            serverActionExecutor.awake();
          } else {
            commandsToEnqueue.put(clusters.getHost(cmd.getHostname()).getHostId(), cmd);
          }
        }
        if (!commandsToEnqueue.isEmpty()) {
          agentCommandsPublisher.sendAgentCommand(commandsToEnqueue);
        }
        LOG.debug("==> Finished.");

        if (!configuration.getParallelStageExecution()) { // If disabled
          return;
        }

        if (exclusiveRequestIsGoing) {
          // As a result, we will prevent any further stages from being executed
          LOG.debug("Stage requires exclusive execution, skipping all executing any further stages");
          break;
        }
      }

      requestsInProgress.retainAll(runningRequestIds);

    } finally {
      LOG.debug("Scheduler finished work.");
      unitOfWork.end();
    }
  }

  /**
   * publish event to load {@link TaskStatusListener#activeTasksMap} {@link TaskStatusListener#activeStageMap}
   * and {@link TaskStatusListener#activeRequestMap} for all running request once during server startup.
   * This is required as some tasks may have been in progress when server was last stopped
   * @param stages list of stages
   */
  private void publishInProgressTasks(List<Stage> stages) {
    if (taskStatusLoaded.compareAndSet(false, true)) {
      if (!stages.isEmpty()) {
        Function<Stage, Long> transform = new Function<Stage, Long>() {
          @Override
          public Long apply(Stage stage) {
            return stage.getRequestId();
          }
        };
        Set<Long> runningRequestID = ImmutableSet.copyOf(Lists.transform(stages, transform));
        List<HostRoleCommand> hostRoleCommands = db.getAllTasksByRequestIds(runningRequestID);
        hostRoleCommandDAO.publishTaskCreateEvent(hostRoleCommands);
      }
    }
  }

  /**
   * Returns filtered list of stages such that the returned list is an ordered
   * list of stages that may be executed in parallel or in the order in which
   * they are presented.
   * <p/>
   * The specified stages must be ordered by request ID and may only contain the
   * next stage in progress per request (as returned by
   * {@link ActionDBAccessor#getFirstStageInProgressPerRequest()}. This is
   * because there is a requirement that within a request, no two stages may
   * ever run in parallel.
   * <p/>
   * The following rules will be applied to the list:
   * <ul>
   * <li>Stages are filtered such that the first stage in the list (assumed to
   * be the first pending stage from the earliest active request) has priority.
   * </li>
   * <li>No stage in any request may be executed before an earlier stage in the
   * same request. This requirement is automatically covered by virtue of the
   * supplied stages only being for the next stage in progress per request.</li>
   * <li>A stage in different request may be performed in parallel
   * if-and-only-if the relevant hosts for the stage in the later requests do
   * not intersect with the union of hosts from (pending) stages in earlier
   * requests. In order to accomplish this</li>
   * </ul>
   *
   * @param firstStageInProgressPerRequest
   *          the stages to process, one stage per request
   * @return a list of stages that may be executed in parallel
   */
  private List<Stage> filterParallelPerHostStages(List<Stage> firstStageInProgressPerRequest) {
    // if there's only 1 stage in progress in 1 request, simply return that stage
    if (firstStageInProgressPerRequest.size() == 1) {
      return firstStageInProgressPerRequest;
    }

    List<Stage> retVal = new ArrayList<>();

    // set the lower range (inclusive) of requests to limit the query a bit
    // since there can be a LOT of commands
    long lowerRequestIdInclusive = firstStageInProgressPerRequest.get(0).getRequestId();

    // determine if this stage can be scheduled in parallel with the other
    // stages from other requests
    for (Stage stage : firstStageInProgressPerRequest) {
      long requestId = stage.getRequestId();

      if (LOG.isTraceEnabled()) {
        LOG.trace("==> Processing stage: {}/{} ({}) for {}", requestId, stage.getStageId(), stage.getRequestContext());
      }

      boolean addStage = true;

      // there are at least 2 request in progress concurrently; determine which
      // hosts are affected
      HashSet<String> hostsInProgressForEarlierRequests = new HashSet<>(
          hostRoleCommandDAO.getBlockingHostsForRequest(lowerRequestIdInclusive, requestId));

      // Iterate over the relevant hosts for this stage to see if any intersect with the set of
      // hosts needed for previous stages.  If any intersection occurs, this stage may not be
      // executed in parallel.
      for (String host : stage.getHosts()) {
        LOG.trace("===> Processing Host {}", host);

        if (hostsInProgressForEarlierRequests.contains(host)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("===>  Skipping stage since it utilizes at least one host that a previous stage requires: {}/{} ({})", stage.getRequestId(), stage.getStageId(), stage.getRequestContext());
          }

          addStage = false;
          break;
        }
      }

      // add the stage is no other prior stages for prior requests intersect the
      // hosts in this stage
      if (addStage) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("===>  Adding stage to return value: {}/{} ({})", stage.getRequestId(), stage.getStageId(), stage.getRequestContext());
        }

        retVal.add(stage);
      }
    }

    return retVal;
  }

  private boolean hasPreviousStageFailed(Stage stage) {
    boolean failed = false;

    long prevStageId = stage.getStageId() - 1;

    if (prevStageId >= 0) {
      // Find previous stage instance
      String actionId = StageUtils.getActionId(stage.getRequestId(), prevStageId);
      Stage prevStage = db.getStage(actionId);

      // If the previous stage is skippable then we shouldn't automatically fail the given stage
      if (prevStage == null || prevStage.isSkippable()) {
        return false;
      }

      Map<Role, Integer> hostCountsForRoles = new HashMap<>();
      Map<Role, Integer> failedHostCountsForRoles = new HashMap<>();

      for (String host : prevStage.getHostRoleCommands().keySet()) {
        Map<String, HostRoleCommand> roleCommandMap = prevStage.getHostRoleCommands().get(host);
        for (String role : roleCommandMap.keySet()) {
          HostRoleCommand c = roleCommandMap.get(role);
          if (hostCountsForRoles.get(c.getRole()) == null) {
            hostCountsForRoles.put(c.getRole(), 0);
            failedHostCountsForRoles.put(c.getRole(), 0);
          }
          int hostCount = hostCountsForRoles.get(c.getRole());
          hostCountsForRoles.put(c.getRole(), hostCount + 1);
          if (c.getStatus().isFailedAndNotSkippableState()) {
            int failedHostCount = failedHostCountsForRoles.get(c.getRole());
            failedHostCountsForRoles.put(c.getRole(), failedHostCount + 1);
          }
        }
      }

      for (Role role : hostCountsForRoles.keySet()) {
        float failedHosts = failedHostCountsForRoles.get(role);
        float totalHosts = hostCountsForRoles.get(role);
        if (((totalHosts - failedHosts) / totalHosts) < prevStage.getSuccessFactor(role)) {
          failed = true;
        }
      }
    }
    return failed;
  }

  /**
   * This method processes command timeouts and retry attempts, and
   * adds new (pending) execution commands to commandsToSchedule list.
   * @return the stats for the roles in the stage which are used to determine
   * whether stage has succeeded or failed
   */
  protected Map<String, RoleStats> processInProgressStage(Stage s, List<ExecutionCommand> commandsToSchedule,
                                                          Multimap<Long, AgentCommand> commandsToEnqueue) throws AmbariException {
    LOG.debug("==> Collecting commands to schedule...");
    // Map to track role status
    Map<String, RoleStats> roleStats = initRoleStats(s);
    long now = System.currentTimeMillis();
    Set<RoleCommandPair> rolesCommandsInProgress = s.getHostRolesInProgress();

    Cluster cluster = null;
    if (null != s.getClusterName()) {
      cluster = clusters.getCluster(s.getClusterName());
    }

    for (String host : s.getHosts()) {

      List<ExecutionCommandWrapper> commandWrappers = s.getExecutionCommands(host);
      Host hostObj = null;
      try {
        hostObj = clusters.getHost(host);
      } catch (AmbariException e) {
        LOG.debug("Host {} not found, stage is likely a server side action", host);
      }

      int i_my = 0;
      LOG.trace("===>host={}", host);

      for (ExecutionCommandWrapper wrapper : commandWrappers) {
        ExecutionCommand c = wrapper.getExecutionCommand();
        String roleStr = c.getRole();
        HostRoleStatus status = s.getHostRoleStatus(host, roleStr);
        i_my++;
        if (LOG.isTraceEnabled()) {
          LOG.trace("Host task {}) id = {} status = {} (role={}), roleCommand = {}", i_my, c.getTaskId(), status, roleStr, c.getRoleCommand());
        }
        boolean hostDeleted = false;
        if (null != cluster) {
          Service svc = null;
          if (c.getServiceName() != null && !c.getServiceName().isEmpty()) {
            svc = cluster.getService(c.getServiceName());
          }

          ServiceComponent svcComp = null;
          Map<String, ServiceComponentHost> scHosts = null;
          try {
            if (svc != null) {
              svcComp = svc.getServiceComponent(roleStr);
              scHosts = svcComp.getServiceComponentHosts();
            }
          } catch (ServiceComponentNotFoundException scnex) {
            String msg = String.format(
              "%s is not not a service component, assuming its an action",
              roleStr);
            LOG.debug(msg);
          }

          hostDeleted = (scHosts != null && !scHosts.containsKey(host));
          if (hostDeleted) {
            String message = String.format(
              "Host component information has not been found.  Details:" +
                "cluster=%s; host=%s; service=%s; component=%s; ",
              c.getClusterName(), host,
              svcComp == null ? "null" : svcComp.getServiceName(),
              svcComp == null ? "null" : svcComp.getName());
            LOG.warn(message);
          }
        }

        //basic timeout for stage
        long commandTimeout = actionTimeout;
        if (taskTimeoutAdjustment) {
          Map<String, String> commandParams = c.getCommandParams();
          String timeoutKey = ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
          if (commandParams != null && commandParams.containsKey(timeoutKey)) {
            String timeoutStr = commandParams.get(timeoutKey);
            commandTimeout += Long.parseLong(timeoutStr) * 1000; // Converting to milliseconds
          } else {
            LOG.error("Execution command has no timeout parameter" +
              c);
          }
        }

        // Check that service host component is not deleted
        boolean isHostStateUnknown = false;
        if (hostDeleted) {

          String message = String.format(
            "Host not found when trying to schedule an execution command. " +
              "The most probable reason for that is that host or host component " +
              "has been deleted recently. The command has been aborted and dequeued." +
              "Execution command details: " +
              "cmdId: %s; taskId: %s; roleCommand: %s",
            c.getCommandId(), c.getTaskId(), c.getRoleCommand());
          LOG.warn("Host {} has been detected as non-available. {}", host, message);
          // Abort the command itself
          // We don't need to send CANCEL_COMMANDs in this case
          db.abortHostRole(host, s.getRequestId(), s.getStageId(), c.getRole(), message);
          if (c.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
            processActionDeath(cluster.getClusterName(), c.getHostname(), roleStr);
          }
          status = HostRoleStatus.ABORTED;
        } else if (timeOutActionNeeded(status, s, hostObj, roleStr, now, commandTimeout)
          || (isHostStateUnknown = isHostStateUnknown(s, hostObj, roleStr))) {
          // Process command timeouts
          if (s.getAttemptCount(host, roleStr) >= maxAttempts || isHostStateUnknown) {
            LOG.warn("Host: {}, role: {}, actionId: {} expired and will be failed", host, roleStr,
              s.getActionId());

            // determine if the task should be auto skipped
            boolean isSkipSupported = s.isAutoSkipOnFailureSupported();
            HostRoleCommand hostRoleCommand = s.getHostRoleCommand(c.getTaskId());
            if (isSkipSupported && null != hostRoleCommand) {
              isSkipSupported = hostRoleCommand.isFailureAutoSkipped();
            }

            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole(), isSkipSupported, isHostStateUnknown);
            //Reinitialize status
            status = s.getHostRoleStatus(host, roleStr);

            if (null != cluster) {
              if (!RoleCommand.CUSTOM_COMMAND.equals(c.getRoleCommand())
                && !RoleCommand.SERVICE_CHECK.equals(c.getRoleCommand())
                && !RoleCommand.ACTIONEXECUTE.equals(c.getRoleCommand())) {
                //commands above don't affect host component state (e.g. no in_progress state in process), transition will fail
                transitionToFailedState(cluster.getClusterName(), c.getServiceName(), roleStr, host, now, false);
              }
              if (c.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
                processActionDeath(cluster.getClusterName(), c.getHostname(), roleStr);
              }
            }

            // Dequeue command
            LOG.info("Removing command from queue, host={}, commandId={} ", host, c.getCommandId());
          } else {
            cancelCommandOnTimeout(Collections.singletonList(s.getHostRoleCommand(host, roleStr)), commandsToEnqueue);

            LOG.info("Host: {}, role: {}, actionId: {} timed out and will be rescheduled", host,
              roleStr, s.getActionId());

            // reschedule command
            commandsToSchedule.add(c);
            LOG.trace("===> commandsToSchedule(reschedule)={}", commandsToSchedule.size());
          }
        } else if (status.equals(HostRoleStatus.PENDING)) {
          // in case of DEPENDENCY_ORDERED stage command can be scheduled only if all of it's dependencies are
          // already finished
          if (CommandExecutionType.STAGE == s.getCommandExecutionType() ||
                (CommandExecutionType.DEPENDENCY_ORDERED == s.getCommandExecutionType() &&
                  CommandExecutionType.DEPENDENCY_ORDERED == configuration.getStageExecutionType() &&
                  areCommandDependenciesFinished(c, s, rolesCommandsInProgress))) {

            //Need to schedule first time
            commandsToSchedule.add(c);
            LOG.trace("===>commandsToSchedule(first_time)={}", commandsToSchedule.size());
          }
        }

        updateRoleStats(status, roleStats.get(roleStr));
        if (status == HostRoleStatus.FAILED) {
          LOG.info("Role {} on host {} was failed", roleStr, host);
        }

      }
    }
    LOG.debug("Collected {} commands to schedule in this wakeup.", commandsToSchedule.size());
    return roleStats;
  }

  /**
   * Returns true if all command dependencies are already finished (not IN_PROGRESS states).
   * @param command
   * @param stage
   * @param rolesCommandsInProgress
   * @return
   */
  private boolean areCommandDependenciesFinished(ExecutionCommand command, Stage stage, Set<RoleCommandPair>
    rolesCommandsInProgress) {
    boolean areCommandDependenciesFinished = true;
    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(stage.getClusterId());
    if (rco != null) {
      RoleCommandPair roleCommand = new
              RoleCommandPair(Role.valueOf(command.getRole()), command.getRoleCommand());
      Set<RoleCommandPair> roleCommandDependencies = rco.getDependencies().get(roleCommand);

      // check if there are any dependencies IN_PROGRESS
      if (roleCommandDependencies != null) {
        // remove eventual references to the same RoleCommand
        roleCommandDependencies.remove(roleCommand);
        if (CollectionUtils.containsAny(rolesCommandsInProgress, roleCommandDependencies)) {
          areCommandDependenciesFinished = false;
        }
      }
    }

    return areCommandDependenciesFinished;

  }

  /**
   * Generate a OPFailed event before aborting all operations in the stage
   * @param stage
   */
  private void abortOperationsForStage(Stage stage) {
    long now = System.currentTimeMillis();

    for (String hostName : stage.getHosts()) {
      List<ExecutionCommandWrapper> commandWrappers =
        stage.getExecutionCommands(hostName);

      for(ExecutionCommandWrapper wrapper : commandWrappers) {
        ExecutionCommand c = wrapper.getExecutionCommand();
        transitionToFailedState(stage.getClusterName(), c.getServiceName(),
                c.getRole(), hostName, now, true);
      }
    }
    Collection<HostRoleCommandEntity> abortedOperations = db.abortOperation(stage.getRequestId());

    for (HostRoleCommandEntity command: abortedOperations) {
      if (command.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
        String clusterName = stage.getClusterName();
        processActionDeath(clusterName, command.getHostName(), command.getRole().name());
      }
    }
  }

  /**
   * Raise a OPFailed event for a SCH
   * @param clusterName
   * @param serviceName
   * @param componentName
   * @param hostname
   * @param timestamp
   */
  private void transitionToFailedState(String clusterName, String serviceName,
                                       String componentName, String hostname,
                                       long timestamp,
                                       boolean ignoreTransitionException) {

    try {
      Cluster cluster = clusters.getCluster(clusterName);

      ServiceComponentHostOpFailedEvent failedEvent =
        new ServiceComponentHostOpFailedEvent(componentName,
          hostname, timestamp);

      if (serviceName != null && ! serviceName.isEmpty() &&
              componentName != null && ! componentName.isEmpty()) {
        Service svc = cluster.getService(serviceName);
        ServiceComponent svcComp = svc.getServiceComponent(componentName);
        ServiceComponentHost svcCompHost =
                svcComp.getServiceComponentHost(hostname);
        svcCompHost.handleEvent(failedEvent);
      } else {
        LOG.info("Service name is " + serviceName + ", component name is " + componentName +
                "skipping sending ServiceComponentHostOpFailedEvent for " + componentName);
      }

    } catch (ServiceComponentNotFoundException scnex) {
      LOG.debug("{} associated with service {} is not a service component, assuming it's an action.", componentName, serviceName);
    } catch (ServiceComponentHostNotFoundException e) {
      String msg = String.format("Service component host %s not found, " +
              "unable to transition to failed state.", componentName);
      LOG.warn(msg, e);
    } catch (InvalidStateTransitionException e) {
      if (ignoreTransitionException) {
        LOG.debug("Unable to transition to failed state.", e);
      } else {
        LOG.warn("Unable to transition to failed state.", e);
      }
    } catch (AmbariException e) {
      LOG.warn("Unable to transition to failed state.", e);
    }
  }


  /**
   * Populates a map < role_name, role_stats>.
   */
  private Map<String, RoleStats> initRoleStats(Stage s) {
    // Meaning: how many hosts are affected by commands for each role
    Map<Role, Integer> hostCountsForRoles = new HashMap<>();
    // < role_name, rolestats >
    Map<String, RoleStats> roleStats = new TreeMap<>();

    for (String host : s.getHostRoleCommands().keySet()) {
      Map<String, HostRoleCommand> roleCommandMap = s.getHostRoleCommands().get(host);
      for (String role : roleCommandMap.keySet()) {
        HostRoleCommand c = roleCommandMap.get(role);
        if (hostCountsForRoles.get(c.getRole()) == null) {
          hostCountsForRoles.put(c.getRole(), 0);
        }
        int val = hostCountsForRoles.get(c.getRole());
        hostCountsForRoles.put(c.getRole(), val + 1);
      }
    }

    for (Role r : hostCountsForRoles.keySet()) {
      RoleStats stats = new RoleStats(hostCountsForRoles.get(r),
          s.getSuccessFactor(r));
      roleStats.put(r.name(), stats);
    }
    return roleStats;
  }

  /**
   * Checks if ambari-agent was restarted during role command execution
   * @param host the host with ambari-agent to check
   * @param stage the stage
   * @param role the role to check
   * @return {@code true} if ambari-agent was restarted
   */
  protected boolean wasAgentRestartedDuringOperation(Host host, Stage stage, String role) {
    String hostName = host.getHostName();
    long taskStartTime = stage.getHostRoleCommand(hostName, role).getStartTime();
    long lastAgentStartTime = host.getLastAgentStartTime();
    return taskStartTime > 0 && lastAgentStartTime > 0 && taskStartTime <= lastAgentStartTime;
  }

  /**
   * Checks if timeout is required.
   * @param status      the status of the current role
   * @param stage       the stage
   * @param host        the host object; can be {@code null} for server-side tasks
   * @param role        the role
   * @param currentTime the current
   * @param taskTimeout the amount of time to determine timeout
   * @return {@code true} if timeout is needed
   * @throws AmbariException
   */
  protected boolean timeOutActionNeeded(HostRoleStatus status, Stage stage,
      Host host, String role, long currentTime, long taskTimeout) throws
    AmbariException {
    if (( !status.equals(HostRoleStatus.QUEUED) ) &&
        ( ! status.equals(HostRoleStatus.IN_PROGRESS) )) {
      return false;
    }

    // tasks are held in a variety of in-memory maps that require a hostname key
    // host being null is ok - that means it's a server-side task
    String hostName = (null == host) ? null : host.getHostName();

    // If we have other command in progress for this stage do not timeout this one
    if (hasCommandInProgress(stage, hostName)
            && !status.equals(HostRoleStatus.IN_PROGRESS)) {
      return false;
    }
    if (currentTime >= stage.getLastAttemptTime(hostName, role)
        + taskTimeout) {
      return true;
    }
    return false;
  }

  private boolean isHostStateUnknown(Stage stage, Host host, String role) {
    if (null != host &&
      (host.getState().equals(HostState.HEARTBEAT_LOST) || wasAgentRestartedDuringOperation(host, stage, role))) {
      LOG.debug("Abort action since agent is not heartbeating or agent was restarted.");
      return true;
    }
    return false;
  }

  private boolean hasCommandInProgress(Stage stage, String host) {
    List<ExecutionCommandWrapper> commandWrappers = stage.getExecutionCommands(host);
    for (ExecutionCommandWrapper wrapper : commandWrappers) {
      ExecutionCommand c = wrapper.getExecutionCommand();
      String roleStr = c.getRole();
      HostRoleStatus status = stage.getHostRoleStatus(host, roleStr);
      if (status == HostRoleStatus.IN_PROGRESS) {
        return true;
      }
    }
    return false;
  }

  private ListMultimap<String, ServiceComponentHostEvent> formEventMap(Stage s, List<ExecutionCommand> commands) {
    ListMultimap<String, ServiceComponentHostEvent> serviceEventMap = ArrayListMultimap.create();
    for (ExecutionCommand cmd : commands) {
      String hostname = cmd.getHostname();
      String roleStr = cmd.getRole();
      if (RoleCommand.ACTIONEXECUTE != cmd.getRoleCommand()) {
          serviceEventMap.put(cmd.getServiceName(), s.getFsmEvent(hostname, roleStr).getEvent());
      }
    }
    return serviceEventMap;
  }

  private void processHostRole(RequestEntity r, Stage s, ExecutionCommand cmd, List<ExecutionCommand> commandsToStart,
                               List<ExecutionCommand> commandsToUpdate)
    throws AmbariException {
    long now = System.currentTimeMillis();
    String roleStr = cmd.getRole();
    String hostname = cmd.getHostname();

    // start time is -1 if host role command is not started yet
    if (s.getStartTime(hostname, roleStr) < 0) {

      commandsToStart.add(cmd);
      s.setStartTime(hostname,roleStr, now);
      s.setHostRoleStatus(hostname, roleStr, HostRoleStatus.QUEUED);
    }
    s.setLastAttemptTime(hostname, roleStr, now);
    s.incrementAttemptCount(hostname, roleStr);


    String requestPK = r.getRequestId().toString();
    String stagePk = s.getStageId() + "-" + s.getRequestId();

    // Try to get clusterHostInfo from cache
    Map<String, Set<String>> clusterHostInfo = clusterHostInfoCache.getIfPresent(requestPK);

    if (clusterHostInfo == null) {
      Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
      clusterHostInfo = StageUtils.getGson().fromJson(r.getClusterHostInfo(), type);
      clusterHostInfoCache.put(requestPK, clusterHostInfo);
    }

    cmd.setClusterHostInfo(clusterHostInfo);

    // Try to get commandParams from cache and merge them with command-level parameters
    Map<String, String> commandParams = commandParamsStageCache.getIfPresent(stagePk);

    if (commandParams == null){
      Type type = new TypeToken<Map<String, String>>() {}.getType();
      commandParams = StageUtils.getGson().fromJson(s.getCommandParamsStage(), type);
      commandParamsStageCache.put(stagePk, commandParams);
    }
    Map<String, String> commandParamsCmd = cmd.getCommandParams();
    commandParamsCmd.putAll(commandParams);
    cmd.setCommandParams(commandParamsCmd);

    try {
      Cluster cluster = clusters.getCluster(s.getClusterName());
      if (null != cluster) {
        // Generate localComponents
        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostname)) {
          cmd.getLocalComponents().add(sch.getServiceComponentName());
        }
      }
    } catch (ClusterNotFoundException cnfe) {
      // NOP
    }

    // Try to get hostParams from cache and merge them with command-level parameters
    Map<String, String> hostParams = hostParamsStageCache.getIfPresent(stagePk);
    if (hostParams == null) {
      Type type = new TypeToken<Map<String, String>>() {}.getType();
      hostParams = StageUtils.getGson().fromJson(s.getHostParamsStage(), type);
      hostParamsStageCache.put(stagePk, hostParams);
    }
    Map<String, String> hostParamsCmd = cmd.getHostLevelParams();
    hostParamsCmd.putAll(hostParams);
    cmd.setHostLevelParams(hostParamsCmd);

    // change the hostname in the command for the host itself
    cmd.setHostname(hostsMap.getHostMap(hostname));

    commandsToUpdate.add(cmd);
  }

  /**
   * @param requestId request will be cancelled on next scheduler wake up
   * (if it is in state that allows cancellation, e.g. QUEUED, PENDING, IN_PROGRESS)
   * @param reason why request is being cancelled
   */
  public void scheduleCancellingRequest(long requestId, String reason) {
    synchronized (requestsToBeCancelled) {
      requestsToBeCancelled.add(requestId);
      requestCancelReasons.put(requestId, reason);
    }
  }


  /**
   * Aborts all stages that belong to requests that are being cancelled
   */
  private void processCancelledRequestsList() throws AmbariException {
    synchronized (requestsToBeCancelled) {
      // Now, cancel stages completely
      for (Long requestId : requestsToBeCancelled) {
        // only pull back entities that have not completed; pulling back all
        // entities for the request can cause OOM problems on large requests,
        // like those for upgrades
        List<HostRoleCommandEntity> entitiesToDequeue = hostRoleCommandDAO.findByRequestIdAndStatuses(
            requestId, HostRoleStatus.NOT_COMPLETED_STATUSES);

        if (!entitiesToDequeue.isEmpty()) {
          List<HostRoleCommand> tasksToDequeue = new ArrayList<>(entitiesToDequeue.size());
          for (HostRoleCommandEntity hrcEntity : entitiesToDequeue) {
            HostRoleCommand task = hostRoleCommandFactory.createExisting(hrcEntity);
            tasksToDequeue.add(task);
          }

          String reason = requestCancelReasons.get(requestId);
          cancelHostRoleCommands(tasksToDequeue, reason);
        }

        // abort any stages in progress that belong to this request; don't execute this for all stages since
        // that could lead to OOM errors on large requests, like those for
        // upgrades
        List<Stage> stagesInProgress = db.getStagesInProgressForRequest(requestId);
        for (Stage stageInProgress : stagesInProgress) {
          abortOperationsForStage(stageInProgress);
        }
      }

      requestsToBeCancelled.clear();
      requestCancelReasons.clear();
    }
  }

  /**
   * Cancels host role commands (those that are not finished yet).
   * Dequeues host role commands that have been added to ActionQueue,
   * and automatically generates and adds to ActionQueue CANCEL_COMMANDs
   * for all hostRoleCommands that have already been sent to an agent for
   * execution.
   * @param hostRoleCommands a list of hostRoleCommands
   * @param reason why the request is being cancelled
   */
  void cancelHostRoleCommands(Collection<HostRoleCommand> hostRoleCommands, String reason) throws AmbariException {
    for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
      // There are no server actions in actionQueue
      if (!Role.AMBARI_SERVER_ACTION.equals(hostRoleCommand.getRole())) {
        if (hostRoleCommand.getStatus() == HostRoleStatus.QUEUED ||
              hostRoleCommand.getStatus() == HostRoleStatus.IN_PROGRESS) {
          CancelCommand cancelCommand = new CancelCommand();
          cancelCommand.setTargetTaskId(hostRoleCommand.getTaskId());
          cancelCommand.setReason(reason);
          agentCommandsPublisher.sendAgentCommand(hostRoleCommand.getHostId(), cancelCommand);
        }
      }

      if (hostRoleCommand.getStatus().isHoldingState()) {
        db.abortHostRole(hostRoleCommand.getHostName(),
            hostRoleCommand.getRequestId(),
            hostRoleCommand.getStageId(), hostRoleCommand.getRole().name());
      }

      // If host role is an Action, we have to send an event
      if (hostRoleCommand.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
        String clusterName = hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName();
        processActionDeath(clusterName,
                hostRoleCommand.getHostName(),
                hostRoleCommand.getRole().name());
      }
    }
  }

  void cancelCommandOnTimeout(Collection<HostRoleCommand> hostRoleCommands, Multimap<Long, AgentCommand> commandsToEnqueue) {
    for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
      // There are no server actions in actionQueue
      if (!Role.AMBARI_SERVER_ACTION.equals(hostRoleCommand.getRole())) {
        if (hostRoleCommand.getStatus() == HostRoleStatus.QUEUED ||
              hostRoleCommand.getStatus() == HostRoleStatus.IN_PROGRESS) {
          CancelCommand cancelCommand = new CancelCommand();
          cancelCommand.setTargetTaskId(hostRoleCommand.getTaskId());
          cancelCommand.setReason("Stage timeout");
          commandsToEnqueue.put(hostRoleCommand.getHostId(), cancelCommand);
        }
      }
    }
  }


  /**
   * Attempts to process kill/timeout/abort of action and send
   * appropriate event to all listeners
   */
  private void processActionDeath(String clusterName,
                                  String hostname,
                                  String role) {
    try {
      // Usually clusterId is defined (except the awkward case when
      // "Distribute repositories/install packages" action has been issued
      // against a concrete host without binding to a cluster)
      Long clusterId = clusterName != null ?
              clusters.getCluster(clusterName).getClusterId() : null;
      CommandReport report = new CommandReport();
      report.setRole(role);
      report.setStdOut("Action is dead");
      report.setStdErr("Action is dead");
      report.setStructuredOut("{}");
      report.setExitCode(1);
      report.setStatus(HostRoleStatus.ABORTED.toString());
      ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
              clusterId, hostname, report, true);
      ambariEventPublisher.publish(event);
    } catch (AmbariException e) {
      LOG.error(String.format("Can not get cluster %s", clusterName), e);
    }
  }

  private void updateRoleStats(HostRoleStatus status, RoleStats rs) {
    switch (status) {
      case COMPLETED:
        rs.numSucceeded++;
        break;
      case FAILED:
        rs.numFailed++;
        break;
      case QUEUED:
        rs.numQueued++;
        break;
      case PENDING:
        rs.numPending++;
        break;
      case TIMEDOUT:
        rs.numTimedOut++;
        break;
      case ABORTED:
        rs.numAborted++;
        break;
      case IN_PROGRESS:
        rs.numInProgress++;
        break;
      case HOLDING:
      case HOLDING_FAILED:
      case HOLDING_TIMEDOUT:
        rs.numHolding++;
        break;
      case SKIPPED_FAILED:
        rs.numSkipped++;
        break;
      default:
        LOG.error("Unknown status " + status.name());
    }
  }


  public void setTaskTimeoutAdjustment(boolean val) {
    taskTimeoutAdjustment = val;
  }

  ServerActionExecutor getServerActionExecutor() {
    return serverActionExecutor;
  }

  /**
   * Handles {@link EntityManagerCacheInvalidationEvent} instances and instructs
   * the thread running this scheduler to evict instances from the
   * {@link EntityManager}.
   *
   * @param event
   *          the event to handle (not {@code null}).
   */
  @Subscribe
  public void onEvent(EntityManagerCacheInvalidationEvent event) {
    try {
      if (null != threadEntityManager && threadEntityManager.isOpen()) {
        threadEntityManager.clear();
      }
    } catch (Throwable throwable) {
      LOG.error("Unable to clear the EntityManager for the scheduler thread", throwable);
    }
  }

  static class RoleStats {
    int numInProgress;
    int numQueued = 0;
    int numSucceeded = 0;
    int numFailed = 0;
    int numTimedOut = 0;
    int numPending = 0;
    int numAborted = 0;
    int numHolding = 0;
    int numSkipped = 0;

    final int totalHosts;
    final float successFactor;

    RoleStats(int total, float successFactor) {
      totalHosts = total;
      this.successFactor = successFactor;
    }

    /**
     * Role successful means the role is successful enough to
     */
    boolean isSuccessFactorMet() {
      int minSuccessNeeded = (int) Math.ceil(successFactor * totalHosts);
      return minSuccessNeeded <= numSucceeded;
    }

    private boolean isRoleInProgress() {
      return numPending + numQueued + numInProgress + numHolding > 0;
    }

    /**
     * Role failure means role is no longer in progress and success factor is
     * not met.
     */
    boolean isRoleFailed() {
      return !(isRoleInProgress() || isSuccessFactorMet());
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("numQueued=").append(numQueued);
      builder.append(", numInProgress=").append(numInProgress);
      builder.append(", numSucceeded=").append(numSucceeded);
      builder.append(", numFailed=").append(numFailed);
      builder.append(", numTimedOut=").append(numTimedOut);
      builder.append(", numPending=").append(numPending);
      builder.append(", numAborted=").append(numAborted);
      builder.append(", numSkipped=").append(numSkipped);
      builder.append(", totalHosts=").append(totalHosts);
      builder.append(", successFactor=").append(successFactor);
      return builder.toString();
    }
  }
}
