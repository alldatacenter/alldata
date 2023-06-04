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

package org.apache.inlong.manager.workflow.event.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.dao.mapper.WorkflowEventLogEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.event.EventListenerNotifier;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.LogableEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WorkflowProcess event notifier
 */
@Slf4j
@Service
public class TaskEventNotifier implements EventListenerNotifier<TaskEvent> {

    @Autowired
    private WorkflowEventLogEntityMapper eventLogMapper;

    public TaskEventNotifier(WorkflowEventLogEntityMapper eventLogMapper) {
        this.eventLogMapper = eventLogMapper;
    }

    @Override
    public ListenerResult notify(TaskEvent event, WorkflowContext context) {
        WorkflowTask task = (WorkflowTask) context.getCurrentElement();
        List<LogableTaskEventListener> logableListeners = task.listeners(event).stream()
                .map(listener -> logableEventListener(listener))
                .collect(Collectors.toList());

        for (LogableTaskEventListener listener : logableListeners) {
            ListenerResult result = listener.listen(context);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return ListenerResult.success();

    }

    @Override
    public ListenerResult notify(String listenerName, WorkflowContext context) {
        WorkflowTask task = (WorkflowTask) context.getCurrentElement();
        TaskEventListener listener = task.listener(listenerName);
        if (listener == null) {
            return ListenerResult.success();
        }
        return logableEventListener(listener).listen(context);
    }

    private LogableTaskEventListener logableEventListener(TaskEventListener listener) {
        if (listener instanceof LogableEventListener) {
            return (LogableTaskEventListener) listener;
        }
        return new LogableTaskEventListener(listener, eventLogMapper);
    }

}
