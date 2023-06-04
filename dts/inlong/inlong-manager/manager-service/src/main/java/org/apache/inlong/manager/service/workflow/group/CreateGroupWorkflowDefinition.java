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
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.listener.group.InitGroupCompleteListener;
import org.apache.inlong.manager.service.listener.group.InitGroupFailedListener;
import org.apache.inlong.manager.service.listener.group.InitGroupListener;
import org.apache.inlong.manager.service.listener.GroupTaskListenerFactory;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create inlong group process definition
 */
@Slf4j
@Component
public class CreateGroupWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private InitGroupListener initGroupListener;
    @Autowired
    private InitGroupCompleteListener initGroupCompleteListener;
    @Autowired
    private InitGroupFailedListener initGroupFailedListener;
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
        process.addListener(initGroupListener);
        process.addListener(initGroupCompleteListener);
        process.addListener(initGroupFailedListener);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // Init MQ
        ServiceTask initMQTask = new ServiceTask();
        initMQTask.setName("InitMQ");
        initMQTask.setDisplayName("Group-InitMQ");
        initMQTask.setServiceTaskType(ServiceTaskType.INIT_MQ);
        initMQTask.setListenerFactory(groupTaskListenerFactory);
        process.addTask(initMQTask);

        // Init Sort
        ServiceTask initSortTask = new ServiceTask();
        initSortTask.setName("InitSort");
        initSortTask.setDisplayName("Group-InitSort");
        initSortTask.setServiceTaskType(ServiceTaskType.INIT_SORT);
        initSortTask.setListenerFactory(groupTaskListenerFactory);
        process.addTask(initSortTask);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        // Task dependency order: 1.MQ -> 2.Sink-in-Stream -> 3.Sort -> 4.Source-in-Stream
        // To ensure that after some tasks fail, data will not start to be collected by source or consumed by sort
        startEvent.addNext(initMQTask);
        initMQTask.addNext(initSortTask);
        initSortTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.CREATE_GROUP_RESOURCE;
    }

}
