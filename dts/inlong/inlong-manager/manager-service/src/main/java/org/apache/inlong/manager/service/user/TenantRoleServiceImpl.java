/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.user;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongTenantEntity;
import org.apache.inlong.manager.dao.entity.TenantUserRoleEntity;
import org.apache.inlong.manager.dao.mapper.InlongTenantEntityMapper;
import org.apache.inlong.manager.dao.mapper.TenantUserRoleEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.TenantRoleInfo;
import org.apache.inlong.manager.pojo.user.TenantRolePageRequest;
import org.apache.inlong.manager.pojo.user.TenantRoleRequest;
import org.apache.inlong.manager.pojo.user.UserRoleCode;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.apache.inlong.common.util.BasicAuth.DEFAULT_TENANT;
import static org.apache.inlong.manager.common.enums.ErrorCodeEnum.TENANT_NOT_EXIST;

/**
 * Tenant Role operation
 */
@Slf4j
@Service
public class TenantRoleServiceImpl implements TenantRoleService {

    @Autowired
    private TenantUserRoleEntityMapper tenantUserRoleEntityMapper;

    @Autowired
    private InlongTenantEntityMapper tenantMapper;

    @Override
    public PageResult<TenantRoleInfo> listByCondition(TenantRolePageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<TenantUserRoleEntity> entityPage = tenantUserRoleEntityMapper.listByCondition(request);
        List<TenantRoleInfo> tenantRoleInfos = CommonBeanUtils.copyListProperties(entityPage, TenantRoleInfo::new);
        return new PageResult<>(tenantRoleInfos,
                entityPage.getTotal(),
                entityPage.getPageNum(), entityPage.getPageSize());
    }

    @Override
    public int save(TenantRoleRequest request, String operator) {
        String tenantName = request.getTenant();
        String username = request.getUsername();
        Preconditions.expectNotBlank(tenantName, "Failed to save tenant user role, tenant should not be blank");
        Preconditions.expectNotBlank(username, "Failed to save tenant user role, user should not be blank");
        Preconditions.expectNotBlank(request.getRoleCode(),
                "Failed to save tenant user role, role code should not be blank");

        InlongTenantEntity tenant = tenantMapper.selectByName(tenantName);
        Preconditions.expectNotNull(tenant, TENANT_NOT_EXIST, String.format(TENANT_NOT_EXIST.getMessage(), tenantName));

        TenantUserRoleEntity entity = CommonBeanUtils.copyProperties(request, TenantUserRoleEntity::new);
        entity.setCreator(operator);
        entity.setModifier(operator);
        tenantUserRoleEntityMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(TenantRoleRequest request, String operator) {
        TenantUserRoleEntity exist = tenantUserRoleEntityMapper.selectById(request.getId());
        Preconditions.expectNotNull(exist, ErrorCodeEnum.RECORD_NOT_FOUND,
                String.format("tenant user role record not found by id=%s", request.getId()));

        TenantUserRoleEntity entity = CommonBeanUtils.copyProperties(request, TenantUserRoleEntity::new);
        entity.setModifier(operator);
        int rowCount = tenantUserRoleEntityMapper.updateById(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format(
                            "fail to update tenant user role with id=%d, request version=%d, updated row=%d",
                            request.getId(), request.getVersion(), rowCount));
        }
        return true;
    }

    @Override
    public TenantRoleInfo get(int id) {
        TenantUserRoleEntity entity = tenantUserRoleEntityMapper.selectById(id);
        if (entity == null) {
            log.debug("not found valid tenant role by id={}", id);
            return null;
        }
        return CommonBeanUtils.copyProperties(entity, TenantRoleInfo::new);
    }

    @Override
    public TenantRoleInfo getByUsernameAndTenant(String name, String tenant) {
        TenantUserRoleEntity entity = tenantUserRoleEntityMapper.selectByUsernameAndTenant(name, tenant);
        if (entity == null) {
            log.debug("not found valid tenant role for name={}, tenant={}", name, tenant);
            return null;
        }
        return CommonBeanUtils.copyProperties(entity, TenantRoleInfo::new);
    }

    @Override
    public List<String> listTenantByUsername(String username) {
        return tenantUserRoleEntityMapper.listByUsername(username);
    }

    @Override
    public Boolean delete(Integer id) {
        String operator = LoginUserUtils.getLoginUser().getName();
        log.info("begin to delete inlong tenant role id={} by user={}", id, operator);
        int success = tenantUserRoleEntityMapper.deleteById(id);
        Preconditions.expectTrue(success == 1, "delete tenant role failed");
        log.info("success delete inlong tenant role id={} by user={}", id, operator);
        return true;
    }

    @Override
    public Integer saveDefault(String username, String operator) {
        // make default public tenant permission
        TenantRoleRequest tenantRoleRequest = new TenantRoleRequest();
        tenantRoleRequest.setTenant(DEFAULT_TENANT);
        tenantRoleRequest.setRoleCode(UserRoleCode.TENANT_OPERATOR);
        tenantRoleRequest.setUsername(username);
        return this.save(tenantRoleRequest, operator);
    }

}
