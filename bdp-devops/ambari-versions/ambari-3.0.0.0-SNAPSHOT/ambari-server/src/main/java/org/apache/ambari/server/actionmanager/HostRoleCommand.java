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

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentHostEvent;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class encapsulates the information for an task on a host for a
 * particular role which action manager needs. It doesn't capture actual command
 * and parameters, but just the stuff enough for action manager to track the
 * request.
 * <p/>
 * Since this class is cached by the {@link ActionDBAccessor}, it should not
 * hold references to JPA entities. It's possible that by holding onto JPA
 * entities, they will inadvertently hold onto the entire cache of entities in
 * the L1 cache.
 */
public class HostRoleCommand {
  private final Role role;
  private final ServiceComponentHostEventWrapper event;
  private long taskId = -1;
  private long stageId = -1;
  private long requestId = -1;
  private long hostId = -1;
  private String hostName;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private String stdout = "";
  private String stderr = "";
  public String outputLog = null;
  public String errorLog = null;
  private String structuredOut = "";
  private int exitCode = 999; //Default is unknown
  private long startTime = -1;
  private long originalStartTime = -1;
  private long endTime = -1;
  private long lastAttemptTime = -1;
  private short attemptCount = 0;
  private final boolean retryAllowed;
  private final boolean autoSkipFailure;
  private RoleCommand roleCommand;
  private String commandDetail;
  private String customCommandName;
  private ExecutionCommandWrapper executionCommandWrapper;
  private boolean isBackgroundCommand = false;
  private String opsDisplayName;

  @Inject
  private ExecutionCommandDAO executionCommandDAO;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private ExecutionCommandWrapperFactory ecwFactory;

  /**
   * Simple constructor, should be created using the Factory class.
   * @param hostName Host name
   * @param role Action to run
   * @param event Event on the host and component
   * @param command Type of command
   * @param hostDAO {@link org.apache.ambari.server.orm.dao.HostDAO} instance being injected
   */
  @AssistedInject
  public HostRoleCommand(String hostName, Role role,
      ServiceComponentHostEvent event, RoleCommand command, HostDAO hostDAO,
      ExecutionCommandDAO executionCommandDAO, ExecutionCommandWrapperFactory ecwFactory) {
    this(hostName, role, event, command, false, false, hostDAO, executionCommandDAO, ecwFactory);
  }

  /**
   * Simple constructor, should be created using the Factory class.
   * @param hostName Host name
   * @param role Action to run
   * @param event Event on the host and component
   * @param roleCommand Type of command
   * @param retryAllowed Whether the command can be repeated
   * @param hostDAO {@link org.apache.ambari.server.orm.dao.HostDAO} instance being injected
   */
  @AssistedInject
  public HostRoleCommand(String hostName, Role role, ServiceComponentHostEvent event,
      RoleCommand roleCommand, boolean retryAllowed, boolean autoSkipFailure, HostDAO hostDAO,
      ExecutionCommandDAO executionCommandDAO, ExecutionCommandWrapperFactory ecwFactory) {
    this.hostDAO = hostDAO;
    this.executionCommandDAO = executionCommandDAO;
    this.ecwFactory = ecwFactory;

    this.role = role;
    this.event = new ServiceComponentHostEventWrapper(event);
    this.roleCommand = roleCommand;
    this.retryAllowed = retryAllowed;
    this.autoSkipFailure = autoSkipFailure;
    this.hostName = hostName;

    HostEntity hostEntity = this.hostDAO.findByName(hostName);
    if (null != hostEntity) {
      hostId = hostEntity.getHostId();
    }
  }

  @AssistedInject
  public HostRoleCommand(Host host, Role role, ServiceComponentHostEvent event,
      RoleCommand roleCommand, boolean retryAllowed, boolean autoSkipFailure, HostDAO hostDAO,
      ExecutionCommandDAO executionCommandDAO, ExecutionCommandWrapperFactory ecwFactory) {
    this.hostDAO = hostDAO;
    this.executionCommandDAO = executionCommandDAO;
    this.ecwFactory = ecwFactory;

    this.role = role;
    this.event = new ServiceComponentHostEventWrapper(event);
    this.roleCommand = roleCommand;
    this.retryAllowed = retryAllowed;
    this.autoSkipFailure = autoSkipFailure;
    hostId = host.getHostId();
    hostName = host.getHostName();
  }

  @AssistedInject
  public HostRoleCommand(@Assisted HostRoleCommandEntity hostRoleCommandEntity, HostDAO hostDAO,
      ExecutionCommandDAO executionCommandDAO, ExecutionCommandWrapperFactory ecwFactory) {
    this.hostDAO = hostDAO;
    this.executionCommandDAO = executionCommandDAO;
    this.ecwFactory = ecwFactory;

    taskId = hostRoleCommandEntity.getTaskId();

    stageId = null != hostRoleCommandEntity.getStageId() ? hostRoleCommandEntity.getStageId()
        : hostRoleCommandEntity.getStage().getStageId();

    requestId = null != hostRoleCommandEntity.getRequestId() ? hostRoleCommandEntity.getRequestId()
        : hostRoleCommandEntity.getStage().getRequestId();

    if (null != hostRoleCommandEntity.getHostEntity()) {
      hostId = hostRoleCommandEntity.getHostId();
    }

    hostName = hostRoleCommandEntity.getHostName();
    role = hostRoleCommandEntity.getRole();
    status = hostRoleCommandEntity.getStatus();
    stdout = hostRoleCommandEntity.getStdOut() != null ? new String(hostRoleCommandEntity.getStdOut()) : "";
    stderr = hostRoleCommandEntity.getStdError() != null ? new String(hostRoleCommandEntity.getStdError()) : "";
    outputLog = hostRoleCommandEntity.getOutputLog();
    errorLog = hostRoleCommandEntity.getErrorLog();
    structuredOut = hostRoleCommandEntity.getStructuredOut() != null ? new String(hostRoleCommandEntity.getStructuredOut()) : "";
    exitCode = hostRoleCommandEntity.getExitcode();
    startTime = hostRoleCommandEntity.getStartTime() != null ? hostRoleCommandEntity.getStartTime() : -1L;
    originalStartTime = hostRoleCommandEntity.getOriginalStartTime() != null ? hostRoleCommandEntity.getOriginalStartTime() : -1L;
    endTime = hostRoleCommandEntity.getEndTime() != null ? hostRoleCommandEntity.getEndTime() : -1L;
    lastAttemptTime = hostRoleCommandEntity.getLastAttemptTime() != null ? hostRoleCommandEntity.getLastAttemptTime() : -1L;
    attemptCount = hostRoleCommandEntity.getAttemptCount();
    retryAllowed = hostRoleCommandEntity.isRetryAllowed();
    autoSkipFailure = hostRoleCommandEntity.isFailureAutoSkipped();
    roleCommand = hostRoleCommandEntity.getRoleCommand();
    event = new ServiceComponentHostEventWrapper(hostRoleCommandEntity.getEvent());
    commandDetail = hostRoleCommandEntity.getCommandDetail();
    opsDisplayName = hostRoleCommandEntity.getOpsDisplayName();
    customCommandName = hostRoleCommandEntity.getCustomCommandName();
    isBackgroundCommand = hostRoleCommandEntity.isBackgroundCommand();
  }

  //todo: why is this not symmetrical with the constructor which takes an entity
  //todo: why are we only setting some fields in this constructor, 8 fields missing?????
  public HostRoleCommandEntity constructNewPersistenceEntity() {
    HostRoleCommandEntity hostRoleCommandEntity = new HostRoleCommandEntity();
    hostRoleCommandEntity.setRole(role);
    hostRoleCommandEntity.setStatus(status);
    hostRoleCommandEntity.setStdError(stderr.getBytes());
    hostRoleCommandEntity.setExitcode(exitCode);
    hostRoleCommandEntity.setStdOut(stdout.getBytes());
    hostRoleCommandEntity.setStructuredOut(structuredOut.getBytes());
    hostRoleCommandEntity.setStartTime(startTime);
    hostRoleCommandEntity.setOriginalStartTime(originalStartTime);
    hostRoleCommandEntity.setEndTime(endTime);
    hostRoleCommandEntity.setLastAttemptTime(lastAttemptTime);
    hostRoleCommandEntity.setAttemptCount(attemptCount);
    hostRoleCommandEntity.setRetryAllowed(retryAllowed);
    hostRoleCommandEntity.setAutoSkipOnFailure(autoSkipFailure);
    hostRoleCommandEntity.setRoleCommand(roleCommand);
    hostRoleCommandEntity.setCommandDetail(commandDetail);
    hostRoleCommandEntity.setOpsDisplayName(opsDisplayName);
    hostRoleCommandEntity.setCustomCommandName(customCommandName);
    hostRoleCommandEntity.setBackgroundCommand(isBackgroundCommand);

    HostEntity hostEntity = hostDAO.findById(hostId);
    if (null != hostEntity) {
      hostRoleCommandEntity.setHostEntity(hostEntity);
    }

    hostRoleCommandEntity.setEvent(event.getEventJson());

    // set IDs if the wrapping object has them - they are most likely
    // non-updatable in JPA since they are retrieved from a relationship,
    // however the JPA cache may choose to not refresh the entity so they would
    // end up being null if not set before persisting the command entity
    if (requestId >= 0) {
      hostRoleCommandEntity.setRequestId(requestId);
    }

    if (stageId >= 0) {
      hostRoleCommandEntity.setStageId(stageId);
    }

    if (taskId >= 0) {
      hostRoleCommandEntity.setTaskId(taskId);
    }

    return hostRoleCommandEntity;
  }

  ExecutionCommandEntity constructExecutionCommandEntity() {
    ExecutionCommandEntity executionCommandEntity = new ExecutionCommandEntity();
    executionCommandEntity.setCommand(executionCommandWrapper.getJson().getBytes());
    return executionCommandEntity;
  }

  public long getTaskId() {
    return taskId;
  }

  public void setRequestId(long requestId) {
    this.requestId = requestId;
  }

  public void setStageId(long stageId) {
    this.stageId = stageId;
  }

  public void setTaskId(long taskId) {
    if (this.taskId != -1) {
      throw new RuntimeException("Attempt to set taskId again, not allowed");
    }
    this.taskId = taskId;
    //todo: do we need to have a wrapper?  This invariant isn't enforced in constructor.
    //todo: for now, I am just going to wrap this in a null check
    if (executionCommandWrapper != null) {
      executionCommandWrapper.getExecutionCommand().setTaskId(taskId);
      //Need to invalidate json because taskId is updated.
      executionCommandWrapper.invalidateJson();
    }
  }

  /**
   * Sets the host ID and name for the host associated with this command.
   *
   * @param hostId
   * @param hostName
   */
  public void setHost(long hostId, String hostName) {
    this.hostId = hostId;
    this.hostName = hostName;
  }

  public String getHostName() {
    return hostName;
  }

  public long getHostId() {
    return hostId;
  }

  public Role getRole() {
    return role;
  }

  public String getCommandDetail() {
    return commandDetail;
  }

  public void setCommandDetail(String commandDetail) {
    this.commandDetail = commandDetail;
  }

  public String getOpsDisplayName() {
    return opsDisplayName;
  }

  public void setOpsDisplayName(String opsDisplayName) {
    this.opsDisplayName = opsDisplayName;
  }
  public String getCustomCommandName() {
    return customCommandName;
  }

  public void setCustomCommandName(String customCommandName) {
    this.customCommandName = customCommandName;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public ServiceComponentHostEventWrapper getEvent() {
    return event;
  }

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  public String getOutputLog() { return outputLog; }

  public void setOutputLog(String outputLog)  {
    this.outputLog = outputLog;
  }

  public String getErrorLog() { return errorLog; }

  public void setErrorLog(String errorLog) {
      this.errorLog = errorLog;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getOriginalStartTime() {
    return originalStartTime;
  }

  public void setOriginalStartTime(long originalStartTime) {
    this.originalStartTime = originalStartTime;
  }

  public long getLastAttemptTime() {
    return lastAttemptTime;
  }

  public void setLastAttemptTime(long lastAttemptTime) {
    this.lastAttemptTime = lastAttemptTime;
  }

  public short getAttemptCount() {
    return attemptCount;
  }

  public void incrementAttemptCount() {
    attemptCount++;
  }

  public boolean isRetryAllowed() {
    return retryAllowed;
  }

  public String getStructuredOut() {
    return structuredOut;
  }

  public void setStructuredOut(String structuredOut) {
    this.structuredOut = structuredOut;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public ExecutionCommandWrapper getExecutionCommandWrapper() {
    if (taskId != -1 && executionCommandWrapper == null) {
      ExecutionCommandEntity commandEntity = executionCommandDAO.findByPK(taskId);
      if (commandEntity == null) {
        throw new RuntimeException("Invalid DB state, broken one-to-one relation for taskId=" + taskId);
      }

      executionCommandWrapper = ecwFactory.createFromJson(new String(commandEntity.getCommand()));
    }

    return executionCommandWrapper;
  }

  public void setExecutionCommandWrapper(ExecutionCommandWrapper executionCommandWrapper) {
    this.executionCommandWrapper = executionCommandWrapper;
  }

  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  public void setRoleCommand(RoleCommand roleCommand) {
    this.roleCommand = roleCommand;
  }

  public long getStageId() {
    return stageId;
  }

  public long getRequestId() {
    return requestId;
  }

  /**
   * Gets whether this command runs in the background and does not block other
   * commands.
   *
   * @return {@code true} if this command runs in the background, {@code false}
   *         otherise.
   */
  public boolean isBackgroundCommand() {
    return isBackgroundCommand;
  }

  /**
   * Sets whether this command runs in the background and does not block other
   * commands.
   *
   * @param isBackgroundCommand
   *          {@code true} if this command runs in the background, {@code false}
   *          otherise.
   */
  public void setBackgroundCommand(boolean isBackgroundCommand) {
    this.isBackgroundCommand = isBackgroundCommand;
  }

  /**
   * Gets whether commands which fail and are retryable are automatically
   * skipped and marked with {@link HostRoleStatus#SKIPPED_FAILED}.
   *
   * @return
   */
  public boolean isFailureAutoSkipped() {
    return autoSkipFailure;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(taskId).hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof HostRoleCommand)) {
      return false;
    }
    HostRoleCommand o = (HostRoleCommand) other;

    return hashCode() == o.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("HostRoleCommand State:\n");
    builder.append("  TaskId: ").append(taskId).append("\n");
    builder.append("  Role: ").append(role).append("\n");
    builder.append("  Status: ").append(status).append("\n");
    builder.append("  Event: ").append(event).append("\n");
    builder.append("  RetryAllowed: ").append(retryAllowed).append("\n");
    builder.append("  AutoSkipFailure: ").append(autoSkipFailure).append("\n");
    builder.append("  Output log: ").append(outputLog).append("\n");
    builder.append("  Error log: ").append(errorLog).append("\n");
    builder.append("  stdout: ").append(stdout).append("\n");
    builder.append("  stderr: ").append(stderr).append("\n");
    builder.append("  exitcode: ").append(exitCode).append("\n");
    builder.append("  Start time: ").append(startTime).append("\n");
    builder.append("  Original Start time: ").append(originalStartTime).append("\n");
    builder.append("  Last attempt time: ").append(lastAttemptTime).append("\n");
    builder.append("  attempt count: ").append(attemptCount).append("\n");
    return builder.toString();
  }
}
