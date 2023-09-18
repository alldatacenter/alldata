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
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.tenant.TenantCreate;
import io.datavines.server.api.dto.bo.tenant.TenantUpdate;
import io.datavines.server.repository.entity.Tenant;
import io.datavines.server.repository.mapper.TenantMapper;
import io.datavines.server.repository.service.TenantService;
import io.datavines.server.utils.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service("tenantService")
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Override
    public long create(TenantCreate tenantCreate) throws DataVinesServerException {
        if (isTenantExist(tenantCreate.getTenant())) {
            throw new DataVinesServerException(Status.TENANT_EXIST_ERROR, tenantCreate.getTenant());
        }
        Tenant tenant = new Tenant();
        BeanUtils.copyProperties(tenantCreate, tenant);
        tenant.setCreateBy(ContextHolder.getUserId());
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateBy(ContextHolder.getUserId());
        tenant.setUpdateTime(LocalDateTime.now());

        if (baseMapper.insert(tenant) <= 0) {
            log.info("create tenant fail : {}", tenantCreate);
            throw new DataVinesServerException(Status.CREATE_TENANT_ERROR, tenantCreate.getTenant());
        }

        return tenant.getId();
    }

    @Override
    public int update(TenantUpdate tenantUpdate) throws DataVinesServerException {

        Tenant tenant = getById(tenantUpdate.getId());
        if ( tenant == null) {
            throw new DataVinesServerException(Status.TENANT_NOT_EXIST_ERROR, tenantUpdate.getTenant());
        }

        BeanUtils.copyProperties(tenantUpdate, tenant);
        tenant.setUpdateBy(ContextHolder.getUserId());
        tenant.setUpdateTime(LocalDateTime.now());

        if (baseMapper.updateById(tenant) <= 0) {
            log.info("update tenant fail : {}", tenantUpdate);
            throw new DataVinesServerException(Status.UPDATE_TENANT_ERROR, tenantUpdate.getTenant());
        }

        return 1;
    }

    @Override
    public Tenant getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<Tenant> listByWorkspaceId(long workspaceId) {
        return baseMapper.selectList(new QueryWrapper<Tenant>().eq("workspace_id", workspaceId));
    }

    @Override
    public int deleteById(long id) {
        return baseMapper.deleteById(id);
    }

    private boolean isTenantExist(String name) {
        Tenant user = baseMapper.selectOne(new QueryWrapper<Tenant>().eq("tenant", name));
        return user != null;
    }
}
