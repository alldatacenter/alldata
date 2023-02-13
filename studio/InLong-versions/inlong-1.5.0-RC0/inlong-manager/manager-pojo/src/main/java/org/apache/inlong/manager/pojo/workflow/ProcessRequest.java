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

package org.apache.inlong.manager.pojo.workflow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.common.PageRequest;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * Workflow process request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel("Workflow process request")
public class ProcessRequest extends PageRequest {

    @ApiModelProperty("Process ID")
    private Integer id;

    @ApiModelProperty("Process id list")
    private List<Integer> idList;

    @ApiModelProperty("Process name list")
    private List<String> nameList;

    @ApiModelProperty("Process display name")
    private String displayName;

    @ApiModelProperty("Applicant")
    private String applicant;

    @ApiModelProperty("Status")
    private ProcessStatus status;

    @ApiModelProperty("Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty("Inlong stream id")
    private String inlongStreamId;

    @ApiModelProperty("Start time-lower limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTimeBegin;

    @ApiModelProperty("Start time-upper limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTimeEnd;

    @ApiModelProperty("End time-lower limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTimeBegin;

    @ApiModelProperty("End time-upper limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTimeEnd;

    @ApiModelProperty("Whether to hide, 0: not hidden, 1: hidden")
    @Builder.Default
    private Integer hidden = 0;

    @ApiModelProperty("Whether to include the current to-do task")
    @Builder.Default
    private Boolean includeCurrentTask = false;

    @ApiModelProperty("Whether to include the form info displayed in the list")
    @Builder.Default
    private Boolean includeShowInList = true;

}
