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
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.pojo.consumption.ConsumptionListVo;
import org.apache.inlong.manager.pojo.consumption.ConsumptionQuery;
import org.apache.inlong.manager.pojo.consumption.ConsumptionSummary;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.service.core.impl.ConsumptionProcessService;
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

/**
 * Data consumption interface
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Consumption-API")
public class ConsumptionController {

    @Autowired
    private ConsumptionService consumptionService;
    @Autowired
    private ConsumptionProcessService processOperation;

    @GetMapping("/consumption/summary")
    @ApiOperation(value = "Get data consumption summary")
    public Response<ConsumptionSummary> getSummary(ConsumptionQuery query) {
        query.setUsername(LoginUserUtils.getLoginUser().getName());
        return Response.success(consumptionService.getSummary(query));
    }

    @GetMapping("/consumption/list")
    @ApiOperation(value = "List data consumptions")
    public Response<PageResult<ConsumptionListVo>> list(ConsumptionQuery query) {
        query.setUsername(LoginUserUtils.getLoginUser().getName());
        return Response.success(consumptionService.list(query));
    }

    @GetMapping("/consumption/get/{id}")
    @ApiOperation(value = "Get consumption details")
    @ApiImplicitParam(name = "id", value = "Consumption ID", dataTypeClass = Integer.class, required = true)
    public Response<ConsumptionInfo> getDetail(@PathVariable(name = "id") Integer id) {
        return Response.success(consumptionService.get(id));
    }

    @DeleteMapping("/consumption/delete/{id}")
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete data consumption")
    @ApiImplicitParam(name = "id", value = "Consumption ID", dataTypeClass = Integer.class, required = true)
    public Response<Object> delete(@PathVariable(name = "id") Integer id) {
        this.consumptionService.delete(id, LoginUserUtils.getLoginUser().getName());
        return Response.success();
    }

    @PostMapping("/consumption/save")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Save data consumption", notes = "Full coverage")
    public Response<Integer> save(@Validated @RequestBody ConsumptionInfo consumptionInfo) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(consumptionService.save(consumptionInfo, currentUser));
    }

    @PostMapping("/consumption/update/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update data consumption")
    public Response<String> update(@PathVariable(name = "id") Integer id,
            @Validated @RequestBody ConsumptionInfo consumptionInfo) {
        consumptionInfo.setId(id);
        consumptionService.update(consumptionInfo, LoginUserUtils.getLoginUser().getName());
        return Response.success();
    }

    @PostMapping("/consumption/startProcess/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Start approval process")
    @ApiImplicitParam(name = "id", value = "Consumption ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> startProcess(@PathVariable(name = "id") Integer id) {
        String username = LoginUserUtils.getLoginUser().getName();
        return Response.success(processOperation.startProcess(id, username));
    }

}
