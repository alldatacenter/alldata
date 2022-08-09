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


import java.util.List;

import org.apache.ambari.server.controller.internal.RequestResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link org.apache.ambari.server.api.services.RequestService#getRequest(String,
 *    javax.ws.rs.core.HttpHeaders, javax.ws.rs.core.UriInfo, String)}
 *
 * The interface is not actually implemented, it only carries swagger annotations.
 */
public interface RequestResponse extends ApiModel {

  @ApiModelProperty(name = RequestResourceProvider.REQUESTS)
  RequestStatusInfo getRequestStatusInfo();

  interface RequestStatusInfo {
    @ApiModelProperty(name = "id")
    String getRequestId();

    @ApiModelProperty(name = "request_status")
    String getStatus();

    @ApiModelProperty(name = "aborted_task_count")
    int getAbortedTaskCount();

    @ApiModelProperty(name = "cluster_name")
    String getClusterName();

    @ApiModelProperty(name = "completed_task_count")
    String getCompletedTaskCount();

    @ApiModelProperty(name = "create_time")
    long getCreateTime();

    @ApiModelProperty(name = "start_time")
    String getStartTime();

    @ApiModelProperty(name = "end_time")
    String getEndTime();

    @ApiModelProperty(name = "exclusive")
    boolean isExclusive();

    @ApiModelProperty(name = "failed_task_count")
    int getFailedTaskCount();

    @ApiModelProperty(name = "inputs")
    String getInputs();

    @ApiModelProperty(name = "operation_level")
    String getOperationLevel();

    @ApiModelProperty(name = "progress_percent")
    double getProgressPercent();

    @ApiModelProperty(name = "queued_task_count")
    int getQueuedTaskCount();

    @ApiModelProperty(name = "request_context")
    String getRequestContext();

    @ApiModelProperty(name = "request_schedule")
    String getRequestSchedule();

    @ApiModelProperty(name = "request_schedule_id")
    long getRequestScheduleId();

    @ApiModelProperty(name = "resource_filters")
    List<RequestPostRequest.RequestResourceFilter> getResourceFilters();

    @ApiModelProperty(name = "task_count")
    int getTaskCount();

    @ApiModelProperty(name = "type")
    String getType();
  }

}
