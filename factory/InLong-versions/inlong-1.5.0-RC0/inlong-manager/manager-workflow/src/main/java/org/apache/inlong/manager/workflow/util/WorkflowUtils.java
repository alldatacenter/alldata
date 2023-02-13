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

package org.apache.inlong.manager.workflow.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.FormParseException;
import org.apache.inlong.manager.common.exceptions.JsonException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.pojo.workflow.ListenerExecuteLog;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskExecuteLog;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.TaskForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.WorkflowContext.ActionContext;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * Workflow utils
 */
public class WorkflowUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowUtils.class);

    /**
     * Build workflow context from WorkflowProcess and WorkflowProcessEntity
     */
    public static WorkflowContext buildContext(ObjectMapper objectMapper, WorkflowProcess process,
            WorkflowProcessEntity processEntity) {
        try {
            ProcessForm form = WorkflowUtils.parseProcessForm(objectMapper, processEntity.getFormData(), process);
            return new WorkflowContext().setProcess(process)
                    .setOperator(processEntity.getApplicant())
                    .setProcessForm(form)
                    .setProcessEntity(processEntity);
        } catch (Exception e) {
            LOGGER.error("build context from process form failed with id=" + processEntity.getId(), e);
            return null;
        }
    }

    /**
     * Get the workflow result from the given workflow context
     */
    public static WorkflowResult getResult(WorkflowContext context) {
        if (context == null) {
            return null;
        }

        WorkflowResult workflowResult = new WorkflowResult();
        workflowResult.setProcessInfo(WorkflowUtils.getProcessResponse(context.getProcessEntity()));
        if (context.getActionContext() != null) {
            ActionContext newAction = context.getActionContext();
            workflowResult.setNewTasks(Lists.newArrayList(WorkflowUtils.getTaskResponse(newAction.getTaskEntity())));
        }
        return workflowResult;
    }

    /**
     * Get process response from process entity
     */
    public static ProcessResponse getProcessResponse(WorkflowProcessEntity entity) {
        if (entity == null) {
            return null;
        }

        ProcessResponse processResponse = ProcessResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .displayName(entity.getDisplayName())
                .type(entity.getType())
                .title(entity.getTitle())
                .applicant(entity.getApplicant())
                .status(ProcessStatus.valueOf(entity.getStatus()))
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
        try {
            if (StringUtils.isNotBlank(entity.getFormData())) {
                processResponse.setFormData(JsonUtils.parseTree(entity.getFormData()));
            }
            if (StringUtils.isNotBlank(entity.getExtParams())) {
                processResponse.setExtParams(JsonUtils.parseTree(entity.getExtParams()));
            }
        } catch (Exception e) {
            LOGGER.error("parse process form error: ", e);
            throw new JsonException("parse process form or ext params error, please contact administrator");
        }

        return processResponse;
    }

    /**
     * Get task response from task entity
     */
    public static TaskResponse getTaskResponse(WorkflowTaskEntity taskEntity) {
        if (taskEntity == null) {
            return null;
        }

        TaskResponse taskResponse = TaskResponse.builder()
                .id(taskEntity.getId())
                .type(taskEntity.getType())
                .processId(taskEntity.getProcessId())
                .processName(taskEntity.getProcessName())
                .processDisplayName(taskEntity.getProcessDisplayName())
                .name(taskEntity.getName())
                .displayName(taskEntity.getDisplayName())
                .applicant(taskEntity.getApplicant())
                .approvers(Arrays.asList(taskEntity.getApprovers().split(WorkflowTaskEntity.APPROVERS_DELIMITER)))
                .operator(taskEntity.getOperator())
                .status(TaskStatus.valueOf(taskEntity.getStatus()))
                .remark(taskEntity.getRemark())
                .startTime(taskEntity.getStartTime())
                .endTime(taskEntity.getEndTime())
                .build();

        try {
            JsonNode formData = null;
            if (StringUtils.isNotBlank(taskEntity.getFormData())) {
                formData = JsonUtils.parseTree(taskEntity.getFormData());
            }
            taskResponse.setFormData(formData);
        } catch (Exception e) {
            LOGGER.error("parse task form error: ", e);
            throw new JsonException("parse task form or ext params error, please contact administrator");
        }

        return taskResponse;
    }

    /**
     * Parse the process form in JSON string format into a WorkflowProcess instance
     */
    public static <T extends ProcessForm> T parseProcessForm(ObjectMapper objectMapper, String form,
            WorkflowProcess process) {
        Preconditions.checkNotNull(process, "process cannot be null");
        if (StringUtils.isEmpty(form)) {
            return null;
        }

        try {
            JavaType javaType = objectMapper.constructType(process.getFormClass());
            return objectMapper.readValue(form, javaType);
        } catch (Exception e) {
            LOGGER.error("parse process form failed for {}", form, e);
            throw new FormParseException("parse process form failed, please contact administrator");
        }
    }

    /**
     * Parse the task form in JSON string format into a WorkflowTask instance
     */
    public static <T extends TaskForm> T parseTaskForm(ObjectMapper objectMapper, WorkflowTaskEntity taskEntity,
            WorkflowProcess process) {
        Preconditions.checkNotNull(taskEntity, "taskEntity cannot be null");
        Preconditions.checkNotNull(process, "process cannot be null");
        if (StringUtils.isEmpty(taskEntity.getFormData())) {
            return null;
        }

        WorkflowTask task = process.getTaskByName(taskEntity.getName());
        Preconditions.checkNotNull(task, "user task not exist " + taskEntity.getName());
        Preconditions.checkTrue(task instanceof UserTask, "task should be userTask " + taskEntity.getName());

        UserTask userTask = (UserTask) task;
        try {
            JavaType javaType = objectMapper.constructType(userTask.getFormClass());
            return objectMapper.readValue(taskEntity.getFormData(), javaType);
        } catch (Exception e) {
            LOGGER.error("parse task form failed for {}", taskEntity.getFormData(), e);
            throw new FormParseException("parse task form failed, please contact the administrator");
        }
    }

    /**
     * Get task execute log from the task entity.
     */
    public static TaskExecuteLog getTaskExecuteLog(WorkflowTaskEntity taskEntity) {
        return TaskExecuteLog.builder()
                .taskType(taskEntity.getType())
                .taskId(taskEntity.getId())
                .taskDisplayName(taskEntity.getDisplayName())
                .status(taskEntity.getStatus())
                .startTime(taskEntity.getStartTime())
                .endTime(taskEntity.getEndTime())
                .build();
    }

    /**
     * Get listener executor log.
     */
    public static ListenerExecuteLog getListenerExecuteLog(WorkflowEventLogEntity eventLogEntity) {
        ListenerExecuteLog executorLog = ListenerExecuteLog.builder()
                .id(eventLogEntity.getId())
                .eventType(eventLogEntity.getEventType())
                .event(eventLogEntity.getEvent())
                .listener(eventLogEntity.getListener())
                .status(eventLogEntity.getStatus())
                .async(eventLogEntity.getAsync())
                .ip(eventLogEntity.getIp())
                .startTime(eventLogEntity.getStartTime())
                .endTime(eventLogEntity.getEndTime())
                .remark(eventLogEntity.getRemark())
                .exception(eventLogEntity.getException())
                .build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String desc = sdf.format(executorLog.getStartTime()) + " ~ " + sdf.format(executorLog.getEndTime())
                + " [" + executorLog.getListener() + "] "
                + "event: [" + executorLog.getEvent() + "], "
                + "executed [" + (executorLog.getStatus() == 1 ? "success" : "failed") + "], "
                + "remark: [" + executorLog.getRemark() + "], "
                + "exception: [" + executorLog.getException() + "]";
        executorLog.setDescription(desc);

        return executorLog;
    }

}
