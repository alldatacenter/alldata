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
package io.datavines.server.api.controller;

import io.datavines.core.aop.RefreshToken;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.common.entity.job.SubmitJob;
import io.datavines.server.api.dto.vo.JobExecutionResultVO;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.service.JobExecutionResultService;
import io.datavines.server.repository.service.JobExecutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

import static io.datavines.common.utils.OSUtils.judgeConcurrentHost;

@Api(value = "job", tags = "job", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/job/execution", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class JobExecutionController {

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private JobExecutionResultService jobExecutionResultService;

    @ApiOperation(value = "submit external data quality job", response = Long.class)
    @PostMapping(value = "/submit/data-quality", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object submitDataQualityJob(@Valid @RequestBody SubmitJob submitJob) throws DataVinesServerException {
        return jobExecutionService.submitJob(submitJob);
    }

    @ApiOperation(value = "submit external data reconciliation job", response = Long.class)
    @PostMapping(value = "/submit/data-reconciliation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object submitDataReconJob(@Valid @RequestBody SubmitJob submitJob) throws DataVinesServerException {
        return jobExecutionService.submitJob(submitJob);
    }

    @ApiOperation(value = "kill job", response = Long.class)
    @DeleteMapping(value = "/kill/{executionId}")
    public Object killTask(@PathVariable("executionId") Long executionId) {
        return jobExecutionService.killJob(executionId);
    }

    @ApiOperation(value = "get job execution status", response = String.class)
    @GetMapping(value = "/status/{executionId}")
    public Object getTaskStatus(@PathVariable("executionId") Long executionId) {
        return jobExecutionService.getById(executionId).getStatus().getDescription();
    }

    @ApiOperation(value = "get job execution list by job id", response = JobExecution.class, responseContainer = "list")
    @GetMapping(value = "/list/{jobId}")
    public Object getJobExecutionListByJobId(@PathVariable("jobId") Long jobId) {
        return jobExecutionService.listByJobId(jobId);
    }

    @Deprecated
    @ApiOperation(value = "get job execution result", response = JobExecutionResultVO.class)
    @GetMapping(value = "/result/{executionId}")
    public Object getJobExecutionResultInfo(@PathVariable("executionId") Long executionId) {
        return jobExecutionResultService.getResultVOByJobExecutionId(executionId);
    }

    @ApiOperation(value = "get job execution result", response = JobExecutionResultVO.class)
    @GetMapping(value = "/list/result/{executionId}")
    public Object getJobExecutionResultInfoList(@PathVariable("executionId") Long executionId) {
        return jobExecutionResultService.getResultVOListByJobExecutionId(executionId);
    }

    @ApiOperation(value = "get job execution page", response = JobExecutionResultVO.class, responseContainer = "page")
    @GetMapping(value = "/page")
    public Object page(@RequestParam(value = "searchVal", required = false) String searchVal,
                       @RequestParam("jobId") Long jobId,
                       @RequestParam("pageNumber") Integer pageNumber,
                       @RequestParam("pageSize") Integer pageSize)  {
        return jobExecutionService.getJobExecutionPage(searchVal, jobId, pageNumber, pageSize);
    }

    @ApiOperation(value = "get job execution error data page", response = Object.class, responseContainer = "page")
    @GetMapping(value = "/errorDataPage")
    public Object readErrorDataPage(@RequestParam("taskId") Long taskId,
                                    @RequestParam("pageNumber") Integer pageNumber,
                                    @RequestParam("pageSize") Integer pageSize,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {

        String taskHost = jobExecutionService.getJobExecutionHost(taskId);
        Boolean isConcurrentHost = judgeConcurrentHost(taskHost);
        if (isConcurrentHost) {
            return jobExecutionService.readErrorDataPage(taskId, pageNumber, pageSize);
        }
        response.sendRedirect(request.getScheme() + "://" + taskHost + "/api/v1/task/errorDataPage?taskId=" + taskId +
                "&pageNumber=" + pageNumber + "&pageSize="+pageSize);
        return null;
    }
}
