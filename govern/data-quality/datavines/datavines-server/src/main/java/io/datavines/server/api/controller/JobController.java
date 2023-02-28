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
import io.datavines.server.api.dto.bo.job.JobCreate;
import io.datavines.server.api.dto.bo.job.JobUpdate;
import io.datavines.server.repository.entity.Job;
import io.datavines.server.repository.service.JobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "job", tags = "job", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/job", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
@Validated
public class JobController {

    @Autowired
    private JobService jobService;

    @ApiOperation(value = "create job", response = long.class)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createJob(@Valid @RequestBody JobCreate jobCreate) throws DataVinesServerException {
        return jobService.create(jobCreate);
    }

    @ApiOperation(value = "delete job", response = int.class)
    @DeleteMapping(value = "/{id}")
    public Object deleteJob(@PathVariable Long id)  {
        return jobService.deleteById(id);
    }

    @ApiOperation(value = "update job", response = int.class)
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object updateJob(@Valid @RequestBody JobUpdate jobUpdate) throws DataVinesServerException {
        return jobService.update(jobUpdate);
    }

    @ApiOperation(value = "get job by id", response = Job.class)
    @GetMapping(value = "/{id}")
    public Object getById(@PathVariable Long id)  {
        return jobService.getById(id);
    }

    @ApiOperation(value = "list job by datasource id", response = Job.class, responseContainer = "list")
    @GetMapping(value = "list/{datasourceId}")
    public Object listByDataSourceId(@PathVariable Long datasourceId)  {
        return jobService.listByDataSourceId(datasourceId);
    }

    @ApiOperation(value = "get job page")
    @GetMapping(value = "/page")
    public Object page(@RequestParam(value = "searchVal", required = false) String searchVal,
                       @RequestParam("datasourceId") Long datasourceId,
                       @RequestParam("pageNumber") Integer pageNumber,
                       @RequestParam("pageSize") Integer pageSize)  {
        return jobService.getJobPage(searchVal, datasourceId, pageNumber, pageSize);
    }

    @ApiOperation(value = "execute job")
    @PostMapping(value = "/execute/{id}")
    public Object executeJob(@PathVariable("id") Long jobId) throws DataVinesServerException {
        return jobService.execute(jobId, null);
    }
}
