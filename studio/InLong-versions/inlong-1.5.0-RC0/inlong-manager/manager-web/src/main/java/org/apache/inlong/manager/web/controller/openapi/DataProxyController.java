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
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfig;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigRequest;
import org.apache.inlong.common.pojo.dataproxy.DataProxyNodeResponse;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.repository.DataProxyConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data proxy controller.
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-DataProxy-API")
public class DataProxyController {

    @Autowired
    @Lazy
    private InlongClusterService clusterService;
    @Autowired
    private DataProxyConfigRepository dataProxyConfigRepository;

    // TODO protocol type must be provided by the DataProxy
    @PostMapping(value = "/dataproxy/getIpList/{inlongGroupId}")
    @ApiOperation(value = "Get data proxy IP list by InlongGroupId")
    public Response<DataProxyNodeResponse> getIpList(@PathVariable String inlongGroupId,
            @RequestParam(required = false) String protocolType) {
        return Response.success(clusterService.getDataProxyNodes(inlongGroupId, protocolType));
    }

    @PostMapping("/dataproxy/getConfig")
    @ApiOperation(value = "Get data proxy topic list")
    public Response<DataProxyConfig> getConfig(@RequestBody DataProxyConfigRequest request) {
        DataProxyConfig config = clusterService.getDataProxyConfig(request.getClusterTag(), request.getClusterName());
        if (CollectionUtils.isEmpty(config.getMqClusterList()) || CollectionUtils.isEmpty(config.getTopicList())) {
            return Response.fail("Failed to get MQ Cluster or Topic, make sure Cluster registered or Topic existed.");
        }
        return Response.success(config);
    }

    @PostMapping("/dataproxy/getAllConfig")
    @ApiOperation(value = "Get all proxy config")
    public String getAllConfig(@RequestBody DataProxyConfigRequest request) {
        return clusterService.getAllConfig(request.getClusterName(), request.getMd5());
    }

    @RequestMapping(value = "/changeClusterTag", method = RequestMethod.PUT)
    @ApiOperation(value = "Change cluster tag and topic for inlong group id")
    public Response<String> changeClusterTag(@RequestParam String inlongGroupId, @RequestParam String clusterTag,
            @RequestParam String topic) {
        String result = dataProxyConfigRepository.changeClusterTag(inlongGroupId, clusterTag, topic);
        return Response.success(result);
    }

    @RequestMapping(value = "/removeBackupClusterTag", method = RequestMethod.PUT)
    @ApiOperation(value = "Remove backup cluster tag and topic for inlong group id")
    public Response<String> removeBackupClusterTag(@RequestParam String inlongGroupId) {
        String result = dataProxyConfigRepository.removeBackupClusterTag(inlongGroupId);
        return Response.success(result);
    }

}
