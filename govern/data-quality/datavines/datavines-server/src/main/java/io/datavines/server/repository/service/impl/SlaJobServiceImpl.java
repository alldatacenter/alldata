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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.common.exception.DataVinesException;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.sla.SlaJobCreateOrUpdate;
import io.datavines.server.api.dto.vo.SlaJobVO;
import io.datavines.server.repository.entity.SlaJob;
import io.datavines.server.repository.mapper.SlaJobMapper;
import io.datavines.server.repository.service.SlaJobService;
import io.datavines.server.utils.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SlaJobServiceImpl extends ServiceImpl<SlaJobMapper, SlaJob> implements SlaJobService {

    @Override
    public List<SlaJobVO>  listSlaJob(Long slaId) {
        return baseMapper.listSlaJob(slaId);
    }

    @Override
    public boolean createOrUpdateSlaJob(SlaJobCreateOrUpdate createOrUpdate) {
        SlaJob slaJob = null;
        if (createOrUpdate.getId() != null) {
            slaJob = getById(createOrUpdate.getId());
            if (slaJob != null) {
                BeanUtils.copyProperties(createOrUpdate, slaJob);
                slaJob.setUpdateBy(ContextHolder.getUserId());
                slaJob.setUpdateTime(LocalDateTime.now());
                return updateById(slaJob);
            } else {
                throw new DataVinesServerException(Status.SLA_JOB_IS_NOT_EXIST_ERROR, createOrUpdate.getId());
            }
        } else {
            LambdaQueryWrapper<SlaJob> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SlaJob::getJobId,createOrUpdate.getJobId());
            wrapper.eq(SlaJob::getWorkspaceId, createOrUpdate.getWorkspaceId());
            SlaJob one = getOne(wrapper);
            if (Objects.nonNull(one)){
                log.info("SlaJob has been create {}", createOrUpdate);
                throw new DataVinesException("SlaJob has been create");
            }

            slaJob = new SlaJob();
            BeanUtils.copyProperties(createOrUpdate, slaJob);
            slaJob.setCreateBy(ContextHolder.getUserId());
            slaJob.setUpdateBy(ContextHolder.getUserId());
            slaJob.setUpdateTime(LocalDateTime.now());
            return save(slaJob);
        }
    }
}
