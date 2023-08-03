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
import org.apache.seatunnel.app.domain.response.executor.JobExecutorRes;
import org.apache.seatunnel.app.service.IJobExecutorService;
import org.apache.seatunnel.app.service.IJobInstanceService;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.io.IOException;

@Slf4j
@RequestMapping("/seatunnel/api/v1/job/executor")
@RestController
public class JobExecutorController {

    @Autowired IJobExecutorService jobExecutorService;
    @Resource private IJobInstanceService jobInstanceService;

    @GetMapping("/execute")
    @ApiOperation(value = "Execute synchronization tasks", httpMethod = "GET")
    public Result jobExecutor(
            @ApiParam(value = "userId", required = true) @RequestAttribute("userId") Integer userId,
            @ApiParam(value = "jobDefineId", required = true) @RequestParam("jobDefineId")
                    Long jobDefineId) {
        return jobExecutorService.jobExecute(userId, jobDefineId);
    }

    @GetMapping("/resource")
    @ApiOperation(value = "get the resource for job executor", httpMethod = "GET")
    public Result<JobExecutorRes> resource(
            @ApiParam(value = "userId", required = true) @RequestParam Integer userId,
            @ApiParam(value = "Job define id", required = true) @RequestParam Long jobDefineId)
            throws IOException {
        try {
            JobExecutorRes executeResource =
                    jobInstanceService.createExecuteResource(userId, jobDefineId);
            return Result.success(executeResource);
        } catch (Exception e) {
            log.error("Get the resource for job executor error", e);
            throw new SeatunnelException(SeatunnelErrorEnum.ILLEGAL_STATE, e.getMessage());
        }
    }

    @GetMapping("/pause")
    public Result jobPause(
            @ApiParam(value = "userId", required = true) @RequestAttribute("userId") Integer userId,
            @ApiParam(value = "jobInstanceId", required = true) @RequestParam Long jobInstanceId) {
        return jobExecutorService.jobPause(userId, jobInstanceId);
    }

    @GetMapping("/restore")
    public Result jobRestore(
            @ApiParam(value = "userId", required = true) @RequestAttribute("userId") Integer userId,
            @ApiParam(value = "jobInstanceId", required = true) @RequestParam Long jobInstanceId) {
        return jobExecutorService.jobStore(userId, jobInstanceId);
    }
}
