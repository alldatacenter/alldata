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
import org.apache.seatunnel.app.domain.request.job.JobDAG;
import org.apache.seatunnel.app.domain.request.job.JobTaskInfo;
import org.apache.seatunnel.app.domain.request.job.PluginConfig;
import org.apache.seatunnel.app.domain.response.job.JobTaskCheckRes;
import org.apache.seatunnel.app.service.IJobTaskService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Resource;

@RestController
@RequestMapping("/seatunnel/api/v1/job/")
public class JobTaskController {

    @Resource private IJobTaskService jobTaskService;

    @PostMapping("/dag/{jobVersionId}")
    @ApiOperation(value = "save job dag", httpMethod = "POST")
    Result<JobTaskCheckRes> saveJobDAG(
            @ApiParam(value = "job version id", required = true) @PathVariable long jobVersionId,
            @ApiParam(value = "task info", required = true) @RequestBody JobDAG jobDAG) {
        return Result.success(jobTaskService.saveJobDAG(jobVersionId, jobDAG));
    }

    @GetMapping("/{jobVersionId}")
    @ApiOperation(value = "get job task and dag info", httpMethod = "GET")
    Result<JobTaskInfo> getJob(
            @ApiParam(value = "job version id", required = true) @PathVariable long jobVersionId) {
        return Result.success(jobTaskService.getTaskConfig(jobVersionId));
    }

    @PostMapping("/task/{jobVersionId}")
    @ApiOperation(value = "save or update single task", httpMethod = "POST")
    Result<Void> saveSingleTask(
            @ApiParam(value = "job version id", required = true) @PathVariable long jobVersionId,
            @ApiParam(value = "task info", required = true) @RequestBody
                    PluginConfig pluginConfig) {
        jobTaskService.saveSingleTask(jobVersionId, pluginConfig);
        return Result.success();
    }

    @GetMapping("/task/{jobVersionId}")
    @ApiOperation(value = "get single task", httpMethod = "GET")
    Result<PluginConfig> getSingleTask(
            @ApiParam(value = "job version id", required = true) @PathVariable long jobVersionId,
            @ApiParam(value = "task plugin id", required = true) @RequestParam String pluginId) {
        return Result.success(jobTaskService.getSingleTask(jobVersionId, pluginId));
    }

    @DeleteMapping("/task/{jobVersionId}")
    @ApiOperation(value = "delete single task", httpMethod = "DELETE")
    Result<Void> deleteSingleTask(
            @ApiParam(value = "job version id", required = true) @PathVariable long jobVersionId,
            @ApiParam(value = "task plugin id", required = true) @RequestParam String pluginId) {
        jobTaskService.deleteSingleTask(jobVersionId, pluginId);
        return Result.success();
    }
}
