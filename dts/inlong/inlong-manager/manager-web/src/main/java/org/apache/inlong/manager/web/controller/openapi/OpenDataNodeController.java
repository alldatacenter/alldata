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
import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.service.node.DataNodeService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;

/**
 * Open InLong Data Node controller
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-DataNode-API")
public class OpenDataNodeController {

    @Autowired
    private DataNodeService dataNodeService;

    @GetMapping(value = "/node/get/{id}")
    @ApiOperation(value = "Get node by id")
    @ApiImplicitParam(name = "id", value = "Data node ID", dataTypeClass = Integer.class, required = true)
    public Response<DataNodeInfo> get(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "data node id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(dataNodeService.get(id, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/node/list")
    @ApiOperation(value = "List data node")
    public Response<List<DataNodeInfo>> list(@RequestBody DataNodePageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(dataNodeService.list(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/node/save")
    @ApiOperation(value = "Save node")
    @OperationLog(operation = OperationType.CREATE)
    public Response<Integer> save(@Validated(SaveValidation.class) @RequestBody DataNodeRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(dataNodeService.save(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/node/update")
    @ApiOperation(value = "Update data node")
    @OperationLog(operation = OperationType.UPDATE)
    public Response<Boolean> update(@Validated(UpdateByIdValidation.class) @RequestBody DataNodeRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY);
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(dataNodeService.update(request, LoginUserUtils.getLoginUser()));
    }

    @DeleteMapping(value = "/node/delete/{id}")
    @ApiOperation(value = "Delete data node by id")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "id", value = "Data node ID", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> delete(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "data node id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(dataNodeService.delete(id, LoginUserUtils.getLoginUser()));
    }
}
