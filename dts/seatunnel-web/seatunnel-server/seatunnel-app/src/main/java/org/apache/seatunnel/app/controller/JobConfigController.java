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
import org.apache.seatunnel.app.domain.request.job.JobConfig;
import org.apache.seatunnel.app.domain.response.job.JobConfigRes;
import org.apache.seatunnel.app.service.IJobConfigService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Resource;

@RestController
@RequestMapping("/seatunnel/api/v1/job/config")
public class JobConfigController {

    @Resource private IJobConfigService jobConfigService;

    @PutMapping("/{jobVersionId}")
    @ApiOperation(value = "update job config", httpMethod = "PUT")
    Result<Void> updateJobConfig(
            @ApiParam(value = "userId", required = true) @RequestAttribute("userId") Integer userId,
            @ApiParam(value = "jobVersionId", required = true) @PathVariable long jobVersionId,
            @ApiParam(value = "jobConfig", required = true) @RequestBody JobConfig jobConfig)
            throws JsonProcessingException {
        jobConfigService.updateJobConfig(userId, jobVersionId, jobConfig);
        return Result.success();
    }

    @GetMapping("/{jobVersionId}")
    @ApiOperation(value = "get job config", httpMethod = "GET")
    Result<JobConfigRes> getJobConfig(
            @ApiParam(value = "jobVersionId", required = true) @PathVariable long jobVersionId)
            throws JsonProcessingException {
        return Result.success(jobConfigService.getJobConfig(jobVersionId));
    }
}
