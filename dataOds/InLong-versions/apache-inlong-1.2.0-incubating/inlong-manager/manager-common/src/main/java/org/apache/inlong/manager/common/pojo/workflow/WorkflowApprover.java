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
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Approver config of workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Approver config of workflow")
public class WorkflowApprover {

    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("process name")
    private String processName;

    @ApiModelProperty("process display name")
    private String processDisplayName;

    @ApiModelProperty("task name")
    private String taskName;

    @ApiModelProperty("task display name")
    private String taskDisplayName;

    @ApiModelProperty("filter key")
    private FilterKey filterKey;

    @ApiModelProperty("filter value")
    private String filterValue;

    @ApiModelProperty("filter value desc")
    private String filterValueDesc;

    @ApiModelProperty("approver list, separate with commas(\",\") when multiple")
    private String approvers;

    private String creator;
    private String modifier;
    private Date createTime;
    private Date modifyTime;
}
