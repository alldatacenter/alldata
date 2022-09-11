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

package org.apache.inlong.manager.workflow.definition;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WorkflowTask
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class WorkflowTask extends NextableElement implements SkippableElement {

    private boolean needAllApprove = false;
    private SkipResolver skipResolver = SkipResolver.DEFAULT_NOT_SKIP;

    private Map<TaskEvent, List<TaskEventListener>> syncListeners = Maps.newHashMap();
    private Map<TaskEvent, List<TaskEventListener>> asyncListeners = Maps.newHashMap();
    private Map<String, TaskEventListener> nameToListenerMap = Maps.newHashMap();

    /**
     * Add listener for workflow task.
     */
    public WorkflowTask addListener(TaskEventListener listener) {
        if (nameToListenerMap.containsKey(listener.name())) {
            throw new WorkflowListenerException(
                    String.format("duplicate listener:%s for task:%s", listener.name(), getName()));
        }
        nameToListenerMap.put(listener.name(), listener);

        if (listener.async()) {
            this.asyncListeners.computeIfAbsent(listener.event(), a -> Lists.newArrayList()).add(listener);
        } else {
            this.syncListeners.computeIfAbsent(listener.event(), a -> Lists.newArrayList()).add(listener);
        }
        return this;
    }

    /**
     * Get async task event listener list.
     */
    public List<TaskEventListener> asyncListeners(TaskEvent taskEvent) {
        return this.asyncListeners.getOrDefault(taskEvent, TaskEventListener.EMPTY_LIST);
    }

    /**
     * Get sync task event listener list.
     */
    public List<TaskEventListener> syncListeners(TaskEvent taskEvent) {
        return this.syncListeners.getOrDefault(taskEvent, TaskEventListener.EMPTY_LIST);
    }

    /**
     * Get task event listener by listener name.
     *
     * @param listenerName listener name.
     * @return task event listener info.
     */
    public TaskEventListener listener(String listenerName) {
        return this.nameToListenerMap.get(listenerName);
    }

    @Override
    public WorkflowTask clone() throws CloneNotSupportedException {
        WorkflowTask cloneTask = (WorkflowTask) super.clone();
        cloneTask.setSyncListeners(new HashMap<>(syncListeners));
        cloneTask.setAsyncListeners(new HashMap<>(asyncListeners));
        cloneTask.setNameToListenerMap(new HashMap<>(nameToListenerMap));
        return cloneTask;
    }

    @Override
    public boolean isSkip(WorkflowContext workflowContext) {
        return Optional.ofNullable(skipResolver)
                .map(skipResolver -> skipResolver.isSkip(workflowContext))
                .orElse(false);
    }

}
