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

import org.apache.inlong.manager.pojo.workflow.form.task.TaskForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessorExecutor;
import org.apache.inlong.manager.workflow.core.TaskService;
import org.apache.inlong.manager.workflow.core.WorkflowContextBuilder;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WorkflowTask service
 */
@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private ProcessorExecutor processorExecutor;
    @Autowired
    private WorkflowContextBuilder workflowContextBuilder;

    @Override
    public WorkflowContext approve(Integer taskId, String remark, TaskForm form, String operator) {
        WorkflowContext context = workflowContextBuilder.buildContextForTask(taskId, WorkflowAction.APPROVE, form,
                remark, operator);
        processorExecutor.executeComplete(context.getActionContext().getTask(), context);
        return context;
    }

    @Override
    public WorkflowContext reject(Integer taskId, String remark, String operator) {
        WorkflowContext context = workflowContextBuilder.buildContextForTask(taskId, WorkflowAction.REJECT, remark,
                operator);
        processorExecutor.executeComplete(context.getActionContext().getTask(), context);
        return context;
    }

    @Override
    public WorkflowContext transfer(Integer taskId, String remark, List<String> to, String operator) {
        WorkflowContext context = workflowContextBuilder
                .buildContextForTask(taskId, WorkflowAction.TRANSFER, to, remark, operator);
        processorExecutor.executeComplete(context.getActionContext().getTask(), context);
        return context;
    }

    @Override
    public WorkflowContext complete(Integer taskId, String remark, String operator) {
        WorkflowContext context = workflowContextBuilder
                .buildContextForTask(taskId, WorkflowAction.COMPLETE, remark, operator);
        ServiceTask serviceTask = (ServiceTask) context.getActionContext().getTask();
        serviceTask.initListeners(context);
        processorExecutor.executeComplete(context.getActionContext().getTask(), context);
        return context;
    }

}
