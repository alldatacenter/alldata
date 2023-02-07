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

package org.apache.inlong.manager.service.consume;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ConsumeStatus;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumeProcessForm;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Operation to the inlong consume process.
 */
@Slf4j
@Service
public class InlongConsumeProcessService {

    @Autowired
    private InlongConsumeService consumeService;
    @Autowired
    private WorkflowService workflowService;

    /**
     * Start the process for the specified ID.
     *
     * @param id inlong consume id
     * @param operator name of operator
     * @return workflow result
     */
    public WorkflowResult startProcess(Integer id, String operator) {
        consumeService.updateStatus(id, ConsumeStatus.TO_BE_APPROVAL.getCode(), operator);
        return workflowService.start(ProcessName.APPLY_CONSUME_PROCESS, operator, genApplyConsumeProcessForm(id));
    }

    private ApplyConsumeProcessForm genApplyConsumeProcessForm(Integer id) {
        ApplyConsumeProcessForm form = new ApplyConsumeProcessForm();
        form.setConsumeInfo(consumeService.get(id));
        return form;
    }

}
