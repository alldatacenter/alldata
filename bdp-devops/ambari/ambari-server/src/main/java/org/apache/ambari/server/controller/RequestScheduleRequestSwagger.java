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

import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.internal.RequestScheduleResourceProvider;

import io.swagger.annotations.ApiModelProperty;


public interface RequestScheduleRequestSwagger {

    @ApiModelProperty(name = RequestScheduleResourceProvider.REQUEST_SCHEDULE)
    RequestScheduleRequestElement getRequestScheduleRequest();

    interface RequestScheduleRequestElement {

        @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_PROPERTY_ID)
        List<BatchRequest> getBatch();

        @ApiModelProperty(name = RequestScheduleResourceProvider.SCHEDULE_PROPERTY_ID)
        ScheduleRequest getSchedule();

        interface BatchRequest {

            @ApiModelProperty(name = RequestScheduleResourceProvider.REQUESTS_PROPERTY_ID)
            List<BatchRequestRequest> getBatchRequests();

            @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_SETTINGS)
            BatchSettings getBatchSettings();

            interface BatchRequestRequest {

                @ApiModelProperty(name = RequestScheduleResourceProvider.ORDER_ID_PROPERTY_ID)
                Long getOrderId();

                @ApiModelProperty(name = RequestScheduleResourceProvider.TYPE_PROPERTY_ID)
                String getType();

                @ApiModelProperty(name = RequestScheduleResourceProvider.URI_PROPERTY_ID)
                String getUri();

                @ApiModelProperty(name = RequestBodyParser.REQUEST_BLOB_TITLE)
                RequestBodyInfo getRequestBodyInfo();

                interface RequestBodyInfo {

                    @ApiModelProperty(name = RequestResourceProvider.REQUEST_INFO)
                    RequestPostRequest.RequestInfo getRequestInfo();

                    @ApiModelProperty(name = RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID)
                    List<RequestResourceFilter> getRequestResourceFilters();
                }
            }

            interface BatchSettings {

                @ApiModelProperty(name = RequestScheduleResourceProvider.BATCH_SEPARATION_IN_SECONDS_PROPERTY_ID)
                Integer getBatchSeparationInSeconds();

                @ApiModelProperty(name = RequestScheduleResourceProvider.TASK_FAILURE_TOLERANCE_LIMIT_PROPERTY_ID)
                Integer getTaskFailureToleranceLimit();
            }
        }

        interface ScheduleRequest {

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

            @ApiModelProperty(name = RequestScheduleResourceProvider.START_TIME_PROPERTY_ID)
            String getStartTime();

            @ApiModelProperty(name = RequestScheduleResourceProvider.END_TIME_PROPERTY_ID)
            String getEndTime();
        }
    }
}