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
import org.apache.seatunnel.app.domain.request.connector.ConnectorStatus;
import org.apache.seatunnel.app.domain.request.connector.SceneMode;
import org.apache.seatunnel.app.domain.response.connector.ConnectorInfo;
import org.apache.seatunnel.app.domain.response.connector.DataSourceInstance;
import org.apache.seatunnel.app.service.IConnectorService;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.common.utils.JsonUtils;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.List;

@RequestMapping("/seatunnel/api/v1/datasource")
@RestController
public class ConnectorDataSourceController {

    @Resource private IConnectorService connectorService;

    @GetMapping("/sources")
    @ApiOperation(value = "Use jobID to list source DataSourceInstance", httpMethod = "GET")
    public Result<List<DataSourceInstance>> listSource(
            @ApiParam(value = "jobCode", required = true) @RequestParam Long jobId,
            @ApiParam(value = "SceneMode", required = true) @RequestParam SceneMode sceneMode,
            @ApiParam(value = "ConnectorStatus", required = true) @RequestParam
                    ConnectorStatus status) {
        return Result.success(
                connectorService.listSourceDataSourceInstances(jobId, sceneMode, status));
    }

    @GetMapping("/sinks")
    @ApiOperation(value = "Use jobID to list sink DataSourceInstance", httpMethod = "GET")
    public Result<List<DataSourceInstance>> listSink(
            @ApiParam(value = "jobCode", required = true) @RequestParam Long jobId,
            @ApiParam(value = "ConnectorStatus", required = true) @RequestParam
                    ConnectorStatus status) {
        return Result.success(connectorService.listSinkDataSourcesInstances(jobId, status));
    }

    @GetMapping("/transforms")
    @ApiOperation(value = "Use jobID to list  transforms", httpMethod = "GET")
    public Result<List<ConnectorInfo>> listAllTransform(
            @ApiParam(value = "jobCode", required = true) @RequestParam Long jobId) {
        return Result.success(connectorService.listTransformsForJob(jobId));
    }

    @GetMapping("/sync")
    @ApiOperation(value = "sync all connector from disk", httpMethod = "GET")
    public Result<List<ConnectorInfo>> sync() throws IOException {
        connectorService.sync();
        return Result.success();
    }

    @GetMapping("/form")
    @ApiOperation(value = "get datasource instance form structure", httpMethod = "GET")
    public Result<String> getDatasourceInstanceFormStructure(
            @ApiParam(value = "jobCode", required = false)
                    @RequestParam(required = false, value = "jobCode")
                    Long jobId,
            @ApiParam(value = "connector type", required = true)
                    @RequestParam(required = true, value = "connectorType")
                    String connectorType,
            @ApiParam(value = "connector name", required = false)
                    @RequestParam(required = false, value = "connectorName")
                    String connectorName,
            @ApiParam(value = "dataSource instanceId", required = false)
                    @RequestParam(required = false, value = "dataSourceInstanceId")
                    Long dataSourceInstanceId) {
        if (PluginType.TRANSFORM.getType().equals(connectorType)) {
            return Result.success(
                    JsonUtils.toJsonString(
                            connectorService.getTransformFormStructure(
                                    connectorType, connectorName)));
        }
        return Result.success(
                JsonUtils.toJsonString(
                        connectorService.getDatasourceFormStructure(
                                jobId, dataSourceInstanceId, connectorType)));
    }
}
