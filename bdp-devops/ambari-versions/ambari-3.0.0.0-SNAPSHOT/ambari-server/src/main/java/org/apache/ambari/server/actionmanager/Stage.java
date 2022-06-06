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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.metadata.RoleCommandPair;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

//This class encapsulates the stage. The stage encapsulates all the information
//required to persist an action.
public class Stage {

  /**
   * Used because in-memory storage of commands requires a hostname for maps
   * when the underlying store does not (host_id is {@code null}).  We also
   * don't want stages getting confused with Ambari vs cluster hosts, so
   * don't use {@link StageUtils#getHostName()}
   */
  public static final String INTERNAL_HOSTNAME = "_internal_ambari";

  private static final Logger LOG = LoggerFactory.getLogger(Stage.class);
  private final long requestId;
  private String clusterName;
  private long clusterId = -1L;
  private long stageId = -1;
  private final String logDir;
  private final String requestContext;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private HostRoleStatus displayStatus = HostRoleStatus.PENDING;
  private String commandParamsStage;
  private String hostParamsStage;

  private CommandExecutionType commandExecutionType = CommandExecutionType.STAGE;

  private boolean skippable;
  private boolean supportsAutoSkipOnFailure;

  private int stageTimeout = -1;

  private volatile boolean wrappersLoaded = false;

  //Map of roles to successFactors for this stage. Default is 1 i.e. 100%
  private Map<Role, Float> successFactors = new HashMap<>();

  //Map of host to host-roles
  Map<String, Map<String, HostRoleCommand>> hostRoleCommands =
    new TreeMap<>();
  private Map<String, List<ExecutionCommandWrapper>> commandsToSend =
    new TreeMap<>();

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private ExecutionCommandWrapperFactory ecwFactory;

  @AssistedInject
  public Stage(@Assisted long requestId,
      @Assisted("logDir") String logDir,
      @Assisted("clusterName") @Nullable String clusterName,
      @Assisted("clusterId") long clusterId,
      @Assisted("requestContext") @Nullable String requestContext,
      @Assisted("commandParamsStage") String commandParamsStage,
      @Assisted("hostParamsStage") String hostParamsStage,
      HostRoleCommandFactory hostRoleCommandFactory, ExecutionCommandWrapperFactory ecwFactory) {
    wrappersLoaded = true;
    this.requestId = requestId;
    this.logDir = logDir;
    this.clusterName = clusterName;
    this.clusterId = clusterId;
    this.requestContext = requestContext == null ? "" : requestContext;
    this.commandParamsStage = commandParamsStage;
    this.hostParamsStage = hostParamsStage;

    skippable = false;
    supportsAutoSkipOnFailure = false;

    this.hostRoleCommandFactory = hostRoleCommandFactory;
    this.ecwFactory = ecwFactory;
  }

  @AssistedInject
  public Stage(@Assisted StageEntity stageEntity, HostRoleCommandDAO hostRoleCommandDAO,
      ActionDBAccessor dbAccessor, Clusters clusters, HostRoleCommandFactory hostRoleCommandFactory,
      ExecutionCommandWrapperFactory ecwFactory) {
    this.hostRoleCommandFactory = hostRoleCommandFactory;
    this.ecwFactory = ecwFactory;

    requestId = stageEntity.getRequestId();
    stageId = stageEntity.getStageId();
    skippable = stageEntity.isSkippable();
    supportsAutoSkipOnFailure = stageEntity.isAutoSkipOnFailureSupported();
    logDir = stageEntity.getLogInfo();

    clusterId = stageEntity.getClusterId().longValue();
    if (-1L != clusterId) {
      try {
        clusterName = clusters.getClusterById(clusterId).getClusterName();
      } catch (Exception e) {
        LOG.debug("Could not load cluster with id {}, the cluster may have been removed for stage {}",
            Long.valueOf(clusterId), Long.valueOf(stageId));
      }
    }

    requestContext = stageEntity.getRequestContext();
    commandParamsStage = stageEntity.getCommandParamsStage();
    hostParamsStage = stageEntity.getHostParamsStage();
    commandExecutionType = stageEntity.getCommandExecutionType();
    status = stageEntity.getStatus();
    displayStatus = stageEntity.getDisplayStatus();

    List<Long> taskIds = hostRoleCommandDAO.findTaskIdsByStage(requestId, stageId);
    Collection<HostRoleCommand> commands = dbAccessor.getTasks(taskIds);

    for (HostRoleCommand command : commands) {
      // !!! some commands won't have a hostname, because they are server-side and
      // don't hold that information.  In that case, use the special key to
      // use in the map
      String hostname = getSafeHost(command.getHostName());

      if (!hostRoleCommands.containsKey(hostname)) {
        hostRoleCommands.put(hostname, new LinkedHashMap<>());
      }

      hostRoleCommands.get(hostname).put(command.getRole().toString(), command);
    }

    for (RoleSuccessCriteriaEntity successCriteriaEntity : stageEntity.getRoleSuccessCriterias()) {
      successFactors.put(successCriteriaEntity.getRole(), successCriteriaEntity.getSuccessFactor().floatValue());
    }
  }

  /**
   * Creates object to be persisted in database
   * @return StageEntity
   */
  public synchronized StageEntity constructNewPersistenceEntity() {
    StageEntity stageEntity = new StageEntity();
    stageEntity.setRequestId(requestId);
    stageEntity.setStageId(getStageId());
    stageEntity.setLogInfo(logDir);
    stageEntity.setSkippable(skippable);
    stageEntity.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);
    stageEntity.setRequestContext(requestContext);
    stageEntity.setHostRoleCommands(new ArrayList<>());
    stageEntity.setRoleSuccessCriterias(new ArrayList<>());
    stageEntity.setCommandParamsStage(commandParamsStage);
    if (null != hostParamsStage) {
      stageEntity.setHostParamsStage(hostParamsStage);
    }
    stageEntity.setCommandExecutionType(commandExecutionType);
    stageEntity.setStatus(status);
    stageEntity.setDisplayStatus(displayStatus);

    for (Role role : successFactors.keySet()) {
      RoleSuccessCriteriaEntity roleSuccessCriteriaEntity = new RoleSuccessCriteriaEntity();
      roleSuccessCriteriaEntity.setRole(role);
      roleSuccessCriteriaEntity.setStage(stageEntity);
      roleSuccessCriteriaEntity.setSuccessFactor(successFactors.get(role).doubleValue());
      stageEntity.getRoleSuccessCriterias().add(roleSuccessCriteriaEntity);
    }
    return stageEntity;
  }

  void checkWrappersLoaded() {
    if (!wrappersLoaded) {
      synchronized (this) { // Stages are not used concurrently now, but it won't be performance loss
        if (!wrappersLoaded) {
          loadExecutionCommandWrappers();
        }
      }
    }
  }

  @Transactional
  void loadExecutionCommandWrappers() {
    for (Map.Entry<String, Map<String, HostRoleCommand>> hostRoleCommandEntry : hostRoleCommands.entrySet()) {
      String hostname = hostRoleCommandEntry.getKey();
      List<ExecutionCommandWrapper> wrappers = new ArrayList<>();
      Map<String, HostRoleCommand> roleCommandMap = hostRoleCommandEntry.getValue();
      for (Map.Entry<String, HostRoleCommand> roleCommandEntry : roleCommandMap.entrySet()) {
        wrappers.add(roleCommandEntry.getValue().getExecutionCommandWrapper());
      }
      commandsToSend.put(hostname, wrappers);
    }
  }

  public List<HostRoleCommand> getOrderedHostRoleCommands() {
    List<HostRoleCommand> commands = new ArrayList<>();
    //Correct due to ordered maps
    for (Map.Entry<String, Map<String, HostRoleCommand>> hostRoleCommandEntry : hostRoleCommands.entrySet()) {
      for (Map.Entry<String, HostRoleCommand> roleCommandEntry : hostRoleCommandEntry.getValue().entrySet()) {
        commands.add(roleCommandEntry.getValue());
      }
    }
    return commands;
  }

  /**
   * Returns <Role, RoleCommand> pairs which are in progress.
   * @return
   */
  public Set<RoleCommandPair> getHostRolesInProgress() {
    Set<RoleCommandPair> commandsToScheduleSet = new HashSet<>();
    for (Map.Entry<String, Map<String, HostRoleCommand>> hostRoleCommandEntry : hostRoleCommands.entrySet()) {
      for (Map.Entry<String, HostRoleCommand> roleCommandEntry : hostRoleCommandEntry.getValue().entrySet()) {
        if (HostRoleStatus.IN_PROGRESS_STATUSES.contains(roleCommandEntry.getValue().getStatus())) {
          commandsToScheduleSet.add(
            new RoleCommandPair(roleCommandEntry.getValue().getRole(), roleCommandEntry.getValue().getRoleCommand()));
        }
      }
    }
    return commandsToScheduleSet;
  }

  public String getCommandParamsStage() {
    return commandParamsStage;
  }

  public void setCommandParamsStage(String commandParamsStage) {
    this.commandParamsStage = commandParamsStage;
  }

  public String getHostParamsStage() {
    return hostParamsStage;
  }

  public void setHostParamsStage(String hostParamsStage) {
    this.hostParamsStage = hostParamsStage;
  }

  public CommandExecutionType getCommandExecutionType() {
    return commandExecutionType;
  }

  public void setCommandExecutionType(CommandExecutionType commandExecutionType) {
    this.commandExecutionType = commandExecutionType;
  }

  /**
   * get current status of the stage
   * @return {@link HostRoleStatus}
   */
  public HostRoleStatus getStatus() {
    return status;
  }

  /**
   * sets status of the stage
   * @param status {@link HostRoleStatus}
   */
  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }


  public synchronized void setStageId(long stageId) {
    if (this.stageId != -1) {
      throw new RuntimeException("Attempt to set stageId again! Not allowed.");
    }
    //used on stage creation only, no need to check if wrappers loaded
    this.stageId = stageId;
    for (String host: commandsToSend.keySet()) {
      for (ExecutionCommandWrapper wrapper : commandsToSend.get(host)) {
        ExecutionCommand cmd = wrapper.getExecutionCommand();
        cmd.setRequestAndStage(requestId, stageId);
      }
    }
  }

  public synchronized long getStageId() {
    return stageId;
  }

  public String getActionId() {
    return StageUtils.getActionId(requestId, getStageId());
  }

  private synchronized ExecutionCommandWrapper addGenericExecutionCommand(String clusterName,
      String hostName, Role role, RoleCommand command, ServiceComponentHostEvent event,
      boolean retryAllowed, boolean autoSkipFailure) {

    boolean isHostRoleCommandAutoSkippable = autoSkipFailure && supportsAutoSkipOnFailure
        && skippable;

    // used on stage creation only, no need to check if wrappers loaded
    HostRoleCommand hrc = hostRoleCommandFactory.create(hostName, role, event, command,
        retryAllowed, isHostRoleCommandAutoSkippable);

    return addGenericExecutionCommand(clusterName, hostName, role, command, event, hrc);
  }

  private ExecutionCommandWrapper addGenericExecutionCommand(Cluster cluster, Host host, Role role,
      RoleCommand command, ServiceComponentHostEvent event, boolean retryAllowed,
      boolean autoSkipFailure) {

    boolean isHostRoleCommandAutoSkippable = autoSkipFailure && supportsAutoSkipOnFailure
        && skippable;

    HostRoleCommand hrc = hostRoleCommandFactory.create(host, role, event, command, retryAllowed,
        isHostRoleCommandAutoSkippable);

    return addGenericExecutionCommand(cluster.getClusterName(), host.getHostName(), role, command,
        event, hrc);
  }

  //TODO refactor method to use Host object (host_id support)
  private ExecutionCommandWrapper addGenericExecutionCommand(String clusterName, String hostName, Role role, RoleCommand command, ServiceComponentHostEvent event, HostRoleCommand hrc) {
    ExecutionCommand cmd = new ExecutionCommand();
    ExecutionCommandWrapper wrapper = ecwFactory.createFromCommand(cmd);
    hrc.setExecutionCommandWrapper(wrapper);
    cmd.setHostname(hostName);
    cmd.setClusterName(clusterName);
    cmd.setRequestAndStage(requestId, stageId);
    cmd.setRole(role.name());
    cmd.setRoleCommand(command);

    cmd.setServiceName("");

    Map<String, HostRoleCommand> hrcMap = hostRoleCommands.get(hostName);
    if (hrcMap == null) {
      hrcMap = new LinkedHashMap<>();
      hostRoleCommands.put(hostName, hrcMap);
    }
    if (hrcMap.get(role.toString()) != null) {
      throw new RuntimeException(
          "Setting the host role command second time for same stage: stage="
              + getActionId() + ", host=" + hostName + ", role=" + role);
    }
    hrcMap.put(role.toString(), hrc);
    List<ExecutionCommandWrapper> execCmdList = commandsToSend.get(hostName);
    if (execCmdList == null) {
      execCmdList = new ArrayList<>();
      commandsToSend.put(hostName, execCmdList);
    }

    if (execCmdList.contains(wrapper)) {
      //todo: proper exception
      throw new RuntimeException(
          "Setting the execution command second time for same stage: stage="
              + getActionId() + ", host=" + hostName + ", role=" + role+ ", event="+event);
    }
    execCmdList.add(wrapper);
    return wrapper;
  }

  /**
   * A new host role command is created for execution. Creates both
   * ExecutionCommand and HostRoleCommand objects and adds them to the Stage.
   * This should be called only once for a host-role for a given stage.
   */
  public synchronized void addHostRoleExecutionCommand(String host, Role role, RoleCommand command,
      ServiceComponentHostEvent event, String clusterName, String serviceName, boolean retryAllowed,
      boolean autoSkipFailure) {

    boolean isHostRoleCommandAutoSkippable = autoSkipFailure && supportsAutoSkipOnFailure
        && skippable;

    ExecutionCommandWrapper commandWrapper = addGenericExecutionCommand(clusterName, host, role,
        command, event, retryAllowed, isHostRoleCommandAutoSkippable);

    commandWrapper.getExecutionCommand().setServiceName(serviceName);
  }

  /**
   * A new host role command is created for execution. Creates both
   * ExecutionCommand and HostRoleCommand objects and adds them to the Stage.
   * This should be called only once for a host-role for a given stage.
   */
  public synchronized void addHostRoleExecutionCommand(Host host, Role role, RoleCommand command,
      ServiceComponentHostEvent event, Cluster cluster, String serviceName, boolean retryAllowed,
      boolean autoSkipFailure) {

    boolean isHostRoleCommandAutoSkippable = autoSkipFailure && supportsAutoSkipOnFailure
        && skippable;

    ExecutionCommandWrapper commandWrapper = addGenericExecutionCommand(cluster, host, role,
        command, event, retryAllowed, isHostRoleCommandAutoSkippable);

    commandWrapper.getExecutionCommand().setServiceName(serviceName);
  }

  /**
   * <p/>
   * Creates server-side execution command.
   * <p/>
   * The action name for this command is expected to be the classname of a
   * {@link org.apache.ambari.server.serveraction.ServerAction} implementation
   * which will be instantiated and invoked as needed.
   *
   * @param actionName
   *          a String declaring the action name (in the form of a classname) to
   *          execute
   * @param userName
   *          the name of the user who created this stage; may be null for
   *          anonymous user
   * @param role
   *          the Role for this command
   * @param command
   *          the RoleCommand for this command
   * @param clusterName
   *          a String identifying the cluster on which to to execute this
   *          command
   * @param event
   *          a ServiceComponentHostServerActionEvent
   * @param commandParams
   *          a Map of String to String data used to pass to the action - this
   *          may be empty or null if no data is relevant
   * @param commandDetail
   *          a String declaring a descriptive name to pass to the action - null
   *          or an empty string indicates no value is to be set
   * @param configTags
   *          a Map of configuration tags to set for this command - if null, no
   *          configurations will be available for the command
   * @param timeout
   *          an Integer declaring the timeout for this action - if null, a
   *          default
   * @param retryAllowed
   *          indicates whether retry after failure is allowed
   */
  public synchronized void addServerActionCommand(String actionName,
      @Nullable String userName,
      Role role, RoleCommand command, String clusterName,
      ServiceComponentHostServerActionEvent event, @Nullable Map<String, String> commandParams,
      @Nullable String commandDetail, @Nullable Map<String, Map<String, String>> configTags,
      @Nullable Integer timeout, boolean retryAllowed, boolean autoSkipFailure) {

    boolean isHostRoleCommandAutoSkippable = autoSkipFailure && supportsAutoSkipOnFailure
        && skippable;

    ExecutionCommandWrapper commandWrapper = addGenericExecutionCommand(clusterName,
        INTERNAL_HOSTNAME, role, command, event, retryAllowed, isHostRoleCommandAutoSkippable);

    ExecutionCommand cmd = commandWrapper.getExecutionCommand();

    Map<String, String> cmdParams = new HashMap<>();
    if (commandParams != null) {
      cmdParams.putAll(commandParams);
    }
    if (timeout != null) {
      cmdParams.put(ExecutionCommand.KeyNames.COMMAND_TIMEOUT, Long.toString(timeout));
    }
    cmd.setCommandParams(cmdParams);

    cmd.setConfigurations(new TreeMap<>());

    Map<String, String> roleParams = new HashMap<>();
    roleParams.put(ServerAction.ACTION_NAME, actionName);
    if (userName != null) {
      roleParams.put(ServerAction.ACTION_USER_NAME, userName);
    }
    cmd.setRoleParams(roleParams);

    if(commandDetail != null) {
      HostRoleCommand hostRoleCommand = getHostRoleCommand(INTERNAL_HOSTNAME, role.toString());
      if (hostRoleCommand != null) {
        hostRoleCommand.setCommandDetail(commandDetail);
        hostRoleCommand.setCustomCommandName(actionName);
      }
    }
  }

  /**
   *  Adds cancel command to stage for given cancelTargets collection of
   *  task id's that has to be canceled in Agent layer.
   */
  public synchronized void addCancelRequestCommand(List<Long> cancelTargets, String clusterName, String hostName) {
    ExecutionCommandWrapper commandWrapper = addGenericExecutionCommand(clusterName, hostName,
        Role.AMBARI_SERVER_ACTION, RoleCommand.ABORT, null, false, false);
    ExecutionCommand cmd = commandWrapper.getExecutionCommand();
    cmd.setCommandType(AgentCommandType.CANCEL_COMMAND);

    Assert.notEmpty(cancelTargets, "Provided targets task Id are empty.");

    Map<String, String> roleParams = new HashMap<>();

    roleParams.put("cancelTaskIdTargets", StringUtils.join(cancelTargets, ','));
    cmd.setRoleParams(roleParams);
  }

  /**
   *
   * @return list of hosts
   */
  public synchronized List<String> getHosts() { // TODO: Check whether method should be synchronized
    return new ArrayList<>(hostRoleCommands.keySet());
  }

  synchronized float getSuccessFactor(Role r) {
    Float f = successFactors.get(r);
    if (f == null) {
      if (r.equals(Role.DATANODE) || r.equals(Role.TASKTRACKER) || r.equals(Role.GANGLIA_MONITOR) ||
          r.equals(Role.HBASE_REGIONSERVER)) {
        return (float) 0.5;
      } else {
        return 1;
      }
    } else {
      return f;
    }
  }

  public synchronized void setSuccessFactors(Map<Role, Float> suc) {
    successFactors = suc;
  }

  public synchronized Map<Role, Float> getSuccessFactors() {
    return successFactors;
  }

  public long getRequestId() {
    return requestId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public long getClusterId() {
    return clusterId;
  }


  public String getRequestContext() {
    return requestContext;
  }

  /**
   * @param hostname  the hostname; {@code null} for a server-side stage
   * @param role      the role
   * @return the last attempt time
   */
  public long getLastAttemptTime(String hostname, String role) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(role).getLastAttemptTime();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @return the number of attempts
   */
  public short getAttemptCount(String hostname, String role) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(role).getAttemptCount();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   */
  public void incrementAttemptCount(String hostname, String role) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).incrementAttemptCount();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @param t           the last time the role was attempted
   */
  public void setLastAttemptTime(String hostname, String role, long t) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).setLastAttemptTime(t);
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @return            the wrapper
   */
  public ExecutionCommandWrapper getExecutionCommandWrapper(String hostname,
      String role) {
    HostRoleCommand hrc = hostRoleCommands.get(getSafeHost(hostname)).get(role);
    if (hrc != null) {
      return hrc.getExecutionCommandWrapper();
    } else {
      return null;
    }
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @return  the list of commands for the host
   */
  public List<ExecutionCommandWrapper> getExecutionCommands(String hostname) {
    checkWrappersLoaded();
    return commandsToSend.get(getSafeHost(hostname));
  }

/**
 * @param hostname    the hostname; {@code null} for a server-side stage
 * @param role        the role
 * @return the start time for the task
 */
  public long getStartTime(String hostname, String role) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(role).getStartTime();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @param startTime   the start time
   */
  public void setStartTime(String hostname, String role, long startTime) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).setStartTime(startTime);
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @return the status
   */
  public HostRoleStatus getHostRoleStatus(String hostname, String role) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(role).getStatus();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @param status      the status
   */
  public void setHostRoleStatus(String hostname, String role,
      HostRoleStatus status) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).setStatus(status);
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param roleStr     the role name
   * @return the wrapper event
   */
  public ServiceComponentHostEventWrapper getFsmEvent(String hostname, String roleStr) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(roleStr).getEvent();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @param exitCode    the exit code
   */
  public void setExitCode(String hostname, String role, int exitCode) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).setExitCode(exitCode);
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @return the exit code
   */
  public int getExitCode(String hostname, String role) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(role).getExitCode();
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @param stdErr      the standard error string
   */
  public void setStderr(String hostname, String role, String stdErr) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).setStderr(stdErr);
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @param stdOut      the standard output string
   */
  public void setStdout(String hostname, String role, String stdOut) {
    hostRoleCommands.get(getSafeHost(hostname)).get(role).setStdout(stdOut);
  }

  public synchronized boolean isStageInProgress() {
    for(String host: hostRoleCommands.keySet()) {
      for (String role : hostRoleCommands.get(host).keySet()) {
        HostRoleCommand hrc = hostRoleCommands.get(host).get(role);
        if (hrc == null) {
          return false;
        }
        if (hrc.getStatus().equals(HostRoleStatus.PENDING) ||
            hrc.getStatus().equals(HostRoleStatus.QUEUED) ||
            hrc.getStatus().equals(HostRoleStatus.IN_PROGRESS)) {
          return true;
        }
      }
    }
    return false;
  }

  public synchronized boolean doesStageHaveHostRoleStatus(
      Set<HostRoleStatus> statuses) {
    for(String host: hostRoleCommands.keySet()) {
      for (String role : hostRoleCommands.get(host).keySet()) {
        HostRoleCommand hrc = hostRoleCommands.get(host).get(role);
        if (hrc == null) {
          return false;
        }
        for (HostRoleStatus status : statuses) {
          if (hrc.getStatus().equals(status)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public Map<String, List<ExecutionCommandWrapper>> getExecutionCommands() {
    checkWrappersLoaded();
    return commandsToSend;
  }

  public String getLogDir() {
    return logDir;
  }

  public Map<String, Map<String, HostRoleCommand>> getHostRoleCommands() {
    return hostRoleCommands;
  }

  /**
   * Gets the {@link HostRoleCommand} matching the specified ID from this stage.
   * This will not hit the database, instead using the pre-cached list of HRCs
   * from the construction of the stage.
   *
   * @param taskId
   *          the ID to match
   * @return the {@link HostRoleCommand} or {@code null} if none match.
   */
  public HostRoleCommand getHostRoleCommand(long taskId) {
    for (Map.Entry<String, Map<String, HostRoleCommand>> hostEntry : hostRoleCommands.entrySet()) {
      Map<String, HostRoleCommand> hostCommands = hostEntry.getValue();
      for (Map.Entry<String, HostRoleCommand> hostCommand : hostCommands.entrySet()) {
        HostRoleCommand hostRoleCommand = hostCommand.getValue();
        if (null != hostRoleCommand && hostRoleCommand.getTaskId() == taskId) {
          return hostRoleCommand;
        }
      }
    }

    return null;
  }

  /**
   * This method should be used only in stage planner. To add
   * a new execution command use
   * {@link #addHostRoleExecutionCommand(String, Role, RoleCommand, ServiceComponentHostEvent, String, String, boolean, boolean)}
   * @param origStage the stage
   * @param hostname  the hostname; {@code null} for a server-side stage
   * @param r         the role
   */
  public synchronized void addExecutionCommandWrapper(Stage origStage,
      String hostname, Role r) {
    //used on stage creation only, no need to check if wrappers loaded

    hostname = getSafeHost(hostname);

    String role = r.toString();
    if (commandsToSend.get(hostname) == null) {
      commandsToSend.put(hostname, new ArrayList<>());
    }
    commandsToSend.get(hostname).add(
        origStage.getExecutionCommandWrapper(hostname, role));
    if (hostRoleCommands.get(hostname) == null) {
      hostRoleCommands.put(hostname, new LinkedHashMap<>());
    }
    // TODO add reference to ExecutionCommand into HostRoleCommand
    hostRoleCommands.get(hostname).put(role,
        origStage.getHostRoleCommand(hostname, role));
  }

  /**
   * @param hostname    the hostname; {@code null} for a server-side stage
   * @param role        the role
   * @return the role command
   */
  public HostRoleCommand getHostRoleCommand(String hostname, String role) {
    return hostRoleCommands.get(getSafeHost(hostname)).get(role);
  }

  /**
   * In this method we sum up all timeout values for all commands inside stage
   */
  public synchronized int getStageTimeout() {
    checkWrappersLoaded();
    if (stageTimeout == -1) {
      for (String host: commandsToSend.keySet()) {
        int summaryTaskTimeoutForHost = 0;
        for (ExecutionCommandWrapper command : commandsToSend.get(host)) {
          Map<String, String> commandParams =
                command.getExecutionCommand().getCommandParams();
          String timeoutKey = ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
          if (commandParams != null && commandParams.containsKey(timeoutKey)) {
            String timeoutStr = commandParams.get(timeoutKey);
            long commandTimeout =
                Long.parseLong(timeoutStr) * 1000; // Converting to milliseconds
            summaryTaskTimeoutForHost += commandTimeout;
          } else {
            LOG.error("Execution command has no timeout parameter" +
              command);
          }
        }
        if (summaryTaskTimeoutForHost > stageTimeout) {
          stageTimeout = summaryTaskTimeoutForHost;
        }
      }
    }
    return stageTimeout;
  }

  /**
   * Determine whether or not this stage is skippable.
   *
   * A skippable stage can be skipped on failure so that the
   * remaining stages of the request can execute.
   * If a stage is not skippable, a failure will cause the
   * remaining stages of the request to be aborted.
   *
   * @return true if this stage is skippable
   */
  public boolean isSkippable() {
    return skippable;
  }

  /**
   * Set skippable for this stage.
   *
   * A skippable stage can be skipped on failure so that the
   * remaining stages of the request can execute.
   * If a stage is not skippable, a failure will cause the
   * remaining stages of the request to be aborted.
   *
   * @param skippable  true if this stage should be skippable
   */
  public void setSkippable(boolean skippable) {
    this.skippable = skippable;
  }

  /**
   * Determine whether this stage supports automatically skipping failures of
   * its commands.
   *
   * @return {@code true} if this stage supports automatically skipping failures
   *         of its commands.
   */
  public boolean isAutoSkipOnFailureSupported() {
    return supportsAutoSkipOnFailure;
  }

  /**
   * Sets whether this stage supports automatically skipping failures of its
   * commands.
   *
   * @param supportsAutoSkipOnFailure
   *          {@code true} if this stage supports automatically skipping
   *          failures of its commands.
   */
  public void setAutoSkipFailureSupported(boolean supportsAutoSkipOnFailure) {
    this.supportsAutoSkipOnFailure = supportsAutoSkipOnFailure;
  }

  @Override //Object
  public synchronized String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("STAGE DESCRIPTION BEGIN\n");
    builder.append("requestId=").append(requestId).append("\n");
    builder.append("stageId=").append(stageId).append("\n");
    builder.append("clusterName=").append(clusterName).append("\n");
    builder.append("logDir=").append(logDir).append("\n");
    builder.append("requestContext=").append(requestContext).append("\n");
    builder.append("commandParamsStage=").append(commandParamsStage).append("\n");
    builder.append("hostParamsStage=").append(hostParamsStage).append("\n");
    builder.append("status=").append(status).append("\n");
    builder.append("displayStatus=").append(displayStatus).append("\n");
    builder.append("Success Factors:\n");
    for (Role r : successFactors.keySet()) {
      builder.append("  role: ").append(r).append(", factor: ").append(successFactors.get(r)).append("\n");
    }
    for (HostRoleCommand hostRoleCommand : getOrderedHostRoleCommands()) {
      builder.append("HOST: ").append(hostRoleCommand.getHostName()).append(" :\n");
      builder.append(hostRoleCommand.getExecutionCommandWrapper().getJson());
      builder.append("\n");
      builder.append(hostRoleCommand);
      builder.append("\n");
    }
    builder.append("STAGE DESCRIPTION END\n");
    return builder.toString();
  }

  /**
   * Helper to make sure the hostname is non-null for internal command map.
   * @param hostname  the hostname for the map key
   * @return the hostname when not {@code null}, otherwise {@link #INTERNAL_HOSTNAME}
   */
  private static String getSafeHost(String hostname) {
    return (null == hostname) ? INTERNAL_HOSTNAME : hostname;
  }

}
