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
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.ApproverPageRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverResponse;
import org.apache.inlong.manager.service.core.WorkflowApproverService;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workflow-Approver controller
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Workflow-Approver-API")
public class WorkflowApproverController {

    @Autowired
    private WorkflowApproverService workflowApproverService;

    @PostMapping("/workflow/approver/save")
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save approver info")
    public Response<Integer> save(@RequestBody ApproverRequest config) {
        return Response.success(workflowApproverService.save(config, LoginUserUtils.getLoginUser().getName()));
    }

    @GetMapping(value = "/workflow/approver/get/{id}")
    @ApiOperation(value = "Get approver by ID")
    @ApiImplicitParam(name = "id", value = "Workflow approver ID", dataTypeClass = Integer.class, required = true)
    public Response<ApproverResponse> get(@PathVariable Integer id) {
        return Response.success(workflowApproverService.get(id));
    }

    @GetMapping("/workflow/approver/list")
    @ApiOperation(value = "List workflow approvers")
    public Response<PageResult<ApproverResponse>> listByCondition(ApproverPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUser().getName());
        request.setIsAdminRole(LoginUserUtils.getLoginUser().getRoles().contains(UserTypeEnum.ADMIN.name()));
        return Response.success(workflowApproverService.listByCondition(request));
    }

    @PostMapping("/workflow/approver/update")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update approver info")
    public Response<Integer> update(@RequestBody ApproverRequest request) {
        return Response.success(workflowApproverService.update(request, LoginUserUtils.getLoginUser().getName()));
    }

    @DeleteMapping("/workflow/approver/delete/{id}")
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete approver by ID")
    @ApiImplicitParam(name = "id", value = "Workflow approver ID", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> delete(@PathVariable Integer id) {
        workflowApproverService.delete(id, LoginUserUtils.getLoginUser().getName());
        return Response.success(true);
    }

}
