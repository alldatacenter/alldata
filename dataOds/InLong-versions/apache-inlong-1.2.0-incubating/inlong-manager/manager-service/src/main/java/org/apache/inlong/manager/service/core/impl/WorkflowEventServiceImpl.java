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

package org.apache.inlong.manager.service.core.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.workflow.EventLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.EventLogView;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.service.core.WorkflowEventService;
import org.apache.inlong.manager.workflow.core.EventListenerService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Workflow event related services
 */
@Service
public class WorkflowEventServiceImpl implements WorkflowEventService {

    @Autowired
    private WorkflowQueryService queryService;
    @Autowired
    private EventListenerService eventListenerService;

    @Override
    public EventLogView get(Integer id) {
        WorkflowEventLogEntity workflowEventLogEntity = queryService.getEventLog(id);
        return CommonBeanUtils.copyProperties(workflowEventLogEntity, EventLogView::new);
    }

    @Override
    public PageInfo<EventLogView> list(EventLogQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowEventLogEntity> page = (Page<WorkflowEventLogEntity>) queryService.listEventLog(query);

        List<EventLogView> viewList = CommonBeanUtils.copyListProperties(page, EventLogView::new);
        PageInfo<EventLogView> pageInfo = new PageInfo<>(viewList);
        pageInfo.setTotal(page.getTotal());

        return pageInfo;
    }

    @Override
    public void executeEventListener(Integer eventLogId) {
        eventListenerService.executeEventListener(eventLogId);
    }

    @Override
    public void executeProcessEventListener(Integer processId, String listenerName) {
        eventListenerService.executeProcessEventListener(processId, listenerName);
    }

    @Override
    public void executeTaskEventListener(Integer taskId, String listenerName) {
        eventListenerService.executeTaskEventListener(taskId, listenerName);
    }

    @Override
    public void triggerProcessEvent(Integer processId, ProcessEvent processEvent) {
        eventListenerService.triggerProcessEvent(processId, processEvent);
    }

    @Override
    public void triggerTaskEvent(Integer taskId, TaskEvent taskEvent) {
        eventListenerService.triggerTaskEvent(taskId, taskEvent);
    }
}
