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

import com.google.common.base.Joiner;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.ApproverAssign;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * WorkflowTask processor
 */
public abstract class AbstractTaskProcessor<T extends WorkflowTask> extends
        AbstractNextableElementProcessor<T> implements SkipableElementProcessor<T> {

    @Autowired
    protected WorkflowTaskEntityMapper taskEntityMapper;

    @Override
    public void skip(T task, WorkflowContext context) {
        WorkflowProcessEntity workflowProcessEntity = context.getProcessEntity();
        Date now = new Date();
        String operators = Joiner.on(WorkflowTaskEntity.APPROVERS_DELIMITER)
                .join(ApproverAssign.DEFAULT_SKIP_APPROVER.assign(context));

        WorkflowTaskEntity taskEntity = new WorkflowTaskEntity();
        taskEntity.setType(task.getClass().getSimpleName());
        taskEntity.setProcessId(workflowProcessEntity.getId());
        taskEntity.setProcessName(workflowProcessEntity.getName());
        taskEntity.setProcessDisplayName(workflowProcessEntity.getDisplayName());
        taskEntity.setApplicant(workflowProcessEntity.getApplicant());
        taskEntity.setApprovers(operators);
        taskEntity.setOperator(operators);
        taskEntity.setName(task.getName());
        taskEntity.setDisplayName(task.getDisplayName());
        taskEntity.setStatus(TaskStatus.SKIPPED.name());
        taskEntity.setRemark("auto skipped");
        taskEntity.setStartTime(now);
        taskEntity.setEndTime(now);

        taskEntityMapper.insert(taskEntity);
        Preconditions.checkNotNull(taskEntity.getId(), "task saved failed");
    }

}
