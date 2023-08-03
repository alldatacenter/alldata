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
import org.apache.seatunnel.app.domain.response.connector.ConnectorInfo;
import org.apache.seatunnel.app.service.IConnectorService;
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

@RequestMapping("/whaletunnel/api/v1/connector")
@RestController
public class ConnectorController {

    @Resource private IConnectorService connectorService;

    @GetMapping("/sources")
    @ApiOperation(value = "list all source connector", httpMethod = "GET")
    public Result<List<ConnectorInfo>> listSource(
            @RequestParam(defaultValue = "ALL") ConnectorStatus status) {
        return Result.success(connectorService.listSources(status));
    }

    @GetMapping("/transforms")
    @ApiOperation(value = "list all transforms", httpMethod = "GET")
    public Result<List<ConnectorInfo>> listAllTransform() {
        return Result.success(connectorService.listTransforms());
    }

    @GetMapping("/sinks")
    @ApiOperation(value = "list all sink connector", httpMethod = "GET")
    public Result<List<ConnectorInfo>> listSink(
            @RequestParam(defaultValue = "ALL") ConnectorStatus status) {
        return Result.success(connectorService.listSinks(status));
    }

    @GetMapping("/sync")
    @ApiOperation(value = "sync all connector from disk", httpMethod = "GET")
    public Result<List<ConnectorInfo>> sync() throws IOException {
        connectorService.sync();
        return Result.success();
    }

    @GetMapping("/form")
    @ApiOperation(value = "get source connector form structure", httpMethod = "GET")
    public Result<String> getConnectorFormStructure(
            @ApiParam(value = "connector type", required = true) @RequestParam String connectorType,
            @ApiParam(value = "connector name", required = true) @RequestParam
                    String connectorName) {
        return Result.success(
                JsonUtils.toJsonString(
                        connectorService.getConnectorFormStructure(connectorType, connectorName)));
    }
}
