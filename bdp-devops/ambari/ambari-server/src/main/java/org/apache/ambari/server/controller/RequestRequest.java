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

import org.apache.ambari.server.actionmanager.HostRoleStatus;

import io.swagger.annotations.ApiModelProperty;

/**
 * Encapsulates all data about update request that came to RequestResourceProvider
 */
public class RequestRequest {

  public RequestRequest(String clusterName, long requestId) {
    this.clusterName = clusterName;
    this.requestId = requestId;
  }

  private String clusterName;

  private long requestId;

  private HostRoleStatus status;

  private String abortReason;

  private boolean removePendingHostRequests = false;


  @ApiModelProperty(name = "request_status", notes = "Only valid value is ABORTED.")
  public HostRoleStatus getStatus() {
    return status;
  }

  @ApiModelProperty(name = "cluster_name")
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @ApiModelProperty(name = "id")
  public long getRequestId() {
    return requestId;
  }

  public void setRequestId(int requestId) {
    this.requestId = requestId;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  @ApiModelProperty(name = "abort_reason")
  public String getAbortReason() {
    return abortReason;
  }

  public void setAbortReason(String abortReason) {
    this.abortReason = abortReason;
  }

  public boolean isRemovePendingHostRequests() {
    return removePendingHostRequests;
  }

  public void setRemovePendingHostRequests(boolean removePendingHostRequests) {
    this.removePendingHostRequests = removePendingHostRequests;
  }

  @Override
  public String toString() {
    return "RequestRequest{" +
            "clusterName='" + clusterName + '\'' +
            ", requestId=" + requestId +
            ", status=" + status +
            ", abortReason='" + abortReason + '\'' +
            ", removePendingHostRequests='" + removePendingHostRequests + '\'' +
            '}';
  }
}
