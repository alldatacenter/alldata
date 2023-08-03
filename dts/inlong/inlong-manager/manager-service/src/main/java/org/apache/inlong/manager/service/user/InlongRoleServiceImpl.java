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
import org.apache.inlong.manager.dao.entity.InlongUserRoleEntity;
import org.apache.inlong.manager.dao.mapper.InlongUserRoleEntityMapper;
import org.apache.inlong.manager.pojo.user.InlongRoleInfo;
import org.apache.inlong.manager.pojo.user.InlongRolePageRequest;
import org.apache.inlong.manager.pojo.user.InlongRoleRequest;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InlongRoleServiceImpl implements InlongRoleService {

    @Autowired
    private InlongUserRoleEntityMapper inlongUserMapper;

    @Override
    public PageInfo<InlongRoleInfo> listByCondition(InlongRolePageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongUserRoleEntity> entityPage = inlongUserMapper.selectByCondition(request);
        return entityPage.toPageInfo(entity -> CommonBeanUtils.copyProperties(entity, InlongRoleInfo::new));
    }

    @Override
    public int save(InlongRoleRequest request, String operator) {
        String username = request.getUsername();
        Preconditions.expectNotBlank(username, "Failed to save inlong user role, user should not be blank");
        Preconditions.expectNotBlank(request.getRoleCode(),
                "Failed to save inlong user role, role code should not be blank");

        InlongUserRoleEntity entity = CommonBeanUtils.copyProperties(request, InlongUserRoleEntity::new);
        entity.setCreator(operator);
        entity.setModifier(operator);
        inlongUserMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(InlongRoleRequest request, String operator) {
        InlongUserRoleEntity exist = inlongUserMapper.selectById(request.getId());
        Preconditions.expectNotNull(exist, ErrorCodeEnum.RECORD_NOT_FOUND,
                String.format("inlong user role record not found by id=%s", request.getId()));

        InlongUserRoleEntity entity = CommonBeanUtils.copyProperties(request, InlongUserRoleEntity::new);
        entity.setModifier(operator);
        int rowCount = inlongUserMapper.updateById(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format(
                            "failure to update inlong user role with id=%d, request version=%d, updated row=%d",
                            request.getId(), request.getVersion(), rowCount));
        }
        return true;
    }

    @Override
    public InlongRoleInfo get(int id) {
        InlongUserRoleEntity entity = inlongUserMapper.selectById(id);
        if (entity == null) {
            log.debug("not found valid inlong role by id={}", id);
            return null;
        }
        return CommonBeanUtils.copyProperties(entity, InlongRoleInfo::new);
    }

    @Override
    public InlongRoleInfo getByUsername(String username) {
        InlongUserRoleEntity entity = inlongUserMapper.selectByUsername(username);
        if (entity == null) {
            log.debug("not found valid inlong role by name={}", username);
            return null;
        }
        return CommonBeanUtils.copyProperties(entity, InlongRoleInfo::new);
    }
}
