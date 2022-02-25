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

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.ambari.server.actionmanager.CommandExecutionType;
import org.apache.ambari.server.actionmanager.HostRoleStatus;

@Entity
@Table(name = "stage")
@IdClass(StageEntityPK.class)
@NamedQueries({
    @NamedQuery(
        name = "StageEntity.findFirstStageByStatus",
        query = "SELECT stage.requestId, MIN(stage.stageId) from StageEntity stage, HostRoleCommandEntity hrc WHERE hrc.status IN :statuses AND hrc.stageId = stage.stageId AND hrc.requestId = stage.requestId GROUP by stage.requestId ORDER BY stage.requestId"),
    @NamedQuery(
        name = "StageEntity.findByRequestIdAndCommandStatuses",
        query = "SELECT stage from StageEntity stage WHERE stage.status IN :statuses AND stage.requestId = :requestId ORDER BY stage.stageId"),
    @NamedQuery(
        name = "StageEntity.removeByRequestStageIds",
        query = "DELETE FROM StageEntity stage WHERE stage.stageId = :stageId AND stage.requestId = :requestId")
})
public class StageEntity {

  @Basic
  @Column(name = "cluster_id", updatable = false, nullable = false)
  private Long clusterId = Long.valueOf(-1L);

  @Id
  @Column(name = "request_id", insertable = false, updatable = false, nullable = false)
  private Long requestId;

  @Id
  @Column(name = "stage_id", insertable = true, updatable = false, nullable = false)
  private Long stageId = 0L;

  @Basic
  @Column(name = "skippable", nullable = false)
  private Integer skippable = Integer.valueOf(0);

  @Basic
  @Column(name = "supports_auto_skip_failure", nullable = false)
  private Integer supportsAutoSkipOnFailure = Integer.valueOf(0);

  @Basic
  @Column(name = "log_info")
  private String logInfo = "";

  @Basic
  @Column(name = "request_context")
  private String requestContext = "";

  @Basic
  @Enumerated(value = EnumType.STRING)
  @Column(name = "command_execution_type", nullable = false)
  private CommandExecutionType commandExecutionType = CommandExecutionType.STAGE;

  /**
   * On large clusters, this value can be in the 10,000's of kilobytes. During
   * an upgrade, all stages are loaded in memory for every request, which can
   * lead to an OOM. As a result, lazy load this since it's barely ever
   * requested or used.
   */
  @Column(name = "command_params")
  @Basic(fetch = FetchType.LAZY)
  private byte[] commandParamsStage;

  @Basic
  @Column(name = "host_params")
  private byte[] hostParamsStage;

  /**
   * This status informs if the advanced criteria for the stage success
   * as established at the time of stage creation has been accomplished or not
   *
   *  Status calculated by taking into account following
   *  a) {@link #roleSuccessCriterias}
   *  b) {@link #skippable}
   *  c) {@link HostRoleCommandEntity#autoSkipOnFailure}
   *  d) {@link HostRoleCommandEntity#status}
   *
   */
  @Column(name = "status",  nullable = false)
  @Enumerated(EnumType.STRING)
  private HostRoleStatus status = HostRoleStatus.PENDING;

  /**
   * This status informs if any of the underlying tasks
   * have faced any type of failures {@link HostRoleStatus#isFailedState()}
   *
   * Status calculated by only taking into account {@link HostRoleCommandEntity#status}
   *
   */
  @Column(name = "display_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private HostRoleStatus displayStatus = HostRoleStatus.PENDING;

  @ManyToOne
  @JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false)
  private RequestEntity request;


  @OneToMany(mappedBy = "stage", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private Collection<HostRoleCommandEntity> hostRoleCommands;

  @OneToMany(mappedBy = "stage", cascade = CascadeType.REMOVE)
  private Collection<RoleSuccessCriteriaEntity> roleSuccessCriterias;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
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

  public String getLogInfo() {
    return defaultString(logInfo);
  }

  public void setLogInfo(String logInfo) {
    this.logInfo = logInfo;
  }

  public String getRequestContext() {
    return defaultString(requestContext);
  }

  public String getCommandParamsStage() {
    return commandParamsStage == null ? new String() : new String(commandParamsStage);
  }

  public void setCommandParamsStage(String commandParamsStage) {
    this.commandParamsStage = commandParamsStage.getBytes();
  }

  public String getHostParamsStage() {
    return hostParamsStage == null ? new String() : new String(hostParamsStage);
  }

  public void setHostParamsStage(String hostParamsStage) {
    this.hostParamsStage = hostParamsStage.getBytes();
  }

  public void setRequestContext(String requestContext) {
    if (requestContext != null) {
      this.requestContext = requestContext;
    }
  }

  public CommandExecutionType getCommandExecutionType() {
    return commandExecutionType;
  }

  public void setCommandExecutionType(CommandExecutionType commandExecutionType) {
    this.commandExecutionType = commandExecutionType;
  }

  /**
   * get status for the stage
   * @return {@link HostRoleStatus}
   */
  public HostRoleStatus getStatus() {
    return status;
  }

  /**
   * sets status for the stage
   * @param status {@link HostRoleStatus}
   */
  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  /**
   * get display status for the stage
   * @return  {@link HostRoleStatus}
   */
  public HostRoleStatus getDisplayStatus() {
    return displayStatus;
  }


  /**
   * sets display status for the stage
   * @param displayStatus {@link HostRoleStatus}
   */
  public void setDisplayStatus(HostRoleStatus displayStatus) {
    this.displayStatus = displayStatus;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StageEntity that = (StageEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }

    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) {
      return false;
    }

    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) {
      return false;
    }

    return !(requestContext != null ? !requestContext.equals(that.requestContext) : that.requestContext != null);
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (requestContext != null ? requestContext.hashCode() : 0);
    return result;
  }

  public Collection<HostRoleCommandEntity> getHostRoleCommands() {
    return hostRoleCommands;
  }

  public void setHostRoleCommands(Collection<HostRoleCommandEntity> hostRoleCommands) {
    this.hostRoleCommands = hostRoleCommands;
  }

  public Collection<RoleSuccessCriteriaEntity> getRoleSuccessCriterias() {
    return roleSuccessCriterias;
  }

  public void setRoleSuccessCriterias(Collection<RoleSuccessCriteriaEntity> roleSuccessCriterias) {
    this.roleSuccessCriterias = roleSuccessCriterias;
  }

  public RequestEntity getRequest() {
    return request;
  }

  public void setRequest(RequestEntity request) {
    this.request = request;
  }

  /**
   * Determine whether this stage is skippable.  If the stage is skippable then in can be skipped on
   * error without failing the entire request.
   *
   * @return true if this stage is skippable
   */
  public boolean isSkippable() {
    return skippable != 0;
  }

  /**
   * Set skippable for this stage.  If the stage is skippable then in can be skipped on
   * error without failing the entire request.
   *
   * @param skippable true indicates that the stage is skippable
   */
  public void setSkippable(boolean skippable) {
    this.skippable = skippable ? 1 : 0;
  }

  /**
   * Determine whether this stage supports automatically skipping failures of
   * its commands.
   *
   * @return {@code true} if this stage supports automatically skipping failures
   *         of its commands.
   */
  public boolean isAutoSkipOnFailureSupported() {
    return supportsAutoSkipOnFailure != 0;
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
    this.supportsAutoSkipOnFailure = supportsAutoSkipOnFailure ? 1 : 0;
  }
}
