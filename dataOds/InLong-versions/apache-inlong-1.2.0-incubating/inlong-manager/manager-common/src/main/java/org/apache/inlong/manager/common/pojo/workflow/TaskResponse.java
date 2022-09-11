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
import org.apache.inlong.manager.common.enums.TaskStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Task response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Task response")
public class TaskResponse {

    @ApiModelProperty(value = "Task ID")
    private Integer id;

    @ApiModelProperty(value = "Task type")
    private String type;

    @ApiModelProperty(value = "Process ID")
    private Integer processId;

    @ApiModelProperty(value = "Process name")
    private String processName;

    @ApiModelProperty(value = "Process display name")
    private String processDisplayName;

    @ApiModelProperty(value = "Task name")
    private String name;

    @ApiModelProperty(value = "Task display name")
    private String displayName;

    @ApiModelProperty(value = "Applicant name")
    private String applicant;

    @ApiModelProperty(value = "Approver list")
    private List<String> approvers;

    @ApiModelProperty(value = "Actual operation approver")
    private String operator;

    @ApiModelProperty(value = "Task status")
    private TaskStatus status;

    @ApiModelProperty(value = "Remarks info")
    private String remark;

    @ApiModelProperty(value = "Current task form")
    private Object formData;

    @ApiModelProperty(value = "Start time")
    private Date startTime;

    @ApiModelProperty(value = "End time")
    private Date endTime;

    @ApiModelProperty(value = "Extended Information")
    private Object extParams;

    @ApiModelProperty(value = "Extra information shown in the list")
    private Map<String, Object> showInList;

}
