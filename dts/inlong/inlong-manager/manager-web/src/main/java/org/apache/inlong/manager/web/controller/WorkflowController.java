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
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.ProcessCountRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskCountRequest;
import org.apache.inlong.manager.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.pojo.workflow.TaskLogRequest;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowApprovalRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowExecuteLog;
import org.apache.inlong.manager.pojo.workflow.WorkflowOperationRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.LoginUserUtils;
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
@RequestMapping("/api")
@Api(tags = "Workflow-API")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/workflow/start")
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Initiation process")
    public Response<WorkflowResult> start(@RequestBody WorkflowOperationRequest request) {
        String applicant = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.start(request.getName(), applicant, request.getForm()));
    }

    @PostMapping("/workflow/cancel/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Cancellation process")
    @ApiImplicitParam(name = "id", value = "Process ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> cancel(@PathVariable Integer id, @RequestBody WorkflowOperationRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.cancel(id, operator, request.getRemark()));
    }

    @PostMapping("/workflow/continue/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Continue process")
    @ApiImplicitParam(name = "id", value = "Process ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> continueProcess(@PathVariable Integer id,
            @RequestBody WorkflowOperationRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.continueProcess(id, operator, request.getRemark()));
    }

    @PostMapping("/workflow/approve/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Approval and consent")
    @ApiImplicitParam(name = "id", value = "Task ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> approve(@PathVariable Integer id, @RequestBody WorkflowApprovalRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.approve(id, request.getRemark(), request.getForm(), operator));
    }

    @PostMapping("/workflow/reject/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Approval rejected")
    @ApiImplicitParam(name = "id", value = "Task ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> reject(@PathVariable Integer id, @RequestBody WorkflowApprovalRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.reject(id, request.getRemark(), operator));
    }

    @PostMapping("/workflow/transfer/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Turn to another approver", notes = "Change approver")
    @ApiImplicitParam(name = "id", value = "Task ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> transfer(@PathVariable Integer id, @RequestBody WorkflowApprovalRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.transfer(id, request.getRemark(),
                request.getTransferTo(), operator));
    }

    @PostMapping("/workflow/complete/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Complete task by ID")
    @ApiImplicitParam(name = "id", value = "Task ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> complete(@PathVariable Integer id, @RequestBody WorkflowApprovalRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.complete(id, request.getRemark(), operator));
    }

    @GetMapping("/workflow/detail/{id}")
    @ApiOperation(value = "Get process detail")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "Process ID", dataTypeClass = Integer.class, required = true),
            @ApiImplicitParam(name = "taskId", value = "Task ID", dataTypeClass = Integer.class, example = "1")
    })
    public Response<ProcessDetailResponse> detail(@PathVariable(name = "id") Integer id,
            @RequestParam(required = false) Integer taskId) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(workflowService.detail(id, taskId, operator));
    }

    @GetMapping("/workflow/listProcess")
    @ApiOperation(value = "Get process list by paginating")
    public Response<PageResult<ProcessResponse>> listProcess(ProcessRequest query) {
        query.setApplicant(LoginUserUtils.getLoginUser().getName());
        return Response.success(workflowService.listProcess(query));
    }

    @GetMapping("/workflow/listTask")
    @ApiOperation(value = "Get task list by paginating")
    public Response<PageResult<TaskResponse>> listTask(TaskRequest query) {
        query.setApprover(LoginUserUtils.getLoginUser().getName());
        return Response.success(workflowService.listTask(query));
    }

    @GetMapping("/workflow/processSummary")
    @ApiOperation(value = "Get process statistics")
    public Response<ProcessCountResponse> processSummary(ProcessCountRequest query) {
        query.setApplicant(LoginUserUtils.getLoginUser().getName());
        return Response.success(workflowService.countProcess(query));
    }

    @GetMapping("/workflow/taskSummary")
    @ApiOperation(value = "Get task statistics")
    public Response<TaskCountResponse> taskSummary(TaskCountRequest query) {
        query.setApprover(LoginUserUtils.getLoginUser().getName());
        return Response.success(workflowService.countTask(query));
    }

    @GetMapping("/workflow/listTaskLogs")
    @ApiOperation(value = "Get task execution logs")
    public Response<PageResult<WorkflowExecuteLog>> listTaskLogs(TaskLogRequest query) {
        return Response.success(workflowService.listTaskLogs(query));
    }

}
