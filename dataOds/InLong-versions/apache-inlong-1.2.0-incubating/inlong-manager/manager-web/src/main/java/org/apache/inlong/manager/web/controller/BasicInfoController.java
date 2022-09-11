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
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.pojo.group.DataSchemaInfo;
import org.apache.inlong.manager.service.core.DataSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Control layer for basic information query
 * <p/>Basic information, such as data_schema, cluster info, etc.
 */
@RestController
@RequestMapping("/basic")
@Api(tags = "Basic Config")
public class BasicInfoController {

    @Autowired
    private DataSchemaService schemaService;

    @ApiOperation(value = "Query data format list")
    @GetMapping(value = "/schema/listAll")
    public Response<List<DataSchemaInfo>> dataSchemaList() {
        return Response.success(schemaService.listAllDataSchema());
    }

}
