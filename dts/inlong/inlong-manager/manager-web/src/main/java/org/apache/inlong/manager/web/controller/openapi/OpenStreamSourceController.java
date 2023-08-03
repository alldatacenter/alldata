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
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.source.SourcePageRequest;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.source.StreamSourceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open InLong Stream Source controller
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-StreamSource-API")
public class OpenStreamSourceController {

    @Autowired
    StreamSourceService sourceService;

    @RequestMapping(value = "/source/get/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<StreamSource> get(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "sourceId cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sourceService.get(id, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/source/list", method = RequestMethod.POST)
    @ApiOperation(value = "List stream sources by paginating")
    public Response<PageResult<? extends StreamSource>> listByCondition(@RequestBody SourcePageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sourceService.listByCondition(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/source/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save stream source")
    public Response<Integer> save(@Validated(SaveValidation.class) @RequestBody SourceRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sourceService.save(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/source/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update stream source")
    public Response<Boolean> update(@Validated(UpdateValidation.class) @RequestBody SourceRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sourceService.update(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/source/delete/{id}", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> delete(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "sourceId cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sourceService.delete(id, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/source/stop/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "Stop stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> stop(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "sourceId cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        boolean result = sourceService.stop(id, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }

    @RequestMapping(value = "/source/restart/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart stream source")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> restart(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "sourceId cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        boolean result = sourceService.restart(id, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }
}
