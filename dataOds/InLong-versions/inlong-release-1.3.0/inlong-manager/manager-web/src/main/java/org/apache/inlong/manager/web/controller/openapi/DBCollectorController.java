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
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.dbcollector.DBCollectorReportTaskRequest;
import org.apache.inlong.manager.pojo.dbcollector.DBCollectorTaskInfo;
import org.apache.inlong.manager.pojo.dbcollector.DBCollectorTaskRequest;
import org.apache.inlong.manager.service.core.DBCollectorTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Database collector controller.
 */
@RestController
@RequestMapping("/openapi/dbCollector")
@Api(tags = "Open-DBCollector-API")
@Deprecated
public class DBCollectorController {

    @Autowired
    private DBCollectorTaskService taskService;

    @Deprecated
    @PostMapping("/getTask")
    @ApiOperation(value = "fetch db collector task")
    public Response<DBCollectorTaskInfo> getTask(@RequestBody DBCollectorTaskRequest req) {
        return Response.success(taskService.getTask(req));
    }

    @PostMapping("/reportTask")
    @ApiOperation(value = "report task state")
    public Response<Integer> reportTask(@RequestBody DBCollectorReportTaskRequest req) {
        return Response.success(taskService.reportTask(req));
    }
}
