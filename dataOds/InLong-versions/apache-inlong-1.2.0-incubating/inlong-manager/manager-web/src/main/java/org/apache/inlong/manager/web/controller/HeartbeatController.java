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
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.common.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.common.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.common.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.common.pojo.heartbeat.StreamHeartbeatResponse;
import org.apache.inlong.manager.service.core.HeartbeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Heartbeat controller.
 */
@RestController
@RequestMapping("/heartbeat")
@Api(tags = "Heartbeat-API")
public class HeartbeatController {

    @Autowired
    private HeartbeatService heartbeatService;

    @RequestMapping(value = "/component/get", method = RequestMethod.POST)
    @ApiOperation(value = "Get component heartbeat")
    public Response<ComponentHeartbeatResponse> getComponentHeartbeat(@RequestBody HeartbeatQueryRequest request) {
        return Response.success(heartbeatService.getComponentHeartbeat(request));
    }

    @RequestMapping(value = "/group/get", method = RequestMethod.POST)
    @ApiOperation(value = "Get group heartbeat")
    public Response<GroupHeartbeatResponse> getGroupHeartbeat(@RequestBody HeartbeatQueryRequest request) {
        return Response.success(heartbeatService.getGroupHeartbeat(request));
    }

    @RequestMapping(value = "/stream/get", method = RequestMethod.POST)
    @ApiOperation(value = "Get stream heartbeat")
    public Response<StreamHeartbeatResponse> getStreamHeartbeat(@RequestBody HeartbeatQueryRequest request) {
        return Response.success(heartbeatService.getStreamHeartbeat(request));
    }

    @RequestMapping(value = "/component/list", method = RequestMethod.POST)
    @ApiOperation(value = "List component heartbeats")
    public Response<PageInfo<ComponentHeartbeatResponse>> listComponentHeartbeat(
            @RequestBody HeartbeatPageRequest request) {
        return Response.success(heartbeatService.listComponentHeartbeat(request));
    }

    @RequestMapping(value = "/group/list", method = RequestMethod.POST)
    @ApiOperation(value = "List group heartbeats")
    public Response<PageInfo<GroupHeartbeatResponse>> listGroupHeartbeat(@RequestBody HeartbeatPageRequest request) {
        return Response.success(heartbeatService.listGroupHeartbeat(request));
    }

    @RequestMapping(value = "/stream/list", method = RequestMethod.POST)
    @ApiOperation(value = "List stream heartbeats")
    public Response<PageInfo<StreamHeartbeatResponse>> listStreamHeartbeat(@RequestBody HeartbeatPageRequest request) {
        return Response.success(heartbeatService.listStreamHeartbeat(request));
    }

}

