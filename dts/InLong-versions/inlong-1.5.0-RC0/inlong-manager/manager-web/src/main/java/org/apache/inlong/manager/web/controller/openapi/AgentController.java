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
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeBindGroupRequest;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.core.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent controller.
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-Agent-API")
public class AgentController {

    @Autowired
    private AgentService agentService;
    @Autowired
    private InlongClusterService clusterService;

    /**
     * Currently not used.
     */
    @PostMapping("/agent/getManagerIpList")
    @ApiOperation(value = "Get inlong manager ip list")
    public Response<List<String>> getInLongManagerIp() {
        return Response.success(clusterService.listNodeIpByType("inlong-openapi"));
    }

    @PostMapping("/agent/reportSnapshot")
    @ApiOperation(value = "Report source task snapshot")
    public Response<Boolean> reportSnapshot(@RequestBody TaskSnapshotRequest request) {
        return Response.success(agentService.reportSnapshot(request));
    }

    @PostMapping("/agent/reportAndGetTask")
    @ApiOperation(value = "Report task result and get next tasks")
    public Response<TaskResult> reportAndGetTask(@RequestBody TaskRequest request) {
        agentService.report(request);
        return Response.success(agentService.getTaskResult(request));
    }

    @PostMapping("/agent/bindGroup")
    @ApiOperation(value = "Divide the agent into different groups, which collect different stream source tasks.")
    public Response<Boolean> bindGroup(@RequestBody AgentClusterNodeBindGroupRequest request) {
        return Response.success(agentService.bindGroup(request));
    }
}
