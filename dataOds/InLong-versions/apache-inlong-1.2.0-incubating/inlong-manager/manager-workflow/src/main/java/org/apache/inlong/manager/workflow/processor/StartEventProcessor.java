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
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.exceptions.JsonException;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Start event handler
 */
@Service
public class StartEventProcessor extends AbstractNextableElementProcessor<StartEvent> {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProcessEventNotifier processEventNotifier;
    @Autowired
    private WorkflowProcessEntityMapper processEntityMapper;

    @Override
    public Class<StartEvent> watch() {
        return StartEvent.class;
    }

    @Override
    public void create(StartEvent startEvent, WorkflowContext context) {
        String applicant = context.getOperator();
        WorkflowProcess process = context.getProcess();
        ProcessForm form = context.getProcessForm();
        if (process.getFormClass() != null) {
            Preconditions.checkNotNull(form, "form cannot be null");
            Preconditions.checkTrue(form.getClass().isAssignableFrom(process.getFormClass()),
                    "form type not match, should be class " + process.getFormClass());
            form.validate();
        } else {
            Preconditions.checkNull(form, "no form required");
        }
        WorkflowProcessEntity processEntity = saveProcessEntity(applicant, process, form);
        context.setProcessEntity(processEntity);
        context.setActionContext(new WorkflowContext.ActionContext().setAction(WorkflowAction.START));
    }

    @Override
    public boolean pendingForAction(WorkflowContext context) {
        return false;
    }

    @Override
    public boolean complete(WorkflowContext context) {
        processEventNotifier.notify(ProcessEvent.CREATE, context);
        return true;
    }

    private WorkflowProcessEntity saveProcessEntity(String applicant, WorkflowProcess process, ProcessForm form) {
        WorkflowProcessEntity processEntity = new WorkflowProcessEntity();
        processEntity.setName(process.getName());
        processEntity.setDisplayName(process.getDisplayName());
        processEntity.setType(process.getType());
        processEntity.setTitle(form.getTitle());
        processEntity.setInlongGroupId(form.getInlongGroupId());
        processEntity.setApplicant(applicant);
        processEntity.setStatus(ProcessStatus.PROCESSING.name());
        try {
            processEntity.setFormData(objectMapper.writeValueAsString(form));
        } catch (Exception e) {
            throw new JsonException("write form to json error: ", e);
        }
        processEntity.setStartTime(new Date());
        processEntity.setHidden(process.getHidden());

        processEntityMapper.insert(processEntity);
        Preconditions.checkNotNull(processEntity.getId(), "process saved failed");
        return processEntity;
    }
}
