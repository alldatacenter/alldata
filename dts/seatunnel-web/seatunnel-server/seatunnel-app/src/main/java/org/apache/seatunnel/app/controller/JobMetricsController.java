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
import org.apache.seatunnel.app.domain.response.metrics.JobDAG;
import org.apache.seatunnel.app.domain.response.metrics.JobPipelineDetailMetricsRes;
import org.apache.seatunnel.app.domain.response.metrics.JobPipelineSummaryMetricsRes;
import org.apache.seatunnel.app.service.IJobMetricsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.List;

/** @Description @ClassName JobMetricsController @Author zhang @Date 2023/7/6 18:30 */
@RequestMapping("/seatunnel/api/v1/job/metrics")
@RestController
public class JobMetricsController {

    @Resource private IJobMetricsService jobMetricsService;

    @GetMapping("/detail")
    @ApiOperation(value = "get the job pipeline detail metrics", httpMethod = "GET")
    public Result<List<JobPipelineDetailMetricsRes>> detail(
            @ApiParam(value = "userId", required = true) @RequestAttribute Integer userId,
            @ApiParam(value = "jobInstanceId", required = true) @RequestParam Long jobInstanceId)
            throws IOException {

        return Result.success(
                jobMetricsService.getJobPipelineDetailMetricsRes(userId, jobInstanceId));
    }

    @GetMapping("/dag")
    @ApiOperation(value = "get the job pipeline dag", httpMethod = "GET")
    public Result<JobDAG> getJobDAG(
            @ApiParam(value = "userId", required = true) @RequestAttribute Integer userId,
            @ApiParam(value = "jobInstanceId", required = true) @RequestParam Long jobInstanceId)
            throws JsonProcessingException {

        return Result.success(jobMetricsService.getJobDAG(userId, jobInstanceId));
    }

    @GetMapping("/summary")
    @ApiOperation(value = "get the job pipeline summary metrics", httpMethod = "GET")
    public Result<List<JobPipelineSummaryMetricsRes>> summary(
            @ApiParam(value = "userId", required = true) @RequestAttribute Integer userId,
            @ApiParam(value = "jobInstanceId", required = true) @RequestParam Long jobInstanceId)
            throws IOException {
        return Result.success(
                jobMetricsService.getJobPipelineSummaryMetrics(userId, jobInstanceId));
    }
}
