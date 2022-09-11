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
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfig;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.pojo.dataproxy.DataProxyNodeInfo;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Data proxy controller.
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-DataProxy-Config")
public class DataProxyController {

    @Autowired
    private InlongClusterService clusterService;

    /**
     * Support GET and POST methods,
     * POST is used for DataProxy requests,
     * GET is used for quick lookup of IP lists (e.g. via browser requests).
     */
    @RequestMapping(value = "/dataproxy/getIpList", method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "Get data proxy ip list by cluster name")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterTag", value = "cluster tag", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterName", value = "cluster name", dataTypeClass = String.class)
    })
    public Response<List<DataProxyNodeInfo>> getIpList(
            @RequestParam(required = false) String clusterTag,
            @RequestParam(required = false) String clusterName) {
        return Response.success(clusterService.getDataProxyNodeList(clusterTag, clusterName));
    }

    @GetMapping("/dataproxy/getConfig")
    @ApiOperation(value = "Get data proxy topic list")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterTag", value = "cluster tag", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterName", value = "cluster name", dataTypeClass = String.class)
    })
    public Response<DataProxyConfig> getConfig(
            @RequestParam(required = false) String clusterTag,
            @RequestParam(required = true) String clusterName) {
        DataProxyConfig config = clusterService.getDataProxyConfig(clusterTag, clusterName);
        if (CollectionUtils.isEmpty(config.getMqClusterList()) || CollectionUtils.isEmpty(config.getTopicList())) {
            return Response.fail("failed to get mq clusters or topics");
        }
        return Response.success(config);
    }

    @GetMapping("/dataproxy/getAllConfig")
    @ApiOperation(value = "Get all proxy config")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterName", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "md5", dataTypeClass = String.class, required = true)
    })
    public String getAllConfig(@RequestParam String clusterName, @RequestParam(required = false) String md5) {
        return clusterService.getAllConfig(clusterName, md5);
    }

}
