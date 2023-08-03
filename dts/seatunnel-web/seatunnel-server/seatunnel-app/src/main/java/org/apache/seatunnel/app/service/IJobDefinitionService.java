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

package org.apache.seatunnel.app.service;

import org.apache.seatunnel.app.dal.entity.JobDefinition;
import org.apache.seatunnel.app.dal.entity.JobVersion;
import org.apache.seatunnel.app.domain.request.job.JobReq;
import org.apache.seatunnel.app.domain.response.PageInfo;
import org.apache.seatunnel.app.domain.response.job.JobDefinitionRes;
import org.apache.seatunnel.server.common.CodeGenerateUtils;

import lombok.NonNull;

import java.util.List;
import java.util.Map;

public interface IJobDefinitionService {

    long createJob(int userId, JobReq jobReq) throws CodeGenerateUtils.CodeGenerateException;

    PageInfo<JobDefinitionRes> getJob(String name, Integer pageNo, Integer pageSize);

    PageInfo<JobDefinitionRes> getJob(
            String name, Integer pageNo, Integer pageSize, String jobMode);

    Map<Long, String> getJob(@NonNull String name);

    JobDefinition getJobDefinitionByJobId(long jobId);

    List<JobVersion> getJobVersionByDataSourceId(long datasourceId);

    boolean getUsedByDataSourceIdAndVirtualTable(long datasourceId, String tableName);

    void deleteJob(long id);
}
