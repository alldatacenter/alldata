/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.controller;

import org.apache.seatunnel.app.common.Result;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.TableSchemaReq;
import org.apache.seatunnel.app.domain.response.job.TableSchemaRes;
import org.apache.seatunnel.app.service.ITableSchemaService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Resource;

@RestController
@RequestMapping("/seatunnel/api/v1/job/table")
public class TableSchemaController {

    @Resource private ITableSchemaService tableSchemaService;

    @PostMapping("/schema")
    Result<TableSchemaRes> querySchemaMapping(
            @ApiParam(value = "datasource plugin name", required = true) @RequestParam
                    String pluginName,
            @ApiParam(value = "task info", required = true) @RequestBody
                    TableSchemaReq tableSchemaReq) {
        return Result.success(tableSchemaService.getSeaTunnelSchema(pluginName, tableSchemaReq));
    }

    @PostMapping("/check")
    @ApiOperation(value = "check database and table is exist", httpMethod = "POST")
    public Result<DataSourceOption> checkDatabaseAndTable(
            @RequestParam String datasourceId, @RequestBody DataSourceOption dataSourceOption) {
        return Result.success(
                tableSchemaService.checkDatabaseAndTable(datasourceId, dataSourceOption));
    }

    @GetMapping("/column-projection")
    Result<Boolean> queryColumnProjection(
            @ApiParam(value = "datasource plugin name", required = true) @RequestParam
                    String pluginName) {
        return Result.success(tableSchemaService.getColumnProjection(pluginName));
    }
}
