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
import org.apache.inlong.manager.common.enums.ProcessStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Process response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Process response")
public class ProcessResponse {

    @ApiModelProperty(value = "Process ID")
    private Integer id;

    @ApiModelProperty(value = "Process name")
    private String name;

    @ApiModelProperty(value = "Process display name")
    private String displayName;

    @ApiModelProperty(value = "Process classification")
    private String type;

    @ApiModelProperty(value = "Process title")
    private String title;

    @ApiModelProperty(value = "Applicant name")
    private String applicant;

    @ApiModelProperty(value = "Process status")
    private ProcessStatus status;

    @ApiModelProperty(value = "Start time")
    private Date startTime;

    @ApiModelProperty(value = "End time")
    private Date endTime;

    @ApiModelProperty(value = "Form information-JSON")
    private Object formData;

    @ApiModelProperty(value = "Extended information-JSON")
    private Object extParams;

    @ApiModelProperty(value = "Tasks currently to be done")
    private List<TaskResponse> currentTasks;

    @ApiModelProperty(value = "Extra information shown in the list")
    private Map<String, Object> showInList;

}
