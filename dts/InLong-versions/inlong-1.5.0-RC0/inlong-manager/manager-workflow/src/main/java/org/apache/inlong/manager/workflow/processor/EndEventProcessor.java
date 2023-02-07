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

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * End event handler
 */
@Slf4j
@Service
public class EndEventProcessor implements ElementProcessor<EndEvent> {

    @Autowired
    private ProcessEventNotifier processEventNotifier;
    @Autowired
    private WorkflowTaskEntityMapper taskEntityMapper;
    @Autowired
    private WorkflowProcessEntityMapper processEntityMapper;

    @Override
    public Class<EndEvent> watch() {
        return EndEvent.class;
    }

    @Override
    public boolean create(EndEvent element, WorkflowContext context) {
        // do nothing
        return true;
    }

    @Override
    public boolean pendingForAction(WorkflowContext context) {
        return false;
    }

    @Override
    public boolean complete(WorkflowContext context) {
        WorkflowProcessEntity processEntity = context.getProcessEntity();
        List<WorkflowTaskEntity> tasks = taskEntityMapper.selectByProcess(processEntity.getId(), TaskStatus.PENDING);
        // If there are unfinished tasks, the process cannot be ended
        if (!CollectionUtils.isEmpty(tasks)) {
            log.warn("have pending task, end event not execute");
            return true;
        }
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        processEntity.setStatus(getProcessStatus(actionContext.getAction()).name());
        processEntity.setEndTime(new Date());
        processEntityMapper.update(processEntity);
        ListenerResult listenerResult = processEventNotifier.notify(mapToEvent(actionContext.getAction()), context);

        return listenerResult.isSuccess();
    }

    @Override
    public List<Element> next(EndEvent element, WorkflowContext context) {
        return Collections.emptyList();
    }

    private ProcessStatus getProcessStatus(WorkflowAction workflowAction) {
        switch (workflowAction) {
            case APPROVE:
            case COMPLETE:
                return ProcessStatus.COMPLETED;
            case REJECT:
                return ProcessStatus.REJECTED;
            case CANCEL:
                return ProcessStatus.CANCELED;
            case TERMINATE:
                return ProcessStatus.TERMINATED;
            default:
                throw new WorkflowException("unknown workflowAction " + workflowAction);
        }
    }

    private ProcessEvent mapToEvent(WorkflowAction workflowAction) {
        switch (workflowAction) {
            case APPROVE:
            case COMPLETE:
                return ProcessEvent.COMPLETE;
            case REJECT:
                return ProcessEvent.REJECT;
            case CANCEL:
                return ProcessEvent.CANCEL;
            case TERMINATE:
                return ProcessEvent.TERMINATE;
            default:
                throw new WorkflowException("unknown workflowAction " + workflowAction);
        }
    }

}
