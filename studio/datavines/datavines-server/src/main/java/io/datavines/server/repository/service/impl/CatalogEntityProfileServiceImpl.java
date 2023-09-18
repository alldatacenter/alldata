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
import io.datavines.common.utils.DateUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.catalog.profile.CatalogProfileScheduleCreateOrUpdate;
import io.datavines.server.api.dto.bo.job.schedule.JobScheduleCreateOrUpdate;
import io.datavines.server.api.dto.vo.DataTime2ValueItem;
import io.datavines.server.repository.entity.JobSchedule;
import io.datavines.server.repository.entity.catalog.CatalogEntityMetricJobRel;
import io.datavines.server.repository.entity.catalog.CatalogEntityProfile;
import io.datavines.server.repository.mapper.CatalogEntityProfileMapper;
import io.datavines.server.repository.service.CatalogEntityMetricJobRelService;
import io.datavines.server.repository.service.CatalogEntityProfileService;
import io.datavines.server.repository.service.JobScheduleService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service("catalogEntityProfileService")
public class CatalogEntityProfileServiceImpl extends ServiceImpl<CatalogEntityProfileMapper, CatalogEntityProfile> implements CatalogEntityProfileService {

    @Resource
    private CatalogEntityMetricJobRelService catalogEntityMetricJobRelService;

    @Resource
    private JobScheduleService jobScheduleService;

    @Override
    public List<CatalogEntityProfile> getEntityProfileByUUID(String uuid, String dataDate) {
        return baseMapper.selectList(new QueryWrapper<CatalogEntityProfile>()
                .eq("entity_uuid", uuid)
                .eq("data_date", dataDate));
    }

    @Override
    public List<CatalogEntityProfile> getEntityProfileByUUIDAndMetric(String uuid, String metricName, String dataDate) {
        return baseMapper.selectList(new QueryWrapper<CatalogEntityProfile>()
                .eq("entity_uuid", uuid)
                .eq("metric_name", metricName)
                .eq("data_date", dataDate));
    }

    @Override
    public DataTime2ValueItem getCurrentTableRecords(String uuid) {

        List<CatalogEntityProfile> tableRowCounts = baseMapper.selectList(new QueryWrapper<CatalogEntityProfile>()
                .eq("entity_uuid", uuid)
                .eq("metric_name", "table_row_count")
                .orderByDesc("data_date"));

        if (CollectionUtils.isEmpty(tableRowCounts)) {
            return null;
        }

        return new DataTime2ValueItem(tableRowCounts.get(0).getDataDate(), tableRowCounts.get(0).getActualValue());
    }

    @Override
    public List<DataTime2ValueItem> listTableRecords(String uuid, String starTime, String endTime) {

        if (StringUtils.isEmpty(starTime) || StringUtils.isEmpty(endTime)) {
            starTime = DateUtils.dateToString(DateUtils.getSomeDay(new Date(), -7));
            endTime = DateUtils.dateToString(DateUtils.getSomeDay(new Date(), 1));
        }

        List<CatalogEntityProfile> tableRowCounts = baseMapper.selectList(new QueryWrapper<CatalogEntityProfile>()
                .eq("entity_uuid", uuid)
                .eq("metric_name", "table_row_count")
                .ge("data_date", starTime)
                .le("data_date", endTime)
                .orderByAsc("data_date"));

        return tableRowCounts.stream().map(item -> {
            return new DataTime2ValueItem(item.getDataDate(), item.getActualValue());
        }).collect(Collectors.toList());
    }

    @Override
    public Double getColumnUniqueCount(String uuid, String dataDate) {
        List<CatalogEntityProfile> distinctCount = baseMapper.selectList(new QueryWrapper<CatalogEntityProfile>()
                .eq("entity_uuid", uuid)
                .eq("metric_name", "column_unique")
                .eq("data_date", dataDate));

        if (CollectionUtils.isEmpty(distinctCount)) {
            return null;
        }

        return Double.valueOf(distinctCount.get(0).getActualValue());
    }

    @Override
    public JobSchedule createOrUpdate(CatalogProfileScheduleCreateOrUpdate createOrUpdate) throws DataVinesServerException {
        JobScheduleCreateOrUpdate jobScheduleCreateOrUpdate = new JobScheduleCreateOrUpdate();
        BeanUtils.copyProperties(createOrUpdate, jobScheduleCreateOrUpdate);

        String entityUUID = createOrUpdate.getEntityUUID();

        List<CatalogEntityMetricJobRel> listRel = catalogEntityMetricJobRelService.list(new QueryWrapper<CatalogEntityMetricJobRel>()
                .eq("entity_uuid", entityUUID)
                .eq("metric_job_type", "DATA_PROFILE"));

        if (CollectionUtils.isNotEmpty(listRel)) {
            jobScheduleCreateOrUpdate.setJobId(listRel.get(0).getMetricJobId());
        }

        return jobScheduleService.createOrUpdate(jobScheduleCreateOrUpdate);
    }

    @Override
    public JobSchedule getById(long id) {
        return jobScheduleService.getById(id);
    }

    @Override
    public JobSchedule getByEntityUUID(String entityUUID) {
        List<CatalogEntityMetricJobRel> listRel = catalogEntityMetricJobRelService.list(new QueryWrapper<CatalogEntityMetricJobRel>()
                .eq("entity_uuid", entityUUID)
                .eq("metric_job_type", "DATA_PROFILE"));

        if (CollectionUtils.isNotEmpty(listRel)) {
            return jobScheduleService.getByJobId(listRel.get(0).getMetricJobId());
        }

        return null;
    }
}
