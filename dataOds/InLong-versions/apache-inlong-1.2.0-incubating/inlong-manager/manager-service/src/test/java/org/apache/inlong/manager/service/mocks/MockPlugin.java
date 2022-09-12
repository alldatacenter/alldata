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

import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.workflow.event.EventSelector;
import org.apache.inlong.manager.workflow.event.task.DataSourceOperateListener;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for process plugin.
 */
public class MockPlugin implements ProcessPlugin {

    public EventSelector stopProcessSelector = new EventSelector() {
        @Override
        public boolean accept(WorkflowContext context) {
            ProcessForm processForm = context.getProcessForm();
            if (!(processForm instanceof GroupResourceProcessForm)) {
                return false;
            }
            GroupResourceProcessForm form = (GroupResourceProcessForm) processForm;
            return form.getGroupOperateType() == GroupOperateType.SUSPEND;
        }
    };

    public EventSelector restartProcessSelector = new EventSelector() {
        @Override
        public boolean accept(WorkflowContext context) {
            ProcessForm processForm = context.getProcessForm();
            if (!(processForm instanceof GroupResourceProcessForm)) {
                return false;
            }
            GroupResourceProcessForm form = (GroupResourceProcessForm) processForm;
            return form.getGroupOperateType() == GroupOperateType.RESTART;
        }
    };

    public EventSelector deleteProcessSelector = new EventSelector() {
        @Override
        public boolean accept(WorkflowContext context) {
            ProcessForm processForm = context.getProcessForm();
            if (!(processForm instanceof GroupResourceProcessForm)) {
                return false;
            }
            GroupResourceProcessForm form = (GroupResourceProcessForm) processForm;
            return form.getGroupOperateType() == GroupOperateType.DELETE;
        }
    };

    @Override
    public Map<DataSourceOperateListener, EventSelector> createSourceOperateListeners() {
        Map<DataSourceOperateListener, EventSelector> listeners = new HashMap<>();
        listeners.put(new MockDeleteSourceListener(), deleteProcessSelector);
        listeners.put(new MockRestartSourceListener(), restartProcessSelector);
        listeners.put(new MockStopSourceListener(), stopProcessSelector);
        return listeners;
    }

    @Override
    public Map<SortOperateListener, EventSelector> createSortOperateListeners() {
        Map<SortOperateListener, EventSelector> listeners = new HashMap<>();
        listeners.put(new MockDeleteSortListener(), deleteProcessSelector);
        listeners.put(new MockRestartSortListener(), restartProcessSelector);
        listeners.put(new MockStopSortListener(), stopProcessSelector);
        return listeners;
    }

}
