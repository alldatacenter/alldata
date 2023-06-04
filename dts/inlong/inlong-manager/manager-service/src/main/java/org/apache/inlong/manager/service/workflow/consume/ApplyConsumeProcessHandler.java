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

package org.apache.inlong.manager.service.workflow.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumeProcessForm;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionService;
import org.apache.inlong.manager.workflow.definition.ProcessDetailHandler;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.util.WorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Apply inlong consume process handler
 */
@Component
public class ApplyConsumeProcessHandler implements ProcessDetailHandler {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProcessDefinitionService processDefinitionService;

    @Override
    public ProcessDetailResponse handle(ProcessDetailResponse processResponse) {
        WorkflowProcess process = processDefinitionService.getByName(processResponse.getWorkflow().getName());
        ApplyConsumeProcessForm processForm = WorkflowUtils.parseProcessForm(objectMapper,
                processResponse.getProcessInfo().getFormData().toString(), process);
        if (processForm == null) {
            return processResponse;
        }

        processResponse.getProcessInfo().setFormData(processForm);
        return processResponse;
    }

}
