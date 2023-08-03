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
import org.apache.seatunnel.app.dal.entity.JobDefinition;
import org.apache.seatunnel.app.domain.request.job.JobReq;
import org.apache.seatunnel.app.domain.response.PageInfo;
import org.apache.seatunnel.app.domain.response.job.JobDefinitionRes;
import org.apache.seatunnel.app.service.IJobDefinitionService;
import org.apache.seatunnel.server.common.CodeGenerateUtils;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Resource;

@RestController
@RequestMapping("/seatunnel/api/v1/job/definition")
public class JobDefinitionController {

    @Resource private IJobDefinitionService jobService;

    /**
     * create job definition
     *
     * @return created job id
     */
    @PostMapping
    @ApiOperation(value = "create job definition", httpMethod = "POST")
    Result<Long> createJobDefinition(
            @ApiParam(value = "userId", required = true) @RequestAttribute("userId") Integer userId,
            @RequestBody JobReq jobReq)
            throws CodeGenerateUtils.CodeGenerateException {
        return Result.success(jobService.createJob(userId, jobReq));
    }

    @GetMapping
    @ApiOperation(value = "get job definition", httpMethod = "GET")
    Result<PageInfo<JobDefinitionRes>> getJobDefinition(
            @ApiParam(value = "job name") @RequestParam(required = false) String searchName,
            @ApiParam(value = "page num", required = true) @RequestParam Integer pageNo,
            @ApiParam(value = "page size", required = true) @RequestParam Integer pageSize,
            @ApiParam(value = "job mode") @RequestParam(required = false) String jobMode) {
        return Result.success(jobService.getJob(searchName, pageNo, pageSize, jobMode));
    }

    @GetMapping("/{jobId}")
    @ApiOperation(value = "get job definition", httpMethod = "GET")
    Result<JobDefinition> getJobDefinition(@PathVariable long jobId) {
        return Result.success(jobService.getJobDefinitionByJobId(jobId));
    }

    @DeleteMapping
    @ApiOperation(value = "delete job definition", httpMethod = "DELETE")
    Result<Void> deleteJobDefinition(
            @ApiParam(value = "id", required = true) @RequestParam long id) {
        jobService.deleteJob(id);
        return Result.success();
    }
}
