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

package org.apache.inlong.manager.service.workflow.group;

import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.InlongGroupApproveForm;
import org.apache.inlong.manager.service.core.WorkflowApproverService;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.listener.group.apply.AfterApprovedTaskListener;
import org.apache.inlong.manager.service.listener.group.apply.ApproveApplyProcessListener;
import org.apache.inlong.manager.service.listener.group.apply.CancelApplyProcessListener;
import org.apache.inlong.manager.service.listener.group.apply.RejectApplyProcessListener;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * New inlong group process definition
 */
@Component
public class ApplyGroupWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private CancelApplyProcessListener cancelApplyProcessListener;
    @Autowired
    private RejectApplyProcessListener rejectApplyProcessListener;
    @Autowired
    private ApproveApplyProcessListener approveApplyProcessListener;
    @Autowired
    private AfterApprovedTaskListener afterApprovedTaskListener;
    @Autowired
    private WorkflowApproverService workflowApproverService;

    @Override
    public WorkflowProcess defineProcess() {
        // Configuration process
        WorkflowProcess process = new WorkflowProcess();
        process.setName(getProcessName().name());
        process.setType(getProcessName().getDisplayName());
        process.setDisplayName(getProcessName().getDisplayName());
        process.setFormClass(ApplyGroupProcessForm.class);
        process.setVersion(1);

        // Set up the listener
        process.addListener(cancelApplyProcessListener);
        process.addListener(rejectApplyProcessListener);
        // Initiate the process of creating inlong group resources, and set the inlong group status
        // to [Configuration Successful]/[Configuration Failed] according to its completion
        process.addListener(approveApplyProcessListener);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        // System administrator approval
        UserTask adminUserTask = new UserTask();
        adminUserTask.setName(UT_ADMIN_NAME);
        adminUserTask.setDisplayName("SystemAdmin");
        adminUserTask.setFormClass(InlongGroupApproveForm.class);
        adminUserTask.setApproverAssign(context -> getTaskApprovers(adminUserTask.getName()));
        adminUserTask.addListener(afterApprovedTaskListener);
        process.addTask(adminUserTask);

        // If you need another approval process, you can add it here

        // Configuration the tasks order
        startEvent.addNext(adminUserTask);
        adminUserTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.APPLY_GROUP_PROCESS;
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
