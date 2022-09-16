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

/**
 * Workflow approver query request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel("Workflow approver query request")
public class ApproverPageRequest extends PageRequest {

    @ApiModelProperty(value = "Keywords, can be process name, task name, approver name, etc.")
    private String keyword;

    @ApiModelProperty("Workflow process name")
    private String processName;

    @ApiModelProperty("Workflow task name")
    private String taskName;

    @ApiModelProperty("Specified workflow approver")
    private String approver;

    @ApiModelProperty(value = "Current user", hidden = true)
    private String currentUser;

    @ApiModelProperty(value = "Whether the current user is in the administrator role", hidden = true)
    private Boolean isAdminRole;

}
