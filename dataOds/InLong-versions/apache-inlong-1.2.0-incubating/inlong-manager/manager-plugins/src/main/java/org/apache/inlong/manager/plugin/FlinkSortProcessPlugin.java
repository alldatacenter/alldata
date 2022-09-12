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

package org.apache.inlong.manager.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.plugin.eventselect.DeleteProcessSelector;
import org.apache.inlong.manager.plugin.eventselect.DeleteStreamSelector;
import org.apache.inlong.manager.plugin.eventselect.RestartProcessSelector;
import org.apache.inlong.manager.plugin.eventselect.RestartStreamSelector;
import org.apache.inlong.manager.plugin.eventselect.StartupProcessSelector;
import org.apache.inlong.manager.plugin.eventselect.StartupStreamSelector;
import org.apache.inlong.manager.plugin.eventselect.SuspendProcessSelector;
import org.apache.inlong.manager.plugin.eventselect.SuspendStreamSelector;
import org.apache.inlong.manager.plugin.listener.DeleteSortListener;
import org.apache.inlong.manager.plugin.listener.DeleteStreamListener;
import org.apache.inlong.manager.plugin.listener.RestartSortListener;
import org.apache.inlong.manager.plugin.listener.RestartStreamListener;
import org.apache.inlong.manager.plugin.listener.StartupSortListener;
import org.apache.inlong.manager.plugin.listener.StartupStreamListener;
import org.apache.inlong.manager.plugin.listener.SuspendSortListener;
import org.apache.inlong.manager.plugin.listener.SuspendStreamListener;
import org.apache.inlong.manager.workflow.event.EventSelector;
import org.apache.inlong.manager.workflow.event.task.DataSourceOperateListener;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plugin of flink sort process.
 */
@Slf4j
public class FlinkSortProcessPlugin implements ProcessPlugin {

    @Override
    public Map<DataSourceOperateListener, EventSelector> createSourceOperateListeners() {
        return new LinkedHashMap<>();
    }

    @Override
    public Map<SortOperateListener, EventSelector> createSortOperateListeners() {
        Map<SortOperateListener, EventSelector> listeners = new LinkedHashMap<>();
        listeners.put(new DeleteSortListener(), new DeleteProcessSelector());
        listeners.put(new RestartSortListener(), new RestartProcessSelector());
        listeners.put(new SuspendSortListener(), new SuspendProcessSelector());
        listeners.put(new StartupSortListener(), new StartupProcessSelector());
        listeners.put(new DeleteStreamListener(), new DeleteStreamSelector());
        listeners.put(new RestartStreamListener(), new RestartStreamSelector());
        listeners.put(new SuspendStreamListener(), new SuspendStreamSelector());
        listeners.put(new StartupStreamListener(), new StartupStreamSelector());
        return listeners;
    }
}
