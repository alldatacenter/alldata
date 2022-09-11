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
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.pojo.stream.FullStreamRequest;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamListResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.StreamBriefResponse;
import org.apache.inlong.manager.common.pojo.user.UserRoleCode;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.core.operation.InlongStreamProcessOperation;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Inlong stream control layer
 */
@RestController
@RequestMapping("/stream")
@Api(tags = "Inlong-Stream-API")
public class InlongStreamController {

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongStreamProcessOperation streamProcessOperation;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong stream info")
    public Response<Integer> save(@RequestBody InlongStreamRequest request) {
        int result = streamService.save(request, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(result);
    }

    @RequestMapping(value = "/saveAll", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong stream info page")
    public Response<Boolean> saveAll(@RequestBody FullStreamRequest pageInfo) {
        return Response.success(streamService.saveAll(pageInfo, LoginUserUtils.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/batchSaveAll", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong stream page info in batch")
    public Response<Boolean> batchSaveAll(@RequestBody List<FullStreamRequest> infoList) {
        boolean result = streamService.batchSaveAll(infoList, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(result);
    }

    @RequestMapping(value = "/exist/{groupId}/{streamId}", method = RequestMethod.GET)
    @ApiOperation(value = "Is exists of the inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> exist(@PathVariable String groupId, @PathVariable String streamId) {
        return Response.success(streamService.exist(groupId, streamId));
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong stream info")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<InlongStreamResponse> get(@RequestParam String groupId, @RequestParam String streamId) {
        return Response.success(streamService.get(groupId, streamId).genResponse());
    }

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ApiOperation(value = "Get inlong stream by paginating")
    public Response<PageInfo<InlongStreamListResponse>> listByCondition(@RequestBody InlongStreamPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUserDetail().getUserName());
        request.setIsAdminRole(LoginUserUtils.getLoginUserDetail().getRoles().contains(UserRoleCode.ADMIN));
        return Response.success(streamService.listByCondition(request));
    }

    @RequestMapping(value = "/listAll", method = RequestMethod.POST)
    @ApiOperation(value = "Get all inlong stream info by paginating")
    public Response<PageInfo<FullStreamResponse>> listAllWithGroupId(@RequestBody InlongStreamPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUserDetail().getUserName());
        request.setIsAdminRole(LoginUserUtils.getLoginUserDetail().getRoles().contains(UserRoleCode.ADMIN));
        return Response.success(streamService.listAllWithGroupId(request));
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update inlong stream info")
    public Response<Boolean> update(@RequestBody InlongStreamRequest request) {
        String username = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(streamService.update(request, username));
    }

    @RequestMapping(value = "/startProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Start inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> startProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(streamProcessOperation.startProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/suspendProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Suspend inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> suspendProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(streamProcessOperation.suspendProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/restartProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> restartProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(streamProcessOperation.restartProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/deleteProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Delete inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> deleteProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(streamProcessOperation.deleteProcess(groupId, streamId, operator, sync));
    }

    @Deprecated
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete inlong stream info")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> delete(@RequestParam String groupId, @RequestParam String streamId) {
        String username = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(streamService.delete(groupId, streamId, username));
    }

    @RequestMapping(value = "/getSummaryList/{groupId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong stream summary list")
    @ApiImplicitParam(name = "groupId", value = "Inlong group id", dataTypeClass = String.class, required = true)
    public Response<List<StreamBriefResponse>> getSummaryList(@PathVariable String groupId) {
        return Response.success(streamService.getBriefList(groupId));
    }

}
