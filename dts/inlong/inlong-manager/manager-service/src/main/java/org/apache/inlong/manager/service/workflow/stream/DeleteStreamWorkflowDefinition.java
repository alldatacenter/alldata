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
import org.apache.inlong.manager.service.listener.stream.UpdateStreamCompleteListener;
import org.apache.inlong.manager.service.listener.stream.UpdateStreamFailedListener;
import org.apache.inlong.manager.service.listener.stream.UpdateStreamListener;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Delete workflow definition for inlong stream
 */
@Slf4j
@Component
public class DeleteStreamWorkflowDefinition implements WorkflowDefinition {

    @Autowired
    private UpdateStreamListener updateStreamListener;
    @Autowired
    private UpdateStreamCompleteListener updateStreamCompleteListener;
    @Autowired
    private UpdateStreamFailedListener updateStreamFailedListener;
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
        process.addListener(updateStreamListener);
        process.addListener(updateStreamCompleteListener);
        process.addListener(updateStreamFailedListener);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // Delete Source
        ServiceTask deleteDataSourceTask = new ServiceTask();
        deleteDataSourceTask.setName("DeleteSource");
        deleteDataSourceTask.setDisplayName("Stream-DeleteSource");
        deleteDataSourceTask.setServiceTaskType(ServiceTaskType.DELETE_SOURCE);
        deleteDataSourceTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(deleteDataSourceTask);

        // Delete MQ
        ServiceTask deleteMQTask = new ServiceTask();
        deleteMQTask.setName("DeleteMQ");
        deleteMQTask.setDisplayName("Stream-DeleteMQ");
        deleteMQTask.setServiceTaskType(ServiceTaskType.DELETE_MQ);
        deleteMQTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(deleteMQTask);

        // Delete Sort
        ServiceTask deleteSortTask = new ServiceTask();
        deleteSortTask.setName("DeleteSort");
        deleteSortTask.setDisplayName("Stream-DeleteSort");
        deleteSortTask.setServiceTaskType(ServiceTaskType.DELETE_SORT);
        deleteSortTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(deleteSortTask);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        startEvent.addNext(deleteDataSourceTask);
        deleteDataSourceTask.addNext(deleteMQTask);
        deleteMQTask.addNext(deleteSortTask);
        deleteSortTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.DELETE_STREAM_RESOURCE;
    }
}
