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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.server.api.dto.vo.catalog.CatalogEntityIssueVO;
import io.datavines.server.api.dto.vo.catalog.CatalogEntityMetricVO;
import io.datavines.server.repository.entity.catalog.CatalogEntityMetricJobRel;
import io.datavines.server.repository.mapper.CatalogEntityMetricJobRelMapper;
import io.datavines.server.repository.service.CatalogEntityMetricJobRelService;
import io.datavines.server.repository.service.JobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("catalogEntityMetricJobRelService")
public class CatalogEntityMetricJobRelServiceImpl
        extends ServiceImpl<CatalogEntityMetricJobRelMapper, CatalogEntityMetricJobRel>
        implements CatalogEntityMetricJobRelService {

    @Autowired
    private JobService jobService;

    @Override
    public IPage<CatalogEntityMetricVO> getEntityMetricPage(Page<CatalogEntityMetricVO> page, String uuid) {
        return baseMapper.getEntityMetricPage(page, uuid);
    }

    @Override
    public IPage<CatalogEntityIssueVO> getEntityIssuePage(Page<CatalogEntityIssueVO> page, String uuid) {
        return baseMapper.getEntityIssuePage(page, uuid);
    }

    @Override
    public boolean deleteByJobId(long jobId) {
        return remove(new QueryWrapper<CatalogEntityMetricJobRel>().eq("metric_job_id", jobId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByEntityUUID(List<String> uuidList) {
        List<CatalogEntityMetricJobRel> relList = list(new QueryWrapper<CatalogEntityMetricJobRel>().in("entity_uuid",uuidList));
        if (CollectionUtils.isEmpty(relList)) {
            return true;
        }
        for (CatalogEntityMetricJobRel rel : relList) {
            jobService.deleteById(rel.getMetricJobId());
        }

        return true;
    }
}
