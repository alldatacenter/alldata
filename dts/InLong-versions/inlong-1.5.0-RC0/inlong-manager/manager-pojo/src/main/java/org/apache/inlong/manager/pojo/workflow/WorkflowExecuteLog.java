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

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Workflow task execution log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecuteLog {

    @ApiModelProperty("Process ID")
    private Integer processId;

    @ApiModelProperty("Process name")
    private String processDisplayName;

    @ApiModelProperty("Process status: same task status, such as processing: PROCESSING, "
            + "completed: COMPLETED, rejected: REJECTED, cancelled: CANCELED, terminated: TERMINATED")
    private String status;

    @ApiModelProperty("Start time")
    private Date startTime;

    @ApiModelProperty("End time")
    private Date endTime;

    @ApiModelProperty("Task execution logs")
    private List<TaskExecuteLog> taskExecuteLogs;

}
