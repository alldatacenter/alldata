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
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Process count query
 */
@Data
@ApiModel("Process count query")
public class ProcessCountQuery {

    @ApiModelProperty("Process name")
    private String name;

    @ApiModelProperty("Process display name")
    private String displayName;

    @ApiModelProperty("Applicant")
    private String applicant;

    @ApiModelProperty("InLong group id")
    private String inlongGroupId;

    @ApiModelProperty("Start time-lower limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTimeBegin;

    @ApiModelProperty("Start time-upper limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTimeEnd;

    @ApiModelProperty("End time-upper limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTimeBegin;

    @ApiModelProperty("End time-lower limit")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTimeEnd;

    @ApiModelProperty("Whether to hide")
    private Integer hidden = 0;

}
