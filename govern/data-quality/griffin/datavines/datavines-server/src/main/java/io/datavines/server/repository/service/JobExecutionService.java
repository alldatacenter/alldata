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
package io.datavines.server.repository.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.datavines.common.entity.job.SubmitJob;
import io.datavines.server.api.dto.vo.JobExecutionVO;
import io.datavines.server.api.dto.vo.MetricExecutionDashBoard;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.core.exception.DataVinesServerException;

public interface JobExecutionService extends IService<JobExecution> {

    long create(JobExecution jobExecution);

    int update(JobExecution jobExecution);

    JobExecution getById(long id);

    List<JobExecution> listByJobId(long jobId);

    int deleteByJobId(long jobId);

    IPage<JobExecutionVO> getJobExecutionPage(String searchVal, Long jobId, Integer pageNumber, Integer pageSize);

    Long submitJob(SubmitJob submitJob) throws DataVinesServerException;

    Long executeJob(JobExecution jobExecution) throws DataVinesServerException;

    Long killJob(Long taskId);

    List<JobExecution> listNeedFailover(String host);

    List<JobExecution> listJobExecutionNotInServerList(List<String> hostList);

    Object readErrorDataPage(Long jobExecutionId, Integer pageNumber, Integer pageSize);

    String getJobExecutionHost(Long jobExecutionId);

    List<MetricExecutionDashBoard> getMetricExecutionDashBoard(Long jobId, String startTime, String endTime);
}
