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

package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.common.utils.JSONUtils;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.job.schedule.JobScheduleCreateOrUpdate;
import io.datavines.server.api.dto.bo.job.schedule.MapParam;
import io.datavines.server.dqc.coordinator.quartz.ScheduleJobInfo;
import io.datavines.server.enums.ScheduleJobType;
import io.datavines.server.repository.entity.Job;
import io.datavines.server.repository.entity.JobSchedule;
import io.datavines.server.repository.mapper.JobScheduleMapper;
import io.datavines.server.repository.service.JobScheduleService;
import io.datavines.server.dqc.coordinator.quartz.QuartzExecutors;
import io.datavines.server.dqc.coordinator.quartz.DataQualityScheduleJob;
import io.datavines.server.dqc.coordinator.quartz.cron.StrategyFactory;
import io.datavines.server.dqc.coordinator.quartz.cron.FunCron;

import io.datavines.server.enums.JobScheduleType;
import io.datavines.server.repository.service.JobService;
import io.datavines.server.utils.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("jobScheduleService")
public class JobScheduleServiceImpl extends ServiceImpl<JobScheduleMapper, JobSchedule>  implements JobScheduleService {

    @Autowired
    private QuartzExecutors quartzExecutor;

    @Autowired
    private JobService jobService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobSchedule createOrUpdate(JobScheduleCreateOrUpdate jobScheduleCreateOrUpdate) throws DataVinesServerException {
        if (jobScheduleCreateOrUpdate.getId() != null && jobScheduleCreateOrUpdate.getId() != 0) {
            return update(jobScheduleCreateOrUpdate);
        } else {
            return create(jobScheduleCreateOrUpdate);
        }
    }

    private JobSchedule create(JobScheduleCreateOrUpdate jobScheduleCreate) throws DataVinesServerException {

        Long jobId = jobScheduleCreate.getJobId();
        JobSchedule jobSchedule = baseMapper.selectOne(new QueryWrapper<JobSchedule>().eq("job_id", jobId));
        if (jobSchedule != null) {
            throw new DataVinesServerException(Status.JOB_SCHEDULE_EXIST_ERROR, jobSchedule.getId());
        }

        jobSchedule = new JobSchedule();
        BeanUtils.copyProperties(jobScheduleCreate, jobSchedule);
        jobSchedule.setCreateBy(ContextHolder.getUserId());
        jobSchedule.setCreateTime(LocalDateTime.now());
        jobSchedule.setUpdateBy(ContextHolder.getUserId());
        jobSchedule.setUpdateTime(LocalDateTime.now());
        jobSchedule.setStatus(true);

        updateJobScheduleParam(jobSchedule, jobScheduleCreate.getType(), jobScheduleCreate.getParam());
        Job job = jobService.getById(jobId);
        if (job == null) {
            throw new DataVinesServerException(Status.JOB_NOT_EXIST_ERROR, jobId);
        } else {
            Long dataSourceId = job.getDataSourceId();
            if (dataSourceId == null) {
                throw new DataVinesServerException(Status.DATASOURCE_NOT_EXIST_ERROR);
            }
            try {
                addScheduleJob(jobScheduleCreate, jobSchedule, job);
            } catch (Exception e) {
                throw new DataVinesServerException(Status.ADD_QUARTZ_ERROR);
            }
        }

        if (baseMapper.insert(jobSchedule) <= 0) {
            log.info("create job schedule fail : {}", jobScheduleCreate);
            throw new DataVinesServerException(Status.CREATE_JOB_SCHEDULE_ERROR);
        }
        log.info("create job schedule success: datasource id : {}, job id :{}, cronExpression : {}",
                job.getDataSourceId(),
                jobSchedule.getJobId(),
                jobSchedule.getCronExpression());

        return jobSchedule;
    }

    private void addScheduleJob(JobScheduleCreateOrUpdate jobScheduleCreate, JobSchedule jobSchedule, Job job) throws ParseException {
        switch (JobScheduleType.of(jobScheduleCreate.getType())) {
            case CYCLE:
            case CRONTAB:
                ScheduleJobInfo scheduleJobInfo = getScheduleJobInfo(jobSchedule, job);
                quartzExecutor.addJob(DataQualityScheduleJob.class, scheduleJobInfo);
                break;
            case OFFLINE:
                break;
            default:
                throw new DataVinesServerException(Status.SCHEDULE_TYPE_NOT_VALIDATE_ERROR, jobScheduleCreate.getType());
        }
    }

    private JobSchedule update(JobScheduleCreateOrUpdate jobScheduleUpdate) throws DataVinesServerException {
        JobSchedule jobSchedule = getById(jobScheduleUpdate.getId());
        if (jobSchedule == null) {
            throw new DataVinesServerException(Status.JOB_SCHEDULE_NOT_EXIST_ERROR, jobScheduleUpdate.getId());
        }

        BeanUtils.copyProperties(jobScheduleUpdate, jobSchedule);
        jobSchedule.setUpdateBy(ContextHolder.getUserId());
        jobSchedule.setUpdateTime(LocalDateTime.now());

        updateJobScheduleParam(jobSchedule, jobScheduleUpdate.getType(), jobScheduleUpdate.getParam());
        Long jobId = jobScheduleUpdate.getJobId();
        Job job = jobService.getById(jobId);
        if (job == null) {
            throw new DataVinesServerException(Status.JOB_NOT_EXIST_ERROR, jobId);
        } else {
            Long dataSourceId = job.getDataSourceId();
            if (dataSourceId == null) {
                throw new DataVinesServerException(Status.DATASOURCE_NOT_EXIST_ERROR);
            }

            try {
                ScheduleJobInfo scheduleJobInfo = getScheduleJobInfo(jobSchedule, job);
                quartzExecutor.deleteJob(scheduleJobInfo);
                addScheduleJob(jobScheduleUpdate, jobSchedule, job);
            } catch (Exception e) {
                throw new DataVinesServerException(Status.ADD_QUARTZ_ERROR, e.getMessage());
            }
        }

        if (baseMapper.updateById(jobSchedule) <= 0) {
            log.info("update job schedule fail : {}", jobScheduleUpdate);
            throw new DataVinesServerException(Status.UPDATE_JOB_SCHEDULE_ERROR, jobScheduleUpdate.getId());
        }

        return jobSchedule;
    }

    private ScheduleJobInfo getScheduleJobInfo(JobSchedule jobSchedule, Job job) {
        return new ScheduleJobInfo(
                            ScheduleJobType.DATA_QUALITY,
                            job.getDataSourceId(),
                            job.getId(),
                            jobSchedule.getCronExpression(),
                            jobSchedule.getStartTime(),
                            jobSchedule.getEndTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(long id) {
        JobSchedule jobSchedule = getById(id);
        Job job = jobService.getById(jobSchedule.getJobId());
        ScheduleJobInfo scheduleJobInfo = getScheduleJobInfo(jobSchedule, job);
        Boolean deleteJob = quartzExecutor.deleteJob(scheduleJobInfo);
        if (!deleteJob ) {
            return 0;
        }
        return baseMapper.deleteById(id);
    }

    @Override
    public JobSchedule getByJobId(Long jobId) {
        return baseMapper.getByJobId(jobId);
    }

    @Override
    public JobSchedule getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public  List<String> getCron(MapParam mapParam){
        List<String> listCron = new ArrayList<String>();
        FunCron api = StrategyFactory.getByType(mapParam.getCycle());
        JobSchedule jobSchedule = new JobSchedule();
        String result1 = JSONUtils.toJsonString(mapParam);
        jobSchedule.setParam(result1);
        String cron = api.funcDeal(result1);
        listCron.add(cron);
        return listCron;
    }

    private void updateJobScheduleParam(JobSchedule jobSchedule, String type, MapParam param) {
        String paramStr = JSONUtils.toJsonString(param);
        switch (JobScheduleType.of(type)){
            case CYCLE:
                if (param == null) {
                    throw new DataVinesServerException(Status.SCHEDULE_PARAMETER_IS_NULL_ERROR);
                }

                if (param.getCycle() == null) {
                    throw new DataVinesServerException(Status.SCHEDULE_PARAMETER_IS_NULL_ERROR);
                }
                jobSchedule.setStatus(true);
                jobSchedule.setParam(paramStr);
                FunCron api = StrategyFactory.getByType(param.getCycle());
                jobSchedule.setCronExpression(api.funcDeal(paramStr));

                log.info("job schedule param: {}", paramStr);
                break;
            case CRONTAB:
                if (param == null) {
                    throw new DataVinesServerException(Status.SCHEDULE_PARAMETER_IS_NULL_ERROR);
                }

                Boolean isValid = quartzExecutor.isValid(param.getCrontab());
                if (!isValid) {
                    throw new DataVinesServerException(Status.SCHEDULE_CRON_IS_INVALID_ERROR, param.getCrontab());
                }
                jobSchedule.setStatus(true);
                jobSchedule.setParam(paramStr);
                jobSchedule.setCronExpression(param.getCrontab());
                break;
            case OFFLINE:
                jobSchedule.setStatus(false);
                break;
            default:
                throw new DataVinesServerException(Status.SCHEDULE_TYPE_NOT_VALIDATE_ERROR, type);
        }
    }
}
