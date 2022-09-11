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
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.workflow.group.listener.GroupCompleteProcessListener;
import org.apache.inlong.manager.service.workflow.group.listener.GroupFailedProcessListener;
import org.apache.inlong.manager.service.workflow.group.listener.GroupInitProcessListener;
import org.apache.inlong.manager.service.workflow.listener.GroupTaskListenerFactory;
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
    private GroupInitProcessListener groupInitProcessListener;
    @Autowired
    private GroupCompleteProcessListener groupCompleteProcessListener;
    @Autowired
    private GroupFailedProcessListener groupFailedProcessListener;
    @Autowired
    private GroupTaskListenerFactory groupTaskListenerFactory;

    @Override
    public WorkflowProcess defineProcess() {

        // Configuration process
        WorkflowProcess process = new WorkflowProcess();
        process.addListener(groupInitProcessListener);
        process.addListener(groupCompleteProcessListener);
        process.addListener(groupFailedProcessListener);

        process.setType("Group Resource Creation");
        process.setName(getProcessName().name());
        process.setDisplayName(getProcessName().getDisplayName());
        process.setFormClass(GroupResourceProcessForm.class);
        process.setVersion(1);
        process.setHidden(1);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // init MQ
        ServiceTask initMQTask = new ServiceTask();
        initMQTask.setName("initMQ");
        initMQTask.setDisplayName("Group-InitMQ");
        initMQTask.addServiceTaskType(ServiceTaskType.INIT_MQ);
        initMQTask.addListenerProvider(groupTaskListenerFactory);
        process.addTask(initMQTask);

        // init Sink
        ServiceTask initSinkTask = new ServiceTask();
        initSinkTask.setName("initSink");
        initSinkTask.setDisplayName("Group-InitSink");
        initSinkTask.addServiceTaskType(ServiceTaskType.INIT_SINK);
        initSinkTask.addListenerProvider(groupTaskListenerFactory);
        process.addTask(initSinkTask);

        // init Sort
        ServiceTask initSortTask = new ServiceTask();
        initSortTask.setName("initSort");
        initSortTask.setDisplayName("Group-InitSort");
        initSortTask.addServiceTaskType(ServiceTaskType.INIT_SORT);
        initSortTask.addListenerProvider(groupTaskListenerFactory);
        process.addTask(initSortTask);

        // init Source
        ServiceTask initSourceTask = new ServiceTask();
        initSourceTask.setName("initSource");
        initSourceTask.setDisplayName("Group-InitSource");
        initSourceTask.addServiceTaskType(ServiceTaskType.INIT_SOURCE);
        initSourceTask.addListenerProvider(groupTaskListenerFactory);
        process.addTask(initSourceTask);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        // Task dependency order: 1.MQ -> 2.Sink -> 3.Sort -> 4.Source
        // To ensure that after some tasks fail, data will not start to be collected by source or consumed by sort
        startEvent.addNext(initMQTask);
        initMQTask.addNext(initSinkTask);
        initSinkTask.addNext(initSortTask);
        initSortTask.addNext(initSourceTask);
        initSourceTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.CREATE_GROUP_RESOURCE;
    }

}
