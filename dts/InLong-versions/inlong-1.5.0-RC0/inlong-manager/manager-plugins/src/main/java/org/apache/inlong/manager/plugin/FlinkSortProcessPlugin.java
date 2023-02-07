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
import org.apache.inlong.manager.plugin.listener.DeleteSortListener;
import org.apache.inlong.manager.plugin.listener.DeleteStreamListener;
import org.apache.inlong.manager.plugin.listener.RestartSortListener;
import org.apache.inlong.manager.plugin.listener.RestartStreamListener;
import org.apache.inlong.manager.plugin.listener.StartupSortListener;
import org.apache.inlong.manager.plugin.listener.StartupStreamListener;
import org.apache.inlong.manager.plugin.listener.SuspendSortListener;
import org.apache.inlong.manager.plugin.listener.SuspendStreamListener;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.SourceOperateListener;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;

import java.util.LinkedList;
import java.util.List;

/**
 * Plugin of Flink Sort process.
 */
@Slf4j
public class FlinkSortProcessPlugin implements ProcessPlugin {

    @Override
    public List<SourceOperateListener> createSourceOperateListeners() {
        return new LinkedList<>();
    }

    @Override
    public List<SortOperateListener> createSortOperateListeners() {
        List<SortOperateListener> listeners = new LinkedList<>();
        listeners.add(new DeleteSortListener());
        listeners.add(new RestartSortListener());
        listeners.add(new SuspendSortListener());
        listeners.add(new StartupSortListener());
        listeners.add(new DeleteStreamListener());
        listeners.add(new RestartStreamListener());
        listeners.add(new SuspendStreamListener());
        listeners.add(new StartupStreamListener());
        return listeners;
    }

}
