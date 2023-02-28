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
import io.datavines.server.api.dto.bo.catalog.metadata.CatalogMetaDataFetchTaskScheduleCreateOrUpdate;
import io.datavines.server.api.dto.bo.job.schedule.MapParam;
import io.datavines.server.dqc.coordinator.quartz.CatalogTaskScheduleJob;
import io.datavines.server.dqc.coordinator.quartz.QuartzExecutors;
import io.datavines.server.dqc.coordinator.quartz.ScheduleJobInfo;
import io.datavines.server.dqc.coordinator.quartz.cron.StrategyFactory;
import io.datavines.server.dqc.coordinator.quartz.cron.FunCron;
import io.datavines.server.enums.JobScheduleType;
import io.datavines.server.enums.ScheduleJobType;
import io.datavines.server.repository.entity.DataSource;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchTaskSchedule;
import io.datavines.server.repository.mapper.CatalogMetaDataFetchTaskScheduleMapper;
import io.datavines.server.repository.service.CatalogMetaDataFetchTaskScheduleService;
import io.datavines.server.repository.service.DataSourceService;
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
@Service("catalogMetaDataFetcherTaskScheduleService")
public class CatalogMetaDataFetchTaskScheduleServiceImpl extends ServiceImpl<CatalogMetaDataFetchTaskScheduleMapper,CatalogMetaDataFetchTaskSchedule>  implements CatalogMetaDataFetchTaskScheduleService {

    @Autowired
    private QuartzExecutors quartzExecutor;

    @Autowired
    private DataSourceService dataSourceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatalogMetaDataFetchTaskSchedule createOrUpdate(CatalogMetaDataFetchTaskScheduleCreateOrUpdate scheduleCreateOrUpdate) throws DataVinesServerException {
        if (scheduleCreateOrUpdate.getId() != null && scheduleCreateOrUpdate.getId() != 0) {
            return update(scheduleCreateOrUpdate);
        } else {
            return create(scheduleCreateOrUpdate);
        }
    }

    private CatalogMetaDataFetchTaskSchedule create(CatalogMetaDataFetchTaskScheduleCreateOrUpdate scheduleCreateOrUpdate) throws DataVinesServerException {

        Long dataSourceId = scheduleCreateOrUpdate.getDataSourceId();
        CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule = baseMapper.selectOne(new QueryWrapper<CatalogMetaDataFetchTaskSchedule>().eq("datasource_id", dataSourceId));
        if (catalogMetaDataFetchTaskSchedule != null) {
            throw new DataVinesServerException(Status.CATALOG_TASK_SCHEDULE_EXIST_ERROR, catalogMetaDataFetchTaskSchedule.getId());
        }

        catalogMetaDataFetchTaskSchedule = new CatalogMetaDataFetchTaskSchedule();
        BeanUtils.copyProperties(scheduleCreateOrUpdate, catalogMetaDataFetchTaskSchedule);
        catalogMetaDataFetchTaskSchedule.setCreateBy(ContextHolder.getUserId());
        catalogMetaDataFetchTaskSchedule.setCreateTime(LocalDateTime.now());
        catalogMetaDataFetchTaskSchedule.setUpdateBy(ContextHolder.getUserId());
        catalogMetaDataFetchTaskSchedule.setUpdateTime(LocalDateTime.now());
        catalogMetaDataFetchTaskSchedule.setStatus(true);

        updateCatalogTaskScheduleParam(catalogMetaDataFetchTaskSchedule, scheduleCreateOrUpdate.getType(), scheduleCreateOrUpdate.getParam());
        DataSource dataSource = dataSourceService.getById(dataSourceId);
        if (dataSource == null) {
            throw new DataVinesServerException(Status.DATASOURCE_NOT_EXIST_ERROR, dataSourceId);
        } else {

            try {
                addScheduleJob(scheduleCreateOrUpdate, catalogMetaDataFetchTaskSchedule);
            } catch (Exception e) {
                throw new DataVinesServerException(Status.ADD_QUARTZ_ERROR);
            }
        }

        if (baseMapper.insert(catalogMetaDataFetchTaskSchedule) <= 0) {
            log.info("create catalog task schedule fail : {}", catalogMetaDataFetchTaskSchedule);
            throw new DataVinesServerException(Status.CREATE_CATALOG_TASK_SCHEDULE_ERROR);
        }

        log.info("create job schedule success: datasource id : {}, cronExpression : {}",
                catalogMetaDataFetchTaskSchedule.getDataSourceId(),
                catalogMetaDataFetchTaskSchedule.getCronExpression());

        return catalogMetaDataFetchTaskSchedule;
    }

    private void addScheduleJob(CatalogMetaDataFetchTaskScheduleCreateOrUpdate scheduleCreateOrUpdate, CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule) throws ParseException {
        switch (JobScheduleType.of(scheduleCreateOrUpdate.getType())) {
            case CYCLE:
            case CRONTAB:
                quartzExecutor.addJob(CatalogTaskScheduleJob.class, getScheduleJobInfo(catalogMetaDataFetchTaskSchedule));
                break;
            case OFFLINE:
                break;
            default:
                throw new DataVinesServerException(Status.SCHEDULE_TYPE_NOT_VALIDATE_ERROR, scheduleCreateOrUpdate.getType());
        }
    }

    private CatalogMetaDataFetchTaskSchedule update(CatalogMetaDataFetchTaskScheduleCreateOrUpdate scheduleCreateOrUpdate) throws DataVinesServerException {
        CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule = getById(scheduleCreateOrUpdate.getId());
        if (catalogMetaDataFetchTaskSchedule == null) {
            throw new DataVinesServerException(Status.CATALOG_TASK_SCHEDULE_NOT_EXIST_ERROR, scheduleCreateOrUpdate.getId());
        }

        BeanUtils.copyProperties(scheduleCreateOrUpdate, catalogMetaDataFetchTaskSchedule);
        catalogMetaDataFetchTaskSchedule.setUpdateBy(ContextHolder.getUserId());
        catalogMetaDataFetchTaskSchedule.setUpdateTime(LocalDateTime.now());

        updateCatalogTaskScheduleParam(catalogMetaDataFetchTaskSchedule, scheduleCreateOrUpdate.getType(), scheduleCreateOrUpdate.getParam());

        Long dataSourceId = scheduleCreateOrUpdate.getDataSourceId();
        if (dataSourceId == null) {
            throw new DataVinesServerException(Status.DATASOURCE_NOT_EXIST_ERROR);
        }

        try {
            quartzExecutor.deleteJob(getScheduleJobInfo(catalogMetaDataFetchTaskSchedule));
            addScheduleJob(scheduleCreateOrUpdate, catalogMetaDataFetchTaskSchedule);
        } catch (Exception e) {
            throw new DataVinesServerException(Status.ADD_QUARTZ_ERROR);
        }


        if (baseMapper.updateById(catalogMetaDataFetchTaskSchedule) <= 0) {
            log.info("update catalog task schedule fail : {}", catalogMetaDataFetchTaskSchedule);
            throw new DataVinesServerException(Status.UPDATE_CATALOG_TASK_SCHEDULE_ERROR, catalogMetaDataFetchTaskSchedule.getId());
        }

        return catalogMetaDataFetchTaskSchedule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(long id) {
        CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule = getById(id);

        Boolean deleteJob = quartzExecutor.deleteJob(getScheduleJobInfo(catalogMetaDataFetchTaskSchedule));
        if (!deleteJob ) {
            return 0;
        }
        return baseMapper.deleteById(id);
    }

    @Override
    public CatalogMetaDataFetchTaskSchedule getByDataSourceId(Long dataSourceId) {
        return baseMapper.getByDataSourceId(dataSourceId);
    }

    @Override
    public CatalogMetaDataFetchTaskSchedule getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public  List<String> getCron(MapParam mapParam){
        List<String> listCron = new ArrayList<String>();
        FunCron api = StrategyFactory.getByType(mapParam.getCycle());
        CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule = new CatalogMetaDataFetchTaskSchedule();
        String result1 = JSONUtils.toJsonString(mapParam);
        catalogMetaDataFetchTaskSchedule.setParam(result1);
        String cron = api.funcDeal(catalogMetaDataFetchTaskSchedule.getParam());
        listCron.add(cron);
        return listCron;
    }

    private void updateCatalogTaskScheduleParam(CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule, String type, MapParam param) {
        String paramStr = JSONUtils.toJsonString(param);
        switch (JobScheduleType.of(type)){
            case CYCLE:
                if (param == null) {
                    throw new DataVinesServerException(Status.SCHEDULE_PARAMETER_IS_NULL_ERROR);
                }

                if (param.getCycle() == null) {
                    throw new DataVinesServerException(Status.SCHEDULE_PARAMETER_IS_NULL_ERROR);
                }
                catalogMetaDataFetchTaskSchedule.setStatus(true);
                catalogMetaDataFetchTaskSchedule.setParam(paramStr);
                FunCron api = StrategyFactory.getByType(param.getCycle());
                catalogMetaDataFetchTaskSchedule.setCronExpression(api.funcDeal(catalogMetaDataFetchTaskSchedule.getParam()));

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
                catalogMetaDataFetchTaskSchedule.setStatus(true);
                catalogMetaDataFetchTaskSchedule.setParam(paramStr);
                catalogMetaDataFetchTaskSchedule.setCronExpression(param.getCrontab());
                break;
            case OFFLINE:
                catalogMetaDataFetchTaskSchedule.setStatus(false);
                break;
            default:
                throw new DataVinesServerException(Status.SCHEDULE_TYPE_NOT_VALIDATE_ERROR, type);
        }
    }

    private ScheduleJobInfo getScheduleJobInfo(CatalogMetaDataFetchTaskSchedule catalogMetaDataFetchTaskSchedule) {
        return new ScheduleJobInfo(
                ScheduleJobType.CATALOG,
                catalogMetaDataFetchTaskSchedule.getDataSourceId(),
                catalogMetaDataFetchTaskSchedule.getDataSourceId(),
                catalogMetaDataFetchTaskSchedule.getCronExpression(),
                catalogMetaDataFetchTaskSchedule.getStartTime(),
                catalogMetaDataFetchTaskSchedule.getEndTime());
    }
}
