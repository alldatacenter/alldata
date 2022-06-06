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

import org.apache.ambari.server.controller.internal.RequestScheduleResourceProvider;

import io.swagger.annotations.ApiModelProperty;


public interface RequestScheduleResponseSwagger {

    @ApiModelProperty(name = RequestScheduleResourceProvider.REQUEST_SCHEDULE)
    RequestScheduleResponseElement getRequestScheduleResponse();

    interface RequestScheduleResponseElement {

        @ApiModelProperty(name = RequestScheduleResourceProvider.ID_PROPERTY_ID)
        Long getId ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.CLUSTER_NAME_PROPERTY_ID)
        String getClusterName ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.DESCRIPTION_PROPERTY_ID)
        String getDescription ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.STATUS_PROPERTY_ID)
        String getStatus ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_PROPERTY_ID)
        BatchResponse getBatch ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.SCHEDULE_PROPERTY_ID)
        ScheduleResponse getSchedule ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.CREATE_USER_PROPERTY_ID)
        String getCreateUser ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.CREATE_TIME_PROPERTY_ID)
        String getCreateTime ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.UPDATE_USER_PROPERTY_ID)
        String getUpdateUser ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.UPDATE_TIME_PROPERTY_ID)
        String getUpdateTime ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.LAST_EXECUTION_STATUS_PROPERTY_ID)
        String getLastExecutionStatus ();

        @ApiModelProperty(name = RequestScheduleResourceProvider.AUTHENTICATED_USER_PROPERTY_ID)
        Integer getAuthenticatedUserId ();

        interface BatchResponse {

            @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_REQUESTS)
            List<BatchRequestResponse> getBatchRequests();

            @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_SETTINGS)
            BatchSettings getBatchSettings();

            interface BatchRequestResponse {

                @ApiModelProperty(name = RequestScheduleResourceProvider.ORDER_ID_PROPERTY_ID)
                Long getOrderId();

                @ApiModelProperty(name = RequestScheduleResourceProvider.REQUEST_TYPE_PROPERTY_ID)
                String getType();

                @ApiModelProperty(name = RequestScheduleResourceProvider.REQUEST_URI_PROPERTY_ID)
                String getUri();

                @ApiModelProperty(name = RequestScheduleResourceProvider.REQUEST_BODY_PROPERTY_ID)
                String getBody();

                @ApiModelProperty(name = RequestScheduleResourceProvider.REQUEST_STATUS_PROPERTY_ID)
                String getStatus();

                @ApiModelProperty(name = RequestScheduleResourceProvider.RETURN_CODE_PROPERTY_ID)
                Integer getReturnCode();

                @ApiModelProperty(name = RequestScheduleResourceProvider.RESPONSE_MESSAGE_PROPERTY_ID)
                String getResponseMsg();
            }

            interface BatchSettings {

                @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_SEPARATION_IN_SECONDS_PROPERTY_ID)
                Integer getBatchSeparationInSeconds();

                @ApiModelProperty(name = RequestScheduleResourceProvider.TASK_FAILURE_TOLERANCE_LIMIT_PROPERTY_ID)
                Integer getTaskFailureToleranceLimit();
            }
        }

        interface ScheduleResponse {

            @ApiModelProperty(name = RequestScheduleResourceProvider.MINUTES_PROPERTY_ID)
            String getMinutes();

            @ApiModelProperty(name = RequestScheduleResourceProvider.HOURS_PROPERTY_ID)
            String getHours();

            @ApiModelProperty(name = RequestScheduleResourceProvider.DAYS_OF_MONTH_PROPERTY_ID)
            String getDaysOfMonth();

            @ApiModelProperty(name = RequestScheduleResourceProvider.MONTH_PROPERTY_ID)
            String getMonth();

            @ApiModelProperty(name = RequestScheduleResourceProvider.DAY_OF_WEEK_PROPERTY_ID)
            String getDayOfWeek();

            @ApiModelProperty(name = RequestScheduleResourceProvider.YEAR_PROPERTY_ID)
            String getYear();

            @ApiModelProperty(name = RequestScheduleResourceProvider.START_TIME_SNAKE_CASE_PROPERTY_ID)
            String getStartTime();

            @ApiModelProperty(name = RequestScheduleResourceProvider.END_TIME_SNAKE_CASE_PROPERTY_ID)
            String getEndTime();
        }
    }
}