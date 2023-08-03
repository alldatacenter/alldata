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
package org.apache.seatunnel.app.dal.dao.impl;

import org.apache.seatunnel.app.dal.dao.IJobMetricsDao;
import org.apache.seatunnel.app.dal.entity.JobMetrics;
import org.apache.seatunnel.app.dal.mapper.JobMetricsMapper;

import org.springframework.stereotype.Repository;

import lombok.NonNull;

import javax.annotation.Resource;

import java.util.List;

@Repository
public class JobMetricsDaoImpl implements IJobMetricsDao {

    @Resource private JobMetricsMapper jobMetricsMapper;

    @Override
    public List<JobMetrics> getByInstanceId(@NonNull Long jobInstanceId) {
        return jobMetricsMapper.queryJobMetricsByInstanceId(jobInstanceId);
    }

    @Override
    public JobMetricsMapper getJobMetricsMapper() {
        return jobMetricsMapper;
    }
}
