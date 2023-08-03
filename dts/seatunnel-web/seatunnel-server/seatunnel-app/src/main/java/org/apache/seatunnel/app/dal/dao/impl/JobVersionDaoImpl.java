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

import org.apache.seatunnel.app.dal.dao.IJobVersionDao;
import org.apache.seatunnel.app.dal.entity.JobVersion;
import org.apache.seatunnel.app.dal.mapper.JobVersionMapper;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JobVersionDaoImpl implements IJobVersionDao {

    @Resource private JobVersionMapper jobVersionMapper;

    @Override
    public void createVersion(JobVersion jobVersion) {
        jobVersionMapper.insert(jobVersion);
    }

    @Override
    public void updateVersion(JobVersion version) {
        jobVersionMapper.updateById(version);
    }

    @Override
    public JobVersion getLatestVersion(long jobId) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("job_id", jobId);
        return jobVersionMapper.selectByMap(queryMap).get(0);
    }

    @Override
    public List<JobVersion> getLatestVersionByJobIds(List<Long> jobIds) {
        QueryWrapper wrapper = new QueryWrapper<JobVersion>();
        wrapper.in("job_id", jobIds);
        return jobVersionMapper.selectList(wrapper);
    }

    @Override
    public JobVersion getVersionById(long jobVersionId) {
        return jobVersionMapper.selectById(jobVersionId);
    }

    @Override
    public List<JobVersion> getVersionsByIds(List<Long> jobVersionIds) {
        return jobVersionMapper.selectBatchIds(jobVersionIds);
    }
}
