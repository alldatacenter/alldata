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

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.RequestType;

@Table(name = "request")
@Entity
@NamedQueries({
  @NamedQuery(name = "RequestEntity.findRequestStageIdsInClusterBeforeDate", query = "SELECT NEW org.apache.ambari.server.orm.dao.RequestDAO.StageEntityPK(request.requestId, stage.stageId) FROM RequestEntity request JOIN StageEntity stage ON request.requestId = stage.requestId WHERE request.clusterId = :clusterId AND request.createTime <= :beforeDate"),
  @NamedQuery(name = "RequestEntity.removeByRequestIds", query = "DELETE FROM RequestEntity request WHERE request.requestId IN :requestIds")
})
public class RequestEntity {

  @Column(name = "request_id")
  @Id
  private Long requestId;

  @Column(name = "cluster_id", updatable = false, nullable = false)
  @Basic
  private Long clusterId;

  @Column(name = "request_schedule_id", updatable = false, insertable = false, nullable = true)
  @Basic
  private Long requestScheduleId;

  @Column(name = "request_context")
  @Basic
  private String requestContext;

  @Column(name = "command_name")
  @Basic
  private String commandName;

  /**
   * On large clusters, this value can be in the 10,000's of kilobytes. During
   * an upgrade, all stages are loaded in memory for every request, which can
   * lead to an OOM. As a result, lazy load this since it's barely ever
   * requested or used.
   */
  @Column(name = "cluster_host_info")
  @Basic(fetch = FetchType.LAZY)
  private byte[] clusterHostInfo;

  @Column(name = "inputs")
  @Lob
  private byte[] inputs = new byte[0];

  @Column(name = "request_type")
  @Enumerated(value = EnumType.STRING)
  private RequestType requestType;

  /**
   * This is the logical status of the request and
   * represents if the intent of the request has been accomplished or not
   *
   *  Status calculated by calculating {@link StageEntity#status} of all belonging stages
   *
   */
  @Column(name = "status", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private HostRoleStatus status = HostRoleStatus.PENDING;

  /**
   * This status informs if any of the underlying tasks
   * have faced any type of failures {@link HostRoleStatus#isFailedState()}
   *
   * Status calculated by only taking into account
   * all belonging {@link HostRoleCommandEntity#status} (or {@link StageEntity#status})
   *
   */
  @Column(name = "display_status", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private HostRoleStatus displayStatus = HostRoleStatus.PENDING;

  @Basic
  @Column(name = "create_time", nullable = false)
  private Long createTime = System.currentTimeMillis();

  @Basic
  @Column(name = "start_time", nullable = false)
  private Long startTime = -1L;

  @Basic
  @Column(name = "end_time", nullable = false)
  private Long endTime = -1L;

  @Basic
  @Column(name = "exclusive_execution", insertable = true, updatable = true, nullable = false)
  private Integer exclusive = 0;

  @Column(name = "user_name")
  private String userName;

  @OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
  private Collection<StageEntity> stages;

  @OneToMany(mappedBy = "requestEntity", cascade = CascadeType.ALL)
  private Collection<RequestResourceFilterEntity> resourceFilterEntities;

  @OneToOne(mappedBy = "requestEntity", cascade = {CascadeType.ALL})
  private RequestOperationLevelEntity requestOperationLevel;

  @ManyToOne(cascade = {CascadeType.MERGE})
  @JoinColumn(name = "request_schedule_id", referencedColumnName = "schedule_id")
  private RequestScheduleEntity requestScheduleEntity;

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long id) {
    this.requestId = id;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String request_context) {
    this.requestContext = request_context;
  }

  public Collection<StageEntity> getStages() {
    return stages;
  }

  public void setStages(Collection<StageEntity> stages) {
    this.stages = stages;
  }

  public String getClusterHostInfo() {
    return clusterHostInfo == null ? "{}" : new String(clusterHostInfo);
  }

  public void setClusterHostInfo(String clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo.getBytes();
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Boolean isExclusive() {
    return exclusive == 0 ? false : true;
  }

  public void setExclusive(Boolean exclusive) {
    this.exclusive = (exclusive == false ? 0 : 1);
  }

  public String getInputs() {
    return inputs != null ? new String(inputs) : null;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs != null ? inputs.getBytes() : null;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public Collection<RequestResourceFilterEntity> getResourceFilterEntities() {
    return resourceFilterEntities;
  }

  public void setResourceFilterEntities(Collection<RequestResourceFilterEntity> resourceFilterEntities) {
    this.resourceFilterEntities = resourceFilterEntities;
  }

  public RequestOperationLevelEntity getRequestOperationLevel() {
    return requestOperationLevel;
  }

  public void setRequestOperationLevel(RequestOperationLevelEntity operationLevel) {
    this.requestOperationLevel = operationLevel;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;

  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   *  get status for the request
   * @return {@link HostRoleStatus}
   */
  public HostRoleStatus getStatus() {
    return status;
  }

  /**
   * sets status for the request
   * @param status {@link HostRoleStatus}
   */
  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  /**
   * get display status for the request
   * @return  {@link HostRoleStatus}
   */
  public HostRoleStatus getDisplayStatus() {
    return displayStatus;
  }

  /**
   * sets display status for the request
   * @param displayStatus {@link HostRoleStatus}
   */
  public void setDisplayStatus(HostRoleStatus displayStatus) {
    this.displayStatus = displayStatus;
  }

  public RequestScheduleEntity getRequestScheduleEntity() {
    return requestScheduleEntity;
  }

  public void setRequestScheduleEntity(RequestScheduleEntity requestScheduleEntity) {
    this.requestScheduleEntity = requestScheduleEntity;
  }

  public Long getRequestScheduleId() {
    return requestScheduleId;
  }

  public void setRequestScheduleId(Long scheduleId) {
    this.requestScheduleId = scheduleId;
  }

  /**
   * Returns the user name associated with the request.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets the user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestEntity that = (RequestEntity) o;

    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return requestId != null ? requestId.hashCode() : 0;
  }
}
