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

package org.apache.inlong.manager.service.workflow.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.listener.StreamTaskListenerFactory;
import org.apache.inlong.manager.service.listener.stream.InitStreamCompleteListener;
import org.apache.inlong.manager.service.listener.stream.InitStreamFailedListener;
import org.apache.inlong.manager.service.listener.stream.InitStreamListener;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Inlong stream access resource creation
 */
@Component
@Slf4j
public class CreateStreamWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private InitStreamListener initStreamListener;
    @Autowired
    private InitStreamCompleteListener initStreamCompleteListener;
    @Autowired
    private InitStreamFailedListener initStreamFailedListener;
    @Autowired
    private StreamTaskListenerFactory streamTaskListenerFactory;

    @Override
    public WorkflowProcess defineProcess() {
        // Configuration process
        WorkflowProcess process = new WorkflowProcess();
        process.setName(getProcessName().name());
        process.setType(getProcessName().getDisplayName());
        process.setDisplayName(getProcessName().getDisplayName());
        process.setFormClass(StreamResourceProcessForm.class);
        process.setVersion(1);
        process.setHidden(1);

        // Set up the listener
        process.addListener(initStreamListener);
        process.addListener(initStreamFailedListener);
        process.addListener(initStreamCompleteListener);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // Init MQ
        ServiceTask initMQTask = new ServiceTask();
        initMQTask.setName("InitMQ");
        initMQTask.setDisplayName("Stream-InitMQ");
        initMQTask.setServiceTaskType(ServiceTaskType.INIT_MQ);
        initMQTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(initMQTask);

        // Init Sink
        ServiceTask initSinkTask = new ServiceTask();
        initSinkTask.setName("InitSink");
        initSinkTask.setDisplayName("Stream-InitSink");
        initSinkTask.setServiceTaskType(ServiceTaskType.INIT_SINK);
        initSinkTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(initSinkTask);

        // Init Sort
        ServiceTask initSortTask = new ServiceTask();
        initSortTask.setName("InitSort");
        initSortTask.setDisplayName("Stream-InitSort");
        initSortTask.setServiceTaskType(ServiceTaskType.INIT_SORT);
        initSortTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(initSortTask);

        // Init Source
        ServiceTask initSourceTask = new ServiceTask();
        initSourceTask.setName("InitSource");
        initSourceTask.setDisplayName("Stream-InitSource");
        initSourceTask.setServiceTaskType(ServiceTaskType.INIT_SOURCE);
        initSourceTask.setListenerFactory(streamTaskListenerFactory);
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
        return ProcessName.CREATE_STREAM_RESOURCE;
    }

}
