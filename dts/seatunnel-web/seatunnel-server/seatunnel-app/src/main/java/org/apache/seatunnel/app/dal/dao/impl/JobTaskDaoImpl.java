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

import org.apache.seatunnel.app.dal.dao.IJobTaskDao;
import org.apache.seatunnel.app.dal.entity.JobTask;
import org.apache.seatunnel.app.dal.mapper.JobTaskMapper;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import javax.annotation.Resource;

import java.util.List;

@Repository
public class JobTaskDaoImpl implements IJobTaskDao {

    @Resource private JobTaskMapper jobTaskMapper;

    @Override
    public List<JobTask> getTasksByVersionId(long jobVersionId) {
        return jobTaskMapper.selectList(
                Wrappers.lambdaQuery(new JobTask()).eq(JobTask::getVersionId, jobVersionId));
    }

    @Override
    public void insertTask(JobTask jobTask) {
        if (jobTask != null) {
            jobTaskMapper.insert(jobTask);
        }
    }

    @Override
    public void updateTask(JobTask jobTask) {
        if (jobTask != null) {
            jobTaskMapper.updateById(jobTask);
        }
    }

    @Override
    public JobTask getTask(long jobVersionId, String pluginId) {
        return jobTaskMapper.selectOne(
                Wrappers.lambdaQuery(new JobTask())
                        .eq(JobTask::getVersionId, jobVersionId)
                        .and(i -> i.eq(JobTask::getPluginId, pluginId)));
    }

    @Override
    public List<JobTask> getJobTaskByDataSourceId(long datasourceId) {
        return jobTaskMapper.selectList(
                Wrappers.lambdaQuery(new JobTask()).eq(JobTask::getDataSourceId, datasourceId));
    }

    @Override
    public void updateTasks(List<JobTask> jobTasks) {
        jobTasks.forEach(jobTaskMapper::updateById);
    }

    @Override
    public void deleteTasks(List<Long> jobTaskIds) {
        if (!jobTaskIds.isEmpty()) {
            jobTaskMapper.deleteBatchIds(jobTaskIds);
        }
    }

    @Override
    public void deleteTask(long jobVersionId, String pluginId) {
        jobTaskMapper.delete(
                Wrappers.lambdaQuery(new JobTask())
                        .eq(JobTask::getVersionId, jobVersionId)
                        .and(i -> i.eq(JobTask::getPluginId, pluginId)));
    }
}
