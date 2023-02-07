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
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.service.consume.InlongConsumeProcessService;
import org.apache.inlong.manager.service.consume.InlongConsumeService;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inlong consume control layer
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Inlong-Consume-API")
public class InlongConsumeController {

    @Autowired
    private InlongConsumeService consumeService;
    @Autowired
    private InlongConsumeProcessService consumeProcessService;

    @RequestMapping(value = "/consume/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong consume")
    public Response<Integer> save(@RequestBody InlongConsumeRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(consumeService.save(request, operator));
    }

    @GetMapping("/consume/get/{id}")
    @ApiOperation(value = "Get inlong consume")
    @ApiImplicitParam(name = "id", value = "Inlong consume ID", dataTypeClass = Integer.class, required = true)
    public Response<InlongConsumeInfo> get(@PathVariable(name = "id") Integer id) {
        return Response.success(consumeService.get(id));
    }

    @GetMapping(value = "/consume/countStatus")
    @ApiOperation(value = "Count inlong consume status by current user")
    public Response<InlongConsumeCountInfo> countStatusByUser() {
        return Response.success(consumeService.countStatus(LoginUserUtils.getLoginUser().getName()));
    }

    @GetMapping("/consume/list")
    @ApiOperation(value = "List inlong consume by pagination")
    public Response<PageResult<InlongConsumeBriefInfo>> list(InlongConsumePageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUser().getName());
        request.setIsAdminRole(LoginUserUtils.getLoginUser().getRoles().contains(UserTypeEnum.ADMIN.name()));
        return Response.success(consumeService.list(request));
    }

    @PostMapping("/consume/update")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update inlong consume")
    public Response<Integer> update(@Validated(UpdateValidation.class) @RequestBody InlongConsumeRequest request) {
        return Response.success(consumeService.update(request, LoginUserUtils.getLoginUser().getName()));
    }

    @DeleteMapping("/consume/delete/{id}")
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete inlong consume by ID")
    @ApiImplicitParam(name = "id", value = "Inlong consume ID", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> delete(@PathVariable(name = "id") Integer id) {
        return Response.success(consumeService.delete(id, LoginUserUtils.getLoginUser().getName()));
    }

    @PostMapping("/consume/startProcess/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Start inlong consume process")
    @ApiImplicitParam(name = "id", value = "Inlong consume ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> startProcess(@PathVariable(name = "id") Integer id) {
        String username = LoginUserUtils.getLoginUser().getName();
        return Response.success(consumeProcessService.startProcess(id, username));
    }

}
