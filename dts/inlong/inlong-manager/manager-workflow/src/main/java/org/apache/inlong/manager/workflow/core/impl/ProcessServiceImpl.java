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

package org.apache.inlong.manager.workflow.core.impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.UserEntity;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.UserEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessService;
import org.apache.inlong.manager.workflow.core.ProcessorExecutor;
import org.apache.inlong.manager.workflow.core.WorkflowContextBuilder;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WorkflowProcess service
 */
@Service
@Slf4j
public class ProcessServiceImpl implements ProcessService {

    @Autowired
    private ProcessorExecutor processorExecutor;
    @Autowired
    private WorkflowTaskEntityMapper taskEntityMapper;
    @Autowired
    private WorkflowContextBuilder workflowContextBuilder;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private UserEntityMapper userMapper;

    @Override
    public WorkflowContext start(String name, String applicant, ProcessForm form) {
        Preconditions.expectNotBlank(name, ErrorCodeEnum.INVALID_PARAMETER, "process name cannot be null");
        Preconditions.expectNotBlank(applicant, ErrorCodeEnum.INVALID_PARAMETER, "applicant cannot be null");
        Preconditions.expectNotNull(form, "form cannot be null");

        // build context
        WorkflowContext context = workflowContextBuilder.buildContextForProcess(name, applicant, form);
        checkUser(context, applicant, "Current user does not have permission to start workflow");
        this.processorExecutor.executeStart(context.getProcess().getStartEvent(), context);
        return context;
    }

    @Override
    public WorkflowContext continueProcess(Integer processId, String operator, String remark) {
        Preconditions.expectNotBlank(operator, ErrorCodeEnum.INVALID_PARAMETER, "operator cannot be null");
        Preconditions.expectNotNull(processId, "processId cannot be null");
        WorkflowContext context = workflowContextBuilder.buildContextForProcess(processId);
        checkUser(context, operator, "Current user does not have permission to operate workflow");
        WorkflowProcessEntity processEntity = context.getProcessEntity();
        ProcessStatus processStatus = ProcessStatus.valueOf(processEntity.getStatus());
        Preconditions.expectTrue(processStatus == ProcessStatus.PROCESSING,
                String.format("processId=%s should be in processing", processId));
        List<WorkflowTaskEntity> startElements = Lists.newArrayList();
        startElements.addAll(taskEntityMapper.selectByProcess(processId, TaskStatus.PENDING));
        startElements.addAll(taskEntityMapper.selectByProcess(processId, TaskStatus.FAILED));
        for (WorkflowTaskEntity taskEntity : startElements) {
            String taskName = taskEntity.getName();
            WorkflowTask task = context.getProcess().getTaskByName(taskName);
            context.setActionContext(new WorkflowContext.ActionContext()
                    .setAction(WorkflowAction.COMPLETE)
                    .setTaskEntity(taskEntity)
                    .setOperator(operator)
                    .setRemark(remark)
                    .setTask(task));
            this.processorExecutor.executeStart(task, context);
        }
        return context;
    }

    @Override
    public WorkflowContext cancel(Integer processId, String operator, String remark) {
        Preconditions.expectNotBlank(operator, ErrorCodeEnum.INVALID_PARAMETER, "operator cannot be null");
        Preconditions.expectNotNull(processId, "processId cannot be null");

        WorkflowContext context = workflowContextBuilder.buildContextForProcess(processId);
        checkUser(context, operator, "Current user does not have permission to cancel workflow");
        List<WorkflowTaskEntity> pendingTasks = taskEntityMapper.selectByProcess(processId, TaskStatus.PENDING);
        for (WorkflowTaskEntity taskEntity : pendingTasks) {
            WorkflowTask task = context.getProcess().getTaskByName(taskEntity.getName());
            context.setActionContext(new WorkflowContext.ActionContext()
                    .setAction(WorkflowAction.CANCEL)
                    .setTaskEntity(taskEntity)
                    .setOperator(operator)
                    .setRemark(remark)
                    .setTask(task));
            this.processorExecutor.executeComplete(task, context);
        }

        return context;
    }

    public void checkUser(WorkflowContext context, String user, String errMsg) {
        String groupId = context.getProcessForm().getInlongGroupId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY,
                ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        UserEntity userEntity = userMapper.selectByName(user);
        boolean isInCharge = Preconditions.inSeparatedString(user, groupEntity.getInCharges(), InlongConstants.COMMA);
        Preconditions.expectTrue(isInCharge || UserTypeEnum.ADMIN.getCode().equals(userEntity.getAccountType()),
                errMsg);
    }

}
