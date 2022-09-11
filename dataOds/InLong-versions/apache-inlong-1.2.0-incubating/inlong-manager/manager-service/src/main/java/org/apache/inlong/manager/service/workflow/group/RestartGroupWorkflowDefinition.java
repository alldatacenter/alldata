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
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.listener.GroupTaskListenerFactory;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.workflow.group.listener.GroupUpdateCompleteListener;
import org.apache.inlong.manager.service.workflow.group.listener.GroupUpdateFailedListener;
import org.apache.inlong.manager.service.workflow.group.listener.GroupUpdateListener;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Restart inlong group process definition
 */
@Slf4j
@Component
public class RestartGroupWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private GroupUpdateListener groupUpdateListener;

    @Autowired
    private GroupUpdateCompleteListener groupUpdateCompleteListener;

    @Autowired
    private GroupUpdateFailedListener groupUpdateFailedListener;

    @Autowired
    private GroupTaskListenerFactory groupTaskListenerFactory;

    @Override
    public WorkflowProcess defineProcess() {
        // Configuration process
        WorkflowProcess process = new WorkflowProcess();
        process.addListener(groupUpdateListener);
        process.addListener(groupUpdateCompleteListener);
        process.addListener(groupUpdateFailedListener);
        process.setType("Group Resource Restart");
        process.setName(getProcessName().name());
        process.setDisplayName(getProcessName().getDisplayName());
        process.setFormClass(GroupResourceProcessForm.class);
        process.setVersion(1);
        process.setHidden(1);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        //restart sort
        ServiceTask restartSortTask = new ServiceTask();
        restartSortTask.setName("restartSort");
        restartSortTask.setDisplayName("Group-RestartSort");
        restartSortTask.addServiceTaskType(ServiceTaskType.RESTART_SORT);
        restartSortTask.addListenerProvider(groupTaskListenerFactory);
        process.addTask(restartSortTask);

        //restart datasource
        ServiceTask restartDataSourceTask = new ServiceTask();
        restartDataSourceTask.setName("restartSource");
        restartDataSourceTask.setDisplayName("Group-RestartSource");
        restartDataSourceTask.addServiceTaskType(ServiceTaskType.RESTART_SOURCE);
        restartDataSourceTask.addListenerProvider(groupTaskListenerFactory);
        process.addTask(restartDataSourceTask);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        startEvent.addNext(restartSortTask);
        restartSortTask.addNext(restartDataSourceTask);
        restartDataSourceTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.RESTART_GROUP_PROCESS;
    }

}
