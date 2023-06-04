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
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.workflow.EventLogRequest;
import org.apache.inlong.manager.pojo.workflow.EventLogResponse;
import org.apache.inlong.manager.service.core.WorkflowEventService;
import org.apache.inlong.manager.workflow.core.EventListenerService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
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
    public EventLogResponse get(Integer id) {
        WorkflowEventLogEntity workflowEventLogEntity = queryService.getEventLog(id);
        return CommonBeanUtils.copyProperties(workflowEventLogEntity, EventLogResponse::new);
    }

    @Override
    public PageResult<EventLogResponse> list(EventLogRequest query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowEventLogEntity> page = (Page<WorkflowEventLogEntity>) queryService.listEventLog(query);

        List<EventLogResponse> viewList = CommonBeanUtils.copyListProperties(page, EventLogResponse::new);

        return new PageResult<>(viewList, page.getTotal(), page.getPageNum(), page.getPageSize());
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
