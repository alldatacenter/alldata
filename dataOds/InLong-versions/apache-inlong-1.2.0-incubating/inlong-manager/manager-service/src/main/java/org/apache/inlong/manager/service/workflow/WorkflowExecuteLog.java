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

package org.apache.inlong.manager.service.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Workflow system task execution log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecuteLog {

    @ApiModelProperty("WorkflowProcess ID")
    private Integer processId;

    @ApiModelProperty("WorkflowProcess name")
    private String processDisplayName;

    @ApiModelProperty("WorkflowProcess status: same task status, such as processing: PROCESSING, "
            + "completed: COMPLETED, rejected: REJECTED, cancelled: CANCELED, terminated: TERMINATED")
    private String status;

    @ApiModelProperty("Start time")
    private Date startTime;

    @ApiModelProperty("End time")
    private Date endTime;

    @ApiModelProperty("WorkflowTask execution log")
    private List<TaskExecutorLog> taskExecutorLogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TaskExecutorLog {

        @ApiModelProperty("WorkflowTask type")
        private String taskType;

        @ApiModelProperty("WorkflowTask ID")
        private Integer taskId;

        @ApiModelProperty("WorkflowTask name")
        private String taskDisplayName;

        @ApiModelProperty("Execution status: same task status, such as "
                + "complete: COMPLETE; failure: FAILED; processing: PENDING")
        private String status;

        @ApiModelProperty("Start time")
        private Date startTime;

        @ApiModelProperty("End time")
        private Date endTime;

        @ApiModelProperty("Listener execution log")
        private List<ListenerExecutorLog> listenerExecutorLogs;

        public static TaskExecutorLog buildFromTaskInst(WorkflowTaskEntity workflowTaskEntity) {
            return TaskExecutorLog.builder()
                    .taskType(workflowTaskEntity.getType())
                    .taskId(workflowTaskEntity.getId())
                    .taskDisplayName(workflowTaskEntity.getDisplayName())
                    .status(workflowTaskEntity.getStatus())
                    .startTime(workflowTaskEntity.getStartTime())
                    .endTime(workflowTaskEntity.getEndTime())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class ListenerExecutorLog {

        @ApiModelProperty("id")
        private Integer id;

        @ApiModelProperty("Event type")
        private String eventType;

        @ApiModelProperty("Event")
        private String event;

        @ApiModelProperty("Listener name")
        private String listener;

        @ApiModelProperty("Status")
        private Integer status;

        @ApiModelProperty("Is it synchronized")
        private Integer async;

        @ApiModelProperty("Execute IP")
        private String ip;

        @ApiModelProperty("Start time")
        private Date startTime;

        @ApiModelProperty("End time")
        private Date endTime;

        @ApiModelProperty("Execution result information")
        private String remark;

        @ApiModelProperty("Exception")
        private String exception;

        @ApiModelProperty("Description")
        private String description;

        /**
         * Get listener executor log.
         */
        public static ListenerExecutorLog fromEventLog(WorkflowEventLogEntity workflowEventLogEntity) {
            ListenerExecutorLog executorLog = ListenerExecutorLog.builder()
                    .id(workflowEventLogEntity.getId())
                    .eventType(workflowEventLogEntity.getEventType())
                    .event(workflowEventLogEntity.getEvent())
                    .listener(workflowEventLogEntity.getListener())
                    .status(workflowEventLogEntity.getStatus())
                    .async(workflowEventLogEntity.getAsync())
                    .ip(workflowEventLogEntity.getIp())
                    .startTime(workflowEventLogEntity.getStartTime())
                    .endTime(workflowEventLogEntity.getEndTime())
                    .remark(workflowEventLogEntity.getRemark())
                    .exception(workflowEventLogEntity.getException())
                    .build();
            executorLog.buildDescription();
            return executorLog;
        }

        private void buildDescription() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sb = sdf.format(startTime) + " ~ " + sdf.format(endTime) + " [" + listener + "] "
                    + "event: [" + event + "], executed [" + (status == 1 ? "success" : "failed") + "], "
                    + "remark: [" + remark + "], exception: [" + exception + "]";
            this.setDescription(sb);
        }
    }

}
