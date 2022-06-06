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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.Schedule;

public class RequestScheduleResponse {
  private Long id;
  private String clusterName;
  private String description;
  private String status;
  private String lastExecutionStatus;
  private Batch batch;
  private Schedule schedule;
  private String createUser;
  private String createTime;
  private String updateUser;
  private String updateTime;
  private Integer authenticatedUserId;

  public RequestScheduleResponse(Long id, String clusterName,
                                 String description, String status,
                                 String lastExecutionStatus,
                                 Batch batch, Schedule schedule,
                                 String createUser, String createTime,
                                 String updateUser, String updateTime,
                                 Integer authenticatedUserId) {
    this.id = id;
    this.clusterName = clusterName;
    this.description = description;
    this.status = status;
    this.lastExecutionStatus = lastExecutionStatus;
    this.batch = batch;
    this.schedule = schedule;
    this.createUser = createUser;
    this.createTime = createTime;
    this.updateUser = updateUser;
    this.updateTime = updateTime;
    this.authenticatedUserId = authenticatedUserId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
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

  public Batch getBatch() {
    return batch;
  }

  public void setBatch(Batch batch) {
    this.batch = batch;
  }

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  public String getCreateUser() {
    return createUser;
  }

  public void setCreateUser(String createUser) {
    this.createUser = createUser;
  }

  public String getCreateTime() {
    return createTime;
  }

  public void setCreateTime(String createTime) {
    this.createTime = createTime;
  }

  public String getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(String updateUser) {
    this.updateUser = updateUser;
  }

  public String getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(String updateTime) {
    this.updateTime = updateTime;
  }

  public String getLastExecutionStatus() {
    return lastExecutionStatus;
  }

  public void setLastExecutionStatus(String lastExecutionStatus) {
    this.lastExecutionStatus = lastExecutionStatus;
  }

  public Integer getAuthenticatedUserId() {
    return authenticatedUserId;
  }

  public void setAuthenticatedUserId(Integer authenticatedUserId) {
    this.authenticatedUserId = authenticatedUserId;
  }
}
