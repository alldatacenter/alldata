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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WorkflowProcess definition
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WorkflowProcess extends Element {

    private String type;

    private StartEvent startEvent;

    private EndEvent endEvent;

    @Getter
    private Map<String, WorkflowTask> nameToTaskMap = Maps.newHashMap();

    private Class<? extends ProcessForm> formClass;

    private ProcessDetailHandler processDetailHandler;
    /**
     * Whether to hide, for example, some processes initiated by the system
     */
    private Integer hidden = 0;

    private Map<ProcessEvent, List<ProcessEventListener>> listeners = Maps.newHashMap();
    private Map<String, ProcessEventListener> nameToListenerMap = Maps.newHashMap();

    private int version;

    /**
     * Add listener to workflow process.
     */
    public WorkflowProcess addListener(ProcessEventListener listener) {
        if (nameToListenerMap.containsKey(listener.name())) {
            throw new WorkflowListenerException("duplicate listener:" + listener.name());
        }
        nameToListenerMap.put(listener.name(), listener);
        listeners.computeIfAbsent(listener.event(), a -> Lists.newArrayList()).add(listener);
        return this;
    }

    /**
     * Get sync process event listener list.
     */
    public List<ProcessEventListener> listeners(ProcessEvent processEvent) {
        return this.listeners.getOrDefault(processEvent, ProcessEventListener.EMPTY_LIST);
    }

    /**
     * Get process event listener by listener name.
     *
     * @param listenerName listener name.
     * @return process event listener.
     */
    public ProcessEventListener listener(String listenerName) {
        return this.nameToListenerMap.get(listenerName);
    }

    /**
     * Add workflow task.
     */
    public WorkflowProcess addTask(WorkflowTask task) {
        if (this.nameToTaskMap.containsKey(task.getName())) {
            throw new WorkflowException("task name cannot duplicate " + task.getName());
        }
        this.nameToTaskMap.put(task.getName(), task);
        return this;
    }

    /**
     * Get workflow task by task name.
     *
     * @param name workflow task name.
     * @return workflow task info.
     */
    public WorkflowTask getTaskByName(String name) {
        if (!this.nameToTaskMap.containsKey(name)) {
            throw new WorkflowException("cannot find task with the name " + name);
        }
        return nameToTaskMap.get(name);
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkNotEmpty(type, "process type cannot be empty");
        Preconditions.checkNotNull(startEvent, "start event cannot be null");
        Preconditions.checkNotNull(endEvent, "end event cannot be null");

        startEvent.validate();
        endEvent.validate();

        nameToTaskMap.values().forEach(WorkflowTask::validate);
    }

    @Override
    public WorkflowProcess clone() throws CloneNotSupportedException {
        WorkflowProcess cloneProcess = (WorkflowProcess) super.clone();
        cloneProcess.setStartEvent((StartEvent) this.startEvent.clone());
        cloneProcess.setEndEvent((EndEvent) this.endEvent.clone());
        Map<String, WorkflowTask> nameToTaskMap = new HashMap<>();

        StartEvent startEvent = cloneProcess.getStartEvent();
        Map<WorkflowAction, List<ConditionNextElement>> workflowActionListMap = startEvent.getActionToNextElementMap();
        Queue<Map<WorkflowAction, List<ConditionNextElement>>> queue = new LinkedBlockingQueue<>();
        if (MapUtils.isNotEmpty(workflowActionListMap)) {
            queue.add(workflowActionListMap);
        }
        while (!queue.isEmpty()) {
            workflowActionListMap = queue.remove();
            for (List<ConditionNextElement> conditionNextElements : workflowActionListMap.values()) {
                for (ConditionNextElement conditionNextElement : conditionNextElements) {
                    Element element = conditionNextElement.getElement();
                    if (element instanceof WorkflowTask) {
                        WorkflowTask workflowTask = (WorkflowTask) element;
                        nameToTaskMap.put(workflowTask.getName(), workflowTask);
                    }
                    if (element instanceof NextableElement) {
                        NextableElement nextableElement = (NextableElement) element;
                        Map<WorkflowAction, List<ConditionNextElement>> childListMap =
                                nextableElement.getActionToNextElementMap();
                        if (MapUtils.isNotEmpty(childListMap)) {
                            queue.add(childListMap);
                        }
                    }
                }
            }
        }

        cloneProcess.setNameToTaskMap(nameToTaskMap);
        Map<ProcessEvent, List<ProcessEventListener>> cloneListeners = Maps.newHashMap();
        cloneListeners.putAll(listeners);
        cloneProcess.listeners = cloneListeners;

        return cloneProcess;
    }

}
