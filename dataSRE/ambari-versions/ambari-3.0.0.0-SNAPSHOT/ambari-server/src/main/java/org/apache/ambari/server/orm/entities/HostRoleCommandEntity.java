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

package org.apache.ambari.server.orm.entities;

import static org.apache.commons.lang.StringUtils.defaultString;

import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.commons.lang.ArrayUtils;

@Entity
@Table(name = "host_role_command"
       , indexes = {
           @Index(name = "idx_hrc_request_id", columnList = "request_id")
         , @Index(name = "idx_hrc_status_role", columnList = "status, role")
       })
@TableGenerator(name = "host_role_command_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "host_role_command_id_seq"
    , initialValue = 1
)
@NamedQueries({
    @NamedQuery(
        name = "HostRoleCommandEntity.findTaskIdsByRequestStageIds",
        query = "SELECT command.taskId FROM HostRoleCommandEntity command WHERE command.stageId = :stageId AND command.requestId = :requestId"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findCountByCommandStatuses",
        query = "SELECT COUNT(command.taskId) FROM HostRoleCommandEntity command WHERE command.status IN :statuses"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findByRequestIdAndStatuses",
        query = "SELECT task FROM HostRoleCommandEntity task WHERE task.requestId=:requestId AND task.status IN :statuses ORDER BY task.taskId ASC"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findTasksByStatusesOrderByIdDesc",
        query = "SELECT task FROM HostRoleCommandEntity task WHERE task.requestId = :requestId AND task.status IN :statuses ORDER BY task.taskId DESC"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findNumTasksAlreadyRanInStage",
        query = "SELECT COUNT(task.taskId) FROM HostRoleCommandEntity task WHERE task.requestId = :requestId AND task.taskId > :taskId AND task.stageId > :stageId AND task.status NOT IN :statuses"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findByCommandStatuses",
        query = "SELECT command FROM HostRoleCommandEntity command WHERE command.status IN :statuses ORDER BY command.requestId, command.stageId"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findByHostId",
        query = "SELECT command FROM HostRoleCommandEntity command WHERE command.hostId=:hostId"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findByHostRole",
        query = "SELECT command FROM HostRoleCommandEntity command WHERE command.hostEntity.hostName=:hostName AND command.requestId=:requestId AND command.stageId=:stageId AND command.role=:role ORDER BY command.taskId"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findByHostRoleNullHost",
        query = "SELECT command FROM HostRoleCommandEntity command WHERE command.hostEntity IS NULL AND command.requestId=:requestId AND command.stageId=:stageId AND command.role=:role"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findByStatusBetweenStages",
        query = "SELECT command FROM HostRoleCommandEntity command WHERE command.requestId = :requestId AND command.stageId >= :minStageId AND command.stageId <= :maxStageId AND command.status = :status"),
    @NamedQuery(
        name = "HostRoleCommandEntity.updateAutoSkipExcludeRoleCommand",
        query = "UPDATE HostRoleCommandEntity command SET command.autoSkipOnFailure = :autoSkipOnFailure WHERE command.requestId = :requestId AND command.roleCommand <> :roleCommand"),
    @NamedQuery(
        name = "HostRoleCommandEntity.updateAutoSkipForRoleCommand",
        query = "UPDATE HostRoleCommandEntity command SET command.autoSkipOnFailure = :autoSkipOnFailure WHERE command.requestId = :requestId AND command.roleCommand = :roleCommand"),
    @NamedQuery(
        name = "HostRoleCommandEntity.removeByTaskIds",
        query = "DELETE FROM HostRoleCommandEntity command WHERE command.taskId IN :taskIds"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findHostsByCommandStatus",
        query = "SELECT DISTINCT(host.hostName) FROM HostRoleCommandEntity command, HostEntity host WHERE (command.requestId >= :iLowestRequestIdInProgress AND command.requestId <= :iHighestRequestIdInProgress) AND command.status IN :statuses AND command.hostId = host.hostId AND host.hostName IS NOT NULL"),
    @NamedQuery(
        name = "HostRoleCommandEntity.getBlockingHostsForRequest",
        query = "SELECT DISTINCT(host.hostName) FROM HostRoleCommandEntity command, HostEntity host WHERE command.requestId >= :lowerRequestIdInclusive AND command.requestId < :upperRequestIdExclusive AND command.status IN :statuses AND command.isBackgroundCommand=0 AND command.hostId = host.hostId AND host.hostName IS NOT NULL"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findLatestServiceChecksByRole",
        query = "SELECT NEW org.apache.ambari.server.orm.dao.HostRoleCommandDAO.LastServiceCheckDTO(command.role, MAX(command.endTime)) FROM HostRoleCommandEntity command WHERE command.roleCommand = :roleCommand AND command.endTime > 0 AND command.stage.clusterId = :clusterId GROUP BY command.role ORDER BY command.role ASC"),
    @NamedQuery(
      name = "HostRoleCommandEntity.findByRequestId",
      query = "SELECT command FROM HostRoleCommandEntity command WHERE command.requestId = :requestId ORDER BY command.taskId")
})
public class HostRoleCommandEntity {

  private static int MAX_COMMAND_DETAIL_LENGTH = 250;

  @Column(name = "task_id")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "host_role_command_id_generator")
  private Long taskId;

  @Column(name = "request_id", insertable = false, updatable = false, nullable = false)
  @Basic
  private Long requestId;

  @Column(name = "stage_id", insertable = false, updatable = false, nullable = false)
  @Basic
  private Long stageId;

  @Column(name = "host_id", insertable = false, updatable = false, nullable = true)
  @Basic
  private Long hostId;

  @Column(name = "role")
  private String role;

  @Column(name = "event", length = 32000)
  @Basic
  @Lob
  private String event = "";

  @Column(name = "exitcode", nullable = false)
  @Basic
  private Integer exitcode = 0;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private HostRoleStatus status = HostRoleStatus.PENDING;

  @Column(name = "std_error")
  @Lob
  @Basic
  private byte[] stdError = new byte[0];

  @Column(name = "std_out")
  @Lob
  @Basic
  private byte[] stdOut = new byte[0];

  @Column(name = "output_log")
  @Basic
  private String outputLog = null;

  @Column(name = "error_log")
  @Basic
  private String errorLog = null;


  @Column(name = "structured_out")
  @Lob
  @Basic
  private byte[] structuredOut = new byte[0];

  @Basic
  @Column(name = "start_time", nullable = false)
  private Long startTime = -1L;

  /**
   * Because the startTime is allowed to be overwritten, introduced a new column for the original start time.
   */
  @Basic
  @Column(name = "original_start_time", nullable = false)
  private Long originalStartTime = -1L;

  @Basic
  @Column(name = "end_time", nullable = false)
  private Long endTime = -1L;

  @Basic
  @Column(name = "last_attempt_time", nullable = false)
  private Long lastAttemptTime = -1L;

  @Basic
  @Column(name = "attempt_count", nullable = false)
  private Short attemptCount = 0;

  @Column(name = "retry_allowed", nullable = false)
  private Integer retryAllowed = Integer.valueOf(0);

  /**
   * If the command fails and is skippable, then this will instruct the
   * scheduler to skip the command.
   */
  @Column(name = "auto_skip_on_failure", nullable = false)
  private Integer autoSkipOnFailure = Integer.valueOf(0);

  // This is really command type as well as name
  @Column(name = "role_command")
  @Enumerated(EnumType.STRING)
  private RoleCommand roleCommand;

  // A readable description of the command
  @Column(name = "command_detail")
  @Basic
  private String commandDetail;

  // An optional property that can be used for setting the displayName for operations window
  @Column(name = "ops_display_name")
  @Basic
  private String opsDisplayName;

  // When command type id CUSTOM_COMMAND and CUSTOM_ACTION this is the name
  @Column(name = "custom_command_name")
  @Basic
  private String customCommandName;

  @OneToOne(mappedBy = "hostRoleCommand", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private ExecutionCommandEntity executionCommand;

  @ManyToOne(cascade = {CascadeType.MERGE})
  @JoinColumns({@JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false), @JoinColumn(name = "stage_id", referencedColumnName = "stage_id", nullable = false)})
  private StageEntity stage;

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = true)
  private HostEntity hostEntity;

  @OneToOne(mappedBy = "hostRoleCommandEntity", cascade = CascadeType.REMOVE)
  private TopologyLogicalTaskEntity topologyLogicalTaskEntity;

  @Basic
  @Column(name = "is_background", nullable = false)
  private short isBackgroundCommand = 0;

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  public String getHostName() {
    return hostEntity != null ? hostEntity.getHostName() : null;
  }

  public Long getHostId() {
    return hostEntity != null ? hostEntity.getHostId() : null;
  }

  public Role getRole() {
    return Role.valueOf(role);
  }

  public void setRole(Role role) {
    this.role = role.name();
  }

  public String getEvent() {
    return defaultString(event);
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public Integer getExitcode() {
    return exitcode;
  }

  public void setExitcode(Integer exitcode) {
    this.exitcode = exitcode;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public byte[] getStdError() {
    return ArrayUtils.nullToEmpty(stdError);
  }

  public void setStdError(byte[] stdError) {
    this.stdError = stdError;
  }

  public byte[] getStdOut() {
    return ArrayUtils.nullToEmpty(stdOut);
  }

  public void setStdOut(byte[] stdOut) {
    this.stdOut = stdOut;
  }

  public String getOutputLog() { return outputLog; }

  public void setOutputLog(String outputLog) { this.outputLog = outputLog; }

  public String getErrorLog() { return errorLog; }

  public void setErrorLog(String errorLog) { this.errorLog = errorLog; }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  /**
   * Get the original time the command was first scheduled on the agent. This value is never overwritten.
   * @return Original start time
   */
  public Long getOriginalStartTime() {
    return originalStartTime;
  }

  /**
   * Set the original start time when the command is first scheduled. This value is never overwritten.
   * @param originalStartTime Original start time
   */
  public void setOriginalStartTime(Long originalStartTime) {
    this.originalStartTime = originalStartTime;
  }

  public Long getLastAttemptTime() {
    return lastAttemptTime;
  }

  public void setLastAttemptTime(Long lastAttemptTime) {
    this.lastAttemptTime = lastAttemptTime;
  }

  public Short getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Short attemptCount) {
    this.attemptCount = attemptCount;
  }

  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  public void setRoleCommand(RoleCommand roleCommand) {
    this.roleCommand = roleCommand;
  }

  public byte[] getStructuredOut() {
    return structuredOut;
  }

  public void setStructuredOut(byte[] structuredOut) {
    this.structuredOut = structuredOut;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public String getCommandDetail() {
    return commandDetail;
  }

  public void setCommandDetail(String commandDetail) {
    String truncatedCommandDetail = commandDetail;
    if (commandDetail != null) {
      if (commandDetail.length() > MAX_COMMAND_DETAIL_LENGTH) {
        truncatedCommandDetail = commandDetail.substring(0, MAX_COMMAND_DETAIL_LENGTH) + "...";
      }
    }
    this.commandDetail = truncatedCommandDetail;
  }

  public String getCustomCommandName() {
    return customCommandName;
  }

  public void setCustomCommandName(String customCommandName) {
    this.customCommandName = customCommandName;
  }

  public String getOpsDisplayName() {
    return opsDisplayName;
  }

  public void setOpsDisplayName(String opsDisplayName) {
    this.opsDisplayName = opsDisplayName;
  }

  

  /**
   * Determine whether this task should hold for retry when an error occurs.
   *
   * @return {@code true} if this task should hold for retry when an error occurs
   */
  public boolean isRetryAllowed() {
    return retryAllowed != 0;
  }

  /**
   * Sets whether this task should hold for retry when an error occurs.
   *
   * @param enabled  {@code true} if this task should hold for retry when an error occurs
   */
  public void setRetryAllowed(boolean enabled) {
    retryAllowed = enabled ? 1 : 0;
  }

  /**
   * Gets whether commands which fail and are retryable are automatically
   * skipped and marked with {@link HostRoleStatus#SKIPPED_FAILED}.
   *
   * @return
   */
  public boolean isFailureAutoSkipped() {
    return autoSkipOnFailure != 0;
  }

  /**
   * Sets whether commands which fail and are retryable are automatically
   * skipped and marked with {@link HostRoleStatus#SKIPPED_FAILED}.
   *
   * @param skipFailures
   */
  public void setAutoSkipOnFailure(boolean skipFailures) {
    autoSkipOnFailure = skipFailures ? 1 : 0;
  }

  /**
   * Sets whether this is a command is a background command and will not block
   * other commands.
   *
   * @param runInBackground
   *          {@code true} if this is a background command, {@code false}
   *          otherwise.
   */
  public void setBackgroundCommand(boolean runInBackground) {
    isBackgroundCommand = (short) (runInBackground ? 1 : 0);
  }

  /**
   * Gets whether this command runs in the background and will not block other
   * commands.
   */
  public boolean isBackgroundCommand() {
    return isBackgroundCommand == 0 ? false : true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HostRoleCommandEntity that = (HostRoleCommandEntity) o;

    if (attemptCount != null ? !attemptCount.equals(that.attemptCount) : that.attemptCount != null) {
      return false;
    }
    if (event != null ? !event.equals(that.event) : that.event != null) {
      return false;
    }
    if (exitcode != null ? !exitcode.equals(that.exitcode) : that.exitcode != null) {
      return false;
    }
    if (hostEntity != null ? !hostEntity.equals(that.hostEntity) : that.hostEntity != null) {
      return false;
    }
    if (lastAttemptTime != null ? !lastAttemptTime.equals(that.lastAttemptTime) : that.lastAttemptTime != null) {
      return false;
    }
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) {
      return false;
    }
    if (role != null ? !role.equals(that.role) : that.role != null) {
      return false;
    }
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) {
      return false;
    }
    if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) {
      return false;
    }
    if (originalStartTime != null ? !originalStartTime.equals(that.originalStartTime) : that.originalStartTime != null) {
      return false;
    }
    if (status != null ? !status.equals(that.status) : that.status != null) {
      return false;
    }
    if (stdError != null ? !Arrays.equals(stdError, that.stdError) : that.stdError != null) {
      return false;
    }
    if (stdOut != null ? !Arrays.equals(stdOut, that.stdOut) : that.stdOut != null) {
      return false;
    }
    if (outputLog != null ? !outputLog.equals(that.outputLog) : that.outputLog != null) {
      return false;
    }
    if (errorLog != null ? !errorLog.equals(that.errorLog) : that.errorLog != null) {
      return false;
    }
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) {
      return false;
    }
    if (structuredOut != null ? !Arrays.equals(structuredOut, that.structuredOut) : that.structuredOut != null) {
      return false;
    }
    if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (hostEntity != null ? hostEntity.hashCode() : 0);
    result = 31 * result + (role != null ? role.hashCode() : 0);
    result = 31 * result + (event != null ? event.hashCode() : 0);
    result = 31 * result + (exitcode != null ? exitcode.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (stdError != null ? Arrays.hashCode(stdError) : 0);
    result = 31 * result + (stdOut != null ? Arrays.hashCode(stdOut) : 0);
    result = 31 * result + (outputLog != null ? outputLog.hashCode() : 0);
    result = 31 * result + (errorLog != null ? errorLog.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (originalStartTime != null ? originalStartTime.hashCode() : 0);
    result = 31 * result + (lastAttemptTime != null ? lastAttemptTime.hashCode() : 0);
    result = 31 * result + (attemptCount != null ? attemptCount.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (structuredOut != null ? Arrays.hashCode(structuredOut) : 0);
    return result;
  }

  public ExecutionCommandEntity getExecutionCommand() {
    return executionCommand;
  }

  public void setExecutionCommand(ExecutionCommandEntity executionCommandsByTaskId) {
    executionCommand = executionCommandsByTaskId;
  }

  public StageEntity getStage() {
    return stage;
  }

  /**
   * Sets the associated {@link StageEntity} for this command. If the
   * {@link StageEntity} has been persisted, then this will also set the
   * commands stage and request ID fields.
   *
   * @param stage
   */
  public void setStage(StageEntity stage) {
    this.stage = stage;

    // ensure that the IDs are also set since they may not be retrieved from JPA
    // when this entity is cached
    if (null != stage) {
      if (null == stageId) {
        stageId = stage.getStageId();
      }

      if (null == requestId) {
        requestId = stage.getRequestId();
      }
    }
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public TopologyLogicalTaskEntity getTopologyLogicalTaskEntity() {
    return topologyLogicalTaskEntity;
  }

  public void setTopologyLogicalTaskEntity(TopologyLogicalTaskEntity topologyLogicalTaskEntity) {
    this.topologyLogicalTaskEntity = topologyLogicalTaskEntity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("HostRoleCommandEntity{ ");
    buffer.append("taskId").append(taskId);
    buffer.append(", stageId=").append(stageId);
    buffer.append(", requestId=").append(requestId);
    buffer.append(", role=").append(role);
    buffer.append(", roleCommand=").append(roleCommand);
    buffer.append(", exitcode=").append(exitcode);
    buffer.append("}");
    return buffer.toString();
  }
}
