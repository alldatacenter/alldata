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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.server.api.dto.vo.catalog.CatalogEntityIssueVO;
import io.datavines.server.api.dto.vo.catalog.CatalogEntityMetricVO;
import io.datavines.server.repository.entity.catalog.CatalogEntityMetricJobRel;
import io.datavines.server.repository.mapper.CatalogEntityMetricJobRelMapper;
import io.datavines.server.repository.service.CatalogEntityMetricJobRelService;
import org.springframework.stereotype.Service;

@Service("catalogEntityMetricJobRelService")
public class CatalogEntityMetricJobRelServiceImpl
        extends ServiceImpl<CatalogEntityMetricJobRelMapper, CatalogEntityMetricJobRel>
        implements CatalogEntityMetricJobRelService {

    @Override
    public IPage<CatalogEntityMetricVO> getEntityMetricPage(Page<CatalogEntityMetricVO> page, String uuid) {
        return baseMapper.getEntityMetricPage(page, uuid);
    }

    @Override
    public IPage<CatalogEntityIssueVO> getEntityIssuePage(Page<CatalogEntityIssueVO> page, String uuid) {
        return baseMapper.getEntityIssuePage(page, uuid);
    }
}
