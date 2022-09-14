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

package org.apache.inlong.manager.service.workflow.consumption;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumptionProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.ConsumptionApproveForm;
import org.apache.inlong.manager.service.core.WorkflowApproverService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.listener.consumption.ConsumptionCancelProcessListener;
import org.apache.inlong.manager.service.listener.consumption.ConsumptionCompleteProcessListener;
import org.apache.inlong.manager.service.listener.consumption.ConsumptionPassTaskListener;
import org.apache.inlong.manager.service.listener.consumption.ConsumptionRejectProcessListener;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * New data consumption workflow definition
 */
@Component
public class ApplyConsumptionWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private ConsumptionCompleteProcessListener consumptionCompleteProcessListener;

    @Autowired
    private ConsumptionPassTaskListener consumptionPassTaskListener;

    @Autowired
    private ConsumptionRejectProcessListener consumptionRejectProcessListener;

    @Autowired
    private ConsumptionCancelProcessListener consumptionCancelProcessListener;

    @Autowired
    private WorkflowApproverService workflowApproverService;

    @Autowired
    private ApplyConsumptionProcessHandler applyConsumptionProcessHandler;

    @Autowired
    private InlongGroupService groupService;

    @Override
    public WorkflowProcess defineProcess() {
        // Define process information
        WorkflowProcess process = new WorkflowProcess();
        process.setName(getProcessName().name());
        process.setType(getProcessName().getDisplayName());
        process.setDisplayName(getProcessName().getDisplayName());
        process.setFormClass(ApplyConsumptionProcessForm.class);
        process.setVersion(1);
        process.setProcessDetailHandler(applyConsumptionProcessHandler);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        // Group approval tasks
        UserTask groupOwnerUserTask = new UserTask();
        groupOwnerUserTask.setName(UT_GROUP_OWNER_NAME);
        groupOwnerUserTask.setDisplayName("GroupApproval");
        groupOwnerUserTask.setApproverAssign(this::groupOwnerUserTaskApprover);
        process.addTask(groupOwnerUserTask);

        // System administrator approval
        UserTask adminUserTask = new UserTask();
        adminUserTask.setName(UT_ADMIN_NAME);
        adminUserTask.setDisplayName("SystemAdmin");
        adminUserTask.setFormClass(ConsumptionApproveForm.class);
        adminUserTask.setApproverAssign(context -> getTaskApprovers(adminUserTask.getName()));
        adminUserTask.addListener(consumptionPassTaskListener);
        process.addTask(adminUserTask);

        // Set order relation
        startEvent.addNext(groupOwnerUserTask);
        groupOwnerUserTask.addNext(adminUserTask);
        adminUserTask.addNext(endEvent);

        // Set up the listener
        process.addListener(consumptionCompleteProcessListener);
        process.addListener(consumptionRejectProcessListener);
        process.addListener(consumptionCancelProcessListener);

        return process;
    }

    private List<String> groupOwnerUserTaskApprover(WorkflowContext context) {
        ApplyConsumptionProcessForm form = (ApplyConsumptionProcessForm) context.getProcessForm();
        InlongGroupInfo groupInfo = groupService.get(form.getConsumptionInfo().getInlongGroupId());
        if (groupInfo == null || groupInfo.getInCharges() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(groupInfo.getInCharges().split(InlongConstants.COMMA));
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.APPLY_CONSUMPTION_PROCESS;
    }

    /**
     * Get task approvers by process name and task name
     *
     * @apiNote Do not delete this method, otherwise the unit tests will fail due to not loading the table
     *         structure in time.
     */
    private List<String> getTaskApprovers(String taskName) {
        return workflowApproverService.getApprovers(this.getProcessName().name(), taskName);
    }

}
