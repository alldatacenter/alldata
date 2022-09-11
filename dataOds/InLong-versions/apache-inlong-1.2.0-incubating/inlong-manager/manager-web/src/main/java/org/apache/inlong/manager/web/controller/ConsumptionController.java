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
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionListVo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionQuery;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionSummary;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.service.core.operation.ConsumptionProcessOperation;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
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
@RequestMapping("/consumption")
@Api(tags = "Data Consumption")
public class ConsumptionController {

    @Autowired
    private ConsumptionService consumptionService;
    @Autowired
    private ConsumptionProcessOperation processOperation;

    @GetMapping("/summary")
    @ApiOperation(value = "Get data consumption summary")
    public Response<ConsumptionSummary> getSummary(ConsumptionQuery query) {
        query.setUserName(LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(consumptionService.getSummary(query));
    }

    @GetMapping("/list")
    @ApiOperation(value = "List data consumptions")
    public Response<PageInfo<ConsumptionListVo>> list(ConsumptionQuery query) {
        query.setUserName(LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success(consumptionService.list(query));
    }

    @GetMapping("/get/{id}")
    @ApiOperation(value = "Get consumption details")
    @ApiImplicitParam(name = "id", value = "Consumption ID", dataTypeClass = Integer.class, required = true)
    public Response<ConsumptionInfo> getDetail(@PathVariable(name = "id") Integer id) {
        return Response.success(consumptionService.get(id));
    }

    @DeleteMapping("/delete/{id}")
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete data consumption")
    @ApiImplicitParam(name = "id", value = "Consumption ID", dataTypeClass = Integer.class, required = true)
    public Response<Object> delete(@PathVariable(name = "id") Integer id) {
        this.consumptionService.delete(id, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success();
    }

    @PostMapping("/save")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Save data consumption", notes = "Full coverage")
    public Response<Integer> save(@Validated @RequestBody ConsumptionInfo consumptionInfo) {
        String currentUser = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(consumptionService.save(consumptionInfo, currentUser));
    }

    @PostMapping("/update/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update data consumption")
    public Response<String> update(@PathVariable(name = "id") Integer id,
            @Validated @RequestBody ConsumptionInfo consumptionInfo) {
        consumptionInfo.setId(id);
        consumptionService.update(consumptionInfo, LoginUserUtils.getLoginUserDetail().getUserName());
        return Response.success();
    }

    @PostMapping("/startProcess/{id}")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Start approval process")
    @ApiImplicitParam(name = "id", value = "Consumption ID", dataTypeClass = Integer.class, required = true)
    public Response<WorkflowResult> startProcess(@PathVariable(name = "id") Integer id) {
        String username = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(processOperation.startProcess(id, username));
    }

}
