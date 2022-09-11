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

package org.apache.inlong.manager.service.workflow.listener;

import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.collections.MapUtils;
import org.apache.inlong.manager.service.mq.CreatePulsarGroupTaskListener;
import org.apache.inlong.manager.service.mq.CreatePulsarResourceTaskListener;
import org.apache.inlong.manager.service.mq.CreateTubeGroupTaskListener;
import org.apache.inlong.manager.service.mq.CreateTubeTopicTaskListener;
import org.apache.inlong.manager.service.mq.PulsarEventSelector;
import org.apache.inlong.manager.service.mq.TubeEventSelector;
import org.apache.inlong.manager.service.resource.SinkResourceListener;
import org.apache.inlong.manager.service.sort.CreateSortConfigListenerV2;
import org.apache.inlong.manager.service.sort.ZookeeperDisabledSelector;
import org.apache.inlong.manager.service.sort.light.LightGroupSortListener;
import org.apache.inlong.manager.service.sort.light.LightGroupSortSelector;
import org.apache.inlong.manager.service.source.listener.SourceDeleteEventSelector;
import org.apache.inlong.manager.service.source.listener.SourceDeleteListener;
import org.apache.inlong.manager.service.source.listener.SourceRestartEventSelector;
import org.apache.inlong.manager.service.source.listener.SourceRestartListener;
import org.apache.inlong.manager.service.source.listener.SourceStopEventSelector;
import org.apache.inlong.manager.service.source.listener.SourceStopListener;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.ServiceTaskListenerProvider;
import org.apache.inlong.manager.workflow.definition.ServiceTaskType;
import org.apache.inlong.manager.workflow.event.EventSelector;
import org.apache.inlong.manager.workflow.event.task.DataSourceOperateListener;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.apache.inlong.manager.workflow.plugin.Plugin;
import org.apache.inlong.manager.workflow.plugin.PluginBinder;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service task listener factory.
 */
@Data
@Component
public class GroupTaskListenerFactory implements PluginBinder, ServiceTaskListenerProvider {

    private Map<DataSourceOperateListener, EventSelector> sourceOperateListeners;

    private Map<QueueOperateListener, EventSelector> queueOperateListeners;

    private Map<SortOperateListener, EventSelector> sortOperateListeners;

    @Autowired
    private SourceStopListener sourceStopListener;
    @Autowired
    private SourceRestartListener sourceRestartListener;
    @Autowired
    private SourceDeleteListener sourceDeleteListener;

    @Autowired
    private CreateTubeTopicTaskListener createTubeTopicTaskListener;
    @Autowired
    private CreateTubeGroupTaskListener createTubeGroupTaskListener;
    @Autowired
    private CreatePulsarResourceTaskListener createPulsarResourceTaskListener;
    @Autowired
    private CreatePulsarGroupTaskListener createPulsarGroupTaskListener;

    @Autowired
    private SinkResourceListener sinkResourceListener;

    @Autowired
    private CreateSortConfigListenerV2 createSortConfigListener;

    @Autowired
    private LightGroupSortListener lightGroupSortListener;

    @PostConstruct
    public void init() {
        sourceOperateListeners = new LinkedHashMap<>();
        sourceOperateListeners.put(sourceStopListener, new SourceStopEventSelector());
        sourceOperateListeners.put(sourceDeleteListener, new SourceDeleteEventSelector());
        sourceOperateListeners.put(sourceRestartListener, new SourceRestartEventSelector());
        queueOperateListeners = new LinkedHashMap<>();
        queueOperateListeners.put(createTubeTopicTaskListener, new TubeEventSelector());
        queueOperateListeners.put(createTubeGroupTaskListener, new TubeEventSelector());
        queueOperateListeners.put(createPulsarResourceTaskListener, new PulsarEventSelector());
        queueOperateListeners.put(createPulsarGroupTaskListener, new PulsarEventSelector());
        sortOperateListeners = new LinkedHashMap<>();
        sortOperateListeners.put(createSortConfigListener, new ZookeeperDisabledSelector());
        sortOperateListeners.put(lightGroupSortListener, new LightGroupSortSelector());
    }

    public void clearListeners() {
        sourceOperateListeners = new LinkedHashMap<>();
        queueOperateListeners = new LinkedHashMap<>();
        sortOperateListeners = new LinkedHashMap<>();
    }

    @Override
    public List<TaskEventListener> get(WorkflowContext workflowContext, ServiceTaskType serviceTaskType) {
        switch (serviceTaskType) {
            case INIT_MQ:
                List<QueueOperateListener> queueOperateListeners = getQueueOperateListener(workflowContext);
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
                List<DataSourceOperateListener> sourceOperateListeners = getSourceOperateListener(workflowContext);
                return Lists.newArrayList(sourceOperateListeners);
            case INIT_SINK:
                return Collections.singletonList(sinkResourceListener);
            default:
                throw new IllegalArgumentException(String.format("UnSupport ServiceTaskType %s", serviceTaskType));
        }
    }

    /**
     * Get data source operate listener list.
     */
    public List<DataSourceOperateListener> getSourceOperateListener(WorkflowContext context) {
        List<DataSourceOperateListener> listeners = new ArrayList<>();
        for (Map.Entry<DataSourceOperateListener, EventSelector> entry : sourceOperateListeners.entrySet()) {
            EventSelector selector = entry.getValue();
            if (selector != null && selector.accept(context)) {
                listeners.add(entry.getKey());
            }
        }
        return listeners;
    }

    /**
     * Get queue operate listener list.
     */
    public List<QueueOperateListener> getQueueOperateListener(WorkflowContext context) {
        List<QueueOperateListener> listeners = new ArrayList<>();
        for (Map.Entry<QueueOperateListener, EventSelector> entry : queueOperateListeners.entrySet()) {
            EventSelector selector = entry.getValue();
            if (selector != null && selector.accept(context)) {
                listeners.add(entry.getKey());
            }
        }
        return listeners;
    }

    /**
     * Get sort operate listener list.
     */
    public List<SortOperateListener> getSortOperateListener(WorkflowContext context) {
        List<SortOperateListener> listeners = new ArrayList<>();
        for (Map.Entry<SortOperateListener, EventSelector> entry : sortOperateListeners.entrySet()) {
            EventSelector selector = entry.getValue();
            if (selector != null && selector.accept(context)) {
                listeners.add(entry.getKey());
            }
        }
        return listeners;
    }

    @Override
    public void acceptPlugin(Plugin plugin) {
        if (!(plugin instanceof ProcessPlugin)) {
            return;
        }
        ProcessPlugin processPlugin = (ProcessPlugin) plugin;
        Map<DataSourceOperateListener, EventSelector> pluginDsOperateListeners =
                processPlugin.createSourceOperateListeners();
        if (MapUtils.isNotEmpty(pluginDsOperateListeners)) {
            sourceOperateListeners.putAll(processPlugin.createSourceOperateListeners());
        }
        Map<QueueOperateListener, EventSelector> pluginQueueOperateListeners =
                processPlugin.createQueueOperateListeners();
        if (MapUtils.isNotEmpty(pluginQueueOperateListeners)) {
            queueOperateListeners.putAll(pluginQueueOperateListeners);
        }
        Map<SortOperateListener, EventSelector> pluginSortOperateListeners =
                processPlugin.createSortOperateListeners();
        if (MapUtils.isNotEmpty(pluginSortOperateListeners)) {
            sortOperateListeners.putAll(pluginSortOperateListeners);
        }
    }

}
