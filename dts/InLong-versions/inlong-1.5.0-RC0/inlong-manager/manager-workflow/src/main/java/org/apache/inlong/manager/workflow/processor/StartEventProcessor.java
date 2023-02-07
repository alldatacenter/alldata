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
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Start event handler
 */
@Slf4j
@Service
public class StartEventProcessor extends AbstractNextableElementProcessor<StartEvent> {

    @Autowired
    private ProcessEventNotifier processEventNotifier;
    @Autowired
    private WorkflowProcessEntityMapper processEntityMapper;

    @Override
    public Class<StartEvent> watch() {
        return StartEvent.class;
    }

    @Override
    public boolean create(StartEvent startEvent, WorkflowContext context) {
        String applicant = context.getOperator();
        WorkflowProcess process = context.getProcess();
        ProcessForm form = context.getProcessForm();
        if (process.getFormClass() != null) {
            Preconditions.checkNotNull(form, "process form cannot be null");
            Preconditions.checkTrue(form.getClass().isAssignableFrom(process.getFormClass()),
                    String.format("form type %s should match the process form type %s",
                            form.getClass(), process.getFormClass()));
            form.validate();
        } else {
            log.warn("not need to provide the form info");
            context.setProcessForm(null);
        }

        WorkflowProcessEntity processEntity = saveProcessEntity(applicant, process, form);
        context.setProcessEntity(processEntity);
        context.setActionContext(new WorkflowContext.ActionContext().setAction(WorkflowAction.START));
        return true;
    }

    @Override
    public boolean pendingForAction(WorkflowContext context) {
        return false;
    }

    @Override
    public boolean complete(WorkflowContext context) {
        ListenerResult listenerResult = processEventNotifier.notify(ProcessEvent.CREATE, context);
        return listenerResult.isSuccess();
    }

    private WorkflowProcessEntity saveProcessEntity(String applicant, WorkflowProcess process, ProcessForm form) {
        WorkflowProcessEntity processEntity = new WorkflowProcessEntity();
        processEntity.setName(process.getName());
        processEntity.setDisplayName(process.getDisplayName());
        processEntity.setType(process.getType());
        processEntity.setTitle(form.getTitle());

        processEntity.setInlongGroupId(form.getInlongGroupId());
        if (form instanceof StreamResourceProcessForm) {
            StreamResourceProcessForm streamForm = (StreamResourceProcessForm) form;
            processEntity.setInlongStreamId(streamForm.getStreamInfo().getInlongStreamId());
        }

        processEntity.setApplicant(applicant);
        processEntity.setStatus(ProcessStatus.PROCESSING.name());
        processEntity.setFormData(JsonUtils.toJsonString(form));
        processEntity.setStartTime(new Date());
        processEntity.setHidden(process.getHidden());

        processEntityMapper.insert(processEntity);
        Preconditions.checkNotNull(processEntity.getId(), "process saved failed");
        return processEntity;
    }
}
