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

package org.apache.seatunnel.app.dal.dao;

import org.apache.seatunnel.app.dal.entity.JobInstance;
import org.apache.seatunnel.app.dal.mapper.JobInstanceMapper;
import org.apache.seatunnel.app.domain.dto.job.SeaTunnelJobInstanceDto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.NonNull;

import java.util.Date;
import java.util.List;

public interface IJobInstanceDao {

    JobInstance getJobInstance(@NonNull Long jobInstanceId);

    JobInstance getJobInstanceByEngineId(@NonNull Long jobEngineId);

    void update(@NonNull JobInstance jobInstance);

    void insert(@NonNull JobInstance jobInstance);

    JobInstanceMapper getJobInstanceMapper();

    IPage<SeaTunnelJobInstanceDto> queryJobInstanceListPaging(
            IPage<JobInstance> page,
            Date startTime,
            Date endTime,
            Long jobDefineId,
            String jobMode);

    List<JobInstance> getAllJobInstance(@NonNull List<Long> jobInstanceIdList);
}
