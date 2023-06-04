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

package org.apache.inlong.manager.web.controller.openapi;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.service.group.InlongGroupProcessService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;

/**
 * Open InLong Group controller
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-InLongGroup-API")
public class OpenInLongGroupController {

    @Autowired
    private InlongGroupService groupService;

    @Autowired
    private InlongGroupProcessService groupProcessOperation;

    @GetMapping(value = "/group/get/{groupId}")
    @ApiOperation(value = "Get InLong group information by groupId")
    @ApiImplicitParam(name = "groupId", value = "InLong Group ID", dataTypeClass = String.class, required = true)
    public Response<InlongGroupInfo> get(@PathVariable String groupId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.INVALID_PARAMETER, "groupId cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(groupService.get(groupId, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/group/list")
    @ApiOperation(value = "List inlong groups by paginating")
    public Response<List<InlongGroupBriefInfo>> listBrief(@RequestBody InlongGroupPageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(groupService.listBrief(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/group/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong group")
    public Response<String> save(@Validated(SaveValidation.class) @RequestBody InlongGroupRequest groupRequest) {
        Preconditions.expectNotNull(groupRequest, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(groupService.save(groupRequest, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/group/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update inlong group")
    public Response<String> update(@Validated(UpdateValidation.class) @RequestBody InlongGroupRequest groupRequest) {
        Preconditions.expectNotNull(groupRequest, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(groupService.update(groupRequest, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/group/delete/{groupId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete inlong group info")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Boolean> delete(@PathVariable String groupId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.INVALID_PARAMETER, "groupId cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(groupProcessOperation.deleteProcess(groupId, LoginUserUtils.getLoginUser()));
    }
}
