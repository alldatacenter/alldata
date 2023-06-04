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

package org.apache.inlong.manager.workflow.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.JsonException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.ServiceTaskForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.WorkflowContext.ActionContext;
import org.apache.inlong.manager.workflow.definition.ApproverAssign;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventNotifier;
import org.apache.inlong.manager.workflow.event.task.TaskEventNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * System task processor
 */
@Service
@NoArgsConstructor
@Slf4j
public class ServiceTaskProcessor extends AbstractTaskProcessor<ServiceTask> {

    private static final Set<TaskStatus> ALLOW_COMPLETE_STATE = ImmutableSet.of(
            TaskStatus.PENDING, TaskStatus.FAILED);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WorkflowTaskEntityMapper taskEntityMapper;
    @Autowired
    private TaskEventNotifier taskEventNotifier;
    @Autowired
    private ProcessEventNotifier processEventNotifier;

    @Override
    public Class<ServiceTask> watch() {
        return ServiceTask.class;
    }

    @Override
    public boolean create(ServiceTask serviceTask, WorkflowContext context) {
        context.setCurrentElement(serviceTask);
        WorkflowTaskEntity workflowTaskEntity = resetActionContext(context);
        try {
            serviceTask.initListeners(context);
            ListenerResult listenerResult = taskEventNotifier.notify(TaskEvent.CREATE, context);
            if (!listenerResult.isSuccess()) {
                failedTask(context, workflowTaskEntity);
            }
            return listenerResult.isSuccess();
        } catch (Exception e) {
            log.error("Create service task failed", e);
            failedTask(context, workflowTaskEntity);
            return false;
        }
    }

    @Override
    public boolean pendingForAction(WorkflowContext context) {
        return false;
    }

    @Override
    public boolean complete(WorkflowContext context) {
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        if (actionContext == null) {
            resetActionContext(context);
            actionContext = context.getActionContext();
        }
        WorkflowTaskEntity taskEntity = actionContext.getTaskEntity();
        Preconditions.expectTrue(ALLOW_COMPLETE_STATE.contains(TaskStatus.valueOf(taskEntity.getStatus())),
                String.format("task status %s not allowed to complete", taskEntity.getStatus()));

        try {
            ListenerResult listenerResult = this.taskEventNotifier.notify(TaskEvent.COMPLETE, context);
            if (!listenerResult.isSuccess()) {
                failedTask(context, taskEntity);
            } else {
                completeTaskEntity(context, taskEntity, TaskStatus.COMPLETED);
            }
            return listenerResult.isSuccess();
        } catch (Exception e) {
            log.error("failed to complete service task: " + taskEntity, e);
            failedTask(context, taskEntity);
            return false;
        }
    }

    private void failedTask(WorkflowContext context, WorkflowTaskEntity workflowTaskEntity) {
        completeTaskEntity(context, workflowTaskEntity, TaskStatus.FAILED);
        this.taskEventNotifier.notify(TaskEvent.FAIL, context);
        this.processEventNotifier.notify(ProcessEvent.FAIL, context);
    }

    private WorkflowTaskEntity resetActionContext(WorkflowContext context) {
        WorkflowProcessEntity processEntity = context.getProcessEntity();
        ServiceTask serviceTask = (ServiceTask) context.getCurrentElement();
        final int processId = processEntity.getId();
        final String serviceName = serviceTask.getName();
        TaskRequest taskQuery = new TaskRequest();
        taskQuery.setProcessId(processId);
        taskQuery.setName(serviceName);
        List<WorkflowTaskEntity> taskEntities = taskEntityMapper.selectByQuery(taskQuery);

        WorkflowTaskEntity taskEntity;
        if (CollectionUtils.isEmpty(taskEntities)) {
            taskEntity = saveTaskEntity(serviceTask, context);
        } else {
            taskEntity = taskEntities.get(0);
        }

        ActionContext actionContext = new WorkflowContext.ActionContext()
                .setTask((WorkflowTask) context.getCurrentElement())
                .setAction(WorkflowAction.COMPLETE)
                .setTaskEntity(taskEntity);
        context.setActionContext(actionContext);
        return taskEntity;
    }

    private WorkflowTaskEntity saveTaskEntity(ServiceTask serviceTask, WorkflowContext context) {
        WorkflowProcessEntity workflowProcessEntity = context.getProcessEntity();
        List<String> approvers = ApproverAssign.DEFAULT_SYSTEM_APPROVER.assign(context);
        WorkflowTaskEntity taskEntity = new WorkflowTaskEntity();
        taskEntity.setType(ServiceTask.class.getSimpleName());
        taskEntity.setProcessId(workflowProcessEntity.getId());
        taskEntity.setProcessName(context.getProcess().getName());
        taskEntity.setProcessDisplayName(context.getProcess().getDisplayName());
        taskEntity.setName(serviceTask.getName());
        taskEntity.setDisplayName(serviceTask.getDisplayName());
        taskEntity.setApplicant(workflowProcessEntity.getApplicant());
        taskEntity.setApprovers(StringUtils.join(approvers, WorkflowTaskEntity.APPROVERS_DELIMITER));
        taskEntity.setStatus(TaskStatus.PENDING.name());
        taskEntity.setStartTime(new Date());

        taskEntityMapper.insert(taskEntity);
        Preconditions.expectNotNull(taskEntity.getId(), "task saved failed");
        return taskEntity;
    }

    private void completeTaskEntity(WorkflowContext context, WorkflowTaskEntity taskEntity, TaskStatus taskStatus) {
        ActionContext actionContext = context.getActionContext();
        taskEntity.setStatus(taskStatus.name());
        taskEntity.setOperator(taskEntity.getApprovers());
        taskEntity.setRemark(actionContext.getRemark());
        try {
            if (actionContext.getForm() == null) {
                ServiceTaskForm serviceTaskForm = new ServiceTaskForm();
                ProcessForm form = context.getProcessForm();
                serviceTaskForm.setInlongGroupId(form.getInlongGroupId());
                if (form instanceof StreamResourceProcessForm) {
                    String streamId = ((StreamResourceProcessForm) form).getStreamInfo().getInlongStreamId();
                    serviceTaskForm.setInlongStreamId(streamId);
                }
                actionContext.setForm(serviceTaskForm);
            }
            taskEntity.setFormData(objectMapper.writeValueAsString(actionContext.getForm()));
        } catch (Exception e) {
            throw new JsonException("write form to json error: ", e);
        }
        taskEntity.setEndTime(new Date());
        taskEntityMapper.update(taskEntity);
    }

}
