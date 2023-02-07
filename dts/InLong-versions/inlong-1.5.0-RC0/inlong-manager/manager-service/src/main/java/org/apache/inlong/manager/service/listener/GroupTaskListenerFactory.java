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

package org.apache.inlong.manager.service.listener;

import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.service.listener.queue.QueueResourceListener;
import org.apache.inlong.manager.service.listener.sink.SinkResourceListener;
import org.apache.inlong.manager.service.listener.sort.SortConfigListener;
import org.apache.inlong.manager.service.listener.source.SourceDeleteListener;
import org.apache.inlong.manager.service.listener.source.SourceRestartListener;
import org.apache.inlong.manager.service.listener.source.SourceStopListener;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.definition.TaskListenerFactory;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.SourceOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.apache.inlong.manager.common.plugin.Plugin;
import org.apache.inlong.manager.common.plugin.PluginBinder;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The TaskEventListener factory for InlongGroup.
 */
@Data
@Component
public class GroupTaskListenerFactory implements PluginBinder, TaskListenerFactory {

    private List<SourceOperateListener> sourceOperateListeners;
    private List<QueueOperateListener> queueOperateListeners;
    private List<SortOperateListener> sortOperateListeners;

    @Autowired
    private SourceStopListener sourceStopListener;
    @Autowired
    private SourceRestartListener sourceRestartListener;
    @Autowired
    private SourceDeleteListener sourceDeleteListener;
    @Autowired
    private QueueResourceListener queueResourceListener;
    @Autowired
    private SinkResourceListener sinkResourceListener;
    @Autowired
    private SortConfigListener sortConfigListener;

    @PostConstruct
    public void init() {
        sourceOperateListeners = new LinkedList<>();
        sourceOperateListeners.add(sourceStopListener);
        sourceOperateListeners.add(sourceDeleteListener);
        sourceOperateListeners.add(sourceRestartListener);
        queueOperateListeners = new LinkedList<>();
        queueOperateListeners.add(queueResourceListener);
        sortOperateListeners = new LinkedList<>();
        sortOperateListeners.add(sortConfigListener);
    }

    @Override
    public void acceptPlugin(Plugin plugin) {
        if (!(plugin instanceof ProcessPlugin)) {
            return;
        }
        ProcessPlugin processPlugin = (ProcessPlugin) plugin;
        List<SourceOperateListener> pluginSourceOperateListeners = processPlugin.createSourceOperateListeners();
        if (CollectionUtils.isNotEmpty(pluginSourceOperateListeners)) {
            sourceOperateListeners.addAll(processPlugin.createSourceOperateListeners());
        }
        List<QueueOperateListener> pluginQueueOperateListeners = processPlugin.createQueueOperateListeners();
        if (CollectionUtils.isNotEmpty(pluginQueueOperateListeners)) {
            queueOperateListeners.addAll(pluginQueueOperateListeners);
        }
        List<SortOperateListener> pluginSortOperateListeners = processPlugin.createSortOperateListeners();
        if (CollectionUtils.isNotEmpty(pluginSortOperateListeners)) {
            sortOperateListeners.addAll(pluginSortOperateListeners);
        }
    }

    @Override
    public List<? extends TaskEventListener> get(WorkflowContext workflowContext, ServiceTaskType taskType) {
        switch (taskType) {
            case INIT_MQ:
            case DELETE_MQ:
                List<QueueOperateListener> queueOperateListeners = getQueueResourceListener(workflowContext);
                return Lists.newArrayList(queueOperateListeners);
            case INIT_SORT:
            case STOP_SORT:
            case RESTART_SORT:
            case DELETE_SORT:
                List<SortOperateListener> sortOperateListeners = getSortOperateListener(workflowContext);
                return Lists.newArrayList(sortOperateListeners);
            case INIT_SOURCE:
            case STOP_SOURCE:
            case RESTART_SOURCE:
            case DELETE_SOURCE:
                List<SourceOperateListener> sourceOperateListeners = getSourceOperateListener(workflowContext);
                return Lists.newArrayList(sourceOperateListeners);
            case INIT_SINK:
                return Collections.singletonList(sinkResourceListener);
            default:
                throw new IllegalArgumentException(String.format("Unsupported ServiceTaskType %s", taskType));
        }
    }

    /**
     * Clear the list of listeners.
     */
    public void clearListeners() {
        sourceOperateListeners = new LinkedList<>();
        queueOperateListeners = new LinkedList<>();
        sortOperateListeners = new LinkedList<>();
    }

    /**
     * Get stream source operate listener list.
     */
    public List<SourceOperateListener> getSourceOperateListener(WorkflowContext context) {
        List<SourceOperateListener> listeners = new ArrayList<>();
        for (SourceOperateListener listener : sourceOperateListeners) {
            if (listener != null && listener.accept(context)) {
                listeners.add(listener);
            }
        }
        return listeners;
    }

    /**
     * Get queue operate listener list.
     */
    public List<QueueOperateListener> getQueueResourceListener(WorkflowContext context) {
        List<QueueOperateListener> listeners = new ArrayList<>();
        for (QueueOperateListener listener : queueOperateListeners) {
            if (listener != null && listener.accept(context)) {
                listeners.add(listener);
            }
        }
        return listeners;
    }

    /**
     * Get sort operate listener list.
     */
    public List<SortOperateListener> getSortOperateListener(WorkflowContext context) {
        List<SortOperateListener> listeners = new ArrayList<>();
        for (SortOperateListener listener : sortOperateListeners) {
            if (listener != null && listener.accept(context)) {
                listeners.add(listener);
            }
        }
        return listeners;
    }

}
