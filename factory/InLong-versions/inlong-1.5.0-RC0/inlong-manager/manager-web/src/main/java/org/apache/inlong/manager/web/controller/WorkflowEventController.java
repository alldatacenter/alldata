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

package org.apache.inlong.manager.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.EventLogRequest;
import org.apache.inlong.manager.pojo.workflow.EventLogResponse;
import org.apache.inlong.manager.service.core.WorkflowEventService;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workflow event related interface
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Workflow-Event-API")
public class WorkflowEventController {

    @Autowired
    private WorkflowEventService workflowEventService;

    @GetMapping("/workflow/event/detail/{id}")
    @ApiOperation(value = "Get event details")
    @ApiImplicitParam(name = "id", value = "Event ID", dataTypeClass = Integer.class, required = true)
    public Response<EventLogResponse> get(@PathVariable Integer id) {
        return Response.success(workflowEventService.get(id));
    }

    @GetMapping("/workflow/event/list")
    @ApiOperation(value = "Get event list by paginating")
    public Response<PageResult<EventLogResponse>> list(EventLogRequest query) {
        return Response.success(workflowEventService.list(query));
    }

    @Deprecated
    @PostMapping("/workflow/event/executeEventListener/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Execute the listener based on the event log ID")
    @ApiImplicitParam(name = "id", value = "Event log ID", dataTypeClass = Integer.class, required = true)
    public Response<Object> executeEventListener(@PathVariable Integer id) {
        workflowEventService.executeEventListener(id);
        return Response.success();
    }

    @Deprecated
    @PostMapping("/workflow/event/executeProcessEventListener")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Re-execute the specified listener based on the process ID")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processId", value = "Process ID", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "listenerName", value = "Listener name", dataTypeClass = String.class)
    })
    public Response<Object> executeProcessEventListener(@RequestParam Integer processId,
            @RequestParam String listenerName) {
        workflowEventService.executeProcessEventListener(processId, listenerName);
        return Response.success();
    }

    @Deprecated
    @PostMapping("/workflow/event/executeTaskEventListener")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Re-execute the specified listener based on the task ID")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "Task ID", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "listenerName", value = "Listener name", dataTypeClass = String.class)
    })
    public Response<Object> executeTaskEventListener(Integer taskId, String listenerName) {
        workflowEventService.executeTaskEventListener(taskId, listenerName);
        return Response.success();
    }

    @Deprecated
    @PostMapping("/workflow/event/triggerProcessEvent")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Re-trigger the process event based on the process ID")
    public Response<Object> triggerProcessEvent(
            @ApiParam(value = "process id", required = true) Integer processId,
            @ApiParam(value = "process event", required = true) ProcessEvent processEvent) {
        workflowEventService.triggerProcessEvent(processId, processEvent);
        return Response.success();
    }

    @Deprecated
    @PostMapping("/workflow/event/triggerTaskEvent")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Re-trigger the task event based on the task ID")
    public Response<Object> triggerTaskEvent(
            @ApiParam(value = "task id", required = true) Integer taskId,
            @ApiParam(value = "task event", required = true) TaskEvent taskEvent) {
        workflowEventService.triggerTaskEvent(taskId, taskEvent);
        return Response.success();
    }

}
