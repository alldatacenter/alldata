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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import io.datavines.core.aop.RefreshToken;
import io.datavines.common.entity.job.SubmitJob;
import io.datavines.server.repository.service.JobExecutionResultService;
import io.datavines.core.exception.DataVinesServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.server.repository.service.JobExecutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.IOException;

import static io.datavines.common.utils.OSUtils.judgeConcurrentHost;

@Api(value = "task", tags = "task", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/task", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class TaskController {
//
//    @Autowired
//    private JobExecutionService jobExecutionService;
//
//    @Autowired
//    private JobExecutionResultService jobExecutionResultService;
//
//    @ApiOperation(value = "submit task")
//    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
//    public Object submitTask(@Valid @RequestBody SubmitJob submitJob) throws DataVinesServerException {
//        return jobExecutionService.submitJob(submitJob);
//    }
//
//    @ApiOperation(value = "kill task")
//    @DeleteMapping(value = "/kill/{id}")
//    public Object killTask(@PathVariable("id") Long taskId) {
//        return jobExecutionService.killJob(taskId);
//    }
//
//    @ApiOperation(value = "get task status")
//    @GetMapping(value = "/status/{id}")
//    public Object getTaskStatus(@PathVariable("id") Long taskId) {
//        return jobExecutionService.getById(taskId).getStatus().getDescription();
//    }
//
//    @ApiOperation(value = "get task list by job id")
//    @GetMapping(value = "/list/{jobId}")
//    public Object getTaskListByJobId(@PathVariable("jobId") Long jobId) {
//        return jobExecutionService.listByJobId(jobId);
//    }
//
//    @ApiOperation(value = "get task result")
//    @GetMapping(value = "/result/{taskId}")
//    public Object getTaskResultInfo(@PathVariable("taskId") Long taskId) {
//        return jobExecutionResultService.getResultVOByJobExecutionId(taskId);
//    }
//
//    @ApiOperation(value = "get task page")
//    @GetMapping(value = "/page")
//    public Object page(@RequestParam(value = "searchVal", required = false) String searchVal,
//                       @RequestParam("jobId") Long jobId,
//                       @RequestParam("pageNumber") Integer pageNumber,
//                       @RequestParam("pageSize") Integer pageSize)  {
//        return jobExecutionService.getJobExecutionPage(searchVal, jobId, pageNumber, pageSize);
//    }
//
//    @ApiOperation(value = "get task error data page")
//    @GetMapping(value = "/errorDataPage")
//    public Object readErrorDataPage(@RequestParam("taskId") Long taskId,
//                                    @RequestParam("pageNumber") Integer pageNumber,
//                                    @RequestParam("pageSize") Integer pageSize,
//                                    HttpServletRequest request, HttpServletResponse response) throws IOException {
//
//        String taskHost = jobExecutionService.getJobExecutionHost(taskId);
//        Boolean isConcurrentHost = judgeConcurrentHost(taskHost);
//        if (isConcurrentHost) {
//            return jobExecutionService.readErrorDataPage(taskId, pageNumber, pageSize);
//        }
//        response.sendRedirect(request.getScheme() + "://" + taskHost + "/api/v1/task/errorDataPage?taskId=" + taskId +
//                "&pageNumber=" + pageNumber + "&pageSize="+pageSize);
//        return null;
//    }
}
