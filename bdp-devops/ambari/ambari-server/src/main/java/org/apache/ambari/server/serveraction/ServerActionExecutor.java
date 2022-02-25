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

package org.apache.ambari.server.serveraction;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeServiceSummary;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeSummary;
import org.apache.ambari.server.state.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ClassUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Server Action Executor used to execute server-side actions (or tasks)
 * <p/>
 * The ServerActionExecutor executes in its own thread, polling for AMBARI_SERVER_ACTION
 * HostRoleCommands queued for execution.  It is expected that this thread is managed by the
 * ActionScheduler such that it is started when the ActionScheduler is started and stopped when the
 * ActionScheduler is stopped.
 */
@StaticallyInject
public class ServerActionExecutor {

  private final static Logger LOG = LoggerFactory.getLogger(ServerActionExecutor.class);
  private final static Long DEFAULT_EXECUTION_TIMEOUT_MS = 1000L * 60 * 5;
  private final static Long POLLING_TIMEOUT_MS = 1000L * 5;

  /**
   * An injector to use to inject objects into ServerAction instances.
   */
  @Inject
  private static Injector injector;

  /**
   * The Ambari configuration used to obtain configured details such as the default server-side action
   * timeout.
   */
  @Inject
  private static Configuration configuration;


  /**
   * Maps request IDs to "blackboards" of shared data.
   * <p/>
   * This map is not synchronized, so any access to it should synchronize on
   * requestSharedDataMap object
   */
  private final Map<Long, ConcurrentMap<String, Object>> requestSharedDataMap =
      new ConcurrentHashMap<>();

  /**
   * Database accessor to query and update the database of action commands.
   */
  private final ActionDBAccessor db;

  /**
   * Internal locking object used to manage access to activeAwakeRequest.
   */
  private final Object wakeupSyncObject = new Object();

  /**
   * Timeout (in milliseconds) used to throttle polling of database for new action commands.
   */
  private final long sleepTimeMS;

  /**
   * Flag used to help keep thing moving in the event an "awake" request was encountered while busy
   * handing an action.
   */
  private boolean activeAwakeRequest = false;

  /**
   * A reference to the Thread handling the work for this ServerActionExecutor
   */
  private Thread executorThread = null;

  /**
   * A timer used to clear out {@link #requestSharedDataMap}. Since this "cache"
   * isn't timer- or access-based, then we must periodically check it in order
   * to clear out any stale data.
   */
  private final Timer cacheTimer = new Timer("server-action-executor-cache-timer", true);

  /**
   * Statically initialize the Injector
   * <p/>
   * This should only be used for unit tests.
   *
   * @param injector the Injector to (manually) statically inject
   */
  public static void init(Injector injector) {
    ServerActionExecutor.injector = injector;
  }

  /**
   * Creates a new ServerActionExecutor
   *
   * @param db          the ActionDBAccessor to use to read and update tasks
   * @param sleepTimeMS the time (in milliseconds) to wait between polling the database for more tasks
   */
  public ServerActionExecutor(ActionDBAccessor db, long sleepTimeMS) {
    this.db = db;
    this.sleepTimeMS = (sleepTimeMS < 1) ? POLLING_TIMEOUT_MS : sleepTimeMS;

    // start in 1 hour, run every hour
    cacheTimer.schedule(new ServerActionSharedRequestEvictor(), TimeUnit.HOURS.toMillis(1),
        TimeUnit.HOURS.toMillis(1));
  }

  /**
   * Starts this ServerActionExecutor's main thread.
   */
  public void start() {
    LOG.info("Starting Server Action Executor thread...");
    executorThread = new Thread(new Runnable() {

      @Override
      public void run() {
        while (!Thread.interrupted()) {
          try {
            synchronized (wakeupSyncObject) {
              if (!activeAwakeRequest) {
                wakeupSyncObject.wait(sleepTimeMS);
              }
              activeAwakeRequest = false;
            }

            doWork();
          } catch (InterruptedException e) {
            LOG.warn("Server Action Executor thread interrupted, starting to shutdown...");
            break;
          }
        }

        LOG.info("Server Action Executor thread shutting down...");
      }
    }, "Server Action Executor");
    executorThread.start();

    if (executorThread.isAlive()) {
      LOG.info("Server Action Executor thread started.");
    }
  }

  /**
   * Attempts to stop this ServerActionExecutor's main thread.
   */
  public void stop() {
    LOG.info("Stopping Server Action Executor thread...");

    if (executorThread != null) {
      executorThread.interrupt();

      // Wait for about 60 seconds for the thread to stop
      for (int i = 0; i < 120; i++) {
        try {
          executorThread.join(500);
        } catch (InterruptedException e) {
          // Ignore this...
        }

        if (!executorThread.isAlive()) {
          break;
        }
      }

      if (!executorThread.isAlive()) {
        executorThread = null;
      }
    }

    if (executorThread == null) {
      LOG.info("Server Action Executor thread stopped.");
    } else {
      LOG.warn("Server Action Executor thread hasn't stopped, giving up waiting.");
    }
  }

  /**
   * Attempts to force this ServerActionExecutor to wake up and do work.
   * <p/>
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

  /**
   * Returns a Map to be used to share data among server actions within a given request context.
   *
   * @param requestId a long identifying the id of the relevant request
   * @return a ConcurrentMap of "shared" data
   */
  private ConcurrentMap<String, Object> getRequestSharedDataContext(long requestId) {
    synchronized (requestSharedDataMap) {
      ConcurrentMap<String, Object> map = requestSharedDataMap.get(requestId);

      if (map == null) {
        map = new ConcurrentHashMap<>();
        requestSharedDataMap.put(requestId, map);
      }

      return map;
    }
  }

  /**
   * A helper method to create CommandReports indicating the action/task is in progress
   *
   * @return a new CommandReport
   */
  private CommandReport createInProgressReport() {
    CommandReport commandReport = new CommandReport();
    commandReport.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    commandReport.setStdErr("");
    commandReport.setStdOut("");
    return commandReport;
  }

  /**
   * A helper method to create CommandReports indicating the action/task had timed out
   *
   * @return a new CommandReport
   */
  private CommandReport createTimedOutReport() {
    CommandReport commandReport = new CommandReport();
    commandReport.setStatus(HostRoleStatus.TIMEDOUT.toString());
    commandReport.setStdErr("");
    commandReport.setStdOut("");
    return commandReport;
  }

  /**
   * A helper method to create CommandReports indicating the action/task has had an error
   *
   * @param message a String containing the error message to report
   * @return a new CommandReport
   */
  private CommandReport createErrorReport(String message) {
    CommandReport commandReport = new CommandReport();
    commandReport.setStatus(HostRoleStatus.FAILED.toString());
    commandReport.setExitCode(1);
    commandReport.setStdOut("Server action failed");
    commandReport.setStdErr((message == null) ? "Server action failed" : message);
    return commandReport;
  }

  /**
   * Stores the status of the task/action
   * <p/>
   * If the command report is not specified (null), an error report will be created.
   *
   * @param hostRoleCommand  the HostRoleCommand for the relevant task
   * @param executionCommand the ExecutionCommand for the relevant task
   * @param commandReport    the CommandReport to store
   */
  private void updateHostRoleState(HostRoleCommand hostRoleCommand, ExecutionCommand executionCommand,
                                   CommandReport commandReport) {
    if (commandReport == null) {
      commandReport = createErrorReport("Unknown error condition");
    }

    db.updateHostRoleState(null, hostRoleCommand.getRequestId(),
        hostRoleCommand.getStageId(), executionCommand.getRole(), commandReport);
  }

  /**
   * Determine what the timeout for this action/task should be.
   * <p/>
   * If the timeout value is not set in the command parameter map (under the key
   * ExecutionCommand.KeyNames.COMMAND_TIMEOUT or "command_timeout", the default timeout value will
   * be used.  It is expected that the timeout value stored in the command parameter map (if any) is
   * in seconds.
   *
   * @param executionCommand the ExecutionCommand for the relevant task
   * @return a long declaring the action/task's timeout
   */
  private long determineTimeout(ExecutionCommand executionCommand) {
    Map<String, String> params = executionCommand.getCommandParams();
    String paramsTimeout = (params == null) ? null : params.get(ExecutionCommand.KeyNames.COMMAND_TIMEOUT);
    Long timeout;

    try {
      timeout = (paramsTimeout == null)
          ? null
          : (Long.parseLong(paramsTimeout) * 1000L); // Convert seconds to milliseconds
    } catch (NumberFormatException e) {
      timeout = null;
    }

    // If a configured timeout value is not set for the command, attempt to get the configured
    // default
    if (timeout == null) {
      Integer defaultTimeoutSeconds = configuration.getDefaultServerTaskTimeout();
      if (defaultTimeoutSeconds != null) {
        timeout = defaultTimeoutSeconds * 1000L; // convert seconds to milliseconds
      }
    }

    // If a timeout value was not determined, return the hard-coded timeout value, else return the
    // determined timeout value.
    return (timeout == null)
        ? DEFAULT_EXECUTION_TIMEOUT_MS
        : ((timeout < 0) ? 0 : timeout);
  }

  /**
   * Execute the logic to handle each task in the queue in the order in which it was queued.
   * <p/>
   * A single task is executed at one time, allowing for a specified (ExecutionCommand.KeyNames.COMMAND_TIMEOUT)
   * or the default timeout for it to complete before considering the task timed out.
   *
   * @throws InterruptedException
   */
  public void doWork() throws InterruptedException {
    List<HostRoleCommand> tasks = db.getTasksByRoleAndStatus(Role.AMBARI_SERVER_ACTION.name(),
      HostRoleStatus.QUEUED);

    if ((tasks != null) && !tasks.isEmpty()) {
      for (HostRoleCommand task : tasks) {
        Long taskId = task.getTaskId();

        LOG.debug("Processing task #{}", taskId);

        if (task.getStatus() == HostRoleStatus.QUEUED) {
          ExecutionCommandWrapper executionWrapper = task.getExecutionCommandWrapper();

          if (executionWrapper != null) {
            ExecutionCommand executionCommand = executionWrapper.getExecutionCommand();

            if (executionCommand != null) {
              // For now, execute only one task at a time. This may change in the future in the
              // event it is discovered that this is a bottleneck. Since this implementation may
              // change, it should be noted from outside of this class, that there is no expectation
              // that tasks will be processed in order or serially.
              Worker worker = new Worker(task, executionCommand);
              Thread workerThread = new Thread(worker, String.format("Server Action Executor Worker %s", taskId));
              Long timeout = determineTimeout(executionCommand);

              updateHostRoleState(task, executionCommand, createInProgressReport());

              LOG.debug("Starting Server Action Executor Worker thread for task #{}.", taskId);
              workerThread.start();

              try {
                workerThread.join(timeout);
              } catch (InterruptedException e) {
                // Make sure the workerThread is interrupted as well.
                workerThread.interrupt();
                throw e;
              }

              if (workerThread.isAlive()) {
                LOG.debug("Server Action Executor Worker thread for task #{} timed out - it failed to complete within {} ms.",
                    taskId, timeout);
                workerThread.interrupt();
                updateHostRoleState(task, executionCommand, createTimedOutReport());
              } else {
                LOG.debug("Server Action Executor Worker thread for task #{} exited on its own.", taskId);
                updateHostRoleState(task, executionCommand, worker.getCommandReport());
              }
            } else {
              LOG.warn("Task #{} failed to produce an ExecutionCommand, skipping.", taskId);
            }
          } else {
            LOG.warn("Task #{} failed to produce an ExecutionCommandWrapper, skipping.", taskId);
          }
        } else {
          LOG.warn("Queued task #{} is expected to have a status of {} but has a status of {}, skipping.",
              taskId, HostRoleStatus.QUEUED, task.getStatus());
        }
      }
    }
  }

  /**
   * Internal class to execute a unit of work in its own thread
   */
  private class Worker implements Runnable {
    /**
     * The task id of the relevant task
     */
    private final Long taskId;

    /**
     * The HostRoleCommand data used by this Worker to execute the task
     */
    private final HostRoleCommand hostRoleCommand;

    /**
     * The ExecutionCommand data used by this Worker to execute the task
     */
    private final ExecutionCommand executionCommand;

    /**
     * The resulting CommandReport used by the caller to update the status of the relevant task
     */
    private CommandReport commandReport = null;

    @Override
    public void run() {
      try {
        LOG.debug("Executing task #{}", taskId);

        // Set an internal administrator user to be the authenticated user in the event an
        // authorization check is needed while performing a server-side action.
        InternalAuthenticationToken authentication = new InternalAuthenticationToken("server_action_executor");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        commandReport = execute(hostRoleCommand, executionCommand);

        LOG.debug("Task #{} completed execution with status of {}",
            taskId, (commandReport == null) ? "UNKNOWN" : commandReport.getStatus());
      } catch (Throwable t) {
        LOG.warn("Task #{} failed to complete execution due to thrown exception: {}:{}",
            taskId, t.getClass().getName(), t.getLocalizedMessage(), t);

        commandReport = createErrorReport(t.getLocalizedMessage());
      }
    }

    /**
     * Returns the resulting CommandReport
     *
     * @return a CommandReport
     */
    public CommandReport getCommandReport() {
      return commandReport;
    }

    /**
     * Attempts to execute the task specified using data from the supplied HostRoleCommand and
     * ExecutionCommand.
     * <p/>
     * Retrieves the role parameters from the supplied ExecutionCommand and queries it for the
     * "ACTON_NAME" property.  The returned String is expected to be the classname of a ServerAction
     * implementation.  If so, an instance of the implementation class is created and executed
     * yielding a CommandReport to (eventually) return back to the parent thread.
     *
     * @param hostRoleCommand  The HostRoleCommand the HostRoleCommand for the relevant task
     * @param executionCommand the ExecutionCommand for the relevant task
     * @return the resulting CommandReport
     * @throws AmbariException
     * @throws InterruptedException
     */
    private CommandReport execute(HostRoleCommand hostRoleCommand, ExecutionCommand executionCommand)
        throws AmbariException, InterruptedException {

      if (hostRoleCommand == null) {
        throw new AmbariException("Missing HostRoleCommand data");
      } else if (executionCommand == null) {
        throw new AmbariException("Missing ExecutionCommand data");
      } else {
        Map<String, String> roleParams = executionCommand.getRoleParams();
        if (roleParams == null) {
          throw new AmbariException("Missing RoleParams data");
        } else {
          String actionClassname = roleParams.get(ServerAction.ACTION_NAME);

          if (actionClassname == null) {
            throw new AmbariException("Missing action classname for server action");
          } else {
            Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
            UpgradeSummary upgradeSummary = executionCommand.getUpgradeSummary();
            if (upgradeSummary != null) {
              Map<String, UpgradeServiceSummary> upgradeServiceSummaries = upgradeSummary.services;
              LOG.debug("UpgradeServiceSummary: " + upgradeServiceSummaries);
              AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
              AmbariMetaInfo ambariMetaInfo = ambariManagementController.getAmbariMetaInfo();
              String serviceName = executionCommand.getServiceName();
              if (serviceName != null && !serviceName.isEmpty()){
                LOG.info(String.format("Server action %s is associated with service %s", actionClassname, serviceName));
                //Execution stage of a given service, only need to examine stack information for this one service
                UpgradeServiceSummary serviceSummary = upgradeServiceSummaries.get(serviceName);
                addServiceInfo(services, ambariMetaInfo, serviceSummary.sourceStackId, serviceName);
              } else {
                LOG.info(String.format("Server action %s is not associated with a service", actionClassname));
                //Load all Jars
                for(String key: upgradeServiceSummaries.keySet()){
                  UpgradeServiceSummary serviceSummary = upgradeServiceSummaries.get(key);
                  addServiceInfo(services, ambariMetaInfo, serviceSummary.sourceStackId, key);
                }
              }
              LOG.info(String.format("Attempt to load server action classes from %s", services.keySet().toString()));
            }
            ServerAction action = createServerAction(actionClassname, services);
            if (action == null) {
              throw new AmbariException("Failed to create server action: " + actionClassname);
            } else {
              // Set properties on the action:
              action.setExecutionCommand(executionCommand);
              action.setHostRoleCommand(hostRoleCommand);

              return action.execute(getRequestSharedDataContext(hostRoleCommand.getRequestId()));
            }
          }
        }
      }
    }

    private void addServiceInfo(Map<String, ServiceInfo> services, AmbariMetaInfo ambariMetaInfo, String stackId, String serviceName) {
      List<String> stackInfo = getStackInfo(stackId);
      LOG.debug(String.format("Stack info list: %s", stackInfo));
      if (stackInfo.size() > 1) {
        try {
          ServiceInfo service = ambariMetaInfo.getService(stackInfo.get(0), stackInfo.get(1), serviceName);
          LOG.debug(String.format("Adding %s to the list of services for loading external Jars...", service.getName()));
          services.put(serviceName, service);
        } catch (AmbariException e) {
          LOG.error(String.format("Failed to obtain service info for stack %s, service name %s", stackId, serviceName), e);
        }
      }
    }

    private List<String> getStackInfo(String stackId) {
      LOG.debug(String.format("Stack id: %s", stackId));
      StringTokenizer tokens = new StringTokenizer(stackId, "-");
      List<String> info = new ArrayList<String>();
      while (tokens.hasMoreElements()) {
        info.add((String)tokens.nextElement());
      }
      return info;
    }

    /**
     * Attempts to create an instance of the ServerAction class implementation specified in
     * classname.
     *
     * @param classname a String declaring the classname of the ServerAction class to instantiate
     * @return the instantiated ServerAction implementation
     * @throws AmbariException
     */
    private ServerAction createServerAction(String classname, Map<String, ServiceInfo> services) throws AmbariException {
      Class<?> actionClass = null;
      actionClass = getServerActionClass(classname);
      if (actionClass == null) {
        LOG.debug(String.format("Did not find %s in Ambari, try to load it from external directories", classname));
        actionClass = getServiceLevelServerActionClass(classname, services);
      }

      if (actionClass == null) {
        throw new AmbariException("Unable to load server action class: " + classname);
      } else {
        LOG.debug(String.format("Ready to init server action %s", classname));
        Class<? extends ServerAction> serverActionClass = actionClass.asSubclass(ServerAction.class);
        if (serverActionClass == null) {
          throw new AmbariException("Unable to execute server action class, invalid type: " + classname);
        } else {
          return injector.getInstance(serverActionClass);
        }
      }
    }

    /**
     * Load server action classes defined in the service level Jar files
     * */
    private Class<?> getServiceLevelServerActionClass(String classname, Map<String, ServiceInfo> services) {
      List<URL> urls = new ArrayList<>();
      for (ServiceInfo service : services.values()) {
        LOG.debug(String.format("Checking service %s", service));
        File dir = service.getServerActionsFolder();
        if ( dir != null) {
          LOG.debug(String.format("Service %s, external dir %s",service.getName(), dir.getAbsolutePath()));
          File[] jars = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              LOG.debug(String.format("Checking folder %s", name));
              return name.endsWith(".jar");
            }
          });
          for (File jar : jars) {
            try {
              URL url = jar.toURI().toURL();
              urls.add(url);
              LOG.info("Adding server action jar to classpath: {}", url);
            }
            catch (Exception e) {
              LOG.error("Failed to add server action jar to classpath: {}", jar.getAbsolutePath(), e);
            }
          }
        } else {
          LOG.error(String.format("%s service server actions folder returned null", service));
        }
      }

      ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassUtils.getDefaultClassLoader());
      Class<?> actionClass = null;
      try {
        actionClass = ClassUtils.resolveClassName(classname, classLoader);
        LOG.debug(String.format("Found external server action %s", classname));
      } catch(IllegalArgumentException illegalArgumentException) {
        LOG.error(String.format("Unable to find server action %s in external server action directories", classname), illegalArgumentException);
      }

      return actionClass;
    }

    /**
     * Load server action classes defined in Ambari source code
     * */
    private Class<?> getServerActionClass(String classname) throws AmbariException{
      Class<?> actionClass = null;
      try {
        actionClass = Class.forName(classname);
        if (actionClass == null) {
          LOG.warn(String.format("Unable to load server action class: %s from Ambari", classname));
        }
      } catch (ClassNotFoundException e) {
        LOG.error(String.format("Unable to load server action class: %s", classname), e);
      }
      return actionClass;
    }

    /**
     * Constructs a new Worker used to execute a task
     *
     * @param hostRoleCommand  the HostRoleCommand for the relevant task
     * @param executionCommand the ExecutionCommand for the relevant task
     */
    private Worker(HostRoleCommand hostRoleCommand, ExecutionCommand executionCommand) {
      taskId = hostRoleCommand.getTaskId();
      this.hostRoleCommand = hostRoleCommand;
      this.executionCommand = executionCommand;
    }
  }

  /**
   * The {@link ServerActionSharedRequestEvictor} is used to clear the shared
   * request cache periodically. This service will only run periodically and,
   * when it does, it will try to make the least expensive call to determine if
   * entries need to be evicted.
   */
  private class ServerActionSharedRequestEvictor extends TimerTask {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      // if the cache is empty, do nothing
      if (requestSharedDataMap.isEmpty()) {
        return;
      }

      // if the cache has requests, see if any are still in progress
      try {
        // find the requests in progress; there's no need to get the request
        // itself since that could be a massive object; we just need the ID
        Set<Long> requestsInProgress = new HashSet<>();
        List<Stage> currentStageInProgressPerRequest = db.getFirstStageInProgressPerRequest();
        for (Stage stage : currentStageInProgressPerRequest) {
          requestsInProgress.add(stage.getRequestId());
        }

        // for every item in the map, get the request and check its status
        synchronized (requestSharedDataMap) {
          Set<Long> cachedRequestIds = requestSharedDataMap.keySet();
          for (long cachedRequestId : cachedRequestIds) {
            if (!requestsInProgress.contains(cachedRequestId)) {
              requestSharedDataMap.remove(cachedRequestId);
            }
          }
        }
      } catch (Exception exception) {
        LOG.warn("Unable to clear the server-side action request cache", exception);
      }
    }
  }
}
