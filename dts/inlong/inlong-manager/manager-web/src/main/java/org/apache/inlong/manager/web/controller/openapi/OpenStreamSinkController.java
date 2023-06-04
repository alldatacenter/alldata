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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Open InLong Stream Sink controller
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-StreamSink-API")
public class OpenStreamSinkController {

    @Autowired
    private StreamSinkService sinkService;

    @RequestMapping(value = "/sink/get/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get stream sink")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<StreamSink> get(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "sinkId cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sinkService.get(id, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/sink/list", method = RequestMethod.POST)
    @ApiOperation(value = "List stream sinks by paginating")
    public Response<List<? extends StreamSink>> listByCondition(@RequestBody SinkPageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sinkService.listByCondition(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/sink/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save stream sink")
    public Response<Integer> save(@Validated @RequestBody SinkRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sinkService.save(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/sink/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update stream sink")
    public Response<Boolean> update(@Validated(UpdateByIdValidation.class) @RequestBody SinkRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sinkService.update(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/sink/delete/{id}", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete stream sink")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "startProcess", dataTypeClass = boolean.class),
            @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    })
    public Response<Boolean> delete(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "false") boolean startProcess) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "sinkId cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(sinkService.delete(id, startProcess, LoginUserUtils.getLoginUser()));
    }
}
