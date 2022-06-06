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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Table(name = "requestschedule")
@Entity
@NamedQueries({
  @NamedQuery(name = "allReqSchedules", query =
    "SELECT reqSchedule FROM RequestScheduleEntity reqSchedule"),
  @NamedQuery(name = "reqScheduleByStatus", query =
    "SELECT reqSchedule FROM RequestScheduleEntity reqSchedule " +
      "WHERE reqSchedule.status=:status")
})
@TableGenerator(name = "schedule_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "requestschedule_id_seq"
  , initialValue = 1
)
public class RequestScheduleEntity {

  @Id
  @Column(name = "schedule_id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "schedule_id_generator")
  private long scheduleId;

  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  private Long clusterId;

  @Column(name = "description")
  private String description;

  @Column(name = "status")
  private String status;

  @Column(name = "batch_separation_seconds")
  private Integer batchSeparationInSeconds;

  @Column(name = "batch_toleration_limit")
  private Integer batchTolerationLimit;

  @Column(name = "batch_toleration_limit_per_batch")
  private Integer batchTolerationLimitPerBatch;

  @Column(name = "pause_after_first_batch")
  private Boolean pauseAfterFirstBatch;

  @Column(name = "authenticated_user_id")
  private Integer authenticatedUserId;

  @Column(name = "create_user")
  private String createUser;

  @Column(name = "create_timestamp")
  private Long createTimestamp;

  @Column(name = "update_user")
  protected String updateUser;

  @Column(name = "update_timestamp")
  private Long updateTimestamp;

  @Column(name = "minutes")
  private String minutes;

  @Column(name = "hours")
  private String hours;

  @Column(name = "days_of_month")
  private String daysOfMonth;

  @Column(name = "month")
  private String month;

  @Column(name = "day_of_week")
  private String dayOfWeek;

  @Column(name = "yearToSchedule")
  private String year;

  @Column(name = "starttime")
  private String startTime;

  @Column(name = "endtime")
  private String endTime;

  @Column(name = "last_execution_status")
  private String lastExecutionStatus;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(mappedBy = "requestScheduleEntity", cascade = CascadeType.ALL)
  private Collection<RequestScheduleBatchRequestEntity>
    requestScheduleBatchRequestEntities;

  @OneToMany(mappedBy = "requestScheduleEntity")
  private List<RequestEntity> requestEntities;

  public long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getBatchSeparationInSeconds() {
    return batchSeparationInSeconds;
  }

  public void setBatchSeparationInSeconds(Integer batchSeparationInSeconds) {
    this.batchSeparationInSeconds = batchSeparationInSeconds;
  }

  public Integer getBatchTolerationLimit() {
    return batchTolerationLimit;
  }

  public void setBatchTolerationLimit(Integer batchTolerationLimit) {
    this.batchTolerationLimit = batchTolerationLimit;
  }

  public String getCreateUser() {
    return createUser;
  }

  public void setCreateUser(String createUser) {
    this.createUser = createUser;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long createTimestamp) {
    this.createTimestamp = createTimestamp;
  }

  public String getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(String updateUser) {
    this.updateUser = updateUser;
  }

  public Long getUpdateTimestamp() {
    return updateTimestamp;
  }

  public void setUpdateTimestamp(Long updateTimestamp) {
    this.updateTimestamp = updateTimestamp;
  }

  public String getMinutes() {
    return minutes;
  }

  public void setMinutes(String minutes) {
    this.minutes = minutes;
  }

  public String getHours() {
    return hours;
  }

  public void setHours(String hours) {
    this.hours = hours;
  }

  public String getDaysOfMonth() {
    return daysOfMonth;
  }

  public void setDaysOfMonth(String daysOfMonth) {
    this.daysOfMonth = daysOfMonth;
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public String getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(String dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getLastExecutionStatus() {
    return lastExecutionStatus;
  }

  public void setLastExecutionStatus(String lastExecutionStatus) {
    this.lastExecutionStatus = lastExecutionStatus;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public Collection<RequestScheduleBatchRequestEntity> getRequestScheduleBatchRequestEntities() {
    return requestScheduleBatchRequestEntities;
  }

  public void setRequestScheduleBatchRequestEntities(
    Collection<RequestScheduleBatchRequestEntity> requestScheduleBatchRequestEntities) {
    this.requestScheduleBatchRequestEntities = requestScheduleBatchRequestEntities;
  }

  public List<RequestEntity> getRequestEntities() {
    return requestEntities;
  }

  public void setRequestEntities(List<RequestEntity> requestEntities) {
    this.requestEntities = requestEntities;
  }

  public Integer getAuthenticatedUserId() {
    return authenticatedUserId;
  }

  public void setAuthenticatedUserId(Integer authenticatedUser) {
    this.authenticatedUserId = authenticatedUser;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestScheduleEntity that = (RequestScheduleEntity) o;

    if (scheduleId != that.scheduleId) return false;
    if (!clusterId.equals(that.clusterId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (scheduleId ^ (scheduleId >>> 32));
    result = 31 * result + clusterId.hashCode();
    return result;
  }

  public Integer getBatchTolerationLimitPerBatch() {
    return batchTolerationLimitPerBatch;
  }

  public void setBatchTolerationLimitPerBatch(Integer batchTolerationLimitPerBatch) {
    this.batchTolerationLimitPerBatch = batchTolerationLimitPerBatch;
  }

  public Boolean isPauseAfterFirstBatch() {
    return pauseAfterFirstBatch;
  }

  public void setPauseAfterFirstBatch(Boolean pauseAfterFirstBatch) {
    this.pauseAfterFirstBatch = pauseAfterFirstBatch;
  }
}
