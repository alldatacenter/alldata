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
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowTask
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class WorkflowTask extends NextableElement {

    private boolean needAllApprove = false;

    private Map<TaskEvent, List<TaskEventListener>> listeners = Maps.newHashMap();
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

        listeners.computeIfAbsent(listener.event(), a -> Lists.newArrayList()).add(listener);
        return this;
    }

    /**
     * Get sync task event listener list.
     */
    public List<TaskEventListener> listeners(TaskEvent taskEvent) {
        return this.listeners.getOrDefault(taskEvent, TaskEventListener.EMPTY_LISTENERS);
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
        cloneTask.setListeners(new HashMap<>(listeners));
        cloneTask.setNameToListenerMap(new HashMap<>(nameToListenerMap));
        return cloneTask;
    }

}
