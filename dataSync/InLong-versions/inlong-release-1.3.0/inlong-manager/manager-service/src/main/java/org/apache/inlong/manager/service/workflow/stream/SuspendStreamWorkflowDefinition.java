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
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowDefinition;
import org.apache.inlong.manager.service.listener.StreamTaskListenerFactory;
import org.apache.inlong.manager.service.listener.stream.UpdateStreamCompleteListener;
import org.apache.inlong.manager.service.listener.stream.UpdateStreamFailedListener;
import org.apache.inlong.manager.service.listener.stream.UpdateStreamListener;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Suspend inlong stream process definition
 */
@Slf4j
@Component
public class SuspendStreamWorkflowDefinition implements WorkflowDefinition {

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
        process.addListener(updateStreamFailedListener);
        process.addListener(updateStreamCompleteListener);

        // Start node
        StartEvent startEvent = new StartEvent();
        process.setStartEvent(startEvent);

        // Stop Source
        ServiceTask stopDataSourceTask = new ServiceTask();
        stopDataSourceTask.setName("StopSource");
        stopDataSourceTask.setDisplayName("Stream-StopSource");
        stopDataSourceTask.setServiceTaskType(ServiceTaskType.STOP_SOURCE);
        stopDataSourceTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(stopDataSourceTask);

        // Stop Sort
        ServiceTask stopSortTask = new ServiceTask();
        stopSortTask.setName("StopSort");
        stopSortTask.setDisplayName("Stream-StopSort");
        stopSortTask.setServiceTaskType(ServiceTaskType.STOP_SORT);
        stopSortTask.setListenerFactory(streamTaskListenerFactory);
        process.addTask(stopSortTask);

        // End node
        EndEvent endEvent = new EndEvent();
        process.setEndEvent(endEvent);

        startEvent.addNext(stopDataSourceTask);
        stopDataSourceTask.addNext(stopSortTask);
        stopSortTask.addNext(endEvent);

        return process;
    }

    @Override
    public ProcessName getProcessName() {
        return ProcessName.SUSPEND_STREAM_RESOURCE;
    }
}
