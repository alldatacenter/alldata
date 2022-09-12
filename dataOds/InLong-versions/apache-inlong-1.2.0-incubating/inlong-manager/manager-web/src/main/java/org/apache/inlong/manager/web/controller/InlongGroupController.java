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
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.common.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.common.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.operation.InlongGroupProcessOperation;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inlong group control layer
 */
@RestController
@RequestMapping("/group")
@Api(tags = "Inlong-Group-API")
public class InlongGroupController {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongGroupProcessOperation groupProcessOperation;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong group info")
    public Response<String> save(@RequestBody InlongGroupRequest groupRequest) {
        groupRequest.checkParams();
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupService.save(groupRequest, operator));
    }

    @RequestMapping(value = "/get/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong group info")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<InlongGroupInfo> get(@PathVariable String groupId) {
        return Response.success(groupService.get(groupId));
    }

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ApiOperation(value = "Get inlong group list by paginating")
    public Response<PageInfo<InlongGroupListResponse>> listByCondition(@RequestBody InlongGroupPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUserDetail().getUserName());
        request.setIsAdminRole(LoginUserUtils.getLoginUserDetail().getRoles().contains(UserTypeEnum.Admin.name()));
        return Response.success(groupService.listByPage(request));
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update inlong group info")
    public Response<String> update(@RequestBody InlongGroupRequest groupRequest) {
        groupRequest.checkParams();
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupService.update(groupRequest, operator));
    }

    @RequestMapping(value = "/exist/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Is exists of the inlong group id")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Boolean> exist(@PathVariable String groupId) {
        return Response.success(groupService.exist(groupId));
    }

    @RequestMapping(value = "/countByStatus", method = RequestMethod.GET)
    @ApiOperation(value = "Count inlong group status for current user")
    public Response<InlongGroupCountResponse> countGroupByUser() {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupService.countGroupByUser(operator));
    }

    @RequestMapping(value = "startProcess/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Start inlong approval process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<WorkflowResult> startProcess(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.startProcess(groupId, operator));
    }

    @RequestMapping(value = "suspendProcess/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Suspend inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<WorkflowResult> suspendProcess(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.suspendProcess(groupId, operator));
    }

    @RequestMapping(value = "restartProcess/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<WorkflowResult> restartProcess(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.restartProcess(groupId, operator));
    }

    @RequestMapping(value = "/delete/{groupId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete inlong group info")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<Boolean> delete(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.deleteProcess(groupId, operator));
    }

    @RequestMapping(value = "suspendProcessAsync/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Suspend inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<String> suspendProcessAsync(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.suspendProcessAsync(groupId, operator));
    }

    @RequestMapping(value = "restartProcessAsync/{groupId}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart inlong group process")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class)
    public Response<String> restartProcessAsync(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.restartProcessAsync(groupId, operator));
    }

    @RequestMapping(value = "/deleteAsync/{groupId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete inlong group info")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<String> deleteAsync(@PathVariable String groupId) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(groupProcessOperation.deleteProcessAsync(groupId, operator));
    }

    @RequestMapping(value = "getTopic/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get topic info")
    public Response<InlongGroupTopicInfo> getTopic(@PathVariable String groupId) {
        return Response.success(groupService.getTopic(groupId));
    }

}