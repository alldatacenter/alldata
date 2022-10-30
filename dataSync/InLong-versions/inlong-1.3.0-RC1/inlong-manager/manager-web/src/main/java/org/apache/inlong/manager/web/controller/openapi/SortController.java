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
import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.manager.service.core.SortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sort controller.
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-Sort-API")
public class SortController {

    @Autowired
    private SortService sortService;

    @GetMapping("/sort/getClusterConfig")
    @ApiOperation(value = "get sort cluster config")
    public SortClusterResponse getSortClusterConfig(
            @RequestParam String clusterName,
            @RequestParam String md5) {
        return sortService.getClusterConfig(clusterName, md5);
    }

    @GetMapping("/sort/getSortSource")
    @ApiOperation(value = "get sort sdk config")
    public SortSourceConfigResponse getSortSourceConfig(
            @RequestParam String clusterName,
            @RequestParam String sortTaskId,
            @RequestParam String md5) {
        return sortService.getSourceConfig(clusterName, sortTaskId, md5);
    }

}
