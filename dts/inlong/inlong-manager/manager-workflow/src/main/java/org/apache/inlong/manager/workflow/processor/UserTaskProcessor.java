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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.JsonException;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.pojo.workflow.form.task.TaskForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.WorkflowContext.ActionContext;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.TaskEventNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User task processor
 */
@Slf4j
@Service
public class UserTaskProcessor extends AbstractTaskProcessor<UserTask> {

    private static final Set<WorkflowAction> SHOULD_CHECK_OPERATOR_ACTIONS = ImmutableSet
            .of(WorkflowAction.APPROVE, WorkflowAction.REJECT, WorkflowAction.TRANSFER);
    private static final Set<WorkflowAction> SUPPORT_ACTIONS = ImmutableSet.of(
            WorkflowAction.APPROVE, WorkflowAction.REJECT, WorkflowAction.TRANSFER, WorkflowAction.CANCEL,
            WorkflowAction.TERMINATE);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskEventNotifier taskEventNotifier;

    @Override
    public Class<UserTask> watch() {
        return UserTask.class;
    }

    @Override
    public boolean create(UserTask userTask, WorkflowContext context) {
        List<String> approvers = userTask.getApproverAssign().assign(context);
        Preconditions.expectNotEmpty(approvers, "Cannot assign approvers for task: " + userTask.getDisplayName()
                + ", as the approvers was empty");

        if (!userTask.isNeedAllApprove()) {
            approvers = Collections.singletonList(StringUtils.join(approvers, WorkflowTaskEntity.APPROVERS_DELIMITER));
        }

        WorkflowProcessEntity processEntity = context.getProcessEntity();
        List<WorkflowTaskEntity> userTaskEntities = approvers.stream()
                .map(approver -> saveTaskEntity(userTask, processEntity, approver))
                .collect(Collectors.toList());

        resetActionContext(context, userTaskEntities);

        ListenerResult listenerResult = taskEventNotifier.notify(TaskEvent.CREATE, context);
        return listenerResult.isSuccess();
    }

    @Override
    public boolean pendingForAction(WorkflowContext context) {
        return true;
    }

    @Override
    public boolean complete(WorkflowContext context) {
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        Preconditions.expectTrue(SUPPORT_ACTIONS.contains(actionContext.getAction()),
                "UserTask not support action:" + actionContext.getAction());

        WorkflowTaskEntity workflowTaskEntity = actionContext.getTaskEntity();
        Preconditions.expectTrue(TaskStatus.PENDING.name().equalsIgnoreCase(workflowTaskEntity.getStatus()),
                "task status should be pending");

        checkOperator(actionContext);
        completeTaskInstance(actionContext);

        ListenerResult listenerResult = taskEventNotifier.notify(toTaskEvent(actionContext.getAction()), context);
        return listenerResult.isSuccess();
    }

    @Override
    public List<Element> next(UserTask userTask, WorkflowContext context) {
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        if (userTask.isNeedAllApprove()) {
            WorkflowTaskEntity workflowTaskEntity = actionContext.getTaskEntity();
            int pendingCount = taskEntityMapper.countByStatus(workflowTaskEntity.getProcessId(),
                    workflowTaskEntity.getName(), TaskStatus.PENDING);

            if (pendingCount > 0) {
                return Lists.newArrayList();
            }
        }

        return super.next(userTask, context);
    }

    private void resetActionContext(WorkflowContext context, List<WorkflowTaskEntity> userTaskEntities) {
        ActionContext actionContext = new WorkflowContext.ActionContext()
                .setTask((WorkflowTask) context.getCurrentElement())
                .setAction(WorkflowAction.COMPLETE)
                .setTaskEntity(userTaskEntities.get(0));
        context.setActionContext(actionContext);
    }

    private WorkflowTaskEntity saveTaskEntity(UserTask task, WorkflowProcessEntity processEntity, String approvers) {
        WorkflowTaskEntity taskEntity = new WorkflowTaskEntity();

        taskEntity.setType(UserTask.class.getSimpleName());
        taskEntity.setProcessId(processEntity.getId());
        taskEntity.setProcessName(processEntity.getName());
        taskEntity.setProcessDisplayName(processEntity.getDisplayName());
        taskEntity.setApplicant(processEntity.getApplicant());
        taskEntity.setName(task.getName());
        taskEntity.setDisplayName(task.getDisplayName());
        taskEntity.setApprovers(approvers);
        taskEntity.setStatus(TaskStatus.PENDING.name());
        taskEntity.setStartTime(new Date());

        taskEntityMapper.insert(taskEntity);
        Preconditions.expectNotNull(taskEntity.getId(), "task saved failed");
        return taskEntity;
    }

    private void checkOperator(WorkflowContext.ActionContext actionContext) {
        WorkflowTaskEntity workflowTaskEntity = actionContext.getTaskEntity();
        if (!SHOULD_CHECK_OPERATOR_ACTIONS.contains(actionContext.getAction())) {
            return;
        }

        boolean operatorIsApprover = ArrayUtils.contains(
                workflowTaskEntity.getApprovers().split(WorkflowTaskEntity.APPROVERS_DELIMITER),
                actionContext.getOperator());

        if (!operatorIsApprover) {
            throw new WorkflowException(
                    String.format("current operator %s not in approvers list: %s", actionContext.getOperator(),
                            workflowTaskEntity.getApprovers()));
        }
    }

    private void completeTaskInstance(WorkflowContext.ActionContext actionContext) {
        WorkflowTaskEntity taskEntity = actionContext.getTaskEntity();

        TaskStatus taskStatus = toTaskState(actionContext.getAction());
        taskEntity.setStatus(taskStatus.name());
        taskEntity.setOperator(actionContext.getOperator());
        taskEntity.setRemark(actionContext.getRemark());

        UserTask userTask = (UserTask) actionContext.getTask();
        try {
            TaskForm taskForm = actionContext.getForm();
            if (needForm(userTask, actionContext.getAction())) {
                Preconditions.expectNotNull(taskForm, "form cannot be null");
                Preconditions.expectTrue(taskForm.getClass().isAssignableFrom(userTask.getFormClass()),
                        "form type not match, should be class " + userTask.getFormClass());
                taskForm.validate();
                taskEntity.setFormData(objectMapper.writeValueAsString(taskForm));
            } else {
                Preconditions.expectNull(taskForm, "no form required");
            }
            taskEntity.setEndTime(new Date());

            Map<String, Object> extMap = new HashMap<>();
            if (StringUtils.isNotBlank(taskEntity.getExtParams())) {
                extMap = objectMapper.readValue(taskEntity.getExtParams(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                if (WorkflowAction.TRANSFER.equals(actionContext.getAction())) {
                    extMap.put(WorkflowTaskEntity.EXT_TRANSFER_USER_KEY, actionContext.getTransferToUsers());
                }
            }
            String extParams = objectMapper.writeValueAsString(extMap);
            taskEntity.setExtParams(extParams);
        } catch (JsonProcessingException e) {
            log.error("parse transfer users error: ", e);
            throw new JsonException("parse transfer users error");
        }
        taskEntityMapper.update(taskEntity);
    }

    private boolean needForm(UserTask userTask, WorkflowAction workflowAction) {
        if (userTask.getFormClass() == null) {
            return false;
        }

        return WorkflowAction.APPROVE.equals(workflowAction) || WorkflowAction.COMPLETE.equals(workflowAction);
    }

    private TaskStatus toTaskState(WorkflowAction workflowAction) {
        switch (workflowAction) {
            case APPROVE:
                return TaskStatus.APPROVED;
            case REJECT:
                return TaskStatus.REJECTED;
            case CANCEL:
                return TaskStatus.CANCELED;
            case TRANSFER:
                return TaskStatus.TRANSFERRED;
            case TERMINATE:
                return TaskStatus.TERMINATED;
            default:
                throw new WorkflowException("unknown workflowAction " + this);
        }
    }

    private TaskEvent toTaskEvent(WorkflowAction workflowAction) {
        switch (workflowAction) {
            case APPROVE:
                return TaskEvent.APPROVE;
            case REJECT:
                return TaskEvent.REJECT;
            case CANCEL:
                return TaskEvent.CANCEL;
            case TRANSFER:
                return TaskEvent.TRANSFER;
            case TERMINATE:
                return TaskEvent.TERMINATE;
            default:
                throw new WorkflowException("unknown workflow action " + this);
        }
    }

}
