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

package org.apache.seatunnel.app.service.impl;

import org.apache.seatunnel.app.common.Result;
import org.apache.seatunnel.app.common.Status;
import org.apache.seatunnel.app.dal.dao.IJobDefinitionDao;
import org.apache.seatunnel.app.dal.dao.IJobInstanceDao;
import org.apache.seatunnel.app.dal.entity.JobDefinition;
import org.apache.seatunnel.app.domain.dto.job.SeaTunnelJobInstanceDto;
import org.apache.seatunnel.app.domain.response.metrics.JobSummaryMetricsRes;
import org.apache.seatunnel.app.service.BaseService;
import org.apache.seatunnel.app.service.IJobDefinitionService;
import org.apache.seatunnel.app.service.IJobMetricsService;
import org.apache.seatunnel.app.service.ITaskInstanceService;
import org.apache.seatunnel.app.utils.PageInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TaskInstanceServiceImpl implements ITaskInstanceService {

    @Autowired IJobInstanceDao jobInstanceDao;

    @Autowired IJobMetricsService jobMetricsService;

    @Autowired IJobDefinitionService jobDefinitionService;

    @Autowired BaseService baseService;

    @Autowired IJobDefinitionDao jobDefinitionDao;

    @Override
    public Result getSyncTaskInstancePaging(
            Integer userId,
            String jobDefineName,
            String executorName,
            String stateType,
            String startTime,
            String endTime,
            String syncTaskType,
            Integer pageNo,
            Integer pageSize) {
        JobDefinition jobDefinition = null;
        IPage<SeaTunnelJobInstanceDto> jobInstanceIPage = null;
        if (jobDefineName != null) {
            jobDefinition = jobDefinitionDao.getJobByName(jobDefineName);
        }

        Result<PageInfo<SeaTunnelJobInstanceDto>> result = new Result<>();
        PageInfo<SeaTunnelJobInstanceDto> pageInfo = new PageInfo<>(pageNo, pageSize);
        result.setData(pageInfo);
        baseService.putMsg(result, Status.SUCCESS);

        Date startDate = dateConverter(startTime);
        Date endDate = dateConverter(endTime);

        if (jobDefinition != null) {
            jobInstanceIPage =
                    jobInstanceDao.queryJobInstanceListPaging(
                            new Page<>(pageNo, pageSize),
                            startDate,
                            endDate,
                            jobDefinition.getId(),
                            syncTaskType);
        } else {
            jobInstanceIPage =
                    jobInstanceDao.queryJobInstanceListPaging(
                            new Page<>(pageNo, pageSize), startDate, endDate, null, syncTaskType);
        }

        List<SeaTunnelJobInstanceDto> records = jobInstanceIPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return result;
        }
        addJobDefineNameToResult(records);
        addRunningTimeToResult(records);
        jobPipelineSummaryMetrics(records, syncTaskType, userId);
        pageInfo.setTotal((int) jobInstanceIPage.getTotal());
        pageInfo.setTotalList(records);
        result.setData(pageInfo);
        return result;
    }

    private void addRunningTimeToResult(List<SeaTunnelJobInstanceDto> records) {
        for (SeaTunnelJobInstanceDto jobInstanceDto : records) {
            long runningTime = 0l;
            Date createTime = jobInstanceDto.getCreateTime();
            long createTimeSecond = createTime.toInstant().getEpochSecond();
            Date endTime = jobInstanceDto.getEndTime();
            if (endTime == null) {
                Date currentData = new Date();
                long currentDateSecond = currentData.toInstant().getEpochSecond();
                runningTime = Math.abs(currentDateSecond - createTimeSecond);
                jobInstanceDto.setRunningTime(runningTime);
            } else {
                long endTimeSecond = jobInstanceDto.getEndTime().toInstant().getEpochSecond();
                runningTime = Math.abs(endTimeSecond - createTimeSecond);
                jobInstanceDto.setRunningTime(runningTime);
            }
        }
    }

    private void addJobDefineNameToResult(List<SeaTunnelJobInstanceDto> records) {
        for (SeaTunnelJobInstanceDto jobInstanceDto : records) {
            JobDefinition jobDefinition =
                    jobDefinitionService.getJobDefinitionByJobId(jobInstanceDto.getJobDefineId());
            if (jobDefinition != null) {
                jobInstanceDto.setJobDefineName(jobDefinition.getName());
            }
        }
    }

    public Date dateConverter(String time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            Date date = dateFormat.parse(time);
            return date;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void jobPipelineSummaryMetrics(
            List<SeaTunnelJobInstanceDto> records, String syncTaskType, Integer userId) {
        try {
            ArrayList<Long> jobInstanceIdList = new ArrayList<>();
            HashMap<Long, Long> jobInstanceIdAndJobEngineIdMap = new HashMap<>();

            for (SeaTunnelJobInstanceDto jobInstance : records) {
                if (jobInstance.getId() != null && jobInstance.getJobEngineId() != null) {
                    jobInstanceIdList.add(jobInstance.getId());
                    jobInstanceIdAndJobEngineIdMap.put(
                            jobInstance.getId(), Long.valueOf(jobInstance.getJobEngineId()));
                }
            }

            Map<Long, JobSummaryMetricsRes> jobSummaryMetrics =
                    jobMetricsService.getALLJobSummaryMetrics(
                            userId,
                            jobInstanceIdAndJobEngineIdMap,
                            jobInstanceIdList,
                            syncTaskType);

            for (SeaTunnelJobInstanceDto taskInstance : records) {
                if (jobSummaryMetrics.get(taskInstance.getId()) != null) {
                    taskInstance.setWriteRowCount(
                            jobSummaryMetrics.get(taskInstance.getId()).getWriteRowCount());
                    taskInstance.setReadRowCount(
                            jobSummaryMetrics.get(taskInstance.getId()).getReadRowCount());
                }
            }
        } catch (Exception e) {
            for (SeaTunnelJobInstanceDto taskInstance : records) {
                log.error(
                        "instance {} {} set instance and engine id error", taskInstance.getId(), e);
            }
        }
    }
}
