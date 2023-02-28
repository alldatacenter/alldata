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
import io.datavines.server.repository.entity.catalog.CatalogEntityRel;
import io.datavines.server.repository.entity.catalog.CatalogSchemaChange;
import io.datavines.server.repository.mapper.CatalogEntityRelMapper;
import io.datavines.server.repository.mapper.CatalogSchemaChangeMapper;
import io.datavines.server.repository.service.CatalogEntityRelService;
import io.datavines.server.repository.service.CatalogSchemaChangeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("catalogSchemaChangeService")
public class CatalogSchemaChangeServiceImpl extends ServiceImpl<CatalogSchemaChangeMapper, CatalogSchemaChange> implements CatalogSchemaChangeService {

    @Override
    public List<CatalogSchemaChange> getSchemaChangeList(String uuid) {
        return baseMapper.selectList(new QueryWrapper<CatalogSchemaChange>().eq("entity_uuid", uuid).or().eq("parent_uuid", uuid));
    }
}
