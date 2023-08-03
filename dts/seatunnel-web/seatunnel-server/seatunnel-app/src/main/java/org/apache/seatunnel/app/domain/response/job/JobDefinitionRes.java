/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.domain.response.job;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@ApiModel(value = "jobDefinitionResponse", description = "job definition response")
@Data
public class JobDefinitionRes {
    @ApiModelProperty(value = "job definition id", dataType = "Long")
    private long id;

    @ApiModelProperty(value = "job name", dataType = "String")
    private String name;

    @ApiModelProperty(value = "job description", dataType = "String")
    private String description;

    @ApiModelProperty(value = "job type", dataType = "String")
    private String jobType;

    @ApiModelProperty(value = "create user id", dataType = "Integer")
    private int createUserId;

    @ApiModelProperty(value = "update user id", dataType = "Integer")
    private int updateUserId;

    @ApiModelProperty(value = "create user name", dataType = "String")
    private String createUserName;

    @ApiModelProperty(value = "update user name", dataType = "String")
    private String updateUserName;

    @ApiModelProperty(value = "job create time", dataType = "String")
    private Date createTime;

    @ApiModelProperty(value = "job update time", dataType = "String")
    private Date updateTime;

    @ApiModelProperty(value = "project code", dataType = "Long")
    private long projectCode;

    @ApiModelProperty(value = "project name", dataType = "String")
    private String projectName;
}
