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

import org.apache.seatunnel.app.dal.dao.IJobLineDao;
import org.apache.seatunnel.app.dal.entity.JobLine;
import org.apache.seatunnel.app.dal.mapper.JobLineMapper;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import javax.annotation.Resource;

import java.util.List;

@Service
public class JobLineDaoImpl implements IJobLineDao {

    @Resource private JobLineMapper jobLineMapper;

    @Override
    public void deleteLinesByVersionId(long jobVersionId) {
        jobLineMapper.deleteLinesByVersionId(jobVersionId);
    }

    @Override
    public void insertLines(List<JobLine> lines) {
        jobLineMapper.insertBatchLines(lines);
    }

    @Override
    public List<JobLine> getLinesByVersionId(long jobVersionId) {
        return jobLineMapper.selectList(
                Wrappers.lambdaQuery(new JobLine()).eq(JobLine::getVersionId, jobVersionId));
    }
}
