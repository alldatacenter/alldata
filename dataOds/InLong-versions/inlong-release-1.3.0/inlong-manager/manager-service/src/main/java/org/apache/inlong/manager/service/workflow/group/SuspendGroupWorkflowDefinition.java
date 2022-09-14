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

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.listener.group.UpdateGroupCompleteListener;
import org.apache.inlong.manager.service.listener.group.UpdateGroupFailedListener;
import org.apache.inlong.manager.service.listener.group.UpdateGroupListener;
import org.apache.inlong.manager.service.listener.GroupTaskListenerFactory;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Suspend workflow definition for inlong group
 */
@Slf4j
@Component
public class SuspendGroupWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private UpdateGroupListener updateGroupListener;
    @Autowired
    private UpdateGroupCompleteListener updateGroupCompleteListener;
    @Autowired
    private UpdateGroupFailedListener updateGroupFailedListener;
    @Autowired
    private GroupTaskListenerFactory groupTaskListenerFactory;

    @Override
    public WorkflowProcess defineProcess() {
        // Configuration process
        WorkflowProcess process = new WorkflowProcess();
        process.setName(getProcessName().name());
        process.setType(getProcessName().getDisplayName());
        process.setDisplayName(getProcessName().getDisplayName());
        process.setFormClass(GroupResourceProcessForm.class);
        process.setVersion(1);
        process.setHidden(1);

        // Set up the listener
        process.addListener(updateGroupListener);
        process.addListener(updateGroupCompleteListener);
        process.addListener(updateGroupFailedListener);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // Stop Source
        ServiceTask stopSourceTask = new ServiceTask();
        stopSourceTask.setName("StopSource");
        stopSourceTask.setDisplayName("Group-StopSource");
        stopSourceTask.setServiceTaskType(ServiceTaskType.STOP_SOURCE);
        stopSourceTask.setListenerFactory(groupTaskListenerFactory);
        process.addTask(stopSourceTask);

        // Stop Sort
        ServiceTask stopSortTask = new ServiceTask();
        stopSortTask.setName("StopSort");
        stopSortTask.setDisplayName("Group-StopSort");
        stopSortTask.setServiceTaskType(ServiceTaskType.STOP_SORT);
        stopSortTask.setListenerFactory(groupTaskListenerFactory);
        process.addTask(stopSortTask);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        startEvent.addNext(stopSourceTask);
        stopSourceTask.addNext(stopSortTask);
        stopSortTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.SUSPEND_GROUP_PROCESS;
    }

}
