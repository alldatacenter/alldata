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

package org.apache.inlong.manager.service.mocks;

import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.SourceOperateListener;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for process plugin.
 */
public class MockPlugin implements ProcessPlugin {

    @Override
    public List<SourceOperateListener> createSourceOperateListeners() {
        List<SourceOperateListener> listeners = new ArrayList<>();
        listeners.add(new MockDeleteSourceListener());
        listeners.add(new MockRestartSourceListener());
        listeners.add(new MockStopSourceListener());
        return listeners;
    }

    @Override
    public List<SortOperateListener> createSortOperateListeners() {
        List<SortOperateListener> listeners = new ArrayList<>();
        listeners.add(new MockDeleteSortListener());
        listeners.add(new MockRestartSortListener());
        listeners.add(new MockStopSortListener());
        return listeners;
    }

}
