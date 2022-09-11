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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApprover;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApproverQuery;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.WorkflowApproverService;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
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
@Slf4j
@RestController
@RequestMapping("/workflow/approver")
@Api(tags = {"Workflow Approver"})
public class WorkflowApproverController {

    @Autowired
    private WorkflowApproverService workflowApproverService;

    @GetMapping("list")
    public Response<List<WorkflowApprover>> list(WorkflowApproverQuery query) {
        return Response.success(this.workflowApproverService.list(query));
    }

    @PostMapping("add")
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Add approver configuration")
    public Response<Object> add(@RequestBody WorkflowApprover config) {
        this.workflowApproverService.add(config, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success();
    }

    @PostMapping("update/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update approver configuration")
    public Response<Object> update(@RequestBody WorkflowApprover config) {
        this.workflowApproverService.update(config, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success();
    }

    @DeleteMapping("delete/{id}")
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete approver configuration")
    @ApiParam(value = "Configuration item ID", required = true)
    public Response<Object> delete(@PathVariable Integer id) {
        this.workflowApproverService.delete(id, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success();
    }
}
