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

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskExecuteLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowTaskRequest;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
import org.apache.inlong.manager.service.workflow.WorkflowExecuteLog;
import org.apache.inlong.manager.service.workflow.WorkflowOperation;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workflow controller
 */
@Slf4j
@RestController
@RequestMapping("/workflow")
@Api(tags = "Workflow Config")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/start")
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Initiation process")
    public Response<WorkflowResult> start(@RequestBody WorkflowOperation operation) {
        String applicant = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.start(operation.getName(), applicant, operation.getForm()));
    }

    @PostMapping("/cancel/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Cancellation process")
    @ApiImplicitParam(name = "id", value = "WorkflowProcess ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> cancel(@PathVariable Integer id, @RequestBody WorkflowOperation operation) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.cancel(id, operator, operation.getRemark()));
    }

    @PostMapping("/approve/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Approval and consent")
    @ApiImplicitParam(name = "id", value = "WorkflowTask ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> approve(@PathVariable Integer id, @RequestBody WorkflowTaskRequest operation) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.approve(id, operation.getRemark(), operation.getForm(), operator));
    }

    @PostMapping("/reject/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Approval rejected")
    @ApiImplicitParam(name = "id", value = "WorkflowTask ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> reject(@PathVariable Integer id, @RequestBody WorkflowTaskRequest operation) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.reject(id, operation.getRemark(), operator));
    }

    @PostMapping("/transfer/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Turn to do", notes = "Change approver")
    @ApiImplicitParam(name = "id", value = "WorkflowTask ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> transfer(@PathVariable Integer id, @RequestBody WorkflowTaskRequest operation) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.transfer(id, operation.getRemark(),
                operation.getTransferTo(), operator));
    }

    @PostMapping("/complete/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Finish task by id")
    @ApiImplicitParam(name = "id", value = "WorkflowTask ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> complete(@PathVariable Integer id, @RequestBody WorkflowTaskRequest request) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.complete(id, request.getRemark(), operator));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation(value = "Get process detail")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "Process ID", dataTypeClass = Integer.class, required = true),
            @ApiImplicitParam(name = "taskId", value = "Task ID", dataTypeClass = Integer.class)
    })
    public Response<ProcessDetailResponse> detail(@PathVariable(name = "id") Integer id,
            @RequestParam(required = false) Integer taskId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(workflowService.detail(id, taskId, operator));
    }

    @GetMapping("/listProcess")
    @ApiOperation(value = "List my processes")
    public Response<PageInfo<ProcessResponse>> listProcess(ProcessQuery query) {
        query.setApplicant(LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(workflowService.listProcess(query));
    }

    @GetMapping("/listTask")
    @ApiOperation(value = "List my tasks")
    public Response<PageInfo<TaskResponse>> listTask(TaskQuery query) {
        query.setApprover(LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(workflowService.listTask(query));
    }

    @GetMapping("/processSummary")
    @ApiOperation(value = "Get process statistics")
    public Response<ProcessCountResponse> processSummary(ProcessCountQuery query) {
        query.setApplicant(LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(workflowService.countProcess(query));
    }

    @GetMapping("/taskSummary")
    @ApiOperation(value = "Get task statistics")
    public Response<TaskCountResponse> taskSummary(TaskCountQuery query) {
        query.setApprover(LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(workflowService.countTask(query));
    }

    @GetMapping("/listTaskExecuteLogs")
    @ApiOperation(value = "Get task execution log")
    public Response<PageInfo<WorkflowExecuteLog>> listTaskExecuteLogs(TaskExecuteLogQuery query) {
        return Response.success(workflowService.listTaskExecuteLogs(query));
    }

}
