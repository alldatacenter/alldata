/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.pojo.workflow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Event log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Workflow event log query conditions")
public class EventLogView {

    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("Process id")
    private Integer processId;

    @ApiModelProperty("Process name ")
    private String processName;

    @ApiModelProperty("Process display name")
    private String processDisplayName;

    @ApiModelProperty("Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty("Task id")
    private Integer taskId;

    @ApiModelProperty("Element name")
    private String elementName;

    @ApiModelProperty("Element display name")
    private String elementDisplayName;

    @ApiModelProperty("Event type")
    private String eventType;

    @ApiModelProperty("Event name")
    private String event;

    @ApiModelProperty("Listener name")
    private String listener;

    @ApiModelProperty("Status")
    private Integer status;

    @ApiModelProperty("Is it synchronized")
    private Boolean async;

    @ApiModelProperty("Execute IP")
    private String ip;

    @ApiModelProperty("Start time ")
    private Date startTime;

    @ApiModelProperty("End time")
    private Date endTime;

    @ApiModelProperty("Remark")
    private String remark;

    @ApiModelProperty("Exception message")
    private String exception;

}
