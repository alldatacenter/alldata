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

package org.apache.inlong.manager.workflow.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.EventStatus;
import org.apache.inlong.manager.common.enums.WorkflowEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.util.NetworkUtils;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowEventLogEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.Element;

import java.util.Date;
import java.util.Optional;

/**
 * Event listener with logging function
 */
@Slf4j
public abstract class LogableEventListener<EventType extends WorkflowEvent> implements EventListener<EventType> {

    private final EventListener<EventType> eventListener;
    private final WorkflowEventLogEntityMapper eventLogMapper;

    public LogableEventListener(EventListener<EventType> eventListener, WorkflowEventLogEntityMapper eventLogMapper) {
        this.eventListener = eventListener;
        this.eventLogMapper = eventLogMapper;
    }

    @Override
    public EventType event() {
        return eventListener.event();
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        if (eventListener.ignoreRecordLog()) {
            return executeListenerWithoutLog(context);
        }

        return executeListenerWithLog(context);
    }

    private ListenerResult executeListenerWithoutLog(WorkflowContext context) {
        WorkflowEventLogEntity workflowEventLogEntity = buildEventLog(context);
        try {
            ListenerResult result = eventListener.listen(context);
            log.debug("listener execute result: {} - {}", workflowEventLogEntity, result);
            return result;
        } catch (Exception e) {
            log.error("execute listener " + workflowEventLogEntity + " error: ", e);
            return ListenerResult.fail(e);
        }
    }

    private ListenerResult executeListenerWithLog(WorkflowContext context) {
        WorkflowEventLogEntity logEntity = buildEventLog(context);
        ListenerResult result;
        try {
            result = eventListener.listen(context);
            logEntity.setStatus(
                    result.isSuccess() ? EventStatus.SUCCESS.getStatus() : EventStatus.FAILED.getStatus());
            logEntity.setRemark(result.getRemark());
            logEntity.setException(
                    Optional.ofNullable(result.getException()).map(Exception::getMessage).orElse(null));
        } catch (Exception e) {
            logEntity.setStatus(EventStatus.FAILED.getStatus());
            logEntity.setException(e.getMessage());
            log.error("execute listener " + logEntity + " error: ", e);
            result = ListenerResult.fail(e);
        } finally {
            logEntity.setEndTime(new Date());
            eventLogMapper.insert(logEntity);
        }
        return result;
    }

    /**
     * Build event log.
     */
    protected WorkflowEventLogEntity buildEventLog(WorkflowContext context) {
        WorkflowProcessEntity workflowProcessEntity = context.getProcessEntity();
        Element currentElement = context.getCurrentElement();

        WorkflowEventLogEntity logEntity = new WorkflowEventLogEntity();

        logEntity.setProcessId(workflowProcessEntity.getId());
        logEntity.setProcessName(workflowProcessEntity.getName());
        logEntity.setProcessDisplayName(workflowProcessEntity.getDisplayName());
        logEntity.setInlongGroupId(context.getProcessForm().getInlongGroupId());
        logEntity.setElementName(currentElement.getName());
        logEntity.setElementDisplayName(currentElement.getDisplayName());
        logEntity.setEventType(event().getClass().getSimpleName());
        logEntity.setEvent(event().name());
        logEntity.setListener(eventListener.name());
        logEntity.setStatus(EventStatus.EXECUTING.getStatus());
        logEntity.setAsync(0);
        logEntity.setIp(NetworkUtils.getLocalIp());
        logEntity.setStartTime(new Date());

        return logEntity;
    }

}
